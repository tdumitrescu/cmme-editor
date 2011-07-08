/*----------------------------------------------------------------------*/
/*

        Module          : EventStringImg

        Package         : Gfx

        Classes Included: EventStringImg

        Purpose         : Low-level drawing information for writing one string
                          within an event

        Programmer      : Ted Dumitrescu

        Date Started    : 9/9/05

Updates:
11/7/06: removed code for pre-rendering into buffered image (to facilitate
         new canvas-scaling model)

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.swing.JLabel;

import DataStruct.Coloration;

/*------------------------------------------------------------------------
Class:   EventStringImg
Extends: EventImg
Purpose: Information about one image for a rendered event
------------------------------------------------------------------------*/

public class EventStringImg extends EventImg
{
/*----------------------------------------------------------------------*/
/* Class variables */

  public static BufferedImage genericBI=new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB);
  public static Graphics2D    genericG=genericBI.createGraphics();
  public static JLabel        BSObserver=new JLabel();

/*----------------------------------------------------------------------*/
/* Instance variables */

  public String                   imgtext,
                                  imgtextWithoutSymbols;
  public ArrayList<EventGlyphImg> specialImages;
  public int                      textXSize;

  int   fontSize,fontStyle;
  Font  imgfont;
  Color imgcolor;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: EventStringImg(String s,int syp,double xo,double yo,double uxo,double uyo,int c,int size,int style)
Purpose:     Initialize image information
Parameters:
  Input:  String s       - text for image
          int syp        - staff y position
          double xo,yo   - XY offset for display
          double uxo,uyo - unscaled XY offset
          int c          - color
          int size,style - font size, style (java.awt.Font)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public EventStringImg(String s,int syp,double xo,double yo,double uxo,double uyo,int c,int size)
  {
    this(s,syp,xo,yo,uxo,uyo,c,size,Font.PLAIN);
  }

  public EventStringImg(String s,int syp,double xo,double yo,double uxo,double uyo,int c,int size,int style)
  {
    imgtext=s;
    imgfont=style==Font.ITALIC ? MusicFont.defaultTextItalFont : MusicFont.defaultTextFont;
    if (imgfont.getSize()!=size)
      imgfont=imgfont.deriveFont(style,(float)size);
    fontSize=size;
    fontStyle=style;
    color=c;
    imgcolor=Coloration.AWTColors[color];
    staffypos=syp;
    xsize=0;
    xoff=xo;
    yoff=yo;
    UNSCALEDxoff=uxo;
    UNSCALEDyoff=uyo;

    initimage();
  }

  /* create image displaying string */
  void initimage()
  {
    /* calculate size of image */
    genericG.setFont(imgfont);
    FontMetrics metrics=genericG.getFontMetrics();
    int textyloc=metrics.getHeight(),
        textdescent=metrics.getDescent(),
        picysize=textyloc+textdescent;

    /* initialize text/image list */
    textXSize=metrics.stringWidth(imgtext);
    specialImages=new ArrayList<EventGlyphImg>();

    /* draw string (translating escape sequences) */
    double  curx=0;
    int    escapeglyphnum;
    char   c;
    char[] textarray=imgtext.toCharArray();
    StringBuffer textarrayWithoutSymbols=new StringBuffer();
    for (int i=0; i<textarray.length; i++)
      {
        if (textarray[i]=='\\')
          {
            /* escape sequence */
            if (i+1<textarray.length && textarray[i+1]=='m')
              {
                /* mensuration sign */
                i+=2;
                if (i<textarray.length)
                  {
                    if (textarray[i]=='O')
                      escapeglyphnum=MusicFont.PIC_MENS_O;
                    else if (textarray[i]=='C')
                      escapeglyphnum=MusicFont.PIC_MENS_C;
                    else
                      escapeglyphnum=MusicFont.PIC_MENS_NONE;
                    if (i+1<textarray.length && textarray[i+1]=='r')
                      {
                        i++;
                        escapeglyphnum=MusicFont.PIC_MENS_CREV;
                      }

                    double mensxoff=curx-MusicFont.PICXOFFSET,
                          mensyoff=MusicFont.PICYCENTER-textyloc+textdescent,
                          USmensxoff=(double)curx*(MusicFont.SCREEN_TO_GLYPH_FACTOR+8),
                          USmensyoff=MusicFont.CONNECTION_ANNOTATION_MENSSYMBOL;
                    EventGlyphImg ei=new EventGlyphImg(
                      MusicFont.PIC_MENSSTART+escapeglyphnum+MusicFont.PIC_MENS_OFFSETSMALL,0,
                      mensxoff,mensyoff,USmensxoff,USmensyoff,color);
                    specialImages.add(ei);
                    textarrayWithoutSymbols.append("   ");

                    /* attributes (stroke/dot) */
                    if (i+1<textarray.length && textarray[i+1]=='|')
                      {
                        i++;
                        specialImages.add(new EventGlyphImg(
                          MusicFont.PIC_MENSSTART+MusicFont.PIC_MENS_STROKE+MusicFont.PIC_MENS_OFFSETSMALL,0,
                          mensxoff,mensyoff,USmensxoff,USmensyoff,color));
                      }
                    if (i+1<textarray.length && textarray[i+1]=='.')
                      {
                        i++;
                        specialImages.add(new EventGlyphImg(
                          MusicFont.PIC_MENSSTART+MusicFont.PIC_MENS_DOT+MusicFont.PIC_MENS_OFFSETSMALL,0,
                          mensxoff,mensyoff,USmensxoff,USmensyoff,color));
                      }

                    curx+=ei.xsize;
                  }
              }
          }
        else
          {
            /* regular character */
            c=textarray[i];
            textarrayWithoutSymbols.append(c);
            curx+=metrics.charWidth(c);
          }
      }

    imgtextWithoutSymbols=textarrayWithoutSymbols.toString();
    xsize=(int)Math.round(curx);
  }

/*------------------------------------------------------------------------
Method:  void draw(java.awt.Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,int c,double VIEWSCALE)
Purpose: Draws image into given graphical context
Parameters:
  Input:  Graphics2D g      - graphical context for drawing
          MusicFont mf      - font for drawing symbols
          ImageObserver ImO - observer for drawImage
          double xl,yl       - location of event in graphical context
          int c             - color
          double VIEWSCALE   - scaling factor
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void draw(Graphics2D g,MusicFont mf,ImageObserver ImO,
                   double xl,double yl,int c,double VIEWSCALE)
  {
    if (this.color==Coloration.GRAY)
      c=this.color; /* cannot override gray */
    imgcolor=Coloration.AWTColors[c];

    g.setFont(mf.chooseTextFont(fontSize,fontStyle));
    g.setColor(imgcolor);
    g.drawString(imgtextWithoutSymbols,(float)(xl+xoff*VIEWSCALE),(float)(yl-yoff*VIEWSCALE));
    for (Iterator i=specialImages.iterator(); i.hasNext();)
      ((EventImg)(i.next())).draw(g,mf,ImO,xl+xoff*VIEWSCALE,yl-yoff*VIEWSCALE);
  }
}
