/*----------------------------------------------------------------------*/
/*

        Module          : Pitch.java

        Package         : DataStruct

        Classes Included: Pitch

        Purpose         : Handle low-level pitch information

        Programmer      : Ted Dumitrescu

        Date Started    : 99

        Updates         : 3/21/05: added comparison function
                          3/29/05: converted Integer data (only needed for
                                   parser interface) to int
                          6/6/05:  added staffspacenum parameter (to allow
                                   clefless display)

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   Pitch
Extends: -
Purpose: Pitch information structure
------------------------------------------------------------------------*/

public class Pitch
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final Pitch HIGHEST_PITCH=new Pitch('A',10000),
                            LOWEST_PITCH= new Pitch('A',-10000);

/*----------------------------------------------------------------------*/
/* Instance variables */

  public char noteletter;
  public int  octave,
              placenum, /* Gamut place number */
              staffspacenum;
  Clef        clef;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  int calcplacenum(char nl,int o)
Purpose: Calculates Gamut place number from pitch letter and octave number
Parameters:
  Input:  char nl - pitch letter
          int o   - octave number
  Output: -
  Return: place number
------------------------------------------------------------------------*/

  public static int calcplacenum(char nl,int o)
  {
    return o*7+nl-'A';
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: Pitch(char nl,int o,Clef c)
Purpose:     Initialize pitch information
Parameters:
  Input:  char nl - note letter
          int o   - octave number
          Clef c  - principal clef for calculating staff location
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Pitch(char nl,int o,Clef c)
  {
    noteletter=nl;
    octave=o;
    placenum=calcplacenum(nl,o);
    clef=c;
    staffspacenum=c!=null ? c.calcypos(this) : 0;
  }

/*------------------------------------------------------------------------
Constructor: Pitch(char nl,int o)
Purpose:     Initialize pitch information (no clef info)
Parameters:
  Input:  char nl - note letter
          int o   - octave number
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Pitch(char nl,int o)
  {
    noteletter=nl;
    octave=o;
    placenum=octave*7+noteletter-'A';
    clef=null;
    staffspacenum=0;
  }

/*------------------------------------------------------------------------
Constructor: Pitch(int ssn)
Purpose:     Initialize pitch information (no letter or clef info)
Parameters:
  Input:  int ssn - place on staff
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Pitch(int ssn)
  {
    noteletter='X';
    octave=0;
    placenum=0;
    clef=null;
    staffspacenum=ssn;
  }

/*------------------------------------------------------------------------
Constructor: Pitch(Pitch p[,Clef c])
Purpose:     Initialize pitch information (copy another Pitch)
Parameters:
  Input:  Pitch p - pitch to copy
          Clef c  - new display clef
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Pitch(Pitch p)
  {
    noteletter=p.noteletter;
    octave=p.octave;
    placenum=p.placenum;
    clef=p.clef;
    staffspacenum=p.staffspacenum;
  }

  public Pitch(Pitch p,Clef c)
  {
    noteletter=p.noteletter;
    octave=p.octave;
    placenum=p.placenum;
    clef=c;
    staffspacenum=c!=null ? c.calcypos(this) : 0;
  }

/*------------------------------------------------------------------------
Method:  void setclef(Clef c)
Purpose: Set clef information and assign Gamut place if it doesn't have one
Parameters:
  Input:  Clef c - new clef information
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setclef(Clef c)
  {
    if (noteletter=='X')
      {
        placenum=c.line1placenum+staffspacenum;
        octave=placenum/7;
        noteletter=(char)(placenum%7+'A');
      }
    clef=c;
    staffspacenum=c!=null ? c.calcypos(this) : 0;
  }

  public void setOctave(int o)
  {
    octave=o;
    placenum=calcplacenum(noteletter,octave);
  }

/*------------------------------------------------------------------------
Method:  Pitch add(int offset)
Purpose: Change pitch
Parameters:
  Input:  int offset - amount to shift pitch
  Output: -
  Return: this
------------------------------------------------------------------------*/

  public Pitch add(int offset)
  {
    if (noteletter=='X')
      staffspacenum+=offset;
    else
      {
        placenum+=offset;
        octave=placenum/7;
        noteletter=(char)(placenum%7+'A');
        staffspacenum+=offset;
      }

    return this;
  }

/*------------------------------------------------------------------------
Method:  Pitch closestpitch(char nl)
Purpose: Calculates closest pitch with a given letter
Parameters:
  Input:  char nl - letter of pitch to calculate
  Output: -
  Return: closest pitch with letter nl
------------------------------------------------------------------------*/

  public Pitch closestpitch(char nl)
  {
    int newoctave=octave-1,
        diff=Math.abs(calcplacenum(nl,newoctave)-placenum);
    while (Math.abs(calcplacenum(nl,newoctave+1)-placenum)<diff)
      {
        newoctave++;
        diff=Math.abs(calcplacenum(nl,newoctave)-placenum);
      }
    return new Pitch(nl,newoctave,clef);
  }

/*------------------------------------------------------------------------
Method:  int calcypos(Clef c)
Purpose: Calculates staff position on which to display a pitch
Parameters:
  Input:  Clef c - clef for calculating position
  Output: -
  Return: y line/space position for displaying pitch
------------------------------------------------------------------------*/

  public int calcypos(Clef c)
  {
    if (c!=null)
      staffspacenum=c.calcypos(this);
    return staffspacenum;
  }

/*------------------------------------------------------------------------
Method:  boolean equals(Pitch other)
Purpose: Calculate whether this pitch equals another one
Parameters:
  Input:  Pitch other - pitch against which to compare
  Output: -
  Return: Whether pitches are equal
------------------------------------------------------------------------*/

  public boolean equals(Pitch other)
  {
    return this.placenum==other.placenum;
  }

/*------------------------------------------------------------------------
Method:  boolean is[Higher|Lower]Than(Pitch other)
Purpose: Compare against another pitch
Parameters:
  Input:  Pitch p - pitch against which to compare
  Output: -
  Return: true if this is higher/lower than other
------------------------------------------------------------------------*/

  public boolean isHigherThan(Pitch other)
  {
    return this.placenum>other.placenum;
  }

  public boolean isLowerThan(Pitch other)
  {
    return this.placenum<other.placenum;
  }

/*------------------------------------------------------------------------
Method:  int toMIDIPitch()
Purpose: Convert to MIDI pitch number (12-tone, middle C=60); no accidentals
Parameters:
  Input:  -
  Output: -
  Return: MIDI pitch integer
------------------------------------------------------------------------*/

  /* convert one letter to a 12-tone pitch number relative to A */
  static int letterToMIDIPitch(char nl)
  {
    int p=(nl-'A')*2;
    if (nl>'B')
      p--;
    if (nl>'E')
      p--;
    return p;
  }

  public int toMIDIPitch()
  {
    return (octave*12)+21+letterToMIDIPitch(noteletter);
  }

/*------------------------------------------------------------------------
Method:  String toString()
Purpose: Convert pitch to string
Parameters:
  Input:  -
  Output: -
  Return: string representation of pitch
------------------------------------------------------------------------*/

  public String toString()
  {
    return noteletter+Integer.toString(octave);
  }
}
