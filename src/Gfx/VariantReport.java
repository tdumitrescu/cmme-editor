/*----------------------------------------------------------------------*/
/*

        Module          : VariantReport.java

        Package         : Gfx

        Classes Included: VariantReport

        Purpose         : Display/analysis of one variant (set of readings
                          at one location)

        Programmer      : Ted Dumitrescu

        Date Started    : 11/2/2009 (moved from CriticalNotesWindow)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   VariantReport
Extends: -
Purpose: Information for display/analysis of one variant (set of readings at one
         location)
------------------------------------------------------------------------*/

public class VariantReport
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public int                voiceNum;
  public int                measureNum;
  public boolean            betweenMeasures;
  public int                revNum;
  public VariantMarkerEvent varMarker;
  public long               varFlags;

  public String             measureLabel;
  public JButton            measureButton;

  public RenderList         renderedVoice;
  public VoiceEventListData voiceEvents;

  public List<VariantVersionData> defaultVersions;

/*------------------------------------------------------------------------
Constructor: VariantReport(MeasureInfo m,RenderList rv,int vmi)
Purpose:     Initialize structure with display info based on variant/event list
Parameters:
  Input:  
  Output: -
------------------------------------------------------------------------*/

  public VariantReport(MeasureInfo m,int vnum,RenderList rv,int vmi)
  {
    voiceNum=vnum+1;
    revNum=vmi;
    renderedVoice=rv;
    voiceEvents=rv.getVoiceEventData();
    varMarker=(VariantMarkerEvent)(rv.getEvent(revNum).getEvent());
    varFlags=varMarker.calcVariantTypes(voiceEvents); //.getVarTypeFlags();
    measureNum=m.getMeasureNum()+1;

    measureLabel=CriticalNotesWindow.createMeasureString(rv,m,vmi);
    measureButton=null;

    betweenMeasures=measureLabel.indexOf("/")>=0;
    RenderedEvent nextre=rv.getEvent(revNum+1);
    if (nextre.getmeasurenum()+1>measureNum)
      measureNum++;
  }

  void initMeasureButton(ActionListener listener)
  {
    measureButton=new JButton(measureLabel);
    measureButton.addActionListener(listener);
  }

  void unregisterListeners(ActionListener listener)
  {
    if (measureButton!=null)
      measureButton.removeActionListener(listener);
  }
}
