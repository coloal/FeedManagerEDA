/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SourceRssConnector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.source.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 *
 * @author alberto
 */
public class KafkaConsumerProvider {
    static final Logger log = LoggerFactory.getLogger(KafkaConsumerProvider.class);
    
    private int partition;
    private static String TOPIC;
    private static String BOOTSTRAP_SERVERS;
    private final KafkaConsumer<String, String> kafkaConsumer;
    private final KafkaProducer<String, String> kafkaProducer;
    KafkaConsumerProvider(String bS, String topic, int p)
    {
        partition = p;
        BOOTSTRAP_SERVERS = bS;
        TOPIC = topic;
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "OffsetReader");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        
        kafkaConsumer = new KafkaConsumer<>(props);
        //kafkaConsumer.subscribe(Collections.singletonList(TOPIC));
        log.info("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&PARTITION ASSIGNED TO PROVIDER KAFKA " + partition);
        kafkaConsumer.assign(Arrays.asList(new TopicPartition(TOPIC, partition)));
        
        Properties resultProps = new Properties();
        resultProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        resultProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        resultProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        kafkaProducer = new KafkaProducer<String, String>(resultProps);
    }
    
    void produceOffsets(Map<String, String> offsets)
    {
        for (Map.Entry<String, String> offset : offsets.entrySet()) {
            kafkaProducer.send(new ProducerRecord<String, String>(TOPIC, partition,
                    offset.getKey(), offset.getValue()));
        }
    }
    
    //Remember to commit processed records
    ConsumerRecords<String, String> getOffsets(){
        return kafkaConsumer.poll(3);//Wait, as much, 3 secs to get records
    }
    
    ConsumerRecords<String, String> getOffsetsFromTheBeginning(){
        kafkaConsumer.seekToBeginning(kafkaConsumer.assignment());
        return kafkaConsumer.poll(1000);//Wait, as much, 3 secs to get records
    }
    
}
