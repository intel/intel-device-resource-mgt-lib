/*
 * url_parser.h
 *
 *  Created on: Dec 10, 2016
 *      Author: xwang98
 */

#ifndef EXTERNAL_URL_PARSER_C_URL_PARSER_H_
#define EXTERNAL_URL_PARSER_C_URL_PARSER_H_
#ifdef __cplusplus
extern "C" {
#endif

typedef struct url_parser_url {
	char *protocol;
	char *host;
	int port;
	char *path;
	char *query_string;
	int host_exists;
	char *host_ip;
} url_parser_url_t;

int parse_url(char *url, bool verify_host, url_parser_url_t *parsed_url);
void free_parsed_url(url_parser_url_t *url_parsed);



#ifdef __cplusplus
}
#endif
#endif /* EXTERNAL_URL_PARSER_C_URL_PARSER_H_ */
