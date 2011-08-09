/*----------------------------------------------------------------------*/
/*

        Module          : EditorWin.java

        Package         : Editor

        Classes Included: EditorWin

        Purpose         : Lays out main music editor window and functions

        Programmer      : Ted Dumitrescu

        Date Started    : 5/4/05

Updates:
6/13/05:  started adding file functions (Open, Save, New)
12/21/05: improved file functions (prompt to overwrite, save on exit, etc)
1/13/06:  added basic import functionality for MIDI files
2/14/06:  began text editor
3/6/06:   consolidated separate event-editing tools (proportion, coloration,
          text annotations, etc.) into a single event editor window
3/10/06:  started Mensuration chooser GUI
3/20/06:  added Rest buttons to main toolbar
4/3/06:   no longer auto-creates PartsWin when opening window (for memory/speed)
4/5/06:   added NoteInfoPanel
5/4/06:   moved static file-browsing/opening functions to Gfx.MusicWin
7/26/06:  added ModernKeySigPanel
9/11/06:  added original texting functions to text editor; temporarily disabled
          individual voice text areas (until actual functionality is implemented)
11/26/08: allowed event editor to be hidden
          added Text menu
6/29/08:  added taskbar icons for dots+mensuration
9/8/09:   GeneralInfoFrame moved to separate class
5/7/10:   Improved error-handling/idiot-proofing in file save (as) functions

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.net.URL;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.Color;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.*;
import java.awt.event.*;

import Gfx.*;
import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   EditorWin
Extends: Gfx.MusicWin
Purpose: Lays out main music editor window and functions
------------------------------------------------------------------------*/

public class EditorWin extends Gfx.MusicWin implements ActionListener,ChangeListener,DocumentListener,ItemListener,WindowFocusListener
{
/*----------------------------------------------------------------------*/
/* Class variables */

  /* GUI images */
  /* notes */
  static ImageIcon[] NVIcons_light,NVIcons_dark;
  static ImageIcon   OtherSM_light,OtherSM_dark;
  static int[]       NVButtonVals=new int[]
                       {
                         NoteEvent.NT_Maxima,
                         NoteEvent.NT_Longa,
                         NoteEvent.NT_Brevis,
                         NoteEvent.NT_Semibrevis,
                         NoteEvent.NT_Minima,
                         NoteEvent.NT_Semiminima,
                         NoteEvent.NT_Fusa,
                         NoteEvent.NT_Semifusa
                       };
  static int         NVB_SEMIMINIMA=5;

  /* rests */
  static ImageIcon[] RVIcons_light,RVIcons_dark;

  /* clefs */
  static ImageIcon[] ClefIcons,
                     AlternateClefIcons;
  static int[]       ClefButtonVals=new int[]
                       {
                         Clef.CLEF_C,
                         Clef.CLEF_F,
                         Clef.CLEF_G,
                         Clef.CLEF_Bmol,
                         Clef.CLEF_Bqua,
                         Clef.CLEF_Diesis
                       },
                     AlternateClefButtonVals=new int[]
                       {
                         Clef.CLEF_NONE,
                         Clef.CLEF_Frnd,
                         Clef.CLEF_NONE,
                         Clef.CLEF_BmolDouble,
                         Clef.CLEF_NONE,
                         Clef.CLEF_NONE
                       };

  /* misc buttons */
  static final int   MISC_BUTTON_DOT=   0,
                     MISC_BUTTON_DOTDIV=1;
  static ImageIcon[] MiscIcons;
  static String[]    MiscButtonVals=new String[]
                       {
                         "Dot","DotDiv"
                       },
                     MiscButtonNames=new String[]
                       {
                         "Dot of Addition","Dot of Division/Perfection/etc"
                       };

  /* mensuration buttons */
  static final int   MENS_BUTTON_O=     0,
                     MENS_BUTTON_C=     1,
                     MENS_BUTTON_DOT=   2,
                     MENS_BUTTON_STROKE=3,
                     MENS_BUTTON_3=     4,
                     MENS_BUTTON_2=     5;
  static ImageIcon[] MensIcons_light,MensIcons_dark;
  static String[]    MensButtonVals=new String[]
                       {
                         "O","C","Dot","Stroke","3","2"
                       },
                     MensButtonNames=new String[]
                       {
                         "O","C","Toggle dot","Toggle stroke","Add 3","Add 2"
                       };

/*----------------------------------------------------------------------*/
/* Instance variables */

  ScoreEditorCanvas EditScr;
  String            windowFilePath=null;
  boolean           modified=false;

  GeneralInfoFrame generalInfoFrame;
  TextEditorFrame  textEditorFrame;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Class methods */

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

  public static void initScoreWindowing(String bdu,String initDir,boolean inApplet)
  {
    Gfx.MusicWin.initScoreWindowing(bdu,initDir,inApplet);
    initEditorIcons();
  }

/*------------------------------------------------------------------------
Method:  void initEditorIcons()
Purpose: Initialize tool icons for editor GUI
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void initEditorIcons()
  {
    try
      {
        int           i;
        BufferedImage curCanvas,curFileImg;
        Graphics2D    curG;

        /* basic image elements */
        BufferedImage lightBG=javax.imageio.ImageIO.read(new URL(BaseDataURL+"imgs/buttonbg-light.gif")),
                      darkBG=javax.imageio.ImageIO.read(new URL(BaseDataURL+"imgs/buttonbg-dark.gif"));
        int           iconWidth=lightBG.getWidth(),
                      iconHeight=lightBG.getHeight();

        /* notes */
        NVIcons_light=new ImageIcon[NVButtonVals.length];
        NVIcons_dark=new ImageIcon[NVButtonVals.length];
        for (i=0; i<NVButtonVals.length; i++)
          {
            NVIcons_light[i]=new ImageIcon(new URL(BaseDataURL+"imgs/noteval-button"+(i+1)+"a.gif"));
            NVIcons_dark[i]=new ImageIcon(new URL(BaseDataURL+"imgs/noteval-button"+(i+1)+"a-dark.gif"));
          }
        OtherSM_light=new ImageIcon(new URL(BaseDataURL+"imgs/noteval-button"+(NVB_SEMIMINIMA+1)+"b.gif"));
        OtherSM_dark=new ImageIcon(new URL(BaseDataURL+"imgs/noteval-button"+(NVB_SEMIMINIMA+1)+"b-dark.gif"));

        /* rests */
        RVIcons_light=new ImageIcon[NVButtonVals.length];
        for (i=0; i<NVButtonVals.length; i++)
          RVIcons_light[i]=new ImageIcon(new URL(BaseDataURL+"imgs/restval-button"+(i+1)+"a.gif"));

        /* clefs */
        ClefIcons=new ImageIcon[ClefButtonVals.length];
        for (i=0; i<ClefButtonVals.length; i++)
          {
            curCanvas=new BufferedImage(iconWidth,iconHeight,BufferedImage.TYPE_INT_ARGB);
            curFileImg=javax.imageio.ImageIO.read(new URL(BaseDataURL+"imgs/clef-button"+Clef.ClefNames[ClefButtonVals[i]]+"1a.gif"));
            curG=curCanvas.createGraphics();
            curG.drawImage(lightBG,0,0,null);
            curG.drawImage(curFileImg,0,0,null);
            ClefIcons[i]=new ImageIcon(curCanvas);
          }

        /* misc buttons */
        MiscIcons=new ImageIcon[MiscButtonVals.length];
        for (i=0; i<MiscButtonVals.length; i++)
          {
            curCanvas=new BufferedImage(iconWidth,iconHeight,BufferedImage.TYPE_INT_ARGB);
            curFileImg=javax.imageio.ImageIO.read(new URL(BaseDataURL+"imgs/misc-button"+MiscButtonVals[i]+".gif"));
            curG=curCanvas.createGraphics();
            curG.drawImage(lightBG,0,0,null);
            curG.drawImage(curFileImg,0,0,null);
            MiscIcons[i]=new ImageIcon(curCanvas);
          }

        /* mensuration buttons */
        MensIcons_light=new ImageIcon[MensButtonVals.length];
        MensIcons_dark=new ImageIcon[MensButtonVals.length];
        for (i=0; i<MensButtonVals.length; i++)
          {
            curCanvas=new BufferedImage(iconWidth,iconHeight,BufferedImage.TYPE_INT_ARGB);
            curFileImg=javax.imageio.ImageIO.read(new URL(BaseDataURL+"imgs/mens-button"+MensButtonVals[i]+".gif"));
            curG=curCanvas.createGraphics();
            curG.drawImage(lightBG,0,0,null);
            curG.drawImage(curFileImg,0,0,null);
            MensIcons_light[i]=new ImageIcon(curCanvas);

            curCanvas=new BufferedImage(iconWidth,iconHeight,BufferedImage.TYPE_INT_ARGB);
            curG=curCanvas.createGraphics();
            curG.drawImage(darkBG,0,0,null);
            curG.drawImage(curFileImg,0,0,null);
            MensIcons_dark[i]=new ImageIcon(curCanvas);
          }
      }
    catch (Exception e)
      {
        System.err.println("Error loading icons: "+e);
      }
  }

/*------------------------------------------------------------------------
Method:  void newfile()
Purpose: Open new music window for new blank score
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void newfile()
  {
    /* create window in separate thread */
    final Gfx.SwingWorker ofthread=new Gfx.SwingWorker()
    {
    public Object construct()
    {

    /* real code */

    /* construct blank piece data */
    PieceData musicdat=new PieceData();
    musicdat.setGeneralData("Title",null,"Composer","Editor",null,null);
    Voice[] vl=new Voice[1];
    vl[0]=new Voice(musicdat,1,"[1]",false);
    musicdat.setVoiceData(vl);
    musicdat.addVariantVersion(new VariantVersionData("Default"));

    MusicMensuralSection blankSec=new MusicMensuralSection(1);
//    blankSec.setVersion(musicdat.getVariantVersions().get(0));
    VoiceMensuralData newv=new VoiceMensuralData(vl[0],blankSec);
    newv.addEvent(new Event(Event.EVENT_SECTIONEND));
    blankSec.setVoice(0,newv);
    musicdat.addSection(blankSec);

    /* open music window */
    try
      {
        int xl=10,yl=10;
        if (curWindow!=null)
          {
            xl=curWindow.getLocation().x+20;
            yl=curWindow.getLocation().y+20;
          }
        new EditorWin("Untitled score",musicdat,null,false,xl,yl);
      }
    catch (Exception e)
      {
        System.err.println("Error creating editor window: "+e);
        e.printStackTrace();
      }

    return null; /* not used */
    }
    }; /* end SwingWorker */

    ofthread.start();
  }

/*------------------------------------------------------------------------
Method:  void exitprogram()
Purpose: Attempt to close all windows and exit program
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static synchronized void exitprogram()
  {
    exitingProgram=true;
    while (fileWindows.size()>0)
      {
        MusicWin mw=fileWindows.getFirst();
        if (!mw.closewin())
          {
            exitingProgram=false;
            return; /* action cancelled */
          }
      }

    System.exit(0);
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: EditorWin(String fn,PieceData p,String fp)
Purpose:     Initialize window
Parameters:
  Input:  String fn   - filename (for window title)
          PieceData p - data for music to display
          String fp   - complete path of file (null for new file)
          int xl,yl   - location for new window
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public EditorWin(String fn,PieceData p,String fp,boolean convertedData,int xl,int yl) throws Exception
  {
    super(fn,p,xl,yl);
    windowFilePath=fp;
    EditScr=(ScoreEditorCanvas)ViewScr;

    createInsertSectionDialog();
    setSectionNum(EditScr.getCurSectionNum());

    if (convertedData)
      fileModified();
  }

  public EditorWin(String fn,PieceData p,String fp) throws Exception
  {
    this(fn,p,fp,false,10,10);
  }

  /* use only for pseudo-static functions (to simulate static inheritance):
     openFile, getWinToReplace, openWin (etc) */
  public EditorWin()
  {
  }

/*------------------------------------------------------------------------
Method:    MusicWin getWinToReplace()
Overrides: Gfx.MusicWin.getWinToReplace
Purpose:   If a new opening score window is to replace another one, return
           window to be replaced
Parameters:
  Input:  -
  Output: -
  Return: window to be replaced
------------------------------------------------------------------------*/

  protected MusicWin getWinToReplace()
  {
    /* if nothing has been opened/modified, replace blank score */
    EditorWin blankWin=null;
    if (fileWindows.size()==1)
      {
        blankWin=(EditorWin)(fileWindows.getFirst());
        if (blankWin.windowFilePath!=null || blankWin.modified)
          blankWin=null;
      }
    return blankWin;
  }

/*------------------------------------------------------------------------
Method:    MusicWin openWin()
Overrides: Gfx.MusicWin.openWin
Purpose:   Open new window after loading music data
Parameters:
  Input:  String fn           - filename
          String path         - canonical path to file
          PieceData musicData - music data from file
          int xl,yl           - coordinates for new window
  Output: -
  Return: new window
------------------------------------------------------------------------*/

  protected MusicWin openWin(String fn,String path,PieceData musicData,boolean convertedData,int xl,int yl) throws Exception
  {
    return new EditorWin(fn,musicData,path,convertedData,xl,yl);
  }

/*------------------------------------------------------------------------
Method:    MusicWin openInViewer()
Purpose:   Open new MusicWin with the same music data
Parameters:
  Input:  -
  Output: -
  Return: new window
------------------------------------------------------------------------*/

  protected MusicWin openInViewer()
  {
    try
      {
        return new MusicWin(this.windowFileName,this.musicData);
      }
    catch (Exception e)
      {
        handleRuntimeError(e);
        return null;
      }
  }

/*------------------------------------------------------------------------
Method:    void addCMMETitle(String fn)
Overrides: Gfx.MusicWin.addCMMETitle
Purpose:   Add title to window
Parameters:
  Input:  String fn - name of file in window
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void addCMMETitle(String fn)
  {
    setTitle(fn+": CMME Editor");
  }

/*------------------------------------------------------------------------
Method:    void initializeoptions()
Overrides: Gfx.MusicWin.initializeoptions
Purpose:   Initialize option set before creating view canvas
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void initializeoptions()
  {
    double newVS=(curWindow!=null) ? curWindow.optSet.getVIEWSCALE() :
                                     (double)DefaultViewScale/100;

    optSet.set_usemodernclefs(false);
    optSet.set_noteShapeType(OptionSet.OPT_NOTESHAPE_ORIG);
    optSet.set_barline_type(OptionSet.OPT_BARLINE_TICK);
    optSet.setVIEWSCALE(newVS);
    optSet.set_displayedittags(true);
    optSet.set_displayOrigText(true);
    optSet.set_displayModText(true);
    optSet.setUseModernAccidentalSystem(false);
    optSet.setMarkVariants(OptionSet.OPT_VAR_ALL);
  }

/*------------------------------------------------------------------------
Method:    void setSubframeLocations()
Overrides: Gfx.MusicWin.setSubframeLocations
Purpose:   Set locations of frames dependent upon main window (after window
           has been packed)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void setSubframeLocations()
  {
    setEventEditorLocation();
    setEditingOptionsLocation();
    setSectionAttribsFrameLocation();
  }

/*------------------------------------------------------------------------
Method:  PartsWin createInitialPartsWin()
Overrides: Gfx.MusicWin.createInitialPartsWin
Purpose: Initialize unscored parts window when opening window
Parameters:
  Input:  -
  Output: -
  Return: null - EditorWin only creates a PartsWin when necessary
------------------------------------------------------------------------*/

  protected PartsWin createInitialPartsWin()
  {
    return null;
  }

/*------------------------------------------------------------------------
Method:  JMenu create*Menu()
Overrides: Gfx.MusicWin.create*Menu
Purpose: Create menus for window
Parameters:
  Input:  -
  Output: -
  Return: menu
------------------------------------------------------------------------*/

  /* File Menu */

  protected JMenuItem FileMenuNew,
                      FileMenuOpen,
                      FileMenuSave,
                      FileMenuSaveAs,
                      FileMenuExit;

  protected JMenu createFileMenu()
  {
    /* create menu and items */
    JMenu FM=new JMenu("File");

    FileMenuNew=new JMenuItem("New");
    FileMenuNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,ActionEvent.CTRL_MASK));
    FileMenuNew.setMnemonic(KeyEvent.VK_N);

    FileMenuOpen=new JMenuItem("Open...");
    FileMenuOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,ActionEvent.CTRL_MASK));
    FileMenuOpen.setMnemonic(KeyEvent.VK_O);

    FileMenuSave=new JMenuItem("Save");
    FileMenuSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,ActionEvent.CTRL_MASK));
    FileMenuSave.setMnemonic(KeyEvent.VK_S);

    FileMenuSaveAs=new JMenuItem("Save As...");
    FileMenuSaveAs.setMnemonic(KeyEvent.VK_A);

	FileMenuExport=new JMenu("Export");
    FileMenuExport.setMnemonic(KeyEvent.VK_E);
    FMExportMIDI=new JMenuItem("MIDI");
    FMExportXML=new JMenuItem("MusicXML");
    FileMenuExport.add(FMExportMIDI);
    FileMenuExport.add(FMExportXML);

    FileMenuGeneratePDF=new JMenuItem("Generate PDF...");
    FileMenuGeneratePDF.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,ActionEvent.CTRL_MASK));
    FileMenuGeneratePDF.setMnemonic(KeyEvent.VK_P);
    FileMenuGeneratePDF.setEnabled(false);

    FileMenuClose=new JMenuItem("Close");
    FileMenuClose.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,ActionEvent.CTRL_MASK));
    FileMenuClose.setMnemonic(KeyEvent.VK_C);

    FileMenuExit=new JMenuItem("Exit");
    FileMenuExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,ActionEvent.CTRL_MASK));
    FileMenuExit.setMnemonic(KeyEvent.VK_X);

    FM.add(FileMenuNew);
    FM.add(FileMenuOpen);
    FM.add(FileMenuSave);
    FM.add(FileMenuSaveAs);
    FM.add(FileMenuExport);
    FM.add(FileMenuGeneratePDF);
    FM.add(FileMenuClose);
    FM.add(FileMenuExit);

    /* handle menu actions */
    FileMenuNew.addActionListener(this);
    FileMenuOpen.addActionListener(this);
    FileMenuSave.addActionListener(this);
    FileMenuSaveAs.addActionListener(this);
    FMExportMIDI.addActionListener(this);
    FMExportXML.addActionListener(this);
    FileMenuGeneratePDF.addActionListener(this);
    FileMenuClose.addActionListener(this);
    FileMenuExit.addActionListener(this);

    return FM;
  }

  /* Edit Menu */

  protected JMenuItem         EditMenuCopy,
                              EditMenuCut,
                              EditMenuPaste,
                              EditMenuSelectAll,
                              EditMenuDelete,
                              EditMenuGeneralInformation;
  protected JCheckBoxMenuItem EditMenuDisplayEventEditor,
                              EditMenuEditingOptions;

  protected EditingOptionsFrame editingOptionsFrame;

  protected JMenu createEditMenu()
  {
    /* editing options */
    editingOptionsFrame=new EditingOptionsFrame(this);

    JMenu EM=new JMenu("Edit");

    EditMenuCopy=new JMenuItem("Copy");
    EditMenuCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,ActionEvent.CTRL_MASK));
    EditMenuCopy.setMnemonic(KeyEvent.VK_C);

    EditMenuCut=new JMenuItem("Cut");
    EditMenuCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,ActionEvent.CTRL_MASK));
    EditMenuCut.setMnemonic(KeyEvent.VK_U);

    EditMenuPaste=new JMenuItem("Paste");
    EditMenuPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,ActionEvent.CTRL_MASK));
    EditMenuPaste.setMnemonic(KeyEvent.VK_P);
    EditMenuPaste.setEnabled(false);

    EditMenuSelectAll=new JMenuItem("Select all");
    EditMenuSelectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A,ActionEvent.CTRL_MASK));
    EditMenuSelectAll.setMnemonic(KeyEvent.VK_A);

    EditMenuDelete=new JMenuItem("Delete");
    EditMenuDelete.setMnemonic(KeyEvent.VK_DELETE);

    EditMenuGeneralInformation=new JMenuItem("General information...");
    EditMenuGeneralInformation.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,ActionEvent.CTRL_MASK));
    EditMenuGeneralInformation.setMnemonic(KeyEvent.VK_G);

    EditMenuDisplayEventEditor=new JCheckBoxMenuItem("Display event editor",false);
    EditMenuDisplayEventEditor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E,ActionEvent.CTRL_MASK));
    EditMenuDisplayEventEditor.setMnemonic(KeyEvent.VK_E);

    EditMenuEditingOptions=new JCheckBoxMenuItem("Display input options",false);

    EM.add(EditMenuCut);
    EM.add(EditMenuCopy);
    EM.add(EditMenuPaste);
    EM.add(EditMenuSelectAll);
    EM.add(EditMenuDelete);
    EM.addSeparator();
    EM.add(EditMenuGeneralInformation);
    EM.add(EditMenuDisplayEventEditor);
    EM.add(EditMenuEditingOptions);

    EditMenuCopy.addActionListener(this);
    EditMenuCut.addActionListener(this);
    EditMenuPaste.addActionListener(this);
    EditMenuSelectAll.addActionListener(this);
    EditMenuDelete.addActionListener(this);

    EditMenuGeneralInformation.addActionListener(this);
    EditMenuDisplayEventEditor.addItemListener(this);
    EditMenuEditingOptions.addItemListener(this);

    return EM;
  }

  /* Sections Menu */

  protected JMenuItem         SectionsMenuInsertSection,
                              SectionsMenuInsertSectionBreak;
  protected JCheckBoxMenuItem SectionsMenuDisplaySectionAttribs;

  protected SectionAttribsFrame sectionAttribsFrame;

  protected JMenu createSectionsMenu()
  {
    initSectionAttribsFrame();

    JMenu SM=new JMenu("Sections");

    SectionsMenuInsertSection=new JMenuItem("Insert new section...");
    SectionsMenuInsertSection.setMnemonic(KeyEvent.VK_S);
//    SectionsMenuInsertSection.setEnabled(false);

    SectionsMenuInsertSectionBreak=new JMenuItem("Insert section break");
    SectionsMenuInsertSectionBreak.setMnemonic(KeyEvent.VK_B);

    SectionsMenuDisplaySectionAttribs=new JCheckBoxMenuItem("Display section attributes",false);
    SectionsMenuDisplaySectionAttribs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,ActionEvent.CTRL_MASK));
    SectionsMenuDisplaySectionAttribs.setMnemonic(KeyEvent.VK_R);

    SM.add(SectionsMenuInsertSection);
    SM.add(SectionsMenuInsertSectionBreak);
    SM.add(SectionsMenuDisplaySectionAttribs);
    SectionsMenuInsertSectionBreak.addActionListener(this);
    SectionsMenuInsertSection.addActionListener(this);
    SectionsMenuDisplaySectionAttribs.addItemListener(this);

    return SM;
  }

  public void initSectionAttribsFrame()
  {
    if (sectionAttribsFrame!=null)
      {
        sectionAttribsFrame.setVisible(false); /* hide for re-init */
        sectionAttribsFrame.unregisterListeners();
      }
    sectionAttribsFrame=new SectionAttribsFrame(this);
  }

  public void setSectionAttribsFrameLocation()
  {
    sectionAttribsFrame.setLocation(getLocation().x+getSize().width,
                                    getLocation().y);
  }

  public void showSectionAttribsFrame(boolean show)
  {
    if (sectionAttribsFrame.isShowing())
      {
        if (!show)
          sectionAttribsFrame.setVisible(false);
      }
    else
      if (show)
        {
          initSectionAttribsFrame(); /* re-init Frame to handle changes in music data */
          setSectionNum(EditScr.getCurSectionNum());
          setSectionAttribsFrameLocation();
          sectionAttribsFrame.setVisible(true);
        }
  }

  /* Text Menu */

  protected JMenuItem TextMenuOpenEditor,
                      TextMenuSetCurrentAsDefault,
                      TextMenuDeleteOriginalText,
                      TextMenuDeleteModernText;

  protected JMenu createTextMenu()
  {
    JMenu TM=new JMenu("Text");

    TextMenuOpenEditor=new JMenuItem("Open text editor");
    TextMenuOpenEditor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T,ActionEvent.CTRL_MASK));
    TextMenuOpenEditor.setMnemonic(KeyEvent.VK_T);

    TextMenuSetCurrentAsDefault=new JMenuItem("Set current version text as default");

    TextMenuDeleteOriginalText=new JMenuItem("Delete original text...");
    TextMenuDeleteOriginalText.setMnemonic(KeyEvent.VK_O);

    TextMenuDeleteModernText=new JMenuItem("Delete modern text...");
    TextMenuDeleteModernText.setMnemonic(KeyEvent.VK_M);

    TM.add(TextMenuOpenEditor);
    TM.add(TextMenuSetCurrentAsDefault);
    TM.add(TextMenuDeleteOriginalText);
    TM.add(TextMenuDeleteModernText);
    TextMenuOpenEditor.addActionListener(this);
    TextMenuSetCurrentAsDefault.addActionListener(this);
    TextMenuDeleteOriginalText.addActionListener(this);
    TextMenuDeleteModernText.addActionListener(this);

    return TM;
  }

  /* View Menu */

  protected JMenuItem ViewMenuPrintPreview,
                      ViewMenuOpenInViewer;

  protected JMenu createViewMenu()
  {
    JMenu VM=super.createViewMenu();

    /* force some options in editor window */
    VMTBothText.setSelected(true);
//    ViewMenuUsemodernclefs.setState(false);
    ViewMenuDisplayallnewlineclefs.setState(true);
    ViewMenuDisplayligbrackets.setState(true);
    VMTOrigText.setEnabled(false);
    VMTModText.setEnabled(false);
    VMTBothText.setEnabled(false);
    VMTNoText.setEnabled(false);
    ViewMenuBarlineStyle.setEnabled(false);
    ViewMenuNoteShapeStyle.setEnabled(false);
    ViewMenuTexting.setEnabled(false);
    ViewMenuPitchSystem.setEnabled(false);
    ViewMenuUsemodernclefs.setEnabled(false);
    ViewMenuDisplayEditorialAccidentals.setEnabled(false);
    ViewMenuModernAccidentalSystem.setEnabled(false);
    ViewMenuDisplayallnewlineclefs.setEnabled(false);
    ViewMenuDisplayligbrackets.setEnabled(false);
    ViewMenuEdCommentary.setEnabled(false);

    VM.remove(ViewMenuPrintPreviews);
    ViewMenuPrintPreview=new JMenuItem("Print Preview");
    ViewMenuPrintPreview.setMnemonic(KeyEvent.VK_R);
    VM.add(ViewMenuPrintPreview);
    ViewMenuPrintPreview.addActionListener(this);

    ViewMenuOpenInViewer=new JMenuItem("Open in CMME Viewer");
    VM.add(ViewMenuOpenInViewer);
    ViewMenuOpenInViewer.addActionListener(this);

    return VM;
  }

  protected JMenuItem VersionsMenuSetVersionAsDefault;

  protected JMenu createVersionsMenu()
  {
    /* create menu and items */
    JMenu VM=new JMenu("Versions");

    VersionsMenuGeneralInfo=new JMenuItem("Variant Version Information...");
    VersionsMenuGeneralInfo.setMnemonic(KeyEvent.VK_I);

    VersionsMenuSetVersionAsDefault=new JMenuItem("Set current version as default");

    VersionsMenuNewNotesWindow=new JMenuItem("New critical notes list...");
    VersionsMenuNewNotesWindow.setMnemonic(KeyEvent.VK_N);

    VersionsMenuSourceAnalysis=new JMenuItem("Source analysis...");
    VersionsMenuSourceAnalysis.setMnemonic(KeyEvent.VK_A);

    VM.add(VersionsMenuGeneralInfo);
    VM.add(VersionsMenuSetVersionAsDefault);
    VM.add(VersionsMenuNewNotesWindow);
//    VM.add(VersionsMenuSourceAnalysis);

    /* handle menu actions */
    VersionsMenuGeneralInfo.addActionListener(this);
    VersionsMenuSetVersionAsDefault.addActionListener(this);
    VersionsMenuNewNotesWindow.addActionListener(this);
    VersionsMenuSourceAnalysis.addActionListener(this);

    return VM;
  }

  protected JMenu createAnalysisMenu()
  {
    return null;
  }

/*------------------------------------------------------------------------
Method:    JPanel createStatusPanel()
Overrides: Gfx.MusicWin.createStatusPanel
Purpose:   Create information panel under viewscreen/scrollbar
Parameters:
  Input:  -
  Output: -
  Return: panel
------------------------------------------------------------------------*/

  protected JPanel createStatusPanel()
  {
    JPanel sp=super.createStatusPanel();
    commentaryTextArea.setContentType("text/plain");
    commentaryTextArea.setText("");
    commentaryScrollPane.setFocusable(false);
    commentaryTextArea.setFocusable(false);
    commentaryTextArea.getDocument().addDocumentListener(this);

    return sp;
  }

/*------------------------------------------------------------------------
Method:  void changeCommentaryTextAreaStatus(boolean enabled)
Purpose: Enable or disable commentary text area editing
Parameters:
  Input:  boolean enabled - whether to enable editing
  Output: -
  Return: -
------------------------------------------------------------------------*/

  int commentaryGUIupdating=0;

  void changeCommentaryTextAreaStatus(boolean enabled)
  {
    if (commentaryTextArea==null)
      return;
    if (commentaryGUIupdating>0)
      {
//        commentaryGUIupdating--;
        return;
      }
    commentaryGUIupdating++;

    commentaryScrollPane.setFocusable(enabled);
    commentaryTextArea.setFocusable(enabled);
    commentaryTextArea.setEditable(enabled);
    if (!enabled)
      commentaryTextArea.setText("");

    commentaryGUIupdating--;
  }

/*------------------------------------------------------------------------
Method:  void loadCommentaryText(Event e)
Purpose: Load text in commentary area from musical Event
Parameters:
  Input:  Event e - event source for commentary text
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void loadCommentaryText(Event e)
  {
    if (commentaryGUIupdating>0)
      {
//        commentaryGUIupdating--;
        return;
      }
    commentaryGUIupdating++;

    String newText=e.getEdCommentary();
    if (newText==null)
      newText="";
    commentaryTextArea.setText(newText);

    commentaryGUIupdating--;
  }

/*------------------------------------------------------------------------
Method:  JToolBar createMainToolBar()
Purpose: Create main tool bar beneath menu and other GUIs for editor-specific
         functions
Parameters:
  Input:  -
  Output: -
  Return: tool bar
------------------------------------------------------------------------*/

  /* note-value button options */
  boolean flagged_semiminima;
  int     selectedNVButton;

  JButton[] NoteValueButtons;
  JButton[] RestValueButtons;
  JButton[] ClefButtons;
  JButton[] MiscButtons;
  JButton[] MensButtons;

  JComboBox variantVersionsBox;

  protected JToolBar createMainToolBar() throws Exception
  {
    JToolBar mtb=new JToolBar();
    mtb.setFloatable(false);
    mtb.setFocusable(false);

    /* note-value buttons */
    mtb.setLayout(new GridBagLayout());
    mtb.setAlignmentY(java.awt.Component.LEFT_ALIGNMENT);
    mtb.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    GridBagConstraints cic=new GridBagConstraints();
    cic.anchor=GridBagConstraints.WEST;
    cic.weightx=0;

    NoteValueButtons=new JButton[NVButtonVals.length];
    for (int i=0; i<NVButtonVals.length; i++)
      {
        NoteValueButtons[i]=new JButton();
        NoteValueButtons[i].setMargin(new Insets(1,1,1,1));
        NoteValueButtons[i].setIcon(NVIcons_dark[i]);
        NoteValueButtons[i].setToolTipText(NoteEvent.NoteTypeNames[NVButtonVals[i]]);
        NoteValueButtons[i].setFocusable(false);
        NoteValueButtons[i].addActionListener(this);

        cic.gridx=i%4; cic.gridy=(int)i/4; mtb.add(NoteValueButtons[i],cic);
      }
    flagged_semiminima=false;

    selectedNVButton=3; /* Semibrevis */
    NoteValueButtons[selectedNVButton].setIcon(NVIcons_light[selectedNVButton]);

    mtb.addSeparator();

    /* rest-value buttons */
    RestValueButtons=new JButton[NVButtonVals.length];
    for (int i=0; i<NVButtonVals.length; i++)
      {
        RestValueButtons[i]=new JButton();
        RestValueButtons[i].setMargin(new Insets(1,1,1,1));
        RestValueButtons[i].setIcon(RVIcons_light[i]);
        RestValueButtons[i].setToolTipText(NoteEvent.NoteTypeNames[NVButtonVals[i]]+" rest");
        RestValueButtons[i].setFocusable(false);
        RestValueButtons[i].addActionListener(this);

        cic.gridx=5+i%4; cic.gridy=(int)i/4; mtb.add(RestValueButtons[i],cic);
      }

    mtb.addSeparator();

    /* clef buttons */
    ClefButtons=new JButton[ClefButtonVals.length];
    for (int i=0; i<ClefButtonVals.length; i++)
      {
        ClefButtons[i]=new JButton();
        ClefButtons[i].setMargin(new Insets(1,1,1,1));
        ClefButtons[i].setIcon(ClefIcons[i]);
        ClefButtons[i].setToolTipText("Clef: "+Clef.ClefNames[ClefButtonVals[i]]);
        ClefButtons[i].setFocusable(false);
        ClefButtons[i].addActionListener(this);

/*        if (i==ClefButtonVals.length-1)
          cic.weightx=1;*/
        cic.gridx=10+i%3; cic.gridy=(int)i/3; mtb.add(ClefButtons[i],cic);
      }

    cic.gridx++;
    mtb.addSeparator();

    /* misc buttons */
    int startx=cic.gridx+1;
    MiscButtons=new JButton[MiscButtonVals.length];
    for (int i=0; i<MiscButtonVals.length; i++)
      {
        MiscButtons[i]=new JButton();
        MiscButtons[i].setMargin(new Insets(1,1,1,1));
        MiscButtons[i].setIcon(MiscIcons[i]);
        MiscButtons[i].setToolTipText(MiscButtonNames[i]);
        MiscButtons[i].setFocusable(false);
        MiscButtons[i].addActionListener(this);

        cic.gridx=startx+(int)i/2; cic.gridy=i%2; mtb.add(MiscButtons[i],cic);
        mtb.add(MiscButtons[i],cic);
      }

    cic.gridx++;
    mtb.addSeparator();

    /* mensuration buttons */
    startx=cic.gridx+1;
    MensButtons=new JButton[MensButtonVals.length];
    for (int i=0; i<MensButtonVals.length; i++)
      {
        MensButtons[i]=new JButton();
        MensButtons[i].setMargin(new Insets(1,1,1,1));
        MensButtons[i].setIcon((i==MENS_BUTTON_DOT || i==MENS_BUTTON_STROKE) ? 
          MensIcons_dark[i] : MensIcons_light[i]);
        MensButtons[i].setToolTipText("Mensuration: "+MensButtonNames[i]);
        MensButtons[i].setFocusable(false);
        MensButtons[i].addActionListener(this);

        cic.gridx=startx+(int)i/2; cic.gridy=i%2; mtb.add(MensButtons[i],cic);
        mtb.add(MensButtons[i],cic);
      }

    cic.gridx++;
    mtb.addSeparator();


    /* RIGHT SIDE */

    cic.weightx=1; cic.anchor=GridBagConstraints.EAST;

    PlayButton=new JButton("PLAY");
    PlayButton.addActionListener(this);
    cic.gridx++; cic.gridy=0; mtb.add(PlayButton,cic);

    cic.gridx++;
    mtb.addSeparator();

    cic.weightx=0; cic.gridx++; cic.gridy=0; mtb.add(createTBZoomControl(),cic);

    JLabel versionsBoxLabel=new JLabel("Version: ");
    variantVersionsBox=null;
    initVariantVersionsBox();

    Box versionsComboGroup=Box.createHorizontalBox();
    versionsComboGroup.add(versionsBoxLabel);
    versionsComboGroup.add(variantVersionsBox);

    cic.weightx=0; cic.anchor=GridBagConstraints.EAST;
    cic.gridy=1; mtb.add(versionsComboGroup,cic);

    /* ------------------------- SEPARATE FRAMES ------------------------- */

    /* modern text editor */
    textEditorFrame=new TextEditorFrame(this);

    /* event editor - must first create modern text editor */
    createEventEditorFrame();

    return mtb;
  }

/*------------------------------------------------------------------------
Method:  void toggleFlaggedSemiminima()
Purpose: Toggle between colored and flagged semiminima style on toolbar
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void toggleFlaggedSemiminima()
  {
    /* swap icon images */
    ImageIcon TMP_light=NVIcons_light[NVB_SEMIMINIMA],
              TMP_dark= NVIcons_dark[NVB_SEMIMINIMA];
    NVIcons_light[NVB_SEMIMINIMA]=OtherSM_light;
    NVIcons_dark[NVB_SEMIMINIMA]= OtherSM_dark;
    OtherSM_light=TMP_light;
    OtherSM_dark= TMP_dark;

    /* update button and flag */
    NoteValueButtons[NVB_SEMIMINIMA].setIcon(selectedNVButton==NVB_SEMIMINIMA ? NVIcons_light[NVB_SEMIMINIMA] :
                                                                                NVIcons_dark[NVB_SEMIMINIMA]);
    flagged_semiminima=!flagged_semiminima;
  }

  public boolean useFlaggedSemiminima()
  {
    return flagged_semiminima;
  }

/*------------------------------------------------------------------------
Method:  void selectNVButton(int bnum)
Purpose: Switch selected note-value button
Parameters:
  Input:  int bnum - number of button to be selected
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void selectNVButton(int bnum)
  {
    if (bnum==selectedNVButton)
      return;
    if (bnum>=NVButtonVals.length)
      bnum=NVButtonVals.length-1;
    else if (bnum<0)
      bnum=0;
    NoteValueButtons[selectedNVButton].setIcon(NVIcons_dark[selectedNVButton]);
    selectedNVButton=bnum;
    NoteValueButtons[selectedNVButton].setIcon(NVIcons_light[selectedNVButton]);
  }

  /* convert note type to button number */
  public int NTtoBNum(int nt)
  {
    for (int i=0; i<NVButtonVals.length; i++)
      if (nt==NVButtonVals[i])
        return i;
    return -1;
  }

/*------------------------------------------------------------------------
Method:  int getSelectedNoteVal()
Purpose: Return currently selected note value (for adding notes)
Parameters:
  Input:  -
  Output: -
  Return: selected note value
------------------------------------------------------------------------*/

  public int getSelectedNoteVal()
  {
    return NVButtonVals[selectedNVButton];
  }

/*------------------------------------------------------------------------
Method:  void setSectionNum(int snum)
Purpose: Update GUI to reflect section number of cursor
Parameters:
  Input:  int snum - cursor's current section number
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setSectionNum(int snum)
  {
    sectionAttribsFrame.setSectionNum(snum);
  }

  public void updateSectionGUI(int snum)
  {
    sectionAttribsFrame.updateSectionGUI(snum);
  }

/*------------------------------------------------------------------------
Method:  void createEventEditorFrame()
Purpose: Create window for editing individual event info
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  JDialog eventEditorFrame;

  /* event info panel */
  JPanel eventInfoPanel;
  JLabel eventTypeLabel;
  JPanel eventVariPanel;

  /* proportion fields / event attributes */
  JPanel   proportionPanel;
  JLabel   proportionLabel;
  JSpinner eventProportion1,eventProportion2;

  JCheckBox eventEditorialCheckBox,
            eventErrorCheckBox;

  /* note info view/editor */
  NoteInfoPanel noteInfoPanel;

  /* modern key signature view/editor */
  ModernKeySigPanel modernKeySigPanel;

  /* mensuration editor */
  MensurationChooser mensurationPanel;

  /* coloration chooser */
  ColorationChooser colorationPanel;

  /* text annotation editor */
  JPanel     annotationPanel;
  JTextField annotationTextField;

  static String NO_EVENT_LABEL=   "No event selected",
                MULTI_EVENT_LABEL=" events selected";

  void createEventEditorFrame()
  {
    eventEditorFrame=new JDialog(this,"Event information",false);
//    eventEditorFrame.setUndecorated(true);
    Container eecp=eventEditorFrame.getContentPane();
    eecp.setLayout(new BoxLayout(eecp,BoxLayout.Y_AXIS));

    JPanel topPanel=new JPanel();
    topPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder(""),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    topPanel.setLayout(new BoxLayout(topPanel,BoxLayout.X_AXIS));
    eventInfoPanel=new JPanel();
    eventTypeLabel=new JLabel(NO_EVENT_LABEL);
    eventTypeLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    eventInfoPanel.add(eventTypeLabel);

    /* event proportion fields */
    eventProportion1=new JSpinner(new SpinnerNumberModel(1,1,999,1));
    eventProportion2=new JSpinner(new SpinnerNumberModel(1,1,999,1));
    JPanel proportionNumberPanel=new JPanel();
    proportionNumberPanel.add(eventProportion1);
    proportionNumberPanel.add(eventProportion2);
    proportionNumberPanel.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
    proportionPanel=new JPanel(new java.awt.GridLayout(1,0));
    proportionLabel=new JLabel();
    proportionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    proportionPanel.add(proportionLabel);
    proportionPanel.add(proportionNumberPanel);

    eventProportion1.setEnabled(false);
    eventProportion2.setEnabled(false);
    eventProportion1.addChangeListener(this);
    eventProportion2.addChangeListener(this);

    eventInfoPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    proportionPanel.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
    topPanel.add(eventInfoPanel);
    topPanel.add(Box.createGlue());
    topPanel.add(proportionPanel);

    JPanel bottomPanel=new JPanel();
    bottomPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder(""),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    bottomPanel.setLayout(new BoxLayout(bottomPanel,BoxLayout.Y_AXIS));

    Box eventAttribsBox=Box.createHorizontalBox();
    eventEditorialCheckBox=new JCheckBox("Editorial");
    eventErrorCheckBox=new JCheckBox("Error");
    eventAttribsBox.add(eventEditorialCheckBox);
    eventAttribsBox.add(eventErrorCheckBox);
    eventAttribsBox.add(Box.createHorizontalGlue());

    eventEditorialCheckBox.setEnabled(false);
    eventErrorCheckBox.setEnabled(false);
    eventEditorialCheckBox.addActionListener(this);
    eventErrorCheckBox.addActionListener(this);

    bottomPanel.add(eventAttribsBox);
    eventVariPanel=new JPanel();
    bottomPanel.add(eventVariPanel);

    /* individual editors which can appear in bottom panel */
    createNoteInfoPanel();
    createModernKeySigPanel();
    createMensurationChooser();
    createColorationChooser();
    createAnnotationEditor();

    /* temporarily add largest editor panel to get size */
    eventVariPanel.add(mensurationPanel);

    eecp.add(topPanel);
    eecp.add(bottomPanel);

    /* pack and play with sizes */
    eventEditorFrame.pack();
    Dimension els=new Dimension(eventTypeLabel.getSize());
    els.width+=20;
    eventTypeLabel.setPreferredSize(els);
    eventVariPanel.setPreferredSize(eventVariPanel.getSize());

    eventEditorFrame.pack();
    eventEditorFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            setEditMenuDisplayEventEditor(false);
          }
        });
    updateEventEditor();
  }

/*------------------------------------------------------------------------
Method:  void setEventEditorLocation()
Purpose: Position event editor relative to parent frame
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setEventEditorLocation()
  {
    /* position relative to main frame */
    int eex=getLocation().x,
        eey=getLocation().y+getSize().height,
        eeWidth=eventEditorFrame.getSize().width,
        eeHeight=eventEditorFrame.getSize().height;
    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();

    if (eey+eeHeight>screenSize.height)
      {
        /* if too low on screen, position to right of main window */
        eey=getLocation().y;
        eex+=getSize().width;
      }
    if (eex+eeWidth>screenSize.width)
      eex=screenSize.width-eeWidth;

    eventEditorFrame.pack();
    eventEditorFrame.setLocation(eex,eey);
//    eventEditorFrame.setVisible(true);
    toFront();
  }

  public void setEditingOptionsLocation()
  {
    editingOptionsFrame.setLocation(eventEditorFrame.getLocation().x+eventEditorFrame.getSize().width,
                                    eventEditorFrame.getLocation().y);
  }

/*------------------------------------------------------------------------
Method:  void disableAllEventEditors()
Purpose: Mark all individual event editor types as disabled
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  boolean noteInfoPanelEnabled=false,
          modernKeySigPanelEnabled=false,
          mensurationChooserEnabled=false,
          colorationChooserEnabled=false,
          annotationEditorEnabled=false,
          allDisabled=false;

  void disableAllEventEditors()
  {
    eventVariPanel.removeAll();
    noteInfoPanelEnabled=false;
    modernKeySigPanelEnabled=false;
    mensurationChooserEnabled=false;
    colorationChooserEnabled=false;
    annotationEditorEnabled=false;
    disableNoteInfoPanel();
    disableModernKeySigPanel();
    disableProportionPanel();
    disableEventAttributesGUI();
    textEditorFrame.disableSetSyllable();
    textEditorFrame.disableRemoveSyllable();
    changeCommentaryTextAreaStatus(false);
    allDisabled=true;
  }

/*------------------------------------------------------------------------
Method:  void clipboard[Copy|Cut|Paste]()
Purpose: Clipboard functions based on current cursor position and
         highlighted items
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void clipboardCopy()
  {
    EditScr.clipboardCopy();
    for (MusicWin ew : MusicWin.fileWindows)
      if (ew instanceof EditorWin)
        ((EditorWin)ew).EditMenuPaste.setEnabled(ScoreEditorCanvas.clipboard!=null);
  }

  protected void clipboardCut()
  {
    EditScr.clipboardCut();
    for (MusicWin ew : MusicWin.fileWindows)
      if (ew instanceof EditorWin)
        ((EditorWin)ew).EditMenuPaste.setEnabled(ScoreEditorCanvas.clipboard!=null);
  }

  protected void clipboardPaste()
  {
    EditScr.clipboardPaste();
  }

/*------------------------------------------------------------------------
Method:  void updateEventEditor([Event e|int ne])
Purpose: Set event editor to reflect state of currently highlighted events
Parameters:
  Input:  Event e - event to edit
          int ne  - number of highlighted events
  Output: -
  Return: -
------------------------------------------------------------------------*/

  /* nothing highlighted */
  protected void updateEventEditor()
  {
    if (allDisabled)
      return;
    disableAllEventEditors();
    eventTypeLabel.setText(NO_EVENT_LABEL);
    eventEditorFrame.pack();
    eventEditorFrame.repaint();

    EditMenuCut.setEnabled(false);
    EditMenuDelete.setEnabled(false);
    updateToolbarMens(null);
  }

  /* multiple events highlighted */
  protected void updateEventEditor(int ne)
  {
    disableAllEventEditors();
    eventTypeLabel.setText(ne+MULTI_EVENT_LABEL);
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    allDisabled=false;

    EditMenuCut.setEnabled(true);
    EditMenuCopy.setEnabled(true);
    EditMenuDelete.setEnabled(true);
    updateToolbarMens(null);
  }

  /* one event highlighted */
  protected void updateEventEditor(RenderedEvent re)
  {
    Event e=re.getEvent();

    disableAllEventEditors();
    eventTypeLabel.setText(e.getTypeName());
    enableEventAttributesGUI(e);
    loadCommentaryText(e);
    changeCommentaryTextAreaStatus(true);

    switch (e.geteventtype())
      {
        case Event.EVENT_NOTE:
          if (e.getLength()!=null)
            enableProportionPanel("Length (minims)",e.getLength());
          textEditorFrame.enableSetSyllable();
          if (((NoteEvent)e).getModernText()!=null)
            textEditorFrame.enableRemoveSyllable();
          enableNoteInfoPanel(re);
          break;
        case Event.EVENT_REST:
          if (e.getLength()!=null)
            enableProportionPanel("Length (minims)",e.getLength());
          break;
        case Event.EVENT_MODERNKEYSIGNATURE:
          enableModernKeySigPanel(((ModernKeySignatureEvent)e).getSigInfo());
          break;
        case Event.EVENT_MENS:
          enableProportionPanel("Proportion",((MensEvent)e).getTempoChange());
          enableMensurationChooser((MensEvent)e);
          updateToolbarMens((MensEvent)e);
          break;
        case Event.EVENT_PROPORTION:
          enableProportionPanel("",((ProportionEvent)e).getproportion());
          break;
        case Event.EVENT_COLORCHANGE:
          enableColorationChooser(((ColorChangeEvent)e).getcolorscheme());
          break;
        case Event.EVENT_ANNOTATIONTEXT:
          enableAnnotationEditor(((AnnotationTextEvent)e).gettext());
          break;
        case Event.EVENT_LACUNA:
          enableProportionPanel("Length (minims)",e.getLength());
          break;

        case Event.EVENT_MULTIEVENT:
          NoteEvent ne=((MultiEvent)e).getLowestNote();
          if (ne!=null)
            {
              textEditorFrame.enableSetSyllable();
              if (ne.getModernText()!=null)
                textEditorFrame.enableRemoveSyllable();
            }
          break;
      }

    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    allDisabled=false;

    EditMenuCut.setEnabled(true);
    EditMenuCopy.setEnabled(true);
    EditMenuDelete.setEnabled(true);
  }

  public void updateToolbarMens(MensEvent me)
  {
    ImageIcon dotIcon=MensIcons_dark[MENS_BUTTON_DOT],
              strokeIcon=MensIcons_dark[MENS_BUTTON_STROKE];

    if (me!=null)
      {
        if (!me.getMainSign().dotted)
          dotIcon=MensIcons_light[MENS_BUTTON_DOT];
        if (!me.getMainSign().stroke)
          strokeIcon=MensIcons_light[MENS_BUTTON_STROKE];
      }
    MensButtons[MENS_BUTTON_DOT].setIcon(dotIcon);
    MensButtons[MENS_BUTTON_STROKE].setIcon(strokeIcon);
  }

  public void setEditMenuDisplayEventEditor(boolean newval)
  {
    EditMenuDisplayEventEditor.setSelected(newval);
  }

  public void setEditMenuEditingOptions(boolean newval)
  {
    EditMenuEditingOptions.setSelected(newval);
  }

  public void toggleEditingOptionsColoration()
  {
    editingOptionsFrame.toggleColorationOption();
  }

  public void setInputColorationOn(boolean newval)
  {
    EditScr.setColorationOn(newval);
  }

/* ------------------------- SECTION WINDOW -------------------------- */

  public void setSectionsMenuDisplaySectionAttribs(boolean newval)
  {
    SectionsMenuDisplaySectionAttribs.setSelected(newval);
  }

  public void setVoiceUsedInSection(int snum,int vnum,boolean newState)
  {
    MusicSection ms=musicData.getSection(snum);
    if (newState==(ms.getVoice(vnum)!=null))
      return;

    if (newState==true)
      ms.initializeNewVoice(vnum,musicData.getVoiceData()[vnum]);
    else
      {
        ms.removeVoice(vnum);
        if (vnum==EditScr.getCurVoiceNum())
          {
            EditScr.setVoicenum(ms.getValidVoicenum(0));
            EditScr.setEventNum(0);
          }
      }

    fileModified();
    rerendermusic=true;
    repaint();
  }

/*------------------------------------------------------------------------
Method:  void [enable|disable]ProportionPanel(String s,Proportion p)
Purpose: Enable/disable proportion display/input on tool bar (for timed events
         and proportions)
Parameters:
  Input:  String s     - label for panel
          Proportion p - value to edit
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void enableProportionPanel(String s,Proportion p)
  {
    eventProportion1.setEnabled(false);
    eventProportion2.setEnabled(false);
    proportionLabel.setText(s);
    eventProportion1.setValue(new Integer(p.i1));
    eventProportion2.setValue(new Integer(p.i2));
    eventProportion1.setEnabled(true);
    eventProportion2.setEnabled(true);
  }

  void disableProportionPanel()
  {
    proportionLabel.setText(null);
    eventProportion1.setEnabled(false);
    eventProportion2.setEnabled(false);
    eventProportion1.setValue(new Integer(1));
    eventProportion2.setValue(new Integer(1));
  }

  boolean proportionPanelEnabled()
  {
    return eventProportion1.isEnabled();
  }

  void enableEventAttributesGUI(Event e)
  {
    eventEditorialCheckBox.setEnabled(false);
    eventErrorCheckBox.setEnabled(false);

    eventEditorialCheckBox.setSelected(e.isEditorial());
    eventErrorCheckBox.setSelected(e.isError());

    eventEditorialCheckBox.setEnabled(true);
    eventErrorCheckBox.setEnabled(true);
  }

  void disableEventAttributesGUI()
  {
    eventEditorialCheckBox.setEnabled(false);
    eventErrorCheckBox.setEnabled(false);
    eventEditorialCheckBox.setSelected(false);
    eventErrorCheckBox.setSelected(false);
  }

/*------------------------------------------------------------------------
Method:  void createNoteInfoPanel()
Purpose: Create and lay out note editor/display
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createNoteInfoPanel()
  {
    noteInfoPanel=new NoteInfoPanel(new MusicFont());
  }

/*------------------------------------------------------------------------
Method:  void [enable|disable]NoteInfoPanel(RenderedEvent rne)
Purpose: Enable/disable note display/input
Parameters:
  Input:  RenderedEvent rne - rendered event with values to edit
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void enableNoteInfoPanel(RenderedEvent rne)
  {
    eventVariPanel.removeAll();
    noteInfoPanel.setInfo(rne);
    eventVariPanel.add(noteInfoPanel);
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    noteInfoPanelEnabled=true;
  }

  void disableNoteInfoPanel()
  {
    eventVariPanel.removeAll();
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    noteInfoPanelEnabled=false;
  }

  boolean noteInfoPanelEnabled()
  {
    return noteInfoPanelEnabled;
  }

/*------------------------------------------------------------------------
Method:  void createModernKeySigPanel()
Purpose: Create and lay out modern key signature display/editor
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createModernKeySigPanel()
  {
    modernKeySigPanel=new ModernKeySigPanel(MusicGfx);
  }

/*------------------------------------------------------------------------
Method:  void [enable|disable]ModernKeySigPanel(ModernKeySignature sigInfo)
Purpose: Enable/disable modern key signature display/input
Parameters:
  Input:  ModernKeySignature sigInfo - values to edit
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void enableModernKeySigPanel(ModernKeySignature sigInfo)
  {
    eventVariPanel.removeAll();
    modernKeySigPanel.setInfo(sigInfo);
    eventVariPanel.add(modernKeySigPanel);
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    modernKeySigPanelEnabled=true;
  }

  void disableModernKeySigPanel()
  {
    eventVariPanel.removeAll();
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    modernKeySigPanelEnabled=false;
  }

  boolean modernKeySigPanelEnabled()
  {
    return modernKeySigPanelEnabled;
  }

/*------------------------------------------------------------------------
Method:  void createMensurationChooser()
Purpose: Create and lay out mensuration editor
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createMensurationChooser()
  {
    mensurationPanel=new MensurationChooser();

    mensurationPanel.mensOButton.addActionListener(this);
    mensurationPanel.mensCButton.addActionListener(this);
    mensurationPanel.mens2Button.addActionListener(this);
    mensurationPanel.mens3Button.addActionListener(this);
    mensurationPanel.deleteButton.addActionListener(this);
    mensurationPanel.mensDotBox.addActionListener(this);
    mensurationPanel.mensStrokeBox.addActionListener(this);
    mensurationPanel.mensReverseBox.addActionListener(this);
    mensurationPanel.mensNoScoreSigBox.addActionListener(this);
    mensurationPanel.prolatioBinaryButton.addActionListener(this);
    mensurationPanel.prolatioTernaryButton.addActionListener(this);
    mensurationPanel.tempusBinaryButton.addActionListener(this);
    mensurationPanel.tempusTernaryButton.addActionListener(this);
    mensurationPanel.modus_minorBinaryButton.addActionListener(this);
    mensurationPanel.modus_minorTernaryButton.addActionListener(this);
    mensurationPanel.modus_maiorBinaryButton.addActionListener(this);
    mensurationPanel.modus_maiorTernaryButton.addActionListener(this);
  }

/*------------------------------------------------------------------------
Method:  void [enable|disable]MensurationChooser(MensEvent me)
Purpose: Enable/disable mensuration display/input
Parameters:
  Input:  MensEvent me - event with values to edit
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void enableMensurationChooser(MensEvent me)
  {
    eventVariPanel.removeAll();
    mensurationPanel.setIndices(me);
    eventVariPanel.add(mensurationPanel);
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    mensurationChooserEnabled=true;
  }

  void disableMensurationChooser()
  {
    eventVariPanel.removeAll();
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    mensurationChooserEnabled=false;
  }

  boolean mensurationChooserEnabled()
  {
    return mensurationChooserEnabled;
  }

/*------------------------------------------------------------------------
Method:  void createColorationChooser()
Purpose: Create and lay out coloration editor
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createColorationChooser()
  {
    colorationPanel=new ColorationChooser();

    colorationPanel.PrimaryColorChooser.addActionListener(this);
    colorationPanel.PrimaryFillChooser.addActionListener(this);
    colorationPanel.SecondaryColorChooser.addActionListener(this);
    colorationPanel.SecondaryFillChooser.addActionListener(this);
  }

/*------------------------------------------------------------------------
Method:  void [enable|disable]ColorationChooser(Coloration c)
Purpose: Enable/disable coloration display/input
Parameters:
  Input:  Coloration c - value to edit
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void enableColorationChooser(Coloration c)
  {
    eventVariPanel.removeAll();
    colorationPanel.setIndices(c);
    eventVariPanel.add(colorationPanel);
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    colorationChooserEnabled=true;
  }

  void disableColorationChooser()
  {
    eventVariPanel.removeAll();
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    colorationChooserEnabled=false;
  }

  boolean colorationChooserEnabled()
  {
    return colorationChooserEnabled;
  }

/*------------------------------------------------------------------------
Method:  void createAnnotationEditor()
Purpose: Create and lay out annotation editor
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createAnnotationEditor()
  {
    annotationPanel=new JPanel();
    annotationTextField=new JTextField(20);
    annotationPanel.add(annotationTextField);

    annotationTextField.addActionListener(this);
  }

/*------------------------------------------------------------------------
Method:  void [enable|disable]AnnotationEditor(String s)
Purpose: Enable/disable text annotation display/input
Parameters:
  Input:  String s - value to edit
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void enableAnnotationEditor(String s)
  {
    eventVariPanel.removeAll();
    annotationTextField.setText(s);
    eventVariPanel.add(annotationPanel);
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    annotationEditorEnabled=true;
  }

  void disableAnnotationEditor()
  {
    eventVariPanel.removeAll();
    eventEditorFrame.pack();
    eventEditorFrame.repaint();
    annotationEditorEnabled=false;
  }

  boolean annotationEditorEnabled()
  {
    return annotationEditorEnabled;
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
    if (!proportionPanelEnabled())
      return;

    Object itemChanged=e.getSource();
    EditScr.setEventProportion(new Proportion(((Integer)(eventProportion1.getValue())).intValue(),
                                              ((Integer)(eventProportion2.getValue())).intValue()));
  }

/*------------------------------------------------------------------------
Method:  void [create|close]GeneralInfoFrame()
Purpose: Create or close panel for editing general/voice info
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  /* general/voice info editing panel */
  boolean inGeneralInfoFrame=false;

  void createGeneralInfoFrame()
  {
    generalInfoFrame=new GeneralInfoFrame(this,EditScr.getCurVoiceNum());
  }

  public void closeGeneralInfoFrame()
  {
    inGeneralInfoFrame=false;
    generalInfoFrame.closewin();
  }

/*------------------------------------------------------------------------
Method:  void [edit|save]GeneralInfo()
Purpose: Edit and apply general info changes to music data and close dialog
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void editGeneralInfo()
  {
    createGeneralInfoFrame();
    inGeneralInfoFrame=true;
    generalInfoFrame.setVisible(true);
  }

  public void saveGeneralInfo()
  {
    generalInfoFrame.saveData();
    boolean numVoicesChanged=generalInfoFrame.numVoicesChanged(),
            editorVoiceDeleted=generalInfoFrame.editorVoiceDeleted();
    int     newEditorVoiceNum=generalInfoFrame.getNewEditorVoiceNum();
    fileModified();
    closeGeneralInfoFrame();

    if (numVoicesChanged)
      {
        reinitVoiceTextAreas();

        EditScr.initdrawingparams();
        EditScr.newsize(EditScr.screensize.width,EditScr.screensize.height);
        pack();
        setSubframeLocations();
      }

    musicData.recalcAllEventParams();
    rerendermusic=true;
    if (editorVoiceDeleted)
      {
        EditScr.setVoicenum(EditScr.getCurSection().getValidVoicenum(0));
        EditScr.setEventNum(0);
      }
    else
      EditScr.setVoicenum(newEditorVoiceNum);
    repaint();
  }

/*------------------------------------------------------------------------
Method:  void editVariantVersionInfo()
Purpose: Edit variant version info in separate dialog
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  VariantVersionInfoFrame variantVersionInfoFrame=null;

  void editVariantVersionInfo()
  {
    variantVersionInfoFrame=new VariantVersionInfoFrame(this);
    variantVersionInfoFrame.setVisible(true);
  }

/*------------------------------------------------------------------------
Method:  void initVariantVersionsBox()
Purpose: Initialize combo box with list of version names
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void initVariantVersionsBox()
  {
    LinkedList<String> versionNames=new LinkedList<String>();
//    versionNames.add("Default");
    for (VariantVersionData vvd : musicData.getVariantVersions())
      versionNames.add(vvd.getID());

    boolean newBox=false;
    if (variantVersionsBox==null)
      {
        variantVersionsBox=new JComboBox();
        newBox=true;
      }
    variantVersionsBox.removeAllItems();
    for (String s : versionNames)
      variantVersionsBox.addItem(s);

    if (newBox)
      variantVersionsBox.addActionListener(this);
  }

/*------------------------------------------------------------------------
Method:  void [show|create]InsertSectionDialog()
Purpose: Bring up dialog for inserting new section
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  JDialog insertSectionDialog;

  void showInsertSectionDialog()
  {
    insertSectionDialog.setLocationRelativeTo(this);
    insertSectionDialog.setVisible(true);
  }

  JRadioButton ISMensuralButton,
               ISChantButton,
               ISTextButton;
  JButton      insertSectionOKButton,
               insertSectionCancelButton;

  void createInsertSectionDialog()
  {
    insertSectionDialog=new JDialog(this,"Insert new section",true);

    JPanel optionsPane=new JPanel();
    optionsPane.setLayout(new BoxLayout(optionsPane,BoxLayout.Y_AXIS));
    optionsPane.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Section type"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    ISMensuralButton=new JRadioButton("Mensural music",true);
    ISChantButton=new JRadioButton("Plainchant");
    ISTextButton=new JRadioButton("Text");
    ButtonGroup SectionTypeGroup=new ButtonGroup();
    SectionTypeGroup.add(ISMensuralButton);
    SectionTypeGroup.add(ISChantButton);
    SectionTypeGroup.add(ISTextButton);
    optionsPane.add(ISMensuralButton);
    optionsPane.add(ISChantButton);
    optionsPane.add(ISTextButton);

    /* action buttons */
    insertSectionOKButton=new JButton("Insert");
    insertSectionCancelButton=new JButton("Cancel");
    Box buttonPane=Box.createHorizontalBox();
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(insertSectionOKButton);
    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(insertSectionCancelButton);
    buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    /* lay out frame */
    Container gpcp=insertSectionDialog.getContentPane();
    gpcp.add(optionsPane,BorderLayout.CENTER);
    gpcp.add(buttonPane,BorderLayout.SOUTH);

    /* register listeners */
    insertSectionOKButton.addActionListener(this);
    insertSectionCancelButton.addActionListener(this);

    insertSectionDialog.pack();
  }

/*------------------------------------------------------------------------
Method:  void insertNewSection()
Purpose: Insert new section, type based on dialog selection
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void insertNewSection()
  {
    int newSectionType=MusicSection.MENSURAL_MUSIC;
    if (ISChantButton.isSelected())
      newSectionType=MusicSection.PLAINCHANT;
    else if (ISTextButton.isSelected())
      newSectionType=MusicSection.TEXT;

    EditScr.insertSection(newSectionType);
    insertSectionDialog.setVisible(false);
  }

/*------------------------------------------------------------------------
Method:  void deleteSection(int snum)
Purpose: Delete one section
Parameters:
  Input:  int snum - number of section to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteSection(int snum)
  {
    EditScr.deleteSection(snum);
  }

/*------------------------------------------------------------------------
Method:  void combineSectionWithNext()
Purpose: Combine section currently holding cursor with following section
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void combineSectionWithNext()
  {
    EditScr.combineSectionWithNext();
  }

/*------------------------------------------------------------------------
Method:  void showTextEditorFrame()
Purpose: Show panel for editing text for music
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void initVoiceTextAreas()
  {
/*    voiceTextAreasPanel.setLayout(new GridBagLayout());
    GridBagConstraints vtc=new GridBagConstraints();
    vtc.anchor=GridBagConstraints.LINE_START;
    voiceTextAreasPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Voice texts"),
      BorderFactory.createEmptyBorder(5,5,5,5)));

    Voice[] voices=musicData.getVoiceData();
    int     numvoices=voices.length;
    voiceTextAreas=new JTextPane[numvoices];
    for (int i=0; i<numvoices; i++)
      {
        JTextPane   curTP=new JTextPane();
        JScrollPane curSP=new JScrollPane(curTP);
        curTP.setEditable(false);
        curSP.setPreferredSize(new Dimension(350,40));
        curSP.setMinimumSize(new Dimension(10,10));
        JLabel curL=new JLabel(voices[i].getName());
        curL.setLabelFor(curSP);
        curL.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        vtc.gridx=0; vtc.gridy=i; voiceTextAreasPanel.add(curL,vtc);
        vtc.gridx=1; vtc.gridy=i; voiceTextAreasPanel.add(curSP,vtc);

        voiceTextAreas[i]=curTP;
      }*/
  }

  public void reinitVoiceTextAreas()
  {
/*    voiceTextAreasPanel.removeAll();
    initVoiceTextAreas();
    textEditorFrame.pack();*/
  }

  void showTextEditorFrame()
  {
    /* position relative to parent frame */
    int tex=getLocation().x+getSize().width,
        tey=getLocation().y,
        teWidth=textEditorFrame.getSize().width,
        teHeight=textEditorFrame.getSize().height;
    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();

    if (tex+teWidth>screenSize.width)
      tex=screenSize.width-teWidth;
    if (tey+teHeight>screenSize.height)
      tey=screenSize.height-teHeight;

    textEditorFrame.setLocation(tex,tey);
    textEditorFrame.setVisible(true);
  }

  void closeTextEditorFrame()
  {
    textEditorFrame.setVisible(false);
  }

  boolean textEditorFrameEnabled()
  {
    return textEditorFrame.isVisible();
  }

/*------------------------------------------------------------------------
Method:  void addOriginalText(String t)
Purpose: Insert string as new OriginalText event
Parameters:
  Input:  String t - text to insert
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addOriginalText(String t)
  {
    EditScr.addOriginalText(t);
  }

/*------------------------------------------------------------------------
Method:  void setNoteSyllable(String t,boolean wordEnd)
Purpose: Set text syllable on currently highlighted note
Parameters:
  Input:  String t        - syllable text
          boolean wordEnd - word end?
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setNoteSyllable(String t,boolean wordEnd)
  {
    EditScr.setNoteSyllable(t,wordEnd);
    highlightNextNote();
  }

/*------------------------------------------------------------------------
Method:  void highlight[Next|Previous]Note()
Purpose: Highlight next or previous note event from current cursor position
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void highlightNextNote()
  {
    int snum=EditScr.getCurSectionNum(),
        vnum=EditScr.getCurVoiceNum(),
        eventnum=EditScr.getCurEventNum();

    if (EditScr.oneItemHighlighted())
      eventnum++;
    int nextnenum=EditScr.getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,eventnum,1);

    if (nextnenum==-1)
      EditScr.moveCursor(snum,vnum,eventnum); /* no note, just move cursor */
    else
      EditScr.highlightOneItem(snum,vnum,nextnenum);
  }

  void highlightPreviousNote()
  {
    int snum=EditScr.getCurSectionNum(),
        vnum=EditScr.getCurVoiceNum(),
        eventnum=EditScr.getCurEventNum(),
        nextnenum=EditScr.getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
    if (nextnenum==-1)
      EditScr.moveCursor(snum,vnum,eventnum); /* no note, just move cursor */
    else
      EditScr.highlightOneItem(snum,vnum,nextnenum);
  }

/*------------------------------------------------------------------------
Method:  void loadVoiceNamesInComboBox(JComboBox cb)
Purpose: Initialize combo box with names of voices
Parameters:
  Input:  JComboBox cb - box to init
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void loadVoiceNamesInComboBox(JComboBox cb)
  {
    for (Voice v : musicData.getVoiceData())
      cb.addItem(v.getName());
  }

/*------------------------------------------------------------------------
Method:  String voice[Orig|Mod]TextToStr(int vnum)
Purpose: Create string containing all texting in one voice (original or
         modern)
Parameters:
  Input:  int vnum - number of voice
  Output: -
  Return: String containing all text in voice
------------------------------------------------------------------------*/

  public String voiceOrigTextToStr(int vnum)
  {
    return EditScr.getMusicData().voiceOrigTextToStr(vnum);
  }

  public String voiceModTextToStr(int vnum)
  {
    return EditScr.getMusicData().voiceModTextToStr(vnum);
  }

/*------------------------------------------------------------------------
Method:  void open[Modern]TextDeleteDialog()
Purpose: Show text deletion dialog
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void openOriginalTextDeleteDialog()
  {
    new OriginalTextDeleteDialog(this);
  }

  void openModernTextDeleteDialog()
  {
    new ModernTextDeleteDialog(this);
  }

/*------------------------------------------------------------------------
Method:  void deleteOriginalText(ArrayList<VariantVersionData> versions,boolean[] voices)
Purpose: Delete original text from a given set of versions/voices
Parameters:
  Input:  ArrayList<VariantVersionData> versions - versions from which to delete
          boolean[] voices                       - voices from which to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteOriginalText(ArrayList<VariantVersionData> versions,boolean[] voices)
  {
    boolean modified=false;

    for (VariantVersionData vvd : versions)
      modified|=musicData.deleteOriginalText(vvd,voices);
    musicData.consolidateAllReadings();

    /* rerender and reset EditScr */
    if (modified)
      fileModified();
    EditScr.resetMusicData();
  }

  public void deleteModernText(boolean[] voices)
  {
    boolean modified=musicData.deleteModernText(voices);

    /* rerender and reset EditScr */
    if (modified)
      fileModified();
    EditScr.resetMusicData();
  }

/*------------------------------------------------------------------------
Method:  void setVersionTextAsDefault(VariantVersionData v)
Purpose: Set the original texting of one version as the default
Parameters:
  Input:  VariantVersionData v - version to get text from
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setVersionTextAsDefault(VariantVersionData v)
  {
    if (v.isDefault())
      return;

    boolean modified=musicData.setVersionTextAsDefault(v);
    musicData.consolidateAllReadings();

    /* rerender and reset EditScr */
    if (modified)
      fileModified();
    EditScr.resetMusicData();
  }

  void setCurrentVersionAsDefault()
  {
    VariantVersionData v=getCurrentVariantVersion();

    if (v.isDefault() ||
       !confirmAction("Copy readings from version "+v.getID()+" to default?",
                      "Confirm new default readings"))
      return;

    boolean modified=musicData.setVersionAsDefault(v);
    musicData.consolidateAllReadings();

    /* rerender and reset EditScr */
    if (modified)
      fileModified();
    EditScr.resetMusicData();
  }

/*------------------------------------------------------------------------
Method:  ViewCanvas createMusicCanvas(PieceData p,MusicFont mf,MusicWin mw,OptionSet os)
Purpose: Create music editing area
Parameters:
  Input:  PieceData p,MusicFont mf,MusicWin mw,OptionSet os - constructor params
  Output: -
  Return: editing canvas
------------------------------------------------------------------------*/

  protected ViewCanvas createMusicCanvas(PieceData p,MusicFont mf,MusicWin mw,OptionSet os)
  {
    return new ScoreEditorCanvas(p,mf,mw,os);
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
Purpose:    Check for action types in menu/tools and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void actionPerformed(ActionEvent event)
  {
    Object item=event.getSource();

    /* File Menu */
    if (item==FileMenuNew)
      newfile();
    else if (item==FileMenuOpen)
      fileChooseAndOpen();
    else if (item==FileMenuSave)
      fileSave();
    else if (item==FileMenuSaveAs)
      fileSaveAs();
    else if (item==FMExportMIDI)
      fileExportAs(FILETYPE_MIDI);
    else if (item==FMExportXML)
      fileExportAs(FILETYPE_XML);
    else if (item==FileMenuGeneratePDF)
      {
        PartsWin tmppw=new PartsWin(musicData,MusicGfx,this,true);
        String   pdfname=windowFileName.equals("Untitled score") ?
                   "untitled.pdf" :
                   "data/PDFout/"+windowFileName.replaceFirst("cmme\\.xml","pdf");
        try
          {
            new PDFCreator(tmppw.getRenderLists()).createPDF(pdfname);
          }
        catch (Exception e)
          {
            handleRuntimeError(e);
          }
        tmppw.closewin();
      }
    else if (item==FileMenuClose)
      closewin();
    else if (item==FileMenuExit)
      exitprogram();

    /* Edit Menu */
    else if (item==EditMenuCopy)
      clipboardCopy();
    else if (item==EditMenuCut)
      clipboardCut();
    else if (item==EditMenuPaste)
      clipboardPaste();

    else if (item==EditMenuSelectAll)
      EditScr.highlightAll();
    else if (item==EditMenuDelete)
      EditScr.deleteHighlightedItems();

    else if (item==EditMenuGeneralInformation)
      editGeneralInfo();
    else if (item==SectionsMenuInsertSection)
      showInsertSectionDialog();
    else if (item==SectionsMenuInsertSectionBreak)
      EditScr.splitMensuralSection();

    /* Text Menu */
    else if (item==TextMenuOpenEditor)
      showTextEditorFrame();
    else if (item==TextMenuSetCurrentAsDefault)
      setVersionTextAsDefault(getCurrentVariantVersion());
    else if (item==TextMenuDeleteOriginalText)
      openOriginalTextDeleteDialog();
    else if (item==TextMenuDeleteModernText)
      openModernTextDeleteDialog();

    /* Versions Menu */
    else if (item==VersionsMenuGeneralInfo)
      editVariantVersionInfo();
    else if (item==VersionsMenuSetVersionAsDefault)
      setCurrentVersionAsDefault();
    else if (item==VersionsMenuNewNotesWindow)
      openNewNotesWindow();
    else if (item==VersionsMenuSourceAnalysis)
      openSourceAnalysisWindow();

    /* View Menu */
    else if (item==ViewMenuViewParts)
      openPartsLayout(false);
    else if (item==ViewMenuPrintPreview)
      openPartsLayout(true);
    else if (item==ViewMenuOpenInViewer)
      openInViewer();

    else if (item==MTZoomControl.viewSizeField)
      ViewSizeFieldAction();
    else if (item==VMVSZoomOut || item==MTZoomControl.zoomOutButton)
      zoomOut();
    else if (item==VMVSZoomIn || item==MTZoomControl.zoomInButton)
      zoomIn();

    /* Insert Section dialog */
    else if (item==insertSectionOKButton)
      insertNewSection();
    else if (item==insertSectionCancelButton)
      insertSectionDialog.setVisible(false);

    /* editor frames */
    else if (item==eventEditorialCheckBox)
      EditScr.toggleEditorial();
    else if (item==eventErrorCheckBox)
      EditScr.toggleError();

    else if (mensurationChooserEnabled())
      {
        if (item==mensurationPanel.mensOButton)
          EditScr.addMensurationElementSign(MensSignElement.MENS_SIGN_O);
        else if (item==mensurationPanel.mensCButton)
          EditScr.addMensurationElementSign(MensSignElement.MENS_SIGN_C);
        else if (item==mensurationPanel.mens2Button)
          EditScr.addMensurationElementNumber(2);
        else if (item==mensurationPanel.mens3Button)
          EditScr.addMensurationElementNumber(3);
        else if (item==mensurationPanel.deleteButton)
          EditScr.deleteMensurationElement(mensurationPanel.getSelectedElementNum());
        else if (item==mensurationPanel.mensDotBox)
          EditScr.toggleMensurationDot();
        else if (item==mensurationPanel.mensStrokeBox)
          EditScr.toggleMensurationStroke();
        else if (item==mensurationPanel.mensReverseBox)
          EditScr.setMensurationSign(mensurationPanel.mensReverseBox.isSelected() ? 
            MensSignElement.MENS_SIGN_CREV : MensSignElement.MENS_SIGN_C);
        else if (item==mensurationPanel.mensNoScoreSigBox)
          EditScr.toggleMensurationNoScoreSig();
        else if (mensurationPanel.isMensurationButton(item))
          EditScr.setEventMensuration(mensurationPanel.createMensuration());
      }

    else if (colorationChooserEnabled() &&
             (item==colorationPanel.PrimaryColorChooser ||
              item==colorationPanel.PrimaryFillChooser ||
              item==colorationPanel.SecondaryColorChooser ||
              item==colorationPanel.SecondaryFillChooser))
      {
        int pci=colorationPanel.PrimaryColorChooser.getSelectedIndex(),
            pfi=colorationPanel.PrimaryFillChooser.getSelectedIndex(),
            sci=colorationPanel.SecondaryColorChooser.getSelectedIndex(),
            sfi=colorationPanel.SecondaryFillChooser.getSelectedIndex();
        if (item==colorationPanel.PrimaryColorChooser)
          sci=pci;
        else if (item==colorationPanel.PrimaryFillChooser)
          sfi=Coloration.complementaryFill(pfi);

        EditScr.setEventColoration(new Coloration(pci,pfi,sci,sfi));
      }

    else if (annotationEditorEnabled() &&
             item==annotationTextField)
      EditScr.setAnnotationText(annotationTextField.getText());

    /* editing options */
    else if (item==editingOptionsFrame.colorationTypeImperfectio)
      EditScr.setEditorColorationType(Coloration.IMPERFECTIO);
    else if (item==editingOptionsFrame.colorationTypeSesquialtera)
      EditScr.setEditorColorationType(Coloration.SESQUIALTERA);
    else if (item==editingOptionsFrame.colorationTypeMinorColor)
      EditScr.setEditorColorationType(Coloration.MINOR_COLOR);

    /* toolbar note buttons */
    for (int i=0; i<NoteValueButtons.length; i++)
      if (item==NoteValueButtons[i])
        {
          selectNVButton(i);
          return;
        }

    /* toolbar rest buttons */
    for (int i=0; i<RestValueButtons.length; i++)
      if (item==RestValueButtons[i])
        {
          if (EditScr.getHighlightBegin()==-1)
            EditScr.addRest(NVButtonVals[i]);
          else if (EditScr.oneItemHighlighted() && EditScr.getCurEvent().getEvent().geteventtype()==Event.EVENT_REST)
            EditScr.modifyNoteType(NVButtonVals[i]);
          return;
        }

    /* toolbar clef buttons */
    for (int i=0; i<ClefButtons.length; i++)
      if (item==ClefButtons[i])
        {
          EditScr.doClefAction(ClefButtonVals[i]);
          return;
        }

    /* toolbar misc buttons */
    for (int i=0; i<MiscButtons.length; i++)
      if (item==MiscButtons[i])
        {
          switch (i)
            {
              case MISC_BUTTON_DOT:
                EditScr.addDot(DotEvent.DT_Addition);
                break;
              case MISC_BUTTON_DOTDIV:
                EditScr.addDot(DotEvent.DT_Division);
                break;
            }
          return;
        }

    /* toolbar mensuration buttons */
    for (int i=0; i<MensButtons.length; i++)
      if (item==MensButtons[i])
        {
          switch (i)
            {
              case MENS_BUTTON_O:
                EditScr.doMensurationAction(MensSignElement.MENS_SIGN_O);
                break;
              case MENS_BUTTON_C:
                EditScr.doMensurationAction(MensSignElement.MENS_SIGN_C);
                break;
              case MENS_BUTTON_DOT:
                EditScr.toggleMensurationDot();
                break;
              case MENS_BUTTON_STROKE:
                EditScr.toggleMensurationStroke();
                break;
              case MENS_BUTTON_3:
                EditScr.doMensurationNumberAction(3);
                break;
              case MENS_BUTTON_2:
                EditScr.doMensurationNumberAction(2);
                break;
            }
          return;
        }

    /* toolbar versions box */
    if (item==variantVersionsBox)
      setCurrentVariantVersion(variantVersionsBox.getSelectedIndex());

    else if (item==PlayButton)
      toggleMIDIPlay();
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
    if (item==EditMenuDisplayEventEditor)
      eventEditorFrame.setVisible(EditMenuDisplayEventEditor.isSelected());
    else if (item==EditMenuEditingOptions)
      editingOptionsFrame.setVisible(EditMenuEditingOptions.isSelected());
    else if (item==editingOptionsFrame.colorationOnCheckBox)
      setInputColorationOn(editingOptionsFrame.colorationOnCheckBox.isSelected());
    else if (item==SectionsMenuDisplaySectionAttribs)
      showSectionAttribsFrame(SectionsMenuDisplaySectionAttribs.isSelected());

    super.itemStateChanged(event);
  }

/*------------------------------------------------------------------------
Method:     void [insert|remove|changed]Update(DocumentEvent e)
Implements: DocumentListener.[insert|remove|changed]Update
Purpose:    Check for changes to commentary text area and take appropriate
            action
Parameters:
  Input:  DocumentEvent e - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void insertUpdate(DocumentEvent e)
  {
    doDocChange();
  }

  public void removeUpdate(DocumentEvent e)
  {
    doDocChange();
  }

  public void changedUpdate(DocumentEvent e)
  {
  }

  /* actual change handler (for both insertion and deletion) */
  void doDocChange()
  {
    if (commentaryGUIupdating>0)
      {
//        commentaryGUIupdating--;
        return;
      }
    commentaryGUIupdating++;
    EditScr.setEventCommentary(commentaryTextArea.getText());
    commentaryGUIupdating--;
  }

/*------------------------------------------------------------------------
Method:  boolean fileSaveAs()
Purpose: Choose new file name and save score
Parameters:
  Input:  -
  Output: -
  Return: whether file saved successfully
------------------------------------------------------------------------*/

  boolean fileSaveAs()
  {
    String origWindowFileName=new String(windowFileName),
           origWindowFilePath=windowFilePath==null ? null : new String(windowFilePath);

    int fcval=getSaveFileChooser().showSaveDialog(this);

    if (fcval==JFileChooser.APPROVE_OPTION)
      try
        {
          File saveFile=saveFileChooser.getSelectedFile();

          /* make sure extension is valid */
          String fn=saveFile.getCanonicalPath();
          if (!isCMMEFilename(fn))
            {
              fn+=FILENAME_EXTENSION_CMME;
              saveFile=new File(fn);
            }

          if (doNotOverwrite(saveFile))
            return false;

          /* save */
          windowFileName=saveFile.getName();
          windowFilePath=fn;
          writeCMMEFile(saveFile);

          return true;
        }
      catch (Exception e)
        {
          displayErrorMessage("Error saving file \""+musicWinFileChooser.getSelectedFile().getName()+"\":\n"+e,"File not saved");

//          System.err.println("Error saving "+musicWinFileChooser.getSelectedFile().getName());
//          e.printStackTrace();

          windowFileName=origWindowFileName;
          windowFilePath=origWindowFilePath;
        }

    return false;
  }

/*------------------------------------------------------------------------
Method:  boolean fileSave()
         void writeCMMEFile(File f)
Purpose: Save current score into file
Parameters:
  Input:  File f - file to save into
  Output: -
  Return: whether file saved successfully
------------------------------------------------------------------------*/

  synchronized boolean fileSave()
  {
    if (windowFilePath==null)
      return fileSaveAs();
    else
      try
        {
          writeCMMEFile(new File(windowFilePath));
        }
      catch (Exception e)
        {
//          System.err.println("Error saving "+windowFilePath);
//          e.printStackTrace();
//displayErrorMessage("Error saving "+windowFilePath+":\n"+e,"File not saved");
          handleRuntimeError(e);
          return false;
        }

    return true;
  }

  synchronized void writeCMMEFile(File f) throws Exception
  {
    CMMEParser.outputPieceData(musicData,new FileOutputStream(f));
    addCMMETitle(windowFileName);
    modified=false;
  }

/*------------------------------------------------------------------------
Method:  void fileModified()
Purpose: Update GUI to reflect modification of the current file from its
         last saved state
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  synchronized void fileModified()
  {
    if (modified)
      return;
    modified=true;
    setTitle(getTitle()+" (modified)");
  }

/*------------------------------------------------------------------------
Method:    void unregisterMenuListeners()
Overrides: Gfx.MusicWin.unregisterMenuListeners
Purpose: Remove all action/item/etc listeners when disposing of window
         resources
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void unregisterListeners()
  {
    WindowFocusListener wl[]=getListeners(WindowFocusListener.class);
    for (int i=0; i<wl.length; i++)
      removeWindowFocusListener(wl[i]);

    super.unregisterListeners();
  }

  protected void unregisterToolListeners()
  {
    FileMenuNew.removeActionListener(this);
    FileMenuOpen.removeActionListener(this);
    FileMenuSave.removeActionListener(this);
    FileMenuSaveAs.removeActionListener(this);
    FMExportMIDI.removeActionListener(this);
    FMExportXML.removeActionListener(this);
    FileMenuGeneratePDF.removeActionListener(this);
    FileMenuClose.removeActionListener(this);
    FileMenuExit.removeActionListener(this);

    EditMenuCopy.removeActionListener(this);
    EditMenuCut.removeActionListener(this);
    EditMenuPaste.removeActionListener(this);
    EditMenuSelectAll.removeActionListener(this);
    EditMenuDelete.removeActionListener(this);
    EditMenuGeneralInformation.removeActionListener(this);
    EditMenuDisplayEventEditor.removeItemListener(this);
    EditMenuEditingOptions.removeItemListener(this);
    SectionsMenuInsertSectionBreak.removeActionListener(this);
    SectionsMenuInsertSection.removeActionListener(this);
    SectionsMenuDisplaySectionAttribs.removeActionListener(this);
    TextMenuOpenEditor.removeActionListener(this);
    TextMenuSetCurrentAsDefault.removeActionListener(this);
    TextMenuDeleteOriginalText.removeActionListener(this);
    TextMenuDeleteModernText.removeActionListener(this);

    for (int i=0; i<VMVSnumItems.length; i++)
      VMVSnumItems[i].removeActionListener(VSListener);
    for (int i=0; i<VMBSItems.length; i++)
      VMBSItems[i].removeActionListener(VMBSListener);
    VMVSZoomOut.removeActionListener(this);
    VMVSZoomIn.removeActionListener(this);
    VMTOrigText.removeActionListener(this);
    VMTModText.removeActionListener(this);
    VMTBothText.removeActionListener(this);
    ViewMenuUsemodernclefs.removeItemListener(this);
    ViewMenuDisplayEditorialAccidentals.removeItemListener(this);
    ViewMenuModernAccidentalSystem.removeItemListener(this);
    ViewMenuDisplayallnewlineclefs.removeItemListener(this);
    ViewMenuDisplayligbrackets.removeItemListener(this);
    ViewMenuEdCommentary.removeItemListener(this);
    ViewMenuViewParts.removeActionListener(this);
    ViewMenuPrintPreview.removeActionListener(this);
    ViewMenuOpenInViewer.removeActionListener(this);
    VersionsMenuGeneralInfo.removeActionListener(this);
    VersionsMenuSetVersionAsDefault.removeActionListener(this);
    VersionsMenuNewNotesWindow.removeActionListener(this);
    VersionsMenuSourceAnalysis.removeActionListener(this);

    for (int i=0; i<NoteValueButtons.length; i++)
      NoteValueButtons[i].removeActionListener(this);
    for (int i=0; i<RestValueButtons.length; i++)
      RestValueButtons[i].removeActionListener(this);
    for (int i=0; i<ClefButtons.length; i++)
      ClefButtons[i].removeActionListener(this);
    for (int i=0; i<MiscButtons.length; i++)
      MiscButtons[i].removeActionListener(this);
    for (int i=0; i<MensButtons.length; i++)
      MensButtons[i].removeActionListener(this);
    MTZoomControl.removeListeners();
    variantVersionsBox.removeActionListener(this);
    PlayButton.removeActionListener(this);

    for (WindowListener wl : eventEditorFrame.getListeners(WindowListener.class))
      removeWindowListener(wl);
    editingOptionsFrame.removeListeners();
    sectionAttribsFrame.unregisterListeners();

    eventProportion1.removeChangeListener(this);
    eventProportion2.removeChangeListener(this);
    eventEditorialCheckBox.removeActionListener(this);
    eventErrorCheckBox.removeActionListener(this);
    commentaryTextArea.getDocument().removeDocumentListener(this);

    mensurationPanel.mensOButton.removeActionListener(this);
    mensurationPanel.mensCButton.removeActionListener(this);
    mensurationPanel.mens2Button.removeActionListener(this);
    mensurationPanel.mens3Button.removeActionListener(this);
    mensurationPanel.deleteButton.removeActionListener(this);
    mensurationPanel.mensDotBox.removeActionListener(this);
    mensurationPanel.mensStrokeBox.removeActionListener(this);
    mensurationPanel.mensReverseBox.removeActionListener(this);
    mensurationPanel.mensNoScoreSigBox.removeActionListener(this);
    mensurationPanel.prolatioBinaryButton.removeActionListener(this);
    mensurationPanel.prolatioTernaryButton.removeActionListener(this);
    mensurationPanel.tempusBinaryButton.removeActionListener(this);
    mensurationPanel.tempusTernaryButton.removeActionListener(this);
    mensurationPanel.modus_minorBinaryButton.removeActionListener(this);
    mensurationPanel.modus_minorTernaryButton.removeActionListener(this);
    mensurationPanel.modus_maiorBinaryButton.removeActionListener(this);
    mensurationPanel.modus_maiorTernaryButton.removeActionListener(this);
    colorationPanel.PrimaryColorChooser.removeActionListener(this);
    colorationPanel.PrimaryFillChooser.removeActionListener(this);
    colorationPanel.SecondaryColorChooser.removeActionListener(this);
    colorationPanel.SecondaryFillChooser.removeActionListener(this);
    annotationTextField.removeActionListener(this);
    textEditorFrame.unregisterListeners();
  }

/*------------------------------------------------------------------------
Method:    boolean closewin()
Overrides: Gfx.MusicWin.closewin
Purpose:   Close window and dependents
Parameters:
  Input:  -
  Output: -
  Return: whether window was closed or not
------------------------------------------------------------------------*/

  public boolean closewin()
  {
    if (modified)
      {
        int confirm_option=JOptionPane.showConfirmDialog(this,
          "Save changes to "+windowFileName+"?",
          "File not saved",
          JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE);

        switch (confirm_option)
          {
            case JOptionPane.YES_OPTION:
              if (fileSave()==false)
                return false;
              break;
            case JOptionPane.NO_OPTION:
              break;
            case JOptionPane.CANCEL_OPTION:
              return false;
            default:
              return false;
          }
      }

    stopMIDIPlay();
    fileWindows.remove(this);
    EditScr.stopThreads();

    ArrayList<CriticalNotesWindow> openNotesWindows=new ArrayList<CriticalNotesWindow>(notesWindows);
    for (CriticalNotesWindow cnw : openNotesWindows)
      cnw.closeFrame();
    genPDFDialog.dispose();
    insertSectionDialog.dispose();
    unregisterListeners();

    if (partsWin!=null)
      partsWin.closewin();
    textEditorFrame.dispose();
    dispose();

/*    if (!exitingProgram && fileWindows.size()==0)
      {
        curWindow=null;
        newfile();
      }*/

    return true;
  }
}
