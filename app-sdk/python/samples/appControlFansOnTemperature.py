'''
Created on Aug 23, 2017

@author: root
'''
import sys
import json
from iagent.framework.iagent_manager import IAgentManager
from iagent.model.data_query_param import DataQueryParam
from iagent.model.device import DeviceInfo
from iagent.model.resource import Resource
from ams_utils.media_type_format import MediaTypeFormat
from ams_utils.utils import Utils


TEST_DEVICE_ID_1 = "TEMP-1"
TEST_RESOURCE_ID_1 = "/30242/0/0"
FAN_RESOURCE_ID_1 = "/30245/0/0"
gateway = IAgentManager()
def start():
    print "-->iagent manager instance init and send rd request"  
    data_monitor_query1 = DataQueryParam(TEST_DEVICE_ID_1, TEST_RESOURCE_ID_1, interval=20, sequence=2, process=True)
    monitor_id = gateway.add_data_monitor(data_monitor_query1, on_data_listener)   

def on_data_listener(di, ri, data):
    temperature = float(data.get_raw_payload())
    enabledFansNum = 3 if temperature >= 40.0 else (2 if temperature >= 30.0 else (1 if temperature >= 20.0 else 0)) 
    print "linstener received data:{}, fan num:{}".format(temperature, enabledFansNum) 
    enbleFan(enabledFansNum)
    return True
    
def enbleFan(num): 
    dev = DeviceInfo()
    for i in range(1, 4): 
        dev.device_id = "FAN-{}".format(i)
        res = Resource()
        res.device = dev
        res.href = FAN_RESOURCE_ID_1
        format = MediaTypeFormat.TEXT_PLAIN
        payload = "1" if i <= num else "0"
        print "set payload: {} for device: {}".format(payload, dev.device_id)   
        result = gateway.do_resource_put(res, format, payload)    

if __name__ == "__main__":
    start()
