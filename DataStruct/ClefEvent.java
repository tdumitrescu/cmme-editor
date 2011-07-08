/*----------------------------------------------------------------------*/
/*

        Module          : ClefEvent.java

        Package         : DataStruct

        Classes Included: ClefEvent

        Purpose         : Clef event type

        Programmer      : Ted Dumitrescu

        Date Started    : 1/99

Updates:
4/99:    cleaned up, consolidated with Gfx code
4/26/99: added b-rotundum and b-quadratum clefs to replace "signatures"
         added automated display modernization
4/27/99: added multiple simultaneous clef support
2/25/05: rearranged clef type system (see Clef)
4/05:    replaced "multi-clef" event list with separate class ClefSet (to
         facilitate more involved clef/signature calculations)
3/23/06: added support for 'full'-colored clefs

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   ClefEvent
Extends: Event
Purpose: Data/routines for clef events
------------------------------------------------------------------------*/

public class ClefEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  ClefSet clefset,       /* clef group */
          modernclefset; /* modern version of clef group */
  Clef    clef;          /* clef data for just this event */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ClefEvent(String n,int l,Pitch p,Event le,Event cie,boolean s)
Purpose:     Creates clef event
Parameters:
  Input:  String n  - clef type
          int l     - staff location of clef
          Pitch p   - clef pitch
          Event le  - previous event
          Event cie - clef info event
          boolean s - signature clef?
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ClefEvent(String n,int l,Pitch p,Event le,Event cie,boolean s)
  {
    int  cleftype=Clef.CLEF_C;
    Clef displayClef=null;

    eventtype=EVENT_CLEF;

    cleftype=Clef.strToCleftype(n);
    if (cie!=null)
      displayClef=cie.getClefSet().getprincipalclef();
    clef=new Clef(cleftype,l,p,false,s,displayClef);
    constructClefSets(le,cie);
  }

/*------------------------------------------------------------------------
Constructor: ClefEvent(Clef c,Event le,Event cie)
Purpose:     Creates clef event
Parameters:
  Input:  Clef  c  - clef information
          Event le - previous event
          Event cie - clef info event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ClefEvent(Clef c,Event le,Event cie)
  {
    eventtype=EVENT_CLEF;

    clef=c;
    constructClefSets(le,cie);
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
    Event e=new ClefEvent(new Clef(this.clef),null,null);
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
    ClefEvent otherCE=(ClefEvent)other;
    return this.clef.equals(otherCE.clef);
  }

/*------------------------------------------------------------------------
Method:  void constructClefSets(Event le,Event cie)
Purpose: Create or modify this event's clef sets
Parameters:
  Input:  Event le  - previous event
          Event cie - clef info event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void constructClefSets(Event le,Event cie)
  {
    clef.resetModClef();
    if (hasSignatureClef() &&
        le!=null && le.hasSignatureClef() && le.getClefSet()!=null)
      {
        clefset=le.getClefSet().addclef(clef);
        modernclefset=le.getClefSet(true).addclef(clef.modernclef);
      }
    else
      {
        clefset=new ClefSet(clef);
        modernclefset=new ClefSet(clef.modernclef);
      }
    if (cie!=null && !clefset.getprincipalclef().isprincipalclef())
      addToSigClefs(cie);

    /* if clef didn't get added to signature set, let's not display it in signature */
    clef.setDrawInSig(clefset.contains(clef));
    clef.modernclef.setDrawInSig(modernclefset.contains(clef.modernclef));
  }

/*------------------------------------------------------------------------
Method:  void removefromclefsets()
Purpose: Remove this clef from its clef sets (for deleting)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removefromclefsets()
  {
    clefset.removeclef(clef);
    modernclefset.removeclef(clef.modernclef);
  }

/*------------------------------------------------------------------------
Method:  void selectClefStyle()
Purpose: Set clef display style based on note type and coloration scheme
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void selectClefStyle()
  {
    clef.setFill(colored ? colorscheme.secondaryFill : colorscheme.primaryFill);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Pitch getPitch()
  {
    return clef.getclefletter()=='B' ? clef.pitch : null;
  }

/*------------------------------------------------------------------------
Method:  ClefSet getClefSet(boolean usemodernclefs)
Purpose: Returns clef set from this event
Parameters:
  Input:  boolean usemodernclefs - whether to return modern clefs
  Output: -
  Return: clef set data
------------------------------------------------------------------------*/

  public ClefSet getClefSet()
  {
    return clefset;
  }

  public ClefSet getClefSet(boolean useModernAccSystem)
  {
    return useModernAccSystem ? modernclefset : clefset;
  }

  public boolean hasAccidentalClef()
  {
    return clef.issharp() || clef.isflat();
  }

  public boolean hasPrincipalClef()
  {
    return clef.isprincipalclef();
  }

  public boolean hasSignatureClef()
  {
    return clef.issignatureclef() || clef.isprincipalclef();
  }

  public boolean drawInSig(boolean useModernClefs,boolean useModernAccSystem)
  {
    return getClef(useModernClefs,useModernAccSystem).drawInSig();
  }

/*------------------------------------------------------------------------
Method:  Clef getClef(boolean useModernClefs,boolean useModernAccSystem)
Purpose: Returns clef data from this event
Parameters:
  Input:  boolean useModernClefs     - whether to return modern clefs
          boolean useModernAccSystem - whether to follow modern accidental rules
  Output: -
  Return: clef data
------------------------------------------------------------------------*/

  public Clef getClef(boolean useModernClefs,boolean useModernAccSystem)
  {
    if (!clef.isprincipalclef())
      return useModernAccSystem ? clef.modernclef : clef;
    return useModernClefs ? clef.modernclef : clef;
  }

  public Clef getClef()
  {
    return getClef(false,false);
  }

/*------------------------------------------------------------------------
Method:  int getnumclefsinset(boolean usemodernclefs)
Purpose: Returns number of clefs in multi-clef set
Parameters:
  Input:  boolean usemodernclefs - whether to use modern clef set
  Output: -
  Return: number of clefs
------------------------------------------------------------------------*/

  public int getnumclefsinset(boolean usemodernclefs)
  {
    return usemodernclefs ? modernclefset.size() :
                            clefset.size();
  }

/*------------------------------------------------------------------------
Method:  boolean contradicts(ClefEvent other,boolean usemodernclefs)
Purpose: Calculate whether this clef contradicts another one
Parameters:
  Input:  ClefEvent other        - event for comparison clef
          boolean usemodernclefs - whether to check modern clefs
  Output: -
  Return: Whether clefs conflict
------------------------------------------------------------------------*/
/*
  public boolean contradicts(ClefEvent other,boolean usemodernclefs)
  {
    return usemodernclefs ? modernclefset.contradicts(other.modernclefset) :
                            clefset.contradicts(other.clefset);
  }*/

/*------------------------------------------------------------------------
Method:  boolean containsClef(Clef c,boolean usemodernclefs)
Purpose: Calculate whether this multiple clef contains another
Parameters:
  Input:  Clef c                 - clef to check for inclusion
          boolean usemodernclefs - whether to check modern clefs
  Output: -
  Return: Whether c is contained in this
------------------------------------------------------------------------*/

  public boolean containsClef(Clef c,boolean usemodernclefs)
  {
    return usemodernclefs ? modernclefset.containsClef(c) : clefset.containsClef(c);
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setClefSet(ClefSet cs,boolean usemodernclefs)
  {
    if (!usemodernclefs)
      clefset=cs;
    else
      modernclefset=cs;
  }

  public void setSignature(boolean s)
  {
    clef.setSignature(s);
  }

  /* overrides Event methods */
  public void setColored(boolean c)
  {
    super.setColored(c);
    selectClefStyle();
  }

  public void setcolorparams(Coloration c)
  {
    colorscheme=c;
    selectClefStyle();
  }

  public void modifyPitch(int offset)
  {
    getPitch().add(offset);
    clef.linespacenum+=offset;
    clef.setDrawInSig(true);
    clef.modernclef.setDrawInSig(true);
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
    clef.prettyprint();
  }
}
