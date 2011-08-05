/*----------------------------------------------------------------------*/
/*

        Module          : ScorePagePreviewWin

        Package         : Gfx

        Classes Included: ScorePagePreviewWin,ScorePageCanvas

        Purpose         : Display scored parts laid out in pages and staff
                          systems

        Programmer      : Ted Dumitrescu

        Date Started    : 5/24/06

Updates:
8/2/06:   added support for arbitrary system sizes (horizontal boundaries)
8/29/07:  added support for multiple-section scores
5/30/08:  removed MusicFont from parameters for constructor; font is now
          created and sized automatically
          added scroll pane for viewing main canvas
12/22/10: added support for ties

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   ScorePagePreviewWin
Extends: JFrame
Purpose: Window for displaying scored music in page layout
------------------------------------------------------------------------*/

public class ScorePagePreviewWin extends JFrame implements ChangeListener,ActionListener
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static int STAFFXSIZE=ScorePageCanvas.STAFFXSIZE,
                    DRAWINGSPACEY=ScorePageCanvas.DRAWINGSPACEY,
                    STAFFSCALE=ScorePageCanvas.STAFFSCALE,
                    CANVASYSCALE=ScorePageCanvas.CANVASYSCALE;

/*----------------------------------------------------------------------*/
/* Instance variables */

  MusicWin parentMusicWin;

  /* data */
  PieceData         musicData;
  ScorePageRenderer renderedPages;

  /* GUI */
  ScorePageCanvas musicScr;
  JSpinner        pageSpinner;
  ZoomControl     viewSizeControl;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ScorePagePreviewWin(PieceData p,MusicWin mw)
Purpose:     Initialize and lay out window
Parameters:
  Input:  PieceData p  - music data
          MusicWin mw  - parent window
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ScorePagePreviewWin(PieceData p,MusicWin mw)
  {
    /* initialize window */
    parentMusicWin=mw;
    musicData=p;
    renderedPages=new ScorePageRenderer(
      musicData,mw.optSet,
      new Dimension(STAFFXSIZE,DRAWINGSPACEY),
      STAFFSCALE,CANVASYSCALE);

    setTitle(parentMusicWin.getTitle()+" (print preview: score)");
    setIconImage(parentMusicWin.windowIcon);

    /* add components */
    Container contentPane=getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));

    /* tool bar */
    JToolBar pageControls=new JToolBar();
    pageControls.setFloatable(false);
//    pageControls.setFocusable(false);
//    pageControls.setLayout(new BoxLayout(pageControls,BoxLayout.X_AXIS));
    pageControls.setLayout(new GridBagLayout());
    pageControls.setAlignmentY(java.awt.Component.LEFT_ALIGNMENT);
    pageControls.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    GridBagConstraints tbc=new GridBagConstraints();
    tbc.anchor=GridBagConstraints.WEST;
    tbc.weightx=0;

    JPanel pageControlsPanel=new JPanel();
//    pageControlsPanel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    pageControlsPanel.add(new JLabel("Page "));
    pageSpinner=new JSpinner(new SpinnerNumberModel(1,1,renderedPages.pages.size(),1));
    pageControlsPanel.add(pageSpinner);
    pageControlsPanel.add(new JLabel(" of "+renderedPages.pages.size()));
    tbc.gridx=0; tbc.gridy=0; tbc.weightx=1; pageControls.add(pageControlsPanel,tbc);

    tbc.gridx++; pageControls.addSeparator();

    viewSizeControl=new ZoomControl((int)(mw.optSet.getVIEWSCALE()*100),this);
    pageControls.add(viewSizeControl);
    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; pageControls.add(viewSizeControl,tbc);

    contentPane.add(pageControls);
    pageSpinner.addChangeListener(this);    

    /* canvas (in scroll pane) */
    musicScr=new ScorePageCanvas(musicData,renderedPages,new MusicFont(0.9),mw);
    JScrollPane musicPane=new JScrollPane(musicScr);
    Dimension actualDisplaySize=java.awt.Toolkit.getDefaultToolkit().getScreenSize(),
              canvasSize=new Dimension(musicScr.getPreferredSize());
    if (canvasSize.width>(float)actualDisplaySize.width*ScorePageCanvas.MAXDISPLAYPORTION)
      canvasSize.width=(int)((float)actualDisplaySize.width*ScorePageCanvas.MAXDISPLAYPORTION);
    if (canvasSize.height>(float)actualDisplaySize.height*ScorePageCanvas.MAXDISPLAYPORTION)
      canvasSize.height=(int)((float)actualDisplaySize.height*ScorePageCanvas.MAXDISPLAYPORTION);
    musicPane.setPreferredSize(canvasSize);
    musicPane.getViewport().setBackground(Color.WHITE);
    contentPane.add(musicPane);

    pack();
    setLocationRelativeTo(mw);
    musicScr.requestFocusInWindow();

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

/*------------------------------------------------------------------------
Method:     void stateChanged(ChangeEvent e)
Implements: ChangeListener.stateChanged
Purpose:    Take action when a proportion-spinner value has changed
Parameters:
  Input:  ChangeEvent e - state-changed event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void stateChanged(ChangeEvent e)
  {
    Object itemChanged=e.getSource();
    if (itemChanged==pageSpinner)
      musicScr.setPage(((Integer)(pageSpinner.getValue())).intValue()-1);
  }

/*------------------------------------------------------------------------
Method:     void actionPerformed(ActionEvent event)
Implements: ActionListener.actionPerformed
Purpose:    Check for action types in menu/tools and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void actionPerformed(ActionEvent event)
  {
    Object item=event.getSource();

    if (item==viewSizeControl.zoomInButton)
      musicScr.setScale(viewSizeControl.zoomIn());
    else if (item==viewSizeControl.zoomOutButton)
      musicScr.setScale(viewSizeControl.zoomOut());
    else if (item==viewSizeControl.viewSizeField)
      musicScr.setScale(viewSizeControl.viewSizeFieldAction());
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
    /* unregister listeners */
    WindowListener wl[]=getListeners(WindowListener.class);
    for (int i=0; i<wl.length; i++)
      removeWindowListener(wl[i]);
    pageSpinner.removeChangeListener(this);

    viewSizeControl.removeListeners();

    dispose();
    musicScr=null;
  }
}


/*------------------------------------------------------------------------
Class:   ScorePageCanvas
Extends: JComponent
Purpose: Canvas for displaying score in page layout
------------------------------------------------------------------------*/

class ScorePageCanvas extends JComponent
{
/*----------------------------------------------------------------------*/
/* Class variables */

  /* standard page size parameters */
  public static final double A4SIZEX=210,
                             A4SIZEY=297,
                             A4SCALEX=A4SIZEX/A4SIZEY;

  public static int STAVESPERPAGE=14, /* including space between systems */
                    STAFFSCALE=10,
                    STAFFPOSSCALE=STAFFSCALE/2,
                    CANVASYSCALE=STAFFSCALE*10,       /* amount of vertical space per staff */
                    XMARGIN=134,YMARGIN=100,          /* margins of drawing space */
                    YSTAFFSTART=YMARGIN+STAFFSCALE*7, /* top margin for staff drawing */
                    DRAWINGSPACEY=CANVASYSCALE*STAVESPERPAGE,
                    CANVASYSIZE=YSTAFFSTART+YMARGIN+DRAWINGSPACEY,
                    CANVASXSIZE=(int)(CANVASYSIZE*A4SCALEX),
                    STAFFXSIZE=CANVASXSIZE-XMARGIN*2;
  public static double MAXDISPLAYPORTION=0.8;

  static Font normalFont=  new Font(null,Font.PLAIN,12),
              snameFont=   new Font(null,Font.PLAIN,15),
              titleFont=   new Font("Serif",Font.PLAIN,24),
              subTitleFont=new Font("Serif",Font.PLAIN,20);

/*----------------------------------------------------------------------*/
/* Instance variables */

  /* music rendering data */
  PieceData         musicData;
  ScorePageRenderer renderedPages;
  OptionSet         musicOptions;
  int               numVoices,
                    curPageNum;
  double            VIEWSCALE;
  boolean           useModernAccSystem;

  /* graphics data */
  BufferedImage canvas,scaledCanvas; /* normal canvas, and scaled display
                                        canvas - drawing directly onto scaled
                                        Graphics2D yields poor results */
  Graphics2D    canvasg2d,scaledCanvasg2d;
  Dimension     canvasSize;
  MusicFont     musicGfx;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ScorePageCanvas(PieceData musicData,ScorePageRenderer rp,MusicFont mf,MusicWin mw)
Purpose:     Initialize canvas and render music
Parameters:
  Input:  PieceData musicData  - event data for all voices
          ScorePageRenderer rp - rendered event data
          MusicFont mf         - music symbols for drawing
          MusicWin mw          - parent window
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ScorePageCanvas(PieceData musicData,ScorePageRenderer rp,MusicFont mf,MusicWin mw)
  {
    /* render data into page/system structure */
    this.musicData=musicData;
    musicOptions=mw.optSet;
    renderedPages=rp;
    curPageNum=0;
    numVoices=musicData.getVoiceData().length;
    useModernAccSystem=musicOptions.getUseModernAccidentalSystem();

    /* create drawing canvas */
    canvasSize=new Dimension(CANVASXSIZE,CANVASYSIZE);
    canvas=new BufferedImage(canvasSize.width,canvasSize.height,BufferedImage.TYPE_INT_ARGB);
    canvasg2d=canvas.createGraphics();
    musicGfx=mf;

    /* created scaled display canvas */
    VIEWSCALE=musicOptions.getVIEWSCALE();
    scaledCanvas=new BufferedImage((int)(canvasSize.width*VIEWSCALE),(int)(canvasSize.height*VIEWSCALE),BufferedImage.TYPE_INT_ARGB);
    scaledCanvasg2d=scaledCanvas.createGraphics();
    scaledCanvasg2d.scale(VIEWSCALE,VIEWSCALE);
    if (VIEWSCALE!=1)
      scaledCanvasg2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    drawPage();

    setPreferredSize(new Dimension((int)(canvasSize.width*VIEWSCALE),(int)(canvasSize.height*VIEWSCALE)));
  }

/*------------------------------------------------------------------------
Method:  void setPage(int newPageNum)
Purpose: Change currently displayed page
Parameters:
  Input:  int newPageNum - number of new page to display
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setPage(int newPageNum)
  {
    if (newPageNum==curPageNum)
      return;
    curPageNum=newPageNum;
    drawPage();
    repaint();
  }

/*------------------------------------------------------------------------
Method:  void setScale(int newScale)
Purpose: Change currently display scale
Parameters:
  Input:  int newScale - new scale * 100
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setScale(int newScale)
  {
    double newVS=((double)newScale)/100;

    /* if scaledCanvas isn't big enough, resize */
    if (canvasSize.width*newVS>scaledCanvas.getWidth() ||
        canvasSize.height*newVS>scaledCanvas.getHeight())
      {
        scaledCanvas=new BufferedImage((int)(canvasSize.width*newVS)+1,(int)(canvasSize.height*newVS)+1,
                                       BufferedImage.TYPE_INT_ARGB);
        scaledCanvasg2d=scaledCanvas.createGraphics();
      }
    else
      scaledCanvasg2d.scale(1/VIEWSCALE,1/VIEWSCALE); /* cancel current scaling */

    VIEWSCALE=newVS;
    scaledCanvasg2d.scale(VIEWSCALE,VIEWSCALE);
    if (VIEWSCALE!=1)
      scaledCanvasg2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);

    setPreferredSize(new Dimension((int)(canvasSize.width*VIEWSCALE),(int)(canvasSize.height*VIEWSCALE)));
    revalidate();
    drawPage();
    repaint();
  }

/*------------------------------------------------------------------------
Method:  void drawPage()
Purpose: Draw music onto canvases
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawPage()
  {
    /* draw onto main canvas */
    canvasg2d.setColor(Color.white);
    canvasg2d.fillRect(0,0,canvasSize.width+1,canvasSize.height+1);

    /* header */
    canvasg2d.setColor(Color.black);
    if (curPageNum==0)
      {
        canvasg2d.setFont(titleFont);
        drawCenteredString(canvasg2d,musicData.getComposer()+": "+musicData.getTitle(),
                           canvasSize.width,YMARGIN);
        if (musicData.getSectionTitle()!=null)
          {
            int yadd=canvasg2d.getFontMetrics().getHeight();
            canvasg2d.setFont(subTitleFont);
            drawCenteredString(canvasg2d,musicData.getSectionTitle(),
                               canvasSize.width,YMARGIN+yadd);
          }
        canvasg2d.setFont(normalFont);
      }
    else
      {
        canvasg2d.setFont(normalFont);
        String smallTitle=musicData.getComposer()+": "+musicData.getTitle();
        if (musicData.getSectionTitle()!=null)
          smallTitle+=" ("+musicData.getSectionTitle()+")";
        canvasg2d.drawString(smallTitle,XMARGIN,YMARGIN);
      }
    if (curPageNum>0)
      drawRightString(canvasg2d,String.valueOf(curPageNum+1),canvasSize.width-XMARGIN,YMARGIN);

    /* music */
    RenderedScorePage curPage=renderedPages.pages.get(curPageNum);
    int startSys=curPage.startSystem,
        endSys=startSys+curPage.numSystems-1;
    if (endSys>=renderedPages.systems.size())
      endSys=renderedPages.systems.size()-1;
    int spaceBetweenSystems=curPage.numSystems<=1 ?
      0 : (DRAWINGSPACEY-curPage.numStaves*CANVASYSCALE)/(curPage.numSystems-(curPageNum==0 ? 0 : 1));
    if (curPage.numSystems<3)
      spaceBetweenSystems=(int)(spaceBetweenSystems/2);

    int curY=YSTAFFSTART+(curPageNum==0 ? spaceBetweenSystems : 0);
    for (int curSys=startSys; curSys<=endSys; curSys++)
      {
        drawSystem(curSys,curY);
        curY+=renderedPages.systems.get(curSys).numVoices*CANVASYSCALE+spaceBetweenSystems;
      }

    /* now scale with acceptable interpolation results by copying onto
       scaled canvas */
    scaledCanvasg2d.setColor(Color.white);
    scaledCanvasg2d.fillRect(0,0,(int)(canvasSize.width/VIEWSCALE)+1,(int)(canvasSize.height/VIEWSCALE)+1);
    scaledCanvasg2d.drawImage(canvas,0,0,this);
  }

/*------------------------------------------------------------------------
Method:  void drawSystem(int sysNum,int starty)
Purpose: Draw music of one system
Parameters:
  Input:  int sysNum - index of system to draw
          int starty - starting y position
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawSystem(int sysNum,int starty)
  {
    RenderedStaffSystem curSystem=renderedPages.systems.get(sysNum);
    int                 clefInfoSize=renderedPages.calcLeftInfoSize(curSystem.startMeasure),
                        rendererNum=ScoreRenderer.calcRendererNum(renderedPages.scoreData,curSystem.startMeasure);
    ScoreRenderer       curRenderer=renderedPages.scoreData[rendererNum];
    MeasureInfo         leftMeasure=curRenderer.getMeasure(curSystem.startMeasure);

    canvasg2d.setColor(Color.black);
    canvasg2d.drawLine(XMARGIN+curSystem.leftX,starty,
                       XMARGIN+curSystem.leftX,starty+(curSystem.numVoices-1)*CANVASYSCALE+4*STAFFSCALE);
    drawSystemBarlines(canvasg2d,XMARGIN+curSystem.leftX+clefInfoSize,starty,curSystem);
    int cury=starty;
    for (int v=0; v<numVoices; v++)
      if (curRenderer.eventinfo[v]!=null)
      {
        if (curSystem.displayVoiceNames)
          {
            canvasg2d.setFont(snameFont);
            drawRightString(canvasg2d,musicData.getVoiceData()[v].getStaffTitle(),
                            XMARGIN+curSystem.leftX-10,cury+STAFFPOSSCALE*5);
          }

        drawStaff(canvasg2d,cury,5,curSystem.leftX,curSystem.rightX);

        int VclefInfoSize=clefInfoSize;
        if (!leftMeasure.beginsWithClef(v) &&
            (curRenderer.getStartingParams()[v].clefSet!=null ||
             curSystem.startMeasure>curRenderer.getFirstMeasureNum()))
          drawClefInfo(canvasg2d,curRenderer,leftMeasure,v,XMARGIN+curSystem.leftX,cury);
        else
          VclefInfoSize=0;

        /* calculate which events go on each staff */
        int leftei=curRenderer.getMeasure(curSystem.startMeasure).reventindex[v],
            rightei;
        if (sysNum<renderedPages.systems.size()-1 &&
            curSystem.endMeasure<curRenderer.getLastMeasureNum()) /* true if NOT the final system in a section */
          rightei=curRenderer.getMeasure(curSystem.endMeasure+1).reventindex[v]-1;
        else
          rightei=curRenderer.eventinfo[v].size()-1;

        /* now draw events */
        RenderedEvent    re=null;
        RenderedLigature ligInfo=null,
                         tieInfo=null;
        for (int ei=leftei; ei<=rightei; ei++)
          {
            re=curRenderer.eventinfo[v].getEvent(ei);
            int xloc=calcXLoc(curSystem,VclefInfoSize,re);

            /* tmp clef BS for very first system */
            if (ei==0 && sysNum==0)
              xloc=(int)(XMARGIN+curSystem.leftX);

            if (re.isdisplayed())
              re.draw(canvasg2d,musicGfx,this,xloc,cury);

            /* draw ligatures */
            ligInfo=re.getLigInfo();
            if (re.isligend() && musicData.getSection(rendererNum).getSectionType()==MusicSection.MENSURAL_MUSIC)
              {
                int ligLeftX=ligInfo.firstEventNum<leftei ? XMARGIN-1 :
                  (int)(XMARGIN+curSystem.leftX+clefInfoSize+4+curRenderer.eventinfo[v].getEvent(ligInfo.firstEventNum).getxloc()*curSystem.spacingCoefficient);
                drawLigature(canvasg2d,ligLeftX,xloc,cury+calcLigY(v,re),XMARGIN+clefInfoSize,XMARGIN+STAFFXSIZE);
              }

            /* tie notes */
            tieInfo=re.getTieInfo();
            if (tieInfo.firstEventNum!=-1)// && tieInfo.lastEventNum==ei)
//            if (tieInfo.firstEventNum!=-1 && tieInfo.lastEventNum==ei)
              {
                RenderedEvent tre1=curRenderer.eventinfo[v].getEvent(tieInfo.firstEventNum);
                int tieLeftX=tieInfo.firstEventNum<leftei ? XMARGIN-1 :
                  (int)(XMARGIN+curSystem.leftX+VclefInfoSize+4+curRenderer.eventinfo[v].getEvent(tieInfo.firstEventNum).getxloc()*curSystem.spacingCoefficient);

                drawTie(canvasg2d,tre1.getTieType(),
                        tieLeftX,xloc,
                        cury+calcTieY(v,re),XMARGIN+VclefInfoSize,XMARGIN+STAFFXSIZE);
              }

            /* some more clef spacing adjustment, for staves beginning with new clefs */
            if (ei==leftMeasure.lastBeginClefIndex[v] && ei<rightei)
              {
                int nextX=calcXLoc(curSystem,VclefInfoSize,
                                   curRenderer.eventinfo[v].getEvent(ei+1))-
                          XMARGIN-curSystem.leftX;
                if (clefInfoSize>nextX)
                  VclefInfoSize=clefInfoSize;//-nextX;
              }
          }

        /* finish any unclosed ligature */
        ligInfo=re==null ? null : re.getLigInfo();
        if (ligInfo!=null && !re.isligend() && ligInfo.firstEventNum!=-1)
          {
            int ligLeftX=ligInfo.firstEventNum<leftei ? XMARGIN-1 :
                  (int)(XMARGIN+curSystem.leftX+VclefInfoSize+4+curRenderer.eventinfo[v].getEvent(ligInfo.firstEventNum).getxloc()*curSystem.spacingCoefficient);
            drawLigature(canvasg2d,ligLeftX,XMARGIN+STAFFXSIZE,cury+calcLigY(v,re),XMARGIN+clefInfoSize,XMARGIN+STAFFXSIZE);
          }

        /* finish any unclosed tie */
        tieInfo=re==null ? null : re.getTieInfo();
        if (tieInfo!=null && tieInfo.firstEventNum!=-1 &&
            (tieInfo.lastEventNum!=rightei || re.doubleTied()))
          {
            RenderedEvent tre1=re.doubleTied() ? re : curRenderer.eventinfo[v].getEvent(tieInfo.firstEventNum);
            int firstEventNum=re.doubleTied() ? rightei : tieInfo.firstEventNum;
            int tieLeftX=firstEventNum<leftei ? XMARGIN-1 :
                  (int)(XMARGIN+curSystem.leftX+VclefInfoSize+4+tre1.getxloc()*curSystem.spacingCoefficient);
            drawTie(canvasg2d,tre1.getTieType(),tieLeftX,XMARGIN+STAFFXSIZE,cury+calcTieY(v,re),XMARGIN+clefInfoSize,XMARGIN+STAFFXSIZE);
          }

        cury+=CANVASYSCALE;
      }
  }

  int calcXLoc(RenderedStaffSystem curSystem,int VclefInfoSize,RenderedEvent re)
  {
    return (int)(XMARGIN+
                 curSystem.leftX+
                 VclefInfoSize+
                 re.getxloc()*curSystem.spacingCoefficient);
  }

/*------------------------------------------------------------------------
Method:  void drawClefInfo(Graphics2D g,int vnum,int mnum,int xloc,int yloc)
Purpose: Draw clefs at left side of staff
Parameters:
  Input:  Graphics2D g  - graphical context
          int vnum      - voice number
          int mnum      - measure number
          int xloc,yloc - starting coordinates for drawing
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawClefInfo(Graphics2D g,ScoreRenderer renderer,MeasureInfo leftMeasure,
                    int vnum,int xloc,int yloc)
  {
    int leftEventIndex=leftMeasure.reventindex[vnum];

    /* draw clefs */
    RenderedClefSet leftCS=renderer.eventinfo[vnum].getClefEvents(leftEventIndex);
    if (leftCS!=null)
      xloc+=(int)leftCS.draw(useModernAccSystem,g,musicGfx,(double)xloc,(double)yloc,1);

    /* modern key signature */
    ModernKeySignature mk=renderer.eventinfo[vnum].getModernKeySig(leftEventIndex);
    if (mk.numEls()>0 && leftCS!=null && useModernAccSystem)
      xloc+=(int)ViewCanvas.drawModKeySig(
        g,musicGfx,mk,leftCS.getPrincipalClefEvent(),(double)xloc,(double)yloc,
        1,false,STAFFSCALE,STAFFPOSSCALE);
  }

/*------------------------------------------------------------------------
Method:  int calcLigY(int vnum,RenderedEvent e)
Purpose: Calculate y position of a ligature at a given event (relative to
         staff)
Parameters:
  Input:  int vnum        - voice number
          RenderedEvent e - event
  Output: -
  Return: y position of ligature
------------------------------------------------------------------------*/

  static final int DEFAULT_LIGYVAL=-7;

  int calcLigY(int vnum,RenderedEvent e)
  {
    RenderedLigature ligInfo=e.getLigInfo();
    RenderedEvent    lige=ligInfo.reventList.getEvent(ligInfo.yMaxEventNum);
    Clef             ligevclef=lige.getClef();
    DataStruct.Event lignoteev=ligInfo.yMaxEvent;

    int ligyval=STAFFSCALE*4-12-
                STAFFPOSSCALE*lignoteev.getPitch().calcypos(ligevclef);
    if (ligyval>DEFAULT_LIGYVAL)
      ligyval=DEFAULT_LIGYVAL;

    return ligyval;
  }

  int calcTieY(int vnum,RenderedEvent e)
  {
    RenderedLigature tieInfo=e.getTieInfo();
    RenderedEvent    tieRE=tieInfo.reventList.getEvent(tieInfo.yMaxEventNum);
    Clef             tieREclef=tieRE.getClef();
    DataStruct.Event tieNoteEv=tieInfo.yMaxEvent;

    return STAFFSCALE*4-12-
           STAFFPOSSCALE*tieNoteEv.getPitch().calcypos(tieREclef);
  }

/*------------------------------------------------------------------------
Method:  void drawLigature(Graphics2D g,int x1,int x2,int y,int leftx,int rightx)
Purpose: Draw ligature bracket for one voice
Parameters:
  Input:  Graphics2D g     - graphical context
          int x1,x2        - left and right coordinates of bracket
          int y            - y level of bracket
          int leftx,rightx - horizontal bounds of drawing space
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawLigature(Graphics2D g,int x1,int x2,int y,int leftx,int rightx)
  {
    if (musicOptions.get_displayligbrackets())
      ViewCanvas.drawLigOnCanvas(g,x1,x2,y,leftx,rightx);
  }

  void drawTie(Graphics2D g,int tieType,int x1,int x2,int y,int leftx,int rightx)
  {
    ViewCanvas.drawTieOnCanvas(g,tieType,x1,x2,y,leftx,rightx);
  }

/*------------------------------------------------------------------------
Method:  void drawStaff(Graphics2D g,int yloc,int numlines,int leftX,int rightX)
Purpose: Draw staff at specified location
Parameters:
  Input:  Graphics2D g     - graphical context
          int yloc         - y location for top of staff
          int numlines     - number of lines for staff
          int leftX,rightX - horizontal bounds of staff
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawStaff(Graphics2D g,int yloc,int numlines,int leftX,int rightX)
  {
    g.setColor(Color.black);
    for (int i=0; i<numlines; i++)
      g.drawLine(XMARGIN+leftX,yloc+i*STAFFSCALE,XMARGIN+rightX,yloc+i*STAFFSCALE);
  }

/*------------------------------------------------------------------------
Method:  void drawSystemBarlines(Graphics2D g,int xloc,int yloc,
                                 RenderedStaffSystem curSystem)
Purpose: Draw barlines across one system
Parameters:
  Input:  Graphics2D g                  - graphical context
          int xloc,yloc                 - location for top left of first staff
          RenderedStaffSystem curSystem - staff system information
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawSystemBarlines(Graphics2D g,int xloc,int yloc,
                          RenderedStaffSystem curSystem)
  {
    int curx=0,
        rendererNum=ScoreRenderer.calcRendererNum(renderedPages.scoreData,curSystem.startMeasure);

    g.setColor(Color.black);
    if (curSystem.startMeasure>0)
      {
        g.setFont(normalFont);
        g.drawString(String.valueOf(curSystem.startMeasure+1),xloc-10,yloc-STAFFSCALE*3); /* measure number */
      }
    for (int i=curSystem.startMeasure; i<curSystem.endMeasure; i++)
      {
        curx+=renderedPages.scoreData[rendererNum].getMeasure(i).xlength;
        drawBarlines(g,(int)(xloc+curx*curSystem.spacingCoefficient+4),yloc,curSystem.numVoices);
      }
    if (curSystem.endMeasure<renderedPages.scoreData[rendererNum].getLastMeasureNum())
      drawBarlines(g,XMARGIN+curSystem.rightX,yloc,curSystem.numVoices);//STAFFXSIZE-1,yloc);
    else
      /* final barline */
      g.drawLine(XMARGIN+curSystem.rightX,yloc,
                 XMARGIN+curSystem.rightX,yloc+(curSystem.numVoices-1)*CANVASYSCALE+4*STAFFSCALE);
  }

/*------------------------------------------------------------------------
Method:  void drawBarlines(Graphics2D g,int xloc,int yloc,int numVoices)
Purpose: Draw barlines at specified x location
Parameters:
  Input:  Graphics2D g  - graphical context
          int xloc,yloc - location for barlines
          int numVoices - number of voices in system
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawBarlines(Graphics2D g,int xloc,int yloc,int numVoices)
  {
    switch (musicOptions.get_barline_type())
      {
        case OptionSet.OPT_BARLINE_NONE:
          break;
        case OptionSet.OPT_BARLINE_MENSS:
          for (int i=0; i<numVoices-1; i++)
            g.drawLine(xloc,yloc+STAFFSCALE*4+i*CANVASYSCALE,xloc,yloc+(i+1)*CANVASYSCALE);
          break;
        case OptionSet.OPT_BARLINE_TICK:
          for (int i=0; i<numVoices; i++)
	    {
              g.drawLine(xloc,yloc-5+i*CANVASYSCALE,xloc,yloc+i*CANVASYSCALE);
              g.drawLine(xloc,yloc+5+STAFFSCALE*4+i*CANVASYSCALE,
                         xloc,yloc+STAFFSCALE*4+i*CANVASYSCALE);
	    }
          break;
        case OptionSet.OPT_BARLINE_MODERN:
          for (int i=0; i<numVoices; i++)
            g.drawLine(xloc,yloc+i*CANVASYSCALE,xloc,yloc+STAFFSCALE*4+i*CANVASYSCALE);
          break;
      }
  }

/*------------------------------------------------------------------------
Method:  void draw[Centered|Right]String(Graphics2D g,String s,[int xsize|xloc],int yloc)
Purpose: Draw centered/right-justified string at specified y-location
Parameters:
  Input:  Graphics2D g  - graphical context
          String s      - string to draw
          int xsize     - total horizontal space available
          int xloc,yloc - string location
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawCenteredString(Graphics2D g,String s,int xsize,int yloc)
  {
    int strSize=g.getFontMetrics().stringWidth(s);
    g.drawString(s,(xsize-strSize)/2,yloc);
  }

  void drawRightString(Graphics2D g,String s,int xloc,int yloc)
  {
    int strSize=g.getFontMetrics().stringWidth(s);
    g.drawString(s,xloc-strSize,yloc);
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
    /* copy current offscreen buffer to screen */
    g.drawImage(scaledCanvas,0,0,this);
  }
}

