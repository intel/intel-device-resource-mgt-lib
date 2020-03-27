var mqtt     = require('mqtt');
    logger   = require('../main/logger')
    mqtt_pub = mqtt.connect({host:'10.238.151.119',port:1883,username:'beihai',password:'intel@123'});

mqtt_pub.on('connect',(data)=> {
   logger.info('## Connect mqtt broker success!');
})

var num     = 0;
var message = '{"dt":"nqsw_dt","st":"iagent","di":"ning_0306","links":[{"rt":["flow"],"href":"/f"}],"ttl":3000,"status":"on"}';
//var message_=JSON.stringify(message);

setInterval(function(){
    mqtt_pub.publish('rd/test',message,{qos:0,retain:false});
    num++;
    console.log('-------> Publish client HELLO has sent '+num+' message');
},5000);