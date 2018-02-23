set(IC_DEVICES_DIR ${CMAKE_CURRENT_LIST_DIR})

set(IC_DEVICES_SOURCE
    ${IC_DEVICES_DIR}/cache_resource_value.cpp
    ${IC_DEVICES_DIR}/calibration.cpp
    ${IC_DEVICES_DIR}/CClient.cpp
    ${IC_DEVICES_DIR}/CClientManager.cpp
    ${IC_DEVICES_DIR}/configure.cpp
    ${IC_DEVICES_DIR}/CResource.cpp
    ${IC_DEVICES_DIR}/CRefresherPoint.cpp
    ${IC_DEVICES_DIR}/CResObject.cpp
    ${IC_DEVICES_DIR}/CResProperty.cpp
    ${IC_DEVICES_DIR}/CResRefresher.cpp
    ${IC_DEVICES_DIR}/CResValue.cpp
    ${IC_DEVICES_DIR}/rd.cpp
    ${IC_DEVICES_DIR}/refresher.cpp
)

include_directories(
    ${IC_DEVICES_DIR}
)