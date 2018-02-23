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


//iagent
#include "iagent_config.h"
#include "ilink_message.h"
#ifdef RUN_ON_LINUX
#include <arpa/inet.h>
#endif

void reset_ilink_message(ilink_message_t * msg)
{
	if(msg->is_payload_alloc && msg->payload)
		free(msg->payload);

	if(msg->is_vheader_alloc && msg->vheader)
		free(msg->vheader);

	memset(msg, 0, sizeof(*msg));
}

static void payload_recieve_done(recv_context_t* ctx)
{
	ctx->phase = P_Not_Start;

    ctx->phase_len = 1;
	ctx->offset = 0;

	if (1 == ctx->ilink_msg.has_vheader)
	{
		if(ctx->ilink_msg.is_vheader_alloc)
			free(ctx->ilink_msg.vheader);

		char *p = ctx->ilink_msg.payload;
		ctx->ilink_msg.vheader_len = ntohs (*((uint16_t*)p));
		ctx->ilink_msg.vheader = ctx->ilink_msg.payload;

		ctx->ilink_msg.is_vheader_alloc = ctx->ilink_msg.is_payload_alloc;
		ctx->ilink_msg.is_payload_alloc = 0;

		ctx->ilink_msg.payload_len = ctx->ilink_msg.payload_len - ctx->ilink_msg.vheader_len;
		ctx->ilink_msg.payload += ctx->ilink_msg.vheader_len;
	}
}


static int length_recieve_done(recv_context_t* ctx)
{
    ctx->phase = P_In_Payload;
    ctx->phase_len = ctx->msglen;
    ctx->offset = 0;

    assert(ctx->ilink_msg.payload == NULL);

    ctx->ilink_msg.payload_len = ctx->msglen;

    if(ctx->ilink_msg.payload_len == 0)
    {
    	ctx->phase = P_Not_Start;
        ctx->phase_len = 1;
    	return 0;
    }


    if(ctx->ilink_msg.payload_len < sizeof(ctx->buffer))
    {
    	ctx->ilink_msg.payload = ctx->buffer;
    	ctx->ilink_msg.is_payload_alloc = 0;
    }
    else
    {
    	ctx->ilink_msg.payload = malloc(ctx->ilink_msg.payload_len);
		if(ctx->ilink_msg.payload == NULL)
		{
			ctx->phase = P_Not_Start;
            ctx->phase_len = 1;
			return -1;
		}
		ctx->ilink_msg.is_payload_alloc = 1;
    }

    return 1;
}


/***********************************************
 * name:     on_byte_arrive
 * function: handle the string from iLink
 * input:    1 byte string from iLink
 * output:   parse result
 * return:   -1 invalide sync btye
 *           1 byte added to buffer, waiting more for complete packet
 *           0 completed packet
 *           2: in recieving payload
 *
 */
int on_byte_arrive(unsigned char ch, recv_context_t* ctx)
{
    // get sycn. 1 byte:0xBA, 0xFA, 0xCE
	int ret = 1;
    switch(ctx->phase)
    {
    case P_Not_Start: // get sync byte

    	reset_ilink_message(&ctx->ilink_msg);
    	memset(ctx, 0, sizeof(recv_context_t));

        if (FIXEDCHAR_NO_MSGID == ch)
        {
            ctx->phase = P_In_Type;
            ctx->phase_len = 1;

            ctx->ilink_msg.leading_byte = ch;
            ctx->ilink_msg.has_mid = 0;
        }
        else if (FIXEDCHAR_REQUEST == ch || FIXEDCHAR_RESPINSE == ch)
        {
            ctx->phase = P_In_Mid;
            ctx->phase_len = 4;

            ctx->ilink_msg.leading_byte = ch;
            ctx->ilink_msg.has_mid = 1;
            if (FIXEDCHAR_REQUEST == ch)
                ctx->ilink_msg.is_req = 1;
        }
        else // sync failed
        {
            ctx->phase = P_Not_Start;
            return 1;
        }
        goto END;
        break;

    case P_In_Mid: // get message ID, if any

        ctx->ilink_msg.msgID = ((ctx->ilink_msg.msgID << 8) & 0xFFFFFF00) | (ch & 0x000000FF);
        ctx->phase_len --;

        if(0 == ctx->phase_len)
        {
            ctx->phase = P_In_Type;
            ctx->phase_len = 1;
        }
        goto END;
        break;

    case P_In_Type: // get message type

        ctx->phase_len --;
        if(0 == ctx->phase_len)
        {
            ctx->phase = P_In_Base_Len;
            ctx->phase_len = 2;

            ctx->ilink_msg.msgType = ch & 0x7F;
            if (ch & 0x80)
                ctx->ilink_msg.has_vheader = 1;
        }
        goto END;
        break;

    case P_In_Base_Len: // get payload length

        ctx->msglen = ((ctx->msglen << 8) & 0xFFFFFF00) | (ch & 0x000000FF);
        ctx->phase_len --;

        if(0 == ctx->phase_len)
        {
            if(ctx->msglen & 0x00008000)
            {
                ctx->phase = P_In_Ext_Len;
                ctx->phase_len = 2;
            }
            else
            {
                ctx->head_buff[ctx->offset] = ch;
                ctx->offset ++;

            	ret = length_recieve_done(ctx);
            	return ret;
            }
        }
        goto END;
        break;

    case P_In_Ext_Len: // get payload extend length

        ctx->msglenex = ((ctx->msglenex << 8) & 0xFFFFFF00) | (ch & 0x000000FF);

        ctx->phase_len --;
        if(0 == ctx->phase_len)
        {
            ctx->msglen = ((ctx->msglenex << 15) & 0x7FFF8000) | (ctx->msglen & 0x00007FFF);

            ctx->head_buff[ctx->offset] = ch;
            ctx->offset ++;

            ret = length_recieve_done(ctx);
            return ret;
        }

        goto END;
        break;

    case P_In_Payload: // get message

        ctx->phase_len --;
        ctx->ilink_msg.payload[ctx->offset] = ch;
        ctx->offset++;

        if (0 == ctx->phase_len)
        {
            payload_recieve_done(ctx);
            return 0;
        }

        return 2;

    default:
        return -1;
        break;
    }

END:
	assert(P_In_Payload != ctx->phase);
    ctx->head_buff[ctx->offset] = ch;
    ctx->offset ++;
    return ret;
}


int on_payload_block(recv_context_t* ctx, char * buffer, int *len)
{
	assert (ctx->phase == P_In_Payload);

	int copy_len  = *len < ctx->phase_len ? *len : ctx->phase_len;
	memcpy(ctx->ilink_msg.payload+ctx->offset, buffer, copy_len);

	ctx->phase_len -= copy_len;
    ctx->offset += copy_len;
	*len = copy_len;

	if(ctx->phase_len == 0)
	{
		payload_recieve_done(ctx);
		return 0;
	}

	return 1;
}



void init_ilink_message(ilink_message_t * msg, ilink_msg_type_e t)
{
	memset(msg, 0, sizeof(*msg));
	msg->msgType = t;
}

void ilink_set_req_resp(ilink_message_t * msg, bool yes_as_req, unsigned long msg_id)
{
	msg->has_mid = 1;
	msg->is_req = yes_as_req;
	msg->msgID = msg_id;
}

void ilink_set_payload(ilink_message_t * msg, char * payload, int len)
{
	if(msg->is_payload_alloc)
		free(msg->payload);

	msg->is_payload_alloc = 0;

	msg->payload_len = len;
	if (0 != len)
	{
	    msg->payload = payload;
	}
	else
	{
		msg->payload = NULL;
	}
}

void ilink_set_vheader(ilink_message_t * msg, ilink_vheader_t * vheader)
{
	int len = 0;
	char * vheader_buffer = vheader_compose(vheader, &len);

	if(msg->is_vheader_alloc)
	{
		if(msg->vheader) free(msg->vheader);
		msg->is_vheader_alloc = 0;
	}

	msg->vheader = NULL;
	msg->vheader_len = len;
	if (0 != len)
	{
		msg->is_vheader_alloc = 1;
	    msg->vheader = vheader_buffer;
	}
}

void compose_ilink_header(char * header, int * len, ilink_message_t * msg)
{
	int offset = 0;
	char * p = header;

	if(!msg->has_mid)
		*p = FIXEDCHAR_NO_MSGID;
	else if(msg->is_req)
		*p = FIXEDCHAR_REQUEST;
	else
		*p = FIXEDCHAR_RESPINSE;

	p++;
	offset++;

	if(msg->has_mid)
	{
		* ((uint32_t*) p) = htonl(msg->msgID);
		p += sizeof(uint32_t);
		offset += sizeof(uint32_t);
	}

	*p = msg->msgType;

	if(msg->vheader_len) *p |= 0x80;
	p++;
	offset++;

	if(msg->payload_len + msg->vheader_len >= 0x80000000)
	{
		// to do:
	    uint32_t msg_len = msg->payload_len + msg->vheader_len;
	    * ((uint32_t*) p) = htons (msg_len & 0x00007FFF);
	    p += sizeof (uint32_t);
	    offset += sizeof (uint32_t);

	    * ((uint32_t*) p) = htons ((msg_len & 0x7FFF8000) >> 15);
	    p += sizeof (uint32_t);
	    offset += sizeof (uint32_t);
	}
	else
	{
		* ((uint16_t*) p) = htons(msg->payload_len + msg->vheader_len);
		p += sizeof(uint16_t);
		offset += sizeof(uint16_t);
	}

	*len = offset;

}

char * compose_ilink_frame(ilink_message_t * msg, uint32_t* frame_len)
{
	char header[20];
	int len;

	compose_ilink_header(header, &len, msg);


	//char * frame = (char *) malloc(len + msg->payload_len + msg->vheader_len + /vheader_len*/1);

    *frame_len = len+msg->vheader_len + msg->payload_len;
	char * frame = (char *) malloc(*frame_len);

	memcpy(frame, header, len);
	if(msg->vheader_len) memcpy(frame+len, msg->vheader, msg->vheader_len);
	if(msg->payload_len) memcpy(frame+len+msg->vheader_len, msg->payload, msg->payload_len);

	return frame;
}

void free_ilink_frame(char * message)
{
	free(message);
}
