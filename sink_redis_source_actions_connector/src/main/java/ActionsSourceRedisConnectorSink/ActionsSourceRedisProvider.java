/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ActionsSourceRedisConnectorSink;


import java.util.HashMap;
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
public class ActionsSourceRedisProvider {
    static final Logger log = LoggerFactory.getLogger(ActionsSourceRedisProvider.class);
      
    final static private String SOURCE_NAMESPACE = "Source:";
    final static private String MAP_SOURCES_NAMESPACE = "MapSources:";
    final static private String SOURCE_ID_NAMESPACE = "Source:id:";
    
    private Jedis sourceRedisClient;
    
    ActionsSourceRedisProvider(Jedis connSource) throws JedisException
    {
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
     * Creates a Source in SourceDB
     * @param source, HashMap<String, String> that may include "Description", "Title", "DateOfCreation" and "url" keys
     * @param nMapSources
     * @param idMapSource
     * @return 
     */
    public long createSource(HashMap<String, String> source, int nMapSources, String idMapSource)
    {
        log.info("Entra en createSource()");
        
        String source_url = source.get("Url");
        log.info("source_url= "+source_url);
        String lock = acquireLockWithTimeout(SOURCE_NAMESPACE + source_url, 10, 1, sourceRedisClient);
        if(lock == null){
            return -1;
        }
        
        log.info("After primer if");
        log.info("source_url= "+source_url);
        //check this url does not already exist in redis
        Transaction trans = sourceRedisClient.multi();
        for(int i=0; i<nMapSources; i++)
        {
            trans.hexists(MAP_SOURCES_NAMESPACE + i, source_url);
        }
        
        List<Object> listResponses = trans.exec();
        int i=0;
        log.info("listResponses size = "+ listResponses.size());
        log.info("Response " + i + " " + (Boolean)listResponses.get(i));
        while(i<listResponses.size() && !(Boolean)listResponses.get(i))
        {
            log.info("Response " + i + " " + (Boolean)listResponses.get(i));
            i++;
            
        }
        log.info("i="+i);
        if(i < 4)//url of rss already exists
            return -1;
        
        log.info("After segundo if");
        
        String mapSourceKey = generateIdString(MAP_SOURCES_NAMESPACE, idMapSource);
        String id = String.valueOf(sourceRedisClient.incr(SOURCE_ID_NAMESPACE));
        String sourceKey = generateIdString(SOURCE_NAMESPACE, id);
        
        HashMap<String, String> real_source = new HashMap<String, String>();
        real_source.put("Title", source.get("Title"));
        real_source.put("Description", source.get("Description"));
        real_source.put("Link", source.get("Link"));
        real_source.put("Url", source.get("Url"));
        real_source.put("DateOfCreation", source.get("DateOfCreation"));
        
        trans = sourceRedisClient.multi();
        trans.hset(mapSourceKey, source_url, id);
        real_source.put("nPosts", "0");
        real_source.put("nFollowers", "0");
        real_source.put("id", id);
        trans.hmset(sourceKey, real_source);
        listResponses = trans.exec();
        
        releaseLock(SOURCE_NAMESPACE + source_url, lock, sourceRedisClient);
                
        return (long)listResponses.get(0);
    }
    
    /**
     * Deletes a Composed from SourceFollowers zset and updates nFollowers of Source in SourceDB
     * @param sourceId
     * @param composedId
     * @param UnfollowingDate
     * @return -1 if number of followers has been exceeded, 1 if all redis commands worked successfully
     */
    public long deleteSource(String source_url, int nMapSources)//lo mismo esto no tiene sentido aquí sino que debería hacerlo en redis directamente con una cuenta atrás.
    {    
        return 0; 
    }
    
    private static String generateIdString(String namespace, String id)
    {
        return namespace + id;
    }
    
    public static void main(String[] args)
    {
    }
}
