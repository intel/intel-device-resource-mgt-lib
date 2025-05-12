# PROJECT NOT UNDER ACTIVE MANAGEMENT #  
This project will no longer be maintained by Intel.  
Intel has ceased development and contributions including, but not limited to, maintenance, bug fixes, new releases, or updates, to this project.  
Intel no longer accepts patches to this project.  
 If you have an ongoing need to use this project, are interested in independently developing it, or would like to maintain patches for the open source software community, please create your own fork of this project.  
  
# intel-device-resource-mgt-lib

Setup environment
=================

1. setup the development environment by running the script: 
   sudo ./dev-setup/prepare_dev.sh


Build plugin SDK and samples
========================
1. build plugin SDK:
   cd plugin_sdk
   ./build_pluginsdk.sh

2. go to the folder "plugin-sdk/samples"

3. run the script:
   ./build_samples.sh
   

Build the framework and startup the framework
====================================================
1. goto the folder "framework/gw-broker"

2. run the script:
   ./iagent_build.sh

3. go the out foler for holding the generated binary files, and run:
   ./gw_broker
