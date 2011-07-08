/*----------------------------------------------------------------------*/
/*

        Module          : MeasureInfo.java

        Package         : Gfx

        Classes Included: MeasureInfo

        Purpose         : Keep rendering information for one measure

        Programmer      : Ted Dumitrescu

        Date Started    : 4/23/99

Updates:
2/24/06: added music time info (in minim-based timekeeping, breves are not
         guaranteed to be of equal length)
5/1/07:  changed from holding clef event indices to holding actual lists
         of clef events (for preservation of clefs across sections)
7/1/10:  converted music time variables from double to Proportion

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   MeasureInfo
Extends: -
Purpose: Structure which holds event rendering information for one measure
------------------------------------------------------------------------*/

public class MeasureInfo
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public Proportion startMusicTime;
  public double     startMusicTimeDbl,
                    xlength,
                    leftx;
  public int        numMinims,
                    reventindex[];
  public boolean    scaleSet;
  public Proportion defaultTempoProportion;

  public Proportion      tempoProportion[];
  public RenderedClefSet startClefEvents[];
  public RenderedEvent   startMensEvent[];

  int measurenum,
      numvoices;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MeasureInfo(int mnum,int nv,Proportion mt,int nm,double xl,double left_x)
Purpose:     Initialize structure
Parameters:
  Input:  int mnum      - this measure's number
          int nv        - the number of music voices
          Proportion mt - music time at start of measure
          int nm        - number of minims in measure
          Proportion tempoProportion - proportion applied to measure
          double xl     - starting x length of measure
          double left_x - left x coord of measure
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MeasureInfo(int mnum,int nv,Proportion mt,int nm,Proportion vTempoProportion,double xl,double left_x)
  {
    this.measurenum=mnum;
    this.numvoices=nv;
    this.startMusicTime=new Proportion(mt);
    this.startMusicTimeDbl=this.startMusicTime.toDouble();
    this.numMinims=nm;
    this.xlength=xl;
    this.leftx=left_x;

    this.scaleSet=false;
    this.defaultTempoProportion=vTempoProportion;

    this.reventindex=new int[numvoices];
    this.startClefEvents=new RenderedClefSet[numvoices];
    this.startMensEvent=new RenderedEvent[numvoices];
    this.tempoProportion=new Proportion[numvoices];

    for (int vi=0; vi<this.numvoices; vi++)
      {
        this.reventindex[vi]=-1;
        this.tempoProportion[vi]=vTempoProportion;
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

  public Proportion getEndMusicTime()
  {
    return Proportion.sum(this.startMusicTime,
      Proportion.quotient(new Proportion(this.numMinims,1),this.defaultTempoProportion));
  }

  public Proportion getEndMusicTime(int vnum)
  {
    return Proportion.sum(this.startMusicTime,
      Proportion.quotient(new Proportion(this.numMinims,1),this.tempoProportion[vnum]));
  }

  public int getMeasureNum()
  {
    return measurenum;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Print out information for all measures in list
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("*------- MEASURE "+measurenum+" -------*");
    System.out.println("Start time="+startMusicTime+" Number of minims="+numMinims);
    System.out.println("xlength="+xlength);
    for (int i=0; i<numvoices; i++)
      System.out.print("REvIndex"+i+": "+reventindex[i]+",");
    System.out.println();
  }
}
