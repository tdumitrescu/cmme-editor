/*-----------------------------------------------------------------------------

        Module          : Util.AppContext

        Purpose         : Contextual info about app (file locations etc)

        Programmer      : Ted Dumitrescu

        Date Started    : 7/8/2011

        Updates         :

-----------------------------------------------------------------------------*/

package Util;

import java.io.*;
import java.net.*;
import javax.swing.JApplet;
public class AppContext
{
  public static final boolean CMME_OPT_TESTING=true,                              CMME_OPT_VALIDATEXML=false;  public static final String NetBaseDataDomain=CMME_OPT_TESTING ?                                                 "test2.cmme.org" :                                                 "www.cmme.org",                             BaseDataRelativeDir="/data/";  public static String       BaseDataDomain,BaseDataURL,                             BaseDataDir;  /*   * Calculates data directories depending on context   *   * @param aContext - applet for context info   * @param local - whether to get data from local file system or net   * @param inApplet - whether the context is a real or simulated applet   */  public static void setBaseDataLocations(JApplet aContext,                                          boolean local,boolean inApplet)  throws Exception  {    if (inApplet)      BaseDataDomain=aContext.getDocumentBase().getHost();    else      BaseDataDomain=NetBaseDataDomain;    if (local)      {        BaseDataDir=getBaseAppPath()+BaseDataRelativeDir;        BaseDataURL="file:///"+BaseDataDir;      }    else      BaseDataURL="http://"+BaseDataDomain+BaseDataRelativeDir;  }  /*
   * Returns path to base running directory.
   * JAR path stuff horked from the internet
   *
   * @param o - object to use for getting base path
   * @return string with base path
   */
  static String getBaseAppPath()
  {
    /* jar path */
    Class c=new AppContext().getClass();
    String JARurl=c.getResource(
      "/"+c.getName().replaceAll("\\.","/")+".class").toString();
    JARurl=JARurl.substring(4).replaceFirst("/[^/]+\\.jar!.*$","/");
    try
      {
        File dir=new File(new URL(JARurl).toURI());
        JARurl=dir.getAbsolutePath();
      }
    catch (MalformedURLException mue)
      {
        JARurl=null;
      }
    catch (URISyntaxException ue)
      {
        JARurl=null;
      }

    /* regular running path */
    String DIRurl=null;
    try
      {
        DIRurl=new File(".").getCanonicalPath();
      }
    catch (Exception e)
      {
        DIRurl=null;
      }

    return JARurl==null ? DIRurl : JARurl;
  }
}