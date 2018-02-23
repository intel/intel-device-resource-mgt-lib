set(EX_AMS_DIR ${CMAKE_CURRENT_LIST_DIR})

if( DEFINED NO_AMS)

  set(EX_AMS_SOURCE    ${EX_AMS_DIR}/ams_path.c ${EX_AMS_DIR}/ams_stub.c)
  message(STATUS "AMS: disabled")

else()

set(EX_AMS_SOURCE
    ${EX_AMS_DIR}/cJSON.c
    ${EX_AMS_DIR}/interface.c
    ${EX_AMS_DIR}/sdk_internal.c
    ${EX_AMS_DIR}/res_config_notify.c
) 

set(EX_PATH_SOURCE
    ${EX_AMS_DIR}/ams_path.c
    ${EX_AMS_DIR}/path.c
) 


endif()


include_directories(
    ${EX_AMS_DIR} 
)
