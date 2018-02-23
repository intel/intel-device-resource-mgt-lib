##############################################
Steps to release SDK
##############################################
1. export a runnable gateway_sdk.jar from eclipse

   cd [your_bdt_path]/iMRT_sdk_linux_edge/

2.
   mkdir temp 
   cd temp
   jar -xf ../tools/mrtc/bin/libcore.jar
   cd ..
3. 
   mkdir temp2
   cd temp2
   jar -xf ../gateway_sdk.jar
   cd ..

4. 
    cp -R temp2/org/eclipse  temp/org/
    cp -R temp2/io  temp/
    cp -R temp2/com/alibaba  temp/com/
    cp -R temp2/com/intel/imrt  temp/com/intel/
5. 
    cd temp
    jar -cvf libcore_new.jar .
    ../tools/mrtc/bin/jeffc -o libcore_new.jeff -jar libcore_new.jar
    cd ..
6.  
    rm ./tools/mrtc/bin/libcore.jar
    rm ./tools/mrtc/bin/libcore.jeff
    cp ./temp/libcore_new.jar ./tools/mrtc/bin/libcore.jar
    cp ./temp/libcore_new.jeff ./tools/mrtc/bin/libcore.jeff

7.  
    rm -rf temp
    rm -rf temp2


shell script
----------------------------------------------------

#!/bin/bash

# 0. check input parameters
if [ $# -lt 2 ]
  then
    echo "usage: combine_gateway_sdk.sh  <full_path_gatway_sdk_jar>  <basepath_BDT_SDK>"
    echo "<full_path_gatway_sdk_jar>:  This jar file is exported from eclipse"
    echo "<basepath_BDT_SDK>:  This path shall contain sub path 'iMRT_sdk_linux_edge' directly"
    exit 1
fi

# 1.
cd $2/iMRT_sdk_linux_edge/

# 2.
   mkdir temp 
   cd temp
   jar -xf ../tools/mrtc/bin/libcore.jar
   cd ..
# 3. 
   mkdir temp2
   cd temp2
   jar -xf ../gateway_sdk.jar
   cd ..

# 4. 
    cp -R temp2/org/eclipse  temp/org/
    cp -R temp2/io  temp/
    cp -R temp2/com/alibaba  temp/com/
    cp -R temp2/com/intel/imrt  temp/com/intel/
# 5. 
    cd temp
    jar -cvf libcore_new.jar .
    ../tools/mrtc/bin/jeffc -o libcore_new.jeff -jar libcore_new.jar
    cd ..
# 6.  
    rm ./tools/mrtc/bin/libcore.jar
    rm ./tools/mrtc/bin/libcore.jeff
    cp ./temp/libcore_new.jar ./tools/mrtc/bin/libcore.jar
    cp ./temp/libcore_new.jeff ./tools/mrtc/bin/libcore.jeff

# 7.  
    rm -rf temp
    rm -rf temp2



##############################################

##############################################



##############################################

##############################################



##############################################

##############################################




