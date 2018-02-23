package com.intel.openiot.lwm2m.client;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JFrame;

import leshan.client.LwM2mClient;
import leshan.client.resource.LwM2mClientObjectDefinition;
import leshan.client.resource.SingleResourceDefinition;

import com.intel.openiot.lwm2m.resource.FloatValueResource;
import com.intel.openiot.lwm2m.resource.IntegerValueResource;
import com.intel.openiot.lwm2m.resource.ResourceEventListener;

public class SwitchDevice extends AbstractDevice implements ResourceEventListener {
	public static final int OBJECT_ID = 30245;
	
	protected LwM2mClientObjectDefinition locationObject;
	
	//first 0: value, second 0: resource id
	private IntegerValueResource resource = new IntegerValueResource(0, 0);
	
	private FloatValueResource latitudeResource = new FloatValueResource(100, 0);
	
	private FloatValueResource longitudeResource = new FloatValueResource(200, 1);

	private String deviceID;

	public static void main(final String[] args) {
        new SwitchDevice(args.length>0?args[0]:"FAN-1");
    }
	
	public SwitchDevice(String deviceID) {
	    this.deviceID = deviceID;
        //init lwm2m objects
    	objectDevice = createObjectDefinition();
    	locationObject = createLocationObjectDefinition();
    	client = new LwM2mClient(objectDevice);
        
        JFrame mainFrame = createMainFrame(this);
        mainFrame.setVisible(true);
        int index = getInedxByID(deviceID);
        mainFrame.setLocation(mainFrame.getWidth()*(index>5?5:(index-1)), 0);
        
        resource.addValueListner(this);
    }

	@Override
	protected JComponent createDevicePanel() {		
		return new SwitchUI("/fan3.gif", "/fan3.gif", this.resource);
	}	
	
	private LwM2mClientObjectDefinition createObjectDefinition() {       
        // Create an object model

        final LwM2mClientObjectDefinition objectDevice = new LwM2mClientObjectDefinition(OBJECT_ID, true, true,
                new SingleResourceDefinition(0, resource, true));

        return objectDevice;
    }
	
	private LwM2mClientObjectDefinition createLocationObjectDefinition() {       
        // Create an object model
		// Location Object ID = 6
		//    Resource Latitude: 0
		//    Resource Longitude: 1
		//    Resource Timestamp: 5
        final LwM2mClientObjectDefinition objectDevice = new LwM2mClientObjectDefinition(6, true, true,
                new SingleResourceDefinition(0, latitudeResource, true),  new SingleResourceDefinition(1, longitudeResource, true));

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
		textArea.append("\n receive read req for resource "+resourceID+", return fan state: "+resource.getValue()+" \n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
		break;
	    case ResourceEventListener.EVENT_VALUE_WRITE:
		textArea.append("\n receive write req for resource "+resourceID+", set fan state to: "+resource.getValue()+" \n");
		textArea.setCaretPosition(textArea.getDocument().getLength());
		break;
	    }
	}  

}
