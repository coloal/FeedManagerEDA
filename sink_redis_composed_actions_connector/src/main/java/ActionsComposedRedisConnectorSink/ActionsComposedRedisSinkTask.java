/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ActionsComposedRedisConnectorSink;

import static ActionsComposedRedisConnectorSink.ActionsComposedRedisSinkConnector.TOPIC_CONFIG_PROPERTY;
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
import static ActionsComposedRedisConnectorSink.ActionsComposedRedisSinkConnector.COMPOSED_REDIS_CONNECTION_CONFIG_PROPERTY;
import static ActionsComposedRedisConnectorSink.ActionsComposedRedisSinkConnector.SOURCE_REDIS_CONNECTION_CONFIG_PROPERTY;
import static ActionsComposedRedisConnectorSink.ActionsComposedRedisSinkConnector.COMPOSED_REDIS_PASSWORD_CONFIG_PROPERTY;
import static ActionsComposedRedisConnectorSink.ActionsComposedRedisSinkConnector.SOURCE_REDIS_PASSWORD_CONFIG_PROPERTY;
/**
 *
 * @author alberto
 */
public class ActionsComposedRedisSinkTask extends SinkTask{
    static final Logger log = LoggerFactory.getLogger(ActionsComposedRedisSinkTask.class);
    
    private String topic;
    private String composed_redis_connection;
    private String source_redis_connection;
    
    private ActionsComposedRedisProvider redisProvider;
    
    private Jedis jedisComposedConn;
    private Jedis jedisSourceConn;
    
    @Override
    public void start(Map<String, String> props) {
        //log.info("[BEGIN] START---------------------------");
        topic = props.get(TOPIC_CONFIG_PROPERTY);
        composed_redis_connection = props.get(COMPOSED_REDIS_CONNECTION_CONFIG_PROPERTY);
        String composed_redis_pass = props.get(COMPOSED_REDIS_PASSWORD_CONFIG_PROPERTY);
        
        source_redis_connection = props.get(SOURCE_REDIS_CONNECTION_CONFIG_PROPERTY);
        String source_redis_pass = props.get(SOURCE_REDIS_PASSWORD_CONFIG_PROPERTY);
        log.info("***********************************************************");
        log.info("* TOPIC: " + topic);
        log.info("* COMPOSED_REDIS_CONNECTION: " + composed_redis_connection);
        log.info("* COMPOSED_REDIS_PASS: " + composed_redis_pass);
        log.info("* SOURCE_REDIS_CONNECTION: " + source_redis_connection);
        log.info("* SOURCE_REDIS_PASS: " + source_redis_pass);
        log.info("***********************************************************");
        
        try{
            JedisShardInfo composedShardInfo = new JedisShardInfo(composed_redis_connection);
            composedShardInfo.setPassword(composed_redis_pass);
            jedisComposedConn = new Jedis(composedShardInfo);
            
            JedisShardInfo sourceShardInfo = new JedisShardInfo(source_redis_connection);
            sourceShardInfo.setPassword(source_redis_pass);
            jedisSourceConn = new Jedis(sourceShardInfo);
            
            redisProvider = new ActionsComposedRedisProvider(jedisComposedConn, jedisSourceConn);
        }catch(JedisException e)
        {
            log.error("**************JEDIS CONNECTION*************");
            log.error(e.getMessage());
            log.error("**************JEDIS CONNECTION*************");
        }finally{
            jedisComposedConn.close();
            jedisSourceConn.close();
        }
        //log.info("[END] START---------------------------");
    }
    
    @Override
    public void put(Collection<SinkRecord> records) {
        log.info("[BEGIN] PUT COMPOSED---------------------------");
        for(SinkRecord record : records){
            log.info("[BEGIN]Processing record with key: " + (String)record.key() + "-------------------------------");
            
            String key = (String)record.key();
            HashMap<String, Object> value = (HashMap)record.value();
                       
            if(key == null)
                throw new DataException("The key for the record cannot be null. ");
            
            String sourceId;              
            String composedId;
            String username;
            long timestamp = (long)value.get("timestamp");      
            
            switch((String)value.get("type")){
                case "sourceFollowedByComposed":
                    sourceId = (String)value.get("sourceId");
                    composedId = (String)value.get("composedId");
                    try{
                        Long response = redisProvider.addComposed2SourceFollowers(sourceId, composedId, timestamp);
                        log.info("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ sourceFollowedByComposed response="+response+"]]]]]]]]]]]]]]]]]]]]]]]]]]]]]");
                    }catch(JedisException e)
                    {
                        log.error(e.getMessage());
                    }finally{//WHAT IF CONNECTION IS CLOSED, SHOULD I TRY TO RECONNECT WITHOUT RESTARTING TASK(RECALLING START)?
                        jedisComposedConn.close();
                        jedisSourceConn.close();
                    }
                    
                    break;
                    
                case "sourceUnfollowedByComposed":
                    sourceId = (String)value.get("sourceId");
                    composedId = (String)value.get("composedId");
                    try{
                        Long response = redisProvider.deleteComposedFromSourceFollowers(sourceId, composedId, timestamp);
                        log.info("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ sourceUnfollowedByComposed response="+response+"]]]]]]]]]]]]]]]]]]]]]]]]]]]]]");
                    }catch(JedisException e)
                    {
                        log.error(e.getMessage());
                    }finally{//WHAT IF CONNECTION IS CLOSED, SHOULD I TRY TO RECONNECT WITHOUT RESTARTING TASK(RECALLING START)?
                        jedisComposedConn.close();
                        jedisSourceConn.close();
                    }
                    
                    break;
                
                case "composedAddAdmin":
                    username = (String)value.get("username");              
                    composedId = (String)value.get("composedId");
                    try{
                        Long response = redisProvider.addUser2ComposedAdmins(username, composedId);
                        log.info("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ composedUserAddedAsAdmin response="+response+"]]]]]]]]]]]]]]]]]]]]]]]]]]]]]");
                    }catch(JedisException e)
                    {
                        log.error(e.getMessage());
                    }finally{//WHAT IF CONNECTION IS CLOSED, SHOULD I TRY TO RECONNECT WITHOUT RESTARTING TASK(RECALLING START)?
                        jedisComposedConn.close();
                        jedisSourceConn.close();
                    }
                    
                    break;
                
                case "composedDelAdmin":
                    username = (String)value.get("username");              
                    composedId = (String)value.get("composedId");
                    try{
                        Long response = redisProvider.deleteComposedFromSourceFollowers(username, composedId);
                        log.info("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[ composedUserDelFromAdmin response="+response+"]]]]]]]]]]]]]]]]]]]]]]]]]]]]]");
                    }catch(JedisException e)
                    {
                        log.error(e.getMessage());
                    }finally{//WHAT IF CONNECTION IS CLOSED, SHOULD I TRY TO RECONNECT WITHOUT RESTARTING TASK(RECALLING START)?
                        jedisComposedConn.close();
                        jedisSourceConn.close();
                    }
                    
                    break;
                    
                default:
                    break;
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
        jedisComposedConn.close();
        jedisSourceConn.close();
    }

    @Override
    public String version() {
        return VersionUtil.getVersion();
    }
    
}
