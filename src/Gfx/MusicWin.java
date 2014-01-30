/*----------------------------------------------------------------------*/
/*

        Module          : MusicWin.java

        Package         : Gfx

        Classes Included: MusicWin

        Purpose         : Lays out main music window and functions

        Programmer      : Ted Dumitrescu

        Date Started    : 3/15/99

Updates:
3/21/99:  converted from MainWin to MusicWin
          began interfacing with DataStruct.PieceData
4/16/99:  added menu structure/event handling
4/22/99:  added double-buffering, display scrolling
4/23/99:  moved most of the graphics calculation out of ViewCanvas into a
          separate "prerenderer" (MusicRenderer; now ScoreRenderer)
4/26/99:  converted GUI to Swing
3/29/04:  fiddled with positioning of barlines and ligature brackets
4/2/04:   added final barline, removed blank staff space when viewing end of
          music
4/3/04:   added support for resizing viewing panel
3/17/05:  removed references to Client classes
3/21/05:  added variable ligature y positioning
4/18/05:  added view scaling options, rearranged data to keep musical info within
          ViewCanvas (MusicWin should be just for GUI)
4/19/05:  moved music viewing options from separate window to View menu
5/2/05:   moved ViewCanvas to separate file
7/8/05:   added interface for displaying unscored parts in separate window
2/28/06:  added support for initial window positioning
4/3/06:   unregisters event listeners when disposing
5/4/06:   moved static file-choosing/opening here from Editor.EditorWin (for
          browsing and opening any local file)
5/23/06:  added print previews submenu (for choosing parts or score view in
          page layout)
5/14/07:  added toolbar buttons for selecting view options
6/29/07:  GUI icons now load through Classloader (for storage in applet JAR)
12/7/07:  added Versions menu
5/08:     expanded Versions menu
8/4/08:   replaced zoom control panel with ZoomControl object
10/17/08: added handleRuntimeError
7/14/09:  added file chooser for PDF generation
6/24/10:  added (menu-only) GUI for toggling modern note shapes

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import java.util.*;
import javax.swing.filechooser.FileFilter;
import java.net.URL;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.zip.*;

import org.jdom.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   MusicWin
Extends: JFrame
Purpose: Lays out and implements main music window
------------------------------------------------------------------------*/

public class MusicWin extends JFrame implements ActionListener,ItemListener,WindowFocusListener
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int FILETYPE_CMME=0,
                          FILETYPE_MIDI=1,
                          FILETYPE_XML= 2;

  public static final String FILENAME_EXTENSION_CMME=".cmme.xml",
                             FILENAME_EXTENSION_MIDI=".MID",
                             FILENAME_EXTENSION_XML= ".xml",
                             FILENAME_EXTENSION_HTML=".html",

                             FILENAME_PATTERN_CMME=".*\\.cmme\\.xml",
                             FILENAME_PATTERN_MIDI=".*\\.[Mm][Ii][Dd]",
                             FILENAME_PATTERN_XML= ".*\\.[Xx][Mm][Ll]",
                             FILENAME_PATTERN_HTML=".*\\.[Hh][Tt][Mm][Ll]";

  public static final Dimension SCREEN_DIMENSION=
    java.awt.Toolkit.getDefaultToolkit().getScreenSize();

  public static final String DEFAULT_PDFDIR="PDFout/",
                             DEFAULT_CRITNOTESDIR="TXTout/";

  public static final int MAX_STACK_TRACE_LEVELS=8;

  public static String BaseDataURL;
  static String        initDir,
                       initPDFDir,
                       initCritNotesDir;
  public static int    DefaultViewScale;

  /* music windows currently open */
  protected static LinkedList<MusicWin> fileWindows=null;
  protected static MusicWin             curWindow=null;
  protected static boolean              exitingProgram=false;
  protected static Viewer.Main          viewerWin=null;

  /* global file chooser variables */
  protected static CMMEFileFilter CMMEFFilter;
  protected static MIDIFileFilter MIDIFFilter;
  protected static XMLFileFilter  XMLFFilter;
  protected static HTMLFileFilter HTMLFFilter;
  protected static PDFFileFilter  PDFFFilter;
  protected static JFileChooser   musicWinFileChooser,
                                  saveFileChooser,
                                  exportFileChooser,
                                  PDFFileChooser,
                                  critNotesFileChooser;

  /* GUI icons */
  static ImageIcon NoteShapesOldIcon_light,NoteShapesOldIcon_dark,
                   NoteShapesModIcon_light,NoteShapesModIcon_dark,
                   ClefsOldIcon_light,ClefsOldIcon_dark,
                   ClefsModIcon_light,ClefsModIcon_dark,
                   EdAccidentalsIcon_light,EdAccidentalsIcon_dark,
                   PitchOldIcon_light,PitchOldIcon_dark,
                   PitchModIcon_light,PitchModIcon_dark,
                   TextingOldIcon_light,TextingOldIcon_dark,
                   TextingModIcon_light,TextingModIcon_dark;

/*----------------------------------------------------------------------*/
/* Instance variables */

  public boolean  updatemusicgfx=true,
                  rerendermusic=false,
                  redrawscr=false;

  protected PieceData           musicData;
  protected MusicFont           MusicGfx;
  public    OptionSet           optSet;
  protected PartsWin            partsWin;
  protected ScorePagePreviewWin scorePageWin;
  protected JDialog             genPDFDialog;


  public Container cp;
  public Dimension origsize,WinBorder;
  public String    windowFileName;
  public Image     windowIcon;

  /* Main options GUI */
  JMenuBar TopMenuBar;
  JMenu    FileMenu,
           EditMenu,
           ViewMenu,
           SectionsMenu,
           TextMenu,
           VersionsMenu,
           AnalysisMenu;
  JToolBar MainToolBar;

  protected ZoomControl MTZoomControl;

  /* Viewing/action area */
  protected ViewCanvas ViewScr;

  JPanel     BottomPanel;
  JPanel     StatusPanel;
  JLabel     StatusMeasureLabel,
             StatusMeasureNum;
  JScrollBar MusicScrollBarX,
             MusicScrollBarY;

  /* action listeners */
  protected ViewSizeListener     VSListener=new ViewSizeListener();
  protected BarlineStyleListener VMBSListener=new BarlineStyleListener();
  protected NoteShapeStyleListener VMNSListener=new NoteShapeStyleListener();

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  Dimension fitInScreen(Dimension dimToCheck,float scrProportion)
Purpose: Check whether dimension can fit within screen space and if not,
         calculate new dimension which does
Parameters:
  Input:  Dimension dimToCheck - dimension to check
          float scrProportion  - proportion of screen space to allow
  Output: -
  Return: New dimension which fits within screen dimensions * scrProportion
------------------------------------------------------------------------*/

  static final float AVAIL_SCREEN_PROPORTION=0.95f;

  public static Dimension fitInScreen(Dimension dimToCheck)
  {
    return fitInScreen(dimToCheck,AVAIL_SCREEN_PROPORTION);
  }

  public static Dimension fitInScreen(Dimension dimToCheck,float scrProportion)
  {
    return fitInScreen(dimToCheck,scrProportion,scrProportion);
  }

  public static Dimension fitInScreen(Dimension dimToCheck,float scrProportionX,float scrProportionY)
  {
    Dimension d=new Dimension(dimToCheck);

    if (d.width>(int)(SCREEN_DIMENSION.width*scrProportionX))
      d.width=(int)(SCREEN_DIMENSION.width*scrProportionX);
    if (d.height>(int)(SCREEN_DIMENSION.height*scrProportionY))
      d.height=(int)(SCREEN_DIMENSION.height*scrProportionY);

    return d;
  }

/*------------------------------------------------------------------------
Method:  void initScoreWindowing(String bdu,String initDir,boolean inApplet)
Purpose: Initialize global objects shared by music windows
Parameters:
  Input:  String bdu       - URL of program data directories
          String initDir   - initial directory for file chooser
          boolean inApplet - running within applet context?
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static boolean isCMMEFilename(String filename)
  {
    return filename.matches(".*\\.[Cc][Mm][Mm][Ee]\\.[Xx][Mm][Ll]");
  }

  public static boolean isMIDIFilename(String filename)
  {
    return filename.matches(FILENAME_PATTERN_MIDI);
  }

  public static boolean isMusicXMLFilename(String filename)
  {
    return filename.matches(FILENAME_PATTERN_XML) &&
           !isCMMEFilename(filename);
  }

  public static boolean isHTMLFilename(String filename)
  {
    return filename.matches(FILENAME_PATTERN_HTML);
  }

  protected static JFileChooser getMusicWinFileChooser()
  {
    if (musicWinFileChooser==null)
      {
        musicWinFileChooser=new JFileChooser(initDir);
        musicWinFileChooser.addChoosableFileFilter(CMMEFFilter);
        musicWinFileChooser.addChoosableFileFilter(MIDIFFilter);
        musicWinFileChooser.addChoosableFileFilter(XMLFFilter);
        musicWinFileChooser.setFileFilter(CMMEFFilter);
      }

    return musicWinFileChooser;
  }

  protected static JFileChooser getSaveFileChooser()
  {
    if (saveFileChooser==null)
      {
        saveFileChooser=new JFileChooser(initDir);
        saveFileChooser.addChoosableFileFilter(CMMEFFilter);
        saveFileChooser.setFileFilter(CMMEFFilter);
      }

    saveFileChooser.setCurrentDirectory(getMusicWinFileChooser().getCurrentDirectory());

    return saveFileChooser;
  }

  protected static JFileChooser getExportFileChooser()
  {
    if (exportFileChooser==null)
      {
        exportFileChooser=new JFileChooser(initDir);
        exportFileChooser.addChoosableFileFilter(MIDIFFilter);
        exportFileChooser.addChoosableFileFilter(XMLFFilter);
        exportFileChooser.setFileFilter(XMLFFilter);
      }

    exportFileChooser.setCurrentDirectory(getSaveFileChooser().getCurrentDirectory());

    return exportFileChooser;
  }

  protected static JFileChooser getPDFFileChooser()
  {
    if (PDFFileChooser==null)
      {
        PDFFileChooser=new JFileChooser(initPDFDir);
        PDFFileChooser.addChoosableFileFilter(PDFFFilter);
        PDFFileChooser.setFileFilter(PDFFFilter);
      }

    return PDFFileChooser;
  }

  protected static JFileChooser getCritNotesFileChooser()
  {
    if (critNotesFileChooser==null)
      {
        critNotesFileChooser=new JFileChooser(initCritNotesDir);
        critNotesFileChooser.addChoosableFileFilter(HTMLFFilter);
        critNotesFileChooser.setFileFilter(HTMLFFilter);
      }

    return critNotesFileChooser;
  }

  public static void initScoreWindowing(String bdu,String initDir,boolean inApplet)
  {
    musicWinFileChooser=null;
    MusicWin.initDir=initDir;
    BaseDataURL=bdu;
    try
      {
        initPDFDir=new URL(BaseDataURL+DEFAULT_PDFDIR).getFile();
        initCritNotesDir=new URL(BaseDataURL+DEFAULT_CRITNOTESDIR).getFile();
      }
    catch (Exception e)
      {
        initPDFDir=new String(initDir);
        initCritNotesDir=new String(initDir);
      }

    CMMEFFilter=new CMMEFileFilter();
    MIDIFFilter=new MIDIFileFilter();
    XMLFFilter=new XMLFileFilter();
    HTMLFFilter=new HTMLFileFilter();
    PDFFFilter=new PDFFileFilter();
    fileWindows=new LinkedList<MusicWin>();

    /* calculate default scaling based on screen dimensions */
    float dvs=((float)SCREEN_DIMENSION.height)/1200;
    DefaultViewScale=Math.round(dvs*100);

    initGUIIcons();
  }

  public static void closeAllWindows()
  {
    LinkedList<MusicWin> fileWindowsList=new LinkedList<MusicWin>(fileWindows);
    for (MusicWin mw : fileWindowsList)
      mw.closewin();
  }

/*------------------------------------------------------------------------
Method:  void initGUIIcons()
Purpose: Initialize tool icons for viewer GUI
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected static void initGUIIcons()
  {
    try
      {
        /* basic image elements */
        BufferedImage lightBG=javax.imageio.ImageIO.read(
                        MusicWin.class.getClassLoader().getResource("data/imgs/GUIicons/buttonbg-light.gif")),
                      darkBG=javax.imageio.ImageIO.read(
                        MusicWin.class.getClassLoader().getResource("data/imgs/GUIicons/buttonbg-dark.gif"));

        /* note shapes */
        NoteShapesOldIcon_light=makeIconWithBG("noteval-buttonORIGa.gif",lightBG);
        NoteShapesOldIcon_dark=makeIconWithBG("noteval-buttonORIGa.gif",darkBG);
        NoteShapesModIcon_light=makeIconWithBG("noteval-buttonMODERNa.gif",lightBG);
        NoteShapesModIcon_dark=makeIconWithBG("noteval-buttonMODERNa.gif",darkBG);

        /* cleffing */
        ClefsOldIcon_light=makeIconWithBG("clef-buttonC1a.gif",lightBG);
        ClefsOldIcon_dark=makeIconWithBG("clef-buttonC1a.gif",darkBG);
        ClefsModIcon_light=makeIconWithBG("clef-buttonMODERNG1a.gif",lightBG);
        ClefsModIcon_dark=makeIconWithBG("clef-buttonMODERNG1a.gif",darkBG);

        /* editorial accidentals */
        EdAccidentalsIcon_light=makeIconWithBG("edacc-button.gif",lightBG);
        EdAccidentalsIcon_dark=makeIconWithBG("edacc-button.gif",darkBG);

        /* pitch system */
        PitchOldIcon_light=makeIconWithBG("clef-buttonBmol1a.gif",lightBG);
        PitchOldIcon_dark=makeIconWithBG("clef-buttonBmol1a.gif",darkBG);
        PitchModIcon_light=makeIconWithBG("clef-buttonMODERNFLAT1a.gif",lightBG);
        PitchModIcon_dark=makeIconWithBG("clef-buttonMODERNFLAT1a.gif",darkBG);

        /* texting */
        TextingOldIcon_light=makeIconWithBG("textorig-button.gif",lightBG);
        TextingOldIcon_dark=makeIconWithBG("textorig-button.gif",darkBG);
        TextingModIcon_light=makeIconWithBG("textmodern-button.gif",lightBG);
        TextingModIcon_dark=makeIconWithBG("textmodern-button.gif",darkBG);
      }
    catch (Exception e)
      {
        System.err.println("Error loading icons: "+e);
        if (MetaData.CMME_OPT_TESTING)
          e.printStackTrace();
      }
  }

/*------------------------------------------------------------------------
Method:  ImageIcon makeIconWithBG(String imgFilename,BufferedImage BG)
Purpose: Combine background image with image file to create icon
Parameters:
  Input:  String imgFilename - filename for foreground image
          BufferedImage BG   - background image
  Output: -
  Return: icon with combined images
------------------------------------------------------------------------*/

  static ImageIcon makeIconWithBG(String imgFilename,BufferedImage BG) throws Exception
  {
    BufferedImage curCanvas=new BufferedImage(BG.getWidth(),BG.getHeight(),BufferedImage.TYPE_INT_ARGB),
                  curFileImg=javax.imageio.ImageIO.read(
                    MusicWin.class.getClassLoader().getResource("data/imgs/GUIicons/"+imgFilename));
    Graphics2D    curG=curCanvas.createGraphics();

    curG.drawImage(BG,0,0,null);
    curG.drawImage(curFileImg,0,0,null);
    return new ImageIcon(curCanvas);
  }

  /* ----- chooser filter for CMME files ----- */
  static class CMMEFileFilter extends FileFilter
  {
    public boolean accept(File f)
    {
      if (f.isDirectory())
        return true;
      String fn=f.getName();
      return fn.matches(FILENAME_PATTERN_CMME) ? true : false;
    }

    public String getDescription()
    {
      return "CMME scores (.cmme.xml)";
    }
  }

  /* ----- chooser filter for MIDI files ----- */
  static class MIDIFileFilter extends FileFilter
  {
    public boolean accept(File f)
    {
      if (f.isDirectory())
        return true;
      String fn=f.getName();
      return fn.matches(FILENAME_PATTERN_MIDI) ? true : false;
    }

    public String getDescription()
    {
      return "MIDI files (.mid)";
    }
  }

  /* ----- chooser filter for XML files ----- */
  static class XMLFileFilter extends FileFilter
  {
    public boolean accept(File f)
    {
      if (f.isDirectory())
        return true;
      String fn=f.getName();
      return isMusicXMLFilename(fn);
    }

    public String getDescription()
    {
      return "MusicXML scores (.xml)";
    }
  }

  /* ----- chooser filter for HTML files ----- */
  static class HTMLFileFilter extends FileFilter
  {
    public boolean accept(File f)
    {
      if (f.isDirectory())
        return true;
      String fn=f.getName();
      return isHTMLFilename(fn);
    }

    public String getDescription()
    {
      return "HTML files (.html)";
    }
  }

  /* ----- chooser filter for PDF files ----- */
  static class PDFFileFilter extends FileFilter
  {
    public boolean accept(File f)
    {
      if (f.isDirectory())
        return true;
      String fn=f.getName();
      return fn.matches(".*\\.[Pp][Dd][Ff]") ? true : false;
    }

    public String getDescription()
    {
      return "PDF files (.pdf)";
    }
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set parameters and options
Parameters:
  Input:  new values for parameters and options
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void setViewerWin(Viewer.Main newval)
  {
    viewerWin=newval;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MusicWin(String fn,PieceData p,int xl,int yl)
Purpose:     Initialize window
Parameters:
  Input:  String fn   - filename (for window title)
          PieceData p - data for music to display
          int xl,yl   - starting window location
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MusicWin(String fn,PieceData p,int xl,int yl) throws Exception
  {
    musicData=p;
    windowFileName=fn;

    origsize=new Dimension(-1,-1);
    WinBorder=new Dimension(-1,-1);
    if (xl==-1)
      {
        xl=10;
        yl=10;
        if (curWindow!=null)
          {
            xl=curWindow.getLocation().x+20;
            yl=curWindow.getLocation().y+20;
          }
      }

    cp=getContentPane();
    addCMMETitle(fn);
    windowIcon=new ImageIcon(new URL(BaseDataURL+"imgs/icon1.gif")).getImage();
    setIconImage(windowIcon);
    cp.setLayout(new BorderLayout());
    optSet=new OptionSet(this);
    initializeOptions();
    MusicGfx=new MusicFont((float)optSet.getVIEWSCALE());

    /* create menus */
    TopMenuBar=new JMenuBar();
    FileMenu=createFileMenu();
    EditMenu=createEditMenu();
    ViewMenu=createViewMenu();
    SectionsMenu=createSectionsMenu();
    TextMenu=createTextMenu();
    VersionsMenu=createVersionsMenu();
    AnalysisMenu=createAnalysisMenu();

    TopMenuBar.add(FileMenu);
    if (EditMenu!=null)
      TopMenuBar.add(EditMenu);
    TopMenuBar.add(ViewMenu);
    if (SectionsMenu!=null)
      TopMenuBar.add(SectionsMenu);
    if (TextMenu!=null)
      TopMenuBar.add(TextMenu);
    TopMenuBar.add(VersionsMenu);

/*    if (AnalysisMenu!=null)
      TopMenuBar.add(AnalysisMenu); analysis menu - temporarily removed! */

    setJMenuBar(TopMenuBar);

    /* create music view areas and ancillary frames */
    ViewScr=createMusicCanvas(musicData,MusicGfx,this,optSet);
    cp.add("Center",ViewScr);
    partsWin=createInitialPartsWin();
    scorePageWin=null;
    createGenPDFDialog();

    /* tool bar */
    MainToolBar=createMainToolBar();
    if (MainToolBar!=null)
      cp.add("North",MainToolBar);

    /* now the bottom part */
    BottomPanel=new JPanel();
    StatusPanel=createStatusPanel();

    /* add scroll bar */
    MusicScrollBarX=new JScrollBar(JScrollBar.HORIZONTAL,0,0,
                                   0,ViewScr.nummeasures);
    MusicScrollBarY=new JScrollBar(JScrollBar.VERTICAL,0,ViewScr.screensize.height,
                                   0,(int)Math.round(ViewScr.SCREEN_MINHEIGHT*optSet.getVIEWSCALE()));
    MusicScrollBarX.setFocusable(false);
    MusicScrollBarY.setFocusable(false);
    MusicScrollBarX.addAdjustmentListener(
      new AdjustmentListener()
        {
          public void adjustmentValueChanged(AdjustmentEvent e)
          {
            int newmeasure=MusicScrollBarX.getValue();
            setmeasurenum(newmeasure+1);
            ViewScr.movedisplay(newmeasure);
          }
        });
    MusicScrollBarY.addAdjustmentListener(
      new AdjustmentListener()
        {
          public void adjustmentValueChanged(AdjustmentEvent e)
          {
            int newy=MusicScrollBarY.getValue();
            ViewScr.newY(newy);
          }
        });

    BottomPanel.setLayout(new BorderLayout());
    BottomPanel.add("South",StatusPanel);
    BottomPanel.add("Center",MusicScrollBarX);
    cp.add("South",BottomPanel);
    cp.add("East",MusicScrollBarY);

    /* handle other window events */
    setKeyboardHandler();

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            closewin();
          }
        });
    addComponentListener(
      new ComponentAdapter()
        {
          public void componentResized(ComponentEvent event)
          {
            if (origsize.getHeight()>=0) /* has size been inited? */
              ViewScr.newsize(cp.getSize().width-WinBorder.width,
                              cp.getSize().height-WinBorder.height+
                              (commentaryPanelDisplayed ? 0 : commentaryPanel.getSize().height));
          }
        });
    addWindowFocusListener(this);

    /* show window */
    pack();
    setLocation(xl,yl);
    setSubframeLocations();
    ViewScr.requestFocusInWindow();
    setVisible(true);
    toFront();

    fileWindows.add(this);
    curWindow=this;

    origsize.setSize(cp.getSize());
    WinBorder.setSize(origsize.width-ViewScr.cursize().width,
                      origsize.height-ViewScr.cursize().height);
//System.out.println("origsize="+origsize+" bordersize="+WinBorder);
  }

  public MusicWin(String fn,PieceData p) throws Exception
  {
    this(fn,p,-1,-1);
  }

  /* use only for pseudo-static functions (to simulate static inheritance):
     openFile, getWinToReplace, openWin (etc) */
  public MusicWin()
  {
  }

/*------------------------------------------------------------------------
Method:  MusicWin getWinToReplace()
Purpose: If a new opening score window is to replace another one, return
         window to be replaced
Parameters:
  Input:  -
  Output: -
  Return: window to be replaced
------------------------------------------------------------------------*/

  protected MusicWin getWinToReplace()
  {
    return null;
  }

/*------------------------------------------------------------------------
Method:  MusicWin openWin(String fn,String path,PieceData musicData,int xl,int yl)
Purpose: Open new window after loading music data
Parameters:
  Input:  String fn           - filename
          String path         - canonical path to file
          PieceData musicData - music data from file
          int xl,yl           - coordinates for new window
  Output: -
  Return: new window
------------------------------------------------------------------------*/

  protected MusicWin openWin(String fn,String path,PieceData musicData,boolean convertedData,
                             int xl,int yl) throws Exception
  {
    return new MusicWin(fn,musicData,xl,yl);
  }

/*------------------------------------------------------------------------
Method:  void openFile(String fn)
Purpose: Open new music window for one file
Parameters:
  Input:  String fn - name of file
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void openFile(final String filename)
  {
    /* load file and create window in separate thread */
    final Gfx.SwingWorker OFThread=new Gfx.SwingWorker()
    {
    public Object construct()
    {
    /* ---------- real code ---------- */

    MusicWin   blankWin=getWinToReplace();
    File       f=new File(filename);
    URL        fURL=null;
    CMMEParser parser=null;
    PieceData  musicdat=null;
    String     windowFilename=null,
               path=null;
    boolean    convertedData=false;

    Gfx.MessageWin lw=new Gfx.MessageWin("Loading, please wait...",curWindow,true);

    /* load music data */
    try
      {
        if (isMIDIFilename(filename))
          {
            musicdat=MIDIReaderWriter.MIDtoCMME(f);
            windowFilename="Untitled score";
            convertedData=true;
          }
        else if (isMusicXMLFilename(filename))
          {
            musicdat=MusicXMLReader.MusicXMLtoCMME(new FileInputStream(f));
            windowFilename="Untitled score";
            convertedData=true;
          }
        else
          {
            fURL=f.toURI().toURL();
            int flen=fURL.openConnection().getContentLength();
            Util.ProgressInputStream musIn=
              new Util.ProgressInputStream(fURL.openStream(),
                                           lw.getProgressBar(),flen,0,75);
            parser=new CMMEParser(musIn,lw.getProgressBar());
            musicdat=parser.piece;
            windowFilename=f.getName();
            path=f.getCanonicalPath();
          }
//System.out.println(" GC: "+(long)(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
      }
    catch (Exception e)
      {
        String errorMsg="Error loading "+filename;
        if (e instanceof JDOMException)
          errorMsg+="\n\n"+e;
        try
          {
            if (Float.parseFloat(CMMEParser.getFileVersion(fURL))>MetaData.CMME_VERSION_FLOAT)
              errorMsg+="\n\n"+
"This file appears to have been created with a later version of the CMME software.\n"+
"Please update your software to the latest version to open this file.";
          }
        catch (Exception jde)
          {}
        JOptionPane.showMessageDialog(curWindow,errorMsg,"Error",JOptionPane.ERROR_MESSAGE);
        if (MetaData.CMME_OPT_TESTING)
          e.printStackTrace();
        blankWin=null;
      }

    /* open music window */
    if (musicdat!=null)
      try
        {
          int xl=10,yl=10;
          if (curWindow!=null)
            {
              xl=curWindow.getLocation().x+20;
              yl=curWindow.getLocation().y+20;
            }
          if (blankWin!=null)
            {
              xl=blankWin.getLocation().x;
              yl=blankWin.getLocation().y;
            }
          openWin(windowFilename,path,musicdat,convertedData,xl,yl);
        }
      catch (Exception e)
        {
          JOptionPane.showMessageDialog(curWindow,"Error creating score window","Error",JOptionPane.ERROR_MESSAGE);
          if (MetaData.CMME_OPT_TESTING)
            {
              System.err.println("Error creating score window: "+e);
              e.printStackTrace();
            }
          blankWin=null;
        }

    lw.dispose();
    if (blankWin!=null)
      blankWin.closewin();

    /* ---------- end real code ---------- */

    return null; /* not used */
    }
    }; /* end SwingWorker */

    OFThread.start();
  }

/*------------------------------------------------------------------------
Method:  String fileChooseAndOpen()
Purpose: Allow user to select file and open
Parameters:
  Input:  -
  Output: -
  Return: name of opened file
------------------------------------------------------------------------*/

  public String fileChooseAndOpen()
  {
    String fn=null;
    int fcval=getMusicWinFileChooser().showOpenDialog(this);
    if (fcval==JFileChooser.APPROVE_OPTION)
      {
        try
          {
            fn=getMusicWinFileChooser().getSelectedFile().getCanonicalPath();
            openFile(fn);
          }
        catch (Exception e)
          {
            System.err.println("Error loading "+getMusicWinFileChooser().getSelectedFile().getName());
          }
      }
    return fn;
  }

/*------------------------------------------------------------------------
Method:  boolean doNotOverwrite(File saveFile)
Purpose: Check whether a file to be saved already exists, and if so,
         display confirmation dialog for user to decide whether to overwrite
Parameters:
  Input:  -
  Output: -
  Return: true if user cancels save, false if file doesn't exist or user
          continues
------------------------------------------------------------------------*/

  public boolean doNotOverwrite(File saveFile)
  {
    if (saveFile.exists())
      {
        if (!confirmAction("Overwrite "+saveFile.getName()+"?","File already exists"))
          return true;
      }
    return false;
  }

  public boolean confirmAction(String queryText,String dialogTitle)
  {
    int confirmOption=JOptionPane.showConfirmDialog(this,
          queryText,dialogTitle,JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);

    if (confirmOption==JOptionPane.YES_OPTION)
      return true;
    return false;
  }

/*------------------------------------------------------------------------
Method:  void addCMMETitle(String fn)
Purpose: Add title to window
Parameters:
  Input:  String fn - name of file in window
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void addCMMETitle(String fn)
  {
    setTitle(fn+": CMME Viewer");
  }

/*------------------------------------------------------------------------
Method:  void initializeOptions()
Purpose: Initialize option set before creating view canvas
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void initializeOptions()
  {
    double newVS=(curWindow!=null) ? curWindow.optSet.getVIEWSCALE() :
                                     (double)DefaultViewScale/100;
    optSet.setVIEWSCALE(newVS);

    try
      {
        optSet.initFromGlobalConfig();
      }
    catch (Exception e)
      {
        handleRuntimeError(e);
      }
  }

/*------------------------------------------------------------------------
Method:  void setSubframeLocations()
Purpose: Set locations of frames dependent upon main window (after window
         has been packed)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void setSubframeLocations()
  {
    genPDFDialog.setLocationRelativeTo(this);
    setVariantDisplayOptionsFrameLocation();
  }

/*------------------------------------------------------------------------
Method:  void setEventEditorLocation()
Purpose: Position event editor relative to parent frame
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setVariantDisplayOptionsFrameLocation()
  {
    /* position relative to main frame */
    int eex=getLocation().x+getSize().width,
        eey=getLocation().y,
        eeWidth=variantDisplayOptionsFrame.getSize().width,
        eeHeight=variantDisplayOptionsFrame.getSize().height;
    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();

    if (eex+eeWidth>screenSize.width)
      eex=screenSize.width-eeWidth;

    variantDisplayOptionsFrame.setLocation(eex,eey);
  }

/*------------------------------------------------------------------------
Method:  PartsWin createInitialPartsWin()
Purpose: Initialize unscored parts window when opening window
Parameters:
  Input:  -
  Output: -
  Return: new parts window
------------------------------------------------------------------------*/

  protected PartsWin createInitialPartsWin()
  {
    return null;
//    return new PartsWin(musicData,MusicGfx,this);
  }

/*------------------------------------------------------------------------
Method:  JMenu createFileMenu()
Purpose: Create File menu for window
Parameters:
  Input:  -
  Output: -
  Return: menu
------------------------------------------------------------------------*/

  protected JMenuItem FileMenuAbout,
                      FileMenuClose,
                      FileMenuGeneratePDF;

  protected JMenu     FileMenuExport;
  protected JMenuItem FMExportMIDI,
                      FMExportXML;

  protected JMenu createFileMenu()
  {
    /* create menu and items */
    JMenu FM=new JMenu("File");

    FileMenuAbout=new JMenuItem("About this edition...");
    FileMenuAbout.setMnemonic(KeyEvent.VK_A);

    FileMenuClose=new JMenuItem("Close window");
    FileMenuClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,ActionEvent.CTRL_MASK));
    FileMenuClose.setMnemonic(KeyEvent.VK_C);

	FileMenuExport=new JMenu("Export");
    FileMenuExport.setMnemonic(KeyEvent.VK_E);
    FMExportMIDI=new JMenuItem("MIDI");
    FMExportXML=new JMenuItem("MusicXML");
    FileMenuExport.add(FMExportMIDI);
    FileMenuExport.add(FMExportXML);

    FileMenuGeneratePDF=new JMenuItem("Generate PDF...");
    FileMenuGeneratePDF.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,ActionEvent.CTRL_MASK));
    FileMenuGeneratePDF.setMnemonic(KeyEvent.VK_P);

    FM.add(FileMenuAbout);
    FM.add(FileMenuClose);
    if (viewerWin==null || !viewerWin.inApplet)
    /* no export/PDF-generation in applet */
      {
        FM.add(FileMenuExport);
        FM.add(FileMenuGeneratePDF);
      }
    /* applet PDF test - figure out security settings to avoid access control exception
    else if (viewerWin.inApplet)
      FM.add(FileMenuGeneratePDF);*/

    /* handle menu actions */
    FileMenuAbout.addActionListener(this);
    FileMenuClose.addActionListener(this);
    FMExportMIDI.addActionListener(this);
    FMExportXML.addActionListener(this);
    FileMenuGeneratePDF.addActionListener(this);

    return FM;
  }

/*------------------------------------------------------------------------
Method:  JMenu create*Menu()
Purpose: Create menus for window
Parameters:
  Input:  -
  Output: -
  Return: menu
------------------------------------------------------------------------*/

  protected JMenu createEditMenu()
  {
    return null;
  }

  protected JMenu createSectionsMenu()
  {
    return null;
  }

  protected JMenu createTextMenu()
  {
    return null;
  }

  /* Versions Menu */

  protected JMenuItem         VersionsMenuGeneralInfo,
                              VersionsMenuNewNotesWindow;
  protected JCheckBoxMenuItem VersionsMenuDisplayVariantOptions;
  protected JMenuItem         VersionsMenuSourceAnalysis;

  protected VariantDisplayOptionsFrame variantDisplayOptionsFrame;

  protected JMenu     VersionsMenuDisplayMenu,
                      VersionsMenuOptionsMenu;
  protected JRadioButtonMenuItem VMDMVersions[];
  protected JRadioButtonMenuItem VMOMMarkAllVariants,
                                 VMOMMarkSubstantiveVariants,
                                 VMOMMarkNoVariants,
                                 VMOMMarkSelectedVariants;
  protected JCheckBoxMenuItem    VMOMCustomVariants[];

  protected JMenu createVersionsMenu()
  {
    /* create menu and items */
    JMenu VM=new JMenu("Versions");

    VersionsMenuGeneralInfo=new JMenuItem("Variant Version Information...");
    VersionsMenuGeneralInfo.setMnemonic(KeyEvent.VK_I);

    VersionsMenuDisplayMenu=new JMenu("Display version");
    VersionsMenuOptionsMenu=new JMenu("Variant marking options");
    if (musicData.getVariantVersions().size()==0)
      {
        VersionsMenuGeneralInfo.setEnabled(false);
        VersionsMenuDisplayMenu.setEnabled(false);
        VersionsMenuOptionsMenu.setEnabled(false);
        VMDMVersions=new JRadioButtonMenuItem[0];
      }
    else
      {
        VMDMVersions=new JRadioButtonMenuItem[musicData.getVariantVersions().size()];
        ButtonGroup VMDMGroup=new ButtonGroup();

/*        VMDMVersions[0]=new JRadioButtonMenuItem("Default");
        VMDMGroup.add(VMDMVersions[0]);
        VersionsMenuDisplayMenu.add(VMDMVersions[0]);
        VMDMVersions[0].setSelected(true);
        VMDMVersions[0].addActionListener(this);*/

        int vi=0;
        for (VariantVersionData vvd : musicData.getVariantVersions())
          {
            VMDMVersions[vi]=new JRadioButtonMenuItem(vvd.getID());
            VMDMGroup.add(VMDMVersions[vi]);
            VersionsMenuDisplayMenu.add(VMDMVersions[vi]);
            VMDMVersions[vi].addActionListener(this);
            vi++;
          }
        VMDMVersions[0].setSelected(true);
      }

    ButtonGroup VMOMMarkGroup=new ButtonGroup();
    VMOMMarkAllVariants=new JRadioButtonMenuItem("Mark all variants on score");
    VMOMMarkAllVariants.setSelected(optSet.getMarkVariants()==optSet.OPT_VAR_ALL);
    VMOMMarkGroup.add(VMOMMarkAllVariants);
    VersionsMenuOptionsMenu.add(VMOMMarkAllVariants);
    VMOMMarkSubstantiveVariants=new JRadioButtonMenuItem("Mark substantive variants on score");
    VMOMMarkSubstantiveVariants.setSelected(optSet.getMarkVariants()==optSet.OPT_VAR_SUBSTANTIVE);
    VMOMMarkGroup.add(VMOMMarkSubstantiveVariants);
    VersionsMenuOptionsMenu.add(VMOMMarkSubstantiveVariants);
    VMOMMarkNoVariants=new JRadioButtonMenuItem("Mark no variants on score");
    VMOMMarkNoVariants.setSelected(optSet.getMarkVariants()==optSet.OPT_VAR_NONE);
    VMOMMarkGroup.add(VMOMMarkNoVariants);
    VersionsMenuOptionsMenu.add(VMOMMarkNoVariants);
    VMOMMarkSelectedVariants=new JRadioButtonMenuItem("Mark selected variants on score:");
    VMOMMarkSelectedVariants.setSelected(optSet.getMarkVariants()==optSet.OPT_VAR_CUSTOM);
    VMOMMarkGroup.add(VMOMMarkSelectedVariants);
    VersionsMenuOptionsMenu.add(VMOMMarkSelectedVariants);

    VMOMCustomVariants=new JCheckBoxMenuItem[VariantReading.typeNames.length-1];
    for (int vi=1; vi<VariantReading.typeNames.length; vi++)
      {
        JCheckBoxMenuItem curCB=new JCheckBoxMenuItem(VariantReading.typeNames[vi]);
        curCB.setSelected(optSet.markCustomVariant(1<<(vi-1)));
        curCB.setEnabled(optSet.getMarkVariants()==optSet.OPT_VAR_CUSTOM);
        curCB.addItemListener(this);
        VersionsMenuOptionsMenu.add(curCB);
        VMOMCustomVariants[vi-1]=curCB;
      }

    VersionsMenuNewNotesWindow=new JMenuItem("New critical notes list...");
    VersionsMenuNewNotesWindow.setMnemonic(KeyEvent.VK_N);

    VersionsMenuSourceAnalysis=new JMenuItem("Source analysis...");
    VersionsMenuSourceAnalysis.setMnemonic(KeyEvent.VK_A);

    VersionsMenuDisplayVariantOptions=new JCheckBoxMenuItem("Display variant marking options");
    VersionsMenuDisplayVariantOptions.setSelected(false);
    VersionsMenuDisplayVariantOptions.setMnemonic(KeyEvent.VK_O);

    variantDisplayOptionsFrame=new VariantDisplayOptionsFrame(this);

//    VM.add(VersionsMenuDisplayMenu);
//    VM.add(VersionsMenuOptionsMenu);
//    VM.add(VersionsMenuGeneralInfo);
    VM.add(VersionsMenuNewNotesWindow);
//    VM.add(VersionsMenuSourceAnalysis);
    VM.add(VersionsMenuDisplayVariantOptions);

    /* handle menu actions */
    VersionsMenuGeneralInfo.addActionListener(this);
    VersionsMenuNewNotesWindow.addActionListener(this);
    VersionsMenuDisplayVariantOptions.addItemListener(this);
    VersionsMenuSourceAnalysis.addActionListener(this);
    variantDisplayOptionsFrame.registerListeners();
    VMOMMarkAllVariants.addActionListener(this);
    VMOMMarkSubstantiveVariants.addActionListener(this);
    VMOMMarkNoVariants.addActionListener(this);
    VMOMMarkSelectedVariants.addActionListener(this);

    return VM;
  }

  /* View Menu */

  protected JMenu                  ViewMenuViewSize,
                                   ViewMenuBarlineStyle,
                                   ViewMenuNoteShapeStyle,
                                   ViewMenuTexting,
                                   ViewMenuPitchSystem;
  protected JMenuItem              VMVSZoomOut,
                                   VMVSZoomIn;
  protected JRadioButtonMenuItem[] VMVSnumItems;
  protected int[]                  VMVSDefaultNums={ 200,175,150,125,100,75,50 },
                                   VMVSnums;
  protected int                    curVMVSnum,
                                   curViewSize;
  protected JRadioButtonMenuItem[] VMBSItems,
                                   VMNSItems;
  protected JRadioButtonMenuItem   VMTOrigText,
                                   VMTModText,
                                   VMTBothText,
                                   VMTNoText;
  protected JCheckBoxMenuItem      ViewMenuUsemodernclefs,
                                   ViewMenuDisplayEditorialAccidentals,
                                   ViewMenuModernAccidentalSystem,
                                   ViewMenuDisplayallnewlineclefs,
                                   ViewMenuDisplayligbrackets,
                                   ViewMenuEdCommentary;
  protected JMenuItem              ViewMenuViewParts;
  protected JMenu                  ViewMenuPrintPreviews;
  protected JMenuItem              VMPPParts,
                                   VMPPScore;

  protected JMenu createViewMenu()
  {
    JMenu VM=new JMenu("View");
    ViewMenuViewSize=new JMenu("View Size");
    ViewMenuBarlineStyle=new JMenu("Barline Style");
    ViewMenuNoteShapeStyle=new JMenu("Note shapes / reduction");
    ViewMenuTexting=new JMenu("Texting");
    ViewMenuPitchSystem=new JMenu("Pitch system");
    ViewMenuUsemodernclefs=new JCheckBoxMenuItem("Modern clefs");
    ViewMenuUsemodernclefs.setSelected(true);
    ViewMenuDisplayallnewlineclefs=new JCheckBoxMenuItem("Display all newline clefs");
    ViewMenuDisplayligbrackets=new JCheckBoxMenuItem("Display ligature brackets");
    ViewMenuEdCommentary=new JCheckBoxMenuItem("Mark editorial commentary");
    ViewMenuDisplayligbrackets.setSelected(true);
    ViewMenuEdCommentary.setSelected(true);
    ViewMenuViewParts=new JMenuItem("View parts window");
    ViewMenuViewParts.setMnemonic(KeyEvent.VK_P);
    ViewMenuPrintPreviews=new JMenu("Print Previews");

    /* view size submenu */
    VMVSZoomOut=new JMenuItem("Zoom out");
    VMVSZoomIn=new JMenuItem("Zoom in");
    VMVSZoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,ActionEvent.CTRL_MASK));
    VMVSZoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,ActionEvent.CTRL_MASK));
    ViewMenuViewSize.add(VMVSZoomOut);
    ViewMenuViewSize.add(VMVSZoomIn);
    ViewMenuViewSize.addSeparator();
    ButtonGroup VMVSgroup=new ButtonGroup();

    /* insert DefaultViewScale into VMVSnums array */
    curVMVSnum=-1;
    for (int i=0; i<VMVSDefaultNums.length; i++)
      if (VMVSDefaultNums[i]==DefaultViewScale)
        {
          curVMVSnum=i;
          break;
        }
    if (curVMVSnum!=-1)
      VMVSnums=VMVSDefaultNums;
    else
      {
        VMVSnums=new int[VMVSDefaultNums.length+1];
        int vi=0;
        for (int i=0; i<VMVSDefaultNums.length; i++)
          {
            if (vi==i && VMVSDefaultNums[i]<DefaultViewScale)
              {
                VMVSnums[vi]=DefaultViewScale;
                curVMVSnum=vi;
                vi++;
              }
            VMVSnums[vi++]=VMVSDefaultNums[i];
          }
        if (vi<=VMVSDefaultNums.length) /* was DefaultViewScale not inserted? */
          {
            VMVSnums[vi]=DefaultViewScale;
            curVMVSnum=vi;
          }
      }
    curViewSize=(curWindow!=null) ? curWindow.curViewSize : DefaultViewScale;

    VMVSnumItems=new JRadioButtonMenuItem[VMVSnums.length];
    for (int i=0; i<VMVSnums.length; i++)
      {
        VMVSnumItems[i]=new JRadioButtonMenuItem(VMVSnums[i]+"%");
        VMVSnumItems[i].setActionCommand(Integer.toString(i));
        VMVSnumItems[i].addActionListener(VSListener);
        VMVSgroup.add(VMVSnumItems[i]);
        ViewMenuViewSize.add(VMVSnumItems[i]);
        if (VMVSnums[i]==curViewSize)
          VMVSnumItems[curVMVSnum=i].setSelected(true);
      }

    /* barline style submenu */
    ButtonGroup VMBSgroup=new ButtonGroup();
    VMBSItems=new JRadioButtonMenuItem[OptionSet.BarlineStrings.length];
    for (int i=0; i<VMBSItems.length; i++)
      {
        VMBSItems[i]=new JRadioButtonMenuItem(OptionSet.BarlineStrings[i]);
        VMBSItems[i].setActionCommand(Integer.toString(i));
        VMBSItems[i].addActionListener(VMBSListener);
        VMBSgroup.add(VMBSItems[i]);
        ViewMenuBarlineStyle.add(VMBSItems[i]);
      }
    VMBSItems[optSet.get_barline_type()].setSelected(true);

    /* note shape / reduction submenu */
    ButtonGroup VMNSgroup=new ButtonGroup();
    VMNSItems=new JRadioButtonMenuItem[OptionSet.NoteShapeStrings.length];
    for (int i=0; i<VMNSItems.length; i++)
      {
        VMNSItems[i]=new JRadioButtonMenuItem(OptionSet.NoteShapeStrings[i]);
        VMNSItems[i].setActionCommand(Integer.toString(i));
        VMNSItems[i].addActionListener(VMNSListener);
        VMNSgroup.add(VMNSItems[i]);
        ViewMenuNoteShapeStyle.add(VMNSItems[i]);
      }
    VMNSItems[optSet.get_noteShapeType()].setSelected(true);

    /* texting submenu */
    ButtonGroup VMTgroup=new ButtonGroup();
    VMTOrigText=new JRadioButtonMenuItem("Original Text");
    VMTOrigText.addActionListener(this);
    VMTgroup.add(VMTOrigText);
    ViewMenuTexting.add(VMTOrigText);
    VMTModText=new JRadioButtonMenuItem("Modern Text");
    VMTModText.addActionListener(this);
    VMTgroup.add(VMTModText);
    ViewMenuTexting.add(VMTModText);
    VMTBothText=new JRadioButtonMenuItem("Original and Modern Texts");
    VMTBothText.addActionListener(this);
    VMTgroup.add(VMTBothText);
    ViewMenuTexting.add(VMTBothText);
    VMTNoText=new JRadioButtonMenuItem("No Text");
    VMTNoText.addActionListener(this);
    VMTgroup.add(VMTNoText);
    ViewMenuTexting.add(VMTNoText);
    VMTOrigText.setSelected(true);

    /* pitch system submenu */
    ViewMenuDisplayEditorialAccidentals=new JCheckBoxMenuItem("Display editorial accidentals");
    ViewMenuDisplayEditorialAccidentals.setSelected(optSet.get_modacc_type()!=OptionSet.OPT_MODACC_NONE);
    ViewMenuModernAccidentalSystem=new JCheckBoxMenuItem("Modern accidentals/signatures");
    ViewMenuModernAccidentalSystem.setSelected(optSet.getUseModernAccidentalSystem());
    ViewMenuPitchSystem.add(ViewMenuDisplayEditorialAccidentals);
    ViewMenuPitchSystem.add(ViewMenuModernAccidentalSystem);
    ViewMenuDisplayEditorialAccidentals.addItemListener(this);
    ViewMenuModernAccidentalSystem.addItemListener(this);

    /* print previews submenu */
    VMPPParts=new JMenuItem("Parts...");
    VMPPScore=new JMenuItem("Score...");
    ViewMenuPrintPreviews.add(VMPPParts);
    ViewMenuPrintPreviews.add(VMPPScore);

    /* add menus and items */
    VM.add(ViewMenuViewSize);
    VM.add(ViewMenuBarlineStyle);
    VM.add(ViewMenuNoteShapeStyle);
    VM.add(ViewMenuTexting);
    VM.add(ViewMenuPitchSystem);
    VM.add(ViewMenuUsemodernclefs);
    VM.add(ViewMenuDisplayallnewlineclefs);
    VM.add(ViewMenuDisplayligbrackets);
    VM.add(ViewMenuEdCommentary);
    VM.addSeparator();
    VM.add(ViewMenuViewParts);
    if (viewerWin==null || !viewerWin.inApplet)
      VM.add(ViewMenuPrintPreviews);

    /* add listeners for menu items */
    VMVSZoomOut.addActionListener(this);
    VMVSZoomIn.addActionListener(this);
    ViewMenuUsemodernclefs.addItemListener(this);
    ViewMenuDisplayallnewlineclefs.addItemListener(this);
    ViewMenuDisplayligbrackets.addItemListener(this);
    ViewMenuEdCommentary.addItemListener(this);
    ViewMenuViewParts.addActionListener(this);
    VMPPParts.addActionListener(this);
    VMPPScore.addActionListener(this);

    return VM;
  }

  /* Analysis Menu */

  JCheckBoxMenuItem AnalysisMenuMarkDissonances,
                    AnalysisMenuMarkDirectedProgressions;

  protected JMenu createAnalysisMenu()
  {
    JMenu AM=new JMenu("Analysis");

    AnalysisMenuMarkDissonances=new JCheckBoxMenuItem("Mark dissonances");
    AnalysisMenuMarkDirectedProgressions=new JCheckBoxMenuItem("Mark directed progressions");

    AM.add(AnalysisMenuMarkDissonances);
    AM.add(AnalysisMenuMarkDirectedProgressions);

    AnalysisMenuMarkDissonances.addItemListener(this);
    AnalysisMenuMarkDirectedProgressions.addItemListener(this);

    return AM;
  }

/*------------------------------------------------------------------------
Method:  ZoomControl createTBZoomControl()
Purpose: Create zoom control for main toolbar
Parameters:
  Input:  -
  Output: -
  Return: zoom control (also stored in MTZoomControl)
------------------------------------------------------------------------*/

  protected ZoomControl createTBZoomControl()
  {
    return MTZoomControl=new ZoomControl(curViewSize,this);
  }

/*------------------------------------------------------------------------
Method:  JToolBar createMainToolBar()
Purpose: Create main tool bar beneath menu
Parameters:
  Input:  -
  Output: -
  Return: tool bar
------------------------------------------------------------------------*/

  JButton MTNoteShapesModButton,MTNoteShapesOldButton,
          MTClefsModButton,MTClefsOldButton,
          MTEdAccidentalsButton,
          MTPitchModButton,MTPitchOldButton,
          MTTextingModButton,MTTextingOldButton;
  JLabel  MTVersionLabel=null;

  protected JButton PlayButton;

  protected JToolBar createMainToolBar() throws Exception
  {
    JToolBar mtb=new JToolBar();
    mtb.setFloatable(false);
    mtb.setFocusable(false);
    mtb.setRollover(true);
    mtb.setLayout(new GridBagLayout());
    mtb.setAlignmentY(java.awt.Component.LEFT_ALIGNMENT);
    mtb.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    GridBagConstraints tbc=new GridBagConstraints();
    tbc.anchor=GridBagConstraints.WEST;
    tbc.weightx=0;

    MTNoteShapesOldButton=createMainTBButton(NoteShapesOldIcon_light,"Original noteshapes");
    MTNoteShapesModButton=createMainTBButton(NoteShapesModIcon_dark,"Modern noteshapes");
    MTClefsOldButton=createMainTBButton(ClefsOldIcon_light,"Original clefs");
    MTClefsModButton=createMainTBButton(ClefsModIcon_dark,"Modern clefs");
    MTEdAccidentalsButton=createMainTBButton(EdAccidentalsIcon_dark,"Show/hide editorial accidentals");
    MTPitchOldButton=createMainTBButton(PitchOldIcon_light,"Original accidental system");
    MTPitchModButton=createMainTBButton(PitchModIcon_dark,"Modern accidental/key system");
    MTTextingOldButton=createMainTBButton(TextingOldIcon_light,"Original texting");
    MTTextingModButton=createMainTBButton(TextingModIcon_dark,"Modern texting");

//    setUseModernNoteShapes(optSet.useModernNoteShapes());
    setTexting(optSet.get_displayOrigText(),optSet.get_displayModText());
    setUseModernClefs(optSet.get_usemodernclefs());
    setUseModernPitch(optSet.getUseModernAccidentalSystem());
    setEdAccidentals(optSet.get_modacc_type());
    updateTBNSButtons();

    PlayButton=new JButton("PLAY");
    PlayButton.addActionListener(this);

    tbc.gridx=-1;

    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTNoteShapesOldButton,tbc);
    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTNoteShapesModButton,tbc);

    tbc.gridx++; mtb.addSeparator();

    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTClefsOldButton,tbc);
    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTClefsModButton,tbc);

    tbc.gridx++; mtb.addSeparator();

    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTEdAccidentalsButton,tbc);

    tbc.gridx++; mtb.addSeparator();

    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTPitchOldButton,tbc);
    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTPitchModButton,tbc);

    tbc.gridx++; mtb.addSeparator();

    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTTextingOldButton,tbc);
    tbc.gridx++; tbc.gridy=0; tbc.weightx=1; mtb.add(MTTextingModButton,tbc);

    tbc.gridx++; mtb.addSeparator();

    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(PlayButton,tbc);
    tbc.gridx++; mtb.addSeparator();

    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTVersionLabel=createTBVersionDisplay(),tbc);
    tbc.gridx++; tbc.gridy=0; tbc.weightx=0; mtb.add(MTZoomControl=createTBZoomControl(),tbc);

    return mtb;
  }

/*------------------------------------------------------------------------
Method:  JButton createMainTBButton(ImageIcon img,String tipText)
Purpose: Create one button for main tool bar
Parameters:
  Input:  ImageIcon img  - image for button
          String tipText - tool tip text
  Output: -
  Return: button
------------------------------------------------------------------------*/

  JButton createMainTBButton(ImageIcon img,String tipText)
  {
    JButton b=new JButton();

    b.setMargin(new Insets(1,1,1,1));
    b.setIcon(img);
    b.setToolTipText(tipText);
    b.setFocusable(false);
    b.addActionListener(this);

    return b;
  }

/*------------------------------------------------------------------------
Method:  JLabel createTBVersionDisplay()
Purpose: Create version display area for main tool bar
Parameters:
  Input:  -
  Output: -
  Return: label
------------------------------------------------------------------------*/

  static final String VERSION_LABEL_TEXT="Version: ",
                      VERSION_DEFAULT_TEXT="Default";

  JLabel createTBVersionDisplay()
  {
    String versionText="";

    if (musicData.getVariantVersions().size()>0)
      {
        versionText=VERSION_LABEL_TEXT+musicData.getVariantVersion(0).getID()+" ";
      }
    JLabel newLabel=new JLabel(versionText);
    newLabel.setForeground(Color.GRAY);

    return newLabel;
  }

/*------------------------------------------------------------------------
Method:  void updateTBVersionDisplay(VariantVersionData v)
Purpose: Update version display area in main tool bar
Parameters:
  Input:  VariantVersionData v - new version for display
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void updateTBVersionDisplay(VariantVersionData v)
  {
    if (MTVersionLabel==null)
      return;

    String newText=v==null ? VERSION_DEFAULT_TEXT : v.getID();
    MTVersionLabel.setText(VERSION_LABEL_TEXT+newText+" ");
    pack();
  }

/*------------------------------------------------------------------------
Method:  ViewCanvas createMusicCanvas(PieceData p,MusicFont mf,MusicWin mw,OptionSet os)
Purpose: Create music viewing area
Parameters:
  Input:  PieceData p,MusicFont mf,MusicWin mw,OptionSet os - constructor params
  Output: -
  Return: viewing canvas
------------------------------------------------------------------------*/

  protected ViewCanvas createMusicCanvas(PieceData p,MusicFont mf,MusicWin mw,OptionSet os)
  {
    return new ViewCanvas(p,mf,mw,os);
  }

/*------------------------------------------------------------------------
Method:  JPanel createStatusPanel()
Purpose: Create information panel under viewscreen/scrollbar
Parameters:
  Input:  -
  Output: -
  Return: panel
------------------------------------------------------------------------*/

  protected static final String commentaryLabel="<font color=\"red\"><i>Commentary: </i></font>",
                                commentaryDefault="No commentary selected";

  protected JEditorPane commentaryTextArea=null;
  protected JScrollPane commentaryScrollPane;
  protected JPanel      commentaryPanel;
  protected boolean     commentaryPanelDisplayed;

  protected JPanel createStatusPanel()
  {
    JPanel measurePanel=new JPanel();
    StatusMeasureLabel=new JLabel("Measure:");
    StatusMeasureNum=new JLabel("1/"+ViewScr.nummeasures);
    measurePanel.add("West",StatusMeasureLabel);
    measurePanel.add("East",StatusMeasureNum);

    commentaryTextArea=new JEditorPane("text/html",commentaryLabel+commentaryDefault);
    commentaryScrollPane=new JScrollPane(commentaryTextArea);
    commentaryTextArea.setEditable(false);
    commentaryScrollPane.setPreferredSize(new Dimension(100,75));
    commentaryScrollPane.setMinimumSize(new Dimension(20,20));
    commentaryPanel=new JPanel();
    commentaryPanel.setLayout(new BoxLayout(commentaryPanel,BoxLayout.X_AXIS));
    commentaryPanel.add(commentaryScrollPane,0);

    commentaryPanelDisplayed=true;
    JPanel sp=new JPanel();
    layoutStatusPanel(sp,measurePanel,commentaryPanel);

    return sp;
  }

  void layoutStatusPanel(JPanel sp,JPanel measurePanel,JPanel commentaryPanel)
  {
    sp.removeAll();
    sp.setLayout(new BoxLayout(sp,BoxLayout.Y_AXIS));
    sp.add(measurePanel);
    if (commentaryPanelDisplayed)
      sp.add(commentaryPanel);
  }

  protected void showCommentaryPanel(boolean show)
  {
    if (show==commentaryPanelDisplayed)
      return;

    commentaryPanelDisplayed=show;
    if (show)
      StatusPanel.add(commentaryPanel);
    else
      StatusPanel.remove(commentaryPanel);
    StatusPanel.validate();
    pack();
  }

  public void updateCommentaryArea(int vnum,int mnum,String edCommentary)
  {
    if (vnum<0)
      {
        /* no commentary selected */
        commentaryTextArea.setText(commentaryLabel+commentaryDefault);
        return;
      }

    vnum++;
    mnum++;
    String commentaryLoc="<font color=\"blue\">Voice "+vnum+", m. "+mnum+": </font>";
    commentaryTextArea.setText(commentaryLabel+commentaryLoc+edCommentary);
  }

/*------------------------------------------------------------------------
Method:  void [show|create]GenPDFDialog()
Purpose: Bring up dialog with PDF-generation options
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void showGenPDFDialog()
  {
    genPDFDialog.setLocationRelativeTo(this);
    genPDFDialog.setVisible(true);
//    new PDFCreator(partsWin.getRenderLists(),"test.pdf");
  }

  JRadioButton PDFScoreButton,
               PDFPartsButton;
  JButton      genPDFOKButton,
               genPDFCancelButton;

  void createGenPDFDialog()
  {
    genPDFDialog=new JDialog(this,"Generate PDF",true);

    JPanel optionsPane=new JPanel();
    optionsPane.setLayout(new BoxLayout(optionsPane,BoxLayout.Y_AXIS));
    optionsPane.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Options"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    PDFScoreButton=new JRadioButton("Score",true);
    PDFPartsButton=new JRadioButton("Parts");
    ButtonGroup ScoreOrPartsGroup=new ButtonGroup();
    ScoreOrPartsGroup.add(PDFScoreButton);
    ScoreOrPartsGroup.add(PDFPartsButton);
    optionsPane.add(PDFScoreButton);
    optionsPane.add(PDFPartsButton);

    /* action buttons */
    genPDFOKButton=new JButton("Generate...");
    genPDFCancelButton=new JButton("Cancel");
    Box buttonPane=Box.createHorizontalBox();
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(genPDFOKButton);
    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(genPDFCancelButton);
    buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    /* lay out frame */
    Container gpcp=genPDFDialog.getContentPane();
    gpcp.add(optionsPane,BorderLayout.CENTER);
    gpcp.add(buttonPane,BorderLayout.SOUTH);

    /* register listeners */
    genPDFOKButton.addActionListener(this);
    genPDFCancelButton.addActionListener(this);

    genPDFDialog.pack();
  }

/*------------------------------------------------------------------------
Method:  boolean fileExportAs(int fileType)
Purpose: Choose file name and export score in external format
Parameters:
  Input:  -
  Output: -
  Return: whether file saved successfully
------------------------------------------------------------------------*/

  String checkAndAddExtension(String fn,int fileType)
  {
    switch (fileType)
      {
        case FILETYPE_MIDI:
          if (!isMIDIFilename(fn))
            fn+=FILENAME_EXTENSION_MIDI;
          break;
        case FILETYPE_XML:
          if (!isMusicXMLFilename(fn))
            fn+=FILENAME_EXTENSION_XML;
      }
    return fn;
  }

  protected boolean fileExportAs(int fileType)
  {
    JFileChooser fc=getExportFileChooser();
    FileFilter   ff=null;
    switch (fileType)
      {
        case FILETYPE_MIDI:
          ff=MIDIFFilter;
          break;
        case FILETYPE_XML:
          ff=XMLFFilter;
          break;
      }
    fc.setFileFilter(ff);
    int fcval=fc.showSaveDialog(this);

    if (fcval==JFileChooser.APPROVE_OPTION)
      try
        {
          File saveFile=exportFileChooser.getSelectedFile();

          /* make sure extension is valid */
          String fn=saveFile.getCanonicalPath(),
                 fullFN=checkAndAddExtension(fn,fileType);
          if (!fn.equals(fullFN))
            {
              fn=fullFN;
              saveFile=new File(fn);
            }

          if (doNotOverwrite(saveFile))
            return false;

          switch (fileType)
            {
              case FILETYPE_MIDI:
                new MIDIPlayer(this,ViewScr.getMusicData(),ViewScr.getRenderedMusic()).exportMIDIFile(fn);
                break;
              case FILETYPE_XML:
                writeMusicXMLFile(saveFile);
                break;
              default:
                throw new Exception("Unsupported file type");
            }
        }
      catch (Exception e)
        {
          displayErrorMessage("Error saving file \""+exportFileChooser.getSelectedFile().getName()+"\":\n"+e,"File not saved");

//          System.err.println("Error saving "+musicWinFileChooser.getSelectedFile().getName());
//          e.printStackTrace();
        }

    return true;
  }

  synchronized void writeMusicXMLFile(File f) throws Exception
  {
    ScorePageRenderer renderedPages=new ScorePageRenderer(
      musicData,OptionSet.makeDEFAULT_FULL_MODERN(this),
      new Dimension(ScorePagePreviewWin.STAFFXSIZE,ScorePagePreviewWin.DRAWINGSPACEY),
      ScorePagePreviewWin.STAFFSCALE,ScorePagePreviewWin.CANVASYSCALE);

    new MusicXMLGenerator(renderedPages).outputPieceData(new FileOutputStream(f));
  }

/*------------------------------------------------------------------------
Method:  void generatePDF()
Purpose: Create PDF with user-chosen options
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void generatePDF()
  {
    String  pdfName=null;
    boolean genParts=PDFPartsButton.isSelected();

    ScorePageRenderer renderedPages=null;
    ArrayList[]       renderedParts=null;

    if (genParts)
      {
        PartsWin PDFPartsWin=new PartsWin(ViewScr.getMusicData(),MusicGfx,this,false);//true);
        renderedParts=PDFPartsWin.getRenderLists();
        PDFPartsWin.closewin();
        pdfName=windowFileName.replaceFirst("\\.cmme\\.xml","-parts.pdf");
      }
    else
      {
        renderedPages=new ScorePageRenderer(
          musicData,optSet,
          new Dimension(ScorePagePreviewWin.STAFFXSIZE,ScorePagePreviewWin.DRAWINGSPACEY),
          ScorePagePreviewWin.STAFFSCALE,ScorePagePreviewWin.CANVASYSCALE);
        pdfName=windowFileName.replaceFirst("cmme\\.xml","pdf");
      }

    PDFFileChooser=getPDFFileChooser();
    File initFile=new File(PDFFileChooser.getCurrentDirectory(),pdfName);
    PDFFileChooser.setSelectedFile(initFile);
    int FCval=PDFFileChooser.showSaveDialog(this);
    if (FCval==JFileChooser.APPROVE_OPTION)
      try
        {
          File saveFile=PDFFileChooser.getSelectedFile();
          if (doNotOverwrite(saveFile))
            return;

          /* save */
          String fn=saveFile.getCanonicalPath();
          if (!fn.matches(".*\\.pdf"))
            {
              fn=fn.concat(".pdf");
              saveFile=new File(fn);
            }

          if (genParts)
            new PDFCreator(renderedParts).createPDF(saveFile);
          else
            new PDFCreator(renderedPages).createPDF(saveFile);
        }
      catch (Exception e)
        {
          System.err.println("Error saving "+PDFFileChooser.getSelectedFile().getName());
          handleRuntimeError(e);
        }

    genPDFDialog.setVisible(false);
  }

/*------------------------------------------------------------------------
Method:  void setViewSize|ViewSizeFieldAction|zoomIn|zoomOut()
Purpose: Functions to control view scale through GUI
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void ViewSizeFieldAction()
  {
    setViewSize(MTZoomControl.viewSizeFieldAction());
  }

  protected void zoomOut()
  {
    VMVSnumItems[MTZoomControl.getSizeIndex(MTZoomControl.zoomOut())].doClick();
  }

  protected void zoomIn()
  {
    VMVSnumItems[MTZoomControl.getSizeIndex(MTZoomControl.zoomIn())].doClick();
  }

  protected void setViewSize(int ns)
  {
    curViewSize=ns;
    if (MTZoomControl!=null)
      MTZoomControl.setViewSize(curViewSize);
    optSet.setVIEWSCALE((double)curViewSize/100);
    ViewScr.newViewScale();
  }

  int getGreaterVSNum(int vs)
  {
    for (int i=VMVSnums.length-1; i>=0; i--)
      if (VMVSnums[i]>vs)
        return i;
    return -1;
  }

  int getLesserVSNum(int vs)
  {
    for (int i=0; i<VMVSnums.length; i++)
      if (VMVSnums[i]<vs)
        return i;
    return -1;
  }

/*------------------------------------------------------------------------
Method:  void setKeyboardHandler()
Purpose: Add keyboard handler
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void setKeyboardHandler()
  {
  }

/*------------------------------------------------------------------------
Method:     void actionPerformed(ActionEvent event)
Implements: ActionListener.actionPerformed
Purpose:    Check for action types in menu and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void actionPerformed(ActionEvent event)
  {
    Object item=event.getSource();

    if (item==FileMenuAbout)
      openAboutWin();
    else if (item==FileMenuClose)
      closewin();
    else if (item==FMExportMIDI)
      fileExportAs(FILETYPE_MIDI);
    else if (item==FMExportXML)
      fileExportAs(FILETYPE_XML);
    else if (item==FileMenuGeneratePDF)
      showGenPDFDialog();

    else if (item==MTZoomControl.viewSizeField)
      ViewSizeFieldAction();
    else if (item==VMVSZoomOut || item==MTZoomControl.zoomOutButton)
      zoomOut();
    else if (item==VMVSZoomIn || item==MTZoomControl.zoomInButton)
      zoomIn();

    else if (item==VMTOrigText)
      setTexting(true,false);
    else if (item==VMTModText)
      setTexting(false,true);
    else if (item==VMTBothText)
      setTexting(true,true);
    else if (item==VMTNoText)
      setTexting(false,false);

    else if (item==ViewMenuViewParts)
      openPartsLayout(false);
    else if (item==VMPPParts)
      openPartsLayout(true);
    else if (item==VMPPScore)
      openScorePagePreview();

    else if (item==VMOMMarkAllVariants)
      setVariantMarkingOption(OptionSet.OPT_VAR_ALL);
    else if (item==VMOMMarkSubstantiveVariants)
      setVariantMarkingOption(OptionSet.OPT_VAR_SUBSTANTIVE);
    else if (item==VMOMMarkNoVariants)
      setVariantMarkingOption(OptionSet.OPT_VAR_NONE);
    else if (item==VMOMMarkSelectedVariants)
      setVariantMarkingOption(OptionSet.OPT_VAR_CUSTOM);
    else if (item==VersionsMenuNewNotesWindow)
      openNewNotesWindow();
    else if (item==VersionsMenuSourceAnalysis)
      openSourceAnalysisWindow();

    else if (item==genPDFOKButton)
      generatePDF();
    else if (item==genPDFCancelButton)
      genPDFDialog.setVisible(false);

    else if (item==MTNoteShapesOldButton)
      {
        if (optSet.useModernNoteShapes())
          VMNSItems[OptionSet.OPT_NOTESHAPE_ORIG].doClick();
      }
    else if (item==MTNoteShapesModButton)
      {
        if (!optSet.useModernNoteShapes())
          VMNSItems[OptionSet.OPT_NOTESHAPE_MOD_1_1].doClick();
      }
    else if (item==MTClefsOldButton)
      {
        if (optSet.get_usemodernclefs())
          ViewMenuUsemodernclefs.doClick();
      }
    else if (item==MTClefsModButton)
      {
        if (!optSet.get_usemodernclefs())
          ViewMenuUsemodernclefs.doClick();
      }
    else if (item==MTEdAccidentalsButton)
      ViewMenuDisplayEditorialAccidentals.doClick();
    else if (item==MTPitchOldButton)
      {
        if (optSet.getUseModernAccidentalSystem())
          ViewMenuModernAccidentalSystem.doClick();
      }
    else if (item==MTPitchModButton)
      {
        if (!optSet.getUseModernAccidentalSystem())
          ViewMenuModernAccidentalSystem.doClick();
      }
    else if (item==MTTextingOldButton)
      {
        if (optSet.get_displayOrigText())
          if (optSet.get_displayModText())
            VMTModText.doClick();
          else
            VMTNoText.doClick();
        else
          if (optSet.get_displayModText())
            VMTBothText.doClick();
          else
            VMTOrigText.doClick();
      }
    else if (item==MTTextingModButton)
      {
        if (optSet.get_displayModText())
          if (optSet.get_displayOrigText())
            VMTOrigText.doClick();
          else
            VMTNoText.doClick();
        else
          if (optSet.get_displayOrigText())
            VMTBothText.doClick();
          else
            VMTModText.doClick();
      }
    else if (item==PlayButton)
      toggleMIDIPlay();

    for (int i=0; i<VMDMVersions.length; i++)
      if (item==VMDMVersions[i])
        setCurrentVariantVersion(i);

    if (updatemusicgfx || rerendermusic)
      repaint();
  }

/*------------------------------------------------------------------------
Method:     void itemStateChanged(ItemEvent event)
Implements: ItemListener.itemStateChanged
Purpose:    Check for item state changes in menu and take appropriate action
Parameters:
  Input:  ItemEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void itemStateChanged(ItemEvent event)
  {
    Object item=event.getItemSelectable();

    if (item==ViewMenuUsemodernclefs)
      setUseModernClefs(!optSet.get_usemodernclefs());
    else if (item==ViewMenuDisplayEditorialAccidentals)
      setEdAccidentals(optSet.get_modacc_type()==OptionSet.OPT_MODACC_NONE ?
        OptionSet.OPT_MODACC_ABOVESTAFF : OptionSet.OPT_MODACC_NONE);
    else if (item==ViewMenuModernAccidentalSystem)
      setUseModernPitch(!optSet.getUseModernAccidentalSystem());
    else if (item==ViewMenuDisplayallnewlineclefs)
      {
        optSet.set_displayallnewlineclefs(!optSet.get_displayallnewlineclefs());
        rerendermusic=true;
      }
    else if (item==ViewMenuDisplayligbrackets)
      {
        optSet.set_displayligbrackets(!optSet.get_displayligbrackets());
        updatemusicgfx=true;
      }
    else if (item==ViewMenuEdCommentary)
      {
        optSet.setViewEdCommentary(!optSet.getViewEdCommentary());
        showCommentaryPanel(optSet.getViewEdCommentary());
        rerendermusic=true;
      }

    else if (item==VersionsMenuDisplayVariantOptions)
      variantDisplayOptionsFrame.setVisible(VersionsMenuDisplayVariantOptions.isSelected());

    else if (item==AnalysisMenuMarkDissonances)
      {
        optSet.set_markdissonances(!optSet.get_markdissonances());
        updatemusicgfx=true;
      }
    else if (item==AnalysisMenuMarkDirectedProgressions)
      {
        optSet.set_markdirectedprogressions(!optSet.get_markdirectedprogressions());
        updatemusicgfx=true;
      }

    if (VMOMCustomVariants!=null)
      for (int vi=0; vi<VMOMCustomVariants.length; vi++)
        if (item==VMOMCustomVariants[vi])
          {
            long vflag=1<<vi;
            if (VMOMCustomVariants[vi].isSelected())
              optSet.addCustomVariantFlags(vflag);
            else
              optSet.removeCustomVariantFlags(vflag);
            rerendermusic=true;
          }

    if (updatemusicgfx || rerendermusic)
      repaint();
  }

/*------------------------------------------------------------------------
Method:  void setCurrentVariantVersion(int vi)
Purpose: Select one variant version for display/editing
Parameters:
  Input:  int vi - index of selected version (0 or -1 for DEFAULT)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void setCurrentVariantVersion(int vi)
  {
    VariantVersionData newVersion=vi<0 ? musicData.getDefaultVariantVersion() :
                                         musicData.getVariantVersion(vi);
    setCurrentVariantVersion(newVersion);
  }

  protected void setCurrentVariantVersion(VariantVersionData newVersion)
  {
    ViewScr.setCurrentVariantVersion(newVersion);
    updateTBVersionDisplay(newVersion);
  }

  public void reconstructCurrentVersion()
  {
    ViewScr.setCurrentVariantVersion(ViewScr.getCurrentVariantVersion());
  }

  public void rerender()
  {
    rerendermusic=true;
    repaint();
  }

/*------------------------------------------------------------------------
Method:  void setUseModern*(boolean newstate)
Purpose: Change state of notational style and update GUI
Parameters:
  Input:  boolean newstate - new state of flag
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void updateTBNSButtons()
  {
    switch (optSet.get_noteShapeType())
      {
        case OptionSet.OPT_NOTESHAPE_ORIG:
          MTNoteShapesOldButton.setIcon(NoteShapesOldIcon_light);
          MTNoteShapesModButton.setIcon(NoteShapesModIcon_dark);
          break;
        default:
          MTNoteShapesOldButton.setIcon(NoteShapesOldIcon_dark);
          MTNoteShapesModButton.setIcon(NoteShapesModIcon_light);
          break;
      }
  }

  void setUseModernClefs(boolean newstate)
  {
/*    if (newstate==optSet.get_usemodernclefs())
      return;*/

    if (newstate==true)
      {
        MTClefsOldButton.setIcon(ClefsOldIcon_dark);
        MTClefsModButton.setIcon(ClefsModIcon_light);
      }
    else
      {
        MTClefsOldButton.setIcon(ClefsOldIcon_light);
        MTClefsModButton.setIcon(ClefsModIcon_dark);
      }

    optSet.set_usemodernclefs(newstate);
    rerendermusic=true;
  }

/*------------------------------------------------------------------------
Method:  void setEdAccidentals(int newstate)
Purpose: Change state of editorial accidental display and update GUI
Parameters:
  Input:  int newstate - new state of flag
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setEdAccidentals(int newstate)
  {
/*    if (newstate==optSet.get_modacc_type())
      return;*/

    if (newstate==OptionSet.OPT_MODACC_NONE)
      MTEdAccidentalsButton.setIcon(EdAccidentalsIcon_dark);
    else
      MTEdAccidentalsButton.setIcon(EdAccidentalsIcon_light);

    optSet.set_modacc_type(newstate);
    rerendermusic=true;
  }

/*------------------------------------------------------------------------
Method:  void setUseModernPitch(boolean newstate)
Purpose: Change state of pitch system and update GUI
Parameters:
  Input:  boolean newstate - new state of flag
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setUseModernPitch(boolean newstate)
  {
/*    if (newstate==optSet.getUseModernAccidentalSystem())
      return;*/

    if (newstate==true)
      {
        MTPitchOldButton.setIcon(PitchOldIcon_dark);
        MTPitchModButton.setIcon(PitchModIcon_light);
      }
    else
      {
        MTPitchOldButton.setIcon(PitchOldIcon_light);
        MTPitchModButton.setIcon(PitchModIcon_dark);
      }

    optSet.setUseModernAccidentalSystem(newstate);
    rerendermusic=true;
  }

/*------------------------------------------------------------------------
Method:  void setTexting(boolean origText,boolean modText)
Purpose: Change state of texting and update GUI
Parameters:
  Input:  boolean origText,modText - new states of flags
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setTexting(boolean origText,boolean modText)
  {
/*    if (origText==optSet.get_displayOrigText() &&
        modText==optSet.get_displayModText())
      return;*/

    MTTextingOldButton.setIcon(origText ? TextingOldIcon_light : TextingOldIcon_dark);
    MTTextingModButton.setIcon(modText ? TextingModIcon_light : TextingModIcon_dark);

    optSet.set_displayOrigText(origText);
    optSet.set_displayModText(modText);
    rerendermusic=true;
  }

/*------------------------------------------------------------------------
Method:  void setVariantMarkingOption(int newOption)
Purpose: Change state of variant marking and update GUI
Parameters:
  Input:  int newOption - new state of flags
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setVariantMarkingOption(int newOption,long newFlags)
  {
    optSet.setCustomVariantFlags(newFlags);
    setVariantMarkingOption(newOption);
  }

  void setVariantMarkingOption(int newOption)
  {
    optSet.setMarkVariants(newOption);
    rerendermusic=true;
    for (JCheckBoxMenuItem cb : VMOMCustomVariants)
      cb.setEnabled(optSet.getMarkVariants()==optSet.OPT_VAR_CUSTOM);
  }

  void setVersionsMenuDisplayVariantOptions(boolean newval)
  {
    VersionsMenuDisplayVariantOptions.setSelected(newval);
  }

/*------------------------------------------------------------------------
Inner Class: ViewSizeListener
Implements:  ActionListener
Purpose:     Handles events for View Scale menu
------------------------------------------------------------------------*/

  class ViewSizeListener implements ActionListener
  {
/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Method:  void actionPerformed(ActionEvent event)
Purpose: Check for action types in menu and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

    public void actionPerformed(ActionEvent event)
    {
      curVMVSnum=Integer.parseInt(event.getActionCommand());
      setViewSize(VMVSnums[curVMVSnum]);
    }
  }

/*------------------------------------------------------------------------
End Inner Class ViewSizeListener
------------------------------------------------------------------------*/


/*------------------------------------------------------------------------
Inner Class: BarlineStyleListener
Implements:  ActionListener
Purpose:     Handles events for Barline Style menu
------------------------------------------------------------------------*/

  class BarlineStyleListener implements ActionListener
  {
/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Method:  void actionPerformed(ActionEvent event)
Purpose: Check for action types in menu and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

    public void actionPerformed(ActionEvent event)
    {
      optSet.set_barline_type(Integer.parseInt(event.getActionCommand()));
      rerendermusic=true;
      repaint();
    }
  }

/*------------------------------------------------------------------------
End Inner Class BarlineStyleListener
------------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Inner Class: NoteShapeStyleListener
Implements:  ActionListener
Purpose:     Handles events for Note Shape / Reduction menu
------------------------------------------------------------------------*/

  class NoteShapeStyleListener implements ActionListener
  {
/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Method:  void actionPerformed(ActionEvent event)
Purpose: Check for action types in menu and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

    public void actionPerformed(ActionEvent event)
    {
      optSet.set_noteShapeType(Integer.parseInt(event.getActionCommand()));
      updateTBNSButtons();
      rerendermusic=true;
      repaint();
    }
  }

/*------------------------------------------------------------------------
End Inner Class NoteShapeStyleListener
------------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Method:  void unregisterListeners()
Purpose: Remove all action/item/etc listeners when disposing of window
         resources
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void unregisterListeners()
  {
    AdjustmentListener al[]=MusicScrollBarX.getListeners(AdjustmentListener.class);
    for (int i=0; i<al.length; i++)
      MusicScrollBarX.removeAdjustmentListener(al[i]);

    al=MusicScrollBarY.getListeners(AdjustmentListener.class);
    for (int i=0; i<al.length; i++)
      MusicScrollBarY.removeAdjustmentListener(al[i]);

    WindowListener wl[]=getListeners(WindowListener.class);
    for (int i=0; i<wl.length; i++)
      removeWindowListener(wl[i]);

    ComponentListener cl[]=getListeners(ComponentListener.class);
    for (int i=0; i<cl.length; i++)
      removeComponentListener(cl[i]);

    genPDFOKButton.removeActionListener(this);
    genPDFCancelButton.removeActionListener(this);

    ViewScr.unregisterListeners();
    unregisterToolListeners();
  }

  /* override just this one when changing menu/tool structures in subclasses */
  protected void unregisterToolListeners()
  {
    FileMenuAbout.removeActionListener(this);
    FileMenuClose.removeActionListener(this);
    FMExportMIDI.removeActionListener(this);
    FMExportXML.removeActionListener(this);
    FileMenuGeneratePDF.removeActionListener(this);

    for (int i=0; i<VMVSnumItems.length; i++)
      VMVSnumItems[i].removeActionListener(VSListener);

    for (int i=0; i<VMBSItems.length; i++)
      VMBSItems[i].removeActionListener(VMBSListener);

    for (int i=0; i<VMNSItems.length; i++)
      VMNSItems[i].removeActionListener(VMNSListener);

    VMVSZoomOut.removeActionListener(this);
    VMVSZoomIn.removeActionListener(this);
    VMTOrigText.removeActionListener(this);
    VMTModText.removeActionListener(this);
    VMTBothText.removeActionListener(this);
    VMTNoText.removeActionListener(this);
    ViewMenuUsemodernclefs.removeItemListener(this);
    ViewMenuDisplayEditorialAccidentals.removeItemListener(this);
    ViewMenuModernAccidentalSystem.removeItemListener(this);
    ViewMenuDisplayallnewlineclefs.removeItemListener(this);
    ViewMenuDisplayligbrackets.removeItemListener(this);
    ViewMenuEdCommentary.removeItemListener(this);
    ViewMenuViewParts.removeActionListener(this);
    VMPPParts.removeActionListener(this);
    VMPPScore.removeActionListener(this);
    MTNoteShapesOldButton.removeActionListener(this);
    MTNoteShapesModButton.removeActionListener(this);
    MTClefsModButton.removeActionListener(this);
    MTClefsOldButton.removeActionListener(this);
    MTZoomControl.removeListeners();
    PlayButton.removeActionListener(this);
    VersionsMenuGeneralInfo.removeActionListener(this);
    for (JRadioButtonMenuItem jr : VMDMVersions)
      jr.removeActionListener(this);
    VersionsMenuNewNotesWindow.removeActionListener(this);
    VersionsMenuDisplayVariantOptions.removeItemListener(this);
    VersionsMenuSourceAnalysis.removeActionListener(this);
    variantDisplayOptionsFrame.unregisterListeners();
    VMOMMarkAllVariants.removeActionListener(this);
    VMOMMarkSubstantiveVariants.removeActionListener(this);
    VMOMMarkNoVariants.removeActionListener(this);
    VMOMMarkSelectedVariants.removeActionListener(this);
    for (JCheckBoxMenuItem cb : VMOMCustomVariants)
      cb.removeItemListener(this);

    AnalysisMenuMarkDissonances.removeItemListener(this);
    AnalysisMenuMarkDirectedProgressions.removeItemListener(this);
  }

/*------------------------------------------------------------------------
Method:  boolean closewin()
Purpose: Close window and dependents
Parameters:
  Input:  -
  Output: -
  Return: whether window was closed or not
------------------------------------------------------------------------*/

  public boolean closewin()
  {
    if (partsWin!=null)
      {
        partsWin.closewin();
        partsWin=null;
      }
    if (scorePageWin!=null)
      {
        scorePageWin.closewin();
        scorePageWin=null;
      }
    ArrayList<CriticalNotesWindow> openNotesWindows=new ArrayList<CriticalNotesWindow>(notesWindows);
    for (CriticalNotesWindow cnw : openNotesWindows)
      cnw.closeFrame();
    genPDFDialog.dispose();
    unregisterListeners();
    stopMIDIPlay();
    fileWindows.remove(this);
    if (viewerWin!=null)
      viewerWin.removeFromWindowList(windowFileName);
    dispose();
    return true;
  }

/*------------------------------------------------------------------------
Method:  void gotomeasure(int mnum)
Purpose: Move scroll bar and display to new measure
Parameters:
  Input:  int mnum - measure number
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public int updateMeasure=-1;

  public void gotomeasure(int mnum)
  {
    MusicScrollBarX.setValue(mnum);
    if (MusicScrollBarX.getValue()!=mnum)
      MusicScrollBarX.setValue(mnum);

    if (MusicScrollBarX.getValue()!=mnum)
      updateMeasure=mnum;
    else
      updateMeasure=-1;
  }

/*------------------------------------------------------------------------
Method:  void gotoHeight(int newY)
Purpose: Move vertical scroll bar and display to new height
Parameters:
  Input:  int newY - new Y val
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void gotoHeight(int newY)
  {
//    while (MusicScrollBarY.getValue()!=newY)
      MusicScrollBarY.setValue(newY);
  }

/*------------------------------------------------------------------------
Method:  void setmeasurenum(int mnum)
Purpose: Display measure number in status panel
Parameters:
  Input:  int mnum - measure number
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setmeasurenum(int mnum)
  {
    StatusMeasureNum.setText(mnum+"/"+ViewScr.nummeasures);
  }

/*------------------------------------------------------------------------
Method:  void setScrollBarXextent(int extent)
Purpose: Set the extent of the horizontal scrollbar
Parameters:
  Input:  int extent - new extent
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setScrollBarXextent(int extent)
  {
    MusicScrollBarX.setVisibleAmount(extent);
    MusicScrollBarX.setBlockIncrement(extent);
  }

/*------------------------------------------------------------------------
Method:  void setScrollBarXmax(int max)
Purpose: Set the maximum value of the horizontal scrollbar
Parameters:
  Input:  int max - new maximum
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setScrollBarXmax(int max)
  {
    MusicScrollBarX.setMaximum(max);
  }

  public void setScrollBarXmax()
  {
    setScrollBarXmax(ViewScr.nummeasures);
  }

/*------------------------------------------------------------------------
Method:  void setScrollBarYparams(int virtual_height,int view_height,int val)
Purpose: Set parameters for the vertical scrollbar
Parameters:
  Input:  int virtual_height - total y size of view screen
          int view_height    - y amount of view screen visible
          int val            - scroll bar position
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setScrollBarYparams(int virtual_height,int view_height,int val)
  {
//System.out.println("VirtH="+virtual_height+" ViewH="+view_height);
    MusicScrollBarY.setValue(val);
    MusicScrollBarY.setMaximum(virtual_height);
    MusicScrollBarY.setVisibleAmount(view_height);
    MusicScrollBarY.setUnitIncrement(10);
    MusicScrollBarY.setBlockIncrement((int)virtual_height/10);
  }

/*------------------------------------------------------------------------
Method:  void openAboutWin()
Purpose: Open general information window
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  GeneralInfoFrame aboutWin=null;

  protected void openAboutWin()
  {
    if (aboutWin==null)
      aboutWin=new GeneralInfoFrame(this);
    aboutWin.setVisible(true);
    aboutWin.toFront();
  }

/*------------------------------------------------------------------------
Method:  void openPartsLayout(boolean printpreview)
Purpose: Open new window with unscored parts in original notation
Parameters:
  Input:  boolean printpreview - whether to open print view or regular view
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void openPartsLayout(boolean printpreview)
  {
    if (partsWin!=null)
      partsWin.closewin();

    if (ViewScr.getCurrentVariantVersion()==musicData.getDefaultVariantVersion())
      printpreview=true;

    partsWin=new PartsWin(ViewScr.getMusicData(),MusicGfx,this,printpreview);
    partsWin.openwin();
  }

  protected void openPartsLayout()
  {
    openPartsLayout(false);
  }

/*------------------------------------------------------------------------
Method:  void openScorePagePreview()
Purpose: Open new window with score in page layout
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void openScorePagePreview()
  {
    if (scorePageWin!=null)
      scorePageWin.closewin();
    scorePageWin=new ScorePagePreviewWin(musicData,this);
    scorePageWin.openwin();
  }

/*------------------------------------------------------------------------
Method:  void openNewNotesWindow()
Purpose: Open new window with critical notes
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected LinkedList<CriticalNotesWindow> notesWindows=new LinkedList<CriticalNotesWindow>();

  protected void openNewNotesWindow()
  {
    CriticalNotesWindow notesWin=new CriticalNotesWindow(
      musicData,this,
      MusicGfx,ViewScr.STAFFSCALE,ViewScr.VIEWSCALE);
    notesWindows.add(notesWin);
    notesWin.setVisible(true);
  }

  public void notesWindowClosed(CriticalNotesWindow notesWin)
  {
    notesWindows.remove(notesWin);
  }

/*------------------------------------------------------------------------
Method:  void openSourceAnalysisWindow()
Purpose: Open window with source/variant analysis visualizations
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void openSourceAnalysisWindow()
  {
    VariantAnalysisList val=new VariantAnalysisList(musicData,this);
  }

  /* MIDI Playback functions */

  MIDIPlayer scorePlayer=null;

  protected void toggleMIDIPlay()
  {
    if (scorePlayer==null || !scorePlayer.currentlyPlaying())
      {
        startMIDIPlay();
        PlayButton.setText("STOP");
      }
    else
      {
        stopMIDIPlay();
        PlayButton.setText("PLAY");
      }
  }

  protected void startMIDIPlay()
  {
    scorePlayer=new MIDIPlayer(this,ViewScr.getMusicData(),ViewScr.getRenderedMusic());
    scorePlayer.play(ViewScr.curmeasure);
  }

  protected void stopMIDIPlay()
  {
    if (scorePlayer!=null)
      scorePlayer.stop();
    ViewScr.MIDIMeasureStarted(-1,null);
  }

  public void MIDIEnded()
  {
    PlayButton.setText("PLAY");
    ViewScr.MIDIMeasureStarted(-1,null);
  }

  public void MIDIMeasureStarted(int mnum)
  {
    ViewScr.MIDIMeasureStarted(mnum,MusicScrollBarX);
  }

/*------------------------------------------------------------------------
Methods:    void window[Gained|Lost]Focus(WindowEvent e)
Implements: WindowFocusListener.window[Gained|Lost]Focus
Purpose:    Take action when window focus is gained or lost
Parameters:
  Input:  WindowEvent e - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void windowGainedFocus(WindowEvent e)
  {
    curWindow=this;
  }

  public void windowLostFocus(WindowEvent e)
  {
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
    return ViewScr.getCurrentVariantVersion();
  }

  public PieceData getMusicData()
  {
    return musicData;
  }

  public String getWindowFileName()
  {
    return windowFileName;
  }

/*------------------------------------------------------------------------
Method:  void handleRuntimeError(Exception e)
Purpose: Default handler for exceptions thrown by AWT threads
Parameters:
  Input:  Exception e - exception to report
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void handleRuntimeError(Exception e)
  {
    if (MetaData.CMME_OPT_TESTING)
      {
        String stackTraceStr="";
        for (int i=0; i<e.getStackTrace().length && i<MAX_STACK_TRACE_LEVELS; i++)
          stackTraceStr+=e.getStackTrace()[i]+"\n";
        displayErrorMessage(
          "An error has occurred: "+e+"\n"+
          "Location: \n"+stackTraceStr,"CMME internal error");
        System.err.println("Exception: "+e);
        e.printStackTrace();
      }
  }

  public void displayErrorMessage(String msg,String winTitle)
  {
    JOptionPane.showMessageDialog(this,msg,winTitle,JOptionPane.ERROR_MESSAGE);
  }
}
