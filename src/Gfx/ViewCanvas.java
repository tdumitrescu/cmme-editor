/*----------------------------------------------------------------------*/
/*

        Module          : ViewCanvas.java

        Package         : Gfx

        Classes Included: ViewCanvas

        Purpose         : Handles music-displaying area

        Programmer      : Ted Dumitrescu

        Date Started    : 99 (moved to separate file 5/2/05)

Updates:
5/23/05:  removed custom double-buffering code (Swing double-buffers
          automatically)
3/20/06:  added ellipsis-drawing support (for incipit-scores)
11/3/06:  removed Swing/AWT-based canvas scaling (too slow); to be replaced
          by custom code altering drawing sizes/scale based on user selection
11/8/06:  converted integer coordinate calculations to float (for accuracy in
          scaling)
12/5/06:  added mouse-listening functions (for selecting events to display
          editorial commentary)
2/20/09:  added line to show audio playback location
12/30/09: display code for text sections

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.util.*;

import DataStruct.*;

/*------------------------------------------------------------------------

/*------------------------------------------------------------------------
Class:   ViewCanvas
Extends: JComponent
Purpose: Handles music-displaying area
------------------------------------------------------------------------*/

public class ViewCanvas extends JComponent implements MouseListener
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static final float  DEFAULT_TEXTFONTSMALL_SIZE=12,
                      DEFAULT_TEXTFONTLARGE_SIZE=18,
                      LEFTBREAK_XSIZE=10,
                      LEFTINFO_XPADDING=5,
                      LEFT_BLANK_BORDER=10;

/*----------------------------------------------------------------------*/
/* Instance variables */

  public MusicWin parentwin;

  protected PieceData musicData;
  protected MusicFont MusicGfx;
  protected OptionSet options;

  public int                   numSections,leftRendererNum;
  public ScoreRenderer         renderedSections[];
  protected VariantVersionData curVariantVersion;
  protected PieceData          curVersionMusicData; /* music data for
                                                       currently active
                                                       variant version */

  public int nummeasures;

  public Dimension viewsize,   /* size of view area, before scaling */
                   screensize; /* screen dimensions of view area (after scaling) */
  public int SCREEN_MINHEIGHT; /* minimum height of view area */

  /* offscreen buffers */
  BufferedImage curbuffer;
  Graphics2D    curbufferg2d;

  /* music display parameters */
  public int   STAFFSCALE,    /* # of pixels per staff line+space */
               STAFFPOSSCALE, /* # of pixels per staff position (line or space) */
               STAFFSPACING,  /* number of staff spaces for each voice's vertical space */
               BREVESCALE;    /* default # of horizontal pixels for one breve of time */
  public float VIEWSCALE,     /* scaling factor (for zooming in/out) */
               XLEFT,YTOP,
               ORIGXLEFT,
               VIEWYSTART,
               TEXT_NAMES_SIZE;
  public int   curmeasure,
               selectedVoicenum,
               selectedEventnum;
  public int   numvoices;

  String voicelabels[];
  Font   FontPLAIN12,FontBOLD18,
         defaultTextFontSMALL,defaultTextFontLARGE;

  /* options loaded at the beginning of a draw */
  int     barline_type;
  boolean usemodernclefs,
          useModernAccSystem,
          displayligbrackets,
          displayEditTags,
          displayVarTexts,
          markdissonances,
          markdirectedprogressions;

  public int  redisplaying=0;   /* number of paintComponent calls currently running */
  public byte num_redisplays=0;
  int         nummeasuresdisplayed=0,
              MIDIMeasurePlaying=-1;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ViewCanvas(PieceData p,MusicFont mf,MusicWin mw,OptionSet os)
Purpose:     Initialize canvas
Parameters:
  Input:  PieceData p  - music data to display
          MusicFont mf - music symbols for drawing
          MusicWin mw  - parent window
          OptionSet os - display options
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ViewCanvas(PieceData p,MusicFont mf,MusicWin mw,OptionSet os)
  {
    MusicGfx=mf;
    musicData=p;
    parentwin=mw;
    options=os;
    curVariantVersion=musicData.getDefaultVariantVersion();
    curVersionMusicData=musicData;

    addMouseListener(this);

    initdrawingparams();
  }

  /* real initialization */
  public void initdrawingparams()
  {
    numvoices=curVersionMusicData.getVoiceData().length;
    curmeasure=0;
    selectedVoicenum=selectedEventnum=-1;

    renderSections();

    loadoptions();
    viewsize=new Dimension(0,0);
//System.out.println("SCREEN_MINHEIGHT="+SCREEN_MINHEIGHT);
    screensize=new Dimension(Math.round(1200*VIEWSCALE),Math.round(SCREEN_MINHEIGHT*VIEWSCALE));
    Dimension actualDisplaySize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    if (screensize.width>(float)actualDisplaySize.width*.9)
      screensize.width=(int)((float)actualDisplaySize.width*.9);
    if (screensize.height>(float)actualDisplaySize.height*.9)
      screensize.height=(int)((float)actualDisplaySize.height*.9);
    VIEWYSTART=0;

    FontPLAIN12=new Font(null,Font.PLAIN,12);
    FontBOLD18=new Font(null,Font.BOLD,18);
    initVoiceLabels();
    initbuffers();
  }

/*------------------------------------------------------------------------
Method:  void renderSections()
Purpose: Pre-render all sections for display
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void renderSections()
  {
    numSections=curVersionMusicData.getNumSections();
    renderedSections=ScoreRenderer.renderSections(curVersionMusicData,options);

    leftRendererNum=0;
    nummeasures=0;
    for (int i=0; i<numSections; i++)
      nummeasures+=renderedSections[i].getNumMeasures();
  }

/*------------------------------------------------------------------------
Method:  void loadoptions()
Purpose: Get low-level drawing options from OptionSet (for quick access
         during drawing routines)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void loadoptions()
  {
    STAFFSCALE=options.getSTAFFSCALE();
    STAFFPOSSCALE=STAFFSCALE/2;
    STAFFSPACING=options.getSTAFFSPACING();
    BREVESCALE=options.getBREVESCALE();
    VIEWSCALE=(float)options.getVIEWSCALE();
    SCREEN_MINHEIGHT=curVersionMusicData.getVoiceData().length*STAFFSCALE*STAFFSPACING+80;
  }

/*------------------------------------------------------------------------
Method:  void initVoiceLabels()
Purpose: Initialize text labels for voice names
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void initVoiceLabels()
  {
    String vn,
           avn; /* abbrev voice name */

    voicelabels=new String[numvoices];
    for (int i=0; i<numvoices; i++)
      {
        vn=curVersionMusicData.getVoiceData()[i].getName();
        if (vn.length()==0)
          avn="";
        else if (vn.length()>1 && vn.charAt(0)=='[')
          avn="["+vn.charAt(1)+"]";
        else
          avn=String.valueOf(vn.charAt(0));
        voicelabels[i]=avn;
      }
  }

/*------------------------------------------------------------------------
Method:  void initbuffers()
Purpose: Initialize offscreen buffers and drawing parameters
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void initbuffers()
  {
    /* calculate buffer sizes */
    loadoptions();
    int vssy=screensize.height>SCREEN_MINHEIGHT ? screensize.height : SCREEN_MINHEIGHT,
        pbsx=screensize.width, /* buffer size */
        pbsy=vssy;
    if (VIEWSCALE>1)
      pbsy*=VIEWSCALE;
    /* viewsize == size in virtual (scaled) coordinates */
    viewsize.setSize(pbsx,pbsy);

    /* create buffer */
    curbuffer=new BufferedImage(pbsx,pbsy,BufferedImage.TYPE_INT_ARGB);
    curbufferg2d=curbuffer.createGraphics();
/*    curbufferg2d.scale(VIEWSCALE,VIEWSCALE);
    if (VIEWSCALE<1)
      curbufferg2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);*/

    /* make text fonts */
    defaultTextFontSMALL=FontPLAIN12.deriveFont((float)DEFAULT_TEXTFONTSMALL_SIZE*VIEWSCALE);
    defaultTextFontLARGE=FontPLAIN12.deriveFont((float)DEFAULT_TEXTFONTLARGE_SIZE*VIEWSCALE);

    curbufferg2d.setFont(defaultTextFontSMALL);
    ORIGXLEFT=calcstaffleft(curbufferg2d);
    TEXT_NAMES_SIZE=calcTextNamesSize(curbufferg2d);
    YTOP=80*VIEWSCALE;
  }

/*------------------------------------------------------------------------
Method:  float calcstaffleft(Graphics2D g)
Purpose: Calculate left x location for staves
Parameters:
  Input:  -
  Output: -
  Return: left x location for staves
------------------------------------------------------------------------*/

  float calcstaffleft(Graphics2D g)
  {
    int         longestnamelen=0;
    FontMetrics metrics=g.getFontMetrics();

    for (int i=0; i<numvoices; i++)
      {
        String vn=voicelabels[i];
        if (metrics.stringWidth(vn)>longestnamelen)
          longestnamelen=metrics.stringWidth(vn);
      }

    return longestnamelen+LEFT_BLANK_BORDER*2*VIEWSCALE;
  }

  float calcTextNamesSize(Graphics2D g)
  {
    int         longestNameWidth=0;
    FontMetrics metrics=g.getFontMetrics();

    for (VariantVersionData vvd : musicData.getVariantVersions())
      {
        int nameWidth=metrics.stringWidth(vvd.getID());
        if (nameWidth>longestNameWidth)
          longestNameWidth=nameWidth;
      }

    return (float)longestNameWidth;
  }

/*------------------------------------------------------------------------
Method:  MeasureInfo getLeftMeasure()
Purpose: Return leftmost measure in score window
Parameters:
  Input:  -
  Output: -
  Return: measure info object
------------------------------------------------------------------------*/

  public MeasureInfo getLeftMeasure()
  {
    return renderedSections[leftRendererNum].getMeasure(curmeasure);
  }

/*------------------------------------------------------------------------
Method:  MeasureInfo getMeasure(int m)
Purpose: Retrieve measure from correct section
Parameters:
  Input:  int m - measure number
  Output: -
  Return: measure info object
------------------------------------------------------------------------*/

  public MeasureInfo getMeasure(int m)
  {
    return renderedSections[ScoreRenderer.calcRendererNum(renderedSections,m)].getMeasure(m);
  }

/*------------------------------------------------------------------------
Method:  double getMeasureX(int m)
Purpose: Calculate global x-location of any measure
Parameters:
  Input:  int m - measure number
  Output: -
  Return: x-coordinate at left of measure
------------------------------------------------------------------------*/

  public double getMeasureX(int m)
  {
    int snum=ScoreRenderer.calcRendererNum(renderedSections,m);
    return renderedSections[snum].getStartX()+renderedSections[snum].getMeasure(m).leftx;
  }

/*------------------------------------------------------------------------
Method:  float calcXLEFT(int mnum)
Purpose: Calculate value for XLEFT at a given measure
Parameters:
  Input:  int mnum - measure number
  Output: -
  Return: XLEFT value (x position at beginning of first displayed measure)
------------------------------------------------------------------------*/

  public float calcXLEFT(int mnum)
  {
    if (mnum==0)
      return ORIGXLEFT;

    int           snum=ScoreRenderer.calcRendererNum(renderedSections,mnum);
    MeasureInfo   leftMeasure=renderedSections[snum].getMeasure(mnum);
    RenderedEvent me;
    float         curXLEFT=ORIGXLEFT,
                  xloc=0,maxx=0;

    if (leftRendererNum>0 && curmeasure==renderedSections[leftRendererNum].getFirstMeasureNum())
      {
        RenderedSectionParams lastSectionParams[]=renderedSections[leftRendererNum-1].getEndingParams();
        for (int i=0; i<numvoices; i++)
          {
            xloc=XLEFT;

            /* clefs */
            RenderedClefSet curCS=lastSectionParams[i].getClefSet();
            if (curCS!=null)
              {
                xloc+=curCS.getXSize()*VIEWSCALE;

                /* modern key signature */
                ModernKeySignature mk=curCS.getLastClefEvent().getModernKeySig();
                if (mk.numEls()>0 && (displayEditTags || useModernAccSystem))
                  xloc+=getModKeySigSize(mk,curCS.getPrincipalClefEvent());
              }

            /* mensuration */
            me=lastSectionParams[i].getMens();
            if (me!=null)
              xloc+=me.getimgxsize()*VIEWSCALE;

            if (xloc>maxx)
              maxx=xloc;
          }
      }
    else
      for (int i=0; i<curVersionMusicData.getSection(snum).getNumVoices(); i++)
        if (curVersionMusicData.getSection(snum).getVoice(i)!=null)
          {
            xloc=curXLEFT;
            RenderedClefSet curCS=renderedSections[snum].eventinfo[i].getClefEvents(leftMeasure.reventindex[i]);
            if (curCS!=null)
              xloc+=curCS.getXSize()*VIEWSCALE;

            /* modern key signature */
            ModernKeySignature mk=renderedSections[snum].eventinfo[i].getModernKeySig(leftMeasure.reventindex[i]);
            if (mk.numEls()>0 && curCS!=null && (displayEditTags || useModernAccSystem))
              xloc+=getModKeySigSize(mk,curCS.getPrincipalClefEvent());

            me=leftMeasure.startMensEvent[i]; //renderedSections[snum].eventinfo[i].getMensEvent(leftMeasure.reventindex[i]);
            if (me!=null)
              {
                me=leftMeasure.startMensEvent[i];
                xloc+=me.getimgxsize()*VIEWSCALE;
              }

            if (xloc>maxx)
              maxx=xloc;
          }

    maxx+=LEFTINFO_XPADDING*VIEWSCALE;

    return maxx+LEFTBREAK_XSIZE*VIEWSCALE;
  }

/*------------------------------------------------------------------------
Method:  void forcePaint()
Purpose: Repaint immediately
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void forcePaint()
  {
    paintImmediately(0,0,screensize.width,screensize.height);
  }

/*------------------------------------------------------------------------
Method:  void rerender()
Purpose: Force immediate music rerendering
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void rerender()
  {
    int oldSCREEN_MINHEIGHT=SCREEN_MINHEIGHT;
    loadoptions();

    numvoices=curVersionMusicData.getVoiceData().length;
    initVoiceLabels();

    int oldnummeasures=nummeasures;
    renderSections();
    if (curmeasure>=nummeasures)
      curmeasure=nummeasures-1;
    if (nummeasures>oldnummeasures)
      {
        parentwin.setScrollBarXmax();
        parentwin.setmeasurenum(curmeasure+1);
      }
    if (SCREEN_MINHEIGHT!=oldSCREEN_MINHEIGHT)
      updateScrollBarY();
    parentwin.updatemusicgfx=true;
  }

  public void rerender(int snum)
  {
    int oldSCREEN_MINHEIGHT=SCREEN_MINHEIGHT;
    loadoptions();

    numvoices=curVersionMusicData.getVoiceData().length;
    initVoiceLabels();

    int oldnummeasures=nummeasures;

    renderedSections[snum].render();

    if (curmeasure>=nummeasures)
      curmeasure=nummeasures-1;
    if (nummeasures>oldnummeasures)
      {
        parentwin.setScrollBarXmax();
        parentwin.setmeasurenum(curmeasure+1);
      }
    if (SCREEN_MINHEIGHT!=oldSCREEN_MINHEIGHT)
      updateScrollBarY();
    parentwin.updatemusicgfx=true;
  }

/*------------------------------------------------------------------------
Method:  void update(Graphics g)
Purpose: Update graphics area
Parameters:
  Input:  Graphics g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void update(Graphics g)
  {
    paintComponent(g);
  }

/*------------------------------------------------------------------------
Method:  void paintComponent(Graphics g)
Purpose: Repaint area
Parameters:
  Input:  Graphics g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void paintComponent(Graphics g)
  {
    redisplaying++;
    super.paintComponent(g);

    /* rerender music if necessary */
    if (parentwin.rerendermusic)
      {
        parentwin.rerendermusic=false;
        rerender();
      }

    /* redraw music if necessary */
    if (parentwin.updatemusicgfx)
      {
        parentwin.updatemusicgfx=false;
        g.setClip(0,0,screensize.width,screensize.height);
        paintbuffer(curbufferg2d);

        parentwin.setScrollBarXmax();
        parentwin.setmeasurenum(curmeasure+1);
      }

    /* set parameters for redraw if necessary */
    if (parentwin.redrawscr)
      {
        parentwin.redrawscr=false;
        g.setClip(0,0,screensize.width,screensize.height);
      }

    /* copy current offscreen buffer to screen */
    g.drawImage(curbuffer,0,Math.round(0-VIEWYSTART),this);

    if (parentwin.updateMeasure!=-1)
      parentwin.gotomeasure(parentwin.updateMeasure);

    redisplaying--;
  }

/*------------------------------------------------------------------------
Method:  Dimension cursize()
Purpose: Return current canvas size
Parameters:
  Input:  -
  Output: -
  Return: current size
------------------------------------------------------------------------*/

  public Dimension cursize()
  {
//    return screensize;
    return getSize();
  }

/*------------------------------------------------------------------------
Method:  Graphics2D getbufferg2d()
Purpose: Return graphics for display buffer
Parameters:
  Input:  -
  Output: -
  Return: curbufferg2d
------------------------------------------------------------------------*/

  public Graphics2D getbufferg2d()
  {
    return curbufferg2d;
  }

/*------------------------------------------------------------------------
Method:  void newsize(int newwidth,int newheight)
Purpose: Resize canvas
Parameters:
  Input:  int newwidth,newheight - new size
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void newsize(int newwidth,int newheight)
  {
    screensize.setSize(newwidth,newheight);
    initbuffers();
    updateScrollBarY();
    parentwin.updatemusicgfx=true;
    repaint();
  }

/*------------------------------------------------------------------------
Method:  void newViewScale()
Purpose: Update graphics when scale has changed
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void newViewScale()
  {
    initbuffers();
    if (VIEWYSTART>SCREEN_MINHEIGHT-screensize.height/VIEWSCALE)
      VIEWYSTART=SCREEN_MINHEIGHT-screensize.height/VIEWSCALE;
    if (VIEWYSTART<0)
      VIEWYSTART=0;
    updateScrollBarY();
    MusicGfx.newScale(VIEWSCALE);
    parentwin.updatemusicgfx=true;
    repaint();
  }

/*------------------------------------------------------------------------
Method:  void updateScrollBarY()
Purpose: Update vertical scroll bar to reflect buffer size
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void updateScrollBarY()
  {
//System.out.println("SMH*VS="+(SCREEN_MINHEIGHT*VIEWSCALE)+" SSH="+screensize.height);
    parentwin.setScrollBarYparams(Math.round(SCREEN_MINHEIGHT*VIEWSCALE),screensize.height,Math.round(VIEWYSTART));
  }

/*------------------------------------------------------------------------
Method:  void newY(int newystart)
Purpose: Change y position of viewport
Parameters:
  Input:  int newystart - new value for VIEWYSTART
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void newY(int newystart)
  {
    redisplaying++;
    VIEWYSTART=newystart;
    paintbuffer(curbufferg2d);
    parentwin.redrawscr=true;
    repaint();
    redisplaying--;
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
    return screensize;
  }

/*------------------------------------------------------------------------
Method:  void movedisplay(int newmeasure)
Purpose: Move display to a new measure location
Parameters:
  Input:  int newmeasure - measure number
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void movedisplay(int newmeasure)
  {
    redisplaying++;
    curmeasure=newmeasure<nummeasures ? newmeasure : nummeasures-1;
    paintbuffer(curbufferg2d);
    parentwin.redrawscr=true;
    repaint();
    redisplaying--;
  }

/*------------------------------------------------------------------------
Method:  void MIDIMeasureStarted(int mnum,JScrollBar musicScrollBarX)
Purpose: Show measure currently being played back
Parameters:
  Input:  int mnum                   - measure number
          JScrollBar musicScrollBarX - scroll bar controlling score position
  Output: -
  Return: -
------------------------------------------------------------------------*/

  boolean measureDisplayed(int mnum)
  {
    if (nummeasures<2)
      return true;
    if (mnum<curmeasure || mnum>=curmeasure+nummeasuresdisplayed-1)
      return false;
    return true;
  }

  public void MIDIMeasureStarted(int mnum,JScrollBar musicScrollBarX)
  {
    MIDIMeasurePlaying=mnum;
    if (MIDIMeasurePlaying<0)
      {
        /* stopped */
        movedisplay(curmeasure);
        return;
      }

    /* redisplay to move playback line, scrolling if necessary */
    if (!measureDisplayed(mnum))
      {
        musicScrollBarX.setValue(mnum-1);
        movedisplay(mnum-1);
      }
    else
      movedisplay(curmeasure);
  }

/*------------------------------------------------------------------------
Method:  void selectEvent(int vnum,int eventnum)
Purpose: Select or de-select music events in GUI
Parameters:
  Input:  int vnum,eventnum - voice number/event number to select (vnum==-1
                              for none)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void selectEvent(int vnum,int eventnum)
  {
    selectedVoicenum=vnum;
    selectedEventnum=eventnum;

    int    measureNum=-1;
    String edCommentary=null;

    if (selectedVoicenum>=0)
      {
        RenderedEvent re=renderedSections[leftRendererNum].eventinfo[vnum].getEvent(eventnum);
        measureNum=re.getmeasurenum();
        edCommentary=re.getEvent().getEdCommentary();
        if (edCommentary==null) /* for now, only select commentary */
          selectedVoicenum=-1;
      }

    parentwin.updatemusicgfx=true;
    repaint();
    parentwin.updateCommentaryArea(selectedVoicenum,measureNum,edCommentary);
  }

/*------------------------------------------------------------------------
Method:  void showVariants(int snum,int vnum,int eventNum,int fx,int fy)
Purpose: Show variant readings popup in GUI (if there are readings at this point)
Parameters:
  Input:  int snum,vnum,eventNum - section number/voice number/event number
                                   to show (vnum==-1 for none)
          int fx,fy -              screen location to pop up frame
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void showVariants(int snum,int vnum,int eventNum,int fx,int fy)
  {
    if (musicData.getVariantVersions().size()==0)
      return;

    RenderedEvent re=renderedSections[snum].eventinfo[vnum].getEvent(eventNum);
    RenderedEventGroup renderedVar=re.getVarReadingInfo();
    if (renderedVar==null)
      return; /* no variants here */

    VariantDisplayFrame varFrame=new VariantDisplayFrame(
      renderedVar,
      musicData.getSection(snum).getVoice(vnum),
      renderedSections[snum],vnum,
      fx,fy,this,MusicGfx,STAFFSCALE,VIEWSCALE);

    varFrame.setVisible(true);
  }

/*------------------------------------------------------------------------
Method:  void paintbuffer(Graphics2D g)
Purpose: Repaint area into offscreen buffer
Parameters:
  Input:  Graphics2D g - offscreen graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public int repaintingbuffer=0;

  void paintbuffer(Graphics2D g)
  {
    /* do not allow re-entrant buffer painting! */
    if (repaintingbuffer>0)
      {
        final Graphics2D repaintg=g;
        Runnable dopaint=new Runnable()
        {
          public void run()
          {
            paintbuffer(repaintg);
          }
        };
        SwingUtilities.invokeLater(dopaint);
        return;
      }

    repaintingbuffer++;
    realpaintbuffer(g);
    repaintingbuffer--;
  }

  /* real painting code, which can be overridden */
  protected void realpaintbuffer(Graphics2D g)
  {
    ORIGXLEFT=displayVarTexts ? TEXT_NAMES_SIZE+LEFT_BLANK_BORDER*2*VIEWSCALE : ORIGXLEFT;
    XLEFT=ORIGXLEFT;
    num_redisplays++;

    /* get drawing options */
    barline_type=options.get_barline_type();
    usemodernclefs=options.get_usemodernclefs();
    useModernAccSystem=options.getUseModernAccidentalSystem();
    displayligbrackets=options.get_displayligbrackets();
    displayEditTags=options.get_displayedittags();
    markdissonances=options.get_markdissonances();
    markdirectedprogressions=options.get_markdirectedprogressions();
    displayVarTexts=options.markVariant(VariantReading.VAR_ORIGTEXT) &&
                    !options.get_displayedittags();

    /* clear area */
    g.setColor(Color.white);
    g.fillRect(0,0,viewsize.width+1,viewsize.height+1);
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

    /* write piece information */
    g.setFont(defaultTextFontSMALL);
    g.setColor(Color.red);
    g.drawString(musicData.getFullTitle(),XLEFT,15*VIEWSCALE);
    g.drawString(musicData.getComposer(),XLEFT,25*VIEWSCALE);

    /* calculate section position */
    leftRendererNum=ScoreRenderer.calcRendererNum(renderedSections,curmeasure);
    MeasureInfo leftMeasure=getLeftMeasure();

    /* prepare staves */
    FontMetrics fm=g.getFontMetrics();
    g.setColor(Color.black);
    double XLEFT_AFTER_BREAK=calcXLEFT(curmeasure);

    int topLeftStaff=-1,bottomLeftStaff=-1,
        topNewStaff=-1,bottomNewStaff=-1;
    RenderedSectionParams lastSectionParams[]=null;
    if (leftRendererNum>0 && curmeasure==renderedSections[leftRendererNum].getFirstMeasureNum())
      lastSectionParams=renderedSections[leftRendererNum-1].getEndingParams();
    for (int i=0; i<numvoices; i++)
      {
        /* voice information */
        if (curVersionMusicData.getVoiceData()[i].isEditorial())
          g.setColor(Color.red);
        g.drawString(voicelabels[i],
                     XLEFT-10-fm.stringWidth(voicelabels[i]),
                     YTOP+(i*(STAFFSCALE*STAFFSPACING)+2*STAFFSCALE+STAFFPOSSCALE-1)*VIEWSCALE);
        g.setColor(Color.black);

        /* draw staves */
        int     si=leftRendererNum;
        double  sectionStartDisplayX=XLEFT_AFTER_BREAK-leftMeasure.leftx*VIEWSCALE,
                sectionStaffStartX=Math.max(sectionStartDisplayX,XLEFT);

        if (lastSectionParams!=null &&
            (lastSectionParams[i].getClefSet()!=null || lastSectionParams[i].getMens()!=null))
          {
            drawStaff(g,(float)XLEFT,(float)Math.min((int)sectionStartDisplayX,viewsize.width-1),
                        YTOP+i*(STAFFSCALE*STAFFSPACING)*VIEWSCALE,5);
            if (topLeftStaff==-1)
              topLeftStaff=i;
            if (i>bottomLeftStaff)
              bottomLeftStaff=i;
          }
/*        else
          if (sectionStaffStartX>XLEFT)
            sectionStaffStartX=XLEFT;*/
        boolean doneStaves=false;
        while (!doneStaves)
          {
            double sectionEndX=sectionStartDisplayX+(renderedSections[si].getXsize()+ScoreRenderer.SECTION_END_SPACING)*VIEWSCALE;
            if (curVersionMusicData.getSection(si).getVoice(i)!=null)
              {
                drawStaff(g,(float)sectionStaffStartX,(float)Math.min((int)sectionEndX,viewsize.width-1),
                            YTOP+i*(STAFFSCALE*STAFFSPACING)*VIEWSCALE,5);
                if (sectionStaffStartX<=XLEFT)
                  {
                    if (topLeftStaff==-1)
                      topLeftStaff=i;
                    if (i>bottomLeftStaff)
                      bottomLeftStaff=i;
                  }
                if (si==leftRendererNum)
                  {
                    if (topNewStaff==-1)
                      topNewStaff=i;
                    if (i>bottomNewStaff)
                      bottomNewStaff=i;
                  }
              }
            si++;
            if (si>=curVersionMusicData.getNumSections() || sectionEndX>=viewsize.width)
              doneStaves=true;
            else
              sectionStartDisplayX=sectionStaffStartX=sectionEndX;
          }
      }

    /* visibly break initial information from music */
    if (curmeasure>0)
      {
        g.setColor(Color.white);
        for (int i=0; i<numvoices; i++)
          g.fillRect((int)Math.round(XLEFT_AFTER_BREAK-LEFTBREAK_XSIZE*VIEWSCALE),Math.round(YTOP+(i*(STAFFSCALE*STAFFSPACING)-1)*VIEWSCALE),
                     Math.round(LEFTBREAK_XSIZE*VIEWSCALE),Math.round((STAFFSCALE*STAFFSPACING+2)*VIEWSCALE));
        g.setColor(Color.black);
      }

    /* left barline */
//    if (curVersionMusicData.getSection(leftRendererNum).getSectionType()==MusicSection.MENSURAL_MUSIC)
    if (lastSectionParams==null) /* don't draw left barline at section change */
      g.drawLine(Math.round(XLEFT),Math.round(YTOP+(topLeftStaff*STAFFSCALE*STAFFSPACING)*VIEWSCALE),
                 Math.round(XLEFT),Math.round(YTOP+(bottomLeftStaff*STAFFSCALE*STAFFSPACING+STAFFSCALE*4)*VIEWSCALE));
    else
      {
        /* section change barlines */
        g.drawLine((int)Math.round(XLEFT_AFTER_BREAK-LEFTBREAK_XSIZE*VIEWSCALE),Math.round(YTOP+(topLeftStaff*STAFFSCALE*STAFFSPACING)*VIEWSCALE),
                   (int)Math.round(XLEFT_AFTER_BREAK-LEFTBREAK_XSIZE*VIEWSCALE),Math.round(YTOP+(bottomLeftStaff*STAFFSCALE*STAFFSPACING+STAFFSCALE*4)*VIEWSCALE));
        g.drawLine((int)Math.round(XLEFT_AFTER_BREAK),Math.round(YTOP+(topNewStaff*STAFFSCALE*STAFFSPACING)*VIEWSCALE),
                   (int)Math.round(XLEFT_AFTER_BREAK),Math.round(YTOP+(bottomNewStaff*STAFFSCALE*STAFFSPACING+STAFFSCALE*4)*VIEWSCALE));
      }

    /* add clef/mensuration at left */
    if (curmeasure!=0)
      drawleftinfo(g);

    /* now draw music */
    if (numvoices>0)
      drawEvents(g);

    /* version names for variant text display */
    if (displayVarTexts)
      writeTextVersionNames(g);

    g.setFont(defaultTextFontSMALL);
  }

  void writeTextVersionNames(Graphics2D g)
  {
    float textx=LEFT_BLANK_BORDER*VIEWSCALE;

    for (int vi=0; vi<numvoices; vi++)
      writeTextVersionNames(g,textx,YTOP+(vi*(STAFFSCALE*STAFFSPACING)+7*STAFFSCALE)*VIEWSCALE);
  }

  void writeTextVersionNames(Graphics2D g,float curx,float cury)
  {
    /* clear area for version names */
    g.setColor(Color.white);
    g.fillRect(Math.round(ORIGXLEFT-1*VIEWSCALE),(int)(cury-(STAFFPOSSCALE*(OptionSet.SPACES_PER_TEXTLINE+1))*VIEWSCALE),
//               Math.round(TEXT_NAMES_SIZE),
               Math.round(3*VIEWSCALE),Math.round((musicData.getVariantVersions().size()*STAFFPOSSCALE*OptionSet.SPACES_PER_TEXTLINE+STAFFPOSSCALE*2)*VIEWSCALE));

    /* write names */
    g.setFont(defaultTextFontSMALL);
    int versionNum=0;
    for (VariantVersionData vvd : musicData.getVariantVersions())
      {
        int txtColor=(versionNum++)%OptionSet.TEXTVERSION_COLORS+1;
        g.setColor(Coloration.AWTColors[txtColor]);
        g.drawString(vvd.getID(),curx,cury);
        cury+=(STAFFPOSSCALE*OptionSet.SPACES_PER_TEXTLINE)*VIEWSCALE;
      }
    g.setColor(Color.black);
  }

/*------------------------------------------------------------------------
Method:  void drawleftinfo(Graphics2D g)
Purpose: Draw clefs/mensuration at left side of viewscreen, and shift event
         drawing space over
Parameters:
  Input:  Graphics2D g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawleftinfo(Graphics2D g)
  {
    MeasureInfo   leftMeasure;
    int           clefi;
    RenderedEvent me;
    float         xloc=0,yloc=0,maxx=0;

    leftMeasure=getLeftMeasure();
    int numLeftSectionVoices=curVersionMusicData.getSection(leftRendererNum).getNumVoices();

    if (leftRendererNum>0 && curmeasure==renderedSections[leftRendererNum].getFirstMeasureNum())
      {
        RenderedSectionParams lastSectionParams[]=renderedSections[leftRendererNum-1].getEndingParams();
        for (int i=0; i<numLeftSectionVoices; i++)
          {
            xloc=XLEFT;
            yloc=YTOP+i*(STAFFSCALE*STAFFSPACING)*VIEWSCALE;

            /* draw clefs */
            RenderedClefSet curCS=lastSectionParams[i].getClefSet();
            if (curCS!=null)
              {
                xloc+=curCS.draw(useModernAccSystem,g,MusicGfx,xloc,yloc,VIEWSCALE);

                /* modern key signature */
                ModernKeySignature mk=curCS.getLastClefEvent().getModernKeySig();
                if (mk.numEls()>0 && (displayEditTags || useModernAccSystem))
                  xloc+=drawModKeySig(g,mk,curCS.getPrincipalClefEvent(),xloc,yloc);
              }

            /* draw mensuration */
            me=lastSectionParams[i].getMens();
            if (me!=null)
              {
                me.drawMens(g,MusicGfx,this,xloc,yloc,VIEWSCALE);
                xloc+=me.getimgxsize()*VIEWSCALE;
              }

            if (xloc>maxx)
              maxx=xloc;
          }
      }
    else
      for (int i=0; i<numLeftSectionVoices; i++)
        if (curVersionMusicData.getSection(leftRendererNum).getVoice(i)!=null)
          {
            xloc=XLEFT;
            yloc=YTOP+i*(STAFFSCALE*STAFFSPACING)*VIEWSCALE;

            /* draw clefs */
            RenderedClefSet curCS=renderedSections[leftRendererNum].eventinfo[i].getClefEvents(leftMeasure.reventindex[i]);
            if (curCS!=null)
              xloc+=curCS.draw(useModernAccSystem,g,MusicGfx,xloc,yloc,VIEWSCALE);

            /* modern key signature */
            ModernKeySignature mk=renderedSections[leftRendererNum].eventinfo[i].getModernKeySig(leftMeasure.reventindex[i]);
            if (mk.numEls()>0 && curCS!=null && (displayEditTags || useModernAccSystem))
              xloc+=drawModKeySig(g,mk,curCS.getPrincipalClefEvent(),xloc,yloc);

            /* draw mensuration */
            me=leftMeasure.startMensEvent[i]; //renderedSections[leftRendererNum].eventinfo[i].getMensEvent(leftMeasure.reventindex[i]);
            if (me!=null)
              {
                me=leftMeasure.startMensEvent[i];
                me.drawMens(g,MusicGfx,this,xloc,yloc,VIEWSCALE);
                xloc+=me.getimgxsize()*VIEWSCALE;
              }

            if (xloc>maxx)
              maxx=xloc;
          }

    maxx+=LEFTINFO_XPADDING*VIEWSCALE;

    /* barline to show section beginning, if necessary 
    if (leftRendererNum!=0 && curmeasure==renderedSections[leftRendererNum].getFirstMeasureNum())
      {
        g.setColor(Color.black);
        g.drawLine(Math.round(maxx),Math.round(YTOP),
                   Math.round(maxx),Math.round(YTOP+((numvoices-1)*STAFFSCALE*STAFFSPACING+STAFFSCALE*4)*VIEWSCALE));
        g.drawLine(Math.round(maxx+LEFTBREAK_XSIZE*VIEWSCALE),Math.round(YTOP),
                   Math.round(maxx+LEFTBREAK_XSIZE*VIEWSCALE),Math.round(YTOP+((numLeftSectionVoices-1)*STAFFSCALE*STAFFSPACING+STAFFSCALE*4)*VIEWSCALE));
      }*/

    /* measure number */
    g.setFont(defaultTextFontSMALL);
    g.setColor(Color.black);
    XLEFT=maxx+LEFTBREAK_XSIZE*VIEWSCALE;
    g.drawString(String.valueOf(curmeasure+1),XLEFT,YTOP-(STAFFSCALE*2)*VIEWSCALE);
  }

/*------------------------------------------------------------------------
Method:  void drawEvents(Graphics2D g)
Purpose: Draw music on staves
Parameters:
  Input:  Graphics2D g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void drawEvents(Graphics2D g)
  {
    int              evloc;
    double           displayX,
                     sectionStartDisplayX;
    RenderedEvent    e;
    RenderedLigature ligInfo=null,tieInfo=null;
    boolean          doneSections,doneVoice;

    ScoreRenderer curRenderer;
    MeasureInfo   leftMeasure;
    int           curRendererNum;

    leftRendererNum=ScoreRenderer.calcRendererNum(renderedSections,curmeasure);
    leftMeasure=renderedSections[leftRendererNum].getMeasure(curmeasure);

    /* draw barlines */
    drawAllBarlines(g);

    /* draw each section in turn */
    curRendererNum=leftRendererNum;
    curRenderer=renderedSections[leftRendererNum];
    sectionStartDisplayX=XLEFT-leftMeasure.leftx*VIEWSCALE;
    doneSections=false;
    int startv=-1,endv=-1;
    while (!doneSections)
      {
/*        if (displayEditTags &&
            (curRendererNum>leftRendererNum || curmeasure==curRenderer.getFirstMeasureNum()))
          addSectionTag(g,curRenderer,(int)Math.round(sectionStartDisplayX),
                                      Math.round(YTOP-STAFFSCALE*2*VIEWSCALE));*/
        if (musicData.isIncipitScore() && curmeasure==curRenderer.getFirstMeasureNum())
          {
            g.setColor(Color.blue);
            g.setFont(defaultTextFontSMALL);
            g.drawString("INCIPIT",Math.round((float)sectionStartDisplayX),Math.round(YTOP-STAFFSCALE*3*VIEWSCALE));
          }

        int numv=curRenderer.getNumVoices();
        startv=numv;
        endv=-1;

        MusicSection curSection=curRenderer.getSectionData();
        if (curSection instanceof MusicTextSection)
          drawSectionText(curRenderer,g,sectionStartDisplayX,YTOP);
        else
        /* draw each voice in turn */
        for (int i=0; i<numv; i++)
          if (curSection.getVoice(i)!=null)
          {
            if (i<startv)
              startv=i;
            if (i>endv)
              endv=i;

            evloc=curRendererNum==leftRendererNum ? leftMeasure.reventindex[i] : 0;
            doneVoice=(evloc>=curRenderer.eventinfo[i].size());
            while (!doneVoice)
              {
                e=curRenderer.eventinfo[i].getEvent(evloc);
                displayX=sectionStartDisplayX+e.getxloc()*VIEWSCALE;
                if (displayX<viewsize.width)
                  {
                    /* draw event */
                    if (e.isdisplayed())
                      if (e.getEvent().geteventtype()==Event.EVENT_ELLIPSIS)
                        drawEllipsisBreak(g,i,displayX,e,curRenderer.eventinfo[i].getEvent(evloc+1));
                      else //if (displayX>=XLEFT)
                        if (i==selectedVoicenum && evloc==selectedEventnum)
                          e.drawHighlighted(g,MusicGfx,this,displayX,YTOP+i*(STAFFSCALE*STAFFSPACING)*VIEWSCALE,VIEWSCALE);
                        else
                          e.draw(g,MusicGfx,this,displayX,YTOP+i*(STAFFSCALE*STAFFSPACING)*VIEWSCALE,VIEWSCALE);

                    /* draw ligatures */
                    ligInfo=e.getLigInfo();
                    if (ligInfo.firstEventNum!=-1)
                      {
                        drawLigType(g,e,displayX+3*VIEWSCALE,calcligy(i,e));
                        if (e.isligend())
                          drawLigature(g,sectionStartDisplayX+(curRenderer.eventinfo[i].getEvent(ligInfo.firstEventNum).getxloc()+4)*VIEWSCALE,
                                       displayX,calcligy(i,e),XLEFT,viewsize.width);
                      }

                    /* tie notes */
                    tieInfo=e.getTieInfo();
                    if (tieInfo.firstEventNum!=-1)// && tieInfo.lastEventNum==evloc)
                      {
                        RenderedEvent tre1=curRenderer.eventinfo[i].getEvent(tieInfo.firstEventNum);
                        drawTie(g,tre1.getTieType(),
                                sectionStartDisplayX+tre1.getxloc()*VIEWSCALE,
                                displayX,
                                calcTieY(i,e),XLEFT,viewsize.width);
                      }

                    /* mark variant readings */
                    RenderedEventGroup varReadingInfo=e.getVarReadingInfo();
                    if (varReadingInfo!=null &&
                        evloc>=varReadingInfo.lastEventNum)
//                      if (varReadingInfo.lastEventNum-varReadingInfo.firstEventNum>1)
                        markVariantReading(g,sectionStartDisplayX+(curRenderer.eventinfo[i].getEvent(varReadingInfo.firstEventNum).getxloc()+4)*VIEWSCALE,
                                           displayX,calcVarMarkY(curRenderer.eventinfo[i],i,e),XLEFT,viewsize.width,
                                           e.getEvent().getVariantReading(curVariantVersion),varReadingInfo.varMarker);
//                      else
//                        markVariantReading(g,displayX,calcVarMarkY(curRenderer.eventinfo[i],i,e));

                    /* analysis functions */
                    draw_analysis(g,curRenderer,evloc,i,(float)displayX,YTOP+i*(STAFFSCALE*STAFFSPACING));
                  }
                else
                  {
                    /* past right edge of view area = last section */
                    doneSections=true;
                    doneVoice=true;
                    break;
                  }

                evloc++;
                if (evloc>=curRenderer.eventinfo[i].size())
                  doneVoice=true;
              }
            e=curRenderer.eventinfo[i].getEvent(evloc);
            ligInfo=e==null ? null : e.getLigInfo();

            /* finish any remaining ligature */
            if (ligInfo!=null && ligInfo.firstEventNum!=-1 &&
                evloc<curRenderer.eventinfo[i].size()-1)
              drawLigature(g,sectionStartDisplayX+(float)(curRenderer.eventinfo[i].getEvent(ligInfo.firstEventNum).getxloc()+4)*VIEWSCALE,
                           viewsize.width,calcligy(i,e),XLEFT,viewsize.width);

            /* finish any remaining variant reading */
            RenderedEventGroup varReadingInfo=e==null ? null : e.getVarReadingInfo();
            if (varReadingInfo!=null &&
                evloc<curRenderer.eventinfo[i].size()-1)
              if (varReadingInfo.lastEventNum-varReadingInfo.firstEventNum>1)
                markVariantReading(g,sectionStartDisplayX+(curRenderer.eventinfo[i].getEvent(varReadingInfo.firstEventNum).getxloc()+4)*VIEWSCALE,
                                   viewsize.width,calcVarMarkY(curRenderer.eventinfo[i],i,e),XLEFT,viewsize.width,
                                   e.getEvent().getVariantReading(curVariantVersion),varReadingInfo.varMarker);
          }

        else if (curRendererNum>leftRendererNum ||
                 curmeasure==curRenderer.getFirstMeasureNum())
          {
            /* tacet text */
            String tacetText=curVersionMusicData.getSection(curRendererNum).getTacetText(i);
            if (tacetText!=null)
              {
                g.setFont(MusicGfx.displayTextFont);
                g.setColor(Color.black);
                g.drawString(tacetText,(float)sectionStartDisplayX+LEFTBREAK_XSIZE*VIEWSCALE,YTOP+(i*(STAFFSCALE*STAFFSPACING)+5*STAFFPOSSCALE)*VIEWSCALE);
              }
          }

        curRendererNum++;
        if (curRendererNum<numSections)
          {
            if (sectionStartDisplayX>=ORIGXLEFT)
              drawSectionBarline(g,(int)Math.round(sectionStartDisplayX),startv,endv,
                                 false);//musicData.isIncipitScore() && numSections>1 && curRendererNum==numSections-1);
            sectionStartDisplayX+=(curRenderer.getXsize()+ScoreRenderer.SECTION_END_SPACING)*VIEWSCALE;
            drawSectionBarline(g,(int)Math.round(sectionStartDisplayX),startv,endv,
                               musicData.isIncipitScore() && numSections>1 && curRendererNum==numSections-1);

            curRenderer=renderedSections[curRendererNum];
          }
        else
//        if (curRendererNum>=numSections)
          doneSections=true;
      }

    /* barlines of last rendered section */
    if (!(curRenderer.getSectionData() instanceof MusicTextSection))
      drawSectionBarline(g,(int)Math.round(sectionStartDisplayX),startv,endv,
                         musicData.isIncipitScore() && numSections>1 && curRendererNum==numSections-1);
    sectionStartDisplayX+=(curRenderer.getXsize()+ScoreRenderer.SECTION_END_SPACING)*VIEWSCALE;
    if (!(curRenderer.getSectionData() instanceof MusicTextSection))
      drawSectionBarline(g,(int)Math.round(sectionStartDisplayX),startv,endv,
                         musicData.isIncipitScore() && numSections>1 && curRendererNum==numSections-1);
//    g.setFont(defaultTextFontSMALL);
  }

/*------------------------------------------------------------------------
Method:  float calcligy(int vnum,RenderedEvent e)
Purpose: Calculate y position of a ligature at a given event
Parameters:
  Input:  int vnum        - voice number
          RenderedEvent e - event
  Output: -
  Return: y position of ligature
------------------------------------------------------------------------*/

  float calcligy(int vnum,RenderedEvent e)
  {
    RenderedLigature ligInfo=e.getLigInfo();
    RenderedEvent    lige=ligInfo.reventList.getEvent(ligInfo.yMaxEventNum);
    Clef             ligevclef=lige.getClef();
    Event            lignoteev=ligInfo.yMaxEvent;

    float ligyval=STAFFSCALE*4-12-
                  STAFFPOSSCALE*lignoteev.getPitch().calcypos(ligevclef);
    if (ligyval>-7)
      ligyval=-7;
    ligyval=YTOP+(ligyval+vnum*STAFFSCALE*STAFFSPACING)*VIEWSCALE;

    return ligyval;
  }

  static public float calcTieY(RenderedEvent tieRE,float yTop,
                               int STAFFSCALE,int STAFFPOSSCALE,float VIEWSCALE)
  {
    return yTop+VIEWSCALE*
           (STAFFSCALE*4-12-
            STAFFPOSSCALE*tieRE.getEvent().getPitch().calcypos(tieRE.getClef()));
  }

  float calcTieY(int vnum,RenderedEvent e)
  {
    return calcTieY(e.getTieInfo().reventList.getEvent(e.getTieInfo().yMaxEventNum),
                    YTOP+vnum*STAFFSCALE*STAFFSPACING*VIEWSCALE,
                    STAFFSCALE,STAFFPOSSCALE,VIEWSCALE);
  }

/*------------------------------------------------------------------------
Method:  float calcVarMarkY(RenderList rl,int vnum,RenderedEvent e)
Purpose: Calculate y position for marking variant readings at a given event
Parameters:
  Input:  RenderList rl   - rendered event list
          int vnum        - voice number
          RenderedEvent e - event
  Output: -
  Return: y position of variant marker
------------------------------------------------------------------------*/

  float calcVarMarkY(RenderList rl,int vnum,RenderedEvent e)
  {
    return YTOP+(-8+vnum*STAFFSCALE*STAFFSPACING)*VIEWSCALE;
  }

/*------------------------------------------------------------------------
Method:  void drawStaff(Graphics2D g,float x1,float x2,float yloc,int numlines)
Purpose: Draw staff at specified location
Parameters:
  Input:  Graphics2D g - graphical context
          float x1,x2  - x location for left and right ends of staff
          float yloc   - y location for top of staff
          int numlines - number of lines for staff
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawStaff(Graphics2D g,float x1,float x2,float yloc,int numlines)
  {
    for (int i=0; i<numlines; i++)
      g.drawLine(Math.round(x1),Math.round(yloc+i*STAFFSCALE*VIEWSCALE),
                 Math.round(x2),Math.round(yloc+i*STAFFSCALE*VIEWSCALE));
  }

  public static void drawStaff(Graphics2D g,float x1,float x2,float yloc,int numlines,float STAFFSCALE,float VIEWSCALE)
  {
    for (int i=0; i<numlines; i++)
      g.drawLine(Math.round(x1),Math.round(yloc+i*STAFFSCALE*VIEWSCALE),
                 Math.round(x2),Math.round(yloc+i*STAFFSCALE*VIEWSCALE));
  }

/*------------------------------------------------------------------------
Method:  void drawLigType(Graphics2D g,RenderedEvent re,double x,double y)
Purpose: Indicate ligature connection type (recta/obliqua) at specified
         location
Parameters:
  Input:  Graphics2D g     - graphical context
          RenderedEvent re - first ligated note
          double x,y       - location in context for drawing
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawLigType(Graphics2D g,RenderedEvent re,double x,double y)
  {
    if (!displayligbrackets)
      return;

    Event e=re.getEvent();
    if (e.geteventtype()==Event.EVENT_NOTE)
      {
        NoteEvent ne=(NoteEvent)e;
        g.setFont(defaultTextFontSMALL);
        g.setColor(Color.gray);
        switch (ne.getligtype())
          {
            case NoteEvent.LIG_RECTA:
              g.drawString("R",(float)x,(float)y);
              break;
            case NoteEvent.LIG_OBLIQUA:
              g.drawString("O",(float)x,(float)y);
              break;
          }
        g.setColor(Color.black);
      }
  }

/*------------------------------------------------------------------------
Method:  void drawLigature(Graphics2D g,double x1,double x2,double y,double leftx,double rightx)
Purpose: Draw ligature bracket for one voice
Parameters:
  Input:  Graphics2D g        - graphical context
          double x1,x2        - left and right coordinates of bracket
          double y            - y level of bracket
          double leftx,rightx - horizontal bounds of drawing space
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawLigature(Graphics2D g,double x1,double x2,double y,double leftx,double rightx)
  {
    if (displayligbrackets)
      drawLigOnCanvas(g,x1,x2,y,leftx,rightx,VIEWSCALE);
  }

  public static void drawLigOnCanvas(Graphics2D g,double x1,double x2,double y,
                                     double leftx,double rightx,double VS)
  {
    x1+=VS;
    if (x1>=leftx)
      g.drawLine((int)Math.round(x1),(int)Math.round(y),(int)Math.round(x1),(int)Math.round(y+3*VS));
    else
      x1=leftx;

    x2+=VS*(4+MusicFont.getDefaultGlyphWidth(MusicFont.PIC_NOTESTART+NoteEvent.NOTEHEADSTYLE_SEMIBREVE));
    if (x2<rightx)
      g.drawLine((int)Math.round(x2),(int)Math.round(y),(int)Math.round(x2),(int)Math.round(y+3*VS));
    else
      x2=rightx-1;

    g.drawLine((int)Math.round(x1),(int)Math.round(y),(int)Math.round(x2),(int)Math.round(y));
  }

  public static void drawLigOnCanvas(Graphics2D g,double x1,double x2,double y,double leftx,double rightx)
  {
    drawLigOnCanvas(g,x1,x2,y,leftx,rightx,1);
  }

  void drawTie(Graphics2D g,int tieType,double x1,double x2,double y,double leftx,double rightx)
  {
    drawTieOnCanvas(g,tieType,x1,x2,y,leftx,rightx,VIEWSCALE);
  }

  public static void drawTieOnCanvas(Graphics2D g,int tieType,
                                     double x1,double x2,double y,
                                     double leftx,double rightx,double VS)
  {
    double xAdjust=VS*(4+MusicFont.getDefaultGlyphWidth(MusicFont.PIC_NOTESTART+NoteEvent.NOTEHEADSTYLE_SEMIBREVE));
    x1=Math.max(x1+xAdjust,leftx-xAdjust/2);
    x2+=VS*4;

    int arc1=0,arc2=180;
    if (tieType==NoteEvent.TIE_UNDER)
      {
        arc1=180;
        y+=MusicFont.CONNECTION_SCREEN_L_UPSTEMY*2*VS;
      }

    g.drawArc((int)Math.round(x1),(int)Math.round(y),
              (int)Math.round(x2-x1),(int)Math.round(15*VS),
              arc1,arc2);
  }

  public static void drawTieOnCanvas(Graphics2D g,int tieType,
                                     double x1,double x2,double y,
                                     double leftx,double rightx)
  {
    drawTieOnCanvas(g,tieType,x1,x2,y,leftx,rightx,1);
  }

/*------------------------------------------------------------------------
Method:  void drawSectionText(MusicTextSection ts,Graphics2D g,double x,double y)
Purpose: Draw text section on canvas
Parameters:
  Input:  ScoreRenderer rs - rendered section
          Graphics2D g     - graphical context
          double x,y       - location at which to draw
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawSectionText(ScoreRenderer rs,Graphics2D g,double x,double y)
  {
    MusicTextSection ts=(MusicTextSection)(rs.getSectionData());
    x+=ScoreRenderer.SECTION_END_SPACING*VIEWSCALE;

    g.setFont(MusicGfx.displayTextFont);
    g.setColor(Color.black);
    g.drawString(ts.getSectionText(),(float)x,(float)y);
  }

/*------------------------------------------------------------------------
Method:  void markVariantReading(Graphics2D g,double x1,double x2,double y,double leftx,double rightx,
                                 VariantReading vr,VariantMarkerEvent vme)
Purpose: Draw variant reading mark
Parameters:
  Input:  Graphics2D g           - graphical context
          double x1,x2           - left and right coordinates of mark
          double y               - y level of mark
          double leftx,rightx    - horizontal bounds of drawing space
          VariantReading vr      - reading being marked
          VariantMarkerEvent vme - event marking start of variant
  Output: -
  Return: -
------------------------------------------------------------------------*/

  Color chooseVarColor(VariantReading vr,VariantMarkerEvent vme)
  {
    if (vr!=null && vr.isError())
      return Color.red;
    if (vme.includesVarType(VariantReading.VAR_RHYTHM) ||
        vme.includesVarType(VariantReading.VAR_PITCH))
      return Color.magenta;

    return Color.green;
  }

  void markVariantReading(Graphics2D g,double x1,double x2,double y,double leftx,double rightx,VariantReading vr,VariantMarkerEvent vme)
  {
    long varTypeFlags=vme.getVarTypeFlags();
    if (varTypeFlags==VariantReading.VAR_ORIGTEXT ||
        !options.markVariant(varTypeFlags & ~VariantReading.VAR_ORIGTEXT))
      return;

    markVariantReadingOnCanvas(g,x1,x2,y,leftx,rightx,VIEWSCALE,chooseVarColor(vr,vme));

    if (vr!=null && vr.isError())
      {
        g.setFont(defaultTextFontSMALL);
        g.drawString("X",(float)x1,(float)y);
      }
  }

  void markVariantReading(Graphics2D g,double x,double y,VariantReading vr,VariantMarkerEvent vme)
  {
    long varTypeFlags=vme.getVarTypeFlags();
    if (varTypeFlags==VariantReading.VAR_ORIGTEXT ||
        !options.markVariant(varTypeFlags & ~VariantReading.VAR_ORIGTEXT))
      return;

    markVariantReadingOnCanvas(g,x,y,VIEWSCALE,chooseVarColor(vr,vme));

    if (vr!=null && vr.isError())
      {
        g.setFont(defaultTextFontSMALL);
        g.drawString("X",(float)x,(float)y);
      }
  }

  void markVariantReadingOnCanvas(
                       Graphics2D g,double x1,double x2,double y,
                       double leftx,double rightx,double VS,Color c)
  {
    double MARKSCALE=5*VS;
    g.setColor(c);

    x1+=VS;
    if (x1>=leftx)
      ;//g.drawLine((int)Math.round(x1),(int)Math.round(y-MARKSCALE),(int)Math.round(x1),(int)Math.round(y+MARKSCALE));
    else
      x1=leftx;

    if (displayEditTags)
      x2+=VS*(4+MusicFont.getDefaultGlyphWidth(MusicFont.PIC_NOTESTART+NoteEvent.NOTEHEADSTYLE_SEMIBREVE));
    if (x2<rightx)
      ;//g.drawLine((int)Math.round(x2),(int)Math.round(y-MARKSCALE),(int)Math.round(x2),(int)Math.round(y+MARKSCALE));
    else
      x2=rightx-1;

    g.drawLine((int)Math.round(x1),(int)Math.round(y),(int)Math.round(x2),(int)Math.round(y));
  }

  void markVariantReadingOnCanvas(Graphics2D g,double x,double y,double VS,Color c)
  {
    double MARKSCALE=3*VS;

    g.setColor(c);
    g.drawLine((int)Math.round(x-2*MARKSCALE),(int)Math.round(y),(int)Math.round(x-MARKSCALE),(int)Math.round(y+2*MARKSCALE));
    g.drawLine((int)Math.round(x-MARKSCALE),(int)Math.round(y+2*MARKSCALE),(int)Math.round(x),(int)Math.round(y));
    g.drawLine((int)Math.round(x-2*MARKSCALE),(int)Math.round(y),(int)Math.round(x),(int)Math.round(y));
  }

/*------------------------------------------------------------------------
Method:  float drawModKeySig(Graphics2D g,ModernKeySignature mk,RenderedEvent cre,float xloc,float yloc)
Purpose: Draw modern key signature on staff with specified clef
Parameters:
  Input:  Graphics2D g          - graphical context
          ModernKeySignature mk - signature to draw
          RenderedEvent cre     - event with current clef on staff
          float xloc,yloc       - location to draw
  Output: -
  Return: amount of x space taken by sig
------------------------------------------------------------------------*/

  float drawModKeySig(Graphics2D g,ModernKeySignature mk,RenderedEvent cre,float xloc,float yloc)
  {
    float curx=xloc+5*VIEWSCALE;
    Clef  c=cre.getClef();
    int   staffApos=c.getApos(),
          accColor=displayEditTags ? Coloration.BLUE : Coloration.BLACK;

    if (staffApos<0)
      staffApos+=7;
    else if (staffApos>5)
      staffApos-=7;

    /* draw individual accidentals in signature */
    for (Iterator i=mk.iterator(); i.hasNext();)
      {
        ModernKeySignatureElement kse=(ModernKeySignatureElement)i.next();

        for (int ai=0; ai<kse.accidental.numAcc; ai++)
          {
            MusicGfx.drawGlyph(
              g,MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlat+kse.accidental.accType,
              curx,yloc+(STAFFSCALE*4-STAFFPOSSCALE*(staffApos+kse.calcAOffset()))*VIEWSCALE,
              Coloration.AWTColors[accColor]);
            curx+=(MusicGfx.getGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlat+kse.accidental.accType)+
                   MusicFont.CONNECTION_SCREEN_MODACC_DBLFLAT)*VIEWSCALE;
          }
      }

    return curx-xloc;
  }

/*------------------------------------------------------------------------
Method:  float getModKeySigSize(ModernKeySignature mk,RenderedEvent cre)
Purpose: Get x-size of modern key signature on staff with specified clef
Parameters:
  Input:  ModernKeySignature mk - signature to draw
          RenderedEvent cre     - event with current clef on staff
  Output: -
  Return: amount of x space taken by sig
------------------------------------------------------------------------*/

  float getModKeySigSize(ModernKeySignature mk,RenderedEvent cre)
  {
    float curx=5*VIEWSCALE;
    Clef  c=cre.getClef();

    /* loop through individual accidentals in signature */
    for (Iterator i=mk.iterator(); i.hasNext();)
      {
        ModernKeySignatureElement kse=(ModernKeySignatureElement)i.next();
        for (int ai=0; ai<kse.accidental.numAcc; ai++)
          curx+=(MusicGfx.getGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlat+kse.accidental.accType)+
                 MusicFont.CONNECTION_SCREEN_MODACC_DBLFLAT)*VIEWSCALE;
      }

    return curx;
  }

/*------------------------------------------------------------------------
Method:  void drawEllipsisBreak(Graphics2D g,int vnum,double xloc,RenderedEvent e1,RenderedEvent e2)
Purpose: Remove part of staff in one voice to show break between incipit and
         explicit (in incipit-score)
Parameters:
  Input:  Graphics2D g        - graphical context
          int vnum            - voice number
          double xloc         - left x-coordinate for drawing
          RenderedEvent e1,e2 - ellipsis event and next event (left and right
                                boundaries of break)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawEllipsisBreak(Graphics2D g,int vnum,double xloc,RenderedEvent e1,RenderedEvent e2)
  {
    double xsize=(e2.getxloc()-e1.getxloc())*VIEWSCALE,
           yloc=YTOP+(vnum*(STAFFSCALE*STAFFSPACING)-STAFFSCALE)*VIEWSCALE,
           ysize=6*STAFFSCALE*VIEWSCALE;

    /* clip */
    if (xloc<XLEFT)
      {
        xsize-=XLEFT-xloc;
        xloc=XLEFT;
      }

    g.setColor(Color.white);
    g.fillRect((int)Math.round(xloc),(int)Math.round(yloc),(int)Math.round(xsize),(int)Math.round(ysize));
    g.setColor(Color.black);
  }

/*------------------------------------------------------------------------
Method:  void drawAllBarlines(Graphics2D g)
Purpose: Draw barlines across screen
Parameters:
  Input:  Graphics2D g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void drawAllBarlines(Graphics2D g)
  {
    int     i,si;
    float   xloc=XLEFT+6*VIEWSCALE;

    g.setColor(Color.black);
    nummeasuresdisplayed=0;
    si=leftRendererNum;
    for (i=curmeasure;
         i<nummeasures-1 && xloc<viewsize.width;
         i++)
      {
        if (i>renderedSections[si].getLastMeasureNum())
          {
            /* advance one section */
            si++;
            xloc+=ScoreRenderer.SECTION_END_SPACING*VIEWSCALE;
          }

        if (i==MIDIMeasurePlaying) /* MIDI playback line */
          drawPlaybackLine(g,Math.round(xloc));

        xloc+=renderedSections[si].getMeasure(i).xlength*VIEWSCALE;
        if (i!=renderedSections[si].getLastMeasureNum()) /* no barline at section change */
          drawBarlines(g,Math.round(xloc),si);
        nummeasuresdisplayed++;

        /* measure number */
        if (nummeasuresdisplayed%5==0)
          g.drawString(String.valueOf(i+2),xloc,YTOP-STAFFSCALE*2*VIEWSCALE);
      }
    if (i>renderedSections[si].getLastMeasureNum())
      si++;

    if (i==MIDIMeasurePlaying) /* MIDI playback line */
      drawPlaybackLine(g,Math.round(xloc));

    /* ending barline */
    if (i==nummeasures-1)
      {
        xloc+=renderedSections[si].getMeasure(i).xlength*VIEWSCALE;
//        drawEndBarline(g,Math.round(xloc));
      }

    if (xloc<viewsize.width)
      nummeasuresdisplayed++;
    parentwin.setScrollBarXextent(nummeasuresdisplayed);
  }

  void drawPlaybackLine(Graphics2D g,int xloc)
  {
    g.setColor(Color.blue);
    g.drawLine(xloc,0,xloc,viewsize.height);
    g.setColor(Color.black);
  }

/*------------------------------------------------------------------------
Method:  void drawBarlines(Graphics2D g,int xloc,int snum)
Purpose: Draw barlines at specified x location
Parameters:
  Input:  Graphics2D g - graphical context
          int xloc     - x location for barlines
          int snum     - section number
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawBarlines(Graphics2D g,int xloc,int snum)
  {
    switch (barline_type)
      {
        case OptionSet.OPT_BARLINE_NONE:
          break;
        case OptionSet.OPT_BARLINE_MENSS:
          int v1=-1;
          for (int i=0; i<numvoices; i++)
            if (curVersionMusicData.getSection(snum).getVoice(i)!=null)
              {
                if (v1!=-1)
                  g.drawLine(xloc,Math.round(YTOP+(v1*(STAFFSCALE*STAFFSPACING)+STAFFSCALE*4)*VIEWSCALE),
                             xloc,Math.round(YTOP+(i *(STAFFSCALE*STAFFSPACING))*VIEWSCALE));
                v1=i;
              }
          break;
        case OptionSet.OPT_BARLINE_TICK:
          for (int i=0; i<numvoices; i++)
            if (curVersionMusicData.getSection(snum).getVoice(i)!=null)
              {
                g.drawLine(xloc,Math.round(YTOP-5*VIEWSCALE+i*(STAFFSCALE*STAFFSPACING)*VIEWSCALE),
                           xloc,Math.round(YTOP+(i*(STAFFSCALE*STAFFSPACING))*VIEWSCALE));
                g.drawLine(xloc,Math.round(YTOP+(5+STAFFSCALE*4+i*(STAFFSCALE*STAFFSPACING))*VIEWSCALE),
                           xloc,Math.round(YTOP+(STAFFSCALE*4+i*(STAFFSCALE*STAFFSPACING))*VIEWSCALE));
              }
          break;
        case OptionSet.OPT_BARLINE_MODERN:
          for (int i=0; i<numvoices; i++)
            if (curVersionMusicData.getSection(snum).getVoice(i)!=null)
              {
                g.drawLine(xloc,Math.round(YTOP+(i*(STAFFSCALE*STAFFSPACING))*VIEWSCALE),
                           xloc,Math.round(YTOP+(STAFFSCALE*4+i*(STAFFSCALE*STAFFSPACING))*VIEWSCALE));
              }
          break;
      }
  }

/*------------------------------------------------------------------------
Method:  void drawSectionBarline(Graphics2D g,int xloc,int startv,int endv,boolean incipitEnd)
Purpose: Draw section barline (end or beginning) at specified x location
Parameters:
  Input:  Graphics2D g       - graphical context
          int xloc           - x location for barline
          int startv,endv    - starting/ending voice number (top/bottom)
          boolean incipitEnd - whether this barline separates an incipit
                               from its finalis
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void drawSectionBarline(Graphics2D g,int xloc,int startv,int endv,boolean incipitEnd)
  {
    if (xloc>=viewsize.width)
      return;
    if (incipitEnd)
      {
        g.setColor(Color.blue);
        g.setFont(defaultTextFontSMALL);
        g.drawString("FINALIS",xloc,Math.round(YTOP-STAFFSCALE*3*VIEWSCALE));
      }
    else
      g.setColor(Color.black);
    g.drawLine(xloc,Math.round(YTOP+(startv*STAFFSCALE*STAFFSPACING)*VIEWSCALE),
               xloc,Math.round(YTOP+(endv*STAFFSCALE*STAFFSPACING+STAFFSCALE*4)*VIEWSCALE));
    if (incipitEnd)
      g.setColor(Color.black);
  }

/*------------------------------------------------------------------------
Method:  void drawEndBarline(Graphics2D g,int xloc)
Purpose: Draw ending (full) barline at specified x location
Parameters:
  Input:  Graphics2D g    - graphical context
          int startv,endv - starting/ending voice number (top/bottom)
          int xloc        - x location for barline
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void drawEndBarline(Graphics2D g,int startv,int endv,int xloc)
  {
    if (xloc>=viewsize.width)
      return;
    g.drawLine(xloc,Math.round(YTOP+(startv*STAFFSCALE*STAFFSPACING)*VIEWSCALE),
               xloc,Math.round(YTOP+(endv*STAFFSCALE*STAFFSPACING+STAFFSCALE*4)*VIEWSCALE));
    g.setColor(Color.white);
    g.fillRect(xloc+1,0,viewsize.width-xloc,viewsize.height+1);
    g.setColor(Color.black);
  }

/*------------------------------------------------------------------------
Method:  void addSectionTag(Graphics2D g,ScoreRenderer rs,int x,int y)
Purpose: Add editing tag for music section at specified location
Parameters:
  Input:  ScoreRenderer rs - rendered section information
          int x,y          - location for display
  Output: Graphics2D g     - graphical context
  Return: -
------------------------------------------------------------------------*/

  protected void addSectionTag(Graphics2D g,ScoreRenderer rs,int x,int y)
  {
  }

/*------------------------------------------------------------------------
Method:  void draw_analysis(Graphics2D g,ScoreRenderer renderedSection,int renum,int vnum,float xloc,float yloc)
Purpose: Mark one event in score with analytical aids
Parameters:
  Input:  Graphics2D g                  - graphical context
          ScoreRenderer renderedSection - section to check
          int renum                     - index of event to check
          int vnum                      - voice number of event
          float xloc,yloc               - location at which event was drawn
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void draw_analysis(Graphics2D g,ScoreRenderer renderedSection,int renum,int vnum,float xloc,float yloc)
  {
    RenderedEvent re=renderedSection.eventinfo[vnum].getEvent(renum);
    Event         e=re.getEvent();
    if (e.geteventtype()!=Event.EVENT_NOTE)
      return;

    double mt=re.getmusictime().toDouble(),
           measurepos=(mt-(int)mt)*4;

    g.setFont(FontBOLD18);

    /* mark dissonances */
    if (markdissonances)
      {
        if ((measurepos==0 || measurepos==2) &&
            isdissonant(renderedSection,re,vnum))
          {
            g.setColor(Color.red);
            g.drawString("X",xloc+8,yloc-5);
          }
        else if ((measurepos==1 || measurepos==3) &&
                 isdissonant(renderedSection,re,vnum))
          {
            g.setColor(Color.blue);
            g.drawString("X",xloc+5,yloc-5);
          }
      }

    /* mark directed progressions */
    if (markdirectedprogressions)
      if (measurepos==0 || measurepos==2)
        {
          g.setColor(Color.red);
          int progdir=getprogressiondir(renderedSection,renum,vnum);
          if (progdir!=0)
            g.drawLine(Math.round(xloc-10),Math.round(yloc),Math.round(xloc+10),Math.round(yloc+(-5*progdir)));
        }

    g.setColor(Color.black);
  }

/*------------------------------------------------------------------------
Method:  boolean isdissonant(ScoreRenderer renderedSection,RenderedEvent re,int vnum)
Purpose: Check whether note is dissonant against other voices
Parameters:
  Input:  ScoreRenderer renderedSection - section to check
          RenderedEvent re              - event to check
          int vnum                      - voice number of event
  Output: -
  Return: true if dissonant
------------------------------------------------------------------------*/

  boolean isdissonant(ScoreRenderer renderedSection,RenderedEvent re,int vnum)
  {
    MeasureInfo measure=renderedSection.getMeasure(re.getmeasurenum());

    Event e=re.getEvent();
    for (int i=0; i<numvoices; i++)
      if (i!=vnum)
        {
          int ei=measure.reventindex[i];
          RenderedEvent otherre=renderedSection.eventinfo[i].getEvent(ei);
          while (otherre!=null && otherre.getmusictime().toDouble()<=re.getmusictime().toDouble())
            {
              Event oe=otherre.getEvent();
              if (oe.geteventtype()==Event.EVENT_NOTE && otherre.getmusictime().equals(re.getmusictime()))
                {
                  int p1=((NoteEvent)e).getPitch().placenum,
                      p2=((NoteEvent)oe).getPitch().placenum,
                      interval=Math.abs(p1-p2)%7+1;
                  if (interval==2 || interval==7)
                    return true;
                }
              otherre=renderedSection.eventinfo[i].getEvent(++ei);
            }
        }
    return false;
  }

/*------------------------------------------------------------------------
Method:  int getprogressiondir(ScoreRenderer renderedSection,int renum,int vnum)
Purpose: Check whether note is the end of a 6-8 directed progression, and whether
         it is ascending or descending
Parameters:
  Input:  ScoreRenderer renderedSection - section to check
          int renum                     - index of event to check
          int vnum                      - voice number of event
  Output: -
  Return: 1=ascending, -1=descending, 0=not a directed progression
------------------------------------------------------------------------*/

  int getprogressiondir(ScoreRenderer renderedSection,int renum,int vnum)
  {
    RenderedEvent re=renderedSection.eventinfo[vnum].getEvent(renum);
    Event         e=re.getEvent();
    MeasureInfo   measure=renderedSection.getMeasure(re.getmeasurenum());

    for (int i=0; i<numvoices; i++)
      if (i!=vnum)
        {
          int ei=measure.reventindex[i];
          RenderedEvent otherre=renderedSection.eventinfo[i].getEvent(ei);
          while (otherre!=null && otherre.getmusictime().toDouble()<=re.getmusictime().toDouble())
            {
              Event oe=otherre.getEvent();
              if (oe.geteventtype()==Event.EVENT_NOTE && otherre.getmusictime().equals(re.getmusictime()))
                {
                  int interval2=getAbsInterval((NoteEvent)e,(NoteEvent)oe);
                  if (interval2==1)
                    {
                      NoteEvent pe1=getPreviousNote(renderedSection,vnum,renum),
                                pe2=getPreviousNote(renderedSection,i,ei);
                      if (pe1!=null && pe2!=null)
                        {
                          int interval1=getAbsInterval(pe1,pe2);
                          if (interval1==6 || interval1==3)
                            {
                              int thisint=e.getPitch().placenum-pe1.getPitch().placenum,
                                  otherint=((NoteEvent)oe).getPitch().placenum-pe2.getPitch().placenum;
                              if (thisint+otherint==0)
                                return thisint;
                            }
                        }
                    }
                }
              otherre=renderedSection.eventinfo[i].getEvent(++ei);
            }
        }

    return 0;
  }

  int getAbsInterval(NoteEvent n1,NoteEvent n2)
  {
    int p1=n1.getPitch().placenum,
        p2=n2.getPitch().placenum;
    return Math.abs(p1-p2)%7+1;
  }

  NoteEvent getPreviousNote(ScoreRenderer renderedSection,int vnum,int evnum)
  {
    for (int i=evnum-1; i>=0; i--)
      {
        Event e=renderedSection.eventinfo[vnum].getEvent(i).getEvent();
        if (e.geteventtype()==Event.EVENT_NOTE)
          return (NoteEvent)e;
        else if (e.geteventtype()==Event.EVENT_REST)
          return null;
      }
    return null;
  }


/*------------------------- PUBLIC CALCULATIONS -------------------------*/

/*------------------------------------------------------------------------
Method:  int calc[SectionNum|VNum|Eventnum|HLEventnum](int newsnum,int newvnum,int x,int y)
Purpose: Calculate section/voice/event number at given x-y screen coordinates
Parameters:
  Input:  int newsnum,newvnum - section/voice number (for calculating event numbers)
          int x,y             - coordinates on screen
  Output: -
  Return: section|voice|event number
------------------------------------------------------------------------*/

  public int calcSectionNum(int x)
  {
    int absoluteX=(int)((renderedSections[leftRendererNum].getStartX()+getLeftMeasure().leftx)*VIEWSCALE-XLEFT+x);
    for (int si=0; si<numSections; si++)
      if (absoluteX<(renderedSections[si].getStartX()+renderedSections[si].getXsize())*VIEWSCALE)
        return si;
    return numSections-1;
  }

  public int calcVNum(int snum,int y)
  {
    int newvnum;
    for (newvnum=0; newvnum<renderedSections[snum].getNumVoices()-1; newvnum++)
      if (y+VIEWYSTART<YTOP+((newvnum+1)*STAFFSCALE*STAFFSPACING-STAFFSCALE*(STAFFSPACING-4)/2)*VIEWSCALE)
        break;
    return curVersionMusicData.getSection(snum).getValidVoicenum(newvnum);
  }

  public int calcEventnum(int newsnum,int newvnum,int x)
  {
//    newvnum=curVersionMusicData.getSection(newsnum).getValidVoicenum(newvnum);

    MeasureInfo   leftMeasure=getLeftMeasure();
    int           neweventnum=newsnum==leftRendererNum ? leftMeasure.reventindex[newvnum] : 0;
    double        sectionXstart=0-leftMeasure.leftx,
                  exloc=0,lastexloc;
    RenderedEvent e;
    boolean       nen_found=false;

    for (int si=leftRendererNum; si<newsnum; si++)
      sectionXstart+=renderedSections[si].getXsize()+ScoreRenderer.SECTION_END_SPACING;

    if (neweventnum>=renderedSections[newsnum].eventinfo[newvnum].size())
      neweventnum=renderedSections[newsnum].eventinfo[newvnum].size()-1;
    while (!nen_found)
      {
        e=renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum);
        exloc=XLEFT+(sectionXstart+e.getxloc())*VIEWSCALE;
        if (x>exloc)
          if (neweventnum+1<renderedSections[newsnum].eventinfo[newvnum].size())
            neweventnum++;
          else
/*            if (newsnum<numSections-1)
              {
                sectionXstart+=renderedSections[newsnum].getXsize()+ScoreRenderer.SECTION_END_SPACING;
                newsnum++;
                neweventnum=0;
              }
            else*/
              nen_found=true;
        else
          nen_found=true;

        if (nen_found)
          {
            if ((newsnum>leftRendererNum && neweventnum>0) ||
                (newsnum==leftRendererNum && neweventnum>leftMeasure.reventindex[newvnum]))
              {
                lastexloc=XLEFT+(sectionXstart+renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum-1).getxloc())*VIEWSCALE;
                if (x<=lastexloc+(exloc-lastexloc)/2)
                  {
                    neweventnum--;
                    exloc=lastexloc;
                  }
              }
          }
      }

    /* make sure selection is a displayed event */
    while (neweventnum<renderedSections[newsnum].eventinfo[newvnum].size() &&
           !renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum).isdisplayed())
      neweventnum++;

    return neweventnum;
  }

  /* calculate event numbers for highlighting */
  public int calcHLEventnum(int newsnum,int newvnum,int x)
  {
    newvnum=curVersionMusicData.getSection(newsnum).getValidVoicenum(newvnum);

    MeasureInfo   leftMeasure=getLeftMeasure();
    int           neweventnum=newsnum==leftRendererNum ? leftMeasure.reventindex[newvnum] : 0;
    double        sectionXstart=0-leftMeasure.leftx,
                  exloc=0,lastexloc;
    RenderedEvent e;
    boolean       nen_found=false;

    for (int si=leftRendererNum; si<newsnum; si++)
      sectionXstart+=renderedSections[si].getXsize()+ScoreRenderer.SECTION_END_SPACING;

    if (neweventnum>=renderedSections[newsnum].eventinfo[newvnum].size())
      neweventnum=renderedSections[newsnum].eventinfo[newvnum].size()-1;
    while (!nen_found)
      {
        e=renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum);
        exloc=XLEFT+(sectionXstart+e.getxloc())*VIEWSCALE;
        if (x>exloc)
          if (neweventnum+1<renderedSections[newsnum].eventinfo[newvnum].size())
            neweventnum++;
          else
/*            if (newsnum<numSections-1)
              {
                sectionXstart+=renderedSections[newsnum].getXsize()+ScoreRenderer.SECTION_END_SPACING;
                newsnum++;
                neweventnum=0;
              }
            else*/
              nen_found=true;
        else
          nen_found=true;

        if (nen_found)
          {
            if ((newsnum>leftRendererNum && neweventnum>0) ||
                (newsnum==leftRendererNum && neweventnum>leftMeasure.reventindex[newvnum]))
              {
                RenderedEvent laste=renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum-1);
                lastexloc=XLEFT+(sectionXstart+renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum-1).getxloc())*VIEWSCALE;
                if (x<=lastexloc+laste.getrenderedxsize())
                  {
                    neweventnum--;
                    exloc=lastexloc;
                  }
              }
          }
      }

    /* make sure selection is a displayed event */
    while (neweventnum<renderedSections[newsnum].eventinfo[newvnum].size() &&
           !renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum).isdisplayed())
      neweventnum++;

    return neweventnum;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public VariantVersionData getCurrentVariantVersion()
  {
    return curVariantVersion;
  }

  public PieceData getMusicData()
  {
    return curVersionMusicData;
  }

  public ScoreRenderer[] getRenderedMusic()
  {
    return renderedSections;
  }

  public boolean inVariantVersion()
  {
    return getCurrentVariantVersion()!=musicData.getDefaultVariantVersion();
  }

/*------------------------------------------------------------------------
Methods: set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attribute values
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setCurrentVariantVersion(VariantVersionData vvd)
  {
    curVariantVersion=vvd;
    setMusicDataForDisplay(curVariantVersion==musicData.getDefaultVariantVersion() ?
      musicData.recalcAllEventParams() :
      curVariantVersion.constructMusicData(musicData).recalcAllEventParams());
  }

  protected void setMusicDataForDisplay(PieceData musicData)
  {
    curVersionMusicData=musicData;
    rerender();
    repaint();
  }

/*--------------------------- EVENT LISTENERS ---------------------------*/

/*------------------------------------------------------------------------
Method:     void mousePressed(MouseEvent e)
Implements: MouseListener.mousePressed
Purpose:    Handle mouse click on canvas
Parameters:
  Input:  MouseEvent e - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void mousePressed(MouseEvent e)
  {
    int newSNum=calcSectionNum(e.getX()),
        newVNum=calcVNum(newSNum,e.getY()),
        newEventnum=calcHLEventnum(newSNum,newVNum,e.getX());

    switch (e.getButton())
      {
        case MouseEvent.BUTTON1:
          selectEvent(newVNum,newEventnum);
          break;
        case MouseEvent.BUTTON2:
        case MouseEvent.BUTTON3:
          showVariants(newSNum,newVNum,newEventnum,e.getXOnScreen(),e.getYOnScreen());
          break;
      }
  }

  /* empty MouseListener methods */
  public void mouseClicked(MouseEvent e) {}
  public void mouseDragged(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}

/*------------------------------------------------------------------------
Method:  void unregisterListeners()
Purpose: Remove all action/item/etc listeners when disposing of resources
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void unregisterListeners()
  {
    removeMouseListener(this);
  }
}
