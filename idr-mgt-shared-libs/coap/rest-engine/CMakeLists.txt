cmake_minimum_required(VERSION 2.8.11)

set(ER_COAP ../er-coap)
set(EXTENSION ../extension)

set(REST_ENGINE_SOURCE
	./rest-engine.c
)

set(REST_ENGINE_HEADER
	./rest-engine.h
	./rest-constants.h
)


include_directories(${ER_COAP})
include_directories(${EXTENSION})

add_library(rest-engine ${REST_ENGINE_SOURCE} ${REST_ENGINE_HEADER})
