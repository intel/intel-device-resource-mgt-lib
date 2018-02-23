set(IC_RESOURCES_DIR ${CMAKE_CURRENT_LIST_DIR})

set(IC_RESOURCES_SOURCE
    ${IC_RESOURCES_DIR}/version.c
    ${IC_RESOURCES_DIR}/res_rd.c
    ${IC_RESOURCES_DIR}/ilink.c
    ${IC_RESOURCES_DIR}/reset.c
    ${IC_RESOURCES_DIR}/v2_ibroker.c
    ${IC_RESOURCES_DIR}/v2_dp.c
    ${IC_RESOURCES_DIR}/v2_rd_monitor.c
    ${IC_RESOURCES_DIR}/v2_rd.c
    ${IC_RESOURCES_DIR}/v2_refresher.c
    ${IC_RESOURCES_DIR}/v2_ams_config.c
    ${IC_RESOURCES_DIR}/v2_ilink.c
    ${IC_RESOURCES_DIR}/v2_sys_resources.c

)

include_directories(
    ${IC_RESOURCES_DIR}
)