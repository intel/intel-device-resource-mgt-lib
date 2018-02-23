package com.intel.openiot.lwm2m.client;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import leshan.client.LwM2mClient;
import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.SingleResourceDefinition;

import com.intel.openiot.lwm2m.resource.FloatValueResource;
import com.intel.openiot.lwm2m.resource.ResourceEventListener;

/*
 * 
 */
public class TemperatureDevice extends AbstractDevice implements ResourceEventListener {
    
	public static final int OBJECT_ID = 30242;
	
	JLabel wIcon = new JLabel();
	
    // resource in this device
    final FloatValueResource temperatureResource = new FloatValueResource(20.0f, 0);

    private String deviceID;

    
    public static void main(final String[] args) {
        new TemperatureDevice(args.length>0?args[0]:"TEMP-1");
    }

    protected JTabbedPane createDevicePanel() {        
        return new ValueUI("/thermometer_blue.png", temperatureResource);
    }

    public TemperatureDevice(String deviceID) {
	this.deviceID = deviceID;
        //init lwm2m objects
    	objectDevice = createObjectDefinition();
        client = new LwM2mClient(objectDevice);
        
        JFrame mainFrame = createMainFrame(this);
        mainFrame.setVisible(true);
        int index = getInedxByID(deviceID);
        mainFrame.setLocation(mainFrame.getWidth()*(index>5?5:(index-1)), mainFrame.getHeight());
        temperatureResource.addValueListner(this);

    }

    
    private LwM2mClientObjectDefinition createObjectDefinition() {        
        // Create an object model
        final LwM2mClientObjectDefinition objectDevice = new LwM2mClientObjectDefinition(OBJECT_ID, true, true,
                new SingleResourceDefinition(0, temperatureResource, true));
        return objectDevice;
    }    
    
    @Override
	protected String getDeviceUUID() {
		return deviceID;
	}

    public void resourceChanged(int resourceID, int event)
    {
	    switch(event)
	    {
	    case ResourceEventListener.EVENT_VALUE_READ:
		textArea.append("\n receive read request for resource "+resourceID+" from gateway, return temprature: "+temperatureResource.getValue()+" \n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
		break;
	    case ResourceEventListener.EVENT_VALUE_WRITE:
		textArea.append("\n receive write request for resource "+resourceID+" from gateway, set temprature to: "+temperatureResource.getValue()+" \n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
		break;
	    }
    }  
}
