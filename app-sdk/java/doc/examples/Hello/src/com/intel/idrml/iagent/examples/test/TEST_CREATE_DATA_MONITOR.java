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

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.framework.OnRDEventListener;
import com.intel.idrml.iagent.model.Device;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.utilities.LogUtil;

public class TEST_CREATE_DATA_MONITOR {
	private static final int ERROR_CODE_SUCESS = 0;
	private static final int ERROR_CODE_DEVICES_NUM = 1;
	private static final int ERROR_CODE_DEVICES_NULL = 2;
	private static final int ERROR_CODE_DEVICES_NAME = 3;
	private static final int ERROR_CODE_RESOURCE_NUM = 4;
	private static final int ERROR_CODE_CREATE_MONITOR_FAIL = 5;
	private List<String> deviceIds = Arrays.asList("modbus_tcp1","modbus_tcp2","modbus_tcp_sanity_test"); 

	public static void main(String[] args) {
		TEST_CREATE_DATA_MONITOR app = new TEST_CREATE_DATA_MONITOR();
		app.run();
	}

	private void run() {
	    RDQueryParam query = new RDQueryParam();
	    query.standardType = Device.StandardType.modbus.toString();
	    try
	    {
		String monitorID = IAgentManager.getInstance().addRDMonitor(query, new OnRDEventListener(){
		public void onDeviceChanged(List<Device> devices)
		{
		    LogUtil.log("received RD change event: changed devices num = "+devices.size());
		    System.exit(ERROR_CODE_SUCESS);
		}

		public void onResourceChanged(List<Resource> arg0, boolean arg1){
		    System.exit(ERROR_CODE_SUCESS);
		}
		
		});
		if(monitorID==null){
		    LogUtil.log("Error: received null monitor ID!");
		    System.exit(ERROR_CODE_CREATE_MONITOR_FAIL);
		}
	    } catch (CoapException e)
	    {
		e.printStackTrace();
		System.exit(ERROR_CODE_CREATE_MONITOR_FAIL);
	    }
	    
	}
}
