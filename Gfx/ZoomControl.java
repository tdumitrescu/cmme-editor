/*----------------------------------------------------------------------*/
/*

        Module          : ZoomControl

        Package         : Gfx

        Classes Included: ZoomControl

        Purpose         : Generic view-zoom control

        Programmer      : Ted Dumitrescu

        Date Started    : 5/30/2008 (moved from MusicWin)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import javax.swing.*;

/*------------------------------------------------------------------------
Class:   ZoomControl
Extends: JPanel
Purpose: Generic zoom-in/out/% control for viewable areas
------------------------------------------------------------------------*/

public class ZoomControl extends JPanel
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int   DEFAULT_MINSIZE=10,
                            DEFAULT_MAXSIZE=400;
  public static final int[] DEFAULT_SIZES={ 200,175,150,125,100,75,50 };

/*----------------------------------------------------------------------*/
/* Instance variables */

  public JButton    zoomOutButton,zoomInButton;
  public JTextField viewSizeField=null;

  ActionListener listener;

  int   curViewSize,
        minSize,maxSize;
  int[] sizes;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ZoomControl(int startSize,ActionListener listener)
Purpose:     Lay out and initialize
Parameters:
  Input:  int startSize           - initial view size
          int minSize,maxSize     - min/max view size
          int[] sizes             - size list for +/- buttons
  Output: ActionListener listener - component to listen for events on this
------------------------------------------------------------------------*/

  public ZoomControl(int startSize,ActionListener listener)
  {
    this(startSize,listener,DEFAULT_MINSIZE,DEFAULT_MAXSIZE,DEFAULT_SIZES);
  }

  public ZoomControl(int startSize,ActionListener listener,int minSize,int maxSize,int[] sizes)
  {
    super();
    this.listener=listener;
    this.curViewSize=startSize;
    this.minSize=minSize;
    this.maxSize=maxSize;
    this.sizes=insertIntoSizes(sizes,curViewSize);

    setLayout(new GridBagLayout());
    GridBagConstraints zcc=new GridBagConstraints();
    zcc.anchor=GridBagConstraints.WEST;
    zcc.weightx=0;

    viewSizeField=new JTextField(curViewSize+"%",3);
    try
      {
        zoomOutButton=new JButton(new ImageIcon(new URL(MusicWin.BaseDataURL+"imgs/zoomout-button.gif")));
        zoomInButton=new JButton(new ImageIcon(new URL(MusicWin.BaseDataURL+"imgs/zoomin-button.gif")));
      }
    catch (MalformedURLException e)
      {
        System.err.println("Error loading Zoom-Control icons: "+e);
      }
    zoomOutButton.setFocusable(false);
    zoomInButton.setFocusable(false);
    zoomOutButton.setMargin(new Insets(1,1,1,1));
    zoomInButton.setMargin(new Insets(1,1,1,1));
    zoomOutButton.setToolTipText("Zoom out");
    zoomInButton.setToolTipText("Zoom in");

    addListeners();

    zcc.weightx=0; zcc.gridx=0; zcc.gridy=0; add(zoomOutButton,zcc);
    zcc.weightx=0; zcc.gridx++;              add(viewSizeField,zcc);
    zcc.weightx=1; zcc.gridx++;              add(zoomInButton,zcc);
  }

/*------------------------------------------------------------------------
Method:  void addListeners()
Purpose: Register GUI listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addListeners()
  {
    viewSizeField.addActionListener(listener);
    zoomInButton.addActionListener(listener);
    zoomOutButton.addActionListener(listener);
  }

/*------------------------------------------------------------------------
Method:  void removeListeners()
Purpose: Unregister GUI listeners (should match addListeners)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removeListeners()
  {
    viewSizeField.removeActionListener(listener);
    zoomInButton.removeActionListener(listener);
    zoomOutButton.removeActionListener(listener);
  }

/*------------------------------------------------------------------------
Method:  void setViewSize|viewSizeFieldAction|zoomIn|zoomOut()
Purpose: Functions to control GUI
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public int viewSizeFieldAction()
  {
    int newVS;
    try
      {
        String intText=viewSizeField.getText().replaceAll("\\D","");
        newVS=(new Integer(intText)).intValue();
        if (newVS<minSize)
          newVS=minSize;
        else if (newVS>maxSize)
          newVS=maxSize;
      }
    catch (Exception e)
      {
        newVS=curViewSize;
      }

    curViewSize=newVS;

    return curViewSize;
  }

  public int zoomOut()
  {
    if (curViewSize>sizes[sizes.length-1])
      {
        int si=getLesserSize(curViewSize);
        setViewSize(sizes[si]);
      }

    return curViewSize;
  }

  public int zoomIn()
  {
    if (curViewSize<sizes[0])
      {
        int si=getGreaterSize(curViewSize);
        setViewSize(sizes[si]);
      }

    return curViewSize;
  }

  protected void setViewSize(int ns)
  {
    curViewSize=ns;
    if (viewSizeField!=null)
      viewSizeField.setText(curViewSize+"%");
  }

  /* get next largest or smallest number in array of available sizes */
  int getGreaterSize(int vs)
  {
    for (int i=sizes.length-1; i>=0; i--)
      if (sizes[i]>vs)
        return i;
    return -1;
  }

  int getLesserSize(int vs)
  {
    for (int i=0; i<sizes.length; i++)
      if (sizes[i]<vs)
        return i;
    return -1;
  }

  int getSizeIndex(int vs)
  {
    for (int i=0; i<sizes.length; i++)
      if (sizes[i]==vs)
        return i;
    return -1;
  }

  int[] insertIntoSizes(int[] oldSizes,int newNum)
  {
    int curIndex=-1;
    for (int i=0; i<oldSizes.length; i++)
      if (oldSizes[i]==newNum)
        {
          curIndex=i;
          break;
        }

    if (curIndex!=-1)
      return oldSizes;

    int[] newSizes=new int[oldSizes.length+1];
    int   vi=0;
    for (int i=0; i<oldSizes.length; i++)
      {
        if (vi==i && oldSizes[i]<newNum)
          {
            newSizes[vi]=newNum;
            vi++;
          }
        newSizes[vi++]=oldSizes[i];
      }
    if (vi<=oldSizes.length) /* was newNum not inserted? */
      newSizes[vi]=newNum;

    return newSizes;
  }
}
