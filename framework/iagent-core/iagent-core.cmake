include(${CMAKE_CURRENT_LIST_DIR}/platforms/linux/ic_platform.cmake)
include(${CMAKE_CURRENT_LIST_DIR}/resources/ic_resources.cmake)
include(${CMAKE_CURRENT_LIST_DIR}/src/devices/ic_devices.cmake)
include(${CMAKE_CURRENT_LIST_DIR}/src/ic_src.cmake)

set(IAGENT_CORE_DIR ${CMAKE_CURRENT_LIST_DIR})

set(IAGENT_CORE_SOURCE
    ${IC_PLATFORM_SOURCE}
    ${IC_RESOURCES_SOURCE}
    ${IC_DEVICES_SOURCE}
    ${IC_SRC_SOURCE}
)

include_directories(
    ${IAGENT_CORE_DIR}
)