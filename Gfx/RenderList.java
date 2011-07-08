/*----------------------------------------------------------------------*/
/*

        Module          : RenderList.java

        Package         : Gfx

        Classes Included: RenderList

        Purpose         : Keep track of rendered events for one voice in a
                          mensural music section

        Programmer      : Ted Dumitrescu

        Date Started    : 4/23/99

Updates:
4/02/04: for clef/mensuration queries beyond end of event list, now returns last
         valid value (for displaying clef/mens information in measures after a
         voice has finished)
3/24/05: moved RenderedEvent to separate file
6/16/05: now extends class ArrayList rather than wrapping it in a separate
         variable
7/14/05: added PDF-writing support for clefset-drawing

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;
import com.lowagie.text.pdf.PdfContentByte;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   RenderList
Extends: java.util.ArrayList
Purpose: Store events in order, with rendering info, for one voice
------------------------------------------------------------------------*/

public class RenderList extends ArrayList<RenderedEvent>
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public int       totalxsize=0;
  public OptionSet options;

  Voice              voicedata;
  MusicSection       section;
  VoiceEventListData voiceEventData;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderList(OptionSet o,Voice v,MusicSection section)
Purpose:     Initialize event list
Parameters:
  Input:  OptionSet o          - rendering/drawing options
          Voice v              - original voice data
          MusicSection section - section data
  Output: -
------------------------------------------------------------------------*/

  public RenderList(OptionSet o,Voice v,MusicSection section)
  {
    super();
    this.options=o;
    this.voicedata=v;
    this.voiceEventData=v==null ? null : section.getVoice(v.getNum()-1);
    this.section=section;
  }

  public RenderList(Voice v,MusicSection section)
  {
    super();
    options=OptionSet.makeDEFAULT_ORIGINAL(null);
    voicedata=v;
    this.section=section;
  }

/*------------------------------------------------------------------------
Method:  RenderedEvent addevent(boolean display,Event e,RenderParams rp)
Purpose: Add event to list
Parameters:
  Input:  boolean display - whether to display event
          Event e         - event
          RenderParams rp - musical parameters at this score location
  Output: -
  Return: new event
------------------------------------------------------------------------*/

  public RenderedEvent addevent(boolean display,Event e,RenderParams rp)
  {
    RenderedEvent newe=new RenderedEvent(display,e,rp,options);
    add(newe);
    return newe;
  }

/*------------------------------------------------------------------------
Method:  int addlig(VoiceEventListData v,int firstevnum,RenderParams rp[,boolean varDisplay])
Purpose: Add ligature to list
Parameters:
  Input:  VoiceEventListData v - event list
          int evnum            - event list index of first note
          RenderParams rp      - musical parameters at this score location
          boolean varDisplay   - whether to stop at variant border
  Output: -
  Return: number of events in ligature
------------------------------------------------------------------------*/

  /* find next event in RenderList representing a NoteEvent */
  RenderedEvent findnextnoteevent(int i)
  {
    for (; i<size(); i++)
      if (getEvent(i).getEvent().hasEventType(Event.EVENT_NOTE))
        return getEvent(i);
    return null;
  }

  public int addlig(EventListData v,int evnum,RenderParams rp)
  {
    return addlig(v,evnum,rp,false);
  }

  public int addlig(EventListData v,int evnum,RenderParams rp,boolean varDisplay)
  {
    Event         e=v.getEvent(evnum);
    NoteEvent     ne=(NoteEvent)e.getFirstEventOfType(Event.EVENT_NOTE);
    RenderedEvent newe;
    int           numeventsinlig=0;

    if (!ne.isligated())
      {
        System.err.println("!! ligature rendering error: attempting to render unligated note as ligature");
        ne.prettyprint();
//        System.err.println("listplace="+e.getListPlace(v.isDefaultVersion())+" evnum="+evnum);
      }
//    if (e==null)
//      return 0;

    boolean done=false;
    while (!done)
      {
        newe=new RenderedEvent(true,e,rp,options);
        add(newe);
        numeventsinlig++;
        ne=(NoteEvent)e.getFirstEventOfType(Event.EVENT_NOTE);
        if (ne!=null && !ne.isligated())
          done=true;
        e=v.getEvent(++evnum);
        if (e==null || (varDisplay && e instanceof VariantMarkerEvent))
          done=true;
      }

    /* now that ligature events have been rendered, redraw as ligature */
    RenderedEvent curligre,lastne=null,nextne=null;
    Event         cure;
    for (int i=size()-numeventsinlig; i<size(); i++)
      {
        curligre=getEvent(i);
        cure=curligre.getEvent();
        ne=(NoteEvent)cure.getFirstEventOfType(Event.EVENT_NOTE);
        if (ne!=null)
          {
            if (ne.isligated())
              nextne=findnextnoteevent(i+1);

            curligre.renderaslig(lastne,nextne);

            lastne=curligre;
          }
      }

    return numeventsinlig;
  }

/*------------------------------------------------------------------------
Method:  float drawclefset(int starti,boolean princOnly,
                           java.awt.Graphics2D g,MusicFont mf,
                           java.awt.image.ImageObserver ImO,
                           float xl,float yl,float VIEWSCALE)
Purpose: Draws multi-clef into given graphical context
Parameters:
  Input:  int starti        - index of first clef event
          boolean princOnly - whether to draw just principal clefs
          Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          ImageObserver ImO - observer for drawImage
          float xl,yl       - location in context to draw event
          float VIEWSCALE   - scaling factor
  Output: -
  Return: x size of total clef set
------------------------------------------------------------------------*/
/*
  public float drawclefset(int starti,boolean princOnly,
                           java.awt.Graphics2D g,MusicFont mf,
                           java.awt.image.ImageObserver ImO,
                           float xl,float yl,float VIEWSCALE)
  {
    float         origxl=xl;
    int           i=starti;
    RenderedEvent re=getEvent(i);
    Event         ce;
    ClefSet       origcs;

    if (re==null)
      return 0;
    ce=re.getEvent();
    origcs=ce.getClefSet(options.get_usemodernclefs());
    boolean done=!ce.hasSignatureClef();

     combine multiple clef events into a single display set 
    if (starti>0 && origcs.getprincipalclef().isprincipalclef() && !ce.hasPrincipalClef())
      xl+=drawclefset(getEvent(starti-1).getClefEventNum(),princOnly,g,mf,ImO,xl,yl,VIEWSCALE);

    while (!done)
      {
        if ((!princOnly) || ce.hasPrincipalClef())
          xl+=re.drawClefs(g,mf,ImO,xl,yl,VIEWSCALE);

        re=getEvent(++i);
        if (re!=null)
          {
            ce=re.getEvent();
            done=ce.getClefSet(options.get_usemodernclefs())!=origcs;
          }
        else
          done=true;
      }
    return xl-origxl;
  }*/

/*------------------------------------------------------------------------
Method:  float drawclefset(int starti,PDFCreator outp,float xl,float yl)
Purpose: Draws multi-clef into PDF
Parameters:
  Input:  int starti        - index of first clef event
          PDFCreator outp   - PDF-writing object
          PdfContentByte cb - PDF graphical context
          float xl,yl       - location in context to draw event
  Output: -
  Return: x size of total clef set
------------------------------------------------------------------------*/
/*
  public float drawclefset(int starti,PDFCreator outp,PdfContentByte cb,float xl,float yl)
  {
    float         origxl=xl;
    int           i=starti;
    RenderedEvent re=getEvent(i);
    Event         ce;
    ClefSet       origcs;

    if (re==null)
      return 0;
    ce=re.getEvent();
    origcs=ce.getClefSet(options.get_usemodernclefs());
    boolean done=!ce.hasSignatureClef();

     combine multiple clef events into a single display set 
    if (starti>0 && origcs.getprincipalclef().isprincipalclef() && !ce.hasPrincipalClef())
      xl+=drawclefset(getEvent(starti-1).getClefEventNum(),outp,cb,xl,yl);

    while (!done)
      {
        xl+=re.drawClefs(outp,cb,xl,yl);

        re=getEvent(++i);
        if (re!=null)
          {
            ce=re.getEvent();
            done=ce.getClefSet(options.get_usemodernclefs())!=origcs;
          }
        else
          done=true;
      }
    return xl-origxl;
  }
*/
/*------------------------------------------------------------------------
Method:  float getclefsetsize(int starti)
Purpose: Calculate x size of multi-clef (without drawing)
Parameters:
  Input:  int starti - index of first clef event
  Output: -
  Return: x size of total clef set
------------------------------------------------------------------------*/
/*
  public float getclefsetsize(int starti)
  {
    float         totalxsize=0;
    int           i=starti;
    RenderedEvent re=getEvent(i);
    Event         ce;
    ClefSet       origcs;

    if (re==null)
      return 0;
    ce=re.getEvent();
    origcs=ce.getClefSet(options.get_usemodernclefs());
    boolean done=!ce.hasSignatureClef();

     combine multiple clef events into a single display set 
    if (starti>0 && origcs.getprincipalclef().isprincipalclef() && !ce.hasPrincipalClef())
      totalxsize+=getclefsetsize(getEvent(starti-1).getClefEventNum());

    while (!done)
      {
        totalxsize+=re.getClefImgXSize();

        re=getEvent(++i);
        if (re!=null)
          {
            ce=re.getEvent();
            done=ce.getClefSet(options.get_usemodernclefs())!=origcs;
          }
        else
          done=true;
      }
    return totalxsize;
  }*/

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public MusicSection getSection()
  {
    return section;
  }

  public Voice getVoiceData()
  {
    return voicedata;
  }

  public VoiceEventListData getVoiceEventData()
  {
    return voiceEventData;
  }

/*------------------------------------------------------------------------
Methods: get*(int i)
Purpose: Routines to return rendering parameters at particular list locations
Parameters:
  Input:  int i - list index to check
  Output: -
  Return: parameters (events/structures)
------------------------------------------------------------------------*/

  public RenderedEvent getEvent(int i)
  {
    if (i>=0 && i<size())
      return get(i);
    else
      return null;
  }

  public RenderedClefSet getClefEvents(int i)
  {
    if (i<0)
      return null;
    else if (i<size())
      return getEvent(i).getClefEvents();
    else if (size()>0)
      return getEvent(size()-1).getClefEvents();
    else
      return null;
  }

  public RenderedEvent getMensEvent(int i)
  {
    if (i<0)
      return null;
    if (i<size())
      return getEvent(i).getMensEvent();
    else if (size()>0)
      return getEvent(size()-1).getMensEvent();
    else
      return null;
  }

  public Coloration getColoration(int i)
  {
    if (i<0)
      return section.getBaseColoration();
    if (i<size())
      return getEvent(i).getColoration();
    else if (size()>0)
      return getEvent(size()-1).getColoration();
    else
      return null;
  }

  public Proportion getProportion(int i)
  {
    if (i<0)
      return Proportion.EQUALITY;
    if (i<size())
      return getEvent(i).getProportion();
    else if (size()>0)
      return getEvent(size()-1).getProportion();
    else
      return null;
  }

  public ModernKeySignature getModernKeySig(int i)
  {
    if (i<0)
      return ModernKeySignature.DEFAULT_SIG;
    if (i<size())
      return getEvent(i).getModernKeySig();
    else if (size()>0)
      return getEvent(size()-1).getModernKeySig();
    else
      return null;
  }

  public OptionSet getOptions()
  {
    return options;
  }

  public boolean inEditorialSection(int i)
  {
    if (i<0)
      return false;
    if (i<size())
      return getEvent(i).inEditorialSection();
    else if (size()>0)
      return getEvent(size()-1).inEditorialSection();
    else
      return false;
  }
}
