/*----------------------------------------------------------------------*/
/*

        Module          : VariantReadingPanel.java

        Package         : Gfx

        Classes Included: VariantReadingPanel

        Purpose         : Panel displaying one reading in musical notation

        Programmer      : Ted Dumitrescu

        Date Started    : 7/19/08 (moved out of functions in VariantDisplayFrame)

        Updates         :
8/4/08: Converted drawing system to on-the-fly painting instead of pre-drawing
        into BufferedImage (to save memory when large numbers of
        VariantReadingPanels are kept in memory simultaneously for a
        critical notes window)

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.util.*;
import DataStruct.*;

/*------------------------------------------------------------------------
Class:   VariantReadingPanel
Extends: JPanel
Purpose: Displays one reading
------------------------------------------------------------------------*/

public class VariantReadingPanel extends JPanel
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static final int IMGYSPACE=100,
                   IMGXBUFFER=25,
                   YINDENT=30;

/*----------------------------------------------------------------------*/
/* Instance variables */

  MusicFont MusicGfx;
  float     STAFFSCALE,VIEWSCALE;
  int       IMGXSPACE;
  double    evXSpace;
  Dimension imgSize;

  StaffEventData renderedStaff;
  boolean        error;
  ArrayList<VariantVersionData> displayedVersions;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantReadingPanel([VoiceEventListData v,int starti,RenderedClefSet rcs,|
                                  VariantReading vr,RenderedClefSet rcs,]boolean error,
                                 MusicFont MusicGfx,float STAFFSCALE,float VIEWSCALE)
Parameters:
  Input:  
  Output: -
------------------------------------------------------------------------*/

  /* create reading image out of voice list data */
  public VariantReadingPanel(VoiceEventListData v,int starti,RenderedClefSet rcs,
                             boolean error,
                             MusicFont MusicGfx,float STAFFSCALE,float VIEWSCALE)
  {
    super();

    if (v.getEvent(starti).geteventtype()!=Event.EVENT_VARIANTDATA_START)
      {
        System.out.println("Error: attempting to initialize variant panel with non-variant data");
        v.getEvent(starti).prettyprint();
      }

    renderedStaff=createVariantStaff();
    initRenderingParams(MusicGfx,STAFFSCALE,VIEWSCALE);
    this.error=error;

    /* render into staff */
    int   i=starti+1,
          iadd;
    Event e=v.getEvent(i);
    while (e.geteventtype()!=Event.EVENT_VARIANTDATA_END)
      {
        iadd=1;
        if (e.geteventtype()==Event.EVENT_NOTE &&
            ((NoteEvent)e).isligated())
          iadd=renderedStaff.addlig(v,i,new RenderParams(rcs),true);
        else
          renderedStaff.addevent(true,e,new RenderParams(rcs));
        i+=iadd;
        e=v.getEvent(i);

        if (e==null)
          {
            System.out.println("no varend starti="+starti+" i="+i);
            for (int tmpi=starti; tmpi<i; tmpi++)
              v.getEvent(tmpi).prettyprint();
          }
      }

    initPaintingParams();
  }

  /* create reading image out of variant reading data */
  public VariantReadingPanel(VariantReading vr,RenderedClefSet rcs,
                             boolean error,
                             MusicFont MusicGfx,float STAFFSCALE,float VIEWSCALE)
  {
    super();

    renderedStaff=createVariantStaff();
    initRenderingParams(MusicGfx,STAFFSCALE,VIEWSCALE);
    this.error=error;

    /* render into staff */
    int   i=0,
          iadd,
          numEvents=vr.getNumEvents();
    Event e=vr.getEvent(i);
    while (i<numEvents)
      {
        iadd=1;
        if (e.geteventtype()==Event.EVENT_NOTE &&
            ((NoteEvent)e).isligated())
          iadd=renderedStaff.addlig(vr.getEvents(),i,new RenderParams(rcs));
        else
          renderedStaff.addevent(true,e,new RenderParams(rcs));
        i+=iadd;
        e=vr.getEvent(i);
      }

    initPaintingParams();
  }

  void initRenderingParams(MusicFont MusicGfx,float STAFFSCALE,float VIEWSCALE)
  {
    this.MusicGfx=MusicGfx;
    this.STAFFSCALE=STAFFSCALE;
    this.VIEWSCALE=VIEWSCALE;
  }

  void initPaintingParams()
  {
    evXSpace=renderedStaff.size()>0 ?
      renderedStaff.padEvents(PartRenderer.MAX_PADDING) :
      IMGXBUFFER;
    IMGXSPACE=(int)evXSpace+2*IMGXBUFFER;
    imgSize=new Dimension(Math.round(IMGXSPACE*VIEWSCALE),Math.round(IMGYSPACE*VIEWSCALE));
    this.setBackground(Color.white);
  }

  /* create and initialize one staff renderer for variant display */
  StaffEventData createVariantStaff()
  {
    StaffEventData renStaff=new StaffEventData();
    renStaff.options.set_displayedittags(false);
    renStaff.options.set_usemodernclefs(false);
    renStaff.options.set_displayorigligatures(true);
    renStaff.options.set_modacc_type(OptionSet.OPT_MODACC_NONE);
    renStaff.options.setViewEdCommentary(true);
    renStaff.options.set_unscoredDisplay(true);

    return renStaff;
  }

/*------------------------------------------------------------------------
Method:  void saveImgFile(String fn)
Purpose: Save img of this reading to file
Parameters:
  Input:  String fn - filename
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void saveImgFile(String fn) throws Exception
  {
    BufferedImage img=new BufferedImage(
      imgSize.width,imgSize.height,BufferedImage.TYPE_INT_RGB);
    Graphics g=img.createGraphics();
    g.setColor(Color.white);
    g.fillRect(0,0,imgSize.width+1,imgSize.height+1);
    paintComponent(g);

    ImageIO.write(img,"jpg",new File(fn));
  }

/*------------------------------------------------------------------------
Method:  Dimension getPreferredSize()
Purpose: Return canvas size preference
Parameters:
  Input:  -
  Output: -
  Return: size preference
------------------------------------------------------------------------*/

  public Dimension getPreferredSize()
  {
    return imgSize;
  }

/*------------------------------------------------------------------------
Method:  void paintComponent(Graphics g)
Purpose: Repaint area
Parameters:
  Input:  Graphics g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  int redisplaying=0;

  public void paintComponent(Graphics g)
  {
    redisplaying++;
    super.paintComponent(g);

    Graphics2D varG=(Graphics2D)g;
    varG.setBackground(Color.white);
    varG.clearRect(0,0,IMGXSPACE,IMGYSPACE);
    varG.setColor(Color.black);

    if (renderedStaff.size()>0)
      {
        ViewCanvas.drawStaff(varG,(float)(IMGXBUFFER*VIEWSCALE),(float)((IMGXBUFFER+evXSpace)*VIEWSCALE),(float)(YINDENT*VIEWSCALE),5,STAFFSCALE,VIEWSCALE);
        for (int ei=0; ei<renderedStaff.size(); ei++)
          {
            RenderedEvent re=renderedStaff.getEvent(ei);
            re.draw(varG,MusicGfx,this,
                    (IMGXBUFFER+re.getxloc())*VIEWSCALE,YINDENT*VIEWSCALE,VIEWSCALE);
          }
      }

    if (error)
      {
        varG.setColor(Color.red);
        varG.setFont(MusicGfx.defaultTextFont);
        varG.drawString("x",10f*VIEWSCALE,15f*VIEWSCALE);
      }

    redisplaying--;
  }
}