/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ActionsComposedRedisConnectorSink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

/**
 *
 * @author alberto
 */
public class ActionsComposedRedisProvider {
    static final Logger log = LoggerFactory.getLogger(ActionsComposedRedisProvider.class);
    
    final static private String lock_NAMESPACE = "lockUserAdminList:";
    final static private String USER_ADMIN_LIST_NAMESPACE = "UserAdminList:";
    final static private String COMPOSED_ADMINS_NAMESPACE = "AdminsList:";
    final static private String COMPOSED_FOLLOWING_NAMESPACE = "ComposedFollowing:";
    final static private String SOURCE_FOLLOWERS_NAMESPACE = "SourceFollowers:";
    final static private String SOURCE_NAMESPACE = "Source:";
    
    private Jedis composedRedisClient;
    private Jedis sourceRedisClient;
    
    ActionsComposedRedisProvider(Jedis connComposed, Jedis connSource) throws JedisException
    {
        composedRedisClient = connComposed;
        sourceRedisClient = connSource;
    }
    
    private String acquireLockWithTimeout(String lock, int acquireTimeout, int lockTimeout, Jedis conn) throws JedisException
    {
        String id = UUID.randomUUID().toString();
        lock = "lock:" + lock;

        long end = System.currentTimeMillis() + (acquireTimeout * 1000);
        while (System.currentTimeMillis() < end) {
            if (conn.setnx(lock, id) >= 1) {
                conn.expire(lock, lockTimeout);
                return id;
            }else if (conn.ttl(lock) <= 0){
                conn.expire(lock, lockTimeout);
            }
        }

        return null;
    }
    
    private boolean releaseLock(String lock, String id, Jedis conn) throws JedisException
    {
        lock = "lock:" + lock;
        while (true) {
            conn.watch(lock);
            if (id.equals(conn.get(lock))) {
                Transaction trans = conn.multi();
                trans.del(lock);
                List<Object> result = trans.exec();
                
                if (result == null){
                    continue;
                }
                return true;
            }

            conn.unwatch();
            break;
        }

        return false;
    }
    
    /**
     * Adds a Composed to the SourceFollowers zset and updates nFollowers of Source in SourceDB
     * @param sourceId
     * @param composedId
     * @param followingDate
     * @return -1 if number of followers has been exceeded, 1 if all redis commands worked successfully
     */
    public long addComposed2SourceFollowers(String sourceId, String composedId, long followingDate)
    {
        String composedFollowingKey = generateIdString(COMPOSED_FOLLOWING_NAMESPACE, composedId);
        String sourceFollowersKey = generateIdString(SOURCE_FOLLOWERS_NAMESPACE, sourceId);
        String lock = acquireLockWithTimeout(sourceFollowersKey, 10, 1, composedRedisClient);
        long nFollowers = composedRedisClient.zcard(sourceFollowersKey);//en vez de zcard accedemos a la variable nFollowers de Source en ComposedBD
        releaseLock(sourceFollowersKey, lock, composedRedisClient);
        
        if(nFollowers >= 1000)
            return -1;//Max number of followers in a redisDB excedeed, this follower should be added in another BD
        
        Transaction trans = composedRedisClient.multi();
        trans.zadd(sourceFollowersKey, followingDate, composedId);
        trans.zadd(composedFollowingKey, followingDate, sourceId);
        List<Object> listResponses = trans.exec();
        //end new code
        long response = 0;
        log.info("RESPONSE INSIDE REDIS PROVIDER=" + listResponses.get(0) + ", " + listResponses.get(1));
        if((long)listResponses.get(0) == 1 && (long)listResponses.get(1) == 1)
        {
            String sourceKey = generateIdString(SOURCE_NAMESPACE, sourceId);
            log.info("Source Key: "+ sourceKey);
            response = sourceRedisClient.hincrBy(sourceKey, "nFollowers", 1);
            log.info("SourceDB response: " + response);
        }
         
        return response; 
    }
    
    /**
     * Deletes a Composed from SourceFollowers zset and updates nFollowers of Source in SourceDB
     * @param sourceId
     * @param composedId
     * @param UnfollowingDate
     * @return -1 if number of followers has been exceeded, 1 if all redis commands worked successfully
     */
    public long deleteComposedFromSourceFollowers(String sourceId, String composedId, long UnfollowingDate)
    {
        String sourceFollowersKey = generateIdString(SOURCE_FOLLOWERS_NAMESPACE, sourceId);
        
        long response = composedRedisClient.zrem(sourceFollowersKey, composedId);
        
        log.info("RESPONSE INSIDE REDIS PROVIDER=" + response);
        if(response == 1)
        {
            String sourceKey = generateIdString(SOURCE_NAMESPACE, sourceId);
            response = sourceRedisClient.hincrBy(sourceKey, "nFollowers", -1);
        }
         
        return response; 
    }
    
    /**
     * Deletes an admin from composed admin list
     * @param username
     * @param composed_id
     * @return -1 if user is not included in admin list, 1 if user popped
     */
    public long deleteComposedFromSourceFollowers(String username, String composed_id)
    {
        String composedAdminsKey = generateIdString(COMPOSED_ADMINS_NAMESPACE, composed_id);
        long response = composedRedisClient.srem(composedAdminsKey, username);
        response += 1;
        
        if(response == 2)
            return (long)1;
        else
            return (long)0;
    }
    
    public long addUser2ComposedAdmins(String username, String composed_id)
    {
        String composedAdminsKey = generateIdString(COMPOSED_ADMINS_NAMESPACE, composed_id);
        String userAdminListKey = generateIdString(USER_ADMIN_LIST_NAMESPACE, username);
        
        String lock = acquireLockWithTimeout(lock_NAMESPACE + username, 10, 1, composedRedisClient);
        if(lock == null){
            return -1;
        }
        
        List<Object> listResponses = new ArrayList<Object>();
        
        if(composedRedisClient.scard(composedAdminsKey) < 10)
        {
            Transaction trans = composedRedisClient.multi();
            trans.sadd(composedAdminsKey, username);
            trans.sadd(userAdminListKey, composed_id);
            listResponses = trans.exec();
        }
        
        releaseLock(lock_NAMESPACE + username, lock, composedRedisClient);
        
        long response = 0;
        for(int i=0; i<listResponses.size(); i++)
            response += (long)listResponses.get(i);
        
        
        return (response==2)?1:0;
    }
    
    private static String generateIdString(String namespace, String id)
    {
        return namespace + id;
    }
    
    public static void main(String[] args)
    {
    }
}
