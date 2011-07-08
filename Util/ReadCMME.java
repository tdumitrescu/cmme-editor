/*----------------------------------------------------------------------*/
/*

	Module		: ReadCMME.java

        Package         : Util

        Classes	Included: ReadCMME

	Purpose		: Standalone program to read CMME files into
                          data structures (for debugging and converting)

        Programmer	: Ted Dumitrescu

	Date Started	: 2/23/05

	Updates		:

									*/
/*----------------------------------------------------------------------*/

package Util;

/*----------------------------------------------------------------------*/
/* Imported packages */

import DataStruct.CMMEParser;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   ReadCMME
Extends: -
Purpose: Read CMME files into data structures
------------------------------------------------------------------------*/

public class ReadCMME
{
/*----------------------------------------------------------------------*/
/* Class variables */

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance variables */

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Class methods */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Method:  void main(String args[])
Purpose: Main routine
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void main(String args[])
  {
    if (args.length!=1)
      {
        System.err.println("Usage: java Util.ReadCMME filename");
        System.exit(1);
      }

    DataStruct.XMLReader.initparser("data\\",true);
    try
      {
        CMMEParser p=new CMMEParser("data\\music\\"+args[0]);
        p.piece.prettyprint();
      }
    catch (Exception e)
      {
        System.err.println("Error loading "+args[0]+": "+e);
      }
  }
}
