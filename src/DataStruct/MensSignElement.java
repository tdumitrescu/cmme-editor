/*----------------------------------------------------------------------*/
/*

        Module          : MensSignElement.java

        Package         : DataStruct

        Classes Included: MensSignElement

        Purpose         : One item in a mensuration sign (symbol, number, etc)

        Programmer      : Ted Dumitrescu

        Date Started    : 7/31/06

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   MensSignElement
Extends: Event
Purpose: One element in a mensuration sign
------------------------------------------------------------------------*/

public class MensSignElement
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int NO_SIGN=       0,
                          NUMBERS=       1,
                          MENS_SIGN_O=   2,
                          MENS_SIGN_C=   3,
                          MENS_SIGN_CREV=4;

  public static final String[] signNames=new String[]
    {
      "NoSign","Numbers","O","C","CRev"
    };

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int        signType;
  public boolean    dotted,
                    stroke;
  public Proportion number;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MensSignElement(int st,[boolean d,boolean str|Proportion n])
Purpose:     Creates mensuration sign element structure
Parameters:
  Input:  int st       - sign type
          boolean d    - dotted?
          boolean str  - stroke?
          Proportion n - number(s) to display
  Output: -
  Return: -
------------------------------------------------------------------------*/

  /* mensuration sign */
  public MensSignElement(int st,boolean d,boolean str)
  {
    signType=st;
    dotted=d;
    stroke=str;
    number=null;
  }

  /* number/proportion */
  public MensSignElement(int st,Proportion n)
  {
    signType=st;
    number=new Proportion(n);
  }

  /* copy another */
  public MensSignElement(MensSignElement other)
  {
    this.signType=other.signType;
    this.dotted=other.dotted;
    this.stroke=other.stroke;
    this.number=(other.number==null) ? null : new Proportion(other.number);
  }

  public boolean equals(MensSignElement other)
  {
    if (number==null)
      return other.number==null &&
             this.signType==other.signType &&
             this.dotted==other.dotted &&
             this.stroke==other.stroke;
    else
      return other.number!=null &&
             this.number.equals(other.number) &&
             this.signType==other.signType;
  }

/*------------------------------------------------------------------------
Method:  String toString()
Purpose: Convert to string representation of main element
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public String toString()
  {
    switch (signType)
      {
        case MENS_SIGN_C:
        case MENS_SIGN_CREV:
          return "C";
        case MENS_SIGN_O:
          return "O";
        case NUMBERS:
          if (number.i2==0)
            return String.valueOf(number.i1);
          else
            return number.i1+"/"+number.i2;
      }
    return "!!!";
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
    System.out.print(signNames[signType]);
    if (signType==NO_SIGN)
      {
        System.out.println();
        return;
      }
    if (signType==NUMBERS)
      {
        System.out.print(": "+number.i1);
        if (number.i2!=0)
          System.out.print("/"+number.i2);
        System.out.println();
      }
    else
      {
        if (dotted)
          System.out.print("Dot");
        if (stroke)
          System.out.print("Stroke");
        System.out.println();
      }
  }
}
