/*----------------------------------------------------------------------*/
/*

        Module          : VoiceEventListData.java

        Package         : DataStruct

        Classes Included: VoiceEventListData

        Purpose         : Abstract type for voices based on event lists

        Programmer      : Ted Dumitrescu

        Date Started    : 7/24/07

        Updates         :
1/15/08: moved basic list functions to EventListData

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   VoiceEventListData
Extends: -
Purpose: Event list data for one voice
------------------------------------------------------------------------*/

public abstract class VoiceEventListData extends EventListData
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Voice                         metaData;
  MusicSection                  section;
  ArrayList<VariantVersionData> missingVersions;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Method:  void initParams(Voice v,MusicSection section)
Purpose: Initialize basic parameters
Parameters:
  Input:  Voice v              - voice metadata
          MusicSection section - section data
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void initParams(Voice v,MusicSection section)
  {
    metaData=v;
    this.section=section;
    this.missingVersions=new ArrayList<VariantVersionData>();
    super.initParams();
  }

  void initParams()
  {
    metaData=null;
    section=null;
    this.missingVersions=new ArrayList<VariantVersionData>();
    super.initParams();
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
    int listPlace=events.size();

    if (isDefaultVersion())
      e.setDefaultListPlace(listPlace);
    else
      e.setListPlace(listPlace);
    super.addEvent(e);
    setVoiceParams(e);
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
    Event addedEvent=e;
    int   listPlace=i;

    if (isDefaultVersion())
      e.setDefaultListPlace(listPlace);
    else
      e.setListPlace(listPlace);
    super.addEvent(i,e);
    setVoiceParams(e);
    if (section.isDefaultVersion())
      {
        e.setDefaultListPlace(listPlace);
        for (Iterator li=events.listIterator(listPlace+1); li.hasNext();)
          {
            e=(Event)li.next();
            e.setDefaultListPlace(e.getDefaultListPlace()+1);
          }
      }
    else
      for (Iterator li=events.listIterator(i+1); li.hasNext();)
        {
          e=(Event)li.next();
          e.setListPlace(++i);
        }
    e.setcolorparams(section.getBaseColoration());

    if (addedEvent.hasSignatureClef() ||
        addedEvent.getMensInfo()!=null ||
        addedEvent.geteventtype()==Event.EVENT_COLORCHANGE ||
        addedEvent.geteventtype()==Event.EVENT_LACUNA ||
        addedEvent.geteventtype()==Event.EVENT_LACUNA_END ||
        addedEvent.geteventtype()==Event.EVENT_MODERNKEYSIGNATURE ||
        addedEvent.geteventtype()==Event.EVENT_VARIANTDATA_START ||
        addedEvent.geteventtype()==Event.EVENT_VARIANTDATA_END)
      recalcEventParams();
  }

/*------------------------------------------------------------------------
Method:  Event deleteEvent(int i[,VoiceEventListData lastv])
Purpose: Remove event from this voice's list
Parameters:
  Input:  int i                    - index of event to delete
          VoiceEventListData lastv - previous section of this voice
  Output: -
  Return: deleted Event
------------------------------------------------------------------------*/

  public Event deleteEvent(int i)
  {
    return deleteEvent(i,null);
  }

  public Event deleteEvent(int i,VoiceEventListData lastv)
  {
    Event e,
          deletedEvent=super.deleteEvent(i);

    if (isDefaultVersion())
      for (Iterator li=events.listIterator(i); li.hasNext();)
        {
          e=(Event)li.next();
          e.setDefaultListPlace(e.getDefaultListPlace()-1);
        }
    else
      for (Iterator li=events.listIterator(i); li.hasNext();)
        {
          e=(Event)li.next();
          e.setListPlace(e.getListPlace(false)-1);
        }

    if (deletedEvent.hasSignatureClef() ||
        deletedEvent.getMensInfo()!=null ||
        deletedEvent.geteventtype()==Event.EVENT_COLORCHANGE ||
        deletedEvent.geteventtype()==Event.EVENT_LACUNA ||
        deletedEvent.geteventtype()==Event.EVENT_LACUNA_END ||
        deletedEvent.geteventtype()==Event.EVENT_MODERNKEYSIGNATURE ||
        deletedEvent.geteventtype()==Event.EVENT_VARIANTDATA_START ||
        deletedEvent.geteventtype()==Event.EVENT_VARIANTDATA_END)
      recalcEventParams(lastv);

    return deletedEvent;
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
    super.truncateEvents(deletionPoint);
    addEvent(new Event(Event.EVENT_SECTIONEND));
    recalcEventParams();
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

  public void recalcEventParams(EventListData lastv)
  {
    if (events.size()==0)
      return;
    Event paramEvent=lastv==null ? getEvent(0) : lastv.getEvent(lastv.getNumEvents()-1);
    recalcEventParams(paramEvent);
  }

  public void recalcEventParams(Event paramEvent)
  {
    if (events.size()==0)
      return;
    super.recalcEventParams(paramEvent,section.getBaseColoration());

    int listPlace=0;
    for (Event curevent : events)
      if (isDefaultVersion())
        curevent.setDefaultListPlace(listPlace++);
      else
        curevent.setListPlace(listPlace++);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Voice getMetaData()
  {
    return metaData;
  }

  public ArrayList<VariantVersionData> getMissingVersions()
  {
    return missingVersions;
  }

  public MusicSection getSection()
  {
    return section;
  }

  public int getVoiceNum()
  {
    return metaData.getNum();
  }

  public boolean isDefaultVersion()
  {
    return section==null ? false : section.isDefaultVersion();
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setMetaData(Voice md)
  {
    metaData=md;
  }

  public void addMissingVersion(VariantVersionData vvd)
  {
    missingVersions.add(vvd);
  }

  public void removeMissingVersion(VariantVersionData vvd)
  {
    missingVersions.remove(vvd);
  }

  public void setMissingVersions(ArrayList<VariantVersionData> missingVersions)
  {
    this.missingVersions=missingVersions;
  }

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
    System.out.println("Voice "+metaData.getNum()+":");
    super.prettyprint();
  }
}
