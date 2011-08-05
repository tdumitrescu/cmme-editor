/*----------------------------------------------------------------------*/
/*

        Module          : RenderedClefSet.java

        Package         : Gfx

        Classes Included: RenderedClefSet

        Purpose         : Handle sets of rendered clef events

        Programmer      : Ted Dumitrescu

        Date Started    : 5/2/07

        Updates:

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.util.*;

import DataStruct.*;

import com.lowagie.text.pdf.PdfContentByte;

/*------------------------------------------------------------------------
Class:   RenderedCleftSet
Extends: java.util.LinkedList
Purpose: Handles one set of rendered clef events
------------------------------------------------------------------------*/

public class RenderedClefSet extends LinkedList<RenderedEvent>
{
/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: RenderedClefSet(RenderedClefSet lastRCS,RenderedEvent ce,boolean modClefs,Clef smc)
Purpose:     Initialize clef set
Parameters:
  Input:  RenderedClefSet lastRCS - clef set preceding the new clef event
          RenderedEvent           - new clef event in list
          boolean modClefs        - use modern clef sets?
          Clef smc                - suggested modern clef
  Output: -
------------------------------------------------------------------------*/

  public RenderedClefSet(RenderedClefSet lastRCS,RenderedEvent ce,boolean modClefs,Clef smc)
  {
    /* create clef set, combining with last valid clef set if necessary */
    if (lastRCS==null || ce.getEvent().hasPrincipalClef())
      add(ce); /* new clef set */
    else
      {
        /* combine with last set */
        addAll(lastRCS);

        ClefSet lastCS=lastRCS.getLastClefSet(modClefs),
                thisCS=ce.getEvent().getClefSet(modClefs);
        if (lastCS==thisCS || thisCS.contradicts(lastCS,modClefs,smc))
          add(ce);
      }
  }

/*------------------------------------------------------------------------
Method:  double draw(boolean princOnly,
                     java.awt.Graphics2D g,MusicFont mf,
                     double xl,double yl,double VIEWSCALE)
Purpose: Draws this clef set into a given graphical context
Parameters:
  Input:  boolean princOnly - whether to draw only principal clefs
          Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          double xl,yl      - location in context to draw event
          double VIEWSCALE  - scaling factor
  Output: -
  Return: amount of x-space used
------------------------------------------------------------------------*/

  public double draw(boolean princOnly,
                     java.awt.Graphics2D g,MusicFont mf,
                     double xl,double yl,double VIEWSCALE)
  {
    double origxl=xl;

    for (RenderedEvent re : this)
      if ((!princOnly) || re.getEvent().hasPrincipalClef())
        xl+=re.drawClefs(g,mf,null,xl,yl,VIEWSCALE);

    return xl-origxl;
  }

/*------------------------------------------------------------------------
Method:  float drawClefs(PDFCreator outp,PdfContentByte cb,float xl,float yl)
Purpose: Draws this clef set into PDF
Parameters:
  Input:  PDFCreator outp   - PDF-writing object
          PdfContentByte cb - PDF graphical context
          float xl,yl       - location in context to draw event
  Output: -
  Return: amount of x-space used
------------------------------------------------------------------------*/

  public float draw(boolean princOnly,
                    PDFCreator outp,PdfContentByte cb,float xl,float yl)
  {
    float origxl=xl;

    for (RenderedEvent re : this)
      if ((!princOnly) || re.getEvent().hasPrincipalClef())
        xl+=re.drawClefs(outp,cb,xl,yl);

    return xl-origxl;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public RenderedEvent getPrincipalClefEvent()
  {
    return get(0);
  }

  public RenderedEvent getLastClefEvent()
  {
    return getLast();
  }

  public ClefSet getLastClefSet(boolean modClefs)
  {
    return getLastClefEvent().getEvent().getClefSet(modClefs);
  }

  public double getXSize()
  {
    double xsize=0;
    for (RenderedEvent re : this)
      xsize+=re.getClefImgXSize();
    return xsize;
  }

/*------------------------------------------------------------------------
Methods: void set*()
Purpose: Routines to set attribute variables
Parameters:
  Input:  new attributes
  Output: -
  Return: -
------------------------------------------------------------------------*/

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
    System.out.println(this);
  }

  public String toString()
  {
    String strVal="RenderedClefSet: [ ";
    for (RenderedEvent re : this)
      strVal+=re.getEvent()+" ";
    strVal+="]";
    return strVal;
  }
}

