#!/bin/bash
gw_broker=$(cd "$(dirname "$0")/" && pwd)
toolchain=
module_name="all"
build_config="Debug"
build_azure=0
buildtype=
cleanall=0
platform=host
tc_param=
build_path=
bin_path=
azure_lib_path=
env_source=
builtin_ibroker_def=

echo gw_broker= [$gw_broker]

usage ()
{
    echo "iagent_build.sh [options]"
    echo " -s   rebult azure base library and plugin sdk on this PC"
    echo " -d,  debug                 "
    echo " -c,  clean                   "
    echo " -b,  enable the builtin iBroker for cloud connection"
    echo " -j,  disable the AMS"
    echo " -p,  <platform>    platform name, default:host"
    echo " -m,  <module>      default:all. support modules list :iagent, modbus_server, lwm2m_server, database_server"
    echo " -r,  <path>        target package path"
    echo " -o   <options>     c: open coap debug"    
    echo " -e   <path>        environment script for source the environment configurations"
    echo " -x   <toolchain-file>        Pass CMake a toolchain file for cross-compiling"
    exit 1
}

while getopts "jbscdx:p:m:a:r:e:" arg
do
    case $arg in
        p)
        platform=$OPTARG;;
        s)
        build_azure=1;;
        b)
        builtin_ibroker_def="-DBUILTIN_IBROKER=1";;
        j)
        disable_ams_def="-DNO_AMS=1";;
        c)
        cleanall=1;;
        m)
        module_name=$OPTARG;;
        r)
        result_path=$OPTARG;;
        x)
        toolchainfile=$(readlink -f $OPTARG);;
        d)
        result_path=$gw_broker/debug/iagent/product
        buildtype=1;;
        # buildtype="-DCMAKE_BUILD_TYPE=Debug";;   
        e)
        env_source=$OPTARG;;
        h)
        usage;;
        ?)
        echo $arg
        usage;;
    esac
done



check_modules()
{
    while  [ "$module_name" != "all" ] && [ "$module_name" != "main" ] && [ "$module_name" != "iagent" ] && [ "$module_name" != "modbus_server" ] && [ "$module_name" != "lwm2m_server" ] && [ "$module_name" != "database_server" ]
    do
        read -p "Enter build target module toolchain file [all, main, iagent, modbus_server, lwm2m_server, database_server]:" read_module
        if [ $read_module = "all" ] || [ $read_module = "main" ] || [ $read_module = "iagent" ] || [ $read_module = "modbus_server" ] || [ $read_module = "lwm2m_server" ] || [ $read_module = "database_server" ];then
            module_name=$read_module
        fi
    done
}

mvfiles ()
{
    echo "move target files...to [$result_path]"
    rm -rf $result_path
    mkdir -p $result_path
    mkdir -p $result_path/lib

    cp $gw_broker/start.sh $result_path
    chmod +x $result_path/start.sh

    cp $gw_broker/../../build/package/config/logcfg.ini $result_path/

    cp $bin_path/* $result_path

    cp $azure_lib_path/lib/libplugin_sdk.so $result_path/lib
    if [ -d $azure_lib_path/lib/x86_64-linux-gnu ]; then
        cp $azure_lib_path/lib/x86_64-linux-gnu/libnanomsg.so.5.0.0 $result_path/lib
        cp $azure_lib_path/lib/x86_64-linux-gnu/libaziotsharedutil.so $result_path/lib
        cp $azure_lib_path/lib/x86_64-linux-gnu/azure_iot_gateway_sdk-1.0.6/libgateway.so $result_path/lib
    else
        cp $azure_lib_path/lib/libnanomsg.so.5.0.0 $result_path/lib
        cp $azure_lib_path/lib/libaziotsharedutil.so $result_path/lib
        cp $azure_lib_path/lib/azure_iot_gateway_sdk-1.0.6/libgateway.so $result_path/lib
    fi
}



module_make()
{
    mkdir -p $dst
    cd $dst 
    echo "current dir: "
    pwd
    cmake $src \
          $toolchain \
          $builtin_ibroker_def \
          $disable_ams_def  \
          -DOUTPUT_PATH=$bin_path \
          -DCMAKE_BUILD_TYPE="$build_config" \
          -DAZURE_BASE=$azure_lib_path
    [ $? -eq 0 ] || exit $?

    make
    [ $? -eq 0 ] || exit $?

}

echo " "
echo " "
echo " "
echo -e "\033[32m >>>>>>>>>>>>>>>>>>>>>>>>>>start to build project\033[0m"
check_modules

build_path=$gw_broker/build/$platform
bin_path=$gw_broker/out/$platform



if [ ! -n "$azure_lib_path" ]; then
    azure_lib_path=$gw_broker/../../plugin-sdk/azure_base/$platform
fi


echo "platform       =  [$platform]"
echo "env_source     =  [$env_source]"
echo "toolchainfile  =  [$toolchainfile]"
echo "build_path     =  [$build_path]"
echo "bin_path       =  [$bin_path]"
echo "sdk_lib_path =    [$azure_lib_path]"
echo "result_path    =  [$result_path]"

if [ -n "$builtin_ibroker_def" ]; then
    echo "Builtin IBROKER is enabled in this build"
fi

if [ -n "$env_source" ]; then
    echo "start sourcing user specified file: $env_source" 
    source "$env_source"
fi

echo ""
echo "checking external open source projects.."
$gw_broker/../../external/download.sh
[ $? -eq 0 ] || exit $?

echo " "

mkdir -p $build_path

if [ $cleanall == 1 ]; then
    echo -e "\033[32m clean all\033[0m"
    rm -rf $azure_lib_path
    rm -rf $build_path
    exit 0
fi

if [ ! -d $azure_lib_path ]; then
    build_azure=1
    echo "Plugin SDK base lib doesn't exist. rebuild it"
fi

if [ $build_azure == 1 ];then

    cd $gw_broker/../../plugin-sdk
    pwd
    if [ -n "$toolchainfile" ]; then
         tc_param="-x $toolchainfile"
    fi
    
    cd $gw_broker/../../plugin-sdk
    ./build_plugin_sdk.sh -p $platform $tc_param
    [ $? -eq 0 ] || exit $?

    echo ">>>>>>>>> Plugin SDK build completed"
    cd - 
fi

echo " "
echo ">>>>>>>>> Start to build the modules.."
echo " "

if [ -n "$toolchainfile" ]
then
  toolchain="-DCMAKE_TOOLCHAIN_FILE=$toolchainfile"
fi

if [ $module_name == "all" ]; then    
    # main    
    echo -e "\033[32m start to build gw_broker\033[0m"
    if [ -f "$bin_path/gw_broker" ]; then
        rm $bin_path/gw_broker
    fi
    src=$gw_broker/main 
    dst=$build_path/main
    module_make

   src=$gw_broker/modules
   dst=$build_path
   module_make
                        
elif [ $module_name == "main" ]; then
    echo -e "\033[32m start to build gw_broker\033[0m"
    if [ -f "$bin_path/gw_broker" ]; then
        rm $bin_path/gw_broker
    fi
    src=$gw_broker/main
    dst=$build_path/main
    module_make  
else
    echo -e "\033[32m start to build $module_name\033[0m" 
    if [ -f "$bin_path/lib$module_name.so" ]; then
        rm "$bin_path/lib$module_name.so"
    fi   
    src=$gw_broker/modules/$module_name
    dst=$build_path/$module_name
    module_make
fi

echo ""
echo "List files in the output folder [$bin_path]:"
ls $bin_path

if [  -n "$result_path" ]; then
    mvfiles
fi

echo -e "\033[32m <<<<<<<<<<<<<<<<<<<<<<<<<<<build pass!\033[0m"

