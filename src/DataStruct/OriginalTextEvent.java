/*----------------------------------------------------------------------*/
/*

        Module          : OriginalTextEvent.java

        Package         : DataStruct

        Classes Included: OriginalTextEvent

        Purpose         : Event type for old-style text phrases

        Programmer      : Ted Dumitrescu

        Date Started    : 9/8/06

        Updates         :
11/28/08: added variant version info (for multi-text display)

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   OriginalTextEvent
Extends: Event
Purpose: Data/routines for original text events
------------------------------------------------------------------------*/

public class OriginalTextEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  String             text;
  VariantVersionData variantVersion; /* if associated with one version */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: OriginalTextEvent(String t,VariantVersionData variantVersion)
Purpose:     Creates original text event
Parameters:
  Input:  String t                          - text phrase
          VariantVersionData variantVersion - version for which to display
  Output: -
------------------------------------------------------------------------*/

  public OriginalTextEvent(String t,VariantVersionData variantVersion)
  {
    eventtype=EVENT_ORIGINALTEXT;
    text=new String(t);
    this.variantVersion=variantVersion;
  }

  public OriginalTextEvent(String t)
  {
    this(t,null);
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
    Event e=new OriginalTextEvent(this.text);
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
    OriginalTextEvent otherOTE=(OriginalTextEvent)other;

    return this.text.equals(otherOTE.text);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public String getText()
  {
    return text;
  }

  public VariantVersionData getVariantVersion()
  {
    return variantVersion;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setText(String t)
  {
    text=t;
  }

  public void setVariantVersion(VariantVersionData variantVersion)
  {
    this.variantVersion=variantVersion;
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
    System.out.println("    Original text: "+text);
  }
}
