/*----------------------------------------------------------------------*/
/*

        Module          : RenderedLigature.java

        Package         : Gfx

        Classes Included: RenderedLigature

        Purpose         : Hold ligature information for one rendered event

        Programmer      : Ted Dumitrescu

        Date Started    : 4/5/2006

        Updates         :
11/22/07: made subclass of RenderedEventGroup
8/26/08:  added types to include ties

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import DataStruct.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   RenderedLigature
Extends: -
Purpose: Structure to hold ligature information for one rendered event
------------------------------------------------------------------------*/

public class RenderedLigature extends RenderedEventGroup
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int LIG=0,
                          TIE=1;

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int                numNotes,
                            groupType;
  public VoiceEventListData curVoice;
  public RenderList         reventList; /* main render list */
  public StaffEventData     rligEvents; /* list holding only this ligature's events,
                                           rendered as a single ligature-shape */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderedLigature(VoiceEventListData v,RenderList rel,int groupType)
Purpose:     Initialize parameter structure
Parameters:
  Input:  VoiceMensuralData v - unrendered event list for voice with ligature
          RenderList rel      - rendered event list
          int groupType       - type (LIG/TIE)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public RenderedLigature(VoiceEventListData v,RenderList rel,int groupType)
  {
    super(-1,-1);
    curVoice=v;
    reventList=rel;
    this.groupType=groupType;
  }

  public RenderedLigature(VoiceEventListData v,RenderList rel)
  {
    this(v,rel,LIG);
  }

/*------------------------------------------------------------------------
Method:  update(int evNum,NoteEvent ne)
Purpose: Update ligature info when rendering a new note (which may or may not
         be part of a ligature)
Parameters:
  Input:  int evNum     - event index within render list
          NoteEvent ne  - note being rendered
  Output: -
  Return: whether or not this is the last note of a ligature
------------------------------------------------------------------------*/

  public boolean updateTie(int evNum,NoteEvent ne)
  {
    if (firstEventNum!=-1)
      {
        numNotes++;
        lastEventNum=evNum;
      }
    else
      if (ne.getTieType()!=NoteEvent.TIE_NONE)
        {
          firstEventNum=yMaxEventNum=yMinEventNum=evNum;
          yMaxEvent=yMinEvent=ne;
          numNotes=1;
        }

    findMinMaxY(evNum,ne);

    return numNotes>1;
  }

  public boolean update(int evNum,NoteEvent ne)
  {
    boolean endlig=false;

    if (groupType==TIE)
      return updateTie(evNum,ne);

    if (firstEventNum!=-1)
      {
        numNotes++;
        if (!ne.isligated()) /* this is the last note of the ligature */
          {
            endlig=true;
            lastEventNum=evNum;

            /* all lig notes have been accounted, now render lig in its
               original shape */
            rligEvents=new StaffEventData(curVoice.getMetaData(),reventList.getSection());
            rligEvents.getOptions().setLigatureList(true);
//            rligEvents.addlig(curVoice,reventList.getEvent(firstEventNum).getEvent().getListPlace(curVoice.isDefaultVersion()),
//                                       reventList.getEvent(firstEventNum).getRenderParams());
            rligEvents.addlig(reventList,firstEventNum,
                              reventList.getEvent(firstEventNum).getRenderParams());
            rligEvents.setgroupxlocs(new RenderedEventGroup(0,lastEventNum-firstEventNum),0);
          }
      }
    else
      if (ne.isligated())    /* start of a new ligature */
        {
          firstEventNum=yMaxEventNum=yMinEventNum=evNum;
          yMaxEvent=yMinEvent=ne;
          numNotes=1;
        }

    findMinMaxY(evNum,ne);

    return endlig;
  }

  void findMinMaxY(int evNum,NoteEvent ne)
  {
    if (firstEventNum!=-1)
      if (ne.getPitch().isHigherThan(yMaxEvent.getPitch()))
        {
          yMaxEventNum=evNum;
          yMaxEvent=ne;
        }
      else if (yMinEvent.getPitch().isHigherThan(ne.getPitch()))
        {
          yMinEventNum=evNum;
          yMinEvent=ne;
        }
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this ligature
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public String toString()
  {
    return "Lig: FEN="+firstEventNum+" LEN="+lastEventNum+
           " yMXN="+yMaxEventNum+" yMXE="+yMaxEvent+
           " yMNN="+yMinEventNum+" yMNE="+yMinEvent;
  }

  public void prettyprint()
  {
    System.out.println(this.toString());
  }
}

