var container = require('rhea');
    logger    = require('./logger');
    url       = require('./PropertiesConfig');

module.exports.createAmqpConnectionAndInit = () => {
    var connection = container.connect({
        host:url.AMQP_PUB_ADDRESS,
        port:5672,
        username:url.AMQP_USER,
        password:url.AMQP_PWD,
        reconnect:10000, //attempt reconnect every 10 seconds
        reconnect_limit:10 //max attempt times
    });
    //raised when connecting success
    connection.on('connection_open',()=>{
        logger.info('## Connect AMQP successfully!');
    });
    //raised when losing connection
    connection.on('disconnected',(details)=>{
        logger.warn('## Lost connection to AMQP.');
    });
    return connection;
}

module.exports.amqp_publish = (amqp_client,queue,msg)=>{
    var sender = amqp_client.open_sender('' + queue + '');
    sender.on('sendable',(context) => {
        logger.info('AMQP-sender prepare to send message. \n msg: ',JSON.stringify(msg,undefined,2));
        sender.send(msg);
    });
    sender.on('accepted',(context) => {
        logger.info('## AMQP has accepted the msg.');
    });
    sender.on('settled',(context) => {
        logger.info('## AMQP has settled the msg.');
        logger.info('======================> AMQP-client sends the message.');
        logger.info('-----------------------------------------------------------');
    });
}