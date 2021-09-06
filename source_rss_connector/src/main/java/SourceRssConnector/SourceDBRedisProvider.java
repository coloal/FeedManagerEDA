/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SourceRssConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

/**
 *
 * @author alberto
 */
public class SourceDBRedisProvider {
    
    final static private String SOURCE_FEED_NAMESPACE = "SourceFeed:";
    
    private Jedis redisClient;
    
    SourceDBRedisProvider(Jedis conn) throws JedisException
    {
        redisClient = conn;
    }
    
    
    
    
    private static String generateIdString(String namespace, String id)
    {
        return namespace + id;
    }
    
    public static void main(String[] args)
    {
    }
}
