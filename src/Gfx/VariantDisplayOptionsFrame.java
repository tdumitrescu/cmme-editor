/*----------------------------------------------------------------------*/
/*

        Module          : VariantDisplayOptionsFrame.java

        Package         : Gfx

        Classes Included: VariantDisplayOptionsFrame

        Purpose         : GUI for selecting variant-display options in viewer

        Programmer      : Ted Dumitrescu

        Date Started    : 12/2/08

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   VariantDisplayOptionsFrame
Extends: JDialog
Purpose: Frame with GUI controls for variant-display
------------------------------------------------------------------------*/

public class VariantDisplayOptionsFrame extends JDialog implements ItemListener
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  MusicWin ownerWin;

  SelectionPanel versionsPanel,
                 variantTypesPanel;
  JRadioButton   buttonAllVariants,
                 buttonNoVariants,
                 buttonSelectedVariants;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  long calcVarFlags(JCheckBox[] variantTypeCheckBoxes)
Purpose: Calculate flag set for filtering variant types based on user selection
Parameters:
  Input:  JCheckBox[] variantTypeCheckBoxes - array of check boxes for all
                                              variant types
  Output: -
  Return: flag set representing user-selected variant types
------------------------------------------------------------------------*/

  public static long calcVarFlags(JCheckBox[] variantTypeCheckBoxes)
  {
    long newFlags=VariantReading.VAR_NONE,
         curFlagVal=VariantReading.VAR_NONSUBSTANTIVE;

    for (int i=0; i<variantTypeCheckBoxes.length; i++)
      {
        if (variantTypeCheckBoxes[i].isSelected())
          newFlags|=curFlagVal;
        curFlagVal<<=1;
      }

    return newFlags;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantDisplayOptionsFrame(MusicWin ownerWin)
Purpose:     Lay out frame
Parameters:
  Input:  MusicWin ownerWin - music window to which this frame is connected
  Output: -
------------------------------------------------------------------------*/

  public VariantDisplayOptionsFrame(MusicWin ownerWin)
  {
    super(ownerWin,"Variant display options",false);
    this.ownerWin=ownerWin;

    versionsPanel=createVersionsPanel(ownerWin.getMusicData().getVariantVersionNames());
    JPanel variantsPanel=createVariantsPanel();

    Container cp=this.getContentPane();
    cp.setLayout(new BoxLayout(cp,BoxLayout.Y_AXIS));
    cp.add(versionsPanel);
    cp.add(variantsPanel);
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

  /* versions panel */

  SelectionPanel createVersionsPanel(ArrayList<String> versionNames)
  {
    SelectionPanel versionsPanel=new SelectionPanel(
      "Display version",versionNames,SelectionPanel.RADIOBUTTON,4);

    return versionsPanel;
  }

  /* variants panel */

  JPanel createVariantsPanel()
  {
    /* variant category controls */
    buttonAllVariants=new JRadioButton("All variants");
    buttonNoVariants=new JRadioButton("No variants");
    buttonSelectedVariants=new JRadioButton("Selected variant types:");
    buttonNoVariants.setSelected(true);

    ButtonGroup categoryButtonGroup=new ButtonGroup();
    categoryButtonGroup.add(buttonAllVariants);
    categoryButtonGroup.add(buttonNoVariants);
    categoryButtonGroup.add(buttonSelectedVariants);

    Box buttonPane=Box.createHorizontalBox();
    buttonPane.add(buttonAllVariants);
    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(buttonNoVariants);
    buttonPane.add(Box.createHorizontalStrut(10));
    buttonPane.add(buttonSelectedVariants);
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    /* variant types */
    variantTypesPanel=new SelectionPanel(
      null,VariantReading.typeNames,SelectionPanel.CHECKBOX,5);
    for (JCheckBox cb : variantTypesPanel.checkBoxes)
      {
        cb.setSelected(false);
        if (!buttonSelectedVariants.isSelected())
          cb.setEnabled(false);
      }
    variantTypesPanel.checkBoxes[VariantReading.varIndex(VariantReading.VAR_RHYTHM)].setSelected(true);
    variantTypesPanel.checkBoxes[VariantReading.varIndex(VariantReading.VAR_PITCH)].setSelected(true);
    variantTypesPanel.checkBoxes[VariantReading.varIndex(VariantReading.VAR_ACCIDENTAL)].setSelected(true);
    variantTypesPanel.checkBoxes[VariantReading.varIndex(VariantReading.VAR_COLORATION)].setSelected(true);
    variantTypesPanel.checkBoxes[VariantReading.varIndex(VariantReading.VAR_LIGATURE)].setSelected(true);
    variantTypesPanel.checkBoxes[VariantReading.varIndex(VariantReading.VAR_MENSSIGN)].setSelected(true);

    JPanel variantsPanel=new JPanel();
    variantsPanel.setLayout(new BoxLayout(variantsPanel,BoxLayout.Y_AXIS));
    variantsPanel.add(buttonPane);
    variantsPanel.add(variantTypesPanel);
    variantsPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Mark on score"),
      BorderFactory.createEmptyBorder(5,5,5,5)));

    return variantsPanel;
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
    try
      {
        Object item=event.getItemSelectable();

        for (int i=0; i<versionsPanel.radioButtons.length; i++)
          if (item==versionsPanel.radioButtons[i])
            ownerWin.setCurrentVariantVersion(i);

        if (item==buttonAllVariants)
          {
            ownerWin.setVariantMarkingOption(OptionSet.OPT_VAR_ALL);
            ownerWin.rerender();
          }
        else if (item==buttonNoVariants)
          {
            ownerWin.setVariantMarkingOption(OptionSet.OPT_VAR_NONE);
            ownerWin.rerender();
          }
        else if (item==buttonSelectedVariants)
          if (buttonSelectedVariants.isSelected())
            {
              for (JCheckBox cb : variantTypesPanel.checkBoxes)
                cb.setEnabled(true);
              ownerWin.setVariantMarkingOption(
                OptionSet.OPT_VAR_CUSTOM,calcVarFlags(variantTypesPanel.checkBoxes));
              ownerWin.rerender();
            }
          else
            {
              for (JCheckBox cb : variantTypesPanel.checkBoxes)
                cb.setEnabled(false);
            }
        for (JCheckBox cb : variantTypesPanel.checkBoxes)
          if (item==cb)
            {
              ownerWin.setVariantMarkingOption(
                OptionSet.OPT_VAR_CUSTOM,calcVarFlags(variantTypesPanel.checkBoxes));
              ownerWin.rerender();
            }
      }
    catch (Exception e)
      {
        ownerWin.handleRuntimeError(e);
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

  void ownerSetVersionsMenuDisplayVariantOptions(boolean newval)
  {
    ownerWin.setVersionsMenuDisplayVariantOptions(newval);
  }

  public void registerListeners()
  {
    versionsPanel.registerListeners(this);
    variantTypesPanel.registerListeners(this);
    buttonAllVariants.addItemListener(this);
    buttonNoVariants.addItemListener(this);
    buttonSelectedVariants.addItemListener(this);
    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            ownerSetVersionsMenuDisplayVariantOptions(false);
          }
        });
  }

  public void unregisterListeners()
  {
    versionsPanel.unregisterListeners(this);
    variantTypesPanel.unregisterListeners(this);
    buttonAllVariants.removeItemListener(this);
    buttonNoVariants.removeItemListener(this);
    buttonSelectedVariants.removeItemListener(this);
    for (WindowListener w : getListeners(WindowListener.class))
      removeWindowListener(w);
  }
}
