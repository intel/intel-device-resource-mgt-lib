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


#ifndef APPS_GW_BROKER_COMMON_BROKER_REST_CONVERT_H_
#define APPS_GW_BROKER_COMMON_BROKER_REST_CONVERT_H_

#include <errno.h>

#include "azure_c_shared_utility/base64.h"
#include "azure_c_shared_utility/constmap.h"
#include "azure_c_shared_utility/crt_abstractions.h"
#include "azure_c_shared_utility/gb_stdio.h"
#include "azure_c_shared_utility/gb_time.h"
#include "azure_c_shared_utility/gballoc.h"
#include "azure_c_shared_utility/map.h"
#include "azure_c_shared_utility/strings.h"
#include "azure_c_shared_utility/doublylinkedlist.h"
#include "azure_c_shared_utility/threadapi.h"
#include "azure_c_shared_utility/lock.h"

#include "message.h"
#include "broker.h"


#include "module_common.h"
#ifdef __cplusplus
extern "C"
{
#endif


#include "er-coap-constants.h"
#include "er-coap.h"


bool convert_bus_msg_to_coap(MESSAGE_HANDLE brokre_msg, coap_packet_t *coap_message);
MESSAGE_HANDLE coap_to_broker_msg(coap_packet_t *coap_message, unsigned long id);
MESSAGE_HANDLE coap_to_bus_msg(coap_packet_t *coap_message, unsigned long id, const char * dest_module);
void publish_message_on_broker(MESSAGE_HANDLE msg);
bool setup_bus_restful_message(MESSAGE_CONFIG *msgConfig, char * tag, int fmt, char * url_path,
		char * query, int code, void * payload, int payload_len);


#ifdef __cplusplus
}
#endif
#endif /* APPS_GW_BROKER_COMMON_BROKER_REST_CONVERT_H_ */
