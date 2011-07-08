/*----------------------------------------------------------------------*/
/*

        Module          : MeasureList.java

        Package         : Gfx

        Classes Included: MeasureList

        Purpose         : Keep measure rendering information

        Programmer      : Ted Dumitrescu

        Date Started    : 4/23/99

        Updates         : 6/16/05: now extends class ArrayList rather than
                                   wrapping it in a separate variable

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   MeasureList
Extends: java.util.ArrayList
Purpose: Expandable list of measure structures
------------------------------------------------------------------------*/

public class MeasureList extends ArrayList<MeasureInfo>
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  int numvoices;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MeasureList(int nv)
Purpose:     Initialize list
Parameters:
  Input:  int nv - the number of music voices
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MeasureList(int nv)
  {
    super();
    numvoices=nv;
  }

/*------------------------------------------------------------------------
Method:  MeasureInfo newMeasure(int mnum,Proportion mt,int nm,double xl,double leftx)
Purpose: Create a new measure and add to list
Parameters:
  Input:  int mnum      - measure number
          Proportion mt - music time at start of measure
          int nm        - number of minims in measure
          Proportion tempoProportion - proportion applied to measure
          double xl    - starting x length of measure
          double leftx - left x coord of measure
  Output: -
  Return: new measure info
------------------------------------------------------------------------*/

  public MeasureInfo newMeasure(int mnum,Proportion mt,int nm,Proportion tempoProportion,double xl,double leftx)
  {
    MeasureInfo newm=new MeasureInfo(mnum,numvoices,mt,nm,tempoProportion,xl,leftx);
    add(newm);
    return newm;
  }

/*------------------------------------------------------------------------
Method:  MeasureInfo getMeasure(int i)
Purpose: Get measure at specified index from list
Parameters:
  Input:  int i - index of event
  Output: -
  Return: Requested measure
------------------------------------------------------------------------*/

  public MeasureInfo getMeasure(int i)
  {
    return i<size() ? (MeasureInfo)get(i) : null;
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
    for (Iterator i=iterator(); i.hasNext();)
      ((MeasureInfo)i.next()).prettyprint();
  }
}
