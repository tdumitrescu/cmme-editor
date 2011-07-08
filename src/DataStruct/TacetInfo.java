/*----------------------------------------------------------------------*/
/*

        Module          : TacetInfo.java

        Package         : DataStruct

        Classes Included: TacetInfo

        Purpose         : Information about tacet text for one voice in
                          one section

        Programmer      : Ted Dumitrescu

        Date Started    : 12/22/07 (moved from MusicMensuralSection)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

public class TacetInfo
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public int    voiceNum;
  public String tacetText;

/*------------------------------------------------------------------------
Constructor: TacetInfo(int voiceNum,String tacetText)
Purpose:     Initialize info
Parameters:
  Input:  int voiceNum     - voice number
          String tacetText - textual tacet instruction
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public TacetInfo(int voiceNum,String tacetText)
  {
    this.voiceNum=voiceNum;
    this.tacetText=tacetText;
  }
}
