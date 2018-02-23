/*
 * app001_rd_query.c
 *
 *  Created on: Jan 12, 2017
 *      Author: yongmingx.bao
 */

#include <pthread.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <ctype.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>
#include <sys/stat.h>
#include <errno.h>
#include <signal.h>
#include <inttypes.h>
#include <stdarg.h>
#include <stdbool.h>

#include "er-coap.h"
#include "connection.h"
#include "er-coap-constants.h"
#include "coap_ext.h"
#include "coap_request.h"

static int g_socket_coap_client = -1;
static coap_context_t * g_coap_ctx = NULL;

static int tx (void * ctx, const uip_ipaddr_t *dst_addr, void *buf, int len)
{
	coap_context_t * coap_ctx = (coap_context_t *) ctx;
	int ret = sendto(coap_ctx->socket, buf, len, 0, (struct sockaddr *) &dst_addr->sock_addr, dst_addr->addr_len);
	printf("send %d bytes,ret = %d\n", len, ret);
	return ret;
}

static int rx (void * ctx, void *buf, int len, int timeout_ms)
{
	coap_context_t * coap_ctx = (coap_context_t *) ctx;
	int numBytes = 0;
	struct timeval tv;
	tv.tv_usec = 0;
	tv.tv_sec = timeout_ms % 1000;

	struct sockaddr_storage addr;
	socklen_t addrLen = sizeof(addr);

	/*
	 * We retrieve the data received
	 */
	setsockopt(g_socket_coap_client, SOL_SOCKET, SO_RCVTIMEO, (char*) &tv, sizeof(struct timeval));
	numBytes = recvfrom(g_socket_coap_client, buf, 1024, 0, (struct sockaddr *) &addr, &addrLen);
	if (0 > numBytes)
	{
		fprintf(stderr, "Error in recvfrom(): %d\r\n", errno);
		return RX_TIMEOUT;
	}
	else if (0 < numBytes)
	{
		char s[INET6_ADDRSTRLEN];
		in_port_t port;

		if (AF_INET == addr.ss_family)
		{
			struct sockaddr_in *saddr = (struct sockaddr_in *) &addr;
			inet_ntop(saddr->sin_family, &saddr->sin_addr, s, INET6_ADDRSTRLEN);
			port = saddr->sin_port;
		}
		else if (AF_INET6 == addr.ss_family)
		{
			struct sockaddr_in6 *saddr = (struct sockaddr_in6 *) &addr;
			inet_ntop(saddr->sin6_family, &saddr->sin6_addr, s, INET6_ADDRSTRLEN);
			port = saddr->sin6_port;
		}

		fprintf(stderr, "%d bytes received from [%s]:%hu\r\n", numBytes, s, ntohs(port));

		return numBytes;
	}

	return RX_TIMEOUT;
}

static void init_coap_ctx (void)
{
	uip_ipaddr_t my_addr;
	int address_family = AF_INET;
	char *port = "56687";

	g_coap_ctx = coap_context_new(&my_addr);
	if (NULL == g_coap_ctx)
		return;

	// create socket
	while (g_socket_coap_client == -1)
	{
		g_socket_coap_client = create_socket((const char*) port, address_family);
		sleep(2);
	}

	g_coap_ctx->socket = g_socket_coap_client;
	g_coap_ctx->tx_data = tx;
	g_coap_ctx->rx_data = rx;
	g_coap_ctx->buf = (char*) malloc(COAP_MAX_PACKET_SIZE);
	g_coap_ctx->buf_size = COAP_MAX_PACKET_SIZE;
}

static uint8_t response_handler(void* user_data, void *response)
{
	if (response)
	{
		coap_packet_t *resp = (coap_packet_t*)response;
		fprintf(stdout, "response_handler, payload_len : %d, payload : %s\n", resp->payload_len, resp->payload);
	}
	else
	{
		fprintf(stdout, "response_handler, -----------------no success\n");
	}

	return 0;
}


#define MAX_DATA_SIZE    (64*1024 - 2)
//extern void coap_nonblocking_request(coap_context_t *coap_ctx, uip_ipaddr_t *dst_addr, coap_packet_t *request, blocking_response_handler request_callback, void * user_data);
static void test_coap_put(uip_ipaddr_t *addr, bool require_response, int fmt, char * url, int payload_len)
{
	coap_packet_t resquest[1];
	unsigned long msg_id;

	char payload[MAX_DATA_SIZE];
	uint16_t check_sum = 0;

	if (payload_len > (MAX_DATA_SIZE))
		payload_len = MAX_DATA_SIZE;

	for (int i = 0; i < payload_len; i++)
	{
		payload[i] = i;
		check_sum += payload[i];
	}

	payload[payload_len] = check_sum >> 8;
	payload[payload_len+1] = check_sum & 0xff;

	fprintf(stdout, ">>>===============================  checksum : 0x%x\n", check_sum);

	msg_id = coap_get_mid();
	if (require_response)
		coap_init_message((void *) resquest, COAP_TYPE_CON, COAP_POST, (uint16_t) msg_id);
	else
		coap_init_message((void *) resquest, COAP_TYPE_NON, COAP_POST, (uint16_t) msg_id);

	// token must be set for the java SDK
	coap_set_token(resquest, (const uint8_t*) &msg_id, sizeof(msg_id));
	coap_set_header_content_format(resquest, fmt);

	coap_set_payload_tcp(resquest, payload, payload_len+2);
	coap_set_header_uri_path(resquest, url);

	coap_nonblocking_request(g_coap_ctx, addr, resquest, response_handler, NULL);
}

extern int coap_handle_packet(coap_context_t *coap_ctx);
void *transaction_task(void *argument)
{
	coap_context_t * ctx = (coap_context_t *)argument;

	while (1)
	{
        uint32_t next_timeout = coap_check_transactions(ctx);

        ctx->buf_len = ctx->rx_data(ctx, ctx->buf, ctx->buf_size, next_timeout);
        if(RX_TIMEOUT == ctx->buf_len)
        		continue;
        coap_handle_packet(ctx);
	}

	return NULL;
}

int main(int args, void **argv)
{
	FILE *fp = NULL;
	char *data;
	int file_len;

	uip_ipaddr_t addr;
	pthread_t thread;

	int cnt = 100;
	char url[10];
	int payload_len;

	if (args > 1)
		cnt = atoi(argv[1]);

	fprintf(stdout, ">>> coap client unit test...\n");

	memset(&addr, 0, sizeof(addr));
	addr.addr_type = A_Sock_Addr;
	addr.sock_addr.sin_family = AF_INET;
	addr.sock_addr.sin_addr.s_addr = inet_addr("127.0.0.1");
	addr.sock_addr.sin_port = htons(5699);
	addr.addr_len = sizeof(addr);

	fprintf(stdout, ">>> init coap ctx\n");
	init_coap_ctx();

	pthread_create(&thread, NULL, transaction_task, g_coap_ctx);

	fprintf(stdout, ">>> test 1: coap_nonblocking_request put:\n");

	test_coap_put(&addr, false, 0, "test1", 15551);


	while(cnt > 0)
	{
		sleep(5);
		sprintf(url, "test%d", cnt%10 + 1);
		payload_len = rand()%20000 + 2000;

		fprintf(stdout, ">>>============================== cnt : %d, url : %s, payload_len : %d\n", cnt, url, payload_len + 2);
		test_coap_put(&addr, true, 0, url, payload_len);

		cnt--;
	}

	sleep(10);
	return 0;
}
