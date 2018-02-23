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
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.utilities.LogUtil;

public class TEST_GET_ONE {
	private static final int ERROR_CODE_SUCESS = 0;
	private static final int ERROR_CODE_DEVICES_NUM = 1;
	private static final int ERROR_CODE_DEVICES_NULL = 2;
	private static final int ERROR_CODE_DEVICES_NAME = 3;
	private static final int ERROR_CODE_RESOURCE_NUM = 4;
	private List<String> deviceIds = Arrays.asList("modbus_tcp1","modbus_tcp2","modbus_tcp_sanity_test"); 

	public static void main(String[] args) {
		TEST_GET_ONE app = new TEST_GET_ONE();
		app.run();
	}

	private void run() {
	    RDQueryParam query = new RDQueryParam();
	    query.deviceID = deviceIds.get(0);
	    List<Device> modbusDevices = IAgentManager.getInstance().DoDeviceQuery(query);
	    
	    if(modbusDevices==null || modbusDevices.size()!=1)
	    {
		LogUtil.log("Error: No devices returned from iAgent!");
		System.exit(ERROR_CODE_DEVICES_NUM);
	    }
	    if(!deviceIds.get(0).equals(modbusDevices.get(0).getDeviceId()))
	    { 
		LogUtil.log("Error: devices id is wrong: "+modbusDevices.get(0).getDeviceId());
		System.exit(ERROR_CODE_DEVICES_NAME);
	    }
	    
	    List<Resource> resources = modbusDevices.get(0).getResources();
	    if(resources==null || resources.size()!=1)
	    { 
		LogUtil.log("Error: No resources for deviec: "+modbusDevices.get(0).getDeviceId());
		System.exit(ERROR_CODE_RESOURCE_NUM);
	    }
	    if(!"/switch/1".equals(resources.get(0).href))
	    { 
		LogUtil.log("Error: resources uri is wrong: "+resources.get(0).href);
		System.exit(ERROR_CODE_DEVICES_NAME);
	    }
	    
	    System.exit(ERROR_CODE_SUCESS);
	}
}
