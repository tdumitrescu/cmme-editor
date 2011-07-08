/*----------------------------------------------------------------------*/
/*

        Module          : Coloration.java

        Package         : DataStruct

        Classes Included: Coloration

        Purpose         : Low-level coloration scheme handling

        Programmer      : Ted Dumitrescu

        Date Started    : 9/14/05

Updates	:

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.Color;

/*------------------------------------------------------------------------
Class:   Coloration
Extends: -
Purpose: Low-level coloration scheme handling
------------------------------------------------------------------------*/

public class Coloration
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int      BLACK=    0,
                               RED=      1,
                               BLUE=     2,
                               GREEN=    3,
                               YELLOW=   4,
                               CYAN=     5,
                               WHITE=    6,
                               GRAY=     7,
                               NONE=     -1;

  public static final int      VOID=         0,
                               FULL=         1,
                               HALF_FULLVOID=2,
                               HALF_VOIDFULL=3;

  /* coloration effects */
  public static final int      IMPERFECTIO= 0,
                               SESQUIALTERA=1,
                               MINOR_COLOR= 2;

  public static final String[] ColorNames=new String[]
                                 {
                                   "Black","Red","Blue","Green","Yellow"
                                 };
  public static final String[] ColorFillNames=new String[]
                                 {
                                   "Void","Full"
                                 };
  public static final Color[]  AWTColors=new Color[]
                                 {
                                   Color.black,Color.red,Color.blue,
                                   Color.green,Color.yellow,Color.cyan,
                                   Color.white,Color.gray
                                 };

  public static final Coloration DEFAULT_COLORATION=new Coloration(BLACK,VOID,
                                                                   BLACK,FULL),
                                 DEFAULT_CHANT_COLORATION=new Coloration(BLACK,FULL,
                                                                         BLACK,VOID);

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  int strtoColor(String c)
Purpose: Convert string to color number
Parameters:
  Input:  String c - string to convert
  Output: -
  Return: color number
------------------------------------------------------------------------*/

  public static int strtoColor(String c)
  {
    if (c==null)
      return NONE;

    for (int i=0; i<ColorNames.length; i++)
      if (c.equals(ColorNames[i]))
        return i;

    return NONE;
  }

/*------------------------------------------------------------------------
Method:  int strtoColorFill(String cf)
Purpose: Convert string to color fill type number
Parameters:
  Input:  String cf - string to convert
  Output: -
  Return: color fill type number
------------------------------------------------------------------------*/

  public static int strtoColorFill(String cf)
  {
    if (cf==null)
      return NONE;

    for (int i=0; i<ColorFillNames.length; i++)
      if (cf.equals(ColorFillNames[i]))
        return i;

    return NONE;
  }

/*------------------------------------------------------------------------
Method:  int complementaryFill(int f)
Purpose: Calculate complementary fill type (VOID vs FULL)
Parameters:
  Input:  int f - fill type to complement
  Output: -
  Return: complementary color fill type number
------------------------------------------------------------------------*/

  public static int complementaryFill(int f)
  {
    if (f==NONE)
      return NONE;
    return f==VOID ? FULL : VOID;
  }

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int primaryColor,
             primaryFill,
             secondaryColor,
             secondaryFill;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: Coloration(int pc,int pf,int sc,int sf)
Purpose:     Initialize structure
Parameters:
  Input:  int pc,pf - parameters for primary coloration
          int sc,sf - parameters for secondary coloration
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Coloration(int pc,int pf,int sc,int sf)
  {
    primaryColor=pc;
    primaryFill=pf;
    secondaryColor=sc;
    secondaryFill=sf;
  }

  /* combine two coloration structures */
  public Coloration(Coloration oldc,Coloration newc)
  {
    primaryColor   = newc.primaryColor   == NONE ? oldc.primaryColor   : newc.primaryColor;
    primaryFill    = newc.primaryFill    == NONE ? oldc.primaryFill    : newc.primaryFill;
    secondaryColor = newc.secondaryColor == NONE ? oldc.secondaryColor : newc.secondaryColor;
    secondaryFill  = newc.secondaryFill  == NONE ? oldc.secondaryFill  : newc.secondaryFill;
  }

  /* copy existing coloration structure */
  public Coloration(Coloration c)
  {
    primaryColor   = c.primaryColor;
    primaryFill    = c.primaryFill;
    secondaryColor = c.secondaryColor;
    secondaryFill  = c.secondaryFill;
  }

/*------------------------------------------------------------------------
Method:  boolean equals(Coloration c)
Purpose: Compare for equality against another color scheme
Parameters:
  Input:  Coloration c - color scheme to compare
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public boolean equals(Coloration c)
  {
    return c!=null &&
           primaryColor==c.primaryColor &&
           primaryFill==c.primaryFill &&
           secondaryColor==c.secondaryColor &&
           secondaryFill==c.secondaryFill;
  }

/*------------------------------------------------------------------------
Method:  Coloration differencebetween(Coloration other)
Purpose: Creates a color scheme representing the difference between this
         and another
Parameters:
  Input:  Coloration other - other color scheme
  Output: -
  Return: new Coloration representing difference
------------------------------------------------------------------------*/

  public Coloration differencebetween(Coloration other)
  {
    int pc=NONE,pf=NONE,
        sc=NONE,sf=NONE;

    if (primaryColor!=other.primaryColor)
      pc=primaryColor;
    if (primaryFill!=other.primaryFill)
      pf=primaryFill;
    if (secondaryColor!=other.secondaryColor)
      sc=secondaryColor;
    if (secondaryFill!=other.secondaryFill)
      sf=secondaryFill;

    return new Coloration(pc,pf,sc,sf);
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this structure
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.print("[");
    if (primaryColor!=NONE)
      System.out.print(ColorNames[primaryColor]);
    if (primaryFill!=NONE)
      System.out.print(ColorFillNames[primaryFill]);
    System.out.print(" / ");
    if (secondaryColor!=NONE)
      System.out.print(ColorNames[secondaryColor]);
    if (secondaryFill!=NONE)
      System.out.print(ColorFillNames[secondaryFill]);
    System.out.println("]");
  }
}
