/*----------------------------------------------------------------------*/
/*

        Module          : Main.java

        Package         : Editor

        Classes Included: Main

        Purpose         : initialization/administration

        Programmer      : Ted Dumitrescu

        Date Started    : 5/3/05

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import javax.swing.*;
import java.net.*;
import java.util.Enumeration;
import java.util.Properties;
import java.io.*;

import DataStruct.CMMEParser;
import DataStruct.MetaData;
import Util.AppContext;

/*------------------------------------------------------------------------
Class:   Main
Extends: -
Purpose: Contains main routines
------------------------------------------------------------------------*/

public class Main
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static String initfilename,
                curdir;

/*----------------------------------------------------------------------*/
/* Instance variables */

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  void main(String args[])
Purpose: Perform initializations as an application
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void main(String args[])
  {
    setProperties();
    parsecmdline(args);
    init();
  }

/*------------------------------------------------------------------------
Method:  void setProperties()
Purpose: load properties from the cmme.properties file, 
         and make them globally available
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/
  static void setProperties()
  {
    String dir = System.getProperty("user.dir");
    System.out.println("Info: loading properties from: " + dir);
    File file = new File(dir, "cmme.properties");
    Properties properties = new Properties();
    try {
      properties.load( new FileInputStream(file) );
      Enumeration<?> names = properties.propertyNames();
      String name, value;
      while (names.hasMoreElements()) {
        name = (String)names.nextElement();
        value = properties.getProperty( name );
        System.setProperty( name, value );
        
      }
    } catch ( FileNotFoundException e ) {
      e.printStackTrace();
    } catch ( IOException e ) {
      e.printStackTrace();
    }
  }

/*------------------------------------------------------------------------
Method:  void parsecmdline(String args[])
Purpose: Parse command line
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: -
------------------------------------------------------------------------*/
  static void parsecmdline(String args[])
  {
    if (args.length>1)
      usageexit();
    initfilename=args.length>0 ? args[0] : null;
  }

/*------------------------------------------------------------------------
Method:  void usagexit()
Purpose: Print program usage and exit
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void usageexit()
  {
    System.out.println("Usage: java Editor.Main [filename]");
    System.exit(0);
  }

/*------------------------------------------------------------------------
Method:  void init()
Purpose: Perform initializations
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void init()
  {
    MetaData.CMME_OPT_TESTING=AppContext.CMME_OPT_TESTING;
    MetaData.CMME_OPT_VALIDATEXML=AppContext.CMME_OPT_VALIDATEXML;

    /* initialize data locations */
    try
      {
        AppContext.setBaseDataLocations(null,true,false);
      }
    catch (Exception e)
      {
        showError("Error loading local file locations: "+e);
      }

    /* initialize edit window objects */
    EditorWin.initScoreWindowing(AppContext.BaseDataURL,AppContext.BaseDataDir+"music/",false);

    /* load XML parser */
    //TODO: make a command line option to specify schema location.
    DataStruct.XMLReader.initparser(null,true);

    /* load base music font */
    try
      {
        Gfx.MusicFont.loadmusicface(AppContext.BaseDataURL);
      }
    catch (Exception e)
      {
        showError("Error loading font: "+e);
      }

    /* start GUI on event dispatching thread */
    SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          if (initfilename!=null)
            new EditorWin().openFile(curdir+"\\"+initfilename);
          else
            EditorWin.newfile();
        }
      });
  }

/*------------------------------------------------------------------------
Method:  void showError(String e)
Purpose: Show error information
Parameters:
  Input:  String e - error info
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void showError(String e)
  {
    System.err.println(e);
  }
}
