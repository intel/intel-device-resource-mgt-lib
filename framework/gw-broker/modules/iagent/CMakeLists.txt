cmake_minimum_required(VERSION 2.8.11)
message("iagentcore...")
project(iagent)

if( DEFINED BUILTIN_IBROKER)
  add_definitions(-DBUILTIN_IBROKER)
  message(STATUS "iAgent: Builtin IBROKER enabled")
endif()

#set paths
set(SSG_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../..)
set(GW_BROKER_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../..)
set(IAGENT_CORE_DIR ${GW_BROKER_DIR}/../iagent-core)
set(EXTERNAL_DIR ${SSG_DIR}/external)

include(${SSG_DIR}/plugin-sdk/common.cmake)
include(${IAGENT_CORE_DIR}/iagent-core.cmake)

include(${SHARED_LIBS_DIR}/utils/ssg_utils.cmake)
include(${SHARED_LIBS_DIR}/coap/ex_coap.cmake)
include(${SHARED_LIBS_DIR}/AMS-SDK/ex_ams.cmake)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/inc
    ${GW_BROKER_DIR}/common
    ${EXTERNAL_DIR}
    ${EXTERNAL_DIR}/wakaama/examples/shared
    ${EXTERNAL_DIR}/wakaama/core
)

add_definitions(-DLOG_TAG="iAgent" -DLOG_MY_MASK_ID=1)


#version number
if(NOT IAGENT_VERSION)
    set(IAGENT_VERSION 1)
endif()

#configuration file
configure_file(
    "${IAGENT_CORE_DIR}/iagent_config.h.in"
    "${IAGENT_CORE_DIR}/iagent_config.h"
)

set(EXTERNAL_SOURCE
    ${SSG_UTILS_SOURCE}
    ${EX_AMS_SOURCE}
    ${EX_COAP_SOURCE}
    ${EXTERNAL_DIR}/wakaama/examples/shared/connection.c 
    ${EXTERNAL_DIR}/url-parser-c/url_parser.c
)

set(IAGENTCORE_SOURCE
    ${IAGENT_CORE_SOURCE}
    ${CMAKE_CURRENT_SOURCE_DIR}/src/iagent.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/rd_bus.cpp
    ${GW_BROKER_DIR}/common/broker_rest_convert.c
    ${GW_BROKER_DIR}/common/module_common.c
    ${EXTERNAL_SOURCE}
)

add_library(iagent MODULE ${IAGENTCORE_SOURCE})
target_link_libraries(iagent dl pthread)
linkSharedUtil(iagent)
