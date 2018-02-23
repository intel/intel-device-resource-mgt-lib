/**********************************************************************************
 * SCENARIO: 
 * User APP need to monitor the resource data(temperature) change and control fans for LWM2M device 
 * 
 * SOLUTION: 
 * step 1. add data change monitor by API IAgentManager.addDataMonitor()
 * step 2. set the number of fans to be opened in listener: 
 *         temp<20 open 0 fans 
 *         temp<30 open 1 fans 
 *         temp<40 open 2 fans 
 *         temp<50 open 3 fans 
 *         temp>50 open 3 fans and alarm 
 * 
 ***************************************/

import java.util.ArrayList;
import java.util.List;

import org.eclipse.californium.core.coap.CoAP.ResponseCode;

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.framework.OnDataListener;
import com.intel.idrml.iagent.model.DataQueryParam;
import com.intel.idrml.iagent.model.DeviceInfo;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourceDataLWM2M;
import com.intel.idrml.iagent.utilities.LogUtil;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;

public class AppControlFansOnTemperature implements OnDataListener {

	private static final String TEST_DEVICE_ID_1 = "TEMP-1";
	private static final String TEST_RESOURCE_ID_1 = "/30242/0/0";
	List<DeviceInfo> fans = new ArrayList<DeviceInfo>();
	private int enabledFansNumLastTime=0;
	private String monitor1;

	public static void main(String[] args) {
//		LogUtil.level = LogUtil.LEVEL.DEBUG;  // open this comments if detailed log in SDK is needed
//		LogUtil.logCodeInfo = true;
		AppControlFansOnTemperature app = new AppControlFansOnTemperature();
		app.init();
		app.run();
	}

	private void init()
	{
		List<DeviceInfo> devices;
		RDQueryParam query = new RDQueryParam();
		//	query.deviceType = Device.StandardType.lwm2m.toString();
		//	devices = IAgentManager.getInstance().DoDeviceQuery(query );
		for(int i=1; i<4; i++)
		{
			query.deviceID = "FAN-"+i;
			devices = IAgentManager.getInstance().DoDeviceQuery(query );
			if(devices!=null && devices.size()>0)
			{
				fans.add(devices.get(0));
			}
		}
	}

	private void run() {
		try {
			// step 1. add data change monitor with value filter
			DataQueryParam queryParam = new DataQueryParam(TEST_DEVICE_ID_1, TEST_RESOURCE_ID_1, 2, 20, false);
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

			int enabledFansNum = (temperature>40)?3:((temperature>30)?2:((temperature>20)?1:0));
			if(enabledFansNum!=enabledFansNumLastTime)
			{
				enableFans(enabledFansNum);
				LogUtil.log("[Fan Control]    "+deviceID + "/" + resourceUri + (temperature>10?" ":"  ") + String.format("%2.1f", temperature)+"C ......... "+getFanStatus(enabledFansNumLastTime, enabledFansNum)+"\n");
				enabledFansNumLastTime = enabledFansNum;
			}
			else
			{
				LogUtil.log("[Fan Control]    "+deviceID + "/" + resourceUri + (temperature>10?" ":"  ") + String.format("%2.1f", temperature)+"C ......... "+getFanStatus(enabledFansNumLastTime, enabledFansNum)+"\n");
			}
		}
		else
		{
			LogUtil.log("[Fan Control]    Invalid reource uri: "+resourceUri);
		}

		return ResponseCode.CHANGED;
	}

	private String getFanStatus(int fansOnNumLastTime, int fansOnNum) 
	{
		if(fansOnNumLastTime==fansOnNum)
		{
			if(fansOnNum==0)
			{
				return "All fans off";
			}
			else
			{
				return ""+fansOnNum+" fans on";
			}
		}
		else if(fansOnNumLastTime < fansOnNum)
		{
			StringBuffer sb = new StringBuffer("Opening");
			for(int i=fansOnNumLastTime; i<fansOnNum; i++)
			{
				sb.append(" FAN-"+(i+1));
			}
			return sb.toString();
		}
		else //if(fansOnNumLastTime>fansOnNum)
		{
			StringBuffer sb = new StringBuffer("Closing");
			for(int i=fansOnNum; i<fansOnNumLastTime; i++)
			{
				sb.append(" FAN-"+(i+1));
			}
			return sb.toString();
		}
	}

	private void enableFans(int num)
	{
		for(int i=0; i<fans.size(); i++)
		{
			try
			{
				IAgentManager.getInstance().DoResourcePUT(fans.get(i).getResources().get(0).getAbsoluteUri()+"/0", MediaTypeFormat.TEXT_PLAIN, (i<num)?"1":"0");
			}
			catch (CoapException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		IAgentManager.getInstance().removeDataMonitor(monitor1);
		super.finalize();
	}
}
