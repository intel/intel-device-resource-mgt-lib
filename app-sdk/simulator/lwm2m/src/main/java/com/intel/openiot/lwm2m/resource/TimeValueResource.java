package com.intel.openiot.lwm2m.resource;

import java.util.Date;

import leshan.client.resource.time.TimeLwM2mExchange;
import leshan.client.resource.time.TimeLwM2mResource;

public class TimeValueResource extends TimeLwM2mResource{

	private Date value;

    public TimeValueResource() {
        this.value = new Date();
    }

    public void setValue(final Date newValue) {
        this.value = newValue;
        notifyResourceUpdated();
    }

    public Date getValue() {
        return value;
    }

    @Override
    public void handleWrite(final TimeLwM2mExchange exchange) {
        setValue(exchange.getRequestPayload());

        exchange.respondSuccess();
    }

    @Override
    public void handleRead(final TimeLwM2mExchange exchange) {
        System.out.println("\tDevice: Reading Current Device Time.");
        exchange.respondContent(getValue());
    }
}
