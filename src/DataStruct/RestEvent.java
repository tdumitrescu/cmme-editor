/*----------------------------------------------------------------------*/
/*

        Module          : RestEvent.java

        Package         : DataStruct

        Classes Included: RestEvent

        Purpose         : Rest event type

        Programmer      : Ted Dumitrescu

        Date Started    : 1/99

        Updates         :
4/99:    cleaned up, consolidated with Gfx code
3/15/06: introduced rest-form calculations based on mensuration (for longas
         and maximas)
7/19/06: added default positioning for rests in modern cleffing

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

/*------------------------------------------------------------------------
Class:   RestEvent
Extends: Event
Purpose: Data/routines for rest events
------------------------------------------------------------------------*/

public class RestEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int defaultBottomLine[]=
    {
      3,3,3,3, /* semifusa - minima */
      4,3,2,2  /* semibrevis - maxima */
    };

/*----------------------------------------------------------------------*/
/* Instance variables */

  int        notetype;
  Proportion length;
  int        bottomline,numlines, /* vertical positioning */
             numSets;             /* for maximas: number of vertical lines */

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  int calcNumLines(int nt,Mensuration m)
Purpose: Calculate number of staff lines covered by rest of a given note type
Parameters:
  Input:  int nt        - note type
          Mensuration m - mensuration data for calculation
  Output: -
  Return: number of lines
------------------------------------------------------------------------*/

  public static int calcNumLines(int nt,Mensuration m)
  {
    switch (nt)
      {
        case NoteEvent.NT_Semifusa:
        case NoteEvent.NT_Fusa:
        case NoteEvent.NT_Semiminima:
        case NoteEvent.NT_Minima:
        case NoteEvent.NT_Semibrevis:
          return 0;
        case NoteEvent.NT_Brevis:
          return 1;
        case NoteEvent.NT_Longa:
        case NoteEvent.NT_Maxima:
          return m.modus_minor;
      }
    return 0;
  }

  /* calc largest note type which fits in a given time */
  public static int calcLargestRestType(double time,Mensuration m)
  {
    for (int nt=NoteEvent.NT_Maxima; nt>=NoteEvent.NT_Semifusa; nt--)
      if (NoteEvent.getTypeLength(nt,m).toDouble()<=time)
        return nt;
    return NoteEvent.NT_Semifusa;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RestEvent(String nt,Proportion p,int bl,int nl,int ns)
Purpose:     Creates rest event
Parameters:
  Input:  int nt       - note type
          Proportion p - length of rest (in music time)
          int bl       - bottom staff line of rest
          int nl       - number of lines which rest covers
          int ns       - number of rest items (for maxima)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public RestEvent(int nt,Proportion p,int bl,int nl,int ns)
  {
    super();
    eventtype=EVENT_REST;
    notetype=nt;
    length=musictime=p==null ? null : new Proportion(p);
    bottomline=bl;
    numlines=nl;
    numSets=notetype==NoteEvent.NT_Maxima ? ns : 1;
  }

  public RestEvent(String nt,Proportion p,int bl,int nl,int ns)
  {
    this(NoteEvent.strtoNT(nt),p,bl,nl,ns);
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
    Event e=new RestEvent(this.notetype,this.length,this.bottomline,this.numlines,this.numSets);
    e.copyEventAttributes(this);
    return e;
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
    RestEvent re=(RestEvent)(this.createCopy());
    re.colored=false;
    re.colorscheme=Coloration.DEFAULT_COLORATION;
    re.bottomline=re.numlines=re.numSets=1;

    timePos=new Proportion(timePos);
    Mensuration mensInfo=this.getBaseMensInfo();
    Proportion  restTime=Proportion.quotient(this.length,timeProp),
                measureLength=new Proportion(measureMinims*measureProp.i2,measureProp.i1),
                endMeasurePos=Proportion.sum(measurePos,measureLength),
                noPropEndMeasurePos=new Proportion(measurePos.i1+measureMinims,measurePos.i2);

/*System.out.println("REST MMNS tp="+timePos+" resttime="+restTime+
                   " emp="+endMeasurePos+" timeprop="+timeProp);*/

if (!measureProp.equals(mensInfo.tempoChange))
{
  noPropEndMeasurePos=Proportion.sum(measurePos,
    new Proportion(measureMinims*measureProp.i2*mensInfo.tempoChange.i1,measureProp.i1*mensInfo.tempoChange.i2));
//System.out.println(" NPEMP="+noPropEndMeasurePos);
}

    while (restTime.i1>0)
      {
        Proportion noPropTimeLeftInMeasure=Proportion.product(
                     Proportion.difference(endMeasurePos,timePos),
                     mensInfo.tempoChange);

//System.out.println("  left in m="+noPropTimeLeftInMeasure);
//Proportion.product(Proportion.difference(noPropEndMeasurePos,timePos),measureProp).toDouble());

        re.notetype=RestEvent.calcLargestRestType(
          Math.min(restTime.toDouble(),//Proportion.product(restTime,timeProp).toDouble(),
                   noPropTimeLeftInMeasure.toDouble()),
/*                   Proportion.product(
                     Proportion.difference(noPropEndMeasurePos,timePos),
                     measureProp).toDouble()),*///Proportion.product(Proportion.difference(endMeasurePos,timePos),timeProp).toDouble()),
          mensInfo);
//System.out.println("  rest type="+NoteEvent.NoteTypeNames[re.notetype]);
        re.setLength(NoteEvent.getTypeLength(re.notetype,mensInfo));
        if (Proportion.quotient(re.getLength(),timeProp).greaterThan(restTime))
          re.setLength(Proportion.product(new Proportion(restTime),timeProp));
        el.add(re);

        restTime.subtract(Proportion.quotient(re.getLength(),timeProp));
        timePos.add(Proportion.quotient(re.getLength(),timeProp));
        if (timePos.greaterThanOrEqualTo(endMeasurePos))
          {
            timePos=new Proportion(endMeasurePos);
            endMeasurePos.add(measureLength);
          }

        re=makeNextRest();
//System.out.println("  rt="+restTime+" tp="+timePos+" emp="+endMeasurePos);
      }

/*    int newNoteType=NT_DoubleWhole;
    switch (re.notetype)
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
    ne.notetype=newNoteType;*/

    return el;
  }

  RestEvent makeNextRest()
  {
    RestEvent re=new RestEvent(NoteEvent.NT_Brevis,Proportion.EQUALITY,1,1,1);
    re.copyEventAttributes(this);
    re.setSignum(null);

    return re;
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
    RestEvent otherRE=(RestEvent)other;

    return this.notetype==otherRE.notetype &&
           this.length.equals(otherRE.length) &&
           this.bottomline==otherRE.bottomline &&
           this.numlines==otherRE.numlines &&
           this.numSets==otherRE.numSets;
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

  public int getModNoteType()
  {
    return notetype+7;
  }

  public Proportion getLength()
  {
    return length;
  }

  public int getbottomline(boolean useModernClefs)
  {
    if (useModernClefs)
      return defaultBottomLine[notetype];
    else
      return bottomline;
  }

  public int getbottomline()
  {
    return getbottomline(false);
  }

  public int getnumlines()
  {
    return numlines;
  }

  public int getNumSets()
  {
    return numSets;
  }

  /* overrides Event methods */
  public int getcolor()
  {
    return colored ? colorscheme.secondaryColor : colorscheme.primaryColor;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setbottomline(int bl)
  {
    bottomline=bl;
  }

  public void setnotetype(int nt,int f,Mensuration m)
  {
    if (notetype==nt)
      return;
    notetype=nt;
    setnumlines(calcNumLines(nt,m));
    numSets=notetype==NoteEvent.NT_Maxima ? m.modus_maior : 1;
  }

  public void setLength(Proportion l)
  {
    length=musictime=l;
  }

  public void setnumlines(int nl)
  {
    numlines=nl;
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
    System.out.println("    Rest: "+NoteEvent.NoteTypeNames[notetype]+","+
                       length.i1+"/"+length.i2+","+
                       bottomline+"."+numlines);
  }
}
