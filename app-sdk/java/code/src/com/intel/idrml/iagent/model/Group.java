/*
 * Copyright (C) 2017 Intel Corporation.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.idrml.iagent.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intel.idrml.iagent.framework.IAgentManager;

public class Group {
    private String name;

    //@Expose(serialize = false, deserialize = false)
    private Map<String, String> attributes;

    public Group(String name) {
        this.name = name;

        attributes = new HashMap<String, String>();
    }

    public String getName() {
        return name;
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

//    public List<Device> getDeviceMembers() {
//    	ArrayList<Device> devices = new ArrayList<Device>();
//    	List<Device> allDevices = IAgentManager.getInstance().getAllDevices();
//    	for(Device device : allDevices)
//    	{
//    		if(device.getGroups().contains(name))
//    		{
//    			devices.add(device);
//    		}
//    	}
//    	
//        return devices;
//    }
//
//    public List<Resource> getResourceMembers() {
//    	ArrayList<Resource> resources = new ArrayList<Resource>();
//    	List<Device> allDevices = IAgentManager.getInstance().getAllDevices();
//    	for(Device device : allDevices)
//    	{
//    		List<Resource> resourcesDevice = device.getResources();
//    		for(Resource resource : resourcesDevice)
//    		{
//    			if(resource.getGroups().contains(name))
//    			{
//    				resources.add(resource);
//    			}
//    		}
//    	}
//    	
//        return resources;
//    }


    @Override
    public boolean equals(Object obj) {
        return name.equals(((Group) obj).getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}
