/*----------------------------------------------------------------------*/
/*

        Module          : ColorationChooser.java

        Package         : Editor

        Classes Included: ColorationChooser

        Purpose         : GUI panel for modifying coloration structures

        Programmer      : Ted Dumitrescu

        Date Started    : 9/21/07 (moved from EditorWin.java)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import Gfx.*;
import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   ColorationChooser
Extends: JPanel
Purpose: GUI for modifying coloration structures
------------------------------------------------------------------------*/

class ColorationChooser extends JPanel
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public JComboBox PrimaryColorChooser,PrimaryFillChooser,
                   SecondaryColorChooser,SecondaryFillChooser;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ColorationChooser([Coloration c])
Purpose:     Initialize and lay out coloration chooser
Parameters:
  Input:  Coloration c - initial coloration values
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ColorationChooser()
  {
    super();

    String[] colorNames=new String[Coloration.ColorNames.length],
             colorFillNames=new String[Coloration.ColorFillNames.length];
    for (int ci=0; ci<colorNames.length; ci++)
      colorNames[ci]=" "+Coloration.ColorNames[ci];
    for (int ci=0; ci<colorFillNames.length; ci++)
      colorFillNames[ci]=" "+Coloration.ColorFillNames[ci];

    PrimaryColorChooser=new JComboBox(colorNames);
    PrimaryFillChooser=new JComboBox(colorFillNames);
    SecondaryColorChooser=new JComboBox(colorNames);
    SecondaryFillChooser=new JComboBox(colorFillNames);

    setLayout(new java.awt.GridLayout(2,3));
    JLabel primaryLabel=new JLabel("Primary:");
    primaryLabel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    add(primaryLabel);
    add(PrimaryColorChooser);
    add(PrimaryFillChooser);
    JLabel secondaryLabel=new JLabel("Secondary:");
    secondaryLabel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    add(secondaryLabel);
    add(SecondaryColorChooser);
    add(SecondaryFillChooser);
  }

  public ColorationChooser(Coloration c)
  {
    this();
    setIndices(c);
  }

/*------------------------------------------------------------------------
Method:  void setIndices(Coloration c)
Purpose: Set values of combo boxes
Parameters:
  Input:  Coloration c - data for combo boxes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setIndices(Coloration c)
  {
    PrimaryColorChooser.setSelectedIndex(c.primaryColor);
    PrimaryFillChooser.setSelectedIndex(c.primaryFill);
    SecondaryColorChooser.setSelectedIndex(c.secondaryColor);
    SecondaryFillChooser.setSelectedIndex(c.secondaryFill);
  }

/*------------------------------------------------------------------------
Method:  Coloration createColoration()
Purpose: Generate Coloration structure with values from combo boxes
Parameters:
  Input:  -
  Output: -
  Return: new coloration structure
------------------------------------------------------------------------*/

  public Coloration createColoration()
  {
    return new Coloration(PrimaryColorChooser.getSelectedIndex(),
                          PrimaryFillChooser.getSelectedIndex(),
                          SecondaryColorChooser.getSelectedIndex(),
                          SecondaryFillChooser.getSelectedIndex());
  }

/*------------------------------------------------------------------------
Method:  void addListener(ItemListener aListener)
Purpose: Register GUI listener
Parameters:
  Input:  ItemListener aListener - listener to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addListener(ItemListener aListener)
  {
    PrimaryColorChooser.addItemListener(aListener);
    PrimaryFillChooser.addItemListener(aListener);
    SecondaryColorChooser.addItemListener(aListener);
    SecondaryFillChooser.addItemListener(aListener);
  }

/*------------------------------------------------------------------------
Method:  void removeListener(ItemListener aListener)
Purpose: Unregister GUI listener
Parameters:
  Input:  ItemListener aListener - listener to remove
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removeListener(ItemListener aListener)
  {
    PrimaryColorChooser.removeItemListener(aListener);
    PrimaryFillChooser.removeItemListener(aListener);
    SecondaryColorChooser.removeItemListener(aListener);
    SecondaryFillChooser.removeItemListener(aListener);
  }

/*------------------------------------------------------------------------
Method:  boolean itemSelected(Object o)
Purpose: Check whether a given object corresponds to one of this chooser's
         items
Parameters:
  Input:  Object o - selected item
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public boolean itemSelected(Object o)
  {
    return o==PrimaryColorChooser   ||
           o==PrimaryFillChooser    ||
           o==SecondaryColorChooser ||
           o==SecondaryFillChooser;
  }
}
