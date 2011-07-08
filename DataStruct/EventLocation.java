/*----------------------------------------------------------------------*/
/*

        Module          : EventLocation.java

        Package         : DataStruct

        Classes Included: EventLocation

        Purpose         : Store event location

        Programmer      : Ted Dumitrescu

        Date Started    : 1/23/08

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   EventLocation
Extends: -
Purpose: Low-level storage for event location (section number, voice
         number, index in event list)
------------------------------------------------------------------------*/

public class EventLocation
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public int sectionNum,voiceNum,eventNum;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: EventLocation(int sectionNum,int voiceNum,int eventNum)
Purpose:     Initialization
Parameters:
  Input:  int sectionNum,voiceNum,eventNum - location attributes
  Output: -
------------------------------------------------------------------------*/

  public EventLocation(int sectionNum,int voiceNum,int eventNum)
  {
    this.sectionNum=sectionNum;
    this.voiceNum=voiceNum;
    this.eventNum=eventNum;
  }

/*------------------------------------------------------------------------
Method:  String toString()
Purpose: Convert to string
Parameters:
  Input:  -
  Output: -
  Return: string representation of this
------------------------------------------------------------------------*/

  public String toString()
  {
    return "S: "+sectionNum+" V: "+voiceNum+" E: "+eventNum;
  }
}
