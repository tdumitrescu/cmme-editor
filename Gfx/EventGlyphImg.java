/*----------------------------------------------------------------------*/
/*

        Module          : EventGlyphImg

        Package         : Gfx

        Classes	Included: EventGlyphImg

        Purpose         : Low-level information for drawing one glyph image
                          within an event (notehead, flag, etc)

        Programmer      : Ted Dumitrescu

        Date Started    : 2005 (moved to separate file 7/25/05)

        Updates         :
7/26/05: added unscaled XY values (for typesetting)
9/2/05:  converted 'EventImg' to abstract class to add support for non-glyph
images (e.g., EventShapeImg)

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.image.*;
import DataStruct.Coloration;

/*------------------------------------------------------------------------
Class:   EventGlyphImg
Extends: EventImg
Purpose: Information about one image for a rendered event
------------------------------------------------------------------------*/

public class EventGlyphImg extends EventImg
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public char imgnum; /* index of image in music font */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: EventGlyphImg(int in,int syp,double xo,double yo,double uxo,double uyo,int c)
Purpose:     Initialize image information
Parameters:
  Input:  int in        - image number
          int syp       - staff y position
          double xo,yo   - XY offset for display
          double uxo,uyo - unscaled XY offset
          int c         - color
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public EventGlyphImg(int in,int syp,double xo,double yo,double uxo,double uyo,int c)
  {
    imgnum=(char)in;
    color=c;
    staffypos=syp;
    xsize=MusicFont.getDefaultGlyphWidth(imgnum);
    xoff=xo;
    yoff=yo;
    UNSCALEDxoff=uxo;
    UNSCALEDyoff=uyo;
  }

/*------------------------------------------------------------------------
Method:  void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,int c,double VIEWSCALE)
Purpose: Draws image into given graphical context
Parameters:
  Input:  Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          ImageObserver ImO - observer for drawImage
          double xl,yl      - location of event in graphical context
          int c             - color
          double VIEWSCALE  - scaling factor
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,int c,double VIEWSCALE)
  {
    if (this.color==Coloration.GRAY)
      c=this.color; /* cannot override gray */

    if (imgnum!=-1)
      mf.drawGlyph(
        g,imgnum,
        xl+(xoff+5)*VIEWSCALE,yl-(yoff-MusicFont.PICYCENTER)*VIEWSCALE,
        Coloration.AWTColors[c]);
  }
}
