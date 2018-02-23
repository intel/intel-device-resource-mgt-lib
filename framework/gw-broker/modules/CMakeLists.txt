cmake_minimum_required(VERSION 2.8.11)
#this is CMakeLists for the modules

set(GW_MODULES_DIR ${CMAKE_CURRENT_LIST_DIR} CACHE INTERNAL "Gateway Modules include directory" FORCE)


function(linkGwModule gwmoduleName)
    include_directories(${GWMODULES_DIR}/${gwmoduleName}/inc)
endfunction(linkGwModule)

include_directories(${GW_MODULES_DIR}/../common)

add_subdirectory(iagent)
add_subdirectory(lwm2m_server)
#add_subdirectory(modbus_server)
add_subdirectory(database_server)
#add_subdirectory(logger)
add_subdirectory(proxy)
#add_subdirectory(siemens-s7)
