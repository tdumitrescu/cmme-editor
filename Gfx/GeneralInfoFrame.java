/*----------------------------------------------------------------------*/
/*

        Module          : GeneralInfoFrame.java

        Package         : Gfx

        Classes Included: GeneralInfoFrame

        Purpose         : Window to display basic information about one
                          edition

        Programmer      : Ted Dumitrescu

        Date Started    : 12/31/08

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.util.*;
import javax.swing.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   GeneralInfoFrame
Extends: JDialog
Purpose: Display basic information about one edition
------------------------------------------------------------------------*/

public class GeneralInfoFrame extends JDialog
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  MusicWin ownerWin;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: GeneralInfoFrame(MusicWin ownerWin)
Purpose:     Lay out frame
Parameters:
  Input:  MusicWin ownerWin - music window to which this frame is connected
  Output: -
------------------------------------------------------------------------*/

  public GeneralInfoFrame(MusicWin ownerWin)
  {
    super(ownerWin,"About this edition",false);
    this.ownerWin=ownerWin;

    Container cp=this.getContentPane();
    cp.add(createTextPanel(ownerWin.getMusicData()));
    pack();
    setLocationRelativeTo(ownerWin);
  }

/*------------------------------------------------------------------------
Method:  JPanel create*Panel()
Purpose: Initialize individual panes within frame
Parameters:
  Input:  -
  Output: -
  Return: one frame section as JPanel
------------------------------------------------------------------------*/

  /* text panel */

  JPanel createTextPanel(PieceData musicData)
  {
    /* construct HTML of info text */
    String infoHTML="";
    infoHTML+="<style type=\"text/css\">"+
                "h1         { text-align:center }"+
                "h2         { font-style:italic; text-align:center }"+
                "h3         { color:red }"+
                "span.label { color:red; font-style:italic }"+
              "</style>\n";
    infoHTML+="<h1>"+musicData.getComposer()+": "+musicData.getTitle()+"</h1>\n";
    if (musicData.getSectionTitle()!=null)
      infoHTML+="<h2>"+musicData.getSectionTitle()+"</h2>\n";
    infoHTML+="<p><span class=\"label\">Editor:</span> "+musicData.getEditor();
    if (musicData.getPublicNotes()!=null && musicData.getPublicNotes().length()>0)
      infoHTML+="<br/><span class=\"label\">Notes:</span> "+
                musicData.getPublicNotes().replaceAll("\\n","<br/>");
    VariantVersionData defaultVersion=musicData.getDefaultVariantVersion();
    if (defaultVersion.getSourceName()!=null)
      infoHTML+="<br/><span class=\"label\">Main source:</span> "+defaultVersion.getSourceName();
    infoHTML+="</p>\n";

    ArrayList<VariantVersionData> versions=musicData.getVariantVersions();
    if (versions.size()>1)
      {
        infoHTML+="<hr/>\n";
        infoHTML+="<h3>Variant Versions</h3>\n";
        infoHTML+="<p>\n";
        for (int vi=1; vi<versions.size(); vi++)
          {
            VariantVersionData vvd=versions.get(vi);
            infoHTML+=vvd.getID();
            if (vvd.getSourceName()!=null && !vvd.getID().equals(vvd.getSourceName()))
              infoHTML+=" (source: "+vvd.getSourceName()+")";
            infoHTML+="<br/>\n";
          }
        infoHTML+="</p>\n";
      }

    /* add to panel */
    JEditorPane textArea=new JEditorPane("text/html",infoHTML);
    textArea.setEditable(false);

    JPanel textPanel=new JPanel();
    textPanel.add(textArea);
    return textPanel;
  }
}
