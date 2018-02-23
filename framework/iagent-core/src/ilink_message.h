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


#ifndef APPS_IAGENT_CORE_MESSAGE_H_
#define APPS_IAGENT_CORE_MESSAGE_H_

//iagent
#include "iagent_config.h"
#include "agent_core_lib.h"

#ifdef __cplusplus
extern "C" {
#endif



#define FIXEDCHAR_NO_MSGID 0XBA
#define FIXEDCHAR_REQUEST 0xFA
#define FIXEDCHAR_RESPINSE 0xCE

/*
 * Section: variable header keys and values definition
 */
#define K_TAG "_tag"
#define TAG_HANDSHAKE1 "handshake1"
#define TAG_HANDSHAKE2 "handshake2"
#define TAG_KEY_PROVISION "key-prov"
#define TAG_PING "ping"

#define K_AGENT_ID "_aid"
#define K_EP "_ep"
#define K_RESPONSE "_re"
#define K_TIME "_tm"

#define K_CLOUD_TM_SEC "_epoch_seconds"
#define K_CLOUD_TM_MIC "_micro"

extern char * g_device_category[];

enum
{
    T_Char,
    T_Short,
    T_Long,
    T_Str,
    T_Blob
};

typedef enum
{
    COAP_OVER_TCP,
    HTTP_OVER_TCP,
    TCP_UDP_TUNNEL,
    TCF_TUNNEL,
    INTEL_IAGENT,
    EVENT_DATA,
    COMMENT_SETTING
}ilink_msg_type_e;

enum
{
    P_Not_Start,
    P_In_Mid,
    P_In_Type,
    P_In_Base_Len,
    P_In_Ext_Len,
    P_In_Payload
};

//struct
typedef struct
{
    char *key;
    bool key_alloc;
    bool value_alloc;
    bool used;

    int type;
    int value_len;
    int value_offet;
    union {
        int32_t i_val32;
        char *s_val;
        char *blob_val;
    } value;
}vheader_node_t;

typedef struct
{
    int    num;
    char *data;
    vheader_node_t *nodes;
    int increase_num;
} ilink_vheader_t;

typedef struct //__attribute__ ((packed))
{
    unsigned char leading_byte;
    uint8_t has_mid;
    uint8_t is_req;
    unsigned long msgID;
    ilink_msg_type_e msgType;
    uint8_t has_vheader;

    uint8_t is_payload_alloc;
    uint8_t is_vheader_alloc;
    uint32_t vheader_len;
    uint32_t payload_len;
    unsigned char*vheader;
    unsigned char*payload;

}ilink_message_t;


typedef struct
{
    int phase;
    int offset;
    int phase_len;

    char head_buff[10];

    uint32_t msglen;
    uint32_t msglenex;
    ilink_message_t ilink_msg;
    char buffer[1024];
}recv_context_t;

//vheader
void vheader_destroy(ilink_vheader_t *vheader);
void vheader_init(ilink_vheader_t *vheader, uint16_t initial_num, uint16_t increase);
vheader_node_t *vheader_find_node(ilink_vheader_t *vheader, char *key, bool alloc_if_miss, bool alloc_key);
char *vheader_compose(ilink_vheader_t *vheader,int *len);
int vheader_raw_find_key(vheader_node_t *node, char *key, char *buffer, int len, bool reuse_buffer);
int vheader_decompose(ilink_vheader_t *vheader, char *buffer, int vheader_len, bool reuse_buffer);
void vheader_unset_node(vheader_node_t *node);
void vheader_set_node_i(vheader_node_t *node, int value);
void vheader_set_node_str(vheader_node_t *node, char *value, bool alloc_value);
void vheader_set_value_s(ilink_vheader_t *vheader, char *key, char *value);
void vheader_set_value_i(ilink_vheader_t *vheader, char *key, int value);

//frame
void init_ilink_message(ilink_message_t *msg, ilink_msg_type_e t);
void ilink_set_req_resp(ilink_message_t *msg, bool yes_as_req, unsigned long msg_id);
void ilink_set_payload(ilink_message_t *msg, char *payload, int len);
void ilink_set_vheader(ilink_message_t *msg, ilink_vheader_t *vheader);
void compose_ilink_header(char *header, int *len, ilink_message_t *msg);
char *compose_ilink_frame(ilink_message_t *msg, uint32_t*frame_len);
void free_ilink_frame(char *message);
int on_byte_arrive(unsigned char ch, recv_context_t*ctx);
int on_payload_block(recv_context_t*ctx, char *buffer, int *len);
void reset_ilink_message(ilink_message_t * msg);

//provision
void init_provision();
int check_provision_timeout();
char *load_iagent_id();
char *get_self_agent_id();
void on_prov_message(ilink_message_t *message);
bool is_my_agent_id(char * id);

//handshake
void init_handshake();
void first_handshake();
int check_handshake_timeout();
void on_handshake_message(ilink_message_t *message);

//ilink_handler
char *get_key_value(ilink_message_t *msg, char *key);
bool get_key_value_i(ilink_message_t *msg, char *key, int *value);
int handle_ilink_message(recv_context_t *message);


// ping.c
void on_ping_message(ilink_message_t *message);


#ifdef __cplusplus
}
#endif

#endif
