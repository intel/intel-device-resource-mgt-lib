#Copyright (c) Microsoft. All rights reserved.
#Licensed under the MIT license. See LICENSE file in the project root for full license information.

cmake_minimum_required(VERSION 2.8.12)

set(PLUGIN_SDK_DIR ${CMAKE_CURRENT_LIST_DIR})
#--add-needed, --no-undefined --no-allow-shlib-undefined, -Wl, -no-as-needed

set(CMAKE_C_FLAGS "-fPIC --std=c99 ${CMAKE_C_FLAGS} -D_GNU_SOURCE")
set(CMAKE_CXX_FLAGS "-fPIC ${CMAKE_CXX_FLAGS} -std=c++0x -Wno-literal-suffix")

if(NOT DEFINED NO_CHECK_UNDEFINE_SHLIB)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wl,--no-allow-shlib-undefined,--add-needed")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,--no-undefined -Wl,--no-allow-shlib-undefined,--add-needed")
endif()


if (NOT CMAKE_BUILD_TYPE)
SET(CMAKE_BUILD_TYPE Debug)
endif (NOT CMAKE_BUILD_TYPE)

# Set the install prefix
if(NOT DEFINED platform)
  set(platform "host")
  message(STATUS "Plugin lib: platform  not defined. set it to: \"${platform}\"")
endif()

set(AZURE_BASE ${PLUGIN_SDK_DIR}/azure_base/${platform})

# Set the install prefix
if(NOT DEFINED OUTPUT_PATH)
  set(OUTPUT_PATH ${AZURE_BASE})
  message(STATUS "Plugin lib: OUTPUT_PATH not defined. set it to: \"${OUTPUT_PATH}\"")
endif()

#set(EXECUTABLE_OUTPUT_PATH ${OUTPUT_PATH})
#set(LIBRARY_OUTPUT_PATH ${OUTPUT_PATH}/lib)

link_directories(${OUTPUT_PATH}) 

include_directories(${AZURE_BASE}/include/azure_iot_gateway_sdk-1.0.6)
include_directories(${AZURE_BASE}/include/azureiot)
link_directories(${AZURE_BASE}/lib/azure_iot_gateway_sdk-1.0.6)
link_directories(${AZURE_BASE}/lib) 

if("${platform}" STREQUAL "host")
link_directories(${AZURE_BASE}/lib/x86_64-linux-gnu/azure_iot_gateway_sdk-1.0.6)
link_directories(${AZURE_BASE}/lib/x86_64-linux-gnu) 
endif()

message(STATUS "azure lib path: \"${AZURE_BASE}\"")

include_directories(${PLUGIN_SDK_DIR}/include)


include(${PLUGIN_SDK_DIR}/../idr-mgt-shared-libs/lib/ssg_lib.cmake)
include(${PLUGIN_SDK_DIR}/../idr-mgt-shared-libs/AMS-SDK/ex_ams.cmake)

set(plugin_sdk_sources
    ${PLUGIN_SDK_DIR}/src/internals.c
    ${PLUGIN_SDK_DIR}/src/message_coder.c
    ${PLUGIN_SDK_DIR}/src/restful_framework.c
    ${PLUGIN_SDK_DIR}/src/request_service.c
    ${PLUGIN_SDK_DIR}/src/plugin_dlist.c
    ${PLUGIN_SDK_DIR}/src/working_thread.c
    ${SSG_LIB_SOURCE}
    ${EX_PATH_SOURCE}
)

set(plugin_sdk_headers
    ${PLUGIN_SDK_DIR}/include/plugin_sdk.h
)


#this builds the hello_world dynamic library
add_library(plugin_sdk MODULE ${plugin_sdk_sources} ${plugin_sdk_headers})
target_link_libraries(plugin_sdk gateway aziotsharedutil  nanomsg ssl crypto dl pthread rt anl)

#this builds the hello_world static library
#add_library(plugin_sdk_static STATIC ${plugin_sdk_sources} ${plugin_sdk_headers})
#target_compile_definitions(plugin_sdk_static PRIVATE BUILD_MODULE_TYPE_STATIC)
#target_link_libraries(plugin_sdk_static gateway aziotsharedutil nanomsg c pthread)

# linkSharedUtil(plugin_sdk)
# linkSharedUtil(plugin_sdk_static)

# add_module_to_solution(plugin_sdk)

if(install_modules)
    install(TARGETS plugin_sdk LIBRARY DESTINATION "${LIB_INSTALL_DIR}/modules") 
endif()

