var logger = require('./logger')
    mqtt   = require('mqtt');
    url    = require('./PropertiesConfig');

/*
mqtt: //ip_address:1883
        1883 is the MQTTBroker port,
            server port setting:
                        61616 for ActiveMQ openwire
                        5672  for ActiveMQ amqp
                        1883  for RabbitMQ\ActiveMQ mqtt
*/
module.exports.createMQTTConnectionAndInit = () => {

    var mqtt_client = mqtt.connect({
        host: url.MQTT_SUB_ADDRESS,
        port: 1883,
        username: url.MQTT_USER,
        password: url.MQTT_PWD,
        reconnectPeriod: 10000
    });

    // triggered when mqtt client connect success
    mqtt_client.on('connect',() => {
        logger.info('## Connect RabbitMQ success');
    })

    // triggered when mqtt client connect failed
    mqtt_client.on('error',(error) => {
        logger.info('## Connect RabbitMQ failed',error);
    })

    // triggered when mqtt client attempt to reconnect
    mqtt_client.on('reconnect',() => {
        logger.info('## Attempt to reconnect RabbitMQ');
    });

    return mqtt_client;
}

module.exports.msg_listener = (mqtt_client, _callback) => {
    /*
    match principle:
                     rd/# : receive all messages start with rd/, including rd/a , rd/a/b, etc.
                     rd/+ : only receive messages rd/a
    */
    mqtt_client.subscribe('rd/#');
    //mqtt_client.subscribe('dp/#');

    /*
    trigger when receiving the msg, invoke callback function defined in Main.js
    */
    mqtt_client.on('message', function (topic, payload, packet) {
        logger.info('\n\n\n\n\n\n\n');
        logger.info('-----------------------------------------------------------');
        logger.info('<====================== mqtt-client receives the message.');
        logger.info('Message payload is :', payload.toString());
        var topic = packet.topic.toString();
        var url_topic= (topic.search("rd")!=-1) ? "rd" : (topic.search("dp")!=-1?"dp":" ");
        var _payload = payload.toString();
        try {
            _callback(url_topic, _payload);
        } catch (e) {
            logger.warn('Meet error  : ', e);
            return;
        }
    });
}
