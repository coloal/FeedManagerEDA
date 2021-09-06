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
public class SourceVersionUtil {
    public static String getVersion() {
    try {
      return SourceVersionUtil.class.getPackage().getImplementationVersion();
    } catch(Exception ex){
      return "0.0.0.0";
    }
  }
}
