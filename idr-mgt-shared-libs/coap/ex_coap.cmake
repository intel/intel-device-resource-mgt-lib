set(EX_COAP_DIR ${CMAKE_CURRENT_LIST_DIR})

set(EX_COAP_SOURCE
    ${EX_COAP_DIR}/er-coap/er-coap.c
    ${EX_COAP_DIR}/er-coap/er-coap-engine.c
    ${EX_COAP_DIR}/er-coap/er-coap-transactions.c
    ${EX_COAP_DIR}/er-coap/er-coap-res-well-known-core.c
    ${EX_COAP_DIR}/extension/list.c
    ${EX_COAP_DIR}/extension/coap_request.c
    ${EX_COAP_DIR}/extension/coap_platforms.c
    ${EX_COAP_DIR}/extension/coap_partial_parse.c
    ${EX_COAP_DIR}/extension/coap_over_tcp.c
    ${EX_COAP_DIR}/extension/request_handler.c
    ${EX_COAP_DIR}/extension/blocking_coap_request.c
    ${EX_COAP_DIR}/rest-engine/rest-engine.c
)

include_directories(
    ${EX_COAP_DIR}/rest-engine 
    ${EX_COAP_DIR}/extension 
    ${EX_COAP_DIR}/er-coap    
)