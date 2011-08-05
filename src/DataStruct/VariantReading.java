/*----------------------------------------------------------------------*/
/*

        Module          : VariantReading.java

        Package         : DataStruct

        Classes Included: VariantReading

        Purpose         : Handles one variant reading within a variant
                          version

        Programmer      : Ted Dumitrescu

        Date Started    : 10/27/2007

        Updates         :
7/2008:   added routines for auto-detection of variant types (hasVariant*)
10/28/09: revised detection routine for variants with accidentals, to
          avoid marking line-end key signature repetitions as significative
          variants
4/23/10:  fixed a few bugs when checking rhythms for notes with null
          rhythmic values (chant sections)

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   VariantReading
Extends: -
Purpose: Information structure for one variant reading
------------------------------------------------------------------------*/

public class VariantReading
{
/*----------------------------------------------------------------------*/
/* Class variables */

  /* flags for possible types of variation between readings */
  public static final long VAR_NONE=          0, /* 0000000000000000 */
                           VAR_NONSUBSTANTIVE=1, /* 0000000000000001 */
                           VAR_RHYTHM=        VAR_NONSUBSTANTIVE<<1,
                           VAR_PITCH=         VAR_RHYTHM<<1,
                           VAR_ORIGTEXT=      VAR_PITCH<<1,
                           VAR_ACCIDENTAL=    VAR_ORIGTEXT<<1,
                           VAR_CLEF=          VAR_ACCIDENTAL<<1,
                           VAR_LINEEND=       VAR_CLEF<<1,
                           VAR_COLORATION=    VAR_LINEEND<<1,
                           VAR_LIGATURE=      VAR_COLORATION<<1,
                           VAR_MENSSIGN=      VAR_LIGATURE<<1,
                           VAR_ERROR=         VAR_MENSSIGN<<1,
                           VAR_ALL=           Long.MAX_VALUE;

  public static String[]   typeNames=new String[]
                             {
                               "Non-substantive",
                               "Rhythm",
                               "Pitch",
                               "Text",
                               "Accidental",
                               "Clef",
                               "Line-break",
                               "Coloration",
                               "Ligature",
                               "Mensuration",
                               "Error"
                             };

  /* positioning of inserted event within reading */
  public static final int NEWVARIANT=0,
                          BEGINNING= 1,
                          MIDDLE=    2,
                          END=       3,
                          DELETED=   4,
                          COMBINED=  5,
                          NEWREADING=6,
                          NOACTION=  7;

/*----------------------------------------------------------------------*/
/* Instance variables */

  ArrayList<VariantVersionData> versions; /* list of versions containing
                                             this reading */
  EventListData events;
  Proportion    length; /* length of music of reading */

  int     sectionNum,
          voiceNum,
          eventIndex; /* index of first event in this reading (in default list) */
  boolean error;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Methods: int varIndex(long varType)
Purpose: Calculate array index of one variant flag
Parameters:
  Input:  long varType - flag
  Output: -
  Return: index in arrays of variant types
------------------------------------------------------------------------*/

  public static int varIndex(long varType)
  {
    int i=0;
    switch ((int)varType)
      {
        case 0:
          i=1; /* call "none" and "non-substantive" the same */
          break;
        case 1:
          i=1;
          break;
        default:
          i=1+(int)(Math.log(varType)/Math.log(2)); /* log2(varType)+1 */
      }
    return i-1;
  }

/*------------------------------------------------------------------------
Methods: String varTypesToStr(long varTypeFlags)
Purpose: Create string representation of variant types in a set of flags
Parameters:
  Input:  long varTypeFlags - flags
  Output: -
  Return: string
------------------------------------------------------------------------*/

  public static String varTypesToStr(long varTypeFlags)
  {
    if (varTypeFlags==VariantReading.VAR_NONE ||
        varTypeFlags==VariantReading.VAR_NONSUBSTANTIVE)
      return "Non-substantive";

    String vs="";

    if ((varTypeFlags&VariantReading.VAR_RHYTHM)>0)
      vs+="Rhythm / ";
    if ((varTypeFlags&VariantReading.VAR_PITCH)>0)
      vs+="Pitch / ";
    if ((varTypeFlags&VariantReading.VAR_ORIGTEXT)>0)
      vs+="Text / ";
    if ((varTypeFlags&VariantReading.VAR_CLEF)>0)
      vs+="Clef / ";
    if ((varTypeFlags&VariantReading.VAR_MENSSIGN)>0)
      vs+="Mensuration / ";
    if ((varTypeFlags&VariantReading.VAR_ACCIDENTAL)>0)
      vs+="Accidental / ";
    if ((varTypeFlags&VariantReading.VAR_COLORATION)>0)
      vs+="Coloration / ";
    if ((varTypeFlags&VariantReading.VAR_LIGATURE)>0)
      vs+="Ligature / ";
    if ((varTypeFlags&VariantReading.VAR_LINEEND)>0)
      vs+="Line break / ";

    try
      {
        vs=vs.substring(0,vs.length()-3); /* remove trailing " / " */
      }
    catch (Exception e)
      {
        vs="";
      }

    return vs;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantReading(int sectionNum,int voiceNum,int eventIndex)
Purpose:     Initialize structure
Parameters:
  Input:  int sectionNum,voiceNum,eventIndex - position of reading
  Output: -
------------------------------------------------------------------------*/

  public VariantReading()
  {
    versions=new ArrayList<VariantVersionData>();
    events=new EventListData();
    error=false;
    length=new Proportion(0,1);
  }

  public VariantReading(int sectionNum,int voiceNum,int eventIndex)
  {
    versions=new ArrayList<VariantVersionData>();
    events=new EventListData();
    error=false;
    length=new Proportion(0,1);
    this.sectionNum=sectionNum;
    this.voiceNum=voiceNum;
    this.eventIndex=eventIndex;
  }

/*------------------------------------------------------------------------
Method:  boolean equals(EventListData el,int i1,int i2)
Purpose: Check whether this reading is identical to another
Parameters:
  Input:  EventListData el - event list to check against this
          int i1,i2        - first and last indices to check
  Output: -
  Return: true if this is equal to the indicated portion of el
------------------------------------------------------------------------*/

  public boolean equals(EventListData el,int i1,int i2)
  {
    if (i2-i1+1!=getNumEvents())
      return false;
    for (int i=i1; i<=i2; i++)
      if (!el.getEvent(i).equals(this.getEvent(i-i1)))
        return false;
    return true;
  }

/*------------------------------------------------------------------------
Method:  boolean eventsEqual(VariantReading other)
Purpose: Check whether two readings contain the same event list
Parameters:
  Input:  VariantReading other - reading to compare against this
  Output: -
  Return: true if other has same list of events
------------------------------------------------------------------------*/

  public boolean eventsEqual(VariantReading other)
  {
    int numEvents=getNumEvents(),
        otherNumEvents=other.getNumEvents();

    if (numEvents!=otherNumEvents)
      return false;

    for (int i=0; i<numEvents; i++)
      if (this.getEvent(i)!=other.getEvent(i))
        return false;

    return true;
  }

/*------------------------------------------------------------------------
Method:  void recalcEventParams(Event paramEvent)
Purpose: Recalculate event parameters
Parameters:
  Input:  Event paramEvent - event for starting parameters
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void recalcEventParams(Event paramEvent)
  {
    events.recalcEventParams(paramEvent,paramEvent.getcoloration());
  }

/*------------------------------------------------------------------------
Methods: long calcVariantTypes(VoiceEventListData v)
Purpose: Calculate which types of variant are present between this reading
         and one other
Parameters:
  Input:  EventListData v - event list for comparison
          int varStarti   - index in list to start looking
  Output: -
  Return: flags representing variation types
------------------------------------------------------------------------*/

  public long calcVariantTypes(EventListData v,int varStarti)
  {
    long varFlags=VariantReading.VAR_NONE;

    if (this.hasVariantRhythm(v,varStarti))
      varFlags|=VariantReading.VAR_RHYTHM;
    if (this.hasVariantPitch(v,varStarti))
      varFlags|=VariantReading.VAR_PITCH;
    if (this.hasVariantOrigText(v,varStarti))
      varFlags|=VariantReading.VAR_ORIGTEXT;
    if (this.hasVariantAccidental(v,varStarti))
      varFlags|=VariantReading.VAR_ACCIDENTAL;
    if (this.hasVariantClef(v,varStarti))
      varFlags|=VariantReading.VAR_CLEF;
    if (this.hasVariantLineEnd(v,varStarti))
      varFlags|=VariantReading.VAR_LINEEND;
    if (this.hasVariantColoration(v,varStarti))
      varFlags|=VariantReading.VAR_COLORATION;
    if (this.hasVariantLigature(v,varStarti))
      varFlags|=VariantReading.VAR_LIGATURE;
    if (this.hasVariantMensSign(v,varStarti))
      varFlags|=VariantReading.VAR_MENSSIGN;
    if (this.isError())
      varFlags|=VariantReading.VAR_ERROR;

    return varFlags;
  }

/*------------------------------------------------------------------------
Methods: boolean hasVariant*(EventListData v,int vi)
Purpose: Calculate whether this reading has different rhythm|pitches|etc
         than the reading in another event list
Parameters:
  Input:  EventListData v - event list for comparison
          int vi          - index in list to start looking
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public boolean hasVariantRhythm(EventListData v,int vi)
  {
    int        i=0;
    Event      e1=null,e2=null;
    Proportion mt1=new Proportion(0,1),mt2=new Proportion(0,1);
    do
      {
        /* find position of next note in each list */
        /* differing rest arrangements (e.g. B vs. SB SB) are NOT rhythmic variants */
        e1=getVarEvent(events,i);
        while (e1!=null && e1.rhythmicEventType()!=Event.EVENT_NOTE)
          {
            mt1.add(e1.calcProportionalMusicLength());
            e1=getVarEvent(events,++i);
          }
        e2=getVarEvent(v,vi);
        while (e2!=null && e2.rhythmicEventType()!=Event.EVENT_NOTE)
          {
            mt2.add(e2.calcProportionalMusicLength());
            e2=getVarEvent(v,++vi);
          }

        /* differing rest lengths? */
        if (!mt1.equals(mt2))
          return true;

        /* differing note lengths? */
        if (e1!=null && e2!=null &&
            !e1.calcProportionalMusicLength().equals(e2.calcProportionalMusicLength()))
          return true;

        if (e1!=null)
          {
            mt1.add(e1.calcProportionalMusicLength());
            i++;
          }
        if (e2!=null)
          {
            mt2.add(e2.calcProportionalMusicLength());
            vi++;
          }

        /* are voices in the same place after adding the previous note(s)? */
        if (!mt1.equals(mt2))
          return true;
      }
    while (e1!=null && e2!=null);

    /* at least 1 list is finished; check that the other doesn't have any
       further rhythms */
    e1=getVarEvent(events,i);
    e2=getVarEvent(v,vi);
    if (e1==null)
      if (e2==null)
        return false;
      else
        while (e2!=null)
          {
            if (e2.getmusictime().i1!=0)
              return true;
            e2=getVarEvent(v,++vi);
          }
    else
      while (e1!=null)
        {
          if (e1.getmusictime().i1!=0)
            return true;
          e1=getVarEvent(events,++i);
        }

    return false;
  }

  public boolean hasVariantPitch(EventListData v,int vi)
  {
    int   i=0;
    Event e1=null,e2=null;
    Event pe1=null,pe2=null; /* events with pitch info */
    do
      {
        /* find position of next note in each list */
        e1=getVarEvent(events,i);
        while (e1!=null && !e1.hasEventType(Event.EVENT_NOTE))
          e1=getVarEvent(events,++i);
        e2=getVarEvent(v,vi);
        while (e2!=null && !e2.hasEventType(Event.EVENT_NOTE))
          e2=getVarEvent(v,++vi);

        /* skip to following notes if pitches are repeated */
        if (e1!=null)
          {
            pe1=e1;
            e1=getVarEvent(events,++i);
            boolean done=e1==null;
            while (!done)
              if (e1.hasEventType(Event.EVENT_NOTE) && !e1.notePitchMatches(pe1))
                done=true;
              else
                done=(e1=getVarEvent(events,++i))==null;

            if (e1!=null) /* found next non-matching pitch? */
              i--;
          }
        if (e2!=null)
          {
            pe2=e2;
            e2=getVarEvent(v,++vi);
            boolean done=e2==null;
            while (!done)
              if (e2.hasEventType(Event.EVENT_NOTE) && !e2.notePitchMatches(pe2))
                done=true;
              else
                done=(e2=getVarEvent(v,++vi))==null;

            if (e2!=null) /* found next non-matching pitch? */
              vi--;
          }

        /* extra pitch(es) in one? */
        if ((pe1==null)!=(pe2==null))
          return true;

        /* no pitches left in either? */
        if (pe1==null)
          return false;

        /* different pitches? */
        if (!pe1.notePitchMatches(pe2))
          return true;

        if (e1!=null)
          i++;
        if (e2!=null)
          vi++;
      }
    while (e1!=null || e2!=null);

    return false;
  }

  public boolean hasVariantOrigText(EventListData v,int vi)
  {
    int        i=0;
    Event      e1=null,e2=null;
    Proportion mt1=new Proportion(0,1),mt2=new Proportion(0,1);

    do
      {
        /* find position of next text event in each list */
        e1=getVarEvent(events,i);
        while (e1!=null && !e1.hasEventType(Event.EVENT_ORIGINALTEXT))
          {
            mt1.add(e1.getmusictime());
            e1=getVarEvent(events,++i);
          }
        e2=getVarEvent(v,vi);
        while (e2!=null && !e2.hasEventType(Event.EVENT_ORIGINALTEXT))
          {
            mt2.add(e2.getmusictime());
            e2=getVarEvent(v,++vi);
          }

        /* text in one but not the other? */
        if ((e1==null)!=(e2==null))
          return true;

        /* no text in either? */
        if (e1==null)
          return false;

        /* same text? */
        if (!((OriginalTextEvent)e1).getText().equals(((OriginalTextEvent)e2).getText()))
          return true;

        /* different positioning? */
        if (!mt1.equals(mt2))
          return true;

        if (e1!=null)
          i++;
        if (e2!=null)
          vi++;
      }
    while (e1!=null || e2!=null);

    return false;
  }

  public boolean hasVariantAccidental(EventListData v,int vi)
  {
    int        i=0;
    Event      e1=null,e2=null;
    Proportion mt1=new Proportion(0,1),mt2=new Proportion(0,1);

//System.out.println("HVA vi="+vi);
    e2=getVarEvent(v,vi);
    Event   mainClefEvent=e2==null ? v.getEvent(vi-1).getClefInfoEvent()
                                   : e2.getClefInfoEvent();
//System.out.println("    mce="+mainClefEvent);
    ClefSet mainClefSet=mainClefEvent==null ? null : mainClefEvent.getClefSet();
    boolean fullClefs=this.hasVariantLineEnd(v,vi) ||
                      vi==0 || mainClefSet==null ||
                      v.getEvent(vi-1).getClefInfoEvent()==null ||
                      (e2!=null && e2.hasPrincipalClef());

//System.out.println("fullClefs="+fullClefs);
    do
      {
        /* find position of next accidental in each list */
        e1=getVarEvent(events,i);
        while (e1!=null && !e1.hasAccidentalClef())
          {
//System.out.println("HVA1");
            mt1.add(e1.getmusictime());
            e1=getVarEvent(events,++i);
          }
        e2=getVarEvent(v,vi);
        while (e2!=null && !e2.hasAccidentalClef())
          {
            mt2.add(e2.getmusictime());
            e2=getVarEvent(v,++vi);
          }

        /* does either event change the key sig? */
        boolean e1NewSig=false,e2NewSig=false;
/*        if (mainClefEvent==null)
          {
            e1NewSig=e1!=null;
            e2NewSig=e2!=null;
          }
        else*/
        if (mainClefSet!=null)
          {
            if (e1!=null)
              e1NewSig=e1.getClefSet().sigContradicts(mainClefSet);
            if (e2!=null)
              e2NewSig=e2.getClefSet().sigContradicts(mainClefSet);
          }

        /* accidental in one but not the other? */
        if ((e1==null)!=(e2==null))
          {
            if (!fullClefs)
              return true;
            else
              if (e1NewSig || e2NewSig)
                return true;
          }

        /* no accidental in either? */
        else if (e1==null)
          return false;

        /* two clefs; same? */
        else
          {
            if (e1.getClefSet().sigContradicts(e2.getClefSet()))
              return true;

            /* different positioning? (if both are clef+sig, doesn't matter) */
            if (!fullClefs)
              {
                if (!mt1.equals(mt2))
                  return true;
              }
          }

        if (e1!=null)
          i++;
        if (e2!=null)
          vi++;
      }
    while (e1!=null || e2!=null);

    return false;
  }

  public boolean hasVariantClef(EventListData v,int vi)
  {
    int        i=0;
    Event      e1=null,e2=null;
    Proportion mt1=new Proportion(0,1),mt2=new Proportion(0,1);

    do
      {
        /* find position of next (principal) clef in each list */
        e1=getVarEvent(events,i);
        while (e1!=null && !e1.hasPrincipalClef())
          {
            mt1.add(e1.getmusictime());
            e1=getVarEvent(events,++i);
          }
        e2=getVarEvent(v,vi);
        while (e2!=null && !e2.hasPrincipalClef())
          {
            mt2.add(e2.getmusictime());
            e2=getVarEvent(v,++vi);
          }

        /* clef in one but not the other? */
        if ((e1==null)!=(e2==null))
          return true;

        /* no clef in either? */
        if (e1==null)
          return false;

        /* same clefs? */
        if (e1.getClefSet().contradicts(e2.getClefSet(),false,null))
          return true;

        /* different positioning? */
        if (!mt1.equals(mt2))
          return true;

        if (e1!=null)
          i++;
        if (e2!=null)
          vi++;
      }
    while (e1!=null || e2!=null);

    return false;
  }

  public boolean hasVariantMensSign(EventListData v,int vi)
  {
    int        i=0;
    Event      e1=null,e2=null;
    Proportion mt1=new Proportion(0,1),mt2=new Proportion(0,1);

    do
      {
        /* find position of next mensuration change in each list */
        e1=getVarEvent(events,i);
        while (e1!=null && !e1.hasEventType(Event.EVENT_MENS))
          {
            mt1.add(e1.getmusictime());
            e1=getVarEvent(events,++i);
          }
        e2=getVarEvent(v,vi);
        while (e2!=null && !e2.hasEventType(Event.EVENT_MENS))
          {
            mt2.add(e2.getmusictime());
            e2=getVarEvent(v,++vi);
          }

        /* mensuration in one but not the other? */
        if ((e1==null)!=(e2==null))
          return true;

        /* no mensuration in either? */
        if (e1==null)
          return false;

        /* same mensurations and signs? */
        MensEvent me1=(MensEvent)(e1.getFirstEventOfType(Event.EVENT_MENS)),
                  me2=(MensEvent)(e2.getFirstEventOfType(Event.EVENT_MENS));
        if (!me1.getMensInfo().equals(me2.getMensInfo()) ||
            !me1.signEquals(me2))
          return true;

        /* different positioning? */
        if (!mt1.equals(mt2))
          return true;

        if (e1!=null)
          i++;
        if (e2!=null)
          vi++;
      }
    while (e1!=null || e2!=null);

    return false;
  }

  public boolean hasVariantLigature(EventListData v,int vi)
  {
    int        i=0;
    Event      e1=null,e2=null;
    Proportion mt1=new Proportion(0,1),mt2=new Proportion(0,1);

    do
      {
        NoteEvent lne1=null,lne2=null;

        /* find position of next ligature in each list */
        e1=getVarEvent(events,i);
        while (e1!=null && lne1==null)
          {
            lne1=(NoteEvent)(e1.getFirstEventOfType(Event.EVENT_NOTE));
            if (lne1!=null && !lne1.isligated())
              lne1=null;
            if (lne1==null)
              {
                mt1.add(e1.getmusictime());
                e1=getVarEvent(events,++i);
              }
          }
        e2=getVarEvent(v,vi);
        while (e2!=null && lne2==null)
          {
            lne2=(NoteEvent)(e2.getFirstEventOfType(Event.EVENT_NOTE));
            if (lne2!=null && !lne2.isligated())
              lne2=null;
            if (lne2==null)
              {
                mt2.add(e2.getmusictime());
                e2=getVarEvent(v,++vi);
              }
          }

        /* ligature in one but not the other? */
        if ((e1==null)!=(e2==null))
          return true;

        /* no ligature in either? */
        if (e1==null)
          return false;

        /* different positioning? */
        if (!mt1.equals(mt2))
          return true;

        /* now check that entire ligature matches */
        boolean doneLigCheck=false;
        while (!doneLigCheck)
          {
            /* same ligature type at each note? */
            if (lne1.getnotetype()!=lne2.getnotetype() ||
                lne1.getligtype()!=lne2.getligtype())
              return true;

            /* yes, now get next note of ligature */
            if (lne1.getligtype()==NoteEvent.LIG_NONE)
              doneLigCheck=true;
            else
              {
                mt1.add(e1.getmusictime());
                mt2.add(e2.getmusictime());
                e1=getVarEvent(events,++i);
                while (e1!=null && !e1.hasEventType(Event.EVENT_NOTE))
                  {
                    mt1.add(e1.getmusictime());
                    e1=getVarEvent(events,++i);
                  }
                e2=getVarEvent(v,++vi);
                while (e2!=null && !e2.hasEventType(Event.EVENT_NOTE))
                  {
                    mt2.add(e2.getmusictime());
                    e2=getVarEvent(v,++vi);
                  }
                lne1=e1==null ? null : (NoteEvent)(e1.getFirstEventOfType(Event.EVENT_NOTE));
                lne2=e2==null ? null : (NoteEvent)(e2.getFirstEventOfType(Event.EVENT_NOTE));
                if ((e1==null)!=(e2==null))
                  return true;
                if (e1==null)
                  return false;
              }
          }

        if (e1!=null)
          i++;
        if (e2!=null)
          vi++;
      }
    while (e1!=null || e2!=null);

    return false;
  }

  public boolean hasVariantLineEnd(EventListData v,int vi)
  {
    int        i=0;
    Event      e1=null,e2=null;
    Proportion mt1=new Proportion(0,1),mt2=new Proportion(0,1);
    do
      {
        /* find position of next line end in each list */
        e1=getVarEvent(events,i);
        while (e1!=null && e1.geteventtype()!=Event.EVENT_LINEEND)
          {
            mt1.add(e1.getmusictime());
            e1=getVarEvent(events,++i);
          }
        e2=getVarEvent(v,vi);
        while (e2!=null && e2.geteventtype()!=Event.EVENT_LINEEND)
          {
            mt2.add(e2.getmusictime());
            e2=getVarEvent(v,++vi);
          }

        /* differing position of line ends? */
        if (e1!=null && e2!=null &&
            !mt1.equals(mt2))
          return true;

        /* one has line end and the other doesn't? */
        if ((e1==null)!=(e2==null))
          return true;

        if (e1!=null)
          i++;
        if (e2!=null)
          vi++;
      }
    while (e1!=null && e2!=null);

    /* at least 1 list is finished; check that the other doesn't have any
       further line ends */
    e1=getVarEvent(events,i);
    e2=getVarEvent(v,vi);
    if (e1==null)
      if (e2==null)
        return false;
      else
        while (e2!=null)
          {
            if (e2.geteventtype()==Event.EVENT_LINEEND)
              return true;
            e2=getVarEvent(v,++vi);
          }
    else
      while (e1!=null)
        {
          if (e1.geteventtype()==Event.EVENT_LINEEND)
            return true;
          e1=getVarEvent(events,++i);
        }

    return false;
  }

  public boolean hasVariantColoration(EventListData v,int vi)
  {
    int        i=0;
    Event      e1=null,e2=null;
    Proportion mt1=new Proportion(0,1),mt2=new Proportion(0,1);

    e1=getVarEvent(events,i);
    e2=getVarEvent(v,vi);

    /* is either list empty? if so, no coloration variant */
    if (e1==null || e2==null)
      return false;

    do
      {
        Proportion nextE1T=Proportion.sum(mt1,e1.getmusictime());
        while (nextE1T.greaterThan(mt2))
          {
            if (mt2.equals(mt1) && e2.hasVariantColoration(e1))
              return true;
            mt2.add(e2.getmusictime());
            e2=getVarEvent(v,++vi);
            if (e2==null)
              return false;
          }
        Proportion nextE2T=Proportion.sum(mt2,e2.getmusictime());
        while (nextE2T.greaterThan(mt1))
          {
            if (mt2.equals(mt1) && e2.hasVariantColoration(e1))
              return true;
            mt1.add(e1.getmusictime());
            e1=getVarEvent(events,++i);
            if (e1==null)
              return false;
          }

        if (mt1.equals(mt2) && nextE1T.equals(nextE2T))
          {
            if (e2.hasVariantColoration(e1))
              return true;
            mt1.add(e1.getmusictime());
            e1=getVarEvent(events,++i);
            mt2.add(e2.getmusictime());
            e2=getVarEvent(v,++vi);
          }
      }
    while (e1!=null && e2!=null);

    /* at least 1 list is finished; no coloration variant */
    return false;
  }

  Event getVarEvent(EventListData el,int i)
  {
    if (i>=el.getNumEvents())
      return null;
    Event e=el.getEvent(i);
    if (e.geteventtype()==Event.EVENT_VARIANTDATA_END)
      return null;
    return e;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Event getEvent(int i)
  {
    return events.getEvent(i);
  }

  public EventListData getEvents()
  {
    return events;
  }

  public int getEventIndex()
  {
    return eventIndex;
  }

  public Proportion getLength()
  {
    return length;
  }

  public int getNumEvents()
  {
    return events.getNumEvents();
  }

  public int getSectionNum()
  {
    return sectionNum;
  }

  public VariantVersionData getVersion(int vi)
  {
    return versions.get(vi);
  }

  public ArrayList<VariantVersionData> getVersions()
  {
    return versions;
  }

  public int getVoiceNum()
  {
    return voiceNum;
  }

  public boolean includesVersion(VariantVersionData v)
  {
    return versions.contains(v);
  }

  public boolean isError()
  {
    return error;
  }

/*------------------------------------------------------------------------
Methods: void add*()
Purpose: Routines to add elements (events, event lists, versions)
Parameters:
  Input:  new elements, indices
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addEvent(Event e)
  {
    addEvent(events.getNumEvents(),e);
  }

  public void addEvent(int i,Event e)
  {
    e.setVariantReading(this);
    events.addEvent(i,e);
    length.add(e.getmusictime());
  }

  public void addEventList(EventListData el)
  {
    int numEvents=el.getNumEvents();
    for (int ei=0; ei<numEvents; ei++)
      addEvent(el.getEvent(ei).createCopy());
  }

  public void addEventList(VariantReading other)
  {
    for (int i=0; i<other.getNumEvents(); i++)
      addEvent(other.getEvent(i).createCopy());
  }

  public void addVersion(VariantVersionData v)
  {
    if (!includesVersion(v))
      versions.add(v);
  }

/*------------------------------------------------------------------------
Methods: void delete*()
Purpose: Routines to delete elements (events, event lists, versions)
Parameters:
  Input:  indices/elements to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteEvent(int i)
  {
    deleteEvent(events.getEvent(i));
  }

  public void deleteEvent(Event e)
  {
    events.deleteEvent(e);
    length.subtract(e.getmusictime());
  }

  public void deleteVersion(VariantVersionData v)
  {
    versions.remove(v);
  }

  /* remove one version into a new reading */
  public VariantReading separateVersion(VariantVersionData versionToSeparate)
  {
    VariantReading newReading=new VariantReading(sectionNum,voiceNum,eventIndex);
    newReading.error=this.error;
    newReading.addVersion(versionToSeparate);

    /* copy events */
    for (int i=0; i<getNumEvents(); i++)
      newReading.addEvent(getEvent(i).createCopy());

    deleteVersion(versionToSeparate);

    return newReading;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setError(boolean error)
  {
    this.error=error;
  }

/*------------------------------------------------------------------------
Method:  String toString()
Purpose: Convert to string
Parameters:
  Input:  -
  Output: -
  Return: string representation of structure
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("Variant events:");
    for (int i=0; i<getNumEvents(); i++)
      getEvent(i).prettyprint();
    System.out.println("End variant");
  }

  public String toString()
  {
    String ret="VR: ";
    for (VariantVersionData vvd : getVersions())
      ret+=vvd.getID()+" ";

    return ret;
  }
}
