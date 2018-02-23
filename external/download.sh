#!/bin/bash

MY_PATH=`dirname "$0"`
MY_PATH=`( cd "$MY_PATH" && pwd )`
echo MY_PATH = $MY_PATH

SCRIPT=$(readlink -f "$0")
echo SCRIPT=$SCRIPT

external_path=$(cd "$(dirname "$0")/" && pwd)

echo "path is " $external_path
cd $external_path

#########################################################################
#
#    iot-edge
#
#########################################################################

if [ ! -d "iot-edge"  ]; then
    echo "now trying to extract the iot-edge repo..."
    
    if [ ! -f "$external_path/iot-edge.tar.gz" ]; then
        echo "can't find the iot-edge.tar.gz file for extract. now clone it from github.."
        git clone -b "2017-08-21" --recursive https://github.com/Azure/iot-edge.git
        
    else
        tar -xzvf iot-edge.tar.gz
        [ $? -eq 0 ] || exit $?
    fi
    
    #cd $build_azure_path
    #git submodule update --init --recursive
fi

if [ ! -d "iot-edge" ]; then
    echo -e "\033[41;33m repo iot-edge did not exist!\033[0m"
    exit 3
fi


#########################################################################
#
#    libmodbus
#
#########################################################################
if [ ! -d "libmodbus"  ]; then
    echo "now trying to download the libmodbus repo..."
    git clone  --recursive https://github.com/stephane/libmodbus.git
fi

if [ ! -d "libmodbus" ]; then
    echo -e "\033[41;33m repo libmodbus did not exist!\033[0m"
    exit 3
fi


#########################################################################
#
#    wakaama
#
#########################################################################
if [ ! -d "wakaama"  ]; then
    echo "now trying to download the wakaama repo..."
    git clone  --recursive https://github.com/eclipse/wakaama.git
fi

if [ ! -d "wakaama" ]; then
    echo -e "\033[41;33m repo wakaama did not exist!\033[0m"
    exit 3
fi


#########################################################################
#
#    sqlite-amalgamation
#
#########################################################################
if [ ! -d "sqlite-amalgamation"  ]; then
    echo "now trying to download the sqlite-amalgamation repo..."
    git clone  --recursive https://github.com/azadkuh/sqlite-amalgamation.git
fi

if [ ! -d "sqlite-amalgamation" ]; then
    echo -e "\033[41;33m repo sqlite-amalgamation did not exist!\033[0m"
    exit 3
fi


echo ""
echo "All external repos are setup, ready to go"

exit 0
