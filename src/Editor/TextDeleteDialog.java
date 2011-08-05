/*----------------------------------------------------------------------*/
/*

        Module          : TextDeleteDialog.java

        Package         : Editor

        Classes Included: TextDeleteDialog

        Purpose         : GUI for deleting text from score

        Programmer      : Ted Dumitrescu

        Date Started    : 11/26/08

        Updates         :
7/13/2011: split into [Original|Modern]TextDeleteDialog

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import DataStruct.*;
import Gfx.*;

/*------------------------------------------------------------------------
Class:   TextDeleteDialog
Extends: JDialog
Purpose: GUI for deleting text from score
------------------------------------------------------------------------*/

abstract class TextDeleteDialog extends JDialog implements ActionListener
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static final int VOICE_BOXES_PER_ROW=12;

/*----------------------------------------------------------------------*/
/* Instance variables */

  EditorWin owner;
  PieceData musicData;

  LinkedList<Component> allPanels;

  JCheckBox[]    voiceCheckBoxes;
  JButton        OKButton,
                 cancelButton;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: TextDeleteDialog(EditorWin owner)
Purpose:     Initialize and lay out frame
Parameters:
  Input:  EditorWin owner - parent frame
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public TextDeleteDialog(EditorWin owner)
  {
    super(owner,"Delete text",true);
    this.owner=owner;
    this.musicData=owner.getMusicData();
    initLayout();
  }

  void initLayout()
  {
    Container cp=getContentPane();
    cp.setLayout(new BoxLayout(cp,BoxLayout.Y_AXIS));

    createPanels();
    addPanelsToLayout(cp);
    registerListeners();

    pack();
    setLocationRelativeTo(owner);
    setVisible(true);
  }

  void createPanels()
  {
    allPanels=new LinkedList<Component>();
    allPanels.add(createVoicesPanel());
    allPanels.add(createButtonPane());
  }

  void addPanelsToLayout(Container cp)
  {
    for (Component p : allPanels)
      cp.add(p);
  }

  JPanel createVoicesPanel()
  {
    int numVoices=this.musicData.getVoiceData().length;
    JPanel voicesPanel=new JPanel();
    voicesPanel.setLayout(new BoxLayout(voicesPanel,BoxLayout.Y_AXIS));

    voiceCheckBoxes=new JCheckBox[numVoices];
    int numVoiceLevels=voiceCheckBoxes.length/VOICE_BOXES_PER_ROW;
    if (voiceCheckBoxes.length%VOICE_BOXES_PER_ROW!=0)
      numVoiceLevels++;

    Box[] voiceBoxesPanes=new Box[numVoiceLevels];
    int curBox=-1;
    for (int i=0; i<voiceCheckBoxes.length; i++)
      {
        if (i%VOICE_BOXES_PER_ROW==0)
          voiceBoxesPanes[++curBox]=Box.createHorizontalBox();

        voiceCheckBoxes[i]=new JCheckBox(String.valueOf(i+1));
        voiceCheckBoxes[i].setSelected(true);
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

    return voicesPanel;
  }

  Box createButtonPane()
  {
    /* action buttons */
    OKButton=new JButton("Delete");
    cancelButton=new JButton("Cancel");
    Box buttonPane=Box.createHorizontalBox();
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(OKButton);
    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(cancelButton);
    buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    return buttonPane;
  }

/*------------------------------------------------------------------------
Method:  void deleteText()
Purpose: Delete text according to selected GUI parameters
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  abstract void deleteText();

/*------------------------------------------------------------------------
Method:     void actionPerformed(ActionEvent event)
Implements: ActionListener.actionPerformed
Purpose:    Check for action types in GUI and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void actionPerformed(ActionEvent event)
  {
    Object item=event.getSource();

    if (item==OKButton)
      {
        deleteText();
        closeDialog();
      }
    else if (item==cancelButton)
      closeDialog();
  }

/*------------------------------------------------------------------------
Method:  void closeDialog()
Purpose: Clean up and disappear
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void closeDialog()
  {
    setVisible(false);
    unregisterListeners();
    dispose();
  }

/*------------------------------------------------------------------------
Method:  void (un)registerListeners()
Purpose: Register/unregister GUI listeners (register and unregister should match)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void registerListeners()
  {
    OKButton.addActionListener(this);
    cancelButton.addActionListener(this);
  }

  public void unregisterListeners()
  {
    OKButton.removeActionListener(this);
    cancelButton.removeActionListener(this);
  }
}
