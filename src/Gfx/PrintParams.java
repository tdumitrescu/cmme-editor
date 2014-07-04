/*----------------------------------------------------------------------*/
/*

        Module          : PrintParams

        Package         : Gfx

        Classes	Included: PrintParams

        Purpose         : Manipulate size parameters for printing

        Programmer      : Ted Dumitrescu

        Date Started    : 7/10/06

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*------------------------------------------------------------------------
Class:   PrintParams
Extends: -
Purpose: Store/manipulate information about related sizing parameters for
         printing music (e.g., staff gage, page margins, font sizes)
------------------------------------------------------------------------*/

public class PrintParams
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int DEFAULT_BookExample1=   0,
                          DEFAULT_A4ScorePortrait=1,
                          DEFAULT_A4PartLandscape=2;

  private static final PrintParams DEFAULTS[]=new PrintParams[]
    {
      /* BookExample1 - for printing within writing block 4.75 inches wide */
      new PrintParams(
        4.75f*72,-1,        /* PAGE[X|Y]SIZE */
        0.25f*72,0.125f*72, /* [X|Y]MARGIN */
        .25f,.2f,           /* STAFFLINEWIDTH, LINEXADJUST */
        4.0f,48,            /* STAFFYSCALE, STAFFYSPACE */
        .5f,                /* STEMWIDTH */
        18,6,6,6,6,18,14),  /* [Music|Plain|Text|StaffName|ScoreAnnotation|Title|Subtitle]FONTSIZE */

      /* A4ScorePortrait */
      new PrintParams(
        595.35f,841.95f,    /* PAGE[X|Y]SIZE (210x297 mm) */
        70.8696f,70.8696f,  /* [X|Y]MARGIN (25x25 mm) */
        .4336f,3,           /* STAFFLINEWIDTH, LINEXADJUST */
        4.668f,50f,         /* STAFFYSCALE, STAFFYSPACE */
        .75f,               /* STEMWIDTH */
        22,7,7,7,8,24,20),  /* [Music|Plain|Text|StaffName|ScoreAnnotation|Title|Subtitle]FONTSIZE */

      /* DEFAULT_A4PartLandscape */
      new PrintParams(
        841.95f,595.35f,    /* PAGE[X|Y]SIZE (297x210 mm) */
        70.8696f,70.8696f,  /* [X|Y]MARGIN (25x25 mm) */
        .4336f,.2f,         /* STAFFLINEWIDTH, LINEXADJUST */
        4.668f,50f,         /* STAFFYSCALE, STAFFYSPACE */
        .75f,               /* STEMWIDTH */
        22,7,7,7,8,24,20)   /* [Music|Plain|Text|StaffName|ScoreAnnotation|Title|Subtitle]FONTSIZE */
    };

/*----------------------------------------------------------------------*/         
/* Instance variables */

  public float
    /* page sizing/location parameters (in points, 72/inch) */
    PAGEXSIZE,PAGEYSIZE,
    XMARGIN,YMARGIN,
    YMUSICSTART,DRAWINGSPACEY,

    /* music/staff sizing */
    STAFFXSIZE,STAFFLINEWIDTH,
    LINEXADJUST, /* amount to shift events past left staff margin */
    STAFFYSCALE,STAFFYPOSSCALE,STAFFYSPACE,STAFFYSIZE,
    XYSCALE,
    STEMWIDTH,
    MusicFONTSIZE,PlainFONTSIZE,TextFONTSIZE,
    StaffNameFONTSIZE,ScoreAnnotationFONTSIZE,
    TitleFONTSIZE,SubtitleFONTSIZE;

/*------------------------------------------------------------------------
Constructor: PrintParams(int defaultNum)
Purpose:     Initialize structure by copying a given default
Parameters:
  Input:  int defaultNum - number of default to copy
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public PrintParams(int defaultNum)
  {
    this(DEFAULTS[defaultNum]);
  }

/*------------------------------------------------------------------------
Constructor: PrintParams(PrintParams other)
Purpose:     Initialize structure by copying another
Parameters:
  Input:  PrintParams other - structure to copy
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public PrintParams(PrintParams other)
  {
    PAGEXSIZE=other.PAGEXSIZE;
    PAGEYSIZE=other.PAGEYSIZE;
    XMARGIN=other.XMARGIN;
    YMARGIN=other.YMARGIN;
    YMUSICSTART=other.YMUSICSTART;
    DRAWINGSPACEY=other.DRAWINGSPACEY;

    STAFFXSIZE=other.STAFFXSIZE;
    STAFFLINEWIDTH=other.STAFFLINEWIDTH;
    LINEXADJUST=other.LINEXADJUST;

    STAFFYSCALE=other.STAFFYSCALE;
    STAFFYPOSSCALE=other.STAFFYPOSSCALE;
    STAFFYSPACE=other.STAFFYSPACE;
    STAFFYSIZE=other.STAFFYSIZE;

    STEMWIDTH=other.STEMWIDTH;

    MusicFONTSIZE=other.MusicFONTSIZE;
    PlainFONTSIZE=other.PlainFONTSIZE;
    TextFONTSIZE=other.TextFONTSIZE;
    StaffNameFONTSIZE=other.StaffNameFONTSIZE;
    ScoreAnnotationFONTSIZE=other.ScoreAnnotationFONTSIZE;
    TitleFONTSIZE=other.TitleFONTSIZE;
    SubtitleFONTSIZE=other.SubtitleFONTSIZE;

    XYSCALE=other.XYSCALE;
  }

/*------------------------------------------------------------------------
Constructor: PrintParams(float psx,float psy,float xm,float ym,
                         float stlw,float lxa,
                         float stysc,float stysp,
                         float sw,
                         float mfs,float pfs,float tfs,float snfs,float safs,
                         float titfs,float stitfs)
Purpose:     Initialize structure with given parameters
Parameters:
  Input:  float * - initial values for size parameters
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public PrintParams(float psx,float psy,float xm,float ym,
                     float stlw,float lxa,
                     float stysc,float stysp,
                     float sw,
                     float mfs,float pfs,float tfs,float snfs,float safs,
                     float titfs,float stitfs)
  {
    PAGEXSIZE=psx;
    PAGEYSIZE=psy;
    XMARGIN=xm;
    YMARGIN=ym;

    STAFFLINEWIDTH=stlw;
    LINEXADJUST=lxa;
    STAFFXSIZE=PAGEXSIZE-2*XMARGIN;

    STAFFYSCALE=stysc;
    STAFFYPOSSCALE=STAFFYSCALE/2;
    STAFFYSPACE=stysp;
    STAFFYSIZE=STAFFYSCALE*4;

    STEMWIDTH=sw;

    MusicFONTSIZE=mfs;
    PlainFONTSIZE=pfs;
    TextFONTSIZE=tfs;
    StaffNameFONTSIZE=snfs;
    ScoreAnnotationFONTSIZE=safs;
    TitleFONTSIZE=titfs;
    SubtitleFONTSIZE=stitfs;

    XYSCALE=(MusicFONTSIZE-1)/2000;
    YMUSICSTART=YMARGIN+STAFFYSCALE*8;
    DRAWINGSPACEY=PAGEYSIZE-YMUSICSTART-YMARGIN;
  }
}
