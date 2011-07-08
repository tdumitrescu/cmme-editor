/*----------------------------------------------------------------------*/
/*

        Module          : MusicFont.java

        Package         : Gfx

        Classes Included: MusicFont

        Purpose         : deal with loading/imaging music graphics from
                          a Truetype font

        Programmer      : Ted Dumitrescu

        Date Started    : 3/16/99

Updates:
4/19/99: added ByteArrayInput, to load font from a remote URL
4/28/99: moved ByteArrayInput to package Main
3/29/04: removed custom outline-drawing code from paintGlyph, replaced with
         calls to Graphics2D functions
3/17/05: removed references to Client classes
9/14/05: added multi-color support
4/3/06:  created static default font, to avoid creating new images for every
         score window
11/4/06: replaced entire freetype-based font-loading/rendering system with
         calls to standard Java libraries (now that dynamic font-loading is
         available in JDK)

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;

import DataStruct.Coloration;
import DataStruct.NoteEvent;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   MusicFont
Extends: -
Purpose: font handler
------------------------------------------------------------------------*/

public class MusicFont
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final String DisplayFontFileName="cmme.ttf",
                             ModernFontFileName="cmme-modern.ttf",
                             PrintFontFileName="cmme-printer.ttf";

  public static final int PIC_OFFSET=0,
                          PIC_NULL=     33,
                          PIC_CLEFSTART=34,
                          PIC_NOTESTART=PIC_CLEFSTART+30,
                          PIC_MODNOTESTART=PIC_NOTESTART+30,
                          PIC_MENSSTART=PIC_NOTESTART+40,
                          PIC_RESTSTART=0xAE,
                          PIC_MODRESTSTART=PIC_RESTSTART+8,
                          PIC_MISCSTART=0xCC;
  public static final int PIC_NOTE_OFFSET_STEMUP=  10,
                          PIC_NOTE_OFFSET_STEMDOWN=20,
                          PIC_MODNOTE_OFFSET_STEMUP=0,
                          PIC_MODNOTE_OFFSET_STEMDOWN=2,
                          PIC_MODNOTE_OFFSET_FLAGUP=7,
                          PIC_MODNOTE_OFFSET_FLAGDOWN=8;
  public static final int PIC_MENS_O=          0,
                          PIC_MENS_C=          1,
                          PIC_MENS_CREV=       2,
                          PIC_MENS_C90CW=      3,
                          PIC_MENS_C90CCW=     4,
                          PIC_MENS_STROKE=     5,
                          PIC_MENS_DOT=        8,
                          PIC_MENS_NONE=       9,
                          PIC_MENS_OFFSETSMALL=10;
  public static final int PIC_MISC_BLANK=           0,
                          PIC_MISC_DOT=             1,
                          PIC_MISC_STEM=            2,
                          PIC_MISC_LEDGER=          3,
                          PIC_MISC_LINEEND=         4,
                          PIC_MISC_CUSTOS=          5,
                          PIC_MISC_FLAGUP=          6,
                          PIC_MISC_FLAGDOWN=        7,
                          PIC_MISC_CORONAUP=        8,
                          PIC_MISC_CORONADOWN=      9,
                          PIC_MISC_SIGNUMUP=        10,
                          PIC_MISC_SIGNUMDOWN=      11,
                          PIC_MISC_ANGBRACKETLEFT=  12,
                          PIC_MISC_ANGBRACKETRIGHT= 13,
                          PIC_MISC_PARENSLEFT=      14,
                          PIC_MISC_PARENSRIGHT=     15,
                          PIC_MISC_PARENSLEFTSMALL= 16,
                          PIC_MISC_PARENSRIGHTSMALL=17,
                          PIC_MISC_BRACKETLEFT=     18,
                          PIC_MISC_BRACKETRIGHT=    19;

  /* connections for glyph combinations (in units of 1/1000 point) */
  public static final double CONNECTION_SB_UPSTEMX=257f,
                            CONNECTION_SB_UPSTEMY=280f,
                            CONNECTION_SB_DOWNSTEMX=257f,
                            CONNECTION_SB_DOWNSTEMY=-1757f,
                            CONNECTION_L_LEFTSTEMX=0f,
                            CONNECTION_L_STEMX=446f,
                            CONNECTION_L_UPSTEMY=282.6f,
                            CONNECTION_L_DOWNSTEMY=-1730f,
                            CONNECTION_MX_STEMX=991f,
                            CONNECTION_STEM_UPFLAGY=1700f,
                            CONNECTION_STEM_DOWNFLAGY=-1650f,
                            CONNECTION_STEM_UPMODFLAGY=1400f,
                            CONNECTION_STEM_DOWNMODFLAGY=-1350f,
                            CONNECTION_MODFLAGX=0f,
                            CONNECTION_FLAGINTERVAL=700f,
                            CONNECTION_CORONAX=-180f,
                            CONNECTION_DOTX=240f,
                            CONNECTION_MODACCX=180f,
                            CONNECTION_MODACC_DBLFLAT=-270f,
                            CONNECTION_MODACC_DBLSHARP=-450f,
                            CONNECTION_MODACC_PARENS=270f,
                            CONNECTION_MODACCSMALLX=180f,
                            CONNECTION_MODACC_SMALLDBLFLAT=-360f,
                            CONNECTION_MODACC_SMALLDBLSHARP=-450f,
                            CONNECTION_MODACC_SMALLNATURAL=90f,
                            CONNECTION_MODACC_SMALLPARENS=45f,
                            CONNECTION_ANGBRACKETLEFT=-240f,
                            CONNECTION_ANGBRACKETRIGHT=450f,
                            CONNECTION_LIG_RECTA=446f,
                            CONNECTION_LIG_UPSTEMY=200f,
                            CONNECTION_LIG_DOWNSTEMY=-1650f,
                            CONNECTION_BARLINEX=200f,
                            CONNECTION_ANNOTATION_MENSSYMBOL=235f,
                            CONNECTION_MENSSIGNX=800f,
                            CONNECTION_MENSNUMBERX=90f,
                            CONNECTION_MENSNUMBERY=-90f;

  public static final double CONNECTION_SCREEN_ANGBRACKETLEFT=-6,
                            CONNECTION_SCREEN_ANGBRACKETRIGHT=13,
                            CONNECTION_SCREEN_BARLINEX=5,
                            CONNECTION_SCREEN_CORONAX=-2,
                            CONNECTION_SCREEN_DOTX=6,
                            CONNECTION_SCREEN_MODFLAGX=-2,
                            CONNECTION_SCREEN_FLAGINTERVAL=8,
                            CONNECTION_SCREEN_LIG_DOWNSTEMY=-36,
                            CONNECTION_SCREEN_LIG_RECTA=11,
                            CONNECTION_SCREEN_LIG_UPSTEMY=6,
                            CONNECTION_SCREEN_L_LEFTSTEMX=0,
                            CONNECTION_SCREEN_L_STEMX=11,
                            CONNECTION_SCREEN_L_UPSTEMY=5,
                            CONNECTION_SCREEN_L_DOWNSTEMY=-35,
                            CONNECTION_SCREEN_MENSNUMBERX=10,
                            CONNECTION_SCREEN_MENSNUMBERY=-2,
                            CONNECTION_SCREEN_MENSSIGNX=15,
                            CONNECTION_SCREEN_MODACCSMALLX=3,
                            CONNECTION_SCREEN_MODACCX=2,
                            CONNECTION_SCREEN_MODACC_DBLFLAT=-2,
                            CONNECTION_SCREEN_MODACC_DBLSHARP=-5,
                            CONNECTION_SCREEN_MODACC_PARENS=4,
                            CONNECTION_SCREEN_MODACC_SMALLDBLFLAT=-2,
                            CONNECTION_SCREEN_MODACC_SMALLDBLSHARP=-4,
                            CONNECTION_SCREEN_MODACC_SMALLNATURAL=3,
                            CONNECTION_SCREEN_MODACC_SMALLPARENSLEFT=5,
                            CONNECTION_SCREEN_MODACC_SMALLPARENSRIGHT=4f,
                            CONNECTION_SCREEN_MX_STEMX=21,
                            CONNECTION_SCREEN_SB_UPSTEMX=6,
                            CONNECTION_SCREEN_SB_UPSTEMY=6,
                            CONNECTION_SCREEN_SB_DOWNSTEMX=6,
                            CONNECTION_SCREEN_SB_DOWNSTEMY=-36,
                            CONNECTION_SCREEN_STEM_UPFLAGY=30,
                            CONNECTION_SCREEN_STEM_DOWNFLAGY=1;

  public static final float DEFAULT_MUSIC_FONTSIZE=42,
                            DEFAULT_TEXT_FONTSIZE=14,
                            DEFAULT_TEXT_SMALLFONTSIZE=12,
                            DEFAULT_TEXT_LARGEFONTSIZE=20;
  public static final int   PICXOFFSET=4,
                            PICYCENTER=41;
  public static final double SCREEN_TO_GLYPH_FACTOR=40f;

  /* static generic G2D for creating FontMetrics */
  static BufferedImage genericBI=new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB);
  static Graphics2D    genericG=genericBI.createGraphics();

  public static Font        baseMusicFont=null,
                            defaultMusicFont=null,
                            defaultTextFont=null,
                            defaultTextItalFont=null;
  public static FontMetrics defaultMusicFontMetrics=null;

/*----------------------------------------------------------------------*/
/* Instance variables */

  public Font          displayOrigFont,
                       displayModFont,
                       displayTextFont,
                       displayTextItalFont,
                       displayTextSmallFont,
                       displayTextItalSmallFont,
                       displayTextLargeFont,
                       displayTextItalLargeFont;
  public FontMetrics   displayOrigFontMetrics,
                       displayTextFontMetrics,
                       displayTextSmallFontMetrics,
                       displayTextLargeFontMetrics;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  void loadmusicface(String database)
Purpose: Load music face from which all instances get their glyphs
Parameters:
  Input:  String database - base data directory location
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void loadmusicface(String database) throws Exception
  {
    baseMusicFont=Font.createFont(
      Font.TRUETYPE_FONT,new URL(database+"fonts/"+DisplayFontFileName).openStream());
    defaultMusicFont=baseMusicFont.deriveFont(DEFAULT_MUSIC_FONTSIZE);
    defaultTextFont=new Font(null,Font.PLAIN,(int)DEFAULT_TEXT_FONTSIZE);
    defaultTextItalFont=new Font(null,Font.ITALIC,(int)DEFAULT_TEXT_FONTSIZE);
    genericG.setFont(defaultMusicFont);
    defaultMusicFontMetrics=genericG.getFontMetrics();
  }

/*------------------------------------------------------------------------
Method:  void destroyMusicFace()
Purpose: Release resources from main static music face
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void destroyMusicFace()
  {
    baseMusicFont=null;
  }

/*------------------------------------------------------------------------
Method:  int getDefaultGlyphWidth(int glyphNum)
Purpose: Calculate default x-width required by one music glyph
Parameters:
  Input:  int glyphNum - number of glyph to check
  Output: -
  Return: default x-space required by glyph
------------------------------------------------------------------------*/

  public static int getDefaultGlyphWidth(int glyphNum)
  {
    return defaultMusicFontMetrics.charWidth((char)(PIC_OFFSET+glyphNum));
  }

  public static double getDefaultPrintGlyphWidth(int glyphNum)
  {
    return getDefaultGlyphWidth(glyphNum)*SCREEN_TO_GLYPH_FACTOR;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MusicFont([double VIEWSCALE])
Purpose:     Load font and initialize graphics
Parameters:
  Input:  double VIEWSCALE - size multiplier
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MusicFont()
  {
    this((double)1);
  }

  public MusicFont(double VIEWSCALE)
  {
    newScale(VIEWSCALE);
  }

/*------------------------------------------------------------------------
Method:  void newScale(double VIEWSCALE)
Purpose: Change font size
Parameters:
  Input:  double VIEWSCALE - new size multiplier
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void newScale(double VIEWSCALE)
  {
    displayOrigFont=baseMusicFont.deriveFont((float)(DEFAULT_MUSIC_FONTSIZE*VIEWSCALE));
    displayTextFont=defaultTextFont.deriveFont((float)(DEFAULT_TEXT_FONTSIZE*VIEWSCALE));
    displayTextItalFont=defaultTextFont.deriveFont(Font.ITALIC,(float)(DEFAULT_TEXT_FONTSIZE*VIEWSCALE));
    displayTextSmallFont=defaultTextFont.deriveFont((float)(DEFAULT_TEXT_SMALLFONTSIZE*VIEWSCALE));
    displayTextItalSmallFont=defaultTextFont.deriveFont(Font.ITALIC,(float)(DEFAULT_TEXT_SMALLFONTSIZE*VIEWSCALE));
    displayTextLargeFont=defaultTextFont.deriveFont((float)(DEFAULT_TEXT_LARGEFONTSIZE*VIEWSCALE));
    displayTextItalLargeFont=defaultTextFont.deriveFont(Font.ITALIC,(float)(DEFAULT_TEXT_LARGEFONTSIZE*VIEWSCALE));

    genericG.setFont(displayOrigFont);
    displayOrigFontMetrics=genericG.getFontMetrics();
    genericG.setFont(displayTextFont);
    displayTextFontMetrics=genericG.getFontMetrics();
    genericG.setFont(displayTextSmallFont);
    displayTextSmallFontMetrics=genericG.getFontMetrics();
    genericG.setFont(displayTextLargeFont);
    displayTextLargeFontMetrics=genericG.getFontMetrics();
  }

/*------------------------------------------------------------------------
Method:  void drawGlyph(Graphics2D g,int glyphNum,double x,double y,Color c)
Purpose: Draw one glyph into graphical context
Parameters:
  Input:  Graphics2D g - graphical context for drawing
          int glyphNum - number of glyph
          double x,y   - location in context to draw
          Color c      - color
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void drawGlyph(Graphics2D g,int glyphNum,double x,double y,Color c)
  {
    g.setFont(displayOrigFont);
    g.setColor(c);
    g.drawString(String.valueOf((char)(MusicFont.PIC_OFFSET+glyphNum)),(float)x,(float)y);
  }

/*------------------------------------------------------------------------
Method:  int getGlyphWidth(int glyphNum)
Purpose: Calculate x-width required by one music glyph
Parameters:
  Input:  int glyphNum - number of glyph to check
  Output: -
  Return: x-space required by glyph
------------------------------------------------------------------------*/

  public int getGlyphWidth(int glyphNum)
  {
    return displayOrigFontMetrics.charWidth((char)(PIC_OFFSET+glyphNum));
  }

/*------------------------------------------------------------------------
Method:  Font chooseTextFont(int fsize,fstyle)
Purpose: Choose (scaled) font based on required size
Parameters:
  Input:  int fsize,fstyle - requested font size/style (unscaled)
  Output: -
  Return: closest available text font
------------------------------------------------------------------------*/

  public Font chooseTextFont(int fsize,int fstyle)
  {
    if (fsize<=DEFAULT_TEXT_SMALLFONTSIZE)
      return fstyle==Font.ITALIC ? displayTextItalSmallFont : displayTextSmallFont;
    if (fsize<=DEFAULT_TEXT_FONTSIZE)
      return fstyle==Font.ITALIC ? displayTextItalFont : displayTextFont;
    return fstyle==Font.ITALIC ? displayTextItalLargeFont : displayTextLargeFont;
  }
}

