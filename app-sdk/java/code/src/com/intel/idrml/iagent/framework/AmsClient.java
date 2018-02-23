/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.intel.idrml.iagent.framework;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.intel.runtime.comm.JsonProperty;

//*********************************************************************
//                    DIR Structure as below
//*********************************************************************
///imrt_apps (all iMRT apps are under this dir)
///applets (callback mode apps)
//	/ce9c1124-bab8-4a52-bbe2-41ffa69a5067/ (app root)
//   	/config (configuration root of this app)
//   	/ce9c1124-bab8-4a52-bbe2-41ffa69a5067.bpk
///apps (main entry mode apps)
//	/500c30ad-be16-4b41-a1dd-b58ca62569e9/ (app root)
//   	/config (configuration root of this app)
//   	/500c30ad-be16-4b41-a1dd-b58ca62569e9.bpk
///imrt_lib (imrt libs)  	
//    /lib_core/ (iMRT core library)
//        /v1_0/
//        /v1_1/
//    /californium/ (third party lib)
//        /v1_0/
//        /v1_1/


/**
 * The AmsClient class is a collection of methods to manage configuration file.<br><br>
 * 
 * In one APP, there is one AmsClient instance. AmsClient.getInstance() can be used to get the singleton instance;  
 * Any action to create a new instance by "new" is NOT allowed.
 * 
 * @author saigon
 * @version 1.0
 * @since Feb 2017
 */
public class AmsClient{
	private static AmsClient instance;
	private String productName;
	private ApplicationConfigeUtility appConfigUtility;

	private AmsClient(String productName)
	{
		this.productName = productName;
        appConfigUtility = ApplicationConfigeUtility.getInstance(productName);
	}
	
    /**
     * Method to download the configuration information of devices, groups or resources.
     * This method is synchronous and the configuration information will be download and stored locally
     * in file format. 
     * 
     * @param targetType The configuration of which to be download and monitor, including global, device, group, resource and so on.
     * @param targetId This parameter has different meaning when targetType is different. For example, 
     * targetId will be device ID when targetType isDEVICE_ID, while it will be resource URI if targetType is RESOURCE_META.
     * @return 0: success , -1: fail
     */
	public int addCheckpoint(String targetType, String targetId){
		try {
			appConfigUtility.checkPoint(AmsRequestAction.ADD, targetType, targetId);
			return 0;
		} catch (CoapException e) {
			e.printStackTrace();
			return -1;
		}
	};
	
    /**
     * Method to delete the download action for the configuration information of devices, groups or resources.
     * 
     * @param targetType The configuration of which to be download and monitor, including global, device, group, resource and so on.
     * @param targetId This parameter has different meaning when targetType is different. For example, 
     * targetId will be device ID when targetType isDEVICE_ID, while it will be resource URI if targetType is RESOURCE_META.
     * @return 0: success , -1: fail
     */
	public int deleteCheckpoint(String targetType, String targetId){
		try {
			appConfigUtility.checkPoint(AmsRequestAction.DELETE, targetType, targetId);
			return 0;
		} catch (CoapException e) {
			e.printStackTrace();
			return -1;
		}
	};
	
    /**
     * Method to monitor the change of configuration information of devices, groups or resources.
     * This method is asynchronous and the configuration information will be download and stored locally
     * in file format. APP shall try to read and parse the configuration file when callback eventHandler is called. 
     * 
     * @param eventHandler This parameter is event handler when configuration information has been download locally from server. 
     * @return 0: success , -1: fail
     * 
     * @see com.intel.idrml.iagent.framework.OnConfigurationListener
     */
	public String registerConfigStatus(OnConfigurationListener eventHandler){
		try {
			return appConfigUtility.addWatchPoint(eventHandler);
		} catch (CoapException e) {
			e.printStackTrace();
			return null;
		}
	};
	
    /**
     * Method to delete monitor for the change of configuration information of devices, groups or resources.
     * This method is synchronous.
     *  
     * @return 0: success , -1: fail
     * 
     * @see com.intel.idrml.iagent.framework.OnConfigurationListener
     */
	public int deregisterConfigStatus(){
		try {
			appConfigUtility.deleteWatchPoint();
			return 0;
		} catch (CoapException e) {
			e.printStackTrace();
			return -1;
		}
	};
	
    /**
     * Method to change the product ID which will be used for APP upgrade.
     * 
     * @param productId ID of the product. 
     * @return 0: success , -1: fail
     */
	public int setProductID(String productId){
		try {
			appConfigUtility.setProductID(productName, productId);
			return 0;
		} catch (CoapException e) {
			e.printStackTrace();
			return -1;
		}
	};
	
    /**
     * Method to compose the full path of the configure file, this is static method as a utility for handling configuration file.
     * This API shall work with OnConfigurationListener, it means that it will be used in callback of OnConfigurationListener since 
     * all parameters come from OnConfigurationListener.
     * 
     * @param configFilePathLocal Local configure file name on the gateway. 
     * @param targetType The same as that in API addCheckpoint.
     * @param targetId The same as that in API addCheckpoint. 
     * @return 0: success , -1: fail
     * 
     * @see com.intel.idrml.iagent.framework.OnConfigurationListener
     */
	public static String getConfigFilePath(String configFilePathLocal, String targetType, String targetId){
		String appPathBase = System.getenv("BPK_CONFIG_PATH");
		String appName = JsonProperty.getCurrentAppletPropertyValue("app_name").toString();
		String appType = JsonProperty.getCurrentAppletPropertyValue("app_type").toString();
		appName=appName.replaceAll("\"", "");
		appType=appType.replaceAll("\"", "");
		appPathBase=appPathBase.endsWith("/")?appPathBase:(appPathBase+"/");
		appPathBase+=appType.equals("main")?"apps":"callback";
		return appPathBase+"/"+appName+"/"+"config"+"/"+targetType+"/"+targetId+"/"+configFilePathLocal;
	}

	public static AmsClient getInstance(String appName) {
		if(instance==null)
		{
			instance = new AmsClient(appName);
		}
		else
		{
			instance.setProductName(appName);
		}
		
		return instance;
	}

	private void setProductName(String productName) {
		this.productName = productName;
		this.appConfigUtility.setAppName(productName);
	};
}

