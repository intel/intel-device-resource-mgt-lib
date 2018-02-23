# -*- coding: utf-8 -*-

import sys
import json
import time
import sys
import gc

from iagent.framework.iagent_manager import IAgentManager
from iagent.model.data_query_param import DataQueryParam
from iagent.model.rdquery_param import RDQueryParam
from iagent.model.device import DeviceInfo
from iagent.model.resource import Resource
from iagent.model.resource_property_data import ResourcePropertyData
from iagent.model.resource_data_general import     ResourceDataGeneral
from ams_utils.media_type_format import MediaTypeFormat



def rd_query(gateway):
       # print gc.collect()
    print "----------------------quey device start---------------------------------"
        
    print "-->query device with rd query param(query lwm2m device)"
    query = RDQueryParam()
    query.standard_type = "lwm2m"
    devices = gateway.do_device_query(query)  # a list contents device instance
    print "-->do device query get device list contents device="
    if devices is None:
        print "****************quey device type fail !!!******************************"
    else:
        for dev in devices:
            print dev.to_json()
                    
    print "-->query device with rd device id(query di=FAN-1)"
    query.device_id = "FAN-1"
    devices = gateway.do_device_query(query)
    if len(devices) != 1: 
        print "****************quey device id fail !!!*******************************"
    else:
        print devices[0].to_json()
    
    print "------------------------quey device type end-------------------------------"

    
def rd_handler(devices):
    print "in rd_handler"
    for device in devices:
        di = device.device_id 
        status = device.device_status
        print "rd handler process for di={}, status={}!".format(di, status)

def rd_monitor(gateway):
    print "----------------------rd monitor start---------------------------------" 
    query = RDQueryParam()
    query.device_id = "FAN-1" 
    print "-->add rd monitor with payload di=FAN-1 "
    monitor_id = gateway.add_rd_monitor(query, rd_handler)
    print "-->monitor id =" + str(monitor_id) 
    print ("sleep 120 seconds to wait device status changed!")
    time.sleep(120)    
    print "------------------------rd monitor end-------------------------------" 
    
    '''
    new_monitor_id = gateway.modify_rd_monitor(monitor_id,query,rd_handler)
    print "-->new monitor id =" +str(new_monitor_id)

    result = gateway.remove_rd_monitor(new_monitor_id)
    print "-->delete monitor result =" + str(result)
    '''
def get_resource_url(gateway):
    print "----------------------get resource by url start---------------------------------"
    print "-->  get devices  by url"
    uri_query = '/dev/gw1-m/10241'
    resource = gateway.do_resource_get(uri_query)
    if resource is None:
        print "****************get resource by url fail !!!*******************************"
    else: 
        print "--> get resource result by url:"  
        print resource.to_json()
    
    print "------------------------get resource by url  end-------------------------------"

def get_resource_instance(gateway):  
    print "----------------------get resource by instance start---------------------------------"
    dev = DeviceInfo()
    dev.device_id = 'gw1-m'
    res = Resource()
    res.device = dev
    res.href = '/10249/0'
    result = gateway.do_resource_get(res)
    if result is None:
        print "****************get resource by instance fail !!!*******************************"
    else:  
        print "--> get resource result by instance:"
        print result.to_json()
    print "------------------------get resource by instance  end-------------------------------"
    
def put_property_url(gateway): 
    
    print "-->do property put by url(lwm2m)"
    property_url = '/dev/FAN-1/30245/0/0'
    payload = "1"
    result = gateway.do_resource_property_put(property_url, payload)
    print "-->  write resource result:", result
    print "------------------------put resource by url  end-------------------------------"
    
        
def put_property_instance(gateway): 
    print "----------------------put property by instance start---------------------------------"
    dev = DeviceInfo()
    dev.device_id = 'FAN-2'
    res = Resource()
    res.device = dev
    res.href = '/30245/0/0'
    payload = "1"
    result = gateway.do_resource_property_put(res, payload)
    print "-->  write resource result:", result
    print "------------------------put resource by instance  end-------------------------------"
    
def put_resource_url(gateway): 
    print "----------------------put resource by url start---------------------------------"
    resource_url = '/dev/modbus_tcp_sanity_test/airflow/1'
    format = MediaTypeFormat.APPLICATION_JSON
    payload = '{"direction":"0","speed":"10"}' 
    # if payload is a json,format need to be json and keys must be match the orgin keys
    result = gateway.do_resource_put(resource_url, format, payload)
    print "-->  write resource result:", result
    
    print "----------------------put resource by url end---------------------------------"
    
    
def put_resource_instancde(gateway): 
    print "----------------------put resource by instance start---------------------------------"
    dev = DeviceInfo()
    dev.device_id = 'modbus_tcp_sanity_test'
    res = Resource()
    res.device = dev
    res.href = '/airflow/1'
    format = MediaTypeFormat.APPLICATION_JSON
    payload = '{"direction":"1","speed":"20"}'
    # if payload is a json,format need to be json and keys must be match the orgin keys
    result = gateway.do_resource_put(res, format, payload)
    print "-->  write resource result:", result
    
    print "----------------------put resource by instance end---------------------------------"
    

def on_data_listener(di, ri, data):
    print "refresh call back function di={}, ri={},data={}".format(di, ri, data.to_json())

def data_refesh(gateway): 
    print "----------------------data refresh start---------------------------------"

    device_id = 'TEMP-1'
    resource_uri = '/30242/0/0'
    print "-->device_id and resource_uri :", device_id, " ", resource_uri
    data_monitor_query = DataQueryParam(device_id, resource_uri)
    print "-->data_monitor_query"
    monitor_id = gateway.add_data_monitor(data_monitor_query, on_data_listener)

    print "-->  the monitor id is:", monitor_id
    print "-->  sleep 60 seconds to wait data change"
    time.sleep(60)
    
    print "-->  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"
    gateway.remove_data_monitor(monitor_id)
    
    print "-->  sleep 60 seconds to check if the data monitor has been deleted"
    time.sleep(60)

    
    print "----------------------data refresh end---------------------------------"

def start():
    print "-->iagent manager instance init"
    gateway = IAgentManager()
      
    rd_query(gateway)
 
    rd_monitor(gateway)

    get_resource_url(gateway)   

    get_resource_instance(gateway)
   
    put_property_url(gateway)
    
    put_property_instance(gateway)
    
    put_resource_url(gateway)
    
    put_resource_instancde(gateway)          
      

    
    Utils.stop_server()

    sys.exit(0)  

if __name__ == "__main__":
    start()
