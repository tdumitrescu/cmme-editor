/*----------------------------------------------------------------------*/
/*

        Module          : ScoreRenderer.java

        Package         : Gfx

        Classes Included: ScoreRenderer,VoiceGfxInfo

        Purpose         : "Prerenders" music events of one mensural music
                          section into scored measure array to facilitate quick
                          random access display

        Programmer      : Ted Dumitrescu

        Date Started    : 4/23/99

Updates:
3/21/05:  added ligature pitch information (for calculating height)
3/23/05:  fixed bug causing ending dot to add an extra measure
4/5/05:   modified rendering cycle to ensure that new untimed events are added to
          the end of the previous measure, rather than the beginning of a new one
4/11/05:  implemented non-displaying rendered events and new insertions into
          render list (for automated key signature conversion routines)
5/18/05:  automatically adds PIECEEND event to the end of every voice's event
          list (to simplify editor functions) - 6/05 moved to parser code
6/16/05:  timed events can now take extra space if their image size is larger
          than the time-based x space allocation (e.g., for semifusae)
          corrected precision for x-coordinates (replaced ints with doubles)
7/21/05:  renamed from MusicRenderer to ScoreRenderer (to differentiate from
          rendering into separate parts)
9/9/05:   implemented arbitrary rhythmic proportional transformation
9/19/05:  converted musictime tally from double to Proportion (to avoid
          imprecision errors)
9/24/05:  added support for simultaneous events at one x-location in one voice
          (XPOS_SIMULTANEOUS event positioning)
2/24/06:  converted from breve- to minim-based timekeeping (for simplification
          of mensuration comparisons, e.g., combining proportions with multiple
          mensurations)
3/15/06:  started modifying renderer to support special layout of incipit-scores:
          incipit - blank space (ellipsis) - explicit
4/5/06:   current ligature information is now held in a separate structure
          (RenderedLigature)
8/06:     fixed various clef-replacement problems (e.g., skipped events still
          being used to set voice parameters; editorial section brackets still
          being displayed when all contents have been skipped)
11/10/06: added position type XPOS_WITHNEXT for rendering events which are
          always in the same measure as the next event (e.g., OriginalText)
2/28/06:  now renders one mensural section instead of entire piece
4/26/07:  added RenderedSectionParams to pass voice parameters (clefs, mens,
          etc.) between sections
1/08:     added respacing functions for rhythmic-error variants
2/13/08:  fixed rendering crash with certain variant-respacing situations
2/16/08:  fixed error where advanceOneMeasure doesn't get called for final
          measure in section (introduced by mods to renderAllMensuralEvents
          for variant-respacing)
8/4/08:   migrated to two-stage modularized rendering process:
          1. create rendered event lists with replacements/insertions/deletions
             as necessary for display (according to selected options)
          2. assign x-positions to all rendered events to align in score
12/27/08: improved dot-spacing so that variant markers/original text/etc
          don't push dots far to the right
1/12/09:  fixed WITHNEXT texting so OriginalText event appears in same
          measure as next visible event even when inside variant
3/20/09:  finished implementing variant text display (showing variant texting
          now yields the same display whether in a default or non-default
          version)
9/24/09:  fixed spacing bug when rhythmic-error variant is within proportional
          section

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.FontMetrics;
import java.util.*;

import DataStruct.*; /* music data structures */

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   VoiceGfxInfo
Extends: -
Purpose: Structure containing data for handling one voice's graphics
         during rendering
------------------------------------------------------------------------*/

class VoiceGfxInfo
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public ScoreRenderer renderer;

  public VoiceEventListData v;         /* event list info */
  public int                voicenum;  /* voice number */
  public VoiceGfxInfo       next,last; /* for voice list;
                                          voices are always sorted in this list by
                                          current musical time (musictime) */

  public Event            clefedata;     /* event data for current clef */
  public RenderedClefSet  clefEvents;    /* current clef set */
  public RenderedEvent    mensEvent;     /* current mensuration sign */
  public Proportion       curProportion, /* currently applied rhythmic proportion */
                          tempoProportion; /* tempo/visual proportion */
  public Coloration       curColoration; /* currently applied coloration scheme */
  public boolean          inEditorialSection; /* whether current events are editorial */
  public boolean          missingInVersion;   /* whether current events are missing in version being rendered */
  public RenderedEvent    lastNoteEvent;      /* previous note event (if relevant) */
  public RenderedEvent    curSoundingEvent;   /* note currently sounding */
  public ArrayList<Event> replacementEvents;  /* extra events to be inserted into render list */
  public int              withnextEvents;     /* number of XPOS_WITHNEXT events to add with next rendered event */
  public int              lastTimedEventNum;  /* index of last timed event */
  public boolean          immediatePositioning; /* true if temporarily forcing every
                                                   event to be XPOS_IMMEDIATE */

  public Proportion musictime;     /* current time position of voice */
  public double     xloc,          /* current graphical x position */
                    xadd,          /* used in temporary position calculations */
                    lastx;         /* starting x coord of last displayed event */
  public int        evloc,         /* current voice position in event list */
                    revloc;        /* current voice position in rendered event list */

  public RenderedLigature   ligInfo,        /* current ligature info */
                            tieInfo;        /* tied note info */
  public RenderedEventGroup varReadingInfo; /* current variant reading start/end */
  public Proportion         varDefaultTimeAdd; /* default length when variant is shroter than default */
  public boolean            respaceAfterVar;   /* whether to respace after variant */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VoiceGfxInfo(ScoreRenderer renderer,VoiceMensuralData voice)
Purpose:     Initialize structure
Parameters:
  Input:  ScoreRenderer renderer  - renderer to which this voice belongs
          VoiceMensuralData voice - data for this structure's voice
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public VoiceGfxInfo(ScoreRenderer renderer,VoiceEventListData voice)
  {
    this.renderer=renderer;
    v=voice;
  }

/*------------------------------------------------------------------------
Method:  RenderedEvent getlastvisibleEvent(RenderList rl,int el)
Purpose: Find previous displayed event at a given index
Parameters:
  Input:  RenderList rl - rendered event list for this voice
          int el        - index of event to start search
  Output: -
  Return: last displayed event
------------------------------------------------------------------------*/

  RenderedEvent getlastvisibleEvent(RenderList rl,int el)
  {
    int i;
    for (i=el-1; i>=0 && rl.getEvent(i).getrenderedxsize()==0; i--);
    return rl.getEvent(i);
  }

/*------------------------------------------------------------------------
Method:  double movebackNOSPACEEvents(RenderList rl,int el,double xamount)
Purpose: Try to push back x-locations of NOSPACE events at a given index
         (to make room for a new NOSPACE event)
Parameters:
  Input:  RenderList rl  - rendered event list for this voice
          int el         - index of event to start 
          double xamount - amount to try to push back events
  Output: -
  Return: new x-location after current event
------------------------------------------------------------------------*/

  double movebackNOSPACEEvents(RenderList rl,int el,double xamount)
  {
    if (el<0)
      return 0;
    RenderedEvent re=rl.getEvent(el);

    if (el>0 &&
        renderer.getxspacing(re.getEvent())==ScoreRenderer.XSPACING_NOSPACE)
      {
        double curxl=movebackNOSPACEEvents(rl,el-1,xamount);
        if (curxl<re.getxloc()-xamount)
          curxl=re.getxloc()-xamount;
        if (renderer.getXPosType(re.getEvent())==ScoreRenderer.XPOS_SIMULTANEOUS)
          curxl=rl.getEvent(el-1).getxloc();
        re.setxloc(curxl);
      }

    return re.getxend();
  }

/*------------------------------------------------------------------------
Method:  void moveupBEFORENEXTEvents(RenderList rl,double xamount)
Purpose: Try to push up x-locations of BEFORENEXT events at current location
         (if the measure has increased in size after the events were already
         rendered)
Parameters:
  Input:  RenderList rl  - rendered event list for this voice
          double xamount - amount to push up events
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void moveupBEFORENEXTevents(RenderList rl,double xamount)
  {
    if (musictime.i1==0) /* hack: don't apply to initial clefs/mensurations */
      return;

    boolean done=false;
    for (int el=revloc-1; el>=0 && !done; el--)
      {
        if (el<0)
          done=true;
        else
          {
            RenderedEvent re=rl.getEvent(el);
            if (renderer.getXSpacing(re)==ScoreRenderer.XSPACING_NOSPACE &&
                renderer.getXPosType(re)==ScoreRenderer.XPOS_BEFORENEXT)
              re.setxloc(re.getxloc()+xamount);
            else
              done=true;
          }
      }
  }

/*------------------------------------------------------------------------
Method:  double calcXLoc(RenderList rl,RenderedEvent e)
Purpose: Calculate x location for a rendered event
Parameters:
  Input:  RenderedEvent e - event
          RenderList rl   - rendered event list for this voice
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public double calcXLoc(RenderList rl,RenderedEvent re)
  {
    double retval;

    /* XPOS_SIMULTANEOUS events (vertically aligned with previous event) */
    if (revloc>0 && renderer.getXPosType(re)==ScoreRenderer.XPOS_SIMULTANEOUS)
      retval=lastx;

    /* XPOS_IMMEDIATE events (e.g., dots of addition) */
    else if (revloc>0 &&
             (renderer.getXPosType(re)==ScoreRenderer.XPOS_IMMEDIATE ||
              this.immediatePositioning))
      retval=lastx+getlastvisibleEvent(rl,revloc).getRenderedXSizeWithoutText();

    /* XSPACING_NOSPACE events (e.g., clefs) */
    else if (renderer.getXSpacing(re)==ScoreRenderer.XSPACING_NOSPACE)
      {
        retval=movebackNOSPACEEvents(rl,revloc-1,re.getrenderedxsize());
        if (retval<xloc-re.getrenderedxsize())
          retval=xloc-re.getrenderedxsize();
      }

    else
      retval=xloc;

    return retval<0 ? 0 : retval;
  }
}


/*------------------------------------------------------------------------
Class:   ScoreRenderer
Extends: -
Purpose: Prerenders music
------------------------------------------------------------------------*/

public class ScoreRenderer
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static double BARLINE_XADD=4; /* amount of blank space at measure beginning
                                   to accommodate barlines */
  static final double CHANT_XPADDING_B=      13, /* space between symbols in chant sections */
                      CHANT_XPADDING_SB=     3,
                      CHANT_XPADDING_DOT=    3,
                      CHANT_XPADDING_DEFAULT=3;

  public static final double SECTION_END_SPACING=10;

  /* event x positioning relative to previous events */
  public static int XPOS_BEFORENEXT=  0, /* immediately before next event */
                    XPOS_IMMEDIATE=   1, /* immediately after previous event */
                    XPOS_HALF=        2, /* halfway between events */
                    XPOS_SIMULTANEOUS=3, /* aligned vertically with previous event */
                    XPOS_WITHNEXT=    4, /* aligned vertically with next event */
                    XPOS_INVISIBLE=   5; /* not displayed, can be pushed around */

  /* x space taken by an event */
  public static int XSPACING_TIMED=  0, /* x space relative to musical time */
                    XSPACING_UNTIMED=1, /* x space based only on image size */
                    XSPACING_NOSPACE=2; /* try not to take any extra space */

/*----------------------------------------------------------------------*/
/* Instance variables */

  public MeasureList                 measures;
  public RenderList                  eventinfo[];  /* rendered event information */
  public ArrayList<RenderedSonority> sonorityList; /* sonority information for entire score */

  PieceData               fullPieceData;
  int                     sectionNum;
  MusicSection            musicData;
  OptionSet               options;
  RenderedSectionParams[] startingParams,endingParams;

  /* music display parameters */
  /* default amount of horizontal space taken by one breve */
  double       MINIMSCALE,BREVESCALE;
  int          numVoices;
  VoiceGfxInfo voicegfx[],
               liststart;
  double       startX;

  /* rendering parameters */
  int              curMeasureNum;
  MeasureInfo      curmeasure;
  double           barxstart;
  Proportion       starttime;
  int              skipevents; /* number of events to 'skip' displaying */
  Proportion       errorRespacingTime; /* for re-spacing so that variant rhythmic
                                          errors don't invalidate the rest of a
                                          voice */
  VoiceGfxInfo     variantVoice;       /* voice with variant causing respacing */
  RenderedSonority curSonority;

  LinkedList<VoiceGfxInfo> finalisList; /* for incipit-scores; displaying explicits
                                           after ellipses */

  /* mensuration parameters */
  public Mensuration baseMensuration;
  public int         numMinimsInBreve;
  Mensuration        lastMens; /* for determining whether multiple voices are
                                  changing mensuration simultaneously */
  Proportion         lastMensTime;

  /* options */
  int     barline_type,
          noteShapeType;
  boolean useModernClefs,
          useModernAccSystem,
          displayallnewlineclefs,
          displayVarTexts;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  ScoreRenderer[] renderSections(PieceData musicToRender,OptionSet options)
Purpose: Render a complete set of music sections, creating renderers as
         necessary
Parameters:
  Input:  PieceData musicToRender - music data to be rendered
          OptionSet options       - display options
  Output: -
  Return: array of score renderers with rendered music section by section
------------------------------------------------------------------------*/

  public static ScoreRenderer[] renderSections(PieceData musicToRender,OptionSet options)
  {
    double startX=0;

    /* initialize voice parameters */
    int numVoices=musicToRender.getVoiceData().length;
    RenderedSectionParams[] sectionParams=new RenderedSectionParams[numVoices];
    for (int i=0; i<numVoices; i++)
      sectionParams[i]=new RenderedSectionParams();

    /* initialize sections */
    int numSections=musicToRender.getNumSections();
    ScoreRenderer[] renderedSections=new ScoreRenderer[numSections];
    int nummeasures=0;
    for (int i=0; i<numSections; i++)
      {
        renderedSections[i]=new ScoreRenderer(i,musicToRender.getSection(i),musicToRender,
                                              sectionParams,
                                              options,nummeasures,startX);
        sectionParams=renderedSections[i].getEndingParams();
        nummeasures+=renderedSections[i].getNumMeasures();
        startX+=renderedSections[i].getXsize()+SECTION_END_SPACING;
      }

    return renderedSections;
  }

/*------------------------------------------------------------------------
Method:  int calcRendererNum(ScoreRenderer[] renderedSections,int m)
Purpose: Calculate index of (rendered) section within an array containing
         a given measure
Parameters:
  Input:  ScoreRenderer[] renderedSections - section array
          int m                            - measure number
  Output: -
  Return: section number
------------------------------------------------------------------------*/

  public static int calcRendererNum(ScoreRenderer[] renderedSections,int m)
  {
    for (int si=0; si<renderedSections.length; si++)
      if (m<=renderedSections[si].getLastMeasureNum())
        return si;

    return -1; /* error: m>number of measures in piece */
  }

/*------------------------------------------------------------------------
Method:  int getxspacing(Event e)
Purpose: Calculate x-spacing type for a given event
Parameters:
  Input:  -
  Output: -
  Return: XSPACING type
------------------------------------------------------------------------*/

  static int getxspacing(Event e)
  {
    if (e.getmusictime().i1>0)
      return XSPACING_TIMED;
    else
      return XSPACING_NOSPACE;
  }

  static int getXSpacing(RenderedEvent re)
  {
    return getxspacing(re.getEvent());
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ScoreRenderer(int sectionNum,MusicSection ms,PieceData fullPieceData,
                           RenderedSectionParams[] rsp,
                           OptionSet o,int fmn,double sx)
Purpose:     Initialize renderer
Parameters:
  Input:  int sectionNum              - section number
          MusicSection ms             - music data
          PieceData fullPieceData     - music data for all sections
          RenderedSectionParams[] rsp - starting parameters for voices
          OptionSet o                 - display options
          int fmn                     - number of first measure in section
          double sx                   - left x-coordinate of section in full score
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ScoreRenderer(int sectionNum,MusicSection ms,PieceData fullPieceData,
                       RenderedSectionParams[] rsp,
                       OptionSet o,int fmn,double sx)
  {
    this.sectionNum=sectionNum;
    this.musicData=ms;
    this.fullPieceData=fullPieceData;
    options=o;
    curMeasureNum=fmn;
    startX=sx;

    startingParams=new RenderedSectionParams[rsp.length];
    for (int i=0; i<rsp.length; i++)
      startingParams[i]=new RenderedSectionParams(rsp[i]);

    render();
  }

  public void render()
  {
    if (musicData instanceof MusicMensuralSection)
      renderMensuralData((MusicMensuralSection)musicData);
    else if (musicData instanceof MusicChantSection)
      renderChantData((MusicChantSection)musicData);
    else if (musicData instanceof MusicTextSection)
      renderTextData((MusicTextSection)musicData);
    else
      System.err.println("Error: Trying to render unsupported section type");

    createEndingParams();
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public RenderedEvent getEvent(int vnum,int ei)
  {
    return eventinfo[vnum].getEvent(ei);
  }

  public int getFirstMeasureNum()
  {
    return measures.getMeasure(0).getMeasureNum();
  }

  public int getLastMeasureNum()
  {
    return getFirstMeasureNum()+getNumMeasures()-1;
  }

  public int getNumMeasures()
  {
    return measures.size();
  }

  public MeasureInfo getMeasure(int mnum)
  {
    return measures.getMeasure(mnum-getFirstMeasureNum());
  }

  public MusicSection getSectionData()
  {
    return musicData;
  }

  public double getStartX()
  {
    return startX;
  }

  public double getXsize()
  {
    MeasureInfo mi=measures.getMeasure(measures.size()-1);
    return mi.leftx+mi.xlength;
  }

  public int getNumVoices()
  {
    return numVoices;
  }

  public RenderList getRenderedVoice(int i)
  {
    return eventinfo[i];
  }

  public double getEventXLoc(int vnum,int evnum)
  {
    return startX+eventinfo[vnum].getEvent(evnum).getxloc();
  }

  public RenderedSectionParams[] getEndingParams()
  {
    return endingParams;
  }

  public RenderedSectionParams[] getStartingParams()
  {
    return startingParams;
  }

/*------------------------------------------------------------------------
Method:  int getVoicedataPlace(int vnum,int reventnum)
Purpose: Return place of a rendered event in original voice event list
Parameters:
  Input:  int vnum      - voice number
          int reventnum - position of event in rendered event list
  Output: -
  Return: list place in original voice data
------------------------------------------------------------------------*/

  public int getVoicedataPlace(int vnum,int reventnum)
  {
    return eventinfo[vnum].getEvent(reventnum).getEvent().getListPlace(voicegfx[vnum].v.isDefaultVersion());
  }

  public int getDefaultVoicedataPlace(int vnum,int reventnum)
  {
    return eventinfo[vnum].getEvent(reventnum).getEvent().getDefaultListPlace();
  }

/*------------------------------------------------------------------------
Method:  NoteEvent getNeighboringNoteEvent(int vnum,int eventnum,int dir)
Purpose: Return last note event before or after specified location
Parameters:
  Input:  int vnum      - voice number to check
          int eventnum  - event index to start search
          int dir       - direction to search (1=right, -1=left)
  Output: -
  Return: last NoteEvent
------------------------------------------------------------------------*/

  public NoteEvent getNeighboringNoteEvent(int vnum,int eventnum,int dir)
  {
    int nenum=getNeighboringEventNumOfType(Event.EVENT_NOTE,vnum,eventnum,dir);
    if (nenum==-1)
      return null;
    else
      {
        Event e=eventinfo[vnum].getEvent(nenum).getEvent();
        if (e.geteventtype()==Event.EVENT_MULTIEVENT)
          return ((MultiEvent)e).getLowestNote();
        else
          return (NoteEvent)e;
      }
  }

  public RenderedEvent getNeighboringEventOfType(int eventType,int vnum,int eventnum,int dir)
  {
    for (int i=eventnum; i>=0 && i<eventinfo[vnum].size(); i+=dir)
      {
        Event e=eventinfo[vnum].getEvent(i).getEvent();
        if (e.hasEventType(eventType))
          return eventinfo[vnum].getEvent(i);
      }
    return null;
  }

  public int getNeighboringEventNumOfType(int eventType,int vnum,int eventnum,int dir)
  {
    for (int i=eventnum; i>=0 && i<eventinfo[vnum].size(); i+=dir)
      {
        Event e=eventinfo[vnum].getEvent(i).getEvent();
        if (e.hasEventType(eventType))
          return i;
      }
    return -1;
  }

/*------------------------------------------------------------------------
Method:  RenderedClefSet getClefEvents(int vnum)
Purpose: Return active clef events at current place in voice
Parameters:
  Input:  int vnum - voice number
  Output: -
  Return: set of rendered clef events
------------------------------------------------------------------------*/

/*  RenderedClefSet getClefEvents(int vnum)
  {
    return voicegfx[vnum].clefEvents;
    VoiceGfxInfo v=voicegfx[vnum];
    if (v.clefevent!=-1)
      return eventinfo[vnum].getEvent(v.clefevent);
    return startingParams[musicData.getVoice(vnum).getMetaData().getNum()-1].getClefSet();
  }*/

  Event getPrincipalClefData(VoiceGfxInfo v)
  {
    if (v.clefEvents==null)
      return null;
    return v.clefEvents.getPrincipalClefEvent().getEvent();
  }

/*------------------------------------------------------------------------
Method:  RenderedEvent getMensEvent(int vnum)
Purpose: Return active mensuration event at current place in voice
Parameters:
  Input:  int vnum - voice number
  Output: -
  Return: rendered mensuration event
------------------------------------------------------------------------*/

/*  RenderedEvent getMensEvent(int vnum)
  {
    return voicegfx[vnum].mensEvent;
    VoiceGfxInfo v=voicegfx[vnum];
    if (v.mensevent!=-1)
      return eventinfo[vnum].getEvent(v.mensevent);
    return startingParams[musicData.getVoice(vnum).getMetaData().getNum()-1].getMens();
  }*/

/*------------------------------------------------------------------------
Method:  int getXPosType(Event e)
Purpose: Calculate x-positioning type for a given event
Parameters:
  Input:  -
  Output: -
  Return: XPOS type
------------------------------------------------------------------------*/

  int getXPosType(Event e)
  {
    if (e.alignedWithPrevious())
      return XPOS_SIMULTANEOUS;
    switch (e.geteventtype())
      {
        case Event.EVENT_DOT:
          return XPOS_IMMEDIATE;
        case Event.EVENT_VARIANTDATA_END:
          if (options.get_displayedittags())
            return XPOS_BEFORENEXT;
          else
            return XPOS_INVISIBLE;
        case Event.EVENT_ORIGINALTEXT:
          if (!options.get_displayedittags())
            return XPOS_WITHNEXT;
        default:
          return XPOS_BEFORENEXT;
      }
  }

  int getXPosType(RenderedEvent re)
  {
    return getXPosType(re.getEvent());
  }

/*------------------------------------------------------------------------
Method:  void initEvents(MusicSection musicData)
Purpose: Initialize voice list for event drawing
Parameters:
  Input:  MusicSection musicData - music data
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void initEvents(MusicSection musicData)
  {
    /* create and initialize voice graphics info array */
    VariantVersionData curVersion=musicData.getVersion();
    numVoices=musicData.getNumVoices();
    voicegfx=new VoiceGfxInfo[numVoices];

    if (numVoices>0)
    {
    for (int i=0; i<numVoices; i++)
      {
        voicegfx[i]=new VoiceGfxInfo(this,musicData.getVoice(i));
        voicegfx[i].voicenum=i;
      }
    for (int i=0; i<numVoices-1; i++)
      voicegfx[i].next=voicegfx[i+1];
    for (int i=1; i<numVoices; i++)
      voicegfx[i].last=voicegfx[i-1];

    /* initialize voices */
    eventinfo=new RenderList[numVoices];
    for (int i=0; i<numVoices; i++)
      if (musicData.getVoice(i)!=null)
        {
          eventinfo[i]=new RenderList(options,musicData.getVoice(i).getMetaData(),musicData);

          int globalVnum=musicData.getVoice(i).getMetaData().getNum()-1;

          voicegfx[i].musictime=new Proportion(0,1);
          voicegfx[i].xadd=0;
          voicegfx[i].xloc=0;
          voicegfx[i].lastx=0;
          voicegfx[i].evloc=voicegfx[i].revloc=0;
          voicegfx[i].ligInfo=new RenderedLigature(musicData.getVoice(i),eventinfo[i]);
          voicegfx[i].tieInfo=new RenderedLigature(musicData.getVoice(i),eventinfo[i],RenderedLigature.TIE);
          voicegfx[i].varReadingInfo=null;
          voicegfx[i].clefEvents=startingParams[globalVnum].getClefSet();
          voicegfx[i].clefedata=getPrincipalClefData(voicegfx[i]);
          voicegfx[i].mensEvent=startingParams[globalVnum].getMens();
          voicegfx[i].curProportion=Proportion.EQUALITY;
          voicegfx[i].tempoProportion=Proportion.EQUALITY;
          voicegfx[i].curColoration=musicData.getBaseColoration();
          voicegfx[i].inEditorialSection=false;
          voicegfx[i].lastNoteEvent=null;
          voicegfx[i].curSoundingEvent=null;
          voicegfx[i].replacementEvents=new ArrayList<Event>();
          voicegfx[i].withnextEvents=0;
          voicegfx[i].lastTimedEventNum=-1;
          voicegfx[i].immediatePositioning=false;
          voicegfx[i].varDefaultTimeAdd=null;
          voicegfx[i].respaceAfterVar=false;
          voicegfx[i].missingInVersion=curVersion==null ? false :
            curVersion.isVoiceMissing(musicData.getVoice(globalVnum).getMetaData()) ||
            musicData.getVoice(i).getMissingVersions().contains(curVersion);
        }
      else
        eventinfo[i]=null;
    for (int i=0; i<numVoices-1; i++)
      voicegfx[i].next=voicegfx[i+1];
    voicegfx[numVoices-1].next=null;
    voicegfx[0].last=null;
    for (int i=1; i<numVoices; i++)
      voicegfx[i].last=voicegfx[i-1];

    /* remove unused voices from rendering list */
    for (int i=0; i<numVoices; i++)
      if (musicData.getVoice(i)==null)
        {
          if (voicegfx[i].last!=null)
            voicegfx[i].last.next=voicegfx[i].next;
          if (voicegfx[i].next!=null)
            voicegfx[i].next.last=voicegfx[i].last;
        }
    }

    /* get drawing options */
    barline_type=options.get_barline_type();
    useModernClefs=options.get_usemodernclefs();
    noteShapeType=options.get_noteShapeType();
    useModernAccSystem=options.getUseModernAccidentalSystem();
    displayallnewlineclefs=options.get_displayallnewlineclefs();
    displayVarTexts=options.markVariant(VariantReading.VAR_ORIGTEXT) &&
                    !options.get_displayedittags();

    /* set initial drawing parameters */
    if (numVoices>0)
      liststart=voicegfx[musicData.getValidVoicenum(0)];

    initCurDrawingParams();

    finalisList=new LinkedList<VoiceGfxInfo>();

    /* initialize measure list */
    measures=new MeasureList(numVoices);
    curmeasure=measures.newMeasure(curMeasureNum,new Proportion(0,1),numMinimsInBreve,Proportion.EQUALITY,BREVESCALE,barxstart);
    for (int i=0; i<numVoices; i++)
      if (musicData.getVoice(i)!=null)
        {
          curmeasure.reventindex[i]=voicegfx[i].revloc;
          curmeasure.startClefEvents[i]=voicegfx[i].clefEvents;
          curmeasure.startMensEvent[i]=voicegfx[i].mensEvent;
        }

    curSonority=new RenderedSonority();
    sonorityList=new ArrayList<RenderedSonority>();
    sonorityList.add(curSonority);
  }

  void initCurDrawingParams()
  {
    barxstart=0;
    skipevents=0;
    errorRespacingTime=null;
    variantVoice=null;

    baseMensuration=null;
    numMinimsInBreve=4; /* default to C */
    MINIMSCALE=25;
    BREVESCALE=MINIMSCALE*numMinimsInBreve;
    lastMens=null;
    lastMensTime=null;
  }

/*------------------------------------------------------------------------
Method:  void createEndingParams()
Purpose: Store voice parameters at the end of rendering (end of section)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createEndingParams()
  {
    endingParams=new RenderedSectionParams[startingParams.length];
    for (int i=0; i<startingParams.length; i++)
      endingParams[i]=new RenderedSectionParams(startingParams[i]);

    for (int i=0; i<numVoices; i++)
      if (musicData.getVoice(i)!=null)
        {
          int vnum=musicData.getVoiceMetaData(i).getNum()-1;
          endingParams[vnum].setClefSet(voicegfx[i].clefEvents);
          endingParams[vnum].setMens(voicegfx[i].mensEvent);
          endingParams[vnum].usedInSection=true;
        }
      else
        endingParams[i].usedInSection=false;
  }

/*------------------------------------------------------------------------
Method:  boolean newVoiceArrangement()
Purpose: Check whether a different set of voices is used in this section
         than in the last
Parameters:
  Input:  -
  Output: -
  Return: true if voice sets are different
------------------------------------------------------------------------*/

  boolean newVoiceArrangement()
  {
    for (int i=0; i<startingParams.length; i++)
      if (startingParams[i].usedInSection!=endingParams[i].usedInSection)
        return true;
    return false;
  }

/*------------------------------------------------------------------------
Method:  void render(MusicSection ms)
Purpose: Render music into measure structure
Parameters:
  Input:  MusicSection ms - music data to render
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void renderMensuralData(MusicMensuralSection ms)
  {
    initEvents(ms);
    createRenderLists();
    positionMensuralEvents();
  }

  void renderChantData(MusicChantSection ms)
  {
    initEvents(ms);
    positionChantEvents(ms);
  }

  void renderTextData(MusicTextSection ms)
  {
    initEvents(ms);
    positionTextSection(ms);
  }

/*------------------------------------------------------------------------
Method:  void createRenderLists()
Purpose: Create lists of rendered events for each voice,
         replacing/deleting/adding events as necessary for display but not
         assigning any x-positioning
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  int withnextStart;

  void createRenderLists()
  {
    for (VoiceGfxInfo v : voicegfx)
      if (v!=null && v.v!=null)
        {
          RenderList rl=eventinfo[v.voicenum];
          curMeasureNum=getFirstMeasureNum();
          curmeasure=measures.get(curMeasureNum-getFirstMeasureNum());
          skipevents=0;
          int withnextStart=-1,
              newEventsInserted=0;

          for (v.evloc=0; v.evloc<v.v.getNumEvents(); v.evloc++)
            {
              Event e=v.v.getEvent(v.evloc);

              while (v.musictime.greaterThan(curmeasure.getEndMusicTime()))
                advanceOneMeasure(v);
              if (positionInNewMeasure(e,v))
                advanceOneMeasure(v);
              if (newEventsInserted>0)
                {
                  adjustNewEventMeasureNums(v,newEventsInserted);
                  newEventsInserted=0;
                }

              RenderedEvent re=addOneEvent(e,rl,v,skipevents==0);

              if (skipevents>0)
                {
                  if (--skipevents==0)
                    newEventsInserted=insertReplacementEvents(v);
                }
              else if (v.replacementEvents.size()>0)
                newEventsInserted=insertReplacementEvents(v);
            }

          /* reset parameters */
          initCurDrawingParams();
        }

    /* fill out info for "empty" measures beyond the end of voice parts */
    for (VoiceGfxInfo v : voicegfx)
      if (v!=null)
        {
          for (int mi=measures.size()-1;
               mi>=0 && measures.get(mi).reventindex[v.voicenum]==-1;
               mi--)
            {
              MeasureInfo m=measures.get(mi);
              m.reventindex[v.voicenum]=v.revloc;
              m.startClefEvents[v.voicenum]=v.clefEvents;
              m.startMensEvent[v.voicenum]=v.mensEvent;
              m.tempoProportion[v.voicenum]=v.tempoProportion;
            }
        }
  }

  RenderedEvent addOneEvent(Event e,RenderList rl,VoiceGfxInfo v,boolean display)
  {
    e=setPrerenderParameters(v,e);
    if (skipevents>0)
      display=false;
    RenderedEvent re=createRenderedEvent(e,rl,v,display);
    finishEvent(v,re);

    return re;
  }

  RenderedEvent createRenderedEvent(Event e,RenderList rl,VoiceGfxInfo v,boolean display)
  {
    RenderedEvent re=rl.addevent(display,e,new RenderParams(
      curMeasureNum,
      v.clefEvents,v.lastNoteEvent,v.mensEvent,
      v.curProportion,v.curColoration,v.inEditorialSection,v.missingInVersion,
      v.ligInfo,endlig,v.tieInfo,
      v.v.getMetaData().getSuggestedModernClef(),
      v.varReadingInfo));
    re.setmusictime(v.musictime);

    return re;
  }

  void pushEventsIntoCurMeasure(VoiceGfxInfo v,int withnextStart,MeasureInfo curMeasure)
  {
    if (withnextStart<curMeasure.reventindex[v.voicenum])
      curMeasure.reventindex[v.voicenum]=withnextStart;
    for (int ei=withnextStart; ei<v.revloc; ei++)
      eventinfo[v.voicenum].getEvent(ei).setMeasureNum(curMeasure.getMeasureNum());
  }

  void adjustNewEventMeasureNums(VoiceGfxInfo v,int numNewEvents)
  {
    for (int ei=v.revloc-numNewEvents; ei<v.revloc; ei++)
      {
        RenderedEvent re=eventinfo[v.voicenum].getEvent(ei);
        int           mnum=re.getmeasurenum()+1;
        MeasureInfo   m=getMeasure(mnum);
        while (m!=null && re.getmusictime().greaterThanOrEqualTo(m.startMusicTime))
          {
            pushEventsIntoCurMeasure(v,ei,m);
            m=getMeasure(++mnum);
          }
      }
  }

/*------------------------------------------------------------------------
Method:  int insertReplacementEvents(VoiceGfxInfo v)
Purpose: Insert newly-created events into render list
Parameters:
  Input:  -
  Output: VoiceGfxInfo v - voice to update
  Return: number of events inserted
------------------------------------------------------------------------*/

  int insertReplacementEvents(VoiceGfxInfo v)
  {
    RenderedEvent re,lastevent=null;
    RenderList    rl=eventinfo[v.voicenum];
    int           numEvents=v.replacementEvents.size();

    for (Event e : v.replacementEvents)
      {
        /* render event */
        re=createRenderedEvent(e,rl,v,true);
        if (skipevents!=0)
          setPostrenderParameters(v,re); // make sure replacement event sets post params?
        finishEvent(v,re);

        /* set voice event parameters 
        if (e.hasSignatureClef())
          {
            v.clefEvents=new RenderedClefSet(v.clefEvents,re,useModernAccSystem,v.v.getMetaData().getSuggestedModernClef());
            v.clefedata=getPrincipalClefData(v);
          }*/
      }
    v.replacementEvents=new ArrayList<Event>();
    return numEvents;
  }

/*------------------------------------------------------------------------
Method:  void finishEvent(VoiceGfxInfo v,RenderedEvent re)
Purpose: Advance voice info counters after rendering an event
Parameters:
  Input:  VoiceGfxInfo  v  - voice to update
          RenderedEvent re - event which has just been rendered
  Output: -
  Return: -
------------------------------------------------------------------------*/

  boolean endlig=false;

  void finishEvent(VoiceGfxInfo v,RenderedEvent re)
  {
    if (skipevents==0)
      setPostrenderParameters(v,re);

    RenderList vl=eventinfo[v.voicenum];

    int XPosType=getXPosType(re);
    if ((XPosType==XPOS_IMMEDIATE || XPosType==XPOS_SIMULTANEOUS) &&
        v.lastTimedEventNum>-1)
      {
        RenderedEvent lastTimedEvent=vl.getEvent(v.lastTimedEventNum);
        int newMeasureNum=lastTimedEvent.getmeasurenum();
        if (newMeasureNum!=curMeasureNum)
          for (int ei=v.lastTimedEventNum+1; ei<=v.revloc; ei++)
            {
              /* position events in same measure as last */
              vl.getEvent(ei).setMeasureNum(newMeasureNum);
              for (int mi=newMeasureNum-getFirstMeasureNum()+1;
                   mi<measures.size() && measures.get(mi).reventindex[v.voicenum]!=-1;
                   mi++)
                measures.get(mi).reventindex[v.voicenum]++;
            }
        lastTimedEvent.setAttachedEventIndex(v.revloc);
      }

    /* check whether to push up previous events into this measure */
    if (withnextStart==-1)
      {
        if (XPosType==XPOS_WITHNEXT)
          withnextStart=v.revloc;
      }
    else
      if (XPosType!=XPOS_WITHNEXT && XPosType!=XPOS_INVISIBLE)
        {
          pushEventsIntoCurMeasure(v,withnextStart,curmeasure);
          withnextStart=-1;
        }

    if (re.getMusicLength().i1>0)
      v.lastTimedEventNum=v.revloc;

    v.revloc++;
    if (re.getEvent().geteventtype()==Event.EVENT_VARIANTDATA_END)
      {
/*        if (skipevents==0)
          v.varReadingInfo.lastEventNum++;*/
        v.varReadingInfo=null;
//        v.inEditorialSection=false;
      }

    if (endlig)
      {
        v.ligInfo=new RenderedLigature(v.v,vl);
        endlig=false;
      }
//    if (v.tieInfo.numNotes>1)
//      v.tieInfo=new RenderedLigature(v.v,vl,RenderedLigature.TIE);

    /* advance music time after timed event */
    if (noteShapeType==OptionSet.OPT_NOTESHAPE_ORIG)
      re.setMusicLength(Proportion.quotient(
                          re.getEvent().getmusictime(),
                          Proportion.product(v.curProportion,v.tempoProportion)));
    else
      re.setMusicLength(Proportion.quotient(
                          re.getEvent().getmusictime(),v.tempoProportion));

    if (v.respaceAfterVar)
      {
        re.getMusicLength().add(v.varDefaultTimeAdd);
        v.varDefaultTimeAdd=null;
        v.respaceAfterVar=false;
      }
    if (errorRespacingTime!=null &&
        Proportion.sum(v.musictime,re.getMusicLength()).greaterThan(errorRespacingTime))
      re.setMusicLength(Proportion.difference(errorRespacingTime,v.musictime));

    v.musictime.add(re.getMusicLength());  //Proportion.product(re.getMusicLength(),v.tempoProportion));
  }

/*------------------------------------------------------------------------
Method:  Event setPrerenderParameters(VoiceGfxInfo v,Event e)
Purpose: Set voice parameters if modified by an event about to be rendered
Parameters:
  Input:  VoiceGfxInfo  v - voice to update
          Event         e - event to be rendered
  Output: -
  Return: event to be rendered, replaced if necessary
------------------------------------------------------------------------*/

  Event setPrerenderParameters(VoiceGfxInfo v,Event e)
  {
    if (skipevents>0)
      return e;

    if (noteShapeType!=OptionSet.OPT_NOTESHAPE_ORIG)
      e=makeModernNoteShapes(e,v);

    if (e.hasSignatureClef())
      {
        int skippedClefEvents=calcNumSkippedClefEvents(v,v.evloc);
        if (skippedClefEvents>0)
          skipevents+=skippedClefEvents;
        if (useModernClefs && e.geteventtype()==Event.EVENT_MULTIEVENT && e.hasPrincipalClef())
          {
            /* separate modern clefs from accidentals */
            for (Iterator ei=((MultiEvent)e).iterator(); ei.hasNext();)
              {
                Event ee=(Event)ei.next();
                if (ee.geteventtype()==Event.EVENT_CLEF && !ee.hasPrincipalClef()) //&& !useModernAccSystem)
                  v.replacementEvents.add(ee);
              }
            e=((MultiEvent)e).noSigClefEvent();
          }

/*        else
          if (v.clefevent==-1 ||
              e.getClefSet(useModernAccSystem)!=v.clefedata.getClefSet(useModernAccSystem))
            {
              v.clefevent=v.revloc;
              v.clefedata=e;
            }*/
      }

    Mensuration mensInfo=e.getMensInfo();
    if (mensInfo!=null)
      {
/*        if (baseMensuration==null ||
             if at least two voices change to the same mensuration
               simultaneously, use as main mensuration 
            (v.musictime.equals(lastMensTime) && lastMens.equals(mensInfo)))*/
            /* set base mensuration info for score measure structure */
        baseMensuration=mensInfo;
        numMinimsInBreve=baseMensuration.prolatio*baseMensuration.tempus;

        double oldBS=BREVESCALE;
        BREVESCALE=MINIMSCALE*numMinimsInBreve/baseMensuration.tempoChange.toDouble();
        v.tempoProportion=baseMensuration.tempoChange;

        /* change current bar if mensuration takes effect here */
        if (!((MensEvent)(e.getFirstEventOfType(Event.EVENT_MENS))).noScoreSig() &&
            v.musictime.lessThan(curmeasure.getEndMusicTime()))
          {
            if (curmeasure.numMinims!=numMinimsInBreve ||
                !curmeasure.scaleSet)
              {
                curmeasure.numMinims=numMinimsInBreve;
                curmeasure.xlength+=BREVESCALE-oldBS;
                curmeasure.defaultTempoProportion=v.tempoProportion;
                curmeasure.scaleSet=true;
              }
            curmeasure.tempoProportion[v.voicenum]=v.tempoProportion;
          }
        lastMens=mensInfo;
        lastMensTime=v.musictime;
      }

    if (e.geteventtype()==Event.EVENT_DOT)
      {
        if ((((DotEvent)e).getdottype() & DotEvent.DT_Addition)!=0)
          v.lastNoteEvent=getLastNoteEvent(v,v.revloc-1);

        /* don't show any original dot events in modern notation
           (notes receive their own new attached dots) */
        if (noteShapeType!=OptionSet.OPT_NOTESHAPE_ORIG)
          skipevents++;
      }

    /* skip lineend custodes unless displaying newline clefs */
    if (!displayallnewlineclefs && e.geteventtype()==Event.EVENT_CUSTOS)
      {
        Event nexte=v.v.getEvent(v.evloc+1);
        if (nexte!=null && nexte.geteventtype()==Event.EVENT_LINEEND)
          skipevents++;
      }

    /* at line end, construct display for next clef set */
    if (e.geteventtype()==Event.EVENT_LINEEND && !displayallnewlineclefs)
      {
        Event nexte=v.v.getEvent(v.evloc+1);
        skipevents++;
        if (nexte!=null && nexte.hasSignatureClef() && nexte.principalClefEquals(v.clefedata,useModernClefs))
          {
            if (useModernAccSystem ||
                nexte.getModernKeySig().equals(e.getModernKeySig()))
//                !nexte.getClefSet(useModernAccSystem).contradicts(v.clefEvents.getLastClefSet(useModernAccSystem),useModernClefs,v.v.getMetaData().getSuggestedModernClef()))
              skipevents+=calcNumClefEvents(v,v.evloc+1);
            if (useModernAccSystem &&
                !nexte.getModernKeySig().equals(e.getModernKeySig()))
//                nexte.getClefSet(useModernAccSystem).contradicts(v.clefEvents.getLastClefSet(useModernAccSystem),useModernClefs,v.v.getMetaData().getSuggestedModernClef()))
              v.replacementEvents.addAll(constructDisplayClefSet(v.clefEvents,nexte));
          }
      }

    if (e.geteventtype()==Event.EVENT_PROPORTION)
      {
        Proportion newp=((ProportionEvent)e).getproportion();
        v.curProportion=new Proportion(v.curProportion);
        v.curProportion.multiply(newp);
      }
    if (e.geteventtype()==Event.EVENT_COLORCHANGE)
      v.curColoration=new Coloration(v.curColoration,((ColorChangeEvent)e).getcolorscheme());

    if (e.geteventtype()==Event.EVENT_VARIANTDATA_START)
      {
//System.out.println("startvar: V"+v.voicenum+", evloc="+v.evloc);
        Event nextEvent=v.v.getEvent(v.evloc+1);
        int   skippedClefEvents=calcNumSkippedClefEvents(v,v.evloc+1);
        Event nextDisplayedEvent=v.v.getEvent(v.evloc+skippedClefEvents+1);

        if (skippedClefEvents>0 && nextDisplayedEvent.geteventtype()==Event.EVENT_VARIANTDATA_END)
          /* nothing displayed in editorial section: don't display brackets */
          skipevents+=skippedClefEvents+2;
        else
          {
//System.out.println("startvar: V"+v.voicenum+", evloc="+v.evloc);
//            v.inEditorialSection=true;
            v.varReadingInfo=new RenderedEventGroup(v.revloc);
            v.varReadingInfo.grouptype=RenderedEventGroup.EVENTGROUP_VARIANTREADING;
            v.varReadingInfo.varReading=e.getVariantReading(musicData.getVersion());
            v.varReadingInfo.varMarker=(VariantMarkerEvent)e;
            if (v.varReadingInfo.varReading!=null && v.varReadingInfo.varReading.isError())
              {
                Proportion defaultReadingLength=((VariantMarkerEvent)e).getDefaultLength(),
                           varReadingLength=v.varReadingInfo.varReading.getLength();
                if (varReadingLength.greaterThan(defaultReadingLength))
                  {
                    defaultReadingLength.divide(v.curProportion);
                    defaultReadingLength.divide(v.tempoProportion);
                    errorRespacingTime=Proportion.sum(v.musictime,defaultReadingLength);
                    variantVoice=v;
                  }
                else if (defaultReadingLength.greaterThan(varReadingLength))
                  {
                    v.varDefaultTimeAdd=Proportion.difference(defaultReadingLength,varReadingLength);
                    v.varDefaultTimeAdd.divide(v.curProportion);
                    v.varDefaultTimeAdd.divide(v.tempoProportion);
                  }
              }

            /* show multiple texts? */
            if (displayVarTexts &&
                v.varReadingInfo.varMarker.includesVarType(VariantReading.VAR_ORIGTEXT))
              v.replacementEvents.addAll(constructVarTextEvents(v));
          }
      }
    if (e.geteventtype()==Event.EVENT_VARIANTDATA_END)
      {
        if (v.varDefaultTimeAdd!=null)
          v.respaceAfterVar=true;

        if (v.varReadingInfo==null)
          {
            System.err.println("Error! VARIANTDATA_END without START; V"+v.voicenum+", evloc="+v.evloc);
            for (int ei=0; ei<v.evloc; ei++)
              v.v.getEvent(ei).prettyprint();
          }
//else
//System.out.println("endvar: V"+v.voicenum+", evloc="+v.evloc);

        v.varReadingInfo.lastEventNum=v.revloc-1;
        v.varReadingInfo.calculateYMinMax(eventinfo[v.voicenum]);
        if (v.varReadingInfo.firstEventNum==v.varReadingInfo.lastEventNum)
          eventinfo[v.voicenum].getEvent(v.varReadingInfo.firstEventNum).setDisplay(true);
        if (skipevents==0)
          v.varReadingInfo.lastEventNum++;
        errorRespacingTime=null;
/*        v.varReadingInfo.lastEventNum++;
        v.varReadingInfo=null;
        v.inEditorialSection=false;*/
      }
    if (e.geteventtype()==Event.EVENT_ORIGINALTEXT)
      {
        if (displayVarTexts)
          {
            skipevents++;
            v.replacementEvents.addAll(constructVarTextEvents(v,(OriginalTextEvent)e));
          }
      }

    if (e.geteventtype()==Event.EVENT_LACUNA)
      {
        v.inEditorialSection=true;
      }
    if (e.geteventtype()==Event.EVENT_LACUNA_END)
      {
        v.inEditorialSection=false;
      }

    return e;
  }

  void setPostrenderParameters(VoiceGfxInfo v,RenderedEvent re)
  {
    setPostrenderParameters(v,re,true);
  }

  void setPostrenderParameters(VoiceGfxInfo v,RenderedEvent re,boolean doLigs)
  {
    Event e=re.getEvent();

    if (doLigs)
      {
        endlig=doLigInfo(v,e);
        re.setLigInfo(v.ligInfo);
        re.setLigEnd(endlig);
      }

    boolean endTie=v.tieInfo.firstEventNum!=-1;
    doTieEndInfo(v,e);
    re.setTieInfo(v.tieInfo);
    doTieStartInfo(v,e);
    if (!endTie)
      re.setTieInfo(v.tieInfo);
    else
      re.setDoubleTied(v.tieInfo.firstEventNum!=-1);

    v.lastNoteEvent=null;

    if (e.hasSignatureClef())
      {
        v.clefEvents=re.getClefEvents();
        v.clefedata=getPrincipalClefData(v);
      }
    if (e.getMensInfo()!=null)
      v.mensEvent=re;
  }

/*------------------------------------------------------------------------
Method:  Event makeModernNoteShape(Event e,ArrayList<Event> el)
Purpose: Replace note/rest with modern notational element(s)
Parameters:
  Input:  Event e - event to check/replace
  Output: ArrayList<Event> el - list of events to be inserted in current voice
  Return: e if no change, otherwise modern notation version of e
------------------------------------------------------------------------*/

  Event makeModernNoteShapes(Event e,VoiceGfxInfo v)
  {
    LinkedList<Event> newEvents=e.makeModernNoteShapes(
      v.musictime,curmeasure.startMusicTime,curmeasure.numMinims,curmeasure.defaultTempoProportion,
      v.curProportion,true/* ties */);
    if (newEvents.size()>1)
      v.replacementEvents.addAll(newEvents.subList(1,newEvents.size()));
    return newEvents.get(0);
  }

/*------------------------------------------------------------------------
Method:  void advanceOneMeasure(VoiceGfxInfo v)
Purpose: Advance measure counter and create one new measure structure if
         necessary
Parameters:
  Input:  VoiceGfxInfo v - voice currently being rendered
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void advanceOneMeasure(VoiceGfxInfo v)
  {
    /* deal with end of bar issues */
    curMeasureNum++;
    if (curMeasureNum-getFirstMeasureNum()>=measures.size())
      curmeasure=measures.newMeasure(
        curMeasureNum,curmeasure.getEndMusicTime(),
        numMinimsInBreve,v.tempoProportion,BREVESCALE,0);
    else
      curmeasure=measures.get(curMeasureNum-getFirstMeasureNum());
    curmeasure.reventindex[v.voicenum]=v.revloc;
    curmeasure.startClefEvents[v.voicenum]=v.clefEvents;
    curmeasure.startMensEvent[v.voicenum]=v.mensEvent;
    curmeasure.tempoProportion[v.voicenum]=new Proportion(v.tempoProportion);
  }

  boolean positionInNewMeasure(Event e,VoiceGfxInfo v)
  {
    if (skipevents>0)
      return false;
    if (e.getmusictime().i1<=0)
      return false;
    return v.musictime.greaterThanOrEqualTo(curmeasure.getEndMusicTime());
  }

/* POSITIONING CODE */

/*------------------------------------------------------------------------
Method:  void positionMensuralEvents()
Purpose: After lists have been rendered for individual voices, assign
         x-positions to all events/measures
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void positionMensuralEvents()
  {
    initVoiceCounters();
    do
      {
        advanceVoices(positionUntimedEvents());

        while (liststart!=null &&
               liststart.musictime.greaterThan(curmeasure.getEndMusicTime()))
          advanceOneMeasureSpacing();

        if (liststart!=null &&
            positionInNewMeasure(eventinfo[liststart.voicenum].getEvent(liststart.revloc).getEvent(),liststart))
          advanceOneMeasureSpacing();

        advanceVoices(positionTimedAndImmediateEvents());

        /* advance one measure if leftmost voice is past current measure limit */
        while (liststart!=null &&
               liststart.musictime.greaterThan(curmeasure.getEndMusicTime()))
          advanceOneMeasureSpacing();
      }
    while (liststart!=null);
  }

  void initVoiceCounters()
  {
    curMeasureNum=0;
    curmeasure=measures.get(curMeasureNum);
    for (VoiceGfxInfo v : voicegfx)
      if (v!=null)
        {
          v.musictime=new Proportion(0,1);
          v.revloc=0;
        }
  }

/*------------------------------------------------------------------------
Method:  double positionUntimedEvents()
Purpose: Position untimed events for all voices in the leftmost place (i.e. those
         at the front of the voice list)
Parameters:
  Input:  -
  Output: -
  Return: Total amount of horizontal space needed by the events
------------------------------------------------------------------------*/

  double positionUntimedEvents()
  {
    double        xadd=0;
    VoiceGfxInfo  v,nextv;
    RenderedEvent re;

    if (liststart==null)
      return xadd;

    starttime=new Proportion(liststart.musictime);
    nextv=liststart.next;
    for (v=liststart;
         v!=null && v.musictime.equals(starttime);
         v=nextv)
      {
        nextv=v.next;
        for (re=eventinfo[v.voicenum].getEvent(v.revloc);
             re!=null && re.getMusicLength().i1==0;
             re=eventinfo[v.voicenum].getEvent(incrementVoicePosition(v)))
          {
            int xpos=getXPosType(re);
            if (xpos==XPOS_WITHNEXT ||
                (xpos==XPOS_INVISIBLE && v.withnextEvents>0))
              v.withnextEvents++;
            else
              xadd=positionUntimedEvent(v,re,xadd);
          }
      }

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  double positionTimedandImmediateEvents()
Purpose: Position one timed event for each voice in last place along with
         XPOS_IMMEDIATE untimed events following it (e.g., dots)
Parameters:
  Input:  -
  Output: -
  Return: Total amount of horizontal space needed by the events
------------------------------------------------------------------------*/

  double positionTimedAndImmediateEvents()
  {
    VoiceGfxInfo  v,nextv;
    RenderedEvent re;
    double        xadd=0;

    if (liststart==null)
      return xadd;

    nextv=liststart.next;
    for (v=liststart;
         v!=null && starttime.greaterThanOrEqualTo(v.musictime);
         v=nextv)
      {
        nextv=v.next;

        re=eventinfo[v.voicenum].getEvent(v.revloc);
        if (re==null)
          removeVoice(v);
        else
          {
            xadd=positionTimedEvent(v,re,xadd);
            recalcVoiceList(v);
          }
      }

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  double positionUntimedEvent(VoiceGfxInfo v,RenderedEvent e,double xadd)
Purpose: Assign position to one untimed event
Parameters:
  Input:  VoiceGfxInfo v    - voice to update
          RenderedEvent e   - event to position
          double       xadd - positioning adjuster for all voices at the
                              end of untimed event rendering
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  double positionUntimedEvent(VoiceGfxInfo v,RenderedEvent re,double xadd)
  {
    RenderList rl=eventinfo[v.voicenum];
    double     curx;

    xadd=positionWITHNEXTEvents(v,xadd);
    re.setxloc(curx=v.calcXLoc(rl,re));
    xadd=finishPositioningUntimedEvent(v,re,curx,xadd);

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  double positionTimedEvent(VoiceGfxInfo v,RenderedEvent re,double xadd)
Purpose: Position one timed event along with XPOS_IMMEDIATE untimed events
         following it (e.g., dots)
Parameters:
  Input:  VoiceGfxInfo v   - voice of event
          RenderedEvent re - event to position
          double xadd      - total amount of horizontal space needed
                             for adjusting all voices after this phase
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  double positionTimedEvent(VoiceGfxInfo v,RenderedEvent re,double xadd)
  {
    RenderList rl=eventinfo[v.voicenum];

    xadd=positionWITHNEXTEvents(v,xadd);
    re.setxloc(v.xloc);
    v.revloc++;

    updateSonority(re,v);

    v.lastx=v.xloc;

    double XLocAdd=MINIMSCALE*re.getMusicLength().toDouble(); //(rl.getEvent(v.revloc).getmusictime().toDouble()-re.getmusictime().toDouble());
    if (XLocAdd<re.getrenderedxsize())
      {
        v.xadd+=re.getrenderedxsize()-XLocAdd;
        if (xadd<v.xadd)
          xadd=v.xadd;
        XLocAdd=re.getrenderedxsize();
      }
    v.xloc+=XLocAdd;

    RenderedEvent nextre=rl.getEvent(v.revloc);

    /* position untimed XPOS_IMMEDIATE events (e.g., dots)
       and XPOS_SIMULTANEOUS events (vertically aligned with current event) */
    int immedEventNum=re.getAttachedEventIndex();
    if (immedEventNum>-1)
      {
        v.withnextEvents=0;
        v.immediatePositioning=true;
        while (v.revloc<=immedEventNum)
          {
            xadd=positionUntimedEvent(v,nextre,xadd);
            nextre=rl.getEvent(incrementVoicePosition(v));
          }
        v.immediatePositioning=false;
      }

    v.musictime=nextre.getmusictime();

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  void recalcVoiceList(VoiceGfxInfo v)
Purpose: Adjust pointers within voice list to maintain sorting order after one
         voice has moved forward
Parameters:
  Input:  VoiceGfxInfo v - voice which has just been advanced (i.e. in
                           which timed events have just been positioned)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void recalcVoiceList(VoiceGfxInfo v)
  {
    VoiceGfxInfo moveplace=v;
    while (!((moveplace.next==null) ||
             (moveplace.next.musictime.greaterThan(v.musictime))))
      moveplace=moveplace.next;

    if (moveplace!=v)
      {
        removeVoice(v);

        /* re-insert */
        if (moveplace.next!=null)
          moveplace.next.last=v;
        v.next=moveplace.next;
        v.last=moveplace;
        moveplace.next=v;
      }
  }

  /* update sonority info for analysis */
  void updateSonority(RenderedEvent re,VoiceGfxInfo v)
  {
    Event e=re.getEvent();

    if (e.getFirstEventOfType(Event.EVENT_REST)!=null)
      {
        if (re.getmusictime().equals(curSonority.getMusicTime()))
          curSonority.remove(v.curSoundingEvent);
        else
          {
            curSonority=curSonority.copyWithout(v.curSoundingEvent);
            curSonority.setMusicTime(re.getmusictime());
            sonorityList.add(curSonority);
          }
        v.curSoundingEvent=null;
      }
    if (e.getFirstEventOfType(Event.EVENT_NOTE)!=null)
      {
        if (re.getmusictime().equals(curSonority.getMusicTime()))
          {
            curSonority.remove(v.curSoundingEvent);
            curSonority.add(re);
          }
        else
          {
            curSonority=curSonority.copyWithout(v.curSoundingEvent);
            curSonority.add(re);
            curSonority.setMusicTime(re.getmusictime());
            sonorityList.add(curSonority);
          }
        v.curSoundingEvent=re;
        re.setSonority(curSonority);
      }
  }

/*------------------------------------------------------------------------
Method:  double finishPositioningUntimedEvent(VoiceGfxInfo v,RenderedEvent re,
                                              double curx,double xadd)
Purpose: Advance voice info counters and x positioning information after
         positioning an untimed event
Parameters:
  Input:  VoiceGfxInfo  v    - voice to update
          RenderedEvent re   - event which has just been positioned
          double        curx - current renderer x position
          double        xadd - positioning adjuster for all voices at the
                               end of untimed event rendering
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  double finishPositioningUntimedEvent(VoiceGfxInfo v,RenderedEvent re,
                                       double curx,double xadd)
  {
    double eventxlen=re.getrenderedxsize();
    if (getxspacing(re.getEvent())==XSPACING_NOSPACE)
      {
        if (curx+eventxlen>v.xloc)
          eventxlen=curx+eventxlen-v.xloc;
        else
          eventxlen=0;
      }
    if (getXPosType(re.getEvent())==XPOS_SIMULTANEOUS)
      eventxlen=0;
    if (re.isdisplayed()) /* leave lastx alone if this event wasn't */
      v.lastx=curx;       /* displayed */
    v.xloc+=eventxlen;
    v.xadd+=eventxlen;
    if (v.xadd>xadd)
      xadd=v.xadd;

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  double positionWITHNEXTEvents(VoiceGfxInfo v,double xadd)
Purpose: Position events with positioning XPOS_WITHNEXT, i.e., untimed events
         which are always in the same measure/position as the next event
Parameters:
  Input:  VoiceGfxInfo v    - voice to update
          double       xadd - positioning adjuster for all voices at the
                              end of untimed event positioning
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  boolean positioningWITHNEXTEvents=false;

  double positionWITHNEXTEvents(VoiceGfxInfo v,double xadd)
  {
    if (positioningWITHNEXTEvents) /* avoid re-entry */
      return xadd;
    positioningWITHNEXTEvents=true;

    for (; v.withnextEvents>0; v.withnextEvents--)
      xadd=positionUntimedEvent(v,eventinfo[v.voicenum].getEvent(v.revloc-v.withnextEvents),xadd);

    positioningWITHNEXTEvents=false;
    return xadd;
  }

/*------------------------------------------------------------------------
Method:  int incrementVoicePosition(VoiceGfxInfo v)
Purpose: Advance event index within one voice list, remove if done
Parameters:
  Input:  VoiceGfxInfo  v - voice to update
  Output: -
  Return: new event index
------------------------------------------------------------------------*/

  int incrementVoicePosition(VoiceGfxInfo v)
  {
    v.revloc++;
    removeVoiceIfFinished(v);
    return v.revloc;
  }

/*------------------------------------------------------------------------
Method:  void removeVoice[IfFinished](VoiceGfxInfo v)
Purpose: Remove voice from rendering to-do list [if all its events have
         been rendered]
Parameters:
  Input:  VoiceGfxInfo  v - voice to update
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void removeVoiceIfFinished(VoiceGfxInfo v)
  {
    if (eventinfo[v.voicenum].getEvent(v.revloc)==null)
      removeVoice(v);
  }

  void removeVoice(VoiceGfxInfo v)
  {
    if (v.next!=null)
      v.next.last=v.last;
    if (v.last!=null)
      v.last.next=v.next;
    else
      liststart=v.next;
  }

/*------------------------------------------------------------------------
Method:  void advanceVoices(double xadd)
Purpose: Move all voices forward (horizontally) after events
Parameters:
  Input:  double xadd - total amount for voices to shift
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void advanceVoices(double xadd)
  {
    if (xadd>0)
      {
        curmeasure.xlength+=xadd;
        for (int i=0; i<numVoices; i++)
          if (musicData.getVoice(i)!=null)
            {
              if (voicegfx[i].xadd<xadd)
                {
                  double vxadd=xadd-voicegfx[i].xadd;
                  voicegfx[i].xloc+=vxadd;
                  if (liststart!=null &&
                      voicegfx[i].musictime.greaterThanOrEqualTo(liststart.musictime))
                    voicegfx[i].moveupBEFORENEXTevents(eventinfo[i],vxadd);
                }
              voicegfx[i].xadd=0;
            }
      }
  }

  void advanceOneMeasureSpacing()
  {
    /* load new measure */
    barxstart+=curmeasure.xlength;
    curMeasureNum++;
    curmeasure=measures.get(curMeasureNum);
    curmeasure.leftx=barxstart;

    /* space for barlines */
    if (options.get_barline_type()!=OptionSet.OPT_BARLINE_NONE)
      {
        double bxadd=BARLINE_XADD;
        switch (options.get_barline_type())
          {
            case OptionSet.OPT_BARLINE_MENSS:
            case OptionSet.OPT_BARLINE_MODERN:
              bxadd*=2;
              break;
          }
        curmeasure.xlength+=bxadd;
        for (int i=0; i<numVoices; i++)
          voicegfx[i].xloc+=bxadd;
      }
  }





/* OLD RENDERING CODE */

/*------------------------------------------------------------------------
Method:  void renderAllMensuralEvents()
Purpose: Normal rendering cycle, beginning wherever individual voices in
         the voice list are currently positioned
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void positionMensuralEventsOld()
  {
    do
      {
        /* render untimed events and advance voice x-positions
           do multiple untimed-event renders if variants requiring respacing
           have been inserted */
        boolean varRespaced;
        do
          {
            advanceVoices(renderUntimedEvents());

            varRespaced=false;
            if (addSpaceAfterVar())
              {
                varRespaced=true;
              }
            else if (errorRespacingTime!=null && liststart!=null &&
                liststart.musictime.greaterThanOrEqualTo(errorRespacingTime))
              {
                renderUnspacedVariantRemainder();
                varRespaced=true;

        while (liststart!=null && liststart.musictime.toDouble()>curmeasure.startMusicTime.toDouble()+curmeasure.numMinims)
          advanceOneMeasureSpacing();
              }
          }
        while (varRespaced && untimedEventsWaiting());

        /* advance one measure if all untimed events at the right border of the current
           measure have been rendered */
if (liststart!=null && liststart.v.getEvent(liststart.evloc)==null)
{
System.out.println("event==null v="+liststart.voicenum+" evloc="+liststart.evloc);
for (int ei=0; ei<liststart.evloc; ei++)
  liststart.v.getEvent(ei).prettyprint();
}
        if (liststart!=null && liststart.musictime.toDouble()>=curmeasure.startMusicTime.toDouble()+curmeasure.numMinims &&
            !(getxspacing(liststart.v.getEvent(liststart.evloc))==XSPACING_NOSPACE &&
              getXPosType(liststart.v.getEvent(liststart.evloc))==XPOS_BEFORENEXT))
          advanceOneMeasureSpacing();

        /* render timed events and advance voice x-positions */
        advanceVoices(renderTimedAndImmediateEvents());

        /* advance one measure if leftmost voice is past current measure limit */
        while (liststart!=null && liststart.musictime.toDouble()>curmeasure.startMusicTime.toDouble()+curmeasure.numMinims)
          advanceOneMeasureSpacing();

        /* respace for variant if necessary */
        if (errorRespacingTime!=null && liststart!=null &&
            liststart.musictime.greaterThanOrEqualTo(errorRespacingTime))
          renderUnspacedVariantRemainder();

        /* advance one measure if leftmost voice is past current measure limit */
        while (liststart!=null && liststart.musictime.toDouble()>curmeasure.startMusicTime.toDouble()+curmeasure.numMinims)
          advanceOneMeasureSpacing();
      }
    while (liststart!=null);
  }

  boolean addSpaceAfterVar()
  {
    boolean spaceAdded=false;

    for (VoiceGfxInfo v : voicegfx)
      if (v.respaceAfterVar)
        {
          v.lastx=v.xloc;
          double xlocadd=MINIMSCALE*v.varDefaultTimeAdd.toDouble();
          v.xloc+=xlocadd;
//            xadd+=xlocadd;

          v.musictime.add(v.varDefaultTimeAdd);
          v.varDefaultTimeAdd=null;
          v.respaceAfterVar=false;

          spaceAdded=true;
//System.out.println("space added");

          recalcVoiceList(v);
        }

    return spaceAdded;
  }

/*------------------------------------------------------------------------
Method:  double renderUntimedEvents()
Purpose: Render untimed events for all voices in the leftmost place (i.e. those
         at the front of the voice list)
Parameters:
  Input:  -
  Output: -
  Return: Total amount of horizontal space needed by the events
------------------------------------------------------------------------*/

  double renderUntimedEvents()
  {
    double       xadd=0;
    VoiceGfxInfo curvoice,nextvoice;
    Event        e;

    if (liststart==null)
      return xadd;

    starttime=new Proportion(liststart.musictime);
    nextvoice=liststart.next;
    for (curvoice=liststart;
         curvoice!=null && curvoice.musictime.equals(starttime);
         curvoice=nextvoice)
      {
        nextvoice=curvoice.next;
        if (curvoice.replacementEvents.size()>0)
          xadd=insertReplacementEvents(curvoice,xadd);
        for (e=curvoice.v.getEvent(curvoice.evloc);
             e!=null && e.getmusictime().i1==0;
             e=curvoice.v.getEvent(incrementVoicePosition(curvoice)))
          if (getXPosType(e)==XPOS_WITHNEXT)
            curvoice.withnextEvents++;
          else
            xadd=renderUntimedEvent(curvoice,e,xadd);
      }

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  boolean untimedEventsWaiting()
Purpose: Check whether any untimed events are waiting at the head of the
         rendering queue
Parameters:
  Input:  -
  Output: -
  Return: true if any untimed events are at the heading of the rendering
          queue
------------------------------------------------------------------------*/

  boolean untimedEventsWaiting()
  {
    if (liststart==null)
      return false;

//    Proportion starttime=new Proportion(liststart.musictime);
    for (VoiceGfxInfo curvoice=liststart;
         curvoice!=null && curvoice.musictime.equals(starttime);
         curvoice=curvoice.next)
      {
        Event e=curvoice.v.getEvent(curvoice.evloc);
        if (e!=null && e.getmusictime().i1==0)
          return true;
      }

    return false;
  }

/*------------------------------------------------------------------------
Method:  double addWithnextEvents(VoiceGfxInfo v,double xadd)
Purpose: Render events with positioning XPOS_WITHNEXT, i.e., untimed events
         which are always in the same measure/position as the next event
Parameters:
  Input:  VoiceGfxInfo v    - voice to update
          double       xadd - positioning adjuster for all voices at the
                              end of untimed event rendering
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  boolean addingWithnextEvents=false;

  double addWithnextEvents(VoiceGfxInfo v,double xadd)
  {
    if (addingWithnextEvents) /* avoid re-entry */
      return xadd;
    addingWithnextEvents=true;

    for (int i=0; i<v.withnextEvents; i++)
      xadd=renderUntimedEvent(v,v.v.getEvent(v.evloc-v.withnextEvents+i),xadd);
    v.withnextEvents=0;

    addingWithnextEvents=false;
    return xadd;
  }

/*------------------------------------------------------------------------
Method:  double renderUntimedEvent(VoiceGfxInfo v,Event e,double xadd)
Purpose: Render and finish one untimed event
Parameters:
  Input:  VoiceGfxInfo v    - voice to update
          Event        e    - event to render
          double       xadd - positioning adjuster for all voices at the
                              end of untimed event rendering
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  double renderUntimedEvent(VoiceGfxInfo v,Event e,double xadd)
  {
    RenderedEvent re;
    RenderList    rl=eventinfo[v.voicenum];
    double        curx;

    xadd=addWithnextEvents(v,xadd);

    e=setVoiceParameters(v,e);
    if (skipevents==0)
      {
        /* render event and advance this voice */
        re=rl.addevent(true,e,new RenderParams(
                curMeasureNum,
                v.clefEvents,v.lastNoteEvent,v.mensEvent,
                v.curProportion,v.curColoration,v.inEditorialSection,v.missingInVersion,
                v.ligInfo,false,v.tieInfo,
                v.v.getMetaData().getSuggestedModernClef(),
                v.varReadingInfo));
        re.setxloc(curx=v.calcXLoc(rl,re));
        re.setmusictime(v.musictime);
        setVoiceEventParameters(v,re);
      }
    else
      {
        re=rl.addevent(false,e,new RenderParams(
                curMeasureNum,
                v.clefEvents,v.lastNoteEvent,v.mensEvent,
                v.curProportion,v.curColoration,v.inEditorialSection,v.missingInVersion,
                v.ligInfo,false,v.tieInfo,
                v.v.getMetaData().getSuggestedModernClef(),
                v.varReadingInfo));
        re.setxloc(curx=v.xloc);
        re.setmusictime(v.musictime);
      }
    xadd=finishUntimedEvent(v,re,curx,xadd);

    if (skipevents>0)
      skipevents--;
    if (skipevents==0)
      xadd=insertReplacementEvents(v,xadd);

    return xadd;
  }

  void setVoiceEventParameters(VoiceGfxInfo v,RenderedEvent re)
  {
    Event e=re.getEvent();

    if (e.hasSignatureClef())
      {
        v.clefEvents=re.getClefEvents();
        v.clefedata=getPrincipalClefData(v);
      }
    if (e.getMensInfo()!=null)
      v.mensEvent=re;
  }

/*------------------------------------------------------------------------
Method:  Event setVoiceParameters(VoiceGfxInfo v,Event e)
Purpose: Set voice parameters if modified by an event about to be rendered
Parameters:
  Input:  VoiceGfxInfo  v - voice to update
          Event         e - event to be rendered
  Output: -
  Return: event to be rendered, replaced if necessary
------------------------------------------------------------------------*/

  Event setVoiceParameters(VoiceGfxInfo v,Event e)
  {
    if (skipevents>0)
      return e;

    v.lastNoteEvent=null;

    if (e.hasSignatureClef())
      {
        int skippedClefEvents=calcNumSkippedClefEvents(v,v.evloc);
        if (skippedClefEvents>0)
          skipevents+=skippedClefEvents;
        if (useModernClefs && e.geteventtype()==Event.EVENT_MULTIEVENT && e.hasPrincipalClef())
          {
            /* separate modern clefs from accidentals */
            for (Iterator ei=((MultiEvent)e).iterator(); ei.hasNext();)
              {
                Event ee=(Event)ei.next();
                if (ee.geteventtype()==Event.EVENT_CLEF && !ee.hasPrincipalClef()) //&& !useModernAccSystem)
                  v.replacementEvents.add(ee);
              }
            e=((MultiEvent)e).noSigClefEvent();
          }
              
/*        else
          if (v.clefevent==-1 ||
              e.getClefSet(useModernAccSystem)!=v.clefedata.getClefSet(useModernAccSystem))
            {
              v.clefevent=v.revloc;
              v.clefedata=e;
            }*/
      }

    Mensuration mensInfo=e.getMensInfo();
    if (mensInfo!=null)
      {
        if (baseMensuration==null ||
            /* if at least two voices change to the same mensuration
               simultaneously, use as main mensuration */
            (v.musictime.equals(lastMensTime) && lastMens.equals(mensInfo)))
          {
            /* set base mensuration info for score measure structure */
            baseMensuration=mensInfo;
            numMinimsInBreve=baseMensuration.prolatio*baseMensuration.tempus;

            double oldBS=BREVESCALE;
            BREVESCALE=MINIMSCALE*numMinimsInBreve;

            /* change current bar if mensuration takes effect here */
            if (v.musictime.lessThan(curmeasure.getEndMusicTime()))
              {
                curmeasure.numMinims=numMinimsInBreve;
                curmeasure.xlength+=BREVESCALE-oldBS;
              }
          }
        lastMens=mensInfo;
        lastMensTime=v.musictime;
      }

    if (e.geteventtype()==Event.EVENT_DOT &&
        (((DotEvent)e).getdottype() & DotEvent.DT_Addition)!=0)
      v.lastNoteEvent=getLastNoteEvent(v,v.revloc-1);

    /* skip lineend custodes unless displaying newline clefs */
    if (!displayallnewlineclefs && e.geteventtype()==Event.EVENT_CUSTOS)
      {
        Event nexte=v.v.getEvent(v.evloc+1);
        if (nexte!=null && nexte.geteventtype()==Event.EVENT_LINEEND)
          skipevents++;
      }

    /* at line end, construct display for next clef set */
    if (e.geteventtype()==Event.EVENT_LINEEND && !displayallnewlineclefs)
      {
        Event nexte=v.v.getEvent(v.evloc+1);
        skipevents++;
        if (nexte!=null && nexte.hasSignatureClef() && nexte.principalClefEquals(v.clefedata,useModernClefs))
          {
            if (useModernAccSystem ||
                nexte.getModernKeySig().equals(e.getModernKeySig()))
//                !nexte.getClefSet(useModernAccSystem).contradicts(v.clefEvents.getLastClefSet(useModernAccSystem),useModernClefs,v.v.getMetaData().getSuggestedModernClef()))
              skipevents+=calcNumClefEvents(v,v.evloc+1);
            if (useModernAccSystem &&
                !nexte.getModernKeySig().equals(e.getModernKeySig()))
//                nexte.getClefSet(useModernAccSystem).contradicts(v.clefEvents.getLastClefSet(useModernAccSystem),useModernClefs,v.v.getMetaData().getSuggestedModernClef()))
              v.replacementEvents.addAll(constructDisplayClefSet(v.clefEvents,nexte));
          }
      }

    if (e.geteventtype()==Event.EVENT_PROPORTION)
      {
        Proportion newp=((ProportionEvent)e).getproportion();
        v.curProportion=new Proportion(v.curProportion);
        v.curProportion.multiply(newp);
      }
    if (e.geteventtype()==Event.EVENT_COLORCHANGE)
      v.curColoration=new Coloration(v.curColoration,((ColorChangeEvent)e).getcolorscheme());

    if (e.geteventtype()==Event.EVENT_VARIANTDATA_START)
      {
//System.out.println("startvar: V"+v.voicenum+", evloc="+v.evloc);
        Event nextEvent=v.v.getEvent(v.evloc+1);
        int   skippedClefEvents=calcNumSkippedClefEvents(v,v.evloc+1);
        Event nextDisplayedEvent=v.v.getEvent(v.evloc+skippedClefEvents+1);

        if (skippedClefEvents>0 && nextDisplayedEvent.geteventtype()==Event.EVENT_VARIANTDATA_END)
          /* nothing displayed in editorial section: don't display brackets */
          skipevents+=skippedClefEvents+2;
        else
          {
//System.out.println("startvar: V"+v.voicenum+", evloc="+v.evloc);
//            v.inEditorialSection=true;
            v.varReadingInfo=new RenderedEventGroup(v.revloc);
            v.varReadingInfo.grouptype=RenderedEventGroup.EVENTGROUP_VARIANTREADING;
            v.varReadingInfo.varReading=e.getVariantReading(musicData.getVersion());
            v.varReadingInfo.varMarker=(VariantMarkerEvent)e;
            if (v.varReadingInfo.varReading!=null && v.varReadingInfo.varReading.isError())
              {
                Proportion defaultReadingLength=((VariantMarkerEvent)e).getDefaultLength(),
                           varReadingLength=v.varReadingInfo.varReading.getLength();
                if (varReadingLength.greaterThan(defaultReadingLength))
                  {
                    errorRespacingTime=Proportion.sum(v.musictime,defaultReadingLength);
                    variantVoice=v;
//System.out.println("var > default: new respacing time: "+errorRespacingTime);
                 }
                else if (defaultReadingLength.greaterThan(varReadingLength))
                  {
                    v.varDefaultTimeAdd=Proportion.difference(defaultReadingLength,varReadingLength);
                  }
              }
          }
      }
    if (e.geteventtype()==Event.EVENT_VARIANTDATA_END)
      {
        if (v.varDefaultTimeAdd!=null)
          v.respaceAfterVar=true;

        if (v.varReadingInfo==null)
          {
            System.err.println("Error! VARIANTDATA_END without START; V"+v.voicenum+", evloc="+v.evloc);
            for (int ei=0; ei<v.evloc; ei++)
              v.v.getEvent(ei).prettyprint();
          }
//else
//System.out.println("endvar: V"+v.voicenum+", evloc="+v.evloc);

        v.varReadingInfo.lastEventNum=v.revloc-1;
        v.varReadingInfo.calculateYMinMax(eventinfo[v.voicenum]);
        if (v.varReadingInfo.firstEventNum==v.varReadingInfo.lastEventNum)
          eventinfo[v.voicenum].getEvent(v.varReadingInfo.firstEventNum).setDisplay(true);
/*        v.varReadingInfo.lastEventNum++;
        v.varReadingInfo=null;
        v.inEditorialSection=false;*/
      }

    return e;
  }

  RenderedEvent getLastNoteEvent(VoiceGfxInfo v,int ei)
  {
    for (; ei>=0; ei--)
      {
        RenderedEvent curre=eventinfo[v.voicenum].getEvent(ei);
        if (curre.getEvent().hasEventType(Event.EVENT_NOTE))
          return curre;
      }
    return null;
  }

/*------------------------------------------------------------------------
Method:  double insertReplacementEvents(VoiceGfxInfo v,double xadd)
Purpose: Insert newly-created events into render list
Parameters:
  Input:  VoiceGfxInfo v    - voice to update
          double       xadd - positioning adjuster
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  double insertReplacementEvents(VoiceGfxInfo v,double xadd)
  {
    RenderedEvent re,lastevent=null;
    RenderList    rl=eventinfo[v.voicenum];
    double        curx;

    for (Event e : v.replacementEvents)
      {
        /* render event */
        re=rl.addevent(true,e,new RenderParams(
                curMeasureNum,
                v.clefEvents,lastevent,v.mensEvent,
                v.curProportion,v.curColoration,v.inEditorialSection,v.missingInVersion,
                v.ligInfo,false,v.tieInfo,
                v.v.getMetaData().getSuggestedModernClef(),
                v.varReadingInfo));
        re.setxloc(curx=v.calcXLoc(rl,re));
        xadd=finishUntimedEvent(v,re,curx,xadd);

        /* set voice event parameters */
        if (e.hasSignatureClef())
          {
            v.clefEvents=new RenderedClefSet(v.clefEvents,re,useModernAccSystem,v.v.getMetaData().getSuggestedModernClef());
            v.clefedata=getPrincipalClefData(v);
          }
      }
    v.replacementEvents=new ArrayList<Event>();
    return xadd;
  }

/*------------------------------------------------------------------------
Method:  int calcNumSkippedClefEvents(VoiceGfxInfo v,int ei)
Purpose: Calculate number of clef events which go undisplayed if clef-display
         customization is used (useModernClefs, !displayallnewlineclefs, etc)
Parameters:
  Input:  VoiceGfxInfo v - voice being checked
          int ei         - index of event to check
  Output: -
  Return: number of events to be skipped
------------------------------------------------------------------------*/

  int calcNumSkippedClefEvents(VoiceGfxInfo v,int ei)
  {
    Event e=v.v.getEvent(ei);
    if (!e.hasSignatureClef())
      return 0;

    if (v.clefEvents!=null && e.hasPrincipalClef() &&
        ((useModernClefs &&
          e.getClefSet(useModernAccSystem)!=v.clefedata.getClefSet(useModernAccSystem) &&
          !e.getClefSet(useModernAccSystem).contradicts(v.clefedata.getClefSet(useModernAccSystem),useModernClefs,v.v.getMetaData().getSuggestedModernClef()))
        || (!displayallnewlineclefs &&
            e.hasPrincipalClef() &&
            !e.getClefSet(useModernAccSystem).contradicts(v.clefedata.getClefSet(useModernAccSystem),useModernClefs,v.v.getMetaData().getSuggestedModernClef()))))
      return calcNumClefEvents(v,ei);

    return 0;
  }

  /* calculate number of events in a clef set at a specified location */
  int calcNumClefEvents(VoiceGfxInfo v,int ei)
  {
    Event   clefe=v.v.getEvent(ei);
    ClefSet origcs=clefe!=null ? clefe.getClefSet(useModernAccSystem) : null;
    int     numEvents=0;
    while (clefe!=null)
      {
        ClefSet cs=clefe.getClefSet(useModernAccSystem);
        if (cs==null || cs!=origcs)
          return numEvents;
        else
          numEvents++;
        clefe=v.v.getEvent(++ei);
      }

    return numEvents;
  }

/*------------------------------------------------------------------------
Method:  ArrayList<Event> constructDisplayClefSet(Event curc,Event newc)
Purpose: Prepare new clef display for insertion into rendering (display
         depends upon modern/original clefs etc)
         Entry condition: newc contradicts curc, but the principal clefs are
                          equal (i.e., only signature clefs differ)
Parameters:
  Input:  Event curc - current clef information
          Event newc - new clef information
  Output: -
  Return: list of new events for clef set display
------------------------------------------------------------------------*/

  ArrayList<Event> constructDisplayClefSet(RenderedClefSet curRCS,Event newc)
  {
    ClefSet          curcs=curRCS.getLastClefSet(useModernAccSystem),
                     newcs=newc.getClefSet(useModernAccSystem);
    Event            le=null,
                     curc=curRCS.getPrincipalClefEvent().getEvent();
    ArrayList<Event> elist=new ArrayList<Event>();

    /* if orig clefs, show line end */
    if (!(useModernClefs || useModernAccSystem))
      elist.add(new LineEndEvent());

     /* need to remove flats? */
    if (newcs.numflats()<curcs.numflats())
      if (!useModernAccSystem)
        {
          /* add principal clef */
          elist.add(le=new ClefEvent(newcs.getprincipalclef(),null,null));
          curc=le; curcs=curc.getClefSet(useModernAccSystem);
        }
      else
        /* add naturals */
        for (Iterator i=curcs.acciterator(); i.hasNext();)
          {
            Clef c=(Clef)i.next();
            if (!newcs.containsClef(c))
              elist.add(le=new ClefEvent(new Clef(Clef.CLEF_MODERNNatural,c.linespacenum,c.pitch,true,true,newcs.getprincipalclef()),le,null));
          }

    /* need to add flats? */
    if (newcs.numflats()>curcs.numflats())
      for (Iterator i=newcs.acciterator(); i.hasNext();)
        {
          Clef c=(Clef)i.next();
          if (!curcs.containsClef(c))
            elist.add(le=new ClefEvent(c,le,curc));
        }

    return elist;
  }

/*------------------------------------------------------------------------
Method:  ArrayList<Event> constructVarTextEvents(VoiceGfxInfo v,[OriginalTextEvent te])
Purpose: Create text events to display variant texting at current variant location
Parameters:
  Input:  VoiceGfxInfo v       - voice in which to render
          OriginalTextEvent te - text in default version to copy
  Output: -
  Return: list of new events for variant text display
------------------------------------------------------------------------*/

  /* calculate what the "default" versions are at one set of readings */
  ArrayList<VariantVersionData> getDefaultVersions(VoiceGfxInfo v)
  {
    if (v.varReadingInfo==null)
      return new ArrayList<VariantVersionData>();

    ArrayList<VariantVersionData> defaultVersions=
      new ArrayList<VariantVersionData>(fullPieceData.getVariantVersions());

    /* remove anything with a variant reading */
    VariantMarkerEvent varEvent=v.varReadingInfo.varMarker;
    for (VariantReading vr : varEvent.getReadings())
      for (VariantVersionData vvd : vr.getVersions())
        defaultVersions.remove(vvd);

    /* remove versions which are missing this voice */
    for (VariantVersionData vvd : fullPieceData.getVariantVersions())
      if (vvd.isVoiceMissing(musicData.getVoice(v.voicenum).getMetaData()) ||
          musicData.getVoice(v.voicenum).getMissingVersions().contains(vvd))
        defaultVersions.remove(vvd);

    return defaultVersions;
  }

  ArrayList<Event> constructVarTextEvents(VoiceGfxInfo v,OriginalTextEvent te)
  {
    ArrayList<Event> textEvents=new ArrayList<Event>();

if (!musicData.isDefaultVersion())
return textEvents;

    /* construct list of versions using this reading */
    ArrayList<VariantVersionData> displayVersions=
      new ArrayList<VariantVersionData>(fullPieceData.getVariantVersions());

    if (v.varReadingInfo!=null)
      if (musicData.isDefaultVersion())
        {
          VariantMarkerEvent varEvent=v.varReadingInfo.varMarker;
          for (VariantReading vr : varEvent.getReadings())
            for (VariantVersionData vvd : vr.getVersions())
              displayVersions.remove(vvd);
        }
      else
        {
          VariantMarkerEvent varEvent=v.varReadingInfo.varMarker;
          for (VariantReading vr : varEvent.getReadings())
            if (vr!=v.varReadingInfo.varReading)
              for (VariantVersionData vvd : vr.getVersions())
                displayVersions.remove(vvd);
          displayVersions.remove(fullPieceData.getDefaultVariantVersion());
        }

    for (VariantVersionData vvd : fullPieceData.getVariantVersions())
      if (vvd.isVoiceMissing(musicData.getVoice(v.voicenum).getMetaData()) ||
          musicData.getVoice(v.voicenum).getMissingVersions().contains(vvd))
        displayVersions.remove(vvd);

    for (VariantVersionData vvd : displayVersions)
      textEvents.add(new OriginalTextEvent(te.getText(),vvd));

    return textEvents;
  }

  ArrayList<Event> constructVarTextEvents(VoiceGfxInfo v)
  {
    ArrayList<Event> textEvents=new ArrayList<Event>();

    VariantMarkerEvent varEvent=v.varReadingInfo.varMarker;

    /* if displaying variant version, add text from default reading */
    if (!musicData.isDefaultVersion())
      {
        int ei=varEvent.getDefaultListPlace()+1,
            eType=-1;
        VoiceEventListData defaultVL=
          fullPieceData.getDefaultMusicData().getSection(sectionNum).getVoice(v.voicenum);
        ArrayList<VariantVersionData> defaultVersions=getDefaultVersions(v);
        do
          {
            Event e=defaultVL.getEvent(ei);
            eType=e.geteventtype();
            if (eType==Event.EVENT_ORIGINALTEXT)
              for (VariantVersionData dv : defaultVersions)
                textEvents.add(new OriginalTextEvent(((OriginalTextEvent)e).getText(),
                               dv));

            ei++;
          }
        while (eType!=Event.EVENT_VARIANTDATA_END);
      }

    /* do variant readings */
    for (VariantReading vr : varEvent.getReadings())
      {
        int numEvents=vr.getNumEvents();
        for (int ei=0; ei<numEvents; ei++)
          {
            Event e=vr.getEvent(ei);
            if (e.geteventtype()==Event.EVENT_ORIGINALTEXT)
              for (VariantVersionData vvd : vr.getVersions())
                textEvents.add(new OriginalTextEvent(((OriginalTextEvent)e).getText(),vvd));
          }
      }

    return textEvents;
  }

/*------------------------------------------------------------------------
Method:  double finishUntimedEvent(VoiceGfxInfo v,RenderedEvent re,
                                   double curx,double xadd)
Purpose: Advance voice info counters and x positioning information after
         rendering an untimed event
Parameters:
  Input:  VoiceGfxInfo  v    - voice to update
          RenderedEvent re   - event which has just been rendered
          double        curx - current renderer x position
          double        xadd - positioning adjuster for all voices at the
                              end of untimed event rendering
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  double finishUntimedEvent(VoiceGfxInfo v,RenderedEvent re,
                            double curx,double xadd)
  {
    v.revloc++;

    double eventxlen=re.getrenderedxsize();
    if (getxspacing(re.getEvent())==XSPACING_NOSPACE)
      {
        if (curx+eventxlen>v.xloc)
          eventxlen=curx+eventxlen-v.xloc;
        else
          eventxlen=0;
      }
    if (getXPosType(re.getEvent())==XPOS_SIMULTANEOUS)
      eventxlen=0;
    if (re.isdisplayed()) /* leave lastx alone if this event wasn't */
      v.lastx=curx;       /* displayed */
    v.xloc+=eventxlen;
    v.xadd+=eventxlen;
    if (v.xadd>xadd)
      xadd=v.xadd;

    if (re.getEvent().geteventtype()==Event.EVENT_VARIANTDATA_END)
      {
        if (skipevents==0)
          v.varReadingInfo.lastEventNum++;
        v.varReadingInfo=null;
//        v.inEditorialSection=false;
      }

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  void renderUnspacedVariantRemainder()
Purpose: When a variant error is longer than the default reading, finish
         rendering it with non-score spacing, then apply extra spacing to
         the non-variant voices so that the parts re-align properly after
         the error
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void renderUnspacedVariantRemainder()
  {
    double xadd=0;

    Event e;
    for (e=variantVoice.v.getEvent(variantVoice.evloc);
         e.geteventtype()!=Event.EVENT_VARIANTDATA_END;
         e=variantVoice.v.getEvent(incrementVoicePosition(variantVoice)))
      xadd=renderUntimedEvent(variantVoice,e,xadd);
    xadd=renderUntimedEvent(variantVoice,e,xadd);
    incrementVoicePosition(variantVoice);

    advanceVoices(xadd);

    errorRespacingTime=null;
    variantVoice=null;
  }

/*------------------------------------------------------------------------
Method:  int incrementVoicePosition(VoiceGfxInfo v)
Purpose: Advance event index within one voice list, remove if done
Parameters:
  Input:  VoiceGfxInfo  v - voice to update
  Output: -
  Return: new event index
------------------------------------------------------------------------*/

  int incrementVoicePositionOld(VoiceGfxInfo v)
  {
    v.evloc++;
    removeVoiceIfFinished(v);
    return v.evloc;
  }

/*------------------------------------------------------------------------
Method:  void removeVoice[IfFinished](VoiceGfxInfo v)
Purpose: Remove voice from rendering to-do list [if all its events have
         been rendered]
Parameters:
  Input:  VoiceGfxInfo  v - voice to update
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void removeVoiceIfFinishedOld(VoiceGfxInfo v)
  {
    if (v.v.getEvent(v.evloc)==null)
      removeVoice(v);
  }

  void removeVoiceOld(VoiceGfxInfo v)
  {
    if (v.next!=null)
      v.next.last=v.last;
    if (v.last!=null)
      v.last.next=v.next;
    else
      liststart=v.next;
  }

/*------------------------------------------------------------------------
Method:  void advanceVoices(double xadd)
Purpose: Move all voices forward (horizontally) after events
Parameters:
  Input:  double xadd - total amount for voices to shift
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void advanceVoicesOld(double xadd)
  {
    if (xadd>0)
      {
        curmeasure.xlength+=xadd;
        for (int i=0; i<numVoices; i++)
          if (musicData.getVoice(i)!=null)
            {
              if (voicegfx[i].xadd<xadd)
                {
                  double vxadd=xadd-voicegfx[i].xadd;
                  voicegfx[i].xloc+=vxadd;
                  if (liststart!=null && voicegfx[i].musictime.toDouble()>=liststart.musictime.toDouble())
                    voicegfx[i].moveupBEFORENEXTevents(eventinfo[i],vxadd);
                }
              voicegfx[i].xadd=0;
            }
      }
  }

  void advanceOneMeasureSpacingOld()
  {
    /* deal with end of bar issues */
    barxstart+=curmeasure.xlength;
    curMeasureNum++;
//    curmeasure=measures.newMeasure(curMeasureNum,curmeasure.startMusicTime+curmeasure.numMinims,numMinimsInBreve,
//                                   BREVESCALE,barxstart);
    for (int i=0; i<numVoices; i++)
      {
        curmeasure.reventindex[i]=voicegfx[i].revloc;
        curmeasure.startClefEvents[i]=voicegfx[i].clefEvents;
        curmeasure.startMensEvent[i]=voicegfx[i].mensEvent;
      }

    /* space for barlines */
    if (options.get_barline_type()!=OptionSet.OPT_BARLINE_NONE)
      {
        double bxadd=BARLINE_XADD;
        switch (options.get_barline_type())
          {
            case OptionSet.OPT_BARLINE_MENSS:
            case OptionSet.OPT_BARLINE_MODERN:
              bxadd*=2;
              break;
          }
        curmeasure.xlength+=bxadd;
        for (int i=0; i<numVoices; i++)
          voicegfx[i].xloc+=bxadd;
      }
  }

/*------------------------------------------------------------------------
Method:  double renderTimedandImmediateEvents()
Purpose: Render one timed event for each voice in last place along with
         XPOS_IMMEDIATE untimed events following it (e.g., dots)
Parameters:
  Input:  -
  Output: -
  Return: Total amount of horizontal space needed by the events
------------------------------------------------------------------------*/

  double renderTimedAndImmediateEvents()
  {
    VoiceGfxInfo  curvoice,nextvoice,moveplace;
    Event         e;
    double        xadd=0;
    Proportion    curevmusictime;

    if (liststart==null)
      return xadd;

    nextvoice=liststart.next;
    for (curvoice=liststart;
         curvoice!=null && curvoice.musictime.toDouble()<=starttime.toDouble();
         curvoice=nextvoice)
      {
        nextvoice=curvoice.next;

        e=curvoice.v.getEvent(curvoice.evloc);
        if (e==null)
          removeVoice(curvoice);
        else if (e.geteventtype()==Event.EVENT_ELLIPSIS)
          {
            removeVoice(curvoice);
            finalisList.add(curvoice);
          }
        else
          {
            xadd=renderTimedEvent(curvoice,e,xadd);
            recalcVoiceList(curvoice);
          }
      }

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  void recalcVoiceList(VoiceGfxInfo curvoice)
Purpose: Adjust pointers within voice list to maintain sorting order after one
         voice has moved forward
Parameters:
  Input:  VoiceGfxInfo curvoice - voice which has just been advanced (i.e. in
                                  which timed events have just been rendered)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void recalcVoiceListOld(VoiceGfxInfo curvoice)
  {
    VoiceGfxInfo moveplace=curvoice;
    while (!((moveplace.next==null) ||
             (moveplace.next.musictime.greaterThan(curvoice.musictime))))
      moveplace=moveplace.next;

    if (moveplace!=curvoice)
      {
        removeVoice(curvoice);

        /* re-insert */
        if (moveplace.next!=null)
          moveplace.next.last=curvoice;
        curvoice.next=moveplace.next;
        curvoice.last=moveplace;
        moveplace.next=curvoice;
      }
  }

  boolean doNoteEventLigInfo(VoiceGfxInfo curvoice,NoteEvent ne)
  {
    if (curvoice.ligInfo.firstEventNum==-1 && ne.isligated())
      curvoice.ligInfo=new RenderedLigature(curvoice.v,eventinfo[curvoice.voicenum]); /* start new ligature */
    return curvoice.ligInfo.update(curvoice.revloc,ne);
  }

  boolean doMultiEventLigInfo(VoiceGfxInfo curvoice,MultiEvent me)
  {
    boolean   endlig=false;
    NoteEvent ligEv=(NoteEvent)me.getFirstEventOfType(Event.EVENT_NOTE); /* TMP: choose one NoteEvent to be in lig */

    if (ligEv!=null)
      endlig=doLigInfo(curvoice,ligEv);
    return endlig;
  }

  boolean doLigInfo(VoiceGfxInfo curvoice,Event e)
  {
    switch (e.geteventtype())
      {
        case Event.EVENT_NOTE:
          return doNoteEventLigInfo(curvoice,(NoteEvent)e);
        case Event.EVENT_MULTIEVENT:
          return doMultiEventLigInfo(curvoice,(MultiEvent)e);
      }
    return false;
  }

  void doNoteEventTieEndInfo(VoiceGfxInfo curvoice,NoteEvent ne)
  {
    /* end of a tie? */
    if (curvoice.tieInfo.firstEventNum!=-1)
      curvoice.tieInfo.update(curvoice.revloc,ne);
  }

  void doNoteEventTieStartInfo(VoiceGfxInfo curvoice,NoteEvent ne)
  {
    curvoice.tieInfo=new RenderedLigature(curvoice.v,eventinfo[curvoice.voicenum],RenderedLigature.TIE); /* start new tie */

    /* start of a new tie? */
    if (ne.getTieType()!=NoteEvent.TIE_NONE)
      curvoice.tieInfo.update(curvoice.revloc,ne);
  }

  void doMultiEventTieEndInfo(VoiceGfxInfo curvoice,MultiEvent me)
  {
    NoteEvent tieEv=(NoteEvent)me.getFirstEventOfType(Event.EVENT_NOTE); /* TMP: choose one NoteEvent to be in lig */

    if (tieEv!=null)
      doTieEndInfo(curvoice,tieEv);
  }

  void doMultiEventTieStartInfo(VoiceGfxInfo curvoice,MultiEvent me)
  {
    NoteEvent tieEv=(NoteEvent)me.getFirstEventOfType(Event.EVENT_NOTE); /* TMP: choose one NoteEvent to be in lig */

    if (tieEv!=null)
      doTieStartInfo(curvoice,tieEv);
  }

  void doTieEndInfo(VoiceGfxInfo curvoice,Event e)
  {
    switch (e.geteventtype())
      {
        case Event.EVENT_NOTE:
          doNoteEventTieEndInfo(curvoice,(NoteEvent)e);
          break;
        case Event.EVENT_MULTIEVENT:
          doMultiEventTieEndInfo(curvoice,(MultiEvent)e);
          break;
      }
  }

  void doTieStartInfo(VoiceGfxInfo curvoice,Event e)
  {
    switch (e.geteventtype())
      {
        case Event.EVENT_NOTE:
          doNoteEventTieStartInfo(curvoice,(NoteEvent)e);
          break;
        case Event.EVENT_MULTIEVENT:
          doMultiEventTieStartInfo(curvoice,(MultiEvent)e);
          break;
      }
  }

/*------------------------------------------------------------------------
Method:  double renderTimedEvent(VoiceGfxInfo curvoice,Event e,double xadd)
Purpose: Render one timed event along with XPOS_IMMEDIATE untimed events
         following it (e.g., dots)
Parameters:
  Input:  VoiceGfxInfo curvoice - voice of event
          Event e               - event to render
          double xadd           - total amount of horizontal space needed
                                  for adjusting all voices after this render
                                  phase
  Output: -
  Return: new value for xadd
------------------------------------------------------------------------*/

  double renderTimedEvent(VoiceGfxInfo curvoice,Event e,double xadd)
  {
    RenderList rl=eventinfo[curvoice.voicenum];

    xadd=addWithnextEvents(curvoice,xadd);

    boolean endlig=doLigInfo(curvoice,e);

    /* handle multi-event special cases */
    if (e.geteventtype()==Event.EVENT_MULTIEVENT)
      {
        if (useModernClefs && e.hasEventType(Event.EVENT_CLEF))
          {
            /* when switching to modern cleffing, do not allow clefs/accidentals
               to co-exist with timed events (e.g., flat above a rest) */
            for (Iterator i=((MultiEvent)e).iterator(); i.hasNext();)
              {
                Event ne=(Event)i.next();
                if (ne.geteventtype()==Event.EVENT_CLEF)
                  curvoice.replacementEvents.add(ne); /* queue clef events for later rendering */
              }
            e=((MultiEvent)e).noClefEvent();
          }
        setVoiceParameters(curvoice,e);
      }

    /* render event */
    RenderedEvent re=rl.addevent(true,e,new RenderParams(
            curMeasureNum,
            curvoice.clefEvents,null,curvoice.mensEvent,
            curvoice.curProportion,curvoice.curColoration,
            curvoice.inEditorialSection,curvoice.missingInVersion,
            curvoice.ligInfo,endlig,curvoice.tieInfo,
            curvoice.v.getMetaData().getSuggestedModernClef(),
            curvoice.varReadingInfo));
    re.setxloc(curvoice.xloc);
    re.setmusictime(curvoice.musictime);
    if (e.geteventtype()==Event.EVENT_MULTIEVENT)
      setVoiceEventParameters(curvoice,re);

    /* if we've just finished a ligature, reset ligInfo */
    if (endlig)
      curvoice.ligInfo=new RenderedLigature(curvoice.v,rl);

    curvoice.revloc++;
    Proportion curevmusictime=new Proportion(e.getmusictime().i1*curvoice.curProportion.i2,
                                             e.getmusictime().i2*curvoice.curProportion.i1);

    /* update sonority info */
    if (e.getFirstEventOfType(Event.EVENT_REST)!=null)
      {
        if (re.getmusictime().equals(curSonority.getMusicTime()))
          curSonority.remove(curvoice.curSoundingEvent);
        else
          {
            curSonority=curSonority.copyWithout(curvoice.curSoundingEvent);
            curSonority.setMusicTime(re.getmusictime());
            sonorityList.add(curSonority);
          }
        curvoice.curSoundingEvent=null;
      }
    if (e.getFirstEventOfType(Event.EVENT_NOTE)!=null)
      {
        if (re.getmusictime().equals(curSonority.getMusicTime()))
          {
            curSonority.remove(curvoice.curSoundingEvent);
            curSonority.add(re);
          }
        else
          {
            curSonority=curSonority.copyWithout(curvoice.curSoundingEvent);
            curSonority.add(re);
            curSonority.setMusicTime(re.getmusictime());
            sonorityList.add(curSonority);
          }
        curvoice.curSoundingEvent=re;
        re.setSonority(curSonority);
      }

    curvoice.evloc++;
    curvoice.lastx=curvoice.xloc;

if (errorRespacingTime!=null && curvoice==variantVoice &&
    Proportion.sum(curvoice.musictime,curevmusictime).greaterThan(errorRespacingTime))
  curevmusictime=Proportion.difference(errorRespacingTime,curvoice.musictime);

    double xlocadd=MINIMSCALE*curevmusictime.toDouble();
    if (xlocadd<re.getrenderedxsize())
      {
        curvoice.xadd+=re.getrenderedxsize()-xlocadd;
        if (xadd<curvoice.xadd)
          xadd=curvoice.xadd;
        xlocadd=re.getrenderedxsize();
      }
    curvoice.xloc+=xlocadd;

    /* render untimed XPOS_IMMEDIATE events (e.g., dots)
       and XPOS_SIMULTANEOUS events (vertically aligned with current event) */
    Event ute=curvoice.v.getEvent(curvoice.evloc);
    int xpostype=getXPosType(ute);
    while (ute!=null && (xpostype==XPOS_IMMEDIATE || xpostype==XPOS_SIMULTANEOUS))
      {
        xadd=renderUntimedEvent(curvoice,ute,xadd);
        ute=curvoice.v.getEvent(incrementVoicePosition(curvoice));
        xpostype=getXPosType(ute);
      }

    curvoice.musictime.add(curevmusictime);

    return xadd;
  }

/*------------------------------------------------------------------------
Method:  void renderEllipses()
Purpose: For incipit-scores: render blank space between incipits and
         explicits
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void renderEllipses()
  {
    if (finalisList.size()<=0)
      return;

    /* find starting x-coord and musictime for explicits */
    double     newx=0.0;
    for (int i=0; i<voicegfx.length; i++)
      if (voicegfx[i].xloc>newx)
        newx=voicegfx[i].xloc;
    newx+=20;
    Proportion newmt=new Proportion((int)curmeasure.startMusicTime.toDouble()+curmeasure.numMinims,1);

    VoiceGfxInfo lastv=null,
                 firstv=null,
                 curv=null;
    for (Iterator i=finalisList.iterator(); i.hasNext();)
      {
        curv=(VoiceGfxInfo)i.next();

        /* render ELLIPSIS event */
        RenderList rl=eventinfo[curv.voicenum];
        RenderedEvent re=rl.addevent(true,curv.v.getEvent(curv.evloc),new RenderParams(
                curMeasureNum,
                curv.clefEvents,null,curv.mensEvent,
                curv.curProportion,curv.curColoration,
                curv.inEditorialSection,curv.missingInVersion,
                curv.ligInfo,false,curv.tieInfo,
                curv.v.getMetaData().getSuggestedModernClef(),
                curv.varReadingInfo));
        re.setxloc(curv.xloc);
        re.setmusictime(curv.musictime);

        curv.revloc++;
        curv.evloc++;
        curv.xloc=newx;
        curv.musictime.setVal(newmt);

        /* re-construct voice list for rendering (finales only) */
        if (lastv!=null)
          {
            curv.last=lastv;
            lastv.next=curv;
          }
        else
          firstv=curv;
        lastv=curv;
      }
    liststart=firstv;

    /* new measure for explicit */
    curmeasure.xlength=newx-barxstart;
    advanceOneMeasureSpacing();
  }


/*--------------------------- CHANT-RENDERING ---------------------------*/

/*------------------------------------------------------------------------
Method:  void renderAllChantEvents(MusicChantSection musicData)
Purpose: Render plainchant section
Parameters:
  Input:  MusicChantSection musicData - music data for this section
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static final Proportion CHANT_TIME_UNIT_SHORT=new Proportion(1,1),
                          CHANT_TIME_UNIT=      new Proportion(1,1),
                          CHANT_TIME_UNIT_LONG= new Proportion(2,1),
                          CHANT_TIME_UNIT_NONE= new Proportion(0,1);

  void positionChantEvents(MusicChantSection musicData)
  {
    for (int vi=0; vi<numVoices; vi++)
      if (musicData.getVoice(vi)!=null)
      {
        VoiceChantData vd=(VoiceChantData)musicData.getVoice(vi);
        VoiceGfxInfo   vg=voicegfx[vi];
        int            numEvents=vd.getNumEvents(),
                       numRendered;
        RenderedEvent  lastre=null,
                       lastNoteEvent=null;
        double         xpadding=0,
                       newXloc;
        boolean        lastIsOrigText=false;
        Proportion     curEventLength;

        int ei=0; boolean done=ei>=numEvents;
        while (!done)
          {
            Event e=vd.getEvent(ei);
            curEventLength=CHANT_TIME_UNIT_NONE;

            e=setPrerenderParameters(vg,e);
            if (ei>0)
              switch(e.geteventtype())
                {
                  case Event.EVENT_NOTE:
                    int nt=((NoteEvent)e).getnotetype();
                    if (nt>NoteEvent.NT_Brevis)
                      {
                        xpadding=CHANT_XPADDING_B;
                        curEventLength=CHANT_TIME_UNIT_LONG;
                      }
                    else if (nt>=NoteEvent.NT_Brevis)
                      {
                        xpadding=CHANT_XPADDING_B;
                        curEventLength=CHANT_TIME_UNIT;
                      }
                    else
                      {
                        xpadding=CHANT_XPADDING_SB;
                        curEventLength=CHANT_TIME_UNIT_SHORT;
                      }
                    break;
                  case Event.EVENT_DOT:
                    xpadding=CHANT_XPADDING_DOT;
                    break;
                  case Event.EVENT_BARLINE:
                    xpadding=CHANT_XPADDING_B;
                    break;
                  default:
                    xpadding=CHANT_XPADDING_DEFAULT;
                    break;
                }

            if (e.geteventtype()==Event.EVENT_NOTE &&
                ((NoteEvent)e).isligated())
              {
                /* render as ligature */
                int firstNotePos=eventinfo[vi].size();
                numRendered=eventinfo[vi].addlig(vd,ei,
                  new RenderParams(
                    curMeasureNum,vg.clefEvents,lastre,vg.mensEvent,
                    Proportion.EQUALITY,vg.curColoration,
                    vg.inEditorialSection,vg.missingInVersion,
                    vg.ligInfo,false,vg.tieInfo,
                    vd.getMetaData().getSuggestedModernClef(),
                    vg.varReadingInfo));
                newXloc=vg.xloc+xpadding;
                if (lastIsOrigText)
                  lastre.setxloc(newXloc); /* align original texting properly */
                for (int ni=firstNotePos; ni<firstNotePos+numRendered; ni++)
                  {
                    RenderedEvent re=eventinfo[vi].getEvent(ni);
                    re.setxloc(newXloc);
                    re.setmusictime(vg.musictime);
                    lastre=re;
                    newXloc+=re.getRenderedXSizeWithoutText();
                    re.setLigEnd(ni==firstNotePos+numRendered-1);
                    re.setMusicLength(curEventLength);
                    vg.musictime.add(re.getMusicLength());
                    if (re.getEvent().geteventtype()==Event.EVENT_NOTE)
                      lastNoteEvent=re;
                  }
              }
            else
              {
                RenderedEvent re=eventinfo[vi].addevent(true,e,new RenderParams(
                  curMeasureNum,vg.clefEvents,lastre,vg.mensEvent,
                  Proportion.EQUALITY,vg.curColoration,
                  vg.inEditorialSection,vg.missingInVersion,
                  vg.ligInfo,false,vg.tieInfo,
                  vd.getMetaData().getSuggestedModernClef(),
                  vg.varReadingInfo));
                numRendered=1;
                re.setxloc(vg.xloc+xpadding);
                re.setmusictime(vg.musictime);
                if (lastIsOrigText)
                  lastre.setxloc(re.getxloc()); /* align original texting properly */
                lastre=re;
                setPostrenderParameters(vg,re,false);
                newXloc=re.getxloc()+re.getRenderedXSizeWithoutText();
                re.setMusicLength(curEventLength);
                vg.musictime.add(re.getMusicLength());
                if (re.getEvent().geteventtype()==Event.EVENT_NOTE)
                  lastNoteEvent=re;
              }

            vg.revloc+=numRendered;
            vg.evloc+=numRendered;

            if (e.geteventtype()!=Event.EVENT_ORIGINALTEXT)
              {
                vg.lastx=vg.xloc;
                vg.xloc=newXloc;
                lastIsOrigText=false;
              }
            else
              lastIsOrigText=true;

            ei+=numRendered;
            done=ei>=numEvents;

            /* make final chant note long */
            if (done && lastNoteEvent!=null)
              lastNoteEvent.setMusicLength(CHANT_TIME_UNIT_LONG);
          }
/*        vg.xloc-=CHANTSYMBOL_XPADDING;  only pad between events */

        if (curmeasure.xlength<vg.xloc)
          curmeasure.xlength=vg.xloc;
        if (curmeasure.numMinims<vg.musictime.toDouble())
          curmeasure.numMinims=(int)(vg.musictime.toDouble());
      }
  }

  void positionTextSection(MusicTextSection musicData)
  {
    EventStringImg.genericG.setFont(MusicFont.defaultTextFont);
    FontMetrics m=EventStringImg.genericG.getFontMetrics();

    curmeasure.xlength=SECTION_END_SPACING+m.stringWidth(musicData.getSectionText());
  }


/*---------------------- POST-RENDERING UTILITIES ----------------------*/

/*------------------------------------------------------------------------
Method:  void adjustMeasureEventPositions(int mnum,double Xadjust)
Purpose: Change x-position of all rendered events within a measure
Parameters:
  Input:  int mnum       - index of measure to adjust
          double Xadjust - x-value to add to each event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void adjustMeasureEventPositions(int mnum,double Xadjust)
  {
    int           starte,ende;
    RenderedEvent re;

    for (int v=0; v<numVoices; v++)
      if (eventinfo[v]!=null)
      {
        /* get starting and ending event indices for this measure */
        starte=measures.get(mnum).reventindex[v];
        if (mnum+1>=measures.size())
          ende=eventinfo[v].size()-1;
        else
          ende=measures.get(mnum+1).reventindex[v]-1;

        /* now adjust all events in measure */
        for (int ei=starte; ei<=ende; ei++)
          {
            re=eventinfo[v].get(ei);
            re.setxloc(re.getxloc()+Xadjust);
          }
      }
  }
}
