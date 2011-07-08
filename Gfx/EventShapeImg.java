/*----------------------------------------------------------------------*/
/*

        Module          : EventShapeImg

        Package         : Gfx

        Classes	Included: EventShapeImg

        Purpose         : Low-level information for drawing one 2d shape
                          image within an event (e.g., stem)

        Programmer      : Ted Dumitrescu

        Date Started    : 9/2/05

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;

import DataStruct.Coloration;

/*------------------------------------------------------------------------
Class:   EventShapeImg
Extends: EventImg
Purpose: Information about one image for a rendered event
------------------------------------------------------------------------*/

public class EventShapeImg extends EventImg
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Shape   screenshape; /* shape outline for screen */
  Color   shapecolor;

  public float   printshapex[],printshapey[];
  public boolean filled;

  public boolean multipleypos=false;
  public int     staffypos2;
  public int     yswitchnum; /* switch from staffypos to staffypos2 after
                                this number of points */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: EventShapeImg(Shape ss,Shape ps)
Purpose:     Initialize image information
Parameters:
  Input:  Shape ss,float psx[],psy[] - shape outlines
          int c,cf                   - coloration parameters
          int ssn1,ssn2              - staff y position(s)
          int switchn                - y switch number
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public EventShapeImg(Shape ss,float psx[],float psy[],int c,int cf,int ssn1)
  {
    screenshape=ss;
    printshapex=psx;
    printshapey=psy;
    xoff=(float)(ss.getBounds().getX());
    xsize=(int)(ss.getBounds().getWidth());
    color=c;
    shapecolor=Coloration.AWTColors[color];
    filled=cf==Coloration.FULL;
    staffypos=ssn1;
  }

  public EventShapeImg(Shape ss,float psx[],float psy[],int c,int cf,int ssn1,int ssn2,int switchn)
  {
    this(ss,psx,psy,c,cf,ssn1);
    multipleypos=true;
    staffypos2=ssn2;
    yswitchnum=switchn;
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

  public void draw(Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,int c,double VIEWSCALE)
  {
    if (this.color==Coloration.GRAY)
      c=this.color; /* cannot override gray */

    g.setColor(Coloration.AWTColors[c]);
    AffineTransform saveAT=g.getTransform();
    g.translate(xl,yl);
    g.scale(VIEWSCALE,VIEWSCALE);
    g.translate(5,0);

    g.draw(screenshape);
    if (filled)
      g.fill(screenshape);

    g.setTransform(saveAT);
  }
}
