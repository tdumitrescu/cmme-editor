package Util;

import java.io.*;
import java.net.URL;
import java.util.*;
import org.jdom.*;

import DataStruct.XMLReader;

public class GlobalConfig
{
/*----------------------------------------------------------------------*/
/* Class variables/methods */

  public static String get(String key)
  {
    return getConfigMap().getVal(key);
  }

  /* lazy-load config */
  private static String       CONFIG_LOC = "config/cmme-config.xml";
  private static GlobalConfig configMap  = null;
  private static GlobalConfig getConfigMap()
  {
    if (configMap == null)
      configMap = new GlobalConfig(AppContext.BaseDataURL + CONFIG_LOC);
    return configMap;
  }

/*----------------------------------------------------------------------*/
/* Instance variables/methods */

  private HashMap<String,String> config;

  public GlobalConfig(String configLoc)
  {
    config = readConfigFromFile(configLoc);
  }

  String getVal(String key)
  {
    return config.get(key);
  }

  private HashMap<String,String> readConfigFromFile(String configLoc)
  {
    Document               configDoc;
    HashMap<String,String> cmap = new HashMap<String,String>();

    try
      {
        configDoc = XMLReader.getNonValidatingParser().build(new URL(configLoc));
      }
    catch (Exception e)
      {
        System.err.println("Exception loading config file: " + e);
        return cmap;
      }

    readConfigTree(configDoc.getRootElement(), cmap, "", true);

    return cmap;
  }

  /* inserts entries with slash-separated keys into cmap, e.g., "MIDI/BPM"="80" */
  private void readConfigTree(Element el, HashMap<String,String> cmap,
                              String mapNS, boolean root)
  {
    String k = el.getName(),
           childNS;
    List   children = el.getChildren();

    if (!children.isEmpty())
      {
        childNS = root ? "" : mapNS + k + "/";
        for (Object child : children)
          readConfigTree((Element)child, cmap, childNS, false);
      }
    else
      cmap.put(mapNS + k, el.getText());
  }
}
