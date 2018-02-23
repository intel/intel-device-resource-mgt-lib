/**********************************************************************************
 * SCENARIO: 
 * User APP need to monitor the resource data(temperature) change and control fans for LWM2M device 
 * 
 * SOLUTION: 
 * step 1. add data change monitor by API IAgentManager.addDataMonitor()
 * step 2. set the number of fans to be opened in listener
 * 
 ***************************************/

import org.eclipse.californium.core.coap.CoAP.ResponseCode;

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.framework.OnDataListener;
import com.intel.idrml.iagent.model.DataQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataLWM2M;
import com.intel.idrml.iagent.model.ResourcePropertyData;
import com.intel.idrml.iagent.utilities.LogUtil;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;

public class AppFilterAbnormalValue implements OnDataListener {

    private static final String TEST_DEVICE_ID_1 = "TEMP-1";
    private static final String TEST_RESOURCE_ID_1 = "/30242/0/0";

    public static void main(String[] args) {
//	LogUtil.level = LogUtil.LEVEL.DEBUG;
//	LogUtil.logCodeInfo = true;
        AppFilterAbnormalValue app = new AppFilterAbnormalValue();
        app.run();
    }

	private String monitor1;
    
    private void run() {
        try {
            // step 1. add data change monitor with value filter
            DataQueryParam queryParam = new DataQueryParam(TEST_DEVICE_ID_1, TEST_RESOURCE_ID_1, 60, 100, true);
            monitor1 =IAgentManager.getInstance().addDataMonitor(queryParam, this);
        } catch (CoapException e) {
            e.printStackTrace();
        }
    }

    public ResponseCode onResourceDataChanged(String deviceID, String resourceUri, ResourceDataGeneral resourceData) {
        if (TEST_RESOURCE_ID_1.equals(resourceUri) || TEST_RESOURCE_ID_1.equals("/"+resourceUri)) {
          if(resourceData.isParsed()) {
              ResourceDataLWM2M propertyTemprature = (ResourceDataLWM2M) (resourceData);
              Float temperature = Float.valueOf(propertyTemprature.items.get(0).v);
              if(temperature>40)
              {
        	  LogUtil.log("[AbnormalValue]  "+deviceID + "/" + resourceUri + (temperature>10?" ":"  ") + String.format("%2.1f", temperature)+"C ......... Dropped");
//            	  temperature=40.0f;
                  return ResponseCode.FORBIDDEN;
              }
              else
              {
        	  LogUtil.log("[AbnormalValue]  "+deviceID + "/" + resourceUri + (temperature>10?" ":"  ") + String.format("%2.1f", temperature)+"C ......... OK");
              }
              ((ResourceDataLWM2M) (resourceData)).items.get(0).v=temperature;
          }
          else
          {
              Float temperature = Float.valueOf(resourceData.getRawPayload());
              if(temperature>40)
              {
        	  LogUtil.log("[AbnormalValue]  "+deviceID + "/" + resourceUri + (temperature>10?" ":"  ") + String.format("%2.1f", temperature)+"C ......... Dropped");
//        	  temperature=40.0f;
//        	  resourceData.setRawPayload(String.valueOf(temperature));
              return ResponseCode.FORBIDDEN;
              }
              else
              {
        	  LogUtil.log("[AbnormalValue]  "+deviceID + "/" + resourceUri + (temperature>10?" ":"  ") + String.format("%2.1f", temperature)+"C ......... OK");
              }
          }
        }
        else
        {
            LogUtil.log("[AbnormalValue]  Invalid reource uri: "+resourceUri);
        }
        
        return ResponseCode.CONTINUE;
    }

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		IAgentManager.getInstance().removeDataMonitor(monitor1);
	}
    
}
