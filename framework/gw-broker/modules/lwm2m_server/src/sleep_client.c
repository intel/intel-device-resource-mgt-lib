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
 * sleep_client.c
 *
 *  Created on: Nov 26, 2016
 *      Author: xwang98
 *
 *  Requirements:
 *  1. manage the active window of clients
 *  2. Queue bus message if not in client wake window
 *  3. Send queued messages to client
 *  4. overwrite the read/obs/write/exec request for the same resource
 *  5. drop all queued request if a client goes offline
 *
 *  Note: This is low priority for implementation
 */


