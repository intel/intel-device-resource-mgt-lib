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
package com.intel.idrml.iagent.examples.applet;
 
import com.intel.util.IntelApplet;

import java.util.List;

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.model.Device;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataOCF;
import com.intel.idrml.iagent.model.ResourcePropertyData;
import com.intel.idrml.iagent.utilities.LogUtil;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;

public class PLC_Light_Demo1Applet extends IntelApplet
{

	public int onInit(byte[] request)
	{
            PLC_Light_Demo1 app = new PLC_Light_Demo1();
            app.run();    
	    return APPLET_SUCCESS;
        }

	public int onClose()
	{

	    return APPLET_SUCCESS;
	}

	public int invokeCommand(int commandId, byte[] request)
	{
            return APPLET_SUCCESS;
        }


        public class PLC_Light_Demo1 {
	private static final int TEST_DEVICE_INDEX = 0;
	private static final String TEST_RESOURCE_URI = "/light-switch";
    private static final String TEST_PROPERTY_NAME = "on-off/0";
	private static final int INTERVAL_DEFAULT = 1000;
	private int interval = INTERVAL_DEFAULT; 

	

	private void run() {
                System.out.println("App Started ...");
		int index=0;
		while(true){
			RDQueryParam queryParam=new RDQueryParam();
			queryParam.standardType=Device.StandardType.modbus.toString();
			
			//step 1. get device and resource info by query API IAgentManager.DoDeviceQuery()
			List<Device> modbusDevices = IAgentManager.getInstance().DoDeviceQuery(queryParam);

			if(modbusDevices!=null && modbusDevices.size()>0){
				if(modbusDevices.get(TEST_DEVICE_INDEX).getResources()!=null && modbusDevices.get(TEST_DEVICE_INDEX).getResources().size()>0){
					try {
						// step 2. get resource data by API IAgentManager.DoResourceGET() which will use self defined parser to parse data
						Resource targetResource = getTargetResourceByUri(modbusDevices.get(TEST_DEVICE_INDEX), TEST_RESOURCE_URI);
						ResourceDataGeneral resourceData = IAgentManager.getInstance().DoResourceGET(targetResource);
						String value = ((ResourceDataOCF)resourceData).getPropertyValue(TEST_PROPERTY_NAME).sv;
						Integer intValue = Integer.valueOf(value);
						intValue=(intValue>0)?0:1;
						
						// step 3. control the device as you want to
						IAgentManager.getInstance().DoResourcePropertyPUT(targetResource.getAbsoluteUri()+"/"+TEST_PROPERTY_NAME, intValue.toString());
//						IAgentManager.getInstance().DoResourcePUT(targetResource, MediaTypeFormat.APPLICATION_JSON, ((ResourceDataOCF)resourceData).toJson());
					} catch (CoapException e) { e.printStackTrace();}
				}
				else{LogUtil.log("Warning: There is no resouces in modbus device: "+modbusDevices.get(TEST_DEVICE_INDEX)+"!");}
			}
			else{ LogUtil.log("Warning: There is no modbus devices in the gateway!");}
			
			try { Thread.sleep(interval*(Math.abs(index++%20-10)*2+1));} catch (InterruptedException e) { e.printStackTrace();}
		}
	}


	private Resource getTargetResourceByUri(Device device, String uri) {
		for(Resource resource:device.getResources())
		{
			if(resource.href.equals(uri))
			{
				return resource;
			}
		}
		return null;
	}

}
}
