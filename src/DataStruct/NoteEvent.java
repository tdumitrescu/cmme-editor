/*----------------------------------------------------------------------*/
/*

        Module          : NoteEvent.java

        Package         : DataStruct

        Classes Included: NoteEvent

        Purpose         : Note event type

        Programmer      : Ted Dumitrescu

        Date Started    : 1/99

        Updates         :
4/99:     cleaned up, consolidated with Gfx code
4/24/99:  added note options system (NoteOpt)
3/31/04:  added ledger line display
3/18/05:  incorporated data from classes NoteType and NoteOpt (no longer need
          to be separate since Parse is gone)
9/14/05:  added complete coloration support
12/22/05: added modern accidental support
2/13/06:  added modern text support
2/24/06:  switched timekeeping to minim-based system (1/1 = 1 minim)
3/15/06:  added full-barline stem
4/7/06:   added basic half-coloration support
7/8/08:   replaced ModernAccidental info with integer pitchOffset
8/23/08:  added tie data
3/12/09:  added modern note types, support for arbitrary number of flags

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

/*------------------------------------------------------------------------
Class:   NoteEvent
Extends: Event
Purpose: Data/routines for note events
------------------------------------------------------------------------*/

public class NoteEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int          NT_Semifusa=  0,
                                   NT_Fusa=      1,
                                   NT_Semiminima=2,
                                   NT_Minima=    3,
                                   NT_Semibrevis=4,
                                   NT_Brevis=    5,
                                   NT_Longa=     6,
                                   NT_Maxima=    7,

                                   NT_Flagged=    8,
                                   NT_Quarter=    9,
                                   NT_Half=       10,
                                   NT_Whole=      11,
                                   NT_DoubleWhole=12,
                                   NT_ModernChant=13;

  public static final String[]     NoteTypeNames=new String[]
                                     {
                                       "Semifusa","Fusa",
                                       "Semiminima","Minima",
                                       "Semibrevis","Brevis",
                                       "Longa","Maxima",

                                       "Modern Flagged Note",
                                       "Quarter note","Half note",
                                       "Whole note","Double whole note",

                                       "UNKNOWN"
                                     };
  public static final Proportion[] DefaultLengths=new Proportion[]
                                     {
                                       new Proportion(1,8),
                                       new Proportion(1,4),
                                       new Proportion(1,2),
                                       new Proportion(1,1),
                                       new Proportion(2,1),
                                       new Proportion(4,1),
                                       new Proportion(8,1),
                                       new Proportion(16,1)
                                     };

  public static final int          LIG_NONE=   0,
                                   LIG_RECTA=  1,
                                   LIG_OBLIQUA=2;
  public static final String[]     LigTypeNames=new String[]
                                     {
                                       "XXX","Recta","Obliqua"
                                     };

  public static final int          TIE_NONE= 0,
                                   TIE_OVER= 1,
                                   TIE_UNDER=2;
  public static final String[]     TieTypeNames=new String[]
                                     {
                                       "XXX","Over","Under"
                                     };

  public static final int          STEM_NONE=   -1,
                                   STEM_UP=     0,
                                   STEM_DOWN=   1,
                                   STEM_LEFT=   2,
                                   STEM_RIGHT=  3,
                                   STEM_BARLINE=4;
  public static final String[]     StemDirs=new String[]
                                     {
                                       "Up","Down","Left","Right","Barline"
                                     };

  public static final int          NOTEHEADSTYLE_BREVE=          0,
                                   NOTEHEADSTYLE_SEMIBREVE=      1,
                                   NOTEHEADSTYLE_FULLBREVE=      2,
                                   NOTEHEADSTYLE_FULLSEMIBREVE=  3,
                                   NOTEHEADSTYLE_MAXIMA=         4,
                                   NOTEHEADSTYLE_FULLMAXIMA=     5,
                                   NOTEHEADSTYLE_FULLVOID_BREVE= 6,
                                   NOTEHEADSTYLE_VOIDFULL_BREVE= 7,
                                   NOTEHEADSTYLE_FULLVOID_MAXIMA=8,
                                   NOTEHEADSTYLE_VOIDFULL_MAXIMA=9,

                                   NOTEHEADSTYLE_MODERN_BREVE=         30,
                                   NOTEHEADSTYLE_MODERN_SEMIBREVE=     31,
                                   NOTEHEADSTYLE_MODERN_MINIM_UP=      32,
                                   NOTEHEADSTYLE_MODERN_CROTCHET_UP=   33,
                                   NOTEHEADSTYLE_MODERN_MINIM_DOWN=    34,
                                   NOTEHEADSTYLE_MODERN_CROTCHET_DOWN= 35,
                                   NOTEHEADSTYLE_MODERN_STEMLESS_CHANT=36;

  public static final int          HALFCOLORATION_NONE=            0,
                                   HALFCOLORATION_PRIMARYSECONDARY=1,
                                   HALFCOLORATION_SECONDARYPRIMARY=2;

/*----------------------------------------------------------------------*/
/* Instance variables */

  int              notetype;
  Proportion       length;
  Pitch            pitch;
  ModernAccidental pitchOffset;

  int              noteheadstyle,
                   halfColoration,
                   stemdir,stemside,
                   ligstatus,
                   tieType,
                   numFlags;
  boolean          wordEnd,
                   modernDot,
                   displayAccidental;
  String           modernText;
  boolean          modernTextEditorial;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  int strtoNT(String nt)
Purpose: Convert string to note type number
Parameters:
  Input:  String nt - string to convert
  Output: -
  Return: note type number
------------------------------------------------------------------------*/

  public static int strtoNT(String nt)
  {
    int i;

    for (i=0; i<NoteTypeNames.length; i++)
      if (nt.equals(NoteTypeNames[i]))
        return i;
    if (i==NoteTypeNames.length)
      i=NoteTypeNames.length-1;

    return i;
  }

  public static int lenToNT(Proportion len)
  {
    int nt=0;

    for (Proportion dl : DefaultLengths)
      if (dl.greaterThan(len))
        return nt>0 ? nt-1 : 0;
      else nt++;

    return DefaultLengths.length-1;
  }

/*------------------------------------------------------------------------
Method:  int strtoStemDir(String s)
Purpose: Convert string to stem direction number
Parameters:
  Input:  String s - string to convert
  Output: -
  Return: stem dir number
------------------------------------------------------------------------*/

  public static int strtoStemDir(String s)
  {
    for (int i=0; i<StemDirs.length; i++)
      if (s.equals(StemDirs[i]))
        return i;
    return -1;
  }

/*------------------------------------------------------------------------
Method:  Proportion getTypeLength(int nt,Mensuration mensinfo)
Purpose: Calculate length of note type (perfect and unaltered when applicable)
Parameters:
  Input:  int nt               - note type
          Mensuration mensinfo - mensuration information
  Output: -
  Return: length as proportion
------------------------------------------------------------------------*/

  public static Proportion getTypeLength(int nt,Mensuration mensinfo)
  {
    int i1=0,i2=0;

    switch (nt)
      {
        case NT_Semifusa:
          i1=1; i2=8;
          break;
        case NT_Fusa:
          i1=1; i2=4;
          break;
        case NT_Semiminima:
          i1=1; i2=2;
          break;
        case NT_Minima:
          i1=1; i2=1;
          break;
        case NT_Semibrevis:
          i1=mensinfo.prolatio; i2=1;
          break;
        case NT_Brevis:
          i1=mensinfo.tempus*mensinfo.prolatio; i2=1;
          break;
        case NT_Longa:
          i1=mensinfo.modus_minor*mensinfo.tempus*mensinfo.prolatio; i2=1;
          break;
        case NT_Maxima:
          i1=mensinfo.modus_maior*mensinfo.modus_minor*mensinfo.tempus*mensinfo.prolatio; i2=1;
          break;
      }

    return new Proportion(i1,i2);
  }

/*------------------------------------------------------------------------
Method:  int oppositefill(int nhs)
Purpose: Calculate notehead with opposite fill type (void/full)
Parameters:
  Input:  int nhs - notehead style
  Output: -
  Return: opposite-fill notehead style
------------------------------------------------------------------------*/

  public static int oppositefill(int nhs)
  {
    switch (nhs)
      {
        case NOTEHEADSTYLE_BREVE:
          return NOTEHEADSTYLE_FULLBREVE;
        case NOTEHEADSTYLE_SEMIBREVE:
          return NOTEHEADSTYLE_FULLSEMIBREVE;
        case NOTEHEADSTYLE_MAXIMA:
          return NOTEHEADSTYLE_FULLMAXIMA;
        case NOTEHEADSTYLE_FULLBREVE:
          return NOTEHEADSTYLE_BREVE;
        case NOTEHEADSTYLE_FULLSEMIBREVE:
          return NOTEHEADSTYLE_SEMIBREVE;
        case NOTEHEADSTYLE_FULLMAXIMA:
          return NOTEHEADSTYLE_MAXIMA;
        case NOTEHEADSTYLE_FULLVOID_BREVE:
          return NOTEHEADSTYLE_VOIDFULL_BREVE;
        case NOTEHEADSTYLE_VOIDFULL_BREVE:
          return NOTEHEADSTYLE_FULLVOID_BREVE;
        case NOTEHEADSTYLE_FULLVOID_MAXIMA:
          return NOTEHEADSTYLE_VOIDFULL_MAXIMA;
        case NOTEHEADSTYLE_VOIDFULL_MAXIMA:
          return NOTEHEADSTYLE_FULLVOID_MAXIMA;
      }

    return -1;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: NoteEvent(String nt,Proportion len,Pitch p,ModernAccidental po,
                       int l,boolean c,int sd,int ss,int f,String mt,boolean we,
                       int tieType)
Purpose:     Creates note event
Parameters:
  Input:  String nt           - note type
          Proportion len      - length of note (in music time)
          Pitch p             - note pitch
          ModernAccidental po - modern pitch offset for editorial pitch realization
          int l               - ligation type
          boolean c           - colored?
          int hc              - half-coloration info
          int sd              - stem direction (-1 for none)
          int ss              - stem side
          int f               - number of flags
          String mt           - modern text syllable
          boolean we          - is this syllable a word end?
          int tieType         - tie data
  Output: -
------------------------------------------------------------------------*/

  public NoteEvent(int nt,Proportion len,Pitch p,ModernAccidental po,
                   int l,boolean c,int hc,int sd,int ss,int f,String mt,boolean we,boolean modernTextEditorial,
                   int tieType)
  {
    super();
    eventtype=EVENT_NOTE;
    notetype=nt;
    length=musictime=len==null ? null : new Proportion(len);
    pitch=p;
    pitchOffset=po;
    stemdir=STEM_UP;
    stemside=STEM_NONE;
    ligstatus=l;
    colored=c;
    halfColoration=hc;
    numFlags=f;
    if (sd!=STEM_NONE)
      {
        stemdir=sd;
        stemside=ss;
      }
    selectNoteheadStyle();
    modernText=mt;
    wordEnd=we;
    this.modernTextEditorial=modernTextEditorial;
    this.tieType=tieType;
    this.modernDot=false;
    this.displayAccidental=true;
  }

  public NoteEvent(String nt,Proportion len,Pitch p,ModernAccidental po,
                   int l,boolean c,int hc,int sd,int ss,int f,String mt,boolean we,boolean mte,
                   int tt)
  {
    this(strtoNT(nt),len,p,po,l,c,hc,sd,ss,f,mt,we,mte,tt);
  }

  public NoteEvent(String nt,Proportion len,Pitch p,ModernAccidental po,
                   int l,boolean c,int hc,int sd,int ss,int f,String mt)
  {
    this(nt,len,p,po,l,c,hc,sd,ss,f,mt,false,false,TIE_NONE);
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
    Event e=new NoteEvent(this.notetype,this.length,new Pitch(this.pitch),
                          new ModernAccidental(this.pitchOffset),
                          this.ligstatus,this.colored,this.halfColoration,
                          this.stemdir,this.stemside,this.numFlags,
                          this.modernText==null ? null : new String(this.modernText),
                          this.wordEnd,this.modernTextEditorial,this.tieType);
    e.copyEventAttributes(this);
    return e;
  }

  public void copyEventAttributes(Event other)
  {
    super.copyEventAttributes(other);

    NoteEvent ne=(NoteEvent)other;
    this.modernDot=ne.modernDot;
    this.displayAccidental=ne.displayAccidental;
  }

/*------------------------------------------------------------------------
Method:    LinkedList<Event> makeModernNoteShapes()
Overrides: Event.makeModernNoteShapes
Purpose:   Make event (copy) in modern notation
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
    NoteEvent ne=(NoteEvent)(this.createCopy());

    if (this.length==null) /* chant */
      {
        ne.notetype=NT_ModernChant;
//        ne.colored=false; ne.halfColoration=HALFCOLORATION_NONE;
//        ne.colorscheme=Coloration.DEFAULT_COLORATION;
        ne.selectNoteheadStyle();
        el.add(ne);
        return el;
      }

    if (useTies)
      {
        timePos=new Proportion(timePos);
        Proportion noPropTimePos=new Proportion(timePos);
        measurePos=new Proportion(measurePos);
        Mensuration mensInfo=this.getBaseMensInfo();//Mensuration.DEFAULT_MENSURATION;
//if (!mensInfo.tempoChange.equals(Proportion.EQUALITY))
//System.out.print("TC="+mensInfo.tempoChange+"|TP="+timeProp+"|");
        Proportion  noteTime=Proportion.quotient(this.length,timeProp),
                    measureLength=new Proportion(measureMinims*measureProp.i2,measureProp.i1),
                    endMeasurePos=Proportion.sum(measurePos,measureLength),
                    noPropEndMeasurePos=new Proportion(measurePos.i1+measureMinims,measurePos.i2);

//System.out.println("------MMNS----- timeprop="+timeProp+
//                   " measureprop="+measureProp+" mi.tempochange="+mensInfo.tempoChange);

if (!measureProp.equals(mensInfo.tempoChange))
{
  noPropEndMeasurePos=Proportion.sum(measurePos,
    new Proportion(measureMinims*measureProp.i2*mensInfo.tempoChange.i1,measureProp.i1*mensInfo.tempoChange.i2));
//System.out.println("XXXXXXXX-NPEMP="+noPropEndMeasurePos+"-XXXXXXXXX");
}

//if (!timeProp.equals(Proportion.EQUALITY) || !mensInfo.tempoChange.equals(Proportion.EQUALITY))
//System.out.println("------time="+timePos+" endmeasure="+endMeasurePos+"||");
        while (noteTime.i1>0)
          {
/*if (!timeProp.equals(Proportion.EQUALITY) || !mensInfo.tempoChange.equals(Proportion.EQUALITY))
System.out.print("  |nt="+noteTime+" nptlim="+noPropTimeLeftInMeasure+" ");
if (!timeProp.equals(Proportion.EQUALITY) || !mensInfo.tempoChange.equals(Proportion.EQUALITY))
System.out.print("nl1="+ne.getLength()+" ");*/

            if (ne.getLength().i1==0)
              break;

            Proportion noPropTimeLeftInMeasure=Proportion.product(
                         Proportion.difference(endMeasurePos,timePos),
                         mensInfo.tempoChange);

/*System.out.println("  1nt="+noteTime+" tp="+timePos+" emp="+endMeasurePos);
if (!measureProp.equals(mensInfo.tempoChange))
System.out.println("XXXXXXXX-NPTLIM="+noPropTimeLeftInMeasure);*/
            /* calcModernNoteTypeAndLength should get measurePos and endMeasurePos
               as if no proportions are applied... */
            ne.calcModernNoteTypeAndLength(noteTime,timeProp,
                                           noPropTimeLeftInMeasure,
                                           Mensuration.DEFAULT_MENSURATION);
//            if (Proportion.quotient(ne.getLength(),timeProp).greaterThan(noteTime))
//              ne.setLength(Proportion.product(new Proportion(noteTime),timeProp));

//            if (ne.getLength().greaterThan(noteTime))
//              ne.setLength(new Proportion(noteTime));
//if (!timeProp.equals(Proportion.EQUALITY) || !mensInfo.tempoChange.equals(Proportion.EQUALITY))
//System.out.print("nl2="+ne.getLength()+" ");
            el.add(ne);

//            noteTime.subtract(Proportion.quotient(ne.getLength(),totalProp));
//            timePos.add(Proportion.quotient(ne.getLength(),totalProp));
//            timePos.add(ne.getLength());
            timePos.add(Proportion.quotient(ne.getLength(),mensInfo.tempoChange));
//if (!timeProp.equals(Proportion.EQUALITY) || !mensInfo.tempoChange.equals(Proportion.EQUALITY))
//System.out.print(" nptp="+noPropTimePos+" ");
            noPropTimePos.add(ne.getLength());
            noteTime.subtract(ne.getLength());
//            timePos.add(ne.getLength());
//if (!timeProp.equals(Proportion.EQUALITY) || !mensInfo.tempoChange.equals(Proportion.EQUALITY))
//System.out.print("TP="+timePos+" EMP="+endMeasurePos+" ");
            if (timePos.greaterThanOrEqualTo(endMeasurePos))
              {
                measurePos.i1+=measureMinims;
                endMeasurePos.add(measureLength);
/*                noPropTimeLeftInMeasure=Proportion.product(
                  new Proportion(measureMinims,1),
                  mensInfo.tempoChange);*/
              }

            ne=ne.makeNextTiedNote();
//if (!timeProp.equals(Proportion.EQUALITY) || !mensInfo.tempoChange.equals(Proportion.EQUALITY))
//System.out.println();
//System.out.println("  2nt="+noteTime+" tp="+timePos+" emp="+endMeasurePos);
          }
      }
    else
      {
        int newNoteType=NT_DoubleWhole;
        switch (ne.notetype)
          {
            case NT_Semibrevis:
              newNoteType=NT_Whole;
              break;
            case NT_Minima:
              newNoteType=NT_Half;
              break;
            case NT_Semiminima:
              newNoteType=NT_Quarter;
              break;
            case NT_Fusa:
            case NT_Semifusa:
              newNoteType=NT_Flagged;
              break;
          }
        ne.notetype=newNoteType;

        ne.colored=false; ne.halfColoration=HALFCOLORATION_NONE;
        ne.colorscheme=Coloration.DEFAULT_COLORATION;
        ne.selectNoteheadStyle();

        el.add(ne);
      }

    return el;
  }

  NoteEvent makeNextTiedNote()
  {
    NoteEvent ne=(NoteEvent)(this.createCopy());
    ne.setModernText(null);
    ne.setEdCommentary(null);
    ne.setDisplayAccidental(false);
    ne.setSignum(null);

    return ne;
  }

  /* should not take proportions into account!!! */
  void calcModernNoteTypeAndLength(
    Proportion noteTime,Proportion timeProp,
    Proportion timeLeftInMeasure,
    Mensuration m)
  {
    Mensuration mensInfo=this.getBaseMensInfo();//Mensuration.DEFAULT_MENSURATION;
/*    Proportion timeInMeasure=Proportion.quotient(
      Proportion.quotient(Proportion.difference(endMeasurePos,timePos),timeProp),
      mensInfo.tempoChange),*/
//    Proportion timeLeftInMeasure=Proportion.difference(endMeasurePos,timePos),
//               maxTime=Proportion.min(Proportion.product(noteTime,timeProp),timeLeftInMeasure);
    Proportion maxTime=Proportion.min(noteTime,timeLeftInMeasure);
//System.out.print(" MaxT="+maxTime+" ");
/*if (!mensInfo.tempoChange.equals(Proportion.EQUALITY))
System.out.print(" TLIM="+timeLeftInMeasure);*/
    int nt=NoteEvent.NT_Maxima;
    while (nt>=NoteEvent.NT_Fusa && NoteEvent.getTypeLength(nt,m).greaterThan(maxTime))
      nt--;
    int newNoteType=NT_DoubleWhole;
    switch (nt)
      {
        case NT_Semibrevis:
          newNoteType=NT_Whole;
          break;
        case NT_Minima:
          newNoteType=NT_Half;
          break;
        case NT_Semiminima:
          newNoteType=NT_Quarter;
          break;
        case NT_Fusa:
          this.numFlags=1;
          newNoteType=NT_Flagged;
          break;
        case NT_Semifusa:
          this.numFlags=2;
          newNoteType=NT_Flagged;
          break;
      }
    this.notetype=newNoteType;

    this.colored=false; this.halfColoration=HALFCOLORATION_NONE;
    this.colorscheme=Coloration.DEFAULT_COLORATION;
    this.selectNoteheadStyle();

//    this.setLength(NoteEvent.getTypeLength(this.notetype,m));
    this.setLength(maxTime);
//System.out.print("L="+this.getLength());
    if (this.getLength().lessThan(noteTime))
      this.tieType=TIE_UNDER;
    else
      this.tieType=TIE_NONE;
    if (this.getLength().toDouble()>=NoteEvent.getTypeLength(nt,m).toDouble()*1.5)
      this.modernDot=true;
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
    NoteEvent otherNE=(NoteEvent)other;

    return this.notetype==otherNE.notetype &&
           this.length.equals(otherNE.length) &&
           this.pitch.equals(otherNE.pitch) &&
           this.pitchOffset.equals(otherNE.pitchOffset) &&
           this.ligstatus==otherNE.ligstatus &&
           this.colored==otherNE.colored &&
           this.halfColoration==otherNE.halfColoration &&
           this.stemdir==otherNE.stemdir &&
           this.stemside==otherNE.stemside &&
           this.numFlags==otherNE.numFlags &&
           this.modernTextEquals(otherNE) &&
           this.wordEnd==otherNE.wordEnd &&
           this.tieType==otherNE.tieType;
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
    if (other.geteventtype()==Event.EVENT_MULTIEVENT)
      return other.notePitchMatches(this);
    if (other.geteventtype()!=Event.EVENT_NOTE)
      return false;

    /* note vs. note */
    return this.getPitch().equals(other.getPitch());
  }

/*  boolean accidentalInfoEquals(NoteEvent otherNE)
  {
    if (this.accidentalInfo==null)
      return otherNE.accidentalInfo==null;
    return this.accidentalInfo.equals(otherNE.accidentalInfo);
  }*/

  boolean modernTextEquals(NoteEvent otherNE)
  {
    if (this.modernText==null)
      return otherNE.modernText==null;
    return this.modernText.equals(otherNE.modernText);
  }

/*------------------------------------------------------------------------
Method:  void selectNoteheadStyle()
Purpose: Set notehead style based on note type and coloration scheme
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void selectNoteheadStyle()
  {
    if (modernNoteType())
      {
        switch(this.notetype)
          {
            case NT_ModernChant:
              this.noteheadstyle=NOTEHEADSTYLE_MODERN_STEMLESS_CHANT;
              break;
            case NT_Flagged:
            case NT_Quarter:
              this.noteheadstyle=NOTEHEADSTYLE_MODERN_CROTCHET_UP;
              break;
            case NT_Half:
              this.noteheadstyle=NOTEHEADSTYLE_MODERN_MINIM_UP;
              break;
            case NT_Whole:
              this.noteheadstyle=NOTEHEADSTYLE_MODERN_SEMIBREVE;
              break;
            default:
              this.noteheadstyle=NOTEHEADSTYLE_MODERN_BREVE;
              break;
          }
        return;
      }

    Coloration c=colorscheme;

    if (halfColoration!=HALFCOLORATION_NONE) /* half-colored notehead */
      switch(notetype)
        {
          case NT_Brevis:
          case NT_Longa:
            if (halfColoration==HALFCOLORATION_PRIMARYSECONDARY)
              noteheadstyle=c.primaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLVOID_BREVE : NOTEHEADSTYLE_VOIDFULL_BREVE;
            else
              noteheadstyle=c.primaryFill==Coloration.FULL ? NOTEHEADSTYLE_VOIDFULL_BREVE : NOTEHEADSTYLE_FULLVOID_BREVE;
            break;
          case NT_Maxima:
            if (halfColoration==HALFCOLORATION_PRIMARYSECONDARY)
              noteheadstyle=c.primaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLVOID_MAXIMA : NOTEHEADSTYLE_VOIDFULL_MAXIMA;
            else
              noteheadstyle=c.primaryFill==Coloration.FULL ? NOTEHEADSTYLE_VOIDFULL_MAXIMA : NOTEHEADSTYLE_FULLVOID_MAXIMA;
            break;
          default:
            System.err.println("Error: attempting to assign half-coloration to an invalid note type");
        }

    else if (colored) /* colored notes always use Secondary coloration */
      switch(notetype)
        {
          case NT_Semifusa:
          case NT_Fusa:
          case NT_Semiminima:
          case NT_Minima:
          case NT_Semibrevis:
            noteheadstyle=c.secondaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLSEMIBREVE : NOTEHEADSTYLE_SEMIBREVE;
            break;
          case NT_Maxima:
            noteheadstyle=c.secondaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLMAXIMA : NOTEHEADSTYLE_MAXIMA;
            break;
          default:
            noteheadstyle=c.secondaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLBREVE : NOTEHEADSTYLE_BREVE;
            break;
        }
    else
      switch(notetype)
        {
          case NT_Semifusa:
          case NT_Fusa:
          case NT_Semiminima:
            noteheadstyle=c.secondaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLSEMIBREVE : NOTEHEADSTYLE_SEMIBREVE;
            break;
          case NT_Minima:
          case NT_Semibrevis:
            noteheadstyle=c.primaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLSEMIBREVE : NOTEHEADSTYLE_SEMIBREVE;
            break;
          case NT_Maxima:
            noteheadstyle=c.primaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLMAXIMA : NOTEHEADSTYLE_MAXIMA;
            break;
          default:
            noteheadstyle=c.primaryFill==Coloration.FULL ? NOTEHEADSTYLE_FULLBREVE : NOTEHEADSTYLE_BREVE;
            break;
        }

    if (notetype==NT_Semiminima && numFlags>0 && !colored)
      noteheadstyle=oppositefill(noteheadstyle);
  }

/*------------------------------------------------------------------------
Method:  boolean canBePerfect(Mensuration mensinfo)
Purpose: Check whether note can be perfected/imperfected under a given
         mensuration (only checks pars propinqua)
Parameters:
  Input:  Mensuration mensinfo - mensuration information
  Output: -
  Return: whether note can be perfected/imperfected
------------------------------------------------------------------------*/

  public boolean canBePerfect(Mensuration mensinfo)
  {
    switch (notetype)
      {
        case NT_Semibrevis:
          return mensinfo.prolatio==Mensuration.MENS_TERNARY;
        case NT_Brevis:
          return mensinfo.tempus==Mensuration.MENS_TERNARY;
        case NT_Longa:
          return mensinfo.modus_minor==Mensuration.MENS_TERNARY;
        case NT_Maxima:
          return mensinfo.modus_maior==Mensuration.MENS_TERNARY;
      }
    return false;
  }

/*------------------------------------------------------------------------
Method:  boolean canBeAltered(Mensuration mensinfo)
Purpose: Check whether note can be altered under a given mensuration
Parameters:
  Input:  Mensuration mensinfo - mensuration information
  Output: -
  Return: whether note can be altered
------------------------------------------------------------------------*/

  public boolean canBeAltered(Mensuration mensinfo)
  {
    switch (notetype)
      {
        case NT_Minima:
          return mensinfo.prolatio==Mensuration.MENS_TERNARY;
        case NT_Semibrevis:
          return mensinfo.tempus==Mensuration.MENS_TERNARY;
        case NT_Brevis:
          return mensinfo.modus_minor==Mensuration.MENS_TERNARY;
        case NT_Longa:
          return mensinfo.modus_maior==Mensuration.MENS_TERNARY;
      }
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

  public int getnotetype()
  {
    return notetype;
  }

  public int getnoteheadstyle()
  {
    return noteheadstyle;
  }

  public int getHalfColoration()
  {
    return halfColoration;
  }

  public Proportion getLength()
  {
    return length;
  }

  public int getMIDIPitch()
  {
    return pitch.toMIDIPitch()+pitchOffset.pitchOffset;
  }

  public Pitch getPitch()
  {
    return pitch;
  }

  public ModernAccidental getPitchOffset()
  {
    return pitchOffset;
  }

  public boolean isligated()
  {
    return ligstatus!=LIG_NONE;
  }

  public int getligtype()
  {
    return ligstatus;
  }

  public boolean modernNoteType()
  {
    return this.notetype>=NT_Flagged && this.notetype<=NT_DoubleWhole;
  }

  public int getNumFlags()
  {
    return numFlags;
  }

  public boolean isMinorColor()
  {
    if (!isColored())
      return false;
    Mensuration m=getBaseMensInfo();
	Proportion dtl=getTypeLength(notetype,m);
    int diffDenominator=Proportion.difference(dtl,length).i2;
    return !m.ternary(notetype) && length.lessThan(dtl) &&
           (diffDenominator<3 || diffDenominator==4 || diffDenominator==8);
  }

  public boolean hasStem()
  {
    return notetype==NT_Semifusa ||
           notetype==NT_Fusa ||
           notetype==NT_Semiminima ||
           notetype==NT_Minima ||
           notetype==NT_Longa ||
           notetype==NT_Maxima ||

           notetype==NT_Flagged ||
           notetype==NT_Quarter ||
           notetype==NT_Half;
  }

  public boolean hasModernDot()
  {
    return modernDot;
  }

  public int getstemdir()
  {
    return stemdir;
  }

  public int getstemside()
  {
    return stemside;
  }

  public int getTieType()
  {
    return tieType;
  }

  public boolean isflagged()
  {
    return numFlags>0 || notetype==NT_Flagged;
  }

/*  public boolean hasstem()
  {
    return stemside!=-1 ||
           (notetype!=NT_Brevis && notetype!=NT_Semibrevis);
  }*/

  public String getModernText()
  {
    return modernText;
  }

  public boolean isModernTextEditorial()
  {
    return modernTextEditorial;
  }

  public boolean isWordEnd()
  {
    return wordEnd;
  }

  public boolean displayAccidental()
  {
    return this.displayAccidental;
  }

  /* overrides Event methods */
  public int getcolor()
  {
    if ((colored && (numFlags==0 || notetype!=NT_Semiminima)) ||
        notetype==NT_Semifusa ||
        notetype==NT_Fusa ||
        notetype==NT_Semiminima)
      return colorscheme.secondaryColor;
    else
      return colorscheme.primaryColor;
  }

  public int getcolorfill()
  {
    if (noteheadstyle==NOTEHEADSTYLE_SEMIBREVE ||
        noteheadstyle==NOTEHEADSTYLE_BREVE ||
        noteheadstyle==NOTEHEADSTYLE_MAXIMA)
      return Coloration.VOID;
    else
      return Coloration.FULL;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setpitch(Pitch p)
  {
    pitch=p;
  }

  public void modifyPitchOffset(int offset)
  {
    this.pitchOffset.pitchOffset+=offset;
  }

  public void setPitchOffset(int pitchOffset)
  {
    this.pitchOffset.pitchOffset=pitchOffset;
  }

  public void setnotetype(int nt,int f,Mensuration m)
  {
    notetype=nt;
    numFlags=f;
    selectNoteheadStyle();
  }

  public void setLength(Proportion l)
  {
    length=musictime=new Proportion(l);
  }

  public void setstemdir(int sd)
  {
    stemdir=sd;
  }

  public void setstemside(int ss)
  {
    stemside=ss;
  }

  public void setligtype(int lt)
  {
    ligstatus=lt;
  }

  public void setColored(boolean c)
  {
    super.setColored(c);
    selectNoteheadStyle();
  }

  public void setHalfColoration(int hc)
  {
    halfColoration=hc;
    selectNoteheadStyle();
  }

  public void setModernText(String s)
  {
    modernText=s;
  }

  public void setTieType(int newval)
  {
    this.tieType=newval;
  }

  public void setWordEnd(boolean we)
  {
    wordEnd=we;
  }

  public void setModernTextEditorial(boolean modernTextEditorial)
  {
    this.modernTextEditorial=modernTextEditorial;
  }

  public void setDisplayAccidental(boolean displayAccidental)
  {
    this.displayAccidental=displayAccidental;
  }

  /* overrides Event methods */
  public void setcolorparams(Coloration c)
  {
    colorscheme=c;
    selectNoteheadStyle();
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
    System.out.println("    Note: "+NoteTypeNames[notetype]+","+
                       length.i1+"/"+length.i2+","+
                       pitch.toString());
  }
}
