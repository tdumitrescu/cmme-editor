/*----------------------------------------------------------------------*/
/*

        Module          : MusicSection.java

        Package         : DataStruct

        Classes Included: MusicSection

        Purpose         : Contents of one piece section (mensural music, chant,
                          text, etc.)

        Programmer      : Ted Dumitrescu

        Date Started    : 2/1/07

        Updates         :
12/1/08: added initParams() for common initialization actions

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   MusicSection
Extends: -
Purpose: Contents of one section
------------------------------------------------------------------------*/

public abstract class MusicSection
{
/*----------------------------------------------------------------------*/
/* Class variables */

  /* section types */
  public static final int MENSURAL_MUSIC=0,
                          PLAINCHANT=    1,
                          TEXT=          2;
  public static String[]  sectionTypeNames=new String[]
                            {
                              "Mensural music",
                              "Plainchant",
                              "Text"
                            };

/*----------------------------------------------------------------------*/
/* Instance variables */

  boolean            editorial;
  String             principalSource=null;
  int                principalSourceNum=0,
                     sectionType;
  Coloration         baseColoration;
  VariantVersionData curVersion=null;

  ArrayList<TacetInfo> tacetInfo;

/*----------------------------------------------------------------------*/
/* Instance methods */

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
    /* to be overridden */
    return null;
  }

  protected void copyBaseInfo(MusicSection copySection)
  {
    copySection.principalSource=this.principalSource;
    copySection.principalSourceNum=this.principalSourceNum;
    copySection.curVersion=this.curVersion;

    copySection.tacetInfo=new ArrayList<TacetInfo>(this.tacetInfo);
  }

/*------------------------------------------------------------------------
Method:  void initParams()
Purpose: Basic initialization to be called from all constructors
Parameters:
  Input:  common MusicSection attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void initParams(boolean editorial,int sectionType)
  {
    this.editorial=editorial;
    this.sectionType=sectionType;
    this.tacetInfo=new ArrayList<TacetInfo>();
  }

/*------------------------------------------------------------------------
Method:  void recalcAllEventParams([VoiceEventListData[] lastv])
Purpose: Recalculate event attributes based on parameters (clef, mensuration
         info) for all voices
Parameters:
  Input:  VoiceEventListData[] last v - voices in last section (providing starting
                                        parameters for this section)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void recalcAllEventParams(VoiceEventListData[] lastv)
  {
    for (int vi=0; vi<getNumVoices(); vi++)
      {
        VoiceEventListData v=getVoice(vi);
        if (v!=null)
          v.recalcEventParams(lastv==null ? null : lastv[vi]);
      }
  }

  public void recalcAllEventParams()
  {
    recalcAllEventParams(null);
  }

/*------------------------------------------------------------------------
Method:  void addVoice(Voice newv)
Purpose: Add new voice to end of voice list (if applicable)
Parameters:
  Input:  Voice newv - new voice metadata
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addVoice(Voice newv)
  {
    System.err.println("Error: called addVoice in no-voice section");
  }

/*------------------------------------------------------------------------
Method:  void initializeNewVoice(int vnum,Voice newv)
Purpose: Create new voice within voice list (if applicable)
Parameters:
  Input:  int vnum   - new voice number
          Voice newv - new voice metadata
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void initializeNewVoice(int vnum,Voice newv)
  {
    System.err.println("Error: called initializeNewVoice in no-voice section");
  }

/*------------------------------------------------------------------------
Method:  void removeVoice(int vnum)
Purpose: Remove voice from section (if applicable)
Parameters:
  Input:  int vnum - number of voice to remove
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removeVoice(int vnum)
  {
    System.err.println("Error: called removeVoice in no-voice section");
  }

/*------------------------------------------------------------------------
Methods: void updateVoiceList(Voice[] oldVL,Voice[] newVL,Voice[] newVoiceOrder)
Purpose: Update voice list to match changes in master voice list (if
         applicable)
Parameters:
  Input:  Voice[] oldVL,newVL   - old and new master voice lists
          Voice[] newVoiceOrder - old voice list rearranged to match new order
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void updateVoiceList(Voice[] oldVL,Voice[] newVL,Voice[] newVoiceOrder)
  {
    /* default: no action */
  }

/*------------------------------------------------------------------------
Methods: int getValidVoicenum(int vnum)
Purpose: Calculate valid voice number closest to a given number
Parameters:
  Input:  int vnum - voice number to attempt to get
  Output: -
  Return: valid voice number (closest to given number)
------------------------------------------------------------------------*/

  public int getValidVoicenum(int vnum)
  {
    if (getVoice(vnum)!=null)
      return vnum;

    /* look for closest valid voice to vnum */
    int     beforeVnum=vnum-1,
            afterVnum=vnum+1;
    boolean done=beforeVnum<0 && afterVnum>=getNumVoices();

    while (!done)
      if (beforeVnum>=0 && getVoice(beforeVnum)!=null)
        return beforeVnum;
      else if (afterVnum<getNumVoices() && getVoice(afterVnum)!=null)
        return afterVnum;
      else
        {
          beforeVnum--;
          afterVnum++;
          done=beforeVnum<0 && afterVnum>=getNumVoices();
        }

    /* no voices in section! */
    return -1;
  }

/*------------------------------------------------------------------------
Methods: void initializeNewVoice(Voice v)
Purpose: Create new voice within 
Parameters:
  Input:  int vnum - voice number to attempt to get
  Output: -
  Return: valid voice number (closest to given number)
------------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Coloration getBaseColoration()
  {
    return baseColoration;
  }

  public ArrayList<VariantVersionData> getMissingVersions(int vnum)
  {
    return getVoice(vnum).getMissingVersions();
  }

  public int getNumVoices()
  {
    return 0;
  }

  public int getNumVoicesUsed()
  {
    int numVoices=getNumVoices(),
        numVoicesUsed=0;

    for (int i=0; i<numVoices; i++)
      if (getVoice(i)!=null)
        numVoicesUsed++;

    return numVoicesUsed;
  }

  public String getPrincipalSource()
  {
    return principalSource;
  }

  public int getPrincipalSourceNum()
  {
    return principalSourceNum;
  }

  public int getSectionType()
  {
    return sectionType;
  }

  public ArrayList<TacetInfo> getTacetInfo()
  {
    return tacetInfo;
  }

  public String getTacetText(int vnum)
  {
    for (TacetInfo ti : tacetInfo)
      if (ti.voiceNum==vnum)
        return ti.tacetText;
    return null;
  }

  public VariantVersionData getVersion()
  {
    return curVersion;
  }

  public VoiceEventListData getVoice(int vnum)
  {
    return null;
  }

  public Voice getVoiceMetaData(int vnum)
  {
    return null;
  }

  public boolean isDefaultVersion()
  {
    return curVersion==null;
  }

  public boolean isEditorial()
  {
    return editorial;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addMissingVersion(int vnum,VariantVersionData vvd)
  {
    this.getVoice(vnum).addMissingVersion(vvd);
  }

  public void removeMissingVersion(int vnum,VariantVersionData vvd)
  {
    this.getVoice(vnum).removeMissingVersion(vvd);
  }

  public void setBaseColoration(Coloration baseColoration)
  {
    this.baseColoration=baseColoration;
  }

  public void setVersion(VariantVersionData version)
  {
    this.curVersion=version;
  }

  public void setEditorial(boolean editorial)
  {
    this.editorial=editorial;
  }

  public void setMissingVersions(int vnum,ArrayList<VariantVersionData> missingVersions)
  {
    this.getVoice(vnum).setMissingVersions(missingVersions);
  }

  public void setPrincipalSource(String principalSource)
  {
    this.principalSource=principalSource;
  }

  public void setPrincipalSourceNum(int principalSourceNum)
  {
    this.principalSourceNum=principalSourceNum;
  }

  public void setTacetText(int vnum,String text)
  {
    int vi;
    for (vi=0; vi<tacetInfo.size() && tacetInfo.get(vi).voiceNum<vnum; vi++)
      ;
    if (vi<tacetInfo.size())
      {
        TacetInfo ti=tacetInfo.get(vi);
        if (ti.voiceNum==vnum)
          if (!text.equals(""))
            ti.tacetText=text; /* replace existing text */
          else
            tacetInfo.remove(vi); /* remove blank text */
        else
          if (!text.equals(""))
            tacetInfo.add(vi,new TacetInfo(vnum,text)); /* insert new entry */
      }
    else
      if (!text.equals(""))
        tacetInfo.add(new TacetInfo(vnum,text)); /* append new entry */
  }

  public void setVoice(int vnum,VoiceEventListData v)
  {
  }
}
