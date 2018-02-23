/*
 Copyright (c) 2013, 2014 Intel Corporation

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

     * Redistributions of source code must retain the above copyright notice,
       this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation
       and/or other materials provided with the distribution.
     * Neither the name of Intel Corporation nor the names of its contributors
       may be used to endorse or promote products derived from this software
       without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 THE POSSIBILITY OF SUCH DAMAGE.

 David Navarro <david.navarro@intel.com>

*/

#include "lwm2m_server.h"

extern void prv_monitor_callback(uint16_t clientID,
                                 lwm2m_uri_t * uriP,
                                 int status,
                                 lwm2m_media_type_t format,
                                 uint8_t * data,
                                 int dataLength,
                                 void * userData);

extern void check_bus_message(lwm2m_context_t * contextP);

#define MAX_PACKET_SIZE 1024

static struct sockaddr_storage g_command_src_addr;
static socklen_t g_command_src_addrLen = 0;

int debug_sock = -1;
int sock = -1;
int g_quit = 0;

lwm2m_context_t * g_lwm2mH = NULL;
sync_ctx_t* g_lwm2m_ctx = NULL;

extern command_desc_t commands[];

int coap_set_token(void *packet, const uint8_t *token, size_t token_len)
{
    return coap_set_header_token(packet, token,  token_len);
}

int coap_set_header_content_format(void *packet, unsigned int format)
{
    return coap_set_header_content_type(packet, format);
}

int coap_set_payload_tcp(void *packet, const void *payload, size_t length)
{
  coap_packet_t *const coap_pkt = (coap_packet_t *)packet;

  coap_pkt->payload = (uint8_t *)payload;
  coap_pkt->payload_len = MIN(1024*1024, length);

  return coap_pkt->payload_len;
}

bool wakeup_lwm2m_thread()
{
    if (sock != -1)
    {
        struct sockaddr_in addr;
        addr.sin_family = AF_INET;
        addr.sin_addr.s_addr = inet_addr("127.0.0.1");
        //addr.sin_port = htons(LWM2M_STANDARD_PORT);
        addr.sin_port = htons(5688);
        sendto(sock, "wake", 4, 0, (struct sockaddr *) &addr, sizeof(addr));

        return true;
    }

    return false;
}

void print_remote(const char* fmt, ...)
{
	char buf[1024];
	if(g_command_src_addrLen == 0 || debug_sock == -1)
		return;
	va_list ap;
	va_start(ap, fmt);
	vsnprintf(buf, sizeof(buf), fmt, ap);
	va_end(ap);

	sendto(debug_sock, buf, strlen(buf)+1, 0, (struct sockaddr *) &g_command_src_addr, g_command_src_addrLen);
}

int thread_lwm2m(void * parameter)
{
#ifdef RUN_ON_LINUX
    prctl (PR_SET_NAME, "lwm2m_main");
#endif
    fd_set readfds;
    struct timeval tv;
    int result;
    lwm2m_context_t * lwm2mH = NULL;
    int i;
    connection_t * connList = NULL;
    int addressFamily = AF_INET;
    //const char * localPort = LWM2M_STANDARD_PORT_STR;
    const char * localPort = "5688";

    debug_sock = create_socket("23456", addressFamily);
    if (debug_sock < 0)
    {
    	ERROR( "Error opening debug socket: %d\r\n", errno);
    }

    sock = create_socket(localPort, addressFamily);
    if (sock < 0)
    {
        ERROR("Error opening socket: %d\r\n", errno);
        return -1;
    }

    lwm2mH = lwm2m_init(NULL);
    if (NULL == lwm2mH)
    {
    	ERROR( "lwm2m_init() failed\r\n");
        return -1;
    }

    for (i = 0 ; commands[i].name != NULL ; i++)
    {
        commands[i].userData = (void *)lwm2mH;
    }
    fprintf(stdout, "> "); fflush(stdout);

    lwm2m_set_monitoring_callback(lwm2mH, prv_monitor_callback, lwm2mH);

    g_lwm2m_ctx = create_sync_ctx();
    while (0 == g_quit)
    {
        FD_ZERO(&readfds);
        FD_SET(sock, &readfds);
        if(debug_sock > 0) FD_SET(debug_sock, &readfds);

        tv.tv_sec = 60;
        tv.tv_usec = 0;

        result = lwm2m_step(lwm2mH, &(tv.tv_sec));
        if (result != 0)
        {
        	WARNING( "lwm2m_step() failed: 0x%X\r\n", result);
            return -1;
        }

        result = select(FD_SETSIZE, &readfds, 0, 0, &tv);

        check_bus_message(lwm2mH);

        g_lwm2mH = lwm2mH;

        if ( result < 0 )
        {
            if (errno != EINTR)
            {
              WARNING("Error in select(): %d\r\n", errno);
            }
        }
        else if (result > 0)
        {
            uint8_t buffer[MAX_PACKET_SIZE];
            int numBytes;

            if (FD_ISSET(sock, &readfds))
            {
                struct sockaddr_storage addr;
                socklen_t addrLen;

                addrLen = sizeof(addr);
                numBytes = recvfrom(sock, buffer, MAX_PACKET_SIZE, 0, (struct sockaddr *)&addr, &addrLen);

                if (numBytes == -1)
                {
                	WARNING( "Error in recvfrom(): %d\r\n", errno);
                }
                else
                {
                    char s[INET6_ADDRSTRLEN];
                    in_port_t port;
                    connection_t * connP;

                    if(numBytes == 4 && strncmp(buffer, "wake", 4) == 0)
                    {
                    	continue;
                    }

					s[0] = 0;
                    if (AF_INET == addr.ss_family)
                    {
                        struct sockaddr_in *saddr = (struct sockaddr_in *)&addr;
                        inet_ntop(saddr->sin_family, &saddr->sin_addr, s, INET6_ADDRSTRLEN);
                        port = saddr->sin_port;
                    }
                    else if (AF_INET6 == addr.ss_family)
                    {
                        struct sockaddr_in6 *saddr = (struct sockaddr_in6 *)&addr;
                        inet_ntop(saddr->sin6_family, &saddr->sin6_addr, s, INET6_ADDRSTRLEN);
                        port = saddr->sin6_port;
                    }

                    //fprintf(stderr, "%d bytes received from [%s]:%hu\r\n", numBytes, s, ntohs(port));
                    // output_buffer(stderr, buffer, numBytes, 0);
                    //LWM2M_LOG_DATA("recieved udp data", buffer, numBytes);

                    connP = connection_find(connList, &addr, addrLen);
                    if (connP == NULL)
                    {
                        connP = connection_new_incoming(connList, sock, (struct sockaddr *)&addr, addrLen);
                        if (connP != NULL)
                        {
                            connList = connP;
                        }
                    }
                    if (connP != NULL)
                    {
                        lwm2m_handle_packet(lwm2mH, buffer, numBytes, connP);
                    }
                }
            }
            else if (debug_sock > 0 && FD_ISSET(debug_sock, &readfds))
            {
            	g_command_src_addrLen = sizeof(g_command_src_addr);
                numBytes = recvfrom(debug_sock, buffer, MAX_PACKET_SIZE, 0, (struct sockaddr *)&g_command_src_addr, &g_command_src_addrLen);

                if (numBytes > 1)
                {
                    buffer[numBytes] = 0;
                    handle_command(commands, (char*)buffer);
                }

                if (g_quit == 0)
                {
                    fprintf(stdout, "> ");
                    fflush(stdout);
                }
            }
        }
    }

    lwm2m_close(lwm2mH);
    close(sock);
    close(debug_sock);
    delete_sync_ctx(g_lwm2m_ctx);
    connection_free(connList);

#ifdef MEMORY_TRACE
    if (g_quit == 1)
    {
        trace_print(0, 1);
    }
#endif

    return 0;
}


#if 0

void handle_sigint(int signum)
{
    g_quit = 2;
}


int main(int argc, char *argv[])
{
    int opt;

    opt = 1;
    while (opt < argc)
    {
        if (argv[opt] == NULL
            || argv[opt][0] != '-'
            || argv[opt][2] != 0)
        {
            print_usage();
            return 0;
        }
        switch (argv[opt][1])
        {
        case '4':
            addressFamily = AF_INET;
            break;
        case 'l':
            opt++;
            if (opt >= argc)
            {
                print_usage();
                return 0;
            }
            localPort = argv[opt];
            break;
        default:
            print_usage();
            return 0;
        }
        opt += 1;
    }

    signal(SIGINT, handle_sigint);

    return 0;
}

#endif
