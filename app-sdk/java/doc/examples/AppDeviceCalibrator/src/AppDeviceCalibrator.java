/**********************************************************************************
 * SCENARIO: 
 * User APP need to calibrate the temperature of the device 
 * 
 * SOLUTION: 
 * step 1. add data change monitor by API IAgentManager.addDataMonitor()
 * step 2. set the number of as the calibrator algorithm
 * 
 ***************************************/

import org.eclipse.californium.core.coap.CoAP.ResponseCode;

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.framework.OnDataListener;
import com.intel.idrml.iagent.model.DataQueryParam;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataLWM2M;
import com.intel.idrml.iagent.utilities.LogUtil;


public class AppDeviceCalibrator implements OnDataListener {

    private static final String TEST_DEVICE_ID_1 = "TEMP-1";
    private static final String TEST_RESOURCE_ID_1 = "/30242/0/0";

    public static void main(String[] args) {
//	LogUtil.level = LogUtil.LEVEL.DEBUG;  // open this comments if detailed log in SDK is needed
//	LogUtil.logCodeInfo = true;
    	AppDeviceCalibrator app = new AppDeviceCalibrator();
        app.run();
    }

	private String monitor1;
    
    private void run() {
        try {
            // step 1. add data change monitor with value filter
            DataQueryParam queryParam = new DataQueryParam(TEST_DEVICE_ID_1, TEST_RESOURCE_ID_1, 2, 150, true);
            monitor1 = IAgentManager.getInstance().addDataMonitor(queryParam, this);
        } catch (CoapException e) {
            e.printStackTrace();
        }
    }

    public ResponseCode onResourceDataChanged(String deviceID, String resourceUri, ResourceDataGeneral resourceData) {
        if (TEST_RESOURCE_ID_1.equals(resourceUri) || TEST_RESOURCE_ID_1.equals("/"+resourceUri)) {
        	Float temperature;
          if(resourceData.isParsed()) {
              ResourceDataLWM2M propertyTemprature = (ResourceDataLWM2M) (resourceData);
              temperature = Float.valueOf(propertyTemprature.items.get(0).v);
          }
          else
          {
              temperature = Float.valueOf(resourceData.getRawPayload());
          }
          String logText = "[Calibration]    "+deviceID + "/" + resourceUri + (temperature>10?" ":"  ") + String.format("%2.1f", temperature);
          temperature=temperature+2;
          if(temperature>50) temperature=50f;
          logText+="C ......... Changed ["+temperature+"]";
          LogUtil.log(logText);
          
          resourceData.setRawPayload(String.valueOf(temperature));
        }
        else
        {
            LogUtil.log("[Calibration]    Invalid reource uri: "+resourceUri);
        }
        
        return ResponseCode.CHANGED;
    }

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		IAgentManager.getInstance().removeDataMonitor(monitor1);
	}
}
