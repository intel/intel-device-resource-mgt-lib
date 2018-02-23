#Copyright (c) Microsoft. All rights reserved.
#Licensed under the MIT license. See LICENSE file in the project root for full license information.

cmake_minimum_required(VERSION 2.8.11)
message("start build cmake for gw_broker")
project(gw_broker)

#set paths
set(SSG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../..)

include(${SSG_DIR}/idr-mgt-shared-libs/utils/ssg_utils.cmake)
include(${SSG_DIR}/plugin-sdk/common.cmake)
include(${SHARED_LIBS_DIR}/AMS-SDK/ex_ams.cmake)


include_directories(${CMAKE_CURRENT_SOURCE_DIR}/inc)


set(EXTERNAL_SOURCE
)

set(gw_broker_sources ${CMAKE_CURRENT_SOURCE_DIR}/src/main.c ${SSG_UTILS_SOURCE} )
set(gw_broker_sources ${gw_broker_sources} ${CMAKE_CURRENT_SOURCE_DIR}/src/gw_broker_lin.json)
set_source_files_properties(${CMAKE_CURRENT_SOURCE_DIR}/src/gw_broker_lin.json PROPERTIES HEADER_FILE_ONLY ON)


add_executable(gw_broker ${gw_broker_sources})
set_target_properties(gw_broker PROPERTIES LINKER_LANGUAGE CXX)

linkSharedUtil(gw_broker)

add_custom_command(
    TARGET gw_broker POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy ${CMAKE_CURRENT_SOURCE_DIR}/src/gw_broker_lin.json  ${EXECUTABLE_OUTPUT_PATH}
)
