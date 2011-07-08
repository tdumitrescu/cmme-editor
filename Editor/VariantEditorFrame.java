/*----------------------------------------------------------------------*/
/*

        Module          : VariantEditorFrame.java

        Package         : Editor

        Classes Included: VariantEditorFrame

        Purpose         : Editing variant reading meta-data at one location

        Programmer      : Ted Dumitrescu

        Date Started    : 1/22/2008

        Updates         :
7/7/2011: added "Set as default" buttons

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.util.*;
import DataStruct.*;
import Gfx.*;

/*------------------------------------------------------------------------
Class:   VariantEditorFrame
Extends: Gfx.VariantDisplayFrame
Purpose: Displays all variant readings at one location, with controls for
         editing meta-data (which versions are connected to which readings
         etc.)
------------------------------------------------------------------------*/

public class VariantEditorFrame extends VariantDisplayFrame implements ActionListener
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  ScoreEditorCanvas editorCanvas;

  JButton                          consolidateButton;
  LinkedList<JButton[]>            delButtons;
  LinkedList<VariantVersionData[]> versionSets;
  LinkedList<JComboBox>            addVersionBoxes;
  LinkedList<JButton>              setDefaultButtons;
  LinkedList<String>               unusedVersionNames;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantEditorFrame(RenderedEventGroup renderedVar,
                                VoiceEventListData v,ScoreRenderer rs,int vnum,
                                int fx,int fy,ViewCanvas canvas,MusicFont MusicGfx,
                                float STAFFSCALE,float VIEWSCALE)
Parameters:
  Input:  
  Output: -
------------------------------------------------------------------------*/

  public VariantEditorFrame(RenderedEventGroup renderedVar,
                            VoiceEventListData v,ScoreRenderer rs,int vnum,
                            int fx,int fy,ViewCanvas canvas,MusicFont MusicGfx,
                            float STAFFSCALE,float VIEWSCALE)
  {
    super(renderedVar,v,rs,vnum,fx,fy,canvas,MusicGfx,STAFFSCALE,VIEWSCALE);
    this.editorCanvas=(ScoreEditorCanvas)(this.canvas);
  }

  protected JPanel createVersionsPanel(ArrayList<VariantVersionData> versions,boolean defaultVersion)
  {
    JPanel versionsPanel=new JPanel();
    versionsPanel.setLayout(new GridBagLayout());
    GridBagConstraints vpc=new GridBagConstraints();
    vpc.anchor=GridBagConstraints.LINE_START;
    vpc.gridwidth=1;

    JButton[] curDelButtons=new JButton[versions.size()];
    for (int i=0; i<curDelButtons.length; i++)
      {
        curDelButtons[i]=new JButton("Del");
        vpc.gridx=0; vpc.gridy=i; versionsPanel.add(new JLabel(" "+versions.get(i).getID()+" "),vpc);
        if (!defaultVersion)
          {
            vpc.gridx=1; vpc.gridy=i; versionsPanel.add(curDelButtons[i],vpc);
          }
      }
    if (delButtons==null)
      {
        /* first time creating a version panel, initialize master lists of GUI items
           and versions */
        delButtons=new LinkedList<JButton[]>();
        versionSets=new LinkedList<VariantVersionData[]>();
        addVersionBoxes=new LinkedList<JComboBox>();
        setDefaultButtons=new LinkedList<JButton>();
        unusedVersionNames=new LinkedList<String>();
        for (VariantVersionData vvd : canvas.getMusicData().getVariantVersions())
          if (!vvd.isDefault() && vStartEvent.getVariantReading(vvd)==null)
            unusedVersionNames.add(vvd.getID());
      }
    delButtons.add(curDelButtons);
    versionSets.add((VariantVersionData[])versions.toArray(new VariantVersionData[1]));

    if (unusedVersionNames.size()>0 && !defaultVersion)
      {
        JComboBox curAddVersionBox=new JComboBox();
        curAddVersionBox.addItem("Add version...");
        for (String s : unusedVersionNames)
          curAddVersionBox.addItem(s);
        addVersionBoxes.add(curAddVersionBox);
        vpc.gridx=0; vpc.gridy++; vpc.gridwidth=2; versionsPanel.add(curAddVersionBox,vpc);
      }

    if (!defaultVersion)
      {
        JButton curSetDefaultButton=new JButton("Set as default reading");
        vpc.gridx=0; vpc.gridy++; vpc.gridwidth=2; versionsPanel.add(curSetDefaultButton,vpc);
      }

    return versionsPanel;
  }

  protected void createAdditionalControls(Container vrcp,GridBagConstraints vrc)
  {
    JPanel addControlsPanel=new JPanel();
    consolidateButton=new JButton(" Consolidate duplicate readings ");
    addControlsPanel.add(consolidateButton);

    vrc.gridx=0; vrc.gridwidth=2;
    vrcp.add(addControlsPanel,vrc);
    vrc.gridy++;
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

    if (item==consolidateButton)
      ((ScoreEditorCanvas)canvas).consolidateReadings(vStartEvent,this);
    for (int bsi=0; bsi<delButtons.size(); bsi++)
      {
        JButton[] bset=delButtons.get(bsi);
        for (int bi=0; bi<bset.length; bi++)
          if (item==bset[bi])
            ((ScoreEditorCanvas)canvas).deleteVariantReading(vStartEvent,versionSets.get(bsi)[bi],this);
      }
    for (int cbi=0; cbi<addVersionBoxes.size(); cbi++)
      {
        JComboBox cb=addVersionBoxes.get(cbi);
        if (item==cb)
          {
            VariantVersionData newVersion=getVersionFromAddVersionBox(cb);
            if (newVersion!=null)
              editorCanvas.addVersionToReading(vStartEvent,cbi,newVersion,this);
          }
      }
    for (int sdbi=0; sdbi<setDefaultButtons.size(); sdbi++)
      {
        JButton sdb=setDefaultButtons.get(sdbi);
        if (item==sdb && this.canvas.parentwin.confirmAction(
                           "Are you sure?","Confirm reading swap"))
          editorCanvas.setReadingAsDefault(vStartEvent,sdbi);
      }
  }

  VariantVersionData getVersionFromAddVersionBox(JComboBox cb)
  {
    int vi=cb.getSelectedIndex();
    if (vi<1)
      return null;

    String vName=(String)cb.getItemAt(vi);
    for (VariantVersionData vvd : canvas.getMusicData().getVariantVersions())
      if (vvd.getID().equals(vName))
        return vvd;

    return null;
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
    super.registerListeners();
    consolidateButton.addActionListener(this);
    for (JButton[] bset : delButtons)
      for (JButton b : bset)
        b.addActionListener(this);
    for (JComboBox cb : addVersionBoxes)
      cb.addActionListener(this);
    for (JButton sdb : setDefaultButtons)
      sdb.addActionListener(this);
  }

  protected void unregisterListeners()
  {
    super.unregisterListeners();
    consolidateButton.removeActionListener(this);
    for (JButton[] bset : delButtons)
      for (JButton b : bset)
        b.removeActionListener(this);
    for (JComboBox cb : addVersionBoxes)
      cb.removeActionListener(this);
    for (JButton sdb : setDefaultButtons)
      sdb.removeActionListener(this);
  }
}
