/*----------------------------------------------------------------------*/
/*

        Module          : MusicChantSection.java

        Package         : DataStruct

        Classes Included: MusicChantSection

        Purpose         : Contents of one plainchant section

        Programmer      : Ted Dumitrescu

        Date Started    : 2/14/07

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   MusicChantSection
Extends: MusicSection
Purpose: Contents of one plainchant section
------------------------------------------------------------------------*/

public class MusicChantSection extends MusicSection
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  VoiceChantData[] voices;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MusicChantSection(int numVoices[,boolean editorial,Coloration baseColoration])
Purpose:     Initialize section
Parameters:
  Input:  int numVoices             - number of voices
          boolean editorial         - whether this section is in a source (not
                                      editorial) or not
          Coloration baseColoration - base coloration scheme of section
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MusicChantSection(int numVoices,boolean editorial,Coloration baseColoration)
  {
    initParams(editorial,MusicSection.PLAINCHANT);

    this.baseColoration=baseColoration;
    voices=new VoiceChantData[numVoices];
    for (int vi=0; vi<voices.length; vi++)
      voices[vi]=null;
  }

  public MusicChantSection(int numVoices)
  {
    this(numVoices,false,Coloration.DEFAULT_CHANT_COLORATION);
  }

/*------------------------------------------------------------------------
Method:  MusicSection shallowCopy()
Purpose: Create a shallow copy of this object
Parameters:
  Input:  -
  Output: -
  Return: new shallow copy of this
------------------------------------------------------------------------*/

  public MusicSection shallowCopy()
  {
    MusicChantSection copySection=new MusicChantSection(voices.length,editorial,baseColoration);

    copyBaseInfo(copySection);

    copySection.voices=new VoiceChantData[voices.length];
    for (int vi=0; vi<voices.length; vi++)
      copySection.voices[vi]=this.voices[vi];
    return copySection;
  }

/*------------------------------------------------------------------------
Method:    void addVoice(Voice newv)
Overrides: MusicSection.addVoice
Purpose:   Add new voice to end of voice list (if applicable)
Parameters:
  Input:  Voice newv - new voice metadata
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addVoice(Voice newv)
  {
    /* copy voice list */
    VoiceChantData[] newVoices=new VoiceChantData[voices.length+1];
    for (int i=0; i<voices.length; i++)
      newVoices[i]=voices[i];

    /* change array pointer; set new voice as null */
    voices=newVoices;
    voices[voices.length-1]=null;
  }

/*------------------------------------------------------------------------
Method:    void initializeNewVoice(int vnum,Voice newv)
Overrides: MusicSection.initializeNewVoice
Purpose:   Create new voice within voice list (if applicable)
Parameters:
  Input:  int vnum   - new voice number
          Voice newv - new voice metadata
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void initializeNewVoice(int vnum,Voice newv)
  {
    voices[vnum]=new VoiceChantData(newv,this);
    voices[vnum].addEvent(new Event(Event.EVENT_SECTIONEND));
  }

/*------------------------------------------------------------------------
Method:    void removeVoice(int vnum)
Overrides: MusicSection.removeVoice
Purpose:   Remove voice from section (if applicable)
Parameters:
  Input:  int vnum - number of voice to remove
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removeVoice(int vnum)
  {
    voices[vnum]=null;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int getNumVoices()
  {
    return voices.length;
  }

  public VoiceEventListData getVoice(int vnum)
  {
    if (vnum>=voices.length)
      return null;
    return voices[vnum];
  }

  public Voice getVoiceMetaData(int vnum)
  {
    return getVoice(vnum).getMetaData();
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setVoice(int vnum,VoiceEventListData v)
  {
    voices[vnum]=(VoiceChantData)v;
  }
}
