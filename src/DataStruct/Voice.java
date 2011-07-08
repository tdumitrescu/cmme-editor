/*----------------------------------------------------------------------*/
/*

        Module          : Voice.java

        Package         : DataStruct

        Classes Included: Voice

        Purpose         : Contains musical data for one voice

        Programmer      : Ted Dumitrescu

        Date Started    : 99

        Updates         :
4/14/05: removed separate classes VoiceData, VoiceList, and EventList
7/6/05:  added parameter recalculation function (for inserting clefs etc.
         in editor)
4/4/06:  added safety checks to prevent multiple ellipses in one incipit
         voice
8/3/06:  added Editorial flag
8/7/06:  added optional suggested modern clef
2/19/06: moved event list to class VoiceMensuralData

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   Voice
Extends: -
Purpose: Voice data structure
------------------------------------------------------------------------*/

public class Voice
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  /* basic metadata */
  int       vnum;
  String    name;
  boolean   editorial;
  Clef      suggestedModernClef;
  PieceData generalData;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: Voice(PieceData gd,int vn,String n,boolean e[,Clef smc])
Purpose:     Creates voice data structure
Parameters:
  Input:  PieceData gd - general data
          int vn       - voice number
          String n     - voice name
          boolean e    - whether voice is editorially supplied
          Clef smc     - editorially suggested clef for modern cleffing
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Voice(PieceData gd,int vn,String n,boolean e,Clef smc)
  {
    vnum=vn;
    name=n;
    editorial=e;
    suggestedModernClef=smc;
    generalData=gd;
  }

  public Voice(PieceData gd,int vn,String n,boolean e)
  {
    this(gd,vn,n,e,null);
  }

  /* (shallow) copy an existing voice */
  public Voice(Voice v)
  {
    vnum=v.vnum;
    name=v.name;
    editorial=v.editorial;
    suggestedModernClef=v.suggestedModernClef;
    generalData=v.generalData;
  }

/*------------------------------------------------------------------------
Methods: boolean hasFinalisSection()
Purpose: Checks whether this voice, in an incipit score, has a non-empty
         Finalis section
Parameters:
  Input:  -
  Output: -
  Return: true if a finalis appears in this voice-incipit
------------------------------------------------------------------------*/

  public boolean hasFinalisSection()
  {
    if (!getGeneralData().isIncipitScore())
      {
        System.err.println("Error: called Voice.hasFinalisSection() in a non-incipit score");
        return false;
      }
    if (getGeneralData().getNumSections()<2)
      return false;
    MusicSection ms=getGeneralData().getSection(getGeneralData().getNumSections()-1);
    VoiceEventListData v=ms.getVoice(getNum()-1);
    if (v==null || v.getNumEvents()<2)
      return false;

    return true;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public String getName()
  {
    return name;
  }

  public char getAbbrevLetter()
  {
    if (this.name.length()==0)
      return ' ';
    if (this.name.length()>1 && this.name.charAt(0)=='[')
      return this.name.charAt(1);
    return this.name.charAt(0);
  }

  public int getNum()
  {
    return vnum;
  }

  public String getStaffTitle()
  {
    if (editorial)
      return name+" (Editorial)";
    return name;
  }

  public boolean isEditorial()
  {
    return editorial;
  }

  public Clef getSuggestedModernClef()
  {
    return suggestedModernClef;
  }

  public PieceData getGeneralData()
  {
    return generalData;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setName(String n)
  {
    name=n;
  }

  public void setNum(int n)
  {
    vnum=n;
  }

  public void setEditorial(boolean e)
  {
    editorial=e;
  }

  public void setSuggestedModernClef(Clef c)
  {
    suggestedModernClef=c;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this voice
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.print("Voice "+vnum);
    if (editorial)
      System.out.print(" (editorial)");
    System.out.println(":");
    System.out.println("  Name: "+name);
  }
}
