/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ActionsComposedRedisConnectorSink;

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
public class ActionsComposedRedisSinkConnector extends SinkConnector{
    private static Logger logger = LoggerFactory
            .getLogger(ActionsComposedRedisSinkConnector.class);
    
    private ActionsComposedRedisSinkConnectorConfig config_;
    private Map<String, String> configProps_;
    
    public static final String TOPIC_CONFIG_PROPERTY = "topics";
    public static final String COMPOSED_REDIS_CONNECTION_CONFIG_PROPERTY = "composed-redis-connection";
    public static final String COMPOSED_REDIS_PASSWORD_CONFIG_PROPERTY = "composed-redis-password";
    public static final String SOURCE_REDIS_CONNECTION_CONFIG_PROPERTY = "source-redis-connection";
    public static final String SOURCE_REDIS_PASSWORD_CONFIG_PROPERTY = "source-redis-password";
    
    @Override
    public void start(Map<String, String> props) {
        logger.info("[BEGIN] CONNECTOR START---------------------------");
        //config = new SourceRssConnectorConfig(props);
        this.config_ = new ActionsComposedRedisSinkConnectorConfig(props);
        this.configProps_ = Collections.unmodifiableMap(props);
        
        logger.info("Starting connector with properties: " + props);
        
        //logger.info("Topics config: " + props.get(TOPIC_CONFIG_PROPERTY));
        //logger.info("Redis-connection config: " + props.get(REDIS_CONNECTION_CONFIG_PROPERTY));
        //logger.info("Redis-password config: " + props.get(REDIS_PASSWORD_CONFIG_PROPERTY));
        
        if(config_.getList(TOPIC_CONFIG_PROPERTY).size() != 1)
            throw new ConfigException("'topics' in RedisSinkConnector "
                    + "requires single topic");
        
        if(config_.getString(COMPOSED_REDIS_CONNECTION_CONFIG_PROPERTY).equals(""))
            throw new ConfigException("ComposedDB redis-connection can't be empty");
        
        if(config_.getString(SOURCE_REDIS_CONNECTION_CONFIG_PROPERTY).equals(""))
            throw new ConfigException("SourceDB redis-connection can't be empty");
              
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
        return ActionsComposedRedisSinkTask.class;
    }

    @Override
    public void stop() {
        
    }

    @Override
    public ConfigDef config() {
        return ActionsComposedRedisSinkConnectorConfig.configDef();
    }
    
    public ActionsComposedRedisSinkConnectorConfig getConfig() {
        return config_;
    }

    @Override
    public String version() {
      return VersionUtil.getVersion();
    }
}
