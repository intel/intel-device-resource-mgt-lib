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


#ifndef DP_CACHE_DB_H_
#define DP_CACHE_DB_H_

#include <time.h>
#include "sqlite3.h"

#define DP_CACHE_DB_PATH "dpCache.db"

//To limit the DB size, the oldest stored data items will be deleted if the total stored rows over  MAX_DP_CACHE_DB_ROW
#define MAX_DP_CACHE_DB_ROW      100000

//To avoide sned too many messages to the gw_broker when  the cload_is_on_line, the thread will sleep 10 secondes after send 100 cached data items
#define MAX_SEND_DP_CACHE_ITEMS_NUM 100

#define VERIFY_SQLITE(arg) \
    if (SQLITE_OK != (arg)) \
    { \
        ERROR( "DP_CACHE_MOD: Error in " #arg ", Error Message: %s",  sqlite3_errmsg(gDpCacheDB)); \
        sqlite3_exec(gDpCacheDB, "ROLLBACK", NULL, NULL, NULL); \
        return SQLITE_ERROR; \
    }

#define CHECK_DATABASE_INIT \
    if (!gDpCacheDB) \
    { \
        ERROR( "DP_CACHE_MOD: Database is not initialized."); \
        return SQLITE_ERROR; \
    }
#define DP_CACHE_TABLE \
    "create table DP_CACHE_DATA_LIST(ID INTEGER PRIMARY KEY AUTOINCREMENT," \
    "URI  VARCHAR(256) NOT NULL," \
    "TIME UNSIGNED BIG INT NOT NULL," \
    "FORMAT INT NOT NULL," \
    "PAYLOAD_LEN INT NOT NULL," \
    "PAYLOAD VARCHAR[1024] NOT NULL);"


typedef struct DpCacheData_St
{
    time_t time ;
    int fmt;
    int payload_len;
    char* payload;
    char uri[256];
} DpCacheData;

typedef void (*sendDpCacheDataFp) (DpCacheData *);
int  intDpCasheDB( );
int  closeDpCacheDB( );
int  storeDpCacheData(DpCacheData * pDpCacheData);
int  deleteOlderDpCacheData (int count, sendDpCacheDataFp sendDpCasheData);
int  checkDpCacheDataNum(int* num);

#endif



