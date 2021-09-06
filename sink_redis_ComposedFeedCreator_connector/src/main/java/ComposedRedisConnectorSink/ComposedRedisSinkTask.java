/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ComposedRedisConnectorSink;

import static ComposedRedisConnectorSink.ComposedSinkConnector.REDIS_CONNECTION_CONFIG_PROPERTY;
import static ComposedRedisConnectorSink.ComposedSinkConnector.REDIS_PASSWORD_CONFIG_PROPERTY;
import static ComposedRedisConnectorSink.ComposedSinkConnector.TOPIC_CONFIG_PROPERTY;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.exceptions.JedisException;

/**
 *
 * @author alberto
 */
public class ComposedRedisSinkTask extends SinkTask{
    static final Logger log = LoggerFactory.getLogger(ComposedRedisSinkTask.class);
    
    private String topic;
    private String redis_connection;
    
    private ComposedRedisProvider redisProvider;
    private Jedis jedisConn;
    
    @Override
    public void start(Map<String, String> props) {
        //log.info("[BEGIN] START---------------------------");
        
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
            redisProvider = new ComposedRedisProvider(jedisConn);
        }catch(JedisException e)
        {
            log.error("**************JEDIS CONNECTION*************");
            log.error(e.getMessage());
            log.error("**************JEDIS CONNECTION*************");
        }finally{
            jedisConn.close();
        }
        //log.info("[END] START---------------------------");
    }
    
    @Override
    public void put(Collection<SinkRecord> records) {
        log.info("[BEGIN] PUT COMPOSED---------------------------");
        for(SinkRecord record : records){
            log.info("[BEGIN]Processing record with key: " + (String)record.key() + "-------------------------------");
            
            Schema valueSchema = record.valueSchema();
            Schema keySchema = record.keySchema();
            
            String key = (String)record.key();
            HashMap<String, String> value = (HashMap)record.value();
            
            
            
            if(key == null)
                throw new DataException("The key for the record cannot be null. ");
            
            //if(keySchema != null)
            //{
                //log.info("********************KeySchema not null***************************");
                //log.info("<<<<<<<<<<<<<<<<<< keySchema:" + valueSchema.name() + ">>>>>>>>>>>>>>>>>>>>>>>");
                //log.info("<<<<<<<<<<<<<<<<<< valueSchema:" + keySchema.type() + ">>>>>>>>>>>>>>>>>>>>>>>");
                log.info("+++++++++++++++ type="+ value.get("type") + ", itemId=" + value.get("sourceId") + ", itemId=" + value.get("itemId") + "++++++++++++++++");
                if(value.get("type").equals("itemAdded2Source"))
                {
                    log.info("********************doing things in redis***************************");
                    //String keyRecord = (String)record.key();
                    //Struct valueRecord = (Struct)record.value();
                    log.info("KEY: "+ key);
                    
                    String sourceId = value.get("sourceId");              
                    String itemId = value.get("itemId");
                    Long pubDate = Long.valueOf(value.get("pubDate"));
                    log.info("&&&&&&&&&&&&&&&&&&&&&& Before referenceItemInComposedFeeds");
                    try{
                        redisProvider.referenceItemInComposedFeeds(sourceId, itemId, pubDate);
                    }catch(JedisException e)
                    {
                        log.error(e.getMessage());
                    }finally{//WHAT IF CONNECTION IS CLOSED, SHOULD I TRY TO RECONNECT WITHOUT RESTARTING TASK(RECALLING START)?
                        jedisConn.close();
                    }
                    log.info("&&&&&&&&&&&&&&&7 After referenceItemInComposedFeeds");
                }   
            //}else{
             //   throw new DataException("The key schema for the record cannot be null. "); 
            //}
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
        if(jedisConn != null)
            jedisConn.close();
    }

    @Override
    public String version() {
        return ComposedVersionUtil.getVersion();
    }
    
}
