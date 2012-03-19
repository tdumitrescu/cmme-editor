/*----------------------------------------------------------------------*/
/*

        Module          : RenderedEvent.java

        Package         : Gfx

        Classes Included: RenderedEvent

        Purpose         : Handles one rendered event (i.e.,
                          DataStruct.Event plus graphics information)

        Programmer      : Ted Dumitrescu

        Date Started    : 99 (moved from RenderList.java 3/24/05)

Updates:
4/05:    moved all drawing code from DataStruct, created EventImg/image-list
         drawing scheme
4/11/05: implemented non-displayed events in score
6/16/05: corrected precision for x-coordinates (replaced ints with doubles)
7/10/05: fixed note display to generate multiple ledger lines
7/25/05: moved class EventImg into separate file (for public access from
         PDFCreator)
2/2/06:  began implementing rendered MultiEvents
4/6/06:  added support for half-colored obliqua ligature components
5/1/06:  minor fixes: variable staff height for modern accidentals above
                      downstem/no-stem notes
                      addition of "optional" images in original-shape ligated
                      notes (e.g., modern text and accidentals)
         added small modern accidental shapes for above-staff display
7/14/05: added PDF-writing support for clefset-drawing

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;
import java.awt.Polygon;
import com.lowagie.text.pdf.PdfContentByte;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   RenderedEvent
Extends: -
Purpose: Information about one rendered event
------------------------------------------------------------------------*/

public class RenderedEvent
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  Event            e;
  RenderParams     musicparams;
  ClefSet          clefset;
  Clef             princlef; /* principal clef, for determining staffpos */
  ModernAccidental accidental;
  OptionSet        options;
  RenderedSonority fullSonority; /* other notes sounding simultaneously */

  LinkedList<RenderedEvent> multiEventList; /* list of rendered events for
                                               one MultiEvent */

  double              xloc;
  boolean             useligxpos, /* whether x-pos should depend on previous note */
                      ligEnd,
                      modernNoteShapes;
  Proportion          musictime,   /* position in terms of time */
                      musicLength; /* length in terms of time */
  int                 ssnum,       /* position on staff */
                      imgcolor;
  double              imgxsize,
                      UNSCALEDMainXSize,
                      imgXSizeWithoutText;
  ArrayList<EventImg> imgs;
  boolean             display;    /* whether to display in score */
  int                 attachedEventIndex; /* index of last dot or other event
                                             immediately following/attached */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderedEvent(boolean d,Event ev,RenderParams rp,OptionSet op)
Purpose:     Initialize event
Parameters:
  Input:  boolean d         - whether to display event
          Event ev          - event
          RenderParams rp   - musical parameters at this score location
          OptionSet op      - drawing/rendering options
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public RenderedEvent(boolean d,Event ev,RenderParams rp,OptionSet op)
  {
    /* initialize event data */
    e=ev;
    options=op;
    display=d;
    musicparams=rp;
    ligEnd=musicparams.endlig;
    attachedEventIndex=-1;
    modernNoteShapes=options.useModernNoteShapes();

    musicLength=ev.getmusictime()!=null ? new Proportion(ev.getmusictime()) : null;

    multiEventList=null;
    if (e.geteventtype()==Event.EVENT_MULTIEVENT)
      {
        RenderedClefSet savedCS=musicparams.clefEvents;
        imgs=null;
        imgxsize=imgXSizeWithoutText=0;
        multiEventList=new LinkedList<RenderedEvent>();
        musicparams.inMultiEvent=true;
        for (Iterator i=((MultiEvent)e).iterator(); i.hasNext();)
          {
            RenderedEvent re=new RenderedEvent(d,(Event)i.next(),musicparams,op);
            multiEventList.add(re);
            if (re.imgxsize>imgxsize)
              imgxsize=re.imgxsize;
            if (re.imgXSizeWithoutText>imgXSizeWithoutText)
              imgXSizeWithoutText=re.imgXSizeWithoutText;
          }
        musicparams.clefEvents=savedCS; /* restore params modified by individual events in multi-event */
      }

    if (ev.hasSignatureClef())
      musicparams.clefEvents=new RenderedClefSet(musicparams.clefEvents,this,options.get_usemodernclefs(),musicparams.suggestedModernClef);
    if (ev.getMensInfo()!=null)
      musicparams.mensEvent=this;

    clefset=musicparams.clefEvents!=null ? musicparams.clefEvents.getLastClefEvent().getEvent().getClefSet(op.get_usemodernclefs()) : null;
    if (options.get_usemodernclefs() && musicparams.suggestedModernClef!=null)
      princlef=musicparams.suggestedModernClef;
    else
      princlef=musicparams.clefEvents!=null ? clefset.getprincipalclef() : null;
    imgcolor=e.isEditorial() || e.displayAsEditorial() ? Coloration.GRAY : e.getcolor();
    this.accidental=null;

    if (multiEventList==null)
      initImages();
  }

/*------------------------------------------------------------------------
Method:  void initImages()
Purpose: Create image set for drawing event
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void initImages()
  {
    int    STAFFPOSSCALE=options.getSTAFFSCALE()/2;
    double VIEWSCALE=options.getVIEWSCALE(),
           curxoff=0,
           UNSCALEDxoff=0;
    EventImg curImg;

    imgs=new ArrayList<EventImg>();

/*    if (e.isEditorial())
      {
        EventGlyphImg edImg=new EventGlyphImg(
          MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_BRACKETLEFT,4,
          curxoff,STAFFPOSSCALE*4,UNSCALEDxoff,0f,Coloration.BLACK);
        imgs.add(edImg);
        curxoff+=edImg.xsize;
      }*/

    switch (e.geteventtype())
      {
        case Event.EVENT_CLEF:
          Clef c=((ClefEvent)e).getClef(options.get_usemodernclefs(),options.getUseModernAccidentalSystem());
          if (c.isprincipalclef() && options.get_usemodernclefs() && musicparams.suggestedModernClef!=null)
            c=musicparams.suggestedModernClef;
          if (c.cleftype==Clef.CLEF_NONE)
            break;
          ssnum=c.getypos(princlef);

          addLedgerLineImages();

          curImg=new EventGlyphImg(
            MusicFont.PIC_CLEFSTART+c.cleftype,ssnum,
            curxoff,STAFFPOSSCALE*ssnum,UNSCALEDxoff,0f,imgcolor);
          imgs.add(curImg);

          if (options.get_displayedittags() && c.issignatureclef() && !c.isprincipalclef())
            {
              int sigMarkerPos=ssnum>4 ? ssnum+6 : 10;
              imgs.add(new EventStringImg(
                " S",sigMarkerPos,curxoff,(sigMarkerPos-9)*STAFFPOSSCALE,UNSCALEDxoff,0f,
                Coloration.GRAY,(int)MusicFont.DEFAULT_TEXT_SMALLFONTSIZE));
            }

          curxoff+=curImg.xsize;
          break;

        case Event.EVENT_MENS:
          MensEvent     mense=(MensEvent)e;
          EventGlyphImg ei;
          int           curssnum=mense.getStaffLoc();
          boolean       smallMens=mense.small();
          if (modernNoteShapes && !e.isEditorial() && !musicparams.inMultiEvent)
            {
              curssnum=4;
              smallMens=false;
            }

          for (Iterator mei=mense.iterator(); mei.hasNext();)
            {
              MensSignElement curSign=(MensSignElement)mei.next();
              switch (curSign.signType)
                {
                  case MensSignElement.NO_SIGN:
                    break;
                  case MensSignElement.NUMBERS:
                    imgs.add(new EventStringImg(
                      String.valueOf(curSign.number.i1),curssnum-1,
                      curxoff+MusicFont.CONNECTION_SCREEN_MENSNUMBERX,STAFFPOSSCALE*(curssnum-1)-options.getSTAFFSCALE()*4-1+(smallMens ? 0 : MusicFont.CONNECTION_SCREEN_MENSNUMBERY),
                      UNSCALEDxoff+MusicFont.CONNECTION_MENSNUMBERX,MusicFont.CONNECTION_MENSNUMBERY,
                      imgcolor,smallMens ? (int)MusicFont.DEFAULT_TEXT_FONTSIZE : (int)MusicFont.DEFAULT_TEXT_LARGEFONTSIZE));
                    if (mense.vertical())
                      curssnum-=2;
                    else
                      {
                        curxoff+=MusicFont.CONNECTION_SCREEN_MENSNUMBERX;
                        UNSCALEDxoff+=MusicFont.CONNECTION_MENSNUMBERX;
                      }
                    break;
                  default:
                    int picnum,picoffset=smallMens ? MusicFont.PIC_MENS_OFFSETSMALL : 0;
                    if (curSign.signType==MensSignElement.MENS_SIGN_O)
                      picnum=MusicFont.PIC_MENS_O+picoffset;
                    else if (curSign.signType==MensSignElement.MENS_SIGN_C)
                      picnum=MusicFont.PIC_MENS_C+picoffset;
                    else if (curSign.signType==MensSignElement.MENS_SIGN_CREV)
                      picnum=MusicFont.PIC_MENS_CREV+picoffset;
                    else
                      picnum=MusicFont.PIC_MENS_NONE+picoffset;

                    imgs.add(ei=new EventGlyphImg(
                      MusicFont.PIC_MENSSTART+picnum,curssnum,
                      curxoff,STAFFPOSSCALE*curssnum,
                      UNSCALEDxoff,0f,imgcolor));
                    if (curSign.stroke)
                      imgs.add(new EventGlyphImg(
                        MusicFont.PIC_MENSSTART+MusicFont.PIC_MENS_STROKE+picoffset,curssnum,
                        curxoff,ei.yoff,
                        UNSCALEDxoff,0f,imgcolor));
                    if (curSign.dotted)
                      imgs.add(new EventGlyphImg(
                        MusicFont.PIC_MENSSTART+MusicFont.PIC_MENS_DOT+picoffset,curssnum,
                        curxoff,ei.yoff,
                        UNSCALEDxoff,0f,imgcolor));
                    if (mense.vertical())
                      curssnum-=smallMens ? 2 : 4;
                    else
                      {
                        curxoff+=MusicFont.CONNECTION_SCREEN_MENSSIGNX*(smallMens ? .5 : 1);
                        UNSCALEDxoff+=MusicFont.CONNECTION_MENSSIGNX*(smallMens ? .5 : 1);
                      }
               }
            }

          /* to ensure proper image size after numbers: 
          imgs.add(new EventStringImg(
            " ",curssnum-1,
            curxoff+MusicFont.CONNECTION_SCREEN_MENSNUMBERX,STAFFPOSSCALE*(curssnum-1)-options.getSTAFFSCALE()*4,
            0f,MusicFont.CONNECTION_MENSNUMBERY,
            Coloration.BLACK,12));*/
          break;

        case Event.EVENT_NOTE:
          NoteEvent ne=(NoteEvent)e;
          double    xoff=0,yoff;
          ssnum=ne.getPitch().calcypos(princlef);

          addLedgerLineImages();

          /* notehead */
          int notetype=ne.getnotetype(),
              stemdir=-1,
              stemPicOffset=0;
          if (ne.hasStem())
            {
              stemdir=calcStemDir(ne,modernNoteShapes || options.get_usemodernclefs());
              if (stemdir==NoteEvent.STEM_DOWN)
                stemPicOffset=modernNoteShapes ? MusicFont.PIC_MODNOTE_OFFSET_STEMDOWN :
                                                 MusicFont.PIC_NOTE_OFFSET_STEMDOWN;
              else if (stemdir==NoteEvent.STEM_UP)
                stemPicOffset=modernNoteShapes ? MusicFont.PIC_MODNOTE_OFFSET_STEMUP :
                                                 MusicFont.PIC_NOTE_OFFSET_STEMUP;
            }

          EventGlyphImg noteheadImg=new EventGlyphImg(
            MusicFont.PIC_NOTESTART+ne.getnoteheadstyle()+stemPicOffset,ssnum,
            xoff=0,yoff=STAFFPOSSCALE*ssnum,0f,0f,imgcolor);
          imgs.add(noteheadImg);
          imgxsize=noteheadImg.xsize;
          UNSCALEDMainXSize=MusicFont.getDefaultPrintGlyphWidth(noteheadImg.imgnum);

          double origxoff=xoff,origyoff=yoff,
                 UNSCALEDyoff=0f;
          UNSCALEDxoff=0f;

          if (stemdir==NoteEvent.STEM_BARLINE)
            imgs.add(new EventShapeImg(
              new Line2D.Float((float)(xoff+MusicFont.CONNECTION_SCREEN_L_STEMX),(float)(0-STAFFPOSSCALE),
                               (float)(xoff+MusicFont.CONNECTION_SCREEN_L_STEMX),(float)(options.getSTAFFSCALE()*4.5)),
                               new float[] { (float)(UNSCALEDxoff+MusicFont.CONNECTION_L_STEMX),(float)(UNSCALEDxoff+MusicFont.CONNECTION_L_STEMX) },
                               new float[] { 0,           0 },
                               imgcolor,Coloration.VOID,
                               8,0,1));

          /* flags */
          if (ne.isflagged() ||
              notetype==NoteEvent.NT_Semifusa ||
              notetype==NoteEvent.NT_Fusa)
            {
              if (!modernNoteShapes)
                {
                  xoff+=1.2;
                  UNSCALEDxoff+=MusicFont.CONNECTION_SB_UPSTEMX;
                }
              else
                {
                  xoff+=MusicFont.CONNECTION_SCREEN_MODFLAGX;
                  UNSCALEDxoff+=MusicFont.CONNECTION_MODFLAGX;
                }

              int flagpic;
              if (stemdir==NoteEvent.STEM_UP)
                {
                  UNSCALEDyoff+=modernNoteShapes ? MusicFont.CONNECTION_STEM_UPMODFLAGY :
                                                   MusicFont.CONNECTION_STEM_UPFLAGY;
                  flagpic=modernNoteShapes ? MusicFont.PIC_MODNOTESTART+MusicFont.PIC_MODNOTE_OFFSET_FLAGUP :
                                             MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_FLAGUP;
                }
              else
                {
                  UNSCALEDyoff+=modernNoteShapes ? MusicFont.CONNECTION_STEM_DOWNMODFLAGY :
                                                   MusicFont.CONNECTION_STEM_DOWNFLAGY;
                  flagpic=modernNoteShapes ? MusicFont.PIC_MODNOTESTART+MusicFont.PIC_MODNOTE_OFFSET_FLAGDOWN :
                                             MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_FLAGDOWN;
                }
              imgs.add(new EventGlyphImg(
                flagpic,ssnum,
                xoff,yoff,UNSCALEDxoff,UNSCALEDyoff,imgcolor));

              int numFlags=ne.getNumFlags(), numFlagsLeft,
                  yincrdir=stemdir==NoteEvent.STEM_UP ? -1 : 1;
              if (notetype==NoteEvent.NT_Semifusa)
                numFlagsLeft=1;
              else if (numFlags>0)
                numFlagsLeft=numFlags-1;
              else
                numFlagsLeft=0;

              while (--numFlagsLeft>=0)
                {
                  UNSCALEDyoff+=yincrdir*MusicFont.CONNECTION_FLAGINTERVAL;
                  yoff+=yincrdir*MusicFont.CONNECTION_SCREEN_FLAGINTERVAL;
                  imgs.add(new EventGlyphImg(
                    flagpic,ssnum,
                    xoff,yoff,UNSCALEDxoff,UNSCALEDyoff,imgcolor));
                }
            }

          addNoteOptionImages(ne,origxoff,origyoff,ssnum,stemdir);
          break;

        case Event.EVENT_REST:
          RestEvent re=(RestEvent)e;

          int numSets=re.getNumSets(),
              bottomLine=re.getbottomline(options.get_usemodernclefs());
          ssnum=2*(bottomLine-1);
          int RIMGBASE=modernNoteShapes ? MusicFont.PIC_MODRESTSTART :
                                          MusicFont.PIC_RESTSTART;

          addLedgerLineImages();

          xoff=0;
          UNSCALEDxoff=0f;
          EventImg curi;
          for (int ri=0; ri<numSets; ri++)
            {
              imgs.add(curi=new EventGlyphImg(
                RIMGBASE+re.getnotetype(),ssnum,
                xoff,STAFFPOSSCALE*ssnum,UNSCALEDxoff,0f,imgcolor));

              for (int i=1; i<re.getnumlines(); i++)
                imgs.add(curi=new EventGlyphImg(
                  RIMGBASE+re.getnotetype(),ssnum+2*i,
                  xoff,STAFFPOSSCALE*(ssnum+2*i),UNSCALEDxoff,0f,imgcolor));

              xoff+=curi.xsize*4f;
              UNSCALEDxoff+=MusicFont.CONNECTION_BARLINEX*1.5f;
            }
          break;

        case Event.EVENT_DOT:
          DotEvent de=(DotEvent)e;
          imgs.add(new EventGlyphImg(
            MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_DOT,calcDotLoc(de,options.get_usemodernclefs(),musicparams.lastEvent),
            MusicFont.CONNECTION_SCREEN_DOTX,STAFFPOSSCALE*(calcDotLoc(de,options.get_usemodernclefs(),musicparams.lastEvent)),
            0f,0f,imgcolor));
          break;

        case Event.EVENT_ORIGINALTEXT:
          OriginalTextEvent oe=(OriginalTextEvent)e;
          ssnum=-6;
          int txtColor=Coloration.BLACK;
          xoff=0;
          if (options.get_displayModText())
            ssnum+=2;

          /* multi-text */
          VariantVersionData textVersion=oe.getVariantVersion();
          if (textVersion!=null)
            {
              int versionNum=textVersion.getNumInList();
              ssnum-=versionNum*OptionSet.SPACES_PER_TEXTLINE;
              txtColor=versionNum%OptionSet.TEXTVERSION_COLORS+1;
            }

          if (options.get_displayedittags())
            {
              EventGlyphImg tagImg=new EventGlyphImg(MusicFont.PIC_NULL,0,0,
                                                     STAFFPOSSCALE*8,0f,0f,Coloration.BLUE);
              imgs.add(tagImg);
              xoff+=tagImg.xsize;
//tagImg.xsize=0;
            }
          if (options.get_displayOrigText())
            {
              EventStringImg textImg=new EventStringImg(oe.getText(),ssnum,xoff,STAFFPOSSCALE*(ssnum)-options.getSTAFFSCALE()*4,
                                                        0f,0f,txtColor,(int)MusicFont.DEFAULT_TEXT_FONTSIZE);
              textImg.xsize=0;
              imgs.add(textImg);
            }
          break;

        case Event.EVENT_CUSTOS:
          CustosEvent custe=(CustosEvent)e;
          imgs.add(new EventGlyphImg(
            MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_CUSTOS,custe.getPitch().calcypos(princlef),
            0,STAFFPOSSCALE*custe.getPitch().calcypos(princlef),0f,0f,imgcolor));
          break;

        case Event.EVENT_LINEEND:
          LineEndEvent le=(LineEndEvent)e;
          imgs.add(new EventGlyphImg(
            MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_LINEEND,0,
            0,0,0f,0f,imgcolor));
          if (le.isPageEnd())
            imgs.add(new EventStringImg("||",9,3,0,0f,0f,
                                        imgcolor,(int)MusicFont.DEFAULT_TEXT_SMALLFONTSIZE));
          break;

        case Event.EVENT_PROPORTION:
          Proportion pp=((ProportionEvent)e).getproportion();
          if (options.get_displayedittags())
            {
              imgs.add(new EventGlyphImg(MusicFont.PIC_NULL,8,0,STAFFPOSSCALE*8,0f,0f,Coloration.BLACK));
              imgs.add(new EventStringImg("["+pp.i1+":"+pp.i2+"]",10,0,options.getSTAFFSCALE(),0f,0f,
                                          Coloration.BLUE,(int)MusicFont.DEFAULT_TEXT_SMALLFONTSIZE));
            }
          break;

        case Event.EVENT_COLORCHANGE:
          if (options.get_displayedittags())
            imgs.add(new EventGlyphImg(MusicFont.PIC_NULL,0,0,STAFFPOSSCALE*8,0f,0f,Coloration.BLACK));
          break;

        case Event.EVENT_BARLINE:
          BarlineEvent be=(BarlineEvent)e;
          ssnum=2*be.getBottomLinePos();
          boolean isRepeatSign=be.isRepeatSign();
          int     repeatSignOffset=isRepeatSign ? 3 : 0;
          for (int i=0; i<be.getNumLines(); i++)
            {
              EventShapeImg esi=new EventShapeImg(
                new Line2D.Float((float)((i+repeatSignOffset)*MusicFont.CONNECTION_SCREEN_BARLINEX),
                                 (float)(STAFFPOSSCALE*(8-ssnum)),
                                 (float)((i+repeatSignOffset)*MusicFont.CONNECTION_SCREEN_BARLINEX),
                                 (float)(STAFFPOSSCALE*(8-ssnum-be.getNumSpaces()*2))),
                                 new float[] { (float)(i*MusicFont.CONNECTION_BARLINEX),(float)(i*MusicFont.CONNECTION_BARLINEX) },
                                 new float[] { 0,                              0 },
                                 imgcolor,Coloration.VOID,
                                 8,0,1);
              esi.xsize+=10;
              imgs.add(esi);
            }
          if (isRepeatSign)
            {
              int dotPos=be.getBottomLinePos();
              for (int di=0; di<be.getNumSpaces(); di++)
                {
                  imgs.add(new EventGlyphImg(
                    MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_DOT,1+(dotPos+di)*2,
                    MusicFont.CONNECTION_SCREEN_DOTX,STAFFPOSSCALE*(1+(dotPos+di)*2),
                    0f,0f,imgcolor));
                  EventGlyphImg esi=new EventGlyphImg(
                    MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_DOT,1+(dotPos+di)*2,
                    (be.getNumLines()+repeatSignOffset)*MusicFont.CONNECTION_SCREEN_BARLINEX,STAFFPOSSCALE*(1+(dotPos+di)*2),
                    0f,0f,imgcolor);
                  esi.xsize+=10;
                  imgs.add(esi);
                }
            }          
          break;

        case Event.EVENT_ANNOTATIONTEXT:
          AnnotationTextEvent ae=(AnnotationTextEvent)e;
          if (options.get_displayedittags())
            imgs.add(new EventGlyphImg(MusicFont.PIC_NULL,0,0,STAFFPOSSCALE*8,0f,0f,Coloration.BLACK));
          EventStringImg textImg=new EventStringImg(ae.gettext(),ae.getstaffloc(),5,STAFFPOSSCALE*ae.getstaffloc()-options.getSTAFFSCALE()*4,
                                                    0f,0f,imgcolor,(int)MusicFont.DEFAULT_TEXT_FONTSIZE);
          textImg.xsize=0;
          imgs.add(textImg);
          break;

        case Event.EVENT_LACUNA:
          imgs.add(new EventGlyphImg(MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_BRACKETLEFT,4,0,STAFFPOSSCALE*4,0f,0f,Coloration.RED));
          break;
        case Event.EVENT_LACUNA_END:
          imgs.add(new EventGlyphImg(MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_BRACKETRIGHT,4,0,STAFFPOSSCALE*4,0f,0f,Coloration.RED));
          break;

        case Event.EVENT_VARIANTDATA_START:
          VariantMarkerEvent vme=(VariantMarkerEvent)e;
          if (options.isLigatureList() || !options.markVariant(vme.getVarTypeFlags()))
            {
              display=false;
              break;
            }
          if (options.get_displayedittags())
            if (vme.getVarTypeFlags()!=VariantReading.VAR_ORIGTEXT)
              imgs.add(new EventGlyphImg(MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_BRACKETLEFT,4,0,STAFFPOSSCALE*4,0f,0f,Coloration.GREEN));
            else
              {
                EventShapeImg esi=new EventShapeImg(
                  new Line2D.Float(
                    (float)MusicFont.CONNECTION_SCREEN_BARLINEX,(float)(STAFFPOSSCALE*-2),
                    (float)MusicFont.CONNECTION_SCREEN_BARLINEX,(float)(STAFFPOSSCALE*2)),
                    new float[] { 0f,0f },
                    new float[] { 0f,0f },
                    Coloration.GREEN,Coloration.VOID,
                    8,0,1);
                esi.xsize+=5;
                imgs.add(esi);
              }
          else
            {
              VariantReading vr=getVarReadingInfo()!=null ? 
                getVarReadingInfo().varReading : null;
              imgs.add(new EventStringImg("v",9,4,0,0f,0f,
                                          (vr!=null && vr.isError()) ? Coloration.RED : Coloration.GREEN,
                                          (int)MusicFont.DEFAULT_TEXT_LARGEFONTSIZE));
              display=false;
            }
          break;
        case Event.EVENT_VARIANTDATA_END:
          vme=(VariantMarkerEvent)e;
          if (options.isLigatureList() || !options.markVariant(vme.getVarTypeFlags()))
            {
              display=false;
              break;
            }
          if (options.get_displayedittags())
            if (vme.getVarTypeFlags()!=VariantReading.VAR_ORIGTEXT)
              imgs.add(new EventGlyphImg(MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_BRACKETRIGHT,4,0,STAFFPOSSCALE*4,0f,0f,Coloration.GREEN));
            else
              {
                EventShapeImg esi=new EventShapeImg(
                  new Line2D.Float(
                    5f,(float)(STAFFPOSSCALE*-2),
                    5f,(float)(STAFFPOSSCALE*2)),
                    new float[] { 0f,0f },
                    new float[] { 0f,0f },
                    Coloration.GREEN,Coloration.VOID,
                    8,0,1);
                esi.xsize+=5;
                imgs.add(esi);
              }
          else
            display=false;
          break;

        case Event.EVENT_ELLIPSIS:
          if (options.get_unscoredDisplay())
            {
              double ellXS=50,ellYS=options.getSTAFFSCALE()*5.5f;
              EventShapeImg esi=new EventShapeImg(
                new Rectangle2D.Float(0,(float)(0-ellYS/10),(float)ellXS,(float)ellYS),
                new float[] { 0,0,(float)ellXS,(float)ellXS }, /* NO - need to use print values */
                new float[] { 0,(float)ellYS,(float)ellYS,0 },
                Coloration.WHITE,Coloration.FULL,9,-1,3);
              imgs.add(esi);
              imgs.add(new EventStringImg(". . .",0,ellXS/3,0-ellYS,0,0,Coloration.BLACK,(int)MusicFont.DEFAULT_TEXT_LARGEFONTSIZE));
            }
          else
            imgs.add(new EventGlyphImg(MusicFont.PIC_NULL,0,0,0,0f,0f,Coloration.BLACK));
          break;

        case Event.EVENT_SECTIONEND:
        case Event.EVENT_BLANK:
          break;

        case Event.EVENT_MODERNKEYSIGNATURE:
          if (options.get_displayedittags())
            {
              imgs.add(new EventGlyphImg(MusicFont.PIC_NULL,0,0,STAFFPOSSCALE*8,0f,0f,imgcolor));
              imgs.add(new EventStringImg(" K",10,0,options.getSTAFFSCALE(),0f,0f,
                                          Coloration.BLUE,(int)MusicFont.DEFAULT_TEXT_SMALLFONTSIZE));
            }
          if (options.getUseModernAccidentalSystem())
            {
              ModernKeySignature mk=((ModernKeySignatureEvent)e).getSigInfo();

              xoff=5;
              c=princlef;
              int staffApos=c.getApos(),
                  accColor=options.get_displayedittags() ? Coloration.BLUE : imgcolor;

              if (staffApos<0)
                staffApos+=7;
              else if (staffApos>5)
                staffApos-=7;

              /* add individual accidentals in signature */
              for (Iterator i=mk.iterator(); i.hasNext();)
                {
                  ModernKeySignatureElement kse=(ModernKeySignatureElement)i.next();

                  for (int ai=0; ai<kse.accidental.numAcc; ai++)
                    {
                      ssnum=staffApos+kse.calcAOffset();
                      EventGlyphImg mkeImg=new EventGlyphImg(
                        MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlat+kse.accidental.accType,
                        ssnum,xoff,STAFFPOSSCALE*ssnum,0f,0f,accColor);
                      imgs.add(mkeImg);
                      xoff+=mkeImg.xsize+MusicFont.CONNECTION_SCREEN_MODACC_DBLFLAT;
                    }
                }
            }
          break;

        default:
          imgs.add(new EventGlyphImg(MusicFont.PIC_NULL,0,0,0,0f,0f,Coloration.BLACK));
      }

    initExtraImgs();

    imgxsize=imgXSizeWithoutText=0;
    int eventType=e.geteventtype();
    for (EventImg ei : imgs)
      {
        if (ei.xsize>0 && ei.xoff+ei.xsize>imgxsize)
          imgxsize=ei.xoff+ei.xsize;
        if (ei.xsize>0 && ei.xoff+ei.xsize>imgXSizeWithoutText)
          if (eventType!=Event.EVENT_NOTE ||
              !(ei instanceof EventStringImg))
            imgXSizeWithoutText=ei.xoff+ei.xsize;
      }

    /* fix for note-spacing at small values */
    if (eventType==Event.EVENT_NOTE)
      {
        int notetype=((NoteEvent)e).getnotetype();
        switch (notetype)
          {
            case NoteEvent.NT_Fusa:
            case NoteEvent.NT_Flagged:
              imgxsize-=2;
              imgXSizeWithoutText-=2;
              break;
            case NoteEvent.NT_Semifusa:
              imgxsize-=4;
              imgXSizeWithoutText-=4;
              break;
          }
      }

    grayOutIfMissing();
  }

  /* gray out music missing in current version */
  void grayOutIfMissing()
  {
    if (musicparams.missingInVersion)
      for (EventImg ei : imgs)
        ei.color=Coloration.GRAY;
  }

/*------------------------------------------------------------------------
Method:  void initExtraImgs()
Purpose: Initialize non-event-specific images (corona, signum congruentiae,
         etc)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void initExtraImgs()
  {
    int STAFFPOSSCALE=options.getSTAFFSCALE()/2;

    Signum s=e.getCorona();
    if (s!=null)
      addSignumImg(s,MusicFont.PIC_MISC_CORONAUP);
    s=e.getSignum();
    if (s!=null)
      addSignumImg(s,MusicFont.PIC_MISC_SIGNUMUP);

    if (e.isError())
      {
        EventStringImg eeImg=new EventStringImg(
          " X",ssnum+4,0,STAFFPOSSCALE*(ssnum+4-9),0f,0f,
          Coloration.RED,(int)MusicFont.DEFAULT_TEXT_LARGEFONTSIZE);
        imgs.add(eeImg);
      }

    String ec=e.getEdCommentary();
    if (ec!=null && options.getViewEdCommentary())
      {
        EventStringImg ecImg=new EventStringImg(
          " *",9,0,options.getSTAFFSCALE()-STAFFPOSSCALE,0f,0f,
          Coloration.BLUE,(int)MusicFont.DEFAULT_TEXT_LARGEFONTSIZE);
        imgs.add(ecImg);
      }
  }

/*------------------------------------------------------------------------
Method:  void addLedgerLineImages()
Purpose: Initialize images for ledger lines
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addLedgerLineImages()
  {
    int STAFFPOSSCALE=options.getSTAFFSCALE()/2;
    if (ssnum<-1 || ssnum>9)
      {
        int curledgerplace,incr;
        if (ssnum<0)
          {
            curledgerplace=-2;
            incr=-2;
          }
        else
          {
            curledgerplace=10;
            incr=2;
          }
        for (; Math.abs(curledgerplace)<=Math.abs(ssnum); curledgerplace+=incr)
          imgs.add(new EventGlyphImg(
            MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_LEDGER,curledgerplace,
            -1,STAFFPOSSCALE*curledgerplace,0f,0f,Coloration.BLACK));
      }
  }

/*------------------------------------------------------------------------
Method:  void addSignumImg(Signum s,int picnumUp)
Purpose: Initialize image for one corona or signum congruentiae
Parameters:
  Input:  Signum s     - sign position/orientation info
          int picnumUp - index of sign image (in up orientation)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addSignumImg(Signum s,int picnumUp)
  {
    int   STAFFPOSSCALE=options.getSTAFFSCALE()/2;
    int   picnum=MusicFont.PIC_MISCSTART+picnumUp+(s.orientation==Signum.DOWN ? 1 : 0),
          syp=ssnum;
    double screen_xoff=0,xoff=0,
           screen_xconnect=0,xconnect=0;

    if (imgs.size()>0)
      {
        EventImg firstimg=(EventImg)(imgs.get(0));
        screen_xoff=firstimg.xoff;
        xoff=firstimg.UNSCALEDxoff;
        syp=firstimg.staffypos;
      }
    if (picnumUp==MusicFont.PIC_MISC_CORONAUP)
      {
        screen_xconnect=MusicFont.CONNECTION_SCREEN_CORONAX;
        xconnect=MusicFont.CONNECTION_CORONAX;
      }
    if (s.side==Signum.LEFT)
      screen_xoff-=5;
    else if (s.side==Signum.RIGHT)
      screen_xoff+=5;
    EventGlyphImg sigImg=new EventGlyphImg(
      picnum,syp+s.offset,
      screen_xoff+screen_xconnect,STAFFPOSSCALE*(syp+s.offset),
      xoff+xconnect,0f,imgcolor);
    sigImg.xsize=0;
    imgs.add(sigImg);
  }

/*------------------------------------------------------------------------
Method:  void renderaslig(RenderedEvent lastnote,RenderedEvent nextnote)
Purpose: Re-do image information to render note as part of a ligature in
         its original shape
Parameters:
  Input:  RenderedEvent lastnote,nextnote - previous and next notes in ligature
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void renderaslig(RenderedEvent lastnote,RenderedEvent nextnote)
  {
    NoteEvent ne=(NoteEvent)e.getFirstEventOfType(Event.EVENT_NOTE);
    int       nnssnum=nextnote==null ? this.ssnum+1 : nextnote.ssnum;
    if (ne!=null)
      {
        imgs=new ArrayList<EventImg>();
        int STAFFSCALE=options.getSTAFFSCALE(),
            STAFFPOSSCALE=STAFFSCALE/2;

        NoteEvent lastne=lastnote==null ? null : (NoteEvent)lastnote.getEvent().getFirstEventOfType(Event.EVENT_NOTE),
                  nextne=nextnote==null ? null : (NoteEvent)nextnote.getEvent().getFirstEventOfType(Event.EVENT_NOTE);
        double    UNSCALEDxoff=0f,UNSCALEDyoff=0f,
                  xoff=0,yoff=STAFFPOSSCALE*ssnum,
                  origxoff=xoff,origyoff=yoff;

        /* ledger lines */
        if (ssnum<-1 || ssnum>9)
          {
            int curledgerplace,incr;
            if (ssnum<0)
              {
                curledgerplace=-2;
                incr=-2;
              }
            else
              {
                curledgerplace=10;
                incr=2;
              }
            for (; Math.abs(curledgerplace)<=Math.abs(ssnum); curledgerplace+=incr)
              imgs.add(new EventGlyphImg(
                MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_LEDGER,curledgerplace,
                xoff-1,STAFFPOSSCALE*curledgerplace,0f,0f,imgcolor));
          }

        /* notehead */
        if (ne.getligtype()==NoteEvent.LIG_OBLIQUA)
          {
            /* obliqua start */
            double xl=MusicFont.CONNECTION_SCREEN_L_LEFTSTEMX,
                   xr=MusicFont.CONNECTION_SCREEN_LIG_RECTA*2,
                   yl=MusicFont.PICYCENTER-yoff-STAFFPOSSCALE,
                   yr=MusicFont.PICYCENTER-STAFFPOSSCALE*nnssnum-STAFFPOSSCALE,
                   totalx=xr-xl,totaly=yr-yl,
                   USxl=MusicFont.CONNECTION_L_LEFTSTEMX,
                   USxr=MusicFont.CONNECTION_LIG_RECTA*2,
                   USyup=MusicFont.CONNECTION_LIG_UPSTEMY,
                   USydown=0-MusicFont.CONNECTION_LIG_UPSTEMY,
                   UStotalx=USxr-USxl;
            boolean fullColored=e.getcolorfill()==Coloration.FULL &&
                                (nextne!=null && nextne.getcolorfill()==Coloration.FULL);
            imgs.add(new EventShapeImg(
              new Polygon(
                new int[] { Math.round((float)xl), Math.round((float)xl),
                            Math.round((float)xr), Math.round((float)xr) },
                new int[] { Math.round((float)yl),            Math.round((float)(yl+STAFFSCALE)),
                            Math.round((float)(yr+STAFFSCALE)), Math.round((float)yr) },
                4),
              new float[] { (float)USxl,  (float)USxl,    (float)USxr,    (float)USxr }, 
              new float[] { (float)USyup, (float)USydown, (float)USydown, (float)USyup },
              imgcolor,fullColored ? Coloration.FULL : Coloration.VOID,
              ssnum,nnssnum,2));

            /* half-coloration */
            if (e.getcolorfill()==Coloration.FULL && !fullColored)
              imgs.add(new EventShapeImg(
                new Polygon(
                  new int[] { Math.round((float)xl),            Math.round((float)xl),
                              Math.round((float)(xl+totalx/2)), Math.round((float)(xl+totalx/2)) },
                  new int[] { Math.round((float)yl),                       Math.round((float)(yl+STAFFSCALE)),
                              Math.round((float)(yl+STAFFSCALE+totaly/2)), Math.round((float)(yl+totaly/2)) },
                  4),
/* FIX THIS!!! implement half-coloration y-vals for 'unscaled' coordinates */
                new float[] { (float)USxl,  (float)USxl,    (float)(USxl+UStotalx/2), (float)(USxl+UStotalx/2) }, 
                new float[] { (float)USyup, (float)USydown, (float)USydown,         (float)USyup           },
                imgcolor,Coloration.FULL,
                ssnum,nnssnum,2));
          }
        else if (lastne!=null && lastne.getligtype()==NoteEvent.LIG_OBLIQUA)
          {
            /* obliqua end */
            boolean fullColored=e.getcolorfill()==Coloration.FULL &&
                                lastne.getcolorfill()==Coloration.FULL;

            /* half-coloration */
            if (e.getcolorfill()==Coloration.FULL && !fullColored)
              {
                double xl=0,
                      xr=MusicFont.CONNECTION_SCREEN_LIG_RECTA,
                      lastyl=MusicFont.PICYCENTER-STAFFPOSSCALE*lastnote.ssnum-STAFFPOSSCALE,
                      yr=MusicFont.PICYCENTER-yoff-STAFFPOSSCALE,
                      yl=lastyl+(yr-lastyl)/2,
                      USxl=0,
                      USxr=MusicFont.CONNECTION_LIG_RECTA,
                      USyup=MusicFont.CONNECTION_LIG_UPSTEMY,
                      USydown=0-MusicFont.CONNECTION_LIG_UPSTEMY;

                imgs.add(new EventShapeImg(
                  new Polygon(
                    new int[] { Math.round((float)xl), Math.round((float)xl),
                                Math.round((float)xr), Math.round((float)xr) },
                    new int[] { Math.round((float)yl),            Math.round((float)(yl+STAFFSCALE)),
                                Math.round((float)(yr+STAFFSCALE)), Math.round((float)yr) },
                    4),
/* FIX THIS!!! implement half-coloration y-vals for 'unscaled' coordinates */
                  new float[] { (float)USxl,  (float)USxl,    (float)USxr,    (float)USxr  }, 
                  new float[] { (float)USyup, (float)USydown, (float)USydown, (float)USyup },
                  imgcolor,Coloration.FULL,
                  lastnote.ssnum,ssnum,2));
              }
          }
        else
          {
            /* recta (start or end) */
            imgs.add(new EventGlyphImg(
              MusicFont.PIC_NOTESTART+(ne.getcolorfill()==Coloration.FULL ? NoteEvent.NOTEHEADSTYLE_FULLBREVE : NoteEvent.NOTEHEADSTYLE_BREVE),ssnum,
              xoff,yoff,0f,0f,imgcolor));
            if (lastne!=null && lastne.getligtype()==NoteEvent.LIG_RECTA)
              useligxpos=true;
          }
        if (ne.getligtype()==NoteEvent.LIG_NONE)
          imgxsize=MusicFont.getDefaultGlyphWidth(MusicFont.PIC_NOTESTART+NoteEvent.NOTEHEADSTYLE_BREVE);
        else
          imgxsize=MusicFont.CONNECTION_SCREEN_L_STEMX;

        /* vertical connecting line (only after recta start) */
        if (ne.getligtype()==NoteEvent.LIG_RECTA)
          {
            double nextyoff=STAFFPOSSCALE*nnssnum,
                  y1,y2,
                  USy1,USy2;
            if (nextyoff>yoff)
              {
                y1=yoff-MusicFont.CONNECTION_SCREEN_L_UPSTEMY;
                y2=nextyoff+MusicFont.CONNECTION_SCREEN_L_UPSTEMY;
                USy1=0-MusicFont.CONNECTION_LIG_UPSTEMY;
                USy2=MusicFont.CONNECTION_LIG_UPSTEMY;
              }
            else
              {
                y1=nextyoff-MusicFont.CONNECTION_SCREEN_L_UPSTEMY;
                y2=yoff+MusicFont.CONNECTION_SCREEN_L_UPSTEMY;
                USy1=MusicFont.CONNECTION_LIG_UPSTEMY;
                USy2=0-MusicFont.CONNECTION_LIG_UPSTEMY;
              }
            imgs.add(new EventShapeImg(
              new Line2D.Float((float)MusicFont.CONNECTION_SCREEN_L_STEMX,(float)(MusicFont.PICYCENTER-y1),
                               (float)MusicFont.CONNECTION_SCREEN_L_STEMX,(float)(MusicFont.PICYCENTER-y2)),
              new float[] { (float)MusicFont.CONNECTION_L_STEMX,(float)MusicFont.CONNECTION_L_STEMX },
              new float[] { (float)USy1,                        (float)USy2 },
              imgcolor,Coloration.VOID,
              ssnum,nnssnum,1));
          }

        /* stem */
        int stemdir=ne.getstemdir(),
            stemside=ne.getstemside(),
            stemssnum=ssnum;
        if (stemside!=NoteEvent.STEM_NONE && stemdir!=NoteEvent.STEM_NONE)
          {
            if (stemside==NoteEvent.STEM_LEFT)
              {
                UNSCALEDxoff+=MusicFont.CONNECTION_L_LEFTSTEMX;
                xoff+=MusicFont.CONNECTION_SCREEN_L_LEFTSTEMX;
                if (stemdir==NoteEvent.STEM_UP)
                  {
                    UNSCALEDyoff=MusicFont.CONNECTION_LIG_UPSTEMY;
                    yoff+=MusicFont.CONNECTION_SCREEN_LIG_UPSTEMY;
                  }
                else
                  {
                    UNSCALEDyoff=MusicFont.CONNECTION_LIG_DOWNSTEMY;
                    yoff+=MusicFont.CONNECTION_SCREEN_LIG_DOWNSTEMY;
                  }
              }
            else
              {
                UNSCALEDxoff+=MusicFont.CONNECTION_L_STEMX;
                xoff+=MusicFont.CONNECTION_SCREEN_L_STEMX;

                /* always down on the right */
                if (ne.isligated() && nnssnum<ssnum)
                  {
                    stemssnum=nnssnum;
                    yoff=STAFFPOSSCALE*stemssnum;
                  }
                UNSCALEDyoff=MusicFont.CONNECTION_LIG_DOWNSTEMY;
                yoff+=MusicFont.CONNECTION_SCREEN_LIG_DOWNSTEMY;
              }
            imgs.add(new EventGlyphImg(
              MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_STEM,stemssnum,
              xoff,yoff,UNSCALEDxoff,UNSCALEDyoff,imgcolor));
          }

        addNoteOptionImages(ne,origxoff,origyoff,ssnum,-1);
      }
    else
      {
        /* non-note event type */
        System.err.println("Error: called renderaslig with non-note type");
      }

    initExtraImgs();
    grayOutIfMissing();
  }

/*------------------------------------------------------------------------
Method:  void addNoteOptionImages(NoteEvent ne,double origxoff,double origyoff,
                                  int ssnum,int stemdir)
Purpose: Create images for note elements besids noteheads and stems (modern
         accidentals, modern text, etc)
Parameters:
  Input:  NoteEvent ne            - note info
          double origxoff,origyoff - original drawing position
          int ssnum               - staff position of note
          int stemdir             - stem direction
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void addNoteOptionImages(NoteEvent ne,double origxoff,double origyoff,
                           int ssnum,int stemdir)
  {
    int STAFFPOSSCALE=options.getSTAFFSCALE()/2;

    /* modern accidental */
    ModernAccidental   po=ne.getPitchOffset(),
                       ma=null;
    ModernKeySignature keySig=ne.getModernKeySig();

    if (!options.getUseModernAccidentalSystem())
      {
        /* if we're showing editorial accidentals but not modern key
           signatures, make sure signature accidentals are shown as
           accidentals on individual notes */
        ModernKeySignature clefKeySig=ModernKeySignature.DEFAULT_SIG;

        if (clefset!=null)
          clefKeySig=clefset.getKeySig();
        ma=clefKeySig.chooseNoteAccidental(ne,po.pitchOffset);
      }
    else
      ma=keySig.chooseNoteAccidental(ne,po.pitchOffset);

    if (ma!=null)
      ma.optional=po.optional;

    if (ma!=null && ne.displayAccidental() &&
        (options.get_displayedittags() ||
         options.get_modacc_type()!=OptionSet.OPT_MODACC_NONE))
      {
        int     accssnum=10;
        double  leftAccX=0,rightAccX=0,
                UNSCALEDleftAccX=0,UNSCALEDrightAccX=0;
        if (stemdir==NoteEvent.STEM_UP && ssnum+9>accssnum)
          accssnum=ssnum+9;
        else if (ssnum+3>accssnum)
          accssnum=ssnum+3;
        if (ne.getCorona()!=null)
          accssnum=ssnum+7;
        if (accssnum<10)
          accssnum=10;

        if (ma.accType==ModernAccidental.ACC_Flat)
          {
            double UNSCALEDcurxoff=0-(ma.numAcc-1)*MusicFont.CONNECTION_MODACCSMALLX,
                   curxoff=0-((ma.numAcc-1)*(MusicFont.getDefaultGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlatSMALL)+MusicFont.CONNECTION_SCREEN_MODACC_SMALLDBLFLAT))/2;

            leftAccX=curxoff;
            UNSCALEDleftAccX=UNSCALEDcurxoff;
            for (int i=0; i<ma.numAcc; i++)
              {
                imgs.add(new EventGlyphImg(
                  MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlatSMALL,accssnum,
                  origxoff+curxoff+MusicFont.CONNECTION_SCREEN_MODACCSMALLX,STAFFPOSSCALE*accssnum,
                  UNSCALEDcurxoff+MusicFont.CONNECTION_MODACCSMALLX,0f,imgcolor));
                curxoff+=MusicFont.getDefaultGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlatSMALL)+MusicFont.CONNECTION_SCREEN_MODACC_SMALLDBLFLAT;
                UNSCALEDcurxoff+=MusicFont.CONNECTION_MODACCSMALLX*2;
              }
            rightAccX=curxoff;
            UNSCALEDrightAccX=UNSCALEDcurxoff;
          }
        else if (ma.accType==ModernAccidental.ACC_Sharp)
          {
            if (stemdir!=NoteEvent.STEM_UP && ssnum>4)
              accssnum++;
            int   numSymbols=ma.numAcc%2+ma.numAcc/2;
            double UNSCALEDcurxoff=0-(numSymbols-1)*MusicFont.CONNECTION_MODACCSMALLX,
                   curxoff=0-((numSymbols-1)*(MusicFont.getDefaultGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNDoubleSharpSMALL)+MusicFont.CONNECTION_SCREEN_MODACC_SMALLDBLSHARP))/2;

            leftAccX=curxoff;
            UNSCALEDleftAccX=UNSCALEDcurxoff;
            if (ma.numAcc==1 || ma.numAcc%2!=0)
              {
                imgs.add(new EventGlyphImg(
                  MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNSharpSMALL,accssnum,
                  origxoff+curxoff+MusicFont.CONNECTION_SCREEN_MODACCSMALLX,STAFFPOSSCALE*accssnum,
                  UNSCALEDcurxoff+MusicFont.CONNECTION_MODACCSMALLX,0f,imgcolor));
                curxoff+=MusicFont.getDefaultGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNSharpSMALL)+MusicFont.CONNECTION_SCREEN_MODACC_SMALLDBLSHARP;
                UNSCALEDcurxoff+=MusicFont.CONNECTION_MODACCSMALLX*2;
                numSymbols--;
              }
            for (int i=0; i<numSymbols; i++)
              {
                imgs.add(new EventGlyphImg(
                  MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNDoubleSharpSMALL,accssnum,
                  origxoff+curxoff+MusicFont.CONNECTION_SCREEN_MODACCSMALLX,STAFFPOSSCALE*accssnum,
                  UNSCALEDcurxoff+MusicFont.CONNECTION_MODACCSMALLX,0f,imgcolor));
                curxoff+=MusicFont.getDefaultGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNDoubleSharpSMALL)+MusicFont.CONNECTION_SCREEN_MODACC_SMALLDBLSHARP;
                UNSCALEDcurxoff+=MusicFont.CONNECTION_MODACCSMALLX*2;
              }
            rightAccX=curxoff;
            UNSCALEDrightAccX=UNSCALEDcurxoff;
          }
        else
          {
            imgs.add(new EventGlyphImg(
              MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlat+ma.accType+Clef.OFFSET_SMALL_ACC,accssnum,
              origxoff+MusicFont.CONNECTION_SCREEN_MODACCSMALLX,STAFFPOSSCALE*accssnum,
              MusicFont.CONNECTION_MODACC_SMALLNATURAL,0f,imgcolor));
            rightAccX=MusicFont.getDefaultGlyphWidth(MusicFont.PIC_CLEFSTART+Clef.CLEF_MODERNFlatSMALL)+MusicFont.CONNECTION_SCREEN_MODACC_SMALLDBLFLAT;;
            UNSCALEDrightAccX=MusicFont.CONNECTION_MODACCSMALLX*2;
          }

        /* parentheses for 'optional' accidentals */
        if (ma.optional)
          {
            /* left parens */
            leftAccX-=MusicFont.getDefaultGlyphWidth(MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_PARENSLEFTSMALL);
            UNSCALEDleftAccX-=MusicFont.CONNECTION_MODACCSMALLX+MusicFont.CONNECTION_MODACC_SMALLPARENS;
            imgs.add(new EventGlyphImg(
              MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_PARENSLEFTSMALL,accssnum,
              origxoff+leftAccX+MusicFont.CONNECTION_SCREEN_MODACC_SMALLPARENSLEFT,STAFFPOSSCALE*accssnum,
              UNSCALEDleftAccX+MusicFont.CONNECTION_MODACC_SMALLPARENS,0f,imgcolor));

            /* right parens */
            imgs.add(new EventGlyphImg(
              MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_PARENSRIGHTSMALL,accssnum,
              origxoff+rightAccX+MusicFont.CONNECTION_SCREEN_MODACC_SMALLPARENSRIGHT,STAFFPOSSCALE*accssnum,
              UNSCALEDrightAccX+MusicFont.CONNECTION_MODACC_SMALLPARENS,0f,imgcolor));
          }
      }
    this.accidental=ma;

    /* modern text */
    String modernText=ne.getModernText();
    int mtssnum=ssnum>-3 ? -6 : ssnum-4, /* if notehead is too low, push syllable below it */
        mtCol=Coloration.BLACK;
    if (options.get_displayedittags() || options.get_displayOrigText())
      {
        mtssnum-=2;
        mtCol=Coloration.BLUE;
      }
    if (modernText!=null && options.get_displayModText())
      {
        if (modernText.length()<=1)
          modernText="  "+modernText;
        else if (modernText.length()<=2)
          modernText=" "+modernText;

        modernText+=" ";
        if (!ne.isWordEnd())
          modernText=modernText+"-"; /* temporary hack to show dashes between syllables! */

        int textStyle=ne.isModernTextEditorial() ? java.awt.Font.ITALIC : java.awt.Font.PLAIN;

        EventStringImg textImg=new EventStringImg(
          modernText,mtssnum,
          origxoff,STAFFPOSSCALE*mtssnum-options.getSTAFFSCALE()*4,
          0f,0f,mtCol,(int)MusicFont.DEFAULT_TEXT_FONTSIZE,textStyle);
        imgs.add(textImg);
      }

    /* modern dot */
    if (ne.hasModernDot())
      {
        int dotLoc=calcModernDotLoc();
        imgs.add(new EventGlyphImg(
          MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_DOT,dotLoc,
          imgxsize+MusicFont.CONNECTION_SCREEN_DOTX,STAFFPOSSCALE*dotLoc,
          UNSCALEDMainXSize+MusicFont.CONNECTION_DOTX,0f,imgcolor));
      }
  }

/*------------------------------------------------------------------------
Method:  void addcolorbracket(int side)
Purpose: Add angle bracket to mark coloration
Parameters:
  Input:  int side - 0=left, 1=right
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addcolorbracket(int side)
  {
    int   STAFFPOSSCALE=options.getSTAFFSCALE()/2,
          bracketssnum=10;
    double xoff=side==0 ? MusicFont.CONNECTION_SCREEN_ANGBRACKETLEFT : MusicFont.CONNECTION_SCREEN_ANGBRACKETRIGHT,
          USxoff=side==0 ? MusicFont.CONNECTION_ANGBRACKETLEFT : MusicFont.CONNECTION_ANGBRACKETRIGHT;
    if (e.geteventtype()==Event.EVENT_NOTE)
      {
        NoteEvent ne=(NoteEvent)e;
        if (side==0 && ne.getstemside()==NoteEvent.STEM_LEFT && ne.getstemdir()==NoteEvent.STEM_UP)
          {
            xoff-=4;
            USxoff-=240;
          }
      }
    imgs.add(new EventGlyphImg(MusicFont.PIC_MISCSTART+MusicFont.PIC_MISC_ANGBRACKETLEFT+side,bracketssnum,
                               xoff,STAFFPOSSCALE*(bracketssnum),
                               USxoff,0f,Coloration.BLACK));
  }

/*------------------------------------------------------------------------
Method:  void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,double VIEWSCALE)
Purpose: Draws event into given graphical context
Parameters:
  Input:  Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          ImageObserver ImO - observer for drawImage
          double xl,yl       - location in context to draw event
          double VIEWSCALE   - scaling factor
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl)
  {
    draw(g,mf,ImO,xl,yl,1f);
  }

  public void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,double VIEWSCALE)
  {
    if (multiEventList!=null)
      /* loop through events */
      for (Iterator i=multiEventList.iterator(); i.hasNext();)
        ((RenderedEvent)(i.next())).draw(g,mf,ImO,xl,yl,VIEWSCALE);

    else
      /* loop through images */
      for (Iterator i=imgs.iterator(); i.hasNext();)
        ((EventImg)(i.next())).draw(g,mf,ImO,xl,yl,VIEWSCALE);
  }

/*------------------------------------------------------------------------
Method:  void drawHighlighted(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                              double xl,double yl,double VIEWSCALE)
Purpose: Draws highlighted version of event into given graphical context
Parameters:
  Input:  Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          ImageObserver ImO - observer for drawImage
          double xl,yl       - location in context to draw event
          double VIEWSCALE   - scaling factor
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void drawHighlighted(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                              double xl,double yl,double VIEWSCALE)
  {
    if (multiEventList!=null)
      /* loop through events */
      for (Iterator i=multiEventList.iterator(); i.hasNext();)
        ((RenderedEvent)(i.next())).drawHighlighted(g,mf,ImO,xl,yl,VIEWSCALE);

    else
      /* loop through images */
      for (Iterator i=imgs.iterator(); i.hasNext();)
        ((EventImg)(i.next())).draw(g,mf,ImO,xl,yl,Coloration.CYAN,VIEWSCALE);
  }

/*------------------------------------------------------------------------
Method:  void drawLig(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                      int xl,int yl)
Purpose: Draws event's ligature (original form) into given graphical context
Parameters:
  Input:  Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          ImageObserver ImO - observer for drawImage
          int xl,yl         - location in context to draw event
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void drawLig(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                      int xl,int yl)
  {
    RenderedLigature li=getLigInfo();
    RenderedEvent    re;
    for (Iterator i=li.rligEvents.iterator(); i.hasNext();)
      {
        re=(RenderedEvent)i.next();
        if (re.getEvent().geteventtype()==Event.EVENT_NOTE)
          re.draw(g,mf,ImO,(int)(xl+re.getxloc()),yl);
      }
  }

/*------------------------------------------------------------------------
Method:  double drawClefs(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                          double xl,double yl,double VIEWSCALE)
Purpose: Draws only clef events into given graphical context
Parameters:
  Input:  Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          ImageObserver ImO - observer for drawImage
          double xl,yl      - location in context to draw event
          double VIEWSCALE  - scaling factor
  Output: -
  Return: amount of x-space used
------------------------------------------------------------------------*/

  public double drawClefs(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                          double xl,double yl,double VIEWSCALE)
  {
    double xs=0;

    if (multiEventList!=null)
      for (Iterator i=multiEventList.iterator(); i.hasNext();)
        {
          RenderedEvent re=(RenderedEvent)i.next();
          Event         ce=re.getEvent();
          if (ce.geteventtype()==Event.EVENT_CLEF &&
              ce.hasSignatureClef() &&
              ((ClefEvent)ce).drawInSig(options.get_usemodernclefs(),options.getUseModernAccidentalSystem()))
            {
              re.draw(g,mf,ImO,xl,yl,VIEWSCALE);
              if (re.getimgxsize()>xs)
                xs=re.getimgxsize();
            }
        }
    else
      if (((ClefEvent)getEvent()).drawInSig(options.get_usemodernclefs(),options.getUseModernAccidentalSystem()))
        {
          draw(g,mf,ImO,xl,yl,VIEWSCALE);
          xs=getimgxsize();
        }

    return xs*VIEWSCALE;
  }

/*------------------------------------------------------------------------
Method:  double drawClefs(PDFCreator outp,PdfContentByte cb,double xl,double yl)
Purpose: Draws only clef events into PDF
Parameters:
  Input:  PDFCreator outp   - PDF-writing object
          PdfContentByte cb - PDF graphical context
          double xl,yl       - location in context to draw event
  Output: -
  Return: amount of x-space used
------------------------------------------------------------------------*/

  public float drawClefs(PDFCreator outp,PdfContentByte cb,float xl,float yl)
  {
    double xs=0;

    if (multiEventList!=null)
      for (Iterator i=multiEventList.iterator(); i.hasNext();)
        {
          RenderedEvent re=(RenderedEvent)i.next();
          Event         ce=re.getEvent();
          if (ce.geteventtype()==Event.EVENT_CLEF &&
              ce.hasSignatureClef() &&
              ((ClefEvent)ce).drawInSig(options.get_usemodernclefs(),options.getUseModernAccidentalSystem()))
            {
              outp.drawEvent(re,xl,yl,false,cb);
              if (re.getimgxsize()>xs)
                xs=re.getimgxsize();
            }
        }
    else
      if (((ClefEvent)getEvent()).drawInSig(options.get_usemodernclefs(),options.getUseModernAccidentalSystem()))
        {
          outp.drawEvent(this,xl,yl,false,cb);
          xs=getimgxsize();
        }

    return (float)(xs*outp.XEVENTSPACE_SCALE);
  }

/*------------------------------------------------------------------------
Method:  double getClefImgXSize()
Purpose: Get x size of visible clef images only
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public double getClefImgXSize()
  {
    double xs=0;

    if (multiEventList!=null)
      for (Iterator i=multiEventList.iterator(); i.hasNext();)
        {
          RenderedEvent re=(RenderedEvent)i.next();
          Event         ce=re.getEvent();
          if (ce.geteventtype()==Event.EVENT_CLEF &&
              ce.hasSignatureClef() &&
              ((ClefEvent)ce).drawInSig(options.get_usemodernclefs(),options.getUseModernAccidentalSystem()))
            {
              if (re.getimgxsize()>xs)
                xs=re.getimgxsize();
            }
        }
    else
      if (((ClefEvent)getEvent()).drawInSig(options.get_usemodernclefs(),options.getUseModernAccidentalSystem()))
        {
          xs=getimgxsize();
        }

    return xs;
  }

/*------------------------------------------------------------------------
Method:  void drawMens(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                       double xl,double yl,double VIEWSCALE)
Purpose: Draws only mensuration events into given graphical context
Parameters:
  Input:  Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          ImageObserver ImO - observer for drawImage
          double xl,yl       - location in context to draw event
          double VIEWSCALE   - scaling factor
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void drawMens(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                       double xl,double yl,double VIEWSCALE)
  {
    if (multiEventList!=null)
      for (Iterator i=multiEventList.iterator(); i.hasNext();)
        {
          RenderedEvent re=(RenderedEvent)i.next();
          if (re.getEvent().geteventtype()==Event.EVENT_MENS)
            re.draw(g,mf,ImO,xl,yl,VIEWSCALE);
        }
    else
      draw(g,mf,ImO,xl,yl,VIEWSCALE);
  }

/*------------------------------------------------------------------------
Method:  int calcStemDir(NoteEvent ne,boolean modernNotation)
Purpose: Calculate stem direction for note, depending on whether
         original notational elements are retained
Parameters:
  Input:  NoteEvent ne           - note to check
          boolean modernNotation - whether modern notation display is on
  Output: -
  Return: stem direction for displaying note
------------------------------------------------------------------------*/

  int calcStemDir(NoteEvent ne,boolean modernNotation)
  {
    if (modernNotation)
      if (princlef.calcypos(ne.getPitch())>=5)
        return NoteEvent.STEM_DOWN;
      else
        return NoteEvent.STEM_UP;
    else
      return ne.getstemdir(); /* take direction specified in transcription */
  }

  /* calculate tie direction depending on notation options */
  int getTieType()
  {
    if (e.geteventtype()!=Event.EVENT_NOTE)
      return NoteEvent.TIE_NONE;

    NoteEvent ne=(NoteEvent)e;
    if (!(modernNoteShapes || options.get_usemodernclefs()))
      return ne.getTieType();
    if (princlef.calcypos(ne.getPitch())>=5)
      return NoteEvent.TIE_OVER;
    return NoteEvent.TIE_UNDER;
  }

/*------------------------------------------------------------------------
Method:  int calcDotLoc(DotEvent de,boolean usemodernclefs,RenderedEvent laste)
Purpose: Calculate staff position for displaying dot, depending on whether
         original clefs are retained
Parameters:
  Input:  -
  Output: -
  Return: staff position for displaying dot
------------------------------------------------------------------------*/

  int calcDotLoc(DotEvent de,boolean usemodernclefs,RenderedEvent laste)
  {
    if (usemodernclefs && laste!=null)
      return laste.calcModernDotLoc();
    else
      return de.calcYPos(princlef);
  }

  /* only for note events */
  int calcModernDotLoc()
  {
    int noteloc=princlef.calcypos(e.getFirstEventOfType(Event.EVENT_NOTE).getPitch());
    return noteloc+(noteloc%2==0 ? 1 : 0);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int getAttachedEventIndex()
  {
    return attachedEventIndex;
  }

  public Event getEvent()
  {
    return e;
  }

  public RenderedEvent getEvent(int i)
  {
    return multiEventList.get(i);
  }

  public LinkedList<RenderedEvent> getEventList()
  {
    return multiEventList;
  }

  public RenderedSonority getFullSonority()
  {
    return fullSonority;
  }

  public OptionSet getOptions()
  {
    return options;
  }

  public RenderParams getRenderParams()
  {
    return musicparams;
  }

  public boolean isdisplayed()
  {
    return display;
  }

  public int getmeasurenum()
  {
    return musicparams.measurenum;
  }

  public double getxloc()
  {
    return xloc;
  }

  public int getssnum()
  {
    return ssnum;
  }

  public Proportion getMusicLength()
  {
    return musicLength;
  }

  public Proportion getmusictime()
  {
    return musictime;
  }

  public double getrenderedxsize()
  {
    return display ? getimgxsize() : 0;
  }

  public double getRenderedXSizeWithoutText()
  {
    return display ? getImgXSizeWithoutText() : 0;
  }

  public double getxend()
  {
    return xloc+getRenderedXSizeWithoutText();
  }

  public ArrayList<EventImg> getimgs()
  {
    return imgs;
  }

  public double getimgxsize()
  {
    return imgxsize;
  }

  public double getImgXSizeWithoutText()
  {
    return imgXSizeWithoutText;
  }

  public RenderedClefSet getClefEvents()
  {
    return musicparams.clefEvents;
  }

  public RenderedEvent getLastClefEvent()
  {
    if (musicparams.clefEvents==null)
      return null;
    return musicparams.clefEvents.getLastClefEvent();
  }

  public ModernAccidental getAccidental()
  {
    return this.accidental;
  }

  public Clef getClef()
  {
    return princlef;
  }

  public RenderedEvent getMensEvent()
  {
    return musicparams.mensEvent;
  }

  public Coloration getColoration()
  {
    return musicparams.curColoration;
  }

  public Proportion getProportion()
  {
    return musicparams.curProportion;
  }

  public ModernKeySignature getModernKeySig()
  {
    return e.getModernKeySig();
  }

  public boolean inEditorialSection()
  {
    return musicparams.inEditorialSection;
  }

  public RenderedLigature getLigInfo()
  {
    return musicparams.ligInfo;
  }

  public boolean isligend()
  {
    return ligEnd;
  }

  public RenderedLigature getTieInfo()
  {
    return musicparams.tieInfo;
  }

  public boolean doubleTied()
  {
    return musicparams.doubleTied;
  }

  public boolean get_useligxpos()
  {
    return useligxpos;
  }

  public RenderedEventGroup getVarReadingInfo()
  {
    return musicparams.varReadingInfo;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set parameters and options
Parameters:
  Input:  new values for parameters and options
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setAttachedEventIndex(int attachedEventIndex)
  {
    this.attachedEventIndex=attachedEventIndex;
  }

  public void setDisplay(boolean display)
  {
    this.display=display;
  }

  public void setEvent(Event e)
  {
    this.e=e;
  }

  public void setLigEnd(boolean ligEnd)
  {
    this.ligEnd=ligEnd;
    musicparams.endlig=ligEnd;
  }

  public void setLigInfo(RenderedLigature ligInfo)
  {
    musicparams.ligInfo=ligInfo;
  }

  public void setMeasureNum(int newval)
  {
    musicparams.measurenum=newval;
  }

  public void setMusicLength(Proportion newval)
  {
    musicLength=new Proportion(newval);
  }

  public void setSonority(RenderedSonority rs)
  {
    fullSonority=rs;
  }

  public void setTieInfo(RenderedLigature tieInfo)
  {
    musicparams.tieInfo=tieInfo;
  }

  public void setDoubleTied(boolean doubleTied)
  {
    musicparams.doubleTied=doubleTied;
  }

  public void setxloc(double xl)
  {
    xloc=xl;
  }

  public void setmusictime(Proportion p)
  {
    musictime=new Proportion(p);
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
    System.out.print("X="+xloc+" m="+musicparams.measurenum+" ");
    getEvent().prettyprint();
  }
}
