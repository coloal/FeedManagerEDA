/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ComposedRedisConnectorSink;

/**
 *
 * @author alberto
 */
public class ComposedVersionUtil {
    public static String getVersion() {
    try {
      return ComposedVersionUtil.class.getPackage().getImplementationVersion();
    } catch(Exception ex){
      return "0.0.0.0";
    }
  }
}
