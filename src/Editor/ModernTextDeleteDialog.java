/*----------------------------------------------------------------------*/
/*

        Module          : ModernTextDeleteDialog.java

        Package         : Editor

        Purpose         : GUI for deleting modern text from score

        Programmer      : Ted Dumitrescu

        Date Started    : 11/26/08 (TextDeleteDialog)

        Updates         :
7/13/2011: split into [Original|Modern]TextDeleteDialog

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

import DataStruct.*;
import Gfx.*;

class ModernTextDeleteDialog extends TextDeleteDialog
{
  public ModernTextDeleteDialog(EditorWin owner)
  {
    super(owner);
  }

/* @Overrides TextDeleteDialog.deleteText */
  void deleteText()
  {
    boolean[] voicesToDelete=new boolean[musicData.getVoiceData().length];

    for (int i=0; i<voiceCheckBoxes.length; i++)
      voicesToDelete[i]=voiceCheckBoxes[i].isSelected();

    owner.deleteModernText(voicesToDelete);
  }
}