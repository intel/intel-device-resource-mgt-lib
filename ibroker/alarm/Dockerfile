FROM openjdk:8-jdk-alpine

ARG JAVA_OPTS="--spring.jmx.enabled=false -noverify -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap"
ARG JAR_FILE=./target/alarm-0.0.1.jar

COPY ${JAR_FILE} /app/app.jar

WORKDIR ["/app"]

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
