'''
Created on Aug 23, 2017

@author: root
'''
import sys
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
monitor_id = None

def start():
    global monitor_id
    print "-->iagent manager sart monitor {}".format(TEST_DEVICE_ID_1)  
    data_monitor_query1 = DataQueryParam(TEST_DEVICE_ID_1, TEST_RESOURCE_ID_1, interval=20, sequence=6, process=True)
    monitor_id = gateway.add_data_monitor(data_monitor_query1, on_data_listener)
    
def on_data_listener(di, ri, data):
    temperature = float(data.get_raw_payload())
    print "linstener received data:{}".format(temperature) 
    if(temperature > 30.0): 
        temperature = 30.0
        print "updated the received data:{}".format(temperature)   
        data.set_raw_payload(str(temperature))
        return False
    return True

def stop_data_listener(id):
    result = gateway.remove_data_monitor(id)
    print "-->iagent manager sart monitor id {}, result:{}".format(monitor_id, result) 
    
if __name__ == "__main__":
    start()
    exit_code = raw_input("Please type stop to stop listerer:")
    if exit_code == "stop":
        stop_data_listener(monitor_id)
        gateway.stop() 
        sys.exit(0)
