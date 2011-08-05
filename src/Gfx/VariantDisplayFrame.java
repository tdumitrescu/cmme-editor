/*----------------------------------------------------------------------*/
/*

        Module          : VariantDisplayFrame.java

        Package         : Gfx

        Classes Included: VariantDisplayFrame

        Purpose         : Frame displaying all variant readings (and default
                          reading) at one location

        Programmer      : Ted Dumitrescu

        Date Started    : 1/21/2008 (moved out of functions in ViewCanvas)

        Updates         :
7/19/08: moved individual reading display to public class VariantReadingPanel

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import DataStruct.*;

/*------------------------------------------------------------------------
Class:   VariantDisplayFrame
Extends: JDialog
Purpose: Displays all variant readings at one location
------------------------------------------------------------------------*/

public class VariantDisplayFrame extends JDialog
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  protected VariantMarkerEvent vStartEvent;
  protected ViewCanvas         canvas;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantDisplayFrame(RenderedEventGroup renderedVar,
                                 VoiceEventListData v,ScoreRenderer rs,int vnum,
                                 int fx,int fy,ViewCanvas canvas,MusicFont MusicGfx,
                                 float STAFFSCALE,float VIEWSCALE)
Parameters:
  Input:  
  Output: -
------------------------------------------------------------------------*/

  public VariantDisplayFrame(RenderedEventGroup renderedVar,
                             VoiceEventListData v,ScoreRenderer rs,int vnum,
                             int fx,int fy,ViewCanvas canvas,MusicFont MusicGfx,
                             float STAFFSCALE,float VIEWSCALE)
  {
    super(canvas.parentwin,false);
    this.canvas=canvas;
    int mnum=rs.eventinfo[vnum].getEvent(renderedVar.firstEventNum).getmeasurenum();
    RenderList rv=rs.eventinfo[vnum];
    this.setTitle("V"+(vnum+1)+", m. "+CriticalNotesWindow.createMeasureString(
      rv,rs.getMeasure(rv.getEvent(renderedVar.firstEventNum).getmeasurenum()),renderedVar.firstEventNum));
    this.setBackground(Color.white);

    vStartEvent=(VariantMarkerEvent)rv.getEvent(renderedVar.firstEventNum).getEvent();
    vStartEvent.calcVariantTypes(v);
    VariantMarkerEvent vEndEvent=(VariantMarkerEvent)rv.getEvent(renderedVar.lastEventNum).getEvent();
    vEndEvent.setVarTypeFlags(vStartEvent.getVarTypeFlags());

    int numReadings=vStartEvent.getNumReadings();
    LinkedList<StaffEventData> variantStaves=new LinkedList<StaffEventData>();

    Container vrcp=this.getContentPane();
    vrcp.setLayout(new GridBagLayout());
    GridBagConstraints vrc=new GridBagConstraints();
    vrc.gridy=0;

    JPanel infoPanel=createInfoPanel();
    vrc.gridx=0; vrc.gridwidth=GridBagConstraints.REMAINDER; vrcp.add(infoPanel,vrc);
    vrc.gridwidth=1;
    vrc.gridy++;

    /* get list of "unused" versions to be listed with default reading */
    PieceData musicData=canvas.getMusicData();
    List<VariantVersionData> defaultVersions=vStartEvent.getDefaultVersions(
      musicData.getVariantVersions(),musicData.getVoice(vnum),v);

    JPanel defaultVersionsPanel=createVersionsPanel(defaultVersions,true),
           defaultVariantPanel=new VariantReadingPanel(
             v,
             vStartEvent.getDefaultListPlace(),
             rs.eventinfo[vnum].getClefEvents(renderedVar.firstEventNum),false,
             MusicGfx,STAFFSCALE,VIEWSCALE);
    vrc.gridx=0; vrcp.add(defaultVersionsPanel,vrc);
    vrc.gridx=1; vrcp.add(defaultVariantPanel,vrc);
    vrc.gridy++;

    for (VariantReading vr : vStartEvent.getReadings())
      {
        JPanel versionsPanel=createVersionsPanel(vr.getVersions(),false),
               variantPanel=new VariantReadingPanel(
                 vr,rs.eventinfo[vnum].getClefEvents(renderedVar.firstEventNum),
                 vr.isError(),
                 MusicGfx,STAFFSCALE,VIEWSCALE);

        vrc.gridx=0; vrcp.add(versionsPanel,vrc);
        vrc.gridx=1; vrcp.add(variantPanel,vrc);
        vrc.gridy++;
      }

    createAdditionalControls(vrcp,vrc);
    registerListeners();

    this.pack();

    /* ensure that the frame is on-screen */
    Dimension screenSize=java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    if (fx+this.getSize().width>screenSize.width)
      fx=screenSize.width-this.getSize().width;
    if (fy+this.getSize().height>screenSize.height)
      fy=screenSize.height-this.getSize().height;
    this.setLocation(fx,fy);
  }

  protected JPanel createInfoPanel()
  {
    JLabel infoLabel=new JLabel(
      "Variant type: "+
      VariantReading.varTypesToStr(vStartEvent.getVarTypeFlags()));
    JPanel infoPanel=new JPanel();
    infoPanel.add(infoLabel);
    return infoPanel;
  }

  protected JPanel createVersionsPanel(List<VariantVersionData> versions,boolean defaultVersion)
  {
    JPanel versionsPanel=new JPanel();
    versionsPanel.setLayout(new BoxLayout(versionsPanel,BoxLayout.Y_AXIS));

//    JTable versionsTable=new JTable(versions.size(),1);
    for (int i=0; i<versions.size(); i++)
//      versionsTable.setValueAt(" "+versions.get(i).getID(),i,0);
      versionsPanel.add(new JLabel(versions.get(i).getID()));
    versionsPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    return versionsPanel;
  }

  /* deprecated
  protected JPanel createVersionsPanel(VariantVersionData version)
  {
    JPanel versionsPanel=new JPanel();
    JTable versionsTable=new JTable(1,1);
    versionsTable.setValueAt(version.getID(),0,0);
    versionsPanel.add(versionsTable);
    return versionsPanel;
  }*/

  protected void createAdditionalControls(Container vrcp,GridBagConstraints vrc)
  {
  }

/*------------------------------------------------------------------------
Method:  void closeFrame()
Purpose: Close frame and clean up
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void closeFrame()
  {
    setVisible(false);
    unregisterListeners();
    dispose();
  }

/*------------------------------------------------------------------------
Method:  void [un]registerListeners()
Purpose: Add and remove event listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void registerListeners()
  {
    addWindowListener(
      new WindowAdapter()
        {
          public void windowClosing(WindowEvent event)
          {
            closeFrame();
          }
        });
  }

  protected void unregisterListeners()
  {
    for (WindowListener w : getListeners(WindowListener.class))
      removeWindowListener(w);
  }
}
