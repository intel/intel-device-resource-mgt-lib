 /**********************************************************************************
 * SCENARIO: 
 * Test those API between SDK and iagent for special test setup: 
 * 
 * device_info:[
 * {di:modbus_tcp1, reousrce:[/switch/1] }
 * {di:modbus_tcp_sanity_test, reousrce:[/airflow/1, boiler/1, /battery/1] }
 * {di:modbus_tcp2, reousrce:[/monitor] }
 * ]
 * 
 ***************************************/
package com.intel.idrml.iagent.examples.test;

import java.util.Arrays;
import java.util.List;

import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.model.Device;
import com.intel.idrml.iagent.utilities.LogUtil;

public class TEST_GET_ALL {
	private static final int ERROR_CODE_SUCESS = 0;
	private static final int ERROR_CODE_DEVICES_NUM = 1;
	private static final int ERROR_CODE_DEVICES_NULL = 2;
	private static final int ERROR_CODE_DEVICES_NAME = 3;
	private List<String> deviceIds = Arrays.asList("modbus_tcp1","modbus_tcp2","modbus_tcp_sanity_test"); 

	public static void main(String[] args) {
		TEST_GET_ALL app = new TEST_GET_ALL();
		app.run();
	}

	private void run() {
	    List<Device> modbusDevices = IAgentManager.getInstance().getAllDevices();
	    
	    if(modbusDevices==null || modbusDevices.size()!=3)
	    {
		LogUtil.log("Error: No devices returned from iAgent!");
		System.exit(ERROR_CODE_DEVICES_NUM);
	    }
	    
	    for(Device device:modbusDevices)
	    {
		if(device==null) System.exit(ERROR_CODE_DEVICES_NULL);
		
		if(!deviceIds.contains(device.getDeviceId()))
		{
		    LogUtil.log("Error: devices id is wrong: "+device.getDeviceId());
		    System.exit(ERROR_CODE_DEVICES_NAME); 
		}
	    }
	    
	    System.exit(ERROR_CODE_SUCESS);
	}
}
