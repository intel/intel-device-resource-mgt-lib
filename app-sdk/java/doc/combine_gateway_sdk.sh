#!/bin/bash

# 1. get iagent offline package
# sshpass -p "iagent123" scp iot@xin-ubuntu.bj.intel.com:/home/iot/saigang/iagent1.8_offline_host.tar.gz /home/saigon/workspace/gateway_sdk/ssg-sto-crtl-iot-imrt-iot-sdk/install/sdk_target/gateway_packages/iagent1.8_offline_host.tar.gz 
# sshpass -p "iagent123" scp iot@xin-ubuntu.bj.intel.com:/home/iot/saigang/ams_client-host-2.0.sh /home/saigon/workspace/gateway_sdk/ssg-sto-crtl-iot-imrt-iot-sdk/install/sdk_target/gateway_packages/ams_client-host-2.0.sh 

# sshpass -p "iagent123" scp iot@xin-ubuntu.bj.intel.com:/home/iot/iMRT1.333_offline_host.tar.gz /home/saigon/workspace/gateway_sdk/ssg-sto-crtl-iot-imrt-iot-sdk/install/sdk_target/gateway_packages/iMRT1.333_offline_host.tar.gz 

# sshpass -p "iagent123" scp iot@xin-ubuntu.bj.intel.com:/home/iot/com.intel.imrt.bdt_2.0.0.201708241814.jar /home/saigon/workspace/gateway_sdk/ssg-sto-crtl-iot-imrt-iot-sdk/install/sdk_target/gateway_packages/iagent1.8_offline_host.tar.gz 

# sshpass -p "iagent123" scp iot@xin-ubuntu.bj.intel.com:/home/iot/iMRT_sdk_linux_edge_201708241744.tar /home/saigon/workspace/gateway_sdk/ssg-sto-crtl-iot-imrt-iot-sdk/install/sdk_dev/iMRT_sdk_linux_edge_201708241744.tar 

# 2. get iMRT offline package

# 3. get ams offline package

# 4. get BDT offline package

update_samples()
{
    cp -R $1/doc/html/* $2/docs/gateway_sdk/
    cp -R $1/doc/examples/* $2/samples/gateway_app/
    rm -R $2/samples/gateway_app/Hello
    find $2/samples/gateway_app -type d -name 'bin' | xargs rm -r
}

update_source_zip()
{
    zip -r $2/docs/gateway_sdk/gateway_sdk_src.zip $1/code/src/*   &>/dev/null
}

update_source_doc_zip()
{
    mkdir temp_doc
    javadoc -d temp_doc/ -sourcepath $1/code/src com.intel.idrml.iagent  com.intel.idrml.iagent.framework com.intel.idrml.iagent.model com.intel.idrml.iagent.utilities  &>/dev/null
    cd temp_doc
    zip -r $2/docs/gateway_sdk/gateway_sdk_javadoc.zip *
    cd ..
    rm -rf temp_doc
}

# 0. check input parameters
if [ $# -lt 1 ]
  then
    echo "USAGE:   combine_gateway_sdk.sh <basepath_BDT_SDK>"
    echo "         "
    echo "         <basepath_BDT_SDK>:  This path shall contain sub path 'iMRT_sdk_linux_edge' directly"
    exit 1
fi

if [ ! -d "$1/tools" ]; then
    echo "ERROR:        sub path 'iMRT_sdk_linux_edge' is not contained in $1 directly!"
    exit 1
fi

BASE_PATH=`pwd`
if [ ! -d "$BASE_PATH/code" ]; then
    echo "ERROR:   code and doc sub folder is not contained $BASE_PATH"
    echo "         "
    echo "USAGE:   combine_gateway_sdk.sh <basepath_BDT_SDK>"
    echo "         "
    echo "         <basepath_BDT_SDK>:  This path shall contain sub path 'iMRT_sdk_linux_edge' directly"
    exit 1
fi

cd $1 ;
SDK_PATH=`pwd` ;
cd $BASE_PATH ;

# 2.
   mkdir temp 
   cd temp
   unzip $SDK_PATH/tools/mrtc/bin/libcore.jar -d .   &>/dev/null
   cd ..

# 4. 
    rm -rf temp/com/intel/idrml
    cp -R $BASE_PATH/code/bin/com/intel/idrml  temp/com/intel/idrml

# 5. 
    cd temp
    jar -cvf libcore_new.jar .   &>/dev/null
    mv ./libcore_new.jar $SDK_PATH/tools/mrtc/bin/libcore.jar
    $SDK_PATH/tools/mrtc/bin/jeffc -o libcore_new.jeff -cd . -rd . -rt "*"   &>/dev/null
    cd ..
# 6.  
    cp ./temp/libcore_new.jeff $SDK_PATH/tools/mrtc/bin/libcore.jeff
    cp ./temp/libcore_new.jeff $SDK_PATH/vm/libcore.jeff

# 7.  
    rm -rf temp

    echo "         "
    echo "SUCEESS update below files:"
    echo "$SDK_PATH/tools/mrtc/bin/libcore.jar"
    echo "$SDK_PATH/tools/mrtc/bin/libcore.jeff"
    echo "         "

while true; do
    read -p "Do you wish to update samples? yes/no: " yn
    case $yn in
        [Yy]* ) 
            update_samples $BASE_PATH $SDK_PATH ; 
            echo "         "
            echo "SUCEESS update below files:"
            echo "$SDK_PATH/samples/gateway_app/"
            echo "$SDK_PATH/docs/gateway_sdk/"
            echo ""
            break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes/y or no/n.";;
    esac
done

while true; do
    read -p "Do you wish to source.zip? yes/no: " yn
    case $yn in
        [Yy]* ) 
            update_source_zip $BASE_PATH $SDK_PATH ; 
            echo "         "
            echo "SUCEESS update below files:"
            echo "$SDK_PATH/docs/gateway_sdk/gateway_sdk_src.zip"
            echo ""
            break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes/y or no/n.";;
    esac
done

while true; do
    read -p "Do you wish to source_doc.zip? yes/no: " yn
    case $yn in
        [Yy]* ) 
            update_source_doc_zip $BASE_PATH $SDK_PATH &>/dev/null; 
            echo "         "
            echo "SUCEESS update below files:"
            echo "$SDK_PATH/docs/gateway_sdk/gateway_sdk_javadoc.zip"
            echo ""
            break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes/y or no/n.";;
    esac
done


