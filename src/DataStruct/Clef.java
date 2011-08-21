/*----------------------------------------------------------------------*/
/*

        Module          : Clef.java

        Package         : DataStruct

        Classes Included: Clef

        Purpose         : Handle low-level clef information

        Programmer      : Ted Dumitrescu

        Date Started    : 1/99

        Updates         : 4/26/99: modernization option added
                          2/25/05: imported clef type enum from ClefEvent,
                                   nominal support for new types

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   Clef
Extends: -
Purpose: Clef information structure
------------------------------------------------------------------------*/

public class Clef
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int CLEF_C=                     0,
                          CLEF_F=                     1,
                          CLEF_G=                     2,
                          CLEF_MODERNG=               3,
                          CLEF_MODERNG8=              4,
                          CLEF_MODERNF=               5,
                          CLEF_MODERNC=               6,
                          CLEF_Bmol=                  7,
                          CLEF_Bqua=                  8,
                          CLEF_Diesis=                9,
                          CLEF_BmolDouble=            10,
                          CLEF_Fis=                   11,
                          CLEF_Frnd=                  12,
                          CLEF_Fsqr=                  13,
                          CLEF_Gamma=                 14,
                          CLEF_MODERNFlat=            15,
                          CLEF_MODERNNatural=         16,
                          CLEF_MODERNSharp=           17,
                          CLEF_MODERNDoubleSharp=     18,
                          CLEF_MODERNFlatSMALL=       19,
                          CLEF_MODERNNaturalSMALL=    20,
                          CLEF_MODERNSharpSMALL=      21,
                          CLEF_MODERNDoubleSharpSMALL=22,
                          CLEF_CFull=                 23,
                          CLEF_FFull=                 24,
                          CLEF_NONE=                  25;
  public static String[]  ClefNames=new String[]
                            {
                              "C","F","G",
                              "MODERNG","MODERNG8","MODERNF","MODERNC",
                              "Bmol","Bqua","Diesis","BmolDouble","Fis",
                              "Frnd","Fsqr",
                              "Gamma",
                              "Flat","Natural","Sharp","DoubleSharp",
                              "Flat(Small)","Natural(Small)","Sharp(Small)","DoubleSharp(Small)",
                              "C","F",
                              "NONE"
                            };
  public static char[]    ClefLetters=new char[]
                            {
                              'C','F','G',
                              'G','G','F','C',
                              'B','B','B','B','B',
                              'F','F',
                              'G',
                              'B','B','B','B',
                              'B','B','B','B',
                              'C','F',
                              'X'
                            };
  public static Pitch[]   DefaultClefPitches=new Pitch[]
                            {
                              new Pitch('C',3),new Pitch('F',2),new Pitch('G',3),
                              new Pitch('G',3),new Pitch('G',2),new Pitch('F',2),new Pitch('C',3),
                              new Pitch('B',3),new Pitch('B',3),new Pitch('B',3),new Pitch('B',3),new Pitch('B',3),
                              new Pitch('F',2),new Pitch('F',2),
                              new Pitch('G',1),
                              new Pitch('B',3),new Pitch('B',3),new Pitch('B',3),new Pitch('B',3),
                              new Pitch('B',3),new Pitch('B',3),new Pitch('B',3),new Pitch('B',3),
                              new Pitch('C',3),new Pitch('F',2),
                              null
                            };
  public static final Clef[] DefaultModernClefs=new Clef[]
    {
      null,null,null, /* non-modern clefs at start of list */
      new Clef(CLEF_MODERNG,3,DefaultClefPitches[CLEF_MODERNG],true,true,null),
      new Clef(CLEF_MODERNG8,3,DefaultClefPitches[CLEF_MODERNG8],true,true,null),
      new Clef(CLEF_MODERNF,7,DefaultClefPitches[CLEF_MODERNF],true,true,null)
    };

  public static final int OFFSET_SMALL_ACC=4; /* add to regular modern accidental
                                                 to get number of small version */

/*----------------------------------------------------------------------*/
/* Instance variables */

  public int     cleftype,
                 linespacenum,
                 line1placenum;
  public Pitch   pitch;
  public Clef    modernclef,
                 origModClef;
  public boolean ismodernclef=false;
  boolean        signature,
                 drawInSig; /* whether to draw when constructing signature display */

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  boolean isFlatType(int ct)
Purpose: Determine whether this type of clef is a type of flat
Parameters:
  Input:  int ct - clef type
  Output: -
  Return: true if this clef type is a flat/round b
------------------------------------------------------------------------*/

  static public boolean isFlatType(int ct)
  {
    return ct==CLEF_Bmol       ||
           ct==CLEF_BmolDouble ||
           ct==CLEF_Fis        ||
           ct==CLEF_MODERNFlat;
  }

/*------------------------------------------------------------------------
Method:  int defaultClefLoc(int ct)
Purpose: Determine default staff location based on clef type
Parameters:
  Input:  int ct - clef type
  Output: -
  Return: staff location
------------------------------------------------------------------------*/

  static public int defaultClefLoc(int ct)
  {
    switch (ClefLetters[ct])
      {
        case 'C':
          return 1;
        case 'F':
          return 7;
        case 'G':
          return 3;
        case 'B':
          return 7;
      }
    return 1;
  }

/*------------------------------------------------------------------------
Method:  int strToCleftype(String s)
Purpose: Translate string to clef type number
Parameters:
  Input:  String s - string to translate
  Output: -
  Return: clef type
------------------------------------------------------------------------*/

  static public int strToCleftype(String s)
  {
    for (int i=0; i<ClefNames.length; i++)
      if (s.equals(Clef.ClefNames[i]))
        return i;
    return CLEF_NONE;
  }

/*------------------------------------------------------------------------
Method:  int lineNumToLinespaceNum(int lnum)
Purpose: Translate staff line number (1-5) to line-space number
Parameters:
  Input:  int lnum - staff line number
  Output: -
  Return: line-space number
------------------------------------------------------------------------*/

  static public int lineNumToLinespaceNum(int lnum)
  {
    return lnum*2-1;
  }

  static public int linespaceNumToLineNum(int lsnum)
  {
    return lsnum/2+1;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: Clef(int ct,int ln,Pitch p,boolean mc,boolean sc,Clef dc)
Purpose:     Initialize clef information
Parameters:
  Input:  int ct     - clef type (e.g., CLEF_Bmol)
          int ln     - staff location of visible clef
          Pitch p    - pitch represented by clef
          boolean mc - is this a modern clef?
          boolean sc - is this a signature clef?
          Clef dc    - principal clef for display information
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Clef(int ct,int ln,Pitch p,boolean mc,boolean sc,Clef dc)
  {
    setattributes(ct,ln,p,mc,sc,dc);
  }

/*------------------------------------------------------------------------
Constructor: Clef(Clef c)
Purpose:     Initialize clef information (copy another Clef)
Parameters:
  Input:  Clef c - clef to copy
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public Clef(Clef c)
  {
    cleftype=c.cleftype;
    linespacenum=c.linespacenum;
    line1placenum=c.line1placenum;
    pitch=new Pitch(c.pitch);
    modernclef=c.modernclef;
    origModClef=c.origModClef;
    ismodernclef=c.ismodernclef;
    signature=c.signature;
    drawInSig=true;
  }

/*------------------------------------------------------------------------
Method : void setattributes(int ct,int ln,Pitch p,boolean mc,boolean sc,Clef dc)
Purpose: Initialize (or re-initialize) clef information
Parameters:
  Input:  int ct     - clef type (e.g., CLEF_Bmol)
          int ln     - staff location of visible clef
          Pitch p    - pitch represented by clef
          boolean mc - is this a modern clef?
          boolean sc - is this a signature clef?
          Clef dc    - principal clef for display information
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setattributes(int ct,int ln,Pitch p,boolean mc,boolean sc,Clef dc)
  {
    cleftype=ct;
    linespacenum=ln;
    pitch=new Pitch(p,dc);
    signature=sc;
    drawInSig=true;

    Clef moderndc=dc==null ? null : dc.modernclef;

    if (isflat() || issharp())
      linespacenum=pitch.staffspacenum+1;

    line1placenum=pitch.placenum-(linespacenum-1);
    ismodernclef=mc;
    modernclef=origModClef=this;

    if (!ismodernclef)
      {
        switch (cleftype)
          {
            case CLEF_C:
            case CLEF_CFull:
              if (linespacenum<=3)
                modernclef=DefaultModernClefs[CLEF_MODERNG];
              else if (linespacenum>7)
                modernclef=DefaultModernClefs[CLEF_MODERNF];
              else
                modernclef=DefaultModernClefs[CLEF_MODERNG8];
              break;
            case CLEF_F:
            case CLEF_FFull:
            case CLEF_Frnd:
            case CLEF_Fsqr:
            case CLEF_Gamma:
              modernclef=DefaultModernClefs[CLEF_MODERNF];
              break;
            case CLEF_G:
              modernclef=DefaultModernClefs[CLEF_MODERNG];
              break;
            case CLEF_Bmol:
            case CLEF_BmolDouble:
              if (signature && pitch.noteletter=='B')
                {
                  if (pitch.octave==4)
                    modernclef=new Clef(CLEF_MODERNFlat,5,new Pitch('B',4),true,signature,moderndc);
                  else if (pitch.octave==3)
                    modernclef=new Clef(CLEF_MODERNFlat,5,new Pitch('B',3),true,signature,moderndc);
                  else if (pitch.octave==2)
                    modernclef=new Clef(CLEF_MODERNFlat,3,new Pitch('B',2),true,signature,moderndc);
                }
              else
                modernclef=new Clef(CLEF_MODERNFlat,0,pitch,true,signature,moderndc);
              break;
            case CLEF_Bqua:
            case CLEF_Diesis:
            case CLEF_Fis:
              modernclef=new Clef(
                pitch.noteletter=='B' || pitch.noteletter=='E' ? 
                  CLEF_MODERNNatural : CLEF_MODERNSharp,
                ln,p,true,signature,moderndc);
              break;
          }
      }
    origModClef=new Clef(modernclef);
  }

/*------------------------------------------------------------------------
Method:  char getclefletter()
Purpose: Return letter name for this clef type
Parameters:
  Input:  -
  Output: -
  Return: letter of clef type
------------------------------------------------------------------------*/

  public char getclefletter()
  {
    return ClefLetters[cleftype];
  }

/*------------------------------------------------------------------------
Method:  int calcypos(Pitch p)
Purpose: Calculates staff position on which to display a pitched event,
         using this as display clef
Parameters:
  Input:  Pitch p - pitch of event
  Output: -
  Return: y line/space position for displaying event
------------------------------------------------------------------------*/

  public int calcypos(Pitch p)
  {
    return p.placenum-line1placenum;
  }

/*------------------------------------------------------------------------
Method:  int getypos(Clef displayclef)
Purpose: Calculates staff position on which to display this clef
Parameters:
  Input:  Clef displayclef - currently valid principal clef
  Output: -
  Return: y line/space position for displaying this clef
------------------------------------------------------------------------*/

  public int getypos(Clef displayclef)
  {
    if (displayclef==null || ClefLetters[cleftype]!='B')
      return linespacenum-1;
    else
      return displayclef.calcypos(pitch);
  }

/*------------------------------------------------------------------------
Method:  boolean isprincipalclef()
Purpose: Determine whether this clef is a type of principal clef
Parameters:
  Input:  -
  Output: -
  Return: true if this clef is a principal clef
------------------------------------------------------------------------*/

  public boolean isprincipalclef()
  {
    return !isflat() && !issharp() && cleftype!=CLEF_NONE;
  }

/*------------------------------------------------------------------------
Method:  boolean issignatureclef()
Purpose: Determine whether this clef is a signature clef
Parameters:
  Input:  -
  Output: -
  Return: true if this clef is a signature clef
------------------------------------------------------------------------*/

  public boolean issignatureclef()
  {
    return signature;
  }

/*------------------------------------------------------------------------
Method:  boolean isflat()
Purpose: Determine whether this clef is a type of flat
Parameters:
  Input:  -
  Output: -
  Return: true if this clef is a flat/round b
------------------------------------------------------------------------*/

  public boolean isflat()
  {
    return isFlatType(cleftype);
  }

/*------------------------------------------------------------------------
Method:  boolean issharp()
Purpose: Determine whether this clef is a type of sharp/natural
Parameters:
  Input:  -
  Output: -
  Return: true if this clef is a sharp/natural
------------------------------------------------------------------------*/

  public boolean issharp()
  {
    return cleftype==CLEF_Bqua          ||
           cleftype==CLEF_Diesis        ||
           cleftype==CLEF_MODERNNatural ||
           cleftype==CLEF_MODERNSharp;
  }

/*------------------------------------------------------------------------
Method:  boolean equals(Clef other)
Purpose: Calculate whether this clef equals another one
Parameters:
  Input:  Clef other - clef against which to compare
  Output: -
  Return: Whether clefs are equal
------------------------------------------------------------------------*/

  public boolean equals(Clef other)
  {
    if (issharp())
      return other.issharp() && pitch.equals(other.pitch);
    else if (isflat())
      return other.isflat() && pitch.equals(other.pitch);
    else if (this.cleftype==other.cleftype &&//ClefLetters[cleftype]==ClefLetters[other.cleftype] &&
             linespacenum==other.linespacenum &&
             pitch.equals(other.pitch))
      return true;
    return false;
  }

/*------------------------------------------------------------------------
Method:  boolean contradicts(Clef other)
Purpose: Calculate whether this clef contradicts another one
Parameters:
  Input:  Clef other - clef against which to compare
  Output: -
  Return: Whether clefs conflict
------------------------------------------------------------------------*/

  public boolean contradicts(Clef other)
  {
    return (line1placenum!=other.line1placenum);
  }

/*------------------------------------------------------------------------
Method:  int getApos()
Purpose: Determine position of A below middle C on staff with this clef
Parameters:
  Input:  -
  Output: -
  Return: position of 'A' on staff
------------------------------------------------------------------------*/

  public int getApos()
  {
    return 21-line1placenum;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public int getcleftype()
  {
    return cleftype;
  }

  public int getloc()
  {
    return linespacenum;
  }

  public Pitch getStaffLocPitch(int staffLoc)
  {
    if (pitch!=null)
      return new Pitch(pitch).add(staffLoc-linespacenum);
    else
      return new Pitch(staffLoc);
  }

  public boolean ismodernclef()
  {
    return ismodernclef;
  }

  public boolean signature()
  {
    return signature;
  }

  public boolean drawInSig()
  {
    return drawInSig;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void resetModClef()
  {
    modernclef=new Clef(origModClef);
  }

  public void setSignature(boolean s)
  {
    signature=s;
  }

  public void setDrawInSig(boolean d)
  {
    drawInSig=d;
  }

  public void setFill(int filltype)
  {
    if (filltype==Coloration.VOID)
      {
        /* only affects main C and F clefs */
        if (cleftype==CLEF_CFull)
          cleftype=CLEF_C;
        else if (cleftype==CLEF_FFull)
          cleftype=CLEF_F;
      }
    else if (filltype==Coloration.FULL)
      {
        if (cleftype==CLEF_C)
          cleftype=CLEF_CFull;
        else if (cleftype==CLEF_F)
          cleftype=CLEF_FFull;
      }
  }

/*------------------------------------------------------------------------
Method:  void prettyprint()
Purpose: Prints information about this clef
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void prettyprint()
  {
    System.out.println("    "+this);
  }

  public String toString()
  {
    return "Clef: "+ClefNames[cleftype]+linespacenum;
  }
}
