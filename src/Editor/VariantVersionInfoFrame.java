/*----------------------------------------------------------------------*/
/*

        Module          : VariantVersionInfoFrame.java

        Package         : Editor

        Classes Included: VariantVersionInfoFrame,VersionPanel

        Purpose         : GUI panel for modifying variant version
                          information

        Programmer      : Ted Dumitrescu

        Date Started    : 10/20/07

        Updates:
2/14/08: fixed bug where adding a new version prevented further switching
         of versions on the score
7/19/08: encapsulated GUI for individual versions in class VersionPanel
         added controls to specify missing voices

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import DataStruct.*;
import Gfx.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   VariantVersionInfoFrame
Extends: JDialog
Purpose: Frame for modifying variant version information
------------------------------------------------------------------------*/

public class VariantVersionInfoFrame extends JDialog implements ActionListener,ItemListener
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final Dimension DEFAULT_PANESIZE=new Dimension(700,600);

/*----------------------------------------------------------------------*/
/* Instance variables */

  EditorWin                     owner;
  int                           numVersions=0,
                                numVoices;
  ArrayList<VariantVersionData> newVersionsList;
  ArrayList<Integer>            originalVersionNums; /* for matching newVersionsList
                                                        to original list */

  JPanel         versionsPanel=null;
  JScrollPane    versionsScrollPane=null;
  VersionPanel[] versionPanels;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantVersionInfoFrame(EditorWin owner)
Purpose:     Initialize and lay out frame
Parameters:
  Input:  EditorWin owner - parent frame
  Output: -
------------------------------------------------------------------------*/

  public VariantVersionInfoFrame(EditorWin owner)
  {
    super(owner,"Variant Version Information",true);
    this.owner=owner;

    Container cp=getContentPane();

    numVoices=owner.getMusicData().getVoiceData().length;
    ArrayList<VariantVersionData> versions=owner.getMusicData().getVariantVersions();
    originalVersionNums=new ArrayList<Integer>();
    for (int vi=0; vi<versions.size(); vi++)
      originalVersionNums.add(new Integer(vi));
    initVersionsPanel(versions);

    Box buttonPane=createButtonPane();
    JScrollPane versionsScrollPane=new JScrollPane(versionsPanel,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    versionsScrollPane.setPreferredSize(MusicWin.fitInScreen(DEFAULT_PANESIZE,0.8f));
    versionsScrollPane.setMinimumSize(new Dimension(20,20));
    cp.add(versionsScrollPane,BorderLayout.NORTH);
    cp.add(buttonPane,BorderLayout.SOUTH);

    addListeners();
    pack();
    setLocationRelativeTo(owner);
  }

/*------------------------------------------------------------------------
Method:  void close()
Purpose: Close dialog
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void close()
  {
    setVisible(false);
    removeListeners();
    dispose();
  }

/*------------------------------------------------------------------------
Method:  void saveAndClose()
Purpose: Save information and close dialog
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void saveAndClose()
  {
    updateNewVersionsList();
    owner.getMusicData().updateVariantVersions(newVersionsList,originalVersionNums);
    owner.initVariantVersionsBox();
    owner.pack();
    close();
  }

/*------------------------------------------------------------------------
Method:  void updateNewVersionsList()
Purpose: Load newVersionsList with user-input values from GUI
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void updateNewVersionsList()
  {
    for (int vi=0; vi<numVersions; vi++)
      {
        VariantVersionData v=newVersionsList.get(vi);
        v.setID(versionPanels[vi].IDField.getText());
        int sourceID=-1;
        try { sourceID=Integer.parseInt(versionPanels[vi].sourceIDField.getText(),10); }
          catch(NumberFormatException nfe) {}

        String sourceName=versionPanels[vi].sourceNameField.getText(),
               versionEditor=versionPanels[vi].editorField.getText(),
               versionDescr=versionPanels[vi].descriptionArea.getText();
        if (sourceName.equals(""))
          sourceName=null;
        if (versionEditor.equals(""))
          versionEditor=null;
        if (versionDescr.equals(""))
          versionDescr=null;
        v.setSourceInfo(sourceName,sourceID);
        v.setEditor(versionEditor);
        v.setDescription(versionDescr);

        for (int i=0; i<numVoices; i++)
          v.setMissingVoice(owner.getMusicData().getVoiceData()[i],versionPanels[vi].missingVoiceBoxes[i].isSelected());
      }
  }

/*------------------------------------------------------------------------
Method:  JPanel createVersionsPanel(ArrayList<VariantVersionData> versions)
Purpose: Create content for versions list panel
Parameters:
  Input:  ArrayList<VariantVersionData> versions - versions lsit for display
  Output: -
  Return: new JPanel containing versions info
------------------------------------------------------------------------*/

  void initVersionsPanel(ArrayList<VariantVersionData> versions)
  {
    if (versionsPanel==null)
      versionsPanel=new JPanel();
    else
      {
        versionsPanel.removeAll();
        removeVersionsPanelListeners();
      }

    numVersions=versions.size();
    /* copy list into newVersions */
    newVersionsList=new ArrayList<VariantVersionData>();
    for (VariantVersionData vvd : versions)
      newVersionsList.add(new VariantVersionData(vvd));

    versionsPanel.setLayout(new BoxLayout(versionsPanel,BoxLayout.Y_AXIS));
/*    GridBagConstraints vic=new GridBagConstraints();
    vic.anchor=GridBagConstraints.LINE_START;*/
    versionsPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Versions"),
      BorderFactory.createEmptyBorder(5,5,5,5)));

    if (numVersions==0)
      {
        JLabel noVariantLabel=new JLabel("No variant versions");
        noVariantLabel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        versionsPanel.add(noVariantLabel);

        return;
      }

    versionPanels=new VersionPanel[numVersions];
    for (int vi=0; vi<numVersions; vi++)
      {
        versionPanels[vi]=new VersionPanel(newVersionsList,vi,numVoices);
        versionPanels[vi].addListeners(this);
        versionsPanel.add(versionPanels[vi]);
      }
  }

/*------------------------------------------------------------------------
Method:  void [add|remove]VersionsPanelListeners()
Purpose: Register/unregister GUI listeners for versions panel
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void removeVersionsPanelListeners()
  {
    for (VersionPanel vp : versionPanels)
      vp.removeListeners(this);
  }

/*------------------------------------------------------------------------
Method:  Box createButtonPane()
Purpose: Create and lay out main button controls
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  JButton addButton,
          OKButton,cancelButton;

  Box createButtonPane()
  {
    addButton=new JButton("Add Version");
    OKButton=new JButton("Apply");
    cancelButton=new JButton("Cancel");

    Box buttonPane=Box.createHorizontalBox();
    buttonPane.add(addButton);
    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(OKButton);
    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(cancelButton);
    buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    return buttonPane;
  }

/*------------------------------------------------------------------------
Method:  void [move|insert|delete]Version([int vnum,int offset])
Purpose: Perform operations on one version in versions panel (move up or
         down one place, insert before, delete)
Parameters:
  Input:  int vnum   - version number to modify
          int offset - amount to displace (1 or -1)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void moveVersion(int vnum,int offset)
  {
    updateNewVersionsList();

    VariantVersionData v=newVersionsList.get(vnum);
    newVersionsList.remove(vnum);
    newVersionsList.add(vnum+offset,v);
    Integer origNum=originalVersionNums.get(vnum);
    originalVersionNums.remove(origNum);
    originalVersionNums.add(vnum+offset,origNum);

    initVersionsPanel(newVersionsList);
    pack();
  }

  void insertVersion()
  {
    insertVersion(newVersionsList.size());
  }

  void insertVersion(int vnum)
  {
    updateNewVersionsList();

    newVersionsList.add(vnum,new VariantVersionData("[New version "+(numVersions+1)+"]"));
    originalVersionNums.add(vnum,new Integer(-1));
    initVersionsPanel(newVersionsList);
    pack();
  }

  void deleteVersion(int vnum)
  {
    updateNewVersionsList();

    newVersionsList.remove(vnum);
    originalVersionNums.remove(vnum);
    initVersionsPanel(newVersionsList);
    pack();
  }

/*------------------------------------------------------------------------
Method:  void addListeners()
Purpose: Register GUI listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addListeners()
  {
    addButton.addActionListener(this);
    OKButton.addActionListener(this);
    cancelButton.addActionListener(this);

    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            removeListeners();
          }
        });
  }

/*------------------------------------------------------------------------
Method:  void removeListeners()
Purpose: Unregister GUI listeners (should match addListeners)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removeListeners()
  {
    addButton.removeActionListener(this);
    OKButton.removeActionListener(this);
    cancelButton.removeActionListener(this);

    for (WindowListener wl : getListeners(WindowListener.class))
      removeWindowListener(wl);

    removeVersionsPanelListeners();
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

    if (item==addButton)
      insertVersion();
    else if (item==cancelButton)
      close();
    else if (item==OKButton)
      saveAndClose();

    for (int vi=0; vi<numVersions; vi++)
      if (item==versionPanels[vi].insertBeforeButton)
        {
          insertVersion(vi);
          return;
        }
      else if (item==versionPanels[vi].insertAfterButton)
        {
          insertVersion(vi+1);
          return;
        }
      else if (item==versionPanels[vi].moveUpButton)
        {
          moveVersion(vi,-1);
          return;
        }
      else if (item==versionPanels[vi].moveDownButton)
        {
          moveVersion(vi,1);
          return;
        }
      else if (item==versionPanels[vi].deleteButton)
        {
          deleteVersion(vi);
          return;
        }
  }

/*------------------------------------------------------------------------
Method:     void itemStateChanged(ItemEvent event)
Implements: ItemListener.itemStateChanged
Purpose:    Check for item state changes in GUI and take appropriate action
Parameters:
  Input:  ItemEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void itemStateChanged(ItemEvent event)
  {
    Object item=event.getItemSelectable();
  }
}

/*------------------------------------------------------------------------
Class:   VersionPanel
Extends: JPanel
Purpose: Panel for manipulating one variant version
------------------------------------------------------------------------*/

class VersionPanel extends JPanel
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public JTextField  IDField,
                     sourceNameField,
                     sourceIDField,
                     editorField;
  public JTextArea   descriptionArea;
  public JCheckBox[] missingVoiceBoxes; 
  public JButton     moveUpButton,
                     moveDownButton,
                     insertBeforeButton,
                     insertAfterButton,
                     deleteButton;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VersionPanel(ArrayList<VariantVersionData> versionsList,int vi)
Purpose:     Initialize and lay out panel
Parameters:
  Input:  
  Output: -
------------------------------------------------------------------------*/

  public VersionPanel(ArrayList<VariantVersionData> versionsList,int vi,int numVoices)
  {
    super();
    setLayout(new GridBagLayout());
    GridBagConstraints ovic=new GridBagConstraints();
    ovic.anchor=GridBagConstraints.LINE_START;
    setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder(""),
      BorderFactory.createEmptyBorder(5,5,5,5)));

    VariantVersionData v=versionsList.get(vi);

    JLabel headingID=new JLabel("Version name"),
           headingSourceName=new JLabel("Source name"),
           headingSourceID=new JLabel("Source ID no."),
           headingEditor=new JLabel("Editor"),
           headingDescription=new JLabel("Description");
    headingID.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    headingSourceName.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    headingSourceID.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    headingEditor.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    headingDescription.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
    ovic.gridx=0; ovic.gridy=0; add(headingID,ovic);
    ovic.gridx=1; ovic.gridy=0; add(headingEditor,ovic);
    ovic.gridx=0; ovic.gridy=2; add(headingSourceName,ovic);
    ovic.gridx=1; ovic.gridy=2; add(headingSourceID,ovic);
    ovic.gridx=0; ovic.gridy=4; add(headingDescription,ovic);

    IDField=new JTextField(v.getID(),20);
    sourceNameField=new JTextField(v.getSourceName(),15);
    sourceIDField=new JTextField(v.getSourceIDString(),5);
    editorField=new JTextField(v.getEditor(),15);
    descriptionArea=new JTextArea(v.getDescription(),2,35);
    JScrollPane descriptionAreaPane=new JScrollPane(descriptionArea);

    ovic.gridx=0; ovic.gridy=1; add(IDField,ovic);
    ovic.gridx=1; ovic.gridy=1; add(editorField,ovic);
    ovic.gridx=0; ovic.gridy=3; add(sourceNameField,ovic);
    ovic.gridx=1; ovic.gridy=3; add(sourceIDField,ovic);
    ovic.gridx=0; ovic.gridy=5; ovic.gridwidth=2; add(descriptionAreaPane,ovic);
    ovic.gridwidth=1;

    /* missing voice checkboxes */
    Box missingVoicesBox=Box.createHorizontalBox();
    JLabel headingMissingVoices=new JLabel("Missing voices: ");
    missingVoiceBoxes=new JCheckBox[numVoices];
    for (int i=0; i<numVoices; i++)
      {
        missingVoiceBoxes[i]=new JCheckBox(String.valueOf(i+1));
        missingVoicesBox.add(missingVoiceBoxes[i]);
      }
    for (Voice missingv : v.getMissingVoices())
      missingVoiceBoxes[missingv.getNum()-1].setSelected(true);
    ovic.gridx=0; ovic.gridy=6; add(headingMissingVoices,ovic);
    ovic.gridx=0; ovic.gridy=7; ovic.gridwidth=4; add(missingVoicesBox,ovic);
    ovic.gridwidth=1;

    /* controls */
    moveUpButton=new JButton("Move up");
    moveDownButton=new JButton("Move down");
    insertBeforeButton=new JButton("Insert before");
    insertAfterButton=new JButton("Insert after");
    deleteButton=new JButton("Delete");

    if (vi<=1)
      moveUpButton.setEnabled(false);
    if (vi==versionsList.size()-1)
      moveDownButton.setEnabled(false);

    if (vi>0)
      {
        ovic.gridx=2; ovic.gridy=1; add(Box.createHorizontalStrut(5),ovic);
        ovic.gridx=4; ovic.gridy=1; add(Box.createHorizontalStrut(5),ovic);

        ovic.gridx=3; ovic.gridy=1; add(moveUpButton,ovic);
        ovic.gridx=3; ovic.gridy=3; add(moveDownButton,ovic);
        ovic.gridx=5; ovic.gridy=1; add(insertBeforeButton,ovic);
        ovic.gridx=5; ovic.gridy=3; add(insertAfterButton,ovic);
        ovic.gridx=3; ovic.gridy=5; add(deleteButton,ovic);
      }
  }

/*------------------------------------------------------------------------
Method:  void addListeners(EventListener listener)
Purpose: Register GUI listeners
Parameters:
  Input:  -
  Output: EventListener listener - listener to add
  Return: -
------------------------------------------------------------------------*/

  public void addListeners(EventListener listener)
  {
    moveUpButton.addActionListener((ActionListener)listener);
    moveDownButton.addActionListener((ActionListener)listener);
    insertBeforeButton.addActionListener((ActionListener)listener);
    insertAfterButton.addActionListener((ActionListener)listener);
    deleteButton.addActionListener((ActionListener)listener);
    for (JCheckBox mvb : missingVoiceBoxes)
      mvb.addItemListener((ItemListener)listener);
  }

/*------------------------------------------------------------------------
Method:  void removeListeners(EventListener listener)
Purpose: Unregister GUI listeners (should match addListeners)
Parameters:
  Input:  -
  Output: EventListener listener - listener to remove
  Return: -
------------------------------------------------------------------------*/

  public void removeListeners(EventListener listener)
  {
    moveUpButton.removeActionListener((ActionListener)listener);
    moveDownButton.removeActionListener((ActionListener)listener);
    insertBeforeButton.removeActionListener((ActionListener)listener);
    insertAfterButton.removeActionListener((ActionListener)listener);
    deleteButton.removeActionListener((ActionListener)listener);
    for (JCheckBox mvb : missingVoiceBoxes)
      mvb.removeItemListener((ItemListener)listener);
  }
}