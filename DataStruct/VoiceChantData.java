/*----------------------------------------------------------------------*/
/*

        Module          : VoiceChantData.java

        Package         : DataStruct

        Classes Included: VoiceChantData

        Purpose         : Contains plainchant musical data for one voice in one
                          section

        Programmer      : Ted Dumitrescu

        Date Started    : 2/21/07

        Updates         :
7/24/07: now extends abstract class VoiceEventListData

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   VoiceChantData
Extends: VoiceEventListData
Purpose: Voice plainchant music data
------------------------------------------------------------------------*/

public class VoiceChantData extends VoiceEventListData
{
/*----------------------------------------------------------------------*/
/* Instance variables */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VoiceChantData(Voice v,MusicSection section)
Purpose:     Creates plainchant music data structure matched to one voice
Parameters:
  Input:  Voice v              - voice metadata
          MusicSection section - section data
  Output: -
------------------------------------------------------------------------*/

  public VoiceChantData(Voice v,MusicSection section)
  {
    initParams(v,section);
  }

  public VoiceChantData()
  {
    initParams();
  }
}
