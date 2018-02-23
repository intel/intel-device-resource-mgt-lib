cmake_minimum_required(VERSION 2.8.11)

#set build configuration
set(CMAKE_C_FLAGS "-fPIC --std=c99 ${CMAKE_C_FLAGS} -D_GNU_SOURCE -Wl,--no-undefined -Wl,--no-allow-shlib-undefined")
set(CMAKE_CXX_FLAGS "-fPIC ${CMAKE_CXX_FLAGS} -std=c++11 -Wno-literal-suffix -Wl,--no-undefined -Wl,--no-allow-shlib-undefined")

if (NOT CMAKE_BUILD_TYPE)
SET(CMAKE_BUILD_TYPE Debug)
endif (NOT CMAKE_BUILD_TYPE)

SET(CMAKE_CXX_FLAGS_DEBUG "$ENV{CXXFLAGS} -O0 -Wall -g2 -ggdb")  
SET(CMAKE_CXX_FLAGS_RELEASE "$ENV{CXXFLAGS} -O3 -Wall")

set(WAKAMA_DIR ${PROJECT_SOURCE_DIR}/../../../../external/wakaama)
set(IMRT_SHARED_LIBS_DIR ${PROJECT_SOURCE_DIR}/../../..)
set(IAGENT_CORE_DIR ${PROJECT_SOURCE_DIR}/../../../../apps/iagent-core)
set(AZURE_DIR ${PROJECT_SOURCE_DIR}/../../../../external/iot-edge)

# WAKAAMA
include(${WAKAMA_DIR}/core/wakaama.cmake)
include(${WAKAMA_DIR}/examples/shared/shared.cmake)
include(${IMRT_SHARED_LIBS_DIR}/lib/ssg_lib.cmake)
add_definitions(-DLWM2M_LITTLE_ENDIAN -DCOAP_DEBUG)

include_directories(
	${WAKAMA_DIR}/core
	${WAKAMA_DIR}/examples/shared
	${IAGENT_CORE_DIR}
	${IMRT_SHARED_LIBS_DIR}/coap/er-coap
	${IMRT_SHARED_LIBS_DIR}/coap/rest-engine
	${IMRT_SHARED_LIBS_DIR}/coap/extension
)

set(COAP_CLIENT_UNIT_SOURCE
	${IMRT_SHARED_LIBS_DIR}/coap/er-coap/
	${IMRT_SHARED_LIBS_DIR}/coap/er-coap/er-coap.c
    ${IMRT_SHARED_LIBS_DIR}/coap/er-coap/er-coap-engine.c
    ${IMRT_SHARED_LIBS_DIR}/coap/er-coap/er-coap-transactions.c
    ${IMRT_SHARED_LIBS_DIR}/coap/er-coap/er-coap-res-well-known-core.c
    ${IMRT_SHARED_LIBS_DIR}/coap/extension/list.c
    ${IMRT_SHARED_LIBS_DIR}/coap/extension/coap_request.c
    ${IMRT_SHARED_LIBS_DIR}/coap/extension/coap_platforms.c
    ${IMRT_SHARED_LIBS_DIR}/coap/extension/coap_partial_parse.c
    ${IMRT_SHARED_LIBS_DIR}/coap/extension/coap_over_tcp.c
    ${IMRT_SHARED_LIBS_DIR}/coap/extension/request_handler.c
    ${IMRT_SHARED_LIBS_DIR}/coap/rest-engine/rest-engine.c
    ${IAGENT_CORE_DIR}/resources/version.c
	${WAKAMA_DIR}/examples/shared/connection.c
 	${PROJECT_SOURCE_DIR}/coap_client_unit.c
)

add_executable(coap_client_unit ${COAP_CLIENT_UNIT_SOURCE} ${SSG_LIB_SOURCE})
target_link_libraries(coap_client_unit pthread)
