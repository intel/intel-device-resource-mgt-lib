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


#include <stdio.h>
#include <signal.h>
#include <fcntl.h>
#include <time.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdint.h>
#include <sys/statfs.h>
#include <stdbool.h>
#include <ctype.h>
#include "logs.h"
#include "iniparser.h"
#include "misc.h"

#ifndef LOG_TAG
#define LOG_TAG						"LOG_UTIL"
#endif
unsigned char log_level 		= 0;
unsigned long log_tag_mask[LOG_MASK_SIZE]  = {0};

// Max log file size limited to 50K Bytes
static unsigned log_size_max = 50;

// in case log printed before/without log_init
// Let's use log_fp only for the log file opened
static FILE* log_fp = NULL;

// app name have no full path, like lwm2mserver
char app_name[24] = {0};

// logname  is full path
static char logname[256] = {0};
static char * g_log_path = NULL;
static char * g_log_cfg_path = "";

static void signal_handler(int signal);

// the function should be called in signal_handler
// when daemon found disk quota reach, log file size
// exceed or other specific condition.

char * stime2(char *s , int len, char * time_format)
{
	time_t timep;
	struct tm *p;
	static char st[100];
	if(s == NULL){
		s = st;
		len = sizeof(st);
	}

	time(&timep);
	p=localtime(&timep); /*取得当地时间*/
	strftime(s, len, time_format, p);
	return s;
}

char * now_str(char *s )
{
	time_t timep;
	struct tm *p;
	static char st[100];
	if(s == NULL)
		s = st;

	time(&timep);
	p=localtime(&timep);
	sprintf(s,"%d-%d-%d_%d-%d",(1900+p->tm_year),(1+p->tm_mon),p->tm_mday,p->tm_hour,p->tm_min);

	return s;

}

static char* log_filename()
{
	if(g_log_path == NULL)
		return "";

	snprintf(logname, sizeof(logname), "%s%s.log", g_log_path, app_name);
	return logname;
}

//


FILE*  create_log()
{
	struct tm *tmp;
	time_t t;
	long size;

	FILE* local_log_fp ;

	local_log_fp = fopen(log_filename(), "a+");
	if(NULL == local_log_fp)
	{
		printf("!!! Failed to create log file %s", logname);
		return NULL;
	}
	else
	{
	    fseek (local_log_fp, 0, SEEK_END);   // non-portable
	    size=ftell (local_log_fp);
	    if(size != 0)
	    {
	    	fprintf(local_log_fp, "\r\n\r\n\r\n");
	    }

		t = time(NULL);
		tmp = localtime(&t);
		if(tmp)
		{
			char str[200];
			strftime(str, sizeof(str), "%Y%m%d-%H%M%S", tmp);
			fprintf(local_log_fp, "Log open time:%s\r\n", str);

		}
	}


	return local_log_fp;
}

// close the active log and move it into the closed sub folder
// and then create a new active log
static void log_refresh(int upload)
{
	char cmd[512];
	struct tm *tmp;
	time_t t;

                if(g_log_path == NULL) return;

	// no log file.
	if((NULL == log_fp) || (stdout == log_fp))
		return;


	t = time(NULL);
	tmp = localtime(&t);
	if(tmp)
	{
		char str[16]; // 20101112-123456, 15Bytes+1 '0'
		strftime(str, sizeof(str), "%Y%m%d-%H%M%S", tmp);
		fprintf(log_fp, "\r\nClosed time:%s\r\n", str);
	}

	FILE * current = log_fp;

	// ensure other thread can access log_fp during recreate the log file
	log_fp = stdout;

	fflush(current);
	fclose(current);
	
	snprintf(cmd, sizeof(cmd), "mv -f %s %sclosed/%s-%s.log", logname, g_log_path, app_name, now_str(NULL));
	system(cmd);

	log_fp = create_log();

	log_print("LOG", "Log level=%d, mask=%X. LOG_ERROR=%d", log_level, log_tag_mask, LOG_ERROR);
	log_print("LOG", "platform = [%s]", SZ_PLATFORM);

}

static int active_log_size = 0;
void check_size_log(int add_len)
{
	// no log file.
	if((NULL == log_fp) || (stdout == log_fp) || (stderr == log_fp))
		return;

	active_log_size += add_len;

	if(add_len > log_size_max)  // KBytes
	{
		log_refresh(0);
	}
}

char* log_to()
{
	if(log_fp)
	{
		return "file";
	}
	else
	{
		return "stdout";
	}
}

int log_update_config(int init)
{
	
	dictionary  *   ini;
	const char* log;
	int ret = 0;
	int i;

	ini = iniparser_load(g_log_cfg_path);
	if(NULL == ini)
	{
		WARNING("Unable to load log config %s\n", g_log_cfg_path);
		return 1;
	}

	log_level	 = iniparser_getint(ini, "default:level", LOG_VERBOSE);
	log_tag_mask[0] = iniparser_getint(ini, "default:mask", 0xFFFF);
	for(i=1;i<LOG_MASK_SIZE;i++) log_tag_mask[i] = log_tag_mask[0];

	log_size_max = iniparser_getint(ini, "default:sizemax", 50);
	log = iniparser_getstring(ini, "default:out", "logfile");

	// overwrite the value if the app has its own settings
	if(app_name[0])
	{
		char str[128];
		strcpy(str, app_name);
		strcat(str, ":level");
		log_level	 = iniparser_getint(ini, str, log_level);

		strcpy(str, app_name);
		strcat(str, ":mask");
		log_tag_mask[0] = iniparser_getint(ini, str, log_tag_mask[0]);
		for(i=1;i<LOG_MASK_SIZE;i++)
		{
			snprintf(str, sizeof(str), "%s:mask-%d", app_name, i);
			log_tag_mask[i] = iniparser_getint(ini, str, log_tag_mask[i]);
		}


		strcpy(str, app_name);
		strcat(str, ":sizemax");
		log_size_max = iniparser_getint(ini, str, log_size_max);

		strcpy(str, app_name);
		strcat(str, ":out");
		log = iniparser_getstring(ini, str, log);
	}

	// When the disk size is below the threshold, the daemon may set
	// urgent log control and send signal 11 for reloading the vales
	log_level	 = iniparser_getint(ini, "urgent:level", log_level);
	log_tag_mask[0] = iniparser_getint(ini, "urgent:mask", log_tag_mask[0]);
	log = iniparser_getstring(ini, "urgent:out", log);

	if(!strcmp("stdout", log) || !strcmp("stderr", log))
	{
		printf("config file: log to stdout!\n");
		if(log_fp)
		{
			fclose(log_fp);
			log_fp = NULL;
		}
		ret = 1;
	}
	else if(log_fp == NULL)
	{
	    log_fp = create_log();
	}

	iniparser_freedict(ini);

	log_print("LOG", "Log level=%d, mask=%X. LOG_ERROR=%d", log_level, log_tag_mask[0], LOG_ERROR);
	log_print("LOG", "platform = [%s]", SZ_PLATFORM);

	return ret;
}

void log_init(const char* app, const char* log_path, const char* log_cfg_path)
{
	char closed_dir[512];
	// register signal handler anyway
	signal(SIGUSR1, signal_handler);
	signal(SIGUSR2, signal_handler);

	g_log_path = strdup(log_path);
	g_log_cfg_path = strdup(log_cfg_path);

	snprintf(closed_dir, sizeof(closed_dir),"%sclosed", g_log_path);
	make_full_dir(closed_dir);

	if(NULL == app)
	{
		printf("app is null\n");
		// never assign stdout or stderr to log_fp.
		return;
	}

	strncpy(app_name, app, sizeof(app_name));

	log_update_config(1);
	printf("print to %s, app_name=%s\n", log_to(), app_name);
}

int log_read_level()
{
	return log_level;
}

int log_write_level(char* file, unsigned char level)
{
		int fd;
		
		if(NULL == file)
		{
			return -1;
		}
		
		if((level < LOG_VERBOSE) || (level > LOG_ERROR))
		{
			return -1;
		}
		
		fd = open(file,O_WRONLY);
		if(fd > 0)
		{
			level += 0x30; // change to readable character
			
			if(1 == write(fd, &level, 1))
			{						
				log_level = level - 0x30; // update global log_level
			}
			else
			{
				return -1;
			}
			close(fd);
		}
		else
		{
			return -1;
		}
		return 0;
}


void log_setlevel(int level)
{
	if((level >= LOG_VERBOSE) && (level <= LOG_ERROR))
	{
		log_level = level;
	}
}

void log_set_tag_mask(int tag_mask)
{
	log_tag_mask[LOG_MY_MASK_ID] |= tag_mask;
}

void log_clear_tag_mask(int tag_mask)
{
	log_tag_mask[LOG_MY_MASK_ID] &= ~tag_mask;
}

FILE * log_get_handle()
{
	if(NULL == log_fp)
		return stderr;
	else
		return log_fp;
}

void log_print(const char* tag, const char* fmt, ...)
{
	va_list ap;
	struct tm tmp = {0};
	time_t t;

	char buf[LOG_BUF_SIZE];
	char tbuf[256];


	t = time(NULL);
	if(localtime_r(&t, &tmp))
	{
		if(!strftime(tbuf, 256, "%y-%m-%d %H:%M:%S", &tmp))
		{
			tbuf[0] = 0;
		}
	}
	else
	{
		tbuf[0] = 0;
	}

	va_start(ap, fmt);
	vsnprintf(buf, LOG_BUF_SIZE, fmt, ap);
	va_end(ap);

	if(NULL == log_fp)
	{
		fprintf(stdout, "%s [%s] %s\r\n",tbuf, tag, buf);
		fflush(stdout);
	}
	else
	{
		fprintf(log_fp, "%s [%s] %s\r\n",tbuf, tag, buf);
		fflush(log_fp);

		/// check if the size of current log exceed the max single log size
		/// check_size_log(strlen(tbuf)+ strlen(tag) + strlen(buf)+ 7);
	}


}

void signal_handler(int signal)
{
	switch(signal)
	{
		case SIGUSR1:
			WARNING( "[%s] SIGUSR1 received, update config..", app_name);
			log_update_config(0);
			break;

		case SIGUSR2:
			WARNING("[%s] SIGUSR2 received, generating new log..", app_name);
			log_refresh(1);
			break;
	}
}


void log_buffer(char * title, void * buffer,
                   int length)
{
    int i;

    FILE * stream = stderr;
    char buf[100];
    char * p = (char*)buffer;

    if (length == 0) return;

    if(log_fp) stream = log_fp;
    fprintf(stream, "[%s] %s\n", now_str(buf), title?title:"data");

    i = 0;
    while (i < length)
    {
        int j;

/*        fprintf(stream, "   ");
        memcpy(array, (uint8_t*)buffer+i, 16);
        for (j = 0 ; j < 16 && i+j < length; j++)
        {
            fprintf(stream, "%02X ", array[j]);
            if (j%4 == 3) fprintf(stream, " ");
        }
        if (length > 16)
        {
            while (j < 16)
            {
                fprintf(stream, "   ");
                if (j%4 == 3) fprintf(stream, " ");
                j++;
            }
        }
*/
        fprintf(stream, " ");
        for (j = 0 ; j < 64 && i+j < length; j++)
        {
            if (isprint(*p))
            	sprintf(buf+j, "%c", *p);
            else
                buf[j]= '.';

            p++;
        }
        buf[j]= '\n';
        buf[j+1]= 0;
        fprintf(stream, "%s", buf);

        i += 64;
    }
}

#ifdef LOG_TEST
int main(int argc, char** argv)
{
	signal(SIGUSR1, signal_handler);
	signal(SIGUSR2, signal_handler);
	
	ERROR("trace %d E", 1);
//	ERROR("trace %d E", 2);

	while(1); // test signal, kill -10 or kill -12
	return 0;
}

#endif
