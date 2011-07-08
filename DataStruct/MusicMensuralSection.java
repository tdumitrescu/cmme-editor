/*----------------------------------------------------------------------*/
/*

        Module          : MusicMensuralSection.java

        Package         : DataStruct

        Classes Included: MusicMensuralSection

        Purpose         : Contents of one mensural music section

        Programmer      : Ted Dumitrescu

        Date Started    : 2/13/07

        Updates         :
12/22/07: moved TacetInfo to separate module (to be used by other types
          of MusicSection as well)

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   MusicMensuralSection
Extends: MusicSection
Purpose: Contents of one mensural music section
------------------------------------------------------------------------*/

public class MusicMensuralSection extends MusicSection
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  VoiceMensuralData[]  voices;

/*------------------------------------------------------------------------
Constructor: MusicMensuralSection(int numVoices[,boolean editorial,Coloration baseColoration])
Purpose:     Initialize section
Parameters:
  Input:  int numVoices             - number of voices
          boolean editorial         - whether this section is in a source (not
                                      editorial) or not
          Coloration baseColoration - base coloration scheme of section
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MusicMensuralSection(int numVoices,boolean editorial,Coloration baseColoration)
  {
    initParams(editorial,MusicSection.MENSURAL_MUSIC);

    this.baseColoration=baseColoration;
    voices=new VoiceMensuralData[numVoices];
    for (int vi=0; vi<voices.length; vi++)
      voices[vi]=null;
  }

  public MusicMensuralSection(int numVoices)
  {
    this(numVoices,false,Coloration.DEFAULT_COLORATION);
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
    MusicMensuralSection copySection=new MusicMensuralSection(voices.length,editorial,baseColoration);

    copyBaseInfo(copySection);

    copySection.voices=new VoiceMensuralData[voices.length];
    for (int vi=0; vi<voices.length; vi++)
      copySection.voices[vi]=this.voices[vi];
    copySection.tacetInfo=new ArrayList<TacetInfo>(this.tacetInfo);
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
    VoiceMensuralData[] newVoices=new VoiceMensuralData[voices.length+1];
    for (int i=0; i<voices.length; i++)
      newVoices[i]=voices[i];

    /* change array pointer and add new voice */
    voices=newVoices;
    initializeNewVoice(voices.length-1,newv);
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
    voices[vnum]=new VoiceMensuralData(newv,this);
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
Method:    void updateVoiceList(Voice[] oldVL,Voice[] newVL)
Overrides: MusicSection.updateVoiceList
Purpose:   Update voice list to match changes in master voice list (if
           applicable)
Parameters:
  Input:  Voice[] oldVL,newVL   - old and new master voice lists
          Voice[] newVoiceOrder - old voice list rearranged to match new order
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void updateVoiceList(Voice[] oldVL,Voice[] newVL,Voice[] newVoiceOrder)
  {
//    LinkedList<VoiceMensuralData> newVoiceList=new LinkedList<VoiceMensuralData>();
    VoiceMensuralData[] newVoiceList=new VoiceMensuralData[newVL.length];
    VoiceMensuralData   vmd;

    for (int i=0; i<newVoiceOrder.length; i++)
      {
        if (Arrays.asList(oldVL).contains(newVoiceOrder[i]))
          {
            /* voice is in both old and new lists */
            vmd=getMensuralDataForVoice(newVoiceOrder[i]);
            if (vmd!=null)
              vmd.setMetaData(newVL[i]);
          }
        else
          {
            /* voice is new */
            vmd=new VoiceMensuralData(newVL[i],this);
            vmd.addEvent(new Event(Event.EVENT_SECTIONEND));
          }
        newVoiceList[i]=vmd;
      }

    voices=newVoiceList; //newVoiceList.toArray(new VoiceMensuralData[1]);
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

  /* search for mensural data for a given set of master voice meta-data */
  public VoiceMensuralData getMensuralDataForVoice(Voice v)
  {
    for (VoiceMensuralData vmd : voices)
      if (vmd!=null && vmd.getMetaData()==v)
        return vmd;
    return null; // not found
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
    voices[vnum]=(VoiceMensuralData)v;
  }
}
