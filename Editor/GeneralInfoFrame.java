/*----------------------------------------------------------------------*/
/*

        Module          : GeneralInfoFrame.java

        Package         : Editor

        Classes Included: GeneralInfoFrame

        Purpose         : GUI window for general file options/metadata

        Programmer      : Ted Dumitrescu

        Date Started    : 9/8/09 (moved from EditorWin.java, created class)

        Updates         :
9/9/09: added scroll pane around voice list to avoid growing beyond
        screen limits

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import DataStruct.*;
import Gfx.*;

/*------------------------------------------------------------------------
Class:   GeneralInfoFrame
Extends: JDialog
Purpose: Window for general file options/metadata
------------------------------------------------------------------------*/

class GeneralInfoFrame extends JDialog implements ActionListener
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  EditorWin owner;
  PieceData musicData;

  JPanel            voiceInfoPanel;
  JScrollPane       voiceInfoScrollPane;
  JTextField        compInfoTFTitle,
                    compInfoTFSection,
                    compInfoTFComposer,
                    compInfoTFEditor,
                    voiceInfoNames[];
  JTextArea         compInfoPubNotesArea,
                    compInfoNotesArea;
  JScrollPane       compInfoPubNotesPane,
                    compInfoNotesPane;
  JCheckBox         incipitCheckBox;
  ColorationChooser baseColorationChooser;
  JLabel            voiceNumLabels[];
  JCheckBox         voiceEditorialCheckBoxes[];
  JButton           OKButton,
                    cancelButton,
                    voiceUpButtons[],voiceDownButtons[],
                    voiceInsertUpButtons[],voiceInsertDownButtons[],voiceDeleteButtons[];

  Voice[] newVoiceList,newVoiceOrderList;

  /* flags/attribs affecting other components */
  int newEditorVoiceNum; /* voice num being edited on editor canvas (in case
                            voices switch position) */
  boolean numVoicesChanged,
          editorVoiceDeleted;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: GeneralInfoFrame(EditorWin owner,int curEditorVoiceNum)
Purpose:     Initialize and lay out frame
Parameters:
  Input:  EditorWin owner       - parent frame
          int curEditorVoiceNum - voice where cursor currently stands on
                                  editor canvas
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public GeneralInfoFrame(EditorWin owner,int curEditorVoiceNum)
  {
    super(owner,"General information: "+owner.getWindowFileName(),true);
    this.owner=owner;
    this.musicData=owner.getMusicData();
    this.newEditorVoiceNum=curEditorVoiceNum;

    /* fields for editing */
    Box editFieldsBox=Box.createVerticalBox();

    JPanel compInfoPanel=new JPanel();
    compInfoPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Composition information"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    compInfoPanel.setLayout(new GridBagLayout());
    GridBagConstraints cic=new GridBagConstraints();

    JLabel compInfoLabelTitle=new JLabel("Title");
    compInfoLabelTitle.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    compInfoTFTitle=new JTextField(musicData.getTitle(),30);
    JLabel compInfoLabelSection=new JLabel("Section");
    compInfoLabelSection.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    compInfoTFSection=new JTextField(musicData.getSectionTitle(),30);
    JLabel compInfoLabelComposer=new JLabel("Composer");
    compInfoLabelComposer.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    compInfoTFComposer=new JTextField(musicData.getComposer(),30);
    JLabel compInfoLabelEditor=new JLabel("Editor");
    compInfoLabelEditor.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    compInfoTFEditor=new JTextField(musicData.getEditor(),30);

    JLabel compInfoLabelPubNotes=new JLabel("Public Notes");
    compInfoLabelPubNotes.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    compInfoPubNotesArea=new JTextArea(musicData.getPublicNotes(),4,30);
    compInfoPubNotesPane=new JScrollPane(compInfoPubNotesArea);

    JLabel compInfoLabelNotes=new JLabel("Private Notes");
    compInfoLabelNotes.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    compInfoNotesArea=new JTextArea(musicData.getNotes(),4,30);
    compInfoNotesPane=new JScrollPane(compInfoNotesArea);

    cic.anchor=GridBagConstraints.LINE_START;
    cic.gridx=0; cic.gridy=0; compInfoPanel.add(compInfoLabelTitle,cic);
    cic.gridx=1;              compInfoPanel.add(compInfoTFTitle,cic);
    cic.gridx=0; cic.gridy++; compInfoPanel.add(compInfoLabelSection,cic);
    cic.gridx=1;              compInfoPanel.add(compInfoTFSection,cic);
    cic.gridx=0; cic.gridy++; compInfoPanel.add(compInfoLabelComposer,cic);
    cic.gridx=1;              compInfoPanel.add(compInfoTFComposer,cic);
    cic.gridx=0; cic.gridy++; compInfoPanel.add(compInfoLabelEditor,cic);
    cic.gridx=1;              compInfoPanel.add(compInfoTFEditor,cic);
    cic.gridx=3; cic.gridy=0;  cic.gridheight=1; compInfoPanel.add(compInfoLabelPubNotes,cic);
    cic.gridx=4;               cic.gridheight=2; compInfoPanel.add(compInfoPubNotesPane,cic);
    cic.gridx=3; cic.gridy+=2; cic.gridheight=1; compInfoPanel.add(compInfoLabelNotes,cic);
    cic.gridx=4;               cic.gridheight=2; compInfoPanel.add(compInfoNotesPane,cic);

    /* voice information */
    int numvoices=musicData.getVoiceData().length;
    newVoiceOrderList=new Voice[numvoices];
    for (int i=0; i<numvoices; i++)
      newVoiceOrderList[i]=musicData.getVoiceData()[i];

    voiceInfoPanel=new JPanel();
    initVoiceInfoPanel(musicData.getVoiceData());
    JScrollPane voiceInfoScrollPane=new JScrollPane(voiceInfoPanel,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
//    voiceInfoScrollPane.setPreferredSize(MusicWin.fitInScreen(DEFAULT_PANESIZE,0.5f));
    voiceInfoScrollPane.setMinimumSize(new Dimension(20,20));
//    voiceInfoScrollPane.setPreferredSize(MusicWin.fitInScreen(MusicWin.SCREEN_DIMENSION,0.9f,0.3f));

    /* other options */
    JPanel generalOptionsPanel=new JPanel();
    generalOptionsPanel.setLayout(new BoxLayout(generalOptionsPanel,BoxLayout.X_AXIS));
    generalOptionsPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Options"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    incipitCheckBox=new JCheckBox("Incipit-score",musicData.isIncipitScore());
    incipitCheckBox.setBorder(BorderFactory.createEmptyBorder(0,5,25,5));
    baseColorationChooser=new ColorationChooser(musicData.getBaseColoration());
    Box cBox=Box.createHorizontalBox();
    cBox.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
    cBox.add(Box.createHorizontalGlue());
    cBox.add(new JLabel("Base coloration: "));
    cBox.add(baseColorationChooser);
    cBox.add(Box.createHorizontalGlue());
    generalOptionsPanel.add(incipitCheckBox);
    generalOptionsPanel.add(cBox);

    editFieldsBox.add(compInfoPanel);
    editFieldsBox.add(voiceInfoScrollPane);
    editFieldsBox.add(generalOptionsPanel);
    editFieldsBox.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    /* action buttons */
    OKButton=new JButton("Apply");
    cancelButton=new JButton("Cancel");
    Box buttonPane=Box.createHorizontalBox();
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(OKButton);
    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(cancelButton);
    buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    Container gicp=this.getContentPane();
    gicp.setLayout(new GridBagLayout());
    GridBagConstraints cc=new GridBagConstraints();
    cc.gridx=0; cc.gridy=0; gicp.add(editFieldsBox,cc);
    cc.gridx=0; cc.gridy++; cc.anchor=GridBagConstraints.EAST; gicp.add(buttonPane,cc);

    OKButton.addActionListener(this);
    cancelButton.addActionListener(this);

    this.pack();
    this.setLocationRelativeTo(owner);

    int freeYSpace=(int)(MusicWin.SCREEN_DIMENSION.height*0.8-
          compInfoPanel.getSize().height-
          generalOptionsPanel.getSize().height-
          buttonPane.getSize().height),
        voiceInfoYSize=voiceInfoPanel.getSize().height;
//  if (freeYSpace<voiceInfoYSize)
      voiceInfoYSize=freeYSpace;

    /* check current size of voiceInfoPanel as basis for preferred scroll pane size */
    voiceInfoScrollPane.setPreferredSize(new Dimension(voiceInfoPanel.getSize().width,
                                                       voiceInfoYSize));
    voiceInfoPanel.revalidate();
    this.pack();
  }

/*------------------------------------------------------------------------
Method:  void initVoiceInfoPanel(Voice[] voices)
Purpose: Create content for voiceInfoPanel
Parameters:
  Input:  Voice[] voices - voice data for display
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void initVoiceInfoPanel(Voice[] voices)
  {
    voiceInfoPanel.setLayout(new GridBagLayout());
    GridBagConstraints vic=new GridBagConstraints();
    vic.anchor=GridBagConstraints.LINE_START;
    voiceInfoPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Voices"),
      BorderFactory.createEmptyBorder(5,5,5,5)));

    int numvoices=voices.length;
    newVoiceList=new Voice[numvoices];

    JLabel headingNum=      new JLabel("No. "),
           headingName=     new JLabel("Name"),
           headingEditorial=new JLabel("Editorial");
    vic.gridx=0; vic.gridy=0; voiceInfoPanel.add(headingNum,vic);
    vic.gridx=1; vic.gridy=0; voiceInfoPanel.add(headingName,vic);
    vic.gridx=2; vic.gridy=0; voiceInfoPanel.add(headingEditorial,vic);

    voiceInfoNames=new JTextField[numvoices];
    voiceNumLabels=new JLabel[numvoices];
    voiceEditorialCheckBoxes=new JCheckBox[numvoices];
    voiceUpButtons=new JButton[numvoices];
    voiceDownButtons=new JButton[numvoices];
    voiceInsertUpButtons=new JButton[numvoices];
    voiceInsertDownButtons=new JButton[numvoices];
    voiceDeleteButtons=new JButton[numvoices];
    for (int i=0; i<numvoices; i++)
      {
        Voice newv=new Voice(voices[i]);
        newVoiceList[i]=newv;

        voiceNumLabels[i]=new JLabel(String.valueOf(i+1));
        voiceNumLabels[i].setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        voiceInfoNames[i]=new JTextField(newv.getName(),20);
        voiceNumLabels[i].setLabelFor(voiceInfoNames[i]);
        voiceEditorialCheckBoxes[i]=new JCheckBox("",newv.isEditorial());
        voiceUpButtons[i]=new JButton('\u2191'+" Move");
        voiceDownButtons[i]=new JButton("Move "+'\u2193');
        voiceInsertUpButtons[i]=new JButton('\u2191'+" Insert");
        voiceInsertDownButtons[i]=new JButton("Insert "+'\u2193');
        voiceDeleteButtons[i]=new JButton("Delete");

        if (i==0)
          voiceUpButtons[i].setEnabled(false);
        if (i+1>=numvoices)
          voiceDownButtons[i].setEnabled(false);

        vic.gridx=0; vic.gridy=i+1; voiceInfoPanel.add(voiceNumLabels[i],vic);
        vic.gridx++; vic.gridy=i+1; voiceInfoPanel.add(voiceInfoNames[i],vic);
        vic.gridx++; vic.gridy=i+1; voiceInfoPanel.add(voiceEditorialCheckBoxes[i],vic);
        vic.gridx++; vic.gridy=i+1; voiceInfoPanel.add(voiceUpButtons[i],vic);
        vic.gridx++; vic.gridy=i+1; voiceInfoPanel.add(voiceDownButtons[i],vic);
        vic.gridx++; vic.gridy=i+1; voiceInfoPanel.add(voiceInsertUpButtons[i],vic);
        vic.gridx++; vic.gridy=i+1; voiceInfoPanel.add(voiceInsertDownButtons[i],vic);
        vic.gridx++; vic.gridy=i+1; voiceInfoPanel.add(voiceDeleteButtons[i],vic);

        voiceUpButtons[i].addActionListener(this);
        voiceDownButtons[i].addActionListener(this);
        voiceInsertUpButtons[i].addActionListener(this);
        voiceInsertDownButtons[i].addActionListener(this);
        voiceDeleteButtons[i].addActionListener(this);
      }
    if (numvoices==1)
      voiceDeleteButtons[0].setEnabled(false);
  }

/*------------------------------------------------------------------------
Method:  void voiceInfo[Move|Insert|Delete](int vnum,int offset)
Purpose: Perform operations on one voice in voice info panel (move up or
         down one place, insert before, delete)
Parameters:
  Input:  int vnum   - voice number to modify
          int offset - amount to displace voice (1 or -1)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void voiceInfoMove(int vnum,int offset)
  {
    if (vnum==this.newEditorVoiceNum)
      this.newEditorVoiceNum+=offset;
    else if (vnum+offset==this.newEditorVoiceNum)
      this.newEditorVoiceNum=vnum;

    /* save name changes */
    newVoiceList[vnum].setName(voiceInfoNames[vnum].getText());
    newVoiceList[vnum+offset].setName(voiceInfoNames[vnum+offset].getText());

    /* swap vnum and vnum+offset */
    Voice tmpv=newVoiceList[vnum];
    newVoiceList[vnum]=newVoiceList[vnum+offset];
    newVoiceList[vnum+offset]=tmpv;
    tmpv=newVoiceOrderList[vnum];
    newVoiceOrderList[vnum]=newVoiceOrderList[vnum+offset];
    newVoiceOrderList[vnum+offset]=tmpv;

    voiceInfoNames[vnum].setText(newVoiceList[vnum].getName());
    voiceInfoNames[vnum+offset].setText(newVoiceList[vnum+offset].getName());
  }

  void voiceInfoInsert(int vnum)
  {
    int     oldNumVoices=newVoiceList.length;
    Voice[] tmpVoiceList=new Voice[oldNumVoices+1],
            tmpVoiceOrderList=new Voice[oldNumVoices+1];

    for (int i=oldNumVoices-1; i>=vnum; i--)
      {
        tmpVoiceList[i+1]=newVoiceList[i];
        tmpVoiceList[i+1].setName(voiceInfoNames[i].getText());
        tmpVoiceOrderList[i+1]=newVoiceOrderList[i];
      }
    tmpVoiceList[vnum]=new Voice(musicData,vnum+1,"["+(oldNumVoices+1)+"]",false);
    tmpVoiceOrderList[vnum]=tmpVoiceList[vnum];
//    tmpVoiceList[vnum].addevent(new Event(Event.EVENT_SECTIONEND));
    for (int i=vnum-1; i>=0; i--)
      {
        tmpVoiceList[i]=newVoiceList[i];
        tmpVoiceList[i].setName(voiceInfoNames[i].getText());
        tmpVoiceOrderList[i]=newVoiceOrderList[i];
      }
    if (this.newEditorVoiceNum>=vnum)
      this.newEditorVoiceNum++;
    newVoiceOrderList=tmpVoiceOrderList;

    reinitVoiceInfoPanel(tmpVoiceList);
  }

  void voiceInfoDelete(int vnum)
  {
    int     oldNumVoices=newVoiceList.length;
    Voice[] tmpVoiceList=new Voice[oldNumVoices-1],
            tmpVoiceOrderList=new Voice[oldNumVoices-1];

    for (int i=0; i<vnum; i++)
      {
        tmpVoiceList[i]=newVoiceList[i];
        tmpVoiceList[i].setName(voiceInfoNames[i].getText());
        tmpVoiceOrderList[i]=newVoiceOrderList[i];
      }
    for (int i=vnum+1; i<oldNumVoices; i++)
      {
        tmpVoiceList[i-1]=newVoiceList[i];
        tmpVoiceList[i-1].setName(voiceInfoNames[i].getText());
        tmpVoiceOrderList[i-1]=newVoiceOrderList[i];
      }
    if (this.newEditorVoiceNum==vnum)
      this.editorVoiceDeleted=true;
    else if (this.newEditorVoiceNum>vnum)
      this.newEditorVoiceNum--;
    newVoiceOrderList=tmpVoiceOrderList;

    reinitVoiceInfoPanel(tmpVoiceList);
  }

  void reinitVoiceInfoPanel(Voice[] newVoiceList)
  {
    unregisterVoiceInfoListeners();
    voiceInfoPanel.removeAll();
    initVoiceInfoPanel(newVoiceList);
    voiceInfoPanel.revalidate();
    pack();
  }

/*------------------------------------------------------------------------
Method:  void saveData()
Purpose: Apply changes to music data
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void saveData()
  {
    int oldNumVoices=musicData.getVoiceData().length;

    musicData.setGeneralData(compInfoTFTitle.getText(),
                             compInfoTFSection.getText(),
                             compInfoTFComposer.getText(),
                             compInfoTFEditor.getText(),
                             compInfoPubNotesArea.getText(),
                             compInfoNotesArea.getText());
    boolean oldIncipitStatus=musicData.isIncipitScore();
    musicData.setIncipitScore(incipitCheckBox.isSelected());

    for (int i=0; i<newVoiceList.length; i++)
      {
        newVoiceList[i].setName(voiceInfoNames[i].getText());
        newVoiceList[i].setNum(i+1);
        newVoiceList[i].setEditorial(voiceEditorialCheckBoxes[i].isSelected());
      }

    /* update sections */
    for (int si=0; si<musicData.getNumSections(); si++)
      musicData.getSection(si).updateVoiceList(musicData.getVoiceData(),newVoiceList,newVoiceOrderList);

    musicData.setVoiceData(newVoiceList);
    Coloration newBaseColoration=baseColorationChooser.createColoration();
    for (MusicSection ms : musicData.getSections())
      if (ms.getBaseColoration().equals(musicData.getBaseColoration()))
        ms.setBaseColoration(newBaseColoration);
    musicData.setBaseColoration(newBaseColoration);

    /* add section for Finales in incipit-score? */
    if (musicData.isIncipitScore() && oldIncipitStatus==false)
      {
        MusicMensuralSection finalisSection=new MusicMensuralSection(musicData.getVoiceData().length,
                                                                     false,
                                                                     musicData.getBaseColoration());
        for (int vi=0; vi<finalisSection.getNumVoices(); vi++)
          {
            VoiceMensuralData newv=new VoiceMensuralData(musicData.getVoiceData()[vi],finalisSection);
            newv.addEvent(new DataStruct.Event(DataStruct.Event.EVENT_SECTIONEND));
            finalisSection.setVoice(vi,newv);
          }
        musicData.addSection(musicData.getNumSections(),finalisSection);
      }

    numVoicesChanged=newVoiceList.length!=oldNumVoices;
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

    /* general/voice information dialog */
    if (item==OKButton)
      owner.saveGeneralInfo();
    else if (item==cancelButton)
      owner.closeGeneralInfoFrame();

    else
      {
        for (int i=0; i<voiceUpButtons.length; i++)
          if (item==voiceUpButtons[i])
            {
              voiceInfoMove(i,-1);
              return;
            }
        for (int i=0; i<voiceDownButtons.length; i++)
          if (item==voiceDownButtons[i])
            {
              voiceInfoMove(i,1);
              return;
            }
        for (int i=0; i<voiceInsertUpButtons.length; i++)
          if (item==voiceInsertUpButtons[i])
            {
              voiceInfoInsert(i);
              return;
            }
        for (int i=0; i<voiceInsertDownButtons.length; i++)
          if (item==voiceInsertDownButtons[i])
            {
              voiceInfoInsert(i+1);
              return;
            }
        for (int i=0; i<voiceDeleteButtons.length; i++)
          if (item==voiceDeleteButtons[i])
            {
              voiceInfoDelete(i);
              return;
            }
      }
  }

/*------------------------------------------------------------------------
Method:  void closewin()
Purpose: Close frame and release resources
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void closewin()
  {
    this.setVisible(false);
    unregisterListeners();
    dispose();
  }

/*------------------------------------------------------------------------
Method:  void unregisterListeners()
Purpose: Unregister GUI listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void unregisterListeners()
  {
    OKButton.removeActionListener(this);
    cancelButton.removeActionListener(this);
    unregisterVoiceInfoListeners();
  }

  public void unregisterVoiceInfoListeners()
  {
    for (int i=0; i<voiceDeleteButtons.length; i++)
      {
        voiceUpButtons[i].removeActionListener(this);
        voiceDownButtons[i].removeActionListener(this);
        voiceInsertUpButtons[i].removeActionListener(this);
        voiceInsertDownButtons[i].removeActionListener(this);
        voiceDeleteButtons[i].removeActionListener(this);
      }
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public boolean editorVoiceDeleted()
  {
    return this.editorVoiceDeleted;
  }

  public int getNewEditorVoiceNum()
  {
    return this.newEditorVoiceNum;
  }

  public boolean numVoicesChanged()
  {
    return this.numVoicesChanged;
  }
}
