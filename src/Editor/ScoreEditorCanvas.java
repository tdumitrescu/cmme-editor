/*----------------------------------------------------------------------*/
/*

        Module          : ScoreEditorCanvas.java

        Package         : Editor

        Classes Included: ScoreEditorCanvas,EditorCursor

        Purpose         : Handles music-editing area (in score view)

        Programmer      : Ted Dumitrescu

        Date Started    : 5/5/05

Updates:
6/05:     added functionality for adding and deleting events
6/15/05:  moved keyboard listener here from EditorWin; added focus listener (to
          show and hide cursor)
7/05:     expanded add event functionality to include all current types
8/05:     implemented mouse-controlled highlighting
9/25/05:  corrected bug where cursor disappears past right side of window in
          borderline cases (where the rightmost measure just fits)
3/14/06:  fixed focus-gain bug where clicking on canvas to regain focus
          doesn't de-highlight events, but does deactivate event editor
7/21/06:  added support for EditorialData sections
          fixed boundary condition when adding events before parameter
          changes (coloration, etc)
7/25/06:  changing clef position now moves dots of addition
7/31/06:  switched to new mensuration sign structure
8/23/06:  new proportion events now default to cancellation of current proportion
8/28/06:  added routine to allow user to turn highlighted events into Lacuna
11/9/06:  added support for vertical cursor movement controlling y-location
          of score viewscreen
2/28/07:  began adding music-sectional division code
10/16/07: modified updateNoteAccidentals so that signature changes carry
          across section divisions
10/22/07: began adding variant-editing code
1/4/08:   began adding code for producing variants caused by event modification
6/16/09:  removed deprecated "section tag" code
          implemented basic copy/cut/paste functionality
7/2/09:   made clipboard static (same for all windows)
7/19/09:  fixed paste bug (multiple pastes of the same material did not create
          new copies of events)

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.*;

import DataStruct.*;
import Gfx.*;

/*------------------------------------------------------------------------
Class:   ScoreEditorCanvas
Extends: Gfx.ViewCanvas
Purpose: Handles music-editing area (in score view)
------------------------------------------------------------------------*/

public class ScoreEditorCanvas
             extends Gfx.ViewCanvas
             implements KeyListener,MouseListener,MouseMotionListener,FocusListener
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static ClipboardData clipboard=null;

/*----------------------------------------------------------------------*/
/* Instance variables */

  EditorWin         parentEditorWin;
  EditorCursor      Cursor;
  javax.swing.Timer Cursor_Timer;

  int     hl_anchor=-1;  /* event index where highlight began */
  boolean focused=false; /* whether the canvas has the input focus */

  /* input-control parameters */
  int     editorColorationType=Coloration.MINOR_COLOR,
          editorStemDir=NoteEvent.STEM_UP;
  boolean colorationOn=false;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ScoreEditorCanvas(PieceData p,MusicFont mf,MusicWin mw,OptionSet os)
Purpose:     Initialize canvas
Parameters:
  Input:  PieceData p,MusicFont mf,MusicWin mw,OptionSet os - ViewCanvas params
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ScoreEditorCanvas(PieceData p,MusicFont mf,MusicWin mw,OptionSet os)
  {
    super(p,mf,mw,os);
    parentEditorWin=(EditorWin)parentwin;

    /* initialize cursor */
    Cursor=new EditorCursor(this);

    /* cursor blinking */
    Action updateCursorAction=new AbstractAction()
      {
        public void actionPerformed(ActionEvent e)
        {
          Cursor.toggleCursor();
        }
      };
    Cursor_Timer=new javax.swing.Timer(500,updateCursorAction);
    Cursor_Timer.start();

    /* input handlers */
    addKeyListener(this);
//    addMouseListener(this);
    addMouseMotionListener(this);
    setFocusable(true);
    addFocusListener(this);
  }

/*------------------------------------------------------------------------
Method:    void realpaintbuffer(Graphics2D g)
Overrides: Gfx.ViewCanvas.realpaintbuffer
Purpose:   Repaint area into offscreen buffer
Parameters:
  Input:  Graphics2D g - offscreen graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void realpaintbuffer(Graphics2D g)
  {
    super.realpaintbuffer(g);
    Cursor.repaintHighlight(g);
  }

/*------------------------------------------------------------------------
Method:  void showCursor()
Purpose: Enable cursor display
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void showCursor()
  {
    Cursor.showCursor();
  }

/*------------------------------------------------------------------------
Method:  void shiftCursorLoc(int xs,int vs)
Purpose: Attempt to shift cursor position
Parameters:
  Input:  int xs - number of events to go left/right
          int vs - number of voices to go up/down
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void shiftCursorLoc(int xs,int vs)
  {
    Cursor.shiftCursorLoc(xs,vs);
    parentEditorWin.updateEventEditor();
  }

/*------------------------------------------------------------------------
Method:  void modifyHighlight(int xs)
Purpose: Attempt to expand or contract highlight
Parameters:
  Input:  int xs - number of events to expand/contract left/right
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void modifyHighlight(int xs)
  {
    Cursor.modifyHighlight(xs);
    if (Cursor.oneItemHighlighted())
      parentEditorWin.updateEventEditor(getCurEvent());
    else if (Cursor.getHighlightBegin()==-1)
      parentEditorWin.updateEventEditor();
    else
      parentEditorWin.updateEventEditor(Cursor.getHighlightEnd()-Cursor.getHighlightBegin()+1);
  }

/*------------------------------------------------------------------------
Method:  void shiftHighlightWithinMultiEvent(int offset)
Purpose: Attempt to shift cursor position within multi-event
Parameters:
  Input:  int offset - number of events to shift
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void shiftHighlightWithinMultiEvent(int offset)
  {
    Cursor.shiftHighlightWithinMultiEvent(offset);
  }

/*------------------------------------------------------------------------
Method:  void moveCursor(int newsnum,int newvnum,int neweventnum)
Purpose: Move cursor to a new position and display
Parameters:
  Input:  int newsnum,newvnum,neweventnum - new cursor parameters
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void moveCursor(int newsnum,int newvnum,int neweventnum)
  {
    Cursor.moveCursor(newsnum,newvnum,neweventnum);
    parentEditorWin.updateEventEditor();
  }

/*------------------------------------------------------------------------
Method:  void newCursorLoc(int x,int y)
Purpose: Attempt to move cursor to a given XY position in canvas
Parameters:
  Input:  int x - desired x position
          int y - desired y position
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void newCursorLoc(int x,int y)
  {
    Cursor.newCursorLoc(x,y);
    parentEditorWin.updateEventEditor();
  }

/*------------------------------------------------------------------------
Method:  void moveCursorToHome()
Purpose: Move cursor to start of current voice
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void moveCursorToHome()
  {
    int vnum=Cursor.getVoiceNum(),
        snum=0;

    /* choose valid location for this voice */
    while (snum<numSections && renderedSections[snum].eventinfo[vnum]==null)
      snum++;
    if (snum==numSections)
      snum=0;

    moveCursor(snum,vnum,0);
  }

/*------------------------------------------------------------------------
Method:  void moveCursorToEnd()
Purpose: Move cursor to end of current voice
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void moveCursorToEnd()
  {
    int vnum=Cursor.getVoiceNum(),
        snum=numSections-1,
        evnum=0;

    /* choose valid location for this voice */
    while (snum>=0 && renderedSections[snum].eventinfo[vnum]==null)
      snum--;
    if (snum<0)
      snum=0;
    else
      evnum=renderedSections[snum].eventinfo[vnum].size()-1;

    moveCursor(snum,vnum,evnum);
  }

/*------------------------------------------------------------------------
Method:  void highlightItems(int snum,int vnum,int firstenum,int lastenum)
Purpose: Highlight one or more events and update GUI if necessary
Parameters:
  Input:  int snum,vnum          - section/voice number
          int firstenum,lastenum - indices of first and last events to be highlighted
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void highlightItems(int snum,int vnum,int firstenum,int lastenum)
  {
    Cursor.highlightEvents(snum,vnum,firstenum,lastenum);
    if (Cursor.oneItemHighlighted())
      parentEditorWin.updateEventEditor(getCurEvent());
    else
      parentEditorWin.updateEventEditor(lastenum-firstenum+1);
  }

  public void highlightOneItem(int snum,int vnum,int eventnum)
  {
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  public void highlightAll()
  {
    hl_anchor=0;
    highlightItems(Cursor.getSectionNum(),Cursor.getVoiceNum(),0,renderedSections[Cursor.getSectionNum()].eventinfo[Cursor.getVoiceNum()].size()-2);
  }

/*------------------------------------------------------------------------
Method:  void clipboard[Copy|Cut|Paste]()
Purpose: Clipboard functions based on current cursor position and
         highlighted items
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void clipboardCopy()
  {
    if (Cursor.getHighlightBegin()<0)
      ScoreEditorCanvas.clipboard=null;
    else
      ScoreEditorCanvas.clipboard=new ClipboardData(renderedSections,Cursor.getSectionNum(),Cursor.getVoiceNum(),
                                                    Cursor.getHighlightBegin(),Cursor.getHighlightEnd());
  }

  public void clipboardCut()
  {
    clipboardCopy();
    deleteHighlightedItems();
  }

  public void clipboardPaste()
  {
    if (Cursor.getHighlightBegin()>=0)
      deleteHighlightedItems();
    insertEventList(Cursor.getSectionNum(),Cursor.getVoiceNum(),Cursor.getEventNum(),
                    ScoreEditorCanvas.clipboard.getEventList().createCopy());
  }

/*------------------------------------------------------------------------
Method:  int deleteItem(int snum,int vnum,int eventnum)
Purpose: Delete one event and adjust context if necessary
Parameters:
  Input:  int snum,vnum,eventnum - section, voice and event number for deletion
  Output: -
  Return: number of items actually deleted
------------------------------------------------------------------------*/

  int secondDeletedIndex; /* if we delete more than one item, index of the second */

  int deleteItem(int snum,int vnum,int eventnum)
  {
    int itemsdeleted=1;

    secondDeletedIndex=-1;

    /* deal with other events affected by the event to be deleted */
    Event ev_to_delete=renderedSections[snum].eventinfo[vnum].getEvent(eventnum).getEvent();

    if (ev_to_delete.geteventtype()==Event.EVENT_NOTE &&
        curVariantVersion==musicData.getDefaultVariantVersion())
      {
        /* delete dot attached to note */
        RenderedEvent nexte=renderedSections[snum].eventinfo[vnum].getEvent(eventnum+1);
        if (nexte!=null &&
            nexte.getEvent().geteventtype()==Event.EVENT_DOT &&
            (((DotEvent)nexte.getEvent()).getdottype()&DotEvent.DT_Addition)!=0)
          {
            musicData.deleteEvent(snum,vnum,renderedSections[snum].getVoicedataPlace(vnum,eventnum+1));
            itemsdeleted++;
          }
      }
    else if (ev_to_delete.geteventtype()==Event.EVENT_DOT &&
             (((DotEvent)ev_to_delete).getdottype()&DotEvent.DT_Addition)!=0 &&
             curVariantVersion==musicData.getDefaultVariantVersion())
      {
        /* change length of notes affected by dots of addition */
        NoteEvent lastne=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
        if (lastne!=null)
          {
            Proportion notelength=lastne.getLength();
            notelength.multiply(2,3);
          }
      }
    else if (ev_to_delete.geteventtype()==Event.EVENT_VARIANTDATA_START)
      {
        int ei=eventnum+1;
        Event cure=renderedSections[snum].eventinfo[vnum].getEvent(ei).getEvent();
        while (cure.geteventtype()!=Event.EVENT_VARIANTDATA_END)
          {
//            cure.setEditorial(false);
            cure=renderedSections[snum].eventinfo[vnum].getEvent(++ei).getEvent();
          }

        /* now delete VARIANTDATA_END event */
        secondDeletedIndex=ei;
        musicData.deleteEvent(snum,vnum,renderedSections[snum].getVoicedataPlace(vnum,ei));
      }
    else if (ev_to_delete.geteventtype()==Event.EVENT_VARIANTDATA_END)
      {
        int ei=eventnum-1;
        Event cure=renderedSections[snum].eventinfo[vnum].getEvent(ei).getEvent();
        while (cure.geteventtype()!=Event.EVENT_VARIANTDATA_START)
          {
//            cure.setEditorial(false);
            cure=renderedSections[snum].eventinfo[vnum].getEvent(--ei).getEvent();
          }

        /* now delete VARIANTDATA_END event, set START as main item to be deleted */
        secondDeletedIndex=ei;
        musicData.deleteEvent(snum,vnum,renderedSections[snum].getVoicedataPlace(vnum,eventnum));
        eventnum=ei;
      }
    else if (ev_to_delete.geteventtype()==Event.EVENT_LACUNA)
      {
// insert code to deal with deleting LACUNA and LACUNA_END simultaneously
      }

    /* delete main event */
    int listPos=renderedSections[snum].getVoicedataPlace(vnum,eventnum);
    if (curVariantVersion!=musicData.getDefaultVariantVersion())
      {
        /* create/modify variant data */
        int eventPos=musicData.deleteVariantEvent(
              curVariantVersion,curVersionMusicData,snum,vnum,listPos);

        switch (eventPos)
          {
            case VariantReading.NEWVARIANT:
              listPos++;
              eventnum++;
              break;
            case VariantReading.BEGINNING:
              break;
            case VariantReading.MIDDLE:
              break;
            case VariantReading.END:
              break;
            case VariantReading.COMBINED:
            case VariantReading.DELETED:
              listPos--;
              eventnum--;
              itemsdeleted+=2;
              secondDeletedIndex=listPos;
              break;
            case VariantReading.NOACTION:
              return itemsdeleted;
            case VariantReading.NEWREADING:
              /* reading has been added; update render list to contain replaced events */
              int vmi1=getNeighboringEventNumOfType(Event.EVENT_VARIANTDATA_START,snum,vnum,eventnum,-1),
                  vmi2=getNeighboringEventNumOfType(Event.EVENT_VARIANTDATA_END,snum,vnum,eventnum,1),
                  varListPlace=renderedSections[snum].getVoicedataPlace(vnum,vmi1)+1;
              for (int vi=vmi1+1; vi<vmi2; vi++)
                {
                  RenderedEvent re=renderedSections[snum].eventinfo[vnum].getEvent(vi);
                  re.setEvent(curVersionMusicData.getEvent(snum,vnum,varListPlace++));
                }
              
              break;
          }
      }

    curVersionMusicData.deleteEvent(snum,vnum,listPos);

    return itemsdeleted;
  }

/*------------------------------------------------------------------------
Method:  void deleteHighlightedItems()
Purpose: Attempt to delete highlighted events
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  int deleteWithoutRender(int snum,int vnum,int firstEventNum,int lastEventNum)
  {
    int newevnum=firstEventNum,
        endEvent=lastEventNum;

    for (int i=firstEventNum; i<=endEvent;)
      {
        i+=deleteItem(snum,vnum,i);
        if (secondDeletedIndex>-1 && secondDeletedIndex<=lastEventNum)
          if (secondDeletedIndex>=firstEventNum)
            endEvent--;
          else
            newevnum--;
      }

    return newevnum;
  }

  int deleteHighlightedWithoutRender()
  {
    return deleteWithoutRender(Cursor.getSectionNum(),Cursor.getVoiceNum(),
                               Cursor.getHighlightBegin(),Cursor.getHighlightEnd());
  }

  public void deleteHighlightedItems()
  {
    if (Cursor.getHighlightBegin()<0)
      return; /* nothing highlighted */

    int mei=Cursor.getMultiEventHLindex();
    if (mei!=-1)
      {
        deleteHighlightedItemWithinMultiEvent(mei);
        return;
      }

    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum();

    /* check for variant markers */
    for (int i=Cursor.getHighlightBegin(); i<=Cursor.getHighlightEnd(); i++)
      if (getEvent(snum,vnum,i).getEvent() instanceof VariantMarkerEvent)
        return;

    int newevnum=deleteHighlightedWithoutRender();

    Cursor.setNoHighlight();
    rerender();
    checkVariant(snum,vnum,newevnum);
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,vnum,newevnum);
  }

  void deleteHighlightedItemWithinMultiEvent(int mei)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();
    Event e=getCurEvent().getEvent(),
          me=getCurMainEvent().getEvent(),
          newme=((MultiEvent)me).deleteEvent(e);

    if (newme==me)
      {
        rerender();
        shiftHighlightWithinMultiEvent(0);
      }
    else
      {
        Cursor.resetMultiEventHLindex();
        deleteItem(snum,vnum,eventnum);
        eventnum=insertEvent(snum,vnum,eventnum,newme);
        rerender();
      }

    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void deleteCurItem(int event_offset)
Purpose: Attempt to delete event at location relative to cursor position
Parameters:
  Input:  int event_offset - offset from current cursor location for event
                             to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteCurItem(int event_offset)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum()+event_offset;
    if (eventnum<0 || eventnum>=renderedSections[snum].eventinfo[vnum].size()-1)
      return;

    RenderedEvent re=renderedSections[snum].eventinfo[vnum].getEvent(eventnum);
    int           offsetAdd=event_offset<0 ? -1 : 1;
    while (re!=null &&
           (!re.isdisplayed() || /* don't delete invisible items */
            re.getEvent().geteventtype()==Event.EVENT_VARIANTDATA_START ||
            re.getEvent().geteventtype()==Event.EVENT_VARIANTDATA_END))
      {
        eventnum+=offsetAdd;
        if (eventnum<0 || eventnum>=renderedSections[snum].eventinfo[vnum].size()-1)
          re=null;
        else
          re=renderedSections[snum].eventinfo[vnum].getEvent(eventnum);
      }
    if (eventnum<0 || eventnum>=renderedSections[snum].eventinfo[vnum].size()-1)
      return;

    Cursor.hideCursor();

    deleteItem(snum,vnum,eventnum);
    if (secondDeletedIndex>-1 && secondDeletedIndex<eventnum)
      eventnum--;

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,vnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  int insertEvent(int snum,int vnum,int eventnum,Event e)
Purpose: Insert event at given location and display
Parameters:
  Input:  int snum,vnum,eventnum - section, voice, and event number for insertion
          Event e                - event to insert
  Output: -
  Return: list index where event was inserted
------------------------------------------------------------------------*/

  int insertEventWithoutRerender(int snum,int vnum,int eventnum,Event e)
  {
    e.setcolorparams(getCurColoration(snum,vnum,eventnum-1));
    e.setModernKeySigParams(getCurModernKeySig(snum,vnum,eventnum-1));
//    e.setEditorial(inEditorialSection(snum,vnum,eventnum-1));

    int listPos=renderedSections[snum].getVoicedataPlace(vnum,eventnum);
    if (curVariantVersion!=musicData.getDefaultVariantVersion())
      {
        /* create variant data */
        int eventPos=musicData.addVariantEvent(curVariantVersion,curVersionMusicData,
                                               snum,vnum,listPos,renderedSections[snum].getDefaultVoicedataPlace(vnum,eventnum),e);

        switch (eventPos)
          {
            case VariantReading.NEWVARIANT:
              listPos++;
              eventnum++;
              break;
            case VariantReading.BEGINNING:
              listPos++;
              eventnum++;
              break;
            case VariantReading.MIDDLE:
              break;
            case VariantReading.END:
              listPos--;
              eventnum--;
              break;
          }
      }

    curVersionMusicData.addEvent(snum,vnum,listPos,e);

    return listPos;
  }

  int insertEvent(int snum,int vnum,int eventnum,Event e)
  {
    eventnum=insertEventWithoutRerender(snum,vnum,eventnum,e);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum, vnum, eventnum + 1);

    return eventnum;
  }

  int insertEventList(int snum,int vnum,int eventnum,EventListData el)
  {
    int origEventnum=eventnum;
    for (int ei=0; ei<el.getNumEvents(); ei++)
      {
        eventnum=insertEventWithoutRerender(snum,vnum,eventnum,el.getEvent(ei))+1;
        rerender(snum);
      }

    curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,vnum,eventnum);

    return eventnum;
  }

/*------------------------------------------------------------------------
Method:  long checkVariant(int snum,int vnum,int eventNum)
Purpose: (Re-)calculate variant type flags at a given location
Parameters:
  Input:  int snum,vnum,eventNum - section, voice, and event number
  Output: -
  Return: new flags representing variation types present
------------------------------------------------------------------------*/

  long checkVariant(int snum,int vnum,int eventNum)
  {
    RenderedEvent re=getEvent(snum,vnum,eventNum);
    RenderedEventGroup renderedVar=null;
    if (re!=null)
      renderedVar=renderedSections[snum].eventinfo[vnum].getEvent(eventNum).getVarReadingInfo();
    if (renderedVar==null)
      return VariantReading.VAR_NONE; /* no variants here */

    VariantMarkerEvent
     vme1=(VariantMarkerEvent)getEvent(snum,vnum,renderedVar.firstEventNum).getEvent(),
     vme2=(VariantMarkerEvent)getEvent(snum,vnum,renderedVar.lastEventNum).getEvent();
    long origvt=vme1.getVarTypeFlags(),
         vt=vme1.calcVariantTypes(musicData.getSection(snum).getVoice(vnum));
    vme2.setVarTypeFlags(vt);

    if (vt!=origvt &&
        (vt==VariantReading.VAR_ORIGTEXT || origvt==VariantReading.VAR_ORIGTEXT))
      rerender();

    return vt;
  }

/*------------------------------------------------------------------------
Method:  Event getEventForModification(int snum,int vnum,int eventnum)
Purpose: Locate event for modification, or create new variant reading if
         necessary with copy of default event
Parameters:
  Input:  int snum,vnum,eventnum - section, voice, and event number
  Output: -
  Return: event to be modified
------------------------------------------------------------------------*/

  Event getEventForModification(int snum,int vnum,int eventnum)
  {
    Event e=getEvent(snum,vnum,eventnum).getEvent();
    if (curVariantVersion!=musicData.getDefaultVariantVersion())
      return musicData.duplicateEventInVariant(
        curVariantVersion,curVersionMusicData,
        snum,vnum,e.getListPlace(false));
    else
      return e; /* in default reading */
  }

/*------------------------------------------------------------------------
Method:  Event getEventsForModification(int snum,int vnum,int e1i,int e2i)
Purpose: Locate events for modification, creating new variant reading if
         necessary
Parameters:
  Input:  int snum,vnum - section and voice number
          int e1i,e2i   - (rendered) event numbers of first and second events
  Output: -
  Return: first event to be modified
------------------------------------------------------------------------*/

  Event getEventsForModification(int snum,int vnum,int e1i,int e2i)
  {
    Event e1=renderedSections[snum].eventinfo[vnum].getEvent(e1i).getEvent(),
          e2=renderedSections[snum].eventinfo[vnum].getEvent(e2i).getEvent();
    if (curVariantVersion!=musicData.getDefaultVariantVersion())
      return musicData.duplicateEventsInVariant(
        curVariantVersion,curVersionMusicData,
        snum,vnum,e1.getListPlace(false),e2.getListPlace(false));
    else
      return e1; /* in default reading */
  }

/*------------------------------------------------------------------------
Method:  NoteEvent getNoteEventForLigation(int snum,int vnum,int note1i,int note2i)
Purpose: Locate note event for modification of ligation, creating new
         variant reading if necessary
Parameters:
  Input:  int snum,vnum     - section and voice, and event number
          int note1i,note2i - (rendered) event numbers of first and second
                              notes for ligature modification
  Output: -
  Return: event to be modified
------------------------------------------------------------------------*/

  NoteEvent getNoteEventForLigation(int snum,int vnum,int note1i,int note2i)
  {
    NoteEvent ne1=(NoteEvent)renderedSections[snum].eventinfo[vnum].getEvent(note1i).getEvent(),
              ne2=(NoteEvent)renderedSections[snum].eventinfo[vnum].getEvent(note2i).getEvent();
    if (curVariantVersion!=musicData.getDefaultVariantVersion())
      return (NoteEvent)musicData.duplicateEventsInVariant(
        curVariantVersion,curVersionMusicData,
        snum,vnum,ne1.getListPlace(false),ne2.getListPlace(false));
    else
      return ne1; /* in default reading */
  }

/*------------------------------------------------------------------------
Method:  void setEventProportion(Proportion p)
Purpose: Modify one number in an event's proportion (Note, Rest, Proportion, etc)
Parameters:
  Input:  Proportion p - new proportion for event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setEventProportion(Proportion p)
  {
    setEventProportion(0, p);
  }

  public void setEventProportion(int eventOffset, Proportion p)
  {
    if (eventOffset == 0 && !Cursor.oneItemHighlighted())
      System.err.println("Error: more than one item highlighted");

    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum() + eventOffset;
    RenderedEvent re = getCurEvent(eventOffset);
    if (re == null) {
      return;
    }

    /* save old proportion to check for file modification */
    Event      origEvent=re.getEvent();
    Proportion oldp;
    if (origEvent.geteventtype()==Event.EVENT_PROPORTION)
      oldp=((ProportionEvent)origEvent).getproportion();
    else if (origEvent.geteventtype()==Event.EVENT_MENS)
      oldp=((MensEvent)origEvent).getTempoChange();
    else
      oldp=new Proportion(origEvent.getLength());
 
   if (p.i2<=0 || p.i1<=0 || p.equals(oldp))
      return;

    Cursor.hideCursor();
    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-origEvent.getListPlace(!inVariantVersion());

    if (e.geteventtype()==Event.EVENT_LACUNA)
      ((LacunaEvent)e).setLength(p);
    else if (e.geteventtype()==Event.EVENT_MENS)
      ((MensEvent)e).setTempoChange(p);
    else if (e.geteventtype()==Event.EVENT_NOTE)
      ((NoteEvent)e).setLength(p);
    else if (e.geteventtype()==Event.EVENT_PROPORTION)
      ((ProportionEvent)e).setproportion(p);
    else if (e.geteventtype()==Event.EVENT_REST)
      ((RestEvent)e).setLength(p);
    else
      System.err.println("Error: no proportion for this type of event");

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();

    parentEditorWin.fileModified();

    if (eventOffset == 0) {
      hl_anchor=eventnum;
      highlightItems(snum,vnum,eventnum,eventnum);
    } else {
      moveCursor(snum, vnum, eventnum - eventOffset);
    }
  }

/*------------------------------------------------------------------------
Method:  void setEventCommentary(String c)
Purpose: Set editorial commentary text for any event
Parameters:
  Input:  String c - new commentary text
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setEventCommentary(String c)
  {
    if (!Cursor.oneItemHighlighted())
      System.err.println("Error: more than one item highlighted");

    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent();

    boolean oldCommNull=orige.getEdCommentary()==null,
            newCommNull=c.equals("");
    if (oldCommNull && newCommNull)
      return;

    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    if (c.equals(""))
      c=null;
    e.setEdCommentary(c);

    Cursor.hideCursor();
    parentEditorWin.fileModified();
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void makeMultiEvent()
Purpose: Combine this event and the previous one to make a multi-event
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void makeMultiEvent()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent();

    /* check if event is valid for combination with previous */
    if (eventnum==0 || !validMultiEventType(orige) ||
        (orige.geteventtype()==Event.EVENT_DOT && (((DotEvent)orige).getdottype()&DotEvent.DT_Addition)!=0))
      return;
    Event origprev=getCurEvent(-1).getEvent();
    if (!validMultiEventType(origprev) && origprev.geteventtype()!=Event.EVENT_MULTIEVENT)
      return;

    Event prev=getEventsForModification(snum,vnum,eventnum-1,eventnum);
    if (prev==null)
      return; /* attempt to create invalid variant arrangement */

    eventnum--; /* eventnum now needs to match first event */
    if (prev!=origprev)
      {
        eventnum+=prev.getListPlace(!inVariantVersion())-origprev.getListPlace(!inVariantVersion());
        rerender();
      }
    Event e=getRenderedEvent(snum,vnum,eventnum+1).getEvent();

    /* combine into multi-event */
    MultiEvent me;
    if (prev.geteventtype()==Event.EVENT_MULTIEVENT)
      {
        me=(MultiEvent)prev;
        me.addEvent(e);
        deleteItem(snum,vnum,eventnum+1);
      }
    else
      {
        me=new MultiEvent();
        me.addEvent(prev);
        me.addEvent(e);
        deleteItem(snum,vnum,eventnum+1);
        deleteItem(snum,vnum,eventnum);
        insertEvent(snum,vnum,eventnum,me);
      }

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  boolean validMultiEventType(Event e)
  {
    if (e.geteventtype()==Event.EVENT_LINEEND    ||
        e.geteventtype()==Event.EVENT_SECTIONEND ||
        e.geteventtype()==Event.EVENT_PROPORTION ||
        e.geteventtype()==Event.EVENT_COLORCHANGE ||
        e instanceof VariantMarkerEvent)
      return false;
    return true;
  }

/*------------------------------------------------------------------------
Method:  void insertEditorialDataSection()
Purpose: Insert editorial section start and end markers at current cursor
         location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void insertEditorialDataSection()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    if (inEditorialSection(snum,vnum,eventnum))
      return;

    Cursor.hideCursor();
    Event eds=new Event(Event.EVENT_VARIANTDATA_START),
          ede=new Event(Event.EVENT_VARIANTDATA_END);

    int insertionPlace=renderedSections[snum].getVoicedataPlace(vnum,eventnum);
    musicData.getSection(snum).getVoice(vnum).addEvent(insertionPlace,ede);
    musicData.getSection(snum).getVoice(vnum).addEvent(insertionPlace,eds);

    Cursor.setNoHighlight();
    rerender();
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,vnum,eventnum+1);
  }

/*------------------------------------------------------------------------
Method:  void toggleEditorialData()
Purpose: Toggle whether highlighted events are part of an editorial data
         section
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void toggleEditorialData()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        firstenum=Cursor.getHighlightBegin(),lastenum=Cursor.getHighlightEnd();

    Cursor.hideCursor();
/*    Event eds=new Event(Event.EVENT_VARIANTDATA_START),
          ede=new Event(Event.EVENT_VARIANTDATA_END);
    if (inEditorialSection(snum,vnum,firstenum))
      {
        musicData.getSection(snum).getVoice(vnum).addEvent(renderedSections[snum].getVoicedataPlace(vnum,lastenum+1),eds);
        musicData.getSection(snum).getVoice(vnum).addEvent(renderedSections[snum].getVoicedataPlace(vnum,firstenum),ede);
      }
    else
      {
        musicData.getSection(snum).getVoice(vnum).addEvent(renderedSections[snum].getVoicedataPlace(vnum,lastenum+1),ede);
        musicData.getSection(snum).getVoice(vnum).addEvent(renderedSections[snum].getVoicedataPlace(vnum,firstenum),eds);
      }*/

    Cursor.setNoHighlight();
    rerender();
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,vnum,firstenum+1);
  }

/*------------------------------------------------------------------------
Method:  void toggle[Editorial|Error]()
Purpose: Change generic attributes of currently highlighted event
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleEditorial()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    e.setEditorial(!e.isEditorial());

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  void toggleHighlightedEditorial()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum(),
        firste=Cursor.getHighlightBegin(),
        laste=Cursor.getHighlightEnd();

    Event orige=renderedSections[snum].eventinfo[vnum].getEvent(firste).getEvent(),
          e=getEventsForModification(snum,vnum,firste,laste);
    if (e==null)
      return; /* attempt to create invalid variant arrangement */

    firste+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    laste+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    rerender();

    for (int i=firste; i<=laste; i++)
      {
        RenderedEvent re=renderedSections[snum].eventinfo[vnum].getEvent(i);
        e=re.getEvent();
        e.setEditorial(!e.isEditorial());
      }

    rerender();
    checkVariant(snum,vnum,firste);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=firste; //eventnum;
    highlightItems(snum,vnum,firste,laste);
  }

  void toggleError()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    e.setError(!e.isError());

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:    void setMusicDataForDisplay(PieceData musicData)
Overrides: Gfx.ViewCanvas.setMusicDataForDisplay
Purpose:   Update rendered data to match current variant version
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void setMusicDataForDisplay(PieceData musicData)
  {
    super.setMusicDataForDisplay(musicData);

    /* ensure cursor remains at a valid location after re-rendering */
    Cursor.setNoHighlight();
    if (getCurEvent()==null)
      setEventNum(renderedSections[Cursor.getSectionNum()].eventinfo[Cursor.getVoiceNum()].size()-1);
  }

/*------------------------------------------------------------------------
Method:  void combineVariantReadings()
Purpose: Combine variant reading at current cursor position with next reading
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void combineVariantReadings()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    /* check that cursor is positioned at a variant reading followed by another
       variant reading */
    RenderedEventGroup vr1=getCurEvent().getVarReadingInfo();
    if (vr1==null)
      return;
    RenderedEventGroup vr2=renderedSections[snum].eventinfo[vnum].getEvent(vr1.lastEventNum+1).getVarReadingInfo();
    if (vr2==null)
      return;

    Cursor.hideCursor();

    musicData.combineReadingWithNext(curVersionMusicData,snum,vnum,
      renderedSections[snum].getVoicedataPlace(vnum,vr1.firstEventNum),
      renderedSections[snum].getVoicedataPlace(vnum,vr2.firstEventNum));

    Cursor.setNoHighlight();
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,vnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void deleteAllVariantReadings()
Purpose: Delete all variant readings at current cursor position
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteAllVariantReadings()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Cursor.hideCursor();

    musicData.deleteAllVariantReadingsAtLoc(curVariantVersion,curVersionMusicData,
      snum,vnum,renderedSections[snum].getVoicedataPlace(vnum,eventnum));

    if (!curVariantVersion.isDefault())
      parentEditorWin.reconstructCurrentVersion();
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    Cursor.setNoHighlight();
    moveCursor(snum,vnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void deleteVariantReading()
Purpose: Delete variant reading at current cursor position
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteVariantReading()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    if (curVersionMusicData.getVariantReading(snum,vnum,renderedSections[snum].getVoicedataPlace(vnum,eventnum),curVariantVersion)==null)
      return;

    Cursor.hideCursor();

    musicData.deleteVariantReading(curVariantVersion,curVersionMusicData,
      snum,vnum,renderedSections[snum].getVoicedataPlace(vnum,eventnum));

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    Cursor.setNoHighlight();
    moveCursor(snum,vnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void deleteVariantReading(VariantMarkerEvent vme,VariantVersionData vvd,VariantEditorFrame vef)
Purpose: Delete variant reading specified in popup GUI
Parameters:
  Input:  VariantMarkerEvent vme - start marker for readings
          VariantVersionData vvd - version to remove
          VariantEditorFrame vef - popup with controls for this variant
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteVariantReading(VariantMarkerEvent vme,VariantVersionData vvd,VariantEditorFrame vef)
  {
    EventLocation loc=musicData.findEvent(vme);
    if (loc==null)
      {
        vef.closeFrame();
        return;
      }

    musicData.deleteVariantReading(vvd,loc.sectionNum,loc.voiceNum,loc.eventNum);

    vef.closeFrame();
    setCurrentVariantVersion(curVariantVersion);
    rerender();
    repaint();
    parentEditorWin.fileModified();
  }

/*------------------------------------------------------------------------
Method:  void toggleVariantError()
Purpose: Toggle whether variant reading contains an error
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void toggleVariantError()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    VariantReading vr=curVersionMusicData.getVariantReading(
      snum,vnum,renderedSections[snum].getVoicedataPlace(vnum,eventnum),curVariantVersion);
    if (vr==null)
      return;
    vr.setError(!vr.isError());

    rerender();
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
  }

/*------------------------------------------------------------------------
Method:  void showCurrentVariants()
Purpose: Open popup displaying readings of variant at cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void showCurrentVariants()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventNum=Cursor.getEventNum();
    Point cursorLoc=Cursor.getLocation();

    showVariants(snum,vnum,eventNum,cursorLoc.x,cursorLoc.y);
  }

/*------------------------------------------------------------------------
Method:    void showVariants(int snum,int vnum,int eventNum,int fx,int fy)
Overrides: Gfx.ViewCanvas.showVariants
Purpose:   Show variant readings popup in GUI (if there are readings at this point)
------------------------------------------------------------------------*/

  protected void showVariants(int snum,int vnum,int eventNum,int fx,int fy)
  {
    if (musicData.getVariantVersions().size()==0)
      return;

    RenderedEvent re=renderedSections[snum].eventinfo[vnum].getEvent(eventNum);
    RenderedEventGroup renderedVar=re.getVarReadingInfo();
    if (renderedVar==null)
      return; /* no variants here */

    VariantEditorFrame varFrame=new VariantEditorFrame(
      renderedVar,
      musicData.getSection(snum).getVoice(vnum),
      renderedSections[snum],vnum,
      fx,fy,this,MusicGfx,STAFFSCALE,VIEWSCALE);

    varFrame.setVisible(true);
  }

/*------------------------------------------------------------------------
Method:  void consolidateReadings(VariantMarkerEvent vme,VariantEditorFrame vef)
Purpose: Find duplicate readings at one location and consolidate
Parameters:
  Input:  VariantMarkerEvent vme - start marker for readings
          VariantEditorFrame vef - popup with controls for this variant
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void consolidateReadings(VariantMarkerEvent vme,VariantEditorFrame vef)
  {
    EventLocation loc=musicData.findEvent(vme);
    if (loc==null)
      {
        vef.closeFrame();
        return;
      }
    if (musicData.consolidateReadings(loc))
      {
        vef.closeFrame();
        setCurrentVariantVersion(curVariantVersion);
        rerender();
        repaint();
        parentEditorWin.fileModified();
      }
  }

/*------------------------------------------------------------------------
Method:  void addVersionToReading(VariantMarkerEvent vme,int ri,
                                  VariantVersionData newv,VariantEditorFrame vef)
Purpose: Associate one variant version with one reading
Parameters:
  Input:  VariantMarkerEvent vme  - start marker for readings
          int ri                  - index of reading within marker's list
          VariantVersionData newv - version to add to reading
          VariantEditorFrame vef  - popup with controls for this variant
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addVersionToReading(VariantMarkerEvent vme,int ri,
                           VariantVersionData newv,VariantEditorFrame vef)
  {
    EventLocation loc=musicData.findEvent(vme);
    if (loc==null)
      {
        vef.closeFrame();
        return;
      }
    if (musicData.addVersionToReading(loc,ri,newv))
      {
        vef.closeFrame();
        setCurrentVariantVersion(curVariantVersion);
        rerender();
        repaint();
        parentEditorWin.fileModified();
      }
  }

/*------------------------------------------------------------------------
Method:  void setReadingAsDefault(VariantMarkerEvent vme,int ri,
                                  VariantEditorFrame vef)
Purpose: Set one variant reading as default, swapping out current default
Parameters:
  Input:  VariantMarkerEvent vme  - start marker for readings
          int ri                  - index of reading within marker's list
          VariantEditorFrame vef  - popup with controls for this variant
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setReadingAsDefault(VariantMarkerEvent vme,int ri,
                           VariantEditorFrame vef)
  {
    EventLocation loc=musicData.findEvent(vme);
    if (loc==null)
      {
        vef.closeFrame();
        return;
      }
    if (musicData.setReadingAsDefault(loc,ri))
      {
        vef.closeFrame();
        setCurrentVariantVersion(curVariantVersion);
        rerender();
        repaint();
        parentEditorWin.fileModified();
      }
  }

/*------------------------------------------------------------------------
Method:  void modifyHighlightedEventLocations(int offset)
Purpose: Change pitch/location of currently highlighted events
Parameters:
  Input:  int offset - number of places to shift event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void modifyHighlightedEventLocations(int offset)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum1=Cursor.getHighlightBegin(),
        eventnum2=Cursor.getHighlightEnd(),
        eventsAdded=0,firstEventNum=eventnum1;

    for (int i=eventnum1; i<=eventnum2; i++)
      {
        Event origEvent=renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent(),
              e=getEventForModification(snum,vnum,i);
        if (e!=origEvent)
          {
            if (i==firstEventNum)
              eventnum1+=e.getListPlace(!inVariantVersion())-origEvent.getListPlace(!inVariantVersion());
            if (e.getListPlace(!inVariantVersion())>origEvent.getListPlace(!inVariantVersion()))
              eventsAdded+=2;
          }

        switch (e.geteventtype())
          {
            case Event.EVENT_NOTE:
              e.modifyPitch(offset);
              ((NoteEvent)e).setPitchOffset(
                getCurModernKeySig(snum,vnum,i).calcNotePitchOffset(e.getPitch(),null));
              break;
            case Event.EVENT_CUSTOS:
            case Event.EVENT_DOT:
              e.modifyPitch(offset);
              break;
            case Event.EVENT_REST:
              RestEvent re=(RestEvent)e;
              re.setbottomline(re.getbottomline()+offset);
              break;
            case Event.EVENT_MENS:
              MensEvent me=(MensEvent)e;
              me.setStaffLoc(me.getStaffLoc()+offset);
              break;
            case Event.EVENT_ANNOTATIONTEXT:
              AnnotationTextEvent ae=(AnnotationTextEvent)e;
              ae.setstaffloc(ae.getstaffloc()+offset);
              break;
          }
      }
    if (eventsAdded>0)
      eventsAdded--;
    eventnum2+=eventsAdded;

    rerender();
    checkVariant(snum,vnum,eventnum1);
    repaint();
    parentEditorWin.fileModified();
    highlightItems(snum,vnum,eventnum1,eventnum2);
  }

/*------------------------------------------------------------------------
Method:  int calcStemDir(Pitch p)
Purpose: Choose stem direction for new note
Parameters:
  Input:  Pitch p - pitch for note
  Output: -
  Return: stem direction
------------------------------------------------------------------------*/

  int calcStemDir(Pitch p)
  {
    if (editorStemDir!=-1)
      return editorStemDir;

    /* editorStemDir==-1, auto-choose based on staff location */
    return (p.staffspacenum>4) ? NoteEvent.STEM_DOWN : NoteEvent.STEM_UP;
  }

/*------------------------------------------------------------------------
Method:  void addNote(int nt,Pitch p)
Purpose: Insert note at current cursor location
Parameters:
  Input:  int nt  - note type
          Pitch p - pitch for note
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addNote(int nt,Pitch p)
  {
    int     snum=Cursor.getSectionNum(),
            vnum=Cursor.getVoiceNum(),
            eventnum=Cursor.getEventNum();
    boolean inChantSection=musicData.getSection(snum).getSectionType()==MusicSection.PLAINCHANT;

    Cursor.hideCursor();
    NoteEvent ne=new NoteEvent(NoteEvent.NoteTypeNames[nt],
                               inChantSection ? null : NoteEvent.getTypeLength(nt,getCurMensInfo(snum,vnum,eventnum)),
                               new Pitch(p),new ModernAccidental(getCurModernKeySig(snum,vnum,eventnum).calcNotePitchOffset(p,null),false),
                               NoteEvent.LIG_NONE,
                               false,NoteEvent.HALFCOLORATION_NONE,
                               calcStemDir(p),-1,parentEditorWin.useFlaggedSemiminima() ? 1 : 0,
                               null);
    if (colorationOn)
      applyNoteColoration(snum,vnum,eventnum,ne,true);
    eventnum=insertEvent(snum,vnum,eventnum,ne);
  }

/*------------------------------------------------------------------------
Method:  void addNote(int nt,char nl)
Purpose: Insert note at current cursor location (assign pitch based on pitch
         of last note or clef)
Parameters:
  Input:  int nt  - note type
          char nl - note letter
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addNote(int nt,char nl)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Pitch p;
    Clef  curclef=getCurClef(snum,vnum,eventnum);
    if (curclef!=null)
      {
        NoteEvent lastnote=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
        p=lastnote!=null ? lastnote.getPitch().closestpitch(nl) :
                           new Pitch(nl,curclef.pitch.octave,curclef);
      }
    else
      p=new Pitch(nl-'A'); /* no clef, assign staff position */
    addNote(nt,p);
  }

/*------------------------------------------------------------------------
Method:  void addNote(int nt)
Purpose: Insert note at current cursor location, repeating pitch of last
         note
Parameters:
  Input:  int nt  - note type
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addNote(int nt)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    NoteEvent lastnote=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
    if (lastnote!=null)
      addNote(nt,lastnote.getPitch());
    else
      addNote(nt,'A');
  }

/*------------------------------------------------------------------------
Method:  void modifyEventPitch(int offset|char nl)
Purpose: Change pitch of currently highlighted event
Parameters:
  Input:  int offset - number of places to shift note
          char nl    - new note letter
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void modifyEventPitch(int eventOffset, int offset)
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum() + eventOffset;
    RenderedEvent re=getCurEvent(eventOffset);

    if (re == null) {
      return;
    }
    Event orige = re.getEvent(),
          e=getEventForModification(snum,vnum,eventnum);

    e.modifyPitch(offset);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    if (e.geteventtype()==Event.EVENT_CLEF)
      {
        curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();
      }
    else if (e.geteventtype()==Event.EVENT_NOTE)
      {
        ((NoteEvent)e).setPitchOffset(
          getCurModernKeySig(snum,vnum,eventnum).calcNotePitchOffset(e.getPitch(),null));
      }

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    if (eventOffset == 0) {
      highlightItems(snum,vnum,eventnum,eventnum);
    }
  }

  void modifyEventPitch(char nl)
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent();
    Pitch oldp=new Pitch(orige.getPitch()),
          newp;

    if (renderedSections[snum].eventinfo[vnum].getClefEvents(Cursor.getEventNum())!=null)
      newp=orige.getPitch().closestpitch(nl);
    else
      newp=new Pitch(nl-'A'); // no principal clef
    if (newp.equals(oldp))
      return;

    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    e.setpitch(newp);

    if (getCurEvent().getEvent().geteventtype()==Event.EVENT_CLEF)
      curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();
    else if (e.geteventtype()==Event.EVENT_NOTE)
      ((NoteEvent)e).setPitchOffset(
        getCurModernKeySig(snum,vnum,eventnum).calcNotePitchOffset(e.getPitch(),null));

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void changeNoteAccidental(int dir)
Purpose: Change modern accidental of currently highlighted note; create if
         none currently exists
Parameters:
  Input:  int dir - direction to shift pitch: 1==sharpward, -1==flatward
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void changeNoteAccidental(int dir)
  {
    changeNoteAccidental(0, dir);
  }

  void changeNoteAccidental(int eventOffset, int dir)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum() + eventOffset;
    RenderedEvent re = getCurEvent(eventOffset);
    if (re == null) {
      return;
    }
    Event orige=re.getEvent();
    if (orige.geteventtype() != Event.EVENT_NOTE) {
      return;
    }

    NoteEvent ne=(NoteEvent)getEventForModification(snum,vnum,eventnum);

/*    ModernKeySignature keySig=getCurModernKeySig(snum,vnum,eventnum);
    int                oldPitchOffset=keySig.calcNotePitchOffset(ne);
    ModernAccidental   newAcc=keySig.chooseNoteAccidental(ne,oldPitchOffset+dir),
                       oldAcc=ne.getModAcc();
    if (newAcc==null || oldAcc==null || !newAcc.equals(oldAcc))
      ne.setModAcc(newAcc);*/
    ne.modifyPitchOffset(dir);

    eventnum+=ne.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    if (eventOffset == 0)
      highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleNoteAccidentalOptional()
Purpose: Toggle optional status of modern accidental of currently highlighted
         note
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleNoteAccidentalOptional()
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    NoteEvent origne=(NoteEvent)getCurEvent().getEvent();

    ModernAccidental ma=origne.getPitchOffset();
    if (ma==null)
      return;

    NoteEvent ne=(NoteEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=ne.getListPlace(!inVariantVersion())-origne.getListPlace(!inVariantVersion());
    ma=ne.getPitchOffset();

    ma.optional=!ma.optional;

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void cycleTieType()
Purpose: Cycle through tie types (over, under, none) for currently highlighted
         note
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void cycleTieType()
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    NoteEvent origne=(NoteEvent)getCurEvent().getEvent();

    int newTieType=NoteEvent.TIE_NONE;
    switch (origne.getTieType())
      {
        case NoteEvent.TIE_NONE:
          newTieType=NoteEvent.TIE_OVER;
          break;
        case NoteEvent.TIE_OVER:
          newTieType=NoteEvent.TIE_UNDER;
          break;
      }

    NoteEvent ne=(NoteEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=ne.getListPlace(!inVariantVersion())-origne.getListPlace(!inVariantVersion());

    ne.setTieType(newTieType);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void modifyNoteType(int nt)
Purpose: Change note type of currently highlighted event
Parameters:
  Input:  int nt - new note type
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void modifyNoteType(int nt)
  {
    modifyNoteType(0, nt);
  }

  void modifyNoteType(int eventOffset, int nt)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum() + eventOffset;
    RenderedEvent re = getCurEvent(eventOffset);
    if (re == null) {
      return;
    }

    Cursor.hideCursor();
    Event orige=re.getEvent(),
          e=getEventForModification(snum,vnum,eventnum);

    int     oldnt=e.getnotetype();
    boolean oldf=e.isflagged();
    e.setnotetype(nt,parentEditorWin.useFlaggedSemiminima() ? 1 : 0,getCurMensInfo(snum,vnum,eventnum));
    if (e.getLength()!=null)
      e.setLength(NoteEvent.getTypeLength(nt,getCurMensInfo(snum,vnum,eventnum)));

    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    if (e.getnotetype()!=oldnt || e.isflagged()!=oldf)
      parentEditorWin.fileModified();
    hl_anchor=eventnum;
    if (eventOffset == 0)
      highlightItems(snum,vnum,eventnum,eventnum);
    else
      moveCursor(snum, vnum, eventnum - eventOffset);
  }

/*------------------------------------------------------------------------
Method:  void modifyEventTime(Proportion p)
Purpose: Change length of currently highlighted event
Parameters:
  Input:  Proportion p - time amount to add/subtract
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void modifyEventTime(Proportion p)
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event e=getCurEvent().getEvent();

    Proportion curLen=e.getLength();
    if (curLen==null)
      return;

    setEventProportion(Proportion.sum(curLen,p));
  }

/*------------------------------------------------------------------------
Method:  void perfectNote()
Purpose: Make currently highlighted note perfect
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void perfectNote()
  {
    perfectNote(0);
  }

  void perfectNote(int eventOffset)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum() + eventOffset;
    RenderedEvent re = getCurEvent(eventOffset);
    if (re == null) {
      return;
    }

    Event e=re.getEvent();
    if (e.geteventtype()!=Event.EVENT_NOTE)
      return;

    NoteEvent   ne=(NoteEvent)e;
    Mensuration curmens=getCurMensInfo(snum,vnum,eventnum);
    if (!ne.canBePerfect(curmens))
      return;

    setEventProportion(eventOffset, new Proportion(NoteEvent.getTypeLength(ne.getnotetype(),curmens)));
  }

/*------------------------------------------------------------------------
Method:  void imperfectNote()
Purpose: Make currently highlighted note imperfect
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void imperfectNote()
  {
    imperfectNote(0);
  }

  void imperfectNote(int eventOffset)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum() + eventOffset;
    RenderedEvent re = getCurEvent(eventOffset);
    if (re == null) {
      return;
    }

    Event e=re.getEvent();
    if (e.geteventtype()!=Event.EVENT_NOTE)
      return;

    NoteEvent   ne=(NoteEvent)e;
    Mensuration curmens=getCurMensInfo(snum,vnum,eventnum);
    if (!ne.canBePerfect(curmens))
      return;

    Proportion newlength=NoteEvent.getTypeLength(ne.getnotetype(),curmens);
    newlength.multiply(2,3);
    setEventProportion(eventOffset, newlength);
  }

/*------------------------------------------------------------------------
Method:  void alterNote()
Purpose: Apply alteration to currently highlighted note
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void alterNote()
  {
    alterNote(0);
  }

  void alterNote(int eventOffset)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum() + eventOffset;
    RenderedEvent re = getCurEvent(eventOffset);
    if (re == null) {
      return;
    }

    Event e=re.getEvent();
    if (e.geteventtype()!=Event.EVENT_NOTE)
      return;

    NoteEvent   ne=(NoteEvent)e;
    Mensuration curmens=getCurMensInfo(snum,vnum,eventnum);
    if (!ne.canBeAltered(curmens))
      return;

    Proportion newlength=NoteEvent.getTypeLength(ne.getnotetype(),curmens);
    newlength.multiply(2,1);
    setEventProportion(eventOffset, newlength);
  }

/*------------------------------------------------------------------------
Method:  void applyNoteColoration(int snum,int vnum,int eventnum,boolean color)
Purpose: Update note event info based on coloration info (do not display)
Parameters:
  Input:  int snum,vnum,eventnum - section/voice/event number
          boolean color          - whether to color or not
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void applyNoteColoration(int snum,int vnum,int eventnum,NoteEvent ne,boolean color)
  {
    ne.setColored(color);

    /* choose coloration effect: sesquialtera, imperfection, or 'minor color' */
    int        nt=ne.getnotetype();
    Proportion curLength=ne.getLength();

    if (curLength==null)
      return;

    if (ne.isColored())
      switch (editorColorationType)
        {
          case Coloration.IMPERFECTIO:
            Mensuration curmens=getCurMensInfo(snum,vnum,eventnum);
//            if (ne.canBePerfect(curmens))
              ne.setLength(NoteEvent.DefaultLengths[ne.getnotetype()]);
            break;
          case Coloration.SESQUIALTERA:
            curLength.multiply(2,3);
            ne.setLength(curLength);
            break;
          case Coloration.MINOR_COLOR:
            int        lnevnum=getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
            NoteEvent  lne=lnevnum==-1 ? null : (NoteEvent)getRenderedEvent(snum,vnum,lnevnum).getEvent();
            Proportion lastDefaultLen=lne!=null ?
              NoteEvent.getTypeLength(lne.getnotetype(),getCurMensInfo(snum,vnum,lnevnum)) : null;
            if (nt<NoteEvent.NT_Semibrevis ||
                (lne!=null && lne.isColored() &&
                 lne.getLength().toDouble()>(double)lastDefaultLen.toDouble()*2/3))
              /* minor color already started; note value is halved */
              curLength.multiply(1,2);
            else
              /* start new minor color; note value *= 3/4 */
              curLength.multiply(3,4);
            ne.setLength(curLength);
            break;
        }
    else
      /* de-color */
      ne.setLength(NoteEvent.getTypeLength(ne.getnotetype(),getCurMensInfo(snum,vnum,eventnum)));
  }

/*------------------------------------------------------------------------
Method:  void toggleNoteColoration()
Purpose: Toggle note between colored/uncolored
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleNoteColoration()
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    Event     orige=getCurEvent().getEvent();
    NoteEvent ne=(NoteEvent)getEventForModification(snum,vnum,eventnum);

    Cursor.hideCursor();

    applyNoteColoration(snum,vnum,eventnum,ne,!ne.isColored());

    eventnum+=ne.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleHighlightedColoration()
Purpose: Toggle highlighted notes between colored/uncolored
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleHighlightedColoration()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum(),
        firste=Cursor.getHighlightBegin(),
        laste=Cursor.getHighlightEnd();

    Event orige=renderedSections[snum].eventinfo[vnum].getEvent(firste).getEvent(),
          e=getEventsForModification(snum,vnum,firste,laste);
    if (e==null)
      return; /* attempt to create invalid variant arrangement */

    firste+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    laste+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    rerender();

    for (int i=firste; i<=laste; i++)
      {
        RenderedEvent re=renderedSections[snum].eventinfo[vnum].getEvent(i);
        if (re.getEvent().geteventtype()==Event.EVENT_NOTE)
          {
            NoteEvent ne=(NoteEvent)re.getEvent();
            applyNoteColoration(snum,vnum,i,ne,!ne.isColored());
          }
      }

    rerender();
    checkVariant(snum,vnum,firste);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=firste; //eventnum;
    highlightItems(snum,vnum,firste,laste);
  }

/*------------------------------------------------------------------------
Method:  void cycleNoteHalfColoration()
Purpose: Change half-coloration attributes of currently highlighted note
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void cycleNoteHalfColoration()
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    NoteEvent origne=(NoteEvent)getCurEvent().getEvent();

    if (origne.getnotetype()!=NoteEvent.NT_Brevis &&
        origne.getnotetype()!=NoteEvent.NT_Longa &&
        origne.getnotetype()!=NoteEvent.NT_Maxima)
      return;

    NoteEvent ne=(NoteEvent)getEventForModification(snum,vnum,eventnum);

    Cursor.hideCursor();

    int newHalfCol=ne.getHalfColoration();
    switch (newHalfCol)
      {
        case NoteEvent.HALFCOLORATION_NONE:
          newHalfCol=NoteEvent.HALFCOLORATION_PRIMARYSECONDARY;
          break;
        case NoteEvent.HALFCOLORATION_PRIMARYSECONDARY:
          newHalfCol=NoteEvent.HALFCOLORATION_SECONDARYPRIMARY;
          break;
        case NoteEvent.HALFCOLORATION_SECONDARYPRIMARY:
          newHalfCol=NoteEvent.HALFCOLORATION_NONE;
          break;
      }
    ne.setHalfColoration(newHalfCol);

    eventnum+=ne.getListPlace(!inVariantVersion())-origne.getListPlace(!inVariantVersion());
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void cycleNoteStemDirection(NoteEvent ne)
Purpose: Change attributes of currently highlighted note - cycle stem
         direction (up/down/barline)
Parameters:
  Input:  NoteEvent ne - note to modify
  Output: NoteEvent ne
  Return: -
------------------------------------------------------------------------*/

  void cycleNoteStemDirection(NoteEvent ne)
  {
    int newStemDir=ne.getstemdir();

    if (ne.getnotetype()==NoteEvent.NT_Longa || ne.getnotetype()==NoteEvent.NT_Maxima)
      switch (newStemDir)
        {
          case NoteEvent.STEM_UP:
            newStemDir=NoteEvent.STEM_DOWN;
            break;
          case NoteEvent.STEM_DOWN:
            newStemDir=NoteEvent.STEM_BARLINE;
            break;
          case NoteEvent.STEM_BARLINE:
            newStemDir=NoteEvent.STEM_UP;
            break;
        }
    else if (getCurSectionType()==MusicSection.PLAINCHANT)
      switch (newStemDir)
        {
          case NoteEvent.STEM_UP:
            newStemDir=NoteEvent.STEM_DOWN;
            break;
          case NoteEvent.STEM_DOWN:
            newStemDir=NoteEvent.STEM_NONE;
            break;
          case NoteEvent.STEM_NONE:
            newStemDir=NoteEvent.STEM_UP;
            if (ne.getstemside()==NoteEvent.STEM_NONE)
              ne.setstemside(NoteEvent.STEM_LEFT);
            break;
        }
    else
      /* simply toggle up/down */
      newStemDir=(newStemDir==NoteEvent.STEM_UP) ? NoteEvent.STEM_DOWN : NoteEvent.STEM_UP;

    ne.setstemdir(newStemDir);
  }

/*------------------------------------------------------------------------
Method:  void cycleHighlightedStemDirections()
Purpose: Change attributes of all currently highlighted notes - cycle stem
         direction (up/down/barline)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void cycleHighlightedStemDirections()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum1=Cursor.getHighlightBegin(),
        eventnum2=Cursor.getHighlightEnd(),
        eventsAdded=0,firstEventNum=eventnum1;

    for (int i=eventnum1; i<=eventnum2; i++)
      {
        Event origEvent=renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent();
        if (origEvent.geteventtype()==Event.EVENT_NOTE)
          {
            Event e=getEventForModification(snum,vnum,i);
            if (e!=origEvent)
              {
                if (i==firstEventNum)
                  eventnum1+=e.getListPlace(!inVariantVersion())-origEvent.getListPlace(!inVariantVersion());
                if (e.getListPlace(!inVariantVersion())>origEvent.getListPlace(!inVariantVersion()))
                  eventsAdded+=2;
              }
            if (e.geteventtype()==Event.EVENT_NOTE)
              cycleNoteStemDirection((NoteEvent)e);
          }
      }
    if (eventsAdded>0)
      eventsAdded--;
    eventnum2+=eventsAdded;

    rerender();
    checkVariant(snum,vnum,eventnum1);
    repaint();
    parentEditorWin.fileModified();
    highlightItems(snum,vnum,eventnum1,eventnum2);
  }

/*------------------------------------------------------------------------
Method:  void ligateTwoNotes(int snum,int vnum,int note1num)
Purpose: Choose ligation type and ligate two notes
Parameters:
  Input:  int snum,vnum - section/voice number
          int note1num  - event number of left note
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void ligateTwoNotes(int snum,int vnum,int note1num)
  {
    NoteEvent ne=(NoteEvent)(renderedSections[snum].eventinfo[vnum].getEvent(note1num).getEvent()),
              nextne=null,lastne=null;

    ne.setligtype(NoteEvent.LIG_RECTA);

    nextne=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,note1num+1,1);
    lastne=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,note1num-1,-1);

    /* lig first note type */
    if (lastne==null || !lastne.isligated())
      {
        /* COP stem */
        if (ne.getnotetype()==NoteEvent.NT_Semibrevis &&
            nextne!=null &&
            nextne.getnotetype()==NoteEvent.NT_Semibrevis)
          {
            ne.setstemdir(NoteEvent.STEM_UP);
            ne.setstemside(NoteEvent.STEM_LEFT);

            /* most commonly recta ascending, obliqua descending */
            if (ne.getPitch().isHigherThan(nextne.getPitch()))
              ne.setligtype(NoteEvent.LIG_OBLIQUA);
          }

        /* CP stem */
        if (ne.getnotetype()==NoteEvent.NT_Brevis)
          if (nextne!=null && ne.getPitch().isHigherThan(nextne.getPitch()))
            {
              if (nextne.getnotetype()==NoteEvent.NT_Brevis)
                ne.setligtype(NoteEvent.LIG_OBLIQUA);
              ne.setstemdir(NoteEvent.STEM_DOWN);
              ne.setstemside(NoteEvent.STEM_LEFT);
            }

        /* longa stems */
        if (ne.getnotetype()==NoteEvent.NT_Longa)
          if (nextne!=null)
            if (nextne.getPitch().isHigherThan(ne.getPitch()))
              {
                ne.setstemdir(NoteEvent.STEM_DOWN);
                ne.setstemside(NoteEvent.STEM_RIGHT);
              }
      }
    else /* ligature middle */
      switch (ne.getnotetype())
        {
          case NoteEvent.NT_Longa:
            ne.setstemdir(NoteEvent.STEM_DOWN);
            ne.setstemside(NoteEvent.STEM_RIGHT);
            break;
        }

    /* ligature end */
    if (nextne!=null && !nextne.isligated())
      switch (nextne.getnotetype())
        {
          case NoteEvent.NT_Brevis:
            if (!nextne.getPitch().isHigherThan(ne.getPitch()))
              ne.setligtype(NoteEvent.LIG_OBLIQUA);
            break;
          case NoteEvent.NT_Longa:
            if (nextne.getPitch().isHigherThan(ne.getPitch()))
              {
                nextne.setstemdir(NoteEvent.STEM_DOWN);
                nextne.setstemside(NoteEvent.STEM_RIGHT);
              }
            else
              nextne.setstemside(-1);
            break;
        }
  }

/*------------------------------------------------------------------------
Method:  void ligateHighlighted()
Purpose: Ligate or un-ligate highlighted notes
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void ligateHighlighted()
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              numnotes=0,
              firstNotei,lastNotei;
    boolean   waslig=false,
              addlig=true,
              modified=false;
    NoteEvent ne=null,
              ne1,ne2;

    /* toggle ligation for each highlighted note */
    firstNotei=getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,Cursor.getHighlightBegin(),1);
    lastNotei=getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,Cursor.getHighlightEnd(),-1);
    if (firstNotei==-1 || lastNotei==-1 || firstNotei==lastNotei)
      return;

    ne1=(NoteEvent)getRenderedEvent(snum,vnum,firstNotei).getEvent();
    ne2=(NoteEvent)getRenderedEvent(snum,vnum,lastNotei).getEvent();
    if (curVariantVersion!=musicData.getDefaultVariantVersion())
      {
        ne=(NoteEvent)musicData.duplicateEventsInVariant(
          curVariantVersion,curVersionMusicData,
          snum,vnum,ne1.getListPlace(false),ne2.getListPlace(false));
        if (ne==null)
          return; /* attempt to create invalid variant arrangement */

        int offset=ne.getListPlace(false)-ne1.getListPlace(false);
        firstNotei+=offset;
        lastNotei+=offset;
        rerender();
      }

    for (int i=firstNotei; i<=lastNotei; i++)
      {
        Event e=renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent();
        if (e.geteventtype()==Event.EVENT_NOTE)
          {
            modified=true;
            numnotes++;
            ne=(NoteEvent)e;
            waslig=ne.isligated();

            /* ligate or de-ligate? depends on first note */
            if (numnotes==1 && waslig)
              addlig=false;

            if (addlig)
              {
                /* ligate! (but don't overwrite existing lig information) */
                if (i<lastNotei && !waslig)
                  ligateTwoNotes(snum,vnum,i);
              }
            else
              ne.setligtype(NoteEvent.LIG_NONE);
          }
      }

    /* remove any NEW ligation mark from final note */
    if (ne!=null && !waslig)
      ne.setligtype(NoteEvent.LIG_NONE);

    rerender();
    checkVariant(snum,vnum,firstNotei);
    repaint();
    if (modified)
      parentEditorWin.fileModified();
    moveCursor(snum,vnum,firstNotei);
  }

/*------------------------------------------------------------------------
Method:  void ligateNoteToLast()
Purpose: Ligate or un-ligate currently highlighted note with previous note
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void ligateNoteToLast()
  {
    ligateNoteToLast(-1);
  }

  void ligateNoteToLast(int eventOffset)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum() + eventOffset,
        lastneNum=getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
    RenderedEvent re = getCurEvent(eventOffset);

    if (lastneNum==-1 || re == null)
      return;
    int origNEplace=re.getEvent().getListPlace(!inVariantVersion());

    NoteEvent origLastNE=(NoteEvent)renderedSections[snum].eventinfo[vnum].getEvent(lastneNum).getEvent(),
              lastne=getNoteEventForLigation(snum,vnum,lastneNum,eventnum);
    if (lastne==null)
      return; /* attempt to create invalid variant arrangement */
    int newNEplace=curVersionMusicData.getSection(snum).getVoice(vnum).getNextEventOfType(
          Event.EVENT_NOTE,lastne.getListPlace(!inVariantVersion())+1,1);
    eventnum+=newNEplace-origNEplace;

    rerender(); /* need to rerender, because ligateTwoNotes reads from rendered list */

    lastneNum=getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
    lastne=(NoteEvent)renderedSections[snum].eventinfo[vnum].getEvent(lastneNum).getEvent();
    if (!lastne.isligated())
      ligateTwoNotes(snum,vnum,lastneNum); //lastne==origLastNE ? lastneNum : lastneNum+1);
    else
      lastne.setligtype(NoteEvent.LIG_NONE); /* toggle ligation */

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void changeLigType(int newligtype)
Purpose: Change ligature connection type of one note
Parameters:
  Input:  int newligtype - new connection type
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void changeLigType(int newligtype)
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum1=Cursor.getEventNum(),
              eventnum2,
              finalEvNum=eventnum1;
    NoteEvent ne=(NoteEvent)getCurEvent().getEvent(),
              origne=ne;

    if (getCurEvent().isligend())
      {
        eventnum2=eventnum1;
        eventnum1=getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,eventnum2-1,-1);
        ne=(NoteEvent)getRenderedEvent(snum,vnum,eventnum1).getEvent();
        origne=ne;
      }
    else
      eventnum2=getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,eventnum1+1,1);
    if (ne.getligtype()==NoteEvent.LIG_NONE || ne.getligtype()==newligtype ||
        eventnum2>=getNumRenderedEvents(snum,vnum))
      return;

    ne=getNoteEventForLigation(snum,vnum,eventnum1,eventnum2);
    if (ne==null)
      return; /* attempt to create invalid variant arrangement */

    finalEvNum+=ne!=origne ? 1 : 0;

    ne.setligtype(newligtype);

    rerender();
    checkVariant(snum,vnum,finalEvNum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=finalEvNum;
    highlightItems(snum,vnum,finalEvNum,finalEvNum);
  }

/*------------------------------------------------------------------------
Method:  void setNoteSyllable(String s,boolean we)
Purpose: Set text syllable on note
Parameters:
  Input:  String s   - syllable text
          boolean we - word end?
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setNoteSyllable(String s,boolean we)
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    Event     orige=getCurEvent().getEvent();
    NoteEvent ne;

    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    if (e.geteventtype()==Event.EVENT_MULTIEVENT)
      ne=((MultiEvent)e).getLowestNote();
    else
      ne=(NoteEvent)e;

    ne.setModernText(s);
    ne.setWordEnd(we);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleEditorialText()
Purpose: Toggle editorial status of text on current note
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

/*  void toggleEditorialText()
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    Event     orige=getCurEvent().getEvent();
    NoteEvent ne;

    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    if (e.geteventtype()==Event.EVENT_MULTIEVENT)
      ne=((MultiEvent)e).getLowestNote();
    else
      ne=(NoteEvent)e;

    ne.setModernTextEditorial(!ne.isModernTextEditorial());

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }*/

  void toggleHighlightedEditorialText()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum1=Cursor.getHighlightBegin(),
        eventnum2=Cursor.getHighlightEnd(),
        eventsAdded=0,firstEventNum=eventnum1;

    for (int i=eventnum1; i<=eventnum2; i++)
      {
        Event origEvent=renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent();
        if (origEvent.geteventtype()==Event.EVENT_NOTE)
          {
            Event e=getEventForModification(snum,vnum,i);
            if (e!=origEvent)
              {
                if (i==firstEventNum)
                  eventnum1+=e.getListPlace(!inVariantVersion())-origEvent.getListPlace(!inVariantVersion());
                if (e.getListPlace(!inVariantVersion())>origEvent.getListPlace(!inVariantVersion()))
                  eventsAdded+=2;
              }
            if (e.geteventtype()==Event.EVENT_NOTE)
              {
                NoteEvent ne=(NoteEvent)e;
                ne.setModernTextEditorial(!ne.isModernTextEditorial());
              }
          }
      }
    if (eventsAdded>0)
      eventsAdded--;
    eventnum2+=eventsAdded;

    rerender();
    checkVariant(snum,vnum,eventnum1);
    repaint();
    parentEditorWin.fileModified();
    highlightItems(snum,vnum,eventnum1,eventnum2);
  }

/*------------------------------------------------------------------------
Method:  void toggleCorona()
Purpose: Toggle presence of corona on a note or rest
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleCorona()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Event orige=getCurEvent().getEvent();
    Cursor.hideCursor();
    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    e.setCorona((e.getCorona()==null) ? new Signum(Signum.UP,Signum.MIDDLE) :
                                        null);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleSignum()
Purpose: Toggle presence of a signum congruentiae on a note or rest
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleSignum()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);

    e.setSignum((e.getSignum()==null) ? new Signum(4,Signum.UP,Signum.MIDDLE) :
                                        null);

    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  void toggleSignumOrientation()
  {
    int    snum=Cursor.getSectionNum(),
           vnum=Cursor.getVoiceNum(),
           eventnum=Cursor.getEventNum();
    Event  orige=getCurEvent().getEvent();
    Signum s=orige.getSignum();
    if (s==null)
      return;
    Event e=getEventForModification(snum,vnum,eventnum);

    s.orientation=s.orientation==Signum.UP ? Signum.DOWN : Signum.UP;

    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  void cycleSignumSide()
  {
    int    snum=Cursor.getSectionNum(),
           vnum=Cursor.getVoiceNum(),
           eventnum=Cursor.getEventNum();
    Event  orige=getCurEvent().getEvent();
    Signum s=orige.getSignum();
    if (s==null)
      return;
    Event e=getEventForModification(snum,vnum,eventnum);

    switch (s.side)
      {
        case Signum.LEFT:
          s.side=Signum.MIDDLE;
          break;
        case Signum.MIDDLE:
          s.side=Signum.RIGHT;
          break;
        case Signum.RIGHT:
          s.side=Signum.LEFT;
          break;
      }

    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  void shiftSignumVertical(int offset)
  {
    int    snum=Cursor.getSectionNum(),
           vnum=Cursor.getVoiceNum(),
           eventnum=Cursor.getEventNum();
    Event  orige=getCurEvent().getEvent();

    if (orige.geteventtype()!=Event.EVENT_NOTE &&
        orige.geteventtype()!=Event.EVENT_REST)
      return;

    Signum s=orige.getSignum();
    if (s==null)
      return;
    Event e=getEventForModification(snum,vnum,eventnum);

    s.offset+=offset;

    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void addRest(int nt)
Purpose: Insert rest at current cursor location
Parameters:
  Input:  int nt  - note type
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addRest(int nt)
  {
    int         snum=Cursor.getSectionNum(),
                vnum=Cursor.getVoiceNum(),
                eventnum=Cursor.getEventNum();
    Mensuration m=getCurMensInfo(snum,vnum,eventnum);
    boolean     inChantSection=musicData.getSection(snum).getSectionType()==MusicSection.PLAINCHANT;

    Cursor.hideCursor();

    /* guess vertical position of rest */
    int newVPos=2, /* default */
        lastNoteNum=getNeighboringEventNumOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1),
        lastRestNum=getNeighboringEventNumOfType(Event.EVENT_REST,snum,vnum,eventnum-1,-1),
        posEvNum=Math.max(lastNoteNum,lastRestNum);
    if (posEvNum>-1)
      newVPos=getRenderedEvent(snum,vnum,posEvNum).getssnum()/2+1;

    /* add rest */
    RestEvent re=new RestEvent(NoteEvent.NoteTypeNames[nt],
                               inChantSection ? null : NoteEvent.getTypeLength(nt,m),
                               newVPos,RestEvent.calcNumLines(nt,m),m.modus_maior);
    if (colorationOn)
      re.setColored(true);
    eventnum=insertEvent(snum,vnum,eventnum,re);
  }

/*------------------------------------------------------------------------
Method:  void addDot([int snum,int vnum,int eventnum,]int dottype)
Purpose: Insert dot at a given location
Parameters:
  Input:  int snum,vnum,eventnum - insertion location
          int dottype            - dot attributes (Addition, Division, etc.)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addDot(int snum,int vnum,int eventnum,int dottype)
  {
    Pitch     dotPitch=null;
    NoteEvent ln=null;

    Cursor.hideCursor();
    RenderedEvent lre=renderedSections[snum].eventinfo[vnum].getEvent(eventnum-1);
    if ((dottype&DotEvent.DT_Addition)==0 ||
        lre==null || lre.getEvent().geteventtype()!=Event.EVENT_NOTE ||
        lre.getEvent().getLength()==null)
      {
        /* not a dot of addition */
        dotPitch=new Pitch(10);
        Clef c=getCurClef(snum,vnum,eventnum);
        if (c!=null)
          dotPitch=new Pitch(c.getStaffLocPitch(10));
        dottype=DotEvent.DT_Division;
      }
    else
      {
        /* dot of addition: adjust length of previous note */
        ln=(NoteEvent)getEventForModification(snum,vnum,eventnum-1);
        eventnum+=ln.getListPlace(!inVariantVersion())-lre.getEvent().getListPlace(!inVariantVersion());
        ln.setLength(Proportion.product(ln.getLength(),new Proportion(3,2)));
        if (inVariantVersion())
          rerender();

        int nsl=ln.getPitch().staffspacenum;
        dotPitch=new Pitch(ln.getPitch());
        if (nsl%2==0)
          dotPitch.add(1);

        /* select next-lowest note value for future input */
        parentEditorWin.selectNVButton(parentEditorWin.NTtoBNum(ln.getnotetype())+1);
      }
    DotEvent de=new DotEvent(dottype,dotPitch,ln);

    eventnum=insertEvent(snum,vnum,eventnum,de);
  }

  public void addDot(int dottype)
  {
    if (Cursor.oneItemHighlighted())
      addDot(Cursor.getSectionNum(),Cursor.getVoiceNum(),Cursor.getHighlightBegin()+1,dottype);
    else if (Cursor.getHighlightBegin()==-1)
      addDot(Cursor.getSectionNum(),Cursor.getVoiceNum(),Cursor.getEventNum(),dottype);
  }

/*------------------------------------------------------------------------
Method:  void shiftDotPositions(int offset,int snum,int vnum,int startei)
Purpose: Change vertical locations of all dots of addition until next principal
         clef
Parameters:
  Input:  int offset    - number of places to shift dots
          int snum,vnum - section and voice number
          int startei   - index of first event in segment
  Output: -
  Return: -
------------------------------------------------------------------------*/

/* FIX: deal with versions and dot positions */
/* FIXED: dot position now represented as pitch */

  void shiftDotPositions(int offset,int snum,int vnum,int startei)
  {
    return;
/*    if (offset==0)
      return;

    Event cure=curVersionMusicData.getSection(snum).getVoice(vnum).getEvent(startei);
    while (cure!=null && !cure.hasPrincipalClef())
      {
        if (cure.geteventtype()==Event.EVENT_DOT)
          {
            DotEvent de=(DotEvent)cure;
            if ((de.getdottype()&DotEvent.DT_Addition)!=0)
              de.modifyPitch(offset);
          }
        cure=curVersionMusicData.getSection(snum).getVoice(vnum).getEvent(++startei);
      }*/
  }

/*------------------------------------------------------------------------
Method:  void doClefAction([int ct])
Purpose: Choose and perform appropriate action after clef-button or key
         has been pressed
Parameters:
  Input:  int ct - clef type indicated by user input
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void doClefAction(int ct)
  {
    if (getHighlightBegin()==-1)
      addClef(ct);
    else if (oneItemHighlighted() && getCurEvent().getEvent().geteventtype()==Event.EVENT_CLEF)
      modifyClefType(ct);
  }

  public void doClefAction()
  {
    if (getHighlightBegin()==-1)
      addClef();
  }

/*------------------------------------------------------------------------
Method:  void addClef([Clef c|int ct])
Purpose: Insert clef at current cursor location
Parameters:
  Input:  Clef c - clef information to insert
          int ct - type for new clef
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addClef(Clef c)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();
    ModernKeySignature oldSig=new ModernKeySignature(getCurModernKeySig(snum,vnum,eventnum));

    Cursor.hideCursor();
    Event laste=null;
    if (eventnum>0 && !c.isprincipalclef()) /* a new principal clef always starts a new clef set */
      laste=renderedSections[snum].eventinfo[vnum].getEvent(eventnum-1).getEvent();
    ClefEvent ce=new ClefEvent(new Clef(c),laste,getCurClefEvent(snum,vnum,eventnum));

    Clef oldc=getCurClef(snum,vnum,eventnum);
    if (oldc!=null)
      shiftDotPositions((oldc.line1placenum-c.line1placenum)/2,snum,vnum,eventnum);

    eventnum=insertEvent(snum,vnum,eventnum,ce);

/*    int startei=firstEventNumAfterClefSet(snum,vnum,eventnum+1,ce.getClefSet());
    updateNoteAccidentals(snum,vnum,startei,oldSig,ce.getClefSet().getKeySig());*/
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  void addClef(int ct)
  {
    Clef curclef=getCurClef(Cursor.getSectionNum(),Cursor.getVoiceNum(),Cursor.getEventNum());
    if (curclef!=null && curclef.cleftype==ct)
      addClef(); /* duplicate current clef */
    else
      addClef(new Clef(ct,Clef.defaultClefLoc(ct),Clef.DefaultClefPitches[ct],
                       false,Clef.isFlatType(ct),curclef));
  }

  /* duplicate current principal clef */
  void addClef()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Clef curclef=getCurClef(snum,vnum,eventnum);
    if (curclef!=null)
      addClef(curclef);
    else
      addClef(new Clef(Clef.CLEF_C,1,Clef.DefaultClefPitches[Clef.CLEF_C],false,false,null));
  }

/*------------------------------------------------------------------------
Method:  void modifyClef*(int ct,int loc|int offset)
Purpose: Change attributes of currently highlighted clef
Parameters:
  Input:  int ct     - new clef type
          int loc    - new vertical location
          int offset - number of places to shift clef vertically
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void modifyClef(int ct,int loc)
  {
    int                snum=Cursor.getSectionNum(),
                       vnum=Cursor.getVoiceNum(),
                       eventnum=Cursor.getEventNum();
    ClefEvent          origce=(ClefEvent)getCurEvent().getEvent(),
                       ce=(ClefEvent)getEventForModification(snum,vnum,eventnum);
    Clef               c=ce.getClef(false,false),
                       displayClef=getCurClef(snum,vnum,eventnum-1);
    int                oldct=c.getcleftype(),
                       oldloc=c.getloc(),
                       oldline1=c.line1placenum;
    boolean            sig=c.signature();
    ModernKeySignature oldSig=getCurModernKeySig(snum,vnum,eventnum);

    eventnum+=ce.getListPlace(!inVariantVersion())-origce.getListPlace(!inVariantVersion());

    if (!c.isflat() && Clef.isFlatType(ct)) /* mark flats as signature clefs by default */
      sig=true;
    c.setattributes(ct,loc,ct!=c.cleftype ? Clef.DefaultClefPitches[ct] : c.pitch,false,sig,displayClef);
    curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();

/*    shiftDotPositions((oldline1-c.line1placenum)/2,snum,vnum,eventnum+1);
    int startei=firstEventNumAfterClefSet(snum,vnum,eventnum+1,ce.getClefSet());
    ModernKeySignature newSig=sig ? ce.getClefSet().getKeySig() :
                                    ce.getModernKeySig();
    updateNoteAccidentals(snum,vnum,startei,oldSig,newSig);*/

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    if (c.getcleftype()!=oldct || c.getloc()!=oldloc || ce!=origce)
      parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  void modifyClefType(int ct)
  {
    int       vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    ClefEvent ce=(ClefEvent)getCurEvent().getEvent();
    Clef      c=ce.getClef(false,false);

    if (ct!=c.cleftype)
      modifyClef(ct,Clef.defaultClefLoc(ct));

    else if (ct==Clef.CLEF_Bmol)
      modifyClef(Clef.CLEF_BmolDouble,c.getloc());
    else if (ct==Clef.CLEF_BmolDouble)
      modifyClef(Clef.CLEF_Bmol,c.getloc());

    else if (ct==Clef.CLEF_F)
      modifyClef(Clef.CLEF_Frnd,c.getloc());
    else if (ct==Clef.CLEF_Frnd)
      modifyClef(Clef.CLEF_F,c.getloc());

    else if (ct==Clef.CLEF_G)
      modifyClef(Clef.CLEF_Gamma,c.getloc());
  }

  void modifyClefLocation(int offset)
  {
    int       vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    ClefEvent ce=(ClefEvent)getCurEvent().getEvent();
    Clef      c=ce.getClef(false,false);

    modifyClef(c.cleftype,c.linespacenum+offset*(c.isprincipalclef() ? 2 : 1));
  }

  void toggleClefSignatureStatus()
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    ClefEvent origce=(ClefEvent)getCurEvent().getEvent();
    Clef      c=origce.getClef(false,false);
    ModernKeySignature oldSig=getCurModernKeySig(snum,vnum,eventnum);

    if (!(c.isflat() || c.issharp()))
      return;

    ClefEvent ce=(ClefEvent)getEventForModification(snum,vnum,eventnum);
    c=ce.getClef(false,false);

    c.setSignature(!c.signature());
    curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();
/*    int startei=firstEventNumAfterClefSet(snum,vnum,eventnum+1,ce.getClefSet());
    ModernKeySignature newSig=c.signature() ? ce.getClefSet().getKeySig() :
                                              ce.getModernKeySig();
    updateNoteAccidentals(snum,vnum,startei,oldSig,newSig);*/

    eventnum+=ce.getListPlace(!inVariantVersion())-origce.getListPlace(!inVariantVersion());
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);    
  }

  void toggleEventColoration()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    e.setColored(!e.isColored());

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);    
  }

/*------------------------------------------------------------------------
Method:  void addMensurationSign(MensSignElement mse)
Purpose: Insert mensuration sign at current cursor location
Parameters:
  Input:  MensSignElement mse - initial element for new sign
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addMensurationSign(MensSignElement mse)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Cursor.hideCursor();
    LinkedList<MensSignElement> signs=new LinkedList<MensSignElement>();
    signs.add(mse);
    MensEvent me=new MensEvent(signs,4);

    eventnum=insertEvent(snum,vnum,eventnum,me);
  }

  public void addMensurationSign()
  {
    addMensurationSign(new MensSignElement(MensSignElement.MENS_SIGN_C,false,false));
  }

  public void doMensurationAction(int signType)
  {
    if (Cursor.getHighlightBegin()==-1)
      addMensurationSign(new MensSignElement(signType,false,false));
    else if (Cursor.oneItemHighlighted())
      setMensurationSign(signType);
  }

/*------------------------------------------------------------------------
Method:  void setMensurationSign(int newSign)
Purpose: Change attributes of currently highlighted mensuration - main sign
Parameters:
  Input:  int newSign - new main sign type
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setMensurationSign(int newSign)
  {
    int             snum=Cursor.getSectionNum(),
                    vnum=Cursor.getVoiceNum(),
                    eventnum=Cursor.getEventNum();

    Event orige=getCurEvent().getEvent();
    if (orige.geteventtype()!=Event.EVENT_MENS)
      return;

    MensEvent       origme=(MensEvent)orige;
    MensSignElement mse=origme.getMainSign();
    if (newSign==mse.signType)
      return;

    MensEvent me=(MensEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=me.getListPlace(!inVariantVersion())-origme.getListPlace(!inVariantVersion());
    mse=me.getMainSign();

    mse.signType=newSign;
    switch (newSign)
      {
        case MensSignElement.MENS_SIGN_O:
          me.getMensInfo().tempus=Mensuration.MENS_TERNARY;
          break;
        case MensSignElement.MENS_SIGN_C:
        case MensSignElement.MENS_SIGN_CREV:
          me.getMensInfo().tempus=Mensuration.MENS_BINARY;
          break;
      }

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void addMensurationElementSign(int newSign)
Purpose: Change attributes of currently highlighted mensuration - add new sign
Parameters:
  Input:  int newSign - new sign type
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addMensurationElementSign(int newSign)
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    MensEvent me=(MensEvent)e;

    me.addSignElement(new MensSignElement(newSign,false,false));
    if (me.getSigns().size()==1)
      switch (newSign)
        {
          case MensSignElement.MENS_SIGN_O:
            me.getMensInfo().tempus=Mensuration.MENS_TERNARY;
            break;
          case MensSignElement.MENS_SIGN_C:
          case MensSignElement.MENS_SIGN_CREV:
            me.getMensInfo().tempus=Mensuration.MENS_BINARY;
            break;
        }

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void addMensurationElementNumber(int num1,int num2)
Purpose: Change attributes of currently highlighted mensuration - add new number(s)
Parameters:
  Input:  int num1,num2 - number pair to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addMensurationElementNumber(int num1,int num2)
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();

    Event orige=getCurEvent().getEvent();
    if (orige.geteventtype()!=Event.EVENT_MENS)
      return;

    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    MensEvent me=(MensEvent)e;

    me.addSignElement(new MensSignElement(MensSignElement.NUMBERS,new Proportion(num1,num2)));

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  public void addMensurationElementNumber(int num)
  {
    addMensurationElementNumber(num,0);
  }

  public void doMensurationNumberAction(int num)
  {
    if (Cursor.oneItemHighlighted())
      addMensurationElementNumber(num);
    else if (Cursor.getHighlightBegin()==-1)
      switch (num)
        {
          case 2:
            addProportion(2,3);
            break;
          case 3:
            addProportion(3,2);
            break;
        }
  }

/*------------------------------------------------------------------------
Method:  void deleteMensurationElement(int elNum)
Purpose: Change attributes of currently highlighted mensuration - delete one
         element
Parameters:
  Input:  int elNum - index of element to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void deleteMensurationElement(int elNum)
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    MensEvent origme=(MensEvent)getCurEvent().getEvent();
    if (origme.getSigns().size()<2)
      return;

    MensEvent me=(MensEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=me.getListPlace(!inVariantVersion())-origme.getListPlace(!inVariantVersion());

    me.deleteSignElement(elNum);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleMensurationStroke()
Purpose: Change attributes of currently highlighted mensuration - toggle
         presence of stroke
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleMensurationStroke()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent();

    if (orige.geteventtype()!=Event.EVENT_MENS)
      return;

    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    MensEvent me=(MensEvent)e;
    MensSignElement mse=me.getMainSign();

    mse.stroke=!mse.stroke;

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleMensurationDot()
Purpose: Change attributes of currently highlighted mensuration - toggle
         presence of dot
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void toggleMensurationDot()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent();

    if (orige.geteventtype()!=Event.EVENT_MENS)
      return;

    Event e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    MensEvent me=(MensEvent)e;
    MensSignElement mse=me.getMainSign();

    mse.dotted=!mse.dotted;
    me.getMensInfo().prolatio=mse.dotted ? Mensuration.MENS_TERNARY : Mensuration.MENS_BINARY;

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleMensurationNoScoreSig()
Purpose: Change attributes of currently highlighted mensuration - toggle
         whether event affects scoring/measure type
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleMensurationNoScoreSig()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    MensEvent me=(MensEvent)e;
    me.toggleNoScoreSig();

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleMensurationSize()
Purpose: Change attributes of currently highlighted mensuration - toggle
         size
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleMensurationSize()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    MensEvent me=(MensEvent)e;
    me.toggleSize();

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void toggleMensurationVertical()
Purpose: Change attributes of currently highlighted mensuration - toggle
         vertical arrangement
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void toggleMensurationVertical()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    MensEvent me=(MensEvent)e;
    me.toggleVertical();

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void setEventMensuration(Mensuration m)
Purpose: Modify mensuration scheme of a MensEvent
Parameters:
  Input:  Mensuration m - new mensuration scheme
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setEventMensuration(Mensuration m)
  {
    int       snum=Cursor.getSectionNum(),
              vnum=Cursor.getVoiceNum(),
              eventnum=Cursor.getEventNum();
    MensEvent origme=(MensEvent)getCurEvent().getEvent();
    if (origme.getMensInfo().equals(m))
      return;

    MensEvent me=(MensEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=me.getListPlace(!inVariantVersion())-origme.getListPlace(!inVariantVersion());

    me.setMensInfo(m);
    curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void addOriginalText(String text)
Purpose: Insert OriginalText event at current cursor location
Parameters:
  Input:  String text - text phrase to add
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addOriginalText(String text)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Cursor.hideCursor();
    OriginalTextEvent oe=new OriginalTextEvent(text);

    eventnum=insertEvent(snum,vnum,eventnum,oe);
  }

  /* add currently highlighted phrase */
  public void addOriginalText()
  {
    parentEditorWin.textEditorFrame.insertOriginalTextPhrase();
  }

/*------------------------------------------------------------------------
Method:  void addProportion()
Purpose: Insert proportion at current cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addProportion(int i1,int i2)
  {
    int        snum=Cursor.getSectionNum(),
               vnum=Cursor.getVoiceNum(),
               eventnum=Cursor.getEventNum();

    Cursor.hideCursor();
    ProportionEvent pe=new ProportionEvent(i1,i2);

    eventnum=insertEvent(snum,vnum,eventnum,pe);
  }

  public void addProportion()
  {
    int        snum=Cursor.getSectionNum(),
               vnum=Cursor.getVoiceNum(),
               eventnum=Cursor.getEventNum();
    Proportion curProp=getCurProportion(snum,vnum,eventnum);
    if (curProp==null || curProp.equals(Proportion.EQUALITY))
      curProp=new Proportion(2,3);

    addProportion(curProp.i2,curProp.i1); /* default: cancel current proportion */
  }

/*------------------------------------------------------------------------
Method:  void addColorChange()
Purpose: Insert coloration change at current cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addColorChange()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Cursor.hideCursor();
    ColorChangeEvent ce=new ColorChangeEvent(new Coloration(getCurColoration(snum,vnum,eventnum)));

    eventnum=insertEvent(snum,vnum,eventnum,ce);
  }

/*------------------------------------------------------------------------
Method:  void setEventColoration(Coloration c)
Purpose: Modify coloration scheme of a ColorChange event
Parameters:
  Input:  Coloration c - new coloration for event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setEventColoration(Coloration c)
  {
    int              snum=Cursor.getSectionNum(),
                     vnum=Cursor.getVoiceNum(),
                     eventnum=Cursor.getEventNum();
    ColorChangeEvent origcce=(ColorChangeEvent)getCurEvent().getEvent();
    if (c.equals(origcce.getcolorscheme()))
      return;

    ColorChangeEvent cce=(ColorChangeEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=cce.getListPlace(!inVariantVersion())-origcce.getListPlace(!inVariantVersion());

    cce.setcolorscheme(c);
    curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void addLineEnd()
Purpose: Insert line end at current cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addLineEnd()
  {
    int        snum=Cursor.getSectionNum(),
               vnum=Cursor.getVoiceNum(),
               eventnum=Cursor.getEventNum();
    Pitch      custosp;
    Coloration curCol=getCurColoration(snum,vnum,eventnum-1);
//    boolean    editorial=inEditorialSection(snum,vnum,eventnum-1);

    Cursor.hideCursor();
    int eventlistplace=renderedSections[snum].getVoicedataPlace(vnum,eventnum);

    /* custos */
    NoteEvent custpitchnote=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,eventnum,1);
    if (custpitchnote==null)
      custpitchnote=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
    Clef curclef=getCurClef(snum,vnum,eventnum);
    if (custpitchnote!=null)
      custosp=new Pitch(custpitchnote.getPitch());
    else if (curclef!=null)
      custosp=new Pitch(curclef.pitch);
    else
      custosp=null;
    if (custosp!=null)
      {
        CustosEvent custe=new CustosEvent(custosp);
        custe.setcolorparams(curCol);
//        custe.setEditorial(editorial);
/*        musicData.getSection(snum).getVoice(vnum).addEvent(eventlistplace++,custe);
        eventnum++;*/
        eventnum=insertEventWithoutRerender(snum,vnum,eventnum,custe)+1;
        rerender();
      }

    /* line end */
    LineEndEvent lee=new LineEndEvent();
    lee.setcolorparams(curCol);
//    lee.setEditorial(editorial);
/*    musicData.getSection(snum).getVoice(vnum).addEvent(eventlistplace++,lee);
    eventnum++;*/
    eventnum=insertEventWithoutRerender(snum,vnum,eventnum,lee)+1;
    rerender();

    /* clefs: duplicate current clef set */
    if (curclef!=null)
      {
        ClefEvent lastce=null;
        Iterator clefi=getCurClefSet(snum,vnum,Cursor.getEventNum()).iterator();
        while (clefi.hasNext())
          {
            lastce=new ClefEvent(new Clef((Clef)(clefi.next())),lastce,getCurClefEvent(snum,vnum,Cursor.getEventNum()));
            lastce.setcolorparams(curCol);
            lastce.setColored(getCurClefEvent(snum,vnum,eventnum-1).isColored());
//            lastce.setEditorial(editorial);
/*            musicData.getSection(snum).getVoice(vnum).addEvent(eventlistplace++,lastce);
            eventnum++;*/
            eventnum=insertEventWithoutRerender(snum,vnum,eventnum,lastce)+1;
            rerender();
          }
      }

    curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();
    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,vnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void togglePageEnd()
Purpose: Toggle page-end attribute of currently highlighted LineEnd
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void togglePageEnd()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    LineEndEvent le=(LineEndEvent)e;
    le.setPageEnd(!le.isPageEnd());

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void addCustos()
Purpose: Insert custos at current cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addCustos()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Pitch custosp;

    Cursor.hideCursor();
    NoteEvent custpitchnote=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,eventnum,1);
    if (custpitchnote==null)
      custpitchnote=(NoteEvent)getNeighboringEventOfType(Event.EVENT_NOTE,snum,vnum,eventnum-1,-1);
    Clef curclef=getCurClef(snum,vnum,eventnum);
    if (custpitchnote!=null)
      custosp=new Pitch(custpitchnote.getPitch());
    else if (curclef!=null)
      custosp=new Pitch(curclef.pitch);
    else
      custosp=new Pitch(4);

    eventnum=insertEvent(snum,vnum,eventnum,new CustosEvent(custosp));
  }

/*------------------------------------------------------------------------
Method:  void addBarline()
Purpose: Insert barline at current cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addBarline()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Cursor.hideCursor();
    BarlineEvent be=new BarlineEvent();

    eventnum=insertEvent(snum,vnum,eventnum,be);
  }

/*------------------------------------------------------------------------
Method:  void changeNumBarlines(int nl)
Purpose: Modify number of lines in barline event
Parameters:
  Input:  int nl - new number of lines for event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void changeBarlineLength(int offset)
  {
    int          snum=Cursor.getSectionNum(),
                 vnum=Cursor.getVoiceNum(),
                 eventnum=Cursor.getEventNum();
    BarlineEvent origbe=(BarlineEvent)getCurEvent().getEvent();
    int          newNumSpaces=origbe.getNumSpaces()+offset;
    if (newNumSpaces<1)
      return;

    BarlineEvent be=(BarlineEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=be.getListPlace(!inVariantVersion())-origbe.getListPlace(!inVariantVersion());

    be.setNumSpaces(newNumSpaces);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  public void changeNumBarlines(int nl)
  {
    int          snum=Cursor.getSectionNum(),
                 vnum=Cursor.getVoiceNum(),
                 eventnum=Cursor.getEventNum();
    BarlineEvent origbe=(BarlineEvent)getCurEvent().getEvent();
    if (nl==origbe.getNumLines())
      return;

    BarlineEvent be=(BarlineEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=be.getListPlace(!inVariantVersion())-origbe.getListPlace(!inVariantVersion());

    be.setNumLines(nl);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  public void moveBarline(int offset)
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    BarlineEvent be=(BarlineEvent)e;
    be.setBottomLinePos(be.getBottomLinePos()+offset);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

  public void toggleRepeatSign()
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    BarlineEvent be=(BarlineEvent)e;
    be.setRepeatSign(!be.isRepeatSign());

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void addAnnotationText()
Purpose: Insert text annotation at current cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addAnnotationText()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Cursor.hideCursor();
    AnnotationTextEvent ae=new AnnotationTextEvent("");

    eventnum=insertEvent(snum,vnum,eventnum,ae);
  }

/*------------------------------------------------------------------------
Method:  void setAnnotationText(String s)
Purpose: Set text of highlighted annotation
Parameters:
  Input:  String s - new text
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void setAnnotationText(String s)
  {
    int                 snum=Cursor.getSectionNum(),
                        vnum=Cursor.getVoiceNum(),
                        eventnum=Cursor.getEventNum();
    AnnotationTextEvent origae=(AnnotationTextEvent)getCurEvent().getEvent();
    if (s.equals(origae.gettext()))
      return;

    AnnotationTextEvent ae=(AnnotationTextEvent)getEventForModification(snum,vnum,eventnum);
    eventnum+=ae.getListPlace(!inVariantVersion())-origae.getListPlace(!inVariantVersion());

    ae.settext(s);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void modifyAnnotationLocation(int offset)
Purpose: Change vertical location of currently highlighted annotation
Parameters:
  Input:  int offset - number of places to shift annotation
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void modifyAnnotationLocation(int offset)
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    AnnotationTextEvent ae=(AnnotationTextEvent)e;
    ae.setstaffloc(ae.getstaffloc()+offset);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void addLacuna()
Purpose: Insert lacuna indicator at current cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addLacuna()
  {
    int     snum=Cursor.getSectionNum(),
            vnum=Cursor.getVoiceNum(),
            eventnum=Cursor.getEventNum();

    Cursor.hideCursor();
    LacunaEvent le=new LacunaEvent(
      !inEditorialSection(snum,vnum,eventnum) ? Event.EVENT_LACUNA : Event.EVENT_LACUNA_END);

    eventnum=insertEvent(snum,vnum,eventnum,le);

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);

    /* deprecated 
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum(),
        eventlistplace=renderedSections[snum].getVoicedataPlace(vnum,eventnum);

    Cursor.hideCursor();
    LacunaEvent lce=new LacunaEvent(len);
    musicData.getSection(snum).getVoice(vnum).addEvent(eventlistplace++,lce);
    musicData.getSection(snum).getVoice(vnum).addEvent(eventlistplace,new Event(Event.EVENT_LACUNA_END));

    rerender();
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);*/
  }

/*------------------------------------------------------------------------
Method:  void transformHighlightedIntoLacuna()
Purpose: Create new lacuna event to replace highlighted events
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void transformHighlightedIntoLacuna()
  {
    int        snum=Cursor.getSectionNum(),
               vnum=Cursor.getVoiceNum(),
               eventnum=Cursor.getEventNum();
    Proportion lacunaLen=new Proportion(0,1);

    /* calculate total length of highlighted */
    for (int i=Cursor.getHighlightBegin(); i<=Cursor.getHighlightEnd(); i++)
      lacunaLen.add(renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent().getmusictime());

    /* delete highlighted */
    int newevnum=deleteHighlightedWithoutRender();
    Cursor.setNoHighlight();

    /* replace with lacuna */
    rerender();
    moveCursor(snum,vnum,newevnum);
//    addLacuna(lacunaLen);
    addLacuna();
  }

/*------------------------------------------------------------------------
Method:  void addModernKeySignature()
Purpose: Insert modern key signature at current cursor location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addModernKeySignature()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum();

    Cursor.hideCursor();
    ModernKeySignatureEvent mkse=new ModernKeySignatureEvent(getCurModernKeySig(snum,vnum,eventnum-1));

    eventnum=insertEvent(snum,vnum,eventnum,mkse);
  }

/*------------------------------------------------------------------------
Method:  void modifyModernKeySignature(int dir)
Purpose: Change currently highlighted modern key signature
Parameters:
  Input:  int dir - positive=sharpward, negative=flatward
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void modifyModernKeySignature(int dir)
  {
    int   snum=Cursor.getSectionNum(),
          vnum=Cursor.getVoiceNum(),
          eventnum=Cursor.getEventNum();
    Event orige=getCurEvent().getEvent(),
          e=getEventForModification(snum,vnum,eventnum);
    eventnum+=e.getListPlace(!inVariantVersion())-orige.getListPlace(!inVariantVersion());

    ModernKeySignatureEvent mkse=(ModernKeySignatureEvent)e;
    ModernKeySignature      oldSig=new ModernKeySignature(((ModernKeySignatureEvent)orige).getSigInfo());

    if (dir>0)
      mkse.addSharp();
    else if (dir<0)
      mkse.addFlat();
    else
      return;
    curVersionMusicData.getSection(snum).getVoice(vnum).recalcEventParams();
    updateNoteAccidentals(snum,vnum,eventnum+1,oldSig,mkse.getSigInfo());

    rerender();
    checkVariant(snum,vnum,eventnum);
    repaint();
    parentEditorWin.fileModified();
    hl_anchor=eventnum;
    highlightItems(snum,vnum,eventnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void updateNoteAccidentals(int snum,int vnum,int startei,
                                    ModernKeySignature oldSig,ModernKeySignature newSig)
Purpose: Make modern accidentals attached to notes coherent with a given key
         signature (e.g., remove 'accidental' b-flat on a note if it's already
         in the signature)
Parameters:
  Input:  int snum,vnum             - section/voice number
          int startei               - first event index to check
          ModernKeySignature oldSig - current key signature applied to note
          ModernKeySignature newSig - new signature to apply
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void updateNoteAccidentals(int snum,int vnum,int startei,
                             ModernKeySignature oldSig,ModernKeySignature newSig)
  {
/*    int     i=startei;
    boolean done=startei>=renderedSections[snum].eventinfo[vnum].size() &&
                 snum==renderedSections.length-1;

    while (!done)
      {
        Event e=renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent();
        if (e.geteventtype()==Event.EVENT_MODERNKEYSIGNATURE ||
            e.hasSignatureClef())
          return;
        if (e.geteventtype()==Event.EVENT_NOTE)
          {
            NoteEvent ne=(NoteEvent)e;
            ModernAccidental newAcc=newSig.chooseNoteAccidental(ne,oldSig.calcNotePitchOffset(ne)),
                             oldAcc=ne.getModAcc();
            if (newAcc==null || oldAcc==null || !newAcc.equals(oldAcc))
              ne.setModAcc(newAcc);
          }

        i++;
        if (i>=renderedSections[snum].eventinfo[vnum].size())
          {
            snum++;
            while (snum<renderedSections.length && renderedSections[snum].eventinfo[vnum]==null)
              snum++;  get to next section which includes this voice 
            i=0;
          }
        if (snum>=renderedSections.length)
          done=true;
      }*/
  }

/*------------------------------------------------------------------------
Method:  void addVoice()
Purpose: Add new voice to end of voice list
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addVoice()
  {
    /* copy master voice list */
    numvoices=musicData.getVoiceData().length;
    Voice[] newvoicelist=new Voice[numvoices+1];
    for (int i=0; i<numvoices; i++)
      newvoicelist[i]=musicData.getVoiceData()[i];

    /* add new voice, change voice list pointer, update all sections */
    newvoicelist[numvoices]=new Voice(musicData,numvoices+1,"["+(numvoices+1)+"]",false);
    musicData.setVoiceData(newvoicelist);
    for (int si=0; si<numSections; si++)
      musicData.getSection(si).addVoice(newvoicelist[numvoices]);
    rerender();

    initdrawingparams();
    newsize(screensize.width,screensize.height);
    parentwin.pack();
    parentEditorWin.setEventEditorLocation();
    parentEditorWin.setEditingOptionsLocation();
    parentEditorWin.reinitVoiceTextAreas();
    parentEditorWin.initSectionAttribsFrame();
    parentEditorWin.setSectionAttribsFrameLocation();
    parentEditorWin.fileModified();
  }

/*------------------------------------------------------------------------
Method:    void renderSections()
Overrides: Gfx.ViewCanvas.renderSections
Purpose:   Pre-render all sections for display
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void renderSections()
  {
    super.renderSections();
  }

/*------------------------------------------------------------------------
Method:    void drawEvents(Graphics2D g)
Overrides: Gfx.ViewCanvas.drawEvents
Purpose:   Draw music on staves
Parameters:
  Input:  Graphics2D g - graphical context
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void drawEvents(Graphics2D g)
  {
    super.drawEvents(g);
  }

/*------------------------------------------------------------------------
Method:  void splitMensuralSection()
Purpose: Create section break at current cursor position within mensural
         music section
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void splitMensuralSection()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum(),
        cursorXloc=Cursor.getCanvasXloc();

    Cursor.hideCursor();
    splitMensuralSectionData(snum,vnum,eventnum,cursorXloc);

    Cursor.setNoHighlight();
    rerender();
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum+1,vnum,0);
  }

/*------------------------------------------------------------------------
Method:  void resetMusicData()
Purpose: Rerender and reposition cursor
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void resetMusicData()
  {
    setCurrentVariantVersion(getCurrentVariantVersion());
    rerender();
    repaint();
    moveCursor(0,0,0);
  }

/*------------------------------------------------------------------------
Method:  void splitMensuralSectionData(int snum,int vnum,int eventnum,int cursorXloc)
Purpose: Split data within mensural section at a given location
Parameters:
  Input:  int snum,vnum,eventnum,cursorXloc - location for split
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void splitMensuralSectionData(int snum,int vnum,int eventnum,int cursorXloc)
  {
    MusicMensuralSection curSection=(MusicMensuralSection)musicData.getSection(snum),
                         newSection=new MusicMensuralSection(curSection.getNumVoices(),
                                                             curSection.isEditorial(),
                                                             getCurColoration(snum,vnum,eventnum));
    for (int vi=0; vi<newSection.getNumVoices(); vi++)
      {
        VoiceMensuralData curv=(VoiceMensuralData)curSection.getVoice(vi),
                          newv=null;

        if (curv!=null)
          {
            newv=new VoiceMensuralData(curv.getMetaData(),newSection);
            int curvEventnum=calcEventnum(calcSectionNum(cursorXloc),vi,cursorXloc),
                deletionPoint=renderedSections[snum].getVoicedataPlace(vi,curvEventnum);

            for (int ei=curvEventnum; ei<curv.getNumEvents(); ei++)
              newv.addEvent(curv.getEvent(ei));
//    newvoicelist[numvoices].addevent(new Event(Event.EVENT_SECTIONEND));
            curv.truncateEvents(deletionPoint);
          }

        newSection.setVoice(vi,newv);
      }
    musicData.addSection(snum+1,newSection);
  }

/*------------------------------------------------------------------------
Method:  void insertSection(int newSectionType)
Purpose: Insert new section at current cursor position
Parameters:
  Input:  int newSectionType - type of section to insert
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void insertSection(int newSectionType)
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=Cursor.getEventNum(),
        cursorXloc=Cursor.getCanvasXloc(),
        newsnum=snum;

    Cursor.hideCursor();

    if (eventnum>0)
      {
        splitMensuralSectionData(snum,vnum,eventnum,cursorXloc);
        newsnum++;
      }
    MusicSection curSection=musicData.getSection(snum),
                 newSection=null;
    switch (newSectionType)
      {
        case MusicSection.MENSURAL_MUSIC:
          MusicMensuralSection newMensSection=new MusicMensuralSection(curSection.getNumVoices(),
                                                                       curSection.isEditorial(),
                                                                       curSection.getBaseColoration());
          for (int vi=0; vi<newMensSection.getNumVoices(); vi++)
            {
              VoiceMensuralData newv=new VoiceMensuralData(curSection.getVoiceMetaData(vi),newMensSection);
              newv.addEvent(new Event(Event.EVENT_SECTIONEND));
              newMensSection.setVoice(vi,newv);
            }
          newSection=newMensSection;
          break;
        case MusicSection.PLAINCHANT:
          MusicChantSection newChantSection=new MusicChantSection(curSection.getNumVoices(),
                                                                  curSection.isEditorial(),
                                                                  Coloration.DEFAULT_CHANT_COLORATION);
          for (int vi=0; vi<1; vi++)
            {
              VoiceChantData newv=new VoiceChantData(curSection.getVoiceMetaData(vi),newChantSection);
              newv.addEvent(new Event(Event.EVENT_SECTIONEND));
              newChantSection.setVoice(vi,newv);
            }
          newSection=newChantSection;
          break;
        case MusicSection.TEXT:
System.out.println("Insert text section (not implemented)");
          break;
      }
    musicData.addSection(newsnum,newSection);

    Cursor.setNoHighlight();
    rerender();
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum+1,vnum,0);
  }

/*------------------------------------------------------------------------
Method:  void deleteSection(int snum)
Purpose: Delete one section
Parameters:
  Input:  int snum - number of section to delete
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void deleteSection(int snum)
  {
    Cursor.hideCursor();

    musicData.deleteSection(snum);
    if (snum>=musicData.getSections().size())
      snum--;
    parentEditorWin.updateSectionGUI(snum);

    Cursor.setNoHighlight();
    rerender();
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,0,0);
  }

/*------------------------------------------------------------------------
Method:  void combineSectionWithNext()
Purpose: Combine section currently holding cursor with following section
         Validity checks must be performed elsewhere before calling this
         function
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void combineSectionWithNext()
  {
    int snum=Cursor.getSectionNum(),
        vnum=Cursor.getVoiceNum(),
        eventnum=renderedSections[snum].eventinfo[vnum].size()-1;

    Cursor.hideCursor();

    MusicSection curSection=musicData.getSection(snum),
                 nextSection=musicData.getSection(snum+1);
    for (int vi=0; vi<numvoices; vi++)
      {
        VoiceEventListData v1=curSection.getVoice(vi),
                           v2=nextSection.getVoice(vi);
        if (v1==null)
          {
            if (v2!=null)
              curSection.setVoice(vi,v2);
          }
        else
          if (v2!=null)
            {
              v1.deleteEvent(v1.getNumEvents()-1); /* remove SectionEnd */
              for (int ei=0; ei<v2.getNumEvents(); ei++)
                v1.addEvent(v2.getEvent(ei));
            }
      }
    musicData.deleteSection(snum+1);

    Cursor.setNoHighlight();
    rerender();
    repaint();
    parentEditorWin.fileModified();
    moveCursor(snum,vnum,eventnum);
  }

/*------------------------------------------------------------------------
Method:  void set[Voice|Event]num(int i)
Purpose: Change cursor settings without immediate display/recalculation
Parameters:
  Input:  int i - new voice|event number
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setVoicenum(int i)
  {
    Cursor.setVoicenum(i);
  }

  public void setEventNum(int i)
  {
    Cursor.setEventNum(i);
  }

  public void setHLAnchor(int i)
  {
    hl_anchor=i;
  }

/*------------------------------------------------------------------------
Method:  int getCur[Section|Voice|Event]Num()
Purpose: Return number of section/voice/event at current cursor position
Parameters:
  Input:  -
  Output: -
  Return: number of current voice
------------------------------------------------------------------------*/

  public int getCurSectionNum()
  {
    return Cursor.getSectionNum();
  }

  public int getCurVoiceNum()
  {
    return Cursor.getVoiceNum();
  }

  public int getCurEventNum()
  {
    return Cursor.getEventNum();
  }

  public int getHLAnchor()
  {
    return hl_anchor;
  }

/*------------------------------------------------------------------------
Method:  boolean oneItemHighlighted()
Purpose: Check whether just one item is highlighted
Parameters:
  Input:  -
  Output: -
  Return: true if only one item is highlighted
------------------------------------------------------------------------*/

  public boolean oneItemHighlighted()
  {
    return Cursor.oneItemHighlighted();
  }

/*------------------------------------------------------------------------
Method:  int getHighlightBegin()
Purpose: Return index of event at start of highlight (-1 for none)
Parameters:
  Input:  -
  Output: -
  Return: index of highlight start
------------------------------------------------------------------------*/

  public int getHighlightBegin()
  {
    return Cursor.getHighlightBegin();
  }

/*------------------------------------------------------------------------
Method:  RenderedEvent getCurEvent(int offset)
Purpose: Return event at or near current cursor position
Parameters:
  Input:  int offset - event number offset from current position
  Output: -
  Return: current RenderedEvent
------------------------------------------------------------------------*/

  public RenderedEvent getCurEvent(int offset)
  {
    RenderedEvent re=renderedSections[Cursor.getSectionNum()].eventinfo[Cursor.getVoiceNum()].getEvent(Cursor.getEventNum()+offset);
    int           mei=Cursor.getMultiEventHLindex();

    if (mei==-1)
      return re;
    else
      return re.getEvent(mei);
  }

  public RenderedEvent getCurEvent()
  {
    return getCurEvent(0);
  }

  public RenderedEvent getEvent(int snum,int vnum,int evnum)
  {
    return renderedSections[snum].eventinfo[vnum].getEvent(evnum);
  }

  /* return primary event (don't look inside multi-events) */
  RenderedEvent getCurMainEvent()
  {
    return renderedSections[Cursor.getSectionNum()].eventinfo[Cursor.getVoiceNum()].getEvent(Cursor.getEventNum());
  }

  MusicSection getCurSection()
  {
    return musicData.getSection(Cursor.getSectionNum());
  }

/*------------------------------------------------------------------------
Method:  RenderedEvent getRenderedEvent(int snum,int vnum,int eventnum)
Purpose: Return event at given position
Parameters:
  Input:  int snum,vnum - section/voice number
          int eventnum  - event index
  Output: -
  Return: RenderedEvent at given position
------------------------------------------------------------------------*/

  RenderedEvent getRenderedEvent(int snum,int vnum,int eventnum)
  {
    return renderedSections[snum].eventinfo[vnum].getEvent(eventnum);
  }

/*------------------------------------------------------------------------
Method:  Clef getCur*(int snum,int vnum,int eventnum)
Purpose: Return rendering parameters valid at specified location
Parameters:
  Input:  int snum,vnum - section/voice number
          int eventnum  - event index
  Output: -
  Return: parameter structures
------------------------------------------------------------------------*/

  Clef getCurClef(int snum,int vnum,int eventnum)
  {
    RenderedClefSet rcs=renderedSections[snum].eventinfo[vnum].getClefEvents(eventnum);
    if (rcs!=null)
      return rcs.getPrincipalClefEvent().getEvent().getClefSet(false).getprincipalclef();
    return null;
  }

  ClefSet getCurClefSet(int snum,int vnum,int eventnum)
  {
    RenderedClefSet rcs=renderedSections[snum].eventinfo[vnum].getClefEvents(eventnum);
    if (rcs!=null)
      return rcs.getLastClefSet(false);
    return null;
  }

  Event getCurClefEvent(int snum,int vnum,int eventnum)
  {
    RenderedClefSet rcs=renderedSections[snum].eventinfo[vnum].getClefEvents(eventnum);
    if (rcs!=null)
      return rcs.getPrincipalClefEvent().getEvent();
    return null;
  }

  Mensuration getCurMensInfo(int snum,int vnum,int eventnum)
  {
    RenderedEvent re=renderedSections[snum].eventinfo[vnum].getMensEvent(eventnum);
    if (re!=null)
      {
        Event me=re.getEvent();
        return me.getMensInfo();
      }
    return Mensuration.DEFAULT_MENSURATION;
  }

  Mensuration getCurMensInfo()
  {
    return getCurMensInfo(Cursor.getSectionNum(),Cursor.getVoiceNum(),Cursor.getEventNum());
  }

  Coloration getCurColoration(int snum,int vnum,int eventnum)
  {
    return renderedSections[snum].eventinfo[vnum].getColoration(eventnum);
  }

  Proportion getCurProportion(int snum,int vnum,int eventnum)
  {
    return renderedSections[snum].eventinfo[vnum].getProportion(eventnum);
  }

  ModernKeySignature getCurModernKeySig(int snum,int vnum,int eventnum)
  {
    if (eventnum<0 && snum>0)
      {
        for (snum--; snum>=0; snum--)
          if (renderedSections[snum].eventinfo[vnum]!=null)
            return renderedSections[snum].eventinfo[vnum].getModernKeySig(renderedSections[snum].eventinfo[vnum].size()-1);
        return ModernKeySignature.DEFAULT_SIG;
      }
    else
      return renderedSections[snum].eventinfo[vnum].getModernKeySig(eventnum);
  }

  boolean inEditorialSection(int snum,int vnum,int eventnum)
  {
    return renderedSections[snum].eventinfo[vnum].inEditorialSection(eventnum);
  }

  int getCurSectionType()
  {
    return musicData.getSection(getCurSectionNum()).getSectionType();
  }

/*------------------------------------------------------------------------
Method:  NoteEvent getNeighboringNoteEvent(int snum,int vnum,int eventnum,int dir)
Purpose: Return last note event before or after specified location
Parameters:
  Input:  int snum,vnum - section/voice number to check
          int eventnum  - event index to start search
          int dir       - direction to search (1=right, -1=left)
  Output: -
  Return: last NoteEvent
------------------------------------------------------------------------*/

/*  NoteEvent getNeighboringNoteEvent(int snum,int vnum,int eventnum,int dir)
  {
    int nenum=getNeighboringNoteEventNum(snum,vnum,eventnum,dir);
    if (nenum==-1)
      return null;
    else
      {
        Event e=renderedSections[snum].eventinfo[vnum].getEvent(nenum).getEvent();
        if (e.geteventtype()==Event.EVENT_MULTIEVENT)
          return ((MultiEvent)e).getLowestNote();
        else
          return (NoteEvent)e;
      }
  }

  public int getNeighboringNoteEventNum(int snum,int vnum,int eventnum,int dir)
  {
    for (int i=eventnum; i>=0 && i<renderedSections[snum].eventinfo[vnum].size(); i+=dir)
      {
        Event e=renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent();
        if (e.hasEventType(Event.EVENT_NOTE))
          return i;
      }
    return -1;
  }*/

  Event getNeighboringEventOfType(int etype,int snum,int vnum,int eventnum,int dir)
  {
    int nenum=getNeighboringEventNumOfType(etype,snum,vnum,eventnum,dir);
    if (nenum==-1)
      return null;
    else
      {
        Event e=renderedSections[snum].eventinfo[vnum].getEvent(nenum).getEvent();
        if (e.geteventtype()==Event.EVENT_MULTIEVENT)
          switch (etype)
            {
              case Event.EVENT_NOTE:
                return ((MultiEvent)e).getLowestNote();
              default:
                return ((MultiEvent)e).getFirstEventOfType(etype);
            }
        else
          return e;
      }
  }

  public int getNeighboringEventNumOfType(int etype,int snum,int vnum,int eventnum,int dir)
  {
    for (int i=eventnum; i>=0 && i<renderedSections[snum].eventinfo[vnum].size(); i+=dir)
      {
        Event e=renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent();
        if (e.hasEventType(etype))
          return i;
      }
    return -1;
  }

  public int getNumRenderedEvents(int snum,int vnum)
  {
    return renderedSections[snum].eventinfo[vnum].size();
  }

/*------------------------------------------------------------------------
Method:  int FirstEventNumAfterClefSet(int snum,int vnum,int startei,ClefSet cs)
Purpose: Return first event number after clef set at a given location
Parameters:
  Input:  int snum,vnum - voice number to check
          int eventnum  - event index to start search
          ClefSet cs    - clef set to check
  Output: -
  Return: event number
------------------------------------------------------------------------*/

  int firstEventNumAfterClefSet(int snum,int vnum,int startei,ClefSet cs)
  {
    for (int i=startei; i<renderedSections[snum].eventinfo[vnum].size(); i++)
      if (renderedSections[snum].eventinfo[vnum].getEvent(i).getEvent().getClefSet()!=cs)
        return i;
    return -1;
  }

/*------------------------------------------------------------------------
Method:  void set*()
Purpose: Set global editing/input parameterts
Parameters:
  Input:  new editing parameter values
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setColorationOn(boolean newState)
  {
    colorationOn=newState;
  }

  /* tmp */
  void cycleEditorStemDir()
  {
    switch (editorStemDir)
      {
        case NoteEvent.STEM_UP:
          editorStemDir=NoteEvent.STEM_DOWN;
          break;
        case NoteEvent.STEM_DOWN:
          editorStemDir=-1;
          break;
        default:
          editorStemDir=NoteEvent.STEM_UP;
          break;
      }
  }

  public void setEditorColorationType(int colType)
  {
    editorColorationType=colType;
  }

  public void moveEventVertical(int eventOffset, int dir, boolean largeShift)
  {
    RenderedEvent re = getCurEvent(eventOffset);
    if (re == null) {
      return;
    }
    Event e = re.getEvent();

    if (e.getPitch()!=null)
      modifyEventPitch(eventOffset, largeShift ? 7 * dir : dir);
    else if (e.geteventtype()==Event.EVENT_BARLINE)
      if (largeShift)
        changeBarlineLength(dir);
      else
        moveBarline(dir);
    else if (e.geteventtype()==Event.EVENT_CLEF)
      modifyClefLocation(dir);
    else
      modifyHighlightedEventLocations(dir);
  }

/*------------------------------------------------------------------------
Method:     void keyPressed(KeyEvent e)
Implements: KeyListener.keyPressed
Purpose:    Act on key press
Parameters:
  Input:  KeyEvent e - event representing keypress
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void keyPressed(KeyEvent e)
  {
    try
    {
    if (e.isControlDown())
      {
        switch (e.getKeyCode())
          {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
              moveEventVertical(-1, 1, e.isShiftDown());
              break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
              moveEventVertical(-1, -1, e.isShiftDown());
              break;

            case KeyEvent.VK_A:
              highlightAll();
              break;
            case KeyEvent.VK_D:
              if (Cursor.getHighlightBegin()!=-1)
                toggleHighlightedEditorial();
              break;
            case KeyEvent.VK_I:
              addVoice();
              break;
            case KeyEvent.VK_L:
              if (Cursor.getHighlightBegin()==-1)
                addLacuna();
              else
                transformHighlightedIntoLacuna();
              break;

            case KeyEvent.VK_BACK_SLASH:
                System.out.println("CurV="+getCurVoiceNum());
                int curenum=getCurEvent().getEvent().getListPlace(false);
                for (int i=0; i<curenum; i++)
                  curVersionMusicData.getSection(getCurSectionNum()).getVoice(getCurVoiceNum()).getEvent(i).prettyprint();
                System.out.println("DONE LIST");
                break;
            /* TMP: show analysis */
            case KeyEvent.VK_SLASH:
              Util.Analyzer a=new Util.Analyzer(musicData,renderedSections);
              a.printGeneralAnalysis(System.out);

              if (parentwin.windowFileName.equals("Untitled score"))
                break;
              java.io.PrintStream outp=null;
              try
                {
                  outp=new java.io.PrintStream("data/stats/"+parentwin.windowFileName+".txt"); //System.out;
                }
              catch (Exception exc)
                {
                  System.err.println("Error opening file: "+exc);
                  exc.printStackTrace();
                }
              System.out.println();
              System.out.print("Writing analysis to file...");
              a.printGeneralAnalysis(outp);
              System.out.println("done");
              outp.close();
              break;
            case KeyEvent.VK_SEMICOLON:
              int snum=Cursor.getSectionNum(),
                  vnum=Cursor.getVoiceNum(),
                  eventnum=Cursor.getEventNum();
              RenderedEvent re=renderedSections[snum].eventinfo[vnum].getEvent(eventnum);
              if (re!=null &&
                  re.getFullSonority()!=null)
                System.out.println(re.getFullSonority());
              break;
            case KeyEvent.VK_PERIOD:
              showCurrentVariants();
              break;
            case KeyEvent.VK_COMMA:
              toggleVariantError();
              break;
            case KeyEvent.VK_9:
              if (Cursor.oneItemHighlighted())
                modifyEventTime(new Proportion(-1,1));
              break;
            case KeyEvent.VK_0:
              if (Cursor.oneItemHighlighted())
                modifyEventTime(new Proportion(1,1));
              break;
            case KeyEvent.VK_DELETE:
              if (e.isShiftDown())
                deleteAllVariantReadings();
              break;
            default:
              RenderedEvent prevre = getCurEvent(-1);
              if (prevre == null) {
                break;
              }
              Event preve = prevre.getEvent();
              char keych=Character.toUpperCase(e.getKeyChar());
              if (preve.geteventtype()==Event.EVENT_NOTE ||
                  preve.geteventtype()==Event.EVENT_REST)
                {
                  if (keych>='1' && keych<='8')
                    {
                      parentEditorWin.selectNVButton(keych-'1');
                      modifyNoteType(-1, parentEditorWin.getSelectedNoteVal());
                    }
                  else if (preve.getLength()!=null)
                    if (e.getKeyCode()==KeyEvent.VK_1 && e.isShiftDown())
                      imperfectNote(-1);
                    else if (e.getKeyCode()==KeyEvent.VK_2 && e.isShiftDown())
                      alterNote(-1);
                    else if (e.getKeyCode()==KeyEvent.VK_3 && e.isShiftDown())
                      perfectNote(-1);
                }
          }

       /* move between events within a multi-event */
        if (Cursor.oneItemHighlighted())
          if (getCurMainEvent().getEvent().geteventtype()==Event.EVENT_MULTIEVENT)
            switch (e.getKeyCode())
              {
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_KP_RIGHT:
                  shiftHighlightWithinMultiEvent(1);
                  break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_KP_LEFT:
                  shiftHighlightWithinMultiEvent(-1);
                  break;
              }
          else
            {
              char keych=Character.toUpperCase(e.getKeyChar());
              /* German keyboard */
              if (keych==']')
                shiftSignumVertical(1);
              else if (keych=='[')
                shiftSignumVertical(-1);
            }
      } // isControlDown

    /* ------------------------ ONE ITEM HIGHLIGHTED ------------------------ */
    else if (Cursor.oneItemHighlighted())
      {
        Event he=getCurEvent().getEvent(); /* highlighted event */

        switch (e.getKeyCode())
          {
            /* cursor movement */
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
              if (e.isShiftDown())
                modifyHighlight(-1);
              else
                shiftCursorLoc(-1,0);
              break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
            case KeyEvent.VK_ENTER:
              if (e.isShiftDown())
                modifyHighlight(1);
              else
                shiftCursorLoc(1,0);
              break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
              moveEventVertical(0, 1, e.isShiftDown());
              break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
              moveEventVertical(0, -1, e.isShiftDown());
              break;
            case KeyEvent.VK_END:
              moveCursorToEnd();
              break;
            case KeyEvent.VK_HOME:
              moveCursorToHome();
              break;

            /* delete event */
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE:
              deleteHighlightedItems();
              break;

            /* modify event */
            case KeyEvent.VK_I:
              if (he.geteventtype()==Event.EVENT_NOTE)
                if (e.isShiftDown())
                  cycleNoteHalfColoration();
                else
                  toggleNoteColoration();
              else if (he.geteventtype()==Event.EVENT_CLEF ||
                       he.geteventtype()==Event.EVENT_REST)
                toggleEventColoration();
              break;
            case KeyEvent.VK_L:
              if (he.geteventtype()==Event.EVENT_NOTE)
                ligateNoteToLast();
              break;
            case KeyEvent.VK_O:
              if (he.geteventtype()==Event.EVENT_NOTE)
                changeLigType(NoteEvent.LIG_OBLIQUA);
              else if (he.geteventtype()==Event.EVENT_MENS)
                setMensurationSign(MensSignElement.MENS_SIGN_O);
              break;
            case KeyEvent.VK_P:
              if (he.geteventtype()==Event.EVENT_NOTE)
                toggleNoteAccidentalOptional();
              else if (he.geteventtype()==Event.EVENT_LINEEND)
                togglePageEnd();
              break;
            case KeyEvent.VK_R:
              if (he.geteventtype()==Event.EVENT_BARLINE)
                toggleRepeatSign();
              else if (he.geteventtype()==Event.EVENT_NOTE)
                changeLigType(NoteEvent.LIG_RECTA);
              else if (he.geteventtype()==Event.EVENT_MENS)
                setMensurationSign(MensSignElement.MENS_SIGN_CREV);
              break;
            case KeyEvent.VK_S:
              if (he.geteventtype()==Event.EVENT_CLEF)
                toggleClefSignatureStatus();
              else if (he.geteventtype()==Event.EVENT_MENS)
                toggleMensurationSize();
              else if (he.geteventtype()==Event.EVENT_NOTE ||
                       he.geteventtype()==Event.EVENT_REST)
                toggleSignum();
              break;
            case KeyEvent.VK_T:
              if (he.geteventtype()==Event.EVENT_NOTE)
                cycleTieType();
              break;
            case KeyEvent.VK_U:
              if (he.geteventtype()==Event.EVENT_NOTE ||
                  he.geteventtype()==Event.EVENT_REST)
                toggleSignumOrientation();
              break;
            case KeyEvent.VK_V:
              if (he.geteventtype()==Event.EVENT_MENS)
                toggleMensurationVertical();
              else
                combineVariantReadings();
              break;
            case KeyEvent.VK_CLOSE_BRACKET:
              shiftSignumVertical(1);
              break;
            case KeyEvent.VK_OPEN_BRACKET:
              shiftSignumVertical(-1);
              break;
            case KeyEvent.VK_PERIOD:
              if (he.geteventtype()==Event.EVENT_NOTE)
                addDot(DotEvent.DT_Addition);
              else if (he.geteventtype()==Event.EVENT_MENS)
                toggleMensurationDot();
              break;
            case KeyEvent.VK_SEMICOLON:
              if (he.geteventtype()==Event.EVENT_NOTE ||
                  he.geteventtype()==Event.EVENT_REST)
                cycleSignumSide();

              /* French keyboards */
              if (e.isShiftDown())
                if (he.geteventtype()==Event.EVENT_NOTE)
                  addDot(Cursor.getSectionNum(),Cursor.getVoiceNum(),Cursor.getHighlightBegin()+1,DotEvent.DT_Addition);
                else if (he.geteventtype()==Event.EVENT_MENS)
                  toggleMensurationDot();                
              break;
            case KeyEvent.VK_SLASH:
            case KeyEvent.VK_COLON:
            case KeyEvent.VK_NUMBER_SIGN: /* German */
              if (he.geteventtype()==Event.EVENT_MENS)
                toggleMensurationStroke();
              else if (he.geteventtype()==Event.EVENT_NOTE)
                cycleHighlightedStemDirections();
              break;
            case KeyEvent.VK_PLUS:
            case KeyEvent.VK_EQUALS:
              if (he.geteventtype()==Event.EVENT_NOTE)
                changeNoteAccidental(1);
              else if (he.geteventtype()==Event.EVENT_MODERNKEYSIGNATURE)
                modifyModernKeySignature(1);
              break;
            case KeyEvent.VK_MINUS:
              if (he.geteventtype()==Event.EVENT_NOTE)
                changeNoteAccidental(-1);
              else if (he.geteventtype()==Event.EVENT_MODERNKEYSIGNATURE)
                modifyModernKeySignature(-1);
              break;
            default:
              char keych=Character.toUpperCase(e.getKeyChar());

              if (he.getPitch()!=null && he.geteventtype()!=Event.EVENT_CLEF)
                {
                  if (keych>='A' && keych<='G')
                    modifyEventPitch(keych);
                }
              else if (he.geteventtype()==Event.EVENT_CLEF)
                {
                  if (keych=='C')
                    doClefAction(Clef.CLEF_C);
                  if (keych=='F')
                    doClefAction(Clef.CLEF_F);
                  if (keych=='G')
                    doClefAction(Clef.CLEF_G);
                  if (keych=='B')
                    doClefAction(Clef.CLEF_Bmol);
                  if (keych=='H')
                    doClefAction(Clef.CLEF_Bqua);
                  if (keych=='X')
                    doClefAction(Clef.CLEF_Diesis);
                }
              else if (he.geteventtype()==Event.EVENT_MENS && keych=='C')
                setMensurationSign(MensSignElement.MENS_SIGN_C);

              /* German keyboard */
              else if (keych==']')
                shiftSignumVertical(1);
              else if (keych=='[')
                shiftSignumVertical(-1);

              else if (he.geteventtype()==Event.EVENT_BARLINE)
                if (keych>='1' && keych<='8')
                  changeNumBarlines((int)(keych-'1'+1));
              if (keych=='0' || keych=='M')
                makeMultiEvent();
              if (he.geteventtype()==Event.EVENT_NOTE ||
                  he.geteventtype()==Event.EVENT_REST)
                {
                  if (keych>='1' && keych<='8')
                    {
                      parentEditorWin.selectNVButton(keych-'1');
                      modifyNoteType(parentEditorWin.getSelectedNoteVal());
                    }
                  else if (keych=='*')
                    toggleCorona();
                  else if (keych=='<')
                    toggleHighlightedEditorialText();
                  else if (he.getLength()!=null)
                    if (e.getKeyCode()==KeyEvent.VK_1 && e.isShiftDown())
                      imperfectNote();
                    else if (e.getKeyCode()==KeyEvent.VK_2 && e.isShiftDown())
                      alterNote();
                    else if (e.getKeyCode()==KeyEvent.VK_3 && e.isShiftDown())
                      perfectNote();
                }
          }
      }

    /* --------------------- MULTIPLE ITEMS HIGHLIGHTED --------------------- */
    else if (Cursor.getHighlightBegin()!=-1) /* multiple items highlighted */
      {
        Event he=getCurEvent().getEvent(); /* first highlighted event */

        switch (e.getKeyCode())
          {
            case KeyEvent.VK_I:
              toggleHighlightedColoration();
              break;
            case KeyEvent.VK_L:
              ligateHighlighted();
              break;
            case KeyEvent.VK_O:
              if (he.geteventtype()==Event.EVENT_NOTE)
                changeLigType(NoteEvent.LIG_OBLIQUA);
              break;
            case KeyEvent.VK_R:
              if (he.geteventtype()==Event.EVENT_NOTE)
                changeLigType(NoteEvent.LIG_RECTA);
              break;
            case KeyEvent.VK_V:
              combineVariantReadings();
              break;
            case KeyEvent.VK_SLASH:
            case KeyEvent.VK_COLON:
            case KeyEvent.VK_NUMBER_SIGN: /* German */
              cycleHighlightedStemDirections();
              break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_KP_UP:
              modifyHighlightedEventLocations(1);
              break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_KP_DOWN:
              modifyHighlightedEventLocations(-1);
              break;

            /* cursor movement */
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
              if (e.isShiftDown())
                modifyHighlight(-1);
              else
                shiftCursorLoc(-1,0);
              break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
              if (e.isShiftDown())
                modifyHighlight(1);
              else
                shiftCursorLoc(1,0);
              break;
            case KeyEvent.VK_END:
              moveCursorToEnd();
              break;
            case KeyEvent.VK_HOME:
              moveCursorToHome();
              break;

            /* delete event */
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE:
              deleteHighlightedItems();
              break;

            default:
              char keych=Character.toUpperCase(e.getKeyChar());
              if (keych=='<')
                toggleHighlightedEditorialText();
          }
      }

    /* ------------------------ NO ITEMS HIGHLIGHTED ------------------------ */
    else
      switch (e.getKeyCode())
        {
          case KeyEvent.VK_BACK_SLASH:
            if (e.isShiftDown())
              {
                cycleEditorStemDir();
                break;
              }

            /* debugging */
            if (getCurEvent(-1)!=null)
              {
                System.out.println("Eventloc "+getCurEvent(-1).getEvent().getListPlace(false)+
                                   ", DefaultEventloc "+getCurEvent(-1).getEvent().getDefaultListPlace());
                getCurEvent(-1).prettyprint();
              }
            System.out.println("Eventloc "+getCurEvent().getEvent().getListPlace(false)+
                               ", DefaultEventloc "+getCurEvent().getEvent().getDefaultListPlace());
            getCurEvent().prettyprint();
            break;

          /* cursor movement */
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_KP_LEFT:
            if (e.isShiftDown())
              modifyHighlight(-1);
            else
              shiftCursorLoc(-1,0);
            break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_KP_RIGHT:
            if (e.isShiftDown())
              modifyHighlight(1);
            else
              shiftCursorLoc(1,0);
            break;
          case KeyEvent.VK_UP:
          case KeyEvent.VK_KP_UP:
            shiftCursorLoc(0,-1);
            break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_KP_DOWN:
            shiftCursorLoc(0,1);
            break;
          case KeyEvent.VK_END:
            moveCursorToEnd();
            break;
          case KeyEvent.VK_HOME:
            moveCursorToHome();
            break;

          /* delete event */
          case KeyEvent.VK_BACK_SPACE:
            deleteCurItem(-1);
            break;
          case KeyEvent.VK_DELETE:
            if (e.isShiftDown())
              deleteVariantReading();
            else
              deleteCurItem(0);
            break;

          /* add events */
          case KeyEvent.VK_PERIOD:
            addDot(DotEvent.DT_Addition);
            break;
          case KeyEvent.VK_COMMA:
            addDot(DotEvent.DT_Division);
            break;
          case KeyEvent.VK_I:
            if (e.isShiftDown())
              parentEditorWin.toggleEditingOptionsColoration();
            break;
          case KeyEvent.VK_K:
            addModernKeySignature();
            break;
          case KeyEvent.VK_L:
            if (e.isShiftDown()) {
              addColorChange();
            } else {
              ligateNoteToLast(-1);
            }
            break;
          case KeyEvent.VK_M:
            addMensurationSign();
            break;
          case KeyEvent.VK_N:
            addAnnotationText();
            break;
          case KeyEvent.VK_P:
            addProportion();
            break;
          case KeyEvent.VK_T:
            addOriginalText();
            break;
          case KeyEvent.VK_V:
            combineVariantReadings();
            break;
          case KeyEvent.VK_X:
            doClefAction();
            break;
          case KeyEvent.VK_Z:
            addRest(parentEditorWin.getSelectedNoteVal());
            break;
          case KeyEvent.VK_SLASH:
            addLineEnd();
            break;
          case KeyEvent.VK_SEMICOLON:
            addCustos();
            break;
          case KeyEvent.VK_CLOSE_BRACKET:
            addBarline();
            break;
/*          case KeyEvent.VK_OPEN_BRACKET:
            addEllipsis();
            break;*/
          case KeyEvent.VK_SPACE:
            addNote(parentEditorWin.getSelectedNoteVal()); /* repeat pitch of last note */
            break;

          case KeyEvent.VK_PLUS:
          case KeyEvent.VK_EQUALS:
            changeNoteAccidental(-1, 1);
            break;
          case KeyEvent.VK_MINUS:
            changeNoteAccidental(-1, -1);
            break;

          /* add note/change attributes */
          default:
            char keych=Character.toUpperCase(e.getKeyChar());
            if (keych>='A' && keych<='G')
              addNote(parentEditorWin.getSelectedNoteVal(),keych);
            else if (keych>='1' && keych<='8')
              parentEditorWin.selectNVButton(keych-'1');
            else if (e.getKeyCode()==KeyEvent.VK_6 && e.isShiftDown())
              parentEditorWin.toggleFlaggedSemiminima();
          break;
      }
    }
    catch (Exception err)
      {
        parentEditorWin.handleRuntimeError(err);
      }
  }

  /* empty KeyListener methods */
  public void keyReleased(KeyEvent e) {}
  public void keyTyped(KeyEvent e) {}

  /* mouse attributes */
  boolean mouseButtonDown=false;

/*------------------------------------------------------------------------
Method:     void mousePressed(MouseEvent e)
Implements: MouseListener.mousePressed
Purpose:    Handle mouse click on canvas
Parameters:
  Input:  MouseEvent e - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  MouseEvent focusingEvent=null;

  public void mousePressed(MouseEvent e)
  {
    if (e.isMetaDown())
      doRightClick(e);
    else
      switch (e.getButton())
        {
          case MouseEvent.BUTTON1:
            doLeftClick(e);
            break;
          case MouseEvent.BUTTON2:
          case MouseEvent.BUTTON3:
            doRightClick(e);
            break;
        }
  }

  public void doLeftClick(MouseEvent e)
  {
    showCursor();
    if (e.isShiftDown())
      {
        /* highlight events */
        int newvnum=Cursor.getVoiceNum(),
            newsnum=calcSectionNum(e.getX()),
            newevnum=calcHLEventnum(newsnum,newvnum,e.getX()),
            cursnum=Cursor.getSectionNum(),
            curevnum=Cursor.getEventNum();
        if (newsnum<cursnum)
          newevnum=0;
        if (newsnum>cursnum)
          newevnum=renderedSections[cursnum].eventinfo[newvnum].size()-1;
        if (newevnum<0)
          newevnum=0;

        if (Cursor.getHighlightBegin()==-1 && newevnum!=curevnum)
          {
            hl_anchor=Cursor.getEventNum();
            highlightItems(cursnum,newvnum,Math.min(curevnum,newevnum),Math.max(curevnum,newevnum)-1);
          }
        else if (Cursor.getHighlightBegin()!=-1)
          if (newsnum==cursnum && newevnum==hl_anchor)
            moveCursor(newsnum,newvnum,newevnum); /* remove highlight */
          else
            highlightItems(cursnum,newvnum,Math.min(hl_anchor,newevnum),Math.max(hl_anchor,newevnum)-1);
      }

    if (!focused)
      {
        focusingEvent=e;
        requestFocusInWindow();
        Cursor.showCursor();
        repaint();
      }

    if (!e.isShiftDown())
      newCursorLoc(e.getX(),e.getY());

    mouseButtonDown=true;
  }

  public void doRightClick(MouseEvent e)
  {
    int x=e.getX(),y=e.getY();

    int newSNum=calcSectionNum(x),
        newVNum=calcVNum(newSNum,y),
        newEventnum=calcEventnum(newSNum,newVNum,x);

    showVariants(newSNum,newVNum,newEventnum,e.getXOnScreen(),e.getYOnScreen());
  }

/*------------------------------------------------------------------------
Method:     void mouseReleased(MouseEvent e)
Implements: MouseListener.mouseReleased
Purpose:    Handle mouse button release
Parameters:
  Input:  MouseEvent e - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void mouseReleased(MouseEvent e)
  {
    mouseButtonDown=false;
  }

/*------------------------------------------------------------------------
Method:     void mouseDragged(MouseEvent e)
Implements: MouseMotionListener.mouseDragged
Purpose:    Handle mouse drag
Parameters:
  Input:  MouseEvent e - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void mouseDragged(MouseEvent e)
  {
    if (!mouseButtonDown)
      return;

    int newvnum=Cursor.getVoiceNum(),
        newsnum=calcSectionNum(e.getX()),
        newevnum=calcHLEventnum(newsnum,newvnum,e.getX()),
        cursnum=Cursor.getSectionNum(),
        curevnum=Cursor.getEventNum();
    if (newsnum<cursnum)
      newevnum=0;
    if (newsnum>cursnum)
      newevnum=renderedSections[cursnum].eventinfo[newvnum].size()-1;
    if (newevnum<0)
      newevnum=0;

    if (Cursor.getHighlightBegin()==-1)
      {
        hl_anchor=Cursor.getEventNum();

        /* create new highlight */
        if (newevnum>Cursor.getEventNum())
          highlightItems(cursnum,newvnum,Cursor.getEventNum(),newevnum-1);
        else if (newevnum<Cursor.getEventNum())
          highlightItems(cursnum,newvnum,newevnum,Cursor.getEventNum()-1);
      }
    else
      {
        /* modify existing highlight */
        if (newevnum==hl_anchor)
          moveCursor(cursnum,newvnum,newevnum); /* remove highlight */
        else if (newevnum<hl_anchor)
          highlightItems(cursnum,newvnum,newevnum,hl_anchor-1);
        else /* newevnum>hl_anchor */
          highlightItems(cursnum,newvnum,hl_anchor,newevnum-1);
      }
  }

  /* empty Mouse*Listener methods */
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}
  public void mouseClicked(MouseEvent e) {}
  public void mouseMoved(MouseEvent e) {}

/*------------------------------------------------------------------------
Methods:    void focus[Gained|Lost](FocusEvent e)
Implements: FocusListener.focus[Gained|Lost]
Purpose:    Take action when keyboard focus is gained or lost
Parameters:
  Input:  FocusEvent e - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void focusGained(FocusEvent e)
  {
    focused=true;
    if (parentwin.rerendermusic)
      rerender();
    if (focusingEvent!=null)
      {
        mousePressed(focusingEvent);
        mouseReleased(null);
        focusingEvent=null;
      }
    Cursor.showCursor();
  }

  public void focusLost(FocusEvent e)
  {
    Cursor.hideCursor();
    focused=false;
  }

/*------------------------------------------------------------------------
Method:  void stopThreads()
Purpose: Stop any timer threads associated with this object
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void stopThreads()
  {
    Cursor_Timer.stop();
  }

/*------------------------------------------------------------------------
Method:    void movedisplay(int newmeasure)
Overrides: ViewCanvas.movedisplay
Purpose:   Move display to a new measure location
Parameters:
  Input:  int newmeasure - leftmost measure of new display location
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void movedisplay(int newmeasure)
  {
    Cursor.hideCursor();
    super.movedisplay(newmeasure);
    Cursor.showCursor();
  }

/*------------------------------------------------------------------------
Method:  void newViewScale()
Overrides: ViewCanvas.newViewScale
Purpose: Update graphics when scale has changed
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void newViewScale()
  {
    Cursor.hideCursor();
    super.newViewScale();
    Cursor.showCursor();
  }

/*------------------------------------------------------------------------
Method:    void newY(int newystart)
Overrides: ViewCanvas.newY
Purpose:   Change y position of viewport
Parameters:
  Input:  int newystart - new value for VIEWYSTART
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void newY(int newystart)
  {
    Cursor.hideCursor();
    super.newY(newystart);
    Cursor.showCursor();
  }

/*------------------------------------------------------------------------
Method:    void drawEndBarline(Graphics2D g,int xloc)
Overrides: Gfx.ViewCanvas.drawEndBarline
Purpose:   Draw score ending (don't break off staves in editor view)
Parameters:
  Input:  Graphics2D g - graphical context
          int xloc   - x location for barline
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void drawEndBarline(Graphics2D g,int xloc)
  {
  }

/*------------------------------------------------------------------------
Method:  void drawHighlightedEvents(Graphics2D g,int snum,int vnum,int firstenum,int lastenum)
Purpose: Paint events in highlighted color
Parameters:
  Input:  Graphics2D g           - graphical context
          int snum,vnum          - section/voice number
          int firstenum,lastenum - indices of first and last events to be highlighted
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void drawHighlightedEvents(Graphics2D g,int snum,int vnum,int firstenum,int lastenum)
  {
    MeasureInfo leftMeasure=getMeasure(curmeasure);

    int           evloc;
    double        xloc,
                  leftMeasureX=getMeasureX(curmeasure);
    RenderedEvent e;
    boolean       done;

    evloc=snum==leftRendererNum ? leftMeasure.reventindex[vnum] : 0;
    if (snum>leftRendererNum || firstenum>evloc)
      evloc=firstenum;
    done=leftRendererNum>snum ||
         evloc>=renderedSections[snum].eventinfo[vnum].size() ||
         evloc>lastenum;

    /* highlight only one event within a multi-event? */
    e=renderedSections[snum].eventinfo[vnum].getEvent(evloc);
    if (!done && e.getEvent().geteventtype()==Event.EVENT_MULTIEVENT && firstenum==lastenum)
      {
        int mei=Cursor.getMultiEventHLindex();
        if (mei!=-1)
          {
            xloc=XLEFT+(renderedSections[snum].getEventXLoc(vnum,evloc)-leftMeasureX)*VIEWSCALE;
            e.getEvent(mei).drawHighlighted(g,MusicGfx,this,(float)xloc,YTOP+vnum*(STAFFSCALE*STAFFSPACING)*VIEWSCALE,VIEWSCALE);
            done=true;
          }
      }

    while (!done)
      {
        e=renderedSections[snum].eventinfo[vnum].getEvent(evloc);
        xloc=XLEFT+(renderedSections[snum].getEventXLoc(vnum,evloc)-leftMeasureX)*VIEWSCALE;
        if (xloc<viewsize.width)
          {
            /* draw event */
            if (xloc>=XLEFT && e.isdisplayed())
              e.drawHighlighted(g,MusicGfx,this,(float)xloc,YTOP+vnum*(STAFFSCALE*STAFFSPACING)*VIEWSCALE,VIEWSCALE);
            evloc++;
            if (evloc>=renderedSections[snum].eventinfo[vnum].size() || evloc>lastenum)
              done=true;
          }
        else
          done=true;
      }
  }

/*------------------------------------------------------------------------
Method:    void unregisterListeners()
Overrides: Gfx.ViewCanvas.unregisterListeners
Purpose:   Remove all action/item/etc listeners when disposing of resources
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void unregisterListeners()
  {
    removeKeyListener(this);
    removeMouseListener(this);
    removeMouseMotionListener(this);
    removeFocusListener(this);
  }
}


/*------------------------------------------------------------------------
Class:   EditorCursor
Extends: -
Purpose: Handles cursor data and thread-safe display
------------------------------------------------------------------------*/

class EditorCursor
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  private ScoreEditorCanvas canvas;

  private int     sectionNum,
                  voiceNum,eventnum, /* voice number, event number */
                  measurenum,
                  score_xloc,canvas_xloc,
                  highlight_begin,highlight_end, /* highlighted event indices:
                                                    -1 for no highlighting */
                  multiEventHLindex;             /* index of single highlighted
                                                    event within a MultiEvent */
  private boolean visible, /* currently showing? */
                  hidden;  /* turned off for redraws etc.? */

  /* number of times canvas has been redrawn (for checking whether cursor
     has been overwritten) */
  private byte lastredisplaynum;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: EditorCursor(ScoreEditorCanvas sec)
Purpose:     Initialize cursor
Parameters:
  Input:  ScoreEditorCanvas sec - canvas on which cursor is displayed
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public EditorCursor(ScoreEditorCanvas sec)
  {
    canvas=sec;
    sectionNum=0;
    voiceNum=canvas.getMusicData().getSection(sectionNum).getValidVoicenum(0);
    eventnum=0;
    measurenum=0;
    score_xloc=canvas_xloc=-1;
    highlight_begin=-1;
    multiEventHLindex=-1;
    visible=false;
    hidden=true; /* cursor display must be turned on explicitly */
    lastredisplaynum=canvas.num_redisplays;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Point getLocation()
  {
    return new Point((int)Math.round(calcCursorX()),(int)Math.round(calcCursorYTop()));
  }

  public int getSectionNum()
  {
    return sectionNum;
  }

  public int getVoiceNum()
  {
    return voiceNum;
  }

  public int getEventNum()
  {
    return eventnum;
  }

  public int getCanvasXloc()
  {
    return canvas_xloc;
  }

  public boolean isHidden()
  {
    return hidden;
  }

  public int getHighlightBegin()
  {
    return highlight_begin;
  }

  public int getHighlightEnd()
  {
    return highlight_end;
  }

  public boolean oneItemHighlighted()
  {
    return highlight_begin!=-1 && highlight_begin==highlight_end;
  }

  public int getMultiEventHLindex()
  {
    return multiEventHLindex;
  }

/*------------------------------------------------------------------------
Methods: set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attribute values
  Output: -
  Return: -
------------------------------------------------------------------------*/

  /* should only be called when cursor is hidden, about to be recalculated */
  public synchronized void setSectionNum(int i)
  {
    sectionNum=i;
  }

  public synchronized void setVoicenum(int i)
  {
    voiceNum=i;
  }

  public synchronized void setEventNum(int i)
  {
    eventnum=i;
  }

  public synchronized void resetMultiEventHLindex()
  {
    multiEventHLindex=-1;
  }

/*------------------------------------------------------------------------
Methods: calc*()
Purpose: Routines to calculate drawing parameters
Parameters:
  Input:  -
  Output: -
  Return: parameters
------------------------------------------------------------------------*/

  double calcCursorX()
  {
    return canvas_xloc+2*canvas.VIEWSCALE;
  }

  double calcCursorYTop(int vn)
  {
    return canvas.YTOP+
           (vn*canvas.STAFFSCALE*canvas.STAFFSPACING-canvas.STAFFSCALE)*canvas.VIEWSCALE;
  }

  double calcCursorYTop()
  {
    return calcCursorYTop(voiceNum);
  }

  double calcCursorYSize()
  {
    return canvas.STAFFSCALE*6*canvas.VIEWSCALE;
  }

/*------------------------------------------------------------------------
Method:  void hideCursor()
Purpose: Disable cursor display
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public synchronized void hideCursor()
  {
    while (visible)
      toggleCursor();
    hidden=true;
  }

/*------------------------------------------------------------------------
Method:  void showCursor()
Purpose: Enable cursor display
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public synchronized void showCursor()
  {
    if (!canvas.focused)
      return;

    hidden=false;
    if (visible)
      return;
    calc_xlocs();
    visible=false;
    toggleCursor();
  }

/*------------------------------------------------------------------------
Method:  void toggleCursor()
Purpose: Draw or erase cursor at current location
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public synchronized void toggleCursor()
  {
    if (hidden || canvas.repaintingbuffer>0 || offScreenX() || highlight_begin!=-1)
      {
        visible=false;
        return;
      }
    if (canvas.num_redisplays!=lastredisplaynum)
      visible=false; /* cursor has been overdrawn by buffer repainting */

    /* now display */
    Graphics2D g=canvas.getbufferg2d();
    g.setColor(Color.black);
    g.setXORMode(Color.white);
    g.fillRect((int)Math.round(calcCursorX()),(int)Math.round(calcCursorYTop()),
               Math.round(2*canvas.VIEWSCALE),(int)Math.round(calcCursorYSize()));
    g.setPaintMode();
    visible=!visible;
    canvas.repaint((int)Math.round(calcCursorX()),(int)Math.round(calcCursorYTop()-canvas.VIEWYSTART),
                   Math.round(2*canvas.VIEWSCALE),(int)Math.round(calcCursorYSize()));
    lastredisplaynum=canvas.num_redisplays;
  }

/*------------------------------------------------------------------------
Method:  void paintHighlight(Graphics2D g[,int diff_begin,int diff_end])
Purpose: Graphically highlight events
Parameters:
  Input:  Graphics2D g            - graphical context for painting highlight
          int diff_begin,diff_end - boundaries of currently painted highlight
                                    (for painting only the difference)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  boolean highlightVisible=false;

  void paintHighlight(Graphics2D g)
  {
    if (highlight_begin==-1)
      return; /* nothing is highlighted */

    /* calculate left and right screen coordinates of highlighted area */
    double leftx=calcevleftx(sectionNum,voiceNum,highlight_begin),
           rightx=calcevrightx(sectionNum,voiceNum,highlight_end),
           ytop=calcCursorYTop(),
           ysize=calcCursorYSize();

    drawHL(g,leftx,rightx,ytop,ysize);
    if (!highlightVisible)
      canvas.drawHighlightedEvents(g,sectionNum,voiceNum,highlight_begin,highlight_end);
    else
      canvas.parentwin.updatemusicgfx=true;

//    canvas.repaint(leftx,ytop,rightx-leftx,ysize);
    canvas.repaint();
    highlightVisible=!highlightVisible;
  }

  void paintHighlight(Graphics2D g,int diff_begin,int diff_end)
  {
    /* calculate left and right screen coordinates of highlighted area */
    double newleftx=calcevleftx(sectionNum,voiceNum,highlight_begin),
           newrightx=calcevrightx(sectionNum,voiceNum,highlight_end),
           diffleftx=calcevleftx(sectionNum,voiceNum,diff_begin),
           diffrightx=calcevrightx(sectionNum,voiceNum,diff_end),
           leftx1=Math.min(newleftx,diffleftx),leftx2=Math.max(newleftx,diffleftx),
           rightx1=Math.min(newrightx,diffrightx),rightx2=Math.max(newrightx,diffrightx),
           ytop=calcCursorYTop(),
           ysize=calcCursorYSize();

    /* draw difference between old and new highlights */
    drawHL(g,leftx1,leftx2,ytop,ysize);
    drawHL(g,rightx1,rightx2,ytop,ysize);
//    canvas.drawHighlightedEvents(g,voiceNum,highlight_begin,highlight_end);
    canvas.parentwin.updatemusicgfx=true;

//    canvas.repaint(leftx1,ytop,rightx2-leftx1,ysize);
    canvas.repaint();
  }

  public void repaintHighlight(Graphics2D g)
  {
    highlightVisible=false;
    paintHighlight(g);
  }

/*------------------------------------------------------------------------
Method:  void drawHL(Graphics2D g,double leftx,double rightx,double ytop,double ysize)
Purpose: Draw one highlight rectangle
Parameters:
  Input:  Graphics2D g        - graphical context for painting highlight
          double leftx,rightx - x bounds
          double ytop,ysize   - y bounds
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawHL(Graphics2D g,double leftx,double rightx,double ytop,double ysize)
  {
    g.setColor(Color.black);
    g.setXORMode(Color.white);
    g.fillRect((int)Math.round(leftx),(int)Math.round(ytop),
               (int)Math.round(rightx-leftx),(int)Math.round(ysize));
    g.setPaintMode();
  }

/*------------------------------------------------------------------------
Method:  void highlightEvents(int sn,int vn,int firstenum,int lastenum)
Purpose: Highlight one or more events
Parameters:
  Input:  int sn,vn              - section/voice number
          int firstenum,lastenum - indices of first and last events to be highlighted
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public synchronized void highlightEvents(int snum,int vnum,int firstenum,int lastenum)
  {
    if (canvas.renderedSections[snum].eventinfo[vnum].size()<=1)
      return; /* no events to highlight */

    if (visible)
      toggleCursor();

    /* save current highlight information */
    int curhl_begin=highlight_begin,
        curhl_end=highlight_end;

    /* change highlight and draw */
    highlight_begin=firstenum;
    highlight_end=lastenum;
    sectionNum=snum;
    voiceNum=vnum;
    eventnum=highlight_begin;
    measurenum=canvas.renderedSections[snum].eventinfo[vnum].getEvent(eventnum).getmeasurenum();
    calc_xlocs();
    hidden=false;

    /* reset multi-event highlight */
    if (curhl_begin!=highlight_begin || curhl_end!=highlight_end)
      multiEventHLindex=-1;

    Graphics2D g=canvas.getbufferg2d();
    if (curhl_begin==-1)
      /* create new highlight */
      paintHighlight(g);
    else
      /* paint difference between old and new highlights */
      paintHighlight(g,curhl_begin,curhl_end);

    if (offScreenX())
      canvas.parentwin.gotomeasure(chooseNewMeasureNum());
  }

/*------------------------------------------------------------------------
Method:  void shiftHighlightWithinMultiEvent(int offset)
Purpose: Attempt to shift cursor position within multi-event
Parameters:
  Input:  int offset - number of events to shift
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void shiftHighlightWithinMultiEvent(int offset)
  {
    MultiEvent me=(MultiEvent)canvas.renderedSections[sectionNum].eventinfo[voiceNum].getEvent(eventnum).getEvent();

    multiEventHLindex+=offset;
    if (multiEventHLindex<0)
      multiEventHLindex=me.getNumEvents()-1;
    else if (multiEventHLindex>=me.getNumEvents())
      multiEventHLindex=0;

    canvas.parentwin.updatemusicgfx=true;
    canvas.repaint();
  }

/*------------------------------------------------------------------------
Method:  double calcev[left|right]x(int sn,int vn,int en)
Purpose: Calculate left or right coordinate of one event
Parameters:
  Input:  int sn,vn - section/voice number
          int en    - event number
  Output: -
  Return: left or right x-value
------------------------------------------------------------------------*/

  double calcevleftx(int sn,int vn,int en)
  {
    double leftMeasureX=canvas.getMeasureX(canvas.curmeasure),
           ex=canvas.renderedSections[sn].getEventXLoc(vn,en),
           leftx=canvas.XLEFT+(ex-leftMeasureX+5)*canvas.VIEWSCALE;
    if (leftx<canvas.XLEFT)
      leftx=canvas.XLEFT;
    return leftx;
  }

  double calcevrightx(int sn,int vn,int en)
  {
    double        leftMeasureX=canvas.getMeasureX(canvas.curmeasure),
                  ex=canvas.renderedSections[sn].getEventXLoc(vn,en);
    RenderedEvent e=canvas.renderedSections[sn].eventinfo[vn].getEvent(en);
    double        rightx=canvas.XLEFT+(ex-leftMeasureX+e.getrenderedxsize()+1)*canvas.VIEWSCALE;
    if (rightx>=canvas.viewsize.width)
      rightx=canvas.viewsize.width;
    return rightx;
  }

/*------------------------------------------------------------------------
Method:  void setNoHighlight()
Purpose: Remove highlight (without repainting)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public synchronized void setNoHighlight()
  {
    highlight_begin=-1;
  }

/*------------------------------------------------------------------------
Method:  void shiftCursorLoc(int xs,int vs)
Purpose: Attempt to shift cursor position
Parameters:
  Input:  int xs - number of events to go left/right
          int vs - number of voices to go up/down
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public synchronized void shiftCursorLoc(int xs,int vs)
  {
    if (hidden)
      return;

    int newsnum,newvnum,neweventnum;
    for (newvnum=voiceNum+vs;
         newvnum>0 && newvnum<canvas.getCurSection().getNumVoices() && canvas.getCurSection().getVoice(newvnum)==null;
         newvnum+=vs)
      ;

    if (highlight_begin==-1)
      neweventnum=eventnum+xs;
    else
      neweventnum=xs>0 ? highlight_end+xs : highlight_begin;

    /* deal with shift in voice number */
    if (newvnum<0 || newvnum>=canvas.getCurSection().getNumVoices())
      return;
    if (vs!=0)
      {
        if (offScreenX())
          canvas.parentwin.gotomeasure(chooseNewMeasureNum());
        newCursorLoc(canvas_xloc,Math.round(canvas.YTOP-canvas.VIEWYSTART+(newvnum*canvas.STAFFSCALE*canvas.STAFFSPACING)*canvas.VIEWSCALE));
        return;
      }

    /* deal with shift in event number */
    newsnum=sectionNum;
    if (neweventnum<0)
      if (sectionNum==0)
        neweventnum=0;
      else
        {
          newsnum--;
          newvnum=canvas.renderedSections[newsnum].getSectionData().getValidVoicenum(newvnum);
          neweventnum=canvas.renderedSections[newsnum].eventinfo[newvnum].size()-1;
        }
    if (neweventnum>=canvas.renderedSections[newsnum].eventinfo[newvnum].size())
      if (sectionNum==canvas.numSections-1)
        neweventnum=canvas.renderedSections[newsnum].eventinfo[newvnum].size()-1;
      else
        {
          newsnum++;
          newvnum=canvas.renderedSections[newsnum].getSectionData().getValidVoicenum(newvnum);
          neweventnum=0;
        }

    /* don't position cursor at undisplayed events (e.g., clefs which have been
       replaced for display) */
    if (xs<0)
      while (neweventnum>0 &&
             !canvas.renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum).isdisplayed())
        neweventnum--;
    else if (xs>0)
      while (neweventnum<canvas.renderedSections[newsnum].eventinfo[newvnum].size() &&
             !canvas.renderedSections[newsnum].eventinfo[newvnum].getEvent(neweventnum).isdisplayed())
        neweventnum++;

    moveCursor(newsnum,newvnum,neweventnum);
  }

/*------------------------------------------------------------------------
Method:  void modifyHighlight(int xs)
Purpose: Attempt to expand or contract highlight
Parameters:
  Input:  int xs - number of events to expand/contract left/right
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public synchronized void modifyHighlight(int xs)
  {
    int neweventnum;
    if (highlight_begin==-1)
      {
        neweventnum=eventnum+xs;
        if (neweventnum<0 || neweventnum>=canvas.renderedSections[sectionNum].eventinfo[voiceNum].size())
          return;
        canvas.setHLAnchor(eventnum);
        highlightEvents(sectionNum,voiceNum,Math.min(eventnum,neweventnum),
                                            Math.max(eventnum,neweventnum)-1);
      }
    else
      {
        int hl_anchor=canvas.getHLAnchor();
        if (highlight_begin>=hl_anchor)
          neweventnum=highlight_end+xs+1;
        else
          neweventnum=highlight_begin+xs;
        if (neweventnum<0)
          neweventnum=0;
        if (neweventnum>=canvas.renderedSections[sectionNum].eventinfo[voiceNum].size())
          neweventnum=canvas.renderedSections[sectionNum].eventinfo[voiceNum].size()-1;
        if (neweventnum==hl_anchor)
          {
            moveCursor(sectionNum,voiceNum,neweventnum); /* remove highlight */
            return;
          }
        highlightEvents(sectionNum,voiceNum,Math.min(hl_anchor,neweventnum),
                                            Math.max(hl_anchor,neweventnum)-1);
      }
  }

/*------------------------------------------------------------------------
Method:  void newCursorLoc(int x,int y)
Purpose: Attempt to move cursor to a given XY position in canvas
Parameters:
  Input:  int x - desired x position
          int y - desired y position
  Output: -
  Return: -
------------------------------------------------------------------------*/

  synchronized void newCursorLoc(int x,int y)
  {
    if (hidden)
      return;

    int newsnum=canvas.calcSectionNum(x),
        newvnum=canvas.calcVNum(newsnum,y),
        neweventnum=canvas.calcEventnum(newsnum,newvnum,x);

    moveCursor(newsnum,newvnum,neweventnum);
  }

/*------------------------------------------------------------------------
Method:  void moveCursor(int newsnum,int newvnum,int neweventnum)
Purpose: Move cursor to a new position and display
Parameters:
  Input:  int newsnum,newvnum,neweventnum - new cursor parameters
  Output: -
  Return: -
------------------------------------------------------------------------*/

  synchronized void moveCursor(int newsnum,int newvnum,int neweventnum)
  {
    hideCursor();

    /* unhighlight events */
    if (highlight_begin!=-1)
      {
        paintHighlight(canvas.getbufferg2d());
        highlight_begin=-1;
      }

    sectionNum=newsnum;
    voiceNum=newvnum;
    eventnum=neweventnum;

    /* ensure a valid location */
    if (sectionNum>=canvas.renderedSections.length)
      sectionNum=canvas.renderedSections.length-1;
    while (voiceNum>=0 && canvas.renderedSections[sectionNum].eventinfo[voiceNum]==null)
      voiceNum--;
    if (voiceNum<0)
      {
        voiceNum=newvnum+1;
        while (voiceNum<canvas.renderedSections[sectionNum].eventinfo.length &&
               canvas.renderedSections[sectionNum].eventinfo[voiceNum]==null)
          voiceNum++;
      }
    if (eventnum>=canvas.renderedSections[sectionNum].eventinfo[voiceNum].size())
      eventnum=canvas.renderedSections[sectionNum].eventinfo[voiceNum].size()-1;

    measurenum=canvas.renderedSections[sectionNum].eventinfo[voiceNum].getEvent(eventnum).getmeasurenum();
    calc_xlocs();

    canvas.parentEditorWin.setSectionNum(sectionNum);

    boolean osx=offScreenX(),osy=offScreenY();
    if (osx)
      canvas.parentwin.gotomeasure(chooseNewMeasureNum());
    if (osy)
      canvas.parentwin.gotoHeight(choose_newHeight());
    if (!(osx || osy))
      showCursor();
  }

/*------------------------------------------------------------------------
Method:  void calc_xlocs()
Purpose: Calculate x-location of cursor on canvas and score, based on
         location in notation
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  synchronized void calc_xlocs()
  {
    score_xloc=(int)canvas.renderedSections[sectionNum].getEventXLoc(voiceNum,eventnum)+1;
    canvas_xloc=Math.round(canvas.XLEFT+(float)(score_xloc-canvas.getMeasureX(canvas.curmeasure))*canvas.VIEWSCALE);
  }

/*------------------------------------------------------------------------
Method:  boolean offScreenX()
Purpose: Check whether cursor is out of visible display area (horizontal)
Parameters:
  Input:  -
  Output: -
  Return: whether cursor is off-screen
------------------------------------------------------------------------*/

  synchronized boolean offScreenX()
  {
    double  scrollXLEFT=canvas.XLEFT;//canvas.curmeasure>0 ? canvas.XLEFT+leftmeasure.xlength : canvas.XLEFT;
    boolean curEventOff=canvas_xloc<scrollXLEFT || canvas_xloc>=canvas.viewsize.width-5*canvas.VIEWSCALE;

    if (highlight_begin==-1 || highlight_begin==highlight_end)
      return curEventOff; /* single event */
    else
      {
        /* multiple highlighted events */
        if (!curEventOff)
          return false;

        /* left side of highlight is offscreen; is right? */
        float right_xloc=(float)(canvas.renderedSections[sectionNum].getEventXLoc(voiceNum,highlight_end)+1)*canvas.VIEWSCALE,
              right_canvas_xloc=canvas.XLEFT+(float)(right_xloc-canvas.getMeasureX(canvas.curmeasure))*canvas.VIEWSCALE;
        return right_canvas_xloc<scrollXLEFT || right_canvas_xloc>=canvas.viewsize.width-5*canvas.VIEWSCALE;
      }
  }

/*------------------------------------------------------------------------
Method:  boolean offScreenY()
Purpose: Check whether cursor is out of visible display area (vertical)
Parameters:
  Input:  -
  Output: -
  Return: whether cursor is off-screen
------------------------------------------------------------------------*/

  synchronized boolean offScreenY()
  {
    double ytop=calcCursorYTop(),
           ybottom=ytop+calcCursorYSize();

    if (Math.round(ybottom)>canvas.VIEWYSTART+canvas.screensize.height ||
        Math.round(ytop)<canvas.VIEWYSTART)
      return true;
    return false;
  }

/*------------------------------------------------------------------------
Method:  int chooseNewMeasureNum()
Purpose: Choose new measure for display, in order to return to current
         cursor location
Parameters:
  Input:  -
  Output: -
  Return: new measure number to go to
------------------------------------------------------------------------*/

  synchronized int chooseNewMeasureNum()
  {
    int newcursormnum=canvas.renderedSections[sectionNum].eventinfo[voiceNum].getEvent(eventnum).getmeasurenum();

    /* to the left of the current display? put new measure near left; leave one
       measure buffer zone to avoid backspacing over offscreen events
       FIX: should this be modified so that there is always an event at the
       left? (i.e., skip back two measures for a Longa etc.) */
    if (newcursormnum<=canvas.curmeasure)
      return newcursormnum>0 ? newcursormnum-1 : 0;

    /* otherwise put new measure at right side of screen */
    float xspace_covered=(float)canvas.getMeasure(newcursormnum).xlength*canvas.VIEWSCALE;
    int   leftmn=newcursormnum-1;

    for (; leftmn>=0 && xspace_covered<canvas.viewsize.width-5*canvas.VIEWSCALE-canvas.calcXLEFT(leftmn+1); leftmn--)
      xspace_covered+=canvas.getMeasure(leftmn).xlength*canvas.VIEWSCALE;
    if (xspace_covered>=canvas.viewsize.width-5*canvas.VIEWSCALE-canvas.calcXLEFT(leftmn+1))
      leftmn++;

    /* correct for cursor past measure end */
    double newLeftMeasureX=canvas.getMeasureX(leftmn+1);
    float  new_canvas_xloc=canvas.calcXLEFT(leftmn+1)+(float)(score_xloc-newLeftMeasureX)*canvas.VIEWSCALE;
    while (new_canvas_xloc>=canvas.viewsize.width-5*canvas.VIEWSCALE)
      {
        leftmn++;
        newLeftMeasureX=canvas.getMeasureX(leftmn+1);
        new_canvas_xloc=canvas.calcXLEFT(leftmn+1)+(float)(score_xloc-newLeftMeasureX)*canvas.VIEWSCALE;
      }

    return leftmn+1;
  }

/*------------------------------------------------------------------------
Method:  int choose_newHeight()
Purpose: Choose new y-value for display, in order to return to current
         cursor location
Parameters:
  Input:  -
  Output: -
  Return: new y-val to go to
------------------------------------------------------------------------*/

  synchronized int choose_newHeight()
  {
    double ytop=calcCursorYTop(),
           ybottom=ytop+calcCursorYSize();
    long   newval=0;

    if (Math.round(ybottom)>canvas.VIEWYSTART+canvas.screensize.height)
      newval=Math.round(calcCursorYTop(voiceNum+1))-canvas.screensize.height;
    else if (Math.round(ytop)<canvas.VIEWYSTART)
      if (voiceNum==0)
        newval=0;
      else
        newval=Math.round(ytop-canvas.STAFFSPACING*3*canvas.VIEWSCALE);

    if (Math.round(ybottom)>newval+canvas.screensize.height)
      newval=Math.round(ybottom)-canvas.screensize.height;
    if (Math.round(ytop)<newval)
      newval=Math.round(ytop);
    if (newval+canvas.screensize.height>canvas.viewsize.height)
      newval=canvas.viewsize.height-canvas.screensize.height;
    if (newval<0)
      newval=0;
    return (int)newval;
  }
}
