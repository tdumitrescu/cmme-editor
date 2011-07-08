/*----------------------------------------------------------------------*/
/*

        Module          : RenderParams.java

        Package         : Gfx

        Classes Included: RenderParams

        Purpose         : Hold musical parameters necessary for rendering
                          one event

        Programmer      : Ted Dumitrescu

        Date Started    : 9/9/2005

Updates:
7/21/06:  added editorial section flag
8/7/06:   added suggested modern clef parameter
1/11/07:  added texting information
5/1/07:   removed clef event indexing, replaced with RenderedClefSet
11/22/07: added variant reading start/end info

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   RenderParams
Extends: -
Purpose: Structure to hold musical parameters necessary for rendering one
         event
------------------------------------------------------------------------*/

public class RenderParams
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public int              measurenum;
  public RenderedClefSet  clefEvents;
  public RenderedEvent    lastEvent,
                          mensEvent;
  public Proportion       curProportion;
  public Coloration       curColoration;
  public boolean          inEditorialSection;
  public boolean          missingInVersion;
  public Clef             suggestedModernClef;

  public RenderedEventGroup varReadingInfo;

  /* ligature-related */
  public RenderedLigature ligInfo,
                          tieInfo;
  public boolean          endlig,
                          doubleTied;

  /* text-related */
  public float            lastModSyllXend,
                          lastOrigPhraseXend;
  public boolean          midWord;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderParams(int mnum,
                          RenderedClefSet ce,RenderedEvent le,RenderedEvent me,
                          Proportion p,Coloration cc,boolean ies,boolean miv,
                          RenderedLigature li,boolean el,
                          RenderedLigature tieInfo,
                          Clef smc,
                          RenderedEventGroup varReadingInfo)
Purpose:     Initialize parameter structure
Parameters:
  Input:  int mnum            - measure number
          RenderedClefSet ce  - events for current clef set on staff
          RenderedEvent le    - last rendered event (only when needed)
          RenderedEvent me    - event for current mensuration
          Proportion p        - current rhythmic proportion
          Coloration cc       - current coloration scheme
          boolean ies         - in editorial section?
          boolean miv         - missing in current version?
          RenderedLigature li - ligature info
          boolean el          - whether this is the end of a ligature
          RenderedLigature tieInfo - tied note info
          Clef smc            - editorially suggested clef for modern cleffing
          RenderedEventGroup varReadingInfo - variant start/end info
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public RenderParams(int mnum,
                      RenderedClefSet ce,RenderedEvent le,RenderedEvent me,
                      Proportion p,Coloration cc,boolean ies,boolean miv,
                      RenderedLigature li,boolean el,
                      RenderedLigature tieInfo,
                      Clef smc,
                      RenderedEventGroup varReadingInfo)
  {
    measurenum=mnum;
    clefEvents=ce;
    lastEvent=le;
    mensEvent=me;
    curProportion=p;
    curColoration=cc;
    inEditorialSection=ies;
    missingInVersion=miv;
    ligInfo=li;
    endlig=el;
    this.tieInfo=tieInfo;
    this.doubleTied=false;
    suggestedModernClef=smc;
    this.varReadingInfo=varReadingInfo;
  }

  public RenderParams(RenderedClefSet ce)
  {
    clefEvents=ce;

    measurenum=-1;
    lastEvent=null;
    mensEvent=null;
    curProportion=null;
    curColoration=null;
    inEditorialSection=false;
    missingInVersion=false;
    ligInfo=null;
    endlig=false;
    this.tieInfo=null;
    this.doubleTied=false;
    suggestedModernClef=null;
    this.varReadingInfo=null;
  }
}
