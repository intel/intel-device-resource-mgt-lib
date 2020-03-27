# Set Environment
### 1. install node & npm
### 2. install npm modules 
+ in root folder:
  + npm install --save **mqtt**
  + npm install --save **rhea**
  + npm install --save **log4js**

# How to start
+ in /main/ folder:
  + node **Main**.js

# Test
+ In /test folder: 
  1. node Main.js
  2. node mqtt_publish_RD.js

# Key npm modules
 1. '**mqtt**' : subscribe & publish for the MQTT broker,like RabbitMQ and ActiveMQ. Default port:1883
 2. '**rhea**': support **AMQP-0.10.0** version, can send & receive from the ActiveMQ. Support reconnect to the broker.

# Log
 + log to both console and log_file
 + log file is in /logs/MQTT_bridge.log

# Workflow
  1. Create MQTT client and subscribe the topic.
  2. Trigger the message re-format from MQTT to AMQP when subscribed messages coming.
  3. Publish the AMQP message to the queue.