/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


#include "iagent_base.h"

//external
#include "ams_sdk_interface.h"
#include "ams_constants.h"
#include "rest-engine.h"
#ifdef RUN_AS_BROKER_MODULE
#include "broker_rest_convert.h"
#endif

char g_config_path[MAX_PATH_LEN] = {0};
int g_cloud_sock = -1;
//int g_is_cloud_connected = iReady_To_Connect;
int g_is_connection_status_changed = 0;

pthread_mutex_t g_sock_mutex1 = PTHREAD_MUTEX_INITIALIZER;
msg_queue_t *g_cloud_queue;
msg_queue_t *g_cloud_ack_queue;
extern sync_ctx_t *g_ilink_ctx;

bool g_report_register = 0;
char g_mac_addr[30];

time_t g_last_cloud_ping_time = 0;
time_t g_connection_status_change_time;
char tbuf_conection_status_change_time[32];

char g_version[64] = "";
char g_rollbackversion[64] = "";

bool g_has_data_cached = 0;

bool g_cache_msg = false;
int g_max_cloud_queue_size = 1000;
int g_cache_delay_after_disconnect = 60;  // cache msg after 60s disconnection.
unsigned int g_message_drop_num = 0;
int g_cloud_server_port = IBROKER_SERVER_PORT;
char g_cloud_server_ip[512] = SERVER_IP;
bool g_watch_daemon = true;
extern char g_quit_thread;
/**
 * If the gateway has been disconnected from the cloud for more than g_max_connect_duration
 * seconds, than we need reboot the gateway.
 */
int g_max_connect_duration = 3600;


void ilink_init()
{
    g_cloud_queue = create_queue();
    g_cloud_ack_queue = create_queue();
    g_ilink_ctx = create_sync_ctx();

    rest_init_engine();
}


static void ilink_send(char *response, int len)
{
    post_msg (g_cloud_queue, response, len);
}


void ilink_msg_send(ilink_message_t *msg)
{
    int len;
    char *frame = compose_ilink_frame(msg, &len);

    // note: the post_msg() need caller to alloc the message body
    //       the frame will be released by the receiver thread.
    if(frame)
    {
        ilink_send(frame, len);
    }
    else
    {
        LOG_MSG("compose link message was NULL");
    }

}


// path size must be MAX_PATH_LEN
char *config_filepath(char *path, char *filename)
{
    strcpy(path, g_config_path);
    strcat(path, filename);

    return path;
}

char *my_ini_filename()
{
    static char g_ini_path[MAX_PATH_LEN] = {0};

    if (g_ini_path[0])
        return g_ini_path;

    get_product_config_pathname("gw_broker.ini", TT_DEVICE, NULL, g_ini_path);

    return g_ini_path;
}


void load_ini()
{
    dictionary  *ini;
    extern int g_ping_expiry_sec;

    char * ini_path = my_ini_filename();
    ini = iniparser_load(ini_path );
    if (NULL != ini)
    {
        const char *p = (iniparser_getstring(ini, "ibroker:ip", SERVER_IP));
        strcpy (g_cloud_server_ip, (char*)p);
        g_cloud_server_port = iniparser_getint(ini, "ibroker:port", IBROKER_SERVER_PORT);
        g_ping_expiry_sec = iniparser_getint(ini, "ibroker:ping_expiry_sec", 60);
        g_max_connect_duration = iniparser_getint(ini, "ibroker:reconnect_sec", 3600);

        // ensure the system won't reset too frequently by wrong settings
        if (g_max_connect_duration < 60 * 10 && g_max_connect_duration != 0)
            g_max_connect_duration = 60 * 10;

        iniparser_freedict(ini);
    }
    else if( 0 != access( ini_path, 0))
    {
          TraceD(FLAG_CLOUD_CONNECT, "ini file for cloud ip [%s] doesn't exist", ini_path );
    }
    else
    {
        TraceD(FLAG_CLOUD_CONNECT, "parse ini file [%s] failed", ini_path );
    }
}


/*
 * ibroker send internal functions
 */
bool wakeup_main_thread(int sock)
{
    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = inet_addr("127.0.0.1");
    addr.sin_port = htons(I_COAP_SERVER_PORT);
    sendto (sock, "wake", 5, 0, (struct sockaddr *) &addr, sizeof(addr));

    return true;
}


cloud_status_e get_cloud_connection_status()
{
    return g_cloud_status;
}


static void broad_cloud_status(cloud_status_e status)
{
    MESSAGE_CONFIG msgConfig = {0};
    JSON_Value *payload_val = json_value_init_object();
    JSON_Object *payload_obj = json_object(payload_val);
    json_object_set_number(payload_obj, "cloud_status", (int)status);
    char *payload = json_serialize_to_string(payload_val);

    if (!payload)
    {
        LOG_GOTO("Set payload failed when send cloud status.\n", end);
    }

    if(!setup_bus_restful_message(&msgConfig, (char *)TAG_EVENT, IA_APPLICATION_JSON, "/ilink", NULL, 0, payload, strlen(payload)+1))
    {
        LOG_GOTO("Set message failed when send cloud status.\n", end);
    }

    publish_message_cfg_on_broker(&msgConfig);

end:
    json_value_free(payload_val);
    if (payload) json_free_serialized_string(payload);
}


void set_cloud_connection_status(cloud_status_e status)
{
    g_connection_status_change_time = bh_get_tick_sec();
    struct tm *tm_tmp;
    time_t t = time(NULL);
    tm_tmp = localtime (&t);
    strftime (tbuf_conection_status_change_time, 32, "[%y-%m-%d %H:%M:%S]\n", tm_tmp);

    g_is_connection_status_changed = (g_cloud_status!=status);
#ifdef RUN_AS_BROKER_MODULE
    if ((iReady_For_Work == status && iReady_For_Work != g_cloud_status) ||
            (iReady_For_Work != status && iReady_For_Work == g_cloud_status))
    {
        broad_cloud_status(status);
    }
#endif
    if (g_cloud_status != status)
        TraceD(FLAG_CLOUD_CONNECT, "ilink status changes from %d to %d\n", g_cloud_status, status);
    if (status == iReady_For_Work)
        TraceD(FLAG_CLOUD_CONNECT, "ilink is ready for work!\n");

    g_cloud_status = status;
}


void prepare_reconnect_cloud()
{
    // tell main tread not listening on it.
    // when main thread get this, then change it to 0 for this thread reconnectng.
    if (g_cloud_status != iReady_To_Connect)
    {
        set_cloud_connection_status (iError_In_Connection);

        wakeup_main_thread(g_socket_coap_ep);

        TraceD (FLAG_CLOUD_CONNECT, "prepare to reconnect to cloud");
    }
}


bool send_cloud_msg(const void *msg, unsigned int len)
{
    assert (g_cloud_sock != -1);
    int cnt = 0;
    ssize_t ret;

resend:
    ret = send(g_cloud_sock, msg, len, 0);
    if (ret == -1)
    {
        LOG_MSG ("send message to cloud error: result=-1");

        if(errno == ECONNRESET)
        {
            prepare_reconnect_cloud();
            LOG_RETURN (false);
        }

        // repeat sending if the outbuffer is full
        if(errno == EAGAIN || errno == EWOULDBLOCK)
        {
            cnt ++;
            if(cnt > 10)
            {
                prepare_reconnect_cloud();
                LOG_RETURN (false);
            }

            sleep(2);
            goto resend;

        }
    }

    return (ret == len);
}


static bool setup_cloud_connect()
{
    struct sockaddr_in server;

    if (g_cloud_sock != -1)
    {
        TraceI (FLAG_CLOUD_CONNECT,"close existing socket %d", g_cloud_sock);
        close (g_cloud_sock);
        g_cloud_sock = -1;
    }

    //Create socket
    g_cloud_sock = socket(AF_INET, SOCK_STREAM | SOCK_CLOEXEC, 0);
    if (g_cloud_sock == -1)
    {
        ERROR("failed create cloud socket: %d", errno);
        return false;
    }

    /*Remote address of the cloud server*/
    struct hostent *nlp_host;
    // Get the IP address from domain name
    if ((nlp_host=gethostbyname (g_cloud_server_ip)) == NULL)
    {
        WARNING("Failed to get IP address from cloud server domain name(%s), try again in 10 seconds...", g_cloud_server_ip);
    }
    else
    {
        server.sin_addr.s_addr = ((struct in_addr*)(nlp_host->h_addr))->s_addr;
        server.sin_family = AF_INET;
        server.sin_port = htons(  g_cloud_server_port );

        TraceI(FLAG_CLOUD_CONNECT, "cloud socket server: [%s] : [%d]", g_cloud_server_ip, g_cloud_server_port);
        if (connect(g_cloud_sock , (struct sockaddr *)&server , sizeof(server)) < 0)
        {
            WARNING("connecting cloud failed, errno: %d. waiting 10 seconds for another try...\n", errno);
        }
        else
        {
            // always send the FIRST_CONTACT after socket connected
           TraceI (FLAG_CLOUD_CONNECT, "socket connection is established");
           int flags;
           flags=fcntl (g_cloud_sock,F_GETFL,0);
           fcntl (g_cloud_sock,F_SETFL,flags | O_NONBLOCK);

           set_cloud_connection_status (iSocket_Connected);

           //update the last cloud ping time after successful connection
           //time(&g_last_cloud_ping_time);

           // wake up the main thread, so the main thread will register active clients immediately
           // g_report_register = true;
           wakeup_main_thread(g_socket_coap_ep);

           // ATTENTION: Don't change below trace, it's used for automation test.
           TraceI (FLAG_CLOUD_CONNECT, "cloud connection is ready for work now. \n");

           return true;
        }
    }

    return false;
}

extern void *thread_ilink_sender(void *arg)
{
#ifdef RUN_ON_LINUX
    prctl (PR_SET_NAME, "iagent_link_sender");
#endif
    time_t t, next_try_time = 0;
    /* Load the cloud server ip address from */
    char *cloud_server_addr = g_cloud_server_ip;

    // Initialize it.
    g_connection_status_change_time = bh_get_tick_sec();

    while (g_quit_thread == 0)
    {
        msg_t *msg;
        if (g_cloud_status < iSocket_Connected && (t = bh_get_tick_sec()) > next_try_time )
        {
            next_try_time = t + 10;

            if (g_cloud_status != iReady_To_Connect)
            {
                TraceV (FLAG_CLOUD_CONNECT,"Waiting main thread cleanup for connecting cloud server for 10 seconds...");
            }
            else
            {
                //Connect to remote server
                load_ini();

                /**
                 * If the gateway has been disconnected from cloud for more than g_max_connect_duration seconds, we need
                 * re-boot the gateway.
                 */
                if ((t - g_connection_status_change_time) > g_max_connect_duration && g_max_connect_duration != 0 )
                {
                    ERROR("Gateway has been disconnected from cloud for %d seconds, now reboot gateway and reconnect again.",
                            bh_get_tick_sec() - g_connection_status_change_time);
                    system("reboot");
                }

                if(!setup_cloud_connect())
                {
                    WARNING("failed to first contact cloud, waiting 60s for another try");
                    next_try_time = t + 60;
                    continue;
                }

                // agent id provision
                if (!get_self_agent_id())
                {
                    set_cloud_connection_status (iCloud_Provisioning);
                    init_provision();
                }
                else
                {
                    init_handshake();
                    first_handshake();
                    set_cloud_connection_status (iCloud_Handshaking);
                }
            }
        }

//check_queue:
        msg = get_msg(g_cloud_queue, 5);

        if (msg)
        {
            if (g_cloud_status > iSocket_Connected)
            {
                send_cloud_msg(msg->body, msg->len);
            }

            if(msg)
            {
                free(msg->body);
                free(msg);
            }
        }
    }

    return NULL;
}
