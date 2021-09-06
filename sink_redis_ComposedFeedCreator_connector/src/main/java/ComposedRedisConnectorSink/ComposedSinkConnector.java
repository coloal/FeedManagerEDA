/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ComposedRedisConnectorSink;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alberto
 */
public class ComposedSinkConnector extends SinkConnector{
    private static Logger logger = LoggerFactory
            .getLogger(ComposedSinkConnector.class);
    
    private ComposedRedisSinkConnectorConfig config_;
    private Map<String, String> configProps_;
    
    public static final String TOPIC_CONFIG_PROPERTY = "topics";
    public static final String REDIS_CONNECTION_CONFIG_PROPERTY = "redis-connection";
    public static final String REDIS_PASSWORD_CONFIG_PROPERTY = "redis-password";
    
    @Override
    public void start(Map<String, String> props) {
        logger.info("[BEGIN] CONNECTOR START---------------------------");
        //config = new SourceRssConnectorConfig(props);
        this.config_ = new ComposedRedisSinkConnectorConfig(props);
        this.configProps_ = Collections.unmodifiableMap(props);
        
        logger.info("Starting connector with properties: " + props);
        
        //logger.info("Topics config: " + props.get(TOPIC_CONFIG_PROPERTY));
        //logger.info("Redis-connection config: " + props.get(REDIS_CONNECTION_CONFIG_PROPERTY));
        //logger.info("Redis-password config: " + props.get(REDIS_PASSWORD_CONFIG_PROPERTY));
        
        if(config_.getList(TOPIC_CONFIG_PROPERTY).size() != 1)
            throw new ConfigException("'topics' in RedisSinkConnector "
                    + "requires single topic");
        
        if(config_.getString(REDIS_CONNECTION_CONFIG_PROPERTY).equals(""))
            throw new ConfigException("redis-connection can't be empty");
        
        //if(config_.getPassword(REDIS_PASSWORD_CONFIG_PROPERTY) != null)
        //    throw new ConfigException("redis-password must be defined");
            
        
        logger.info("[END] CONNECTOR START---------------------------");
    }
    
    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        
        List<Map<String,String>> listTaskConfigs = new ArrayList<>(maxTasks);
        int i=0;
        
        while(i < maxTasks)
        {
            Map<String,String> taskProps = new HashMap<>(configProps_);
            listTaskConfigs.add(taskProps);
            i++;
        }

        return listTaskConfigs;
    }
    
    @Override
    public Class<? extends Task> taskClass() {
        return ComposedRedisSinkTask.class;
    }

    @Override
    public void stop() {
        
    }

    @Override
    public ConfigDef config() {
        return ComposedRedisSinkConnectorConfig.configDef();
    }
    
    public ComposedRedisSinkConnectorConfig getConfig() {
        return config_;
    }

    @Override
    public String version() {
      return ComposedVersionUtil.getVersion();
    }
}
