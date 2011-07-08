/*----------------------------------------------------------------------*/
/*

        Module          : ModernKeySignatureElement.java

        Package         : DataStruct

        Classes Included: ModernKeySignatureElement

        Purpose         : Handle one element (flat, sharp) in a modern key
                          signature

        Programmer      : Ted Dumitrescu

        Date Started    : 7/25/06

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   ModernKeySignatureElement
Extends: -
Purpose: One accidental element in a key signature
------------------------------------------------------------------------*/

public class ModernKeySignatureElement
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  public Pitch            pitch;
  public ModernAccidental accidental;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ModernKeySignatureElement([Pitch p,ModernAccidental a]|[Clef c])
Purpose:     Initialize one signature element
Parameters:
  Input:  Pitch p            - pitch of accidental
          ModernAccidental a - accidental
          Clef c             - less-principal clef
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ModernKeySignatureElement(Pitch p,ModernAccidental a)
  {
    pitch=p;
    p.octave=-1;
    accidental=a;
  }

  public ModernKeySignatureElement(Clef c)
  {
    pitch=new Pitch(c.pitch);
//    if (c.ismodernclef())
      pitch.octave=-1; /* for modern clef sets, key signatures use octave duplication */

    if (c.isflat())
      accidental=new ModernAccidental(ModernAccidental.ACC_Flat);
    else if (c.issharp())
      accidental=new ModernAccidental(ModernAccidental.ACC_Sharp);
    else
      accidental=null;
  }

  /* copy another object */
  public ModernKeySignatureElement(ModernKeySignatureElement other)
  {
    pitch=new Pitch(other.pitch);
    accidental=new ModernAccidental(other.accidental);
  }

/*------------------------------------------------------------------------
Method:  boolean matchesPitch(Pitch p)
Purpose: Pitch equality test, allowing for accidentals with octave duplication
         (octave==-1)
Parameters:
  Input:  Pitch p - pitch to test against this accidental's pitch
  Output: -
  Return: true if pitches are equal
------------------------------------------------------------------------*/

  public boolean matchesPitch(Pitch p)
  {
    if (pitch.octave==-1)
      return pitch.noteletter==p.noteletter;
    else
      return pitch.equals(p);
  }

/*------------------------------------------------------------------------
Method:  int calcAOffset()
Purpose: Calculates staff-height display offset from generic pitch 'A'
Parameters:
  Input:  -
  Output: -
  Return: staff-height offset from A
------------------------------------------------------------------------*/

  public int calcAOffset()
  {
    if (accidental.accType==ModernAccidental.ACC_Flat)
      return pitch.noteletter-'A'+
             7*ModernKeySignature.SigFlatOctaveOffsets[pitch.noteletter-'A'];
    else
      return pitch.noteletter-'A'+
             7*ModernKeySignature.SigSharpOctaveOffsets[pitch.noteletter-'A'];
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
    System.out.print("    Pitch: "+pitch.noteletter);
    if (pitch.octave!=-1)
      System.out.print("("+pitch.octave+")");
    System.out.println();
    accidental.prettyprint();
  }
}
