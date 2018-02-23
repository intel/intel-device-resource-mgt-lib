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
#include <pthread.h>
#include <assert.h>
#include <dirent.h>
#include <fcntl.h>
#include <time.h>
#include "iniparser.h"
#include "logs.h"
#include "misc.h"

#define LOG_TAG "mics"

#define LOCKMODE (S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH)

// return true if this function malloc a new buffer
char * get_string(char * data, int data_len, bool *allocated)
{
	bool ret = false;
	*allocated = false;
	for(int i=data_len-1;i>=0;i--)
	{
		if(data[i] == 0)
			return data;
	}


	char *str_out = (char*) malloc(data_len+1);
	memcpy(str_out, data, data_len);
	str_out[data_len] = 0;
	*allocated = 1;
	return str_out;
}


int lockfile(int fd)
{
    struct flock fl;

    fl.l_type = F_WRLCK;  /* write lock */
    fl.l_start = 0;
    fl.l_whence = SEEK_SET;
    fl.l_len = 0;  //lock the whole file

    return(fcntl(fd, F_SETLK, &fl));
}

int already_running(const char *filename)
{
    int fd;
    char buf[16];

    fd = open(filename, O_RDWR | O_CREAT, LOCKMODE);
    if (fd < 0)
    {
        WARNING("can't open %s: %m\n", filename);
        exit(1);
    }
    if (lockfile(fd) == -1)
    {
        if (errno == EACCES || errno == EAGAIN)
        {
            WARNING("file: %s already locked\n", filename);
            close(fd);
            return 1;
        }
        WARNING("can't lock %s: %m\n", filename);
        exit(1);
    }
    ftruncate(fd, 0);
    sprintf(buf, "%ld", (long)getpid());
    write(fd, buf, strlen(buf) + 1);
    return 0;
}

int copy_to_found_char(char * source, int source_len, char c, char * dest, int dest_len)
{

	int cnt = 0;
	char * p = source;
	while(cnt < source_len && *p != c)
	{
		cnt ++;
		p++;
	}

	if(cnt == source_len)
		return 0;

	if(cnt >= dest_len)
		cnt = dest_len -1;
	memcpy(dest, source, cnt);
	dest[cnt] = 0;

	return cnt;
}

void prv_output_buffer(char * buffer,
                              int length)
{
    int i;
    uint8_t array[16];

    i = 0;
    while (i < length)
    {
        int j;
        fprintf(stderr, "  ");

        memcpy(array, buffer+i, 16);

        for (j = 0 ; j < 16 && i+j < length; j++)
        {
            fprintf(stderr, "%02X ", array[j]);
        }
        while (j < 16)
        {
            fprintf(stderr, "   ");
            j++;
        }
        fprintf(stderr, "  ");
        for (j = 0 ; j < 16 && i+j < length; j++)
        {
            if (isprint(array[j]))
                fprintf(stderr, "%c ", array[j]);
            else
                fprintf(stderr, ". ");
        }
        fprintf(stderr, "\n");

        i += 16;
    }
}



// use the mkdir -p to create all parent folders
void make_full_dir(char * path)
{
	char cmd[512];
	DIR *dirptr = NULL;

	if ((dirptr = opendir(path)) == NULL)
	{
		snprintf(cmd,sizeof(cmd),"mkdir -p %s",path);
		system(cmd);
	}
	else
	{
		closedir(dirptr);
	}

}



#include <sys/stat.h>

unsigned long get_file_size(const char *path , time_t * modi_time)
{
	unsigned long filesize = -1;
	struct stat statbuff;
	if(stat(path, &statbuff) < 0){
		WARNING( "Fail to get file stats. err:%s, file: %s",
				strerror (errno),path);
		return filesize;
	}else{
		filesize = statbuff.st_size;

		if(modi_time) *modi_time = statbuff.st_mtime;
	}
	return filesize;
}



unsigned long dir_files_size(char * path)
{
	unsigned long total = 0;
    DIR           *d;
    struct dirent *dir;
    d = opendir(path);
    if (d)
    {
      while ((dir = readdir(d)) != NULL )
      {
    	  if(dir->d_type != DT_REG)
    		  continue;

    	  time_t modi_time;
    	  char filename[512];
    	  strcpy(filename, path);
    	  strcat(filename, dir->d_name);

    	  unsigned long filesize = get_file_size(filename, &modi_time);
    	  if(filesize == -1)
    		  continue;

    	  total += filesize;
      }
      closedir(d);
    }

    return total;
}



int ini_get_int(const char* cfgname, char * key, int def)
{
	dictionary  *   ini;
	int val;

	ini = iniparser_load(cfgname);

	val = iniparser_getint(ini, key, def);

	if(NULL == ini)
	{
		WARNING("Unable to load log config %s\n", cfgname);
	}
	else
	{
		iniparser_freedict(ini);
	}

    return val;
}


// value=NULL: unset the key from the file
bool set_ini_key(char *cfgname , const char * key, const char * value)
{
	dictionary  *   ini;
	bool ret = false;
    char tmp_name[512];
	if (0 != access(cfgname, F_OK))
	{
	    FILE * file = fopen(cfgname, "w+");
	    fclose(file);
	}
    ini = iniparser_load(cfgname);


	if(NULL == ini)
	{
		// if the file exist, we should not continue to overwrite it.
		int result = access(cfgname,R_OK|W_OK );
		if(result == 0)
		{
			WARNING("set_ini_key: Unable load %s", cfgname);
			return false;
		}
	}

	if(value)
	{
		// add the section first
		char * str = strdup(key);
		if(!str)return false;

		char * p = strchr(str, ':');
		if(p)
		{
			*p = 0;
			iniparser_set(ini,str,  NULL);
			*p = ':';
		}
		iniparser_set(ini,key,  value);
		free(str);

	}
	else
	{
		iniparser_unset(ini, key);
	}

	strcpy(tmp_name, cfgname);
	strcat(tmp_name, ".tmp");
	remove (tmp_name);
	FILE * fp = fopen(tmp_name, "w+");
	if(NULL != fp){
		iniparser_dump_ini(ini, fp);
		fclose(fp);

		int result = rename(tmp_name, cfgname);
		if(result != 0)
			WARNING("set_ini_key: failed to rename %s, err:%s", cfgname, strerror(errno));
		ret = true;
	}

	iniparser_freedict(ini);

	return ret;
}



#include <stdio.h>

int load_file_to_memory(const char *filename, char **result)
{
	int size = 0;
	FILE *f = fopen(filename, "rb");
	if (f == NULL)
	{
		*result = NULL;
		return -1; // -1 means file opening fail
	}
	fseek(f, 0, SEEK_END);
	size = ftell(f);
	fseek(f, 0, SEEK_SET);
	*result = (char *)malloc(size+1);
	if (size != fread(*result, sizeof(char), size, f))
	{
		free(*result);
		return -2; // -2 means file reading fail
	}
	fclose(f);
	(*result)[size] = 0;
	return size;
}


// create a passive udp socket, no udp port specified
int create_socket_r()
{
    int s = -1;
    struct addrinfo hints;
    struct addrinfo *res;
    struct addrinfo *p;

    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_DGRAM;
    hints.ai_flags = AI_PASSIVE;

    if (0 != getaddrinfo("127.0.0.1", "", &hints, &res))
    {
        return -1;
    }

    for(p = res ; p != NULL && s == -1 ; p = p->ai_next)
    {
        s = socket(p->ai_family, p->ai_socktype | SOCK_CLOEXEC, p->ai_protocol);
        if (s >= 0)
        {
            if (-1 == bind(s, p->ai_addr, p->ai_addrlen))
            {
                close(s);
                s = -1;
            }
        }
    }

    freeaddrinfo(res);

    return s;
}
