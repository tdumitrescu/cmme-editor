/*----------------------------------------------------------------------*/
/*

        Module          : PDFCreator

        Package         : Gfx

        Classes Included: PDFCreator

        Purpose         : Save rendered music in PDF files

        Programmer      : Ted Dumitrescu

        Date Started    : 7/21/05

Updates:
7/10/06:  moved printing parameters into class PrintParams to allow
          arbitrary page-/block-sizing
          added support for multi-events
7/11/06:  began adding support for score layout PDFs
8/30/07:  added support for multi-section scores
7/14/09:  separated initialization and generation steps
9/22/09:  fixed divide-by-zero bug for scores with one system per page
12/22/10: automatically opens PDF upon generation
          added support for ties

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import java.util.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   PDFCreator
Extends: -
Purpose: Translate music with rendering information into printing data and
         save into a PDF file
------------------------------------------------------------------------*/

public class PDFCreator
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static final String CMME_PDF_INFO_STRING="PDF generated automatically from CMME music data (www.cmme.org)";

/*----------------------------------------------------------------------*/
/* Instance variables */

  ArrayList[]       renderedParts;
  ScorePageRenderer renderedScore;
  boolean           partsLayout=false;

  PieceData musicData;
  int       numVoices;

  PrintParams PP;
  BaseFont    CMMEBaseFont,
              PlainFont,
              TextFont,TextItalicFont,
              StaffNameFont,
              ScoreAnnotationFont,
              TitleFont,SubtitleFont;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: PDFCreator([ArrayList[]|ScorePageRenderer] rendered[Parts|Score])
Purpose:    Initialize
Parameters:
  Input:  renderedParts/Score - event rendering information for all voices
            ArrayList[]       - layout in parts, staff by staff
            ScorePageRenderer - multi-page score layout
  Output: -
------------------------------------------------------------------------*/

  public PDFCreator(ArrayList[] renderedParts)
  {
    this.partsLayout=true;
    this.renderedParts=renderedParts;
  }

  public PDFCreator(ScorePageRenderer renderedScore)
  {
    this.partsLayout=false;
    this.renderedScore=renderedScore;
  }

/*------------------------------------------------------------------------
Method:  void createPDF(OutputStream outs)
Purpose: Create PDF data and save to file/output stream
Parameters:
  Input:  OutputStream outs   - destination for PDF data
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void createPDF(File f) throws Exception
  {
    createPDF(new FileOutputStream(f));
    java.awt.Desktop.getDesktop().open(f);
  }

  public void createPDF(String fn) throws Exception
  {
    createPDF(new File(fn));
  }

  public void createPDF(OutputStream outs)
  {
    if (partsLayout)
      {
        PP=createPrintParams(renderedParts);
      }
    else
      {
        PP=createPrintParams(renderedScore);
      }

    PdfContentByte cb=initPDF(outs);

    if (partsLayout)
      {
        drawParts(renderedParts,cb);
      }
    else
      {
        drawScore(renderedScore,cb);
      }
    closePDF();
  }

/*------------------------------------------------------------------------
Method:  void createPrintParams([ArrayList[]|ScorePageRenderer] renderer)
Purpose: Initialize print sizing parameters based on layout type
Parameters:
  Input:  renderer - event rendering information for all voices
            ArrayList[]       - layout in parts, staff by staff
            ScorePageRenderer - multi-page score layout
  Output: -
  Return: -
------------------------------------------------------------------------*/

  PrintParams createPrintParams(ArrayList[] renderer)
  {
	return new PrintParams(PrintParams.DEFAULT_A4PartLandscape);
  }

  PrintParams createPrintParams(ScorePageRenderer renderer)
  {
    return new PrintParams(PrintParams.DEFAULT_A4ScorePortrait);
  }

/*------------------------------------------------------------------------
Method:  PdfContentByte initPDF(OutputStream outs)
Purpose: Open new PDF and prepare for writing
Parameters:
  Input:  OutputStream outs   - destination for PDF data
  Output: -
  Return: content byte for random-access writing into PDF
------------------------------------------------------------------------*/

  com.lowagie.text.Document outPDF;
  PdfWriter                 writer;

  PdfContentByte initPDF(OutputStream outs)
  {
    PdfContentByte             cb=null;
    com.lowagie.text.Rectangle pageSize=new com.lowagie.text.Rectangle(PP.PAGEXSIZE,PP.PAGEYSIZE);

    outPDF=new com.lowagie.text.Document(pageSize);
    try
      {
        writer=PdfWriter.getInstance(outPDF,outs);
        outPDF.open();

        /* initialize page/graphics params */
        cb=writer.getDirectContent();
        CMMEBaseFont=BaseFont.createFont(
          Util.AppContext.BaseDataDir+MusicFont.FontRelativeDir+MusicFont.PrintFontFileName,
          BaseFont.CP1252,BaseFont.EMBEDDED);
        PlainFont=BaseFont.createFont(BaseFont.HELVETICA,BaseFont.CP1252,BaseFont.NOT_EMBEDDED);
        TextFont=BaseFont.createFont(BaseFont.HELVETICA,BaseFont.CP1252,BaseFont.NOT_EMBEDDED);
        TextItalicFont=BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE,BaseFont.CP1252,BaseFont.NOT_EMBEDDED);
        StaffNameFont=BaseFont.createFont(BaseFont.HELVETICA,BaseFont.CP1252,BaseFont.NOT_EMBEDDED);
        ScoreAnnotationFont=BaseFont.createFont(BaseFont.HELVETICA,BaseFont.CP1252,BaseFont.NOT_EMBEDDED);
        TitleFont=BaseFont.createFont(BaseFont.TIMES_ITALIC,BaseFont.CP1252,BaseFont.NOT_EMBEDDED);
        SubtitleFont=BaseFont.createFont(BaseFont.TIMES_ROMAN,BaseFont.CP1252,BaseFont.NOT_EMBEDDED);
      }
    catch (Exception e)
      {
        System.err.println("Error generating PDF: "+e);
        e.printStackTrace();
      }

    return cb;
  }

/*------------------------------------------------------------------------
Method:  void closePDF()
Purpose: Finish PDF file writing
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void closePDF()
  {
    try
      {
        outPDF.close();
      }
    catch (Exception e)
      {
        System.err.println("Error generating PDF: "+e);
        e.printStackTrace();
      }
  }

/*------------------------------------------------------------------------
Method:  void drawParts(ArrayList[] renderer,PdfContentByte cb)
Purpose: Draw music (parts) into initialized PDF
Parameters:
  Input:  ArrayList[] renderer - event rendering information for all voices
                                 (layout in parts, staff by staff)
          PdfContentByte cb    - PDF output structure allowing spacial random
                                 access
  Output: -
  Return: -
------------------------------------------------------------------------*/

  float cury,lastNoteX,
        XEVENTSPACE_SCALE; /* coefficient to translate between screen-based
                              rendering coordinates and PDF points */

  void drawParts(ArrayList[] renderer,PdfContentByte cb)
  {
    float curx;

    cury=PP.PAGEYSIZE-(PP.YMARGIN+PP.STAFFYSCALE*3);
    XEVENTSPACE_SCALE=PP.STAFFXSIZE/PartsWin.getDefaultSTAFFXSIZE();
    lastNoteX=0f;
    numVoices=renderer.length;

    for (int vnum=0; vnum<numVoices; vnum++)
      {
        if (renderer[vnum].size()>0)
          {
            if(vnum != 0) {
              /* Start every voice on a new page */
              outPDF.newPage();
              /* Reset page parameters*/
              cury=PP.PAGEYSIZE-(PP.YMARGIN+PP.STAFFYSCALE*3);
              XEVENTSPACE_SCALE=PP.STAFFXSIZE/PartsWin.getDefaultSTAFFXSIZE();
              lastNoteX=0f;
            }

            String vname=((RenderList)renderer[vnum].get(0)).getVoiceData().getName();
            float  namexsize=StaffNameFont.getWidthPoint(vname,PP.StaffNameFONTSIZE),
                   nameysize=StaffNameFont.getAscentPoint(vname,PP.StaffNameFONTSIZE);

            cb.beginText();
            cb.setFontAndSize(StaffNameFont,PP.StaffNameFONTSIZE);
            cb.setTextMatrix(0,1,-1,0, /* rotate text 90 degrees */
                             PP.XMARGIN-nameysize,
                             cury-PP.STAFFYSCALE*2-namexsize/2);
            cb.showText(vname);
            cb.endText();
          }

        for (Iterator i=renderer[vnum].iterator(); i.hasNext();)
          {
            RenderList curstaff=(RenderList)i.next();

            /* draw staff */
            drawStaff(cb,cury,5,0,curstaff.totalxsize*XEVENTSPACE_SCALE);

            /* draw events */
            cb.beginText();
            cb.setFontAndSize(CMMEBaseFont,PP.MusicFONTSIZE);
            for (int ei=0; ei<curstaff.size(); ei++)
              {
                RenderedEvent e=curstaff.getEvent(ei);
                curx=e.get_useligxpos() ? (float)(lastNoteX+MusicFont.CONNECTION_LIG_RECTA*PP.XYSCALE) :
                                          (float)(PP.XMARGIN+e.getxloc()*XEVENTSPACE_SCALE);
                drawEvent(e,curx,cury,cb);
              }

            cb.endText();

            cury-=PP.STAFFYSPACE;
          }
      }
  }

/*------------------------------------------------------------------------
Method:  void drawScore(ScorePageRenderer renderer,PdfContentByte cb)
Purpose: Draw music (score) into initialized PDF
Parameters:
  Input:  ScorePageRenderer renderer - event rendering information for all voices
                                       (multi-page score layout)
          PdfContentByte cb          - PDF output structure allowing spacial random
                                       access
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawScore(ScorePageRenderer renderer,PdfContentByte cb)
  {
    numVoices=renderer.numVoices;
    musicData=renderer.musicData;

    try
      {
        for (int i=0; i<renderer.pages.size()-1; i++)
          {
            drawScorePage(i,renderer,cb);
            outPDF.newPage();
          }
        drawScorePage(renderer.pages.size()-1,renderer,cb);
      }
    catch (Exception e)
      {
        System.err.println("Error generating PDF: "+e);
        e.printStackTrace();
      }
  }

/*------------------------------------------------------------------------
Method:  void drawScorePage(int curPageNum,ScorePageRenderer renderedPages,PdfContentByte cb)
Purpose: Draw one score page into initialized PDF
Parameters:
  Input:  int curPageNum             - index of page to draw
          ScorePageRenderer renderer - event rendering information for all voices
                                       (multi-page score layout)
          PdfContentByte cb          - PDF output structure allowing spacial random
                                       access
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawScorePage(int curPageNum,ScorePageRenderer renderedPages,PdfContentByte cb)
  {
    /* header/footer */
    cb.beginText();

    if (curPageNum==0)
      {
        drawScoreTitle(cb);
        drawFooter(cb);
      }
    else
      {
        cb.setFontAndSize(PlainFont,PP.PlainFONTSIZE);
        String composer=musicData.getComposer();
        if (composer!=null && !composer.isEmpty())
          composer+=": ";
        String smallTitle=composer+musicData.getTitle();
        if (musicData.getSectionTitle()!=null)
          smallTitle+=" ("+musicData.getSectionTitle()+")";
        cb.setTextMatrix(PP.XMARGIN,PP.PAGEYSIZE-PP.YMARGIN);
        cb.showText(smallTitle);
      }
    cb.setFontAndSize(PlainFont,PP.PlainFONTSIZE);
    if (curPageNum>0)
      cb.showTextAligned(PdfContentByte.ALIGN_RIGHT,
                         String.valueOf(curPageNum+1),
                         PP.PAGEXSIZE-PP.XMARGIN,PP.PAGEYSIZE-PP.YMARGIN,0);

    cb.endText();

    /* music */
    RenderedScorePage curPage=renderedPages.pages.get(curPageNum);
    int startSys=curPage.startSystem,
        endSys=startSys+curPage.numSystems-1;
    if (endSys>=renderedPages.systems.size())
      endSys=renderedPages.systems.size()-1;
    float spaceBetweenSystems=curPage.numSystems<=1 ?
      0 :
      (PP.DRAWINGSPACEY-curPage.numStaves*PP.STAFFYSPACE)/(curPage.numSystems-(curPageNum==0 ? 0 : 1));
    if (curPage.numSystems<3)
      spaceBetweenSystems/=2;

    float curY=PP.PAGEYSIZE-PP.YMUSICSTART-(curPageNum==0 ? spaceBetweenSystems : 0);
    for (int curSys=startSys; curSys<=endSys; curSys++)
      {
        drawSystem(curSys,curY,renderedPages,cb);
        curY-=renderedPages.systems.get(curSys).numVoices*PP.STAFFYSPACE+spaceBetweenSystems;
      }
  }

/*------------------------------------------------------------------------
Method:  void draw[ScoreTitle|Footer](PdfContentByte cb)
Purpose: Write text information on first page
Parameters:
  Input:  -
  Output: PdfContentByte cb - PDF output structure allowing spacial random
                              access
  Return: -
------------------------------------------------------------------------*/

  void drawScoreTitle(PdfContentByte cb)
  {
    float baseY=PP.PAGEYSIZE-PP.YMARGIN,
          subtextYadd=TitleFont.getAscentPoint(musicData.getTitle(),PP.TitleFONTSIZE)*1.5f;

    cb.setFontAndSize(TitleFont,PP.TitleFONTSIZE);

    /* piece title */
    cb.showTextAligned(PdfContentByte.ALIGN_CENTER,
                       musicData.getTitle(),
                       PP.PAGEXSIZE/2,baseY,0);

    /* section title */
    if (musicData.getSectionTitle()!=null)
      {
        cb.setFontAndSize(SubtitleFont,PP.SubtitleFONTSIZE);
        cb.showTextAligned(PdfContentByte.ALIGN_CENTER,
                           musicData.getSectionTitle(),
                           PP.PAGEXSIZE/2,baseY-subtextYadd,0);
      }

    /* composer */
    cb.setFontAndSize(SubtitleFont,PP.SubtitleFONTSIZE*0.4f);
    cb.showTextAligned(PdfContentByte.ALIGN_RIGHT,
                       musicData.getComposer(),
                       PP.PAGEXSIZE-PP.XMARGIN,baseY-subtextYadd*1.2f,0);

    /* editor */
    String editor=musicData.getEditor();
    if (editor!=null && !editor.isEmpty())
      {
        cb.setFontAndSize(SubtitleFont,PP.SubtitleFONTSIZE*0.4f);
        cb.showTextAligned(PdfContentByte.ALIGN_LEFT,
                           "Edited by "+editor,
                           PP.XMARGIN,baseY-subtextYadd*1.2f,0);
      }
  }

  void drawFooter(PdfContentByte cb)
  {
    cb.setFontAndSize(PlainFont,PP.PlainFONTSIZE);
    cb.showTextAligned(PdfContentByte.ALIGN_RIGHT,
                       CMME_PDF_INFO_STRING,
                       PP.PAGEXSIZE-PP.XMARGIN,PP.YMARGIN,0);
  }

/*------------------------------------------------------------------------
Method:  void drawSystem(int sysNum,float starty,ScorePageRenderer renderedPages,PdfContentByte cb)
Purpose: Draw one system in score layout
Parameters:
  Input:  int sysNum                 - index of system to draw
          float starty               - top y coordinate for system
          ScorePageRenderer renderer - event rendering information for all voices
                                       (multi-page score layout)
          PdfContentByte cb          - PDF output structure allowing spacial random
                                       access
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawSystem(int sysNum,float starty,ScorePageRenderer renderedPages,PdfContentByte cb)
  {
    RenderedStaffSystem curSystem=renderedPages.systems.get(sysNum);
    int                 rendererNum=ScoreRenderer.calcRendererNum(renderedPages.scoreData,curSystem.startMeasure);
    ScoreRenderer       curRenderer=renderedPages.scoreData[rendererNum];
    MeasureInfo         leftMeasure=curRenderer.getMeasure(curSystem.startMeasure);

    XEVENTSPACE_SCALE=(PP.STAFFXSIZE-PP.LINEXADJUST)/ScorePagePreviewWin.STAFFXSIZE;//PP.STAFFXSIZE/ScorePagePreviewWin.STAFFXSIZE;
    float clefInfoSize=renderedPages.calcLeftInfoSize(curSystem.startMeasure)*XEVENTSPACE_SCALE,
          spacingCoeff=(float)curSystem.spacingCoefficient;

    /* draw left-side system line and all barlines */
    cb.setLineWidth(PP.STAFFLINEWIDTH);
    cb.moveTo(PP.XMARGIN+curSystem.leftX*XEVENTSPACE_SCALE,starty);
    cb.lineTo(PP.XMARGIN+curSystem.leftX*XEVENTSPACE_SCALE,starty-(curSystem.numVoices-1)*PP.STAFFYSPACE-4*PP.STAFFYSCALE);
    cb.stroke();
    drawSystemBarlines(cb,renderedPages,PP.XMARGIN+curSystem.leftX*XEVENTSPACE_SCALE+clefInfoSize,starty,curSystem);

    cury=starty;
    for (int v=0; v<numVoices; v++)
      if (curRenderer.eventinfo[v]!=null)
      {
        if (curSystem.displayVoiceNames)
          {
            cb.beginText();
            cb.setFontAndSize(StaffNameFont,PP.StaffNameFONTSIZE);
            cb.showTextAligned(PdfContentByte.ALIGN_RIGHT,
                               musicData.getVoiceData()[v].getStaffTitle()+"   ",
                               PP.XMARGIN+curSystem.leftX*XEVENTSPACE_SCALE,cury-PP.STAFFYPOSSCALE*5,0);
            cb.endText();
          }

        drawStaff(cb,cury,5,curSystem.leftX*XEVENTSPACE_SCALE,curSystem.rightX*XEVENTSPACE_SCALE+PP.LINEXADJUST);

        cb.beginText();
        cb.setFontAndSize(CMMEBaseFont,PP.MusicFONTSIZE);

/*        if (sysNum>0)
          drawClefInfo(renderedPages,v,curSystem.startMeasure,
                       PP.XMARGIN+PP.LINEXADJUST+curSystem.leftX*XEVENTSPACE_SCALE,cury,cb);*/
        float VclefInfoSize=clefInfoSize;
        if (!leftMeasure.beginsWithClef(v) &&
            (curRenderer.getStartingParams()[v].clefSet!=null ||
             curSystem.startMeasure>curRenderer.getFirstMeasureNum()))
          drawClefInfo(curRenderer,leftMeasure,v,
                       PP.XMARGIN+PP.LINEXADJUST+curSystem.leftX*XEVENTSPACE_SCALE,cury,cb);
        else
          VclefInfoSize=0;

        /* calculate which events go on each staff */
        int leftei=curRenderer.getMeasure(curSystem.startMeasure).reventindex[v],
            rightei=renderedPages.getLastEventInMeasure(sysNum,rendererNum,v,curSystem.endMeasure);

        /* now draw events */
        RenderedEvent    re=null;
        RenderedLigature ligInfo=null,
                         tieInfo=null;

        for (int ei=leftei; ei<=rightei; ei++)
          {
            re=curRenderer.getEvent(v,ei);
            float xloc=PP.XMARGIN+PP.LINEXADJUST+curSystem.leftX*XEVENTSPACE_SCALE+
                       VclefInfoSize+(float)re.getxloc()*spacingCoeff*XEVENTSPACE_SCALE;

            if (ei==0 && sysNum==0)
              xloc=PP.XMARGIN+PP.LINEXADJUST+curSystem.leftX*XEVENTSPACE_SCALE; // TMP

            if (re.isdisplayed())
              drawEvent(re,xloc,cury,cb);

            /* draw ligatures */
            ligInfo=re.getLigInfo();
            if (re.isligend() && musicData.getSection(rendererNum).getSectionType()==MusicSection.MENSURAL_MUSIC)
              {
                float ligLeftX=ligInfo.firstEventNum<leftei ? PP.XMARGIN-1 :
                  PP.XMARGIN+PP.LINEXADJUST+curSystem.leftX*XEVENTSPACE_SCALE+
                  VclefInfoSize+(float)curRenderer.eventinfo[v].getEvent(ligInfo.firstEventNum).getxloc()*spacingCoeff*XEVENTSPACE_SCALE;
                drawLigature(ligLeftX,xloc,cury+calcLigY(v,re),PP.XMARGIN+PP.LINEXADJUST+clefInfoSize,PP.XMARGIN+PP.STAFFXSIZE,
                             renderedPages.options,cb);
              }

            /* tie notes */
            tieInfo=re.getTieInfo();
            if (tieInfo.firstEventNum!=-1 && tieInfo.lastEventNum==ei)
              {
                RenderedEvent tre1=curRenderer.eventinfo[v].getEvent(tieInfo.firstEventNum);
                float tieLeftX=tieInfo.firstEventNum<leftei ? PP.XMARGIN-1 :
                  PP.XMARGIN+PP.LINEXADJUST+curSystem.leftX*XEVENTSPACE_SCALE+
                  VclefInfoSize+(float)curRenderer.eventinfo[v].getEvent(tieInfo.firstEventNum).getxloc()*spacingCoeff*XEVENTSPACE_SCALE;

                drawTies(tre1,re,v,
                         tieLeftX,xloc,cury,PP.XMARGIN+PP.LINEXADJUST+clefInfoSize,PP.XMARGIN+PP.STAFFXSIZE,
                         renderedPages.options,cb);
              }

            /* some more clef spacing adjustment, for staves beginning with new clefs */
            if (ei==leftMeasure.lastBeginClefIndex[v] && ei<rightei)
              {
/*                float nextX=calcXLoc(curSystem,VclefInfoSize,
                                   curRenderer.eventinfo[v].getEvent(ei+1))-
                          XMARGIN-curSystem.leftX;
                if (clefInfoSize>nextX)*/
                  VclefInfoSize=clefInfoSize;
              }
          }

        /* finish any unclosed ligature */
        ligInfo=re==null ? null : re.getLigInfo();
        if (ligInfo!=null && !re.isligend() && ligInfo.firstEventNum!=-1)
          {
            float ligLeftX=ligInfo.firstEventNum<leftei ? PP.XMARGIN-1 :
              PP.XMARGIN+PP.LINEXADJUST+curSystem.leftX*XEVENTSPACE_SCALE+
              VclefInfoSize+(float)curRenderer.eventinfo[v].getEvent(ligInfo.firstEventNum).getxloc()*spacingCoeff*XEVENTSPACE_SCALE;
            drawLigature(ligLeftX,PP.XMARGIN+PP.STAFFXSIZE+1,cury+calcLigY(v,re),PP.XMARGIN+PP.LINEXADJUST+clefInfoSize,PP.XMARGIN+PP.STAFFXSIZE,
                         renderedPages.options,cb);
          }

        /* finish any unclosed tie */
        tieInfo=re==null ? null : re.getTieInfo();
        if (tieInfo!=null && tieInfo.firstEventNum!=-1 &&
            (tieInfo.lastEventNum!=rightei || re.doubleTied()))
          {
            RenderedEvent tre1=re.doubleTied() ? re : curRenderer.eventinfo[v].getEvent(tieInfo.firstEventNum);
            int firstEventNum=re.doubleTied() ? rightei : tieInfo.firstEventNum;
            float tieLeftX=firstEventNum<leftei ? PP.XMARGIN-1 :
              PP.XMARGIN+PP.LINEXADJUST+curSystem.leftX*XEVENTSPACE_SCALE+
              VclefInfoSize+(float)tre1.getxloc()*spacingCoeff*XEVENTSPACE_SCALE;

            drawTies(tre1,curRenderer.eventinfo[v].getEvent(tieInfo.lastEventNum),v,
                     tieLeftX,PP.XMARGIN+PP.STAFFXSIZE+1,cury,PP.XMARGIN+PP.LINEXADJUST+clefInfoSize,PP.XMARGIN+PP.STAFFXSIZE,
                     renderedPages.options,cb);
          }

        cb.endText();

        cury-=PP.STAFFYSPACE;
      }
  }

/*------------------------------------------------------------------------
Method:  void drawEvent(RenderedEvent e,float curx,float cury,[boolean checkDisp,]PdfContentByte cb)
Purpose: Draw one rendered event (or MultiEvent)
Parameters:
  Input:  RenderedEvent e   - event to draw
          float curx,cury   - x/y position
          boolean checkDisp - whether to check event's "display" flag before drawing
          PdfContentByte cb - PDF output structure allowing spacial random
                              access
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawEvent(RenderedEvent e,float curx,float cury,PdfContentByte cb)
  {
    drawEvent(e,curx,cury,true,cb);
  }

  void drawEvent(RenderedEvent e,float curx,float cury,boolean checkDisp,PdfContentByte cb)
  {
    /* multi-event */
    if (e.multiEventList!=null)
      {
        /* loop through events */
        for (Iterator i=e.multiEventList.iterator(); i.hasNext();)
          drawEvent((RenderedEvent)(i.next()),curx,cury,cb);
        return;
      }

    /* single event */
    if (e.getEvent().geteventtype()==Event.EVENT_NOTE)
      lastNoteX=curx;

    if (e.isdisplayed() || !checkDisp)
      /* draw all images for one event */
      for (Iterator imgi=e.getimgs().iterator(); imgi.hasNext();)
        {
          EventImg evimg=(EventImg)(imgi.next());

          if (evimg instanceof EventGlyphImg)
            {
              EventGlyphImg evgimg=(EventGlyphImg)evimg;
              drawGlyph(evgimg.imgnum,curx,cury,
                        evgimg.UNSCALEDxoff,evgimg.UNSCALEDyoff,
                        evgimg.staffypos,
                        cb);
            }

          else if (evimg instanceof EventShapeImg)
            {
              cb.endText();
              cb.setLineWidth(PP.STEMWIDTH);

              EventShapeImg evsimg=(EventShapeImg)evimg;
              float basex=curx+PP.LINEXADJUST,basey=cury-PP.STAFFYSIZE+evimg.staffypos*PP.STAFFYPOSSCALE;
              cb.moveTo(basex+evsimg.printshapex[0]*PP.XYSCALE,basey+evsimg.printshapey[0]*PP.XYSCALE);

              for (int psi=1; psi<evsimg.printshapex.length; psi++)
                {
                  if (evsimg.multipleypos && psi>=evsimg.yswitchnum)
                    basey=cury-PP.STAFFYSIZE+evsimg.staffypos2*PP.STAFFYPOSSCALE;
                  cb.lineTo(basex+evsimg.printshapex[psi]*PP.XYSCALE,basey+evsimg.printshapey[psi]*PP.XYSCALE);
                }

              if (evsimg.filled)
                cb.closePathFillStroke();
              else
                cb.closePathStroke();

              cb.beginText();
              cb.setFontAndSize(CMMEBaseFont,PP.MusicFONTSIZE);
            }

          else if (evimg instanceof EventStringImg)
            {
              EventStringImg evsimg=(EventStringImg)evimg;
              double         basex=curx+evsimg.UNSCALEDxoff*PP.XYSCALE,
                             basey=cury-PP.STAFFYSIZE+evsimg.staffypos*PP.STAFFYPOSSCALE+evsimg.UNSCALEDyoff*PP.XYSCALE;

              /* display string without special symbols */
              if (e.getEvent().geteventtype()==Event.EVENT_NOTE)
                cb.setFontAndSize(evsimg.fontStyle==java.awt.Font.ITALIC ? 
                                    TextItalicFont : TextFont,
                                  PP.TextFONTSIZE);
              else
                cb.setFontAndSize(ScoreAnnotationFont,PP.ScoreAnnotationFONTSIZE);
              cb.setTextMatrix((float)basex,(float)basey);
              cb.showText(evsimg.imgtextWithoutSymbols);

              /* add symbols */
              cb.setFontAndSize(CMMEBaseFont,PP.MusicFONTSIZE);
              for (Iterator sii=evsimg.specialImages.iterator(); sii.hasNext();)
                {
                  EventGlyphImg sei=(EventGlyphImg)(sii.next());
                  cb.setTextMatrix((float)(basex+sei.UNSCALEDxoff*PP.XYSCALE),
                                   (float)(basey+sei.UNSCALEDyoff*PP.XYSCALE));
                  cb.showText(String.valueOf((char)(MusicFont.PIC_OFFSET+sei.imgnum)));
                }
            }
        }
  }

  public void drawGlyph(int glyphNum,float curx,float cury,
                        double UNSCALEDxoff,double UNSCALEDyoff,
                        int staffYPos,
                        PdfContentByte cb)
  {
    cb.setTextMatrix((float)(curx+UNSCALEDxoff*PP.XYSCALE),
                     (float)(cury-PP.STAFFYSIZE+staffYPos*PP.STAFFYPOSSCALE+UNSCALEDyoff*PP.XYSCALE));
    cb.showText(String.valueOf((char)(MusicFont.PIC_OFFSET+glyphNum)));
  }

  public void drawGlyph(char glyphNum,float curx,float cury,
                        double UNSCALEDxoff,double UNSCALEDyoff,
                        int staffYPos,
                        PdfContentByte cb)
  {
    drawGlyph((int)glyphNum,curx,cury,
              UNSCALEDxoff,UNSCALEDyoff,
              staffYPos,cb);
  }

/*------------------------------------------------------------------------
Method:  void drawClefInfo(ScorePageRenderer renderedPages,int vnum,int mnum,
                           float xloc,float yloc,PdfContentByte cb)
Purpose: Draw clefs at left side of staff
Parameters:
  Input:  ScorePageRenderer renderedPages - event rendering information for all voices
                                            (multi-page score layout)
          int vnum                        - voice number
          int mnum                        - measure number
          int xloc,yloc                   - starting coordinates for drawing
          PdfContentByte cb               - PDF output structure allowing spacial random
                                            access
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawClefInfo(ScoreRenderer renderer,MeasureInfo leftMeasure,
                    int vnum,float xloc,float yloc,PdfContentByte cb)
  {
    int leftEventIndex=leftMeasure.reventindex[vnum];
    boolean useModernAccSystem=renderedScore.options.getUseModernAccidentalSystem();

    /* draw clefs */
    RenderedClefSet leftCS=renderer.eventinfo[vnum].getClefEvents(leftEventIndex);
    if (leftCS!=null)
      xloc+=leftCS.draw(useModernAccSystem,this,cb,xloc,yloc);

    /* modern key signature */
    ModernKeySignature mk=renderer.eventinfo[vnum].getModernKeySig(leftEventIndex);
    if (mk.numEls()>0 && leftCS!=null && useModernAccSystem)
      xloc+=ViewCanvas.drawModKeySig(
        this,cb,mk,leftCS.getPrincipalClefEvent(),xloc,yloc);
  }

/*------------------------------------------------------------------------
Method:  float calcLigY(int vnum,RenderedEvent e)
Purpose: Calculate y position of a ligature at a given event (relative to
         staff)
Parameters:
  Input:  int vnum        - voice number
          RenderedEvent e - event
  Output: -
  Return: y position of ligature
------------------------------------------------------------------------*/

  float calcLigY(int vnum,RenderedEvent e)
  {
    RenderedLigature ligInfo=e.getLigInfo();
    RenderedEvent    lige=ligInfo.reventList.getEvent(ligInfo.yMaxEventNum);
    Clef             ligevclef=lige.getClef();
    Event            lignoteev=ligInfo.yMaxEvent;

    float ligyval=0-PP.STAFFYSCALE*4+PP.STAFFYPOSSCALE*2+
                  PP.STAFFYPOSSCALE*lignoteev.getPitch().calcypos(ligevclef);
    if (ligyval<PP.STAFFYSCALE*.7f)
      ligyval=PP.STAFFYSCALE*.7f;

    return ligyval;
  }

  float calcTieY(int vnum,RenderedEvent e)
  {
    RenderedLigature tieInfo=e.getTieInfo();
    RenderedEvent    tieRE=e;//tieInfo.reventList.getEvent(tieInfo.yMaxEventNum);
    Clef             tieREclef=tieRE.getClef();
    DataStruct.Event tieNoteEv=e.getEvent();//tieInfo.yMaxEvent;

    return 0-PP.STAFFYSCALE*4+//PP.STAFFYPOSSCALE*2+
           PP.STAFFYPOSSCALE*tieNoteEv.getPitch().calcypos(tieREclef);
  }

/*------------------------------------------------------------------------
Method:  void drawLigature(float x1,float x2,float y,float leftx,float rightx)
Purpose: Draw ligature bracket for one voice
Parameters:
  Input:  float x1,x2            - left and right coordinates of bracket
          float y                - y level of bracket
          float leftx,rightx     - horizontal bounds of drawing space
          OptionSet musicOptions - drawing/rendering options
          PdfContentByte cb      - graphical context in PDF
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawLigature(float x1,float x2,float y,float leftx,float rightx,
                    OptionSet musicOptions,PdfContentByte cb)
  {
    if (musicOptions.get_displayligbrackets())
      drawLigOnCanvas(x1,x2,y,leftx,rightx,cb);
  }

  void drawLigOnCanvas(float x1,float x2,float y,float leftx,float rightx,PdfContentByte cb)
  {
    cb.endText();
    cb.setLineWidth(PP.STAFFLINEWIDTH);

    /* left bracket end */
    if (x1>=leftx)
      {
        cb.moveTo(x1,y);
        cb.lineTo(x1,y-PP.STAFFYSCALE*.3f);
      }
    else
      x1=leftx;

    /* right bracket end */
    x2+=MusicFont.CONNECTION_LIG_RECTA*PP.XYSCALE*1.5;
    if (x2<rightx)
      {
        cb.moveTo(x2,y);
        cb.lineTo(x2,y-PP.STAFFYSCALE*.3f);
      }
    else
      x2=rightx;

    /* main bracket line */
    cb.moveTo(x1,y);
    cb.lineTo(x2,y);

    cb.stroke();
    cb.beginText();
    cb.setFontAndSize(CMMEBaseFont,PP.MusicFONTSIZE);
  }

  void drawTies(RenderedEvent tre1,RenderedEvent tre2,int vi,
                float x1,float x2,float cury,float leftx,float rightx,
                OptionSet musicOptions,PdfContentByte cb)
  {
    cb.endText();
    cb.setLineWidth(PP.STAFFLINEWIDTH);

    List<RenderedEvent> multiEventList=tre1.getEventList();
    if (multiEventList==null)
      drawTie(tre1.getTieType(),x1,x2,cury+calcTieY(vi,tre2),leftx,rightx,
              musicOptions,cb);
    else
      for (RenderedEvent re : multiEventList)
        {
          int tieType=re.getTieType();
          if (tieType!=NoteEvent.TIE_NONE)
            drawTie(tieType,x1,x2,cury+calcTieY(vi,re),leftx,rightx,
                    musicOptions,cb);
        }

    cb.beginText();
    cb.setFontAndSize(CMMEBaseFont,PP.MusicFONTSIZE);
  }

  void drawTie(int tieType,float x1,float x2,float y,float leftx,float rightx,
               OptionSet musicOptions,PdfContentByte cb)
  {
    double xAdjust=MusicFont.getDefaultPrintGlyphWidth(MusicFont.PIC_NOTESTART+NoteEvent.NOTEHEADSTYLE_SEMIBREVE)*PP.XYSCALE;
    x1=(float)Math.max(x1+xAdjust,leftx-xAdjust/2);
    x2+=4*MusicFont.SCREEN_TO_GLYPH_FACTOR*PP.XYSCALE;

    float arc1=0,arc2=180;
    if (tieType==NoteEvent.TIE_UNDER)
      {
        arc1=180;
        y-=MusicFont.CONNECTION_L_UPSTEMY*3*PP.XYSCALE;
      }

    cb.arc(x1,y,x2,(float)(y+MusicFont.CONNECTION_L_UPSTEMY*3*PP.XYSCALE),
           arc1,arc2);

    cb.stroke();
  }

/*------------------------------------------------------------------------
Method:  void drawStaff(PdfContentByte cb,float yloc,int numlines,float leftX,float rightX)
Purpose: Draw staff at specified location
Parameters:
  Input:  PdfContentByte cb  - graphical context in PDF
          float yloc         - y location for top of staff
          int numlines       - number of lines for staff
          float leftX,rightX - horizontal bounds of staff
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawStaff(PdfContentByte cb,float yloc,int numlines,float leftX,float rightX)
  {
    cb.setLineWidth(PP.STAFFLINEWIDTH);
    for (int i=0; i<numlines; i++)
      {
        cb.moveTo(PP.XMARGIN+leftX,yloc-i*PP.STAFFYSCALE);
        cb.lineTo(PP.XMARGIN+rightX,yloc-i*PP.STAFFYSCALE);
      }
    cb.stroke();
  }

  void drawStaff(PdfContentByte cb,float yloc,int numlines)
  {
    drawStaff(cb,yloc,numlines,0,PP.STAFFXSIZE);
  }

/*------------------------------------------------------------------------
Method:  void drawSystemBarlines(PdfContentByte cb,ScorePageRenderer renderedPages,
                                 float xloc,float yloc,
                                 RenderedStaffSystem curSystem)
Purpose: Draw barlines across one system
Parameters:
  Input:  PdfContentByte cb               - PDF output structure allowing spacial random
                                            access
          ScorePageRenderer renderedPages - event rendering information for all voices
                                            (multi-page score layout)
          float xloc,yloc                 - location for top left of first staff
          RenderedStaffSystem curSystem   - staff system information
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawSystemBarlines(PdfContentByte cb,ScorePageRenderer renderedPages,
                          float xloc,float yloc,
                          RenderedStaffSystem curSystem)
  {
    float curx=PP.LINEXADJUST;
    int   rendererNum=ScoreRenderer.calcRendererNum(renderedPages.scoreData,curSystem.startMeasure);

    /* measure number */
    if (curSystem.startMeasure>0)
      {
        cb.beginText();
        cb.setFontAndSize(PlainFont,PP.PlainFONTSIZE);
        cb.setTextMatrix(xloc-PP.STAFFYSCALE,yloc+PP.STAFFYSCALE*3);
        cb.showText(String.valueOf(curSystem.startMeasure+1));
        cb.endText();
      }

    for (int i=curSystem.startMeasure; i<curSystem.endMeasure; i++)
      {
        curx+=renderedPages.scoreData[rendererNum].getMeasure(i).xlength*XEVENTSPACE_SCALE;
        drawBarlines(cb,renderedPages.options,xloc+(float)(curx*curSystem.spacingCoefficient),yloc,curSystem.numVoices);
      }
    if (curSystem.endMeasure<renderedPages.scoreData[rendererNum].getLastMeasureNum())
      drawBarlines(cb,renderedPages.options,PP.XMARGIN+PP.LINEXADJUST+curSystem.rightX*XEVENTSPACE_SCALE,yloc,curSystem.numVoices);
    else
      {
        /* final barline */
        cb.moveTo(PP.XMARGIN+PP.LINEXADJUST+curSystem.rightX*XEVENTSPACE_SCALE,yloc);
        cb.lineTo(PP.XMARGIN+PP.LINEXADJUST+curSystem.rightX*XEVENTSPACE_SCALE,yloc-(curSystem.numVoices-1)*PP.STAFFYSPACE-PP.STAFFYSCALE*4);
      }
  }

/*------------------------------------------------------------------------
Method:  void drawBarlines(PdfContentByte cb,OptionSet musicOptions,float xloc,float yloc)
Purpose: Draw barlines at specified location
Parameters:
  Input:  PdfContentByte cb      - PDF output structure allowing spacial random
                                   access
          OptionSet musicOptions - drawing/rendering options
          float xloc,yloc        - location for barlines
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void drawBarlines(PdfContentByte cb,OptionSet musicOptions,float xloc,float yloc,int numVoices)
  {
    switch (musicOptions.get_barline_type())
      {
        case OptionSet.OPT_BARLINE_NONE:
          break;
        case OptionSet.OPT_BARLINE_MENSS:
          for (int i=0; i<numVoices-1; i++)
            {
              cb.moveTo(xloc,yloc-PP.STAFFYSCALE*4-i*PP.STAFFYSPACE);
              cb.lineTo(xloc,yloc-(i+1)*PP.STAFFYSPACE);
            }
          cb.stroke();
          break;
        case OptionSet.OPT_BARLINE_TICK:
          for (int i=0; i<numVoices; i++)
	    {
              cb.moveTo(xloc,yloc+PP.STAFFYPOSSCALE-i*PP.STAFFYSPACE);
              cb.lineTo(xloc,yloc-i*PP.STAFFYSPACE);
              cb.moveTo(xloc,yloc-PP.STAFFYPOSSCALE-PP.STAFFYSCALE*4-i*PP.STAFFYSPACE);
              cb.lineTo(xloc,yloc-PP.STAFFYSCALE*4-i*PP.STAFFYSPACE);
	    }
          cb.stroke();
          break;
        case OptionSet.OPT_BARLINE_MODERN:
          for (int i=0; i<numVoices; i++)
            {
              cb.moveTo(xloc,yloc-i*PP.STAFFYSPACE);
              cb.lineTo(xloc,yloc-PP.STAFFYSCALE*4-i*PP.STAFFYSPACE);
            }
          cb.stroke();
          break;
      }
  }
}
