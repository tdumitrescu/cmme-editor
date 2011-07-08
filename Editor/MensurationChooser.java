/*----------------------------------------------------------------------*/
/*

        Module          : MensurationChooser.java

        Package         : Editor

        Classes Included: MensurationChooser

        Purpose         : GUI panel for modifying mensuration structures

        Programmer      : Ted Dumitrescu

        Date Started    : 9/21/07 (moved from EditorWin.java)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.util.*;
import javax.swing.*;

import Gfx.*;
import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   MensurationChooser
Extends: JPanel
Purpose: GUI for modifying mensuration structures
------------------------------------------------------------------------*/

class MensurationChooser extends JPanel
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public JRadioButton prolatioBinaryButton,prolatioTernaryButton,
                      tempusBinaryButton,tempusTernaryButton,
                      modus_minorBinaryButton,modus_minorTernaryButton,
                      modus_maiorBinaryButton,modus_maiorTernaryButton;
  public JList        signsList;
  public JButton      mensOButton,
                      mensCButton,
                      mens2Button,
                      mens3Button;
  public JCheckBox    mensDotBox,
                      mensStrokeBox,
                      mensReverseBox,
                      mensNoScoreSigBox;
  public JSpinner     num1Spinner,
                      num2Spinner;
  public JButton      deleteButton;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MensurationChooser([MensEvent me])
Purpose:     Initialize and lay out mensuration chooser
Parameters:
  Input:  MensEvent me - event with initial mensuration values
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MensurationChooser()
  {
    super();

    setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));

    /* sign designer */
    JPanel signPanel=new JPanel();
    signPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Sign"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    signPanel.setLayout(new BoxLayout(signPanel,BoxLayout.Y_AXIS));

    Box signControlsBox=Box.createHorizontalBox(),
        signAddDeleteBox=Box.createVerticalBox(),
        signAddBox=Box.createHorizontalBox();
    JLabel addLabel=new JLabel("Add: ");
    signAddBox.add(addLabel);
    mensOButton=new JButton("O");
    mensCButton=new JButton("C");
    mens2Button=new JButton("2");
    mens3Button=new JButton("3");
    signAddBox.add(mensOButton);
    signAddBox.add(mensCButton);
    signAddBox.add(mens2Button);
    signAddBox.add(mens3Button);
    signAddDeleteBox.add(signAddBox);
    signAddDeleteBox.add(Box.createVerticalStrut(5));
    deleteButton=new JButton("Delete");
    signAddDeleteBox.add(deleteButton);
    signControlsBox.add(signAddDeleteBox);
    signControlsBox.add(Box.createHorizontalStrut(10));
    Box signParamsBox=Box.createVerticalBox();
    mensDotBox=new JCheckBox("Dot");
    mensStrokeBox=new JCheckBox("Stroke");
    mensReverseBox=new JCheckBox("Reversed");
    mensNoScoreSigBox=new JCheckBox("No score effect");
    signParamsBox.add(mensDotBox);
    signParamsBox.add(mensStrokeBox);
    signParamsBox.add(mensReverseBox);
    signParamsBox.add(mensNoScoreSigBox);
    Box numSpinnerBox=Box.createHorizontalBox();
    numSpinnerBox.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
    num1Spinner=new JSpinner(new SpinnerNumberModel(2,0,999,1));
    numSpinnerBox.add(Box.createHorizontalGlue());
    numSpinnerBox.add(new JLabel("Num: "));
    numSpinnerBox.add(num1Spinner);
    signParamsBox.add(numSpinnerBox);
    signControlsBox.add(signParamsBox);
    signControlsBox.add(Box.createHorizontalGlue());

    String[] signNames={ "C" }; //{ "Sign","Num","etc","etc" };
    signsList=new JList(signNames);
    signsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    signsList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
    signsList.setVisibleRowCount(1);
    JScrollPane signScroller=new JScrollPane(signsList);

    signPanel.add(signScroller);
    signPanel.add(Box.createVerticalStrut(5));
    signPanel.add(signControlsBox);

    /* mensuration information chooser */
    JPanel mensurationsPanel=new JPanel();
    mensurationsPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Mensuration information"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    mensurationsPanel.setLayout(new GridBagLayout());
    GridBagConstraints mpc=new GridBagConstraints();

    /* create mensuration radio buttons, groups, and labels */
    prolatioBinaryButton=new JRadioButton("Minor (Binary)",true);
    prolatioTernaryButton=new JRadioButton("Major (Ternary)");
    tempusBinaryButton=new JRadioButton("Imperfect (Binary)",true);
    tempusTernaryButton=new JRadioButton("Perfect (Ternary)");
    modus_minorBinaryButton=new JRadioButton("Imperfect (Binary)",true);
    modus_minorTernaryButton=new JRadioButton("Perfect (Ternary)");
    modus_maiorBinaryButton=new JRadioButton("Imperfect (Binary)",true);
    modus_maiorTernaryButton=new JRadioButton("Perfect (Ternary)");
    JLabel prolatioLabel=new JLabel("Prolatio: "),
           tempusLabel=new JLabel("Tempus: "),
           modus_minorLabel=new JLabel("Modus minor: "),
           modus_maiorLabel=new JLabel("Modus maior: ");
    ButtonGroup prolatioButtons=new ButtonGroup(),
                tempusButtons=new ButtonGroup(),
                modus_minorButtons=new ButtonGroup(),
                modus_maiorButtons=new ButtonGroup();
    prolatioButtons.add(prolatioBinaryButton);
    prolatioButtons.add(prolatioTernaryButton);
    tempusButtons.add(tempusBinaryButton);
    tempusButtons.add(tempusTernaryButton);
    modus_minorButtons.add(modus_minorBinaryButton);
    modus_minorButtons.add(modus_minorTernaryButton);
    modus_maiorButtons.add(modus_maiorBinaryButton);
    modus_maiorButtons.add(modus_maiorTernaryButton);

    /* create grid */
    mpc.anchor=GridBagConstraints.LINE_START;
    mpc.gridx=0; mpc.gridy=0; mensurationsPanel.add(prolatioLabel,mpc);
    mpc.gridx=1; mpc.gridy=0; mensurationsPanel.add(prolatioBinaryButton,mpc);
    mpc.gridx=2; mpc.gridy=0; mensurationsPanel.add(prolatioTernaryButton,mpc);
    mpc.gridx=0; mpc.gridy=1; mensurationsPanel.add(tempusLabel,mpc);
    mpc.gridx=1; mpc.gridy=1; mensurationsPanel.add(tempusBinaryButton,mpc);
    mpc.gridx=2; mpc.gridy=1; mensurationsPanel.add(tempusTernaryButton,mpc);
    mpc.gridx=0; mpc.gridy=2; mensurationsPanel.add(modus_minorLabel,mpc);
    mpc.gridx=1; mpc.gridy=2; mensurationsPanel.add(modus_minorBinaryButton,mpc);
    mpc.gridx=2; mpc.gridy=2; mensurationsPanel.add(modus_minorTernaryButton,mpc);
    mpc.gridx=0; mpc.gridy=3; mensurationsPanel.add(modus_maiorLabel,mpc);
    mpc.gridx=1; mpc.gridy=3; mensurationsPanel.add(modus_maiorBinaryButton,mpc);
    mpc.gridx=2; mpc.gridy=3; mensurationsPanel.add(modus_maiorTernaryButton,mpc);

    add(signPanel);
    add(mensurationsPanel);
  }

  public MensurationChooser(MensEvent me)
  {
    this();
    setIndices(me);
  }

/*------------------------------------------------------------------------
Method:  void setIndices(MensEvent me)
Purpose: Set values for GUI
Parameters:
  Input:  MensEvent me - data for GUI
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setIndices(MensEvent me)
  {
    Vector<Object> signEls=new Vector<Object>();
    for (MensSignElement mse : me.getSigns())
      signEls.add(mse.toString());
    signsList.setListData(signEls);

    signsList.setSelectedIndex(0);
    setSignElGUI(me);

    Mensuration m=me.getMensInfo();
    if (m.prolatio==Mensuration.MENS_BINARY)
      prolatioBinaryButton.setSelected(true);
    else
      prolatioTernaryButton.setSelected(true);
    if (m.tempus==Mensuration.MENS_BINARY)
      tempusBinaryButton.setSelected(true);
    else
      tempusTernaryButton.setSelected(true);
    if (m.modus_minor==Mensuration.MENS_BINARY)
      modus_minorBinaryButton.setSelected(true);
    else
      modus_minorTernaryButton.setSelected(true);
    if (m.modus_maior==Mensuration.MENS_BINARY)
      modus_maiorBinaryButton.setSelected(true);
    else
      modus_maiorTernaryButton.setSelected(true);
  }

/*------------------------------------------------------------------------
Method:  void setSignElGUI(MensEvent me)
Purpose: Set GUI values for currently selected mensuration sign element
Parameters:
  Input:  MensEvent me - data for GUI
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setSignElGUI(MensEvent me)
  {
    MensSignElement mse=me.getSigns().get(signsList.getSelectedIndex());
    mensDotBox.setSelected(mse.dotted);
    mensStrokeBox.setSelected(mse.stroke);
    mensReverseBox.setSelected(mse.signType==MensSignElement.MENS_SIGN_CREV);
    mensNoScoreSigBox.setSelected(me.noScoreSig());

    if (mse.signType==MensSignElement.MENS_SIGN_C ||
        mse.signType==MensSignElement.MENS_SIGN_O ||
        mse.signType==MensSignElement.MENS_SIGN_CREV)
      {
        mensDotBox.setEnabled(true);
        mensStrokeBox.setEnabled(true);
      }
    else
      {
        mensDotBox.setEnabled(false);
        mensStrokeBox.setEnabled(false);
      }
    mensReverseBox.setEnabled(mse.signType==MensSignElement.MENS_SIGN_C ||
                              mse.signType==MensSignElement.MENS_SIGN_CREV);
    num1Spinner.setEnabled(mse.signType==MensSignElement.NUMBERS);
  }

/*------------------------------------------------------------------------
Method:  Mensuration createMensuration()
Purpose: Generate Mensuration structure with values from radio buttons
Parameters:
  Input:  -
  Output: -
  Return: new mensuration structure
------------------------------------------------------------------------*/

  public Mensuration createMensuration()
  {
    return new Mensuration(
      prolatioTernaryButton.isSelected() ? Mensuration.MENS_TERNARY : Mensuration.MENS_BINARY,
      tempusTernaryButton.isSelected() ? Mensuration.MENS_TERNARY : Mensuration.MENS_BINARY,
      modus_minorTernaryButton.isSelected() ? Mensuration.MENS_TERNARY : Mensuration.MENS_BINARY,
      modus_maiorTernaryButton.isSelected() ? Mensuration.MENS_TERNARY : Mensuration.MENS_BINARY);
  }

/*------------------------------------------------------------------------
Method:  int getSelectedElementNum()
Purpose: Returns index of element currently selected in GUI
Parameters:
  Input:  -
  Output: -
  Return: index of currently selected element
------------------------------------------------------------------------*/

  public int getSelectedElementNum()
  {
    return signsList.getSelectedIndex();
  }

/*------------------------------------------------------------------------
Method:  boolean isMensurationButton(Object o)
Purpose: Checks whether an object is one of the buttons in the mensuration
         chooser (for checking action sources)
Parameters:
  Input:  Object o - object to compare
  Output: -
  Return: whether o is one of the buttons in this panel
------------------------------------------------------------------------*/

  public boolean isMensurationButton(Object o)
  {
    return o==prolatioBinaryButton    || o==prolatioTernaryButton    ||
           o==tempusBinaryButton      || o==tempusTernaryButton      ||
           o==modus_minorBinaryButton || o==modus_minorTernaryButton ||
           o==modus_maiorBinaryButton || o==modus_maiorTernaryButton;
  }
}
