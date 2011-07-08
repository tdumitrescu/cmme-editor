/*----------------------------------------------------------------------*/
/*

        Module          : RenderedEventGroup

        Package         : Gfx

        Classes Included: RenderedEventGroup

        Purpose         : Hold indices for one group of rendered events
                          (e.g., ligature)

        Programmer      : Ted Dumitrescu

        Date Started    : 7/05 (moved to separate file 3/27/06)

Updates:
11/22/07: added min/max pitch info (moved from RenderedLigature)
12/2/07:  added parameter varReading

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   RenderedEventGroup
Extends: -
Purpose: Information about one event group (e.g., ligature)
------------------------------------------------------------------------*/

public class RenderedEventGroup
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int EVENTGROUP_NONE=          0,
                          EVENTGROUP_LIG=           1,
                          EVENTGROUP_VARIANTREADING=2;

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int            grouptype,
                        firstEventNum,lastEventNum, /* -1 for no events */
                        yMaxEventNum,yMinEventNum;
  public Event          yMaxEvent,yMinEvent;

  public VariantReading     varReading;
  public VariantMarkerEvent varMarker;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderedEventGroup([int fei,int lei]|int ei)
Purpose:     Initialize group structure
Parameters:
  Input:  int fei,lei - indices of first and last events in group (for
                        multi-event groups)
          int ei      - index of event in single-event group
  Output: -
------------------------------------------------------------------------*/

  public RenderedEventGroup(int ei)
  {
    grouptype=EVENTGROUP_NONE;
    firstEventNum=lastEventNum=yMaxEventNum=yMinEventNum=ei;
    yMaxEvent=yMinEvent=null;
    varReading=null;
    varMarker=null;
  }

  public RenderedEventGroup(int fei,int lei)
  {
    grouptype=EVENTGROUP_LIG;
    firstEventNum=fei;
    lastEventNum=lei;
    yMaxEventNum=yMinEventNum=fei;
    yMaxEvent=yMinEvent=null;
    varReading=null;
    varMarker=null;
  }

/*------------------------------------------------------------------------
Method:  void calculateYMinMax(RenderList rl)
Purpose: Calculate minimum and maximum y-vals for events within group
Parameters:
  Input:  RenderList rl - rendered event list
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void calculateYMinMax(RenderList rl)
  {
    Event e;
    for (int ei=firstEventNum; ei<=lastEventNum; ei++)
      {
        e=rl.getEvent(ei).getEvent();
        if (e.geteventtype()==Event.EVENT_NOTE)
          {
            Pitch p=e.getPitch();
            if (yMaxEvent==null || p.isHigherThan(yMaxEvent.getPitch()))
              {
                yMaxEventNum=ei;
                yMaxEvent=e;
              }
            if (yMinEvent==null || yMinEvent.getPitch().isHigherThan(p))
              {
                yMinEventNum=ei;
                yMinEvent=e;
              }
        }
      }
  }
}