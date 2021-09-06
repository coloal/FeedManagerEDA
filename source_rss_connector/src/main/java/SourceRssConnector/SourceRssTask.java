/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SourceRssConnector;

import SourceRssConnector.FeedProvider.FeedResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ListIterator;
import java.util.logging.Level;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;

/**
 *
 * @author alberto
 */
public class SourceRssTask extends SourceTask {
    static final Logger log = LoggerFactory.getLogger(SourceRssTask.class);
    
    //private String sUrl;
    private String topic;
    
    private Schema myItemSchema;
    
    private ObjectMapper mapper;
    
    private KafkaConsumerProvider offsetProvider;
    
    private CloseableHttpClient httpClient;
  
    HashMap<String, FeedProvider> feeds;
    HashMap<String, HashSet<String>> itemsPerFeed;
    HashMap<String, String> offsetFeeds;
    String offsetsTopic;
    
    List<SourceRecord> records;
    HashMap<String, String> mapOffsets;
    
    /*
    public String parseKey(String recordKey){
        List<Object> keyMap;
        String key="";
        try {
            keyMap = mapper.readValue(recordKey, List.class);
            LinkedHashMap<String,String> jsonKeyObj = (LinkedHashMap<String,String>)keyMap.get(1);
            key = jsonKeyObj.entrySet().iterator().next().getKey();
        } catch (JsonProcessingException ex) {
            java.util.logging.Logger.getLogger(SourceRssTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        return key;
    }
    */
    public String parseValue(String recordValue){
        String value="";
        try {
            Map<String,String> valueMap = mapper.readValue(recordValue, Map.class); 
            value = valueMap.entrySet().iterator().next().getValue();
        } catch (JsonProcessingException ex) {
            java.util.logging.Logger.getLogger(SourceRssTask.class.getName()).log(Level.SEVERE, null, ex);
        }
        return value;
    }
    
    @Override
    public void start(Map<String, String> props) {
        mapper = new ObjectMapper();

        httpClient = HttpClients.createMinimal();
        
        String kafkaBootstrap = props.get("kafka-bootstrap");
        topic = props.get("topic");
        offsetsTopic = props.get("offsets-topic");
        int taskOffsetPartition = Integer.parseInt(props.get("partition-assigned"));
        //log.info("!!!!!!!!!!!!!!!El bootstrap de KAFKA: " + kafkaBootstrap);
        offsetProvider = new KafkaConsumerProvider(kafkaBootstrap, offsetsTopic, taskOffsetPartition);
        //log.info("!!!!!!!!!!!!!!El offset kafka provider: "+ offsetProvider); 
        ConsumerRecords<String, String> allOffsets = offsetProvider.getOffsetsFromTheBeginning();
        //log.info("++++++++++++++allOffsets: "+ allOffsets.count() +"+++++++++++++++++++++");
        feeds = new HashMap();
        offsetFeeds = new HashMap();
        itemsPerFeed = new HashMap();
        mapOffsets = new HashMap();
        
        log.info("+++++++++++++++++++ START +++++++++++++++++++++++++");
        allOffsets.forEach(record -> {
            String key = record.key();
            log.info("***************offset value en start: " + record.value() + "********************");
            
            feeds.put(key, new FeedProvider(key,httpClient, new SyndFeedInput(), false));
            offsetFeeds.put(key, record.value());
            itemsPerFeed.put(key, new HashSet());
            
        });
     
        myItemSchema = SchemaBuilder.struct().name("ItemRecord")
                .field("title", Schema.STRING_SCHEMA)
                .field("link", Schema.STRING_SCHEMA)
                .field("date", Schema.STRING_SCHEMA)
                .field("description", Schema.STRING_SCHEMA)
                .build();   
        
        
        String msg = "Inside start ";
        for (Map.Entry<String, String> offset : offsetFeeds.entrySet()) {
            msg = "Key offset->" + offset.getKey() + "Value offset->" + offset.getValue() + " ";
        }
        log.info(msg);
        //log.info("END START TASK++++++++++++++");
    }
    
    /*void setFeedProvider(FeedProvider f)
    {
       feed = f;
    }*/
    
    @Override
    public void commit(){
        log.info("--------------------COMMIT---------------------------");
        offsetProvider.produceOffsets(mapOffsets);
        offsetFeeds.putAll(mapOffsets);
        for (Map.Entry<String, String> offset : mapOffsets.entrySet())
            log.info("Se está produciendo " + offset.getKey() + ":" + offset.getValue());
        mapOffsets = new HashMap();
        log.info("--------------------END COMMIT---------------------------");
    }
    
    @Override
    public List<SourceRecord> poll() {
        //Updating Sources this task is reponsible from
        ConsumerRecords<String, String> collectionOffsets = offsetProvider.getOffsets();
        /*for (Map.Entry<String, FeedProvider> offset : feeds.entrySet()) {
            log.info("**Feeds in poll**->" + offset.getKey() + "Value offset->" + offset.getValue().getUrl());
        }*/
        
        collectionOffsets.forEach(record -> {
            String key = record.key();
            log.info("Key from new RSS: " + key + " value: " + record.value());
            if(!feeds.containsKey(key))
            {
                feeds.put(key, new FeedProvider(key,httpClient, new SyndFeedInput(), true));
                itemsPerFeed.put(key, new HashSet());
                offsetFeeds.put(key, null);
                log.info("*************Nuevo RSS************");
            }
            //esto ya se hace en el commit
            /*else{
                log.info("!!!!!Update offset with value: " + record.value());
                offsetFeeds.put(key, record.value());
            }*/
        });
                                
        records = new ArrayList<>();
        
        if(feeds != null){
            //log.info("****************** feeds size: " + feeds.size() + "*************************");
            feeds.forEach((k,feed) -> {
            //log.info("****************READING FEED: " + feed.getUrl() + "******************************8");
                if(feed.unreaded())
                {
                   
                   List<SourceRecord> recs = firstFeedPoll(feed);
                   log.info(" Calling firstFeedPoll: " + feed.getUrl());
                   log.info("(firstFeedPoll)Size of list of records" + recs.size());
                   records.addAll(recs);
                   feed.setReaded();//esto se debería hacer en commit()
                }else{
                   //Está petando porque le payload en followingFeedPoll es null, osea que offsetFeeds.get(url) está devolviendo null, vamos que después de la primera lectura del rss no se está seteando bien el offsetFeeds
                   if(offsetFeeds.get(feed.getUrl()) != null) //no probado, esto evita que se intente leer si aún no se ha actualizado correctamente el offset de ese rss en la línea 173
                   {
                       
                       /*String msg = "++Inside poll++ ";
                       for (Map.Entry<String, String> offset : offsetFeeds.entrySet()) {
                            msg = "Key offset-> " + offset.getKey() + "Value offset-> " + offset.getValue() + " ";
                       }
                       log.info(msg);
                       */
                       //log.info("ENTRA EN FOLLOWING FEEDPOLL, OFFSETFEEDS NOT NULL");
                       List<SourceRecord> recs = followingFeedPoll(feed);
                       log.info("Calling followingFeedPoll: " + feed.getUrl());
                       log.info("(followingFeedPoll)Size of list of records" + recs.size());
                       records.addAll(recs);
                   }

                }
            });
        }
        /*
        for(SourceRecord rec: records)
        {
            log.info("Record: " + rec.key());
        }
        */
        return records;    
    }   
    
    private List<SourceRecord> followingFeedPoll(FeedProvider feed)
    {
        //log.info("22222222222222222 followingFeedPoll: "+feed.getUrl());
        List<SourceRecord> listItemsToProduce = new ArrayList<>();
        String payload = (String)offsetFeeds.get(feed.getUrl());
        Map<String, String> headerOffset = null;
        int statusCode = 0;

        try {
            headerOffset = mapper.readValue(payload, Map.class);
            statusCode = feed.hasChangedCode((String)headerOffset.get("etag"),
                (String)headerOffset.get("last-modified"));
        } catch (JsonProcessingException ex) {
            log.error("Unable to parse json String representing the last stored offset ", ex);
        }
        catch(MyConnectorException ex){
            log.error("Unable to check if feed has been updated, feedURL: " + feed.getUrl(), ex);
        }

        if(statusCode==200)//Something changed in the rss and the response is OK
        {
            //Here we are only adding totally new items,
            //if an item is updated it is not being republished.

            FeedResponse res = null;
            try {
                res = feed.getWhole();
            } catch (MyConnectorException ex) {
                log.error("Unable to read feed", ex);
            }

            ListIterator<SyndEntry> itItem = res.getItems().listIterator();
            Map<String,SyndEntry> guidsJustRead = new LinkedHashMap<>();
            while(itItem.hasNext())
            {
                SyndEntry entry = (SyndEntry) itItem.next();
                guidsJustRead.put(entry.getUri(), entry);
            }
            
            HashSet<String> guidsPublished = itemsPerFeed.get(feed.getUrl());
            //First we are going to stop recording the items that have been 
            //published but exists no more in the rss.
            guidsPublished.retainAll(guidsJustRead.keySet());

            //Now in guidsPublished we have the set of already published 
            //items from those which are still in the rss, so we let in
            //guidsJustRead those which hasn't been published:
            guidsJustRead.keySet().removeAll(guidsPublished);

            for(Map.Entry<String, SyndEntry> entry : guidsJustRead.entrySet())
            {

                if(entry.getValue().getTitle() != null && 
                        entry.getValue().getLink() != null && 
                        entry.getValue().getDescription() != null)
                {
                    Date pubDate = entry.getValue().getPublishedDate();

                    if(pubDate == null)
                        pubDate = new Date();

                    SourceRecord record = generateRecord(feed.getUrl(), 
                            entry.getValue().getUri(), res.getLastModified(),
                            res.getEtag(), entry.getValue().getTitle(), 
                            entry.getValue().getLink(),
                            entry.getValue().getDescription().getValue(),
                            String.valueOf(pubDate.getTime())
                    );

                    listItemsToProduce.add(record);

                    guidsPublished.add(entry.getValue().getUri());
                    //mapOffsets.put(entry.getValue().getUri(), (String)record.value());
                    mapOffsets.put(feed.getUrl(), (String)record.sourceOffset().entrySet().iterator().next().getValue());
                }
            }
            itemsPerFeed.put(feed.getUrl(), guidsPublished);
            return listItemsToProduce;

        }else if(statusCode!=304){//If status code is 304 the rss did not changed
            //The status code returned is not 200 nor 304 so something is wrong.
        }
        return listItemsToProduce;
    }
    
    private List<SourceRecord> firstFeedPoll(FeedProvider feed)
    {
        log.info("1111111111111111111 firstFeedPoll: "+feed.getUrl());
        
        List<SourceRecord> listItemsToProduce = new ArrayList<>();
        FeedResponse res = null;
        try {
            res = feed.getWhole();
        } catch (MyConnectorException ex) {
            java.util.logging.Logger.getLogger(SourceRssTask.class
                    .getName()).log(Level.SEVERE, null, ex);
        } 
        
        if(res != null)
        {
            ListIterator<SyndEntry> itItem = res.getItems().listIterator();
            HashSet<String> guidsPublished = new HashSet<String>();
            while(itItem.hasNext())
            {
                SyndEntry entry = (SyndEntry) itItem.next();

                if(entry.getTitle() != null && entry.getLink() != null 
                        && entry.getDescription() != null)
                {
                    Date pubDate = entry.getPublishedDate();

                    if(pubDate == null)
                        pubDate = new Date();

                    SourceRecord record = generateRecord(feed.getUrl(), 
                            entry.getUri(), res.getLastModified(),
                            res.getEtag(), entry.getTitle(), 
                            entry.getLink(),
                            entry.getDescription().getValue(),
                            String.valueOf(pubDate.getTime())
                    );

                    listItemsToProduce.add(record);

                    guidsPublished.add(entry.getUri());

                    mapOffsets.put(feed.getUrl(), (String)record.sourceOffset().entrySet().iterator().next().getValue());
                }
            }
            itemsPerFeed.put(feed.getUrl(), guidsPublished);
        }
        
        return listItemsToProduce;
    }
    
    private SourceRecord generateRecord(String sUrl, String guid, String lastModified,
            String etag, String title, String link, String desc, String date)
    {
        Map<String,String> partitionSource = Collections.singletonMap(
                                sUrl, sUrl);
                        
        Map<String, String> newOffset = new HashMap<>(3);

        if(lastModified != null)
            newOffset.put("last-modified", lastModified);

        if(etag != null)
            newOffset.put("etag", etag);

        newOffset.put("guid", guid);

        String stringOffNewOffset=null;
        try{
            stringOffNewOffset = new ObjectMapper().
                writeValueAsString(newOffset);
        }catch(JsonProcessingException e){
            log.error("Unable to create new offset String from item {}", guid, e);
        }

        Map<String,String> newOffsetSource = 
                Collections.singletonMap(
                sUrl, stringOffNewOffset);

        Struct entryStruct = new Struct(myItemSchema)
                                 .put("title", title)
                                 .put("link", link)
                                 .put("description", desc)
                                 .put("date", date);

        SourceRecord record = new SourceRecord(partitionSource, 
                newOffsetSource, topic, 
                org.apache.kafka.connect.data.Schema.STRING_SCHEMA, 
                sUrl, myItemSchema, entryStruct);
        
        return record;
    }
    
    @Override
    public void stop() {
        
      //TODO: Do whatever is required to stop your task.
    }

    @Override
    public String version() {
        return VersionUtil.getVersion();
    }
}
