package SourceRssConnector;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alberto
 */
public class SourceRssConnector extends SourceConnector {
    private static Logger logger = LoggerFactory.getLogger(SourceRssConnector.class);
    
    public static final String TOPIC_CONFIG_PROPERTY = "topic";
    public static final String KAFKA_BOOTSTRAP_CONFIG_PROPERTY = "kafka-bootstrap";
 
    private SourceRssConnectorConfig config_;
    private Map<String, String> configProps_;
    
    public SourceRssConnectorConfig getConfig() { return config_; }
    
    @Override
    public String version() {
      return VersionUtil.getVersion();
    }

    @Override
    public void start(Map<String, String> props) {
        //config = new SourceRssConnectorConfig(props);
        this.config_ = new SourceRssConnectorConfig(props);
        this.configProps_ = Collections.unmodifiableMap(props);
        
        logger.info("Starting connector with properties: " + props);
        
        if(config_.getList(TOPIC_CONFIG_PROPERTY).size() != 1)
            throw new ConfigException("'topic' in SourceRssConnector requires single topic");
        
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        //TODO: Define the individual task configurations that will be executed.
        
        List<Map<String,String>> listTaskConfigs = new ArrayList<>(maxTasks);
        int i=0;

        while(i < maxTasks)
        {
            Map<String,String> taskProps = new HashMap<>(configProps_);
            //taskProps.put("source-db-endpoint", listEndPoints.get(i));
            taskProps.put("partition-assigned", String.valueOf(i));
            listTaskConfigs.add(taskProps);
            i++;
        }

        return listTaskConfigs;
    }
    
    @Override
    public Class<? extends Task> taskClass() {
        //TODO: Return your task implementation.
        return SourceRssTask.class;
    }
    
    @Override
    public void stop() {
      //TODO: Do things that are necessary to stop your connector.
    }

    @Override
    public ConfigDef config() {
      return SourceRssConnectorConfig.configDef();
    }
}
