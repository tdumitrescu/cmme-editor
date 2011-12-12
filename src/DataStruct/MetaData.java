/*----------------------------------------------------------------------*/
/*

        Module          : MetaData.java

        Package         : DataStruct

        Classes Included: MetaData

        Purpose         : CMME system meta-data

        Programmer      : Ted Dumitrescu

        Date Started    : 3/1/07

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   MetaData
Extends: -
Purpose: CMME system meta-data
------------------------------------------------------------------------*/

public class MetaData
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final String CMME_VERSION="0.98";
  public static final float  CMME_VERSION_FLOAT=Float.parseFloat(CMME_VERSION);

  public static final String CMME_SOFTWARE_NAME="CMME Editor v"+CMME_VERSION;

  public static boolean CMME_OPT_TESTING=false,
                        CMME_OPT_VALIDATEXML=false;
}

