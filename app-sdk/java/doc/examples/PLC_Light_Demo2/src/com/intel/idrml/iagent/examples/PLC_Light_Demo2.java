/**********************************************************************************
 * SCENARIO: 
 * The device connected to gateway is special, user need to a self defined 
 * parser for resource data to control the special device
 * 
 * SOLUTION: 
 * step 1. add a subclass of ResourceDataGeneral
 * step 2. add a self define parser by API IAgentManager.addPayloadParser()
 * step 3. get device and resource info by query API IAgentManager.DoDeviceQuery()
 * step 4. get resource info by API IAgentManager.DoResourceGET() which will use self defined parser to parse data
 * step 5. control the light according to the suer configure property "interval" and "algorithm" of this device
 *         algorithm=1: on-off-on-off...  
 * 
 ***************************************/
package com.intel.idrml.iagent.examples;

import java.util.List;

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.model.DeviceInfo;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataOCF;
import com.intel.idrml.iagent.model.ResourcePropertyData;
import com.intel.idrml.iagent.utilities.LogUtil;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;

public class PLC_Light_Demo2 {
	private static final int TEST_DEVICE_INDEX = 0;
	private static final String TEST_RESOURCE_URI = "/light-switch";
    private static final String TEST_PROPERTY_NAME = "on-off/0";
	private static final int INTERVAL_DEFAULT = 5000;
	private int interval = INTERVAL_DEFAULT;
	private static final int ALGORITHM_DEFAULT = 1;
	private int algorithm = ALGORITHM_DEFAULT; // 1. on off ...;   2. 10*on off ----  3. 10*off on ------
	private static final String KEY_PROP_INTERVAL = "interval";
	private static final String KEY_PROP_ALG = "algorithm";

	public static void main(String[] args) {
		PLC_Light_Demo2 app = new PLC_Light_Demo2();
		app.run();
	}

	private void run() {
		int index=0;
		while(index++<2000){
			RDQueryParam queryParam=new RDQueryParam();
			queryParam.standardType=DeviceInfo.StandardType.modbus.toString();
			
			//step 1. get device and resource info by query API IAgentManager.DoDeviceQuery()
			List<DeviceInfo> modbusDevices = IAgentManager.getInstance().DoDeviceQuery(queryParam);

			Integer intValue = 0;
			if(modbusDevices!=null && modbusDevices.size()>0){
				if(modbusDevices.get(TEST_DEVICE_INDEX).getResources()!=null && modbusDevices.get(TEST_DEVICE_INDEX).getResources().size()>0){
					interval = getUserCfgProperty(modbusDevices.get(TEST_DEVICE_INDEX), KEY_PROP_INTERVAL, INTERVAL_DEFAULT);
					algorithm = getUserCfgProperty(modbusDevices.get(TEST_DEVICE_INDEX), KEY_PROP_ALG, ALGORITHM_DEFAULT);
					try {
						// step 2. get resource data by API IAgentManager.DoResourceGET() which will use self defined parser to parse data
						//					Resource targetResource = getTargetResourceByIndex(modbusDevices.get(TEST_DEVICE_INDEX), TEST_RESOURCE_INDEX);
						Resource targetResource = getTargetResourceByUri(modbusDevices.get(TEST_DEVICE_INDEX), TEST_RESOURCE_URI);
						if(targetResource==null)
						{
							LogUtil.log("Error: There is no resource on target modbus device: "+TEST_RESOURCE_URI);
							continue;
						}
						ResourceDataGeneral resourceData = IAgentManager.getInstance().DoResourceGET(targetResource);
						String value = ((ResourceDataOCF)resourceData).getPropertyValue(TEST_PROPERTY_NAME).sv;
						intValue = Integer.valueOf(value);
						intValue=(intValue>0)?0:1;
						
						// step 3. control the device as you want to
						IAgentManager.getInstance().DoResourcePropertyPUT(targetResource.getAbsoluteUri()+"/"+TEST_PROPERTY_NAME, intValue.toString());
					} catch (CoapException e) {
						e.printStackTrace();
					}
				}
				else{LogUtil.log("Warning: There is no resouces in modbus device: "+modbusDevices.get(TEST_DEVICE_INDEX)+"!");}
			}
			else{LogUtil.log("Warning: There is no modbus devices in the gateway!");}
			
			int time=interval; 
			if(algorithm==2) time=(intValue>0)?interval*2:interval;
			else if(algorithm==3) time=(intValue>0)?interval:interval*2;
			try {Thread.sleep(time);}  catch (InterruptedException e) { e.printStackTrace();}
		}
	}



	private Resource getTargetResourceByUri(DeviceInfo device, String uri) {
		for(Resource resource:device.getResources())
		{
			if(resource.href.equals(uri))
			{
				return resource;
			}
		}
		return null;
	}
	private int getUserCfgProperty(DeviceInfo device, String key, int defaultValue) {
		if(device.getAttrs()==null) return defaultValue;
    	String value=device.getAttrs().get(key);
		if(value==null) return defaultValue;
    	LogUtil.log("Get user config attribute "+key+" : "+value);
		return Integer.valueOf(value).intValue();
	}

}
