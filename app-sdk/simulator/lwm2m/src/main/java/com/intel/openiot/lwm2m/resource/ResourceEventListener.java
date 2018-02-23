package com.intel.openiot.lwm2m.resource;

public interface ResourceEventListener {
	
	int EVENT_VALUE_CHANGED = 0;
	int EVENT_VALUE_READ = 1;
	int EVENT_VALUE_WRITE = 2;

	public void resourceChanged(int resourceID, int event);

}
