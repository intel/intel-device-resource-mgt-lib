cmake_minimum_required(VERSION 2.8.11)
message("database...")
project(database_server)

#set paths
set(SSG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../..)
set(GW_BROKER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../..)
set(IAGENT_CORE_DIR ${GW_BROKER_DIR}/../iagent-core)
set(EXTERNAL_DIR ${SSG_DIR}/external)

include(${GW_BROKER_DIR}/../../plugin-sdk/common.cmake)
include(${CMAKE_CURRENT_SOURCE_DIR}/src/dp_cache/ic_dpcache.cmake)

include(${SHARED_LIBS_DIR}/utils/ssg_utils.cmake)
include(${SHARED_LIBS_DIR}/coap/ex_coap.cmake)
include(${SHARED_LIBS_DIR}/AMS-SDK/ex_ams.cmake)

add_definitions(-DLOG_TAG="DB" -DLOG_MY_MASK_ID=3)


include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/inc
    ${IAGENT_CORE_DIR}
    ${EXTERNAL_DIR}/sqlite-amalgamation
    ${GW_BROKER_DIR}/common
)

set(EXTERNAL_SOURCE
    ${EXTERNAL_DIR}/sqlite-amalgamation/sqlite3.c
    ${SSG_UTILS_SOURCE}
    ${EX_COAP_SOURCE}
    ${SSG_UTILS_SOURCE}
    ${EX_AMS_SOURCE}
)

set(DATABASE_SOURCE
    ${IC_DPCACHE_SOURCE}
    ${CMAKE_CURRENT_SOURCE_DIR}/src/database_module.c
    ${GW_BROKER_DIR}/common/broker_rest_convert.c
    ${GW_BROKER_DIR}/common/module_common.c
    
    ${EXTERNAL_SOURCE}
)

#this builds the database dynamic library
add_library(database_server MODULE ${DATABASE_SOURCE})
target_link_libraries(database_server dl pthread m)
linkSharedUtil(database_server)