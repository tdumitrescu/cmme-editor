/*----------------------------------------------------------------------*/
/*

        Module          : ClipboardData.java

        Package         : Editor

        Classes Included: ClipboardData

        Purpose         : Handles data for cut/copy/paste actions

        Programmer      : Ted Dumitrescu

        Date Started    : 6/16/09

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import DataStruct.*;
import Gfx.*;

/*------------------------------------------------------------------------
Class:   ClipboardData
Extends: -
Purpose: Data for cut/copy/paste actions
------------------------------------------------------------------------*/

public class ClipboardData
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  EventListData events;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ClipboardData(int snum,int vnum,int evnum1,int evnum2)
Purpose:     Init and copy events into list
Parameters:
  Input:  
  Output: -
------------------------------------------------------------------------*/

  public ClipboardData(ScoreRenderer[] renderedSections,
                       int snum,int vnum,int evnum1,int evnum2)
  {
    events=new EventListData();

    /* copy events */
    for (int ei=evnum1; ei<=evnum2; ei++)
      {
        Event e=renderedSections[snum].eventinfo[vnum].getEvent(ei).getEvent();
        switch (e.geteventtype())
          {
            case Event.EVENT_VARIANTDATA_START:
            case Event.EVENT_VARIANTDATA_END:
              break;
            default:
              Event newe=e.createCopy();
              events.addEvent(newe);
          }
      }
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public EventListData getEventList()
  {
    return events;
  }
}
