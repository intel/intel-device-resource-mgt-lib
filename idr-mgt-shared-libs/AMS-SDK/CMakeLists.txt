cmake_minimum_required (VERSION 2.8.3)
message ("ams_client_interface..." )
project (ams_client_interface)
#-std=c++11 
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -g -O0 -Wno-literal-suffix ")
SET(CMAKE_C_FLAGS " ${CMAKE_C_FLAGS} -g -O0 -Wall ")

#if (NOT NO_M32)
#SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -m32")
#SET(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -m32")
#endif (NOT NO_M32)

set (CMAKE_BUILD_TYPE Debug)

add_definitions(-Wall -Wno-pointer-sign)
set (SHARED_LIBS ${CMAKE_CURRENT_SOURCE_DIR}/..)

include(${CMAKE_CURRENT_SOURCE_DIR}/../coap/ex_coap.cmake)

SET(SOURCES
	interface.c
	path.c
	sdk_internal.c
    cJSON.c
    res_config_notify.c
)


include_directories(${PROJECT_SOURCE_DIR} )
include_directories(${PROJECT_SOURCE_DIR}/../lib )

add_library(ams_client_interface ${SOURCES} ${EX_COAP_SOURCE})
target_link_libraries(ams_client_interface -lpthread dl -lm)
