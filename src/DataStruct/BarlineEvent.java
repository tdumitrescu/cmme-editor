/*----------------------------------------------------------------------*/
/*

        Module          : BarlineEvent.java

        Package         : DataStruct

        Classes Included: BarlineEvent

        Purpose         : Event type for barlines

        Programmer      : Ted Dumitrescu

        Date Started    : 9/23/05

        Updates         :
11/10/07: added attributes repeatSign, bottomLinePos, and numSpaces

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   BarlineEvent
Extends: Event
Purpose: Data/routines for barline events
------------------------------------------------------------------------*/

public class BarlineEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  int     numLines,
          bottomLinePos,
          numSpaces;
  boolean repeatSign;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: BarlineEvent([int nl,boolean repeatSign,int bottomLinePos,int numSpaces])
Purpose:     Creates barline event
Parameters:
  Input:  int nl             - number of barlines
          boolean repeatSign - whether this is a repeat sign
          int bottomLinePos  - staff position of bottom of barline
          int numSpaces      - number of staff spaces covered by line
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public BarlineEvent()
  {
    this(1,false,0,4);
  }

  public BarlineEvent(int nl,boolean repeatSign,int bottomLinePos,int numSpaces)
  {
    super();
    eventtype=EVENT_BARLINE;
    this.numLines=nl;
    this.repeatSign=repeatSign;
    this.bottomLinePos=bottomLinePos;
    this.numSpaces=numSpaces;
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
    Event e=new BarlineEvent(this.numLines,this.repeatSign,this.bottomLinePos,this.numSpaces);
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
    BarlineEvent otherBE=(BarlineEvent)other;

    return this.numLines==otherBE.numLines &&
           this.repeatSign==otherBE.repeatSign &&
           this.bottomLinePos==otherBE.bottomLinePos &&
           this.numSpaces==otherBE.numSpaces;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int getBottomLinePos()
  {
    return bottomLinePos;
  }

  public int getNumLines()
  {
    return numLines;
  }

  public int getNumSpaces()
  {
    return numSpaces;
  }

  public boolean isRepeatSign()
  {
    return repeatSign;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setBottomLinePos(int bottomLinePos)
  {
    this.bottomLinePos=bottomLinePos;
  }

  public void setNumLines(int numLines)
  {
    this.numLines=numLines;
  }

  public void setNumSpaces(int numSpaces)
  {
    this.numSpaces=numSpaces;
  }

  public void setRepeatSign(boolean repeatSign)
  {
    this.repeatSign=repeatSign;
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
    System.out.println("    Barlines: "+numLines);
  }
}
