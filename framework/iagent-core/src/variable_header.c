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


/*
 * variable_header.c
 *
 *  Created on: Nov 1, 2016
 *      Author: xwang98
 *
 *  The variable header format:
 *
 *  Length of item part (2 bytes), Item Num ( 2 bytes), Many Items
 *  One Item: Key (string with '\0' ended),
 *            Type (one byte),
 *            Value length (only exist for blob type)
 *            Value content
 */

#include "iagent_base.h"
#ifdef RUN_ON_LINUX
#include <arpa/inet.h>
#endif

//iagent
#include "ilink_message.h"

void vheader_init(ilink_vheader_t * vheader, uint16_t initial_num, uint16_t increase)
{
    memset (vheader, 0, sizeof(*vheader));
    if (initial_num)
    {
        vheader->nodes = malloc (initial_num * sizeof(vheader_node_t));
        memset (vheader->nodes, 0, initial_num * sizeof(vheader_node_t));
    }

    vheader->num = initial_num;
    if (increase)
        vheader->increase_num = increase;
    else
        vheader->increase_num = 5; //if increase memory is 0, set default value 5
}

void vheader_destroy(ilink_vheader_t * vheader)
{
    int i ;
    int first_free = -1;
    for (i=0; i< vheader->num; i++)
    {
        vheader_unset_node(&vheader->nodes[i]); // release nodes if needed
    }

    if (vheader->nodes)
    {
        free (vheader->nodes);
    }
    memset (vheader, 0, sizeof(*vheader));
}

/// !!! the returned node can be invalid after further calling vheader_find_node()
///     because vheader_find_node can re-malloc the buffer for nodes.
///
vheader_node_t *vheader_find_node(ilink_vheader_t *vheader, char *key, bool alloc_if_miss, bool alloc_key)
{
    int i ;
    int first_free = -1;

    for (i=0; i< vheader->num; i++)
    {
        if (alloc_if_miss && first_free == -1 && vheader->nodes[i].used == 0)
        {
            first_free = i;
        }
        else if (vheader->nodes[i].used && vheader->nodes[i].key != NULL && (strcmp (key, vheader->nodes[i].key) == 0))
        {
            return &vheader->nodes[i];
        }
    }

    if (alloc_if_miss)
    {
        vheader_node_t *node;
        if (first_free == -1)
        {
            int inc_num = (vheader->increase_num?vheader->increase_num:3);
            vheader_node_t *nodes = (vheader_node_t*)malloc (sizeof(*nodes) * (vheader->num + inc_num));
            memset (nodes, 0, sizeof (*nodes) * (vheader->num + inc_num));
            memcpy (nodes, vheader->nodes, sizeof (*nodes) * vheader->num);
            node = &nodes[vheader->num];
            vheader->num += inc_num;

            free (vheader->nodes);
            vheader->nodes = nodes;
        }
        else
        {
            node = &vheader->nodes[first_free];
        }

        node->used = 1;
        if(alloc_key)
        {
            node->key = (char *)malloc (strlen(key)+1);
            node->key_alloc = 1;
            strcpy (node->key, key);
        }
        else
        {
            node->key = key;
            node->key_alloc = 0;
        }

        return node;
    }

    return NULL;
}

void vheader_unset_node(vheader_node_t *node)
{
    node->used = 0;
    if (node->key_alloc)
    {
        node->key_alloc = 0;
        free (node->key);
    }
    if (node->value_alloc)
    {
        node->value_alloc = 0;
        free (node->value.blob_val);
    }
}

void vheader_set_node_i(vheader_node_t *node, int value)
{
    if (node->value_alloc)
    {
        free (node->value.blob_val);
        node->value_alloc = 0;
    }
    node->type = T_Long;
    node->value.i_val32 = value;
}

void vheader_set_node_str(vheader_node_t *node, char *value, bool alloc_value)
{
    if (node->value_alloc)
    {
        free (node->value.blob_val);
        node->value_alloc = 0;
    }

    node->type = T_Str;

    if (alloc_value)
    {
        node->value.s_val = malloc (strlen (value)+1);
        strcpy (node->value.s_val, value);
        node->value_alloc = 1;
    }
    else
    {
        node->value.s_val = value;
        node->value_alloc = 0;
    }
}

int vheader_get_nodes_num(ilink_vheader_t *vheader)
{
    uint16_t cnt = 0;
    int i;

    for (i = 0; i < vheader->num; i++)
    {
        if (vheader->nodes[i].used)
            cnt++;
    }
    return cnt;
}

int vheader_node_len(vheader_node_t *node)
{
    if (0 == node->used)
        return 0;
	switch (node->type)
	{
	case T_Str:
		return /*key*/(strlen (node->key) + 1) + /*value*/(strlen (node->value.s_val) + 1) + /*type*/1;
	case T_Long:
		return /*key*/(strlen (node->key) + 1) + /*value*/4 + /*type*/1;
	default:
		return 0;
	}
}

char *vheader_compose(ilink_vheader_t *vheader,int *len)
{
    int i;
    int total = 0;
    char * buffer = NULL;
    for (i=0; i< vheader->num; i++)
    {
        total += vheader_node_len(&vheader->nodes[i]);
    }

    if (total == 0)
    {
        *len = 0;
        return NULL;
    }

    buffer = (char *) malloc (total + 4);

    char * p = buffer;
    *((uint16_t*)p) = htons (total + 4); // vheader total length, 2 bytes
    p += 2;

    *((uint16_t*)p) = htons (vheader_get_nodes_num(vheader)); // number of nodes, 2 bytes
    p += 2;

    for (i=0; i< vheader->num; i++)
    {
        if (1 == vheader->nodes[i].used)
        {
            vheader_node_t *node = &vheader->nodes[i];

            strcpy (p, node->key); // key of Key-value pairs, variable length
            p += strlen (node->key)+1;

            *p ++ = node->type; // type of Key-value pairs, 1 byte

            switch(node->type) // value of Key-value pairs, variable length
            {
            case T_Str:
                strcpy (p, node->value.s_val);
                p += strlen (node->value.s_val)+1;
                break;
            case T_Long:
                *((int32_t*)p) = htonl (node->value.i_val32);
                p += sizeof(uint32_t);
                break;
            default:
                ;
            }
        }
    }

    assert((buffer+ total + 4) == p);

    *len = total + 4;
    return buffer;
}

int vheader_decompose(ilink_vheader_t *vheader,
		char *buffer,
		int vheader_len,
		bool reuse_buffer)
{

#define ERR_GOTO_END(x) {error = x; goto end;};

    bool alloc = !reuse_buffer;
    char *p  = (char *) buffer;
    uint16_t total  = ntohs (*((uint16_t*)p)); // vheader total length, 2 bytes
    uint16_t offset = 0;
    int error = -1;
    p += 2;
    offset += 2;

    uint16_t num  = ntohs(*((uint16_t*)p)); // number of nodes, 2 bytes
    uint16_t idx = 0;
    p += 2;
    offset += 2;

    if (total > vheader_len)
        return -99;

    vheader_init(vheader, num, 5);

    while (offset < total && idx < num)
    {
        // key: can't be blank
        int len = strlen (p) + 1; // key of Key-value pairs, variable length
        if (len == 0) ERR_GOTO_END(-10);

        // check buffer boundrary for key
        offset += len;
        if (offset >= total) ERR_GOTO_END(-20);

        vheader_node_t *node = vheader_find_node(vheader, p, true, alloc); // find node of vheader one by one
        if (node == NULL) ERR_GOTO_END(-30);

        p += len;

        uint8_t type = *p; // type of Key-value pairs, 1 byte
        p++;
        offset ++;
        if (offset >= total) ERR_GOTO_END(-40);

        switch (type) // value of Key-value pairs, variable length
        {
        case T_Str:
            len = strlen (p) + 1;
            offset += len;
            if (offset > total) ERR_GOTO_END(-50);

            vheader_set_node_str(node, p, alloc);
            p += len;

            break ;
        case T_Long:
            offset += 4;
            if (offset > total) ERR_GOTO_END(-60);

            vheader_set_node_i(node, ntohl (*((uint32_t*)p)));
            p += 4;

            break ;
        default:
            ERR_GOTO_END(-100);
        }
        idx ++;
    }

    if (idx != num) ERR_GOTO_END(-70);

    if (offset != total)ERR_GOTO_END(-80);

    return num;

end:
    vheader_destroy(vheader);
    return error;
}



int vheader_raw_find_key(vheader_node_t *node, char *key,
		char *buffer,
		int len,
		bool reuse_buffer)
{

#define ERR_GOTO_EXIT(x) {error = x; goto EXIT;};

    bool alloc = !reuse_buffer;
    char *p  = (char *) buffer;
    uint16_t total  = ntohs (*((uint16_t*)p)); // vheader total length, 2 bytes
    uint16_t offset = 0;
    int error = -1;

    if (total > len)
        return -99;

    p += 2;
    uint16_t num  = ntohs (*((uint16_t*)p)); // number of nodes, 2 bytes
    uint16_t idx = 0;

    p += 2;

    while(offset < total && idx < num)
    {
        bool match = 0;

        // key: can't be blank
        int len = strlen (p) + 1;
        if (len == 0) ERR_GOTO_EXIT(-10);

        // check buffer boundrary for key
        offset += len;
        if (offset >= total) ERR_GOTO_EXIT(-20);

        if (strcmp (key, p) == 0) match = 1; // key of Key-value pairs, variable length

        p += len;

        uint8_t type = *p;
        p++;
        offset ++;
        if(offset >= total) ERR_GOTO_EXIT(-40);

        switch (type)
        {
        case T_Str:
            len = strlen (p) + 1;
            offset += len;
            if (offset > total) ERR_GOTO_EXIT(-50);

            if (match)
            {
                vheader_set_node_str(node, p, alloc);
                return 0;
            }
            p += len;

            break ;
        case T_Long:
            offset += 4;
            if(offset > total) ERR_GOTO_EXIT(-60);

            if(match)
            {
                vheader_set_node_i(node, ntohl (*((uint32_t*)p)));
                return 0;
            }
            p += 4;

            break ;
        default:
            ERR_GOTO_EXIT(-100);
        }
        idx ++;
    }

EXIT:
    return error;
}

void vheader_set_value_s(ilink_vheader_t *vheader, char *key, char *value)
{
    vheader_node_t * node;
    if ((node = vheader_find_node(vheader, (char *)key, (bool)1, (bool)0)) == NULL)
    {
        WARNING("Set vheader failed (key=%s)when send request to ibroker\n", key);
        return;
    }
    vheader_set_node_str(node, value, 1);
}

void vheader_set_value_i(ilink_vheader_t *vheader, char *key, int value)
{
    vheader_node_t *node;
    if ((node = vheader_find_node(vheader, (char *)key, (bool)1, (bool)0)) == NULL)
    {
        return;
    }

    vheader_set_node_i(node, value);
}
