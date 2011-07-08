/*----------------------------------------------------------------------*/
/*

        Module          : ProgressInputStream

        Package         : Util

        Classes Included: ProgressInputStream

        Purpose         : BufferedInputStream which provides updates on progress
                          to a JProgressBar

        Programmer      : Ted Dumitrescu

        Date Started    : 6/22/2007

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Util;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import javax.swing.JProgressBar;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   ProgressGZIPInputStream
Extends: java.util.zip.GZIPInputStream
Purpose: provide progress updates while reading from GZIP stream
------------------------------------------------------------------------*/

public class ProgressInputStream extends BufferedInputStream
{
/*----------------------------------------------------------------------*/
/* Class variables */

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance variables */

  JProgressBar progressBar;
  int          contentLen,    /* length of stream */
               PBStart,PBEnd, /* numbers for setting progress bar
                                              at start and end of read */
               curStreamPos;
  double       curBarPos,lastBarPos;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ProgressInputStream(InputStream in,JProgressBar progressBar,
                                 int contentLen,int PBStart,int PBEnd)
Purpose:     Initialize stream with progress bar attributes
Parameters:
  Input:  InputStream in           - stream from which to read data
          JProgressBar progressBar - bar to update
          int contentLen           - length of stream
          int PBStart,PBEnd        - progress bar positions at start and end
                                     of read
  Output: -
------------------------------------------------------------------------*/

  public ProgressInputStream(InputStream in,JProgressBar progressBar,
                             int contentLen,int PBStart,int PBEnd) throws IOException
  {
    super(in);
    this.progressBar=progressBar;
    this.contentLen=contentLen;
    this.PBStart=PBStart;
    this.PBEnd=PBEnd;
    curStreamPos=0;
    curBarPos=lastBarPos=(double)PBStart;
  }

  public int read(byte[] b,int off,int len) throws IOException
  {
    int amountRead=super.read(b,off,len);

    if (amountRead>=0)
      {
        curStreamPos+=amountRead;
        curBarPos+=((double)amountRead/(double)contentLen)*(PBEnd-PBStart);
        if (curBarPos>lastBarPos+10)
          {
            progressBar.setValue((int)Math.round(curBarPos));
            lastBarPos=curBarPos;
          }
      }
    else
      progressBar.setValue(PBEnd); /* EOF */

    return amountRead;
  }
}
