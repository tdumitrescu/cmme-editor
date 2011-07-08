/*----------------------------------------------------------------------*/
/*

        Module          : RenderedScorePage

        Package         : Gfx

        Classes Included: RenderedScorePage

        Purpose         : Parameters for one page in score page display

        Programmer      : Ted Dumitrescu

        Date Started    : 8/2/06

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*------------------------------------------------------------------------
Class:   RenderedScorePage
Extends: -
Purpose: One page in scored page display
------------------------------------------------------------------------*/

public class RenderedScorePage
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public int startSystem,numSystems,
             numStaves,
             ySpace;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderedScorePage(int ss,int ns)
Purpose:     Initialize structure
Parameters:
  Input:  int ss - starting system number
          int ns - number of systems on page
  Output: -
------------------------------------------------------------------------*/

  public RenderedScorePage(int ss,int ns)
  {
    startSystem=ss;
    numSystems=ns;

    this.numStaves=0;
    this.ySpace=0;
  }
}
