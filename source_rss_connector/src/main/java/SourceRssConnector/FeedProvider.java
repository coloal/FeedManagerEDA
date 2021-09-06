/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SourceRssConnector;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alberto
 */
public class FeedProvider {
    static final org.slf4j.Logger log = LoggerFactory.getLogger(FeedProvider.class);
    private final String URL;
    private CloseableHttpClient client;
    private Header offsetEtag;
    private Header offsetLastModified;
    private SyndFeedInput input;
    private SyndFeed feed;
    private boolean unreaded;
       
    FeedProvider(String url, CloseableHttpClient c, SyndFeedInput s, boolean un)
    {
        URL = url;
        client = c;
        offsetEtag = null;
        offsetLastModified = null;
        input = s;
        feed = null;
        unreaded = un;
    }
    
    public String getUrl() { return URL; }
    
    public FeedResponse getWhole() throws MyConnectorException
    {
        //log.info("INSIDE FeedProvider url: "+URL);
        HttpUriRequest request = new HttpGet(URL);
        
        try (CloseableHttpResponse response = client.execute(request);
             InputStream stream = response.getEntity().getContent();
             XmlReader xmlReader = this.createXmlReader(stream)
                ){
            
            offsetEtag = response.getFirstHeader("etag");
            offsetLastModified = response.getFirstHeader("last-modified");
            
            feed = input.build(xmlReader);
            
            List<SyndEntry> listItems = feed.getEntries();
            String lastModified = null;
            if(offsetLastModified != null)
                lastModified = offsetLastModified.getValue();
            String etag = null;
            if(offsetEtag != null)
                etag = offsetEtag.getValue();
        
        return new FeedResponse(listItems, etag,
                lastModified);
        } catch(IOException ex) {
            throw new MyConnectorException("Exception(IOException) thrown at FeedProvider getWhole");
        }catch(FeedException ex) {
            throw new MyConnectorException("Exception(IOException) thrown at FeedProvider getWhole");
        }
        
    }
    
    public int hasChangedCode(String Etag, String LastModified) throws MyConnectorException {
        HttpHead hasChangedRequest = new HttpHead(URL);
        hasChangedRequest.setHeader("If-Modified-Since", LastModified);
        hasChangedRequest.setHeader("If-None-Match", Etag);

        //CloseableHttpResponse headResponse = null;
        try (CloseableHttpResponse headResponse = client.execute(hasChangedRequest)){
            
            return headResponse.getStatusLine().getStatusCode();
            
        }catch(NoHttpResponseException ex)
        {
            throw new MyConnectorException("hasChangedCode has thrown NoHttpResponseException");
        }
        catch(IOException ex)
        {
            throw new MyConnectorException("hasChangedCode has thrown General NoHttpResponseException");
        }
         
    }
    
    XmlReader createXmlReader(InputStream inStream) throws IOException
    { 
        return new XmlReader(inStream); 
    }
    
    boolean unreaded() { return unreaded;}
    
    public void setReaded() { unreaded = false;}
    
    public class FeedResponse{
        private final List<SyndEntry> items;
        private final String etag;
        private final String lastModified;
        
        FeedResponse(List<SyndEntry> i, String e, String l)
        {
            items = i;
            etag = e;
            lastModified = l;
        }
        
        public List<SyndEntry> getItems(){ return items; }
        public String getEtag(){ return etag; }
        public String getLastModified(){ return lastModified; }
    }
}
