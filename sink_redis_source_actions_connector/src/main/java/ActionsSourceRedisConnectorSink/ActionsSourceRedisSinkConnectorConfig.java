/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ActionsSourceRedisConnectorSink;

import java.util.Map;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;

/**
 *
 * @author alberto
 */
class ActionsSourceRedisSinkConnectorConfig extends AbstractConfig{
    public ActionsSourceRedisSinkConnectorConfig(Map originals){
       super(configDef(), originals);
    }
    
    protected static ConfigDef configDef() {
        return new ConfigDef()
        .define("topics", 
                ConfigDef.Type.LIST,
                ConfigDef.Importance.HIGH,
                "Name of kafka topic to consume from")
        .define("source-redis-connection", 
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                "SourceDB Redis host to connect to")
        .define("source-redis-password", 
                ConfigDef.Type.PASSWORD,
                ConfigDef.Importance.HIGH,
                "SourceD password")
        .define("nMapSources", 
                ConfigDef.Type.STRING,
                ConfigDef.Importance.HIGH,
                "Number of MapSources:<id> hashes in redis db");
    }
}
