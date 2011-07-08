/*----------------------------------------------------------------------*/
/*

        Module          : LacunaEvent.java

        Package         : DataStruct

        Classes Included: LacunaEvent

        Purpose         : Event type for lacunae

        Programmer      : Ted Dumitrescu

        Date Started    : 8/1/06

        Updates         :
8/18/08: added proportionless begin/end markers for LacunaEvents within
         variant versions

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   LacunaEvent
Extends: Event
Purpose: Data/routines for lacuna events
------------------------------------------------------------------------*/

public class LacunaEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Proportion length;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: LacunaEvent(Proportion l)
Purpose:     Creates lacuna event
Parameters:
  Input:  Proportion l - length of lacuna (in music time)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public LacunaEvent(Proportion l)
  {
    eventtype=EVENT_LACUNA;
    length=musictime=new Proportion(l);
  }

  public LacunaEvent(int eventType)
  {
    this.eventtype=eventType;
    length=musictime=new Proportion(0,1);
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
    Event e=this.length.i1>0 ? new LacunaEvent(this.length) :
                               new LacunaEvent(this.geteventtype());
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
    LacunaEvent otherLE=(LacunaEvent)other;

    return this.length.equals(otherLE.length);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Proportion getLength()
  {
    return length;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setLength(Proportion l)
  {
    length=musictime=l;
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
    System.out.println("    Lacuna: "+length.i1+"/"+length.i2);
  }
}
