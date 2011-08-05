/*----------------------------------------------------------------------*/
/*

        Module          : OriginalTextDeleteDialog.java

        Package         : Editor

        Purpose         : GUI for deleting original text from score

        Programmer      : Ted Dumitrescu

        Date Started    : 11/26/08 (TextDeleteDialog)

        Updates         :
7/13/2011: split into [Original|Modern]TextDeleteDialog

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import DataStruct.*;
import Gfx.*;

class OriginalTextDeleteDialog extends TextDeleteDialog
{
  SelectionPanel versionsPanel;

  public OriginalTextDeleteDialog(EditorWin owner)
  {
    super(owner);
  }

/* @Overrides TextDeleteDialog.createPanels */
  void createPanels()
  {
    super.createPanels();
    versionsPanel=createVersionsPanel();
  }

/* @Overrides TextDeleteDialog.addPanelsToLayout */
  void addPanelsToLayout(Container cp)
  {
    cp.add(versionsPanel);
    super.addPanelsToLayout(cp);
  }

/* @Overrides TextDeleteDialog.deleteText */
  void deleteText()
  {
    ArrayList<VariantVersionData> versionsToDelete=new ArrayList<VariantVersionData>();
    boolean[]                     voicesToDelete=new boolean[musicData.getVoiceData().length];

    for (int i=0; i<versionsPanel.checkBoxes.length; i++)
      if (versionsPanel.checkBoxes[i].isSelected())
        versionsToDelete.add(musicData.getVariantVersions().get(i));
    for (int i=0; i<voiceCheckBoxes.length; i++)
      voicesToDelete[i]=voiceCheckBoxes[i].isSelected();

    owner.deleteOriginalText(versionsToDelete,voicesToDelete);
  }

  SelectionPanel createVersionsPanel()
  {
    SelectionPanel versionsPanel=new SelectionPanel("Versions",musicData.getVariantVersionNames(),
                                                    SelectionPanel.CHECKBOX,4);
    versionsPanel.checkBoxes[0].setSelected(false);
    versionsPanel.checkBoxes[0].setEnabled(false);
    for (int i=1; i<versionsPanel.checkBoxes.length; i++)
      versionsPanel.checkBoxes[i].setSelected(
        musicData.getVariantVersion(i)==owner.getCurrentVariantVersion());

    return versionsPanel;
  }
}