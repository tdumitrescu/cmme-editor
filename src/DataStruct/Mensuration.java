/*----------------------------------------------------------------------*/
/*

        Module          : Mensuration.java

        Package         : DataStruct

        Classes Included: Mensuration

        Purpose         : Mensuration description structure

        Programmer      : Ted Dumitrescu

        Date Started    : 11/30/05

        Updates         :
2/24/06:  changed MENS_BINARY and MENS_TERNARY constants to 2 and 3, to allow
          instance variables to be used directly in calculations
12/22/08: added tempo change information

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   Mensuration
Extends: Event
Purpose: Structure to hold information about mensuration
------------------------------------------------------------------------*/

public class Mensuration
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int MENS_BINARY= 2,
                          MENS_TERNARY=3;

  public static final Proportion  DEFAULT_TEMPO_CHANGE=new Proportion(1,1);
  public static final Mensuration DEFAULT_MENSURATION=new Mensuration(MENS_BINARY,
                                                                      MENS_BINARY,
                                                                      MENS_BINARY,
                                                                      MENS_BINARY);

/*----------------------------------------------------------------------*/
/* Instance variables */

  /* four levels of mensural division (currently BINARY or TERNARY) */
  public int        prolatio,
                    tempus,
                    modus_minor,
                    modus_maior;
  public Proportion tempoChange;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: Mensuration(int p,int t,int m1,int m2)
Purpose:     Creates mensuration structure
Parameters:
  Input:  int p,t,m1,m2 - division types at different note levels
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Mensuration(int p,int t,int m1,int m2)
  {
    this(p,t,m1,m2,DEFAULT_TEMPO_CHANGE);
  }

  public Mensuration(int p,int t,int m1,int m2,Proportion tempoChange)
  {
    prolatio=p;
    tempus=t;
    modus_minor=m1;
    modus_maior=m2;
    this.tempoChange=new Proportion(tempoChange);
  }

  /* copy other mensuration */
  public Mensuration(Mensuration other)
  {
    if (other==null)
      copyMensuration(DEFAULT_MENSURATION);
    else
      copyMensuration(other);
  }

  public void copyMensuration(Mensuration other)
  {
    this.prolatio=   other.prolatio;
    this.tempus=     other.tempus;
    this.modus_minor=other.modus_minor;
    this.modus_maior=other.modus_maior;
    this.tempoChange=new Proportion(other.tempoChange);
  }

/*------------------------------------------------------------------------
Method:  boolean equals(Mensuration m)
Purpose: Compare for equality against another mensuration
Parameters:
  Input:  Mensuration m - mensuration to compare
  Output: -
  Return: true if equal
------------------------------------------------------------------------*/

  public boolean equals(Mensuration m)
  {
    return m.prolatio==prolatio       &&
           m.tempus==tempus           &&
           m.modus_minor==modus_minor &&
           m.modus_maior==modus_maior &&
           m.tempoChange.equals(this.tempoChange);
  }

/*------------------------------------------------------------------------
Method:  boolean ternary(int noteType)
Purpose: Check if a note type is ternary under this mensuration
Parameters:
  Input:  int noteType - note type to check
  Output: -
  Return: true if type is ternary
------------------------------------------------------------------------*/

  public boolean ternary(int noteType)
  {
    switch (noteType)
      {
        case NoteEvent.NT_Semibrevis:
          return this.prolatio==MENS_TERNARY;
        case NoteEvent.NT_Brevis:
          return this.tempus==MENS_TERNARY;
        case NoteEvent.NT_Longa:
          return this.modus_minor==MENS_TERNARY;
        case NoteEvent.NT_Maxima:
          return this.modus_maior==MENS_TERNARY;
      }

    return false;
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
    System.out.print(prolatio==   MENS_TERNARY ? "3" : "2");
    System.out.print(tempus==     MENS_TERNARY ? "3" : "2");
    System.out.print(modus_minor==MENS_TERNARY ? "3" : "2");
    System.out.print(modus_maior==MENS_TERNARY ? "3" : "2");
    if (!tempoChange.equals(DEFAULT_TEMPO_CHANGE))
      System.out.print(" Tempo: "+tempoChange);
    System.out.println();
  }
}
