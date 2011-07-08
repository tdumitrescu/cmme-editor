/*----------------------------------------------------------------------*/
/*

        Module          : VariantMarkerEvent.java

        Package         : DataStruct

        Classes Included: VariantMarkerEvent

        Purpose         : Event type for variant reading begin/end

        Programmer      : Ted Dumitrescu

        Date Started    : 11/16/07

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported classes */

import java.util.*;

/*------------------------------------------------------------------------
Class:   VariantMarkerEvent
Extends: Event
Purpose: Data/routines for variant marker events
------------------------------------------------------------------------*/

public class VariantMarkerEvent extends Event
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  ArrayList<VariantReading> readings;      /* simultaneous variant readings */
  Proportion                defaultLength; /* length of default reading */
  long                      varTypeFlags;  /* types of variants */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: VariantMarkerEvent(int eventType[,ArrayList<VariantReading> readings])
Purpose:     Creates VariantMarker event
Parameters:
  Input:  int eventType                      - event type (VARIANTDATA_START,
                                               VARIANTDATA_END)
          ArrayList<VariantReading> readings - set of variant readings
  Output: -
------------------------------------------------------------------------*/

  public VariantMarkerEvent(int eventType)
  {
    this(eventType,null);
  }

  public VariantMarkerEvent(int eventType,ArrayList<VariantReading> readings)
  {
    this.eventtype=eventType;
    this.readings=readings!=null ? readings : new ArrayList<VariantReading>();
    this.defaultLength=new Proportion(0,1);
  }

/*------------------------------------------------------------------------
Methods: long calcVariantTypes(VoiceEventListData v)
Purpose: Calculate which types of variant are present within this set of
         readings
Parameters:
  Input:  VoiceEventListData v - default event list of voice containing this
  Output: -
  Return: new flags
------------------------------------------------------------------------*/

  public long calcVariantTypes(VoiceEventListData v)
  {
    long newFlags=VariantReading.VAR_NONE;
    int  varStarti=this.getDefaultListPlace()+1;

    for (VariantReading r : readings)
      {
        r.recalcEventParams(this);
        newFlags|=r.calcVariantTypes(v,varStarti);
      }

    if (newFlags==VariantReading.VAR_NONE)
      newFlags=VariantReading.VAR_NONSUBSTANTIVE;

    this.varTypeFlags=newFlags;
    return this.varTypeFlags;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Proportion getDefaultLength()
  {
    return defaultLength;
  }

  public int getNumReadings()
  {
    return readings.size();
  }

  public VariantReading getReading(int i)
  {
    return readings.get(i);
  }

  public ArrayList<VariantReading> getReadings()
  {
    return readings;
  }

  public long getVarTypeFlags()
  {
    return varTypeFlags;
  }

  public boolean includesVarType(long varType)
  {
    return (varTypeFlags&varType)!=0;
  }

  /* get reading associated with a specific version */
  public VariantReading getVariantReading(VariantVersionData version)
  {
    for (VariantReading vr : readings)
      if (vr.includesVersion(version))
        return vr;
    return null;
  }

  public LinkedList<VariantVersionData> getDefaultVersions(List<VariantVersionData> allVersions)
  {
    LinkedList<VariantVersionData> defaultVersions=new LinkedList<VariantVersionData>();
    for (VariantVersionData vvd : allVersions)
      if (getVariantReading(vvd)==null)
        defaultVersions.add(vvd);
    return defaultVersions;
  }

  public boolean inVariant()
  {
    return true;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addReading(VariantReading vr)
  {
    readings.add(vr);
  }

  public void removeReading(VariantReading vr)
  {
    readings.remove(vr);
  }

  public void setDefaultLength(Proportion p)
  {
    defaultLength=p;
  }

  public void setReadingsList(ArrayList<VariantReading> readings)
  {
    this.readings=readings;
  }

  public void setVarTypeFlags(long newval)
  {
    this.varTypeFlags=newval;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this event
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.print("    Variant ");
    if (eventtype==Event.EVENT_VARIANTDATA_START)
      System.out.println("begin");
    else
      System.out.println("end");
  }
}
