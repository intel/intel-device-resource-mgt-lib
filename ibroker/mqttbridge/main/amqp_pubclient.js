/* ##################################################
   Abandoned casuse npm module-'amqp10' does not support reconnect 
   and will not be maintained anymore.
################################################### */




//var AMQPClient = require('amqp10').Client,
//     //Promise = require('bluebird');
//     Policy     = require('amqp10').Policy.ActiveMQ;  // Uses AMQP policy include how to classify the topic or queue url. bind with the protocol
//     client     = new AMQPClient(Policy);
//     logger     = require('./logger');
//     url_       = require('./PropertiesConfig');

// module.exports.createAmqpConnection = () => {
//     amqp_client = client.connect(url_.AMQP_PUB_ADDRESS, {'saslMechanism': 'ANONYMOUS'});

//     // triggered when amqp client connect success OR fail.
//     amqp_client.then(
//         (success)=>{
//         logger.info('## Connect ActiveMQ success.')
//     },
//         (error)=>{
//         logger.error('!! Connect ActiveMQ failed.',error)
//     });
//     return amqp_client;
// }

/*
! amqp_client is a promise instance, should follow the promise using principle

    ? @param amqp_client: return from amqp.connect
    ?              queue: the target queue

*/
// module.exports.amqp_publish = (amqp_client,queue,msg)=>{
//     amqp_client.then(()=>{
//         logger.info('amqp_client create Sender.');
//         return client.createSender('' + queue + '');

//     }).then((sender)=>{
//         logger.info('amqp_sender prepare to send message. \n msg: ',JSON.stringify(msg,undefined,2));
//         return sender.send(msg);

//     }).then(
//         ()=>{
//             logger.info('======================> amqp_sender send msg success.');   //promise resolve
//             logger.info('-----------------------------------------------------------')
//         },
//         (err)=> {
//         logger.error("error: ", err);// promise reject
//     });
// }


/*
TODO : now we don't need msg response from the broker, open following codes if necessary to realize the Send request & Receive response

// module.exports.amqp_PubNSub = (amqp_client,msg)=>{
//     amqp_client.then(()=>{
//         return Promise.all([
//             client.createReceiver('myres'), //set replyTo property in msg properties
//             client.createSender('/rd/'),
//         ]);

//     })
//     .spread(function (receiver, sender) {
//         receiver.on('errorReceived', function (err) {
//             logger.error('error: ' + err)
//         });
//         receiver.on('message', function (message) {
//             logger.info(message);
//             logger.info('Rx message: ' + JSON.stringify(message, undefined, 2));
//         });

//         return sender.send(msg);
//     })
//     .error(function (err) {
//         console.log("error: ", err);
//     });
// }
*/