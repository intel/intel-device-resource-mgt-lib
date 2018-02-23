set(IC_SRC_DIR ${CMAKE_CURRENT_LIST_DIR})

set(IC_SRC_SOURCE
    ${IC_SRC_DIR}/handshake.c
    ${IC_SRC_DIR}/ilink_frame.c
    ${IC_SRC_DIR}/ilink_handler.c
    ${IC_SRC_DIR}/provision.c
    ${IC_SRC_DIR}/variable_header.c
    ${IC_SRC_DIR}/ping.c
    ${IC_SRC_DIR}/iagent_core.c
    
)

include_directories(
    ${IC_SRC_DIR}
)
