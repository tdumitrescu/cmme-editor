/*----------------------------------------------------------------------*/
/*

        Module          : ColorChangeEvent.java

        Package         : DataStruct

        Classes Included: ColorChangeEvent

        Purpose         : Event type for coloration changes

        Programmer      : Ted Dumitrescu

        Date Started    : 9/13/05

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   ColorChangeEvent
Extends: Event
Purpose: Data/routines for coloration-change events
------------------------------------------------------------------------*/

public class ColorChangeEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Coloration newcolor;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ColorChangeEvent(Coloration nc)
Purpose:     Creates coloration-change event
Parameters:
  Input:  Coloration nc - new coloration scheme
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ColorChangeEvent(Coloration nc)
  {
    super();
    eventtype=EVENT_COLORCHANGE;
    newcolor=nc;
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
    Event e=new ColorChangeEvent(new Coloration(this.newcolor));
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
    ColorChangeEvent otherCCE=(ColorChangeEvent)other;

    return this.newcolor.equals(otherCCE.newcolor);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Coloration getcolorscheme()
  {
    return newcolor;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setcolorscheme(Coloration nc)
  {
    newcolor=nc;
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
    System.out.print("    Color change: ");
    newcolor.prettyprint();
  }
}
