cmake_minimum_required(VERSION 2.8.11)
message("unit_test...")
project(iagentcoretest)

#set build configuration
set(CMAKE_C_FLAGS "-fPIC --std=c99 ${CMAKE_C_FLAGS} -D_GNU_SOURCE -Wl,--no-undefined -Wl,--no-allow-shlib-undefined")
set(CMAKE_CXX_FLAGS "-fPIC ${CMAKE_CXX_FLAGS} -std=c++11 -Wno-literal-suffix -Wl,--no-undefined -Wl,--no-allow-shlib-undefined")

if (NOT CMAKE_BUILD_TYPE)
SET(CMAKE_BUILD_TYPE Debug)
endif (NOT CMAKE_BUILD_TYPE)

SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g2 -ggdb")  
SET(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")


set(COAP_DIR ${CMAKE_CURRENT_LIST_DIR}/..)
set(ER-COAP ${COAP_DIR}/er-coap)
set(EXTENSION ${COAP_DIR}/extension)
set(REST-ENGINE ${COAP_DIR}/rest-engine)

include_directories(
  ${ER-COAP}
  ${EXTENSION}
  ${REST-ENGINE}
  ${CMAKE_CURRENT_LIST_DIR}/../../../lib
)

set(TEST_SOURCE
  ${EXTENSION}/test_linux.c
  ${ER-COAP}/er-coap.c
  ${EXTENSION}/coap_request.c
  ${EXTENSION}/list.c
  ${EXTENSION}/coap_platforms.c
  ${EXTENSION}/blocking_coap_request.c
  ${ER-COAP}/er-coap-transactions.c
)

add_executable(test_linux ${TEST_SOURCE})
target_link_libraries(test_linux pthread)

