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



#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include "parson.h"
#include "er-coap-engine.h"
#include "iagent_config.h"
#include "iagent_base.h"
#include "logs.h"


void set_reset_handle(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
    system ("reboot");
}

/*---------------------------------------------------------------------------*/
RESOURCE(res_set_reset, "", set_reset_handle, NULL,
         NULL, NULL);
/*---------------------------------------------------------------------------*/
