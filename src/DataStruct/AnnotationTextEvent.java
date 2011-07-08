/*----------------------------------------------------------------------*/
/*

        Module          : AnnotationTextEvent.java

        Package         : DataStruct

        Classes Included: AnnotationTextEvent

        Purpose         : Event type for text annotations

        Programmer      : Ted Dumitrescu

        Date Started    : 9/26/05

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   AnnotationTextEvent
Extends: Event
Purpose: Data/routines for text annotation events
------------------------------------------------------------------------*/

public class AnnotationTextEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int DEFAULT_STAFFLOC=-3;

/*----------------------------------------------------------------------*/
/* Instance variables */

  String text;
  int    staffloc;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: AnnotationTextEvent(String t,int sl)
Purpose:     Creates text annotation event
Parameters:
  Input:  String t - text of annotation
          int sl   - vertical staff position
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public AnnotationTextEvent(String t,int sl)
  {
    eventtype=EVENT_ANNOTATIONTEXT;
    text=new String(t);
    staffloc=sl;
  }

  public AnnotationTextEvent(String t)
  {
    eventtype=EVENT_ANNOTATIONTEXT;
    text=new String(t);
    staffloc=DEFAULT_STAFFLOC;
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
    Event e=new AnnotationTextEvent(this.text,this.staffloc);
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
    AnnotationTextEvent otherATE=(AnnotationTextEvent)other;

    return this.text.equals(otherATE.text) &&
           this.staffloc==otherATE.staffloc;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public String gettext()
  {
    return text;
  }

  public int getstaffloc()
  {
    return staffloc;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void settext(String t)
  {
    text=t;
  }

  public void setstaffloc(int sl)
  {
    staffloc=sl;
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
    System.out.println("    Text annotation: "+text);
  }
}
