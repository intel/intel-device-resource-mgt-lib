var logger   = require('./logger')
    trans    = require('./mqtt_amqp_trans')
    amqp_pub = require('./amqp_pubclient_rhea')
    mqtt_sub = require('./mqtt_subclient')
    events   = require('events');

/**
 * create mqtt & amqp connection
 */
var mqtt_client = mqtt_sub.createMQTTConnectionAndInit();
var amqp_client = amqp_pub.createAmqpConnectionAndInit();

/**
 * invoke the message_listener in mqtt_subclient.js
 *    callback function will be triggered inside the msg_listener function
 * TODO: when AMQP server down while MQTT subscriber still receives the msgs, may receive too much msgs but can't publish to ActiveMQ before restarting the ActiveMQ service.
 */
mqtt_sub.msg_listener(mqtt_client,
    (topic,payload) =>{ //callback function
        switch(topic){
            case 'rd':
                amqp_pub.amqp_publish(amqp_client, '/rd', trans.get_amqp_message('rd', payload));
                break;
            //case 'dp':
                //amqp_pub.amqp_publish(amqp_client, '/dp', trans.get_amqp_message('dp', payload));
                //break;
            default:
                logger.warn('There is no subscriber.')
        }
        return;
});