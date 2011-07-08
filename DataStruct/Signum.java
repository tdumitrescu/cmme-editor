/*----------------------------------------------------------------------*/
/*

        Module          : Signum.java

        Package         : DataStruct

        Classes Included: Signum

        Purpose         : Handle signum congruentiae or corona attached to
                          score element

        Programmer      : Ted Dumitrescu

        Date Started    : 11/9/06

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   Signum
Extends: -
Purpose: One signum congruentiae or corona
------------------------------------------------------------------------*/

public class Signum
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int UP=0,
                          DOWN=1,

                          LEFT=0,
                          MIDDLE=1,
                          RIGHT=2,

                          DEFAULT_YOFFSET=4;

  public static final String[] orientationNames={ "Up","Down" },
                               sideNames={ "Left","Middle","Right" };

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int offset,
             orientation,
             side;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: Signum([int of,]int or,int s)
Purpose:     Initialize signum
Parameters:
  Input:  int of - y-offset from main event position
          int or - orientation
          int s  - side
  Output: -
------------------------------------------------------------------------*/

  public Signum(int of,int or,int s)
  {
    offset=of;
    orientation=or;
    side=s;
  }

  public Signum(int or,int s)
  {
    this(DEFAULT_YOFFSET,or,s);
  }

  public Signum(Signum other)
  {
    this.offset=other.offset;
    this.orientation=other.orientation;
    this.side=other.side;
  }

  public boolean equals(Signum other)
  {
    return other!=null &&
           this.offset==other.offset &&
           this.orientation==other.orientation &&
           this.side==other.side;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this element
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("    Signum");
  }
}
