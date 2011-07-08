/*----------------------------------------------------------------------*/
/*

        Module          : RecursiveFileList.java

        Package         : Util

        Classes Included: RecursiveFileList,WildcardFilter

        Purpose         : Creates lists of files within a directory structure

        Programmer      : Ted Dumitrescu
                          Wildcard-parsing tools based on code from nsf tools

        Date Started    : 7/14/07 (most code moved from ConvertCMME.java)

Updates:

                                                                        */
/*----------------------------------------------------------------------*/

package Util;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import java.net.*;
import java.util.*;


/*------------------------------------------------------------------------
Class:   RecursiveFileList
Extends: LinkedList<File>
Purpose: Create and store list of files matching a given pattern, recursing
         to subdirectories if requested
------------------------------------------------------------------------*/

public class RecursiveFileList extends LinkedList<File>
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  boolean recursive=false;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RecursiveFileList(String mainFilename,boolean recursive)
Purpose:     Create file list
Parameters:
  Input:  String mainFilename - name of file or directory to list
          boolean recursive   - whether to recurse through subdirectories
  Output: -
------------------------------------------------------------------------*/

  public RecursiveFileList(String mainFilename,boolean recursive)
  {
    super();
    this.recursive=recursive;
    createFileList(mainFilename,null);
  }

/*------------------------------------------------------------------------
Method:  void createFileList(String mainFilename,String subdirName)
Purpose: Convert one set of files (recursing to subdirectories if necessary)
Parameters:
  Input:  String mainFilename - name of file set in one directory
          String subdirName   - name of subdirectory (null for base directory)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createFileList(String mainFilename,String subdirName)
  {
    String fn="";

    try
      {
        String dirname,filePattern,
               filenames[];

        /* get directory name and file pattern */
        int sepi=mainFilename.lastIndexOf(File.separator);
        dirname=mainFilename.substring(0,sepi);
        if (sepi<mainFilename.length()-1)
          filePattern=mainFilename.substring(sepi+1);
        else
          filePattern=mainFilename;

        /* now get file list */
        File dirf=new File(dirname);
        filenames=dirf.list(new WildcardFilter(filePattern));

        for (int i=0; i<filenames.length; i++)
          {
            fn=dirname+File.separator+filenames[i];
            File   curfile=new File(fn);
            String curShortname=filenames[i];
            if (subdirName!=null)
              curShortname=subdirName+File.separator+curShortname;

            if (recursive && curfile.isDirectory())
              createFileList(fn+File.separator+"*",curShortname);
            else
              if (filenames[i].matches(".*\\.[Cc][Mm][Mm][Ee]\\.[Xx][Mm][Ll]")) /* skip non-.cmme.xml files */
                this.add(curfile);
          }
      }
    catch (Exception e)
      {
        System.err.println("Error at "+fn+": "+e);
        e.printStackTrace();
      }
  }
}


/* wildcard functions based on code from nsf tools
  (http://www.nsftools.com/tips/JavaTips.htm) */
class WildcardFilter implements FilenameFilter
{
  String mainPattern;

  public WildcardFilter(String p)
  {
    mainPattern=p.toLowerCase();
  }

  public boolean accept(File dir,String name)
  {
    /* case-insensitive for Windows/DOS filenames */
    String ldir=dir.toString().toLowerCase();
    String lname=name.toLowerCase();

    return (isMatch(ldir+dir.separator+lname,mainPattern) ||
            isMatch(lname,mainPattern));
  }

  boolean isMatch(String checkString,String pattern)
  {
    char patternChar;
    int  patternPos=0;
    char lastPatternChar;
    char thisChar;
    int  i,j;

    for (i=0; i<checkString.length(); i++)
      {
        // if we're at the end of the pattern but not the end
        // of the string, return false
        if (patternPos>=pattern.length())
          return false;

        // grab the characters we'll be looking at
        patternChar=pattern.charAt(patternPos);
        thisChar=checkString.charAt(i);

        switch(patternChar)
          {
            // check for '*', which is zero or more characters
            case '*':
              // if this is the last thing we're matching,
              // we have a match
              if (patternPos>=(pattern.length()-1))
                return true;

              // otherwise, do a recursive search
              for (j=i; j<checkString.length(); j++)
                if (isMatch(checkString.substring(j),pattern.substring(patternPos + 1)))
                  return true;

              // if we never returned from that, there is no match
              return false;

            // check for '?', which is a single character
            case '?':
              // do nothing, just advance the patternPos at the end
              break;

            // check for '[', which indicates a range of characters
            case '[':
              // if there's nothing after the bracket, we have
              // a syntax problem
              if (patternPos>=(pattern.length()-1))
                return false;

              lastPatternChar='\u0000';
              for (j=patternPos+1; j<pattern.length(); j++)
                {
                  patternChar=pattern.charAt(j);
                  if (patternChar==']')
                    return false;
                  else if (patternChar=='-')
                    {
                      // we're matching a range of characters
                      j++;
                      if (j==pattern.length())
                        return false; // bad syntax

                      patternChar=pattern.charAt(j);
                      if (patternChar==']')
                        return false;	// bad syntax
                      else
                        if ((thisChar>=lastPatternChar) && (thisChar<=patternChar))
                          break; // found a match
                    }
                  else if (thisChar==patternChar)
                    // if we got here, we're doing an exact match
                    break;

                  lastPatternChar = patternChar;
                }

              // if we broke out of the loop, advance to the end bracket
              patternPos=j;
              for (j=patternPos; j<pattern.length(); j++)
                if (pattern.charAt(j)==']')
                  break;
              patternPos=j;
              break;

            default:
              // the default condition is to do an exact character match
              if (thisChar!=patternChar)
                return false;
          }

        // advance the patternPos before we loop again
        patternPos++;

      }

    // if there's still something in the pattern string, check to
    // see if it's one or more '*' characters. If that's all it is,
    // just advance to the end
    for (j=patternPos; j<pattern.length(); j++)
      if (pattern.charAt(j)!='*')
        break;
    patternPos=j;

    // at the end of all this, if we're at the end of the pattern
    // then we have a good match
    return patternPos==pattern.length();
  }
}
