#for unit test unmask line 6,22,25,34,42   mask line 40
cmake_minimum_required(VERSION 2.8.11)
message("er-coap...")
project(iagentcore)

#SET(CMAKE_C_FLAGS "--std=gnu99  -pthread ${CMAKE_C_FLAGS}")


SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wno-literal-suffix ")
set (CMAKE_BUILD_TYPE Debug)
#add_definitions (-DLINUX)

include (ex_coap.cmake)
include_directories(
  ${CMAKE_CURRENT_LIST_DIR}/../lib
)

set(ERCOAP_SOURCE
	${EX_COAP_SOURCE}
#	${EXTENSION_DIR}/test_linux.c
)

set(ERCOAP_HEADER )

add_library(coap ${ERCOAP_SOURCE} ${ERCOAP_HEADER})

#add_executable( coap_unit ${ERCOAP_SOURCE} ${ERCOAP_HEADER})

