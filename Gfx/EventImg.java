/*----------------------------------------------------------------------*/
/*

	Module		: EventImg

        Package         : Gfx

        Classes	Included: EventImg

	Purpose		: To be extended with low-level drawing information
                          for one image within an event (notehead, stem, etc)

        Programmer	: Ted Dumitrescu

	Date Started	: 9/2/05 (original EventImg moved to EventGlyphImg)

	Updates		:
11/8/06: Converted drawing location coordinates from int to float (for
         accuracy in scaling)

									*/
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.image.*;

/*------------------------------------------------------------------------
Class:   EventImg
Extends: -
Purpose: Information about one image for a rendered event
------------------------------------------------------------------------*/

public abstract class EventImg
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public int   staffypos, /* base y position on staff */
               xsize,     /* horizontal space occupied by image */
               color;
  public double xoff,yoff, /* offset from event beginning */
               UNSCALEDxoff,UNSCALEDyoff; /* in 1/1000 em square units */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Method:  void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl[,int c,double VIEWSCALE])
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

  public abstract void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                            double xl,double yl,int c,double VIEWSCALE);

  public void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,double VIEWSCALE)
  {
    draw(g,mf,ImO,xl,yl,color,VIEWSCALE);
  }

  public void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl)
  {
    draw(g,mf,ImO,xl,yl,color);
  }

  public void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,int c)
  {
    draw(g,mf,ImO,xl,yl,c,1.0f);
  }
}
