/*----------------------------------------------------------------------*/
/*

        Module          : CMMEParser.java

        Package         : DataStruct

        Classes Included: CMMEParser

        Purpose         : Input, parsing, and output of CMME music files
                          (replacement for package Parse)

        Programmer      : Ted Dumitrescu

        Date Started    : 2/23/05

Updates:
3/3/05:   XML output routine started
3/8/05:   initial XML output routine completed
3/30/06:  no longer keeps DOM Document in memory after parsing
7/20/06:  added support for 'editorial' event segments
7/24/06:  added page-end marker to LineEnd events
2/07:     converted format/internal representation to introduce "MusicSection"
          as high-level musical structure
6/21/07:  added calls to update progress bar while loading
10/12/07: added support for loading/saving Plainchant sections
11/17/07: added support for saving variant readings
11/19/07: added support for loading variant readings
12/23/07: added support for loading/saving tacet texts in reduced-scoring
          sections
6/14/08:  now automatically consolidates variant readings upon saving
          (PieceData.consolidateAllReadings())
12/30/09: parse/save text sections

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
import javax.swing.JProgressBar;

import DataStruct.XMLReader;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   CMMEParser
Extends: -
Purpose: Input, parsing, and output of CMME music files
------------------------------------------------------------------------*/

public class CMMEParser
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static Namespace cmmens;

/*----------------------------------------------------------------------*/
/* Instance variables */

  public PieceData piece;

  float fileVersion;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  String getFileVersion(URL remoteloc)
Purpose: Return version of CMME file
Parameters:
  Input:  URL remoteloc - URL for input
  Output: -
  Return: Version as specified in CMMEversion attribute, "0.5" for old files
          with no version information
------------------------------------------------------------------------*/

  public static String getFileVersion(URL remoteloc) throws JDOMException,IOException
  {
    /* wasteful! */
    Document  CMMEdoc=XMLReader.getNonValidatingParser().build(remoteloc);
    Attribute CMMEversion=CMMEdoc.getRootElement().getAttribute("CMMEversion");

    if (CMMEversion==null)
      return "0.5";
    else
      return CMMEversion.getValue();
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: CMMEParser(String fn)
Purpose:     Parse local file
Parameters:
  Input:  String fn - filename for input
  Output: -
------------------------------------------------------------------------*/

  public CMMEParser(String fn,JProgressBar pbar) throws JDOMException,IOException
  {
    constructPieceData(XMLReader.getparser().build(fn),pbar);
  }

  public CMMEParser(String fn) throws JDOMException,IOException
  {
    this(fn,null);
  }

/*------------------------------------------------------------------------
Constructor: CMMEParser(URL remoteloc)
Purpose:     Parse remote resource
Parameters:
  Input:  URL remoteloc - URL for input
  Output: -
------------------------------------------------------------------------*/

  public CMMEParser(URL remoteloc,JProgressBar pbar) throws JDOMException,IOException
  {
    constructPieceData(XMLReader.getparser().build(remoteloc),pbar);
  }

  public CMMEParser(URL remoteloc) throws JDOMException,IOException
  {
    this(remoteloc,null);
  }

  public CMMEParser(InputStream musIn,JProgressBar pbar) throws JDOMException,IOException
  {
    constructPieceData(XMLReader.getparser().build(musIn),pbar);
  }

/*------------------------------------------------------------------------
Method:  void constructPieceData(Document cmmedoc)
Purpose: Use parsed document to construct data structure for piece
Parameters:
  Input:  Document cmmedoc - DOM tree holding CMME data
  Output: -
  Return: -
------------------------------------------------------------------------*/

  /* parameters for use while parsing one voice */
  Event              clefinfoevent[],
                     mensinfoevent[];
  Coloration         curcolor[];
  ModernKeySignature curModKeySig[];
  Event              lastevent[];

  NoteEvent lastNoteEvent;

  void constructPieceData(Document cmmedoc,JProgressBar progressBar)
  {
    piece=new PieceData();
    cmmens=Namespace.getNamespace("http://www.cmme.org");
    fileVersion=0f;

    /* version */
    Attribute CMMEversion=cmmedoc.getRootElement().getAttribute("CMMEversion");
    if (CMMEversion==null)
      System.err.println("Deprecated document version (pre-.81)");
    else
      fileVersion=Float.parseFloat(CMMEversion.getValue());

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
    clefinfoevent=new Event[numvoices];
    mensinfoevent=new Event[numvoices];
    curcolor=new Coloration[numvoices];
    curModKeySig=new ModernKeySignature[numvoices];
    lastevent=new Event[numvoices];

    int vi=0;
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

        clefinfoevent[vi]=null;
        mensinfoevent[vi]=null;
        curcolor[vi]=piece.getBaseColoration();
        curModKeySig[vi]=ModernKeySignature.DEFAULT_SIG;
        lastevent[vi]=null;

        vl[vi++]=curv;
      }
    piece.setVoiceData(vl);

    /* Variant version declarations */
    int varVersionNum=0;
    if (fileVersion<0.895f)
      /* pre-0.895 files do not declare the "default" variant version;
         create one */
      piece.addVariantVersion(new VariantVersionData("Default",varVersionNum++));
    for (Object curObj : GDNode.getChildren("VariantVersion",cmmens))
      {
        Element curVVel=(Element)curObj;

        VariantVersionData vvd=new VariantVersionData(curVVel.getChildText("ID",cmmens),varVersionNum++);
        Element sourceEl=curVVel.getChild("Source",cmmens);
        if (sourceEl!=null)
          try { vvd.setSourceInfo(sourceEl.getChildText("Name",cmmens),
                                  Integer.parseInt(sourceEl.getChildText("ID",cmmens))); }
          catch(NumberFormatException nfe) { }
        vvd.setEditor(curVVel.getChildText("Editor",cmmens));
        vvd.setDescription(curVVel.getChildText("Description",cmmens));

        Element mvEl=curVVel.getChild("MissingVoices",cmmens);
        if (mvEl!=null)
          for (Object curMVObj : mvEl.getChildren("VoiceNum",cmmens))
            {
              Element curMVel=(Element)curMVObj;
              int vnum=1;
              try { vnum=Integer.parseInt(curMVel.getText()); }
              catch(NumberFormatException nfe) { }

              for (Voice v : piece.getVoiceData())
                if (v.getNum()==vnum)
                  vvd.setMissingVoice(v,true);
            }

        piece.addVariantVersion(vvd);
      }

    /* Music sections */
    List sectionList=cmmedoc.getRootElement().getChildren("MusicSection",cmmens);
    int  numSections=sectionList.size(),
         PBAdd=0;
    if (progressBar!=null)
      PBAdd=(progressBar.getMaximum()-progressBar.getValue())/numSections;
    for (Object curObj : sectionList)
      {
        Element      sectionEl=(Element)curObj;
        boolean      editorialSection=false;
        String       sectionSource=null;
        int          sectionSourceNum=0;
        MusicSection curSection=null;

        for (Object curSectionChildObj : sectionEl.getChildren())
          {
            Element curSectionChild=(Element)curSectionChildObj;
            String  childName=curSectionChild.getName();

            if (childName.equals("Editorial"))
              editorialSection=true;
            else if (childName.equals("PrincipalSource"))
              {
                sectionSource=curSectionChild.getChildText("Name",cmmens);
                sectionSourceNum=Integer.parseInt(curSectionChild.getChildText("ID",cmmens));
              }

            else if (childName.equals("MensuralMusic"))
              curSection=parseMensuralMusicSection(curSectionChild);
            else if (childName.equals("Plainchant"))
              curSection=parsePlainchantSection(curSectionChild);
            else if (childName.equals("Text"))
              curSection=parseTextSection(curSectionChild);
          }
        curSection.setEditorial(editorialSection);
        curSection.setPrincipalSource(sectionSource);
        curSection.setPrincipalSourceNum(sectionSourceNum);
//        curSection.setVersion(piece.getVariantVersions().get(0));
        piece.addSection(curSection);

        if (progressBar!=null)
          progressBar.setValue(progressBar.getValue()+PBAdd);
      }

    piece.recalcAllEventParams();
  }

/*------------------------------------------------------------------------
Method:  MusicMensuralSection parseMensuralMusicSection(Element curSectionEl)
Purpose: Create section of mensural music from document tree segment
Parameters:
  Input:  Element curSectionEl - tree segment representing mensural music section
  Output: -
  Return: mensural music section
------------------------------------------------------------------------*/

  MusicMensuralSection parseMensuralMusicSection(Element curSectionEl)
  {
    int                  numVoices=Integer.parseInt(curSectionEl.getChildText("NumVoices",cmmens)),
                         sectionVoiceNum=0;
    MusicMensuralSection curSection=new MusicMensuralSection(piece.getVoiceData().length,false,piece.getBaseColoration());

    if (curSectionEl.getChild("BaseColoration",cmmens)!=null)
      curSection.setBaseColoration(parseColoration(curSectionEl.getChild("BaseColoration",cmmens)));
    for (int vi=0; vi<curcolor.length; vi++)
      curcolor[vi]=curSection.getBaseColoration();

    parseTacetInstructions(curSectionEl,curSection);

    for (Object curObj : curSectionEl.getChildren("Voice",cmmens))
      {
        Element curVoiceEl=(Element)curObj;

        /* event list for one voice */
        int vnum=Integer.parseInt(curVoiceEl.getChildText("VoiceNum",cmmens))-1;
        lastNoteEvent=null;

        VoiceMensuralData curv=new VoiceMensuralData(piece.getVoiceData()[vnum],curSection);

        for (Object curMVObj : curVoiceEl.getChildren("MissingVersionID",cmmens))
          curv.addMissingVersion(piece.getVariantVersion(((Element)curMVObj).getText()));

        for (Object curEvObj : curVoiceEl.getChild("EventList",cmmens).getChildren())
          {
            /* create structure for current event depending on type */
            Element cureventel=(Element)curEvObj;
            String eventtype=cureventel.getName();

            if (eventtype.equals("VariantReadings"))
              addVariantReadings(vnum,curv,cureventel);
            else if (eventtype.equals("EditorialData"))
              addEditorialEvents(vnum,curv,cureventel);
            else
              addSingleOrMultiEvent(curv,cureventel);
          }
        /* add SectionEnd event at end of each voice */
        addNewEvent(vnum,curv,new Event(Event.EVENT_SECTIONEND));

        /* set voice data in section */
        curSection.setVoice(vnum,curv);
      }

    return curSection;
  }

/*------------------------------------------------------------------------
Method:  MusicChantSection parsePlainchantSection(Element curSectionEl)
Purpose: Create section of plainchant from document tree segment
Parameters:
  Input:  Element curSectionEl - tree segment representing plainchant section
  Output: -
  Return: plainchant section
------------------------------------------------------------------------*/

  MusicChantSection parsePlainchantSection(Element curSectionEl)
  {
    int               numVoices=Integer.parseInt(curSectionEl.getChildText("NumVoices",cmmens));
    MusicChantSection curSection=new MusicChantSection(piece.getVoiceData().length,false,Coloration.DEFAULT_CHANT_COLORATION);

    if (curSectionEl.getChild("BaseColoration",cmmens)!=null)
      curSection.setBaseColoration(parseColoration(curSectionEl.getChild("BaseColoration",cmmens)));
    for (int vi=0; vi<curcolor.length; vi++)
      curcolor[vi]=curSection.getBaseColoration();

    parseTacetInstructions(curSectionEl,curSection);

    for (Object curObj : curSectionEl.getChildren("Voice",cmmens))
      {
        Element curVoiceEl=(Element)curObj;

        /* event list for one voice */
        int vnum=Integer.parseInt(curVoiceEl.getChildText("VoiceNum",cmmens))-1;
        lastNoteEvent=null;

        VoiceChantData curv=new VoiceChantData(piece.getVoiceData()[vnum],curSection);
        for (Object curMVObj : curVoiceEl.getChildren("MissingVersionID",cmmens))
          curv.addMissingVersion(piece.getVariantVersion(((Element)curMVObj).getText()));

        for (Object curEvObj : curVoiceEl.getChild("EventList",cmmens).getChildren())
          {
            /* create structure for current event depending on type */
            Element cureventel=(Element)curEvObj;
            String eventtype=cureventel.getName();

            if (eventtype.equals("VariantReadings"))
              addVariantReadings(vnum,curv,cureventel);
            else if (eventtype.equals("EditorialData"))
              addEditorialEvents(vnum,curv,cureventel);
            else
              addSingleOrMultiEvent(curv,cureventel);
          }
        /* add SectionEnd event at end of each voice */
        addNewEvent(vnum,curv,new Event(Event.EVENT_SECTIONEND));

        /* set voice data in section */
        curSection.setVoice(vnum,curv);
      }

    return curSection;
  }

/*------------------------------------------------------------------------
Method:  void parseTacetInstructions(Element curSectionEl,MusicSection curSection)
Purpose: Parse list of tacet instructions and add to section
Parameters:
  Input:  Element curSectionEl - tree segment representing section
  Output: MusicSection curSection - section being created
  Return: -
------------------------------------------------------------------------*/

  void parseTacetInstructions(Element curSectionEl,MusicSection curSection)
  {
    for (Object curObj : curSectionEl.getChildren("TacetInstruction",cmmens))
      {
        Element curTacetEl=(Element)curObj;

        curSection.setTacetText(
          Integer.parseInt(curTacetEl.getChildText("VoiceNum",cmmens))-1,
          curTacetEl.getChildText("TacetText",cmmens));
      }
  }

/*------------------------------------------------------------------------
Method:  MusicTextSection parseTextSection(Element curSectionEl)
Purpose: Create text section from document tree segment
Parameters:
  Input:  Element curSectionEl - tree segment representing section
  Output: -
  Return: text section
------------------------------------------------------------------------*/

  MusicTextSection parseTextSection(Element curSectionEl)
  {
    String sectionText=curSectionEl.getChildText("Content",cmmens);
    MusicTextSection curSection=new MusicTextSection(sectionText);

    return curSection;
  }

/*------------------------------------------------------------------------
Method:  void addVariantReadings(int vnum,VoiceEventListData v,Element varRootEl)
Purpose: Parse segment of variant readings and add to voice data
Parameters:
  Input:  int vnum             - voice number
          Element varRootEl    - root variant node to parse
  Output: VoiceEventListData v - voice being constructed
  Return: -
------------------------------------------------------------------------*/

  boolean parsingVariant=false;

  void addVariantReadings(int vnum,VoiceEventListData v,Element varRootEl)
  {
    int varStarti=v.getNumEvents();

    VariantMarkerEvent vd1=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_START);
    addNewEvent(vnum,v,vd1);
    lastevent[vnum]=vd1;

    for (Object reObj : varRootEl.getChildren("Reading",cmmens))
      {
        Element            curReadingEl=(Element)reObj;
        VariantReading     curReading=null;
        List               versionIDList=curReadingEl.getChildren("VariantVersionID",cmmens);
        VoiceEventListData evList=null;
        int                readingVnum=vnum;

        if (versionIDList.size()==1 &&
            "DEFAULT".equals(curReadingEl.getChildText("VariantVersionID",cmmens)))
          evList=v;
        else if (versionIDList.size()>0)
          {
            parsingVariant=true;
            curReading=new VariantReading();
            for (Object vIDe : versionIDList)
              curReading.addVersion(piece.getVariantVersion(((Element)vIDe).getText()));
            if (curReadingEl.getChild("Error",cmmens)!=null)
              curReading.setError(true);

//            readingVnum=-1;
            if (v instanceof VoiceMensuralData)
              evList=new VoiceMensuralData();
            else if (v instanceof VoiceChantData)
              evList=new VoiceChantData();
          }

        addEventList(readingVnum,evList,curReadingEl.getChild("Music",cmmens));
        if (curReading!=null)
          {
            curReading.addEventList(evList);
            vd1.addReading(curReading);
          }

        parsingVariant=false;
      }

    VariantMarkerEvent vd2=new VariantMarkerEvent(Event.EVENT_VARIANTDATA_END,vd1.getReadings());
    addNewEvent(vnum,v,vd2);
    lastevent[vnum]=vd2;

    vd1.calcVariantTypes(v);
    vd2.setVarTypeFlags(vd1.getVarTypeFlags());
  }

/*------------------------------------------------------------------------
Method:  void addEventList(int vnum,VoiceEventListData v,Element elistEl)
Purpose: Parse event list for one default or variant reading
Parameters:
  Input:  int vnum             - voice number for event list (-1 for variant)
          Element elistEl      - root node of event list tree
  Output: VoiceEventListData v - list being constructed
  Return: -
------------------------------------------------------------------------*/

  void addEventList(int vnum,VoiceEventListData v,Element elistEl)
  {
    for (Iterator ei=elistEl.getChildren().iterator(); ei.hasNext();)
      {
        addSingleOrMultiEvent(vnum,v,(Element)ei.next());
/*        if (vnum!=-1)
          lastevent[vnum].setEditorial(true);*/
      }
  }

/*------------------------------------------------------------------------
Method:  void addEditorialEvents(int vnum,VoiceEventListData v,Element cureventel)
Purpose: Parse one editorial event list (for backwards compatibility) and
         add to event list of a given voice
Parameters:
  Input:  int vnum             - voice number (-1 for variant)
          Element cureventel   - event node to parse
  Output: VoiceEventListData v - voice being constructed
  Return: -
------------------------------------------------------------------------*/

  void addEditorialEvents(int vnum,VoiceEventListData v,Element cureventel)
  {
    Event ed=new AnnotationTextEvent("ED DATA START");
    addNewEvent(vnum,v,ed);
    lastevent[vnum]=ed;
    for (Iterator edei=cureventel.getChild("NewReading",cmmens).getChildren().iterator();
         edei.hasNext();)
      addSingleOrMultiEvent(v,(Element)edei.next());
    addNewEvent(vnum,v,ed=new AnnotationTextEvent("ED DATA END"));
    lastevent[vnum]=ed;
  }

/*------------------------------------------------------------------------
Method:  void addNewEvent(int vnum,VoiceEventListData v,Event e)
Purpose: Add one parsed event to event list of a given voice
Parameters:
  Input:  int vnum             - voice number (-1 for variant)
          Event e              - parsed event
  Output: VoiceEventListData v - voice being constructed
  Return: -
------------------------------------------------------------------------*/

  void addNewEvent(int vnum,VoiceEventListData v,Event e)
  {
    if (vnum!=-1)
      {
        e.setclefparams(clefinfoevent[vnum]);
        e.setmensparams(mensinfoevent[vnum]);
        e.setcolorparams(curcolor[vnum]);
        e.setModernKeySigParams(curModKeySig[vnum]);
      }
    v.addEvent(e);
  }

/*------------------------------------------------------------------------
Method:  void addSingleOrMultiEvent(VoiceEventListData v,Element cureventel)
Purpose: Parse one event and add to event list of a given voice
Parameters:
  Input:  Element cureventel   - event node to parse
  Output: VoiceEventListData v - voice being constructed
  Return: -
------------------------------------------------------------------------*/

  void addSingleOrMultiEvent(VoiceEventListData v,Element cureventel)
  {
    addSingleOrMultiEvent(v.getMetaData().getNum()-1,v,cureventel);
  }

  void addSingleOrMultiEvent(int vnum,VoiceEventListData v,Element cureventel)
  {
    String eventtype=cureventel.getName();
    Event  curevent;

    if (eventtype.equals("MultiEvent"))
      curevent=parseMultiEvent(vnum,cureventel);
    else
      curevent=parseSingleEvent(vnum,cureventel);

    /* add event to list */
    addNewEvent(vnum,v,curevent);

    /* add data-free marker for end of lacunae 
    if (curevent.geteventtype()==Event.EVENT_LACUNA)
      addNewEvent(vnum,v,curevent=new Event(Event.EVENT_LACUNA_END));*/

    if (vnum!=-1)
      lastevent[vnum]=curevent;
  }

/*------------------------------------------------------------------------
Method:  MultiEvent parseMultiEvent(int vnum,Element cureventel)
Purpose: Create multi-event (list of simultaneous events) from document
         tree segment
Parameters:
  Input:  int vnum           - voice number
          Element cureventel - multi-event node
  Output: -
  Return: multi-event
------------------------------------------------------------------------*/

  MultiEvent parseMultiEvent(int vnum,Element cureventel)
  {
    MultiEvent curevent=new MultiEvent();
    Event      curclefevent=clefinfoevent[vnum];

    for (Iterator ei=cureventel.getChildren().iterator(); ei.hasNext();)
      {
        Event e=parseSingleEvent(vnum,(Element)ei.next());
        curevent.addEvent(e);
        if (vnum!=-1 && mensinfoevent[vnum]==e)
          mensinfoevent[vnum]=curevent;
      }

    if (curevent.hasSignatureClef())
      {
        curevent.constructClefSets(lastevent[vnum],curclefevent);
        checkClefInfoEvent(vnum,curevent);
      }

    return curevent;
  }

/*------------------------------------------------------------------------
Method:  Event parseSingleEvent(int vnum,Element cureventel)
Purpose: Create single event from document tree segment
Parameters:
  Input:  int vnum           - voice number
          Element cureventel - event node
  Output: -
  Return: event
------------------------------------------------------------------------*/

  Event parseSingleEvent(int vnum,Element cureventel)
  {
    Event  curevent;
    String eventtype=cureventel.getName();

    if (eventtype.equals("Clef"))
      {
        curevent=parseClefEvent(vnum,cureventel,lastevent[vnum]);
        checkClefInfoEvent(vnum,curevent);
      }
    else if (eventtype.equals("Mensuration"))
      {
        curevent=parseMensurationEvent(cureventel);
        if (vnum!=-1)
          mensinfoevent[vnum]=curevent;
      }
    else if (eventtype.equals("Rest"))
      curevent=parseRestEvent(cureventel,mensinfoevent[vnum]);
    else if (eventtype.equals("Note"))
      curevent=parseNoteEvent(cureventel,clefinfoevent[vnum],curModKeySig[vnum]);
    else if (eventtype.equals("Dot"))
      curevent=parseDotEvent(cureventel,mensinfoevent[vnum],clefinfoevent[vnum]);
    else if (eventtype.equals("OriginalText"))
      curevent=parseOriginalTextEvent(cureventel);
    else if (eventtype.equals("Proportion"))
      curevent=parseProportionEvent(cureventel);
    else if (eventtype.equals("ColorChange"))
      {
        curevent=parseColorChangeEvent(cureventel,curcolor[vnum]);
        if (vnum!=-1)
          curcolor[vnum]=((ColorChangeEvent)curevent).getcolorscheme();
      }
    else if (eventtype.equals("Custos"))
      curevent=parseCustosEvent(cureventel,clefinfoevent[vnum]);
    else if (eventtype.equals("LineEnd"))
      curevent=parseLineEndEvent(cureventel);
    else if (eventtype.equals("MiscItem"))
      curevent=parseMiscItemEvent(cureventel);

    else if (eventtype.equals("ModernKeySignature"))
      {
        curevent=parseModernKeySignatureEvent(cureventel);
        if (vnum!=-1)
          curModKeySig[vnum]=((ModernKeySignatureEvent)curevent).getSigInfo();
      }

    else
{
System.err.println("Unknown event type: "+eventtype);
      curevent=new Event();
}

    if (vnum!=-1)
      {
        curevent.setclefparams(clefinfoevent[vnum]);
        curevent.setmensparams(mensinfoevent[vnum]);
        curevent.setcolorparams(curcolor[vnum]);
        curevent.setModernKeySigParams(curModKeySig[vnum]);
      }

    /* generic event attributes */
    if (cureventel.getChild("Colored",cmmens)!=null)
      curevent.setColored(true);
    if (cureventel.getChild("Editorial",cmmens)!=null)
      curevent.setEditorial(true);
    if (cureventel.getChild("Error",cmmens)!=null)
      curevent.setError(true);
    curevent.setEdCommentary(cureventel.getChildText("EditorialCommentary",cmmens));

    return curevent;
  }

/*------------------------------------------------------------------------
Method:  void checkClefInfoEvent(int vnum,Event e)
Purpose: Check whether an event has clef/signature info, and set voice
         params if necessary
Parameters:
  Input:  int vnum - voice number
          Event e  - event to check
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void checkClefInfoEvent(int vnum,Event e)
  {
    if (e.hasSignatureClef() &&
        vnum!=-1 && !parsingVariant &&
        (clefinfoevent[vnum]==null || e.getClefSet()!=clefinfoevent[vnum].getClefSet()))
      {
        clefinfoevent[vnum]=e;
        curModKeySig[vnum]=e.getClefSet().getKeySig();
      }
  }

/*------------------------------------------------------------------------
Method:  Event parseClefEvent(int vnum,Element e,Event le)
Purpose: Create clef event from document tree segment
Parameters:
  Input:  int vnum  - voice number
          Element e - "Clef" node
          Event le  - last event parsed
  Output: -
  Return: clef event
------------------------------------------------------------------------*/

  Event parseClefEvent(int vnum,Element e,Event le)
  {
    return new ClefEvent(
      e.getChildText("Appearance",cmmens),
      Integer.parseInt(e.getChildText("StaffLoc",cmmens)),
      new Pitch(
        e.getChild("Pitch",cmmens).getChildText("LetterName",cmmens).charAt(0),
        Integer.parseInt(e.getChild("Pitch",cmmens).getChildText("OctaveNum",cmmens))),
      le,vnum>=0 ? clefinfoevent[vnum] : null,
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
    boolean                     vertical=false,small=false,noScoreSig=false;

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
            Integer.parseInt(curmensel.getChildText("ModusMaior",cmmens)),
            curmensel.getChild("TempoChange",cmmens)==null ?
              Mensuration.DEFAULT_TEMPO_CHANGE :
              parseProportion(curmensel.getChild("TempoChange",cmmens)));

        else if (curmensel.getName().equals("NoScoreEffect"))
          noScoreSig=true;
      }

    return new MensEvent(signs,ssnum,small,vertical,mensInfo,noScoreSig);
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
    re.setCorona(parseSignum(e.getChild("Corona",cmmens)));
    re.setSignum(parseSignum(e.getChild("Signum",cmmens)));

    return re;
  }

/*------------------------------------------------------------------------
Method:  Event parseNoteEvent(Element e,Event ce,ModernKeySignature keySig)
Purpose: Create note event from document tree segment
Parameters:
  Input:  Element e                 - "Note" node
          Event ce                  - current clef event
          ModernKeySignature keySig - current modern key signature
  Output: -
  Return: note event
------------------------------------------------------------------------*/

  Event parseNoteEvent(Element e,Event ce,ModernKeySignature keySig)
  {
    ModernAccidental pitchOffset;
    int              ligstatus=NoteEvent.LIG_NONE,
                     tieType=NoteEvent.TIE_NONE;
    Element          opte;
    boolean          col=false,wordEnd=false,modernTextEditorial=false;
    int              numFlags=0,
                     stemdir=NoteEvent.STEM_NONE,stemside=NoteEvent.STEM_NONE,
                     halfCol=NoteEvent.HALFCOLORATION_NONE;
    String           modernText=null;

    Pitch pitch=new Pitch(e.getChildText("LetterName",cmmens).charAt(0),
                Integer.parseInt(e.getChildText("OctaveNum",cmmens)),
                ce!=null ? ce.getPrincipalClef(false) : null);
    pitchOffset=parseModernAccidental(e,pitch,keySig);

    if ((opte=e.getChild("Lig",cmmens))!=null)
      if (opte.getText().equals("Recta"))
        ligstatus=NoteEvent.LIG_RECTA;
      else if (opte.getText().equals("Obliqua"))
        ligstatus=NoteEvent.LIG_OBLIQUA;

    if ((opte=e.getChild("Tie",cmmens))!=null)
      if (opte.getText().equals("Under"))
        tieType=NoteEvent.TIE_UNDER;
      else
        tieType=NoteEvent.TIE_OVER;

    if (e.getChild("Colored",cmmens)!=null)
      col=true;
    if ((opte=e.getChild("Flagged",cmmens))!=null)
      numFlags=((opte=opte.getChild("NumFlags",cmmens))==null) ?
        1 : Integer.parseInt(opte.getText());

    if ((opte=e.getChild("Stem",cmmens))!=null)
      {
        stemdir=NoteEvent.strtoStemDir(opte.getChildText("Dir",cmmens));
        String ss=opte.getChildText("Side",cmmens);
        if (ss!=null)
          stemside=NoteEvent.strtoStemDir(ss);
      }

    String halfColType=e.getChildText("HalfColoration",cmmens);
    if (halfColType!=null)
      if (halfColType.equals("PrimarySecondary"))
        halfCol=NoteEvent.HALFCOLORATION_PRIMARYSECONDARY;
      else if (halfColType.equals("SecondaryPrimary"))
        halfCol=NoteEvent.HALFCOLORATION_SECONDARYPRIMARY;

    if ((opte=e.getChild("ModernText",cmmens))!=null)
      {
        modernText=opte.getChildText("Syllable",cmmens);
        if (opte.getChild("WordEnd",cmmens)!=null)
          wordEnd=true;
        if (opte.getChild("Editorial",cmmens)!=null)
          modernTextEditorial=true;
      }

    NoteEvent ne=new NoteEvent(
      e.getChildText("Type",cmmens),
      parseProportion(e.getChild("Length",cmmens)),
      pitch,pitchOffset,
      ligstatus,col,halfCol,stemdir,stemside,
      numFlags,modernText,wordEnd,modernTextEditorial,tieType);
    ne.setCorona(parseSignum(e.getChild("Corona",cmmens)));
    ne.setSignum(parseSignum(e.getChild("Signum",cmmens)));

    lastNoteEvent=ne;

    return ne;
  }

/*------------------------------------------------------------------------
Method:  Event parseDotEvent(Element e,Event me)
Purpose: Create dot event from document tree segment
Parameters:
  Input:  Element e - "Dot" node
          Event me  - current mensuration event
          Event ce  - current clef event
  Output: -
  Return: dot event
------------------------------------------------------------------------*/

  Event parseDotEvent(Element e,Event me,Event ce)
  {
    Mensuration mens=me==null ? Mensuration.DEFAULT_MENSURATION : me.getMensInfo();
    NoteEvent   lne=lastNoteEvent;
    if (lne!=null && lne.getmusictime()!=null &&
        mens.ternary(lne.getnotetype()) &&
        lne.getmusictime().greaterThan(NoteEvent.getTypeLength(lne.getnotetype(),mens)))
      lne=null;

    Element  sle=e.getChild("Pitch",cmmens);
    Pitch    p=null;
    int      sl=0;
    DotEvent de;

    if (sle!=null)
      de=new DotEvent(new Pitch(sle.getChildText("LetterName",cmmens).charAt(0),
                                Integer.parseInt(sle.getChildText("OctaveNum",cmmens)),
                                ce!=null ? ce.getPrincipalClef(false) : null),lne);
    else
      de=new DotEvent(
        oldDotSLToPitch(Integer.parseInt(e.getChildText("StaffLoc",cmmens)),ce),lne);

/* IMPLEMENT: relative staff location */

    lastNoteEvent=null;
    return de;
  }

  /* convert deprecated dot staff location (int) to Pitch */
  Pitch oldDotSLToPitch(int sl,Event ce)
  {
    int   staffLoc=sl*2-1;
    Pitch p=new Pitch(staffLoc);
    if (ce!=null)
      p.setclef(ce.getPrincipalClef(false));

    return p;
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
        Element attribEl=me.getChild("NumLines",cmmens);
        int numLines=attribEl==null ? 1 : Integer.parseInt(attribEl.getText());

        boolean repeatSign=me.getChild("RepeatSign",cmmens)!=null;

        attribEl=me.getChild("BottomStaffLine",cmmens);
        int bottomLinePos=attribEl==null ? 0 : Integer.parseInt(attribEl.getText());

        attribEl=me.getChild("NumSpaces",cmmens);
        int numSpaces=attribEl==null ? 4 : Integer.parseInt(attribEl.getText());

        return new BarlineEvent(numLines,repeatSign,bottomLinePos,numSpaces);
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
      return parseLacunaEvent(me);

    if ((me=e.getChild("Ellipsis",cmmens))!=null)
      return new Event(Event.EVENT_ELLIPSIS);

    return null;
  }

/*------------------------------------------------------------------------
Method:  Event parseLacunaEvent(Element e)
Purpose: Create lacuna event from document tree segment
Parameters:
  Input:  Element e - "Lacuna" node
  Output: -
  Return: event
------------------------------------------------------------------------*/

  Event parseLacunaEvent(Element e)
  {
    Element lengthEl=e.getChild("Length",cmmens);
    if (lengthEl!=null)
      return new LacunaEvent(parseProportion(lengthEl));
    if (e.getChild("Begin",cmmens)!=null)
      return new LacunaEvent(Event.EVENT_LACUNA);
    if (e.getChild("End",cmmens)!=null)
      return new LacunaEvent(Event.EVENT_LACUNA_END);

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
Method:  ModernAccidental parseModernAccidental(Element e,Pitch p,ModernKeySignature keySig)
Purpose: Choose pitch offset for NoteEvent based on document tree segment
Parameters:
  Input:  Element e                 - note element node
          Pitch p                   - note pitch
          ModernKeySignature keySig - current key signature
  Output: -
  Return: ModernAccidental structure
------------------------------------------------------------------------*/

  /* note accidental */
  ModernAccidental parseModernAccidental(Element e,Pitch p,ModernKeySignature keySig)
  {
    int     pitchOffset=0;
    boolean optional=false;
    Element acce;

    if ((acce=e.getChild("ModernAccidental",cmmens))!=null)
      {
        if (acce.getChild("AType",cmmens)!=null)
          {
            /* parse deprecated ModernAccidental structure */
            int numa=(acce.getChild("Num",cmmens)!=null) ?
                       Integer.parseInt(acce.getChildText("Num",cmmens)) : 1;
            ModernAccidental depAcc=new ModernAccidental(
              ModernAccidental.strtoMA(acce.getChildText("AType",cmmens)),numa,false);

            pitchOffset=keySig.calcNotePitchOffset(p,depAcc);
          }
        else
          pitchOffset=Integer.parseInt(acce.getChildText("PitchOffset",cmmens));

        optional=acce.getChild("Optional",cmmens)!=null;
      }
    /* legacy code: pre-0.894 files have no pitch offset info for notes
       modified by key signature */
    else if (fileVersion<0.894f)
      pitchOffset=keySig.calcNotePitchOffset(p,null);

    return new ModernAccidental(pitchOffset,optional);
  }

  /* signature accidental */
  ModernAccidental parseModernAccidental(Element acce)
  {
    int numa=(acce.getChild("Num",cmmens)!=null) ?
               Integer.parseInt(acce.getChildText("Num",cmmens)) : 1;
    return new ModernAccidental(
      ModernAccidental.strtoMA(acce.getChildText("AType",cmmens)),numa,
      acce.getChild("Optional",cmmens)!=null);
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

    int orientation=Signum.UP,
        side=Signum.MIDDLE;

    String offsetStr=e.getChildText("Offset",cmmens),
           os=e.getChildText("Orientation",cmmens);
    if (os!=null && os.equals("Down"))
      orientation=Signum.DOWN;
    String ss=e.getChildText("Side",cmmens);
    if (ss!=null)
      if (ss.equals("Left"))
        side=Signum.LEFT;
      else if (ss.equals("Right"))
        side=Signum.RIGHT;

    if (offsetStr==null)
      return new Signum(orientation,side);
    return new Signum(Integer.parseInt(offsetStr),orientation,side);
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
    return e==null ? null :
             new Proportion(Integer.parseInt(e.getChildText("Num",cmmens)),
                            Integer.parseInt(e.getChildText("Den",cmmens)));
  }


/*----------------------------------------------------------------------*/
/*--------------------------- OUTPUT METHODS ---------------------------*/
/*----------------------------------------------------------------------*/


/*------------------------------------------------------------------------
Method:  void outputPieceData(PieceData pdata,OutputStream outs)
Purpose: Output CMME format file from PieceData structure
Parameters:
  Input:  PieceData pdata   - structure containing music
          OutputStream outs - output destination
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static PieceData mainData;

  public static void outputPieceData(PieceData pdata,OutputStream outs)
  {
    Document outputdoc;
    Element  rootel;

    mainData=pdata;
    cmmens=Namespace.getNamespace("http://www.cmme.org");
    Namespace xsins=Namespace.getNamespace("xsi","http://www.w3.org/2001/XMLSchema-instance");

    /* header/root */
    rootel=new Element("Piece",cmmens);
    rootel.addNamespaceDeclaration(cmmens);
    rootel.addNamespaceDeclaration(xsins);
    rootel.setAttribute("schemaLocation","http://www.cmme.org cmme.xsd",xsins);
    rootel.setAttribute("CMMEversion",MetaData.CMME_VERSION);//,cmmens);

    /* actual content */
    pdata.consolidateAllReadings();
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
    String publicNotes=pdata.getPublicNotes();
    if (publicNotes!=null && publicNotes.length()>0)
      gdel.addContent(new Element("PublicNotes",cmmens).setText(publicNotes));
    String notes=pdata.getNotes();
    if (notes!=null && notes.length()>0)
      gdel.addContent(new Element("Notes",cmmens).setText(notes));

    /* variant version declarations */
    int vvi=0;
    for (VariantVersionData vvd : pdata.getVariantVersions())
      {
        Element vvel=new Element("VariantVersion",cmmens);
        if (vvi++==0)
          vvel.addContent(new Element("Default",cmmens));
        vvel.addContent(new Element("ID",cmmens).setText(vvd.getID()));
        int sourceID=vvd.getSourceID();
        if (sourceID>-1)
          vvel.addContent(createSourceInfoTree("Source",vvd.getSourceName(),sourceID));
        String vEditor=vvd.getEditor();
        if (vEditor!=null && vEditor.length()>0)
          vvel.addContent(new Element("Editor",cmmens).setText(vEditor));
        String vDescription=vvd.getDescription();
        if (vDescription!=null && vDescription.length()>0)
          vvel.addContent(new Element("Description",cmmens).setText(vDescription));
        ArrayList<Voice> missingVoices=vvd.getMissingVoices();
        if (missingVoices.size()>0)
          {
            Element mvel=new Element("MissingVoices",cmmens);
            for (Voice v : missingVoices)
              mvel.addContent(new Element("VoiceNum",cmmens).setText(String.valueOf(v.getNum())));
            vvel.addContent(mvel);
          }

        gdel.addContent(vvel);
      }

    if (!pdata.getBaseColoration().equals(Coloration.DEFAULT_COLORATION))
      gdel.addContent(addColorationData(new Element("BaseColoration",cmmens),pdata.getBaseColoration()));
    return gdel;
  }


/*------------------------------------------------------------------------
Method:  Element createSourceInfoTree(String elName,String sName,int sNum)
Purpose: Construct tree segment for one source info
Parameters:
  Input:  String elName - name of element to be created
          String sName  - name of source
          int sNum      - source ID no.
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createSourceInfoTree(String elName,String sName,int sNum)
  {
    Element sourceEl=new Element(elName,cmmens);
    sourceEl.addContent(new Element("Name",cmmens).setText(sName));
    sourceEl.addContent(new Element("ID",cmmens).setText(String.valueOf(sNum)));
    return sourceEl;
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
    String msSource=ms.getPrincipalSource();
    if (msSource!=null && !msSource.equals(""))
      msel.addContent(createSourceInfoTree("PrincipalSource",msSource,ms.getPrincipalSourceNum()));

    switch (ms.getSectionType())
      {
        case MusicSection.MENSURAL_MUSIC:
          msel.addContent(createMusicMensuralSectionTree((MusicMensuralSection)ms));
          break;
        case MusicSection.PLAINCHANT:
          msel.addContent(createMusicChantSectionTree((MusicChantSection)ms));
          break;
        default:
          System.err.println("Save error: Attempting to create tree for unsupported section type");
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
    mmsel.addContent(new Element("NumVoices",cmmens).setText(Integer.toString(mms.getNumVoicesUsed())));
    if (mms.getBaseColoration()!=null && !mms.getBaseColoration().equals(mainData.getBaseColoration()))
      mmsel.addContent(addColorationData(new Element("BaseColoration",cmmens),mms.getBaseColoration()));

    addTacetInstructions(mmsel,mms);

    for (int vi=0; vi<mms.getNumVoices(); vi++)
      if (mms.getVoice(vi)!=null)
        {
          VoiceMensuralData curvmd=(VoiceMensuralData)mms.getVoice(vi);

          Element curvel=new Element("Voice",cmmens);
          curvel.addContent(new Element("VoiceNum",cmmens).setText(Integer.toString(curvmd.getVoiceNum())));
          for (VariantVersionData vvd : curvmd.getMissingVersions())
            curvel.addContent(new Element("MissingVersionID",cmmens).setText(vvd.getID()));
          curvel.addContent(createVoiceEventTree(curvmd));

          mmsel.addContent(curvel);
        }

    return mmsel;
  }

/*------------------------------------------------------------------------
Method:  Element createMusicChantSectionTree(MusicChantSection mcs)
Purpose: Construct tree segment for one MusicChantSection
Parameters:
  Input:  MusicChantSection mcs - plainchant section
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createMusicChantSectionTree(MusicChantSection mcs)
  {
    Element mcsel=new Element("Plainchant",cmmens);
    mcsel.addContent(new Element("NumVoices",cmmens).setText(Integer.toString(mcs.getNumVoicesUsed())));
    if (mcs.getBaseColoration()!=null && !mcs.getBaseColoration().equals(Coloration.DEFAULT_CHANT_COLORATION))
      mcsel.addContent(addColorationData(new Element("BaseColoration",cmmens),mcs.getBaseColoration()));

    addTacetInstructions(mcsel,mcs);

    for (int vi=0; vi<mcs.getNumVoices(); vi++)
      if (mcs.getVoice(vi)!=null)
        {
          VoiceEventListData curvd=mcs.getVoice(vi);

          Element curvel=new Element("Voice",cmmens);
          curvel.addContent(new Element("VoiceNum",cmmens).setText(Integer.toString(curvd.getVoiceNum())));
          for (VariantVersionData vvd : curvd.getMissingVersions())
            curvel.addContent(new Element("MissingVersionID",cmmens).setText(vvd.getID()));
          curvel.addContent(createVoiceEventTree(curvd));

          mcsel.addContent(curvel);
        }

    return mcsel;
  }

/*------------------------------------------------------------------------
Method:  void addTacetInstructions(Element sectionEl,MusicSection section)
Purpose: Add tree segments for voice tacet texts in one MusicSection
Parameters:
  Input:  MusicSection section - music section
  Output: Element sectionEl    - element representing music section
  Return: -
------------------------------------------------------------------------*/

  static void addTacetInstructions(Element sectionEl,MusicSection section)
  {
    if (section.getTacetInfo()==null)
      return;

    for (TacetInfo ti : section.getTacetInfo())
      if (!ti.tacetText.equals(""))
        {
          Element tacetEl=new Element("TacetInstruction",cmmens);
          tacetEl.addContent(new Element("VoiceNum",cmmens).setText(Integer.toString(ti.voiceNum+1)));
          tacetEl.addContent(new Element("TacetText",cmmens).setText(ti.tacetText));

          sectionEl.addContent(tacetEl);
        }
  }

/*------------------------------------------------------------------------
Method:  Element createVoiceEventTree(VoiceEventListData v)
Purpose: Construct tree segment for all Events in one voice section
Parameters:
  Input:  VoiceEventListData v - data for one voice
  Output: -
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Coloration lastcol;

  static Element createVoiceEventTree(VoiceEventListData v)
  {
    Element elel=new Element("EventList",cmmens),
            curevel;
    Event   cure;

    lastcol=v.getSection().getBaseColoration();
    for (Iterator i=v.events.iterator(); i.hasNext();)
      {
        cure=(Event)i.next();

        if (cure.geteventtype()==Event.EVENT_VARIANTDATA_START)
          curevel=createVariantDataTree(i,((VariantMarkerEvent)cure).getReadings());
        else
          curevel=createOneEventTree(cure);

        if (curevel!=null)
          elel.addContent(curevel);
      }

    return elel;
  }

/*------------------------------------------------------------------------
Method:  Element createVariantDataTree(Iterator i,ArrayList<VariantReadings> varReadings)
Purpose: Construct tree segment for set of variant readings
Parameters:
  Input:  Iterator i - event list iterator, positioned at start of variant
                       segment
          ArrayList<VariantReadings> varReadings - variant readings
  Output: Iterator i
  Return: root element (for this segment)
------------------------------------------------------------------------*/

  static Element createVariantDataTree(Iterator i,ArrayList<VariantReading> varReadings)
  {
    Element variantRootEl=new Element("VariantReadings",cmmens),
            readingEl=new Element("Reading",cmmens),
            eventListEl=null,
            curevel;
    Event   cure;

    /* create event list for default reading */
    readingEl.addContent(new Element("VariantVersionID",cmmens).setText("DEFAULT"));
    eventListEl=new Element("Music",cmmens);
    cure=i.hasNext() ? (Event)i.next() : null;
    while (cure.geteventtype()!=Event.EVENT_VARIANTDATA_END)
      {
        curevel=createOneEventTree(cure);
        if (curevel!=null)
          eventListEl.addContent(curevel);

        cure=i.hasNext() ? (Event)i.next() : null;
      }
    readingEl.addContent(eventListEl);
    variantRootEl.addContent(readingEl);

    /* create event lists for variant readings */
    for (VariantReading vr : varReadings)
      {
        readingEl=new Element("Reading",cmmens);
        for (VariantVersionData vvd : vr.getVersions())
          readingEl.addContent(new Element("VariantVersionID",cmmens).setText(vvd.getID()));
        if (vr.isError())
          readingEl.addContent(new Element("Error",cmmens));
        eventListEl=new Element("Music",cmmens);
        for (int vi=0; vi<vr.getNumEvents(); vi++)
          {
            Event ve=vr.getEvent(vi);
            curevel=createOneEventTree(ve);
            if (curevel!=null)
              eventListEl.addContent(curevel);
          }
        readingEl.addContent(eventListEl);
        variantRootEl.addContent(readingEl);
      }

    return variantRootEl;
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
              if (!mi.tempoChange.equals(Mensuration.DEFAULT_TEMPO_CHANGE))
                miel.addContent(addProportion(new Element("TempoChange",cmmens),mi.tempoChange));
              curevel.addContent(miel);
            }
          if (m.noScoreSig())
            curevel.addContent(new Element("NoScoreEffect",cmmens));
          break;

        case Event.EVENT_REST:
          curevel=new Element("Rest",cmmens);
          RestEvent r=(RestEvent)cure;
          addNoteInfoData(curevel,r.getnotetype(),r.getLength());
          curevel.addContent(new Element("BottomStaffLine",cmmens).setText(
            Integer.toString(r.getbottomline())));
          curevel.addContent(new Element("NumSpaces",cmmens).setText(
            Integer.toString(r.getnumlines())));
          if (r.getCorona()!=null)
            curevel.addContent(addSignumData(new Element("Corona",cmmens),r.getCorona()));
          if (r.getSignum()!=null)
            curevel.addContent(addSignumData(new Element("Signum",cmmens),r.getSignum()));
          break;

        case Event.EVENT_NOTE:
          curevel=new Element("Note",cmmens);
          NoteEvent n=(NoteEvent)cure;
          addNoteInfoData(curevel,n.getnotetype(),n.getLength());
          addLocusData(curevel,n.getPitch());

          ModernAccidental ma=n.getPitchOffset();
          if (ma.pitchOffset!=0 || ma.optional==true)
            curevel.addContent(addModernAccidentalData(new Element("ModernAccidental",cmmens),ma));
          if (n.isligated())
            curevel.addContent(new Element("Lig",cmmens).setText(NoteEvent.LigTypeNames[n.getligtype()]));
          if (n.getTieType()!=NoteEvent.TIE_NONE)
            curevel.addContent(new Element("Tie",cmmens).setText(NoteEvent.TieTypeNames[n.getTieType()]));
          int numFlags=n.getNumFlags();
          if (numFlags>0)
            {
              Element flagEl=new Element("Flagged",cmmens);
              if (numFlags>1)
                flagEl.addContent(new Element("NumFlags",cmmens).setText(Integer.toString(numFlags)));
              curevel.addContent(flagEl);
            }

          Element stemel=null;
          if (n.getstemside()!=-1 && n.getstemdir()!=-1)
            {
              stemel=new Element("Stem",cmmens);
              stemel.addContent(new Element("Dir",cmmens).setText(NoteEvent.StemDirs[n.getstemdir()]));
              stemel.addContent(new Element("Side",cmmens).setText(NoteEvent.StemDirs[n.getstemside()]));
            }
          else if (n.getstemdir()!=NoteEvent.STEM_UP && n.getstemdir()!=-1)
            {
              stemel=new Element("Stem",cmmens);
              stemel.addContent(new Element("Dir",cmmens).setText(NoteEvent.StemDirs[n.getstemdir()]));
            }
          if (stemel!=null)
            curevel.addContent(stemel);

          if (n.getHalfColoration()!=NoteEvent.HALFCOLORATION_NONE)
            {
              String halfColText=null;
              switch (n.getHalfColoration())
                {
                  case NoteEvent.HALFCOLORATION_PRIMARYSECONDARY:
                    halfColText="PrimarySecondary";
                    break;
                  case NoteEvent.HALFCOLORATION_SECONDARYPRIMARY:
                    halfColText="SecondaryPrimary";
                    break;
                }
              curevel.addContent(new Element("HalfColoration",cmmens).setText(halfColText));
            }

          if (n.getCorona()!=null)
            curevel.addContent(addSignumData(new Element("Corona",cmmens),n.getCorona()));
          if (n.getSignum()!=null)
            curevel.addContent(addSignumData(new Element("Signum",cmmens),n.getSignum()));

          String mt=n.getModernText();
          if (mt!=null)
            {
              Element mtel=new Element("ModernText",cmmens);
              mtel.addContent(new Element("Syllable",cmmens).setText(mt));
              if (n.isWordEnd())
                mtel.addContent(new Element("WordEnd",cmmens));
              if (n.isModernTextEditorial())
                mtel.addContent(new Element("Editorial",cmmens));
              curevel.addContent(mtel);
            }
          break;

        case Event.EVENT_DOT:
          curevel=new Element("Dot",cmmens);
          curevel.addContent(
            addLocusData(new Element("Pitch",cmmens),((DotEvent)cure).getPitch()));

/*        -- deprecated --
          curevel.addContent(new Element("StaffLoc",cmmens).setText(
            Integer.toString(((DotEvent)cure).getstaffloc())));*/
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
          BarlineEvent curbe=(BarlineEvent)cure;

          int nl=curbe.getNumLines();
          if (nl!=1)
            ble.addContent(new Element("NumLines",cmmens).setText(Integer.toString(nl)));

          boolean isRepeatSign=curbe.isRepeatSign();
          if (isRepeatSign)
            ble.addContent(new Element("RepeatSign",cmmens));

          int bl=curbe.getBottomLinePos();
          if (bl!=0)
            ble.addContent(new Element("BottomStaffLine",cmmens).setText(Integer.toString(bl)));

          int ns=curbe.getNumSpaces();
          if (ns!=4)
            ble.addContent(new Element("NumSpaces",cmmens).setText(Integer.toString(ns)));

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

          if (le.getLength().i1>0)
            mile.addContent(addProportion(new Element("Length",cmmens),le.getLength()));
          else
            mile.addContent(new Element("Begin",cmmens));
          curevel.addContent(mile);
          break;
        case Event.EVENT_LACUNA_END:
          curevel=new Element("MiscItem",cmmens);
          mile=new Element("Lacuna",cmmens);
          mile.addContent(new Element("End",cmmens));
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
          return null;

        default:
          curevel=new Element("Event",cmmens);
      }

    /* generic event attributes */
    if (cure.isColored())
      curevel.addContent(new Element("Colored",cmmens));
    if (cure.isEditorial())
      curevel.addContent(new Element("Editorial",cmmens));
    if (cure.isError())
      curevel.addContent(new Element("Error",cmmens));
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
    if (ma.accType==ModernAccidental.ACC_NONE)
      e.addContent(new Element("PitchOffset",cmmens).setText(Integer.toString(ma.pitchOffset)));
    else
      {
        e.addContent(new Element("AType",cmmens).setText(ModernAccidental.AccidentalNames[ma.accType]));
        if (ma.numAcc!=1)
          e.addContent(new Element("Num",cmmens).setText(Integer.toString(ma.numAcc)));
      }

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
    if (l!=null && l.i1!=0)
      e.addContent(addProportion(new Element("Length",cmmens),l.reduce()));

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
    if (s.offset!=Signum.DEFAULT_YOFFSET)
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

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public float getFileVersion()
  {
    return fileVersion;
  }
}
