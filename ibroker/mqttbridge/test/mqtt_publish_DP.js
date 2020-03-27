var mqtt     = require('mqtt');
    logger   = require('../main/logger');
    mqtt_pub = mqtt.connect({host:'10.238.151.119',port:1883,username:'beihai',password:'intel@123'});

mqtt_pub.on('connect',(data)=> {
   logger.info('## Connect mqtt broker success!');
})

var num     = 0;
var message = '{"e":[{"t":0,"v":22,"n":"n"}],"bn":"/f/1"},"deviceId":"test_di_0305"}';

//var message_=JSON.stringify(message);
setInterval(function(){
    mqtt_pub.publish('/dp/j/test_di_0305',message,{qos:1,retain:false});
    num++;
    console.log('-------> Publish client HELLO has sent '+num+' message');
},5000);