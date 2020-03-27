var log4js = require('log4js');

log4js.configure({
  appenders: {
    console:{
        type: 'console'
    },
    bridgeLogs:{
        type: 'file', filename: '../logs/MQTT_bridge.log', category: 'MQTT_bridge'
    }
  },

  categories: {
        default: {
            appenders: [
                'console',
                'bridgeLogs'
            ],
            level: 'info'}

    }
});

var logger = log4js.getLogger('MQTT_bridge');

module.exports = logger;
