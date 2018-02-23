cmake_minimum_required(VERSION 2.8.11)
message("iagentcore...")
project(iagentcore)

#set build configuration
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wno-literal-suffix ")
SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g2 -ggdb")  
SET(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")

#version number
if(NOT IAGENT_VERSION)
    set(IAGENT_VERSION 1)
endif()

#set options
option(RUN_ON_LINUX "build on linux platform" ON)
option(RUN_ON_VXWORKS "build on vxworks platform" OFF)
option(RUN_ON_ZEPHRY "build on zephry platform" OFF)
option(RUN_ON_STM32 "build on stm32 platform" OFF)
option(RUN_AS_BROKER_MODULE "run an a module of ms-broker" OFF)
option(OPTION_DEBUG "open debug infomation" ON)

#set path
if (NOT SSG_DIR)
    set(SSG_DIR ${CMAKE_CURRENT_LIST_DIR}/../..)
    message(${SSG_DIR})
    set(IAGENT_CORE_DIR ${SSG_DIR}/apps/iagent-core)
    set(COAP_DIR ${SSG_DIR}/external/imrt_shared_libs/coap)
    set(EXTERNAL_DIR ${SSG_DIR}/external)
    set(UTILS_DIR ${SSG_DIR}/utils)
endif()

set(WAKAAM_SHARE_DIR ${EXTERNAL_DIR}/wakaama/examples/shared)

#configuration file
configure_file(
  "${IAGENT_CORE_DIR}/iagent_config.h.in"
  "${IAGENT_CORE_DIR}/iagent_config.h"
  )


# WAKAAMA
include(${EXTERNAL_DIR}/wakaama/core/wakaama.cmake)
add_definitions(-DLWM2M_CLIENT_MODE -DLWM2M_LITTLE_ENDIAN -DLWM2M_BOOTSTRAP -DLWM2M_SUPPORT_JSON)
add_definitions(${WAKAAMA_DEFINITIONS})
add_definitions(-D_PLATFORM=${_PLATFORM})

#sub_directory
include(${IAGENT_CORE_DIR}/lib/ic_lib.cmake)
include(${PLATFORM_DIR}/ic_platform.cmake)
include(${IAGENT_CORE_DIR}/resources/ic_resources.cmake)
include(${IAGENT_CORE_DIR}/src/ic_src.cmake)

include_directories(
    ${IC_LIB_DIR}
    ${IC_RESOURCES_DIR}
    ${IC_SRC_DIR}
    ${IAGENT_CORE_DIR}/src/devices
    ${IAGENT_CORE_DIR}
    ${COAP_DIR}/rest-engine 
    ${COAP_DIR}/extension 
    ${COAP_DIR}/er-coap
    ${WAKAAMA_SOURCES_DIR}
    ${WAKAAM_SHARE_DIR}
    ${EXTERNAL_DIR}/iniparser/src
    ${SSG_DIR}/jsoncpp
    ${SSG_DIR}/utils
)

set(EXTERNAL_SOURCE
    ${UTILS_DIR}/logs.c
    ${UTILS_DIR}/misc.cpp
    ${UTILS_DIR}/path.c
    ${EXTERNAL_DIR}/iniparser/src/iniparser.c
    ${EXTERNAL_DIR}/iniparser/src/dictionary.c
    ${SSG_DIR}/jsoncpp/jsoncpp.cpp
)

set(COAP_SOURCE
    ${COAP_DIR}/er-coap/er-coap.c
    ${COAP_DIR}/er-coap/er-coap-engine.c
    ${COAP_DIR}/er-coap/er-coap-transactions.c
    ${COAP_DIR}/er-coap/er-coap-res-well-known-core.c
    ${COAP_DIR}/extension/list.c
    ${COAP_DIR}/extension/coap_request.c
    ${COAP_DIR}/extension/coap_platforms.c
    ${COAP_DIR}/extension/coap_partial_parse.c
    ${COAP_DIR}/rest-engine/rest-engine.c
)

set(IAGENTCORE_SOURCE
    ${IC_LIB_SOURCE}
    ${IC_PLATFORM_SOURCE}
    ${IC_RESOURCES_SOURCE}
    ${IC_SRC_SOURCE}
    ${EXTERNAL_SOURCE}
    ${COAP_SOURCE}
    ${WAKAAM_SHARE_DIR}/connection.c 
)

#set(EXECUTABLE_OUTPUT_PATH ${PROJECT_BINARY_DIR}/bin)
add_executable(iagent-core ${IAGENTCORE_SOURCE})
target_link_libraries(iagent-core pthread)# LIBPLATFORM libcoap )

