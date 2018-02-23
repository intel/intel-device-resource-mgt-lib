set(IC_DPCACHE_DIR ${CMAKE_CURRENT_LIST_DIR})

set(IC_DPCACHE_SOURCE
    ${IC_DPCACHE_DIR}/dp_cache_db.c
    ${IC_DPCACHE_DIR}/dp_cache_server.c
)

include_directories(
    ${IC_DPCACHE_DIR}
)
