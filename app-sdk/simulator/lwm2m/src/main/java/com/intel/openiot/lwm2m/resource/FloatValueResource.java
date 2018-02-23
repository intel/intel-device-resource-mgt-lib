package com.intel.openiot.lwm2m.resource;

import java.util.ArrayList;
import java.util.List;

import leshan.client.exchange.LwM2mExchange;
import leshan.client.resource.decimal.FloatLwM2mExchange;
import leshan.client.resource.decimal.FloatLwM2mResource;
import leshan.client.response.ExecuteResponse;

public class FloatValueResource extends FloatLwM2mResource {

    private Float value;
    private final int resourceId;
    
    public int getResourceId()
    {
        return resourceId;
    }

    private List<ResourceEventListener> valueListeners = new ArrayList<ResourceEventListener>();

    public FloatValueResource(final float initialValue, final int resourceId) {
        value = initialValue;
        this.resourceId = resourceId;
    }

    public void setValue(final Float newValue) {
        value = newValue;
        notifyResourceUpdated();
        notifyEventListener(ResourceEventListener.EVENT_VALUE_CHANGED);
    }

    public Float getValue() {
        return value;
    }

    @Override
    public void handleWrite(final FloatLwM2mExchange exchange) {
//        System.out.println("\tDevice: Writing on Float Resource " + resourceId);
        setValue(exchange.getRequestPayload());

        exchange.respondSuccess();
        notifyEventListener(ResourceEventListener.EVENT_VALUE_WRITE);
    }

    @Override
    public void handleRead(final FloatLwM2mExchange exchange) {
//        System.out.println("\tDevice: Reading on Float Resource " + resourceId);
        exchange.respondContent(value);
        notifyEventListener(ResourceEventListener.EVENT_VALUE_READ);
    }    
    
    @Override
    protected void handleExecute(LwM2mExchange exchange) {
        System.out.println("\tDevice: handleExecute  " + resourceId + " successfully.");
        exchange.respond(ExecuteResponse.success());
    }

    public void addValueListner(ResourceEventListener listener) {
    	valueListeners.add(listener);
    }
    
    private void notifyEventListener(int event) {
		for(ResourceEventListener listner : valueListeners) {
			listner.resourceChanged(resourceId, event);
		}		
	}

}