/*----------------------------------------------------------------------*/
/*

        Module          : MessageWin.java

        Package         : Gfx

        Classes Included: MessageWin

        Purpose         : Handles simple text windows (e.g., "Loading")

        Programmer      : Ted Dumitrescu

        Date Started    : 3/16/05

Updates:
2/14/06: now centers relative to parent window rather than to entire screen
6/21/07: added progress bar

									*/
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.BorderLayout;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   MessageWin
Extends: JFrame
Purpose: Handles a simple text window
------------------------------------------------------------------------*/

public class MessageWin extends JFrame
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  JProgressBar progressBar=null;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MessageWin(String msg,Component parentWin,boolean pbar)
Purpose:     Create and display window
Parameters:
  Input:  String msg          - message to display in window
          Component parentWin - parent window (for positioning)
          boolean pbar        - whether to include progress bar
  Output:
  Return:
------------------------------------------------------------------------*/

  public MessageWin(String msg,Component parentWin,boolean pbar)
  {
    Container cp=getContentPane();

    setTitle("CMME");
    cp.setLayout(new BorderLayout());
    JLabel MsgLabel=new JLabel(msg,SwingConstants.CENTER);
    MsgLabel.setFont(MsgLabel.getFont().deriveFont((float)14.0));
    MsgLabel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
    cp.add(MsgLabel,"Center");

    if (pbar)
      {
        progressBar=new JProgressBar(0,100);
        progressBar.setValue(0);
        cp.add(progressBar,"South");
      }

    /* pack and show window */
    pack();
    setLocationRelativeTo(parentWin);
    setVisible(true);
    toFront();
  }

  public MessageWin(String msg,Component parentWin)
  {
    this(msg,parentWin,false);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public JProgressBar getProgressBar()
  {
    return progressBar;
  }
}
