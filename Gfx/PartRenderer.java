/*----------------------------------------------------------------------*/
/*

        Module          : PartRenderer

        Package         : Gfx

        Classes Included: PartRenderer

        Purpose         : Render unscored parts in original notation

        Programmer      : Ted Dumitrescu

        Date Started    : 7/05 (moved to separate file 3/27/06)

Updates:
3/27/06: moved StaffEventData to separate class
9/3/07:  added support for multi-section scores

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   PartRenderer
Extends: -
Purpose: Renders one voice part in unscored original notation
------------------------------------------------------------------------*/

public class PartRenderer
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static double MAX_PADDING=13,
                       MIN_PADDING=7;

/*----------------------------------------------------------------------*/
/* Instance variables */

  /* graphics data */
  OptionSet options;
  boolean   printPreview;
  int       STAFFXSIZE;

  /* music data */
  ArrayList<RenderList> renderedStaves;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  void incipitJustify(ArrayList<ArrayList<RenderList>> voices)
Purpose: For incipit view: adjust positions of events in voices so that
         all incipits take up the same horizontal space
Parameters:
  Input:  ArrayList<ArrayList<RenderList>> voices - rendered event information
  Output: voices
  Return: -
------------------------------------------------------------------------*/

  static public void incipitJustify(ArrayList<ArrayList<RenderList>> voices)
  {
    int            numVoices=voices.size(),
                   maxX=0,longestVoice=0;
    StaffEventData curStaff;

    /* find longest incipit */
    for (int i=0; i<numVoices; i++)
      {
        curStaff=(StaffEventData)voices.get(i).get(0);
        int incipitXspace=curStaff.calcEventSpace(0,curStaff.getIncipitEndGroupIndex()-1);
        if (incipitXspace>maxX)
          {
            maxX=incipitXspace;
            longestVoice=i;
          }
      }

    /* voice with longest incipit: set x coordinates */
    curStaff=(StaffEventData)voices.get(longestVoice).get(0);
    double totalIncipitSize=curStaff.padEvents(MAX_PADDING,0,curStaff.getIncipitEndGroupIndex()-1,0);
    curStaff.totalxsize=(int)curStaff.padEvents(MAX_PADDING,
      curStaff.getIncipitEndGroupIndex(),curStaff.eventgroups.size()-1,totalIncipitSize);

    /* space other voices to match longest length */
    double         paddingspace;
    for (int vi=0; vi<numVoices; vi++)
      if (vi!=longestVoice)
        {
          curStaff=(StaffEventData)voices.get(vi).get(0);
          int curspace=curStaff.calcEventSpace(0,curStaff.getIncipitEndGroupIndex()-1);
          paddingspace=(totalIncipitSize-curspace)/(double)(curStaff.getIncipitEndGroupIndex());
          curStaff.padEvents(paddingspace,0,curStaff.getIncipitEndGroupIndex()-1,0);
          curStaff.totalxsize=(int)curStaff.padEvents(MAX_PADDING,
            curStaff.getIncipitEndGroupIndex(),curStaff.eventgroups.size()-1,totalIncipitSize);
        }
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: PartRenderer(Voice voiceinfo,int sxs,boolean pp)
Purpose:     Render music of one voice part
Parameters:
  Input:  Voice voiceinfo - voice/event data
          int sxs         - total available x-size for each staff
          boolean pp      - whether to use 'print preview' mode
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public PartRenderer(Voice voiceinfo,int sxs,boolean pp)
  {
    STAFFXSIZE=sxs;
    printPreview=pp;
    options=OptionSet.makeDEFAULT_ORIGINAL(null);
    options.set_usemodernclefs(false);
    options.set_displayorigligatures(true);
    options.setViewEdCommentary(false);
    options.set_modacc_type(OptionSet.OPT_MODACC_NONE);
    options.set_unscoredDisplay(true);

    renderedStaves=renderMusicIntoStaves(voiceinfo);
  }

  boolean atLineend(VoiceEventListData v,int evnum)
  {
    while (evnum<v.getNumEvents())
      {
        Event e=v.getEvent(evnum);
        int   etype=e.geteventtype();
        if (etype==Event.EVENT_LINEEND)
          return true;
        else if (etype!=Event.EVENT_CUSTOS &&
                 etype!=Event.EVENT_COLORCHANGE)
          return false;
        evnum++;
      }
    return false;
  }

  int skipLineendEvents(VoiceEventListData v,int evnum,Event curclef)
  {
    int     i=evnum,lineendevnum;
    boolean done=false;

    /* skip to line end */
    while (!done)
      if (v.getEvent(i++).geteventtype()==Event.EVENT_LINEEND)
        done=true;
    lineendevnum=i-1;

    /* skip clef information */
    done=i>=v.getNumEvents();
    while (!done)
      {
        Event e=v.getEvent(i);
        if (!e.hasSignatureClef())
          done=true;
        else
          if (e.getClefSet(false).contradicts(curclef.getClefSet(false),false,null))
            done=true;
        if (!done)
          i++;
      }

    return i-evnum;
  }

/*------------------------------------------------------------------------
Method:  ArrayList<RenderList> renderMusicIntoStaves(Voice voiceInfo)
Purpose: Divide event list into staves and calculate x-locations for events
Parameters:
  Input:  Voice voiceInfo - original event information for all sections
  Output: -
  Return: list of structures, each containing event placement information
          for one staff
------------------------------------------------------------------------*/

  static final int DEFAULT_GROUPS_PER_STAFF=35,
                   MIN_GROUPS_FROM_SECTION=8;

  protected ArrayList<RenderList> renderMusicIntoStaves(Voice voiceInfo)
  {
    boolean addstaff=true,
            bracketopen=false,
            endSection=false;
    int     cureventnum,
            renderedeventnum=0,
            clefeventnum=-1,vcen=-1,
            eventnumadd,
            colorbracketleft=-1;

    Event                 curevent;
    ArrayList<RenderList> staves=new ArrayList<RenderList>();
    StaffEventData        curstaff=null;
    StaffEventData        clefStaffData=null;
    VoiceEventListData    clefVoice=null;

    int numSections=voiceInfo.getGeneralData().getNumSections();
    for (int si=0; si<numSections; si++)
      {
        MusicSection       curSection=voiceInfo.getGeneralData().getSection(si);
        VoiceEventListData v=curSection.getVoice(voiceInfo.getNum()-1);

        boolean doneSection=v==null || (v.getNumEvents()<2 && !addstaff) ||
                            curSection.isEditorial();

        if (!doneSection && voiceInfo.getGeneralData().isIncipitScore() && numSections>1 && si==numSections-1)
          {
            curstaff.addevent(true,new Event(Event.EVENT_ELLIPSIS),
                              new RenderParams(getClefEvents(clefStaffData,clefeventnum)));
          }

        cureventnum=0;
        while (!doneSection)
          {
            eventnumadd=1;

            if (addstaff)
              {
                if (curstaff!=null)
                  curstaff.totalxsize=calcstaffxlocs(curstaff,STAFFXSIZE,endSection); /* do finishing calculations */

                /* initialize new blank staff */
                curstaff=new StaffEventData(options,v.getMetaData(),curSection);
                staves.add(curstaff);
                renderedeventnum=0;
                clefeventnum=-1;
                addstaff=false;
                endSection=false;

                /* create new clef set for non-original line breaks */
                if (printPreview && (cureventnum>0 || si>0))
                  {
                    clefeventnum=0;
                    renderedeventnum+=curstaff.addclefgroup(clefVoice,vcen,
                                                            new RenderParams(getClefEvents(clefStaffData,clefeventnum)));
                  }
              }

            curevent=v.getEvent(cureventnum);
            int etype=curevent.geteventtype();
            if (etype==Event.EVENT_LINEEND && !printPreview)
              addstaff=true;
            else if (printPreview && atLineend(v,cureventnum))
              {
                eventnumadd=skipLineendEvents(v,cureventnum,clefVoice.getEvent(vcen));
                if (v.getEvent(cureventnum+eventnumadd-1).geteventtype()==Event.EVENT_LINEEND)
                  {
                    curstaff.addevent(true,new LineEndEvent(),
                                      new RenderParams(getClefEvents(clefStaffData,clefeventnum)));
                    renderedeventnum++;
                  }
              }
            else if (etype==Event.EVENT_COLORCHANGE && printPreview)
              {
                if ((!bracketopen) && ((ColorChangeEvent)curevent).getcolorscheme().primaryColor!=Coloration.BLACK)
                  colorbracketleft=renderedeventnum;
                else if (bracketopen && ((ColorChangeEvent)curevent).getcolorscheme().primaryColor==Coloration.BLACK)
                  {
                    curstaff.getEvent(renderedeventnum-1).addcolorbracket(1);
                    bracketopen=false;
                  }
              }
            else if (etype!=Event.EVENT_PROPORTION)
              {
                if (curevent.hasPrincipalClef())
                  {
                    clefStaffData=curstaff;
                    clefeventnum=renderedeventnum;
                    clefVoice=v;
                    vcen=cureventnum;
                  }

                if (etype==Event.EVENT_NOTE &&
                    ((NoteEvent)curevent).isligated())
                  eventnumadd=curstaff.addlig(v,cureventnum,
                                              new RenderParams(getClefEvents(clefStaffData,clefeventnum)));
                else
                  curstaff.addevent(true,curevent,
                                    new RenderParams(getClefEvents(clefStaffData,clefeventnum)));
                renderedeventnum+=eventnumadd;
              }

/*            if (etype==Event.EVENT_BARLINE)
              endSection=true;
            else*/
              if (etype!=Event.EVENT_LINEEND)
                endSection=false;
              else
                if (((LineEndEvent)curevent).isPageEnd())
                  endSection=true;

            if (colorbracketleft!=-1 && renderedeventnum>colorbracketleft)
              {
                curstaff.getEvent(colorbracketleft).addcolorbracket(0);
                colorbracketleft=-1;
                bracketopen=true;
              }

            cureventnum+=eventnumadd;
            doneSection=cureventnum>=v.getNumEvents();

            if ((printPreview && !doneSection &&
                curstaff.eventgroups.size()>=DEFAULT_GROUPS_PER_STAFF &&
                !curstaff.groupwithprevious(v.getEvent(cureventnum)) &&
                v.getNumEvents()-cureventnum>=MIN_GROUPS_FROM_SECTION &&
                !(bracketopen && v.getEvent(cureventnum).geteventtype()==Event.EVENT_COLORCHANGE) && /* don't start a new line with a close bracket */
                etype!=Event.EVENT_CLEF)
             || (printPreview && doneSection &&
                 curstaff.eventgroups.size()>DEFAULT_GROUPS_PER_STAFF-MIN_GROUPS_FROM_SECTION))
              addstaff=true;
            if (addstaff)
              curstaff.addevent(false,new Event(),
                                new RenderParams(getClefEvents(clefStaffData,clefeventnum)));
          }
      }

    if (curstaff!=null)
      if (staves.size()>1)
        curstaff.totalxsize=calcstaffxlocs(curstaff,STAFFXSIZE,true);
      else
        if (curstaff.eventgroups.size()>DEFAULT_GROUPS_PER_STAFF-MIN_GROUPS_FROM_SECTION)
          curstaff.totalxsize=calcstaffxlocs(curstaff,STAFFXSIZE,true);
        else
          curstaff.totalxsize=(int)(curstaff.padEvents(MIN_PADDING));

    return staves;
  }

  RenderedClefSet getClefEvents(StaffEventData clefStaffData,int clefEventNum)
  {
    return clefStaffData==null ? null : clefStaffData.getClefEvents(clefEventNum);
  }

/*------------------------------------------------------------------------
Method:  int calcstaffxlocs(StaffEventData events,int xsize,boolean laststaff)
Purpose: Calculate x-locations for events on one staff (justified spacing)
Parameters:
  Input:  StaffEventData events - events on staff
          int xsize             - total x space to fill
          boolean laststaff     - whether this is the last staff of the section
  Output: -
  Return: total space used
------------------------------------------------------------------------*/

  int calcstaffxlocs(StaffEventData events,int xsize,boolean laststaff)
  {
    /* calculate justification size */
    int totaleventspace=events.calcEventSpace();
    double paddingspace=(xsize-totaleventspace)/(double)(events.eventgroups.size());
    if (laststaff && paddingspace>MAX_PADDING)
      paddingspace=MAX_PADDING;

    /* set x coordinates */
    return (int)(events.padEvents(paddingspace));
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public ArrayList<RenderList> getRenderedData()
  {
    return renderedStaves;
  }
}

