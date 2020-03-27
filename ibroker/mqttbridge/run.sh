#!/bin/bash
echo "--> create the PropertiesConfig.js"
file="/opt/mqttbridge/main/PropertiesConfig.js"
echo "{
    const COMPONENT_NAME   = \"MqttBridge\";
    var   MESSAGE_NUMBER   = 0;
    const MQTT_SUB_ADDRESS = \"${RABBITMQ_ADDRESS}\";
    const AMQP_PUB_ADDRESS = \"${ACTIVEMQ_ADDRESS}\";
    const MQTT_USER=\"${RABBITMQ_USER}\";
    const MQTT_PWD=\"${RABBITMQ_PWD}\";
    const AMQP_USER=\"${ACTIVEMQ_USER}\";
    const AMQP_PWD=\"${ACTIVEMQ_PWD}\";
    module.exports.COMPONENT_NAME   = COMPONENT_NAME;
    module.exports.MESSAGE_NUMBER   = MESSAGE_NUMBER;
    module.exports.MQTT_SUB_ADDRESS = MQTT_SUB_ADDRESS;
    module.exports.AMQP_PUB_ADDRESS = AMQP_PUB_ADDRESS;
    module.exports.MQTT_USER = MQTT_USER;
    module.exports.MQTT_PWD = MQTT_PWD;
    module.exports.AMQP_USER = AMQP_USER;
    module.exports.AMQP_PWD = AMQP_PWD;
}" > $file

cat $file

echo "--> mqttbridge prepare to start ..."
node /opt/mqttbridge/main/Main.js


