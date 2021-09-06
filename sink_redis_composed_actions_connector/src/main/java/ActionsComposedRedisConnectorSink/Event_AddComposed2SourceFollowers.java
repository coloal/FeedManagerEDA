/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ActionsComposedRedisConnectorSink;

/**
 *
 * @author alberto
 */
public class Event_AddComposed2SourceFollowers {
    private final String Title;
    private final String Description;
    private final String Link;
    private final double Date;
    Event_AddComposed2SourceFollowers(String t, String d, String l, String date)
    {
        Title = t;
        Description = d;
        Link = l;
        Date = Long.valueOf(date).doubleValue();
    }
    
    public String getTitle(){ return Title; }
    public String getDescription(){ return Description; }
    public String getLink(){ return Link; }
    public double getPubDate(){ return Date; }
    
}
