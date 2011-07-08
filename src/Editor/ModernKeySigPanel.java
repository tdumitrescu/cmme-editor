/*----------------------------------------------------------------------*/
/*

        Module          : ModernKeySigPanel.java

        Package         : Editor

        Classes Included: ModernKeySigPanel

        Purpose         : GUI panel for viewing/editing information about
                          one modern key signature event

        Programmer      : Ted Dumitrescu

        Date Started    : 9/21/07 (moved from EditorWin.java)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

import Gfx.*;
import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   ModernKeySigPanel
Extends: JPanel
Purpose: GUI for viewing/editing modern key signature info
------------------------------------------------------------------------*/

class ModernKeySigPanel extends JPanel
{
/*----------------------------------------------------------------------*/
/* Class variables */

  /* standard canvas sizes */
  static final int XSMALL=200,YSMALL=200,
                   XLARGE=500,YLARGE=300,

                   STAFFPOSSCALE=5,STAFFSCALE=STAFFPOSSCALE*2,
                   YTOP=YSMALL/2-STAFFSCALE*2,
                   XMARGIN=15;

/*----------------------------------------------------------------------*/
/* Instance variables */

  MusicFont     musicGfx;

  JPanel        sigPanel;
  JLabel        sigLabel;
  ImageIcon     sigIcon;
  BufferedImage sigImage;
  Graphics2D    sigG;

/*------------------------------------------------------------------------
Constructor: ModernKeySigPanel(MusicFont mf)
Purpose:     Initialize and lay out modern key signature panel
Parameters:
  Input:  MusicFont mf - music font (for drawing music)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ModernKeySigPanel(MusicFont mf)
  {
    super();

    musicGfx=mf;

    sigImage=new BufferedImage(XSMALL,YSMALL,BufferedImage.TYPE_INT_ARGB);
    sigG=sigImage.createGraphics();

    sigIcon=new ImageIcon(sigImage);
    sigLabel=new JLabel(sigIcon);
    sigPanel=new JPanel();
    sigPanel.add(sigLabel);

    add(sigPanel);
  }

/*------------------------------------------------------------------------
Method:  void setInfo(ModernKeySignature sigInfo)
Purpose: Set values for GUI
Parameters:
  Input:  ModernKeySignature sigInfo - data for GUI
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static Color ClearColor=new Color(0,true);

  public void setInfo(ModernKeySignature sigInfo)
  {
    int curx=XMARGIN;

    sigG.setBackground(ClearColor);
    sigG.clearRect(0,0,XSMALL,YSMALL);
    sigG.setColor(Color.black);

    /* draw staff and clef */
    drawStaff(sigG,0,YTOP,XSMALL,STAFFSCALE,5);
    musicGfx.drawGlyph(sigG,MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNG,
                       curx,YTOP+STAFFSCALE*4-STAFFPOSSCALE*2,Color.black);
    curx+=musicGfx.getGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNG);

    /* draw individual accidentals in signature */
    for (Iterator i=sigInfo.iterator(); i.hasNext();)
      {
        ModernKeySignatureElement kse=(ModernKeySignatureElement)i.next();

        for (int ai=0; ai<kse.accidental.numAcc; ai++)
          {
            musicGfx.drawGlyph(sigG,MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlat+kse.accidental.accType,
                               curx,YTOP+STAFFSCALE*4-STAFFPOSSCALE*(3+kse.calcAOffset()),Color.black);
            curx+=musicGfx.getGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlat+kse.accidental.accType)+MusicFont.CONNECTION_SCREEN_MODACC_DBLFLAT;
          }
      }

    sigIcon.setImage(sigImage);
    sigLabel.setIcon(sigIcon);
  }

/*------------------------------------------------------------------------
Method:  void drawStaff(Graphics2D g,int xloc,int yloc,int xsize,int yscale,int numlines)
Purpose: Draw staff at specified location
Parameters:
  Input:  Graphics2D g - graphical context
          int xloc     - x location for left end of staff
          int yloc     - y location for top of staff
          int xsize    - x size of staf
          int yscale   - vertical size of one staff space
          int numlines - number of lines for staff
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawStaff(Graphics2D g,int xloc,int yloc,int xsize,int yscale,int numlines)
  {
    for (int i=0; i<numlines; i++)
      g.drawLine(xloc,yloc+i*yscale,
                 xloc+xsize-1,yloc+i*yscale);
  }
}
