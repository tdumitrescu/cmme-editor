/*----------------------------------------------------------------------*/
/*

        Module          : MensEvent.java

        Package         : DataStruct

        Classes Included: MensEvent

        Purpose         : Mensuration event type

        Programmer      : Ted Dumitrescu

        Date Started    : 1/99

        Updates         :
4/99:     cleaned up, consolidated with Gfx code
4/14/05:  removed class MensAttrib, replaced with attribute flag system
7/31/06:  re-structured sign system for representation as a list of elements
          (LinkedList<MensSignElement>), to allow arbitrary sizes and
          combinations of signs and numbers (as already implemented in the
          XML format)
12/22/08: added attribute scoreSig to allow explicit control of time signature
          changes in score

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

/*------------------------------------------------------------------------
Class:   MensEvent
Extends: Event
Purpose: Data/routines for mensuration events
------------------------------------------------------------------------*/

public class MensEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Mensuration                 mensInfo;
  LinkedList<MensSignElement> signs;
  int                         ssnum;    /* place on staff */
  boolean                     small,
                              vertical, /* visual arrangement of multiple signs */
                              noScoreSig; /* sets time signature for all parts in score */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MensEvent(LinkedList<MensSignElement> sl,int ssn[,boolean sm,boolean v,Mensuration mi])
Purpose:     Creates mensuration event
Parameters:
  Input:  LinkedList<MensSignElement> sl - list of signs representing mensuration
          int ssn                        - location on staff
          boolean sm                     - whether signs are small or normal-size
          boolean v                      - whether signs are arranged vertically
          Mensuration mi                 - mensuration information
          boolean noScoreSig               - sets time signature?
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MensEvent(LinkedList<MensSignElement> sl,int ssn,boolean sm,boolean v,Mensuration mi,boolean noScoreSig)
  {
    this.eventtype=EVENT_MENS;
    this.signs=sl;
    this.ssnum=ssn;
    this.small=sm;
    this.vertical=v;
    this.noScoreSig=noScoreSig;
    if (mi!=null)
      this.mensInfo=mi;
    else
      initMensInfo();
  }

  public MensEvent(LinkedList<MensSignElement> sl,int ssn,boolean sm,boolean v,Mensuration mi)
  {
    this(sl,ssn,sm,v,mi,false);
  }

  public MensEvent(LinkedList<MensSignElement> sl,int ssn,boolean sm,boolean v)
  {
    this(sl,ssn,sm,v,null);
  }

  public MensEvent(LinkedList<MensSignElement> sl,int ssn)
  {
    this(sl,ssn,false,false);
  }

  void initMensInfo()
  {
    MensSignElement mainEl=getMainSign();
    this.mensInfo=new Mensuration(
      mainEl.dotted ? Mensuration.MENS_TERNARY : Mensuration.MENS_BINARY,
      mainEl.signType==MensSignElement.MENS_SIGN_O ? Mensuration.MENS_TERNARY : Mensuration.MENS_BINARY,
      Mensuration.MENS_BINARY,
      Mensuration.MENS_BINARY);
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
    LinkedList<MensSignElement> copySigns=new LinkedList<MensSignElement>();
    for (MensSignElement s : this.signs)
      copySigns.add(new MensSignElement(s));

    Event e=new MensEvent(copySigns,ssnum,
                          small,vertical,new Mensuration(mensInfo),
                          this.noScoreSig);
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
    MensEvent otherME=(MensEvent)other;
    if (this.signs.size()!=otherME.signs.size())
      return false;
    for (int i=0; i<this.signs.size(); i++)
      if (!this.signs.get(i).equals(otherME.signs.get(i)))
        return false;
    return this.mensInfo.equals(otherME.mensInfo) &&
           this.ssnum==otherME.ssnum &&
           this.small==otherME.small &&
           this.noScoreSig==otherME.noScoreSig &&
           this.vertical==otherME.vertical;
  }

  public boolean signEquals(MensEvent otherME)
  {
    if (this.signs.size()!=otherME.signs.size())
      return false;
    for (int i=0; i<this.signs.size(); i++)
      if (!this.signs.get(i).equals(otherME.signs.get(i)))
        return false;
    return true;
  }

/*------------------------------------------------------------------------
Methods: boolean nonStandard()
Purpose: Determine whether the mensural interpretation of this sign is standard
         (e.g., O = perfect tempus, minor prolatio)
Parameters:
  Input:  -
  Output: -
  Return: true if non-standard mensural interpretation of sign
------------------------------------------------------------------------*/

  public boolean nonStandard()
  {
    if (signs.size()>1)
      return true;
    if (!mensInfo.tempoChange.equals(Mensuration.DEFAULT_TEMPO_CHANGE))
      return true;
    MensSignElement mainEl=getMainSign();
    if (mainEl.dotted && mensInfo.prolatio!=Mensuration.MENS_TERNARY)
      return true;
    switch (mainEl.signType)
      {
        case MensSignElement.MENS_SIGN_O:
          if (mensInfo.tempus!=Mensuration.MENS_TERNARY)
            return true;
          break;
        case MensSignElement.MENS_SIGN_C:
          if (mensInfo.tempus==Mensuration.MENS_TERNARY)
            return true;
          break;
        default:
          return true;
      }
    if (mensInfo.modus_minor==Mensuration.MENS_TERNARY ||
        mensInfo.modus_maior==Mensuration.MENS_TERNARY)
      return true;

    return false;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Mensuration getMensInfo()
  {
    return mensInfo;
  }

  public LinkedList<MensSignElement> getSigns()
  {
    return signs;
  }

  public MensSignElement getMainSign()
  {
    return (MensSignElement)signs.getFirst();
  }

  public int getStaffLoc()
  {
    return ssnum;
  }

  public Iterator iterator()
  {
    return signs.iterator();
  }

  public Proportion getTempoChange()
  {
    return this.mensInfo.tempoChange;
  }

  public boolean noScoreSig()
  {
    return this.noScoreSig;
  }

  public boolean small()
  {
    return small;
  }

  public boolean vertical()
  {
    return vertical;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set parameters and options
Parameters:
  Input:  new values for parameters and options
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addSignElement(MensSignElement mse)
  {
    signs.add(mse);
  }

  public void deleteSignElement(int elNum)
  {
    signs.remove(elNum);
  }

  public void setMensInfo(Mensuration m)
  {
    mensInfo=m;
  }

  public void setNoScoreSig(boolean noScoreSig)
  {
    this.noScoreSig=noScoreSig;
  }

  public void setStaffLoc(int ssn)
  {
    ssnum=ssn;
  }

  public void setTempoChange(Proportion tempoChange)
  {
    this.mensInfo.tempoChange=new Proportion(tempoChange);
  }

  public void toggleNoScoreSig()
  {
    noScoreSig=!noScoreSig;
  }

  public void toggleSize()
  {
    small=!small;
  }

  public void toggleVertical()
  {
    vertical=!vertical;
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
    System.out.println("    Mensuration: ");
    for (Iterator i=iterator(); i.hasNext();)
      ((MensSignElement)i.next()).prettyprint();
    if (nonStandard())
      {
        System.out.println("    ");
        mensInfo.prettyprint();
      }
  }
}
