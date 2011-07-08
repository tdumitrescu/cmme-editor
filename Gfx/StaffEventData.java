/*----------------------------------------------------------------------*/
/*

        Module          : StaffEventData

        Package         : Gfx

        Classes Included: StaffEventData

        Purpose         : Hold event locations for one unscored staff

        Programmer      : Ted Dumitrescu

        Date Started    : 7/05 (moved to separate file 3/27/06)

        Updates         :
11/28/08: created versions of addlig() which take RenderLists instead of
          EventListData (to render properly when events have been added
          during the rendering phase)

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   StaffEventData
Extends: Gfx.RenderList
Purpose: Structure containing event locations for one staff
------------------------------------------------------------------------*/

public class StaffEventData extends RenderList
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public ArrayList<RenderedEventGroup> eventgroups; /* index list of event groups */
  int                                  incipitEndGroupIndex; /* group index for end of incipit */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: StaffEventData(OptionSet o,Voice v,MusicSection section)
Purpose:     Initialize rendering structure
Parameters:
  Input:  OptionSet o          - rendering options
          Voice v              - current voice being rendered
          MusicSection section - section data
  Output: -
------------------------------------------------------------------------*/

  public StaffEventData(OptionSet o,Voice v,MusicSection section)
  {
    super(o,v,section);
    eventgroups=new ArrayList<RenderedEventGroup>();
    incipitEndGroupIndex=-1;
  }

  public StaffEventData(Voice v,MusicSection section)
  {
    this(OptionSet.makeDEFAULT_ORIGINAL(null),v,section);
  }

  public StaffEventData()
  {
    this(null,null);
  }

  public RenderedEventGroup getEventgroup(int i)
  {
    return (RenderedEventGroup)(eventgroups.get(i));
  }

/*------------------------------------------------------------------------
Method:  int calcEventSpace([,int starti,int endi])
Purpose: Calculate x-space taken up by rendered events on staff
Parameters:
  Input:  int starti,endi       - start and end group indices for summation
  Output: -
  Return: total x-space
------------------------------------------------------------------------*/

  public int calcEventSpace(int starti,int endi)
  {
    RenderedEventGroup eg;
    int                totaleventspace=0;

    for (int i=starti; i<=endi; i++)
      {
        eg=getEventgroup(i);
        totaleventspace+=getgrouprenderedxsize(eg);
      }
    return totaleventspace;
  }

  public int calcEventSpace()
  {
    return calcEventSpace(0,eventgroups.size()-1);
  }

  public int getgrouprenderedxsize(RenderedEventGroup eg)
  {
    int           totalsize=0,lastx=0;
    RenderedEvent cure;
    for (int i=eg.firstEventNum; i<=eg.lastEventNum; i++)
      {
        cure=getEvent(i);
        if (eg.grouptype!=RenderedEventGroup.EVENTGROUP_LIG || /* within a ligature, */
            cure.getEvent().geteventtype()==Event.EVENT_NOTE)  /* only notes take up space */
          {
            lastx=totalsize;
            totalsize+=cure.getrenderedxsize();
          }
      }
    return totalsize;
  }

  public void setgroupxlocs(RenderedEventGroup eg,double x)
  {
    double lastx=x;
    for (int i=eg.firstEventNum; i<=eg.lastEventNum; i++)
      {
        RenderedEvent re=getEvent(i);
        if (eg.grouptype!=RenderedEventGroup.EVENTGROUP_LIG || /* within a ligature, */
            re.getEvent().hasEventType(Event.EVENT_NOTE))      /* only notes take up space */
          {
            lastx=x;
            re.setxloc(x);
            x+=re.getrenderedxsize();
          }
        else
          re.setxloc(lastx+MusicFont.getDefaultGlyphWidth(MusicFont.PIC_NOTESTART+NoteEvent.NOTEHEADSTYLE_BREVE));
      }
  }

/*------------------------------------------------------------------------
Method:  double padEvents(double paddingspace[,int starti,int endi,double curx])
Purpose: Set event group x-locations, given a padding value (space between
         groups)
Parameters:
  Input:  double paddingspace   - x-space to place between groups
          int starti,endi       - start and end group indices for operation
          double curx           - starting x-value
  Output: events
  Return: total x-space
------------------------------------------------------------------------*/

  public double padEvents(double paddingspace,int starti,int endi,double curx)
  {
    RenderedEventGroup eg;
    for (int i=starti; i<=endi; i++)
      {
        eg=getEventgroup(i);
        setgroupxlocs(eg,curx);
        curx+=getgrouprenderedxsize(eg)+paddingspace;
      }

    return curx;
  }

  public double padEvents(double paddingspace)
  {
    return padEvents(paddingspace,0,eventgroups.size()-1,0);
  }

  /* should this event be grouped with the last one? e.g., dot + note */
  public boolean groupwithprevious(Event e)
  {
    if (eventgroups.size()<=0)
      return false;
    if (e.alignedWithPrevious())
      return true;

    Event laste=getEvent(this.size()-1).getEvent();
    switch (e.geteventtype())
      {
        case Event.EVENT_DOT:
          if ((((DotEvent)e).getdottype()&DotEvent.DT_Addition)!=0)
            return true;
          break;
      }
    if (e.hasEventType(Event.EVENT_CLEF) &&
        e.getClefSet()!=null && laste.getClefSet()!=null &&
        e.getClefSet().containsClef(laste.getClefSet().getprincipalclef()))
      return true;

    switch (laste.geteventtype())
      {
        case Event.EVENT_ANNOTATIONTEXT:
        case Event.EVENT_COLORCHANGE:
        case Event.EVENT_MODERNKEYSIGNATURE:
        case Event.EVENT_ORIGINALTEXT:
        case Event.EVENT_PROPORTION:
        case Event.EVENT_VARIANTDATA_START:
        case Event.EVENT_VARIANTDATA_END:
          return true;
      }

    return false;
  }

/*------------------------------------------------------------------------
Method:  boolean isEllipsis(RenderedEventGroup eg)
Purpose: Is this event group an Ellipsis event (end of incipit)?
Parameters:
  Input:  RenderedEventGroup eg - event group to check
  Output: -
  Return: whether this is an Ellipsis event
------------------------------------------------------------------------*/

  public boolean isEllipsis(RenderedEventGroup eg)
  {
    return getEvent(eg.firstEventNum).getEvent().geteventtype()==Event.EVENT_ELLIPSIS;
  }

  /* override RenderList functions to incorporate groups */
  public RenderedEvent addevent(boolean display,Event e,RenderParams rp)
  {
    if (e.geteventtype()==Event.EVENT_ELLIPSIS)
      incipitEndGroupIndex=eventgroups.size();
    if (groupwithprevious(e))
      /* add to end of last group */
      getEventgroup(eventgroups.size()-1).lastEventNum=this.size();
    else
      eventgroups.add(new RenderedEventGroup(this.size()));
    return super.addevent(display,e,rp);
  }

  public int addlig(EventListData v,int evnum,RenderParams rp)
  {
    return addlig(v,evnum,rp,false);
  }

  public int addlig(EventListData v,int evnum,RenderParams rp,boolean varDisplay)
  {
    boolean gwp=groupwithprevious(v.getEvent(evnum));
    int     ligsize=super.addlig(v,evnum,rp,varDisplay);

    if (gwp)
      {
        RenderedEventGroup lastGroup=getEventgroup(eventgroups.size()-1);
        lastGroup.lastEventNum=this.size()-1;
        lastGroup.grouptype=RenderedEventGroup.EVENTGROUP_LIG;
      }
    else
      eventgroups.add(new RenderedEventGroup(this.size()-ligsize,this.size()-1));
    return ligsize;
  }

  public int addlig(RenderList rl,int revnum,RenderParams rp)
  {
    return addlig(rl,revnum,rp,false);
  }

  EventListData createLigList(RenderList rl,int revnum)
  {
    EventListData ligList=new EventListData();

    Event   e=rl.getEvent(revnum).getEvent();
    boolean done=false;
    while (!done)
      {
        ligList.addEvent(e);
        NoteEvent ne=(NoteEvent)e.getFirstEventOfType(Event.EVENT_NOTE);
        if (ne!=null && !ne.isligated())
          done=true;
        else
          e=rl.getEvent(++revnum).getEvent();
      }

    return ligList;
  }

  public int addlig(RenderList rl,int revnum,RenderParams rp,boolean varDisplay)
  {
    boolean gwp=groupwithprevious(rl.getEvent(revnum).getEvent());

    EventListData ligList=createLigList(rl,revnum);
    int ligsize=super.addlig(ligList,0,rp,varDisplay);

    if (gwp)
      {
        RenderedEventGroup lastGroup=getEventgroup(eventgroups.size()-1);
        lastGroup.lastEventNum=this.size()-1;
        lastGroup.grouptype=RenderedEventGroup.EVENTGROUP_LIG;
      }
    else
      eventgroups.add(new RenderedEventGroup(this.size()-ligsize,this.size()-1));
    return ligsize;
  }

  public int addclefgroup(VoiceEventListData v,int cenum,RenderParams rp)
  {
    if (cenum==-1)
      return 0;

    int     i=cenum;
    Event   e=v.getEvent(i);
    ClefSet origcs=null;
    Clef    origClef=null;

    if (e==null || !e.hasSignatureClef())
      return 0;
    origcs=e.getClefSet(options.get_usemodernclefs());
    origClef=origcs.getprincipalclef();
    boolean done=false;
    while (!done)
      {
        addevent(true,e,rp);
        e=v.getEvent(++i);
        if (e==null || !e.hasSignatureClef() || !e.getClefSet(options.get_usemodernclefs()).containsClef(origClef))
          done=true;
      }
    return i-cenum;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int getIncipitEndGroupIndex()
  {
    if (incipitEndGroupIndex==-1)
      return eventgroups.size();
    else
      return incipitEndGroupIndex;
  }

  public int getNextEventWithType(int eventType,int i,int dir)
  {
    for (; i>=0 && i<this.size(); i+=dir)
      if (getEvent(i).getEvent().hasEventType(eventType))
        return i;
    return -1;
  }
}

