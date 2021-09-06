FROM quay.io/strimzi/kafka:0.24.0-kafka-2.8.0
USER root:root
RUN mkdir /opt/kafka/plugins/
#COPY kaliy-kafka-connect-rss-0.1.0/kaliy-kafka-connect-rss-0.1.0 /opt/kafka/plugins/kaliy-kafka-connect-rss-0.1.0
COPY *.jar /opt/kafka/plugins/my-connectors/
#Only include in developing image in order to change .jar connector in Kafka connect container
#RUN chmod -R a+rwX /opt/kafka/plugins/
USER 1001
