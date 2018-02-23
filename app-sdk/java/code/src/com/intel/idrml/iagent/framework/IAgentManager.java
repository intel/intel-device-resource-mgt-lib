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


package com.intel.idrml.iagent.framework;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.network.config.NetworkConfig;

import com.intel.idrml.iagent.model.DataQueryParam;
import com.intel.idrml.iagent.model.DeviceInfo;
import com.intel.idrml.iagent.model.Group;
import com.intel.idrml.iagent.model.RDQueryParam;
import com.intel.idrml.iagent.model.Resource;
import com.intel.idrml.iagent.model.ResourceDataGeneral;
import com.intel.idrml.iagent.model.ResourcePropertyData;
import com.intel.idrml.iagent.utilities.LogUtil;
import com.intel.idrml.iagent.utilities.MediaTypeFormat;
import com.intel.idrml.iagent.utilities.PayloadParser;


/**
 * The IAgentManager class is a collection of methods to access all resources on the gateway managed by iAgent.<br><br>
 * 
 * This is the major access entry for all resources under iAgent. In any APP, IAgentManager.getInstance() shall be called firstly 
 * to get the singleton instance. Any action to create a new instance by "new" is NOT allowed.<br><br>
 * 
 * Please note the API name convention that any API with prefix “Do” in the name will trigger the message interaction with 
 * iAgent process. For example API DoDeviceQuery() API will query the devices and associated resources from iAgent according 
 * to the query parameters, and the API DoResourceGET() will trigger iAgent to send GET request to the final service provider 
 * and return the result to the caller.<br><br>
 * 
 * The SDK will cache all the devices and resource queried from API DoDeviceQuery(). The APIs getAllDevices() , getDevice() 
 * and getReource() can be used to find the device and resources cached from previous query result.  It helps the user 
 * application avoid frequently calling DoDeviceQuery() for retrieve the devices. The results of two DoDeviceQuery() calls 
 * will be overlapped, rather than being overwritten.<br><br>
 * 
 * Please note the SDK won’t update the online status or resources of a cached device if the device was actually going 
 * offline or had resource changes. To monitor the status change of device and resource, the API addRDMonitor() is available 
 * for this purpose. Your application can use the RDQueryParam to specify the monitoring condition. The API addRDMonitor() 
 * can be called multiple times, and the devices reported by the monitors are also cached in the application locally.<br><br>
 * 
 * To get the data of resources managed by the iAgent, the application can use one-time call DoResourceGET(), or use the 
 * data monitor API addDataMonitor() for automatically data reporting. The listener API for the data monitor is:<br>
 * onResourceDataChanged(java.lang.String deviceID, java.lang.String resouceUri, ResourceDataGeneral data)<br>
 * 
 * The content of reported date is provided by the parameter of class ResourceDataGeneral object in the callback API. 
 * As the data reporting was actually implemented in a COAP message from iAgent to the user application, the COAP payload 
 * is parsed and converted to a object of class ResourceDataGeneral or its extended subclasses. The SDK already implemented 
 * two data parsers for LWM2M and OCF payload format, which output the instance of  ResourceDataLWM2M  and  
 * ResourceDataOCF (subclass of ResourceDataGeneral) respectively. The SDK provides a mechanism for adding user defined 
 * payload parser through calling API addPayloadParser().<br><br>
 *
 * @author saigon
 * @version 1.0
 * @since Dec 2016
 */
public class IAgentManager {
    private static IAgentManager instance = null;
    static {
        CaliforniumLogger.initialize();
        CaliforniumLogger.setLevel(Level.OFF); //For BDT is based on Java 1.6 which has no method System.getLineSeperator()
        instance = new IAgentManager();
        instance.init();
    }

    private List<DeviceInfo> devices;
    private RDUtility rdUtility;
    private DataUtility dataUtility;
    private ApplicationConfigeUtility appConfigUtility;
	private List<Group> groups;
	private List<String> monitorsIdData;
	private List<String> monitorsIdRD;
    private IAgentManager() {
        devices = new ArrayList<DeviceInfo>();
        groups = new ArrayList<Group>();
        monitorsIdData = new ArrayList<String>();
        monitorsIdRD = new ArrayList<String>();

        int size = 1024 *100;
        NetworkConfig.getStandard()
        .set(NetworkConfig.Keys.UDP_CONNECTOR_DATAGRAM_SIZE, size)
        .set(NetworkConfig.Keys.MAX_RESOURCE_BODY_SIZE, size);
        rdUtility = new RDUtility();
        dataUtility = new DataUtility();
        appConfigUtility = ApplicationConfigeUtility.getInstance("sdk_product");
	
	Runtime.getRuntime().addShutdownHook(new Thread()
	{
	    @Override
	    public void run()
	    {
		try
		{
		    myfinalize();
		}
		catch (Throwable e)
		{
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	});
    }

    private void init() {
        start();
//        initDevices();
    }

//    private void initDevices() {
//        try {
//            RDQueryParam query = new RDQueryParam();
//            List<Device> tempDevices = rdUtility.queryRD();
//            if (tempDevices != null)
//                devices = tempDevices;
//            
//            refreshGroupscashed(devices);
//
//            cacheMonitorID=rdUtility.createMonitor(query, new OnRDEventListener() {
//                @Override
//                public void onDeviceChanged(List<Device> devicesDelta) {
//                    refreshDevicesCached(devicesDelta);
//                    refreshGroupscashed(devicesDelta);
//                }
//
//                @Override
//                public void onResourceChanged(List<Resource> resourcesChanged, boolean isAddedOrRemove) {
//                	//TODO saigon seems that this API is meanless
////                    refreshResourcesCached(resourcesChanged, isAddedOrRemove);
//                }
//            });
//        } catch (CoapException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void refreshGroupscashed(List<Device> devicesDelta) {
//    	List<String> newGroupNames = getNewGroupNames(devicesDelta);
//    	for(String groupName: newGroupNames)
//    	{
//    		groups.add(new Group(groupName));
//    	}
//	}

	private List<String> getNewGroupNames(List<DeviceInfo> devicesDelta) {
    	List<String> newGroupNames = new ArrayList<String>();
    	if(devicesDelta!=null)
    	{
    		for(DeviceInfo device: devicesDelta)
    		{
    			List<String> deviceGroups = device.getGroups();
    			if(deviceGroups!=null)
    			{
    				for(String groupName:deviceGroups)
    				{
    					if(isNewGroupName(groupName))
    					{
    						newGroupNames.add(groupName);
    					}
    				}
    			}
    		}
    	}
		return newGroupNames;
	}

	private boolean isNewGroupName(String groupName) {
		for(Group group:groups)
		{
			if(group.getName().equals(groupName))
			{
				return false;
			}
		}
		return true;
	}

    protected void refreshDevicesCached(List<DeviceInfo> devicesDelta) {
    	if(devicesDelta==null) return;
    	
        HashSet<DeviceInfo> newDevices = new HashSet<DeviceInfo>(devices);
        newDevices.addAll(devicesDelta);

        devices = new ArrayList<DeviceInfo>(newDevices);
    }

    /**
     * Get the global instance of IAgentManager. <p>
     * @return  The global instance.
     */
    public static IAgentManager getInstance() {
        if (instance == null) {
            instance = new IAgentManager();
            instance.init();
        }

        return instance;
    }

    private void start() {
        MyCoapServer.getInstance().startServer();
    }

//    /**
//     * Method to get all devices on the gateway.
//     * 
//     * @return All devices in list on the gateway.
//     */
//    public List<Device> getAllDevices() {
//        return devices;
//    }

//    /**
//     * Method to get all groups on the gateway.
//     * 
//     * @return All groups in list on the gateway.
//     */
//    public List<Group> getAllGroups() {
//        return groups;
//    }

//    @Override
    protected void myfinalize() throws Throwable {
	LogUtil.log(LogUtil.LEVEL.INFO, "Cleanin run time env ...");
	
	for(String monitorID:monitorsIdData)
	{
	    LogUtil.log(LogUtil.LEVEL.INFO, "Removing data monitor: "+monitorID);
	    dataUtility.stopDataObserver(monitorID);
	}
	for(String monitorID:monitorsIdRD)
	{
	    LogUtil.log(LogUtil.LEVEL.INFO, "Removing RD monitor: "+monitorID);
	    rdUtility.removeMonitor(monitorID);
	}
	
        stop();
        super.finalize();
    }

    private void stop() {
        MyCoapServer.getInstance().stopServer();
        AppConfigCoapServerManager.getInstance().stopServer();
    }

//    /**
//     * Method to get the device specified by ID on the gateway.
//     * 
//     * @param di Device ID
//     * @return The specified device if existing on the gateway or NULL if not existing.
//     */
//    public Device getDevice(String di) {
//        if (devices == null)
//            return null;
//
//        for (Device device : devices) {
//            if (device.getDeviceId().equals(di)) {
//                return device;
//            }
//        }
//        return null;
//    }

//    /**
//     * Method to get the group specified by group name on the gateway.
//     * 
//     * @param groupName Group name
//     * @return The specified group if existing on the gateway or NULL if not existing.
//     */
//    public Group getGroup(String groupName) {
//    	for(Group group:groups)
//    	{
//    		if(group.getName().equals(groupName))
//    		{
//    			return group;
//    		}
//    	}
//        return null;
//    }

//    /**
//     * Method to get the resource specified by device ID and resource URI on the gateway.
//     * 
//     * @param di Device ID
//     * @param ri Resource URI
//     * @return The specified resource if existing on the gateway or NULL if not existing.
//     */
//    public Resource getReource(String di, String ri) {
//        for (Device device : devices) {
//            if (device.getDeviceId().equals(di)) {
//                return device.getResource(ri);
//            }
//        }
//        return null;
//    }

    private List<DeviceInfo> queryRD(RDQueryParam query, String result) throws CoapException {
        return rdUtility.queryRD(query, result);
    }

    // API for RD
    /**
     * Method to add a monitor for property change of devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. Property change event will be notified by the second parameter 
     * onRDEventListener. 
     * 
     * @param query This parameter is for different query condition of target device, group or resource.
     * @param onRDEventListener This parameter is event handler when attribute value for monitored target is changed. 
     * @return ID of this monitor, which shall be stored by APP and used when this RD monitor to deleted.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
     * @see com.intel.idrml.iagent.model.RDQueryParam
     * @see com.intel.idrml.iagent.framework.OnDataListener
     */
    public String addRDMonitor(RDQueryParam query, OnRDEventListener onRDEventListener) throws CoapException {
        String monitorID = rdUtility.createMonitor(query, onRDEventListener);
        if(!monitorsIdRD.contains(monitorID)) monitorsIdRD.add(monitorID);
        return monitorID;
    }

    /**
     * Method to modify a monitor for property change of devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. 
     * 
     * @param mid ID of this monitor, which is from returned value of API addRDMonitor.
     * @param query This parameter is for different query condition of target device, group or resource.
     * @param onRDEventListener This parameter is event handler when attribute value for monitored target is changed. 
     * @return ID of this monitor, which shall be stored by APP and used when this RD monitor to deleted.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
     * @see com.intel.idrml.iagent.model.RDQueryParam
     * @see com.intel.idrml.iagent.framework.OnDataListener
     */
    public String modifyRDMonitor(String mid, RDQueryParam query, OnRDEventListener onRDEventListener) throws CoapException {
        return rdUtility.modifyMonitor(mid, query, onRDEventListener);
    }

    /**
     * Method to delete a monitor for property change of devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. 
     * 
     * @param rdMoniterID ID of this monitor, which is from returned value of API addRDMonitor.
     * @return true - Success  false - Fail
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
     * @see com.intel.idrml.iagent.model.RDQueryParam
     * @see com.intel.idrml.iagent.framework.OnDataListener
     */
    public boolean removeRDMonitor(String rdMoniterID) throws CoapException {
	if(monitorsIdRD.contains(rdMoniterID))
	{
	    monitorsIdRD.remove(rdMoniterID);
	    return rdUtility.removeMonitor(rdMoniterID);
	}
	else
	{
	    return true;
	}
    }

    //API for data

    /**
     * Method to add a monitor for value change of attribute in devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. Value change event will be notified by the second parameter 
     * eventHandler. 
     * 
     * @param query This parameter is for different query condition of target attribute.
     * @param eventHandler This parameter is event handler when attribute value for monitored target is changed. 
     * @return ID of this monitor, which shall be stored by APP and used when this RD monitor to deleted.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
     * @see com.intel.idrml.iagent.model.RDQueryParam
     * @see com.intel.idrml.iagent.framework.OnDataListener
     */
    public String addDataMonitor(DataQueryParam query, OnDataListener eventHandler) throws CoapException {
        String monitorID = dataUtility.startDataObserver(query, eventHandler);
        if(!monitorsIdData.contains(monitorID)) monitorsIdData.add(monitorID);
        return monitorID;
    }


    /**
     * Method to delete a monitor for value change of attribute in devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. Value change event will be notified by the second parameter 
     * eventHandler. 
     * 
     * @param dataMonitorID ID of this monitor, which is from returned value of API addDataMonitor.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * @return true - Success  false - Fail
     * 
     */
    public boolean removeDataMonitor(String dataMonitorID) throws CoapException {
	if(monitorsIdData.contains(dataMonitorID))
	{
	    monitorsIdData.remove(dataMonitorID);
	    return dataUtility.stopDataObserver(dataMonitorID);
	}
	else
	    return true;
    }

    /**
     * Method to get attribute value of resource in devices, groups.
     * This method is synchronous and the timer for timeout is a 2s. Returned value is string in JSON format, 
     * which means APP itself need to parse the returned JSON string according to such different devices as LWM2M, 
     * OIC, MODBUS and son.
     * 
     * @param resource This parameter is the target resource object.
     * @return String of attributes value in JSON format.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
     * @see com.intel.idrml.iagent.model.RDQueryParam
     * @see com.intel.idrml.iagent.framework.OnDataListener
     */
    public ResourceDataGeneral DoResourceGET(Resource resource) throws CoapException {
        return dataUtility.DoResourceGET(resource);
    }

    public ResourceDataGeneral DoResourcePropertyGET(Resource resource, String property) throws CoapException {
        return dataUtility.DoResourceGET(Constants.getIAgentServerUri()+resource.getAbsoluteUri()+"/"+property);
    }
    /**
     * Method to get attribute value of resource in devices, groups.
     * This method is synchronous and the timer for timeout is a 2s. Returned value is string in JSON format, 
     * which means APP itself need to parse the returned JSON string according to such different devices as LWM2M, 
     * OIC, MODBUS and son.
     * 
     * @param resourceUrl This parameter is URL for the target resource object.
     * @return String of attributes value in JSON format.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
     * @see com.intel.idrml.iagent.model.RDQueryParam
     * @see com.intel.idrml.iagent.framework.OnDataListener
     */
    public ResourceDataGeneral DoResourceGET(String resourceUrl) throws CoapException {
        return dataUtility.DoResourceGET(resourceUrl);
    }

    /**
     * Method to change attribute value of resource in devices, groups.
     * This method is synchronous and the timer for timeout is a 2s. APP itself need to compose   
     * the request payload string in JSON format according to such different devices as LWM2M, 
     * OIC, MODBUS and son.
     * 
     * @param resource This parameter is the target resource object.
     * @param format This parameter is media format, which defined in {@link com.intel.idrml.iagent.utilities.MediaTypeFormat}.
     * @param payload This parameter is the request message with different content format for different devices.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * @return true - Success  false - Fail
     * 
     * @see com.intel.idrml.iagent.model.Resource
     * @see com.intel.idrml.iagent.utilities.MediaTypeFormat
     */
    public boolean DoResourcePUT(Resource resource, int format, String payload) throws CoapException {
    	LogUtil.log(LogUtil.LEVEL.INFO, "payload: "+payload);
        return dataUtility.DoResourcePUT(resource, format, payload);
    }

    public boolean DoResourcePropertyPUT(Resource resource, int format, ResourcePropertyData proprty) throws CoapException {
        return this.DoResourcePUT(resource, format, proprty.toJson());
    }

	public boolean DoResourcePropertyPUT(String resourceUrl, String propertyValue) throws CoapException {
        return this.DoResourcePUT(resourceUrl, MediaTypeFormat.TEXT_PLAIN, propertyValue);
	}

    /**
     * Method to change attribute value of resource in devices, groups.
     * This method is synchronous and the timer for timeout is a 2s. APP itself need to compose   
     * the request payload string in JSON format according to such different devices as LWM2M, 
     * OIC, MODBUS and son.
     * 
     * @param resourceUrl This parameter is URL of the target resource object.
     * @param format This parameter is media format, which defined in {@link com.intel.idrml.iagent.utilities.MediaTypeFormat}.
     * @param payload This parameter is the request message with different content format for different devices.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * @return true - Success  false - Fail
     * 
     * @see com.intel.idrml.iagent.model.Resource
     * @see com.intel.idrml.iagent.utilities.MediaTypeFormat
     */
    public boolean DoResourcePUT(String resourceUrl, int format, String payload) throws CoapException {
        return dataUtility.DoResourcePUT(resourceUrl, format, payload);
    }

    /**
     * Method to register the parser for resource property on special device. 
     * 
     * @param formatType The format of resource property on special device, which shall not duplicate with the result of API getFormatSupported.
     * This formatType can be self-defined.
     * @param parser This parameter is the parser implementation for resource property on special device.
     * 
     * @see com.intel.idrml.iagent.utilities.PayloadParser
     */
    public void addPayloadParser(int formatType, PayloadParser parser) {
        dataUtility.registerPayloadParser(formatType, parser);
    }
    
    /**
     * Method to register the parser for resource property on special device. 
     * 
     * @return all format types of resource property that are supported on this gateway.
     */
    public List<Integer> getFormatSupported()
    {
    	List<Integer> formats = new ArrayList<Integer>();
    	formats.add(MediaTypeFormat.APPLICATION_JSON);
    	formats.add(MediaTypeFormat.APPLICATION_JSON_LWM2M);
    	
    	return formats;
    }

    /**
     * Method to get devices that is specified by parameter query. 
     * 
     * @param query This parameter is for different query condition of target devices.
     * @return The specified device by parameter query if existing on the gateway or NULL if not existing.
     * 
     * @see com.intel.idrml.iagent.model.RDQueryParam
     */
    public List<DeviceInfo> DoDeviceQuery(RDQueryParam query) {
        String result = "";
        try {
            List<DeviceInfo> devices = this.queryRD(query, result);

            refreshDevicesCached(devices);

            return devices;
        } catch (CoapException e) {
            e.printStackTrace();
            List<DeviceInfo> resultDevices = new ArrayList<DeviceInfo>();
            return resultDevices;
        }
    }

}
