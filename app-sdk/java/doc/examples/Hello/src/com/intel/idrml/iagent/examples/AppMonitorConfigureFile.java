/**********************************************************************************
 * SCENARIO: 
 * User APP need to monitor the change of its configure file 
 * 
 * SOLUTION: 
 * step 1. specify configure file monitor parameters by API AmsClient.addCheckpoint()
 * step 2. add listener for notification by AmsClient.registerConfigStatus
 * step 3. do action code in listener callback 
 * 
 ***************************************/
package com.intel.idrml.iagent.examples;

import java.util.List;

import com.intel.idrml.iagent.framework.AmsClient;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.framework.OnConfigurationListener;
import com.intel.idrml.iagent.model.Device;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.utilities.LogUtil;

public class AppMonitorConfigureFile {
	private AmsClient amsClient;
	
    public static void main(String[] args) {
    	new AppMonitorConfigureFile().start();
    }

	private void start() 
	{
		amsClient = AmsClient.getInstance(AppMonitorConfigureFile.class.getSimpleName());
		try {
			RDQueryParam queryParam=new RDQueryParam();
			queryParam.standardType=Device.StandardType.modbus.toString();
			
			List<Device> modbusDevices = IAgentManager.getInstance().DoDeviceQuery(queryParam);
			while(modbusDevices==null || modbusDevices.size()==0){
				LogUtil.log("No modbus devices, polling 2 seconds later!");
				Thread.sleep(2000);
				modbusDevices = IAgentManager.getInstance().DoDeviceQuery(queryParam);
			}
			// step 1. specify configure file monitor parameters by API AmsClient.addCheckpoint()
			amsClient.addCheckpoint("device_on_gateway", modbusDevices.get(0).deviceId);
			// step 2. add listener for notification by AmsClient.registerConfigStatus
			amsClient.registerConfigStatus(new OnConfigurationListener(){
				public void onConfigChanged(String configFilePathLocal, String targetType, String targetId) {
					// step 3. do action code in listener callback
					LogUtil.log("Get notification for change of config file: "+AmsClient.getConfigFilePath(configFilePathLocal, targetType, targetId));
				}});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
