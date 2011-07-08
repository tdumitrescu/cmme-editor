/*----------------------------------------------------------------------*/
/*

        Module          : PieceData.java

        Package         : DataStruct

        Classes Included: PieceData

        Purpose         : Contains all information for one piece (metadata
                          and musical data)

        Programmer      : Ted Dumitrescu

        Date Started    : 99

        Updates         :
4/14/05:  removed separate classes VoiceData, VoiceList, and PGenData
1/31/06:  added "Editor" field
3/8/06:   added "Section Title" field
          added base coloration data
3/20/06:  added incipit-score flag
2/07:     modified basic structure to incorporate muiltiple sections
10/19/07: added "Notes" field

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   PieceData
Extends: -
Purpose: Piece information structure
------------------------------------------------------------------------*/

public class PieceData
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  String      title,section,composer,editor,publicNotes,notes,
              fullTitle;
  boolean     isIncipit=false;
  Coloration  baseColoration=Coloration.DEFAULT_COLORATION;
  Mensuration baseMensuration=Mensuration.DEFAULT_MENSURATION;

  Voice[]                       voiceData;
  ArrayList<MusicSection>       musicSections;

  ArrayList<VariantVersionData> variantVersions;
  ArrayList<VariantReading>     variantReadings;
  VariantVersionData            curVersion;
  PieceData                     defaultMusicData;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: PieceData()
Purpose:     Initialize structure
Parameters:
  Input:  -
  Output: -
------------------------------------------------------------------------*/

  public PieceData()
  {
    this.defaultMusicData=this;

    musicSections=new ArrayList<MusicSection>();
    variantVersions=new ArrayList<VariantVersionData>();
    variantReadings=new ArrayList<VariantReading>();
    this.curVersion=null;
  }

  /* shallow copy */
  public PieceData(PieceData other)
  {
    this.defaultMusicData=other;

    setGeneralData(other.title,other.section,other.composer,other.editor,other.publicNotes,other.notes);
    this.baseColoration=other.baseColoration;
    this.baseMensuration=other.baseMensuration;
    this.voiceData=other.voiceData;
    this.musicSections=other.musicSections;
    this.variantVersions=other.variantVersions;
    this.variantReadings=other.variantReadings;

    this.curVersion=null;
  }

/*------------------------------------------------------------------------
Method:  void setGeneralData(String t,String st,String c,String e,String publicNotes,String notes)
Purpose: Set values for general data
Parameters:
  Input:  String t           - title
          String st          - section title
          String c           - composer
          String e           - editor
          String publicNotes - public notes
          String notes       - private notes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setGeneralData(String t,String st,String c,String e,String publicNotes,String notes)
  {
    title=t;
    composer=c;
    editor=e;
    if (st!=null && !st.equals(""))
      section=st;
    else
      section=null;
    this.publicNotes=publicNotes;
    this.notes=notes;
    createFullTitle();
  }

/*------------------------------------------------------------------------
Method:  void createFullTitle()
Purpose: Recalculate full display title for score
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createFullTitle()
  {
    fullTitle=title;
    if (section!=null)
      fullTitle+=": "+section;
    if (isIncipit)
      fullTitle+=" (incipit)";
  }

/*------------------------------------------------------------------------
Method:  void recalcAllEventParams()
Purpose: Recalculate event attributes based on parameters (clef, mensuration
         info) for all voices in all sections
Parameters:
  Input:  -
  Output: -
  Return: this
------------------------------------------------------------------------*/

  public PieceData recalcAllEventParams()
  {
    VoiceEventListData[] v=new VoiceEventListData[voiceData.length];
    for (int vi=0; vi<v.length; vi++)
      v[vi]=null;

    for (MusicSection s : musicSections)
      {
        s.recalcAllEventParams(v);
        for (int vi=0; vi<voiceData.length; vi++)
          {
            VoiceEventListData tmpv=s.getVoice(vi);
            if (tmpv!=null)
              v[vi]=tmpv;
          }
      }
    return this;
  }

/*------------------------------------------------------------------------
Method:  void addVariantVersion(VariantVersionData newVersion)
Purpose: Add variant version declaration to list
Parameters:
  Input:  VariantVersionData newVersion - version to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addVariantVersion(VariantVersionData newVersion)
  {
    if (variantVersions.size()==0)
      newVersion.setDefault(true);
    variantVersions.add(newVersion);
  }

/*------------------------------------------------------------------------
Method:  void addSection([int si,]MusicSection newSection)
Purpose: Add section to list
Parameters:
  Input:  int si                  - index of location to insert new section
          MusicSection newSection - section to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addSection(int si,MusicSection newSection)
  {
    musicSections.add(si,newSection);
  }

  public void addSection(MusicSection newSection)
  {
    addSection(getNumSections(),newSection);
  }

/*------------------------------------------------------------------------
Method:  void deleteSection(int si)
Purpose: Remove section from list
Parameters:
  Input:  int si - index of section to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteSection(int si)
  {
    musicSections.remove(si);
    recalcAllEventParams();
  }

/*------------------------------------------------------------------------
Method:  void addEvent(int snum,int vnum,int i,Event e)
Purpose: Insert event in one section and update parameters throughout score
Parameters:
  Input:  int snum,vnum - section and voice number
          int i         - index in voice's event list for new event
          Event e       - new event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addEvent(int snum,int vnum,int i,Event e)
  {
    VoiceEventListData v=getSection(snum).getVoice(vnum),
                       lastv=v;
    v.addEvent(i,e);

    for (snum++; snum<getNumSections(); snum++)
      {
        v=getSection(snum).getVoice(vnum);
        if (v!=null)
          {
            v.recalcEventParams(lastv);
            lastv=v;
          }
      }
  }

/*------------------------------------------------------------------------
Method:  int addVariantEvent(VariantVersionData vvd,int snum,int vnum,int vi,int di,Event e)
Purpose: Insert event in one variant version
Parameters:
  Input:  VariantVersionData vvd - variant version for insertion
          PieceData vmd          - variant music data
          int snum,vnum          - section and voice number
          int vi                 - index in variant voice event list
          int di                 - index in default voice event list
          Event e                - new event
  Output: -
  Return: positioning of new event within reading (beginning, end, middle)
------------------------------------------------------------------------*/

  public int addVariantEvent(VariantVersionData vvd,PieceData vmd,int snum,int vnum,int vi,int di,Event e)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
    Event              prevEvent=v.getEvent(vi-1),
                       nextEvent=v.getEvent(vi);
    VariantReading     prevEventReading=prevEvent!=null ? prevEvent.getVariantReading(vvd) : null,
                       nextEventReading=nextEvent.getVariantReading(vvd);
    VariantMarkerEvent vm1,vm2;

    /* adding within reading? */
    if (prevEventReading!=null && prevEventReading==nextEventReading)
      {
        /* eventToDelete is within a variant reading */
        if (prevEventReading.getVersions().size()>1)
          return addEventInVersion(vvd,vmd,snum,vnum,vi,di,e); // create separate reading and add

        prevEventReading.addEvent(v.calcIndexWithinReading(vi),e);
        return VariantReading.MIDDLE;
      }

    /* not already in a variant reading */
    /* create new variant */
    VariantReading newReading=new VariantReading();
    newReading.addVersion(vvd);
    variantReadings.add(newReading);

    int vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi-1,-1),
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi-1,-1);
    if (vmi1>vmi2)
      {
        /* currently in-between two variant markers
           this means that a variant reading exists here in one of the other
           versions, so we need to attach this new reading to the same markers */
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,1);
        vm1=(VariantMarkerEvent)v.getEvent(vmi1);
        vm2=(VariantMarkerEvent)v.getEvent(vmi2);

        for (int i=vmi1+1; i<vmi2; i++) /* copy default reading into new reading */
          {
            Event newEventCopy=v.getEvent(i).createCopy();
            vmd.replaceEvent(snum,vnum,i,newEventCopy);
            if (i==vi)
              newReading.addEvent(e);
            newReading.addEvent(newEventCopy);
          }
        if (vi==vmi2)
          newReading.addEvent(e);

        vm1.addReading(newReading);
        return VariantReading.MIDDLE;
      }

    newReading.addEvent(e);
    vm1=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_START);
    vm2=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_END);
    vm1.addReading(newReading);
    vm2.setReadingsList(vm1.getReadings());
    vm2.setDefaultLength(vm1.getDefaultLength()); /* link default lengths in markers */
    addEvent(snum,vnum,di,vm1);
    addEvent(snum,vnum,di+1,vm2);

    vmd.addEvent(snum,vnum,vi,vm1);
    vmd.addEvent(snum,vnum,vi+1,vm2);

    return VariantReading.NEWVARIANT;
  }

/*------------------------------------------------------------------------
Method:  void deleteEvent(int snum,int vnum,int i)
Purpose: Delete event in one section and update parameters throughout score
Parameters:
  Input:  int snum,vnum - section and voice number
          int i         - index in voice's event list for event to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteEvent(int snum,int vnum,int i)
  {
    VoiceEventListData v=getSection(snum).getVoice(vnum),
                       lastv=null;
    for (snum--; lastv==null && snum>=0; snum--)
      lastv=getSection(snum).getVoice(vnum);

    v.deleteEvent(i,lastv);
  }

/*------------------------------------------------------------------------
Method:  int [add|delete]VariantEvent(VariantVersionData vvd,PieceData vmd,
                                int snum,int vnum,int vi)
Purpose: Add/delete event in one variant version
Parameters:
  Input:  VariantVersionData vvd - variant version for addition/deletion
          PieceData vmd          - variant music data
          int snum,vnum          - section and voice number
          int vi                 - index in variant voice event list
  Output: -
  Return: positioning of new event within reading (beginning, end, middle)
------------------------------------------------------------------------*/

  public int addEventInVersion(VariantVersionData vvd,PieceData vmd,int snum,int vnum,int vi,int di,Event e)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
    Event              prevEvent=v.getEvent(vi-1),
                       nextEvent=v.getEvent(vi);
    VariantReading     prevEventReading=prevEvent!=null ? prevEvent.getVariantReading(vvd) : null,
                       nextEventReading=nextEvent.getVariantReading(vvd);

    /* if necessary, remove this one version from the reading */
    if (prevEventReading!=null && prevEventReading==nextEventReading &&
        prevEventReading.getVersions().size()>1)
      createSeparateReadingForVersion(vvd,vmd,snum,vnum,vi);

    return addVariantEvent(vvd,vmd,snum,vnum,vi,di,e);
  }

  public int deleteEventInVersion(VariantVersionData vvd,PieceData vmd,int snum,int vnum,int vi)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
    Event              eventToDelete=v.getEvent(vi);
    VariantReading     curEventReading=eventToDelete.getVariantReading(vvd);

    /* if necessary, remove this one version from the reading */
    if (curEventReading!=null && curEventReading.getVersions().size()>1)
      createSeparateReadingForVersion(vvd,vmd,snum,vnum,vi);

    return deleteVariantEvent(vvd,vmd,snum,vnum,vi);
  }

  public Event duplicateEventInVersion(VariantVersionData vvd,PieceData vmd,int snum,int vnum,int vi)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
    Event              eventToDuplicate=v.getEvent(vi);
    VariantReading     curEventReading=eventToDuplicate.getVariantReading(vvd);

    /* if necessary, remove this one version from the reading */
    if (curEventReading!=null && curEventReading.getVersions().size()>1)
      createSeparateReadingForVersion(vvd,vmd,snum,vnum,vi);

    return duplicateEventInVariant(vvd,vmd,snum,vnum,vi);
  }

  public Event duplicateEventsInVersion(VariantVersionData vvd,PieceData vmd,
                                        int snum,int vnum,int vi1,int vi2)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
    Event              eventToDuplicate=v.getEvent(vi1);
    VariantReading     curEventReading=eventToDuplicate.getVariantReading(vvd);

    /* if necessary, remove this one version from the reading */
    if (curEventReading!=null && curEventReading.getVersions().size()>1)
      createSeparateReadingForVersion(vvd,vmd,snum,vnum,vi1);

    return duplicateEventsInVariant(vvd,vmd,snum,vnum,vi1,vi2);
  }

  void createSeparateReadingForVersion(VariantVersionData vvd,PieceData vmd,int snum,int vnum,int vi)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
    int vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi,-1),
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,1);
    VariantMarkerEvent vm1=(VariantMarkerEvent)v.getEvent(vmi1);

    /* copy reading into separate one for this version only */
    VariantReading newReading=vm1.getVariantReading(vvd).separateVersion(vvd);
    vm1.addReading(newReading);

    /* update vmd to reflect new reading */
    for (int i=vmi1+1; i<vmi2; i++)
      vmd.replaceEvent(snum,vnum,i,newReading.getEvent(i-vmi1-1));
  }

  public int deleteVariantEvent(VariantVersionData vvd,PieceData vmd,int snum,int vnum,int vi)
  {
    /* check to see whether to delete from an existing variant reading, or create a new one */
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum),
                       defaultV=getSection(snum).getVoice(vnum);
    Event              eventToDelete=v.getEvent(vi),
                       prevEvent=v.getEvent(vi-1),
                       nextEvent=v.getEvent(vi+1);
    VariantReading     curEventReading=eventToDelete.getVariantReading(vvd),
                       prevEventReading=prevEvent!=null ? prevEvent.getVariantReading(vvd) : null,
                       nextEventReading=nextEvent.getVariantReading(vvd);
    VariantMarkerEvent vm1,vm2;

    if (eventToDelete.geteventtype()==Event.EVENT_VARIANTDATA_START ||
        eventToDelete.geteventtype()==Event.EVENT_VARIANTDATA_END)
      {
        System.err.println("ERROR: Attempting to delete VariantMarkerEvent: event no. "+vi);
        return VariantReading.NOACTION;
      }

    if (curEventReading!=null)
      {
        /* eventToDelete is within a variant reading */
        if (curEventReading.getVersions().size()>1)
          return deleteEventInVersion(vvd,vmd,snum,vnum,vi); // create separate reading and delete

        curEventReading.deleteEvent(eventToDelete);

        if (curEventReading.getNumEvents()==0)
          {
            /* REMOVED: automatic deletion of empty variant
               all readings must be manually removed or automatically consolidated 
               upon save (otherwise editing actions which temporarily delete events
               won't work, e.g., making a multi-event) */

            /* deleted last event in reading 
            VariantMarkerEvent vme=(VariantMarkerEvent)prevEvent;
            int di=vme.getDefaultListPlace(),
                numEventsInDefault=defaultV.getEvent(defaultV.getNextEventOfType(Event.EVENT_VARIANTDATA_END,di,1)).getDefaultListPlace()-di-1;

            if (numEventsInDefault==0)
              {
                vme.removeReading(curEventReading);
                if (vme.getNumReadings()==0)
                  {
                     deleted last variant reading at this position;
                       now delete marker events 
                    deleteEvent(snum,vnum,
                                defaultV.getNextEventOfType(Event.EVENT_VARIANTDATA_END,di,1));
                    deleteEvent(snum,vnum,
                                defaultV.getNextEventOfType(Event.EVENT_VARIANTDATA_START,di,-1));

                    vmd.deleteEvent(snum,vnum,vi+1);
                    vmd.deleteEvent(snum,vnum,vi-1);

                    return VariantReading.DELETED;
                  }
              }*/
          }

        return VariantReading.MIDDLE;
      }

    /* eventToDelete is not already in a variant reading */
    /* create new variant */
    VariantReading newReading=new VariantReading();
    newReading.addVersion(vvd);
    variantReadings.add(newReading);

    int vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi,-1),
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,-1);
    if (vmi1>vmi2)
      {
        /* currently in-between two variant markers
           this means that a variant reading exists here in one of the other
           versions, so we need to attach this new reading to the same markers */
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,1);
        vm1=(VariantMarkerEvent)v.getEvent(vmi1);
        vm2=(VariantMarkerEvent)v.getEvent(vmi2);

        for (int i=vmi1+1; i<vmi2; i++) /* copy default reading into new reading */
          {
            Event newEventCopy=v.getEvent(i).createCopy();
            vmd.replaceEvent(snum,vnum,i,newEventCopy);
            if (i!=vi)
              newReading.addEvent(newEventCopy);
          }

        vm1.addReading(newReading);
        return VariantReading.NEWREADING;
      }

    int di=eventToDelete.getDefaultListPlace();

    vm1=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_START);
    vm2=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_END);
    vm1.addReading(newReading);
    vm2.setReadingsList(vm1.getReadings());
    vm1.getDefaultLength().add(eventToDelete.getmusictime());
    vm2.setDefaultLength(vm1.getDefaultLength()); /* link default lengths in markers */
    addEvent(snum,vnum,di,vm1);
    addEvent(snum,vnum,di+2,vm2);

    vmd.addEvent(snum,vnum,vi,vm1);
    vmd.addEvent(snum,vnum,vi+2,vm2);

    return VariantReading.NEWVARIANT;
  }

/*------------------------------------------------------------------------
Method:  void deleteAllVariantReadingsAtLoc(VariantVersionData vvd,PieceData vmd,
                                            int snum,int vnum,int vi)
Purpose: Delete all readings at one location
Parameters:
  Input:  VariantVersionData vvd - variant version
          PieceData vmd          - variant music data
          int snum,vnum          - section and voice number
          int vi                 - index in variant voice event list
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteAllVariantReadingsAtLoc(VariantVersionData vvd,PieceData vmd,int snum,int vnum,int vi)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum),
                       defaultV=getSection(snum).getVoice(vnum);
    int                vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi,-1),
                       vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi-1,-1);

    if (!(vmi1>vmi2))
      return; /* not in-between markers */

    VariantMarkerEvent vme=(VariantMarkerEvent)v.getEvent(vmi1);
    for (VariantReading vr : vme.getReadings())
      variantReadings.remove(vr);

    vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,1);
    int di1=vme.getDefaultListPlace(),
        di2=defaultV.getNextEventOfType(Event.EVENT_VARIANTDATA_END,di1,1);

    /* delete marker events, end of story */
    deleteEvent(snum,vnum,di2);
    deleteEvent(snum,vnum,di1);

    if (vvd.isDefault())
      return;

    vmd.deleteEvent(snum,vnum,vmi2);
    vmd.deleteEvent(snum,vnum,vmi1);
  }

/*------------------------------------------------------------------------
Method:  void deleteVariantReading(VariantVersionData vvd,PieceData vmd,
                                   int snum,int vnum,int vi)
Purpose: Delete reading in one variant version
Parameters:
  Input:  VariantVersionData vvd - variant version
          PieceData vmd          - variant music data
          int snum,vnum          - section and voice number
          int vi                 - index in variant voice event list
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteVariantReading(VariantVersionData vvd,PieceData vmd,int snum,int vnum,int vi)
  {
    /* check to see whether to delete from an existing variant reading, or create a new one */
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum),
                       defaultV=getSection(snum).getVoice(vnum);
    int                vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi,-1),
                       vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,1);

    /* delete events from variant PieceData */
    for (int i=vmi2-1; i>vmi1; i--)
      vmd.deleteEvent(snum,vnum,i);

    /* replace with events from default reading */
    VariantMarkerEvent vme=(VariantMarkerEvent)v.getEvent(vmi1);
    int di1=vme.getDefaultListPlace(),
        di2=defaultV.getNextEventOfType(Event.EVENT_VARIANTDATA_END,di1,1);
    vmi2=vmi1+1;
    for (int i=di1+1; i<di2; i++)
      vmd.addEvent(snum,vnum,vmi2++,defaultV.getEvent(i));

    /* remove links to reading */
    VariantReading readingToDelete=vme.getVariantReading(vvd);
    readingToDelete.deleteVersion(vvd);
    if (readingToDelete.getVersions().size()==0)
      {
        vme.removeReading(readingToDelete);
        variantReadings.remove(readingToDelete);
        if (vme.getNumReadings()==0)
          {
            /* deleted last variant reading at this position;
               now delete marker events */
            deleteEvent(snum,vnum,di2);
            deleteEvent(snum,vnum,di1);

            vmd.deleteEvent(snum,vnum,vmi2);
            vmd.deleteEvent(snum,vnum,vmi1);
          }
      }
  }

  /* delete version but only update default PieceData */
  public void deleteVariantReading(VariantVersionData vvd,int snum,int vnum,int vi)
  {
    VoiceEventListData defaultV=getSection(snum).getVoice(vnum);
    int                vmi1=defaultV.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi,-1),
                       vmi2=defaultV.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,1);
    VariantMarkerEvent vme=(VariantMarkerEvent)defaultV.getEvent(vmi1);

    /* remove links to reading */
    VariantReading readingToDelete=vme.getVariantReading(vvd);
    readingToDelete.deleteVersion(vvd);
    if (readingToDelete.getVersions().size()==0)
      {
        vme.removeReading(readingToDelete);
        variantReadings.remove(readingToDelete);
        if (vme.getNumReadings()==0)
          {
            /* deleted last variant reading at this position;
               now delete marker events */
            deleteEvent(snum,vnum,vmi2);
            deleteEvent(snum,vnum,vmi1);
          }
      }
  }

/*------------------------------------------------------------------------
Method:  Event duplicateEventInVariant(VariantVersionData vvd,PieceData vmd,
                                       int snum,int vnum,int vi)
Purpose: Create new variant containing copy of one event from default list
Parameters:
  Input:  VariantVersionData vvd - variant version for new reading
          PieceData vmd          - variant music data
          int snum,vnum          - section and voice number
          int vi                 - index in variant voice event list
  Output: -
  Return: new copy of Event
------------------------------------------------------------------------*/

  public Event duplicateEventInVariant(VariantVersionData vvd,PieceData vmd,
                                       int snum,int vnum,int vi)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
    Event              eventToCopy=vmd.getSection(snum).getVoice(vnum).getEvent(vi),
                       newEventCopy;
    VariantReading     curEventReading=eventToCopy.getVariantReading(vvd);
    int                di=eventToCopy.getDefaultListPlace();
    VariantMarkerEvent vm1,vm2;

    if (curEventReading!=null)
      if (curEventReading.getVersions().size()>1)
        return duplicateEventInVersion(vvd,vmd,snum,vnum,vi);
      else
        return eventToCopy; /* already in reading, don't create another */

    /* create new variant */
    VariantReading newReading=new VariantReading();
    newReading.addVersion(vvd);
    variantReadings.add(newReading);

    int vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi,-1),
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,-1);
    if (vmi1>vmi2)
      {
        /* currently in-between two variant markers
           this means that a variant reading exists here in one of the other
           versions, so we need to attach this new reading to the same markers */
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,1);
        vm1=(VariantMarkerEvent)v.getEvent(vmi1);
        vm2=(VariantMarkerEvent)v.getEvent(vmi2);

        Event selectedEvent=null;
        for (int i=vmi1+1; i<vmi2; i++) /* copy default reading into new reading */
          {
            newEventCopy=v.getEvent(i).createCopy();
            if (i==vi)
              selectedEvent=newEventCopy;
            vmd.replaceEvent(snum,vnum,i,newEventCopy);
            newReading.addEvent(newEventCopy);
          }

        vm1.addReading(newReading);
        return selectedEvent;
      }

    newEventCopy=eventToCopy.createCopy();
    vmd.replaceEvent(snum,vnum,vi,newEventCopy);
    newReading.addEvent(newEventCopy);

    vm1=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_START);
    vm2=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_END);
    vm1.addReading(newReading);
    Proportion defaultLength=Proportion.copyProportion(newEventCopy.getmusictime());
    vm1.setDefaultLength(defaultLength==null ? new Proportion(0,1) : defaultLength);
    vm2.setReadingsList(vm1.getReadings());
    vm2.setDefaultLength(vm1.getDefaultLength()); /* link default lengths in markers */
    addEvent(snum,vnum,di,vm1);
    addEvent(snum,vnum,di+2,vm2);

    vmd.addEvent(snum,vnum,vi,vm1);
    vmd.addEvent(snum,vnum,vi+2,vm2);

    return newEventCopy;
  }

/*------------------------------------------------------------------------
Method:  Event duplicateEventsInVariant(VariantVersionData vvd,PieceData vmd,
                                        int snum,int vnum,int vi)
Purpose: Create new variant containing copy of two events from default list
Parameters:
  Input:  VariantVersionData vvd - variant version for new reading
          PieceData vmd          - variant music data
          int snum,vnum          - section and voice number
          int vi1,vi2            - index in variant voice event list of
                                   each event
  Output: -
  Return: new copy of first Event
------------------------------------------------------------------------*/

  boolean variantsBetweenEvents(VoiceEventListData v,int vi1,int vi2)
  {
    int vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi1+1,1),
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vmi1,1);
    return vmi1>vi1 && vmi1<vi2 && vmi2>vmi1 && vmi2<vi2;
  }

  public Event duplicateEventsInVariant(VariantVersionData vvd,PieceData vmd,
                                        int snum,int vnum,int vi1,int vi2)
  {
    VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
    Event              eventToCopy1=v.getEvent(vi1),
                       eventToCopy2=v.getEvent(vi2);
    VariantReading     curEventReading1=eventToCopy1.getVariantReading(vvd),
                       curEventReading2=eventToCopy2.getVariantReading(vvd);
    int                di1=eventToCopy1.getDefaultListPlace(),
                       di2=eventToCopy2.getDefaultListPlace(),
                       vmi1,vmi2;
    VariantMarkerEvent vm1,vm2;

    /* check that no other variants are in between events */
    if (variantsBetweenEvents(v,vi1,vi2))
      return null;

    if (curEventReading1!=null)
      if (curEventReading1==curEventReading2)
        if (curEventReading1.getVersions().size()>1)
          return duplicateEventsInVersion(vvd,vmd,snum,vnum,vi1,vi2);
        else
          return eventToCopy1; /* already in the same reading, don't create another */
      else
        if (curEventReading2==null)
          {
            /* extend first reading to encompass second event */

            /* remove end marker and reinsert after second event */
            vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi1,-1);
            vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi1,1);
            vm1=(VariantMarkerEvent)v.getEvent(vmi1);
            vm2=(VariantMarkerEvent)v.getEvent(vmi2);
            deleteEvent(snum,vnum,vm2.getDefaultListPlace());
            vmd.deleteEvent(snum,vnum,vmi2);
            addEvent(snum,vnum,di2,vm2);
            vmd.addEvent(snum,vnum,vi2,vm2);

            /* duplicate last events in all readings */
            for (int i=vmi2; i<vm2.getListPlace(false); i++)
              for (VariantReading vr : vm1.getReadings())
                {
                  Event newEventCopy=v.getEvent(i).createCopy();
                  if (vr==curEventReading1)
                    vmd.replaceEvent(snum,vnum,i,newEventCopy);
                  vr.addEvent(newEventCopy);
                }

            return v.getEvent(vi1);
          }
        else
          {
            /* combine readings */
System.out.println("duplicateEventsInVariant: case not implemented (combine readings)");
            return null;
          }
    else /* curEventReading1==null */
      if (curEventReading2!=null)
        {
          /* extend second reading to encompass first event */

          /* remove start marker and reinsert before first event */
          vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi1,1);
          vm1=(VariantMarkerEvent)v.getEvent(vmi1);
          deleteEvent(snum,vnum,vm1.getDefaultListPlace());
          vmd.deleteEvent(snum,vnum,vmi1);
          addEvent(snum,vnum,di1,vm1);
          vmd.addEvent(snum,vnum,vi1,vm1);

          /* duplicate first events in all readings */
          for (int i=vmi1; i>vm1.getListPlace(false); i--) 
            for (VariantReading vr : vm1.getReadings())
              {
                Event newEventCopy=v.getEvent(i).createCopy();
                if (vr==curEventReading2)
                  vmd.replaceEvent(snum,vnum,i,newEventCopy);
                vr.addEvent(0,newEventCopy);
              }

          return v.getEvent(vm1.getListPlace(false)+1);
        }

    /* neither event is already in a reading; create a new one */
    VariantReading newReading=new VariantReading();
    newReading.addVersion(vvd);
    variantReadings.add(newReading);

    vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi1,-1);
    vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi1,-1);
    if (vmi1>vmi2)
      {
        /* event 1 is currently in-between two variant markers
           this means that a variant reading exists here in one of the other
           versions, so we need to attach this new reading to the same markers */
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi1,1);

        if (vmi2>vi2)
          {
            /* both events are within the same markers */

            vm1=(VariantMarkerEvent)v.getEvent(vmi1);
            vm2=(VariantMarkerEvent)v.getEvent(vmi2);

            Event selectedEvent=null;
            for (int i=vmi1+1; i<vmi2; i++) /* copy default reading into new reading */
              {
                Event newEventCopy=v.getEvent(i).createCopy();
                if (i==vi1)
                  selectedEvent=newEventCopy;
                vmd.replaceEvent(snum,vnum,i,newEventCopy);
                newReading.addEvent(newEventCopy);
              }

            vm1.addReading(newReading);
            return selectedEvent;
          }

        vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi2,-1);
        if (vmi1>vi1)
          {
            /* combine readings */
System.out.println("duplicateEventsInVariant: case not implemented (combine readings)");
            return null;
          }

        /* event 1 is currently in-between two variant markers;
           event 2 is not */

        /* remove end marker and reinsert after second event */
        vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi1,-1);
        vm1=(VariantMarkerEvent)v.getEvent(vmi1);
        vm2=(VariantMarkerEvent)v.getEvent(vmi2);
        deleteEvent(snum,vnum,vm2.getDefaultListPlace());
        vmd.deleteEvent(snum,vnum,vmi2);
        addEvent(snum,vnum,di2,vm2);
        vmd.addEvent(snum,vnum,vi2,vm2);

        /* duplicate last events in all readings */
        for (int i=vmi2; i<vm2.getListPlace(false); i++)
          for (VariantReading vr : vm1.getReadings())
            vr.addEvent(v.getEvent(i).createCopy());

        /* create new reading */
        for (int i=vm1.getListPlace(false)+1; i<vm2.getListPlace(false); i++)
          {
            Event newEventCopy=vmd.getSection(snum).getVoice(vnum).getEvent(i).createCopy();
            vmd.replaceEvent(snum,vnum,i,newEventCopy);
            newReading.addEvent(newEventCopy);
          }

        vm1.addReading(newReading);
        return v.getEvent(vm1.getListPlace(false)+1);
      }
    else
      {
        vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi2,-1);
        vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi2,-1);
        if (vmi1>vmi2)
          {
            /* event 2 is currently in-between two variant markers;
               event 1 is not */

            /* remove start marker and reinsert before first event */
            vmi1=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,vi1,1);
            vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi2,1);
            vm1=(VariantMarkerEvent)v.getEvent(vmi1);
            deleteEvent(snum,vnum,vm1.getDefaultListPlace());
            vmd.deleteEvent(snum,vnum,vmi1);
            addEvent(snum,vnum,di1,vm1);
            vmd.addEvent(snum,vnum,vi1,vm1);

            /* duplicate first events in all readings */
            for (int i=vmi1; i>vm1.getListPlace(false); i--) 
              for (VariantReading vr : vm1.getReadings())
                vr.addEvent(0,v.getEvent(i).createCopy());

            /* create new reading */
            for (int i=vm1.getListPlace(false)+1; i<vmi2; i++)
              {
                Event newEventCopy=vmd.getSection(snum).getVoice(vnum).getEvent(i).createCopy();
                vmd.replaceEvent(snum,vnum,i,newEventCopy);
                newReading.addEvent(newEventCopy);
              }
            vm1.addReading(newReading);
            return vmd.getSection(snum).getVoice(vnum).getEvent(vm1.getListPlace(false)+1);
          }
      }

    /* create new reading with new markers */
    for (int i=vi1; i<=vi2; i++)
      {
        Event newEventCopy=vmd.getSection(snum).getVoice(vnum).getEvent(i).createCopy();
        vmd.replaceEvent(snum,vnum,i,newEventCopy);
        newReading.addEvent(newEventCopy);
      }

    vm1=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_START);
    vm2=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_END);
    vm1.addReading(newReading);
    vm1.setDefaultLength(new Proportion(newReading.getLength()));
    vm2.setReadingsList(vm1.getReadings());
    vm2.setDefaultLength(vm1.getDefaultLength()); /* link default lengths in markers */
    addEvent(snum,vnum,di1,vm1);
    addEvent(snum,vnum,di2+2,vm2);

    vmd.addEvent(snum,vnum,vi1,vm1);
    vmd.addEvent(snum,vnum,vi2+2,vm2);

    return newReading.getEvent(0);
  }

/*------------------------------------------------------------------------
Method:  void moveEvent(int snum,int vnum,int i,int offset)
Purpose: Move event in one section and update parameters throughout score
Parameters:
  Input:  int snum,vnum - section and voice number
          int i         - index in voice's event list for event to move
          int offset    - amount to move event in voice list
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void moveEvent(int snum,int vnum,int i,int offset)
  {
    Event e=getSection(snum).getVoice(vnum).getEvent(i);
    deleteEvent(snum,vnum,i);
    addEvent(snum,vnum,i+offset,e);
  }

/*------------------------------------------------------------------------
Method:  void replaceEvent(int snum,int vnum,int i,Event newEvent)
Purpose: Replace event in one section and update parameters throughout score
Parameters:
  Input:  int snum,vnum  - section and voice number
          int i          - index in voice's event list for event to replace
          Event newEvent - replacement event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void replaceEvent(int snum,int vnum,int i,Event newEvent)
  {
    deleteEvent(snum,vnum,i);
    addEvent(snum,vnum,i,newEvent);
  }

/*------------------------------------------------------------------------
Method:  void combineReadingWithNext(PieceData vmd,int snum,int vnum,int VM1starti,int VM2starti)
Purpose: Combine one set of variant readings with following set in score
Parameters:
  Input:  PieceData vmd - variant music data
          int snum,vnum - section and voice number
          int VM1starti,
              VM2starti - index in event list of start marker of
                          each variant set
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void combineReadingWithNext(PieceData vmd,int snum,int vnum,int VM1starti,int VM2starti)
  {
    VoiceEventListData defaultV=getSection(snum).getVoice(vnum),
                       v=vmd.getSection(snum).getVoice(vnum);
    int                VM1endi=VM2starti-1,
                       VM2endi=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,VM2starti+1,1);
    VariantMarkerEvent vmStart1=(VariantMarkerEvent)v.getEvent(VM1starti),
                       vmStart2=(VariantMarkerEvent)v.getEvent(VM2starti),
                       vmEnd1=(VariantMarkerEvent)v.getEvent(VM1endi),
                       vmEnd2=(VariantMarkerEvent)v.getEvent(VM2endi);
    int defaultVM1starti=vmStart1.getDefaultListPlace(),
        defaultVM2starti=vmStart2.getDefaultListPlace(),
        defaultVM1endi=vmEnd1.getDefaultListPlace(),
        defaultVM2endi=vmEnd2.getDefaultListPlace();

    /* construct default event lists */
    VariantReading defaultReading1=new VariantReading(),
                   defaultReading2=new VariantReading();
    for (int i=defaultVM1starti+1; i<defaultVM1endi; i++)
      defaultReading1.addEvent(defaultV.getEvent(i));
    for (int i=defaultVM2starti+1; i<defaultVM2endi; i++)
      defaultReading2.addEvent(defaultV.getEvent(i));

    /* construct combined variant readings */
    ArrayList<VariantReading> newReadingList=new ArrayList<VariantReading>();
    for (VariantReading varReading1 : vmStart1.getReadings())
      for (VariantVersionData vvd : varReading1.getVersions())
        {
          VariantReading varReading2=vmStart2.getVariantReading(vvd),
                         newReading=new VariantReading();
          newReading.addVersion(vvd);
          newReading.addEventList(varReading1);
          newReading.addEventList(varReading2!=null ? varReading2 : defaultReading2);
          newReadingList.add(newReading);
        }
    for (VariantReading varReading2 : vmStart2.getReadings())
      for (VariantVersionData vvd : varReading2.getVersions())
        if (vmStart1.getVariantReading(vvd)==null)
          {
            VariantReading newReading=new VariantReading();
            newReading.addVersion(vvd);
            newReading.addEventList(defaultReading1);
            newReading.addEventList(varReading2);
            newReadingList.add(newReading);
          }

    /* combine duplicates in new reading list
       pre-condition: each reading in newReadingList is attached to only one version */
    int ri=0;
    while (ri<newReadingList.size())
      {
        VariantReading vr1=newReadingList.get(ri);
        int ri2=ri+1;
        while (ri2<newReadingList.size())
          {
            VariantReading vr2=newReadingList.get(ri2);
            if (vr1.eventsEqual(vr2))
              {
                vr1.addVersion(vr2.getVersion(0));
                newReadingList.remove(ri2);
              }
            else
              ri2++;
          }
        ri++;
      }

    vmStart1.setReadingsList(newReadingList);
    vmEnd2.setReadingsList(newReadingList);

    /* delete markers in middle of new combined default reading */
    deleteEvent(snum,vnum,defaultVM2starti);
    deleteEvent(snum,vnum,defaultVM1endi);
    if (vmd!=this)
      {
        vmd.deleteEvent(snum,vnum,VM2starti);
        vmd.deleteEvent(snum,vnum,VM1endi);

        /* replace voice list events with those in new reading */
        VariantReading newReading=vmStart1.getVariantReading(vmd.curVersion);
        if (newReading!=null)
          for (int ei=0; ei<newReading.getNumEvents(); ei++)
            vmd.replaceEvent(snum,vnum,VM1starti+1+ei,newReading.getEvent(ei));
      }
    vmStart1.setDefaultLength(Proportion.sum(defaultReading1.getLength(),defaultReading2.getLength()));
    vmEnd2.setDefaultLength(vmStart1.getLength());
  }

/*------------------------------------------------------------------------
Method:  boolean addVersionToReading(EventLocation loc,int ri,VariantVersionData newv)
Purpose: Associate one variant version with one reading
Parameters:
  Input:  EventLocation loc       - location of start marker
          int ri                  - index of reading within marker's list
          VariantVersionData newv - version to add to reading
  Output: -
  Return: true if anything has been changed
------------------------------------------------------------------------*/

  public boolean addVersionToReading(EventLocation loc,int ri,VariantVersionData newv)
  {
    VariantMarkerEvent vme1=(VariantMarkerEvent)getEvent(loc);
    VoiceEventListData defaultV=getSection(loc.sectionNum).getVoice(loc.voiceNum);

    if (vme1.getVariantReading(newv)!=null)
      return false; /* this version is already attached to a reading here */

    vme1.getReading(ri).addVersion(newv);
    return true;
  }

/*------------------------------------------------------------------------
Method:  boolean setReadingAsDefault(EventLocation loc,int ri)
Purpose: Set one reading as default, swapping out current default (or
         deleting if the current default is only in the default version)
Parameters:
  Input:  EventLocation loc - location of start marker
          int ri            - index of reading within marker's list
  Output: -
  Return: true if anything has been changed
------------------------------------------------------------------------*/

  public boolean setReadingAsDefault(EventLocation loc,int ri)
  {
    VariantMarkerEvent vme1=(VariantMarkerEvent)getEvent(loc);
    VoiceEventListData defaultV=getSection(loc.sectionNum).getVoice(loc.voiceNum);

System.out.println("Set reading as default");

    return true;
  }

/*------------------------------------------------------------------------
Method:  boolean consolidateReadings(EventLocation loc)
Purpose: Find identical variant readings at one location and combine
Parameters:
  Input:  EventLocation loc - location of start marker
  Output: -
  Return: true if anything has been changed
------------------------------------------------------------------------*/

  public void consolidateAllReadings()
  {
    EventLocation loc=new EventLocation(0,0,0);
    int           totalConsolidated=0;

    for (loc.sectionNum=0; loc.sectionNum<getNumSections(); loc.sectionNum++)
      {
        MusicSection ms=getSection(loc.sectionNum);
        for (loc.voiceNum=0; loc.voiceNum<ms.getNumVoices(); loc.voiceNum++)
          if (ms.getVoice(loc.voiceNum)!=null)
            {
              VoiceEventListData curv=ms.getVoice(loc.voiceNum);
              for (loc.eventNum=0; loc.eventNum<curv.getNumEvents(); loc.eventNum++)
                if (curv.getEvent(loc.eventNum).geteventtype()==Event.EVENT_VARIANTDATA_START)
                  totalConsolidated+=consolidateReadings(loc) ? 1 : 0;
            }
      }
  }

  public boolean consolidateReadings(EventLocation loc)
  {
    boolean changed=false;

    VariantMarkerEvent vme1=(VariantMarkerEvent)getEvent(loc);
    VoiceEventListData defaultV=getSection(loc.sectionNum).getVoice(loc.voiceNum);

    int vmi1=vme1.getDefaultListPlace(),
        vmi2=defaultV.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vmi1,1);

    VariantMarkerEvent vme2=(VariantMarkerEvent)(getSection(loc.sectionNum).getVoice(loc.voiceNum).getEvent(vmi2));

    /* check readings against default */
    LinkedList<VariantReading> delList=new LinkedList<VariantReading>();
    for (VariantReading vr : vme1.getReadings())
      if (vr.equals(defaultV,vmi1+1,vmi2-1))
        delList.add(vr);
    for (VariantReading vr : delList)
      {
        vme1.getReadings().remove(vr);
        variantReadings.remove(vr);
      }
    if (delList.size()>0)
      changed=true;

    /* check readings against each other */
    delList=new LinkedList<VariantReading>();
    for (int i1=0; i1<vme1.getNumReadings(); i1++)
      {
        VariantReading vr1=vme1.getReading(i1);
        for (int i2=i1+1; i2<vme1.getNumReadings(); i2++)
          {
            VariantReading vr2=vme1.getReading(i2);
            if (!delList.contains(vr2) &&
                vr1.equals(vr2.getEvents(),0,vr2.getNumEvents()-1) &&
                vr1.isError()==vr2.isError())
              {
                vr1.getVersions().addAll(vr2.getVersions());
                delList.add(vr2);
              }
          }
      }
    for (VariantReading vr : delList)
      {
        vme1.getReadings().remove(vr);
        variantReadings.remove(vr);
      }
    if (delList.size()>0)
      changed=true;

    if (vme1.getNumReadings()==0)
      {
        /* all readings have been eliminated; remove variant */
        deleteEvent(loc.sectionNum,loc.voiceNum,vmi2);
        deleteEvent(loc.sectionNum,loc.voiceNum,vmi1);
      }
    else
      vme2.setVarTypeFlags(vme1.calcVariantTypes(defaultV));

    return changed;
  }

/*------------------------------------------------------------------------
Method:  EventLocation findEvent(Event e)
Purpose: Find event
Parameters:
  Input:  Event e - event to seek
  Output: -
  Return: location information (section num, voice num, event index)
------------------------------------------------------------------------*/

  public EventLocation findEvent(Event e)
  {
    for (int si=0; si<getNumSections(); si++)
      {
        MusicSection s=getSection(si);
        for (int vi=0; vi<s.getNumVoices(); vi++)
          {
            VoiceEventListData v=s.getVoice(vi);
            if (v!=null)
              for (int ei=0; ei<v.getNumEvents(); ei++)
                if (v.getEvent(ei)==e)
                  return new EventLocation(si,vi,ei);
          }
      }

    return null;
  }

/*------------------------------------------------------------------------
Method:  void updateVariantVersions(ArrayList<VariantVersionData> newVariantVersions,
                                    ArrayList<Integer> originalVersionNums)
Purpose: Update versions list to match a new set
Parameters:
  Input:  ArrayList<VariantVersionData> newVariantVersions - new versions list
          ArrayList<Integer> originalVersionNums -           indices in current list of items in new list (-1 for new items)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void updateVariantVersions(ArrayList<VariantVersionData> newVariantVersions,
                                    ArrayList<Integer> originalVersionNums)
  {
    ArrayList<VariantVersionData> finalVersions=new ArrayList<VariantVersionData>(newVariantVersions.size());
    for (int vi=0; vi<newVariantVersions.size(); vi++)
      {
        int origi=originalVersionNums.get(vi).intValue();
        if (origi==-1)
          finalVersions.add(newVariantVersions.get(vi)); /* new item */
        else
          {
            /* update existing version, rather than replacing */
            VariantVersionData vvd=this.variantVersions.get(origi);
            vvd.copyData(newVariantVersions.get(vi));
            finalVersions.add(vvd);
          }
      }

    this.variantVersions=finalVersions;
  }

/*------------------------------------------------------------------------
Method:  boolean deleteOriginalText(VariantVersionData vvd,boolean delVoices[])
Purpose: Delete all original text in some voices in one version (creating
         variants against default version)
Parameters:
  Input:  VariantVersionData vvd - variant version
          boolean delVoices[]    - voices to delete (true = delete)
  Output: -
  Return: whether anything has been deleted
------------------------------------------------------------------------*/

  public boolean deleteOriginalText(VariantVersionData vvd,boolean delVoices[])
  {
    boolean modified=false;

    PieceData vmd=vvd.constructMusicData(this);

    for (int vnum=0; vnum<delVoices.length; vnum++)
      if (delVoices[vnum] && !vvd.getMissingVoices().contains(getVoice(vnum)))
        for (int snum=0; snum<getNumSections(); snum++)
          {
            VoiceEventListData v=vmd.getSection(snum).getVoice(vnum);
            if (v!=null && !v.getMissingVersions().contains(vvd))
              {
                int ei=0;
                while (ei<v.getNumEvents())
                  {
                    Event e=v.getEvent(ei);
                    if (e.geteventtype()==Event.EVENT_ORIGINALTEXT)
                      {
                        if (deleteEventInVersion(vvd,vmd,snum,vnum,ei)==VariantReading.NEWVARIANT)
                          ei++;
                        modified=true;
                      }

                    ei++;
                  }
              }
          }

    return modified;
  }

/*------------------------------------------------------------------------
Method:  boolean setVersionTextAsDefault(VariantVersionData textVersion)
Purpose: Set original text of one version as the default (moving current
         default text to other matching variant versions)
Parameters:
  Input:  VariantVersionData textVersion - variant version for default text
  Output: -
  Return: whether anything has been modified
------------------------------------------------------------------------*/

  public boolean setVersionTextAsDefault(VariantVersionData textVersion)
  {
    boolean modified=false;

    /* step 1: disconnect all original text from default version */
    for (int si=0; si<getNumSections(); si++)
      {
        MusicSection s=getSection(si);
        for (int vi=0; vi<s.getNumVoices(); vi++)
          {
            VoiceEventListData v=s.getVoice(vi);
            if (v!=null)
              {
                int ei=0;
                while (ei<v.getNumEvents())
                  {
                    Event e=v.getOrigTextOnlyVariant(ei);
                    if (e!=null)
                      {
                        VariantMarkerEvent vme=(VariantMarkerEvent)(v.getEvent(ei));
                        for (VariantVersionData vvd : vme.getDefaultVersions(getVariantVersions()))
                          if (!vvd.isDefault())
                            duplicateEventInVariant(vvd,this,si,vi,ei+1);
                        deleteEvent(si,vi,ei+1);
                      }
                    ei++;
                  }
              }
          }
      }

    /* step 2: place current version's text in default */
    PieceData vmd=textVersion.constructMusicData(this);
    for (int si=0; si<getNumSections(); si++)
      {
        MusicSection s=vmd.getSection(si);
        for (int vi=0; vi<s.getNumVoices(); vi++)
          {
            VoiceEventListData v=s.getVoice(vi);
            if (v!=null)
              {
                int ei=0;
                while (ei<v.getNumEvents())
                  {
                    Event e=v.getOrigTextOnlyVariant(ei);
                    if (e!=null)
                      {
                        VariantMarkerEvent vme=(VariantMarkerEvent)(v.getEvent(ei));
                        separateDefaultReading(si,vi,vme);
                        addEvent(si,vi,vme.getDefaultListPlace()+1,e);
                      }
                    ei++;
                  }
              }
          }
      }

    return true; //modified;
  }

/*------------------------------------------------------------------------
Method:  boolean setVersionAsDefault(VariantVersionData newDefaultVersion)
Purpose: Set one version's readings as default
Parameters:
  Input:  VariantVersionData newDefaultVersion - variant version for default
  Output: -
  Return: whether anything has been modified
------------------------------------------------------------------------*/

  public boolean setVersionAsDefault(VariantVersionData newDefaultVersion)
  {
    boolean modified=false;

    for (int si=0; si<getNumSections(); si++)
      {
        MusicSection s=getSection(si);
        for (int vi=0; vi<s.getNumVoices(); vi++)
          {
            VoiceEventListData v=s.getVoice(vi);
            if (v!=null)
              {
                int ei=0;
                while (ei<v.getNumEvents() && ei>=0)
                  {
                    ei=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,ei,1);
                    if (ei!=-1)
                      {
                        VariantMarkerEvent vme=(VariantMarkerEvent)(v.getEvent(ei));
                        int vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,ei+1,1);

                        List defaultVersions=vme.getDefaultVersions(getVariantVersions());
                        if (defaultVersions.size()==1 || // skip if default is unique
                            defaultVersions.contains(newDefaultVersion))
                          ei=vmi2+1;
                        else
                          {
                            separateDefaultReading(si,vi,vme);

                            /* delete default events */
                            for (int dei=vmi2-1; dei>ei; dei--)
                              deleteEvent(si,vi,dei);

                            /* copy new events into default */
                            VariantReading vr=vme.getVariantReading(newDefaultVersion);
                            int listPlace=vme.getDefaultListPlace()+1;
                            for (Event e : vr.getEvents().getEvents())
                              addEvent(si,vi,listPlace++,e);
                          }
                        ei=v.getNextEventOfType(Event.EVENT_VARIANTDATA_START,ei+1,1);
                      }
                  }
              }
          }
      }

    return true; //modified;
  }
  
  void separateDefaultReading(int snum,int vnum,VariantMarkerEvent vme)
  {
    VoiceEventListData v=getSection(snum).getVoice(vnum);
    int                vi=vme.getDefaultListPlace()+1;

    /* create new variant */
    VariantReading newReading=new VariantReading();
    for (VariantVersionData vvd : vme.getDefaultVersions(getVariantVersions()))
      if (!vvd.isDefault())
        newReading.addVersion(vvd);
    variantReadings.add(newReading);

    int vmi2=v.getNextEventOfType(Event.EVENT_VARIANTDATA_END,vi,1);

    Event selectedEvent=null;
    for (int i=vi; i<vmi2; i++) /* copy default reading into new reading */
      {
        Event newEventCopy=v.getEvent(i).createCopy();
        if (i==vi)
          selectedEvent=newEventCopy;
//            vmd.replaceEvent(snum,vnum,i,newEventCopy);
        newReading.addEvent(newEventCopy);
      }

    if (newReading.getVersions().size()>0)
      vme.addReading(newReading);
  }

/*------------------------------------------------------------------------
Method:  String voiceOrigTextToStr(int vnum)
Purpose: Create string containing all original texting in one voice
Parameters:
  Input:  int vnum - number of voice
  Output: -
  Return: String containing all text in voice, phrases/syllables delimited
          by markers according to style (original or modern)
------------------------------------------------------------------------*/

  public String voiceOrigTextToStr(int vnum)
  {
    return voiceTextToStr(vnum,false);
  }

  public String voiceModTextToStr(int vnum)
  {
    return voiceTextToStr(vnum,true);
  }

  public String voiceTextToStr(int vnum,boolean modText)
  {
    String text="";
    for (MusicSection ms : musicSections)
      if (ms.getVoice(vnum)!=null)
        {
          String sectionText=modText ?
            ms.getVoice(vnum).modTextToStr() :
            ms.getVoice(vnum).origTextToStr();
          if (sectionText.length()>0)
            text+=(text.length()>0 ? '\n' : "")+sectionText;
        }
    return text;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public PieceData getDefaultMusicData()
  {
    return defaultMusicData;
  }

  public String getTitle()
  {
    return title;
  }

  public String getFullTitle()
  {
    return fullTitle;
  }

  public String getSectionTitle()
  {
    return section;
  }

  public String getComposer()
  {
    return composer;
  }

  public String getEditor()
  {
    return editor;
  }

  public String getNotes()
  {
    return notes;
  }

  public String getPublicNotes()
  {
    return publicNotes;
  }

  public Coloration getBaseColoration()
  {
    return baseColoration;
  }

  public Mensuration getBaseMensuration()
  {
    return baseMensuration;
  }

  public Event getEvent(EventLocation loc)
  {
    return getEvent(loc.sectionNum,loc.voiceNum,loc.eventNum);
  }

  public Event getEvent(int snum,int vnum,int evnum)
  {
    return getSection(snum).getVoice(vnum).getEvent(evnum);
  }

  public Voice getVoice(int vnum)
  {
    return voiceData[vnum];
  }

  public Voice[] getVoiceData()
  {
    return voiceData;
  }

  public int getNumSections()
  {
    return musicSections.size();
  }

  public MusicSection getSection(int sectionNum)
  {
    return musicSections.get(sectionNum);
  }

  public ArrayList<MusicSection> getSections()
  {
    return musicSections;
  }

  public VariantReading getVariantReading(int snum,int vnum,int eventnum,VariantVersionData version)
  {
    return getSection(snum).getVoice(vnum).getEvent(eventnum).getVariantReading(version);
  }

  public VariantVersionData getDefaultVariantVersion()
  {
    return getVariantVersion(0);
  }

  public VariantVersionData getVariantVersion(int versionNum)
  {
    return variantVersions.get(versionNum);
  }

  public VariantVersionData getVariantVersion(String versionID)
  {
    for (VariantVersionData vvd : variantVersions)
      if (vvd.getID().equals(versionID))
        return vvd;
    return null;
  }

  public ArrayList<String> getVariantVersionNames()
  {
    ArrayList<String> versionNames=new ArrayList<String>(variantVersions.size());
    for (VariantVersionData vvd : variantVersions)
      versionNames.add(vvd.getID());
    return versionNames;
  }

  public ArrayList<VariantVersionData> getVariantVersions()
  {
    return variantVersions;
  }

  public VariantVersionData getVersion()
  {
    return curVersion;
  }

  public boolean isDefaultVersion()
  {
    return curVersion==null;
  }

  public boolean isIncipitScore()
  {
    return isIncipit;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setBaseColoration(Coloration c)
  {
    baseColoration=c;
  }

  public void setCurVersion(VariantVersionData version)
  {
    this.curVersion=version;
  }

  public void setIncipitScore(boolean i)
  {
    isIncipit=i;
    createFullTitle();
  }

  public void setSections(ArrayList<MusicSection> musicSections)
  {
    this.musicSections=musicSections;
  }

  public void setVariantVersions(ArrayList<VariantVersionData> variantVersions)
  {
    this.variantVersions=variantVersions;
  }

  public void setVoiceData(Voice[] vd)
  {
    voiceData=vd;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this structure
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("Title:    "+title);
    if (section!=null)
      System.out.println("Section:  "+section);
    System.out.println("Composer: "+composer);
    System.out.println("Editor:   "+editor);
    System.out.println("Number of voices: "+voiceData.length);
    for (int i=0; i<voiceData.length; i++)
      voiceData[i].prettyprint();
  }
}
