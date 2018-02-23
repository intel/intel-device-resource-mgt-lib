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
#include <dlfcn.h>
#include <stdlib.h>
#include <linux/prctl.h>

#include "gateway.h"
#include "path.h"
#include "logs.h"
#define LOG_TAG "GW-BROKER"
extern char * getExecPath (char * path,size_t dest_len, char * argv0);

int main(int argc, char** argv)
{
    prctl (PR_SET_NAME, "broker_main");

	char cfgname[256];
    char cmd[1024];
	char buf[256] = {0};
	char str_time[200] = {0};

	now_str(str_time);
	getExecPath(buf, sizeof(buf), argv[0]);
	printf("gw-broker: now it is [%s], start executing from [%s]\n", str_time, buf);


	path_init(argv[0]);

	log_init("gw-broker", get_log_path(), get_local_config_path(cfgname, sizeof(cfgname),"logcfg.ini"));

	snprintf(cmd, sizeof(cmd), "%sgw_broker.pid",buf);
    if (already_running(cmd))
    {
        WARNING("Process gw_broker is already running\n");
        printf("Process gw_broker is already running\n");
        return 1;
    }

	snprintf(cmd, sizeof(cmd), "%slog.txt",buf);
	if(access(cmd, 0) == 0)
	{
		snprintf(cmd, sizeof(cmd), "mv -f %slog.txt %sclosed/logger_%s.log", buf, get_log_path(), str_time);
		system(cmd);
	}

    strcat(buf, "gw_broker_lin.json");
	char * config_path = buf;
    if (argc < 2)
    {
        printf("usage: iagent_sample configFile\n");
        printf("where configFile is the name of the file that contains the Gateway configuration\n");
        WARNING("trying to load default config file [gw_broker_lin.json]\n");
        printf("trying to load default config file [gw_broker_lin.json]\n");
    }
    else
    {
    	config_path = argv[1];
    	WARNING("trying to load  config file [%s]\n", config_path);
    }


	double (*cosine)(double);
	char *error;
#ifdef OPTION_DEBUG
    void *handle;
	handle = dlopen("./modules/iagent/libiagent.so", RTLD_LAZY);
	if (!handle)
	{
		fprintf(stderr, "%s\n", dlerror());
		exit(EXIT_FAILURE);
	}

    handle = dlopen("./modules/modbus_server/libmodbus_server.so", RTLD_LAZY);
    if (!handle)
    {
        fprintf(stderr, "%s\n", dlerror());
        exit(EXIT_FAILURE);
    }

    handle = dlopen("./modules/lwm2m_server/liblwm2m_server.so", RTLD_LAZY);
    if (!handle)
    {
        fprintf(stderr, "%s\n", dlerror());
        exit(EXIT_FAILURE);
    }
#endif

    GATEWAY_HANDLE gateway;

	if ((gateway = Gateway_CreateFromJson(config_path)) == NULL)
	{
		WARNING("failed to create the gateway from JSON\n");
		return -1;
	}
	else
	{
		WARNING("gateway successfully created from JSON\n");
		while(1)
		{
		   sleep(5000);
		}
		Gateway_Destroy(gateway);
	}
    return 0;
}
