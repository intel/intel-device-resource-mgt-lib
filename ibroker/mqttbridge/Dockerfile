# set node v10 as base image
FROM node:10.15
# set work directory
WORKDIR /opt

COPY run.sh        /
# give authority to execute run.sh
RUN chmod 777 /run.sh
# copy mqttbridge file to /opt/mqttbridge
COPY main          ./mqttbridge/main
COPY test          ./mqttbridge/test
COPY node_modules  ./mqttbridge/node_modules
COPY README        ./mqttbridge/README
COPY package.json  ./mqttbridge

# set environment values
ENV RABBITMQ_ADDRESS=""
ENV ACTIVEMQ_ADDRESS=""
ENV RABBITMQ_USER=beihai \
RABBITMQ_PWD=intel@123 \
ACTIVEMQ_USER=admin \
ACTIVEMQ_PWD=admin

ENTRYPOINT ["/run.sh"]