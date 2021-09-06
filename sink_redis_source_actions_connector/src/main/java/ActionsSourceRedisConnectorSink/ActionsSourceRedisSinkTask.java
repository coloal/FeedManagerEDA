/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ActionsSourceRedisConnectorSink;

import static ActionsSourceRedisConnectorSink.ActionsSourceRedisSinkConnector.N_MAP_SOURCES;
import static ActionsSourceRedisConnectorSink.ActionsSourceRedisSinkConnector.TOPIC_CONFIG_PROPERTY;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.exceptions.JedisException;
import static ActionsSourceRedisConnectorSink.ActionsSourceRedisSinkConnector.SOURCE_REDIS_CONNECTION_CONFIG_PROPERTY;
import static ActionsSourceRedisConnectorSink.ActionsSourceRedisSinkConnector.SOURCE_REDIS_PASSWORD_CONFIG_PROPERTY;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
/**
 *
 * @author alberto
 */
public class ActionsSourceRedisSinkTask extends SinkTask{
    static final Logger log = LoggerFactory.getLogger(ActionsSourceRedisSinkTask.class);
    
    private String bootstrapServers;
    private String result_topic;
    private KafkaProducer<String, String> kafka_producer;
    private String topic;
    private String mapSource_id;
    private String source_redis_connection;
    private String nMapSources;
    
    private ActionsSourceRedisProvider redisProvider;
   
    private Jedis jedisSourceConn;
    
    @Override
    public void start(Map<String, String> props) {
        //log.info("[BEGIN] START---------------------------");
        //new
        result_topic = "offsets-topic";//Has to be added via props
        bootstrapServers = "my-kafka-cluster-kafka-bootstrap.kafka-cluster.svc.cluster.local:9092";
        
        Properties resultProps = new Properties();
        resultProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        resultProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        resultProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        kafka_producer = new KafkaProducer<String, String>(resultProps);
        
        //end of new
        
        topic = props.get(TOPIC_CONFIG_PROPERTY);
        mapSource_id = props.get("MapSource_id");
        nMapSources = props.get(N_MAP_SOURCES);
        source_redis_connection = props.get(SOURCE_REDIS_CONNECTION_CONFIG_PROPERTY);
        String source_redis_pass = props.get(SOURCE_REDIS_PASSWORD_CONFIG_PROPERTY);
        
        log.info("***********************************************************");
        log.info("* TOPIC: " + topic);
        log.info("* SOURCE_REDIS_CONNECTION: " + source_redis_connection);
        log.info("* SOURCE_REDIS_PASS: " + source_redis_pass);
        log.info("***********************************************************");
        
        try{
            JedisShardInfo sourceShardInfo = new JedisShardInfo(source_redis_connection);
            sourceShardInfo.setPassword(source_redis_pass);
            jedisSourceConn = new Jedis(sourceShardInfo);
            
            redisProvider = new ActionsSourceRedisProvider(jedisSourceConn);
        }catch(JedisException e)
        {
            log.error("**************JEDIS CONNECTION*************");
            log.error(e.getMessage());
            log.error("**************JEDIS CONNECTION*************");
        }finally{
            jedisSourceConn.close();
        }
        //log.info("[END] START---------------------------");
    }
    
    @Override
    public void put(Collection<SinkRecord> records) {
        log.info("[BEGIN] PUT SOURCE---------------------------");
        for(SinkRecord record : records){
            log.info("[BEGIN]Processing record with key: " + (String)record.key() + "-------------------------------");
            
            String key = (String)record.key();
            HashMap<String, String> value = (HashMap)record.value();
            log.info("Url: " + value.get("Url"));
            log.info("Description: " + value.get("Description"));
            log.info("Title: " + value.get("Title"));
            log.info("DateOfCreation: " + value.get("DateOfCreation"));
            
            if(key == null)
                throw new DataException("The key for the record cannot be null. ");
            
            if(value.get("Description") == null || value.get("Title") == null ||
               value.get("DateOfCreation") == null || value.get("Url") == null ||
               value.get("Url").equals(""))//hay que añadir una condición que verifique que la url es de un rss y accesible.  
                log.warn("sourceNew event lacks information.");
            else{
                switch((String)value.get("type")){
                case "sourceNew":        
                    try{
                        value.remove("type");//left the information required to create Source
                        Long response = redisProvider.createSource(value, Integer.parseInt(nMapSources), mapSource_id);
                        log.info("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ newSource response="+response+"]]]]]]]]]]]]]]]]]]]]]]]]]]]]]");
                        
                        if(response==1)
                        {
                            ProducerRecord<String, String> result_record 
                                    = new ProducerRecord<String, String>(result_topic, value.get("Url"), 
                                    "{\"" + value.get("Url") + "\":\"{}\"}");
                            kafka_producer.send(result_record);
                        }
                        
                    }catch(JedisException e)
                    {
                        log.error(e.getMessage());
                    }finally{//WHAT IF CONNECTION IS CLOSED, SHOULD I TRY TO RECONNECT WITHOUT RESTARTING TASK(RECALLING START)?
                        jedisSourceConn.close();
                    }
                    
                    
                        
                    break;
                                     
                default:
                    break;
            }
            }
            
          
            log.info("[END]Processing record with key: " + (String)record.key() + "-------------------------------");
            log.info("-");
            
        }
        log.info("[END] PUT----------------------");
        log.info("-");
        log.info("-");
        log.info("-");
    }

    @Override
    public void stop() {
        jedisSourceConn.close();
    }

    @Override
    public String version() {
        return VersionUtil.getVersion();
    }
    
}
