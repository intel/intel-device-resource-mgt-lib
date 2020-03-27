var logger       = require('./logger');
    propty_cfg   = require('./PropertiesConfig');

var amqp_msg_obj = {
    "durable":"",
    "correlation_id":"",
    "application_properties":
    {
        "payload": ""
    },
    "body": {
    }
};

/**
 * @param {*} dst 
 * @param {*} key
 * @param {*} value
 */
function add_elements(dst, _KEY_, _VALUE_) {
    switch (dst) {
        case 'payload_rd':
            amqp_msg_obj['application_properties']['' + _KEY_ + ''] = "["+_VALUE_+"]";
            break;
        // case 'payload_dp':
        //     amqp_msg_obj['application_properties']['' + _KEY_ + ''] = _VALUE_;
        //     break;
        case 'application_properties':
            amqp_msg_obj['' + dst + '']['' + _KEY_ + ''] = _VALUE_;
            break;
        default:
            amqp_msg_obj['' + _KEY_ + ''] = _VALUE_;
    }
}

/**
 *receive the data from mqtt,
 *   set the value for "body:{}" of the amqp message
 * @param {*} topic
 *             subscribe msg topic from the mqtt broker
 * @param {*} body
 *
 * @returns
 */
function get_amqp_message(topic,body) {
    logger.info('==> MQTT message transform to AMQP start.');

    add_elements("durable","durable",true);
    //add_elements("reply_to", 'myres');
    add_elements("correlation_id","correlation_id", ""+ propty_cfg.COMPONENT_NAME +"-messageId-"+ (++propty_cfg.MESSAGE_NUMBER) +"");
    add_elements("application_properties", "action", 'POST');
    add_elements("application_properties", "uri", 'jms:/'+topic+'');
    add_elements("application_properties", "payload_format", 'application/json');
     // rd and dp payload have different payload format
     if(topic=="rd"){
        add_elements("payload_rd", "payload", body);
     }
    //  else if(topic == "dp"){
    //     add_elements("payload_dp", "payload", body);
    //  };

    logger.info('<== MQTT message transform to AMQP finish.');

    return amqp_msg_obj;
}


//console.log(JSON.stringify(get_amqp_message({}), undefined, 2));

module.exports = {
    get_amqp_message
}