/*----------------------------------------------------------------------*/
/*

        Module          : CriticalNotesWindow

        Package         : Gfx

        Classes Included: CriticalNotesWindow

        Purpose         : Window listing (user-configurable) critical
                          apparatus

        Programmer      : Ted Dumitrescu

        Date Started    : 5/19/2008

        Updates         :
8/7/08:   removed references to current version and pre-rendered music; new
          window now re-renders based on default data (for getting default
          readings)
10/17/08: implemented variant type filter
10/18/08: added version selection controls
11/24/08: added voice selection controls
12/1/08:  moved version controls into separate class VersionsCheckBoxPanel
          (now SelectionPanel)
3/16/09:  added event handling for measure label buttons in notes list
3/17/09:  finished implementing version selection
11/2/09:  VariantReport moved to separate module

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.html.*;

import org.jgraph.*;
import org.jgraph.graph.*;
import com.jgraph.layout.*;
import com.jgraph.layout.organic.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   CriticalNotesWindow
Extends: JFrame
Purpose: Main window listing notes
------------------------------------------------------------------------*/

public class CriticalNotesWindow extends JFrame implements ActionListener,ItemListener
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  PieceData       musicData;
  MusicWin        parentWin;
  MusicFont       MusicGfx;
  float           STAFFSCALE,VIEWSCALE;

  VariantAnalysisList           varReports;
  ArrayList<VariantVersionData> versionsToReport;
  boolean[]                     voicesToReport;
  long                          varTypeFlags;

  JPanel      controlsPanel,
              notesPanel;
  JScrollPane notesScrollPane;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  String createMeasureString(RenderList rv,MeasureInfo m,int revNum)
Purpose: Construct string representing measure where one variant reading
         begins
Parameters:
  Input:  RenderList rv - rendered data for voice
          MeasureInfo m - measure in which variant start marker stands
          int revNum    - index in render list of variant start marker
  Output: -
  Return: String with measure number(s)
------------------------------------------------------------------------*/

  public static String createMeasureString(RenderList rv,MeasureInfo m,int revNum)
  {  
    int           measureNum=m.getMeasureNum()+1;
    RenderedEvent nextre=rv.getEvent(revNum+1);
    if (nextre.getmeasurenum()+1>measureNum)
      measureNum++;
    String measureLabel=String.valueOf(measureNum);

    if (measureNum==m.getMeasureNum()+1 &&
        nextre.getmusictime().greaterThanOrEqualTo(m.getEndMusicTime()))
      measureLabel+="/"+String.valueOf(measureNum+1);

    return measureLabel;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: CriticalNotesWindow(PieceData musicData,MusicWin parentWin)
Purpose:     Initialize notes window with a given piece
Parameters:
  Input:  PieceData musicData              - music data
          MusicWin parentWin               - parent window
  Output: -
------------------------------------------------------------------------*/

  public CriticalNotesWindow(PieceData musicData,MusicWin parentWin,
                             MusicFont MusicGfx,float STAFFSCALE,float VIEWSCALE)
  {
    this.musicData=musicData;
    this.parentWin=parentWin;
    this.MusicGfx=MusicGfx;
    this.STAFFSCALE=STAFFSCALE;
    this.VIEWSCALE=VIEWSCALE;

    setTitle(parentWin.getTitle()+" (Critical apparatus)");
    setIconImage(parentWin.windowIcon);
    Container contentPane=getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));

    varReports=new VariantAnalysisList(musicData,ScoreRenderer.renderSections(musicData,OptionSet.makeDEFAULT_ORIGINAL(parentWin)));
    controlsPanel=createControlsPanel();
    notesPanel=createNotesPanel(MusicGfx,STAFFSCALE,VIEWSCALE);
    notesScrollPane=new JScrollPane(notesPanel);

    setNotesPaneSize();
    notesScrollPane.getHorizontalScrollBar().setUnitIncrement(10);
    notesScrollPane.getVerticalScrollBar().setUnitIncrement(10);

    contentPane.add(controlsPanel);
    contentPane.add(notesScrollPane);

    pack();
    setLocationRelativeTo(parentWin);

    registerListeners();
  }

  void setNotesPaneSize()
  {
    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize(),
              curSize=notesPanel.getPreferredSize(),
              newSize=new Dimension(curSize);
    newSize.height=(int)(((float)newSize.height)*1.1f);
    newSize.width=(int)(((float)newSize.width)*1.1f);
    if (newSize.height>((float)screenSize.height)*0.5f)
      newSize.height=(int)(((float)screenSize.height)*0.5f);
    if (newSize.width>((float)screenSize.width)*0.75f)
      newSize.width=(int)(((float)screenSize.width)*0.75f);
    if (!newSize.equals(curSize))
      notesScrollPane.setPreferredSize(newSize);
  }

  void resetNotesPane()
  {
    for (VariantReport vr : varReports)
      vr.unregisterListeners(this);
    notesScrollPane.setViewportView(notesPanel=createNotesPanel(MusicGfx,STAFFSCALE,VIEWSCALE));              
    setNotesPaneSize();
  }

/*------------------------------------------------------------------------
Method:  JPanel create*Panel()
Purpose: Initialize individual panes within window
Parameters:
  Input:  -
  Output: -
  Return: one window section as JPanel
------------------------------------------------------------------------*/

  JRadioButton buttonAllVariants,
               buttonSubstantiveVariants,
               buttonSelectedVariants;
  JButton      buttonSaveNotes,
               buttonSourceAnalysis;
  JCheckBox[]  variantTypeCheckBoxes,
               voiceCheckBoxes;

  SelectionPanel versionsPanel;

  static final int VARTYPE_BOXES_PER_ROW=6,
                   VOICE_BOXES_PER_ROW=12;

  JPanel createControlsPanel()
  {
    JPanel panel=new JPanel();
    panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));

    /* variant category controls */
    buttonAllVariants=new JRadioButton("All variants");
    buttonSubstantiveVariants=new JRadioButton("Substantive variants");
    buttonSelectedVariants=new JRadioButton("Selected variant types:");
    buttonSelectedVariants.setSelected(true);

    ButtonGroup categoryButtonGroup=new ButtonGroup();
    categoryButtonGroup.add(buttonAllVariants);
    categoryButtonGroup.add(buttonSubstantiveVariants);
    categoryButtonGroup.add(buttonSelectedVariants);

    buttonSaveNotes=new JButton("Save critical notes...");
    buttonSourceAnalysis=new JButton("Source analysis...");
    Box specialCommandsButtons=Box.createHorizontalBox();
    specialCommandsButtons.add(buttonSaveNotes);
    specialCommandsButtons.add(Box.createHorizontalStrut(10));
    specialCommandsButtons.add(buttonSourceAnalysis);

    Box buttonPane=Box.createHorizontalBox();
    buttonPane.add(buttonAllVariants);
    buttonPane.add(Box.createHorizontalStrut(10));
//    buttonPane.add(buttonSubstantiveVariants);
//    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(buttonSelectedVariants);
    buttonPane.add(Box.createHorizontalGlue());

    if (MetaData.CMME_OPT_TESTING)
      buttonPane.add(specialCommandsButtons);

    buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

// TMP DISABLE    
    if (MetaData.CMME_OPT_TESTING)
      panel.add(buttonPane);

    /* variant type selection controls */
    variantTypeCheckBoxes=new JCheckBox[VariantReading.typeNames.length];
    int numBoxLevels=variantTypeCheckBoxes.length/VARTYPE_BOXES_PER_ROW;
    if (variantTypeCheckBoxes.length%VARTYPE_BOXES_PER_ROW!=0)
      numBoxLevels++;
    Box[] typeBoxesPanes=new Box[numBoxLevels];
    int curBox=-1;
    for (int i=0; i<variantTypeCheckBoxes.length; i++)
      {
        if (i%VARTYPE_BOXES_PER_ROW==0)
          typeBoxesPanes[++curBox]=Box.createHorizontalBox();

        variantTypeCheckBoxes[i]=new JCheckBox(VariantReading.typeNames[i]);
        if (!buttonSelectedVariants.isSelected())
          variantTypeCheckBoxes[i].setEnabled(false);
        typeBoxesPanes[curBox].add(variantTypeCheckBoxes[i]);
        typeBoxesPanes[curBox].add(Box.createHorizontalStrut(5));
      }
    variantTypeCheckBoxes[VariantReading.varIndex(VariantReading.VAR_RHYTHM)].setSelected(true);
    variantTypeCheckBoxes[VariantReading.varIndex(VariantReading.VAR_PITCH)].setSelected(true);
    variantTypeCheckBoxes[VariantReading.varIndex(VariantReading.VAR_ACCIDENTAL)].setSelected(true);
    variantTypeCheckBoxes[VariantReading.varIndex(VariantReading.VAR_COLORATION)].setSelected(true);
    variantTypeCheckBoxes[VariantReading.varIndex(VariantReading.VAR_LIGATURE)].setSelected(true);
    variantTypeCheckBoxes[VariantReading.varIndex(VariantReading.VAR_MENSSIGN)].setSelected(true);
    variantTypeCheckBoxes[VariantReading.varIndex(VariantReading.VAR_ERROR)].setSelected(true);

    if (buttonSelectedVariants.isSelected())
      varTypeFlags=VariantDisplayOptionsFrame.calcVarFlags(variantTypeCheckBoxes);
    else if (buttonAllVariants.isSelected())
      varTypeFlags=VariantReading.VAR_ALL;

    for (Box b : typeBoxesPanes)
      {
        b.add(Box.createHorizontalGlue());
        b.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        panel.add(b);
      }

    /* version selection controls */
    versionsPanel=new SelectionPanel("Versions",musicData.getVariantVersionNames(),
                                     SelectionPanel.CHECKBOX,4);
    versionsToReport=new ArrayList<VariantVersionData>(musicData.getVariantVersions());

    /* voice selection controls */
    int numVoices=musicData.getVoiceData().length;
    voicesToReport=new boolean[numVoices];
    JPanel voicesPanel=new JPanel();
    voicesPanel.setLayout(new BoxLayout(voicesPanel,BoxLayout.Y_AXIS));

    voiceCheckBoxes=new JCheckBox[numVoices];
    int numVoiceLevels=voiceCheckBoxes.length/VOICE_BOXES_PER_ROW;
    if (voiceCheckBoxes.length%VOICE_BOXES_PER_ROW!=0)
      numVoiceLevels++;

    Box[] voiceBoxesPanes=new Box[numVoiceLevels];
    curBox=-1;
    for (int i=0; i<voiceCheckBoxes.length; i++)
      {
        voicesToReport[i]=true;

        if (i%VOICE_BOXES_PER_ROW==0)
          voiceBoxesPanes[++curBox]=Box.createHorizontalBox();

        voiceCheckBoxes[i]=new JCheckBox(String.valueOf(i+1));
        voiceCheckBoxes[i].setSelected(voicesToReport[i]);
        voiceBoxesPanes[curBox].add(voiceCheckBoxes[i]);
        voiceBoxesPanes[curBox].add(Box.createHorizontalStrut(5));
      }

    for (Box b : voiceBoxesPanes)
      {
        b.add(Box.createHorizontalGlue());
        b.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        voicesPanel.add(b);
      }
    voicesPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Voices"),
      BorderFactory.createEmptyBorder(5,5,5,5)));

    /* add version and voice panels in one row */
    Box versionsVoicesPanels=Box.createHorizontalBox();
    versionsVoicesPanels.add(versionsPanel);
    versionsVoicesPanels.add(voicesPanel);
    panel.add(versionsVoicesPanels);

    panel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder(""),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    return panel;
  }

  class ReadingSet
  {
    VariantReport                  report;
    ArrayList<VariantReadingPanel> panels=new ArrayList<VariantReadingPanel>();
  }

  ArrayList<ReadingSet> displayedReadings=null;

  JPanel createNotesPanel(MusicFont MusicGfx,float STAFFSCALE,float VIEWSCALE)
  {
    JPanel panel=new JPanel();
    panel.setBackground(Color.white);
    panel.setLayout(new GridBagLayout());
    GridBagConstraints pc=new GridBagConstraints();
    pc.gridy=0;

    /* heading */
    int NUM_COLUMNS=3;
    JLabel[] headingLabels=new JLabel[NUM_COLUMNS];
    headingLabels[0]=new JLabel("Measure");
    headingLabels[1]=new JLabel("Voice");
    headingLabels[2]=new JLabel("Readings");

    for (int i=0; i<NUM_COLUMNS; i++)
      {
        headingLabels[i].setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        headingLabels[i].setForeground(Color.red);
        pc.gridx=i; panel.add(headingLabels[i],pc);
      }
    pc.gridy++;

    displayedReadings=new ArrayList<ReadingSet>();
    for (VariantReport vReport : varReports)
      if (((varTypeFlags & vReport.varFlags)>0) ||
          buttonAllVariants.isSelected())// ||
//          (vReport.varFlags==VariantReading.VAR_NONE && variantTypeCheckBoxes[0].isSelected()))
      if (voicesToReport[vReport.voiceNum-1])
      {
        ReadingSet curReadings=new ReadingSet();
        displayedReadings.add(curReadings);
        curReadings.report=vReport;

        vReport.initMeasureButton(this);
        JLabel vLabel=new JLabel("V"+vReport.voiceNum);
        vLabel.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));

        pc.gridx=0; panel.add(vReport.measureButton,pc);
        pc.gridx=1; panel.add(vLabel,pc);

        /* "default" reading */
        List<VariantVersionData> defaultVersions=vReport.varMarker.getDefaultVersions(
          musicData.getVariantVersions(),musicData.getVoice(vReport.voiceNum-1),vReport.voiceEvents);

        /* remove any versions from "default" which the user doesn't want reported */
        LinkedList<VariantVersionData> defaultRemove=new LinkedList<VariantVersionData>();
        for (VariantVersionData vvd : defaultVersions)
          if (!versionsToReport.contains(vvd) ||
              vvd.isVoiceMissing(musicData.getVoiceData()[vReport.voiceNum-1]))
            defaultRemove.add(vvd);
        for (VariantVersionData vvd : defaultRemove)
          defaultVersions.remove(vvd);
        vReport.defaultVersions=defaultVersions;

        JLabel nameLabel;
        Box    namesBox=Box.createVerticalBox();
        for (VariantVersionData vvd : defaultVersions)
          {
            nameLabel=new JLabel(vvd.getID()+":");
            nameLabel.setForeground(Color.blue);
            namesBox.add(nameLabel);
          }
        namesBox.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));

        VariantReadingPanel defaultPanel=new VariantReadingPanel(
          vReport.voiceEvents,vReport.varMarker.getDefaultListPlace(),
          vReport.renderedVoice.getClefEvents(vReport.revNum),false,
          MusicGfx,STAFFSCALE,VIEWSCALE);

        /* don't show default reading if the user has deselected all versions
           containing it */
        if (defaultVersions.size()>0)
          {
            pc.gridx++; panel.add(namesBox,pc);
            pc.gridx++; panel.add(defaultPanel,pc);

            defaultPanel.displayedVersions=defaultVersions;
            curReadings.panels.add(defaultPanel);
          }
        Box defaultNamesBox=namesBox;

        /* variant readings */
        int totalVariantsReported=0;
        JPanel lastReadingPanel=null; /* last added reading panel */
        Box    lastNamesBox=null;
        for (VariantReading vr : vReport.varMarker.getReadings())
          {
            namesBox=Box.createVerticalBox();
            int versionsReported=0;
            ArrayList<VariantVersionData> displayedVersions=new ArrayList<VariantVersionData>();
            for (VariantVersionData vvd : vr.getVersions())
              if (versionsToReport.contains(vvd))
                {
                  nameLabel=new JLabel(vvd.getID()+":");
                  nameLabel.setForeground(Color.blue);
                  namesBox.add(nameLabel);
                  displayedVersions.add(vvd);
                  versionsReported++;
                }
            namesBox.setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
            VariantReadingPanel readingPanel=new VariantReadingPanel(
              vr,
              vReport.renderedVoice.getClefEvents(vReport.revNum),vr.isError(),
              MusicGfx,STAFFSCALE,VIEWSCALE);

            if (versionsReported>0)
              {
                pc.gridx++; panel.add(namesBox,pc);
                pc.gridx++; panel.add(readingPanel,pc);

                readingPanel.displayedVersions=displayedVersions;
                curReadings.panels.add(readingPanel);

                lastNamesBox=namesBox;
                lastReadingPanel=readingPanel;
                totalVariantsReported++;
              }
          }

        if (totalVariantsReported==0 ||
            (totalVariantsReported==1 && defaultVersions.size()==0))
          {
            panel.remove(vReport.measureButton);
            panel.remove(vLabel);
            panel.remove(defaultNamesBox);
            panel.remove(defaultPanel);
            if (lastNamesBox!=null)
              {
                panel.remove(lastNamesBox);
                panel.remove(lastReadingPanel);
              }
          }
        else
          pc.gridy++;
      }

    panel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder(""),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    return panel;
  }

/*------------------------------------------------------------------------
Method:  void saveNotes()
Purpose: Output currently displayed notes list to file
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static final String DEFAULT_IMG_DIR_SUFFIX="img";

  void saveNotes()
  {
    String critNotesName=parentWin.windowFileName.replaceFirst("\\.cmme\\.xml","-notes.html");

    parentWin.critNotesFileChooser=parentWin.getCritNotesFileChooser();
    File initFile=new File(parentWin.critNotesFileChooser.getCurrentDirectory(),critNotesName);
    parentWin.critNotesFileChooser.setSelectedFile(initFile);
    int FCval=parentWin.critNotesFileChooser.showSaveDialog(this);
    if (FCval==JFileChooser.APPROVE_OPTION)
      try
        {
          File saveFile=parentWin.critNotesFileChooser.getSelectedFile();
          if (parentWin.doNotOverwrite(saveFile))
            return;

          /* save */
          String fn=saveFile.getCanonicalPath();
          if (!fn.matches(MusicWin.FILENAME_PATTERN_HTML))
            {
              fn=fn.concat(MusicWin.FILENAME_EXTENSION_HTML);
              saveFile=new File(fn);
            }

          outputNotes(new FileOutputStream(fn),
                      saveFile.getName().replaceFirst("\\"+MusicWin.FILENAME_EXTENSION_HTML,
                                                      "-"+DEFAULT_IMG_DIR_SUFFIX),
                      fn.replaceFirst("\\"+MusicWin.FILENAME_EXTENSION_HTML,
                                      "-"+DEFAULT_IMG_DIR_SUFFIX));
//          outputNotes(System.out);
        }
      catch (Exception e)
        {
          System.err.println("Error saving "+parentWin.critNotesFileChooser.getSelectedFile().getName());
          parentWin.handleRuntimeError(e);
        }
  }

  void outputNotes(OutputStream out,String imgDir,String imgDirFull) throws Exception
  {
/*    HTMLDocument notesDoc=new HTMLDocument();
    HTMLWriter writer=new HTMLWriter(new OutputStreamWriter(out),notesDoc);
    writer.write();*/
    File imgDirFile=new File(imgDirFull);
    if (!imgDirFile.isDirectory())
      imgDirFile.mkdir();

    String HTMLString=new String("<html>\n");
    HTMLString+="<table border=\"1\">\n";

    HTMLString+="<tr>\n";

    HTMLString+="<td>";
    HTMLString+="Voice";
    HTMLString+="</td>";

    HTMLString+="<td>";
    HTMLString+="Measure";
    HTMLString+="</td>\n";

    HTMLString+="<td>";
    HTMLString+="Readings";
    HTMLString+="</td>\n";

    HTMLString+="</tr>\n";

    int variantNum=1;
    for (ReadingSet rs : displayedReadings)
      {
        HTMLString+="<tr>\n";

        HTMLString+="<td>";
        HTMLString+="V"+rs.report.voiceNum;
        HTMLString+="</td>";

        HTMLString+="<td>";
        HTMLString+=rs.report.measureLabel;
        HTMLString+="</td>\n";

        int readingNum=1;
        for (VariantReadingPanel vrp : rs.panels)
          {
            HTMLString+="<td>";
            int vvdnum=0;
            for (VariantVersionData vvd : vrp.displayedVersions)
              {
                if (vvdnum++>0)
                  HTMLString+="<br/>";
                HTMLString+=vvd.getID();
              }
            HTMLString+="</td>\n";

            HTMLString+="<td>";
            String readingImgName="Var"+variantNum+"-Reading"+readingNum+".JPG",
                   readingImgPath=imgDir+"/"+readingImgName,
                   readingImgFullPath=imgDirFull+"/"+readingImgName;
            vrp.saveImgFile(readingImgFullPath);
            HTMLString+="<img src=\""+readingImgPath+"\" alt=\"\"/>";
            HTMLString+="</td>\n";

            readingNum++;
          }

        HTMLString+="</tr>\n";

        variantNum++;
      }

    HTMLString+="</table>\n";
    HTMLString+="</html>";

    out.write(HTMLString.getBytes("UTF-8"));
    out.close();
  }

/* Wow. There is some really really never-refactored code hanging out
   under this point. Mostly written in an overpriced conference hotel during
   AMS 2009 a day or two before I had to present this stuff to an airless
   roomfull of musicologists. Be forewarned.
   Man if I could rewrite this crap in Ruby. */

/*------------------------------------------------------------------------
Method:  void sourceAnalysis()
Purpose: Perform source analysis.........
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  class VarAgreementTable extends Hashtable<VariantVersionData,Hashtable<VariantVersionData,Integer>>
  {
    public void increment(VariantVersionData vv1,VariantVersionData vv2)
    {
if (this.get(vv1)==null || this.get(vv1).get(vv2)==null)
System.out.println("null: "+vv1+" -> "+vv2);
      int numAgreements=this.get(vv1).get(vv2).intValue();
      this.get(vv1).put(vv2,new Integer(numAgreements+1));
    }
  }

  class VersionAgreementCount
  {
    public VariantVersionData vv1,vv2;
    public int                numAgreements;

    public VersionAgreementCount(VariantVersionData vv1,VariantVersionData vv2,int numAgreements)
    {
      this.vv1=vv1;
      this.vv2=vv2;
      this.numAgreements=numAgreements;
    }
  }

  /* increment agreement table for sources at one variant */
  /* returns best possible agreement 'score' for this variant */
  int calcVarAgreements(VariantReport vReport,long varTypeFlags,
                         ArrayList<VariantVersionData> versionsToCheck,
                         VarAgreementTable varAgreements)
  {
    int numVersions=vReport.defaultVersions.size();

    boolean varElim[]=new boolean[vReport.varMarker.getReadings().size()];
    for (int i=0; i<varElim.length; i++)
      varElim[i]=false;

    for (VariantVersionData vv1 : vReport.defaultVersions)
      for (VariantVersionData vv2 : vReport.defaultVersions)
        varAgreements.increment(vv1,vv2);

    for (VariantReading vr : vReport.varMarker.getReadings())
      {
        for (VariantVersionData vv1 : vr.getVersions())
          if (versionsToCheck.contains(vv1))
            {
              numVersions++;
              for (VariantVersionData vv2 : vr.getVersions())
                if (versionsToCheck.contains(vv2))
                  varAgreements.increment(vv1,vv2);
            }
      }

    /* increase agreement counts for readings which are listed separately but supposed to
       be counted as non-varying (e.g., if two readings differ only in texting and we don't
       want to consider texting according to varTypeFlags, then consider them to agree) */
    int i1=0;
    for (VariantReading vr : vReport.varMarker.getReadings())
      {
        /* first: default vs. other readings */
        long vt=vr.calcVariantTypes(vReport.voiceEvents,vReport.varMarker.getDefaultListPlace());
        if ((varTypeFlags&vt)==0)
          {
            varElim[i1]=true;
            for (VariantVersionData vv1 : vReport.defaultVersions)
              for (VariantVersionData vv2 : vr.getVersions())
                if (versionsToCheck.contains(vv2))
                  varAgreements.increment(vv1,vv2);
          }

        /* now each variant reading against the others */
        for (int i2=i1+1; i2<vReport.varMarker.getReadings().size(); i2++)
          {
            VariantReading vr2=vReport.varMarker.getReadings().get(i2);
            vt=vr.calcVariantTypes(vr2.getEvents(),0);
            if ((varTypeFlags&vt)==0)
              {
                varElim[i2]=true;
                for (VariantVersionData vv1 : vr.getVersions())
                  if (versionsToCheck.contains(vv1))
                    for (VariantVersionData vv2 : vr2.getVersions())
                      if (versionsToCheck.contains(vv2))
                        varAgreements.increment(vv1,vv2);
              }
          }
        i1++;
      }

    /* ideal score = number of variations - number of source groups */
    int score=numVersions;
    for (boolean elim : varElim)
      if (!elim)
        score--;
    score--; /* default */

    return score;
  }

  LinkedList<Variation> getVars()
  {
    LinkedList<Variation> stemmaVariants=new LinkedList<Variation>();
    for (VariantReport vReport : varReports)
      if (((varTypeFlags & vReport.varFlags)>0) ||
          buttonAllVariants.isSelected())
        if (voicesToReport[vReport.voiceNum-1])
          {
            Variation var=new Variation(vReport);
            if (var.numGroups>1)  /* don't add if there's no variation between groups */
              stemmaVariants.add(var);
          }

    return stemmaVariants;
  }

  VarAgreementTable makePairCountTable()
  {
    VarAgreementTable varAgreements=new VarAgreementTable();
    for (VariantVersionData vv1 : versionsToReport)
      {
        Hashtable<VariantVersionData,Integer> oneVerAgreements=new Hashtable<VariantVersionData,Integer>();
        for (VariantVersionData vv2 : versionsToReport)
          oneVerAgreements.put(vv2,new Integer(0));
        varAgreements.put(vv1,oneVerAgreements);
      }

    return varAgreements;
  }

  class Group extends LinkedList<VariantVersionData>
  {
  }

  class PCChoice
  {
    int pairCount,lastEqual,
        sigla[];

    public PCChoice()
    {
      pairCount=0;
      lastEqual=0;
      sigla=new int[2];
    }
  }

  class Variation
  {
    VariantReport vReport;

    int   type,numGroups,numAbsent,fullValue,
          footnoteNum;
    LinkedList<Group> group;

    public Variation(VariantReport vReport)
    {
      this.vReport=vReport;
      calcGroups();
      calcNumAbsent();
    }

    void calcGroups()
    {
      boolean readingElim[]=new boolean[vReport.varMarker.getReadings().size()];
      for  (int i=0; i<readingElim.length; i++)
        readingElim[i]=false;

      group=new LinkedList<Group>();
      Group curGroup=new Group();
      for (VariantVersionData vv : vReport.defaultVersions)
        if (versionsToReport.contains(vv))
          curGroup.add(vv);

      int i1=0;
      for (VariantReading vr : vReport.varMarker.getReadings())
        {
          /* first: default vs. other readings */
          long vt=vr.calcVariantTypes(vReport.voiceEvents,vReport.varMarker.getDefaultListPlace());
          if ((varTypeFlags&vt)==0)
            readingElim[i1]=true;

          /* now each variant reading against the others */
          for (int i2=i1+1; i2<vReport.varMarker.getReadings().size(); i2++)
            {
              VariantReading vr2=vReport.varMarker.getReadings().get(i2);
              vt=vr.calcVariantTypes(vr2.getEvents(),0);
              if ((varTypeFlags&vt)==0)
                readingElim[i2]=true;
            }
          i1++;
        }

      numGroups=1;
      for (boolean elim : readingElim)
        if (!elim)
          numGroups++;
    }

    void calcNumAbsent()
    {
      int numSources=0;
      for (VariantVersionData vv : vReport.defaultVersions)
        if (versionsToReport.contains(vv))
          numSources++;
      for (VariantReading vr : vReport.varMarker.getReadings())
        for (VariantVersionData vv : vr.getVersions())
          if (versionsToReport.contains(vv))
            numSources++;

      numAbsent=versionsToReport.size()-numSources;
    }
  }

  class MSCalcAttribs
  {
    boolean disjoint,eliminated;
    String  name;
    int     maxPairCount;

    public MSCalcAttribs(VariantVersionData vv)
    {
      disjoint=true;
      maxPairCount=0;
      name=new String(vv.getID());
    }
  }

  void RapsonAnalysis()
  {
    LinkedList<Variation> variation=getVars();
    int numMSS=versionsToReport.size(),
        numVar=variation.size(),
        numPairs=(int)(numMSS*(numMSS-1)/2),bufferSize=2*numMSS+1,blank=0,
        j=0,k,maxDiagSize,maxDiagScore=0,shortFall,pointer,groupNum,
        brackets,
        buffer[][]=new int[numVar][bufferSize];
    VarAgreementTable pairCount=makePairCountTable();
    boolean stillWrong=true;
    Hashtable<VariantVersionData,MSCalcAttribs> MSattribs=new Hashtable<VariantVersionData,MSCalcAttribs>();
    char character;
    PCChoice[] pcChoice=new PCChoice[numPairs];

    for (VariantVersionData vv : versionsToReport)
      {
        MSCalcAttribs attribs=new MSCalcAttribs(vv);
        MSattribs.put(vv,attribs);
      }
    for (int i=0; i<numPairs; i++)
      pcChoice[i]=new PCChoice();

    /* go through all variations, do preliminary calculations */
    for (Variation var : variation)
      {
System.out.println("m. "+var.vReport.measureLabel+": numgroups="+var.numGroups);

        /* maximum pair count for each source */
      }
  }

  void sourceAnalysis()
  {
/*System.out.println("---begin Rapson");
    RapsonAnalysis();
System.out.println("---end Rapson");
System.out.println();*/

    /* Rapson-style analysis */
    int numSources=versionsToReport.size();

    VarAgreementTable varAgreements=new VarAgreementTable();
    for (VariantVersionData vv1 : versionsToReport)
      {
        Hashtable<VariantVersionData,Integer> oneVerAgreements=new Hashtable<VariantVersionData,Integer>();
        for (VariantVersionData vv2 : versionsToReport)
          oneVerAgreements.put(vv2,new Integer(0));
        varAgreements.put(vv1,oneVerAgreements);
      }

    int numVariantsUsed=0,
        firstIdealScore=0;
    LinkedList<VariantReport> stemmaVariants=new LinkedList<VariantReport>();
    for (VariantReport vReport : varReports)
      if (((varTypeFlags & vReport.varFlags)>0) ||
          buttonAllVariants.isSelected())
        if (voicesToReport[vReport.voiceNum-1])
          {
//System.out.println("m. "+vReport.measureLabel);
            stemmaVariants.add(vReport);
            numVariantsUsed++;

            /* do calculations of number of agreements between sources */
            firstIdealScore+=calcVarAgreements(vReport,varTypeFlags,versionsToReport,varAgreements);
          }

    /* store and sort agreements */
    ArrayList<VersionAgreementCount> sortedAgreementCounts=new ArrayList<VersionAgreementCount>();
    int i1=0;
    for (VariantVersionData vv1 : versionsToReport)
      {
        for (int i2=i1+1; i2<versionsToReport.size(); i2++)
          {
            VariantVersionData vv2=versionsToReport.get(i2);
            sortedAgreementCounts.add(new VersionAgreementCount(vv1,vv2,varAgreements.get(vv1).get(vv2).intValue()));
          }
        i1++;
      }
    Collections.sort(sortedAgreementCounts,
      new Comparator<VersionAgreementCount>()
        {
          public int compare(VersionAgreementCount c1,VersionAgreementCount c2)
          {
            if (c1.numAgreements<c2.numAgreements)
              return 1;
            else if (c1.numAgreements>c2.numAgreements)
              return -1;
            else
              return 0;
          }
        });

    /* print agreements */
System.out.println("Source agreements; total variants employed: "+numVariantsUsed);
    for (VersionAgreementCount c : sortedAgreementCounts)
      System.out.println(c.vv1.getID()+"\t<>\t"+c.vv2.getID()+"\t:\t"+c.numAgreements);

    /* construct diagram */
    int    numNodes=numSources;
    Stemma stemma=new Stemma(versionsToReport,sortedAgreementCounts,numVariantsUsed);
    stemma.idealScore=firstIdealScore;

    /* connect all sources using highest agreement counts */
    for (VersionAgreementCount vPair : sortedAgreementCounts)
      {
        StemmaNode n1=stemma.findNode(vPair.vv1);
        if (n1==null)
          {
            n1=new StemmaNode(vPair.vv1);
            stemma.nodes.add(n1);
          }
        StemmaNode n2=stemma.findNode(vPair.vv2);
        if (n2==null)
          {
            n2=new StemmaNode(vPair.vv2);
            stemma.nodes.add(n2);
          }
        stemma.makeUnorderedLink(n1,n2,vPair.numAgreements,numVariantsUsed);

        if (stemma.containsCycle())
          stemma.removeLink(n1,n2);
        else
          stemma.score+=vPair.numAgreements;
      }
System.out.println("Ideal score: "+stemma.idealScore);
System.out.println("Score without archetypes: "+stemma.score+" = "+((float)stemma.score/(float)stemma.idealScore));

    /* add inferential states (hyparchetypes) for variants which conflict with
       stemma orientation */
    int     nodesReplaced;
    boolean reset;
int tmp=0;
    do
    {
    LinkedList<NodePair> problemPairs=new LinkedList<NodePair>();
    NodeAndLinkPair singleInterruptor;
    for (VariantReport vReport : stemmaVariants)
      {
        NodePair problemPair=stemma.PrincipleOfExclusionMaintained(vReport);
        if (problemPair!=null)
          {
            NodePair existingPair=problemPair.findInList(problemPairs);
            if (existingPair==null)
              problemPairs.add(problemPair);
            else
              existingPair.count++;
          }
      }
    nodesReplaced=0;
System.out.println("Number of problem pairs: "+problemPairs.size());
    reset=false;
    for (NodePair np : problemPairs)
      if (!reset && (singleInterruptor=stemma.findSingleInterruptor(np))!=null)
        {
System.out.print("Single interruptor: "+np.n1+" -> "+singleInterruptor.n+" -> "+np.n2+": ");
          if (singleInterruptor.n.isInferential())
            {
              if (singleInterruptor.n.hasNeighbor(np.n1) &&
                  singleInterruptor.n.hasNeighbor(np.n2))
                {
int oldLinkVal1=np.n1.findLink(singleInterruptor.n).val,
    oldLinkVal2=np.n2.findLink(singleInterruptor.n).val;
                  StemmaNode newn=stemma.drawOutInferentialNode(singleInterruptor.n,np);
                  reset=true;
                  nodesReplaced++;
int newLinkVal=numVariantsUsed-np.count;
stemma.findLink(newn,singleInterruptor.n).setVal(newLinkVal);
stemma.findLink(newn,np.n1).setVal(oldLinkVal1);
stemma.findLink(newn,np.n2).setVal(oldLinkVal2);
stemma.score+=newLinkVal;

/*int newLinkVal=numVariantsUsed-np.count;
stemma.findLink(newn,singleInterruptor.n).setVal(newLinkVal);
stemma.findLink(newn,np.n1).setVal(oldLinkVal1+np.count);
stemma.findLink(newn,np.n2).setVal(oldLinkVal2+np.count);
stemma.score+=newLinkVal+np.count*2;*/

System.out.println("drawn out");
                }
else
System.out.println("no action");
            }
          else
            {
              StemmaNode newn=stemma.replaceWithInferential(singleInterruptor.n,problemPairs);
              reset=true;
              nodesReplaced++;
int newLinkVal=numVariantsUsed-np.count;
newn.links.get(0).setVal(newLinkVal);
singleInterruptor.l1.setVal(singleInterruptor.l1.val);
singleInterruptor.l2.setVal(singleInterruptor.l2.val);
stemma.score+=newLinkVal;

/*int newLinkVal=numVariantsUsed-np.count;
newn.links.get(0).setVal(newLinkVal);
singleInterruptor.l1.setVal(singleInterruptor.l1.val+np.count);
singleInterruptor.l2.setVal(singleInterruptor.l2.val+np.count);
stemma.score+=newLinkVal+np.count*2;*/

System.out.println("new inf");
            }
        }
tmp++;
if (nodesReplaced>0)
  stemma.idealScore+=numVariantsUsed;
System.out.println("new ideal score: "+stemma.idealScore);
System.out.println("new stemma score: "+stemma.score+" = "+((float)stemma.score/(float)stemma.idealScore));
    }
    while ((nodesReplaced!=0 || reset) && tmp<10);
//    while (nodesReplaced!=0);

    showGraph(stemma);
  }

  class NodeAndLinkPair
  {
    StemmaNode n;
    StemmaLink l1,l2;

    public NodeAndLinkPair(StemmaNode n,StemmaLink l1,StemmaLink l2)
    {
      this.n=n;
      this.l1=l1;
      this.l2=l2;
    }
  }

  ArrayList<StemmaFrame> stemmaFrames=new ArrayList<StemmaFrame>();

  void showGraph(Stemma stemma)
  {
    StemmaFrame sf=new StemmaFrame(this,stemma);
    stemmaFrames.add(sf);
    sf.setVisible(true);
  }

  public void stemmaFrameClosed(StemmaFrame sf)
  {
    stemmaFrames.remove(sf);
  }

  class StemmaFrame extends JFrame
  {
    CriticalNotesWindow parentWin;
    Stemma              stemma;

    JFrame rawDataFrame;

  public StemmaFrame(CriticalNotesWindow parentWin,Stemma stemma)
  {
    this.parentWin=parentWin;
    this.stemma=stemma;

    setTitle(parentWin.parentWin.getTitle()+" (Source graph)");
    setIconImage(parentWin.parentWin.windowIcon);
    Container contentPane=getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));

    if (stemma!=null)
      contentPane.add(makeStemmaPane());

    pack();
    setLocationRelativeTo(parentWin);

    rawDataFrame=makeRawDataFrame();
    rawDataFrame.setVisible(true);
  }

/*------------------------------------------------------------------------
Method:  void closeFrame()
Purpose: Close frame and clean up
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void closeFrame()
  {
    rawDataFrame.setVisible(false);
    rawDataFrame.dispose();

    setVisible(false);
    unregisterListeners();
    dispose();
    parentWin.stemmaFrameClosed(this);
  }

/*------------------------------------------------------------------------
Method:  void [un]registerListeners()
Purpose: Add and remove event listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void registerListeners()
  {
    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            closeFrame();
          }
        });
  }

  protected void unregisterListeners()
  {
    for (WindowListener w : getListeners(WindowListener.class))
      removeWindowListener(w);
  }

  JScrollPane makeStemmaPane()
  {
    /* graph test */
    GraphModel model=new DefaultGraphModel();
    GraphLayoutCache view=new GraphLayoutCache(model,new DefaultCellViewFactory());
    JGraph graph=new JGraph(model,view);
    LinkedList<StemmaLink> linksInGraph=new LinkedList<StemmaLink>();
    for (StemmaNode n : stemma.nodes)
      graph.getGraphLayoutCache().insert(n.visNode);
    for (StemmaLink l : stemma.links)
      graph.getGraphLayoutCache().insert(l.visLink);

    JGraphFacade facade = new JGraphFacade(graph); // Pass the facade the JGraph instance
    JGraphLayout layout = new WideLayout(); // Create an instance of the appropriate layout
    layout.run(facade); // Run the layout on the facade. Note that layouts do not implement the Runnable interface, to avoid confusion
    Map nested = facade.createNestedMap(true, true); // Obtain a map of the resulting attribute changes from the facade
    graph.getGraphLayoutCache().edit(nested); // Apply the results to the actual graph

    JScrollPane graphPane=new JScrollPane(graph);

    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize(),
              curSize=graph.getPreferredSize(),
              newSize=new Dimension(curSize);
    if (newSize.height>((float)screenSize.height)*0.9f)
      newSize.height=(int)(((float)screenSize.height)*0.9f);
    newSize.width*=1.1f;
    graphPane.setPreferredSize(newSize);

    return graphPane;
  }

  JFrame makeRawDataFrame()
  {
    JFrame rdf=new JFrame();

    rdf.setTitle(parentWin.parentWin.getTitle()+" (Source analysis data)");
    setIconImage(parentWin.parentWin.windowIcon);
    Container contentPane=rdf.getContentPane();
    contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));

    JLabel scoreLabel=new JLabel("Stemma Score: "+stemma.score+"/"+stemma.idealScore+
                                 " = "+(Math.round(100*stemma.score/stemma.idealScore))+"%");
    Box dataBox=Box.createHorizontalBox();
    dataBox.add(scoreLabel);

    JTable pairTable=new JTable(
      new AbstractTableModel()
        {
          public int getColumnCount() { return 3; }
          public int getRowCount()    { return stemma.sortedAgreementCounts.size(); }
          public Object getValueAt(int row,int col)
          {
            switch(col)
              {
                case 0:
                  return stemma.sortedAgreementCounts.get(row).vv1.getID();
                case 1:
                  return stemma.sortedAgreementCounts.get(row).vv2.getID();
              }
            return new Integer(stemma.sortedAgreementCounts.get(row).numAgreements);
          }
          public String getColumnName(int column)
          {
            switch(column)
              {
                case 0:
                  return "Source 1";
                case 1:
                  return "Source 2";
              }
            return "Agreement count";
          }
        });

    contentPane.add(dataBox);
    contentPane.add(new JScrollPane(pairTable));

    rdf.pack();
    rdf.setLocationRelativeTo(parentWin);

    return rdf;
  }
  }

  class WideLayout extends JGraphFastOrganicLayout
  {
    public void run(JGraphFacade graph)
    {
      super.run(graph);
      double[][] locs=graph.getLocations(graph.getVertices().toArray());
      for (int x=0; x<locs.length; x++)
        for (int y=0; y<locs[x].length; y++)
          locs[x][y]=20+locs[x][y]*2;
      graph.setLocations(graph.getVertices().toArray(),locs);
    }
  }

  void graphTest()
  {
    /* graph test */
    GraphModel model=new DefaultGraphModel();
    GraphLayoutCache view = new GraphLayoutCache(model,new DefaultCellViewFactory());
//JGraph graph=new JGraph();
    JGraph graph = new JGraph(model, view);
    DefaultGraphCell[] cells = new DefaultGraphCell[3];

cells[0] = new DefaultGraphCell(new String("Hello"));
//GraphConstants.setBounds(cells[0].getAttributes(), new Rectangle2D.Double(20,20,40,20));
GraphConstants.setGradientColor(
cells[0].getAttributes(),
Color.orange);
GraphConstants.setOpaque(cells[0].getAttributes(), true);
DefaultPort port0 = new DefaultPort();
cells[0].add(port0);

cells[1] = new DefaultGraphCell(new String("World"));
//GraphConstants.setBounds(cells[1].getAttributes(), new Rectangle2D.Double(140,140,40,20));
GraphConstants.setGradientColor(
cells[1].getAttributes(),
Color.red);
GraphConstants.setOpaque(cells[1].getAttributes(), true);
DefaultPort port1 = new DefaultPort();
cells[1].add(port1);

DefaultEdge edge = new DefaultEdge();
edge.setSource(cells[0].getChildAt(0));
edge.setTarget(cells[1].getChildAt(0));
cells[2] = edge;

int arrow = GraphConstants.ARROW_CLASSIC;
GraphConstants.setLineEnd(edge.getAttributes(), arrow);
GraphConstants.setEndFill(edge.getAttributes(), true);
graph.getGraphLayoutCache().insert(cells);

JFrame frame = new JFrame();
frame.getContentPane().add(new JScrollPane(graph));
frame.pack();
frame.setVisible(true);
  }

  class Stemma
  {
    public LinkedList<StemmaNode> nodes;
    public LinkedList<StemmaLink> links;

    ArrayList<VariantVersionData> versionsToCheck;
    public ArrayList<VersionAgreementCount> sortedAgreementCounts;
    public int                              maxLinkVal;

    public int score,idealScore;

    public Stemma(ArrayList<VariantVersionData> versionsToCheck,ArrayList<VersionAgreementCount> sortedAgreementCounts,
                  int maxLinkVal)
    {
      nodes=new LinkedList<StemmaNode>();
      links=new LinkedList<StemmaLink>();

      this.versionsToCheck=versionsToCheck;
      this.sortedAgreementCounts=sortedAgreementCounts;
      this.maxLinkVal=maxLinkVal;

      this.score=this.idealScore=0;
    }

    void resetNodesChecked()
    {
      for (StemmaNode n : nodes)
        n.cycleChecked=false;
    }

    void resetLinksChecked()
    {
      for (StemmaLink l : links)
        l.cycleChecked=false;
    }

    public boolean containsCycle()
    {
      resetNodesChecked();
      resetLinksChecked();

      for (StemmaNode n : nodes)
        if (!n.cycleChecked && containsCycle(n))
          return true;

      return false;
    }

    boolean containsCycle(StemmaNode n)
    {
      n.cycleChecked=true;
      for (StemmaLink l : n.links)
        if (!l.cycleChecked)
          {
            l.cycleChecked=true;
            StemmaNode n1=l.getOtherNode(n);
            if (n1.cycleChecked)
              return true; /* we followed a new link and hit an already-traversed node */
            if (containsCycle(n1)) /* keep traversin' */
              return true;
          }

      return false;
    }

    public StemmaNode findNode(VariantVersionData v)
    {
      for (StemmaNode n : nodes)
        if (n.versions.contains(v))
          return n;
      return null;
    }

    public StemmaLink findLink(StemmaNode n1,StemmaNode n2)
    {
      return n1.findLink(n2);
    }

    public void makeUnorderedLink(StemmaNode n1,StemmaNode n2,int val,int maxVal)
    {
      links.add(n1.makeUnorderedLink(n2,val,maxVal));
    }

    public void removeLink(StemmaNode n1,StemmaNode n2)
    {
      StemmaLink linkToRemove=n1.removeLink(n2);
      links.remove(linkToRemove);
    }

    int  infNum=1;
    char infLetter='\u03B1'; /* alpha */
    String nextInfStateName()
    {
      return String.valueOf(infLetter++);
    }

    public StemmaNode replaceWithInferential(StemmaNode n,LinkedList<NodePair> problemPairs)
    {
      StemmaNode oldn=n.turnIntoInferential(nextInfStateName());
      nodes.add(oldn);
      makeUnorderedLink(n,oldn,0,maxLinkVal);

      for (NodePair np : problemPairs)
        if (np.n1==n)
          np.n1=oldn;
        else if (np.n2==n)
          np.n2=oldn;

      return oldn;
    }

    public StemmaNode drawOutInferentialNode(StemmaNode i,NodePair np)
    {
      StemmaNode newi=new StemmaNode(nextInfStateName());

      removeLink(i,np.n1);
      removeLink(i,np.n2);

      nodes.add(newi);
      makeUnorderedLink(i,newi,0,maxLinkVal);
      makeUnorderedLink(newi,np.n1,0,maxLinkVal);
      makeUnorderedLink(newi,np.n2,0,maxLinkVal);

      return newi;
    }

    public NodePair PrincipleOfExclusionMaintained(VariantReport vReport)
    {
      LinkedList<StemmaNode> inferentialNodesUsed=new LinkedList<StemmaNode>();
      LinkedList<StemmaNode> inferentialNodesNotallowed=new LinkedList<StemmaNode>();
      for (VariantVersionData vv1 : vReport.defaultVersions)
        {
          StemmaNode n1=findNode(vv1);
          for (VariantVersionData vv2 : vReport.defaultVersions)
            if (vv1!=vv2)
              {
                StemmaNode n2=findNode(vv2);
                resetLinksChecked();
                if (!POEmaintained(n1,n2,vReport.defaultVersions,
                                   inferentialNodesUsed,inferentialNodesNotallowed))
{
System.out.println("!POE: m"+vReport.measureLabel+": "+vv1.getID()+"->"+vv2.getID());
                  return new NodePair(n1,n2);
}
              }
        }

      inferentialNodesNotallowed.addAll(inferentialNodesUsed);

      /* non-default versions */
      for (VariantReading vr : vReport.varMarker.getReadings())
        {
          inferentialNodesUsed=new LinkedList<StemmaNode>();

          for (VariantVersionData vv1 : vr.getVersions())
            if (versionsToCheck.contains(vv1))
              {
                StemmaNode n1=findNode(vv1);
                for (VariantVersionData vv2 : vr.getVersions())
                  if (vv1!=vv2 && versionsToCheck.contains(vv2))
                    {
                      StemmaNode n2=findNode(vv2);
                      for (StemmaLink l : links)
                        l.cycleChecked=false;
                      if (!POEmaintained(n1,n2,vr.getVersions(),
                                         inferentialNodesUsed,inferentialNodesNotallowed))
{
System.out.println("!POEvar: m"+vReport.measureLabel+": "+vv1.getID()+"->"+vv2.getID());
                        return new NodePair(n1,n2);
}
                    }
              }

          inferentialNodesNotallowed.addAll(inferentialNodesUsed);
        }

      return null;
    }

    boolean POEmaintained(StemmaNode n1,StemmaNode n2,
                          List<VariantVersionData> OKversions,
                          LinkedList<StemmaNode> inferentialNodesUsed,
                          LinkedList<StemmaNode> inferentialNodesNotallowed)
    {
      if (n1==n2)
        return true; /* reached the destination node */

      if (n1.isInferential())
        if (inferentialNodesNotallowed.contains(n1))
          return false;
        else if (!inferentialNodesUsed.contains(n1))
          inferentialNodesUsed.add(n1);

      for (VariantVersionData vv : n1.versions)
        if (!OKversions.contains(vv))
          return false; /* tried to pass through a conflicting node */

      /* keep traversing */
      for (StemmaLink l : n1.links)
        if (!l.cycleChecked)
          {
            l.cycleChecked=true;
            StemmaNode linkEnd=l.getOtherNode(n1);
            if (POEmaintained(linkEnd,n2,OKversions,
                              inferentialNodesUsed,inferentialNodesNotallowed))
              return true;
          }

      if (n1.isInferential())
        inferentialNodesUsed.remove(n1);

      return false; /* no POE route to destination found */
    }

    /* if 2 nodes are one apart, find the node in-between
       inferential states don't count */
    public NodeAndLinkPair findSingleInterruptor(NodePair np)
    {
      resetLinksChecked();
      return fsi(np);
    }

    private NodeAndLinkPair fsi(NodePair np)
    {
      for (StemmaLink l1 : np.n1.links)
        if (!l1.cycleChecked)
          {
            l1.cycleChecked=true;
            StemmaNode middlen=l1.getOtherNode(np.n1);
            if (middlen.isInferential())
              {
                NodeAndLinkPair newMiddlen=fsi(new NodePair(middlen,np.n2));
                if (newMiddlen!=null)
                  return newMiddlen;

                /* didn't find anything; reset links */
                for (StemmaLink l2 : middlen.links)
                  if (l2!=l1)
                    l2.cycleChecked=false;
              }

            for (StemmaLink l2 : middlen.links)
              if (!l2.cycleChecked)
                {
                  l2.cycleChecked=true;
                  if (l2!=l1 && (l2.getOtherNode(middlen)==np.n2))
                    return new NodeAndLinkPair(middlen,l1,l2);
                }
          }

      return null;
    }
  }

  class NodePair
  {
    public StemmaNode n1,n2;
    public int        count;

    public NodePair(StemmaNode n1,StemmaNode n2)
    {
      this.n1=n1;
      this.n2=n2;

      this.count=0;
    }

    public NodePair findInList(LinkedList<NodePair> l)
    {
      for (NodePair np : l)
        if ((np.n1==n1 && np.n2==n2) || (np.n1==n2 && np.n2==n1))
          return np;
      return null;
    }
  }

  class StemmaNode
  {
    public LinkedList<VariantVersionData> versions;
    public LinkedList<StemmaLink>         links;
    public DefaultGraphCell               visNode;
    public String                         archName;

    public boolean cycleChecked;

    public StemmaNode(LinkedList<VariantVersionData> v)
    {
      this.archName="";
      init(v);
    }

    public StemmaNode(VariantVersionData v)
    {
      this.archName="";
      LinkedList<VariantVersionData> versions=new LinkedList<VariantVersionData>();
      if (v!=null)
        versions.add(v);
      init(versions);
    }

    public StemmaNode(String archName)
    {
      this.archName=archName;
      init(new LinkedList<VariantVersionData>());
    }

    void init(LinkedList<VariantVersionData> versions)
    {
      this.versions=new LinkedList<VariantVersionData>(versions);
      this.links=new LinkedList<StemmaLink>();

      visNode=new DefaultGraphCell(this);
//      GraphConstants.setBounds(visNode.getAttributes(),new Rectangle2D.Double(120,120,40,20));
      GraphConstants.setResize(visNode.getAttributes(),true);
      GraphConstants.setAutoSize(visNode.getAttributes(),true);
//      GraphConstants.setGradientColor(visNode.getAttributes(),Color.red);
//      GraphConstants.setOpaque(visNode.getAttributes(),true);
      visNode.add(new DefaultPort());
    }

    public boolean hasNeighbor(StemmaNode n)
    {
      for (StemmaLink l : links)
        if (l.getOtherNode(this)==n)
          return true;
      return false;
    }

    public StemmaLink makeUnorderedLink(StemmaNode other,int val,int maxVal)
    {
      StemmaLink l=new StemmaLink(this,other,false,false,val,maxVal);
      links.add(l);
      other.links.add(l);

      return l;
    }

    public StemmaLink findLink(StemmaNode other)
    {
      for (StemmaLink l : links)
        if ((l.n1==this && l.n2==other) ||
            (l.n2==this && l.n1==other))
          return l;
      return null;
    }

    public StemmaLink removeLink(StemmaNode other)
    {
      StemmaLink linkToRemove=findLink(other);
      if (linkToRemove!=null)
        {
          links.remove(linkToRemove);
          other.links.remove(linkToRemove);
        }

      return linkToRemove;
    }

    public StemmaNode turnIntoInferential(String archName)
    {
      StemmaNode oldn=new StemmaNode(this.versions);

      this.archName=archName;
      this.versions=new LinkedList<VariantVersionData>();

      return oldn;
    }

    public boolean isInferential()
    {
      return versions.size()==0;
    }

    public String toString()
    {
      if (isInferential())
        return archName;

      String sval=versions.get(0).getID();
      for (int i=1; i<versions.size(); i++)
        sval+=" + "+versions.get(i).getID();
      return sval;
    }
  }

  class StemmaLink
  {
    public StemmaNode  n1,n2;
    public boolean     to,from;
    public int         val,maxVal;
    public DefaultEdge visLink;

    public boolean cycleChecked;

    public StemmaLink(StemmaNode n1,StemmaNode n2,boolean to,boolean from,int val,int maxVal)
    {
      this.n1=n1;
      this.n2=n2;
      this.to=to;
      this.from=from;
      this.val=val;
      this.maxVal=maxVal;

      visLink=new DefaultEdge(this);
      visLink.setSource(n1.visNode.getChildAt(0));
      visLink.setTarget(n2.visNode.getChildAt(0));
      GraphConstants.setLabelAlongEdge(visLink.getAttributes(),true);
      calcLineWidth();
//      GraphConstants.setLineColor(visLink.getAttributes(),Color.red);
    }

    void setVal(int val)
    {
      this.val=val;
      calcLineWidth();
    }

    void calcLineWidth()
    {
      GraphConstants.setLineWidth(visLink.getAttributes(),2.5f*((float)val/(float)maxVal));
    }

    public StemmaNode getOtherNode(StemmaNode n)
    {
      if (this.n1==n)
        return this.n2;
      else if (this.n2==n)
        return this.n1;
      return null;
    }

    public String toString()
    {
//      return String.valueOf(val);
//      return " "+val+" ";
      return "      ";
    }
  }

/*------------------------------------------------------------------------
Method:  void closeFrame()
Purpose: Close frame and clean up
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void closeFrame()
  {
    setVisible(false);
    unregisterListeners();

    ArrayList<StemmaFrame> openStemmaWindows=new ArrayList<StemmaFrame>(stemmaFrames);
    for (StemmaFrame sf : openStemmaWindows)
      sf.closeFrame();

    dispose();
    parentWin.notesWindowClosed(this);
  }

/*------------------------------------------------------------------------
Method:  void [un]registerListeners()
Purpose: Add and remove event listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void registerListeners()
  {
    buttonAllVariants.addItemListener(this);
    buttonSelectedVariants.addItemListener(this);
    buttonSaveNotes.addActionListener(this);
    buttonSourceAnalysis.addActionListener(this);
    for (JCheckBox cb : variantTypeCheckBoxes)
      cb.addItemListener(this);
    versionsPanel.registerListeners(this);
    for (JCheckBox cb : voiceCheckBoxes)
      cb.addItemListener(this);

    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            closeFrame();
          }
        });
  }

  protected void unregisterListeners()
  {
    buttonAllVariants.removeItemListener(this);
    buttonSelectedVariants.removeItemListener(this);
    buttonSaveNotes.removeActionListener(this);
    buttonSourceAnalysis.removeActionListener(this);
    for (JCheckBox cb : variantTypeCheckBoxes)
      cb.removeItemListener(this);
    versionsPanel.unregisterListeners(this);
    for (JCheckBox cb : voiceCheckBoxes)
      cb.removeItemListener(this);
    for (VariantReport vr : varReports)
      vr.unregisterListeners(this);

    for (WindowListener w : getListeners(WindowListener.class))
      removeWindowListener(w);
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

    for (VariantReport vr : varReports)
      if (item==vr.measureButton)
        {
          parentWin.gotomeasure(vr.measureNum-1);
          parentWin.toFront();
        }

    if (item==buttonSaveNotes)
      saveNotes();
    else if (item==buttonSourceAnalysis)
      sourceAnalysis();
  }

/*------------------------------------------------------------------------
Method:     void itemStateChanged(ItemEvent event)
Implements: ItemListener.itemStateChanged
Purpose:    Check for GUI item state changes and take appropriate action
Parameters:
  Input:  ItemEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void itemStateChanged(ItemEvent event)
  {
    try
      {
        Object item=event.getItemSelectable();

        if (item==buttonSelectedVariants)
          if (buttonSelectedVariants.isSelected())
            {
              for (JCheckBox cb : variantTypeCheckBoxes)
                cb.setEnabled(true);
              varTypeFlags=VariantDisplayOptionsFrame.calcVarFlags(variantTypeCheckBoxes);
              resetNotesPane();
            }
          else
            {
              for (JCheckBox cb : variantTypeCheckBoxes)
                cb.setEnabled(false);
            }
        else if (item==buttonAllVariants)
          {
            varTypeFlags=VariantReading.VAR_ALL;
            resetNotesPane();
          }

        for (JCheckBox cb : variantTypeCheckBoxes)
          if (item==cb)
            {
              varTypeFlags=VariantDisplayOptionsFrame.calcVarFlags(variantTypeCheckBoxes);
              resetNotesPane();
            }
        for (int i=0; i<versionsPanel.checkBoxes.length; i++)
          if (item==versionsPanel.checkBoxes[i])
            {
              if (versionsPanel.checkBoxes[i].isSelected())
                versionsToReport.add(musicData.getVariantVersion(i));
              else
                versionsToReport.remove(musicData.getVariantVersion(i));
              resetNotesPane();
            }
        for (int i=0; i<voiceCheckBoxes.length; i++)
          if (item==voiceCheckBoxes[i])
            {
              voicesToReport[i]=voiceCheckBoxes[i].isSelected();
              resetNotesPane();
            }
      }
    catch (Exception e)
      {
        parentWin.handleRuntimeError(e);
      }
  }
}
