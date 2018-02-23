set(SSG_UTILS_DIR ${CMAKE_CURRENT_LIST_DIR})

include_directories(${SSG_UTILS_DIR})

set(SSG_UTILS_SOURCE
    ${SSG_UTILS_DIR}/parson_ext.c
    ${SSG_UTILS_DIR}/string_parser.c
    ${SSG_UTILS_DIR}/url_util.c
)