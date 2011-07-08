/*----------------------------------------------------------------------*/
/*

        Module          : MusicXMLGenerator.java

        Package         : Gfx

        Classes	Included: MusicXMLGenerator

        Purpose         : functions for outputting MusicXML data, based
                          on rendered score data

        Programmer      : Ted Dumitrescu

        Date Started    : 8/6/10

Updates:
6/3/2011: bug fix: add empty measures when voice is not in section

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.output.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   MusicXMLGenerator
Extends: -
Purpose: MusicXML output functions
------------------------------------------------------------------------*/

public class MusicXMLGenerator
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static final String MUSICXML_VERSION="2.0";

  static Namespace MusicXMLNS=Namespace.getNamespace("http://www.musicxml.org"),
                   xsins=Namespace.getNamespace("xsi","http://www.w3.org/2001/XMLSchema-instance");

/*----------------------------------------------------------------------*/
/* Instance variables */

  ScorePageRenderer renderedScore;
  PieceData         musicData;

/*----------------------------------------------------------------------*/
/* Class methods */

  /* create MusicXML ID based on CMME voice number */
  static String makePartID(int vnum)
  {
    return "P"+(vnum+1);
  }

  static int calcIntDuration(Proportion l,int divisions)
  {
    return (l.i1*divisions*2)/l.i2;
  }

  static final String[] MUSICXML_NOTETYPES=new String[]
    {
      "X","X","X","256th","128th","64th","32nd","16th",
      "eighth","quarter","half","whole","breve","long"
    };

  static String notetypeToMusicXMLNotetype(int nt,Proportion l)
  {
    return MUSICXML_NOTETYPES[nt];
  }

  static int CMMEtoMusicXMLOctave(Pitch p)
  {
    return p.octave+(p.noteletter>'B' ? 1 : 0);
  }

  static String CMMEtoMusicXMLAccidental(ModernAccidental acc)
  {
    return ModernAccidental.AccidentalNames[acc.accType].toLowerCase();
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MusicXMLGenerator(ScorePageRenderer renderedScore)
Purpose:     Initialize
Parameters:
  Input:  ScorePageRenderer renderedScore - rendered data, multi-page score layout
  Output: -
------------------------------------------------------------------------*/

  public MusicXMLGenerator(ScorePageRenderer renderedScore)
  {
    this.renderedScore=renderedScore;
    this.musicData=renderedScore.musicData;
  }

/*------------------------------------------------------------------------
Method:  void outputPieceData(OutputStream outs)
Purpose: Output MusicXML format file
Parameters:
  Input:  OutputStream outs - output destination
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void outputPieceData(OutputStream outs)
  {
    Document outputDoc;
    Element  rootEl;

    /* header/root */
    rootEl=new Element("score-partwise");
    rootEl.setAttribute("version",MUSICXML_VERSION);

    /* actual content */
    rootEl.addContent(createIdentificationTree());
    rootEl.addContent(createPartListTree());
    addPartMusicTrees(rootEl);

    /* output */
    outputDoc=new Document(rootEl);
    outputDoc.setDocType(new DocType("score-partwise",
                                     "-//Recordare//DTD MusicXML 2.0 Partwise//EN",
                                     "http://www.musicxml.org/dtds/partwise.dtd"));
    try
      {
        XMLOutputter xout=new XMLOutputter(Format.getRawFormat().setIndent("  "));
        xout.output(outputDoc,outs);
      }
    catch (Exception e)
      {
        System.err.println("Error writing XML doc: "+e);
      }
  }

/*------------------------------------------------------------------------
Method:  Element createIdentificationTree()
Purpose: Construct tree segment "identification" for basic metadata
Parameters:
  Input:  -
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  Element createIdentificationTree()
  {
    Element baseEl=new Element("identification");

    /* encoding */
    Element encodingEl=new Element("encoding");
    encodingEl.addContent(new Element("software").setText(MetaData.CMME_SOFTWARE_NAME));
    encodingEl.addContent(new Element("encoding-date").setText(
      new java.text.SimpleDateFormat("yyyy-MM-dd").format(
        new Date(System.currentTimeMillis()),new StringBuffer(""),
        new java.text.FieldPosition(java.text.DateFormat.DATE_FIELD)).toString()));
    encodingEl.addContent(new Element("supports").
      setAttribute("attribute","new-system").
      setAttribute("element","print").
      setAttribute("type","yes").
      setAttribute("value","yes"));
    encodingEl.addContent(new Element("supports").
      setAttribute("attribute","new-page").
      setAttribute("element","print").
      setAttribute("type","yes").
      setAttribute("value","yes"));

    baseEl.addContent(encodingEl);
    return baseEl;
  }

/*------------------------------------------------------------------------
Method:  Element createPartListTree()
Purpose: Construct tree segment "part-list" for voice information
Parameters:
  Input:  -
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  Element createPartListTree()
  {
    Element baseEl=new Element("part-list");
    baseEl.addContent(new Element("part-group").
      setAttribute("number","1").
      setAttribute("type","start"));

    /* voice definitions */
    int vnum=0;
    for (Voice v : this.musicData.getVoiceData())
      {
        Element voiceEl=new Element("score-part");
        voiceEl.setAttribute("id",makePartID(vnum));

        voiceEl.addContent(new Element("part-name").setText(v.getName()));
        voiceEl.addContent(new Element("part-abbreviation").setText(Character.toString(v.getAbbrevLetter())));

        baseEl.addContent(voiceEl);
        vnum++;
      }

    baseEl.addContent(new Element("part-group").
      setAttribute("number","1").
      setAttribute("type","stop"));
    return baseEl;
  }

/*------------------------------------------------------------------------
Method:  void addPartMusicTrees(Element rootEl)
Purpose: Construct tree segment for each voice and add to root
Parameters:
  Input:  -
  Output: Element rootEl - root element to add parts to
  Return: -
------------------------------------------------------------------------*/

  void addPartMusicTrees(Element rootEl)
  {
    for (int vi=0; vi<this.musicData.getVoiceData().length; vi++)
      rootEl.addContent(createPartMusicTree(vi));
  }

/*------------------------------------------------------------------------
Method:  Element createPartMusicTree(int vnum)
Purpose: Construct tree segment for music of one voice
Parameters:
  Input:  int vnum - voice number (in CMME data)
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  boolean            addDivisions;
  int                DIVISIONS;
  ModernKeySignature curKeySig;
  boolean            midWord;

  Element createPartMusicTree(int vnum)
  {
    Element baseEl=new Element("part");
    baseEl.setAttribute("id",makePartID(vnum));

    /* initialize GLOBAAAAALS */
    addDivisions=true;
    DIVISIONS=4;
    curKeySig=ModernKeySignature.DEFAULT_SIG;
    midWord=false;

    for (int pi=0; pi<renderedScore.pages.size(); pi++)
      addPage(vnum,pi,baseEl);

    return baseEl;
  }

/*------------------------------------------------------------------------
Method:  void addPage(int vnum,int pnum,Element partEl)
Purpose: Add nodes for music of one page of one part to doc
Parameters:
  Input:  int vnum,pnum  - voice/page number
  Output: Element partEl - root element to add music to
  Return: -
------------------------------------------------------------------------*/

  void addPage(int vnum,int pnum,Element partEl)
  {
    RenderedScorePage curPage=renderedScore.pages.get(pnum);
    int startSys=curPage.startSystem,
        endSys=startSys+curPage.numSystems-1;
    if (endSys>=renderedScore.systems.size())
      endSys=renderedScore.systems.size()-1;
    for (int curSys=startSys; curSys<=endSys; curSys++)
      addSystem(vnum,curSys,partEl);
  }

/*------------------------------------------------------------------------
Method:  void addSystem(int vnum,int snum,Element partEl)
Purpose: Add nodes for music of one system of one part to doc
Parameters:
  Input:  int vnum,snum  - voice/system number
  Output: Element partEl - root element to add music to
  Return: -
------------------------------------------------------------------------*/

  void addSystem(int vnum,int snum,Element partEl)
  {
    RenderedStaffSystem curSystem=renderedScore.systems.get(snum);
    int                 rendererNum=ScoreRenderer.calcRendererNum(renderedScore.scoreData,curSystem.startMeasure);

    for (int mi=curSystem.startMeasure; mi<=curSystem.endMeasure; mi++)
      {
        MeasureInfo m=renderedScore.scoreData[rendererNum].getMeasure(mi);
        Element measureEl=new Element("measure");
        measureEl.setAttribute("number",Integer.toString(mi+1));

        Element attributesEl=createDivisionsEl();
        if (attributesEl!=null)
          measureEl.addContent(attributesEl);

        if (renderedScore.scoreData[rendererNum].eventinfo[vnum]!=null)
          {
            int leftei=m.reventindex[vnum],
                rightei=renderedScore.getLastEventInMeasure(snum,rendererNum,vnum,mi);
            addEvents(measureEl,renderedScore.scoreData[rendererNum],vnum,leftei,rightei);
          }

        partEl.addContent(measureEl);
      }
  }

  void addEvents(Element measureEl,
                 ScoreRenderer renderer,int vnum,int leftei,int rightei)
  {
    for (int ei=leftei; ei<=rightei; ei++)
      addOneEvent(measureEl,renderer.eventinfo[vnum],renderer.getEvent(vnum,ei));
  }

  void addMultiEvent(Element measureEl,RenderList elist,RenderedEvent rme)
  {
    for (RenderedEvent re : rme.getEventList())
      addOneEvent(measureEl,elist,re);
  }

  void addOneEvent(Element measureEl,RenderList elist,RenderedEvent re)
  {
    Element eventEl=null;
    Event   e=re.getEvent();
    boolean attrib=false;
    switch (e.geteventtype())
      {
        case Event.EVENT_MULTIEVENT:
          addMultiEvent(measureEl,elist,re);
          break;
        case Event.EVENT_CLEF:
          if (e.hasPrincipalClef())
            {
              eventEl=new Element("clef");
              Clef c=re.getClef();
              eventEl.addContent(new Element("sign").setText(
                Character.toString(Clef.ClefLetters[c.cleftype])));
              eventEl.addContent(new Element("line").setText(
                Integer.toString(Clef.linespaceNumToLineNum(c.linespacenum))));
              if (c.cleftype==Clef.CLEF_MODERNG8)
                eventEl.addContent(new Element("clef-octave-change").setText("-1"));
            }
          else if (e.hasSignatureClef())
            eventEl=createKeyEl(re);
          attrib=true;
          break;
        case Event.EVENT_REST:
          eventEl=new Element("note").addContent(new Element("rest"));
          RestEvent reste=(RestEvent)e;
          addNoteInfoData(eventEl,elist,re,reste.getModNoteType(),reste.getLength());
          break;
        case Event.EVENT_NOTE:
          eventEl=new Element("note");
          NoteEvent ne=(NoteEvent)e;
          addPitchData(eventEl,ne.getPitch(),ne.getPitchOffset());
          addNoteInfoData(eventEl,elist,re,ne.getnotetype(),ne.getLength());
          if (ne.hasModernDot())
            eventEl.addContent(new Element("dot"));

          ModernAccidental acc=re.getAccidental();
          if (acc!=null)
            eventEl.addContent(new Element("accidental").setText(
              CMMEtoMusicXMLAccidental(acc)));

          if (ne.getModernText()!=null)
            addLyricData(eventEl,ne);
          break;
      }

    if (eventEl==null)
      return;

    if (!attrib)
      measureEl.addContent(eventEl);
    else
      addMeasureAttrib(measureEl,eventEl);
  }

  void addMeasureAttrib(Element measureEl,Element eventEl)
  {
    Element attributesEl=measureEl.getChild("attributes");
    if (attributesEl!=null)
      {
        if (eventEl.getName().equals("key"))
          {
            int clefIndex=attributesEl.indexOf(attributesEl.getChild("clef"));
            if (clefIndex!=-1)
              {
                /* key signature has to come before clef. ?! */
                attributesEl.addContent(clefIndex,eventEl);
                return;
              }
          }
        attributesEl.addContent(eventEl);
      }
    else
      {
        attributesEl=new Element("attributes").addContent(eventEl);
        measureEl.addContent(attributesEl);
      }
  }

  Element createDivisionsEl()
  {
    Element attributesEl=null;
    if (addDivisions)
      attributesEl=new Element("attributes").
        addContent(new Element("divisions").setText(Integer.toString(DIVISIONS)));
    addDivisions=false;
    return attributesEl;
  }

  Element createKeyEl(RenderedEvent re)
  {
    ModernKeySignature k=re.getModernKeySig();
    if (k.equals(curKeySig))
      return null;

    Element eventEl=new Element("key");
    eventEl.addContent(new Element("fifths").setText(
      Integer.toString(k.getAccDistance())));
    eventEl.addContent(new Element("mode").setText("major"));

    curKeySig=k;
    return eventEl;
  }

  /* duration including tie info */
  void addNoteInfoData(Element eventEl,RenderList elist,RenderedEvent re,int nt,Proportion l)
  {
    eventEl.addContent(new Element("duration").setText(
      Integer.toString(calcIntDuration(l,DIVISIONS))));

    RenderedLigature tieInfo=re.getTieInfo();
    if (tieInfo.firstEventNum!=-1)
      {
        Element tieEl=new Element("tie");
        RenderedEvent tre1=elist.getEvent(tieInfo.firstEventNum);
        tieEl.setAttribute("type",(tre1==re) ? "start" : "stop");
        eventEl.addContent(tieEl);
        if (re.doubleTied())
          eventEl.addContent(new Element("tie").setAttribute("type","start"));
      }

    eventEl.addContent(new Element("type").setText(
      notetypeToMusicXMLNotetype(nt,l)));
  }

  void addPitchData(Element eventEl,Pitch p,ModernAccidental acc)
  {
    Element pitchEl=new Element("pitch").
      addContent(new Element("step").setText(
        Character.toString(p.noteletter)));

    int alter=(acc!=null) ? acc.pitchOffset : 0;
    if (alter!=0)
      pitchEl.addContent(new Element("alter").setText(
        Integer.toString(alter)));

    pitchEl.addContent(new Element("octave").setText(
      Integer.toString(CMMEtoMusicXMLOctave(p))));

    eventEl.addContent(pitchEl);
  }

  void addLyricData(Element eventEl,NoteEvent ne)
  {
    Element lyricEl=new Element("lyric");
    String  syllabicType="";
    if (ne.isWordEnd())
      {
        syllabicType=midWord ? "end" : "single";
        midWord=false;
      }
    else if (midWord)
      syllabicType="middle";
    else
      {
        syllabicType="begin";
        midWord=true;
      }
    lyricEl.addContent(new Element("syllabic").setText(syllabicType));
    lyricEl.addContent(new Element("text").setText(ne.getModernText()));

    eventEl.addContent(lyricEl);
  }
}
