set(SSG_LIB_DIR ${CMAKE_CURRENT_LIST_DIR})

include_directories(${SSG_LIB_DIR}
    ${SSG_LIB_DIR}/iniparser/src
)

set(SSG_LIB_SOURCE
    ${SSG_LIB_DIR}/message_queue.c
    ${SSG_LIB_DIR}/transaction.c
    ${SSG_LIB_DIR}/linux/sync_bsp.c
    ${SSG_LIB_DIR}/logs.c
    ${SSG_LIB_DIR}/url_match.c
    ${SSG_LIB_DIR}/task.c
    ${SSG_LIB_DIR}/path_util.c    
    ${SSG_LIB_DIR}/misc.cpp    
    ${SSG_LIB_DIR}/iniparser/src/iniparser.c
    ${SSG_LIB_DIR}/iniparser/src/dictionary.c
    
)
