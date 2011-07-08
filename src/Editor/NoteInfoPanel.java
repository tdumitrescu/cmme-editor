/*----------------------------------------------------------------------*/
/*

        Module          : NoteInfoPanel.java

        Package         : Editor

        Classes Included: NoteInfoPanel

        Purpose         : GUI panel for viewing/editing information about
                          one note event

        Programmer      : Ted Dumitrescu

        Date Started    : 9/21/07 (moved from EditorWin.java)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import javax.swing.*;
import java.awt.*;
import java.awt.image.*;

import Gfx.*;
import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   NoteInfoPanel
Extends: JPanel
Purpose: GUI for viewing/editing note info
------------------------------------------------------------------------*/

class NoteInfoPanel extends JPanel
{
/*----------------------------------------------------------------------*/
/* Class variables */

  /* standard note shape canvas sizes */
  static int XSMALL=200,YSMALL=200,
             XLARGE=500,YLARGE=300;

/*----------------------------------------------------------------------*/
/* Instance variables */

  MusicFont     musicGfx;

  JPanel        shapePanel;
  JLabel        noteShapeLabel;
  ImageIcon     noteShapeIcon;
  BufferedImage smallNoteShapeImage,
                largeNoteShapeImage,
                XlargeNoteShapeImage;
  Graphics2D    smallG,
                largeG;

/*------------------------------------------------------------------------
Constructor: NoteInfoPanel(MusicFont mf)
Purpose:     Initialize and lay out note info panel
Parameters:
  Input:  MusicFont mf - music font (for drawing notes)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public NoteInfoPanel(MusicFont mf)
  {
    super();

    musicGfx=mf;

    smallNoteShapeImage=new BufferedImage(XSMALL,YSMALL,BufferedImage.TYPE_INT_ARGB);
    smallG=smallNoteShapeImage.createGraphics();
    smallG.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

    noteShapeIcon=new ImageIcon(smallNoteShapeImage);
    noteShapeLabel=new JLabel(noteShapeIcon);
    shapePanel=new JPanel();
    shapePanel.add(noteShapeLabel);

    add(shapePanel);
  }

/*------------------------------------------------------------------------
Method:  void setInfo(RenderedEvent rne)
Purpose: Set values for GUI
Parameters:
  Input:  RenderedEvent rne - data for GUI
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static Color ClearColor=new Color(0,true);

  public void setInfo(RenderedEvent rne)
  {
    smallG.setBackground(ClearColor);
    smallG.clearRect(0,0,XSMALL,YSMALL);
    smallG.setColor(Color.black);

    if (rne.getLigInfo().firstEventNum!=-1)
      rne.drawLig(smallG,musicGfx,this,5,100);
    else
      rne.draw(smallG,musicGfx,this,5,100);

    noteShapeIcon.setImage(smallNoteShapeImage);
    noteShapeLabel.setIcon(noteShapeIcon);
  }
}
