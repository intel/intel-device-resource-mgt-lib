#!/bin/bash

SCRIPT=$(readlink -f "$0")
echo SCRIPT=$SCRIPT

build_azure_path=$(cd "$(dirname "$0")/" && pwd)
azure_root=
platform="host"
crosefile=
toolchainfile=
toolchain_def=
install_path=
test_mode=
sdk_only=

usage ()
{
    echo "build.sh [options]"
    echo " -d,  debug bug                 "
    echo " -s,  build plugin sdk only. skip azure sdk build. "
    echo " -p,  <platform>    default:host"
    echo " -t,  <test mode>, [skip]"
    echo " -r,  release <build_path> <result_path> "
    echo " -x   <toolchain file>        Pass CMake a toolchain file for cross-compiling"
    echo " -a,  <azure gateway sdk repo path>"
    echo " -e,  <envirnetn set script>"
    exit 1
}


while getopts "sdx:p:t:a:e:" arg
do
    case $arg in
        p)
        platform=$OPTARG;;
        t)
        test_mode=$OPTARG;;
        a)
        azure_root=$OPTARG;;
        x)
        toolchainfile=$(readlink -f $OPTARG)
        toolchain_def="-DCMAKE_TOOLCHAIN_FILE=$toolchainfile"
        toolchainfile="--toolchain-file $toolchainfile";;
        d)
        buildtype="-DCMAKE_BUILD_TYPE=Debug";;   
        s)
        sdk_only="yes";;   
        e)
        env_source=$OPTARG;;
        h)
        usage;;
        ?)
        echo $arg
        usage;;
    esac
done


echo " "
echo ">>>>>>>>> build_Intel Device Resource Mgt Library <<<<<<<<<<<<<<"
echo "start building the iot-edge and plugin sdk..."
echo "platform = [$platform]"
echo "toolchainfile = [$toolchainfile]"
echo build_azure_path=[$build_azure_path]

if [ -n "$env_source" ]; then
    echo "start sourcing user specified file: $env_source" 
    source "$env_source"
fi

install_path=$build_azure_path/azure_base/$platform
echo "install path = [$install_path]"
mkdir -p $install_path


if [ ! -n "$azure_root" ]; then
    azure_root="$build_azure_path/../external/iot-edge"
fi
echo "azure_root = [$azure_root]"


if [ ! -d "$azure_root"  ]; then
    echo "now trying to extract the iot-edge repo..."
    cd $build_azure_path/../external
    
    if [ ! -f "$build_azure_path/../external/iot-edge.tar.gz" ]; then
        echo "can't find the iot-edge.tar.gz file for extract. now clone it from github.."
        
        git clone -b "2017-08-21" --recursive https://github.com/Azure/iot-edge.git
        
    else
        tar -xzvf iot-edge.tar.gz
        [ $? -eq 0 ] || exit $?
    fi
    
    cd $build_azure_path
    
fi

if [ ! -d "$azure_root" ]; then
    echo -e "\033[41;33m azure gateway sdk repo path [$azure_root] did not exist!\033[0m"
    exit 3
fi

#sdk_only=1

if [ ! -n "$sdk_only" ]; then
	
	rm -rf $azure_root/install-deps
	
	cd "$azure_root"
	./tools/build.sh --config Release -cl -g -cl -ggdb3 -cl O0 --disable-ble-module --disable-native-remote-modules --rebuild-deps  $toolchainfile --use-xplat-uuid
	[ $? -eq 0 ] || exit $?
	
	if [ ! -d "$azure_root/install-deps/lib" ] ; then 
	    echo -e "\033[41;33m build azure lib failed!\033[0m" 
	    cd -
	    exit 2    
	fi    
	if [ ! -d "$azure_root/install-deps/include" ] ; then 
	    echo -e "\033[41;33m build azure include failed!\033[0m" 
	    cd -
	    exit 2    
	fi    
	
	
	rm -rf $install_path/lib
	rm -rf $install_path/include

	cd $azure_root/build
	#cmake $toolchain_def -DCMAKE_INSTALL_PREFIX:PATH=$install_path --build .
	
	make DESTDIR=$install_path install
	mv $install_path/usr/local/* $install_path
	
	cp -fr $azure_root/install-deps/lib/* $install_path/lib/
	cp -fr $azure_root/install-deps/include/* $install_path/include/ 
fi

rm -rf $build_azure_path/build/$platform
mkdir -p $build_azure_path/build/$platform
cd $build_azure_path/build/$platform

echo "toolchain_def=[$toolchain_def]"
cmake $toolchain_def -Dplatform=$platform -B. -H../..   
[ $? -eq 0 ] || exit $?

make
[ $? -eq 0 ] || exit $?

if [ ! -f libplugin_sdk.so ]; then
    pwd
    ls
    echo -e "\033[41;33m build plugin-sdk failed!\033[0m"
    exit 1
fi
cp libplugin_sdk.so $install_path/lib/

echo -e "\033[32m <<<<<<<<<<<<<<<<<<<<<<<<<<<build pass!\033[0m"
