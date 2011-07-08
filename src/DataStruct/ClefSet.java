/*----------------------------------------------------------------------*/
/*

        Module          : ClefSet.java

        Package         : DataStruct

        Classes Included: ClefSet

        Purpose         : Handle grouped clefs

        Programmer      : Ted Dumitrescu

        Date Started    : 4/6/05

        Updates         : 7/5/05: now extends class ArrayList rather than
                                  wrapping it in a separate variable

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   ClefSet
Extends: java.util.ArrayList<Clef>
Purpose: Clef group information structure
------------------------------------------------------------------------*/

public class ClefSet extends ArrayList<Clef>
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  ModernKeySignature keySig;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: ClefSet(Clef c)
Purpose:     Initialize clef group information
Parameters:
  Input:  Clef c - first clef in group
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public ClefSet(Clef c)
  {
    super();
    keySig=new ModernKeySignature();
    add(c);
    keySig.addClef(c);
  }

  /* (shallow) copy existing clefset */
  public ClefSet(ClefSet cs)
  {
    super();
    for (Clef c : cs)
      add(c);
    keySig=new ModernKeySignature(cs.keySig);
  }

/*------------------------------------------------------------------------
Method:  ClefSet addclef(Clef c)
Purpose: Add clef to group
Parameters:
  Input:  Clef c - clef to add
  Output: -
  Return: this object after modification
------------------------------------------------------------------------*/

  public ClefSet addclef(Clef c)
  {
    if (c.ismodernclef)
      {
        choosemodernclef(c);

        /* avoid duplicated accidentals in modern clef sets */
        if (containsClef(c) && this.indexOf(c)==-1) /* set clef type to NONE if it
                                                       duplicates another one in the set,
                                                       but not if this actual clef object
                                                       is already in the set */
          c.cleftype=Clef.CLEF_NONE;
      }
    if (c.cleftype!=Clef.CLEF_NONE && !containsClef(c))
      {
        if (c.isprincipalclef())
          add(0,c);
        else
          add(c);
        keySig.addClef(c);
      }

    return this;
  }

/*------------------------------------------------------------------------
Method:  void removeclef(Clef c)
Purpose: Remove clef from group
Parameters:
  Input:  Clef c - clef to remove
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void removeclef(Clef c)
  {
    int     i=0;
    boolean done=i>=size();
    while (!done)
      {
        Clef ic=(Clef)get(i);
        if (ic.equals(c))
          {
            remove(i);
            keySig.removeClef(c);
            done=true;
          }
        else
          done=i>=size();
        i++;
      }
  }

/*------------------------------------------------------------------------
Method:  void choosemodernclef(Clef c)
Purpose: Set modern clef parameters for a new clef within this set
Parameters:
  Input:  -
  Output: Clef c - clef to modify
  Return: -
------------------------------------------------------------------------*/

  void choosemodernclef(Clef c)
  {
    if (c.getclefletter()!='B' || !c.signature)
      return;

    Pitch pp=getprincipalclef().pitch,
          newpitch=c.pitch;
    switch (pp.noteletter)
      {
        case 'G':
          newpitch=new Pitch(c.pitch.noteletter,pp.octave+1);
          break;
        case 'F':
        case 'C':
          newpitch=new Pitch(c.pitch.noteletter,pp.octave);
          break;
      }
    c.pitch=newpitch;
  }

/*------------------------------------------------------------------------
Method:  void recalcModKeySig()
Purpose: Recalculate modern key signature data (after clef set has changed)
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void recalcModKeySig()
  {
    keySig=new ModernKeySignature();
    for (Clef c : this)
      keySig.addClef(c);
  }

/*------------------------------------------------------------------------
Method:  Clef getprincipalclef()
Purpose: Return the principal clef in this set
Parameters:
  Input:  -
  Output: -
  Return: principal clef
------------------------------------------------------------------------*/

  public Clef getprincipalclef()
  {
    return (Clef)(get(0));
  }

  public boolean hasPrincipalClef()
  {
    for (Clef c : this)
      if (c.isprincipalclef())
        return true;
    return false;
  }

/*------------------------------------------------------------------------
Method:  int numflats()
Purpose: Return number of flat clefs (round bs) in set
Parameters:
  Input:  -
  Output: -
  Return: number of flats
------------------------------------------------------------------------*/

  public int numflats()
  {
    int nf=0;
    for (Clef c : this)
      if (c.isflat())
        nf++;
    return nf;
  }

/*------------------------------------------------------------------------
Method:  boolean contradicts(ClefSet other,boolean umc,Clef smc)
Purpose: Calculate whether this clef set contradicts another one
Parameters:
  Input:  ClefSet other - clef set for comparison
          boolean umc   - use modern clefs?
          Clef smc      - editorially suggested modern clef for this voice
  Output: -
  Return: Whether clefs conflict
------------------------------------------------------------------------*/

  public boolean contradicts(ClefSet other)
  {
    return contradicts(other,false,null);
  }

  public boolean contradicts(ClefSet other,boolean umc,Clef smc)
  {
    if (other==null)
      return true;

    if (umc)
      return !this.getKeySig().equals(other.getKeySig());

    Iterator ci1=iterator(),
             ci2=other.iterator();

    while (ci1.hasNext())
      if (!ci2.hasNext())
        return true;
      else
        {
          Clef c1c=(Clef)(ci1.next()),
               c2c=(Clef)(ci2.next());
          if (umc && c1c.isprincipalclef() && c2c.isprincipalclef())
            if (smc!=null)
              c1c=c2c=smc;
            else
              {
                c1c=c1c.modernclef;
                c2c=c2c.modernclef;
              }
          if (!c1c.equals(c2c))
            return true;
        }
    return ci2.hasNext();
  }

  public boolean sigContradicts(ClefSet other)
  {
    if (other==null)
      return true;

    Iterator ci1=iterator(),
             ci2=other.iterator();
    Clef     c1c=null,c2c=null;

    /* iterate past principal clefs */
    while (ci1.hasNext())
      {
        c1c=(Clef)(ci1.next());
        if (!c1c.isprincipalclef())
          break;
      }
    while (ci2.hasNext())
      {
        c2c=(Clef)(ci2.next());
        if (!c2c.isprincipalclef())
          break;
      }

    while (c1c!=null)
      if (c2c==null)
        return true;
      else
        {
          if (!c1c.equals(c2c))
            return true;
          c1c=ci1.hasNext() ? (Clef)(ci1.next())  : null;
          c2c=ci2.hasNext() ? (Clef)(ci2.next())  : null;
        }
    return c2c!=null;
  }

/*------------------------------------------------------------------------
Method:  boolean containsClef(Clef c)
Purpose: Calculate whether this clef set contains a clef
Parameters:
  Input:  Clef c - clef to check for inclusion
  Output: -
  Return: Whether c is contained in this
------------------------------------------------------------------------*/

  public boolean containsClef(Clef c)
  {
    for (Clef curc : this)
      if (curc.equals(c))
        return true;
    return false;
  }

/*------------------------------------------------------------------------
Method:  Iterator acciterator()
Purpose: Creates iterator over less principal clefs (accidentals) in set
Parameters:
  Input:  -
  Output: -
  Return: iterator
------------------------------------------------------------------------*/

  public Iterator acciterator()
  {
    return listIterator(1);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public ModernKeySignature getKeySig()
  {
    return keySig;
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this clef set
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("   Clefset:");
    for (Clef c : this)
      c.prettyprint();
    System.out.println("   End ClefSet");
  }
}
