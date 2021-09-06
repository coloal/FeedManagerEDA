 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SourceRedisConnectorSink;

import static SourceRedisConnectorSink.SourceRedisSinkConnector.REDIS_CONNECTION_CONFIG_PROPERTY;
import static SourceRedisConnectorSink.SourceRedisSinkConnector.REDIS_PASSWORD_CONFIG_PROPERTY;
import static SourceRedisConnectorSink.SourceRedisSinkConnector.TOPIC_CONFIG_PROPERTY;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;

import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.exceptions.JedisException;

import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 *
 * @author alberto
 */
public class SourceRedisSinkTask extends SinkTask{
    static final Logger log = LoggerFactory.getLogger(SourceRedisSinkTask.class);
    
    /*Result Producer attributes*/
    private String bootstrapServers;
    private String result_topic;
    private KafkaProducer<String, Event_AddItem2Source> kafka_producer;
    /*End of Result Producer attributes*/
    
    private String topic;
    private String redis_connection;
    
    private SourceRedisProvider redisProvider;
    private Jedis jedisConn;
    
    @Override
    public void start(Map<String, String> props) {
        log.info("[BEGIN] START---------------------------");
        /* Result Producer start*/
        result_topic = "result-item2source-topic";//Has to be added via props
        bootstrapServers = "my-kafka-cluster-kafka-bootstrap.kafka-cluster.svc.cluster.local:9092";
        
        Properties resultProps = new Properties();
        resultProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        resultProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        resultProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        
        kafka_producer = new KafkaProducer<String, Event_AddItem2Source>(resultProps);
        /* End of Result Producer start*/
        
        
        topic = props.get(TOPIC_CONFIG_PROPERTY);
        redis_connection = props.get(REDIS_CONNECTION_CONFIG_PROPERTY);
        String redis_pass = props.get(REDIS_PASSWORD_CONFIG_PROPERTY);
        log.info("***********************************************************");
        log.info("* TOPIC: " + topic);
        log.info("* REDIS_CONNECTION: " + redis_connection);
        log.info("* REDIS_PASS: " + redis_pass);
        log.info("***********************************************************");
        
        try{
            JedisShardInfo shardInfo = new JedisShardInfo(redis_connection);
            shardInfo.setPassword(redis_pass);
            jedisConn = new Jedis(shardInfo);
            redisProvider = new SourceRedisProvider(jedisConn);
        }catch(JedisException e)
        {
            log.error("**************JEDIS CONNECTION*************");
            log.error(e.getMessage());
            log.error("**************JEDIS CONNECTION*************");
        }finally{
            jedisConn.close();
        }
        log.info("[END] START---------------------------");
    }
    
    @Override
    public void put(Collection<SinkRecord> records) {
        log.info("[BEGIN] PUT SOURCE---------------------------");
        log.info("Number of records recieved=" + records.size());
        for(SinkRecord record : records){
            log.info("[BEGIN]Processing record with key: " + (String)record.key() + "-------------------------------");
            
            Schema valueSchema = record.valueSchema();
            Schema keySchema = record.keySchema();
            
            if(record.key() == null)
                log.error("The key for the record cannot be null. ");
            else{
                if(keySchema != null)
                {
                    log.info("********************KeySchema not null***************************");
                    log.info("<<<<<<<<<<<<<<<<<<" + valueSchema.name() + ">>>>>>>>>>>>>>>>>>>>>>>");
                    log.info("<<<<<<<<<<<<<<<<<<" + keySchema.type() + ">>>>>>>>>>>>>>>>>>>>>>>");
                    
                    if("ItemRecord".equals(valueSchema.name()) &&
                            keySchema.type() == Schema.Type.STRING)
                    {
                        log.info("********************doing things in redis***************************");
                        String keyRecord = (String)record.key();
                        Struct valueRecord = (Struct)record.value();
                        log.info("URL passed to redis(keyRecord): "+ keyRecord);

                        List<Field> listFields = valueSchema.fields();
                        Map<String, String> mapItem = new HashMap<>();
                        Map<String, String> response = new HashMap<>();

                        if(listFields.size() == 4)//Title, Description and Link are mandatory so they should always be in the record, and date is mandatory for us so if it is not specified it won't be processsed. We should include some where in the topology a logic that if date does not exists it sets in date a timestamp
                        {
                            log.info("Before FOR");
                            for (Field field : listFields) {
                                log.info("Key: " + field.name() + " value: " + valueRecord.getString(field.name()));
                                mapItem.put(field.name(), valueRecord.getString(field.name()));
                            }
                            //log.info("After FOR");

                            log.info("Before postItem");
                            try{
                                response = redisProvider.postItem2SourceFeed(keyRecord, mapItem);
                            }catch(JedisException e)
                            {
                                log.error("Error with Jedis when posting item", e);
                            }finally{//WHAT IF CONNECTION IS CLOSED, SHOULD I TRY TO RECONNECT WITHOUT RESTARTING TASK(RECALLING START)?
                                jedisConn.close();
                            }
                            //log.info("After postItem");

                            log.info("***********RESPONSE "+ response + "********************************");
                            if(response == null)
                            {
                                log.info("The url feed specified in the key (\"" + keyRecord + "\") of the record does not exist in the Redis database, record ignored");
                            }
                            else
                            {
                                log.info("*+*+*+*+*+*+*+*+*+*+*+*+*+*+ Begin kafka producer *+*+*+*+*+*+*+*+*+*+*+*+*+*+");

                                String SourceFeedId = response.get("sourceFeed_id");
                                String ItemId = response.get("item_id");
                                String pubDate = response.get("pubDate");
                                //log.info("The item was added to " + SourceFeedId + " SourceFeed");
                                //String todaviaNoSeQuePoner = "holi";// el evento debería ser escrito en todas las particiones para luego que cada task se encargue de producir en su correspondiente ConsumerFeed Redis DB. O que todas las tasks lean de la misma partición pero eso no se puede hacer con un Connector.
                                Event_AddItem2Source eventResult = new Event_AddItem2Source(ItemId, SourceFeedId, pubDate, "itemAdded2Source");
                                //String value = ItemId + " added to " + SourceFeedId;
                                ProducerRecord<String, Event_AddItem2Source> result_record 
                                        = new ProducerRecord<String, Event_AddItem2Source>(result_topic, SourceFeedId, eventResult);
                                kafka_producer.send(result_record);

                                log.info("*+*+*+*+*+*+*+*+*+*+*+*+*+*+ End kafka producer *+*+*+*+*+*+*+*+*+*+*+*+*+*+");
                            }
                        }
                    }   
                }else{
                    log.error("The key schema for the record cannot be null. "); 
                }
            }
            
            log.info("[END]Processing record with key: " + (String)record.key() + "-------------------------------");
            //log.info("-");
            
        }
        log.info("[END] PUT SOURCE----------------------");
        //log.info("-");
        //log.info("-");
        //log.info("-");
        
    }

    @Override
    public void stop() {
        if(jedisConn != null)
            jedisConn.close();
    }

    @Override
    public String version() {
        return SourceVersionUtil.getVersion();
    }
    
}
