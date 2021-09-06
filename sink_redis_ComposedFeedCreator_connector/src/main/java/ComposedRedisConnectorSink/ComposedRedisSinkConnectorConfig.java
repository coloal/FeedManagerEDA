/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ComposedRedisConnectorSink;

import java.util.Map;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

/**
 *
 * @author alberto
 */
class ComposedRedisSinkConnectorConfig extends AbstractConfig{
    public ComposedRedisSinkConnectorConfig(Map originals){
       super(configDef(), originals);
    }
    
    protected static ConfigDef configDef() {
        return new ConfigDef()
        .define("topics", 
                ConfigDef.Type.LIST,
                ConfigDef.Importance.HIGH,
                "Name of kafka topic to consume from")
        .define("redis-connection", 
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                "Redis host to connect to")
        .define("redis-password", 
                ConfigDef.Type.PASSWORD,
                ConfigDef.Importance.HIGH,
                "Redis host to connect to");
    }
}
