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


#include "dp_cache_server.h"
#include "dp_cache_db.h"
#include "sqlite3.h"

static const short uri_index = 2;
static const short tm_index = 3;
static const short fmt_index = 4;
static const short pl_len_index = 5;
static const short pl_index = 6;

sqlite3* gDpCacheDB;
int g_max_dpcache_send_speed = MAX_SEND_DP_CACHE_ITEMS_NUM;
int g_max_dpcache_row = MAX_DP_CACHE_DB_ROW;
int g_current_item_in_db =0;

static void errorCallback(void *arg, int errCode, const char *errMsg)
{
    ERROR("DP_CACHE_MOD: DP_CACHE_MOD: SQLLite Error: %s : %d", errMsg, errCode);
}

int intDpCasheDB( )
{
    if (SQLITE_OK == sqlite3_config(SQLITE_CONFIG_LOG, errorCallback))
    {
        TraceI(FLAG_DP_CACHE_DB, "DP_CACHE_MOD: SQLite debugging log initialized.");
    }
    char filename[256];
    snprintf(filename, sizeof(filename), "%s/%s", get_cache_path(), DP_CACHE_DB_PATH);
    int sqlRet = sqlite3_open_v2(filename, &gDpCacheDB, SQLITE_OPEN_READWRITE, NULL);
    if (SQLITE_OK != sqlRet)
    {
        TraceI(FLAG_DP_CACHE_DB, "DP_CACHE_MOD: RD database file did not exists, creating new table");
        sqlRet = sqlite3_open_v2(filename, &gDpCacheDB,
                                 SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE, NULL);
        if (SQLITE_OK == sqlRet)
        {
            VERIFY_SQLITE(sqlite3_exec(gDpCacheDB, DP_CACHE_TABLE, NULL, NULL, NULL));
            TraceI(FLAG_DP_CACHE_DB, "DP_CACHE_MOD:RD created RD_DEVICE_LIST table.");
            sqlRet = SQLITE_OK;
        }
    }
    else
    	sqlRet=checkDpCacheDataNum(&g_current_item_in_db);

    return sqlRet;
}

int  closeDpCacheDB()
{
    VERIFY_SQLITE(sqlite3_close_v2(gDpCacheDB));
    return SQLITE_OK;
}

int storeDpCacheData(DpCacheData * pDpCacheData)
{
     CHECK_DATABASE_INIT;
     const char *insertDpCacheData= "INSERT INTO DP_CACHE_DATA_LIST VALUES(?,?,?,?,?,?)";
     sqlite3_stmt *stmt = 0;
     VERIFY_SQLITE(sqlite3_exec(gDpCacheDB, "BEGIN TRANSACTION", NULL, NULL, NULL));
     VERIFY_SQLITE(sqlite3_prepare_v2(gDpCacheDB, insertDpCacheData, strlen(insertDpCacheData) + 1, &stmt, NULL));
     VERIFY_SQLITE(sqlite3_bind_text(stmt, uri_index, pDpCacheData->uri , strlen(pDpCacheData->uri), SQLITE_STATIC));
     VERIFY_SQLITE(sqlite3_bind_int(stmt, fmt_index, pDpCacheData->fmt));
     VERIFY_SQLITE(sqlite3_bind_int64(stmt, tm_index,  pDpCacheData->time));
     VERIFY_SQLITE(sqlite3_bind_int(stmt, pl_len_index, pDpCacheData->payload_len));
     VERIFY_SQLITE(sqlite3_bind_text(stmt, pl_index, pDpCacheData->payload, pDpCacheData->payload_len ,SQLITE_STATIC));
     if (SQLITE_DONE != sqlite3_step(stmt))
     {
        sqlite3_finalize(stmt);
	    ERROR("DP_CACHE_MOD:SQLLite Error Insert dp cache data into DB uri=%s", pDpCacheData->uri);
        return SQLITE_ERROR;
     }
    VERIFY_SQLITE(sqlite3_finalize(stmt));
    VERIFY_SQLITE(sqlite3_exec(gDpCacheDB, "COMMIT", NULL, NULL, NULL));
    g_current_item_in_db++;
    TraceI(FLAG_DP_CACHE_DB, "DP_CACHE_MOD: Insert dp cache data into DB uri=%s",pDpCacheData->uri);
    return SQLITE_OK;

}

static int deleteDpCacheData(int autoGenID)
{
    CHECK_DATABASE_INIT;
    VERIFY_SQLITE(sqlite3_exec(gDpCacheDB, "BEGIN TRANSACTION", NULL, NULL, NULL));

    sqlite3_stmt *stmt = 0;
    char *delDpCacheData = "DELETE FROM DP_CACHE_DATA_LIST WHERE ID=?";
    VERIFY_SQLITE(sqlite3_prepare_v2(gDpCacheDB, delDpCacheData, strlen(delDpCacheData) + 1, &stmt, NULL));
    VERIFY_SQLITE(sqlite3_bind_int(stmt, 1, autoGenID));

    if (SQLITE_DONE != sqlite3_step(stmt))
    {
        sqlite3_exec(gDpCacheDB, "ROLLBACK", NULL, NULL, NULL);
        sqlite3_finalize(stmt);
        return SQLITE_ERROR;
    }
    VERIFY_SQLITE(sqlite3_finalize(stmt));
    VERIFY_SQLITE(sqlite3_exec(gDpCacheDB, "COMMIT", NULL, NULL, NULL));
    if(g_current_item_in_db>0)
    	g_current_item_in_db--;
    return SQLITE_OK;
}

/*
   function: deleteOlderDpCacheData
    Parameter:
           count:  How many rows will be deleted
           sendDpCasheData: If not Null, the cached data will be sent the agent moudle before they are deleted
*/

int  deleteOlderDpCacheData ( int count, sendDpCacheDataFp sendDpCasheData)
{
    const char * queryDpCacheData = "SELECT * FROM DP_CACHE_DATA_LIST ORDER BY TIME ASC LIMIT ?";
    sqlite3_stmt *stmt = 0;
    DpCacheData pDpCacheData;
    int autoGenID=0;
    int sendCacheDataNum=0;
    int deleted = 0;

    VERIFY_SQLITE(sqlite3_prepare_v2(gDpCacheDB,queryDpCacheData, -1, &stmt, NULL) );
    VERIFY_SQLITE(sqlite3_bind_int(stmt, 1, count ));
    while (sqlite3_step(stmt) == SQLITE_ROW && deleted  < count)
    {
	     if(sendDpCasheData !=NULL)
	     {
              strncpy(pDpCacheData.uri ,((char*)sqlite3_column_text(stmt, uri_index-1)),256);
              pDpCacheData.time= sqlite3_column_int64(stmt, tm_index-1);
              pDpCacheData.fmt= sqlite3_column_int(stmt, fmt_index-1);
              pDpCacheData.payload_len= sqlite3_column_int(stmt, pl_len_index-1);
	          pDpCacheData.payload= (char*)sqlite3_column_text(stmt, pl_index-1);
	          sendDpCasheData( &pDpCacheData);
	          sendCacheDataNum++;
	     }

	     autoGenID= sqlite3_column_int(stmt, 0);
	     VERIFY_SQLITE(deleteDpCacheData(autoGenID));
               deleted  ++;
     }
     TraceI(FLAG_DP_CACHE_DB, "DP_CACHE_MOD: delete dp cache data from  DB max_num=%d: sendCacheDataNum=%d", g_current_item_in_db, sendCacheDataNum);
     return sendCacheDataNum;
}

int checkDpCacheDataNum(int* num)
{
	char * errmsg = NULL;
	char **dbResult = NULL;
	int nRow, nColumn;
	//const char *queryDpCacheDataNum = "SELECT * FROM DP_CACHE_DATA_LIST";
	const char *queryDpCacheDataNum = "SELECT COUNT(*) AS DATA_NUM FROM DP_CACHE_DATA_LIST";
	*num=0;
	VERIFY_SQLITE(sqlite3_get_table( gDpCacheDB, queryDpCacheDataNum, &dbResult, &nRow, &nColumn, &errmsg ));
	if(nRow == 1 && nColumn == 1)
	{
		TraceI(FLAG_DP_CACHE_DB, "checkDpCacheDataNum return %s as record num\n", dbResult[1]);
		* num = atoi(dbResult[1]);
	}
	else
	{
		ERROR("checkDpCacheDataNum failed to get record num. row=%d, col=%d\n", nRow,nColumn);
	}

	if(dbResult) sqlite3_free_table(dbResult);
	return SQLITE_OK;	
}




