/*----------------------------------------------------------------------*/
/*

        Module          : ScorePageRenderer

        Package         : Gfx

        Classes Included: ScorePageRenderer

        Purpose         : Render score in page layout

        Programmer      : Ted Dumitrescu

        Date Started    : 5/25/06

Updates:
8/2/06:  moved page and staff system parameters to separate objects
         (RenderedScorePage and RenderedStaffSystem)
8/29/07: added support for multiple-section scores

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;
import java.awt.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   ScorePageRenderer
Extends: -
Purpose: Renders voice parts as score in page-based (printable) layout
------------------------------------------------------------------------*/

public class ScorePageRenderer
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static int STAFFSCALE,
             CANVASYSCALE; /* amount of vertical space per staff */

/*----------------------------------------------------------------------*/
/* Instance variables */

  OptionSet options;
  Dimension canvasSize;
  int       numVoices;

  public PieceData                      musicData;
  public ScoreRenderer[]                scoreData;
  public int                            systemsPerPage;
  public ArrayList<RenderedStaffSystem> systems;
  public ArrayList<RenderedScorePage>   pages;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ScorePageRenderer(PieceData p,OptionSet o,Dimension d,
                               int STAFFSCALE,int CANVASYSCALE)
Purpose:     Initialize renderer
Parameters:
  Input:  PieceData p                 - music data
          OptionSet o                 - display options
          Dimension d                 - size of drawing block
          int STAFFSCALE,CANVASYSCALE - drawing space parameters
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ScorePageRenderer(PieceData p,OptionSet o,Dimension d,
                           int STAFFSCALE,int CANVASYSCALE)
  {
    this.STAFFSCALE=STAFFSCALE;
    this.CANVASYSCALE=CANVASYSCALE;
    musicData=p;
    options=o;
    canvasSize=d;
    numVoices=musicData.getVoiceData().length;

    /* initialize voice parameters */
    RenderedSectionParams[] sectionParams=new RenderedSectionParams[numVoices];
    for (int i=0; i<numVoices; i++)
      sectionParams[i]=new RenderedSectionParams();

    /* render, first into continuous score, then into page structure */
    int    numSections=musicData.getNumSections(),
           numMeasures=0;
    double startX=0;
    scoreData=new ScoreRenderer[numSections];
    for (int i=0; i<numSections; i++)
      {
        scoreData[i]=new ScoreRenderer(i,musicData.getSection(i),musicData,
                                       sectionParams,
                                       options,numMeasures,startX);
        sectionParams=scoreData[i].getEndingParams();
        numMeasures+=scoreData[i].getNumMeasures();
        startX+=scoreData[i].getXsize();
      }

    renderPages(scoreData);
  }

/*------------------------------------------------------------------------
Method:  void renderPages(ScoreRenderer[] scoreData)
Purpose: Render scored music into page-layout structure
Parameters:
  Input:  ScoreRenderer[] scoreData - rendered event/measure info
  Output: -
  Return: -
------------------------------------------------------------------------*/

  RenderedScorePage curPage;

  void renderPages(ScoreRenderer[] scoreData)
  {
    /* initialize parameters */
//    int spacePerSystem=(int)(((double)numVoices+.5)*CANVASYSCALE);
//    systemsPerPage=canvasSize.height/spacePerSystem;

    systems=new ArrayList<RenderedStaffSystem>();
    pages=new ArrayList<RenderedScorePage>();

    int numSystems=0;
    curPage=new RenderedScorePage(numSystems,0);
    for (ScoreRenderer rs : scoreData)
      {
        numSystems+=renderSection(rs,numSystems);
      }
    pages.add(curPage);
  }

  int calcSystemSpace(int numv)
  {
    return (int)(((double)numv+.5)*CANVASYSCALE);
  }

  /* new page? */
  void checkAndAddPage(int spacePerSystem,int curSystemNum)
  {
    if (curPage.ySpace+spacePerSystem>canvasSize.height)
      {
        pages.add(curPage);
        curPage=new RenderedScorePage(curSystemNum,0);
      }
  }

  int renderSection(ScoreRenderer curSection,int startSystemNum)
  {
    /* add measures to page layout one at a time, adding systems and pages
       when necessary */
    boolean             displayVoiceNames=startSystemNum==0 || curSection.newVoiceArrangement();
    MeasureInfo         curMeasure;
    double              leftInfoSize=calcLeftInfoSize(curSection.getFirstMeasureNum()),
                        systemStartx=leftInfoSize+
                          (displayVoiceNames ? calcVoiceNamesIndent() : 0),
                        curx=systemStartx,leftx=0;
    int                 curSystemNum=startSystemNum,
                        numSectionVoices=curSection.getSectionData().getNumVoicesUsed(),//curSection.getNumVoices(),
                        spacePerSystem=calcSystemSpace(numSectionVoices);
    RenderedStaffSystem curSystem=new RenderedStaffSystem(curSection.getFirstMeasureNum(),
                                                          displayVoiceNames ? (int)(systemStartx-leftInfoSize) : 0,
                                                          canvasSize.width-1,0,
                                                          numSectionVoices,displayVoiceNames);

    checkAndAddPage(spacePerSystem,curSystemNum);

    for (int m=0; m<curSection.measures.size(); m++)
      {
        curMeasure=curSection.measures.get(m);
        if (m>0 && curx+curMeasure.xlength>canvasSize.width)
          {
            /* finish current system */
            curSystem.endMeasure=m-1+curSection.getFirstMeasureNum();
            curSystem.spacingCoefficient=(curSystem.rightX-systemStartx-4)/(curx-systemStartx);
//System.out.println("system "+curSystemNum+" co="+curSystem.spacingCoefficient);
//System.out.println(" startx="+systemStartx+" rightx="+curSystem.rightX+" curx="+curx);
            systems.add(curSystem);
            curPage.ySpace+=spacePerSystem;
            curPage.numStaves+=numSectionVoices;
            curPage.numSystems++;

            /* new system */
            curSystemNum++;
            curSystem=new RenderedStaffSystem(m+curSection.getFirstMeasureNum(),
                                              0,canvasSize.width-1,curPage.ySpace,
                                              numSectionVoices,false);
            curx=systemStartx=calcLeftInfoSize(m+curSection.getFirstMeasureNum());
            leftx=curMeasure.leftx;

            checkAndAddPage(spacePerSystem,curSystemNum);
          }
        curx+=curMeasure.xlength;
        curSection.adjustMeasureEventPositions(m,0-leftx);
      }
    curSystem.endMeasure=curSection.measures.size()-1+curSection.getFirstMeasureNum();
    curSystem.rightX=(int)curx;
    systems.add(curSystem);
    curPage.ySpace+=spacePerSystem;
    curPage.numStaves+=numSectionVoices;
    curPage.numSystems++;

    return curSystemNum-startSystemNum+1;
  }

/*------------------------------------------------------------------------
Method:  int calcVoiceNamesIndent()
Purpose: Calculate amount of space taken by voice names before first system
Parameters:
  Input:  -
  Output: -
  Return: x-space required by voice names+space before first system
------------------------------------------------------------------------*/

  int calcVoiceNamesIndent()
  {
    Font tmpFont=new Font(null,Font.PLAIN,15);
    EventStringImg.genericG.setFont(tmpFont);
    FontMetrics metrics=EventStringImg.genericG.getFontMetrics();
    int maxSize=0,curSize;

    for (Voice v : musicData.getVoiceData())
      {
        curSize=metrics.stringWidth(v.getStaffTitle());
        if (curSize>maxSize)
          maxSize=curSize;
      }

    return maxSize+10;
  }

/*------------------------------------------------------------------------
Method:  int calcLeftInfoSize(int mnum)
Purpose: Calculate amount of space taken by clef info (+possibly mensuration
         info) if the current measure is at a system start
Parameters:
  Input:  int mnum                - measure number
  Output: -
  Return: x-space required by clef/signature for all voices
------------------------------------------------------------------------*/

  public int calcLeftInfoSize(int mnum)
  {
    if (mnum==0)
      return 0;

    ScoreRenderer renderedSection=scoreData[ScoreRenderer.calcRendererNum(scoreData,mnum)];

    MeasureInfo leftMeasure=renderedSection.measures.getMeasure(mnum-renderedSection.getFirstMeasureNum());
    double      xloc,maxx=0;
    for (int i=0; i<numVoices; i++)
      if (renderedSection.eventinfo[i]!=null)
      {
        xloc=0;

        if (!leftMeasure.beginsWithClef(i))
          {
            RenderedClefSet leftCS=renderedSection.eventinfo[i].getClefEvents(leftMeasure.reventindex[i]);
            if (leftCS!=null)
              xloc+=leftCS.getXSize();
          }
        else
          {
            RenderedEvent re=renderedSection.eventinfo[i].getEvent(
              leftMeasure.lastBeginClefIndex[i]);
            xloc=re.getxend()-leftMeasure.leftx;
          }

        if (xloc>maxx)
          maxx=xloc;
      }
    return Math.round((float)maxx+5);
  }

  int getLastEventInMeasure(int sysNum,int rendererNum,int vnum,int mnum)
  {
    ScoreRenderer renderer=this.scoreData[rendererNum];
    if (//sysNum<this.systems.size()-1 &&
        mnum<renderer.getLastMeasureNum())
      return renderer.getMeasure(mnum+1).reventindex[vnum]-1;
    else
      return renderer.eventinfo[vnum].size()-1;
  }
}

