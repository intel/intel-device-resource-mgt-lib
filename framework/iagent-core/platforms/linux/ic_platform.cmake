set(IC_PLATFORM_DIR ${CMAKE_CURRENT_LIST_DIR})

set(IC_PLATFORM_SOURCE
    ${IC_PLATFORM_DIR}/coap_endpoint.c
    ${IC_PLATFORM_DIR}/coap_server.c
    ${IC_PLATFORM_DIR}/ibroker_send.c
    ${IC_PLATFORM_DIR}/ports_handler.c
    ${IC_PLATFORM_DIR}/sub_agent_handler.c
    ${IC_PLATFORM_DIR}/sub_agent_sender.c
    ${IC_PLATFORM_DIR}/ibroker_gwbus.c
)

include_directories(
    ${IC_PLATFORM_DIR}
)