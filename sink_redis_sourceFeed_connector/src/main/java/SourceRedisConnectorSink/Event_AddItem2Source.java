/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SourceRedisConnectorSink;

/**
 *
 * @author alberto
 */
public class Event_AddItem2Source {
    private String itemId;
    private String sourceId;
    private String pubDate;
    private String type;
    
    Event_AddItem2Source(String iId, String sId, String sDate, String t)
    {
        itemId = iId;
        sourceId = sId;
        pubDate = sDate;
        type = t;
    }
    
    public String getItemId(){ return itemId; }
    public String getSourceId(){ return sourceId; }
    public String getPubDate(){ return pubDate; }
    public String getType(){ return type; }
    
    public void setItemId(String id){ itemId = id; }
    public void setSourceId(String id){ sourceId = id; }
    public void setPubDate(String d){ pubDate = d; }
    public void setType(String t){ type = t; }
    
    @Override
    public String toString() {
        return "{\"type\": \"itemAdded2Source\"," +
                "\"itemId\": \"" + itemId + "\"" +
                "\"sourceId\": \"" + sourceId + "\"" +
                "\"pubDate\": \"" + pubDate + "\"}";
    }
}

