/*----------------------------------------------------------------------*/
/*

        Module          : RenderedStaffSystem

        Package         : Gfx

        Classes	Included: RenderedStaffSystem

        Purpose         : Parameters for one staff system in page display

        Programmer      : Ted Dumitrescu

        Date Started    : 8/2/06

        Updates:

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*------------------------------------------------------------------------
Class:   RenderedStaffSystem
Extends: -
Purpose: One staff system in page display
------------------------------------------------------------------------*/

public class RenderedStaffSystem
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public int     startMeasure,endMeasure,
                 leftX,rightX,
                 topY,
                 numVoices;
  public double  spacingCoefficient=1;
  public boolean displayVoiceNames;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderedStaffSystem(int sm,int lx,int rx,int topY,int numVoices,boolean displayVoiceNames)
Purpose:     Initialize structure
Parameters:
  Input:  int sm                    - starting measure number
          int lx,rx                 - left and right X-coordinates of system
          int topY                  - top Y-coordinate
          int numVoices             - number of voices
          boolean displayVoiceNames - whether to display voice names at left
  Output: -
------------------------------------------------------------------------*/

  public RenderedStaffSystem(int sm,int lx,int rx,int topY,int numVoices,boolean displayVoiceNames)
  {
    startMeasure=endMeasure=sm;
    leftX=lx; rightX=rx;
    this.topY=topY;
    this.numVoices=numVoices;
    this.displayVoiceNames=displayVoiceNames;
  }
}

