/*----------------------------------------------------------------------*/
/*

        Module          : ProportionEvent.java

        Package         : DataStruct

        Classes Included: ProportionEvent

        Purpose         : Event type for rhythmic proportion (not a sign)

        Programmer      : Ted Dumitrescu

        Date Started    : 9/9/05

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   ProportionEvent
Extends: Event
Purpose: Data/routines for proportion events
------------------------------------------------------------------------*/

public class ProportionEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Proportion proportion;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ProportionEvent(int i1,int i2)
Purpose:     Creates proportion event
Parameters:
  Input:  int i1,i2 - numerator and denominator of the proportion represented
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ProportionEvent(int i1,int i2)
  {
    eventtype=EVENT_PROPORTION;
    proportion=new Proportion(i1,i2);
  }

  public ProportionEvent(Proportion p)
  {
    eventtype=EVENT_PROPORTION;
    proportion=p;
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
    Event e=new ProportionEvent(new Proportion(this.proportion));
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
    ProportionEvent otherPE=(ProportionEvent)other;

    return this.proportion.equals(otherPE.proportion);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Proportion getproportion()
  {
    return proportion;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setproportion(int i1,int i2)
  {
    proportion=new Proportion(i1,i2);
  }

  public void setproportion(Proportion p)
  {
    proportion=p;
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
    System.out.println("    Proportion: "+proportion.i1+"/"+proportion.i2);
  }
}
