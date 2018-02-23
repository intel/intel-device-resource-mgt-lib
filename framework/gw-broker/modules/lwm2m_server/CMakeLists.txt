#Copyright (c) Microsoft. All rights reserved.
#Licensed under the MIT license. See LICENSE file in the project root for full license information.
cmake_minimum_required(VERSION 2.8.11)
message("lwm2m_server...")
project (gwbroker)

add_definitions(-DLOG_TAG="LWM2M" -DLOG_MY_MASK_ID=4 -DUSE_WAKAAMA_COAP=1 -DREST_MAX_CHUNK_SIZE=1024)

#set paths
set(SSG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../..)
set(GW_BROKER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../..)
set(IAGENT_CORE_DIR ${GW_BROKER_DIR}/../iagent-core)
set(UTILS_DIR ${SSG_DIR}/utils)
set(EXTERNAL_DIR ${SSG_DIR}/external)

include(${GW_BROKER_DIR}/../../plugin-sdk/common.cmake)

include(${SHARED_LIBS_DIR}/utils/ssg_utils.cmake)
#include(${SHARED_LIBS_DIR}/coap/ex_coap.cmake)
include(${SHARED_LIBS_DIR}/AMS-SDK/ex_ams.cmake)

# WAKAAMA
include(${EXTERNAL_DIR}/wakaama/core/wakaama.cmake)
include(${EXTERNAL_DIR}/wakaama/examples/shared/shared.cmake)
add_definitions(-DLWM2M_SERVER_MODE -DLWM2M_LITTLE_ENDIAN -DLWM2M_BOOTSTRAP -DLWM2M_SUPPORT_JSON)
add_definitions(${WAKAAMA_DEFINITIONS})
add_definitions(-D_PLATFORM=${_PLATFORM})

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/inc
    ${GW_BROKER_DIR}/common
    ${EXTERNAL_DIR}
    ${WAKAAMA_SOURCES_DIR}
    ${SHARED_SOURCES_DIR}
    ${IAGENT_CORE_DIR}
)

#include_directories(
#    ${WAKAAMA_SOURCES_DIR}/er-coap-13
#)

# SOURCE
set(EXTERNAL_SOURCE
    ${SHARED_LIBS_DIR}/AMS-SDK/ams_path.c
    ${WAKAAMA_SOURCES}
    ${SHARED_SOURCES}
    ${SSG_UTILS_SOURCE}
    ${GW_BROKER_DIR}/common/module_common.c
)
	
set(LWM2MSERVER_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/src/lwm2m_module.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/client_reg_handler.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/command_handler.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/handle_bus_request.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/lwm2mserver.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/obs_manager.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/wrapper.cpp
    ${EXTERNAL_SOURCE}
)

#this builds the lwm2m_server dynamic library
add_library(lwm2m_server MODULE  ${LWM2MSERVER_SOURCES} )
target_link_libraries(lwm2m_server dl pthread)
linkSharedUtil(lwm2m_server)
