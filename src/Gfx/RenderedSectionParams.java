/*----------------------------------------------------------------------*/
/*

        Module          : RenderedSectionParams.java

        Package         : Gfx

        Classes Included: RenderedSectionParams

        Purpose         : Manipulate parameters at section beginnings/endings
                          (e.g., clefs, mensurations, etc.)

        Programmer      : Ted Dumitrescu

        Date Started    : 4/26/07

        Updates:

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   RenderedSectionParams
Extends: -
Purpose: Parameters for one voice at section beginning/end
------------------------------------------------------------------------*/

public class RenderedSectionParams
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  RenderedClefSet clefSet;
  RenderedEvent   mensEvent;

  public boolean  usedInSection;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderedSectionParams()
Purpose:     Initialize parameter set
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public RenderedSectionParams()
  {
    clefSet=null;
    mensEvent=null;
    usedInSection=false;
  }

  /* copy existing parameter set */
  public RenderedSectionParams(RenderedSectionParams rsp)
  {
    this.clefSet=rsp.clefSet;
    this.mensEvent=rsp.mensEvent;
    this.usedInSection=rsp.usedInSection;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public RenderedClefSet getClefSet()
  {
    return clefSet;
  }

  public RenderedEvent getMens()
  {
    return mensEvent;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setClefSet(RenderedClefSet cs)
  {
    clefSet=cs;
  }

  public void setMens(RenderedEvent me)
  {
    mensEvent=me;
  }
}

