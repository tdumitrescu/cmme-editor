/*----------------------------------------------------------------------*/
/*

        Module          : CustosEvent.java

        Package         : DataStruct

        Classes Included: CustosEvent

        Purpose         : Custos event type

        Programmer      : Ted Dumitrescu

        Date Started    : 4/25/99

        Updates         : 

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   CustosEvent
Extends: Event
Purpose: Data/routines for custos events
------------------------------------------------------------------------*/

public class CustosEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Class variables */

/*----------------------------------------------------------------------*/
/* Instance variables */

  Pitch pitch;    /* pitch of sign */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: CustosEvent(Pitch p)
Purpose:     Creates custos event
Parameters:
  Input:  Pitch p - custos pitch information
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public CustosEvent(Pitch p)
  {
    eventtype=EVENT_CUSTOS;
    pitch=p;
  }

/*------------------------------------------------------------------------
Method:    Event createCopy()
Overrides: Event.createCopy
Purpose:   create copy of current event
Parameters:
  Input:  -
  Output: -
  Return: copy of this
------------------------------------------------------------------------*/

  public Event createCopy()
  {
    Event e=new CustosEvent(new Pitch(this.pitch));
    e.copyEventAttributes(this);
    return e;
  }

/*------------------------------------------------------------------------
Methods: boolean equals(Event other)
Purpose: Check whether the data of this event is exactly equal to another
Parameters:
  Input:  Event other - event to check against
  Output: -
  Return: true if events are equal
------------------------------------------------------------------------*/

  public boolean equals(Event other)
  {
    if (!super.equals(other))
      return false;
    CustosEvent otherCE=(CustosEvent)other;

    return this.pitch.equals(otherCE.pitch);
  }

/*------------------------------------------------------------------------
Method:  Pitch getPitch()
Purpose: Returns pitch of custos
Parameters:
  Input:  -
  Output: -
  Return: pitch structure
------------------------------------------------------------------------*/

  public Pitch getPitch()
  {
    return pitch;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this event
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("    Custos: "+pitch.toString());
  }
}
