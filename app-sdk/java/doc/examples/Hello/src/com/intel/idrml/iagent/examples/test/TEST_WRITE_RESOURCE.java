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
import com.intel.idrml.iagent.model.Device;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataOCF;
import com.intel.idrml.iagent.utilities.LogUtil;

public class TEST_WRITE_RESOURCE {
	private static final int ERROR_CODE_SUCESS = 0;
	private static final int ERROR_CODE_DEVICES_NUM = 1;
	private static final int ERROR_CODE_DEVICES_NULL = 2;
	private static final int ERROR_CODE_DEVICES_NAME = 3;
	private static final int ERROR_CODE_RESOURCE_NUM = 4;
	private static final int ERROR_CODE_CREATE_MONITOR_FAIL = 5;
	private static final int ERROR_CODE_GET_RESOURCE_FAIL = 6;
	private static final int ERROR_CODE_RESOURCE_DATA_FORMAT = 7;
	private static final String TEST_PROPERTY_NAME = "";
	private List<String> deviceIds = Arrays.asList("modbus_tcp1","modbus_tcp2","modbus_tcp_sanity_test");
	private String testResource = "/switch/1"; 

	public static void main(String[] args) {
		TEST_WRITE_RESOURCE app = new TEST_WRITE_RESOURCE();
		app.run();
	}

	private void run() {
	    RDQueryParam query = new RDQueryParam();
	    query.standardType = Device.StandardType.modbus.toString();
	    try
	    {
		if(IAgentManager.getInstance().getAllDevices()==null || IAgentManager.getInstance().getAllDevices().size()==0) System.exit(ERROR_CODE_DEVICES_NUM);
		Resource targetResource = IAgentManager.getInstance().getAllDevices().get(0).getResource(testResource);
		ResourceDataGeneral resourceData = IAgentManager.getInstance().DoResourceGET(targetResource);
		if(resourceData==null || !resourceData.isParsed())
		{
		    LogUtil.log("Get resource data with wrong format!");
		    System.exit(ERROR_CODE_RESOURCE_DATA_FORMAT);
		}

		String value = ((ResourceDataOCF)resourceData).getPropertyValue(TEST_PROPERTY_NAME).sv;
		Integer intValue = Integer.valueOf(value);
		intValue=(intValue>0)?0:1;
		IAgentManager.getInstance().DoResourcePropertyPUT(targetResource.getAbsoluteUri()+"/"+TEST_PROPERTY_NAME, intValue.toString());
		
		System.exit(ERROR_CODE_SUCESS);
	    } catch (CoapException e)
	    {
		e.printStackTrace();
		System.exit(ERROR_CODE_GET_RESOURCE_FAIL);
	    }
	}
}
