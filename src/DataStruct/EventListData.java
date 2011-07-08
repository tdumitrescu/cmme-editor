/*----------------------------------------------------------------------*/
/*

        Module          : EventListData.java

        Package         : DataStruct

        Classes Included: EventListData

        Purpose         : Generic type for event lists

        Programmer      : Ted Dumitrescu

        Date Started    : 1/15/08 (moved from VoiceEventListData)

        Updates         :
12/1/08: made non-abstract, for implementing generic lists (variant reading,
         ligature data, etc.)
12/5/08: moved recalcEventParams from VoiceEventListData

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   EventListData
Extends: -
Purpose: List of events
------------------------------------------------------------------------*/

public class EventListData
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  /* music events */
  ArrayList<Event> events;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: EventListData()
Purpose:     Initialize list
Parameters:
  Input:  -
  Output: -
------------------------------------------------------------------------*/

  public EventListData()
  {
    initParams();
  }

/*------------------------------------------------------------------------
Method:  EventListData createCopy()
Purpose: Copy list with new copies of events
Parameters:
  Input:  -
  Output: -
  Return: list duplicating this one with copied events
------------------------------------------------------------------------*/

  public EventListData createCopy()
  {
    EventListData newELD=new EventListData();

    for (Event e : this.events)
      newELD.addEvent(e.createCopy());

    return newELD;
  }

/*------------------------------------------------------------------------
Method:  void initParams()
Purpose: Initialize basic parameters
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void initParams()
  {
    events=new ArrayList<Event>();
  }

/*------------------------------------------------------------------------
Method:  void setVoiceParams(Event e)
Purpose: Update voice parameter variables after adding a new event
Parameters:
  Input:  Event e - event just added
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setVoiceParams(Event e)
  {
  }

/*------------------------------------------------------------------------
Method:  void addEvent(Event e)
Purpose: Add event to this voice's list (at end)
Parameters:
  Input:  Event e - event to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addEvent(Event e)
  {
    events.add(e);
  }

/*------------------------------------------------------------------------
Method:  void addEvent(int i,Event e)
Purpose: Add event to this voice's list (at specified location)
Parameters:
  Input:  int i   - index of location for addition
          Event e - event to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addEvent(int i,Event e)
  {
    events.add(i,e);
  }

/*------------------------------------------------------------------------
Method:  Event deleteEvent(int i)
Purpose: Remove event from this voice's list
Parameters:
  Input:  int i - index of event to delete
  Output: -
  Return: deleted Event
------------------------------------------------------------------------*/

  public Event deleteEvent(int i)
  {
    Event deletedEvent=events.get(i);
    events.remove(i);
    return deletedEvent;
  }

  public Event deleteEvent(Event e)
  {
    events.remove(e);
    return e;
  }

/*------------------------------------------------------------------------
Method:  void truncateEvents(int deletionPoint)
Purpose: Truncate event list at a given point
Parameters:
  Input:  int deletionPoint - index of first event to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void truncateEvents(int deletionPoint)
  {
    for (int i=events.size()-1; i>=deletionPoint; i--)
      events.remove(i);
  }

/*------------------------------------------------------------------------
Method:  int calcIndexWithinReading(int i)
Purpose: Calculate "sub-index" for event within a variant reading
Parameters:
  Input:  int i - index of event
  Output: -
  Return: index within variant reading's event list
------------------------------------------------------------------------*/

  public int calcIndexWithinReading(int i)
  {
    int   ri=0; /* index for event within reading */
    Event ve=null;
    do
      {
        ri++;
        ve=getEvent(--i);
      }
    while (ve!=null && ve.geteventtype()!=Event.EVENT_VARIANTDATA_START);

    return ri-1;
  }

/*------------------------------------------------------------------------
Method:  void recalcEventParams([EventListData lastv,Event paramEvent])
Purpose: Recalculate event attributes based on parameters (clef, mensuration
         info, etc)
Parameters:
  Input:  EventListData lastv - previous section of this voice
          Event paramEvent    - event containing starting parameters
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void recalcEventParams()
  {
    recalcEventParams((EventListData)null);
  }

  public void recalcEventParams(EventListData lastv)
  {
    if (events.size()==0)
      return;
    Event paramEvent=lastv==null ? getEvent(0) : lastv.getEvent(lastv.getNumEvents()-1);
    recalcEventParams(paramEvent,paramEvent.getcoloration());
  }

  public void recalcEventParams(Event paramEvent,Coloration curcolor)
  {
    if (events.size()==0)
      return;

    Event              lastevent=null,
                       clefinfoevent=paramEvent.getClefInfoEvent(),
                       mensinfoevent=paramEvent.getMensInfoEvent();
    Proportion         curProportion=paramEvent.getProportion();
    ModernKeySignature curModKeySig=paramEvent.getModernKeySig();
    boolean            displayEditorial=false;
    int                listPlace=0;

    for (Event curevent : events)
      {
        if (curevent.hasSignatureClef())
          {
            if (curevent.hasPrincipalClef())
              lastevent=null;
            curevent.constructClefSets(lastevent,clefinfoevent);
            clefinfoevent=curevent;
            curModKeySig=curevent.getClefSet().getKeySig();
          }
        if (curevent.getMensInfo()!=null)
          mensinfoevent=curevent;
        if (curevent.geteventtype()==Event.EVENT_COLORCHANGE)
          curcolor=new Coloration(curcolor,((ColorChangeEvent)curevent).getcolorscheme());
        else if (curevent.geteventtype()==Event.EVENT_MODERNKEYSIGNATURE)
          curModKeySig=((ModernKeySignatureEvent)curevent).getSigInfo();
        else if (curevent.geteventtype()==Event.EVENT_PROPORTION)
          curProportion=((ProportionEvent)curevent).getProportion();
        else if (curevent.geteventtype()==Event.EVENT_LACUNA)
          displayEditorial=true;
        else if (curevent.geteventtype()==Event.EVENT_LACUNA_END)
          displayEditorial=false;

        curevent.setclefparams(clefinfoevent);
        curevent.setmensparams(mensinfoevent);
        curevent.setcolorparams(curcolor);
        curevent.setModernKeySigParams(curModKeySig);
        curevent.setProportion(curProportion);
        curevent.setDisplayEditorial(displayEditorial);

        lastevent=curevent;
      }
  }

/*------------------------------------------------------------------------
Method:  Event getEvent(int i)
Purpose: Return event from list
Parameters:
  Input:  int i - index of event to return
  Output: -
  Return: event
------------------------------------------------------------------------*/

  public Event getEvent(int i)
  {
    if (i>=0 && i<events.size())
      return events.get(i);
    else
      return null;
  }

  public ArrayList<Event> getEvents()
  {
    return events;
  }

/*------------------------------------------------------------------------
Method:  int getNextEventOfType(int evType,int i,int dir)
Purpose: Return index of next event from list of a given type
Parameters:
  Input:  int evType - event type to find
          int i      - index of event to begin search
          int dir    - direction to search (1=forward,-1=backwards)
  Output: -
  Return: event index (-1 if not found)
------------------------------------------------------------------------*/

  public int getNextEventOfType(int evType,int i,int dir)
  {
    for (; i>=0 && i<events.size(); i+=dir)
      if (getEvent(i).geteventtype()==evType)
        return i;

    return -1;
  }

  /* is there a text-only variant at the indicated location? return it if so */
  public Event getOrigTextOnlyVariant(int ei)
  {
    if (ei>=events.size()-2 ||
        getEvent(ei).geteventtype()!=Event.EVENT_VARIANTDATA_START ||
        getEvent(ei+2).geteventtype()!=Event.EVENT_VARIANTDATA_END)
      return null;
    Event te=getEvent(ei+1);
    return (te.geteventtype()==Event.EVENT_ORIGINALTEXT) ? te : null;
  }

/*------------------------------------------------------------------------
Method:  String [orig|mod]TextToStr()
Purpose: Create string containing all texting (original or modern) in list
Parameters:
  Input:  -
  Output: -
  Return: string with all original text, phrases delimited by markers
          according to style (@ for original phrases, - and space for
          modern syllables)
------------------------------------------------------------------------*/

  public String origTextToStr()
  {
    String text="";
    for (int ei=getNextEventOfType(Event.EVENT_ORIGINALTEXT,0,1);
         ei!=-1;
         ei=getNextEventOfType(Event.EVENT_ORIGINALTEXT,ei+1,1))
      text+=((OriginalTextEvent)getEvent(ei)).getText()+"@";
    if (text.length()>0)
     text=text.substring(0,text.length()-1);
    return text;
  }

  public String modTextToStr()
  {
    String text="";
    for (int ei=0; ei<getNumEvents(); ei++)
      {
        Event e=getEvent(ei);
        switch (e.geteventtype())
          {
            case Event.EVENT_NOTE:
              text=addNoteTextToStr((NoteEvent)e,text);
              break;
            case Event.EVENT_MULTIEVENT:
              for (Iterator i=((MultiEvent)e).iterator(); i.hasNext();)
                {
                  Event e1=(Event)i.next();
                  if (e1.geteventtype()==Event.EVENT_NOTE)
                    text=addNoteTextToStr((NoteEvent)e1,text);
                }
              break;
          }
      }
    if (text.length()>0)
     text=text.substring(0,text.length()-1);
    return text;
  }

  String addNoteTextToStr(NoteEvent ne,String s)
  {
    String noteText=ne.getModernText();
    if (noteText==null)
      return s;
    s+=noteText+(ne.isWordEnd() ? " " : "-");
    return s;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int getNumEvents()
  {
    return events.size();
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this voice
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("  Events:");
    for (Event e : events)
      e.prettyprint();
  }
}
