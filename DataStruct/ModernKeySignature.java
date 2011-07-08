/*----------------------------------------------------------------------*/
/*

        Module          : ModernKeySignature.java

        Package         : DataStruct

        Classes Included: ModernKeySignature

        Purpose         : Handle low-level key signature information for modern
                          notation (sharp/flat/natural rather than square b/
                          diesis/round b)

        Programmer      : Ted Dumitrescu

        Date Started    : 7/25/06

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

/*------------------------------------------------------------------------
Class:   ModernKeySignature
Extends: -
Purpose: Modern key signature information structure
------------------------------------------------------------------------*/

public class ModernKeySignature
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final ModernKeySignature DEFAULT_SIG=new ModernKeySignature();

  public static final Pitch[] CircleOfFifthsPitches=new Pitch[]
                               {
                                 new Pitch('B',-1),
                                 new Pitch('E',-1),
                                 new Pitch('A',-1),
                                 new Pitch('D',-1),
                                 new Pitch('G',-1),
                                 new Pitch('C',-1),
                                 new Pitch('F',-1)
                               };

  /* octave offsets for positioning elements of a standard key signature */
  public static final int[]  SigFlatOctaveOffsets=new int[]
                               {
                                 0,0,0,0,0,-1,-1 /* ABCDEFG */
                               },
                             SigSharpOctaveOffsets=new int[]
                               {
                                 0,0,0,0,0,0,0
                               };

/*----------------------------------------------------------------------*/
/* Instance variables */

  LinkedList<ModernKeySignatureElement> accElements;

  /* pitch 'distance' from a blank sig; 1==1 sharp, -1==1 flat, 2==2 sharps,
     etc */
  int accDistance;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ModernKeySignature()
Purpose:     Initialize modern key signature information
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ModernKeySignature()
  {
    accElements=new LinkedList<ModernKeySignatureElement>();
    accDistance=0;
  }

  /* copy another key signature */
  public ModernKeySignature(ModernKeySignature other)
  {
    /* make a deep copy of the other list */
    accElements=new LinkedList<ModernKeySignatureElement>();
    for (Iterator i=other.iterator(); i.hasNext();)
      accElements.add(new ModernKeySignatureElement((ModernKeySignatureElement)i.next()));

    accDistance=other.accDistance;
  }

/*------------------------------------------------------------------------
Method:  int calcNotePitchOffset(NoteEvent ne)
Purpose: Calculates a numerical representation of the pitch offset from a
         note's natural state, under this key signature (e.g., a B with no
         accidental marked, under a 1-flat sig, will be -1, i.e., a half
         step below B-natural)
Parameters:
  Input:  NoteEvent ne - note to use for calculation
  Output: -
  Return: numerical pitch offset value
------------------------------------------------------------------------*/

  public int calcNotePitchOffset(Pitch pitch,ModernAccidental noteAcc)
  {
    ModernAccidental sigAcc=getAccidentalAtPitch(pitch);
    int notePitchOffset=sigAcc==null ? 0 : sigAcc.calcPitchOffset();
    if (noteAcc!=null)
      notePitchOffset=noteAcc.calcPitchOffset();

    return notePitchOffset;
  }

  public int calcNotePitchOffset(NoteEvent ne)
  {
    return calcNotePitchOffset(ne.getPitch(),ne.getPitchOffset());
  }

/*------------------------------------------------------------------------
Method:  ModernAccidental chooseNoteAccidental(NoteEvent ne,int notePitchOffset)
Purpose: Creates a modern accidental to attach to a given note so that it has
         the given numerical offset from a 'natural' pitch (0)
Parameters:
  Input:  NoteEvent ne        - note to which to attach modern accidental
          int notePitchOffset - numerical representation of exact pitch offset
                                from natural note state
  Output: -
  Return: new modern accidental
------------------------------------------------------------------------*/

  public ModernAccidental chooseNoteAccidental(NoteEvent ne,int notePitchOffset)
  {
    ModernAccidental sigAcc=getAccidentalAtPitch(ne.getPitch());
    int sigPitchOffset=sigAcc==null ? 0 : sigAcc.calcPitchOffset();

    /* is the accidental subsumed into the signature? */
    if (notePitchOffset==sigPitchOffset)
      return null;
    else
      /* does it become a natural? */
      if (notePitchOffset==0)
        return new ModernAccidental(ModernAccidental.ACC_Natural,1);
      else
        return ModernAccidental.pitchOffsetToAcc(notePitchOffset);
  }

/*------------------------------------------------------------------------
Method:  ModernAccidental getAccidentalAtPitch(Pitch p)
Purpose: Checks whether a given Locus (generic pitch position) is modified
         by this key signature; if so, returns accidental information
Parameters:
  Input:  Pitch p - pitch to check for accidental
  Output: -
  Return: accidental associated with given pitch; null if none
------------------------------------------------------------------------*/

  public int getAccDistance()
  {
    return accDistance;
  }

  public ModernAccidental getAccidentalAtPitch(Pitch p)
  {
    for (ModernKeySignatureElement mske : accElements)
      if (mske.matchesPitch(p))
        return mske.accidental;
    return null;
  }

/*------------------------------------------------------------------------
Methods: boolean equals(ModernKeySignature other)
Purpose: Check whether this modern key signature is equivalent to another
Parameters:
  Input:  ModernKeySignature other - other signature
  Output: -
  Return: true if this and the other signature are the same
------------------------------------------------------------------------*/

  public boolean equals(ModernKeySignature other)
  {
    return this.accDistance==other.accDistance;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public Iterator iterator()
  {
    return accElements.iterator();
  }

  public int numEls()
  {
    return accElements.size();
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void addElement(ModernKeySignatureElement e)
  {
    ModernAccidental ma=getAccidentalAtPitch(e.pitch);
    if (ma!=null)
      return; /* already in signature */

    accElements.add(e);
    if (e.accidental.accType==ModernAccidental.ACC_Flat)
      accDistance-=e.accidental.numAcc;
    else if (e.accidental.accType==ModernAccidental.ACC_Sharp)
      accDistance+=e.accidental.numAcc;
  }

  public void addClef(Clef c)
  {
    if (!c.isflat() && !c.issharp())
      return;
    addElement(new ModernKeySignatureElement(c));
  }

  public void removeClef(Clef c)
  {
/* TMP */
  }

  public void addFlat()
  {
    /* if sig is currently blank, add first item */
    if (accElements.size()==0)
      accElements.add(new ModernKeySignatureElement(
        CircleOfFifthsPitches[0],new ModernAccidental(ModernAccidental.ACC_Flat)));

    else
      {
        ModernKeySignatureElement laste=accElements.getLast();

        /* for sigs of less than seven elements, simply add or delete one item */
        if (accElements.size()<7)
          if (laste.accidental.accType==ModernAccidental.ACC_Sharp)
            accElements.removeLast();
          else
            accElements.add(new ModernKeySignatureElement(
              CircleOfFifthsPitches[accElements.size()],
              new ModernAccidental(ModernAccidental.ACC_Flat)));

        /* for 'full' sigs (7 or more), we may need to change the number of
           applications of the farthest accidental */
        else
          {
            /* make laste correspond with the 'furthest' accidental along the
               cycle of fifths */
            laste=accElements.get((Math.abs(accDistance)-1)%7);

            if (laste.accidental.accType==ModernAccidental.ACC_Sharp)
              if (accDistance==7)
                accElements.removeLast(); /* remove one sharp */
              else
                laste.accidental.numAcc--; /* remove one sharp application */
            else
              {
                laste=accElements.get(Math.abs(accDistance)%7);
                laste.accidental.numAcc++; /* add one flat application */
              }
          }
      }

    accDistance--;
  }

  public void addSharp()
  {
    /* if sig is currently blank, add first item */
    if (accElements.size()==0)
      accElements.add(new ModernKeySignatureElement(
        CircleOfFifthsPitches[6],new ModernAccidental(ModernAccidental.ACC_Sharp)));

    else
      {
        ModernKeySignatureElement laste=accElements.getLast();

        /* for sigs of less than seven elements, simply add or delete one item */
        if (accElements.size()<7)
          if (laste.accidental.accType==ModernAccidental.ACC_Flat)
            accElements.removeLast();
          else
            accElements.add(new ModernKeySignatureElement(
              CircleOfFifthsPitches[6-accElements.size()],
              new ModernAccidental(ModernAccidental.ACC_Sharp)));

        /* for 'full' sigs (7 or more), we may need to change the number of
           applications of the farthest accidental */
        else
          {
            /* make laste correspond with the 'furthest' accidental along the
               cycle of fifths */
            laste=accElements.get((Math.abs(accDistance)-1)%7);

            if (laste.accidental.accType==ModernAccidental.ACC_Flat)
              if (accDistance==-7)
                accElements.removeLast(); /* remove one flat */
              else
                laste.accidental.numAcc--; /* remove one flat application */
            else
              {
                laste=accElements.get(Math.abs(accDistance)%7);
                laste.accidental.numAcc++; /* add one sharp application */
              }
          }
      }

    accDistance++;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this key signature
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("    --Modern key signature");
    for (ModernKeySignatureElement mske : accElements)
      mske.prettyprint();
    System.out.println("    --End modern key signature");
  }
}
