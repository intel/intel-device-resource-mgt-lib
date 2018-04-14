#Copyright (c) Microsoft. All rights reserved.
#Licensed under the MIT license. See LICENSE file in the project root for full license information.

cmake_minimum_required(VERSION 2.8.12)


set(CMAKE_C_FLAGS "-fPIC --std=c99 ${CMAKE_C_FLAGS} -D_GNU_SOURCE")
set(CMAKE_CXX_FLAGS "-fPIC ${CMAKE_CXX_FLAGS} -std=c++0x -Wno-literal-suffix")

set(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g2 -ggdb")  
set(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")

if(NOT DEFINED NO_CHECK_UNDEFINE_SHLIB)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wl,--no-allow-shlib-undefined,--copy-dt-needed-entries")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,--no-undefined -Wl,--no-allow-shlib-undefined,--copy-dt-needed-entries")
endif()
#--add-needed

SET(PLUGIN_SDK_DIR ${CMAKE_CURRENT_LIST_DIR})
SET(SHARED_LIBS_DIR ${CMAKE_CURRENT_LIST_DIR}/../idr-mgt-shared-libs)


if (NOT CMAKE_BUILD_TYPE)
SET(CMAKE_BUILD_TYPE Debug)
endif (NOT CMAKE_BUILD_TYPE)

if(NOT DEFINED platform)
  set(platform "host")
  message(STATUS "common.cmake: platform  not defined. set it to: \"${platform}\"")
else()
  message(STATUS "common.cmake: platform is [${platform}]")
endif()

# Set the install prefix
if(NOT DEFINED AZURE_BASE)
  set(AZURE_BASE ${PLUGIN_SDK_DIR}/azure_base/${platform})
  message(STATUS "common.cmake: AZURE_BASE not defined. set it to: \"${AZURE_BASE}\"")
endif()

# Set the install prefix
if(NOT DEFINED OUTPUT_PATH)
  set(OUTPUT_PATH ${AZURE_BASE}/lib)
  message(STATUS "common.cmake: OUTPUT_PATH not defined. set it to: \"${OUTPUT_PATH}\"")
endif()


if (ON_M32)
message (${ON_M32})
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -m32")
SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -m32")
LINK_DIRECTORIES(/usr/lib32 /lib32)
endif (ON_M32)

add_definitions(-DRUN_ON_LINUX -DRUN_AS_BROKER_MODULE)

set(EXECUTABLE_OUTPUT_PATH ${OUTPUT_PATH})
set(LIBRARY_OUTPUT_PATH ${OUTPUT_PATH})
link_directories(${OUTPUT_PATH}) 


include_directories(${AZURE_BASE}/include)
include_directories(${PLUGIN_SDK_DIR}/include)
include_directories(${AZURE_BASE}/include/azure_iot_gateway_sdk-1.0.6)
include_directories(${AZURE_BASE}/include/azureiot)

link_directories(${AZURE_BASE}/lib)
link_directories(${AZURE_BASE}/lib/azure_iot_gateway_sdk-1.0.6)
#if("${platform}" STREQUAL "host")
link_directories(${AZURE_BASE}/lib/x86_64-linux-gnu/azure_iot_gateway_sdk-1.0.6)
link_directories(${AZURE_BASE}/lib/x86_64-linux-gnu) 
#endif()

include(${CMAKE_CURRENT_LIST_DIR}/../idr-mgt-shared-libs/lib/ssg_lib.cmake)

function(linkSharedUtil whatIsBuilding)
  target_link_libraries(${whatIsBuilding} plugin_sdk gateway aziotsharedutil nanomsg dl)
endfunction(linkSharedUtil)
