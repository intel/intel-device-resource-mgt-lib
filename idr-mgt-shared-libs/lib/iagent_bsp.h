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
 * iagent_bsp.h
 *
 *  Created on: May 9, 2017
 *      Author: xin
 */

#ifndef LIB_IAGENT_BSP_H_
#define LIB_IAGENT_BSP_H_

#ifdef __cplusplus
extern "C" {
#endif

#if defined(RUN_ON_LINUX) | 1
#include "linux/iagent_bsp_linux.h"
#elif defined (RUN_ON_VXWORKS)

#elif defined (RUN_ON_ZEPHRY)

#endif

typedef  unsigned long tick_time_t;

time_t bh_bsp_get_time();
ptr_sync_t bh_bsp_create_syncobj();
void bh_bsp_delete_syncobj(ptr_sync_t);
void bh_bsp_lock(ptr_sync_t);
void bh_bsp_unlock(ptr_sync_t);
int bh_bsp_wait(ptr_sync_t sync_obj, int timeout_ms, bool hold_lock);
void bh_bsp_wakeup(ptr_sync_t, bool hold_lock);
uint32_t bh_get_elpased_ms(uint32_t * last_system_clock);
tick_time_t bh_get_tick_ms();
tick_time_t bh_get_tick_sec();

#ifdef __cplusplus
}
#endif
#endif /* LIB_IAGENT_BSP_H_ */
