#!/bin/bash

# 0. check input parameters
BASE_PATH=`pwd`
if [ $# -lt 0 ]; then
    $BASE_PATH=`pwd`
    echo "$BASE_PATH"
else
    $BASE_PATH=$1
fi

if [ ! -d "$BASE_PATH/code" ]; then
    echo "ERROR:   code and doc sub folder is not contained in $1"
    echo "         "
    echo "USAGE:   create_gateway_sdk_package.sh  [basepath_code_doc]"
    echo "         "
    echo "         [basepath_code_doc]:  This path shall contain sub path 'code' and 'doc' directly"
    exit 1
fi

# 1. prepare file system
   cd $BASE_PATH/
   mkdir temp 
   cd temp
   mkdir simulator
   mkdir classes
   mkdir docs
   mkdir docs/gateway_sdk
   mkdir samples
   mkdir samples/gateway_app
   cd ..

# 2. copy html and samples
    cp -R doc/html/* temp/docs/gateway_sdk/
    cp -R doc/examples/* temp/samples/gateway_app/
    rm -R temp/samples/gateway_app/Hello
    find temp/samples/gateway_app -type d -name 'bin' | xargs rm -r

# 3. extract classes of 3thrd lib
   mkdir temp2
   cd temp2
   jar -xf $BASE_PATH/code/libs/californium-core.jar
   jar -xf $BASE_PATH/code/libs/fastjson-1.2.9.jar
   cd ..

# 4. create classes.jar
    mkdir temp/classes/org
    cp -R temp2/org/eclipse  temp/classes/org/eclipse
    cp -R temp2/io  temp/classes/io
    mkdir temp/classes/com
    cp -R temp2/com/alibaba  temp/classes/com/alibaba
    mkdir temp/classes/com/intel
    cp -R $BASE_PATH/code/bin/com/intel/idrml  temp/classes/com/intel/idrml
    cd temp/classes
    jar -cvf classes.jar .
    cp classes.jar ../
    cd ../..
    rm -rf temp/classes

# 5. create sdk source zip
    cd $BASE_PATH/code/src
    zip -r $BASE_PATH/temp/docs/gateway_sdk/gateway_sdk_src.zip *
    cd ../..

# 6. create sdk javadoc zip
    mkdir temp3
    javadoc -d temp3/ -sourcepath $BASE_PATH/code/src com.intel.idrml.iagent  com.intel.idrml.iagent.framework com.intel.idrml.iagent.model com.intel.idrml.iagent.utilities
    cd temp3
    zip -r ../temp/docs/gateway_sdk/gateway_sdk_javadoc.zip *
    cd ..

# 7. copy simulator
    cp $BASE_PATH/simulator/lwm2m/target/*.jar  temp/simulator/

# 8. create the final sdk release
    cd temp
    tar -zcvf GATEWAY_SDK.tar.gz *
    cp GATEWAY_SDK.tar.gz ../
    cd ..

# 9. clean env 
    rm -rf temp
    rm -rf temp2
    rm -rf temp3

    echo "         "
    echo "         "
    echo "SUCEESS create :"
    echo "$BASE_PATH/GATEWAY_SDK.tar.gz"

