#Copyright (c) Microsoft. All rights reserved.
#Licensed under the MIT license. See LICENSE file in the project root for full license information.

cmake_minimum_required(VERSION 2.8.11)
message("hello service sample...")
project(hello_service)

#set paths

# Set the install prefix
if(NOT DEFINED PLATFORM)
  set(PLATFORM "host")
  message(STATUS "hello world service: PLATFORM  not defined. set it to: \"${PLATFORM}\"")
endif()

if(NOT DEFINED PLUGIN_SDK_DIR)
  set(PLUGIN_SDK_DIR ${CMAKE_CURRENT_LIST_DIR}/../..)
  message(STATUS "hello world service: PLUGIN_SDK_DIR  not defined. set it to: \"${PLUGIN_SDK_DIR}\"")
endif()

set(AZURE_BASE ${PLUGIN_SDK_DIR}/azure_base/${PLATFORM})

include(${PLUGIN_SDK_DIR}/common.cmake)
include(${SHARED_LIBS_DIR}/lib/ssg_lib.cmake)
include(${SHARED_LIBS_DIR}/coap/ex_coap.cmake)


include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/inc
    ${PLUGIN_SDK_DIR}/include
)

set(hello_world_sources
    ${CMAKE_CURRENT_SOURCE_DIR}/src/hello_world.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/app_impl.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/resources.c
    ${CMAKE_CURRENT_SOURCE_DIR}/src/routines.c
)

set(hello_world_headers
    ${CMAKE_CURRENT_SOURCE_DIR}/inc/hello_world.h
)

#this builds the hello_world dynamic library
add_library(hello_world_service MODULE ${hello_world_sources} ${hello_world_headers})
linkSharedUtil(hello_world_service)

