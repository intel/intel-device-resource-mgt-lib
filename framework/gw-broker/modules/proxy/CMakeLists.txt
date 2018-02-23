#Copyright (c) Microsoft. All rights reserved.
#Licensed under the MIT license. See LICENSE file in the project root for full license information.

cmake_minimum_required(VERSION 2.8.11)
message("iagentcore...")
project(iagent)

if( DEFINED NO_IBROKER)
  add_definitions(-DDISABLE_IBROKER)
  message(STATUS "iAgent: IBROKER disabled")
endif()

#set paths
set(SSG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../..)
set(GW_BROKER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../..)

include(${GW_BROKER_DIR}/../../plugin-sdk/common.cmake)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/inc)
#include_directories(${CMAKE_CURRENT_SOURCE_DIR}/inc)

set(proxy_sources
    ${CMAKE_CURRENT_SOURCE_DIR}/src/proxy.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/myredis.c    
)

set(proxy_headers
    ${CMAKE_CURRENT_SOURCE_DIR}/inc/proxy.h
    ${CMAKE_CURRENT_SOURCE_DIR}/inc/myredis.h    
)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/inc)
include_directories(${SSG_DIR}/external/azure-iot-gateway-sdk/modules/common)

add_library(proxy MODULE ${proxy_sources}  ${proxy_headers})
target_link_libraries(proxy dl pthread)
linkSharedUtil(proxy)
