# -*- coding: utf-8 -*-

# Copyright (C) 2017 Intel Corporation.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


from .data_utility import DataUtility
from .rdutility import RDUtility
from .ibroker_utility import IBrokerUtility
from ..model.data_query_param import DataQueryParam
from ..model.device import DeviceInfo
from ..model.group import Group
from ..model.rdquery_param import RDQueryParam
from ..model.resource import Resource
from ..model.resource_data_general import ResourceDataGeneral
from ..model.resource_property_data import ResourcePropertyData
from .my_coap_server import MyCoapServer
from ams_utils.coap_exception import CoapException
from ams_utils.constants import *
from ams_utils.singleton import singleton
from ams_utils.log_util import LogUtil
from ams_utils.media_type_format import MediaTypeFormat
from ams_utils.payload_parser import PayloadParser
'''
 * The IAgentManager class is a collection of methods to access all resources on the gateway managed by iAgent.<br><br>
 * 
 * This is the major access entry for all resources under iAgent. In any APP, IAgentManager.get_instance() shall be called firstly 
 * to get the singleton instance. Any action to create a new instance by "new" is NOT allowed.<br><br>
 * 
 * Please note the API name convention that any API with prefix “Do” in the name will trigger the message interaction with 
 * iAgent process. For example API do_device_query() API will query the devices and associated resources from iAgent according 
 * to the query parameters, and the API do_resource_get() will trigger iAgent to send GET request to the final service provider 
 * and return the result to the caller.<br><br>
 * 
 * The SDK will cache all the devices and resource queried from API do_device_query(). The APIs get_all_devices() , get_device() 
 * and get_reource() can be used to find the device and resources cached from previous query result.  It helps the user 
 * application avoid frequently calling do_device_query() for retrieve the devices. The results of two do_device_query() calls 
 * will be overlapped, rather than being overwritten.<br><br>
 * 
 * Please note the SDK won’t update the online status or resources of a cached device if the device was actually going 
 * offline or had resource changes. To monitor the status change of device and resource, the API add_rd_monitor() is available 
 * for this purpose. Your application can use the RDQueryParam to specify the monitoring condition. The API add_rd_monitor() 
 * can be called multiple times, and the devices reported by the monitors are also cached in the application locally.<br><br>
 * 
 * To get the data of resources managed by the iAgent, the application can use one-time call do_resource_get(), or use the 
 * data monitor API add_data_monitor() for automatically data reporting. The listener API for the data monitor is:<br>
 * on_resource_data_changed(deviceID, resouceUri, ResourceDataGeneral data)<br>
 * 
 * The content of reported date is provided by the parameter of class ResourceDataGeneral object in the callback API. 
* As the data reporting was actually implemented in a COAP message from iAgent to the user application, the COAP payload 
 * is parsed and converted to a object of class ResourceDataGeneral or its extended subclasses. The SDK already implemented 
 * two data parsers for LWM2M and OCF payload format, which output the instance of  ResourceDataLWM2M  and  
 * ResourceDataOCF (subclass of ResourceDataGeneral) respectively. The SDK provides a mechanism for adding user defined 
 * payload parser through calling API add_payload_parser().<br><br>
 *
 * @author yaoying
 * @version 1.0
 * @since Dec 2017

'''
@singleton
class IAgentManager(object):
    def __init__(self):
        self.__rd_utility = RDUtility()
        self.__data_utility = DataUtility()
        self.__ibroker_utility=IBrokerUtility()

    def add_data_monitor(self, query, event_handler):
        return self.__data_utility.start_data_observer(query, event_handler)
    '''
     * Method to register the parser for resource property on special device. 
     * 
     * @param formatType The format of resource property on special device, which shall not duplicate with the result of API getFormatSupported.
     * This formatType can be self-defined.
     * @param parser This parameter is the parser implementation for resource property on special device.
     * 
     * @see iagent.utilities.payload_parser.PayloadParser
    '''
    def add_payload_parser(self, format_type, parser):
        return self.__data_utility.register_payload_parser(format_type, parser)

    '''
     * Method to add a monitor for property change of devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. Property change event will be notified by the second parameter 
     * on_rd_event_listener. 
     * 
     * @param query This parameter is for different query condition of target device, group or resource.
     * @param on_rdevent_listener This parameter is event handler when attribute value for monitored target is changed. 
     * @return ID of this monitor, which shall be stored by APP and used when this RD monitor to deleted.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
     * @see query is a instance of iagent.model.rd_query_param.RDQueryParam
     * @see on_rdevent_listener is a function 
    '''
    def add_rd_monitor(self, query, on_rdevent_listener):
        return self.__rd_utility.create_monitor(query, on_rdevent_listener)

    '''
     * Method to get devices that is specified by parameter query. 
     * 
     * @param query This parameter is for different query condition of target devices.
     * @return The specified device by parameter query if existing on the gateway or NULL if not existing.
     * 
     * @see iagent.model.rd_query_param.RDQueryParam
    '''
    def do_device_query(self, query):
        result = ""
        try:
            devices = self.query_rd(query, result)
            return devices
        except CoapException, e:
            e.print_stack_trace()
            result_devices = []
            return result_devices

    '''
     * Method to get attribute value of resource in devices, groups.
     * This method is synchronous and the timer for timeout is a 2s. Returned value is string in JSON format, 
     * which means APP itself need to parse the returned JSON string according to such different devices as LWM2M, 
     * OIC, MODBUS and son.
     * 
     * @param resource This parameter is the target resource object or the resource_url of resource object.
     * @return String of attributes value in JSON format.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
    '''
    def do_resource_get(self, resource):
        return self.__data_utility.do_resource_get(resource)


    def do_resource_property_get(self, resource, property):
        return self.__data_utility.do_resource_get(
            resource.get_absolute_uri() + 
            "/" + property)

    def do_resource_property_put(self,
                                 resource,
                                 property,
                                 format=MediaTypeFormat.TEXT_PLAIN):
        if isinstance(property, basestring):
	    return self.do_resource_put(resource, format, property)
        else:
            return self.do_resource_put(resource, format, property.to_json())

    '''
     * Method to change attribute value of resource in devices, groups.
     * This method is synchronous and the timer for timeout is a 2s. APP itself need to compose   
     * the request payload string in JSON format according to such different devices as LWM2M, 
     * OIC, MODBUS and son.
     * 
     * @param resource This parameter is the target resource object or the url of resource object.
     * @param format This parameter is media format, which defined in {@link com.intel.imrt.iagent.utilities.MediaTypeFormat}.
     * @param payload This parameter is the request message with different content format for different devices.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * @return true - Success  false - Fail
     * 
     * @see iagent.model.resource.Resource
     * @see iagent.utilities.media_format_type.MediaTypeFormat
    '''
    def do_resource_put(self, resource, format, payload):
        return self.__data_utility.do_resource_put(resource, format, payload)

    def get_format_supported(self):
        formats = []
        formats.append(MediaTypeFormat.APPLICATION_JSON)
        formats.append(MediaTypeFormat.APPLICATION_LWM2M)
        return formats

    '''
     * Method to get the group specified by group name on the gateway.
     * 
     * @param groupName Group name
     * @return The specified group if existing on the gateway or NULL if not existing.
    '''
    def get_group(self, group_name):
        for item in self.__groups:
            if item.get_name() == group_name:
                return item
        return None
    
    '''
     * Method to modify a monitor for property change of devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. 
     * 
     * @param mid ID of this monitor, which is from returned value of API addRDMonitor.
     * @param query This parameter is for different query condition of target device, group or resource.
     * @param on_rdevent_listener This parameter is event handler when attribute value for monitored target is changed. 
     * @return ID of this monitor, which shall be stored by APP and used when this RD monitor to deleted.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
     * @see iagent.model.rd_query_param.RDQueryParam
     * @see on_rdevent_listener is a function
    '''
    def modify_rd_monitor(self, mid, query, on_rdevent_listener):
        return self.__rd_utility.modify_monitor(mid, query,
                                                on_rdevent_listener)

    def query_rd(self, query, result):
        return self.__rd_utility.query_rd(query, result)


    '''
     * Method to delete a monitor for value change of attribute in devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. Value change event will be notified by the second parameter 
     * eventHandler. 
     * 
     * @param data_monitor_id ID of this monitor, which is from returned value of API add_data_monitor.
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * @return true - Success  false - Fail
     * 
    '''
    def remove_data_monitor(self, data_monitor_id):
        return self.__data_utility.stop_data_observer(data_monitor_id)

    '''
     * Method to delete a monitor for property change of devices, groups or resources.
     * This method is synchronous, that means this method maybe blocked for network issue, but
     * there is a 2s timer for timeout. 
     * 
     * @param rdMoniterID ID of this monitor, which is from returned value of API addRDMonitor.
     * @return true - Success  false - Fail
     * @throws CoapException exception will be thrown when no response or invalid response code from server
     * 
    '''
    def remove_rd_monitor(self, rd_monitor_id):
        return self.__rd_utility.remove_monitor(rd_monitor_id)


    def start(self):
        MyCoapServer().start_server()

    def stop(self):
        MyCoapServer().stop_server()
        
    def ibroker_put_msg(self,uri,payload):
        return self.__ibroker_utility.ibroker_put(uri,payload) 
        
    def ibroker_get_msg(self,uri):
        return self.__ibroker_utility.ibroker_gut(uri) 
         
