/*----------------------------------------------------------------------*/
/*

        Module          : ModernKeySignatureEvent.java

        Package         : DataStruct

        Classes Included: ModernKeySignatureEvent

        Purpose         : Event type for modern key signatures/signature changes

        Programmer      : Ted Dumitrescu

        Date Started    : 7/25/06

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   ModernKeySignatureEvent
Extends: Event
Purpose: Data/routines for modern key signature events
------------------------------------------------------------------------*/

public class ModernKeySignatureEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  ModernKeySignature sig;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ModernKeySignatureEvent([ModernKeySignature s])
Purpose:     Creates modern key signature event
Parameters:
  Input:  ModernKeySignature s - signature information
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ModernKeySignatureEvent()
  {
    this(ModernKeySignature.DEFAULT_SIG);
  }

  public ModernKeySignatureEvent(ModernKeySignature s)
  {
    eventtype=EVENT_MODERNKEYSIGNATURE;
    sig=new ModernKeySignature(s);
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
    Event e=new ModernKeySignatureEvent(this.sig);
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
    ModernKeySignatureEvent otherMKE=(ModernKeySignatureEvent)other;

    return this.sig.equals(otherMKE.sig);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public ModernKeySignature getSigInfo()
  {
    return sig;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addFlat()
  {
    sig.addFlat();
  }

  public void addSharp()
  {
    sig.addSharp();
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
    sig.prettyprint();
  }
}
