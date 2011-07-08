/*----------------------------------------------------------------------*/
/*

        Module          : ModernAccidental.java

        Package         : DataStruct

        Classes Included: ModernAccidental

        Purpose         : Handle low-level accidental information for modern
                          notation (sharp/flat/natural rather than square b/
                          diesis/round b)

        Programmer      : Ted Dumitrescu

        Date Started    : 12/22/05

        Updates         :
4/25/06: added "optional" flag (display accidental in parentheses)
7/8/08:  converted note accidental to simple pitch offset integer
         (leaving appearance to be decided by renderer)

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   ModernAccidental
Extends: -
Purpose: Modern accidental information structure
------------------------------------------------------------------------*/

public class ModernAccidental
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int ACC_Flat=   0,
                          ACC_Natural=1,
                          ACC_Sharp=  2,
                          ACC_NONE=   3;
  public static String[]  AccidentalNames=new String[]
                            {
                              "Flat","Natural","Sharp",
                              "NONE"
                            };

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int     pitchOffset; /* -1==1 step flatward from natural, 1==sharp, etc */
  public boolean optional;    /* true for accidentals considered optional */

  /* only for signature accidentals */
  public int     accType,  /* flat/natural/sharp */
                 numAcc;   /* number of applications: 2 for double-flat, etc. */

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  int strtoMA(String at)
Purpose: Convert string to accidental type number
Parameters:
  Input:  String at - string to convert
  Output: -
  Return: accidental type number
------------------------------------------------------------------------*/

  public static int strtoMA(String at)
  {
    int i;

    for (i=0; i<AccidentalNames.length; i++)
      if (at.equals(AccidentalNames[i]))
        return i;
    if (i==AccidentalNames.length)
      i=AccidentalNames.length-1;

    return i;
  }

/*------------------------------------------------------------------------
Method:  ModernAccidental pitchOffsetToAcc(int po)
Purpose: Convert numerical pitch offset to accidental
Parameters:
  Input:  int po - pitch offset
  Output: -
  Return: new accidental
------------------------------------------------------------------------*/

  public static ModernAccidental pitchOffsetToAcc(int po)
  {
    if (po<0)
      return new ModernAccidental(ACC_Flat,0-po);
    else if (po>0)
      return new ModernAccidental(ACC_Sharp,po);
    return new ModernAccidental(ACC_Natural,1);
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ModernAccidental(int at,int na[,boolean o])
Purpose:     Initialize modern accidental information
Parameters:
  Input:  int at    - accidental type
          int na    - number of applications
          boolean o - whether accidental is optional
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ModernAccidental(int at,int na,boolean o)
  {
    accType=at;
    numAcc=na;
    optional=o;
  }

  public ModernAccidental(int at,int na)
  {
    this(at,na,false);
  }

  public ModernAccidental(int at)
  {
    this(at,1,false);
  }

  public ModernAccidental(int pitchOffset,boolean optional)
  {
    this.pitchOffset=pitchOffset;
    this.optional=optional;
    this.accType=ACC_NONE;
  }

  public ModernAccidental(ModernAccidental other)
  {
    this.pitchOffset=other.pitchOffset;
    this.optional=other.optional;

    this.accType=other.accType;
    this.numAcc=other.numAcc;
  }

/*------------------------------------------------------------------------
Method:  int calcPitchOffset()
Purpose: Calculate a numerical representation of the pitch distance from
         a 'natural' state (-1==1 flat, -2==2 flats, etc.)
Parameters:
  Input:  -
  Output: -
  Return: numerical pitch offset
------------------------------------------------------------------------*/

  public int calcPitchOffset()
  {
    switch (accType)
      {
        case ACC_Flat:
          return 0-numAcc;
        case ACC_Sharp:
          return numAcc;
      }
    return 0;
  }

/*------------------------------------------------------------------------
Method:  boolean equals(ModernAccidental other)
Purpose: Compare two accidentals for equality
Parameters:
  Input:  ModernAccidental other - accidental for comparison to this
  Output: -
  Return: true if accidentals are equal (type and number of applications)
------------------------------------------------------------------------*/

  public boolean equals(ModernAccidental other)
  {
    if (other==null)
      return false;

    if (this.accType==ACC_NONE)
      return this.pitchOffset==other.pitchOffset &&
             this.optional==other.optional;

    return this.accType==other.accType &&
           this.numAcc==other.numAcc;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this modern accidental
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.print("    Modern accidental: "+AccidentalNames[accType]);
    if (numAcc>1)
      System.out.print(" (x"+numAcc+")");
    if (optional)
      System.out.print(" (optional)");
    System.out.println();
  }
}
