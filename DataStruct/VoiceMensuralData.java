/*----------------------------------------------------------------------*/
/*

        Module          : VoiceMensuralData.java

        Package         : DataStruct

        Classes Included: VoiceMensuralData

        Purpose         : Contains mensural musical data for one voice in one
                          section

        Programmer      : Ted Dumitrescu

        Date Started    : 99 (class Voice); moved to class VoiceMensuralData
                          2/19/07

        Updates         :
7/24/07: moved basic event list functions to abstract class VoiceEventListData

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   VoiceMensuralData
Extends: VoiceEventListData
Purpose: Voice mensural music data
------------------------------------------------------------------------*/

public class VoiceMensuralData extends VoiceEventListData
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  int ellipsisEventNum;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VoiceMensuralData(Voice v,MusicSection section)
Purpose:     Creates mensural music data structure matched to one voice
Parameters:
  Input:  Voice v              - voice metadata
          MusicSection section - section data
  Output: -
------------------------------------------------------------------------*/

  public VoiceMensuralData(Voice v,MusicSection section)
  {
    initParams(v,section);
    ellipsisEventNum=-1;
  }

  public VoiceMensuralData()
  {
    initParams();
    ellipsisEventNum=-1; /* vestigial limb ! */
  }

/*------------------------------------------------------------------------
Method:  void setVoiceParams(Event e)
Purpose: Update voice parameter variables after adding a new event
Parameters:
  Input:  Event e - event just added
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setVoiceParams(Event e)
  {
    if (e.geteventtype()==Event.EVENT_ELLIPSIS)
      {
        if (ellipsisEventNum!=-1)
          System.err.println("Error: Multiple ellipses in one voice");
        if (!metaData.getGeneralData().isIncipitScore())
          System.err.println("Error: Adding ellipsis to non-incipit score");
        ellipsisEventNum=e.getListPlace(isDefaultVersion());
      }
  }

/*------------------------------------------------------------------------
Method:  Event deleteEvent(int i)
Purpose: Remove event from this voice's list
Parameters:
  Input:  int i - index of event to delete
  Output: -
  Return: deleted Event
------------------------------------------------------------------------*/

  public Event deleteEvent(int i)
  {
    Event deletedEvent=super.deleteEvent(i);

    if (deletedEvent.geteventtype()==Event.EVENT_ELLIPSIS)
      ellipsisEventNum=-1;

    return deletedEvent;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int getEllipsisEventNum()
  {
    return ellipsisEventNum;
  }
}
