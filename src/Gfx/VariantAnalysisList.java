/*----------------------------------------------------------------------*/
/*

        Module          : VariantAnalysisList.java

        Package         : Gfx

        Classes Included: VariantAnalysisList

        Purpose         : Information about variants in a composition,
                          to be used for source-relation analysis or
                          critical notes

        Programmer      : Ted Dumitrescu

        Date Started    : 11/1/2009 (parts moved from CriticalNotesWindow)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   VariantAnalysisList
Extends: ArrayList<VariantReport>
Purpose: Information about variants in a composition
------------------------------------------------------------------------*/

public class VariantAnalysisList extends ArrayList<VariantReport>
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  ScoreRenderer[] renderedSections;
  PieceData       musicData;
  MusicWin        parentWin;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantAnalysisList(PieceData musicData,ScoreRenderer[] renderedSections)
Purpose:     Initialize structure
Parameters:
  Input:  PieceData musicData - complete music data including all variant
                                versions
          MusicWin parentWin  - parent window
  Output: -
------------------------------------------------------------------------*/

  public VariantAnalysisList(PieceData musicData,MusicWin parentWin)
  {
    this(musicData,ScoreRenderer.renderSections(musicData,parentWin.optSet));
  }

  public VariantAnalysisList(PieceData musicData,ScoreRenderer[] renderedSections)
  {
    super();

    this.renderedSections=renderedSections;
    this.musicData=musicData;
    this.parentWin=parentWin;

    createVariantList();
  }

/*------------------------------------------------------------------------
Method:  void createVariantList()
Purpose: Initialize list (this) with variants in all sections
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void createVariantList()
  {
    for (ScoreRenderer rs : renderedSections)
      addVariantList(rs,this);

    Collections.sort(this,
      new Comparator<VariantReport>()
        {
          public int compare(VariantReport v1,VariantReport v2)
          {
            if (v1.measureNum<v2.measureNum)
              return -1;
            else if (v1.measureNum>v2.measureNum)
              return 1;
            else if (v1.betweenMeasures==v2.betweenMeasures)
              return 0;
            else if (v1.betweenMeasures && !v2.betweenMeasures)
              return 1;
            else
              return -1;
          }
        });
  }

/*------------------------------------------------------------------------
Method:  void addVariantList(ScoreRenderer rs,ArrayList<VariantReport> varReports)
Purpose: Add variants in one section to list
Parameters:
  Input:  ScoreRenderer rs                    - rendered music for one section
  Output: ArrayList<VariantReport> varReports - list to expand
  Return: -
------------------------------------------------------------------------*/

  void addVariantList(ScoreRenderer rs,ArrayList<VariantReport> varReports)
  {
    int numVoices=rs.getNumVoices();

    for (int mi=rs.getFirstMeasureNum(); mi<=rs.getLastMeasureNum(); mi++)
      {
        MeasureInfo m=rs.getMeasure(mi);
        RenderList  rv;
        for (int vi=0; vi<numVoices; vi++)
          if ((rv=rs.getRenderedVoice(vi))!=null)
            {
              int starti=m.reventindex[vi],
                  endi=mi==rs.getLastMeasureNum() ?
                    rv.size()-1 : rs.getMeasure(mi+1).reventindex[vi]-1;

              for (int ei=starti; ei<=endi; ei++)
                if (rv.getEvent(ei).getEvent().geteventtype()==Event.EVENT_VARIANTDATA_START)
                  varReports.add(new VariantReport(m,vi,rv,ei));
            }
      }
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public ScoreRenderer[] getRenderedSections()
  {
    return renderedSections;
  }
}
