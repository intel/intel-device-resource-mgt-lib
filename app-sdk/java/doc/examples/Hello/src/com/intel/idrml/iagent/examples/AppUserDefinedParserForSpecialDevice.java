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
 * step 5. control the device as you want to
 * 
 ***************************************/
package com.intel.idrml.iagent.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.model.Device;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;
import com.intel.idrml.iagent.utilities.PayloadParser;

public class AppUserDefinedParserForSpecialDevice{
    private static final int TEST_DEVICE_INDEX = 0;
    private static final int TEST_RESOURCE_INDEX = 0;
    
    // step 1. add a subclass of ResourceDataGeneral
    private class ResourceDataModbus extends ResourceDataGeneral{
    	public Map<String, String> properties;
		public ResourceDataModbus(String rawPayload) {
			super(rawPayload);
			properties = new HashMap<String, String>();
		}
		@Override
		public String toJson() {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			String[] keys = properties.keySet().toArray(new String[properties.keySet().size()]);
			for(int i=0; i<keys.length; i++){
				sb.append("\""+keys[i]+"\":\""+properties.get(keys[i])+(i<(keys.length-1)?"\",":"\""));
			}
			sb.append("}");
			return sb.toString();
		}
    }

    public static void main(String[] args) {
        AppUserDefinedParserForSpecialDevice app = new AppUserDefinedParserForSpecialDevice();
        app.run();
    }
    
    private void run() {
        // step 2. add a self define parser by API IAgentManager.addPayloadParser()
    	IAgentManager.getInstance().addPayloadParser(MediaTypeFormat.APPLICATION_JSON, new PayloadParser(){
			public ResourceDataGeneral parse(String text) {
				return parseModbusResourceData(text);
			}});
    	RDQueryParam queryParam=new RDQueryParam();
    	queryParam.standardType=Device.StandardType.modbus.toString();
    	//step 3. get device and resource info by query API IAgentManager.DoDeviceQuery()
		List<Device> modbusDevices = IAgentManager.getInstance().DoDeviceQuery(queryParam);
		
		if(modbusDevices!=null && modbusDevices.size()>0){
			if(modbusDevices.get(TEST_DEVICE_INDEX).getResources()!=null && modbusDevices.get(TEST_DEVICE_INDEX).getResources().size()>0){
				int index=0;
				while(index++<100){
				try {
					// step 4. get resource info by API IAgentManager.DoResourceGET() which will use self defined parser to parse data
					ResourceDataGeneral resourceData = IAgentManager.getInstance().DoResourceGET(modbusDevices.get(TEST_DEVICE_INDEX).getResources().get(TEST_RESOURCE_INDEX));
					if(resourceData.isParsed()){
						Set<String> keys = ((ResourceDataModbus)resourceData).properties.keySet();
						if(keys!=null && keys.size()>0){
							Integer intValue=0;
							for(String key: keys){
								intValue = Integer.valueOf(((ResourceDataModbus)resourceData).properties.get(key));
								intValue=(intValue>0)?0:1;
								((ResourceDataModbus)resourceData).properties.put(key, intValue.toString());
							}
							// step 5. control the device as you want to
							IAgentManager.getInstance().DoResourcePUT(modbusDevices.get(TEST_DEVICE_INDEX).getResources().get(TEST_RESOURCE_INDEX), MediaTypeFormat.APPLICATION_JSON, ((ResourceDataModbus)resourceData).toJson());
							Thread.sleep(1000+index*1000);
						}
					}
				} catch (CoapException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				}
			}
		}
    }
    
    protected ResourceDataGeneral parseModbusResourceData(String text) {
		ResourceDataModbus resourceData = new ResourceDataModbus(text);
		resourceData.properties = JSON.parseObject(text, new TypeReference<Map<String, String>>(){});
		resourceData.isParsed = true;
		return resourceData;
	}

}
