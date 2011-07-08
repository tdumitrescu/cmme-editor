/*----------------------------------------------------------------------*/
/*

        Module          : SelectionPanel.java

        Package         : Gfx

        Classes Included: SelectionPanel

        Purpose         : GUI item allowing user to select items

        Programmer      : Ted Dumitrescu

        Date Started    : 12/1/08 (moved from CriticalNotesWindow and
                          TextDeleteDialog)

        Updates         :
12/2/08: added radio button support
         generalized from VersionsCheckBoxPanel to generic selection panel

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   SelectionPanel
Extends: JPanel
Purpose: GUI panel with generic selection elements
------------------------------------------------------------------------*/

public class SelectionPanel extends JPanel
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int CHECKBOX=0,
                          RADIOBUTTON=1;

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int            selectionType, /* check box, radio button, etc */
                        numItems;
  public JCheckBox[]    checkBoxes;
  public JRadioButton[] radioButtons;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: SelectionPanel(String title,List<String> itemNames,
                            int selectionType,int numItemsPerRow)
Purpose:     Init panel
Parameters:
  Input:  String title           - panel title
          List<String> itemNames - names of buttons/boxes to add
          int selectionType      - type of selection element
          int numItemsPerRow     - number of elements in each row of panel
  Output: -
------------------------------------------------------------------------*/

  public SelectionPanel(String title,String[] itemNames,
                        int selectionType,int numItemsPerRow)
  {
    this(title,Arrays.asList(itemNames),selectionType,numItemsPerRow);
  }

  public SelectionPanel(String title,List<String> itemNames,
                        int selectionType,int numItemsPerRow)
  {
    this.selectionType=selectionType;
    this.numItems=itemNames.size();
    int numItemLevels=numItems/numItemsPerRow;
    if (numItems%numItemsPerRow!=0)
      numItemLevels++;

    ButtonGroup itemGroup=new ButtonGroup();
    switch (selectionType)
      {
        case CHECKBOX:
          checkBoxes=new JCheckBox[numItems];
          radioButtons=new JRadioButton[0];
          break;
        case RADIOBUTTON:
          checkBoxes=new JCheckBox[0];
          radioButtons=new JRadioButton[numItems];
          break;
      }

    Box[] itemRowPanes=new Box[numItemLevels];
    int curBox=-1;
    for (int i=0; i<numItems; i++)
      {
        if (i%numItemsPerRow==0)
          itemRowPanes[++curBox]=Box.createHorizontalBox();

        switch (selectionType)
          {
            case CHECKBOX:
              checkBoxes[i]=new JCheckBox(itemNames.get(i));
              checkBoxes[i].setSelected(true);
              itemRowPanes[curBox].add(checkBoxes[i]);
              break;
            case RADIOBUTTON:
              radioButtons[i]=new JRadioButton(itemNames.get(i));
              if (i==0)
                radioButtons[i].setSelected(true);
              itemGroup.add(radioButtons[i]);
              itemRowPanes[curBox].add(radioButtons[i]);
              break;
          }
        itemRowPanes[curBox].add(Box.createHorizontalStrut(5));
      }

    this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    for (Box b : itemRowPanes)
      {
        b.add(Box.createHorizontalGlue());
        b.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        this.add(b);
      }
    if (title!=null)
      this.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder(title),
        BorderFactory.createEmptyBorder(5,5,5,5)));
  }

/*------------------------------------------------------------------------
Method:  void [un]registerListeners()
Purpose: Add and remove event listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void registerListeners(ItemListener listener)
  {
    for (JCheckBox cb : checkBoxes)
      cb.addItemListener(listener);
    for (JRadioButton rb : radioButtons)
      rb.addItemListener(listener);
  }

  public void unregisterListeners(ItemListener listener)
  {
    for (JCheckBox cb : checkBoxes)
      cb.removeItemListener(listener);
    for (JRadioButton rb : radioButtons)
      rb.removeItemListener(listener);
  }
}
