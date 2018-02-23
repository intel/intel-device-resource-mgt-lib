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


#include <stdlib.h>
#include <unistd.h>

#include "proxy.h"
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/xlogging.h"
#include "azure_c_shared_utility/crt_abstractions.h"
//#include "messageproperties.h"
#include "message.h"
#include "module.h"
#include "broker.h"

#include <parson.h>
#include "scredis.h"

#define DEBUG_PRINT //printf

redis_client *redis_h = NULL;
char redis_ip_buff[16];
char redis_port_buff[7];
char url_map_buff[65];
char redis_pub_name[70];
char redis_get_name[70];

char module_name[128];
unsigned char shared_buff[1024];
char redis_topic[129];
    
int redis_lock = 0;  
int refresh_rd_flag = 0;


typedef struct SIMULATEDDEVICE_DATA_TAG
{
    BROKER_HANDLE       broker;
    THREAD_HANDLE       monitor_thread;
    const char *        map_url_str;
    unsigned int        messagePeriod;
    unsigned int        simulatedDeviceRunning : 1;
} SIMULATEDDEVICE_DATA;

typedef struct SIMULATEDDEVICE_CONFIG_TAG
{
    char *              map_url_str;
    unsigned int        messagePeriod;
} SIMULATEDDEVICE_CONFIG;


static int create_post_response(SIMULATEDDEVICE_DATA* module_data, const char* id, char* msg_payload)
{
    MESSAGE_CONFIG newMessageCfg;
    MAP_HANDLE newProperties = Map_Create(NULL);
    if (newProperties == NULL)
    {
        LogError("Failed to create message properties");
        goto j_cpr_exit;
    }
    else
    {       
        // _tag = resp
        if (Map_Add(newProperties, "_tag", "resp") != MAP_OK)  goto j_cpr_exit;

        // _status = 69
        if (Map_Add(newProperties, "_status", "69") != MAP_OK) goto j_cpr_exit;

        // _fmt = 0
        if (Map_Add(newProperties, "_fmt", "0") != MAP_OK)  goto j_cpr_exit;

        // _src = proxy
        if (Map_Add(newProperties, "_src", module_name) != MAP_OK)  goto j_cpr_exit;

        // _id = 0       
        if (Map_Add(newProperties, "_id", id) != MAP_OK) goto j_cpr_exit;
        
        // _dest = agent       
        if (Map_Add(newProperties, "_dest", "agent") != MAP_OK) goto j_cpr_exit;
        
        newMessageCfg.sourceProperties = newProperties;
        newMessageCfg.size = strlen(msg_payload);
        newMessageCfg.source = (const unsigned char*)msg_payload;
        MESSAGE_HANDLE newMessage = Message_Create(&newMessageCfg);
        
        if (newMessage == NULL)
        {
            LogError("Failed to create new message");
            goto j_cpr_exit;
        }
        else
        {
            if (Broker_Publish(module_data->broker, (MODULE_HANDLE)module_data, newMessage) != BROKER_OK)
            {
                puts("Failed to create new message");
                goto j_cpr_exit;
            }
            Message_Destroy(newMessage);
            Map_Destroy(newProperties);            
            return 0;
        }

    j_cpr_exit: 
        if (newProperties) Map_Destroy(newProperties);
        printf("create_post_response error\r\n");
        return -1;

    }    
}

static int report_rd(SIMULATEDDEVICE_DATA* module_data, char* msg_payload)
{
    MESSAGE_CONFIG newMessageCfg;
    MAP_HANDLE newProperties = Map_Create(NULL);
    if (newProperties == NULL)
    {
        LogError("Failed to create message properties");
        goto j_rr_exit;
    }
    else
    {       
        // _tag = req
        if (Map_Add(newProperties, "_tag", "req") != MAP_OK) goto j_rr_exit;

        // _ac = POST
        if (Map_Add(newProperties, "_ac", "POST") != MAP_OK) goto j_rr_exit;

        // _uri = /rd/
        if (Map_Add(newProperties, "_uri", "/rd/") != MAP_OK) goto j_rr_exit;

        // _qry = 
        
        // _src = proxy
        if (Map_Add(newProperties, "_src", module_name) != MAP_OK) goto j_rr_exit;

        
        // _id = xxx
        // int msg_id = bh_gen_id(g_broker_ctx);
        int msg_id = random();
        char c_mid[32];
        memset(c_mid,0,32);
        snprintf (c_mid, 32, "%d", msg_id);    
        if (Map_Add(newProperties, "_id", c_mid) != MAP_OK) goto j_rr_exit;; 
        
        
        newMessageCfg.sourceProperties = newProperties;
        newMessageCfg.size = strlen(msg_payload);
        newMessageCfg.source = (const unsigned char*)msg_payload;
        MESSAGE_HANDLE newMessage = Message_Create(&newMessageCfg);
        
        if (newMessage == NULL)
        {
            LogError("Failed to create new message");
            goto j_rr_exit;;
        }
        else
        {
            if (Broker_Publish(module_data->broker, (MODULE_HANDLE)module_data, newMessage) != BROKER_OK)
            {
                puts("Failed to create new message");
                goto j_rr_exit;;
            }
            Message_Destroy(newMessage);
            Map_Destroy(newProperties);            
            return 0;
        }

    j_rr_exit: 
        if (newProperties) Map_Destroy(newProperties);
        printf("report_rd error\r\n");
        return -1;

    }    
}


void dump_constmap(CONSTMAP_HANDLE properties,MESSAGE_HANDLE messageHandle)
{
    const char* const * keys;
    const char* const * values;
    size_t count;

    if (ConstMap_GetInternals(properties, &keys, &values, &count) == CONSTMAP_OK)
    {
        const CONSTBUFFER* content = Message_GetContent(messageHandle);
        if (content != NULL)
        {
            puts("================ proxy message dump =====================");
            // dump const list
            printf(
                "Received a message\r\n"
                "Properties:\r\n"
                );

            for (size_t i = 0; i < count; ++i)
            {
                printf("  %s = %s\r\n", keys[i], values[i]);
            }
            
            // dump payload
            printf("Content:\r\n");
            printf("  %.*s\r\n", (int)content->size, content->buffer);
            fflush(stdout);
        }
    }
}    

static void encode_coap_pkg(CONSTMAP_HANDLE properties,MESSAGE_HANDLE messageHandle,char* pkg)
{
    const char* const * keys;
    const char* const * values;
    size_t count;
    int idx;
    
    pkg[0] = 0;
    idx = 0;
    if (ConstMap_GetInternals(properties, &keys, &values, &count) == CONSTMAP_OK)
    {
        const CONSTBUFFER* content = Message_GetContent(messageHandle);
        if (content != NULL)
        {
            for (size_t i = 0; i < count; ++i)
            {
                sprintf(pkg+idx,"%d\n%s\n%s",strlen(keys[i]) + strlen(values[i]),keys[i],values[i]);
                idx = strlen(pkg);              
            }
            sprintf(pkg+idx,"%d\n%s",(int)content->size, content->buffer);
        }
    }
} 

static void extract_payload(CONSTMAP_HANDLE properties,MESSAGE_HANDLE messageHandle,char* pkg)
{
    const char* const * keys;
    const char* const * values;
    size_t count;
    int idx;
    
    const CONSTBUFFER* content = Message_GetContent(messageHandle);
    if (content != NULL)
    {
        memcpy(pkg,content->buffer,content->size);
        pkg[content->size] = 0;
    }
}

static int call_rpc_hook(char* json_in, char* json_out)
{   

    int rc;
    
    DEBUG_PRINT("call_rpc_hook -- debug 1\r\n");
    
    if (redis_h == NULL)
    {
        printf("call_rpc_hook -- invalid redis handle.\r\n");
        return -1;
    }
    
    
    rc = redis_del(redis_h,redis_get_name);
    if (rc < 0)
    {
            redis_h = NULL;                                                
            printf("del reqr key failed\n");
            return -1;
    }

                
    rc = redis_publish(redis_h,redis_pub_name,json_in);
    if (rc < 0)
    {
        redis_h = NULL;
        //goto j000_exit;
        return -1;
    }

    DEBUG_PRINT("call_rpc_hook -- debug 2\r\n");
    int tmt_cnt = 0;
    while (1) // loop with timeout, until it has valid response
    {         
    
        DEBUG_PRINT("call_rpc_hook -- debug 3\r\n");        
        // rc = redis_set(redis_h, "reqr", "test");
        rc = redis_get(redis_h,redis_get_name,shared_buff);
        
        DEBUG_PRINT("call_rpc_hook -- debug 4\r\n");                
        if (rc == 0)
        {
            if (strlen(shared_buff) > 0)  
            {    
                //if (create_post_response(module_data,ConstMap_GetValue(properties, "_id"),shared_buff) == 0)
                //{    
                    // rc = credis_set(redis, "reqr","!");
                    
                rc = redis_del(redis_h,redis_get_name);
                if (rc < 0)
                {
                        redis_h = NULL;                                                
                        printf("del reqr key failed\n");
                        return -1;
                }
                break;
                
            }
            else
            {
                if (tmt_cnt > 1000)
                {
                    printf("timeout, return error directly 1\r\n");
                    // strcpy(json_out,"{\"code\":-1,\"info\":\"no response from plugin.\"}");
                    // create_post_response(module_data,ConstMap_GetValue(properties, "_id"),shared_buff);
                    return -1;
                }
                else
                {                     
                    // 1ms
                    usleep(1000);
                    tmt_cnt++;
                }    
            }    
        }
        else
        {
            // timeout = 100 msec
            if (tmt_cnt > 1000)
            {
                printf("timeout, return error directly 2\r\n");
                //strcpy(json_out,"{\"code\":-1,\"info\":\"no response from plugin.\"}");
                // create_post_response(module_data,ConstMap_GetValue(properties, "_id"),shared_buff);
                return -1;
            }
            else
            {           
                // 1ms
                usleep(1000);
                tmt_cnt++;
            } 
        } // rc != 0
    } // while (1)

    return 0;
}    

static void SimulatedDevice_Receive(MODULE_HANDLE moduleHandle, MESSAGE_HANDLE messageHandle)
{
    int rc;
    int tmt_cnt;
    char* str_p;
    
    static int recv_cnt = 0;
    const char* p_url_str = ((SIMULATEDDEVICE_DATA*)moduleHandle)->map_url_str;
    SIMULATEDDEVICE_DATA* module_data = (SIMULATEDDEVICE_DATA*)moduleHandle;


#if 0    
    if (redis_lock == 1)
    {
        printf("re-enter _Receive, exit\r\n");
        return;
    }
#endif

    
    redis_lock = 1;
    
    
    if (refresh_rd_flag == 1)
    {
        printf("refresh proxy rd.\r\n");
        shared_buff[0] = 0;
        rc = call_rpc_hook("{\"method\":\"getrd\"}", shared_buff);
        if (rc >= 0)
        {    
            DEBUG_PRINT("report_rd start\r\n");
            printf("rd:%s\r\n",shared_buff);
            // int rc = report_rd(module_data,"{\"di\":\"0685B960-736F-46F7-BEC0-9E6CBD610002\",\"st\":\"ocr\"}");
            int rc = report_rd(module_data,shared_buff);
            if (rc >= 0)
            {    
                DEBUG_PRINT("report_rd rc:%d\r\n",rc);
                DEBUG_PRINT("report_rd end\r\n");
                printf("report_rd ok!\r\n");
            }
            else
            {
                printf("report_rd fail!\r\n");
            }
        }
        else
        {
            printf("refresh proxy rd failed.\r\n");
        }            
        refresh_rd_flag = 0;
    }
    
    // Print the properties & content of the received message
    CONSTMAP_HANDLE properties = Message_GetProperties(messageHandle);
    
    
    if (properties != NULL)
    {
        
#if 0 // debug only, remove it in release
        dump_constmap(properties, messageHandle);
#endif
        
        if ((ConstMap_ContainsKey(properties, "_uri") == true && strcmp(p_url_str, ConstMap_GetValue(properties, "_uri")) == 0) &&
              (ConstMap_ContainsKey(properties, "_ac") == true && strcmp("POST", ConstMap_GetValue(properties, "_ac")) == 0))
        {
            recv_cnt++;
            
            extract_payload(properties,messageHandle,shared_buff);
            DEBUG_PRINT("_Receive coap payload: %s\n", shared_buff);
            // hexdump(shared_buff,strlen(shared_buff),"_Receive payload");
            
            DEBUG_PRINT("Start to process received message.\r\n");                        
            if (redis_h != NULL)
            {
                
            
                rc = call_rpc_hook(shared_buff, shared_buff);
                if (rc < 0)
                    printf("call_rpc_hook fail\r\n");
                else
                    create_post_response(module_data,ConstMap_GetValue(properties, "_id"),shared_buff);

            }
            else
            {
                printf("! Invalid redis handler, reconnect redis.\r\n");
                // reconnect redis server
                redis_h = redis_connect(redis_ip_buff,redis_port_buff);
                // redis_h = redis_connect("127.0.0.1","6379");
                
                if (redis_h->fd < 0) 
                {
                    printf("reconnect redis fail, exit\r\n");
                    ConstMap_Destroy(properties);
                    redis_h = NULL;
                    redis_lock = 0;
                    goto j000_exit;
                }  
                // goto j000_exit;
            }
            

        }
j000_exit:
        ConstMap_Destroy(properties);
    }

    redis_lock = 0;
    return;
}

static void SimulatedDevice_Destroy(MODULE_HANDLE moduleHandle)
{
    if (moduleHandle == NULL)
    {
        LogError("Attempt to destroy NULL module");
    }
    else
    {
        SIMULATEDDEVICE_DATA* module_data = (SIMULATEDDEVICE_DATA*)moduleHandle;
        int result;

        /* Tell thread to stop */
        module_data->simulatedDeviceRunning = 0;
        /* join the thread */
        ThreadAPI_Join(module_data->monitor_thread, &result);
        /* free module data */
        free((void*)module_data->map_url_str);
        free(module_data);
    }
}

static int simulated_device_worker(void * user_data)
{
    while(1)
    {
        refresh_rd_flag = 1;
        sleep(30);
    }

    return 0;
}

static void SimulatedDevice_Start(MODULE_HANDLE moduleHandle)
{
    int rc;
    
    
    if (moduleHandle == NULL)
    {
        LogError("Attempt to start NULL module");
    }
    else
    {
        SIMULATEDDEVICE_DATA* module_data = (SIMULATEDDEVICE_DATA*)moduleHandle;
        /* OK to start */

        while (1)
        {    
            redis_h = redis_connect(redis_ip_buff,redis_port_buff);
            if (redis_h == NULL)
            {
                printf("! Connect redis fail, Invalid redis handler 2\r\n"); 
                sleep(1);
                
            }            
            else
            {
                printf("connected to redis\r\n");
                
            }
            
            rc = call_rpc_hook("{\"method\":\"getrd\"}", shared_buff);
            if (rc >= 0)
            {    
                DEBUG_PRINT("report_rd start\r\n");
                // int rc = report_rd(module_data,"{\"di\":\"0685B960-736F-46F7-BEC0-9E6CBD610002\",\"st\":\"ocr\"}");
                printf("rd:\s\r\n",shared_buff);
                int rc = report_rd(module_data,shared_buff);
                if (rc >= 0)
                {    
                    DEBUG_PRINT("report_rd rc:%d\r\n",rc);
                    DEBUG_PRINT("report_rd end\r\n");
                    printf("report_rd ok!\r\n");
                    break;
                }
                else
                {
                    printf("report_rd fail!\r\n");
                }
            }
            else
            {    
                printf("get resource fail. retry\r\n");
                sleep(1);
            }
        }


        /* Create a resource monitor thread.  */
        if (ThreadAPI_Create(
            &(module_data->monitor_thread),
            simulated_device_worker,
            (void*)module_data) != THREADAPI_OK)
        {
            LogError("ThreadAPI_Create failed");
            module_data->monitor_thread = NULL;
        }

        
        else
        {
            /* Thread started, module created, all complete.*/
        }

    }
}

static MODULE_HANDLE SimulatedDevice_Create(BROKER_HANDLE broker, const void* configuration)
{
    SIMULATEDDEVICE_DATA * result;
    SIMULATEDDEVICE_CONFIG * config = (SIMULATEDDEVICE_CONFIG *) configuration;
    if (broker == NULL || config == NULL)
    {
        LogError("invalid SIMULATED DEVICE module args.");
        result = NULL;
    }
    else
    {
       
        /* allocate module data struct */
        result = (SIMULATEDDEVICE_DATA*)malloc(sizeof(SIMULATEDDEVICE_DATA));
        if (result == NULL)
        {
            LogError("couldn't allocate memory for BLE Module");
        }
        else
        {
            /* save the message broker */
            result->broker = broker;
            /* set module is running to true */
            result->simulatedDeviceRunning = 1;
            /* save fake map_url_str */
            char * new_map_url_str;
            int status = mallocAndStrcpy_s(&new_map_url_str, config -> map_url_str);

            if (status != 0)
            {
                LogError("map_url_str did not copy");
            }
            else
            {
                result->map_url_str = new_map_url_str;
                result -> messagePeriod = config -> messagePeriod;
                result->monitor_thread = NULL;

            }

        }
    }
    return result;
}

static void * SimulatedDevice_ParseConfigurationFromJson(const char* configuration)
{
	SIMULATEDDEVICE_CONFIG * result;
    if (configuration == NULL)
    {
        LogError("invalid module args.");
        result = NULL;
    }
    else
    {
        JSON_Value* json = json_parse_string((const char*)configuration);
        if (json == NULL)
        {
            LogError("unable to json_parse_string");
            result = NULL;
        }
        else
        {
            JSON_Object* root = json_value_get_object(json);
            if (root == NULL)
            {
                LogError("unable to json_value_get_object");
                result = NULL;
            }
            else
            {
                SIMULATEDDEVICE_CONFIG config;
                
                
                const char* name_p = json_object_get_string(root, "name");
                if (name_p == NULL)
                {
                    LogError("unable to find name configuration value");
                    result = NULL;
                    goto j002_exit;
                }
                else
                {
                    printf("module_name = %s\r\n",name_p);
                    strcpy(module_name,name_p);
                }

                
                const char* redis_ip = json_object_get_string(root, "redis_ip");
                if (redis_ip == NULL)
                {
                    LogError("unable to find redis_ip configuration value");
                    result = NULL;
                    goto j002_exit;
                }
                else
                {
                    printf("redis_ip = %s\r\n",redis_ip);
                    strcpy(redis_ip_buff,redis_ip);
                }
                
                
                const char* redis_port = json_object_get_string(root, "redis_port");
                if (redis_port == NULL)
                {
                    LogError("unable to find redis_port configuration value");
                    result = NULL;
                    goto j002_exit;
                }
                else
                {
                    printf("redis_port = %s\r\n",redis_port);                    
                    strcpy(redis_port_buff,redis_port);
                }
                
                const char* map_url_str = json_object_get_string(root, "url_map");
                if (map_url_str == NULL)
                {
                    LogError("unable to json_object_get_string");
                    result = NULL;
                }
                else
                {
                    // copy map_url_str to url_map_buff
                    if (strlen(map_url_str) < 64)
                    {    
                        strcpy(url_map_buff,map_url_str);

                        strcpy(redis_pub_name,"req");
                        strcat(redis_pub_name,url_map_buff);
                        
    
                        strcpy(redis_get_name,"reqr");
                        strcat(redis_get_name,url_map_buff);
                        
                        
                    }
                    int period = 10000; // period, set const for demo
                    if (period <= 0)
                    {
                        LogError("Invalid period time specified");
                        result = NULL;
                    }
                    else
                    {
                        if (mallocAndStrcpy_s(&(config.map_url_str), map_url_str) != 0)
                        {
                            result = NULL;
                        }
                        else
                        {
                            config.messagePeriod = period;
                            result = malloc(sizeof(SIMULATEDDEVICE_CONFIG));
                            if (result == NULL) {
                                free(config.map_url_str);
                                LogError("allocation of configuration failed");
                            }
                            else
                            {
                                *result = config;
                            }
                        }
                    }
                }
            }
        j002_exit:    
            json_value_free(json);
        }
    }
    return result;
}

void SimulatedDevice_FreeConfiguration(void * configuration)
{
	if (configuration != NULL)
	{
        SIMULATEDDEVICE_CONFIG * config = (SIMULATEDDEVICE_CONFIG *)configuration;
        free(config->map_url_str);
        free(config);
	}
}

/*
 *    Required for all modules:  the public API and the designated implementation functions.
 */
static const MODULE_API_1 SimulatedDevice_APIS_all =
{
    {MODULE_API_VERSION_1},

	SimulatedDevice_ParseConfigurationFromJson,
	SimulatedDevice_FreeConfiguration,
    SimulatedDevice_Create,
    SimulatedDevice_Destroy,
    SimulatedDevice_Receive,
    SimulatedDevice_Start
};

#ifdef BUILD_MODULE_TYPE_STATIC
MODULE_EXPORT const MODULE_API* MODULE_STATIC_GETAPI(SIMULATED_DEVICE_MODULE)(MODULE_API_VERSION gateway_api_version)
#else
MODULE_EXPORT const MODULE_API* Module_GetApi(MODULE_API_VERSION gateway_api_version)
#endif
{
    (void)gateway_api_version;
    return (const MODULE_API *)&SimulatedDevice_APIS_all;
}
