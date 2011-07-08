/*----------------------------------------------------------------------*/
/*

        Module          : MultiEvent.java

        Package         : DataStruct

        Classes Included: MultiEvent

        Purpose         : Stores multiple simultaneous events

        Programmer      : Ted Dumitrescu

        Date Started    : 2/1/06

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

/*------------------------------------------------------------------------
Class:   MultiEvent
Extends: Event
Purpose: Container for multiple simultaneous events
------------------------------------------------------------------------*/

public class MultiEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  LinkedList<Event> eventList;

  Mensuration mensInfo=null;
  ClefSet     clefset=null,       /* clef group */
              modernclefset=null; /* modern version of clef group */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MultiEvent()
Purpose:     Creates multi-event
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MultiEvent()
  {
    eventtype=EVENT_MULTIEVENT;
    eventList=new LinkedList<Event>();
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
    MultiEvent me=new MultiEvent();
    for (Event e : eventList)
      me.addEvent(e.createCopy());

    me.copyEventAttributes(this);
    me.constructClefSets(null,null);

    return me;
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
    MultiEvent otherME=(MultiEvent)other;

    if (this.getNumEvents()!=otherME.getNumEvents())
      return false;
    for (int i=0; i<this.getNumEvents(); i++)
      if (!this.getEvent(i).equals(otherME.getEvent(i)))
        return false;

    return true;
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
      {
        /* multi-event vs. multi-event */

        /* every note pitch in this must appear in other, and vice versa */
        for (Event e : eventList)
          if (e.geteventtype()==Event.EVENT_NOTE)
            if (!other.hasNotePitch(e.getPitch()))
              return false;
        for (Event e : ((MultiEvent)other).eventList)
          if (e.geteventtype()==Event.EVENT_NOTE)
            if (!this.hasNotePitch(e.getPitch()))
              return false;

        return true;
      }
    else
      {
        /* multi-event vs. single event */
        if (other.geteventtype()!=Event.EVENT_NOTE)
          return false;

        boolean foundMatch=false;
        for (Event e : eventList)
          if (e.geteventtype()==Event.EVENT_NOTE)
            if (e.notePitchMatches(other))
              foundMatch=true;
            else
              return false;

        return foundMatch;
      }
  }

  public boolean hasNotePitch(Pitch p)
  {
    for (Event e : eventList)
      if (e.geteventtype()==Event.EVENT_NOTE)
        if (e.getPitch().equals(p))
          return true;
    return false;
  }

/*------------------------------------------------------------------------
Methods: void calcMusicTime()
Purpose: Calculate amount of musical time taken by this (length of longest
         timed event)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void calcMusicTime()
  {
    for (Iterator i=iterator(); i.hasNext();)
      {
        Proportion imt=((Event)i.next()).getmusictime();
        if (imt.greaterThan(musictime))
          musictime.setVal(imt);
      }
  }

/*------------------------------------------------------------------------
Methods: void addEvent(Event e)
Purpose: Add one event to list
Parameters:
  Input:  Event e - event to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addEvent(Event e)
  {
    eventList.add(e);
    if (e.getmusictime().greaterThan(musictime))
      musictime.setVal(e.getmusictime());

    Mensuration m=e.getMensInfo();
    if (m!=null)
      mensInfo=m;
  }

/*------------------------------------------------------------------------
Methods: Event deleteEvent(Event e)
Purpose: Delete one event from list
Parameters:
  Input:  Event e - event to add
  Output: -
  Return: this event after deletion
------------------------------------------------------------------------*/

  public Event deleteEvent(Event e)
  {
    eventList.remove(eventList.indexOf(e));
    if (eventList.size()>=2)
      {
        /* recalculate parameters for this multi-event */
        if (e.getmusictime().equals(musictime))
          calcMusicTime();
        if (e.getMensInfo()!=null)
          {
            mensInfo=null;
            for (Iterator i=iterator(); i.hasNext();)
              {
                Mensuration m=((Event)i.next()).getMensInfo();
                if (m!=null)
                  mensInfo=m;
              }
          }

        return this;
      }
    else
      /* no more multi-event (only one event left) */
      return (Event)(eventList.getFirst());
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Iterator iterator()
  {
    return eventList.iterator();
  }

  public int getNumEvents()
  {
    return eventList.size();
  }

  public Event getEvent(int i)
  {
    return eventList.get(i);
  }

  public Mensuration getMensInfo()
  {
    return mensInfo;
  }

  public boolean hasEventType(int etype)
  {
    for (Event e : eventList)
      if (e.hasEventType(etype))
        return true;
    return false;
  }

  public Event getFirstEventOfType(int etype)
  {
    for (Event e : eventList)
      if (e.hasEventType(etype))
        return e;
    return null;
  }

  public NoteEvent getLowestNote()
  {
    NoteEvent lowestNote=null;
    for (Event e : eventList)
      if (e.geteventtype()==Event.EVENT_NOTE)
        {
          NoteEvent ne=(NoteEvent)e;
          if (lowestNote==null || lowestNote.getPitch().isHigherThan(ne.getPitch()))
            lowestNote=ne;
        }
    return lowestNote;
  }

  public boolean hasAccidentalClef()
  {
    for (Event e : eventList)
      if (e.hasAccidentalClef())
        return true;
    return false;
  }

  public boolean hasPrincipalClef()
  {
    for (Event e : eventList)
      if (e.hasPrincipalClef())
        return true;
    return false;
  }

  public boolean hasSignatureClef()
  {
    for (Event e : eventList)
      if (e.hasSignatureClef())
        return true;
    return false;
  }

  public int rhythmicEventType()
  {
    int lastType=geteventtype();

    for (Event e : eventList)
      if (e.geteventtype()==Event.EVENT_NOTE)
        return Event.EVENT_NOTE;
      else if (e.getmusictime().i1!=0)
        lastType=e.geteventtype();
    return lastType;
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

  public ClefSet getClefSet(boolean usemodernclefs)
  {
    return usemodernclefs ? modernclefset : clefset;
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
    for (Iterator i=iterator(); i.hasNext();)
      {
        Event e=(Event)i.next();
        if (e.geteventtype()==EVENT_CLEF)
          {
            e.constructClefSets(le,cie);
            le=e;
          }
      }
/*    clefset=le.getClefSet(false);
    modernclefset=le.getClefSet(true);
*/
    clefset=le==null ? null : le.getClefSet(false);
    modernclefset=le==null ? null : le.getClefSet(true);
/*    for (Iterator i=iterator(); i.hasNext();)
      {
        Event e=(Event)i.next();
        if (e.geteventtype()==EVENT_CLEF)
          {
            clefset=e.getClefSet(false);
            modernclefset=e.getClefSet(true);
          }
      }*/

    /* set individual events to have the same clef sets */
    for (Iterator i=iterator(); i.hasNext();)
      {
        Event e=(Event)i.next();
        if (e.geteventtype()==EVENT_CLEF)
          {
            e.setClefSet(clefset,false);
            e.setClefSet(modernclefset,true);
//            clefset.addclef(((ClefEvent)e).getClef(false,false));
/* mod clefset */
          }
      }

    /* add to clef set */
//    if (!getClefSet().getprincipalclef().isprincipalclef())
//    if (cie!=null)
    if (cie!=null && !clefset.getprincipalclef().isprincipalclef())
      addToSigClefs(cie);
  }

/*------------------------------------------------------------------------
Method:  Event noClefEvent()
Purpose: Create copy of this event with no clefs
Parameters:
  Input:  -
  Output: -
  Return: new clefless event
------------------------------------------------------------------------*/

  public Event noClefEvent()
  {
    MultiEvent newEvent=new MultiEvent();

    for (Iterator i=iterator(); i.hasNext();)
      {
        Event e=(Event)i.next();
        if (e.geteventtype()!=EVENT_CLEF)
          newEvent.addEvent(e);
      }
    if (newEvent.getNumEvents()==0)
      return null;
    if (newEvent.getNumEvents()==1)
      return newEvent.getEvent(0);
    return newEvent;
  }

/*------------------------------------------------------------------------
Method:  Event noSigClefEvent()
Purpose: Create copy of this event with no signature less-principal clefs
Parameters:
  Input:  -
  Output: -
  Return: new event
------------------------------------------------------------------------*/

  public Event noSigClefEvent()
  {
    MultiEvent newEvent=new MultiEvent();
    Event      le=null;

    for (Iterator i=iterator(); i.hasNext();)
      {
        Event e=(Event)i.next();
        if (e.geteventtype()!=EVENT_CLEF ||
            e.hasPrincipalClef())
          {
            newEvent.addEvent(e);
            if (e.hasPrincipalClef())
              le=e;
          }
      }
    if (newEvent.getNumEvents()==0)
      return null;
    if (newEvent.getNumEvents()==1)
      return newEvent.getEvent(0);

    newEvent.clefset=le==null ? null : le.getClefSet(false);
    newEvent.modernclefset=le==null ? null : le.getClefSet(true);

    return newEvent;
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

/*------------------------------------------------------------------------
Method:  void set*params
Purpose: Sets music parameters current at this event (clef, mensuration)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setmensparams(Event me)
  {
    for (Iterator i=iterator(); i.hasNext();)
      ((Event)i.next()).setmensparams(me);
  }

  public void setcolorparams(Coloration c)
  {
    for (Iterator i=iterator(); i.hasNext();)
      ((Event)i.next()).setcolorparams(c);
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
    System.out.println("   Multi-Event begin");
    for (Iterator i=iterator(); i.hasNext();)
      ((Event)i.next()).prettyprint();
    System.out.println("   Multi-Event end");
  }
}
