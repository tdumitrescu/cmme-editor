/*----------------------------------------------------------------------*/
/*

        Module          : EditingOptionsFrame.java

        Package         : Editor

        Classes Included: EditingOptionsFrame

        Purpose         : GUI panel for modifying editing options

        Programmer      : Ted Dumitrescu

        Date Started    : 9/21/07 (moved from EditorWin.java, created class)

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

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   EditingOptionsFrame
Extends: JDialog
Purpose: Frame for modifying editing options
------------------------------------------------------------------------*/

class EditingOptionsFrame extends JDialog
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  EditorWin    owner;
  JCheckBox    colorationOnCheckBox;
  JRadioButton colorationTypeMinorColor,
               colorationTypeSesquialtera,
               colorationTypeImperfectio;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: EditingOptionsFrame(EditorWin owner)
Purpose:     Initialize and lay out frame
Parameters:
  Input:  EditorWin owner - parent frame
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public EditingOptionsFrame(EditorWin owner)
  {
    super(owner,"Input options",false);

    this.owner=owner;

    Container eecp=getContentPane();
    Box editOptionsBox=Box.createVerticalBox();

    /* note/rest input panel */
    JPanel noteInputPanel=new JPanel();
    noteInputPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Note/Rest Input"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    noteInputPanel.setLayout(new BoxLayout(noteInputPanel,BoxLayout.Y_AXIS));
    Box cBox=Box.createHorizontalBox();
    colorationOnCheckBox=new JCheckBox("Coloration",false);
    cBox.add(colorationOnCheckBox);
    cBox.add(Box.createHorizontalGlue());
    noteInputPanel.add(cBox);

    /* coloration type panel */
    JPanel colorationTypePanel=new JPanel();
    colorationTypePanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Current Coloration Type"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    colorationTypePanel.setLayout(new BoxLayout(colorationTypePanel,BoxLayout.X_AXIS));
    Box ctBox=Box.createVerticalBox();
    colorationTypeMinorColor=new JRadioButton("'Minor color'",true);
    colorationTypeSesquialtera=new JRadioButton("Sesquialtera (3/2)");
    colorationTypeImperfectio=new JRadioButton("Imperfectio");
    ButtonGroup ctGroup=new ButtonGroup();
    ctGroup.add(colorationTypeMinorColor);
    ctGroup.add(colorationTypeSesquialtera);
    ctGroup.add(colorationTypeImperfectio);
    ctBox.add(colorationTypeMinorColor);
    ctBox.add(colorationTypeSesquialtera);
    ctBox.add(colorationTypeImperfectio);
    colorationTypePanel.add(ctBox);
    colorationTypePanel.add(Box.createHorizontalGlue());

    colorationOnCheckBox.addItemListener(owner);
    colorationTypeMinorColor.addActionListener(owner);
    colorationTypeSesquialtera.addActionListener(owner);
    colorationTypeImperfectio.addActionListener(owner);

    editOptionsBox.add(noteInputPanel);
    editOptionsBox.add(colorationTypePanel);
    editOptionsBox.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    eecp.add(editOptionsBox);
    pack();
    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            ownerSetEditMenuEditingOptions(false);
          }
        });
  }

  void ownerSetEditMenuEditingOptions(boolean newval)
  {
    owner.setEditMenuEditingOptions(newval);
  }

/*------------------------------------------------------------------------
Method:  void setLocation(int eox,int eoy)
Purpose: Position frame
Parameters:
  Input:  int eox,eoy - new location to attempt
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setLocation(int eox,int eoy)
  {
    /* position relative to event editor frame 
    int eox=eventEditorFrame.getLocation().x+eventEditorFrame.getSize().width,
        eoy=eventEditorFrame.getLocation().y,*/
    int eoWidth=getSize().width,
        eoHeight=getSize().height;
    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();

    if (eox+eoWidth>screenSize.width)
      eox=screenSize.width-eoWidth;
    if (eoy+eoHeight>screenSize.height)
      eoy=screenSize.height-eoHeight;

    super.setLocation(eox,eoy);
  }

/*------------------------------------------------------------------------
Method:  void [set|toggle]*Option()
Purpose: Control options in frame
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void toggleColorationOption()
  {
    boolean newState=!colorationOnCheckBox.isSelected();
    colorationOnCheckBox.setSelected(newState);
    owner.setInputColorationOn(newState);
  }

/*------------------------------------------------------------------------
Method:  void removeListeners()
Purpose: Unregister GUI listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removeListeners()
  {
    colorationOnCheckBox.removeItemListener(owner);
    colorationTypeImperfectio.removeActionListener(owner);
    colorationTypeSesquialtera.removeActionListener(owner);
    colorationTypeMinorColor.removeActionListener(owner);

    for (WindowListener wl : getListeners(WindowListener.class))
      removeWindowListener(wl);
  }
}
