/*----------------------------------------------------------------------*/
/*

        Module          : ConvertCMME.java

        Package         : Util

        Classes Included: ConvertCMME,WildcardFilter

        Purpose         : Convert files to/from different CMME formats

        Programmer      : Ted Dumitrescu
                          Wildcard-parsing tools based on code from nsf tools

        Date Started    : 3/3/05

Updates:
2/23/06: re-added to software suite; now converts XML->XML (for format
         changes within the XML-based representation)
3/21/06: added support for wildcards in filenames (/multi-file conversions
         in one run)
3/24/07: added support for recursive traversal of subdirectories
7/14/07: moved recursive file-seeking functions to module RecursiveFileList

                                                                        */
/*----------------------------------------------------------------------*/

package Util;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import java.net.*;
import java.util.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   ConvertCMME
Extends: -
Purpose: Convert files to/from different CMME formats
------------------------------------------------------------------------*/

public class ConvertCMME
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static boolean screenoutput=false,
                 recursive=false;

  public static final String BaseDataDir="/data/";
  public static String       BaseDataURL;

  static String initdirectory;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  void main(String args[])
Purpose: Main routine
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void main(String args[])
  {
    String cmdlineFilename=parseCmdLine(args);

    /* initialize data locations */
    try
      {
        initdirectory=new File(".").getCanonicalPath()+BaseDataDir;
        BaseDataURL="file:///"+initdirectory;
      }
    catch (Exception e)
      {
        System.err.println("Error loading local file locations: "+e);
        e.printStackTrace();
      }

    DataStruct.XMLReader.initparser(BaseDataURL,false);
    convertFiles(cmdlineFilename);
  }

/*------------------------------------------------------------------------
Method:  void convertFiles(String mainFilename)
Purpose: Convert one set of files (recursing to subdirectories if necessary)
Parameters:
  Input:  String mainFilename - name of file set in one directory
          String subdirName   - name of subdirectory (null for base directory)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void convertFiles(String mainFilename)
  {
    try
      {
        OutputStream outs;

        RecursiveFileList fl=new RecursiveFileList(mainFilename,recursive);
        for (File curfile : fl)
          {
            URL fileURL=curfile.toURI().toURL();
            String fileName=curfile.getName(),
                   fileVersion=CMMEParser.getFileVersion(fileURL);
            if (Float.valueOf(fileVersion).floatValue()>=0.8)
              System.out.println(fileName+": already v. "+fileVersion+", skipping...");
            else
              {
                System.out.print("Converting: "+fileName+"...");

                PieceData musicdat=new CMMEOldVersionParser(fileURL).piece;
                musicdat=convertCMMEData(musicdat);

                outs=screenoutput ? System.out : new FileOutputStream(curfile);
                CMMEParser.outputPieceData(musicdat,outs);
                if (!screenoutput)
                  outs.close();

                System.out.println("done");
              }
          }
      }
    catch (Exception e)
      {
        System.err.println("Error: "+e);
        e.printStackTrace();
      }
  }

/*------------------------------------------------------------------------
Method:  String parseCmdLine(String args[])
Purpose: Parse command line
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: filename (or "*" if recursive with no filename specified)
------------------------------------------------------------------------*/

  static String parseCmdLine(String args[])
  {
    String fn=null;

    if (args.length<1)
      usage_exit();

    for (int i=0; i<args.length; i++)
      if (args[i].charAt(0)=='-')
        /* options */
        for (int opti=1; opti<args[i].length(); opti++)
          switch (args[i].charAt(opti))
            {
              case 's':
                screenoutput=true;
                break;
              case 'r':
                recursive=true;
                break;
              default:
                usage_exit();
            }
      else
        /* filename */
        if (i!=args.length-1)
          usage_exit();
        else
          fn=args[i];

    if (fn==null)
      if (recursive)
        fn="*";
      else
        usage_exit();

    return "data\\music\\"+fn;
  }

/*------------------------------------------------------------------------
Method:  void usage_exit()
Purpose: Exit for invalid command line
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void usage_exit()
  {
    System.err.println("Usage: java Util.ConvertCMME [options] filename");
    System.err.println("Options:");
    System.err.println("  -s: Screen output");
    System.err.println("  -r: Recursively search subdirectories");
    System.exit(1);
  }

/*------------------------------------------------------------------------
Method:  PieceData convertCMMEData(PieceData p)
Purpose: Transform data from old to new format (internal representation)
Parameters:
  Input:  PieceData p - music data
  Output: -
  Return: converted music data
------------------------------------------------------------------------*/

  static PieceData convertCMMEData(PieceData p)
  {
    /* version .67 conversion: note lengths now to be expressed in terms of
       minims, not breves 

    int lengthMultiplier=4;  4 minims to the breve in our "default" mensuration 

    Voice[] v=p.getVoiceData();
    for (int vi=0; vi<v.length; vi++)
      for (int ei=0; ei<v[vi].getnumevents(); ei++)
        {
          Event e=v[vi].getevent(ei);

           set base mensuration info (number of minims in a breve) 
          Mensuration mensInfo=e.getMensInfo();
          if (mensInfo!=null)
            {
              lengthMultiplier=mensInfo.prolatio==Mensuration.MENS_BINARY ? 2 : 3;
              lengthMultiplier*=mensInfo.tempus==Mensuration.MENS_BINARY ? 2 : 3;
            }

           adjust lengths of timed events 
          Proportion l=e.getLength();
          if (l!=null)
            l.multiply(lengthMultiplier,1);

           mark all flats as signature clefs by default 
          if (e.geteventtype()==Event.EVENT_CLEF)
            markFlatAsSig((ClefEvent)e);
          else if (e.geteventtype()==Event.EVENT_MULTIEVENT)
            for (Iterator i=((MultiEvent)e).iterator(); i.hasNext();)
              {
                Event cure=(Event)i.next();
                if (cure.geteventtype()==Event.EVENT_CLEF)
                  markFlatAsSig((ClefEvent)cure);
              }
        }*/

    return p;
  }

  static void markFlatAsSig(ClefEvent ce)
  {
    Clef c=ce.getClef(false,false);
    if (c.isflat())
      ce.setSignature(true);
  }
}

