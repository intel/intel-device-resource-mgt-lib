package com.intel.openiot.lwm2m.resource;

import java.util.ArrayList;
import java.util.List;

import leshan.client.resource.integer.IntegerLwM2mExchange;
import leshan.client.resource.integer.IntegerLwM2mResource;

public class IntegerValueResource extends IntegerLwM2mResource {

    private Integer value;
    private final int resourceId;
    private List<ResourceEventListener> valueListeners = new ArrayList<ResourceEventListener>();

    public int getResourceId()
    {
        return resourceId;
    }

    
    public IntegerValueResource(final int initialValue, final int resourceId) {
        value = initialValue;
        this.resourceId = resourceId;
    }

    public void setValue(final Integer newValue) {
        value = newValue;
        //notify the observer
        notifyResourceUpdated();
        //notify the UI related to this resource
        notifyEventListener(ResourceEventListener.EVENT_VALUE_CHANGED);
    }

    private void notifyEventListener(int event) {
	for(ResourceEventListener listner : valueListeners) {
	    listner.resourceChanged(resourceId, event);
	}		
    }

    public Integer getValue() {
	return value;
    }

    @Override
    public void handleWrite(final IntegerLwM2mExchange exchange) {
        System.out.println("\tDevice: Writing on Integer Resource " + resourceId);
        setValue(exchange.getRequestPayload());

        exchange.respondSuccess();
        notifyEventListener(ResourceEventListener.EVENT_VALUE_WRITE);
    }

    @Override
    public void handleRead(final IntegerLwM2mExchange exchange) {
        System.out.println("\tDevice: Reading on IntegerResource " + resourceId);
        exchange.respondContent(value);
        notifyEventListener(ResourceEventListener.EVENT_VALUE_READ);
    }
    
    
    
    public void addValueListner(ResourceEventListener listener) {
    	valueListeners.add(listener);
    }

}
