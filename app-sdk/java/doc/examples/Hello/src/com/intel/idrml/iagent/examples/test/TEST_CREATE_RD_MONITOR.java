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
import com.intel.idrml.iagent.framework.OnDataListener;
import com.intel.idrml.iagent.model.DataQueryParam;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.utilities.LogUtil;

public class TEST_CREATE_RD_MONITOR {
	private static final int ERROR_CODE_SUCESS = 0;
	private static final int ERROR_CODE_DEVICES_NUM = 1;
	private static final int ERROR_CODE_DEVICES_NULL = 2;
	private static final int ERROR_CODE_DEVICES_NAME = 3;
	private static final int ERROR_CODE_RESOURCE_NUM = 4;
	private static final int ERROR_CODE_CREATE_MONITOR_FAIL = 5;
	private List<String> deviceIds = Arrays.asList("modbus_tcp1","modbus_tcp2","modbus_tcp_sanity_test");
	private String resourceURI = "/switch/1"; 

	public static void main(String[] args) {
		TEST_CREATE_RD_MONITOR app = new TEST_CREATE_RD_MONITOR();
		app.run();
	}

	private void run() {
	    DataQueryParam query = new DataQueryParam(deviceIds.get(0), resourceURI);
	    try
	    {
		String monitorID = IAgentManager.getInstance().addDataMonitor(query, new OnDataListener(){
		public void onResourceDataChanged(String deviceID, String resouceUri, ResourceDataGeneral data)
		{
		    LogUtil.log("received Data change event: for device: "+deviceID+"   resource: "+resouceUri);
		    System.exit(ERROR_CODE_SUCESS);
		}
		
		});
		if(monitorID==null){
		    LogUtil.log("Error: received null Data Monitor ID!");
		    System.exit(ERROR_CODE_CREATE_MONITOR_FAIL);
		}
	    } catch (CoapException e)
	    {
		e.printStackTrace();
		System.exit(ERROR_CODE_CREATE_MONITOR_FAIL);
	    }
	    
	}
}
