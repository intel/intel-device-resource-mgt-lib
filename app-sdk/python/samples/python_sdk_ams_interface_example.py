import sys
# import json

from ams.ams_client import AmsClient
import time

def add_watch_point_event(localPath, targetType, targetID):
    print "hi this is add-watch-point callback localPath= {}, targetType= {}, targetID= {}".format(localPath, targetType, targetID)
    
def start():
    print "--> configure checkpoint"
    ams_client1 = AmsClient("iagent")
    ams_client1.add_checkpoint("device", "gw1")  # device id
    ams_client1.register_config_status(add_watch_point_event)
    print "sleep 120 seconds to wait config file change"

    time.sleep(120)
    ams_client1.stop()
    sys.exit(0) 

if __name__ == "__main__":
    start()
