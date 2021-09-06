/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SourceRssConnector;

import java.util.Map;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

/**
 *
 * @author alberto
 */
public class SourceRssConnectorConfig extends AbstractConfig {
    public SourceRssConnectorConfig(Map originals){
       super(configDef(), originals);
    }
    
    protected static ConfigDef configDef() {
        return new ConfigDef()
        .define("topic", 
                ConfigDef.Type.LIST,
                ConfigDef.Importance.HIGH,
                "Name of kafka topic to produce to")
        .define("offsets-topic", 
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                "Name of offset kafka topic to consume from")
        .define("kafka-bootstrap", 
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                "Kafka bootstrap");
    }
}
