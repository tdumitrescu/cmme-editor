/*----------------------------------------------------------------------*/
/*

        Module          : DotEvent.java

        Package         : DataStruct

        Classes Included: DotEvent

        Purpose         : Dot event type

        Programmer      : Ted Dumitrescu

        Date Started    : 1/99

        Updates:
4/99:    cleaned up, consolidated with Gfx code
4/24/99: added spacenum parameter
5/6/05:  began adding type information
2/9/08:  added note parameter
7/7/08:  changed staff location to Pitch basis

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   DotEvent
Extends: Event
Purpose: Data/routines for dot events
------------------------------------------------------------------------*/

public class DotEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Class variables */

  /* dot type flags */
  public static final int DT_Addition=1, /* 001 */
                          DT_Division=2; /* 010 */

/*----------------------------------------------------------------------*/
/* Instance variables */

  int       dottype;
  Pitch     staffLoc;
  NoteEvent note; /* note affected by dot of addition */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: DotEvent(int dt,int i,NoteEvent ne)
Purpose:     Creates dot event
Parameters:
  Input:  int dt       - dot type
          Pitch p      - staff location represented as Pitch
          int i        - dot's staff space number
          NoteEvent ne - note affected by dot of addition
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public DotEvent(int dt,Pitch p,NoteEvent ne)
  {
    super();
    this.eventtype=EVENT_DOT;
    this.staffLoc=p;
    this.dottype=dt;
    this.note=ne;
  }

  public DotEvent(Pitch p,NoteEvent ne)
  {
    this(DT_Addition,p,ne);
  }

  public DotEvent(Pitch p)
  {
    this(DT_Addition,p,null);
  }

/*  public DotEvent(int dt,int i)
  {
    this(dt,i,null);
  }

  public DotEvent(int dt,int i,NoteEvent ne)
  {
    this.eventtype=EVENT_DOT;
    this.spacenum=i;
    this.dottype=dt;
    this.note=ne;
  }

  public DotEvent(int i,NoteEvent ne)
  {
    this(DT_Addition,i,ne);
  }

  public DotEvent(int i)
  {
    this(i,null);
  }*/

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
    Event e=new DotEvent(this.dottype,new Pitch(this.staffLoc),null);
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
    DotEvent otherDE=(DotEvent)other;

    return this.dottype==otherDE.dottype &&
           this.staffLoc.equals(otherDE.staffLoc);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int getdottype()
  {
    return this.dottype;
  }

  public NoteEvent getNote()
  {
    return this.note;
  }

  public Pitch getPitch()
  {
    return staffLoc;
  }

  public int calcYPos(Clef c)
  {
    return staffLoc.calcypos(c);
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setNote(NoteEvent n)
  {
    this.note=n;
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
    System.out.println("    Dot: "+staffLoc);
  }
}
