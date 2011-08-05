/*----------------------------------------------------------------------*/
/*

        Module          : LineEndEvent.java

        Package         : DataStruct

        Classes Included: LineEndEvent

        Purpose         : Event type for line-ends

        Programmer      : Ted Dumitrescu

        Date Started    : 7/24/06

        Updates         :
7/24/06: originally this was a "dataless" event type without its own class;
         now it contains an optional page-end marker.

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   LineEndEvent
Extends: Event
Purpose: Data/routines for line-end events
------------------------------------------------------------------------*/

public class LineEndEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  boolean pageEnd;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: LineEndEvent([boolean pe])
Purpose:     Creates LineEnd event
Parameters:
  Input:  boolean pe - whether this is also a page-end
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public LineEndEvent()
  {
    this(false);
  }

  public LineEndEvent(boolean pe)
  {
    super();
    eventtype=EVENT_LINEEND;
    pageEnd=pe;
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
    Event e=new LineEndEvent(this.pageEnd);
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
    LineEndEvent otherLEE=(LineEndEvent)other;

    return this.pageEnd==otherLEE.pageEnd;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public boolean isPageEnd()
  {
    return pageEnd;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setPageEnd(boolean pe)
  {
    pageEnd=pe;
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
    System.out.print("    LineEnd");
    if (pageEnd)
      System.out.println(" (page-end)");
    else
      System.out.println();
  }
}
