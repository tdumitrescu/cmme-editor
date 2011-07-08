/*----------------------------------------------------------------------*/
/*

        Module          : OptionSet

        Package         : Gfx

        Classes Included: OptionSet

        Purpose         : Stores and handles options for one MusicWin

        Programmer      : Ted Dumitrescu

        Date Started    : 4/21/99

Updates:
4/26/99: converted GUI to Swing
4/18/05: converted OptionsWin to OptionSet (to represent option data without
         requiring link to GUI)

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import DataStruct.VariantReading;

/*------------------------------------------------------------------------
Class:   OptionSet
Extends: -
Purpose: Information about music display options
------------------------------------------------------------------------*/

public class OptionSet
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static final int OPT_BARLINE_NONE=  0,
                          OPT_BARLINE_MENSS= 1,
                          OPT_BARLINE_TICK=  2,
                          OPT_BARLINE_MODERN=3,

                          OPT_NOTESHAPE_ORIG=0,
                          OPT_NOTESHAPE_MOD_1_1=1,
                          OPT_NOTESHAPE_MOD_2_1=2,
                          OPT_NOTESHAPE_MOD_4_1=3,
                          OPT_NOTESHAPE_MOD_8_1=4,

                          OPT_MODACC_NONE=0,
                          OPT_MODACC_ABOVESTAFF=1,

                          OPT_VAR_ALL=0,
                          OPT_VAR_SUBSTANTIVE=1,
                          OPT_VAR_NONE=2,
                          OPT_VAR_CUSTOM=3;

  static final String[] BarlineStrings={ "None",
                                         "Mensurstrich",
                                         "Tick",
                                         "Modern" },
                        NoteShapeStrings={ "Original",
                                           "Modern 1:1" };/*,
                                           "Modern 2:1",
                                           "Modern 4:1",
                                           "Modern 8:1" };*/

  public static final int DEFAULT_STAFFSPACING=12,
                          SPACES_PER_TEXTLINE=2,
                          TEXTVERSION_COLORS=3,
                          MIN_TEXTS_FOR_RESPACING=4;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance variables */

  /* option variables */
  int      barline_type,
           noteShapeType,
           modacc_type;
  boolean  displayOrigText,
           displayModText,
           usemodernclefs,
           useModernAccidentalSystem,
           displayallnewlineclefs,
           displayorigligatures,
           displayligbrackets,
           viewEdCommentary,
           markdissonances,
           markdirectedprogressions,
           displayedittags,
           unscoredDisplay,
           ligatureList;
  int      markVariants;
  long     customVariantFlags;

  MusicWin musicWin;

  /* music display parameters */
  int    STAFFSCALE=10,                     /* # of pixels per staff line+space */
         STAFFSPACING=DEFAULT_STAFFSPACING, /* number of staff spaces for each
                                               voice's vertical space */
         BREVESCALE=100;                    /* default # of horizontal pixels for
                                               one breve of time */
  double VIEWSCALE=1;

/*----------------------------------------------------------------------*/
/* Class methods */

  /* create OptionSet for fully modern notation */
  public static OptionSet makeDEFAULT_FULL_MODERN(MusicWin mwin)
  {
    OptionSet o=new OptionSet(mwin);
    o.set_barline_type(OPT_BARLINE_MODERN);
    o.set_modacc_type(OPT_MODACC_ABOVESTAFF);
    o.set_noteShapeType(OPT_NOTESHAPE_MOD_1_1);
    o.set_displayOrigText(false);
    o.set_displayModText(true);
    o.set_usemodernclefs(true);
    o.setUseModernAccidentalSystem(true);

    return o;
  }

  /* create OptionSet for original notation */
  public static OptionSet makeDEFAULT_ORIGINAL(MusicWin mwin)
  {
    OptionSet o=new OptionSet(mwin);
    o.set_barline_type(OPT_BARLINE_TICK);
    o.set_modacc_type(OPT_MODACC_ABOVESTAFF);
    o.set_noteShapeType(OPT_NOTESHAPE_ORIG);
    o.set_displayOrigText(true);
    o.set_displayModText(false);
    o.set_usemodernclefs(false);
    o.setUseModernAccidentalSystem(false);

    return o;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: OptionSet(MusicWin mwin)
Purpose:     Initialize options
Parameters:
  Input:  MusicWin mwin - music window for these options
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public OptionSet(MusicWin mwin)
  {
    musicWin=mwin;

    /* initialize options for Viewer */
    barline_type=OPT_BARLINE_MENSS;
    modacc_type=OPT_MODACC_ABOVESTAFF;
    noteShapeType=OPT_NOTESHAPE_MOD_1_1;
    displayOrigText=false;
    displayModText=true;
    usemodernclefs=true;
    useModernAccidentalSystem=true;
    displayallnewlineclefs=false;
    displayorigligatures=false;
    displayligbrackets=true;
    viewEdCommentary=true;
    markdissonances=false;
    markdirectedprogressions=false;
    displayedittags=false;
    unscoredDisplay=false;
    ligatureList=false;

    markVariants=OPT_VAR_NONE;
    customVariantFlags=VariantReading.VAR_NONE;
  }

/*------------------------------------------------------------------------
Methods: boolean markVariant(long varFlags)
Purpose: Check whether a variant of a certain type should be marked with
         the current option settings
Parameters:
  Input:  long varFlags - flags marking which types of variant are present
  Output: -
  Return: true if variant should be marked
------------------------------------------------------------------------*/

  public boolean markVariant(long varFlags)
  {
    switch (markVariants)
      {
        case OPT_VAR_NONE:
          return false;
        case OPT_VAR_ALL:
          return true;
        case OPT_VAR_SUBSTANTIVE:
          return varFlags!=VariantReading.VAR_NONE;
        case OPT_VAR_CUSTOM:
          return (customVariantFlags&varFlags)>0;
      }

    return false;
  }

/*------------------------------------------------------------------------
Methods: get*()
Purpose: Routines to return parameters and options
Parameters:
  Input:  -
  Output: -
  Return: parameter/option variables
------------------------------------------------------------------------*/

  public int getSTAFFSCALE()
  {
    return STAFFSCALE;
  }

  public int getSTAFFSPACING()
  {
    if (musicWin!=null)
      {
        /* calculate spacing based on text to display */
        STAFFSPACING=DEFAULT_STAFFSPACING;
        int numVersions=musicWin.musicData.getVariantVersions().size();
        if (markVariant(VariantReading.VAR_ORIGTEXT) && !get_displayedittags() &&
            numVersions>=MIN_TEXTS_FOR_RESPACING)
          STAFFSPACING+=(numVersions-MIN_TEXTS_FOR_RESPACING)*SPACES_PER_TEXTLINE;
      }

    return STAFFSPACING;
  }

  public int getBREVESCALE()
  {
    return BREVESCALE;
  }

  public double getVIEWSCALE()
  {
    return VIEWSCALE;
  }

  public int get_barline_type()
  {
    return barline_type;
  }

  public int get_noteShapeType()
  {
    return noteShapeType;
  }

  public boolean useModernNoteShapes()
  {
    return noteShapeType!=OPT_NOTESHAPE_ORIG;
  }

  public int get_modacc_type()
  {
    return modacc_type;
  }

  public boolean get_displayOrigText()
  {
    return displayOrigText;
  }

  public boolean get_displayModText()
  {
    return displayModText;
  }

  public boolean get_usemodernclefs()
  {
    return usemodernclefs;
  }

  public boolean getUseModernAccidentalSystem()
  {
    return useModernAccidentalSystem;
  }

  public boolean get_displayorigligatures()
  {
    return displayorigligatures;
  }

  public boolean get_displayligbrackets()
  {
    return displayligbrackets;
  }

  public boolean getViewEdCommentary()
  {
    return viewEdCommentary;
  }

  public boolean get_displayallnewlineclefs()
  {
    return displayallnewlineclefs;
  }

  public boolean get_markdissonances()
  {
    return markdissonances;
  }

  public boolean get_markdirectedprogressions()
  {
    return markdirectedprogressions;
  }

  public boolean get_displayedittags()
  {
    return displayedittags;
  }

  public boolean get_unscoredDisplay()
  {
    return unscoredDisplay;
  }

  public boolean isLigatureList()
  {
    return ligatureList;
  }

  public int getMarkVariants()
  {
    return markVariants;
  }

  public boolean markCustomVariant(long varFlags)
  {
    return (customVariantFlags&varFlags)>0;
  }

/*------------------------------------------------------------------------
Methods: void set*
Purpose: Routines to set parameters and options
Parameters:
  Input:  new values for parameters and options
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setVIEWSCALE(double newval)
  {
    VIEWSCALE=newval;
  }

  public void set_barline_type(int newval)
  {
    barline_type=newval;
  }

  public void addCustomVariantFlags(long newFlags)
  {
    customVariantFlags|=newFlags;
  }

  public void removeCustomVariantFlags(long newFlags)
  {
    customVariantFlags&=~newFlags;
  }

  public void setCustomVariantFlags(long newFlags)
  {
    customVariantFlags=newFlags;
  }

  public void setLigatureList(boolean newval)
  {
    ligatureList=newval;
  }

  public void set_modacc_type(int newval)
  {
    modacc_type=newval;
  }

  public void set_noteShapeType(int newval)
  {
    noteShapeType=newval;
  }

  public void set_displayOrigText(boolean newval)
  {
    displayOrigText=newval;
  }

  public void set_displayModText(boolean newval)
  {
    displayModText=newval;
  }

  public void set_usemodernclefs(boolean newval)
  {
    usemodernclefs=newval;
  }

  public void setUseModernAccidentalSystem(boolean newval)
  {
    useModernAccidentalSystem=newval;
  }

  public void set_displayorigligatures(boolean newval)
  {
    displayorigligatures=newval;
  }

  public void set_displayligbrackets(boolean newval)
  {
    displayligbrackets=newval;
  }

  public void setViewEdCommentary(boolean newval)
  {
    viewEdCommentary=newval;
  }

  public void set_displayallnewlineclefs(boolean newval)
  {
    displayallnewlineclefs=newval;
  }

  public void set_markdissonances(boolean newval)
  {
    markdissonances=newval;
  }

  public void set_markdirectedprogressions(boolean newval)
  {
    markdirectedprogressions=newval;
  }

  public void set_displayedittags(boolean newval)
  {
    displayedittags=newval;
  }

  public void set_unscoredDisplay(boolean newval)
  {
    unscoredDisplay=newval;
  }

  public void setMarkVariants(int newval)
  {
    markVariants=newval;
  }
}
