/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SourceRedisConnectorSink;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
public class SourceRedisProvider {
        static final Logger log = LoggerFactory.getLogger(SourceRedisProvider.class);

    final static private String SOURCE_FEED_NAMESPACE = "SourceFeed:";
    final static private String ITEM_NAMESPACE = "Item:";
    final static private String SOURCE_NAMESPACE = "Source:";
    final static private String MAP_SOURCES_NAMESPACE = "MapSources:";
    
    private Jedis redisClient;
    
    SourceRedisProvider(Jedis conn) throws JedisException
    {
        redisClient = conn;
    }
    
    private String acquireLockWithTimeout(String lock, int acquireTimeout, int lockTimeout) throws JedisException
    {
        String id = UUID.randomUUID().toString();
        lock = "lock:" + lock;

        long end = System.currentTimeMillis() + (acquireTimeout * 1000);
        while (System.currentTimeMillis() < end) {
            if (redisClient.setnx(lock, id) >= 1) {
                redisClient.expire(lock, lockTimeout);
                return id;
            }else if (redisClient.ttl(lock) <= 0){
                redisClient.expire(lock, lockTimeout);
            }
        }

        return null;
    }
    
    private boolean releaseLock(String lock, String id) throws JedisException
    {
        lock = "lock:" + lock;
        while (true) {
            redisClient.watch(lock);
            if (id.equals(redisClient.get(lock))) {
                Transaction trans = redisClient.multi();
                trans.del(lock);
                List<Object> result = trans.exec();
                
                if (result == null){
                    continue;
                }
                return true;
            }

            redisClient.unwatch();
            break;
        }

        return false;
    }
    
    
    
    /**
     * Creates an item and adds it to its SourceFeed
     * 
     * @param conn            redis client connection
     * @param sourceFeedId    id of the
     * @param item            item to create
     * @return      0 if Source doesn't exist. If item is correctly created
     *              returns its id.
     */        
    public Map<String, String> postItem2SourceFeed(String sourceFeedUrl, Map<String, String> item) throws JedisException
    {
        log.info("************** HA CAMBIADO **********************");
        Transaction trans = redisClient.multi();
        
        for(int i=0; i < 4; i++)
        {
            trans.hexists(MAP_SOURCES_NAMESPACE + i, sourceFeedUrl);
        }
        List<Object> listResponses = trans.exec();
        int i=0;
        //log.info("listResponses size = "+ listResponses.size());
        //log.info("Response " + i + " " + (Boolean)listResponses.get(i));
        while(i<listResponses.size() && !(Boolean)listResponses.get(i))
        {
            log.info("Response " + i + " " + (Boolean)listResponses.get(i));
            i++;
            
        }
        log.info("--------------+++++++++++++--------------- i="+i);
        if(i >= 4)//url of rss already exists
            return null;
        
        trans = redisClient.multi();
        trans.hget(MAP_SOURCES_NAMESPACE+i, sourceFeedUrl);
        trans.incr(ITEM_NAMESPACE + "id");//get new item id
        List<Object> response = trans.exec();
        
        String sourceId = (String)response.get(0);
        if(sourceId == null)
            return null;
        
        String sourceFeedKey = generateIdString(SOURCE_FEED_NAMESPACE, sourceId);
        String sourceKey = generateIdString(SOURCE_NAMESPACE, sourceId); 
        
        Long longItemId = (Long)response.get(1);
        String sItemId = Long.toString(longItemId);
        String itemKey = generateIdString(ITEM_NAMESPACE, sItemId);
        
        Map<String, String> mapItem = new HashMap<String, String>(item);
        //in item already comes pubDate, title, link and description
        mapItem.put("sourceId", sourceId);
        mapItem.put("id", sItemId);
        
        trans = redisClient.multi();
        trans.hincrBy(sourceKey, "nPosts", 1);
        trans.hmset(itemKey, mapItem);
        log.info("------------------------------DATE: "+item.get("date") +"-------------------------------------------");
        trans.zadd(sourceFeedKey, Double.parseDouble(item.get("date")), sItemId);
        response = trans.exec();
        
        Map<String, String> filteredResponse = new HashMap<>();
        if((long)response.get(2) == 1)
        {
            filteredResponse.put("item_id", sItemId);
            filteredResponse.put("sourceFeed_id", sourceId);
            filteredResponse.put("pubDate", item.get("date"));
        }     
        
        return filteredResponse;       
    }
    
    private static String generateIdString(String namespace, String id)
    {
        return namespace + id;
    }
    
    public static void main(String[] args)
    {
        System.out.println("--------------Init execution---------------------");
        JedisShardInfo shardInfo = new JedisShardInfo("localhost", 7000);
        shardInfo.setPassword("pUO893PgGX");
        Jedis jedis = new Jedis(shardInfo);
        
        SourceRedisProvider redisProvider = new SourceRedisProvider(jedis);
        
        Random rander = new Random();
        double rNumber = rander.nextInt(1000);
 
        
        System.out.println("---------------End execution---------------------");
    }
}
