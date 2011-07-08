/*----------------------------------------------------------------------*/
/*

        Module          : Main.java

        Package         : Viewer

        Classes Included: Main,ApplicationFrame

        Purpose         : initialization/administration

        Programmer      : Ted Dumitrescu

        Date Started    : 3/15/99

Updates:
3/20/99: converted from MainApp to Main
         separated graphics classes into package Gfx
         consolidated with parsing routines
4/26/99: converted GUI to Swing
4/28/99: added multiple-window/file selection
6/24/03: moved URLs out of code body
6/25/03: support for non-network operation
2/22/05: converted FileInfo into public class PieceRecord
         replaced TXT piece index with XML (with FileInfoReader)
3/8/05:  renamed package Main to Client (other utils and forthcoming editor
         use other packages)
         switched to XML music file format
3/16/05: added threading to move file loading/MusicWin display out of swing's
         event dispatching thread
5/2/05:  renamed package Client as Viewer (for adding package Editor)
11/1/06: changed openfile to openScore, made public and added filename-based
         loading option, for external calls from javascript
         added CMME_OPT_VALIDATEXML flag so that "testing" versions do not
         necessarily require validation packages
11/2/06: removed file-table system for selecting scores (FileInfoReader);
         now application browses local file system and applet gets filenames
         from database-connected web system
6/22/07: added GZIP support for loading compressed score files

                                                                        */
/*----------------------------------------------------------------------*/

package Viewer;

/*----------------------------------------------------------------------*/
/* Imported packages */

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.net.*;
import java.util.ArrayList;
import java.util.zip.*;
import java.io.*;

import DataStruct.CMMEParser;
import DataStruct.MetaData;
import Gfx.MusicWin;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   Main
Extends: JApplet
Purpose: Contains main routines
------------------------------------------------------------------------*/

public class Main extends JApplet
{
/*----------------------------------------------------------------------*/
/* Class variables */

  private static final boolean CMME_OPT_TESTING=false,
                               CMME_OPT_VALIDATEXML=false;

  public static final String NetBaseDataDomain=CMME_OPT_TESTING ?
                                                 "test2.cmme.org" :
                                                 "www.cmme.org",
                             BaseDataDir="/data/";
  public static String       BaseDataDomain,BaseDataURL;

  static final String TitleText1=CMME_OPT_TESTING ?
                                 "CMME Score Viewer (DEBUGGING VERSION "+DataStruct.MetaData.CMME_VERSION+")" :
                                 "CMME Score Viewer (beta "+DataStruct.MetaData.CMME_VERSION+")",
                      TitleText2="Currently viewing:";
  
  public static String StatusStr="CMME System loaded";

  public static boolean inApplet=true,
                        local=false,
                        appletInited=false;

  static Main parentWin;

  static MusicWin musicWinFuncs=new MusicWin();
  static ArrayList<String> startingFilenames=null;

/*----------------------------------------------------------------------*/
/* Instance variables */

  /* GUI */
  JPanel           TitlePanel;
  JLabel           TitleLab1,TitleLab2;
  JList            WindowListDisplay;
  DefaultListModel WindowListData;
  JScrollPane      FileInfoScroller;
  JButton          BrowseButton=null;

  ArrayList musicfiles=null;
  int       curselection=-1;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  void main(String args[])
Purpose: Perform initializations as an application
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void main(String args[])
  {
    inApplet=false;
    local=true;
    startingFilenames=new ArrayList<String>();
    parsecmdline(args);

    /* set up GUI to run on swing's event-dispatching thread */
    SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          createAppletFrame();
        }
      });
  }

/*------------------------------------------------------------------------
Method:  void createAppletFrame()
Purpose: Create frame to simulate applet
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void createAppletFrame()
  {
    ApplicationFrame appletwin=new ApplicationFrame("CMME Applet");
    appletwin.setSize(500,300);
    appletwin.setVisible(true);
  }

/*------------------------------------------------------------------------
Method:  void parsecmdline(String args[])
Purpose: Parse command line
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void parsecmdline(String args[])
  {
    for (int i=0; i<args.length; i++)
      if (args[i].equals("-local"))
        local=true;
      else if (args[i].charAt(0)!='-')
        startingFilenames.add(args[i]);
      else
        usageexit();
  }

/*------------------------------------------------------------------------
Method:  void usagexit()
Purpose: Print program usage and exit
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void usageexit()
  {
    System.out.println("Usage: java Viewer.Main [-local]");
/* exit(); */
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Method:  void realinit()
Purpose: Perform initialization for both environments (applet & application)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  ButtonListener bListener;

  /* May 2011: JRE is currently weird about running applets a second time. It calls
     init() a second time, but retains objects and variables from the previous
     run. reinit() tries to deal. */
  void reinit()
  {
    /* hack: JRE 1.6.0_22 does not re-init ParserDelegator when applet is
       brought up a second time */
    DTDSetter.resetDefaultDTD();
    layoutAppletFrame();

    startingFilenames=new ArrayList<String>();
    if (inApplet && getParameter("file")!=null)
      startingFilenames.add(getParameter("file"));
    for (String fn : startingFilenames)
      openScore(fn);
  }

  void realinit()
  {
    appletInited=true;
    Main.parentWin=this;
    MetaData.CMME_OPT_TESTING=this.CMME_OPT_TESTING;
    MetaData.CMME_OPT_VALIDATEXML=this.CMME_OPT_VALIDATEXML;

    /* initialize data locations */
    String initDirectory=null;
    MusicWin.setViewerWin(this);
    if (inApplet)
      BaseDataDomain=getDocumentBase().getHost();
    else
      BaseDataDomain=NetBaseDataDomain;
    if (local)
      try
        {
          BaseDataURL="file:///"+new File(".").getCanonicalPath()+BaseDataDir;
          initDirectory=new File(".").getCanonicalPath()+BaseDataDir;
        }
      catch (Exception e)
        {
          showError("Error loading local file locations: "+e);
        }
    else
      BaseDataURL="http://"+BaseDataDomain+BaseDataDir;

    /* load XML parser */
    DataStruct.XMLReader.initparser(BaseDataURL,CMME_OPT_VALIDATEXML);

    /* initialize graphics/score windowing system */
    MusicWin.initScoreWindowing(BaseDataURL,initDirectory+"music/",inApplet);
    try
      {
        Gfx.MusicFont.loadmusicface(BaseDataURL);
      }
    catch (Exception e)
      {
        showError("Error loading font: "+e);
        e.printStackTrace();
      }

    layoutAppletFrame();

    /* open initial score file specified in applet parameter */
    if (startingFilenames==null)
      startingFilenames=new ArrayList<String>();
    if (inApplet && getParameter("file")!=null)
      startingFilenames.add(getParameter("file"));

    for (String fn : startingFilenames)
      if (local)
        openLocalScore(fn);
      else
        openScore(fn);
  }

  void layoutAppletFrame()
  {
    /* lay out applet frame */
    TitlePanel=new JPanel();
    TitlePanel.setLayout(new BorderLayout());
    TitleLab1=new JLabel(TitleText1);
    TitleLab1.setHorizontalAlignment(SwingConstants.CENTER);
    TitleLab2=new JLabel(TitleText2);
    TitleLab2.setHorizontalAlignment(SwingConstants.CENTER);
    TitlePanel.add(TitleLab1,"North");
    TitlePanel.add(TitleLab2,"South");

    createWindowListDisplay();
    FileInfoScroller=new JScrollPane(WindowListDisplay);

    Box ButtonPane=Box.createHorizontalBox();
    ButtonPane.add(Box.createHorizontalGlue());
    if (local)
      {
        BrowseButton=new JButton("Browse...");
        ButtonPane.add(BrowseButton);
        ButtonPane.add(Box.createHorizontalStrut(10));
      }
    ButtonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    if (local)
      {
        bListener=new ButtonListener();
        BrowseButton.addActionListener(bListener);
      }

    Container cp=getContentPane();
    cp.add(TitlePanel,"North");
    cp.add(FileInfoScroller,"Center");
    cp.add(ButtonPane,"South");

    addComponentListener(
      new ComponentAdapter()
        {
          public void componentShown(ComponentEvent event)
          {
            if (inApplet)
              showStatus(StatusStr);
          }
        });
  }

/*------------------------------------------------------------------------
Inner Class: ButtonListener
Implements:  ActionListener
Purpose:     Handles button events
------------------------------------------------------------------------*/

  class ButtonListener implements ActionListener
  {
/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Method:  void actionPerformed(ActionEvent event)
Purpose: Check for action types on buttons and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

    public void actionPerformed(ActionEvent event)
    {
      Object item=event.getSource();
      if (local && item==BrowseButton)
        {
          String fn=musicWinFuncs.fileChooseAndOpen();
          if (fn!=null)
            {
              fn=(new File(fn)).getName();
              insertIntoWindowList(fn);
            }
        }
    }
  }

/*------------------------------------------------------------------------
Method:  void openScore(String filename)
Purpose: Open new music window for one file - APPLET VERSION
Parameters:
  Input:  String filename - name of file (local or remote)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void openLocalScore(final String filename)
  {
//    if (musicWinFuncs.openFile(filename))
//      insertIntoWindowList(filename);
  }

  static String jscriptOpenFilename;
  static ActionListener jscriptOpenLauncher=new ActionListener()
    {
      public void actionPerformed(ActionEvent evt)
      {
        realOpenScore(jscriptOpenFilename);
      }
    };

  public void openScore(final String filename)
  {
    /* trick the security policy when this method is called from Javascript,
       otherwise the applet is not allowed to open a network connection even
       back to its originating host!!!
       the workaround: execute the score-opening code on a different thread by
       using a Timer to launch it */
    jscriptOpenFilename=new String(filename);
    javax.swing.Timer launcher=new javax.swing.Timer(50,jscriptOpenLauncher);
    launcher.setRepeats(false);
    launcher.start();
  }

  static DataStruct.PieceData tmpmusicdat;
  static void realOpenScore(final String filename)
  {
    /* load file and create window in separate thread */
    final Gfx.SwingWorker ofthread=new Gfx.SwingWorker()
    {
    public Object construct()
    {

    /* real code */
    tmpmusicdat=null;

    final Gfx.MessageWin lw=new Gfx.MessageWin("Loading, please wait...",parentWin,true);

    /* use doPrivileged() to allow Javascript-invoked calls to open network connection 
       back to the originating host */
    java.security.AccessController.doPrivileged(new java.security.PrivilegedAction()
    {
    public Object run() {
    // privileged code goes here:

    /* load music data */
    try
      {
            URL fURL=new URL(BaseDataURL+"music/"+filename+".gz");
            int flen=fURL.openConnection().getContentLength();
            GZIPInputStream zipIn=new GZIPInputStream(
              new Util.ProgressInputStream(fURL.openStream(),
                                           lw.getProgressBar(),flen,0,75));
            tmpmusicdat=new CMMEParser(zipIn,lw.getProgressBar()).piece;

      }
    catch (Exception e)
      {
        JOptionPane.showMessageDialog(parentWin,
          "Error loading "+filename+"\n\n"+e+"\n\nURL: "+BaseDataURL+"music/"+filename+".gz",
          "Error",JOptionPane.ERROR_MESSAGE);
        lw.dispose();
        if (CMME_OPT_TESTING)
          e.printStackTrace();
        return null;
      }

    return null;
    }
    });

    /* open music window */
    try
      {
        MusicWin newWin=new MusicWin(filename,tmpmusicdat);
      }
    catch (Exception e)
      {
        parentWin.showError("Error creating music window: "+e);
        e.printStackTrace();
      }

    lw.dispose();
    parentWin.insertIntoWindowList(filename);

    return null; /* not used */
    }
    }; /* end SwingWorker */

    ofthread.start();
  }

/*------------------------------------------------------------------------
Method:  void createWindowListDisplay()
Purpose: Create list for display of currently open score windows
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createWindowListDisplay()
  {
    WindowListData=new DefaultListModel();
    WindowListDisplay=new JList(WindowListData);
    WindowListDisplay.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    WindowListDisplay.setLayoutOrientation(JList.VERTICAL);
    WindowListDisplay.setVisibleRowCount(-1);
  }

/*------------------------------------------------------------------------
Method:  void insertIntoWindowList(String fn)
Purpose: Add new title to list of open windows
Parameters:
  Input:  String fn - filename to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void insertIntoWindowList(String fn)
  {
    int insertPos=0;

    for (int i=0; i<WindowListData.getSize(); i++)
      {
        String curfn=(String)WindowListData.getElementAt(i);
        if (curfn.compareTo(fn)>0) /* order alphabetically/lexically */
          break;
        else
          insertPos=i+1;
      }
    WindowListData.insertElementAt(fn,insertPos);
  }

/*------------------------------------------------------------------------
Method:  void removeFromWindowList(String fn)
Purpose: Remove title from list of open windows
Parameters:
  Input:  String fn - filename to remove
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removeFromWindowList(String fn)
  {
    int i=0;
    boolean done=false;
    while (!done)
      {
        String curfn=(String)WindowListData.getElementAt(i);
        if (curfn.equals(fn))
          {
            WindowListData.removeElementAt(i);
            done=true;
          }
        else
          {
            i++;
            done=i>=WindowListData.getSize();
          }
      }
  }

/*------------------------------------------------------------------------
Method:  void init/start()
Purpose: Perform applet initializations
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void init()
  {
    if (!appletInited)
      realinit();
    else
      reinit();
  }

  public void start()
  {
    if (!appletInited)
      realinit();
  }

/*------------------------------------------------------------------------
Method:  void exitprog()
Purpose: Clean up and get out
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void exitprog()
  {
/*      mw.dispose();*/
    destroy();
  }

  public void destroy()
  {
    Gfx.MusicWin.closeAllWindows();
    if (local)
      BrowseButton.removeActionListener(bListener);
    ComponentListener cl[]=getListeners(ComponentListener.class);
    for (int i=0; i<cl.length; i++)
      removeComponentListener(cl[i]);
//    Gfx.MusicFont.destroyMusicFace();
  }

/*------------------------------------------------------------------------
Method:  void showError(String e)
Purpose: Show error information
Parameters:
  Input:  String e - error info
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void showError(String e)
  {
    System.err.println(e);
    if (inApplet)
      showStatus(e);
    StatusStr=e;
  }
}


/*------------------------------------------------------------------------
Class:   ApplicationFrame
Extends: JFrame
Purpose: Window for application to simulate applet frame
------------------------------------------------------------------------*/

class ApplicationFrame extends JFrame
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Main mainapplet;

/*------------------------------------------------------------------------
Constructor: ApplicationFrame(String title)
Purpose:     Initialize window
Parameters:
  Input:  String title - window title
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ApplicationFrame(String title)
  {
    super(title);

    mainapplet=new Main();
    mainapplet.start();
    getContentPane().add(mainapplet,"Center");

    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            dispose();
            mainapplet.exitprog();
            System.exit(0);
          }
        });
  }
}

  class DTDSetter extends javax.swing.text.html.parser.ParserDelegator
  {
    static void resetDefaultDTD()
    {
      javax.swing.text.html.parser.ParserDelegator.setDefaultDTD();
    }
  }
