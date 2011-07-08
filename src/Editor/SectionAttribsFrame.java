/*----------------------------------------------------------------------*/
/*

        Module          : SectionAttribsFrame.java

        Package         : Editor

        Classes Included: SectionAttribsFrame

        Purpose         : GUI panel for modifying section attributes

        Programmer      : Ted Dumitrescu

        Date Started    : 9/26/07

        Updates         :
7/15/08: added sectionInfoLock
         added voice deletion confirmation dialog
12/1/08: added missing version controls
12/2/08: changed missing version controls to missing versions for each
         voice

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import DataStruct.*;
import Gfx.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   SectionAttribsFrame
Extends: JDialog
Purpose: Frame for modifying section attributes
------------------------------------------------------------------------*/

public class SectionAttribsFrame extends JDialog implements ActionListener,ItemListener
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  EditorWin owner;
  int       curSectionNum=-1,
            numVoices=0;

  MusicSection curSection=null;

  int sectionInfoLock=0; /* >0 while updating GUI (check to avoid marking
                            file as modified when a new section is loaded) */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: SectionAttribsFrame(EditorWin owner)
Purpose:     Initialize and lay out frame
Parameters:
  Input:  EditorWin owner - parent frame
  Output: -
------------------------------------------------------------------------*/

  public SectionAttribsFrame(EditorWin owner)
  {
    super(owner,"Section attributes",false);
    this.owner=owner;

    Container cp=getContentPane();

    JPanel sectionInfoPanel=createSectionInfoPanel(),
           voiceInfoPanel=createVoiceInfoPanel(owner.getMusicData().getVoiceData(),
                                               owner.getMusicData().getVariantVersionNames()),
           controlPanel=createControlPanel();
    JScrollPane voiceInfoScrollPane=new JScrollPane(voiceInfoPanel);

    Box topPanels=Box.createVerticalBox();
    topPanels.add(sectionInfoPanel);
    cp.add(topPanels,BorderLayout.NORTH);
    cp.add(voiceInfoScrollPane,BorderLayout.CENTER);
    cp.add(controlPanel,BorderLayout.SOUTH);
    pack();

    registerListeners();
  }

/*------------------------------------------------------------------------
Method:  JPanel create*Panel()
Purpose: Initialize individual panes within frame
Parameters:
  Input:  -
  Output: -
  Return: one frame section as JPanel
------------------------------------------------------------------------*/

  /* section info panel */

  JLabel            sectionNumLabel;
  JCheckBox         sectionEditorialCheckBox;
  JTextField        sectionSourceField,sectionSourceNumField;
  ColorationChooser sectionColorationChooser;

  JPanel createSectionInfoPanel()
  {
    JPanel sectionInfoPanel=new JPanel();
    sectionInfoPanel.setLayout(new GridBagLayout());
    GridBagConstraints sic=new GridBagConstraints();
    sic.anchor=GridBagConstraints.LINE_START;
    sectionInfoPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    JLabel sectionNumHeaderLabel=new JLabel("Section number: ");
    sectionNumHeaderLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    sectionNumLabel=new JLabel("0");
    sectionNumLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    sic.gridy=0;
    sic.gridx=0; sectionInfoPanel.add(sectionNumHeaderLabel,sic);
    sic.gridx=1; sectionInfoPanel.add(sectionNumLabel,sic);

    JLabel sectionEditorialLabel=new JLabel("Editorial ");
    sectionEditorialLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    sectionEditorialCheckBox=new JCheckBox();
    sic.gridy++;
    sic.gridx=0; sectionInfoPanel.add(sectionEditorialLabel,sic);
    sic.gridx=1; sectionInfoPanel.add(sectionEditorialCheckBox,sic);

    JLabel sectionSourceLabel=new JLabel("Principal source: ");
    sectionSourceLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    sectionSourceField=new JTextField(20);
    sic.gridy++;
    sic.gridx=0; sectionInfoPanel.add(sectionSourceLabel,sic);
    sic.gridx=1; sectionInfoPanel.add(sectionSourceField,sic);

    JLabel sectionSourceNumLabel=new JLabel("Principal source ID no.: ");
    sectionSourceNumLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    sectionSourceNumField=new JTextField(5);
    sic.gridy++;
    sic.gridx=0; sectionInfoPanel.add(sectionSourceNumLabel,sic);
    sic.gridx=1; sectionInfoPanel.add(sectionSourceNumField,sic);

    JLabel sectionColorationLabel=new JLabel("Coloration ");
    sectionColorationLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    sectionColorationChooser=new ColorationChooser();
    sic.gridy++;
    sic.gridx=0; sectionInfoPanel.add(sectionColorationLabel,sic);
    sic.gridx=1; sectionInfoPanel.add(sectionColorationChooser,sic);

    return sectionInfoPanel;
  }

  /* versions panel */

  SelectionPanel createVersionsPanel(ArrayList<String> versionNames)
  {
    SelectionPanel versionsPanel=new SelectionPanel(
      "Missing in version",versionNames,SelectionPanel.CHECKBOX,4);
    versionsPanel.checkBoxes[0].setSelected(false);
    versionsPanel.checkBoxes[0].setEnabled(false);
    for (int i=1; i<versionsPanel.checkBoxes.length; i++)
      versionsPanel.checkBoxes[i].setSelected(false);

    return versionsPanel;
  }

  /* voice info panel */

  JCheckBox  voiceUsedCheckBoxes[];
  JLabel     voiceNumLabels[],
             voiceNameLabels[],
             voiceTacetLabels[];
  JTextField voiceTacetTexts[];

  SelectionPanel voiceVersionsPanels[];

  static final int ROWS_PER_VOICE=3;

  JPanel createVoiceInfoPanel(Voice[] voices,ArrayList<String> versionNames)
  {
    numVoices=voices.length;
    JPanel voiceInfoPanel=new JPanel();
    voiceInfoPanel.setLayout(new GridBagLayout());
    GridBagConstraints vic=new GridBagConstraints();
    vic.anchor=GridBagConstraints.LINE_START;
    voiceInfoPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Voices"),
      BorderFactory.createEmptyBorder(5,5,5,5)));

    JLabel headingUsed=new JLabel(" "),
           headingNum= new JLabel("No. "),
           headingName=new JLabel("Name");
    headingUsed.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    headingNum.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    headingName.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    vic.gridx=0; vic.gridy=0; voiceInfoPanel.add(headingUsed,vic);
    vic.gridx=1; vic.gridy=0; voiceInfoPanel.add(headingNum,vic);
    vic.gridx=2; vic.gridy=0; voiceInfoPanel.add(headingName,vic);

    voiceUsedCheckBoxes=new JCheckBox[numVoices];
    voiceNumLabels=new JLabel[numVoices];
    voiceNameLabels=new JLabel[numVoices];
    voiceTacetLabels=new JLabel[numVoices];
    voiceTacetTexts=new JTextField[numVoices];
    voiceVersionsPanels=new SelectionPanel[numVoices];
    for (int i=0; i<numVoices; i++)
      {
        /* line 1 */

        voiceUsedCheckBoxes[i]=new JCheckBox();
        voiceNumLabels[i]=new JLabel(String.valueOf(i+1));
        voiceNumLabels[i].setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        voiceNameLabels[i]=new JLabel(voices[i].getName());
        voiceNameLabels[i].setBorder(BorderFactory.createEmptyBorder(0,5,0,5));

        vic.gridx=0; vic.gridy=(i*ROWS_PER_VOICE)+1; voiceInfoPanel.add(voiceUsedCheckBoxes[i],vic);
        vic.gridx=1; vic.gridy=(i*ROWS_PER_VOICE)+1; voiceInfoPanel.add(voiceNumLabels[i],vic);
        vic.gridx=2; vic.gridy=(i*ROWS_PER_VOICE)+1; voiceInfoPanel.add(voiceNameLabels[i],vic);

        /* line 2 */

        voiceTacetLabels[i]=new JLabel("Tacet text: ");
        voiceTacetLabels[i].setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        voiceTacetLabels[i].setEnabled(false);
        voiceTacetTexts[i]=new JTextField(30);
        voiceTacetTexts[i].setEnabled(false);
        vic.gridx=1; vic.gridy=(i*ROWS_PER_VOICE)+2; voiceInfoPanel.add(voiceTacetLabels[i],vic);
        vic.gridx=2; vic.gridy=(i*ROWS_PER_VOICE)+2; voiceInfoPanel.add(voiceTacetTexts[i],vic);

        /* line 3: missing versions */
        voiceVersionsPanels[i]=new SelectionPanel(
//          "V"+(i+1)+" missing in versions:",
          "Missing versions",versionNames,SelectionPanel.CHECKBOX,4);
        voiceVersionsPanels[i].checkBoxes[0].setSelected(false);
        voiceVersionsPanels[i].checkBoxes[0].setEnabled(false);
        for (int ci=1; ci<voiceVersionsPanels[i].checkBoxes.length; ci++)
          voiceVersionsPanels[i].checkBoxes[ci].setSelected(false);
        vic.gridwidth=3;
        vic.gridx=1; vic.gridy=(i*ROWS_PER_VOICE)+3; voiceInfoPanel.add(voiceVersionsPanels[i],vic);
        vic.gridwidth=1;
      }

    return voiceInfoPanel;
  }

/*------------------------------------------------------------------------
Method:  JPanel createControlPanel()
Purpose: Create content for panel containing general controls (delete, combine)
Parameters:
  Input:  -
  Output: -
  Return: new JPanel containing controls
------------------------------------------------------------------------*/

  JButton sectionDeleteButton,
          sectionCombineButton;

  JPanel createControlPanel()
  {
    JPanel controlPanel=new JPanel();
    controlPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

    sectionDeleteButton=new JButton("Delete section");
    sectionCombineButton=new JButton("Combine with next section");

    Box buttonBox=Box.createHorizontalBox();
    buttonBox.add(Box.createHorizontalGlue());
    buttonBox.add(sectionDeleteButton);
    buttonBox.add(Box.createHorizontalStrut(10));
    buttonBox.add(sectionCombineButton);
//    buttonBox.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    controlPanel.add(buttonBox);
    return controlPanel;
  }

/*------------------------------------------------------------------------
Method:  void setLocation(int newx,int newy)
Purpose: Position frame
Parameters:
  Input:  int newx,newy - new location to attempt
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setLocation(int newx,int newy)
  {
    /* position relative to event editor frame 
    int eox=eventEditorFrame.getLocation().x+eventEditorFrame.getSize().width,
        eoy=eventEditorFrame.getLocation().y,*/
    Dimension frameSize=getSize(),
              screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();

    if (newx+frameSize.width>screenSize.width)
      newx=screenSize.width-frameSize.width;
    if (newy+frameSize.height>screenSize.height)
      newy=screenSize.height-frameSize.height;

    super.setLocation(newx,newy);
  }

/*------------------------------------------------------------------------
Method:  void setSectionNum(int newSectionNum)
Purpose: Update GUI if necessary to reflect correct section data
Parameters:
  Input:  int newSectionNum - section number for display
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setSectionNum(int newSectionNum)
  {
    if (newSectionNum==curSectionNum)
      return;
    updateSectionGUI(newSectionNum);
  }

  public void updateSectionGUI(int newSectionNum)
  {
    sectionInfoLock++;

    if (curSectionNum>=0)
      for (int vi=0; vi<numVoices; vi++)
        curSection.setTacetText(vi,voiceTacetTexts[vi].getText());

    curSectionNum=newSectionNum;
    curSection=owner.getMusicData().getSection(curSectionNum);
    Voice[] voices=owner.getMusicData().getVoiceData();

    sectionNumLabel.setText(
      String.valueOf(curSectionNum+1)+
      " ("+MusicSection.sectionTypeNames[curSection.getSectionType()]+")");
    sectionEditorialCheckBox.setSelected(curSection.isEditorial());
    String sourceName=curSection.getPrincipalSource();
    int    sourceNum=curSection.getPrincipalSourceNum();
    if (sourceName!=null)
      {
        sectionSourceField.setText(sourceName);
        sectionSourceNumField.setText(sourceNum>0 ? String.valueOf(sourceNum) : "");
      }
    else
      {
        sectionSourceField.setText("");
        sectionSourceNumField.setText("");
      }
    sectionColorationChooser.setIndices(curSection.getBaseColoration());

    for (int vi=0; vi<numVoices; vi++)
      {
        VoiceEventListData curVoice=curSection.getVoice(vi);
        voiceTacetTexts[vi].setText("");
        if (curVoice==null)
          {
            voiceUsedCheckBoxes[vi].setSelected(false);
            voiceNumLabels[vi].setEnabled(false);
            voiceNameLabels[vi].setEnabled(false);
            voiceTacetLabels[vi].setEnabled(true);
            voiceTacetTexts[vi].setEnabled(true);

            for (JCheckBox cb : voiceVersionsPanels[vi].checkBoxes)
              {
                cb.setSelected(false);
                cb.setEnabled(false);
              }
          }
        else
          {
            voiceUsedCheckBoxes[vi].setSelected(true);
            voiceNumLabels[vi].setEnabled(true);
            voiceNameLabels[vi].setEnabled(true);
            voiceTacetLabels[vi].setEnabled(false);
            voiceTacetTexts[vi].setEnabled(false);

            for (JCheckBox cb : voiceVersionsPanels[vi].checkBoxes)
              {
                cb.setSelected(false);
                cb.setEnabled(true);
              }
            for (VariantVersionData vvd : curSection.getVoice(vi).getMissingVersions())
              voiceVersionsPanels[vi].checkBoxes[vvd.getNumInList()].setSelected(true);
          }
      }
    for (TacetInfo ti : curSection.getTacetInfo())
      voiceTacetTexts[ti.voiceNum].setText(ti.tacetText);

    enableOrDisableVoiceUsedCheckBoxes();

    /* control panel */
    int numSections=owner.getMusicData().getSections().size();
    sectionDeleteButton.setEnabled(numSections>1);
    sectionCombineButton.setEnabled(curSectionNum<(numSections-1) &&
      curSection.getSectionType()==owner.getMusicData().getSection(curSectionNum+1).getSectionType());

    sectionInfoLock--;
  }

/*------------------------------------------------------------------------
Method:  boolean confirmVoiceDelete(int vnum)
Purpose: Request user confirmation before deleting voice from section
Parameters:
  Input:  int vnum - number of voice to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  boolean confirmVoiceDelete(int vnum)
  {
    int confirmOption=JOptionPane.showConfirmDialog(this,
          "Remove voice "+(vnum+1)+" from section? (This will delete all music associated with this voice in this section)",
          "Voice deletion",
          JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);

    switch (confirmOption)
      {
        case JOptionPane.YES_OPTION:
          return true;
        case JOptionPane.NO_OPTION:
          return false;
      }

    return false;
  }

/*------------------------------------------------------------------------
Method:  void enableOrDisableVoiceUsedCheckBoxes()
Purpose: Update GUI to enable or disable check boxes to allow or disallow
         deleting of voices from a section
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void enableOrDisableVoiceUsedCheckBoxes()
  {
    /* don't allow last voice to be removed */
    if (curSection.getNumVoicesUsed()==1)
      voiceUsedCheckBoxes[curSection.getValidVoicenum(0)].setEnabled(false);
    else
      for (int i=0; i<numVoices; i++)
        if (!voiceUsedCheckBoxes[i].isEnabled())
          voiceUsedCheckBoxes[i].setEnabled(true);
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

  void fileModified()
  {
    if (sectionInfoLock==0)
      owner.fileModified();
  }

  public void actionPerformed(ActionEvent event)
  {
    Object item=event.getSource();

    if (item==sectionSourceField)
      {
        curSection.setPrincipalSource(sectionSourceField.getText());
        fileModified();
      }
    else if (item==sectionSourceNumField)
      {
        curSection.setPrincipalSourceNum(Integer.parseInt(sectionSourceNumField.getText()));
        fileModified();
      }
    else if (item==sectionDeleteButton)
      {
        int confirmOption=JOptionPane.showConfirmDialog(this,
              "Delete section "+(curSectionNum+1)+"?",
              "Section deletion",
              JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if (confirmOption==JOptionPane.YES_OPTION)
          owner.deleteSection(curSectionNum);
      }
    else if (item==sectionCombineButton)
      {
        int confirmOption=JOptionPane.showConfirmDialog(this,
              "Combine sections "+(curSectionNum+1)+" and "+(curSectionNum+2)+"?",
              "Section combination",
              JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
        if (confirmOption==JOptionPane.YES_OPTION)
          owner.combineSectionWithNext();
      }

    for (int vi=0; vi<numVoices; vi++)
      if (item==voiceTacetTexts[vi])
        {
          curSection.setTacetText(vi,voiceTacetTexts[vi].getText());
          owner.rerendermusic=true;
          owner.repaint();
          fileModified();
        }
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
    if (sectionInfoLock>0)
      return;

    Object item=event.getItemSelectable();

    if (item==sectionEditorialCheckBox)
      if (curSection.isEditorial()!=sectionEditorialCheckBox.isSelected())
        {
          curSection.setEditorial(sectionEditorialCheckBox.isSelected());
          fileModified();
        }
    if (sectionColorationChooser.itemSelected(item))
      {
        Coloration newColoration=sectionColorationChooser.createColoration();
        if (!curSection.getBaseColoration().equals(newColoration))
          {
            curSection.setBaseColoration(newColoration);
            fileModified();
            owner.getMusicData().recalcAllEventParams();
            owner.rerendermusic=true;
            owner.repaint();
          }
      }

    for (int vi=0; vi<numVoices; vi++)
      if (item==voiceUsedCheckBoxes[vi])
        {
          boolean isUsed=voiceUsedCheckBoxes[vi].isSelected();
          if ((curSection.getVoice(vi)!=null)!=isUsed)
            {
              if (!isUsed && !confirmVoiceDelete(vi))
                {
                  voiceUsedCheckBoxes[vi].setSelected(true);
                  return;
                }

              owner.setVoiceUsedInSection(curSectionNum,vi,isUsed);
              fileModified();
              voiceNumLabels[vi].setEnabled(isUsed);
              voiceNameLabels[vi].setEnabled(isUsed);
              voiceTacetLabels[vi].setEnabled(!isUsed);
              voiceTacetTexts[vi].setEnabled(!isUsed);
              for (int i=0; i<voiceVersionsPanels[vi].checkBoxes.length; i++)
                voiceVersionsPanels[vi].checkBoxes[i].setEnabled(isUsed);

              enableOrDisableVoiceUsedCheckBoxes();
            }
        }
      else
        for (int i=0; i<voiceVersionsPanels[vi].checkBoxes.length; i++)
          if (item==voiceVersionsPanels[vi].checkBoxes[i] &&
              curSection.getVoice(vi)!=null)
            {
              VariantVersionData vvd=owner.getMusicData().getVariantVersion(i);
              if (curSection.getVoice(vi).getMissingVersions().contains(vvd)!=voiceVersionsPanels[vi].checkBoxes[i].isSelected())
                {
                  if (voiceVersionsPanels[vi].checkBoxes[i].isSelected())
                    curSection.addMissingVersion(vi,vvd);
                  else
                    curSection.removeMissingVersion(vi,vvd);
                  fileModified();
                  owner.reconstructCurrentVersion();
                  owner.repaint();
                }
            }
  }

/*------------------------------------------------------------------------
Method:  void (un)registerListeners()
Purpose: Register/unregister GUI listeners (register and unregister should match)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void ownerSetSectionsMenuDisplaySectionAttribs(boolean newval)
  {
    owner.setSectionsMenuDisplaySectionAttribs(newval);
  }

  public void registerListeners()
  {
    sectionEditorialCheckBox.addItemListener(this);
    sectionSourceField.addActionListener(this);
    sectionSourceNumField.addActionListener(this);
    sectionColorationChooser.addListener(this);

    for (int vi=0; vi<numVoices; vi++)
      {
        voiceUsedCheckBoxes[vi].addItemListener(this);
        voiceTacetTexts[vi].addActionListener(this);
        voiceVersionsPanels[vi].registerListeners(this);
      }

    sectionDeleteButton.addActionListener(this);
    sectionCombineButton.addActionListener(this);

    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            ownerSetSectionsMenuDisplaySectionAttribs(false);
          }
        });
  }

  public void unregisterListeners()
  {
    sectionEditorialCheckBox.removeItemListener(this);
    sectionSourceField.removeActionListener(this);
    sectionSourceNumField.removeActionListener(this);
    sectionColorationChooser.removeListener(this);

    for (int vi=0; vi<numVoices; vi++)
      {
        voiceUsedCheckBoxes[vi].removeItemListener(this);
        voiceTacetTexts[vi].removeActionListener(this);
        voiceVersionsPanels[vi].unregisterListeners(this);
      }

    sectionDeleteButton.removeActionListener(this);
    sectionCombineButton.removeActionListener(this);

    for (WindowListener w : getListeners(WindowListener.class))
      removeWindowListener(w);
  }
}
