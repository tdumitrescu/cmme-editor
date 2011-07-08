/*----------------------------------------------------------------------*/
/*

        Module          : CMMEOldVersionParser.java

        Package         : DataStruct

        Classes Included: CMMEOldVersionParser

        Purpose         : Input of deprecated files (for conversion to current
                          format)

        Programmer      : Ted Dumitrescu

        Date Started    : 3/1/07

        Updates:

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import org.jdom.*;
import org.jdom.output.*;

import java.net.*;
import java.io.*;
import java.util.*;

import DataStruct.XMLReader;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   CMMEOldVersionParser
Extends: -
Purpose: Input of deprecated CMME music files
------------------------------------------------------------------------*/

public class CMMEOldVersionParser
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static Namespace cmmens;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance variables */

  public PieceData piece;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Class methods */

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: CMMEOldVersionParser(String fn)
Purpose:     Parse local file
Parameters:
  Input:  String fn - filename for input
  Output: -
------------------------------------------------------------------------*/

  public CMMEOldVersionParser(String fn) throws JDOMException,IOException
  {
    constructPieceData(XMLReader.getparser().build(fn));
  }

/*------------------------------------------------------------------------
Constructor: CMMEOldVersionParser(URL remoteloc)
Purpose:     Parse remote resource
Parameters:
  Input:  URL remoteloc - URL for input
  Output: -
------------------------------------------------------------------------*/

  public CMMEOldVersionParser(URL remoteloc) throws JDOMException,IOException
  {
    constructPieceData(XMLReader.getparser().build(remoteloc));
  }

/*------------------------------------------------------------------------
Method:  void constructPieceData(Document cmmedoc)
Purpose: Use parsed document to construct data structure for piece
Parameters:
  Input:  Document cmmedoc - DOM tree holding CMME data
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void constructPieceData(Document cmmedoc)
  {

    /* --- OLD VERSIONS: .8 and earlier --- */

    piece=new PieceData();
    cmmens=Namespace.getNamespace("http://www.cmme.org");

    /* version
    Attribute CMMEversion=cmmedoc.getRootElement().getAttribute("CMMEversion");
    if (CMMEversion==null)
      System.err.println("Deprecated document version (pre-.81)");*/

    /* General data section */
    Element GDNode=cmmedoc.getRootElement().getChild("GeneralData",cmmens);
    piece.setGeneralData(GDNode.getChildText("Title",cmmens),
                         GDNode.getChildText("Section",cmmens),
                         GDNode.getChildText("Composer",cmmens),
                         GDNode.getChildText("Editor",cmmens),
                         GDNode.getChildText("PublicNotes",cmmens),
                         GDNode.getChildText("Notes",cmmens));
    Element BC=GDNode.getChild("BaseColoration",cmmens);
    if (BC!=null)
      piece.setBaseColoration(parseColoration(BC));
    if (GDNode.getChild("Incipit",cmmens)!=null)
      piece.setIncipitScore(true);

    /* Voice data section */
    Element VDNode=cmmedoc.getRootElement().getChild("VoiceData",cmmens);
    int     numvoices=Integer.parseInt(VDNode.getChildText("NumVoices",cmmens));

    Voice[] vl=new Voice[numvoices];
    int     vi=0;
    for (Object curObj : VDNode.getChildren("Voice",cmmens))
      {
        Element curvel=(Element)curObj;
        Clef    suggestedModernClef=null;
        if (curvel.getChild("SuggestedModernClef",cmmens)!=null)
          suggestedModernClef=Clef.DefaultModernClefs[Clef.strToCleftype(
            curvel.getChildText("SuggestedModernClef",cmmens))];
        Voice curv=new Voice(piece,vi+1,curvel.getChildText("Name",cmmens),
                             curvel.getChild("Editorial",cmmens)!=null,
                             suggestedModernClef);
        vl[vi++]=curv;
      }
    piece.setVoiceData(vl);

    /* Music sections */
    MusicMensuralSection curSection=parseMensuralMusic(VDNode);
    piece.addSection(curSection);
  }

/*------------------------------------------------------------------------
Method:  MusicMensuralSection parseMensuralMusic(Element VDNode)
Purpose: Create section of mensural music from document tree segment
Parameters:
  Input:  Element VDNode - tree segment representing pre-.81 voice data
  Output: -
  Return: mensural music section
------------------------------------------------------------------------*/

  /* parameters for use while parsing one voice */
  Event              clefinfoevent,
                     mensinfoevent;
  Coloration         curcolor;
  ModernKeySignature curModKeySig;
  Event              lastevent;

  MusicMensuralSection parseMensuralMusic(Element VDNode)
  {
    int                  numVoices=Integer.parseInt(VDNode.getChildText("NumVoices",cmmens)),
                         sectionVoiceNum=0;
    MusicMensuralSection curSection=new MusicMensuralSection(numVoices);

    for (Object curObj : VDNode.getChildren("Voice",cmmens))
      {
        Element curVoiceEl=(Element)curObj;

        /* event list for one voice */
        clefinfoevent=null;
        mensinfoevent=null;
        curcolor=piece.getBaseColoration();
        curModKeySig=ModernKeySignature.DEFAULT_SIG;

        VoiceMensuralData curv=new VoiceMensuralData(piece.getVoiceData()[sectionVoiceNum],curSection);

        lastevent=null;
        for (Object curEvObj : curVoiceEl.getChild("EventList",cmmens).getChildren())
          {
            /* create structure for current event depending on type */
            Element cureventel=(Element)curEvObj;
            String eventtype=cureventel.getName();

            if (eventtype.equals("EditorialData"))
              {
                Event ed=new Event(Event.EVENT_VARIANTDATA_START);
                ed.setEditorial(true);
                curv.addEvent(ed);
                lastevent=ed;
                for (Iterator edei=cureventel.getChild("NewReading",cmmens).getChildren().iterator();
                     edei.hasNext();)
                  {
                    addSingleOrMultiEvent(curv,(Element)edei.next());
                    lastevent.setEditorial(true);
                  }
                curv.addEvent(ed=new Event(Event.EVENT_VARIANTDATA_END));
                lastevent=ed;
              }
            else
              addSingleOrMultiEvent(curv,cureventel);
          }
        /* add SectionEnd event at end of each voice */
        addNewEvent(curv,new Event(Event.EVENT_SECTIONEND));

        /* set voice data in section */
        curSection.setVoice(sectionVoiceNum++,curv);
      }

    return curSection;
  }

/*------------------------------------------------------------------------
Method:  void addNewEvent(VoiceMensuralData v,Event e)
Purpose: Add one parsed event to event list of a given voice
Parameters:
  Input:  Event e             - parsed event
  Output: VoiceMensuralData v - voice being constructed
  Return: -
------------------------------------------------------------------------*/

  void addNewEvent(VoiceMensuralData v,Event e)
  {
    e.setclefparams(clefinfoevent);
    e.setmensparams(mensinfoevent);
    e.setcolorparams(curcolor);
    e.setModernKeySigParams(curModKeySig);
    v.addEvent(e);
  }

/*------------------------------------------------------------------------
Method:  void addSingleOrMultiEvent(VoiceMensuralData v,Element cureventel)
Purpose: Parse one event and add to event list of a given voice
Parameters:
  Input:  Element cureventel  - event node to parse
  Output: VoiceMensuralData v - voice being constructed
  Return: -
------------------------------------------------------------------------*/

  void addSingleOrMultiEvent(VoiceMensuralData v,Element cureventel)
  {
    String eventtype=cureventel.getName();
    Event  curevent;

    if (eventtype.equals("MultiEvent"))
      curevent=parseMultiEvent(cureventel);
    else
      curevent=parseSingleEvent(cureventel);

    /* add event to list */
    addNewEvent(v,curevent);

    /* add data-free marker for end of lacunae */
    if (curevent.geteventtype()==Event.EVENT_LACUNA)
      addNewEvent(v,curevent=new Event(Event.EVENT_LACUNA_END));

    lastevent=curevent;
  }

/*------------------------------------------------------------------------
Method:  MultiEvent parseMultiEvent(Element cureventel)
Purpose: Create multi-event (list of simultaneous events) from document
         tree segment
Parameters:
  Input:  Element cureventel - multi-event node
  Output: -
  Return: multi-event
------------------------------------------------------------------------*/

  MultiEvent parseMultiEvent(Element cureventel)
  {
    MultiEvent curevent=new MultiEvent();
    Event      curclefevent=clefinfoevent;

    for (Iterator ei=cureventel.getChildren().iterator(); ei.hasNext();)
      {
        Event e=parseSingleEvent((Element)ei.next());
        curevent.addEvent(e);
        if (mensinfoevent==e)
          mensinfoevent=curevent;
      }

    if (curevent.hasSignatureClef())
      {
        curevent.constructClefSets(lastevent,curclefevent);
        if (curclefevent==null || curevent.getClefSet()!=curclefevent.getClefSet())
          clefinfoevent=curevent;
      }
    return curevent;
  }

/*------------------------------------------------------------------------
Method:  Event parseSingleEvent(Element cureventel)
Purpose: Create single event from document tree segment
Parameters:
  Input:  Element cureventel - event node
  Output: -
  Return: event
------------------------------------------------------------------------*/

  Event parseSingleEvent(Element cureventel)
  {
    Event  curevent;
    String eventtype=cureventel.getName();

    if (eventtype.equals("Clef"))
      {
        curevent=parseClefEvent(cureventel,lastevent);

        /* set clefinfoevent if necessary */
        ClefEvent ce=(ClefEvent)curevent;
        if (ce.hasSignatureClef() &&
            (clefinfoevent==null || ce.getClefSet()!=clefinfoevent.getClefSet()))
          {
            clefinfoevent=ce;
            curModKeySig=ce.getClefSet().getKeySig();
          }
      }
    else if (eventtype.equals("Mensuration"))
      {
        curevent=parseMensurationEvent(cureventel);
        mensinfoevent=curevent;
      }
    else if (eventtype.equals("Rest"))
      curevent=parseRestEvent(cureventel,mensinfoevent);
    else if (eventtype.equals("Note"))
      curevent=parseNoteEvent(cureventel,clefinfoevent);
    else if (eventtype.equals("Dot"))
      curevent=parseDotEvent(cureventel);
    else if (eventtype.equals("OriginalText"))
      curevent=parseOriginalTextEvent(cureventel);
    else if (eventtype.equals("Proportion"))
      curevent=parseProportionEvent(cureventel);
    else if (eventtype.equals("ColorChange"))
      {
        curevent=parseColorChangeEvent(cureventel,curcolor);
        curcolor=((ColorChangeEvent)curevent).getcolorscheme();
      }
    else if (eventtype.equals("Custos"))
      curevent=parseCustosEvent(cureventel,clefinfoevent);
    else if (eventtype.equals("LineEnd"))
      curevent=parseLineEndEvent(cureventel);
    else if (eventtype.equals("MiscItem"))
      curevent=parseMiscItemEvent(cureventel);

    else if (eventtype.equals("ModernKeySignature"))
      {
        curevent=parseModernKeySignatureEvent(cureventel);
        curModKeySig=((ModernKeySignatureEvent)curevent).getSigInfo();
      }

    else
      curevent=new Event();

    curevent.setclefparams(clefinfoevent);
    curevent.setmensparams(mensinfoevent);
    curevent.setcolorparams(curcolor);
    curevent.setModernKeySigParams(curModKeySig);
    if (cureventel.getChild("Colored",cmmens)!=null)
      curevent.setColored(true);
    curevent.setEdCommentary(cureventel.getChildText("EditorialCommentary",cmmens));
    return curevent;
  }

/*------------------------------------------------------------------------
Method:  Event parseClefEvent(Element e,Event le)
Purpose: Create clef event from document tree segment
Parameters:
  Input:  Element e - "Clef" node
          Event le  - last event parsed
  Output: -
  Return: clef event
------------------------------------------------------------------------*/

  Event parseClefEvent(Element e,Event le)
  {
    return new ClefEvent(
      e.getChildText("Appearance",cmmens),
      Integer.parseInt(e.getChildText("StaffLoc",cmmens)),
      new Pitch(
        e.getChild("Pitch",cmmens).getChildText("LetterName",cmmens).charAt(0),
        Integer.parseInt(e.getChild("Pitch",cmmens).getChildText("OctaveNum",cmmens))),
      le,clefinfoevent,
      e.getChild("Signature",cmmens)!=null);
  }

/*------------------------------------------------------------------------
Method:  Event parseMensurationEvent(Element e)
Purpose: Create mensuration event from document tree segment
Parameters:
  Input:  Element e - "Mensuration" node
  Output: -
  Return: mensuration event
------------------------------------------------------------------------*/

  Event parseMensurationEvent(Element e)
  {
    LinkedList<MensSignElement> signs=new LinkedList<MensSignElement>();
    Mensuration                 mensInfo=null;
    int                         ssnum=4;
    boolean                     vertical=false,small=false;

    Element curmensel;
    for (Iterator i=e.getChildren().iterator(); i.hasNext(); )
      {
        curmensel=(Element)i.next();
        if (curmensel.getName().equals("Sign"))
	  {
            String  ms=curmensel.getChildText("MainSymbol",cmmens);
            int     signType=MensSignElement.NO_SIGN;
            boolean dotted=false,stroke=false;
            if (ms.equals("C"))
              {
                Element or=curmensel.getChild("Orientation",cmmens);
                if (or!=null && or.getText().equals("Reversed"))
                  signType=MensSignElement.MENS_SIGN_CREV;
                else
                  signType=MensSignElement.MENS_SIGN_C;
              }
            else if (ms.equals("O"))
              signType=MensSignElement.MENS_SIGN_O;
            if ((curmensel.getChild("Strokes",cmmens)!=null) &&
                Integer.parseInt(curmensel.getChildText("Strokes",cmmens))>0)
              stroke=true;
            if (curmensel.getChild("Dot",cmmens)!=null)
              dotted=true;

            signs.add(new MensSignElement(signType,dotted,stroke));
          }
        else if (curmensel.getName().equals("Number"))
          signs.add(new MensSignElement(
            MensSignElement.NUMBERS,parseProportion(curmensel)));

        else if (curmensel.getName().equals("StaffLoc"))
          ssnum=Integer.parseInt(curmensel.getText());
        else if (curmensel.getName().equals("Orientation") &&
                 curmensel.getText().equals("Vertical"))
          vertical=true;
        else if (curmensel.getName().equals("Small"))
          small=true;

        else if (curmensel.getName().equals("MensInfo"))
          mensInfo=new Mensuration(
            Integer.parseInt(curmensel.getChildText("Prolatio",cmmens)),
            Integer.parseInt(curmensel.getChildText("Tempus",cmmens)),
            Integer.parseInt(curmensel.getChildText("ModusMinor",cmmens)),
            Integer.parseInt(curmensel.getChildText("ModusMaior",cmmens)));
      }

    return new MensEvent(signs,ssnum,small,vertical,mensInfo);
  }

/*------------------------------------------------------------------------
Method:  Event parseRestEvent(Element e,Event me)
Purpose: Create rest event from document tree segment
Parameters:
  Input:  Element e - "Rest" node
          Event me  - current mensuration event
  Output: -
  Return: rest event
------------------------------------------------------------------------*/

  Event parseRestEvent(Element e,Event me)
  {
    RestEvent re=new RestEvent(
      e.getChildText("Type",cmmens),
      parseProportion(e.getChild("Length",cmmens)),
      Integer.parseInt(e.getChildText("BottomStaffLine",cmmens)),
      Integer.parseInt(e.getChildText("NumSpaces",cmmens)),
      me==null ? 2 : me.getMensInfo().modus_maior);
    if (e.getChild("Coronata",cmmens)!=null)
      re.setCorona(new Signum(Signum.UP,Signum.MIDDLE));
    re.setSignum(parseSignum(e.getChild("Signum",cmmens)));
    return re;
  }

/*------------------------------------------------------------------------
Method:  Event parseNoteEvent(Element e,Event ce)
Purpose: Create note event from document tree segment
Parameters:
  Input:  Element e - "Note" node
          Event ce  - current clef event
  Output: -
  Return: note event
------------------------------------------------------------------------*/

  Event parseNoteEvent(Element e,Event ce)
  {
    ModernAccidental ma=null;
    int              ligstatus=NoteEvent.LIG_NONE;
    Element          opte;
    boolean          col=false,flagged=false,wordEnd=false;
    int              stemdir=-1,stemside=-1;
    String           modernText=null;

    if ((opte=e.getChild("ModernAccidental",cmmens))!=null)
      ma=parseModernAccidental(opte);

    if ((opte=e.getChild("Lig",cmmens))!=null)
      if (opte.getText().equals("Recta"))
        ligstatus=NoteEvent.LIG_RECTA;
      else if (opte.getText().equals("Obliqua"))
        ligstatus=NoteEvent.LIG_OBLIQUA;

    if (e.getChild("Colored",cmmens)!=null)
      col=true;
    if (e.getChild("Flagged",cmmens)!=null)
      flagged=true;

    if ((opte=e.getChild("Stem",cmmens))!=null)
      {
        stemdir=NoteEvent.strtoStemDir(opte.getChildText("Dir",cmmens));
        String ss=opte.getChildText("Side",cmmens);
        if (ss!=null)
          stemside=NoteEvent.strtoStemDir(ss);
      }

    if ((opte=e.getChild("ModernText",cmmens))!=null)
      {
        modernText=opte.getChildText("Syllable",cmmens);
        if (opte.getChild("WordEnd",cmmens)!=null)
          wordEnd=true;
      }

    NoteEvent ne=new NoteEvent(
      e.getChildText("Type",cmmens),
      parseProportion(e.getChild("Length",cmmens)),
      new Pitch(e.getChildText("LetterName",cmmens).charAt(0),
                Integer.parseInt(e.getChildText("OctaveNum",cmmens)),
                ce!=null ? ce.getPrincipalClef(false) : null),
      ma,ligstatus,col,NoteEvent.HALFCOLORATION_NONE,stemdir,stemside,
      flagged ? 1 : 0,modernText,wordEnd,false,NoteEvent.TIE_NONE);
    if (e.getChild("Coronata",cmmens)!=null)
      ne.setCorona(new Signum(Signum.UP,Signum.MIDDLE));
    ne.setSignum(parseSignum(e.getChild("Signum",cmmens)));

    return ne;
  }

/*------------------------------------------------------------------------
Method:  Event parseDotEvent(Element e)
Purpose: Create dot event from document tree segment
Parameters:
  Input:  Element e - "Dot" node
  Output: -
  Return: dot event
------------------------------------------------------------------------*/

  Event parseDotEvent(Element e)
  {
    return new DotEvent(null);
  }

/*------------------------------------------------------------------------
Method:  Event parseOriginalTextEvent(Element e)
Purpose: Create original text event from document tree segment
Parameters:
  Input:  Element e - "OriginalText" node
  Output: -
  Return: original text event
------------------------------------------------------------------------*/

  Event parseOriginalTextEvent(Element e)
  {
    return new OriginalTextEvent(e.getChildText("Phrase",cmmens));
  }

/*------------------------------------------------------------------------
Method:  Event parseProportionEvent(Element e)
Purpose: Create proportion event from document tree segment
Parameters:
  Input:  Element e - "Proportion" node
  Output: -
  Return: proportion event
------------------------------------------------------------------------*/

  Event parseProportionEvent(Element e)
  {
    return new ProportionEvent(parseProportion(e));
  }

/*------------------------------------------------------------------------
Method:  Event parseColorChangeEvent(Element e,Coloration lastc)
Purpose: Create color change event from document tree segment
Parameters:
  Input:  Element e        - "ColorChange" node
          Coloration lastc - previous coloration scheme
  Output: -
  Return: color change event
------------------------------------------------------------------------*/

  Event parseColorChangeEvent(Element e,Coloration lastc)
  {
    /* new coloration = last coloration + differences */
    return new ColorChangeEvent(new Coloration(lastc,parseColoration(e)));
  }

/*------------------------------------------------------------------------
Method:  Event parseCustosEvent(Element e,Event ce)
Purpose: Create custos event from document tree segment
Parameters:
  Input:  Element e - "Custos" node
          Event ce  - current clef event
  Output: -
  Return: custos event
------------------------------------------------------------------------*/

  Event parseCustosEvent(Element e,Event ce)
  {
    return new CustosEvent(
      new Pitch(e.getChildText("LetterName",cmmens).charAt(0),
                Integer.parseInt(e.getChildText("OctaveNum",cmmens)),
                ce!=null ? ce.getPrincipalClef(false) : null));
  }

/*------------------------------------------------------------------------
Method:  Event parseLineEndEvent(Element e)
Purpose: Create line end event from document tree segment
Parameters:
  Input:  Element e - "LineEnd" node
  Output: -
  Return: line end event
------------------------------------------------------------------------*/

  Event parseLineEndEvent(Element e)
  {
    return new LineEndEvent(e.getChild("PageEnd",cmmens)!=null);
  }

/*------------------------------------------------------------------------
Method:  Event parseMiscItemEvent(Element e)
Purpose: Create misc event from document tree segment
Parameters:
  Input:  Element e - "MiscItem" node
  Output: -
  Return: event
------------------------------------------------------------------------*/

  Event parseMiscItemEvent(Element e)
  {
    Element me;

    if ((me=e.getChild("Barline",cmmens))!=null)
      {
        Element nle=me.getChild("NumLines",cmmens);
        if (nle!=null)
          return new BarlineEvent(Integer.parseInt(nle.getText()),false,0,4);
        else
          return new BarlineEvent();
      }

    if ((me=e.getChild("TextAnnotation",cmmens))!=null)
      {
        String  s=me.getChildText("Text",cmmens);
        Element sle=me.getChild("StaffLoc",cmmens);
        if (sle!=null)
          return new AnnotationTextEvent(s,Integer.parseInt(sle.getText()));
        else
          return new AnnotationTextEvent(s);
      }

    if ((me=e.getChild("Lacuna",cmmens))!=null)
      return new LacunaEvent(parseProportion(me.getChild("Length",cmmens)));

    if ((me=e.getChild("Ellipsis",cmmens))!=null)
      return new Event(Event.EVENT_ELLIPSIS);

    return null;
  }

/*------------------------------------------------------------------------
Method:  Event parseModernKeySignatureEvent(Element e)
Purpose: Create modern key signature event from document tree segment
Parameters:
  Input:  Element e - "ModernKeySignature" node
  Output: -
  Return: event
------------------------------------------------------------------------*/

  Event parseModernKeySignatureEvent(Element e)
  {
    ModernKeySignature mks=new ModernKeySignature();
    for (Iterator ei=e.getChildren().iterator(); ei.hasNext();)
      {
        Element curElTree=(Element)ei.next();
        char pl=curElTree.getChildText("Pitch",cmmens).charAt(0);
        int  po=-1;
        if (curElTree.getChild("Octave",cmmens)!=null)
          po=Integer.parseInt(curElTree.getChildText("Octave",cmmens));
        ModernAccidental ma=parseModernAccidental(curElTree.getChild("Accidental",cmmens));

        mks.addElement(new ModernKeySignatureElement(new Pitch(pl,po),ma));
      }

    return new ModernKeySignatureEvent(mks);
  }

/*------------------------------------------------------------------------
Method:  Proportion parseColoration(Element e)
Purpose: Create Coloration structure from document tree segment
Parameters:
  Input:  Element e - coloration node
  Output: -
  Return: coloration structure
------------------------------------------------------------------------*/

  Coloration parseColoration(Element e)
  {
    int pc=Coloration.NONE,pf=Coloration.NONE,
        sc=Coloration.NONE,sf=Coloration.NONE;

    Element colel=e.getChild("PrimaryColor",cmmens);
    pc=Coloration.strtoColor(colel.getChildText("Color",cmmens));
    pf=Coloration.strtoColorFill(colel.getChildText("Fill",cmmens));

    if ((colel=e.getChild("SecondaryColor",cmmens))!=null)
      {
        sc=Coloration.strtoColor(colel.getChildText("Color",cmmens));
        sf=Coloration.strtoColorFill(colel.getChildText("Fill",cmmens));
      }
    else
      {
        /* secondary color unspecified: use default complement */
        sc=pc;
        sf=Coloration.complementaryFill(pf);
      }

    return new Coloration(pc,pf,sc,sf);
  }

/*------------------------------------------------------------------------
Method:  ModernAccidental parseModernAccidental(Element e)
Purpose: Create ModernAccidental structure from document tree segment
Parameters:
  Input:  Element e - accidental node
  Output: -
  Return: accidental structure
------------------------------------------------------------------------*/

  ModernAccidental parseModernAccidental(Element e)
  {
    int numa=(e.getChild("Num",cmmens)!=null) ?
              Integer.parseInt(e.getChildText("Num",cmmens)) : 1;
    return new ModernAccidental(ModernAccidental.strtoMA(e.getChildText("AType",cmmens)),
                                numa,e.getChild("Optional",cmmens)!=null);
  }

/*------------------------------------------------------------------------
Method:  Signum parseSignum(Element e)
Purpose: Create Signum structure from document tree segment
Parameters:
  Input:  Element e - signum node
  Output: -
  Return: signum
------------------------------------------------------------------------*/

  Signum parseSignum(Element e)
  {
    if (e==null)
      return null;

    int offset=Integer.parseInt(e.getChildText("Offset",cmmens)),
        orientation=Signum.UP,
        side=Signum.MIDDLE;

   String os=e.getChildText("Orientation",cmmens);
   if (os!=null && os.equals("Down"))
     orientation=Signum.DOWN;
   String ss=e.getChildText("Side",cmmens);
   if (ss!=null)
     if (ss.equals("Left"))
       side=Signum.LEFT;
     else if (ss.equals("Right"))
       side=Signum.RIGHT;

   return new Signum(offset,orientation,side);
  }

/*------------------------------------------------------------------------
Method:  Proportion parseProportion(Element e)
Purpose: Create Proportion structure from document tree segment
Parameters:
  Input:  Element e - proportion node
  Output: -
  Return: proportion structure
------------------------------------------------------------------------*/

  Proportion parseProportion(Element e)
  {
    return new Proportion(Integer.parseInt(e.getChildText("Num",cmmens)),
                          Integer.parseInt(e.getChildText("Den",cmmens)));
  }


/*--------------------------- OUTPUT METHODS ---------------------------*/

/*------------------------------------------------------------------------
Method:  void outputPieceData(PieceData pdata,OutputStream outs)
Purpose: Output CMME format file from PieceData structure
Parameters:
  Input:  PieceData pdata   - structure containing music
          OutputStream outs - output destination
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void outputPieceData(PieceData pdata,OutputStream outs)
  {
    Document outputdoc;
    Element  rootel;

System.err.println("Outputting...");
    cmmens=Namespace.getNamespace("http://www.cmme.org");
    Namespace xsins=Namespace.getNamespace("xsi","http://www.w3.org/2001/XMLSchema-instance");

    /* header/root */
    rootel=new Element("Piece",cmmens);
    rootel.setAttribute("CMMEversion",MetaData.CMME_VERSION);
    rootel.addNamespaceDeclaration(cmmens);
    rootel.addNamespaceDeclaration(xsins);
    rootel.setAttribute("schemaLocation","http://www.cmme.org cmme.xsd",xsins);

    /* actual content */
    rootel.addContent(createGeneralDataTree(pdata));
    rootel.addContent(createVoiceDataTree(pdata.getVoiceData()));
    for (int si=0; si<pdata.getNumSections(); si++)
      rootel.addContent(createMusicSectionTree(pdata.getSection(si)));

    outputdoc=new Document(rootel);

    /* output */
    try
      {
        XMLOutputter xout=new XMLOutputter(Format.getRawFormat().setIndent("  "));
        xout.output(outputdoc,outs);
      }
    catch (Exception e)
      {
        System.err.println("Error writing XML doc: "+e);
      }
  }

/*------------------------------------------------------------------------
Method:  Element createGeneralDataTree(PieceData pdata)
Purpose: Construct tree segment for GeneralData
Parameters:
  Input:  PieceData pdata - overall piece structure
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createGeneralDataTree(PieceData pdata)
  {
    Element gdel=new Element("GeneralData",cmmens);
    if (pdata.isIncipitScore())
      gdel.addContent(new Element("Incipit",cmmens));
    gdel.addContent(new Element("Title",cmmens).setText(pdata.getTitle()));
    String st=pdata.getSectionTitle();
    if (st!=null)
      gdel.addContent(new Element("Section",cmmens).setText(st));
    gdel.addContent(new Element("Composer",cmmens).setText(pdata.getComposer()));
    gdel.addContent(new Element("Editor",cmmens).setText(pdata.getEditor()));
    if (!pdata.getBaseColoration().equals(Coloration.DEFAULT_COLORATION))
      gdel.addContent(addColorationData(new Element("BaseColoration",cmmens),pdata.getBaseColoration()));
    return gdel;
  }

/*------------------------------------------------------------------------
Method:  Element createVoiceDataTree(Voice[] vdata)
Purpose: Construct tree segment for VoiceData
Parameters:
  Input:  Voice[] vdata - array of structures containing voice data
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createVoiceDataTree(Voice[] vdata)
  {
    Element vdel=new Element("VoiceData",cmmens);
    vdel.addContent(new Element("NumVoices",cmmens).setText(Integer.toString(vdata.length)));

    for (Voice curv : vdata)
      {
        Element curvel=new Element("Voice",cmmens);
        curvel.addContent(new Element("Name",cmmens).setText(curv.getName()));
        if (curv.isEditorial())
          curvel.addContent(new Element("Editorial",cmmens));
        Clef c=curv.getSuggestedModernClef();
        if (c!=null)
          curvel.addContent(new Element("SuggestedModernClef",cmmens).setText(Clef.ClefNames[c.cleftype]));

        vdel.addContent(curvel);
      }

    return vdel;
  }

/*------------------------------------------------------------------------
Method:  Element createMusicSectionTree(MusicSection ms)
Purpose: Construct tree segment for one MusicSection
Parameters:
  Input:  MusicSection ms - music section
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createMusicSectionTree(MusicSection ms)
  {
    Element msel=new Element("MusicSection",cmmens);
    if (ms.isEditorial())
      msel.addContent(new Element("Editorial",cmmens));

    switch (ms.getSectionType())
      {
        case MusicSection.MENSURAL_MUSIC:
          msel.addContent(createMusicMensuralSectionTree((MusicMensuralSection)ms));
          break;
        default:
System.out.println("creating tree for non-mensural music section");
          break;
      }

    return msel;
  }

/*------------------------------------------------------------------------
Method:  Element createMusicMensuralSectionTree(MusicMensuralSection mms)
Purpose: Construct tree segment for one MusicMensuralSection
Parameters:
  Input:  MusicMensuralSection mms - mensural music section
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createMusicMensuralSectionTree(MusicMensuralSection mms)
  {
    Element mmsel=new Element("MensuralMusic",cmmens);
    mmsel.addContent(new Element("NumVoices",cmmens).setText(Integer.toString(mms.getNumVoices())));

/* INSERT tacet instructions */

    for (int vi=0; vi<mms.getNumVoices(); vi++)
      {
        VoiceMensuralData curvmd=(VoiceMensuralData)mms.getVoice(vi);

        Element curvel=new Element("Voice",cmmens);
        curvel.addContent(new Element("VoiceNum",cmmens).setText(Integer.toString(curvmd.getVoiceNum()+1)));
        curvel.addContent(createVoiceEventTree(curvmd));

        mmsel.addContent(curvel);
      }

    return mmsel;
  }

/*------------------------------------------------------------------------
Method:  Element createVoiceEventTree(VoiceMensuralData v)
Purpose: Construct tree segment for all Events in one voice section
Parameters:
  Input:  VoiceMensuralData v - data for one voice
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Coloration lastcol;

  static Element createVoiceEventTree(VoiceMensuralData v)
  {
    Element elel=new Element("EventList",cmmens),
            curevel;
    Event   cure;

    lastcol=v.getMetaData().getGeneralData().getBaseColoration();
    for (Iterator i=v.events.iterator(); i.hasNext();)
      {
        cure=(Event)i.next();

        if (cure.geteventtype()==Event.EVENT_VARIANTDATA_START)
          curevel=createEditorialDataTree(i);
        else
          curevel=createOneEventTree(cure);

        if (curevel!=null)
          elel.addContent(curevel);
      }

    return elel;
  }

/*------------------------------------------------------------------------
Method:  Element createEditorialDataTree(Iterator i)
Purpose: Construct tree segment for segment of editorially-supplied events
Parameters:
  Input:  Iterator i - event list iterator, positioned at start of editorial
                       segment
  Output: Iterator i
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createEditorialDataTree(Iterator i)
  {
    Element edel=new Element("EditorialData",cmmens),
            nrel=new Element("NewReading",cmmens),
            orel=new Element("OriginalReading",cmmens),
            curevel;
    Event   cure;

    /* create event list for editorial reading */
    cure=i.hasNext() ? (Event)i.next() : null;
    if (cure==null || cure.geteventtype()==Event.EVENT_VARIANTDATA_END)
      return null; /* can't have empty editorial data sections */
    while (cure!=null && cure.geteventtype()!=Event.EVENT_VARIANTDATA_END)
      {
        curevel=createOneEventTree(cure);
        if (curevel!=null)
          nrel.addContent(curevel);

        cure=i.hasNext() ? (Event)i.next() : null;
      }

    /* create structure for 'original' reading (currently limited to lacunae) */
    orel.addContent(new Element("Lacuna",cmmens));

    /* create main structure */
    edel.addContent(nrel);
    edel.addContent(orel);

    return edel;
  }

/*------------------------------------------------------------------------
Method:  Element createMultiEventTree(MultiEvent cure)
Purpose: Construct tree segment for a multi-event
Parameters:
  Input:  MultiEvent cure - multi-event data
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createMultiEventTree(MultiEvent cure)
  {
    Element melel=new Element("MultiEvent",cmmens);

    for (Iterator i=cure.iterator(); i.hasNext();)
      melel.addContent(createOneEventTree((Event)i.next()));

    return melel;
  }

/*------------------------------------------------------------------------
Method:  Element createOneEventTree(Event cure)
Purpose: Construct tree segment for a single Event
Parameters:
  Input:  Event cure - single event data
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createOneEventTree(Event cure)
  {
    Element curevel;

    switch (cure.geteventtype())
      {
        case Event.EVENT_MULTIEVENT:
          return createMultiEventTree((MultiEvent)cure);

        case Event.EVENT_CLEF:
          curevel=new Element("Clef",cmmens);
          Clef c=((ClefEvent)cure).getClef(false,false);
          curevel.addContent(new Element("Appearance",cmmens).setText(
            Clef.ClefNames[c.cleftype]));
          curevel.addContent(new Element("StaffLoc",cmmens).setText(
            Integer.toString(c.linespacenum)));
          curevel.addContent(addLocusData(new Element("Pitch",cmmens),c.pitch));
          if (c.signature)
            curevel.addContent(new Element("Signature",cmmens));
          break;

        case Event.EVENT_MENS:
          /* need to add Orientation etc */
          curevel=new Element("Mensuration",cmmens);
          MensEvent m=(MensEvent)cure;

          for (Iterator i=m.iterator(); i.hasNext();)
            {
              MensSignElement mse=(MensSignElement)i.next();
              switch (mse.signType)
                {
                  case MensSignElement.MENS_SIGN_O:
                  case MensSignElement.MENS_SIGN_C:
                  case MensSignElement.MENS_SIGN_CREV:
                    Element signel=new Element("Sign",cmmens);
                    signel.addContent(new Element("MainSymbol",cmmens).setText(
                      mse.signType==MensSignElement.MENS_SIGN_O ? "O" : "C"));
                    if (mse.signType==MensSignElement.MENS_SIGN_CREV)
                      signel.addContent(new Element("Orientation",cmmens).setText("Reversed"));
                    if (mse.stroke)
                      signel.addContent(new Element("Strokes",cmmens).setText("1"));
                    if (mse.dotted)
                      signel.addContent(new Element("Dot",cmmens));
                    curevel.addContent(signel);
                    break;
                  case MensSignElement.NUMBERS:
                    Element numel=addProportion(new Element("Number",cmmens),mse.number);
                    curevel.addContent(numel);
                    break;
                }
            }

          if (m.getStaffLoc()!=4)
            curevel.addContent(new Element("StaffLoc",cmmens).setText(Integer.toString(m.getStaffLoc())));
          if (m.vertical())
            curevel.addContent(new Element("Orientation",cmmens).setText("Vertical"));
          if (m.small())
            curevel.addContent(new Element("Small",cmmens));
          if (m.nonStandard())
            {
              Mensuration mi=m.getMensInfo();
              Element miel=new Element("MensInfo",cmmens);
              miel.addContent(new Element("Prolatio",cmmens).setText(
                mi.prolatio==Mensuration.MENS_TERNARY ? "3" : "2"));
              miel.addContent(new Element("Tempus",cmmens).setText(
                mi.tempus==Mensuration.MENS_TERNARY ? "3" : "2"));
              miel.addContent(new Element("ModusMinor",cmmens).setText(
                mi.modus_minor==Mensuration.MENS_TERNARY ? "3" : "2"));
              miel.addContent(new Element("ModusMaior",cmmens).setText(
                mi.modus_maior==Mensuration.MENS_TERNARY ? "3" : "2"));
              curevel.addContent(miel);
            }
          break;

        case Event.EVENT_REST:
          curevel=new Element("Rest",cmmens);
          RestEvent r=(RestEvent)cure;
          addNoteInfoData(curevel,r.getnotetype(),r.getLength());
          curevel.addContent(new Element("BottomStaffLine",cmmens).setText(
            Integer.toString(r.getbottomline())));
          curevel.addContent(new Element("NumSpaces",cmmens).setText(
            Integer.toString(r.getnumlines())));
//          if (r.isCoronata())
//            curevel.addContent(new Element("Coronata",cmmens));
          if (r.getSignum()!=null)
            curevel.addContent(addSignumData(new Element("Signum",cmmens),r.getSignum()));
          break;

        case Event.EVENT_NOTE:
          curevel=new Element("Note",cmmens);
          NoteEvent n=(NoteEvent)cure;
          addNoteInfoData(curevel,n.getnotetype(),n.getLength());
          addLocusData(curevel,n.getPitch());

          ModernAccidental ma=n.getPitchOffset();
          if (ma!=null)
            curevel.addContent(addModernAccidentalData(new Element("ModernAccidental",cmmens),ma));
          if (n.isligated())
            curevel.addContent(new Element("Lig",cmmens).setText(NoteEvent.LigTypeNames[n.getligtype()]));
          if (n.isflagged())
            curevel.addContent(new Element("Flagged",cmmens));

          Element stemel=null;
          if (n.getstemside()!=-1)
            {
              stemel=new Element("Stem",cmmens);
              stemel.addContent(new Element("Dir",cmmens).setText(NoteEvent.StemDirs[n.getstemdir()]));
              stemel.addContent(new Element("Side",cmmens).setText(NoteEvent.StemDirs[n.getstemside()]));
            }
          else if (n.getstemdir()!=NoteEvent.STEM_UP)
            {
              stemel=new Element("Stem",cmmens);
              stemel.addContent(new Element("Dir",cmmens).setText(NoteEvent.StemDirs[n.getstemdir()]));
            }
          if (stemel!=null)
            curevel.addContent(stemel);

//          if (n.isCoronata())
//            curevel.addContent(new Element("Coronata",cmmens));
          if (n.getSignum()!=null)
            curevel.addContent(addSignumData(new Element("Signum",cmmens),n.getSignum()));

          String mt=n.getModernText();
          if (mt!=null)
            {
              Element mtel=new Element("ModernText",cmmens);
              mtel.addContent(new Element("Syllable",cmmens).setText(mt));
              if (n.isWordEnd())
                mtel.addContent(new Element("WordEnd",cmmens));
              curevel.addContent(mtel);
            }
          break;

        case Event.EVENT_DOT:
          curevel=new Element("Dot",cmmens);
          curevel.addContent(
            addLocusData(new Element("Pitch",cmmens),((DotEvent)cure).getPitch()));
          break;

        case Event.EVENT_ORIGINALTEXT:
          curevel=new Element("OriginalText",cmmens);
          curevel.addContent(new Element("Phrase",cmmens).setText(
            ((OriginalTextEvent)cure).getText()));
          break;

        case Event.EVENT_PROPORTION:
          curevel=new Element("Proportion",cmmens);
          addProportion(curevel,((ProportionEvent)cure).getproportion());
          break;

        case Event.EVENT_COLORCHANGE:
          curevel=new Element("ColorChange",cmmens);
          Coloration newcol=((ColorChangeEvent)cure).getcolorscheme(),
                     coldiff=newcol.differencebetween(lastcol);

          Element colel=new Element("PrimaryColor",cmmens);
          if (coldiff.primaryColor!=Coloration.NONE ||
              coldiff.primaryFill==Coloration.NONE)
            colel.addContent(new Element("Color",cmmens).setText(
                               Coloration.ColorNames[newcol.primaryColor]));
          if (coldiff.primaryFill!=Coloration.NONE)
            colel.addContent(new Element("Fill",cmmens).setText(
                               Coloration.ColorFillNames[newcol.primaryFill]));
          curevel.addContent(colel);

          if (newcol.secondaryColor!=newcol.primaryColor ||
              (newcol.secondaryColor==newcol.primaryColor && newcol.secondaryFill==newcol.primaryFill))
            {
              colel=new Element("SecondaryColor",cmmens);
              if (newcol.secondaryColor!=newcol.primaryColor)
                colel.addContent(new Element("Color",cmmens).setText(Coloration.ColorNames[newcol.secondaryColor]));
              colel.addContent(new Element("Fill",cmmens).setText(Coloration.ColorFillNames[newcol.secondaryFill]));
              curevel.addContent(colel);
            }

          break;

        case Event.EVENT_CUSTOS:
          curevel=new Element("Custos",cmmens);

          /* need to add support for StaffLoc with no Locus */
          addLocusData(curevel,((CustosEvent)cure).getPitch());
          break;

        case Event.EVENT_LINEEND:
          curevel=new Element("LineEnd",cmmens);
          if (((LineEndEvent)cure).isPageEnd())
            curevel.addContent(new Element("PageEnd",cmmens));
          break;

        case Event.EVENT_BARLINE:
          curevel=new Element("MiscItem",cmmens);
          Element ble=new Element("Barline",cmmens);
          int     nl=((BarlineEvent)cure).getNumLines();
          if (nl!=1)
            ble.addContent(new Element("NumLines",cmmens).setText(Integer.toString(nl)));
          curevel.addContent(ble);
          break;

        case Event.EVENT_ANNOTATIONTEXT:
          curevel=new Element("MiscItem",cmmens);

          Element             tae=new Element("TextAnnotation",cmmens);
          AnnotationTextEvent ae=(AnnotationTextEvent)cure;
          int                 sl=ae.getstaffloc();

          tae.addContent(new Element("Text",cmmens).setText(ae.gettext()));
          if (sl!=AnnotationTextEvent.DEFAULT_STAFFLOC)
            tae.addContent(new Element("StaffLoc",cmmens).setText(Integer.toString(sl)));
          curevel.addContent(tae);
          break;

        case Event.EVENT_LACUNA:
          curevel=new Element("MiscItem",cmmens);

          Element     mile=new Element("Lacuna",cmmens);
          LacunaEvent le=(LacunaEvent)cure;

          mile.addContent(addProportion(new Element("Length",cmmens),le.getLength()));
          curevel.addContent(mile);
          break;

        case Event.EVENT_MODERNKEYSIGNATURE:
          curevel=new Element("ModernKeySignature",cmmens);

          ModernKeySignatureEvent mkse=(ModernKeySignatureEvent)cure;
          for (Iterator i=mkse.getSigInfo().iterator(); i.hasNext();)
            {
              ModernKeySignatureElement curSigEl=(ModernKeySignatureElement)i.next();
              Element sigElTree=new Element("SigElement",cmmens);
              sigElTree.addContent(new Element("Pitch",cmmens).setText(
                String.valueOf(curSigEl.pitch.noteletter)));
              if (curSigEl.pitch.octave!=-1)
                sigElTree.addContent(new Element("Octave",cmmens).setText(
                  String.valueOf(curSigEl.pitch.octave)));
              sigElTree.addContent(
                addModernAccidentalData(new Element("Accidental",cmmens),curSigEl.accidental));

              curevel.addContent(sigElTree);
            }
          break;

        case Event.EVENT_ELLIPSIS:
          curevel=new Element("MiscItem",cmmens);
          curevel.addContent(new Element("Ellipsis",cmmens));
          break;

        case Event.EVENT_SECTIONEND:
        case Event.EVENT_LACUNA_END:
          return null;

        default:
          curevel=new Element("Event",cmmens);
      }

    if (cure.isColored())
      curevel.addContent(new Element("Colored",cmmens));
    String ec=cure.getEdCommentary();
    if (ec!=null)
      curevel.addContent(new Element("EditorialCommentary",cmmens).setText(ec));

    lastcol=cure.getcoloration();
    return curevel;
  }

/*------------------------------------------------------------------------
Method:  Element addModernAccidentalData(Element e,ModernAccidental ma)
Purpose: Construct tree segment for modern accidental data and add to element
Parameters:
  Input:  ModernAccidental ma - accidental data
  Output: Element e           - target element
  Return: new element containing accidental data tree
------------------------------------------------------------------------*/

  static Element addModernAccidentalData(Element e,ModernAccidental ma)
  {
    e.addContent(new Element("AType",cmmens).setText(ModernAccidental.AccidentalNames[ma.accType]));
    if (ma.numAcc!=1)
      e.addContent(new Element("Num",cmmens).setText(Integer.toString(ma.numAcc)));
    if (ma.optional)
      e.addContent(new Element("Optional",cmmens));

    return e;
  }

/*------------------------------------------------------------------------
Method:  Element addColorationData(Element e,Coloration c)
Purpose: Construct tree segment for coloration data and add to element
Parameters:
  Input:  Coloration c - coloration data
  Output: Element e    - target element
  Return: modified element
------------------------------------------------------------------------*/

  static Element addColorationData(Element e,Coloration c)
  {
    Element colel=new Element("PrimaryColor",cmmens);
    colel.addContent(new Element("Color",cmmens).setText(Coloration.ColorNames[c.primaryColor]));
    colel.addContent(new Element("Fill",cmmens).setText(Coloration.ColorFillNames[c.primaryFill]));
    e.addContent(colel);

    colel=new Element("SecondaryColor",cmmens);
    colel.addContent(new Element("Color",cmmens).setText(Coloration.ColorNames[c.secondaryColor]));
    colel.addContent(new Element("Fill",cmmens).setText(Coloration.ColorFillNames[c.secondaryFill]));
    e.addContent(colel);

    return e;
  }

/*------------------------------------------------------------------------
Method:  Element addLocusData(Element e,Pitch p)
Purpose: Construct tree segment for Locus (pitch) data and add to element
Parameters:
  Input:  Pitch p   - pitch data
  Output: Element e - target element
  Return: modified element
------------------------------------------------------------------------*/

  static Element addLocusData(Element e,Pitch p)
  {
    e.addContent(new Element("LetterName",cmmens).setText(String.valueOf(p.noteletter)));
    e.addContent(new Element("OctaveNum",cmmens).setText(Integer.toString(p.octave)));

    return e;
  }

/*------------------------------------------------------------------------
Method:  Element addNumberSequence(Element e,int num)
Purpose: Construct tree segment for NumberSequence and add to element
Parameters:
  Input:  Element e - target element
          int num   - number data
  Output: -
  Return: modified element
------------------------------------------------------------------------*/

  static Element addNumberSequence(Element e,int num)
  {
    e.addContent(new Element("Num",cmmens).setText(Integer.toString(num)));

    return e;
  }

/*------------------------------------------------------------------------
Method:  Element addNoteInfoData(Element e,int nt,Proportion l)
Purpose: Construct tree segment for NoteInfoData and add to element
Parameters:
  Input:  Element e    - target element
          int nt       - note type
          Proportion l - note length
  Output: -
  Return: modified element
------------------------------------------------------------------------*/

  static Element addNoteInfoData(Element e,int nt,Proportion l)
  {
    e.addContent(new Element("Type",cmmens).setText(NoteEvent.NoteTypeNames[nt]));
    e.addContent(addProportion(new Element("Length",cmmens),l));

    return e;
  }

/*------------------------------------------------------------------------
Method:  Element addSignumData(Element e,Signum s)
Purpose: Construct tree segment for Signum and add to element
Parameters:
  Input:  Element e - target element
          Signum s  - signum data
  Output: -
  Return: modified element
------------------------------------------------------------------------*/

  static Element addSignumData(Element e,Signum s)
  {
    e.addContent(new Element("Offset",cmmens).setText(Integer.toString(s.offset)));
    if (s.orientation!=Signum.UP)
      e.addContent(new Element("Orientation",cmmens).setText(Signum.orientationNames[s.orientation]));
    if (s.side!=Signum.MIDDLE)
      e.addContent(new Element("Side",cmmens).setText(Signum.sideNames[s.side]));

    return e;
  }

/*------------------------------------------------------------------------
Method:  Element addProportion(Element e,Proportion p)
Purpose: Construct tree segment for Proportion and add to element
Parameters:
  Input:  Element e    - target element
          Proportion p - proportion data
  Output: -
  Return: modified element
------------------------------------------------------------------------*/

  static Element addProportion(Element e,Proportion p)
  {
    e.addContent(new Element("Num",cmmens).setText(Integer.toString(p.i1)));
    e.addContent(new Element("Den",cmmens).setText(Integer.toString(p.i2)));

    return e;
  }
}
