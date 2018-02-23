/**********************************************************************************
 * SCENARIO: 
 * User APP need to monitor the resource data(temperature) change and control fans for LWM2M device 
 * 
 * SOLUTION: 
 * step 1. add data change monitor by API IAgentManager.addDataMonitor()
 * step 2. set the number of fans to be opened in listener
 * 
 ***************************************/
package com.intel.idrml.iagent.examples;

import java.util.List;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.framework.OnDataListener;
import com.intel.idrml.iagent.model.DataQueryParam;
import com.intel.idrml.iagent.model.DeviceInfo;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataLWM2M;
import com.intel.idrml.iagent.model.ResourcePropertyData;
import com.intel.idrml.iagent.utilities.LogUtil;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;

public class AppMonitorResourceDataForTemperature implements OnDataListener {

    private static final String TEST_DEVICE_ID_1 = "lwm2m_client1";
    private static final String TEST_DEVICE_ID_2 = "lwm2m_client2";
    private static final String TEST_RESOURCE_ID_1 = "/boiler/1";
    private static final String TEST_RESOURCE_ID_2 = "/boiler/1";
    private static final String TEST_ATTRIBUTE_NAME_2 = "on-off/1";

    public static void main(String[] args) {
        AppMonitorResourceDataForTemperature app = new AppMonitorResourceDataForTemperature();
        app.run();
    }

	private DeviceInfo device2;
    
    private void run() {
        try {
            RDQueryParam queryParam2 = new RDQueryParam();
            queryParam2.deviceID = TEST_DEVICE_ID_2;
        	List<DeviceInfo> devices = IAgentManager.getInstance().DoDeviceQuery(queryParam2);
        	if(devices!=null && devices.size()>0)
        	{
        		device2 = devices.get(0);
        		
        		DataQueryParam queryParam = new DataQueryParam(TEST_DEVICE_ID_1, TEST_RESOURCE_ID_1);
        		// step 1. add data change monitor by API IAgentManager.addDataMonitor()
        		IAgentManager.getInstance().addDataMonitor(queryParam, this);
        	}
        	
            
        } catch (CoapException e) {
            e.printStackTrace();
        }
    }

    public ResponseCode onResourceDataChanged(String deviceID, String resourceUri, ResourceDataGeneral resourceData) {
        LogUtil.log("OnResourceData: " + deviceID + "/" + resourceUri + " value: \n" + resourceData);
        if (TEST_RESOURCE_ID_1.equals(resourceUri) || TEST_RESOURCE_ID_1.equals("/"+resourceUri)) {
        	if(resourceData.isParsed()) {
        		ResourceDataLWM2M propertyTemprature = (ResourceDataLWM2M) (resourceData);;
        		Float temperature = Float.valueOf(propertyTemprature.items.get(0).v);
        		int fanNumToOpen = (temperature>200)?4:(temperature>150?3:(temperature>100?2:(temperature>50?1:0)));
        		
        		try {
        			Resource resource = device2.getResource(TEST_RESOURCE_ID_2);
        			ResourcePropertyData property = new ResourcePropertyData();
        			property.propName = TEST_ATTRIBUTE_NAME_2;
        			property.valueType = ResourcePropertyData.ValueType.FLOAT;
        			property.v = fanNumToOpen;
        			// step 2. set the number of fans to be opened in listener
        			if(IAgentManager.getInstance().DoResourcePropertyPUT(resource, MediaTypeFormat.APPLICATION_JSON_LWM2M, property)){
        				LogUtil.log("Successfully open "+fanNumToOpen+" fans!");
        			}
        		} catch (CoapException e) {
        			e.printStackTrace();
        		}
        	}
        }
        return ResponseCode.CONTINUE;
    }
}
