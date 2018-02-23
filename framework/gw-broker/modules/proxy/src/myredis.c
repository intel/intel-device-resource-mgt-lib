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


#include "myredis.h"

#include <unistd.h>
#include <stdarg.h>
#include <string.h>
#include <ctype.h>
#include <netinet/tcp.h> 
#include <sys/types.h>
#include <sys/socket.h>
#include <signal.h>

redis_response resp_shared;
redis_client redis_client_shared;
char buf[1024];

#define DEBUG_PRINT //printf

void hexdump(unsigned char *p,int size,char* info)
{
    int i;
    printf("\r\n======hexdump for %s========\r\n",info);
    for(i=0; i<size; i++)
    {
        printf("%2X ",p[i]);
    }    
    printf("\r\n==============\r\n");
}    

static void pack_cmd(const char *cmd,size_t cmdsize, char *packed,size_t *packsize);
static void pack_cmd2(const char *cmd,size_t cmdsize, char *packed,size_t *packsize, char* payload, int payload_size);
redis_response *redis_command2(redis_client *c, char* payload, int payload_size, const char* fmt,...);

static int send_cmd(int fd, const char *cmd,size_t len)
{
	char packedcmd[1024] = {0};
	size_t size = 0;
	pack_cmd(cmd,len,packedcmd,&size);
    
    int nsend = send(fd,packedcmd,size,0);
	if(nsend == -1)
		return nsend;
   
	while (nsend < len)
	{      
		int err = send(fd,packedcmd,size-nsend,0);
		if (err<0)
			return err;
		nsend += err;
	}
	return nsend;
}

static int send_cmd2(int fd, const char *cmd,size_t len,char* payload,int payload_size)
{
	char packedcmd[1024] = {0};
	size_t size = 0;
	pack_cmd2(cmd,len,packedcmd,&size,payload,payload_size);
       
	int nsend = send(fd,packedcmd,size,0); 
    
	if(nsend == -1)
		return nsend;   
	while (nsend < len)
	{       
		int err = send(fd,packedcmd,size-nsend,0);
		if (err<0)
			return err;
		nsend += err;
	}
	return nsend;
}

static redis_response* get_response(int fd)
{
	redis_response *resp = &resp_shared;
	memset(buf,0,1024); //FIXME: magic number

	int nread = recv(fd,buf,1023,0);
	if(nread == -1)
    {    
        printf("get_response read error\r\n");
		return NULL;
	}
    resp->len = nread;
	resp->data = buf;
    
    buf[nread] = 0;
    DEBUG_PRINT("response:%s,len=%d\r\n",buf,nread);
	return resp;
}

redis_client* redis_connect(const char *host,const char *port)
{
	redis_client *client = &redis_client_shared;
	client->fd = -1;
	struct addrinfo hints,*servinfo,*p;
	memset(&hints,0,sizeof(hints));
	hints.ai_family=AF_INET;
	hints.ai_socktype = SOCK_STREAM;
	int s;
	int rv;
    int flag = 1;
    int result;
    
    // no exit when server exit
    signal(SIGPIPE,SIG_IGN);
    
	if ((rv = getaddrinfo(host,port,&hints,&servinfo))!= 0)
		//die("getaddrinfo:%s",gai_strerror(rv));
		return NULL;
      
	for(p = servinfo; p != NULL; p = p->ai_next)
	{
		if ((s = socket(p->ai_family,p->ai_socktype,p->ai_protocol)) == -1)
			continue;
        
		if (connect(s,p->ai_addr,p->ai_addrlen) == -1)
        {    
			//die("connect:%s",strerror(errno)); //FIXME: don't die               
			continue;
        }                         
		client->fd = s;
	}                      
	return client;

}

static void pack_cmd(const char *cmd,size_t cmdsize, char *packed,size_t *packsize)
{
	const char *delim = "\r\n";
	char packedcmd[1024]={0};
	int cur = 0;
	int i = 0,argc = 0,per = 0;
	int prelen = 0;
	while(i < cmdsize) {
		while(cmd[i] && isspace(cmd[i])){++i;} // skip spaces
		prelen = i;
		per = 0;
		while((cmd[i] != '\0') && !isspace(cmd[i++]))++per;
		sprintf(packedcmd+cur,"%s$%d\r\n%s",delim,per,strndup(cmd+prelen,per));
		cur += strlen(packedcmd+cur);
		++ argc;
	}
	sprintf(packedcmd+cur,"%s",delim);
	char num[10]={0};
	snprintf(num,9,"%d",argc);
	//char packed[2048]={0};
	packed[0]='*';
	strcat(packed,num);
	strcat(packed,packedcmd);
	*packsize = strlen(packed);
    
    DEBUG_PRINT("pack_cmd debug:[%s], size=%d",packed,*packsize);
}

static void pack_cmd2(const char *cmd,size_t cmdsize, char *packed,size_t *packsize, char* payload, int payload_size)
{
	const char *delim = "\r\n";
	char packedcmd[1024]={0};
    char temp[1024]={0};
    
	int cur = 0;
	int i = 0,argc = 0,per = 0;
	int prelen = 0;
    
	while(i < cmdsize) {
		while(cmd[i] && isspace(cmd[i])){++i;} // skip spaces
		prelen = i;
		per = 0;
		while((cmd[i] != '\0') && !isspace(cmd[i++])) ++per;
        
        DEBUG_PRINT("source:%s,len=%d\r\n",cmd+prelen,i-prelen);
		//sprintf(packedcmd+cur,"%s$%d\r\n%s",delim,per,strndup(cmd+prelen,i-prelen));
        sprintf(packedcmd+cur,"%s$%d\r\n%s",delim,per,strndup(cmd+prelen,per));
        
#if 0        
        hexdump((unsigned char*)(packedcmd+cur),strlen(packedcmd+cur),"pack_cmd2,debug");
#endif
        
		cur += strlen(packedcmd+cur);
		++ argc;
	}
	sprintf(packedcmd+cur,"%s",delim);
	char num[10]={0};
	snprintf(num,9,"%d",argc+1);
	//char packed[2048]={0};
	packed[0]='*';
	strcat(packed,num);
	strcat(packed,packedcmd);
#if 0    
    hexdump((unsigned char*)packed,strlen(packed),"pack_cmd2 step 1");
#endif    
    sprintf(temp,"$%d\r\n",payload_size);
    i = strlen(temp);
    memcpy(temp + i,payload,payload_size);
    temp[i+payload_size] = '\r';
    temp[i+payload_size+1] = '\n';
    temp[i+payload_size+2] = 0;
    
    // append payload: $payload_bytes\r\npayload\r\n
	*packsize = strlen(packed);
    strcat(packed,temp);
    *packsize += i + payload_size + 2;

#if 0    
    hexdump((unsigned char*)packed,strlen(packed),"pack_cmd2 step 2");
#endif
    
    DEBUG_PRINT("pack_cmd2 debug:[%s], size=%d",packed,*packsize);
}

redis_response *redis_command(redis_client *c, const char* fmt,...)
{
	char cmd[128]={0};
	va_list ap;
	va_start(ap,fmt);
	vsnprintf(cmd,127,fmt,ap); // command will be truncated if longer than 127
	va_end(ap);
	//send cmd
	int len = strlen(cmd);
	if(send_cmd(c->fd,cmd,len)<0)
		return NULL;
	//get response
	return (redis_response *)get_response(c->fd);
}

redis_response *redis_command2(redis_client *c, char* payload, int payload_size, const char* fmt,...)
{
	char cmd[128]={0};
	va_list ap;
	va_start(ap,fmt);
	vsnprintf(cmd,127,fmt,ap); // command will be truncated if longer than 127
	va_end(ap);
	//send cmd
	int len = strlen(cmd);
    
    DEBUG_PRINT("redis_command2 debug 1\r\n");
	if(send_cmd2(c->fd,cmd,len,payload,payload_size)<0)
    {
        printf("redis_command2 error\r\n");
		return NULL;
    }
	//get response
	return (redis_response *)get_response(c->fd);

}

int redis_publish(redis_client *c, char* topic, char* content)
{
    DEBUG_PRINT("redis_publish\r\n");
	redis_response *resp = redis_command2(c,content,strlen(content),"PUBLISH %s",topic);

    if ((resp != NULL) && (strlen(resp->data) > 0))
    {
        if (resp->data[0] == '-')
            return -1;

        else if ((resp->data[0] == '$') && (resp->data[1] == '-') && (resp->data[2] == '1'))
        {
            return -1;
        }
        
        else
        {
            DEBUG_PRINT("resp->data:%s, resp=0x%x\r\n",resp->data,resp);            
            return 0;
        }    
    }    
    else
    {        
        printf("redis_publish error\r\n");
        return -1;
    }    
}

int redis_get(redis_client *c, char* key, char* value)
{
    int i,j,k;
    int status = 0;
    char str_temp[8];
    char* p;
    
    DEBUG_PRINT("redis_get\r\n");
	redis_response *resp = redis_command(c,"GET %s",key);
    DEBUG_PRINT("resp->data:%s\r\n",resp->data);
    
    if ((resp != NULL) && (strlen(resp->data) > 0))
    {    
        if (resp->data[0] == '-')
        {    
            return -1;
        }    
        else if ((resp->data[0] == '$') && (resp->data[1] == '-') && (resp->data[2] == '1'))
        {
            return -1;
        }
        else if ((resp->data[0] == '$') && (resp->data[1] != '-'))
        {            
            p = resp->data;
            
            for(i = 1;i<strlen(p);i++)
            {
                if ((p[i-1] == '\r') && (p[i] == '\n'))
                {    
                    status = 1;
                    j = i+1;  // j -> "{...}"
                    
                    p[i-1] = 0;
                    sscanf(p+1,"%d",&k);
                    
                    
                    memcpy(value,p+i+1,k);
                    value[k] = 0;
                    
                    return 0;
                            
                }    
            }    

        }
    }    
    else
    {
        printf("redis_get error\r\n");
        return -1;
    } 

}

int redis_set(redis_client *c, char* key, char* value)
{
    DEBUG_PRINT("redis_set\r\n");
	redis_response *resp = redis_command2(c,value,strlen(value),"SET %s",key);
    DEBUG_PRINT("resp->data:%s\r\n",resp->data);
    
    if ((resp != NULL) && (strlen(resp->data) > 0))
    {
        
        if (resp->data[0] == '-')
            return -1;
        else
        {
            // strcpy(value,resp->data);   
            return 0;
        }    
    }
    else if ((resp->data[0] == '$') && (resp->data[1] == '-') && (resp->data[2] == '1'))
    {
        return -1;
    }
    
    else
    {
        printf("redis_set error\r\n");
        return -1;
    } 

}

int redis_del(redis_client *c, char* key)
{
    DEBUG_PRINT("redis_del\r\n");
	redis_response *resp = redis_command(c,"DEL %s",key);
    DEBUG_PRINT("resp->data:%s\r\n",resp->data);    
    
    if ((resp != NULL) && (strlen(resp->data) > 0))
    {
        if (resp->data[0] == '-')
            return -1;
        else
        {
            // strcpy(value,resp->data);   
            return 0;
        }    
    }
    else if ((resp->data[0] == '$') && (resp->data[1] == '-') && (resp->data[2] == '1'))
    {
        return -1;
    }    
    else
    {
        printf("redis_publish error\r\n");
        return -1;
    } 

}
