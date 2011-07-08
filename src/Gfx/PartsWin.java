/*----------------------------------------------------------------------*/
/*

        Module          : PartsWin

        Package         : Gfx

        Classes Included: PartsWin,VoicePartView,IncipitView,PartPainter

        Purpose         : Display unscored parts in original notation

        Programmer      : Ted Dumitrescu

        Date Started    : 7/8/05

Updates:
3/27/06:  Moved unscored part renderer to separate public class
          (PartRenderer.java)
          Created PartPainter class to share drawing functions between different
          classes
          Added IncipitView, which puts all parts on one canvas
11/13/06: No longer pre-draws parts images into buffers holding entire parts
          (too much memory usage, and drawing on the fly is fast enough)

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   PartsWin
Extends: JFrame
Purpose: Window for displaying unscored original parts
------------------------------------------------------------------------*/

public class PartsWin extends JFrame implements ActionListener
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  /* windowing variables */
  boolean  win_visible;
  MusicWin musicwin;

  /* data */
  PieceData   musicData;
  int         numvoices;
  boolean     printPreview;
  ArrayList[] renderedVoices;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute values
Parameters:
  Input:  -
  Output: -
  Return: attribute values
------------------------------------------------------------------------*/

  static public int getDefaultSTAFFXSIZE()
  {
    return VoicePartView.defaultSTAFFXSIZE;
  }

  static public int getDefaultSTAFFSCALE()
  {
    return VoicePartView.defaultSTAFFSCALE;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: PartsWin(PieceData p,MusicFont mf,MusicWin mw,boolean pp)
Purpose:     Initialize and lay out window
Parameters:
  Input:  PieceData p  - music data
          MusicFont mf - notation font
          MusicWin mw  - parent window
          boolean pp   - whether to use 'print preview' mode
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public PartsWin(PieceData p,MusicFont mf,MusicWin mw,boolean pp)
  {
    /* initialize window */
    musicwin=mw;
    musicData=p;
    printPreview=pp;
    win_visible=false;
    setTitle(musicwin.getTitle()+(printPreview ? " (print preview)" : " (parts layout)"));
    setIconImage(musicwin.windowIcon);

    /* add components */
    Container contentPane=getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));
    numvoices=musicData.getVoiceData().length;
    if (printPreview && musicData.isIncipitScore())
      layoutIncipitWindow(contentPane);
    else
      layoutPartsPanels(contentPane);
    pack();
    setLocationRelativeTo(mw);

    /* handle other window events */
    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            closewin();
          }
        });
  }

  public PartsWin(PieceData p,MusicFont mf,MusicWin mw)
  {
    this(p,mf,mw,false);
  }

/*------------------------------------------------------------------------
Method:  void layoutPartsPanels(Container c)
Purpose: Add parts displays in separate panels
Parameters:
  Input:  Container c  - component to which to add parts display
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void layoutPartsPanels(Container c)
  {
    VoicePartView[] voiceAreas;
    JScrollPane[]   voiceAreaPanes;
    float           VS=(float)musicwin.optSet.getVIEWSCALE();
    Dimension       panelSize=new Dimension(Math.round(830*VS),Math.round(200*VS));

    voiceAreas=new VoicePartView[numvoices];
    voiceAreaPanes=new JScrollPane[numvoices];
    renderedVoices=new ArrayList[numvoices];
    for (int i=0; i<numvoices; i++)
      {
        voiceAreas[i]=new VoicePartView(musicData.getVoiceData()[i],musicwin.optSet,printPreview);
        voiceAreaPanes[i]=new JScrollPane(voiceAreas[i]);
        voiceAreaPanes[i].setPreferredSize(panelSize);
        voiceAreaPanes[i].getViewport().setBackground(Color.WHITE);
        c.add(voiceAreaPanes[i]);

        renderedVoices[i]=voiceAreas[i].getRenderedData();
      }
  }

/*------------------------------------------------------------------------
Method:  void layoutIncipitWindow(Container c)
Purpose: Add parts displays on a single canvas
Parameters:
  Input:  Container c - component to which to add parts display
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static JFileChooser incipitImageFileChooser=null;
  JButton             generateImageButton=null;
  IncipitView         iView;

  void layoutIncipitWindow(Container c)
  {
    try
      {
        if (incipitImageFileChooser==null)
          incipitImageFileChooser=new JFileChooser(new File(".").getCanonicalPath()+"/data/IMGout/");
      }
    catch (Exception e)
      {
        System.err.println("Error initializing incipit image file chooser: "+e);
      }

    JPanel controlsPanel=new JPanel();
    generateImageButton=new JButton("Generate image file...");
    generateImageButton.addActionListener(this);
    controlsPanel.add(generateImageButton);
    c.add(controlsPanel);

    iView=new IncipitView(musicData,musicwin.optSet);
    JScrollPane iPane=new JScrollPane(iView);
    iPane.getViewport().setBackground(Color.WHITE);
    c.add(iPane);
  }

/*------------------------------------------------------------------------
Method:     void actionPerformed(ActionEvent event)
Implements: ActionListener.actionPerformed
Purpose:    Check for actions in GUI and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void actionPerformed(ActionEvent event)
  {
    Object item=event.getSource();

    if (item==generateImageButton)
      genIncipitImageFile();
  }

/*------------------------------------------------------------------------
Method:  void genIncipitImageFile()
Purpose: Save incipit image to file
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void genIncipitImageFile()
  {
    String defaultImgName=musicwin.windowFileName.replaceFirst("\\.cmme\\.xml","-incipit.JPG");
    File saveFile=new File(defaultImgName);
    incipitImageFileChooser.setSelectedFile(saveFile);
    int saveResponse=incipitImageFileChooser.showSaveDialog(this);
    if (saveResponse==JFileChooser.APPROVE_OPTION)
      try
        {
          File savefile=incipitImageFileChooser.getSelectedFile();

          /* overwrite existing file? */
          if (savefile.exists())
            {
              int confirm_option=JOptionPane.showConfirmDialog(this,
                "Overwrite "+savefile.getName()+"?",
                "File already exists",
                JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);

              if (confirm_option==JOptionPane.NO_OPTION)
                return;
            }

          /* save */
          String fn=savefile.getCanonicalPath();
          if (!fn.matches(".*\\.[Jj][Pp][Gg]"))
            {
              fn=fn.concat(".JPG");
              savefile=new File(fn);
            }

          ImageIO.write(iView.createBufferedImage(),"jpg",savefile);
        }
      catch (Exception e)
        {
          System.err.println("Error creating "+incipitImageFileChooser.getSelectedFile().getName());
        }
  }

/*------------------------------------------------------------------------
Method:  void openwin()
Purpose: Make window appear in front
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void openwin()
  {
    setVisible(true);
    toFront();
    win_visible=true;
  }

/*------------------------------------------------------------------------
Method:  void closewin()
Purpose: Hide window
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void closewin()
  {
    win_visible=false;

    /* unregister listeners */
    WindowListener wl[]=getListeners(WindowListener.class);
    for (int i=0; i<wl.length; i++)
      removeWindowListener(wl[i]);
    if (generateImageButton!=null)
      generateImageButton.removeActionListener(this);

    dispose();
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public ArrayList[] getRenderLists()
  {
    return renderedVoices;
  }
}


class PartsSizeParams
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static int defaultSTAFFSCALE=10,
                    defaultCANVASXSIZE=1000,
                    defaultCANVASYSCALE=defaultSTAFFSCALE*10,              /* amount of vertical space per staff */
                    defaultXMARGIN=20,defaultYMARGIN=20,                   /* margins of drawing space */
                    defaultYSTAFFSTART=defaultYMARGIN+defaultSTAFFSCALE*3, /* top margin for staff drawing */
                    defaultSTAFFXSIZE=defaultCANVASXSIZE-defaultXMARGIN*2;
  static float      defaultVIEWSCALE=.75f;

/*----------------------------------------------------------------------*/
/* Instance variables */

  public float STAFFSCALE,
               CANVASXSIZE,CANVASYSCALE,
               XMARGIN,YMARGIN,
               YSTAFFSTART,STAFFXSIZE,
               VIEWSCALE;

  public Dimension canvasSize;
  public MusicFont musicGfx;

/*------------------------------------------------------------------------
Constructor: PartsSizeParams(float vs,ArrayList<RenderList> staves)
Purpose:     Initialize size parameters based on given scaling factor
Parameters:
  Input:  float vs                     - new scaling factor
          ArrayList<RenderList> staves - number of rendered staves for
                                         display
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public PartsSizeParams(float vs,ArrayList<RenderList> staves)
  {
    VIEWSCALE=vs;

    STAFFSCALE=vs*(float)defaultSTAFFSCALE;
    CANVASXSIZE=vs*(float)defaultCANVASXSIZE;
    CANVASYSCALE=vs*(float)defaultCANVASYSCALE;
    XMARGIN=vs*(float)defaultXMARGIN;
    YMARGIN=vs*(float)defaultYMARGIN;
    YSTAFFSTART=vs*(float)defaultYSTAFFSTART;
    STAFFXSIZE=vs*(float)defaultSTAFFXSIZE;

    if (staves!=null && staves.size()==1)
      {
        STAFFXSIZE=staves.get(0).totalxsize*vs;
        CANVASXSIZE=2*XMARGIN+STAFFXSIZE;
      }
    int numStaves=(staves==null) ? 0 : staves.size();

    musicGfx=new MusicFont(VIEWSCALE);
    canvasSize=new Dimension(Math.round(CANVASXSIZE),
                             Math.round(YSTAFFSTART+CANVASYSCALE*numStaves));
  }

  public PartsSizeParams(float vs)
  {
    this(vs,null);
  }
}

/*------------------------------------------------------------------------
Class:   VoicePartView
Extends: JComponent
Purpose: Canvas for displaying one voice part in original notation
------------------------------------------------------------------------*/

class VoicePartView extends JComponent
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static int defaultSTAFFSCALE=PartsSizeParams.defaultSTAFFSCALE,
                    defaultSTAFFXSIZE=PartsSizeParams.defaultSTAFFXSIZE;

/*----------------------------------------------------------------------*/
/* Instance variables */

  PartsSizeParams sizeParams;
  PartPainter     painter;

  /* music data */
  PartRenderer          renderer;
  ArrayList<RenderList> renderedStaves;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VoicePartView(Voice voiceinfo,OptionSet os,boolean pp)
Purpose:     Initialize canvas and render music
Parameters:
  Input:  Voice voiceinfo - voice/event data
          OptionSet os    - parent window's rendering options
          boolean pp      - whether to use 'print preview' mode
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public VoicePartView(Voice voiceinfo,OptionSet os,boolean pp)
  {
    /* create music rendering information */
    renderer=new PartRenderer(voiceinfo,defaultSTAFFXSIZE,pp);
    renderedStaves=renderer.getRenderedData();

    /* set up graphics */
    sizeParams=new PartsSizeParams(PartsSizeParams.defaultVIEWSCALE*(float)os.getVIEWSCALE(),
                                   renderedStaves);
    painter=new PartPainter(voiceinfo,
                            renderedStaves,sizeParams,pp);
    setPreferredSize(sizeParams.canvasSize);
  }

/*------------------------------------------------------------------------
Method:    void paintComponent(Graphics g)
Overrides: javax.swing.JComponent.paintComponent
Purpose:   Repaint area
Parameters:
  Input:  Graphics g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void paintComponent(Graphics g)
  {
    painter.draw(g,0f,0f);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public ArrayList<RenderList> getRenderedData()
  {
    return renderedStaves;
  }
}


/*------------------------------------------------------------------------
Class:   IncipitView
Extends: JComponent
Purpose: Canvas for displaying all incipits together
------------------------------------------------------------------------*/

class IncipitView extends JComponent
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static double defaultVIEWSCALE=.8;

/*----------------------------------------------------------------------*/
/* Instance variables */

  /* ridiculous array of rendered voices (arrays of staves); no type-safe
     generic array creation (ArrayList<T>[]) */
  ArrayList<ArrayList<RenderList>> renderedVoices;

  /* graphics data */
  public PartsSizeParams sizeParams;
  PartPainter            painter[];
  float                  nameHeight;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: IncipitView(PieceData musicData,OptionSet os)
Purpose:     Initialize canvas and render music
Parameters:
  Input:  PieceData musicData - event data for all voices
          OptionSet os        - parent window's rendering options
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public IncipitView(PieceData musicData,OptionSet os)
  {
    int   numVoices=musicData.getVoiceData().length,
          totalNumStaves=0,cury=0,
          maxx=0;

    renderedVoices=new ArrayList<ArrayList<RenderList>>();

    /* render music */
    for (int i=0; i<numVoices; i++)
      {
        renderedVoices.add(new PartRenderer(musicData.getVoiceData()[i],
                                            PartsSizeParams.defaultSTAFFXSIZE,
                                            true).getRenderedData());
        totalNumStaves+=renderedVoices.get(i).size();
      }
    PartRenderer.incipitJustify(renderedVoices);
    for (int i=0; i<numVoices; i++)
      if (renderedVoices.get(i).get(0).totalxsize>maxx)
        maxx=renderedVoices.get(i).get(0).totalxsize;

    int numColumns=(numVoices>1) ? 2 : 1,
        numRows=(int)Math.round(Math.ceil(((float)numVoices)/2));
    sizeParams=new PartsSizeParams((float)(defaultVIEWSCALE)); //*os.getVIEWSCALE()));
    sizeParams.STAFFXSIZE=maxx*sizeParams.VIEWSCALE;
    nameHeight=sizeParams.musicGfx.displayTextFontMetrics.getHeight();
    Dimension cs=new Dimension(Math.round((sizeParams.STAFFXSIZE+nameHeight)*numColumns+
                               sizeParams.XMARGIN*(numColumns+2)),
                               Math.round((numRows+1)*sizeParams.YSTAFFSTART+numRows*sizeParams.CANVASYSCALE));
    sizeParams.canvasSize=cs;

    setPreferredSize(sizeParams.canvasSize);

    painter=new PartPainter[numVoices];
    for (int i=0; i<numVoices; i++)
      painter[i]=new PartPainter(musicData.getVoiceData()[i],
                                 renderedVoices.get(i),sizeParams,true);
  }

/*------------------------------------------------------------------------
Method:  BufferedImage createBufferedImage()
Purpose: Create image object displaying music in incipit layout
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public BufferedImage createBufferedImage()
  {
    BufferedImage incImg=new BufferedImage(
      sizeParams.canvasSize.width,sizeParams.canvasSize.height,BufferedImage.TYPE_INT_RGB);
    Graphics g=incImg.createGraphics();
    g.setColor(Color.white);
    g.fillRect(0,0,sizeParams.canvasSize.width+1,sizeParams.canvasSize.height+1);
    draw(g);

    return incImg;
  }

/*------------------------------------------------------------------------
Method:    void paintComponent(Graphics g)
Overrides: javax.swing.JComponent.paintComponent
Purpose:   Repaint area
Parameters:
  Input:  Graphics g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void paintComponent(Graphics g)
  {
    draw(g);
  }

/*------------------------------------------------------------------------
Method:  void draw(Graphics g)
Purpose: Draw incipit into any graphical context
Parameters:
  Input:  Graphics g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void draw(Graphics g)
  {
    for (int i=0; i<painter.length; i++)
      painter[i].draw(g,(i%2)*(sizeParams.XMARGIN*2+nameHeight+sizeParams.STAFFXSIZE),
                        sizeParams.YSTAFFSTART/2+(i/2)*(sizeParams.YSTAFFSTART+sizeParams.CANVASYSCALE));
  }
}


/*------------------------------------------------------------------------
Class:   PartPainter
Extends: -
Purpose: Paints one unscored voice part into a given graphical context
------------------------------------------------------------------------*/

class PartPainter
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Voice                 voiceinfo;
  ArrayList<RenderList> staves;
  PartsSizeParams       sizeParams;
  boolean               printPreview,
                        incipitScore;

/*------------------------------------------------------------------------
Constructor: PartPainter(Voice voiceinfo,ArrayList<RenderList> staves,
                         PartsSizeParams psp,boolean pp)
Purpose:     Initialize canvas and render music
Parameters:
  Input:  Voice voiceinfo              - voice data
          ArrayList<RenderList> staves - rendered event placement information
          PartsSizeParams psp          - size/spacing/font information
          boolean pp                   - whether to use 'print preview' mode
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public PartPainter(Voice voiceinfo,ArrayList<RenderList> staves,
                     PartsSizeParams psp,boolean pp)
  {
    this.voiceinfo=voiceinfo;
    this.staves=staves;
    sizeParams=psp;
    printPreview=pp;
    incipitScore=voiceinfo.getGeneralData().isIncipitScore();
  }

/*------------------------------------------------------------------------
Method:  void draw(Graphics g)
Purpose: Draw area
Parameters:
  Input:  Graphics g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void draw(Graphics g1d,float startX,float startY)
  {
    Graphics2D g=(Graphics2D)g1d;
    Rectangle  bounds=g.getClipBounds();
    float      XMARGIN=sizeParams.XMARGIN;

    /* initialize drawing context */
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

    /* write voice information */
    if (printPreview)
      {
        /* print voice name sideways at left */
        g.setColor(Color.black);
        g.setFont(sizeParams.musicGfx.displayTextFont);
        String      vn=voiceinfo.getName();
        FontMetrics fmetrics=sizeParams.musicGfx.displayTextFontMetrics;
        float       xs=fmetrics.stringWidth(vn),ys=fmetrics.getHeight();
        XMARGIN+=ys;

        java.awt.geom.AffineTransform saveTransform=g.getTransform();
        g.rotate(Math.toRadians(-90));
        g.drawString(vn,0-startY-sizeParams.YSTAFFSTART-sizeParams.STAFFSCALE*2-xs/2,
                        startX+XMARGIN-ys/2);
        g.setTransform(saveTransform);
      }
    else
      {
        g.setColor(Color.red);
        g.setFont(sizeParams.musicGfx.displayTextLargeFont);
        g.drawString(voiceinfo.getName(),startX+XMARGIN,startY+sizeParams.YMARGIN);
      }

    /* draw music */
    g.setColor(Color.black);
    float cury=startY+sizeParams.YSTAFFSTART;
    for (Iterator i=staves.iterator(); i.hasNext();)
      {
        StaffEventData curstaff=(StaffEventData)i.next();

        drawStaff(g,startX+XMARGIN,cury,5,((float)curstaff.totalxsize)*sizeParams.VIEWSCALE);
        for (int ei=0; ei<curstaff.size(); ei++)
          {
            RenderedEvent e=curstaff.getEvent(ei);
            if (e.isdisplayed())
              {
                e.draw(g,sizeParams.musicGfx,null,
                       startX+XMARGIN+((float)e.getxloc())*sizeParams.VIEWSCALE,cury,sizeParams.VIEWSCALE);

                /* tie */
                if (e.getEvent().hasEventType(Event.EVENT_NOTE))
                  {
                    NoteEvent ne=(NoteEvent)(e.getEvent().getFirstEventOfType(Event.EVENT_NOTE));
                    if (ne.getTieType()!=NoteEvent.TIE_NONE)
                      {
                        double x1=startX+XMARGIN+((float)e.getxloc())*sizeParams.VIEWSCALE,
                               x2=x1+(MusicFont.CONNECTION_SCREEN_LIG_RECTA)*sizeParams.VIEWSCALE;
                        int    e2i=curstaff.getNextEventWithType(Event.EVENT_NOTE,ei+1,1);
                        if (e2i!=-1)
                          x2=startX+XMARGIN+((float)curstaff.getEvent(e2i).getxloc())*sizeParams.VIEWSCALE;

                        ViewCanvas.drawTieOnCanvas(
                          g,ne.getTieType(),
                          x1,x2,ViewCanvas.calcTieY(
                                  e,cury,
                                  (int)(sizeParams.STAFFSCALE/sizeParams.VIEWSCALE),
                                  (int)(sizeParams.STAFFSCALE/(2*sizeParams.VIEWSCALE)),
                                  sizeParams.VIEWSCALE),
                          startX,startX+sizeParams.STAFFXSIZE,sizeParams.VIEWSCALE);
                      }
                  }
              }
          }

        cury+=sizeParams.CANVASYSCALE;
      }
  }

/*------------------------------------------------------------------------
Method:  void drawStaff(Graphics g,float xloc,float yloc,int numlines,float xsize)
Purpose: Draw staff at specified location
Parameters:
  Input:  Graphics g      - graphical context
          float xloc,yloc - location for top/left of staff
          int numlines    - number of lines for staff
          float xsize     - width of staff
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawStaff(Graphics g,float xloc,float yloc,int numlines,float xsize)
  {
    if (xsize>sizeParams.STAFFXSIZE-10 && xsize<sizeParams.STAFFXSIZE)
      xsize=sizeParams.STAFFXSIZE;

    g.setColor(Color.black);
    for (int i=0; i<numlines; i++)
      g.drawLine(Math.round(xloc),Math.round(yloc+i*sizeParams.STAFFSCALE),
                 Math.round(xloc+xsize-1),Math.round(yloc+i*sizeParams.STAFFSCALE));

    /* ending barline */
    if (printPreview && incipitScore && voiceinfo.hasFinalisSection())
      g.drawLine(Math.round(xloc+xsize-1),Math.round(yloc),
                 Math.round(xloc+xsize-1),Math.round(yloc+(numlines-1)*sizeParams.STAFFSCALE));
  }

  void drawStaff(Graphics g,float xloc,float yloc,int numlines)
  {
    drawStaff(g,xloc,yloc,numlines,sizeParams.STAFFXSIZE);
  }
}

