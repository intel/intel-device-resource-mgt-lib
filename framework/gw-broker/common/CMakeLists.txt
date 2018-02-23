cmake_minimum_required(VERSION 2.8.11)
message("iagentcore...")
project(gwbroker)

#set build configuration
set (CMAKE_C_FLAGS "--std=c99 ${CMAKE_C_FLAGS}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wno-literal-suffix ")
SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g2 -ggdb")  
SET(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")


include_directories(
	${PROJECT_SOURCE_DIR}
	${AZURE_DEPS_CSHARE_DIR}
	${AZURE_MODULE_DIR}/logger/inc
	${AZURE_CORE_DIR}/inc
	${COAP_DIR}/er-coap
	${PROJECT_SOURCE_DIR}/../../../utils)


set(GWBROKER_SOURCE
	./broker_rest_convert.c ./module_common.c)


add_library(gw-broker SHARED ${GWBROKER_SOURCE})
