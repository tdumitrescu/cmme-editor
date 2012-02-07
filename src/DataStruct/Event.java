/*----------------------------------------------------------------------*/
/*

        Module          : Event.java

        Package         : DataStruct

        Classes Included: Event

        Purpose         : Base type for music events

        Programmer      : Ted Dumitrescu

        Date Started    : 1/99

        Updates         :
4/99:     cleaned up, consolidated with Gfx code
4/25/99:  added variable x length for untimed events
3/22/05:  removed deprecated event types
          replaced x-positioning dot hack with MusicRenderer.XPOS and XSPACING systems
3/25/05:  moved drawing code to Gfx.RenderedEvent
4/01/05:  moved XPOS/XSPACING calculation to Gfx.MusicRenderer (last of Gfx code
          in DataStruct)
5/18/05:  removed subtype LineendEvent; now data-free events are created simply
          as Events with a particular eventtype
2/1/06:   created MultiEvent type for simultaneous events
3/15/06:  created Ellipsis event type to represent skipped music between
          incipit and finalis (in incipit scores)
3/21/06:  made 'colored' an attribute of all events (not just notes)
7/20/06:  added 'editorial' attribute for marking editorial replacements/additions
11/28/06: added editorial commentary
2/19/07:  replaced type PIECEEND with SECTIONEND
3/26/07:  changed flag 'coronata' to Signum 'corona' (for extra position info)
11/7/07:  replaced types EDITORIALDATA_START/END with VARIANTDATA_START/END
1/5/08:   added function createCopy

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

/*------------------------------------------------------------------------
Class:   Event
Extends: -
Purpose: Base data/routines for events
------------------------------------------------------------------------*/

public class Event
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int EVENT_BASE=               0,
                          EVENT_CLEF=               1,
                          EVENT_MENS=               2,
                          EVENT_NOTE=               3,
                          EVENT_REST=               4,
                          EVENT_DOT=                5,
                          EVENT_ORIGINALTEXT=       6,
                          EVENT_CUSTOS=             7,
                          EVENT_LINEEND=            8,
                          EVENT_SECTIONEND=         9,
                          EVENT_PROPORTION=         10,
                          EVENT_COLORCHANGE=        11,
                          EVENT_BARLINE=            12,
                          EVENT_ANNOTATIONTEXT=     13,
                          EVENT_LACUNA=             14,
                          EVENT_LACUNA_END=         15,

                          EVENT_MODERNKEYSIGNATURE= 16,

                          EVENT_MULTIEVENT=         17,
                          EVENT_ELLIPSIS=           18, /* marker for skipped body in incipit scores */
                          EVENT_VARIANTDATA_START=  19,
                          EVENT_VARIANTDATA_END=    20,
                          EVENT_BLANK=              21;
  public static String[]  EventNames=new String[]
                            {
                              "Event","Clef","Mensuration","Note","Rest","Dot","OriginalText",
                              "Custos","Line End","Section End","Proportion",
                              "ColorChange","Barline","Annotation",
                              "Lacuna","LacunaEnd",

                              "ModernKeySignature",

                              "MultiEvent","Ellipsis","VariantDataStart","VariantDataEnd",
                              "Blank"
                            };

/*----------------------------------------------------------------------*/
/* Instance variables */

  int               eventtype;
  LinkedList<Event> eventList; /* to integrate MultiEvents */

  Proportion musictime=new Proportion(0,1);
  boolean    verticallyAligned=false, /* for multiple events at the same x-loc */
             colored=false,
             editorial=false,
             error=false,
             displayEditorial=false;
  Signum     corona=null,
             signum=null;
  String     edCommentary=null;

  protected Coloration         colorscheme=Coloration.DEFAULT_COLORATION;
  protected ModernKeySignature modernKeySig=ModernKeySignature.DEFAULT_SIG;
  protected Proportion         rhythmicProportion=Proportion.EQUALITY;

  Event      clefinfoevent,mensinfoevent; /* current clef and mensuration */

  private int            listplace,           /* place in voice's event list */
                         defaultListPlace;    /* place in event list without variants */
  private VariantReading variantReading=null; /* variant to which event belongs */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: Event()
Purpose:     Creates event of base type
Parameters:
  Input:  -
  Output: -
------------------------------------------------------------------------*/

  public Event()
  {
    eventtype=EVENT_BASE;
    eventList=new LinkedList<Event>();
    eventList.add(this);
  }

/*------------------------------------------------------------------------
Constructor: Event(int etype)
Purpose:     Creates event of a given type (for data-free marker events
             such as LINEEND and SECTIONEND)
Parameters:
  Input:  int etype - event type
  Output: -
------------------------------------------------------------------------*/

  public Event(int etype)
  {
    this();
    eventtype=etype;

    if (eventtype==EVENT_ELLIPSIS)
      musictime.i1=1; /* treat as timed event */
  }

/*------------------------------------------------------------------------
Method:  Event createCopy()
Purpose: Create copy of current event (to be overridden)
Parameters:
  Input:  -
  Output: -
  Return: copy of this
------------------------------------------------------------------------*/

  public Event createCopy()
  {
    return new Event(this.eventtype);
  }

/*------------------------------------------------------------------------
Method:  void copyEventAttributes(Event other)
Purpose: Copy attributes from another event
Parameters:
  Input:  Event other - event with attributes to copy
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void copyEventAttributes(Event other)
  {
    this.musictime=Proportion.copyProportion(other.musictime);
    this.verticallyAligned=other.verticallyAligned;
    this.colored=other.colored;
    this.editorial=other.editorial;
    this.error=other.error;
    this.corona=other.corona==null ? null : new Signum(other.corona);
    this.signum=other.signum==null ? null : new Signum(other.signum);
    this.edCommentary=other.edCommentary==null ? null : new String(other.edCommentary);
    this.colorscheme=new Coloration(other.colorscheme);
    this.modernKeySig=new ModernKeySignature(other.modernKeySig);

    this.clefinfoevent=other.clefinfoevent;
    this.mensinfoevent=other.mensinfoevent;
  }

/*------------------------------------------------------------------------
Method:  LinkedList<Event> makeModernNoteShapes()
Purpose: Make event (copy) in modern notation (to be overridden)
Parameters:
  Input:  -
  Output: -
  Return: copy of this with modern note shape, expanded into multiple
          events if necessary
------------------------------------------------------------------------*/

  public LinkedList<Event> makeModernNoteShapes(Proportion timePos,Proportion measurePos,
                                                int measureMinims,Proportion measureProp,
                                                Proportion timeProp,
                                                boolean useTies)
  {
    LinkedList<Event> el=new LinkedList<Event>();
    el.add(this);

    return el;
  }

  public boolean isMinorColor()
  {
    return false;
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
    return this.eventtype==other.eventtype &&
           this.musictime.equals(other.musictime) &&
           this.verticallyAligned==other.verticallyAligned &&
           this.colored==other.colored &&
           this.editorial==other.editorial &&
           this.error==other.error &&
           this.coronaEquals(other) &&
           this.signumEquals(other) &&
           this.edCommentaryEquals(other);
  }

  public boolean coronaEquals(Event other)
  {
    if (this.corona==null)
      return other.corona==null;
    return this.corona.equals(other.corona);
  }

  public boolean signumEquals(Event other)
  {
    if (this.signum==null)
      return other.signum==null;
    return this.signum.equals(other.signum);
  }

  public boolean edCommentaryEquals(Event other)
  {
    if (this.edCommentary==null)
      return other.edCommentary==null;
    return this.edCommentary.equals(other.edCommentary);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int geteventtype()
  {
    return eventtype;
  }

  public String getTypeName()
  {
    return EventNames[eventtype];
  }

  public Proportion getmusictime()
  {
    return musictime;
  }

  public boolean isColored()
  {
    return colored;
  }

  public String getEdCommentary()
  {
    return edCommentary;
  }

  public int getcolor()
  {
    return colorscheme.primaryColor;
  }

  public int getcolorfill()
  {
    return colorscheme.primaryFill;
  }

  public Coloration getcoloration()
  {
    return colorscheme;
  }

  public Signum getCorona()
  {
    return corona;
  }

  public Signum getSignum()
  {
    return signum;
  }

  public boolean isEditorial()
  {
    return editorial;
  }

  public boolean displayAsEditorial()
  {
    return displayEditorial;
  }

  public boolean isError()
  {
    return error;
  }

  public boolean alignedWithPrevious()
  {
    return verticallyAligned;
  }

  /* to be overridden */
  public Pitch getPitch()
  {
    return null;
  }

  public Proportion getLength()
  {
    return null;
  }

  public int getnotetype()
  {
    System.err.println("Error: trying to get notetype of a non-note event");
    return -1;
  }

  public boolean isflagged()
  {
    return false;
  }

  public Event getClefInfoEvent()
  {
    return clefinfoevent;
  }

  public ClefSet getClefSet()
  {
    return null;
  }

  public ClefSet getClefSet(boolean usemodernclefs)
  {
    return null;
  }

  public Clef getPrincipalClef(boolean usemodernclefs)
  {
    ClefSet cs=getClefSet(usemodernclefs);
    if (cs==null)
      return null;
    return cs.getprincipalclef();
  }

  public Proportion getProportion()
  {
    return rhythmicProportion;
  }

  public Proportion calcTotalProportion()
  {
    if (mensinfoevent==null)
      return rhythmicProportion;
    else
      return Proportion.product(rhythmicProportion,mensinfoevent.getMensInfo().tempoChange);
  }

  public Proportion calcProportionalMusicLength()
  {
    Proportion ml=Proportion.quotient(getmusictime(),calcTotalProportion());

    return ml!=null ? ml : new Proportion(0,1);
  }

  public VariantReading getVariantReading(VariantVersionData version)
  {
    if (variantReading==null || !variantReading.includesVersion(version))
      return null;
    return variantReading;
  }

  public List<Event> getSubEvents()
  {
    return eventList;
  }

  public boolean hasEventType(int etype)
  {
    return eventtype==etype;
  }

  public Event getFirstEventOfType(int etype)
  {
    if (eventtype==etype)
      return this;
    else
      return null;
  }

  public boolean hasAccidentalClef()
  {
    return false;
  }

  public boolean hasPrincipalClef()
  {
    return false;
  }

  public boolean hasSignatureClef()
  {
    return false;
  }

  public boolean hasVariantColoration(Event other)
  {
    if (this.eventtype!=other.eventtype)
      return false;
    return this.colored!=other.colored;
  }

  public boolean inVariant()
  {
    return variantReading!=null;
  }

  public Mensuration getBaseMensInfo()
  {
    return (mensinfoevent!=null) ? mensinfoevent.getMensInfo() :
                                   Mensuration.DEFAULT_MENSURATION;
  }

  public Mensuration getMensInfo()
  {
    return null;
  }

  public Event getMensInfoEvent()
  {
    return mensinfoevent;
  }

  public ModernKeySignature getModernKeySig()
  {
    return modernKeySig;
  }

  public int rhythmicEventType()
  {
    return eventtype;
  }

/*------------------------------------------------------------------------
Method:  boolean notePitchMatches(Event other)
Purpose: Calculate whether this event's pitch(es) match(es) those of another;
         only for note events
Parameters:
  Input:  Event other - event for comparison
  Output: -
  Return: Whether pitches are equal
------------------------------------------------------------------------*/

  public boolean notePitchMatches(Event other)
  {
    return false; /* not a note */
  }

  public boolean hasNotePitch(Pitch p)
  {
    return false;
  }

/*------------------------------------------------------------------------
Method:  boolean principalClefEquals(Event other,boolean usemodernclefs)
Purpose: Calculate whether this event's clef set has the same principal clef as
         another one
Parameters:
  Input:  Event other            - event for comparison clef
          boolean usemodernclefs - whether to check modern clefs
  Output: -
  Return: Whether principal clefs are equal
------------------------------------------------------------------------*/

  public boolean principalClefEquals(Event other,boolean usemodernclefs)
  {
    Clef pc1=getPrincipalClef(usemodernclefs),
         pc2=other.getPrincipalClef(usemodernclefs);

    if (pc1==null || pc2==null)
      return false;
    return pc1.equals(pc2);
  }

/*------------------------------------------------------------------------
Method:  void addToSigClefs(Event sigEvent)
Purpose: Create a new clef set for this event, adding this event's clefs
         to the current signature set
Parameters:
  Input:  Event sigEvent - event with current clef set
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addToSigClefs(Event sigEvent)
  {
    /* original clefs */
    ClefSet cs=new ClefSet(sigEvent.getClefSet());
    for (Clef c : this.getClefSet())
      cs.addclef(c);
    setClefSet(cs,false);

    /* modern clefs */
    cs=new ClefSet(sigEvent.getClefSet(true));
    for (Clef c : this.getClefSet(true))
      cs.addclef(c);
    setClefSet(cs,true);
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setColored(boolean c)
  {
    colored=c;
  }

  public void setEdCommentary(String s)
  {
    edCommentary=s;
  }

  public void setCorona(Signum c)
  {
    corona=c;
  }

  public void setProportion(Proportion p)
  {
    rhythmicProportion=p;
  }

  public void setSignum(Signum s)
  {
    signum=s;
  }

  public void setDisplayEditorial(boolean newVal)
  {
    displayEditorial=newVal;
  }

  public void setEditorial(boolean e)
  {
    editorial=e;
  }

  public void setError(boolean newVal)
  {
    error=newVal;
  }

  public void setAlignmentWithPrevious(boolean a)
  {
    verticallyAligned=a;
  }

  public void modifyPitch(int offset)
  {
    getPitch().add(offset);
  }

  /* to be overridden */
  public void setpitch(Pitch p)
  {
    System.err.println("Error: trying to assign pitch to an unpitched event");
  }

  public void setnotetype(int nt,int f,Mensuration m)
  {
    System.err.println("Error: trying to assign notetype to a non-note event");
  }

  public void setLength(Proportion l)
  {
    System.err.println("Error: trying to assign length to an untimed event");
  }

  public void setClefSet(ClefSet cs,boolean usemodernclefs)
  {
    System.err.println("Error: trying to set clef set on an invalid event");
  }

  public void constructClefSets(Event le,Event cie)
  {
  }

/*------------------------------------------------------------------------
Method:  void set*params
Purpose: Sets music parameters current at this event (clef, mensuration)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setclefparams(Event ce)
  {
    clefinfoevent=ce;

    /* assign clef information to pitches */
    if (ce!=null && ce.hasPrincipalClef())
      {
        Pitch p=getPitch();
        if (p!=null)
          {
            p.setclef(ce.getClefSet().getprincipalclef());
            if (geteventtype()==EVENT_CLEF)
              ((ClefEvent)this).getClef(false,false).linespacenum=p.staffspacenum+1;
          }
      }
  }

  public void setmensparams(Event me)
  {
    mensinfoevent=me;
  }

  public void setcolorparams(Coloration c)
  {
    colorscheme=c;
  }

  public void setModernKeySigParams(ModernKeySignature mks)
  {
    modernKeySig=mks;
  }

  public void setVariantReading(VariantReading variantReading)
  {
    this.variantReading=variantReading;
  }

/*------------------------------------------------------------------------
Method:  int getListPlace()
Purpose: Returns the list place of this event
Parameters:
  Input:  -
  Output: -
  Return: list place
------------------------------------------------------------------------*/

  public int getListPlace(boolean defaultPlace)
  {
    return defaultPlace ? defaultListPlace : listplace;
  }

  public int getDefaultListPlace()
  {
    return defaultListPlace;
  }

/*------------------------------------------------------------------------
Method:  void setListPlace(int lp)
Purpose: Sets the list place of this event
Parameters:
  Input:  int lp - new list place
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setListPlace(int lp)
  {
    listplace=lp;
  }

  public void setDefaultListPlace(int defaultListPlace)
  {
    this.defaultListPlace=defaultListPlace;
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
    System.out.println("    "+EventNames[eventtype]);
  }
}
