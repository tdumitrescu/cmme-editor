/*----------------------------------------------------------------------*/
/*

        Module          : VariantVersionData.java

        Package         : DataStruct

        Classes Included: VariantVersionData

        Purpose         : Contains general information about variant
                          versions of piece/edition

        Programmer      : Ted Dumitrescu

        Date Started    : 10/20/2007

        Updates         :
7/19/08: added missing voices list

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   VariantVersionData
Extends: -
Purpose: Information structure for one variant version
------------------------------------------------------------------------*/

public class VariantVersionData
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  String           ID,
                   sourceName;
  int              sourceID,
                   numInList;
  String           editor,
                   description;
  ArrayList<Voice> missingVoices;

  boolean defaultVersion=false;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantVersionData(String ID)
Purpose:     Initialize structure
Parameters:
  Input:  -
  Output: -
------------------------------------------------------------------------*/

  public VariantVersionData(String ID,int numInList)
  {
    this.ID=ID;
    this.numInList=numInList;
    sourceID=-1;
    missingVoices=new ArrayList<Voice>(16);
  }

  public VariantVersionData(String ID)
  {
    this(ID,0);
  }

  /* copy existing structure */
  public VariantVersionData(VariantVersionData other)
  {
    copyData(other);
  }

  public void copyData(VariantVersionData other)
  {
    this.ID=other.ID;
    this.sourceName=other.sourceName;
    this.sourceID=other.sourceID;
    this.numInList=other.numInList;
    this.editor=other.editor;
    this.description=other.description;
    this.missingVoices=new ArrayList<Voice>(other.missingVoices);
  }

/*------------------------------------------------------------------------
Method:  PieceData constructMusicData(PieceData defaultMusicData[,PieceData newMusicData])
Purpose: Create music data set incorporating this version's variant
         readings
Parameters:
  Input:  PieceData defaultMusicData - base music version without variants
          PieceData newMusicData     - variant music structure to write into
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public PieceData constructMusicData(PieceData defaultMusicData)
  {
    return constructMusicData(defaultMusicData,new PieceData(defaultMusicData));
  }

  public PieceData constructMusicData(PieceData defaultMusicData,PieceData newMusicData)
  {
    newMusicData.setCurVersion(this);

    newMusicData.setSections(new ArrayList<MusicSection>());
    for (MusicSection origSection : defaultMusicData.getSections())
      {
        MusicSection newSection=origSection.shallowCopy();
        newSection.setVersion(this);
        for (int vi=0; vi<origSection.getNumVoices(); vi++)
          {
            VoiceEventListData origV=origSection.getVoice(vi),
                               newV=null;
            if (origV!=null)
              {
                if (newSection instanceof MusicMensuralSection)
                  newV=new VoiceMensuralData(origV.getMetaData(),newSection);
                else if (newSection instanceof MusicChantSection)
                  newV=new VoiceChantData(origV.getMetaData(),newSection);
                newV.setMissingVersions(origV.getMissingVersions());

                /* iterate through events list, copying everything but replacing
                   VARIANTDATA segments when necessary */
                int     ei=0;
                boolean done=ei>=origV.getNumEvents();
                while (!done)
                  {
                    Event e=origV.getEvent(ei);
                    newV.addEvent(e);
                    if (e.geteventtype()==Event.EVENT_VARIANTDATA_START)
                      {
                        VariantReading vr=e.getVariantReading(this);
                        if (vr!=null)
                          {
                            Proportion variantLength=vr.getLength(),
                                       defaultLength=((VariantMarkerEvent)e).getDefaultLength(),
                                       curProp=new Proportion(1,1);
                            variantLength.setVal(0,1);
                            defaultLength.setVal(0,1);

                            /* add variant events */
                            for (int i=0; i<vr.getNumEvents(); i++)
                              {
                                Event ve=vr.getEvent(i);
                                newV.addEvent(ve);
                                variantLength.add(Proportion.product(ve.getmusictime(),curProp));
                                if (ve.geteventtype()==Event.EVENT_PROPORTION)
                                  curProp.divide(((ProportionEvent)ve).getproportion());
                              }

                            /* skip default reading */
                            curProp=new Proportion(1,1);
                            while (e.geteventtype()!=Event.EVENT_VARIANTDATA_END)
                              {
                                e=origV.getEvent(++ei);
                                defaultLength.add(Proportion.product(e.getmusictime(),curProp));
                                if (e.geteventtype()==Event.EVENT_PROPORTION)
                                  curProp.divide(((ProportionEvent)e).getproportion());
                              }
                          }
                        else
                          ei++;
                      }
                    else
                      ei++;
                    done=ei>=origV.getNumEvents();
                  }
              }

            newSection.setVoice(vi,newV);

/*            if (newSection instanceof MusicMensuralSection)
              ((MusicMensuralSection)newSection).setVoice(vi,(VoiceMensuralData)newV);
            else if (newSection instanceof MusicChantSection)
              ((MusicChantSection)newSection).setVoice(vi,(VoiceChantData)newV);*/
          }

        newMusicData.addSection(newSection);
      }
    newMusicData.recalcAllEventParams();

    return newMusicData;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setDefault(boolean defaultVersion)
  {
    this.defaultVersion=defaultVersion;
  }

  public void setDescription(String description)
  {
    this.description=description;
  }

  public void setEditor(String editor)
  {
    this.editor=editor;
  }

  public void setID(String ID)
  {
    this.ID=ID;
  }

  public void setMissingVoice(Voice v,boolean missing)
  {
    if (missing)
      {
        if (!missingVoices.contains(v))
          missingVoices.add(v);
      }
    else
      missingVoices.remove(v);
  }

  public void setNumInList(int numInList)
  {
    this.numInList=numInList;
  }

  public void setSourceInfo(String sourceName,int sourceID)
  {
    this.sourceName=sourceName;
    this.sourceID=sourceID;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public String getDescription()
  {
    return description;
  }

  public String getEditor()
  {
    return editor;
  }

  public String getID()
  {
    return ID;
  }

  public ArrayList<Voice> getMissingVoices()
  {
    return missingVoices;
  }

  public int getNumInList()
  {
    return numInList;
  }

  public int getSourceID()
  {
    return sourceID;
  }

  public String getSourceIDString()
  {
    return sourceID==-1 ? null : String.valueOf(sourceID);
  }

  public String getSourceName()
  {
    return sourceName;
  }

  public boolean isDefault()
  {
    return defaultVersion;
  }

  public boolean isVoiceMissing(Voice v)
  {
    return missingVoices.contains(v);
  }

/*------------------------------------------------------------------------
Method:  String toString()
Purpose: Convert to string
Parameters:
  Input:  -
  Output: -
  Return: string representation of structure
------------------------------------------------------------------------*/

  public String toString()
  {
    String ret="Variant Version "+ID;
    if (sourceName!=null)
      ret+="; source: "+sourceName+" (ID "+sourceID+")";
    if (editor!=null)
      ret+="; editor: "+editor;
    if (description!=null)
      ret+="; "+description;

    return ret;
  }
}
