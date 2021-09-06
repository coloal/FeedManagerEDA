/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ComposedRedisConnectorSink;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

/**
 *
 * @author alberto
 */
public class ComposedRedisProvider {
    static final Logger log = LoggerFactory.getLogger(ComposedRedisProvider.class);
    
    final static private String SOURCE_FOLLOWERS_NAMESPACE = "SourceFollowers:";
    final static private String COMPOSED_FEED_NAMESPACE = "ComposedFeed:";
    
    private Jedis redisClient;
    
    ComposedRedisProvider(Jedis conn) throws JedisException
    {
        redisClient = conn;
    }
    
    /**
     * Adds existent item to ComposedFeed followers of the Source the item was published on
     * 
     * @param sourceId    id of the Source
     * @param itemId            item to create
     * 
     */        
    public void referenceItemInComposedFeeds(String sourceId, String itemId, Long pubDate) throws JedisException
    {
        String SourceFollowersKey = generateIdString(SOURCE_FOLLOWERS_NAMESPACE, sourceId);
        Set<String> followers = redisClient.zrange(SourceFollowersKey, 0, -1);//The max number of followers in a RedisDB  instance is 1k
        log.info("%%%%%%%%%%%%%%%%% SIZE OF SET OF FOLLOWERS: " + followers.size());
        Transaction trans = redisClient.multi();
        for(String composedId: followers)
        { 
            String composedFeedKey = generateIdString(COMPOSED_FEED_NAMESPACE, composedId);
            trans.zadd(composedFeedKey, pubDate, itemId);
            
            trans.publish("composedchannel", composedId + ":" + itemId);
        }
        List<Object> result = trans.exec();
        log.info("**** ZADD ITEMS: ");
        for(Object o : result)
            log.info(" | " + (long)o + " | ");
        
    }
    
    
    
    private static String generateIdString(String namespace, String id)
    {
        return namespace + id;
    }
}
