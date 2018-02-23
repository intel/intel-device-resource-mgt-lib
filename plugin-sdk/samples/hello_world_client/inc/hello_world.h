// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

#ifndef HELLO_WORLD_H
#define HELLO_WORLD_H

#include "module.h"
#include <pthread.h>
#include <stdbool.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <fcntl.h>
#include <printf.h>
#include <errno.h>
#include <sys/stat.h>
#include <linux/prctl.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/prctl.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

#include "plugin_sdk.h"
#include "azure_c_shared_utility/xlogging.h"
#include "azure_c_shared_utility/condition.h"

#ifdef __cplusplus
extern "C"
{
#endif

MODULE_EXPORT const MODULE_API* MODULE_STATIC_GETAPI(HELLOWORLD_MODULE)(MODULE_API_VERSION gateway_api_version);

#ifdef __cplusplus
}
#endif


#endif /*HELLO_WORLD_H*/
