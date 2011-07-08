/*----------------------------------------------------------------------*/
/*

        Module          : MusicXMLReader.java

        Package         : DataStruct

        Classes	Included: MusicXMLReader

        Purpose         : functions for parsing MusicXML data

        Programmer      : Ted Dumitrescu

        Date Started    : 4/28/10

Updates:
5/14/10: first working version: notes, rests, dots, text, key sigs,
         accidentals; assumes duple time
8/6/10:  renamed MusicXMLReaderWriter to MusicXMLReader (MusicXML generation
         is based on score-rendered data in package Gfx)

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import java.util.*;

import org.jdom.*;
import org.jdom.output.*;

/*------------------------------------------------------------------------
Class:   MusicXMLReader
Extends: -
Purpose: MusicXML functions
------------------------------------------------------------------------*/

public class MusicXMLReader
{
/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  PieceData MusicXMLtoCMME(File mf)
Purpose: Load CMME music data from a MusicXML file
Parameters:
  Input:  File mf - MusicXML file
  Output: -
  Return: CMME music data structure
------------------------------------------------------------------------*/

  public static PieceData MusicXMLtoCMME(InputStream musIn) throws JDOMException,IOException
  {
    Document  XMLdoc=XMLReader.getNoEntityParser().build(musIn);
    PieceData piece;

    /* version */
    float fileVersion=0f;

    Element rootEl=XMLdoc.getRootElement();
    if (!rootEl.getName().equals("score-partwise"))
      throw new JDOMException("Unsupported non-CMME XML file type (currently only MusicXML score-partwise is supported)");
    Attribute MusicXMLVersion=rootEl.getAttribute("version");
    if (MusicXMLVersion!=null)
      fileVersion=Float.parseFloat(MusicXMLVersion.getValue());

    /* initialize output structure */
    piece=new PieceData();
    piece.setGeneralData("Title",null,"Composer","Editor","","Converted from MusicXML data");
    piece.addVariantVersion(new VariantVersionData("Default"));

    /* Voice data section */
    int               numVoices=0;
    Element           VDNode=rootEl.getChild("part-list");
    LinkedList<Voice> vl=new LinkedList<Voice>();
    ArrayList<String> vidl=new ArrayList<String>();
    for (Object curObj : VDNode.getChildren("score-part"))
      {
        Element vEl=(Element)curObj;
        String  vName=vEl.getChildText("part-name");
        Voice   v=new Voice(piece,++numVoices,vName,false);
        vl.add(v);
        vidl.add(vEl.getAttribute("id").getValue());
      }

    Voice[] va=vl.toArray(new Voice[0]);
    MusicMensuralSection curSec=new MusicMensuralSection(numVoices);

    for (Object curObj : rootEl.getChildren("part"))
      {
        Element vEl=(Element)curObj;
        String  id=vEl.getAttribute("id").getValue();
        int     vnum=vidl.indexOf(id);

        VoiceMensuralData curv=new VoiceMensuralData(va[vnum],curSec);

        /* clef + key sig */
        Clef curclef=chooseClef(vEl);
        ClefEvent clefEvent=new ClefEvent(curclef,null,null);
        curv.addEvent(clefEvent);
        addKeySig(curv,clefEvent,vEl);

        /* time signature */
        /* TBD */

        /* notes + rests */
        int     divisions=1,
                curLen=0;
        Element firstNoteEl=null; /* in tied notes, save first note */
        for (Object curMObj : vEl.getChildren("measure"))
          {
            Element mEl=(Element)curMObj;
            for (Object curMChildObj : mEl.getChildren())
              {
                Element mChildEl=(Element)curMChildObj;
                if (mChildEl.getName().equals("note"))
                  {
                    if (firstNoteEl==null)
                      firstNoteEl=mChildEl;

                    if (beginTie(mChildEl))
                      curLen+=noteLen(mChildEl);
                    else
                      {
                        addNoteOrRestEvent(curv,curclef,curLen,divisions,mChildEl,firstNoteEl);
                        curLen=0;
                        firstNoteEl=null;
                      }
                  }
                else if (mChildEl.getName().equals("attributes"))
                  {
                    for (Object dObj : mChildEl.getChildren("divisions"))
                      divisions=XMLReader.getIntVal(dObj);
                  }
              }
          }

        /* add PieceEnd event at end of each voice */
        curv.addEvent(new Event(Event.EVENT_SECTIONEND));

        /* add voice to list */
        curSec.setVoice(vnum,curv);
      }

    piece.setVoiceData(va);
    piece.addSection(curSec);
    return piece;
  }

/*------------------------------------------------------------------------
Method:  void addNoteOrRestEvent(VoiceMensuralData v,Clef curClef,int curLen,int divisions,
                                 Element noteEl,Element firstNoteEl)
Purpose: Generate [Note|Rest]Event (and optional dot?) based on MusicXML data
Parameters:
  Input:  Clef curClef        - current clef
          int curLen          - starting length (from tied notes)
          int divisions       - rhythmic division value
          Element noteEl      - note node of MusicXML data
          Element firstNoteEl - note node for first note in set of tied notes
  Output: VoiceMensuralData v - voice to which to add events
  Return: added event
------------------------------------------------------------------------*/

  static Event addNoteOrRestEvent(VoiceMensuralData v,Clef curClef,int curLen,int divisions,
                                  Element noteEl,Element firstNoteEl)
  {
    int        noteType,
               XMLduration;
    Proportion len;
    boolean    dot=false;
    DotEvent   de=null;

    XMLduration=curLen+noteLen(noteEl);
    len=new Proportion(XMLduration,divisions*2).reduce();
    noteType=NoteEvent.lenToNT(len);
    if (len.toDouble()==NoteEvent.DefaultLengths[noteType].toDouble()*1.5)
      dot=true;

    Event e=null;

    if (noteEl.getChild("rest")==null)
      {
        Pitch            p=parsePitch(noteEl.getChild("pitch"),curClef);
        ModernAccidental a=parseAccidental(noteEl.getChild("pitch"));

        Element lyricEl=firstNoteEl!=null ? firstNoteEl.getChild("lyric") /* get syllable from first note if tied */
                                          : noteEl.getChild("lyric");
        String  modText=parseNoteText(lyricEl);
        boolean wordEnd=isWordEnd(lyricEl);

        NoteEvent ne=new NoteEvent(NoteEvent.NoteTypeNames[noteType],len,p,a,
                                   NoteEvent.LIG_NONE,false,NoteEvent.HALFCOLORATION_NONE,
                                   -1,-1,0,modText,wordEnd,false,NoteEvent.TIE_NONE);
        if (dot)
          {
            Pitch dotPitch=new Pitch(ne.getPitch());
            if (dotPitch.staffspacenum%2==0)
              dotPitch.add(1);
            de=new DotEvent(DotEvent.DT_Addition,dotPitch,ne);
          }

        e=ne;
      }
    else
      {
        RestEvent re=new RestEvent(NoteEvent.NoteTypeNames[noteType],len,
                                   3,RestEvent.calcNumLines(noteType,Mensuration.DEFAULT_MENSURATION),2);
        e=re;
dot=false;
      }

    v.addEvent(e);
    if (dot)
      v.addEvent(de);

    return e;
  }

  /* MusicXML parsing functions for basic notation elements */

  static Pitch parsePitch(Element pitchEl,Clef curClef)
  {
    if (pitchEl==null)
      return null;

    char letter=XMLReader.getCharVal(pitchEl.getChild("step"));
    int  octave=XMLReader.getIntVal(pitchEl.getChild("octave"));
    if (letter>'B')
      octave--;
    return new Pitch(letter,octave,curClef);
  }

  static ModernAccidental parseAccidental(Element pitchEl)
  {
    if (pitchEl==null)
      return null;

    Element ae=pitchEl.getChild("alter");
    int     alter=ae==null ? 0 : XMLReader.getIntVal(ae);

    return new ModernAccidental(alter,false);
  }

  static String parseNoteText(Element lyricEl)
  {
    if (lyricEl==null)
      return null;
    return lyricEl.getChildText("text");
  }

  static boolean isWordEnd(Element lyricEl)
  {
    if (lyricEl==null)
      return false;
    lyricEl=lyricEl.getChild("syllabic");
    return lyricEl!=null && lyricEl.getText().equals("end");
  }

  static boolean beginTie(Element noteEl)
  {
    Element tieEl=noteEl.getChild("notations");
    if (tieEl!=null)
      {
        tieEl=tieEl.getChild("tied");
        if (tieEl!=null)
          if (tieEl.getAttribute("type").getValue().equals("start"))
            return true;
      }
    return false;
  }

  static int noteLen(Element noteEl)
  {
    return XMLReader.getIntVal(noteEl.getChild("duration"));
  }

/*------------------------------------------------------------------------
Method:  Clef chooseClef(Element voiceEl)
Purpose: Pick a clef for one voice based on MusicXML clef info
Parameters:
  Input:  Element voiceEl - root element for voice part
  Output: -
  Return: clef
------------------------------------------------------------------------*/

  public static Clef chooseClef(Element voiceEl)
  {
    /* find first clef info in MusicXML data for one voice */
    for (Object curObj : voiceEl.getChildren("measure"))
      {
        Element attribEl=((Element)curObj).getChild("attributes");
        if (attribEl!=null)
          {
            Element clefEl=attribEl.getChild("clef");
            if (clefEl!=null)
              {
                int clefType=Clef.strToCleftype(clefEl.getChildText("sign")),
                    clefLine=Clef.lineNumToLinespaceNum(Integer.parseInt(clefEl.getChildText("line")));
                switch (clefType)
                  {
                    case Clef.CLEF_NONE:
                      clefType=Clef.CLEF_C;
                      clefLine=1;
                      break;
                    case Clef.CLEF_G:
                      Element ocEl=clefEl.getChild("clef-octave-change");
                      if (ocEl!=null && Integer.parseInt(ocEl.getText())==-1)
                        {
                          clefType=Clef.CLEF_C;
                          clefLine=7;
                        }
                      break;
                  }
                return new Clef(clefType,clefLine,Clef.DefaultClefPitches[clefType],false,false,null);
              }
          }
      }

    /* no clef found in data; default to C1 */
    return new Clef(Clef.CLEF_C,1,Clef.DefaultClefPitches[Clef.CLEF_C],false,false,null);
  }

/*------------------------------------------------------------------------
Method:  void addKeySig(VoiceMensuralData curv,ClefEvent mainClef,Element voiceEl)
Purpose: Add key signature clefs to one voice based on MusicXML key info
Parameters:
  Input:  ClefEvent mainClef - event with main clef in voice part
          Element voiceEl    - root element for voice part
  Output: VoiceMensuralData curv - voice event data
  Return: -
------------------------------------------------------------------------*/

  public static void addKeySig(VoiceMensuralData curv,ClefEvent mainClef,Element voiceEl)
  {
    Event lastEvent=mainClef;

    /* find first keysig info in MusicXML data for one voice */
    for (Object curObj : voiceEl.getChildren("measure"))
      {
        Element attribEl=((Element)curObj).getChild("attributes");
        if (attribEl!=null)
          {
            Element keyEl=attribEl.getChild("key");
            if (keyEl!=null)
              {
                Element fifthsEl=keyEl.getChild("fifths");
                if (fifthsEl!=null)
                  {
                    int fifths=Integer.parseInt(fifthsEl.getText());
                    for (int i=0; i<fifths*-1 && i<ModernKeySignature.CircleOfFifthsPitches.length; i++)
                      {
                        Pitch roundBPitch=new Pitch(ModernKeySignature.CircleOfFifthsPitches[i]);
                        roundBPitch.setOctave(mainClef.getClef().pitch.octave);
                        Clef sigClef=new Clef(Clef.CLEF_Bmol,Clef.defaultClefLoc(Clef.CLEF_Bmol),
                                              roundBPitch,false,true,mainClef.getClef());
                        ClefEvent sigClefEvent=new ClefEvent(sigClef,lastEvent,mainClef);
                        curv.addEvent(sigClefEvent);
                        lastEvent=sigClefEvent;
                      }
                  }
                return;
              }
          }
      }
  }
}
