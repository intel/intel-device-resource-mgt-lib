/**********************************************************************************
 * SCENARIO: 
 * User APP need to monitor the change of all devices with type of resource "oic.r.light" 
 * 
 * SOLUTION: 
 * step 1. add RD monitor by API IAgentManager.addRDMonitor()
 * step 2. do action code in RD listener callback 
 * 
 ***************************************/
package com.intel.idrml.iagent.examples.applet;

import com.intel.util.IntelApplet;

import java.util.List;

import com.intel.idrml.iagent.framework.CoapException;
import com.intel.idrml.iagent.framework.IAgentManager;
import com.intel.idrml.iagent.framework.OnRDEventListener;
import com.intel.idrml.iagent.model.Device;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataLWM2M;
import com.intel.idrml.iagent.utilities.LogUtil;

public class AppMonitorDeviceAndResourceApplet extends IntelApplet
{

	public int onInit(byte[] request)
	{
            AppMonitorDeviceAndResource app = new AppMonitorDeviceAndResource();
            app.start();    
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


    public class AppMonitorDeviceAndResource {
    
    private static final String RESOURCE_TYPE_TEST = "oic.r.light";

    private IAgentManager gateway;

    private void start() {

        System.out.println("App Started ...");

        gateway = IAgentManager.getInstance();

        RDQueryParam query = new RDQueryParam();
        query.resouceType = RESOURCE_TYPE_TEST;
        try {
        	// step 1. add RD monitor by API IAgentManager.addRDMonitor()
            gateway.addRDMonitor(query, new OnRDEventListener() {
                public void onDeviceChanged(List<Device> device) {
                	// step 2. do action code in RD listener callback
                    LogUtil.log("OnDeviceEvent: " + device.size() + " devices");
                }

                public void onResourceChanged(List<Resource> resourcesChanged, boolean isAddedOrRemove) {
                    LogUtil.log("OnResourceEvent: " + resourcesChanged.size() + " resources");
                }
            });
        } catch (CoapException e) {
            e.printStackTrace();
        }
    }
}
}
