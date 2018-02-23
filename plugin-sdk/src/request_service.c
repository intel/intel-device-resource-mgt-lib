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

#include "plugin_sdk.h"
#include "sdk_internals.h"
#include "plugin_dlist.h"


typedef struct
{
	void * handler;
	void * user_data;
} response_ctx_data_t;


// ensure the user handler is always called for all situations
static int cb_request(void * ctx_data, void * data, int len, unsigned char format)
{
		// todo: implement it
	if (T_Broker_Message_Handle != format)
		return -1;

	response_ctx_data_t * user_ctx = (response_ctx_data_t*)ctx_data;
	Service_Result_Handler handler = (Service_Result_Handler) user_ctx->handler;

	// timeout
	if(data == NULL)
	{
		handler(NULL, user_ctx->user_data);
		goto end;
	}

	restful_response_t response = {0};

	if(decode_response((MESSAGE_HANDLE) data, &response))
	{
		handler(&response, user_ctx->user_data);
	}
	else
	{
		handler(NULL, user_ctx->user_data);
		goto end;
	}

end:
	if (data)
	{
		Message_Destroy((MESSAGE_HANDLE)data);
	}

	if(user_ctx)
	{
		trans_free_ctx(user_ctx);
	}

	return 0;
}


// post the callback to ports handler thread for avoiding thread confliction
static int post_cb_working_thread(callback_t * cb)
{
	if(cb->format == T_Broker_Message_Handle)
		cb->data = Message_Clone((MESSAGE_HANDLE)cb->data);
	dlist_post(g_framework_ctx->internal_queue, T_Callback, cb, NULL);
	wakeup_working_thread(g_framework_ctx);
	return 0;
}



int request_bus_service(restful_request_t * request, Service_Result_Handler response_handler, void * user_data, int timeout)
{
	MESSAGE_CONFIG msgConfig;

	request->mid = bh_gen_id(g_framework_ctx->transactions_ctx);
	request->src_module = g_framework_ctx->module_name;
	MESSAGE_HANDLE message = encode_request(request);
	if(message == NULL) return -1;

	response_ctx_data_t * ctx = (response_ctx_data_t*)	trans_malloc_ctx(sizeof(response_ctx_data_t));

	memset(ctx, 0, sizeof(response_ctx_data_t));
	ctx->handler = response_handler;
	ctx->user_data = user_data;

	void * thread_handler = NULL;
	if(g_framework_ctx->internal_queue)
		thread_handler = post_cb_working_thread;

	bh_wait_response_async(g_framework_ctx->transactions_ctx,
			request->mid ,
			cb_request,
			ctx,
			timeout,
			(void *)thread_handler);

	(void)Broker_Publish(g_framework_ctx->broker_handle, (MODULE_HANDLE)g_framework_ctx->module_handle, message);
	Message_Destroy(message);

	return 0;
}
