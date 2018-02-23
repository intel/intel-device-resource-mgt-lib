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
gateway = IAgentManager()
def start():
    print "-->iagent manager instance init and send rd request"  
    data_monitor_query1 = DataQueryParam(TEST_DEVICE_ID_1, TEST_RESOURCE_ID_1, interval=20, sequence=4, process=True)
    monitor_id = gateway.add_data_monitor(data_monitor_query1, on_data_listener)   

def on_data_listener(di, ri, data):
    temperature = float(data.get_raw_payload())
    print "linstener received data:{}".format(temperature) 
    temperature = temperature + 2.0;
    if(temperature > 50.0): 
        temperature = 50.0
    
    print "updated the received data:{}".format(temperature)   
    data.set_raw_payload(str(temperature))
    return True
        
if __name__ == "__main__":
    start()
