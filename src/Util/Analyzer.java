/*----------------------------------------------------------------------*/
/*

        Module          : Analyzer.java

        Package         : Util

        Classes Included: Analyzer, VoiceAnalysisData

        Purpose         : Music analysis utilities

        Programmer      : Ted Dumitrescu

        Date Started    : 7/14/07

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Util;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import java.net.*;
import java.util.*;

import DataStruct.*;
import Gfx.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   Analyzer
Extends: -
Purpose: Music analysis utilities for one score
------------------------------------------------------------------------*/

public class Analyzer
{
/*----------------------------------------------------------------------*/
/* Class variables */

  /* for standalone application use */
  static boolean screenoutput=false,
                 recursive=false;

  public static final String BaseDataDir="/data/";
  public static String       BaseDataURL;

  static String initdirectory;


  static final int    NUM_MENSURATIONS=4,
                      MENS_O=0,
                      MENS_C=1,
                      MENS_3=2,
                      MENS_P=3;
  static final String MENSURATION_NAMES[]=new String[] { "O","C","3","P" };
  static final int    NUMVOICES_FOR_RHYTHM_AVG=2;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance variables */

  PieceData     musicData;
  ScoreRenderer renderedSections[];
  int           numVoices;

  /* analysis results */
  public VoiceAnalysisData vad[];
  public int               totalUnpreparedSBDiss=0,
                           totalPassingDissSMPair=0,
                           totalOffbeatDissM=0;
  public double            avgRhythmicDensity[],
                           avgSyncopationDensity=0,
                           OCLength=0,
                           passingSMDissDensity=0,
                           offbeatMDissDensity=0,
                           dissDensity=0;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  void main(String args[])
Purpose: Main routine
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void main(String args[])
  {
    String cmdlineFilename=parseCmdLine(args);

    /* initialize data locations */
    try
      {
        initdirectory=new File(".").getCanonicalPath()+BaseDataDir;
        BaseDataURL="file:///"+initdirectory;
      }
    catch (Exception e)
      {
        System.err.println("Error loading local file locations: "+e);
        e.printStackTrace();
      }

    DataStruct.XMLReader.initparser(BaseDataURL,false);
    MusicWin.initScoreWindowing(BaseDataURL,initdirectory+"music/",false);
    try
      {
        Gfx.MusicFont.loadmusicface(BaseDataURL);
      }
    catch (Exception e)
      {
        System.err.println("Error loading font: "+e);
        e.printStackTrace();
      }

    analyzeFiles(cmdlineFilename);
  }

/*------------------------------------------------------------------------
Method:  void analyzeFiles(String mainFilename)
Purpose: Analyze one set of files (recursing to subdirectories if necessary)
Parameters:
  Input:  String mainFilename - name of file set in one directory
          String subdirName   - name of subdirectory (null for base directory)
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void analyzeFiles(String mainFilename)
  {
    try
      {
        PrintStream outs;
        OptionSet   optSet=new OptionSet(null);

        RecursiveFileList    fl=new RecursiveFileList(mainFilename,recursive);
        LinkedList<Analyzer> results=new LinkedList<Analyzer>();

        /* analyze individual pieces */
        for (File curfile : fl)
          {
            URL fileURL=curfile.toURI().toURL();
            String fileName=curfile.getName();
                   
            System.out.print("Analyzing: "+fileName+"...");

            PieceData       musicData=new CMMEParser(fileURL).piece;
            ScoreRenderer[] renderedSections=renderSections(musicData,optSet);
            Analyzer        a=new Analyzer(musicData,renderedSections);

            outs=screenoutput ? System.out : new PrintStream("data/stats/"+fileName+".txt");
            a.printGeneralAnalysis(outs);
            if (!screenoutput)
              outs.close();

            System.out.println("done");

            results.add(a);
          }

        /* output result summary */
        outs=screenoutput ? System.out : new PrintStream("data/stats/summary.txt");
        if (screenoutput)
          {
            outs.println();
            outs.println("SUMMARY");
            outs.println();
          }
        outs.println("Composer\tTitle\tDensity (O)\tDensity (C)\tDensity (3)\tDensity (P)\tSyncop density\tUnprepared syncop diss\tPassing SM diss density\tOffbeat M diss density");
        for (Analyzer a : results)
          {
            outs.print(a.musicData.getComposer()+"\t"+a.musicData.getFullTitle());
            for (int mi=0; mi<NUM_MENSURATIONS; mi++)
              {
                outs.print("\t");
                if (a.avgRhythmicDensity[mi]>0)
                  outs.print(String.valueOf(a.avgRhythmicDensity[mi]));
                else
                  outs.print("-");
              }
            outs.print("\t"+a.avgSyncopationDensity+"\t"+a.totalUnpreparedSBDiss);
            outs.print("\t"+a.passingSMDissDensity+"\t"+a.offbeatMDissDensity);
            outs.println();
          }
        if (!screenoutput)
          outs.close();
      }
    catch (Exception e)
      {
        System.err.println("Error: "+e);
        e.printStackTrace();
      }
  }

/*------------------------------------------------------------------------
Method:  String parseCmdLine(String args[])
Purpose: Parse command line
Parameters:
  Input:  String args[] - program arguments
  Output: -
  Return: filename (or "*" if recursive with no filename specified)
------------------------------------------------------------------------*/

  static String parseCmdLine(String args[])
  {
    String fn=null;

    if (args.length<1)
      usage_exit();

    for (int i=0; i<args.length; i++)
      if (args[i].charAt(0)=='-')
        /* options */
        for (int opti=1; opti<args[i].length(); opti++)
          switch (args[i].charAt(opti))
            {
              case 's':
                screenoutput=true;
                break;
              case 'r':
                recursive=true;
                break;
              default:
                usage_exit();
            }
      else
        /* filename */
        if (i!=args.length-1)
          usage_exit();
        else
          fn=args[i];

    if (fn==null)
      if (recursive)
        fn="*";
      else
        usage_exit();

    return "data\\music\\"+fn;
  }

/*------------------------------------------------------------------------
Method:  void usage_exit()
Purpose: Exit for invalid command line
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void usage_exit()
  {
    System.err.println("Usage: java Util.Analyzer [options] filename");
    System.err.println("Options:");
    System.err.println("  -s: Screen output");
    System.err.println("  -r: Recursively search subdirectories");
    System.exit(1);
  }

/*------------------------------------------------------------------------
Method:  ScoreRenderer[] void renderSections()
Purpose: Pre-render all sections of one piece
Parameters:
  Input:  -
  Output: -
  Return: rendered section array
------------------------------------------------------------------------*/

  static final double SECTION_END_SPACING=10;

  static ScoreRenderer[] renderSections(PieceData musicData,OptionSet options)
  {
    double startX=0;

    /* initialize voice parameters */
    int numVoices=musicData.getVoiceData().length;
    RenderedSectionParams[] sectionParams=new RenderedSectionParams[numVoices];
    for (int i=0; i<numVoices; i++)
      sectionParams[i]=new RenderedSectionParams();

    /* initialize sections */
    int numSections=musicData.getNumSections();
    ScoreRenderer[] renderedSections=new ScoreRenderer[numSections];
    int nummeasures=0;
    for (int i=0; i<numSections; i++)
      {
        renderedSections[i]=new ScoreRenderer(
          i,musicData.getSection(i),musicData,
          sectionParams,
          options,nummeasures,startX);
        sectionParams=renderedSections[i].getEndingParams();
        nummeasures+=renderedSections[i].getNumMeasures();
        startX+=renderedSections[i].getXsize()+SECTION_END_SPACING;
      }

    return renderedSections;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: Analyzer(PieceData musicData,ScoreRenderer renderedSections[])
Purpose:     Initialize analysis functions for one score
Parameters:
  Input:  PieceData musicData              - original music data
          ScoreRenderer renderedSections[] - scored+rendered music data
  Output: -
------------------------------------------------------------------------*/

  public Analyzer(PieceData musicData,ScoreRenderer renderedSections[])
  {
    this.musicData=musicData;
    this.renderedSections=renderedSections;
    this.numVoices=musicData.getVoiceData().length;
  }

/*------------------------------------------------------------------------
Method:  void printGeneralAnalysis(PrintStream outp)
Purpose: Print generic score analysis
Parameters:
  Input:  -
  Output: PrintStream outp - output destination
  Return: -
------------------------------------------------------------------------*/

  public void printGeneralAnalysis(PrintStream outp)
  {
    outp.println("General analysis: "+musicData.getComposer()+", "+musicData.getFullTitle());
    outp.println();
    outp.println("Number of voices: "+numVoices);
    outp.println("Number of sections: "+renderedSections.length);
    outp.println();

    vad=new VoiceAnalysisData[numVoices];
    for (int i=0; i<numVoices; i++)
      {
        vad[i]=new VoiceAnalysisData();
        vad[i].vData=musicData.getVoiceData()[i];
      }
    totalUnpreparedSBDiss=0;
    totalPassingDissSMPair=0;

    for (ScoreRenderer s : renderedSections)
      {
        for (int i=0; i<s.getNumVoices(); i++)
          {
            int        curMens=MENS_C;
            Proportion curProp=new Proportion(Proportion.EQUALITY);
            double     curPropVal=curProp.toDouble(),
                       curMensStartTime=0;

            RenderList rl=s.getRenderedVoice(i);
            if (rl==null)
              break;

            int vnum=-1;
            for (int i1=0; i1<numVoices; i1++)
              if (rl.getVoiceData()==vad[i1].vData)
                vnum=i1;

            NoteEvent     ne=null;
            RenderedEvent rne=null;
            int           renum=0;
            for (RenderedEvent re : rl)
              {
                switch (re.getEvent().geteventtype())
                  {
                    case Event.EVENT_NOTE:
                      ne=(NoteEvent)re.getEvent();
                      rne=re;
                      vad[i].numNotes[curMens]++;
                      vad[i].totalNumNotes++;
                      vad[i].totalNoteLengths[curMens]+=ne.getmusictime().toDouble()*curPropVal;

                      if (ne.getPitch().isLowerThan(vad[i].lowestPitch))
                        vad[i].lowestPitch=ne.getPitch();
                      if (ne.getPitch().isHigherThan(vad[i].highestPitch))
                        vad[i].highestPitch=ne.getPitch();

                      if (curMens!=MENS_3 &&
                          curMens!=MENS_P)
                        {
                          vad[i].numNotesOC++;
                          if (syncopated(s,re))
                            vad[i].totalOffbeat++;
                          if (isUnpreparedSuspension(s,re))
                            {
                              totalUnpreparedSBDiss++;
                              outp.println("Unprepared SB/M dissonance in m. "+(re.getmeasurenum()+1));
                            }
                          if (isPassingDissonantSMPair(s,i,renum))
                            totalPassingDissSMPair++;
                          if (isOffbeatDissonantM(s,re))
                            totalOffbeatDissM++;
                        }

                      break;
                    case Event.EVENT_MULTIEVENT:
                      // to be implemented
                      Mensuration m=re.getEvent().getMensInfo();
                      if (m!=null)
                        curMens=getMensType(m);
                      break;
                    case Event.EVENT_MENS:
                      vad[i].totalPassageLengths[curMens]+=re.getmusictime().toDouble()-curMensStartTime;
                      curMensStartTime=re.getmusictime().toDouble();

                      curMens=getMensType(re.getEvent().getMensInfo());
                      if (curMens==MENS_O &&
                          ((MensEvent)re.getEvent()).getSigns().size()>1 ||
                          ((MensEvent)re.getEvent()).getMainSign().signType==MensSignElement.NUMBERS)
                        curMens=MENS_3;
                      break;
                    case Event.EVENT_PROPORTION:
                      curProp.multiply(((ProportionEvent)re.getEvent()).getproportion());
                      curPropVal=curProp.toDouble();
                      break;
                  }
                renum++;
              }
            if (ne!=null)
              {
                /* discount final longa */
                vad[i].numNotes[curMens]--;
                vad[i].totalNumNotes--;
                vad[i].totalNoteLengths[curMens]-=ne.getmusictime().toDouble()*curPropVal;
                vad[i].totalPassageLengths[curMens]+=rne.getmusictime().toDouble()-curMensStartTime;
              }
          }
      }

    if (totalUnpreparedSBDiss>0)
      outp.println();

    for (int i=0; i<numVoices; i++)
      {
        outp.println("Voice "+(i+1)+": "+musicData.getVoiceData()[i].getName());
        outp.println("  Range: "+vad[i].lowestPitch+" - "+vad[i].highestPitch);

        for (int mi=0; mi<NUM_MENSURATIONS; mi++)
          if (vad[i].numNotes[mi]!=0)
            {
              outp.println("  Mensuration type: "+MENSURATION_NAMES[mi]);
              outp.println("    Number of notes (not including final): "+vad[i].numNotes[mi]);
              outp.println("    Total note lengths: "+vad[i].totalNoteLengths[mi]);
              vad[i].rhythmicDensity[mi]=vad[i].totalNoteLengths[mi]/(double)vad[i].numNotes[mi];
              outp.println("    Rhythmic density: "+vad[i].rhythmicDensity[mi]);
            }

        vad[i].syncopationDensity=(double)vad[i].totalOffbeat/(double)vad[i].numNotesOC;
        outp.println("  Number of syncopated notes: "+vad[i].totalOffbeat);
        outp.println("  Syncopation density: "+vad[i].syncopationDensity);

        outp.println();
      }

    /* averages of the top two voices */
    avgRhythmicDensity=new double[] { 0,0,0,0 };
    outp.println();
    outp.println("Averages for highest "+NUMVOICES_FOR_RHYTHM_AVG+" voices");
    for (int i=0; i<numVoices && i<NUMVOICES_FOR_RHYTHM_AVG; i++)
      for (int mi=0; mi<NUM_MENSURATIONS; mi++)
        if (vad[i].numNotes[mi]>0)
          avgRhythmicDensity[mi]+=vad[i].rhythmicDensity[mi]/2; // for SB, not minims
    for (int mi=0; mi<NUM_MENSURATIONS; mi++)
      {
        if (numVoices>NUMVOICES_FOR_RHYTHM_AVG-1)
          avgRhythmicDensity[mi]/=(double)NUMVOICES_FOR_RHYTHM_AVG;
        if (avgRhythmicDensity[mi]>0)
          outp.println("  Rhythmic density in SB ("+MENSURATION_NAMES[mi]+"): "+
                       avgRhythmicDensity[mi]);
      }
    avgSyncopationDensity=0;
    for (int i=0; i<numVoices && i<NUMVOICES_FOR_RHYTHM_AVG; i++)
      avgSyncopationDensity+=vad[i].syncopationDensity;
    if (numVoices>NUMVOICES_FOR_RHYTHM_AVG-1)
      avgSyncopationDensity/=(double)NUMVOICES_FOR_RHYTHM_AVG;
    outp.println("  Syncopation density: "+avgSyncopationDensity);

    if (totalUnpreparedSBDiss>0)
      outp.println("Number of unprepared syncopated SB/M dissonances: "+totalUnpreparedSBDiss);
    outp.println("Number of passing dissonant SM pairs: "+totalPassingDissSMPair);
    outp.println("Number of dissonant offbeat Ms: "+totalOffbeatDissM);

    double avgMensLengths[]=new double[] { 0,0,0,0 };
    for (int i=0; i<numVoices; i++)
      for (int mi=0; mi<NUM_MENSURATIONS; mi++)
        avgMensLengths[mi]+=vad[i].totalPassageLengths[mi];
    for (int mi=0; mi<NUM_MENSURATIONS; mi++)
      avgMensLengths[mi]/=(double)numVoices;
    OCLength=(avgMensLengths[MENS_O]+avgMensLengths[MENS_C])/2; /* in SB */
    passingSMDissDensity=totalPassingDissSMPair/OCLength;
    offbeatMDissDensity=totalOffbeatDissM/OCLength;
    dissDensity=(totalPassingDissSMPair+totalOffbeatDissM)/OCLength;
    outp.println("Total length of O/C sections: "+OCLength);
    outp.println("Passing dissonant SM pair density: "+passingSMDissDensity);
    outp.println("Offbeat dissonant M density: "+offbeatMDissDensity);
//    outp.println("Basic dissonance density: "+dissDensity);
  }

  int getMensType(Mensuration m)
  {
    if (m.prolatio==Mensuration.MENS_TERNARY)
      return MENS_P;
    if (m.tempus==Mensuration.MENS_TERNARY)
      return MENS_O;
    return MENS_C;
  }

  boolean isUnpreparedSuspension(ScoreRenderer s,RenderedEvent re)
  {
    MeasureInfo measure=s.getMeasure(re.getmeasurenum());
    double      mt=re.getmusictime().toDouble(),
                measurePos=mt-measure.startMusicTime.toDouble(),
                len=re.getEvent().getLength().toDouble();

    NoteEvent ne=(NoteEvent)re.getEvent();
    if (re.getmusictime().i2%3==0) /* avoid sesquialtera/tripla */
      return false;
    if (len<1)
      return false;
    if (len>=2 &&
        ((int)measurePos)%2==0)
      return false;
    if (len>=1 && len<2 &&
        (double)(measurePos-(int)measurePos)<=0.0)
      return false;

    Pitch            p=ne.getPitch();
    RenderedSonority rs=re.getFullSonority();
    for (int i=0; i<rs.getNumPitches(); i++)
      if (isDissonant(p,rs.getPitch(i),i==0) &&
          rs.getRenderedNote(i).getmusictime().toDouble()<mt)
        return true;

    return false;
  }

  boolean isPassingDissonantSMPair(ScoreRenderer s,int vnum,int renum)
  {
    RenderedEvent re=s.eventinfo[vnum].getEvent(renum);
    MeasureInfo   measure=s.getMeasure(re.getmeasurenum());
    double        mt=re.getmusictime().toDouble(),
                  measurePos=mt-measure.startMusicTime.toDouble();

    NoteEvent ne=(NoteEvent)re.getEvent();
    if (ne.getnotetype()!=NoteEvent.NT_Semiminima ||
        (double)(measurePos-(int)measurePos)>0 ||
        ((int)measurePos)%2==0)
      return false;

    /* check next note */
    RenderedEvent nextNote=s.getNeighboringEventOfType(Event.EVENT_NOTE,vnum,renum+1,1);
    if (nextNote==null ||
        nextNote.getmusictime().toDouble()>mt+0.5)
      return false;
    Event     e=nextNote.getEvent();
    NoteEvent ne2=e.geteventtype()==Event.EVENT_MULTIEVENT ?
                   ((MultiEvent)e).getLowestNote() :
                   (NoteEvent)e;
    if (ne2.getnotetype()!=NoteEvent.NT_Semiminima)
      return false;

    /* check for dissonance */
    Pitch            p=ne.getPitch();
    RenderedSonority rs=re.getFullSonority();
    for (int i=0; i<rs.getNumPitches(); i++)
      if (isDissonant(p,rs.getPitch(i),i==0))
        return true;

    return false;
  }

  boolean isOffbeatDissonantM(ScoreRenderer s,RenderedEvent re)
  {
    MeasureInfo measure=s.getMeasure(re.getmeasurenum());
    double      mt=re.getmusictime().toDouble(),
                measurePos=mt-measure.startMusicTime.toDouble(),
                len=re.getEvent().getLength().toDouble();

    NoteEvent ne=(NoteEvent)re.getEvent();
    if (ne.getnotetype()!=NoteEvent.NT_Minima ||
        (double)(measurePos-(int)measurePos)>0 ||
        ((int)measurePos)%2==0)
      return false;
    if (len>1)
      return false;

    Pitch            p=ne.getPitch();
    RenderedSonority rs=re.getFullSonority();
    for (int i=0; i<rs.getNumPitches(); i++)
      if (isDissonant(p,rs.getPitch(i),i==0) &&
          rs.getRenderedNote(i).getmusictime().toDouble()<mt)
        return true;

    return false;
  }

  boolean isDissonant(Pitch p1,Pitch p2,boolean bassInterval)
  {
    int interval=getAbsInterval(p1,p2);
    if (interval==2 || interval==7 ||
        (bassInterval && interval==4))
      return true;
    return false;
  }

  int getAbsInterval(Pitch p1,Pitch p2)
  {
    return Math.abs(p1.placenum-p2.placenum)%7+1;
  }

  boolean syncopated(ScoreRenderer s,RenderedEvent re)
  {
    MeasureInfo measure=s.getMeasure(re.getmeasurenum());
    double      mt=re.getmusictime().toDouble(),
                measurePos=mt-measure.startMusicTime.toDouble(),
                len=re.getEvent().getLength().toDouble();

    if (re.getmusictime().i2%3==0) /* avoid sesquialtera/tripla */
      return false;
    if (len<=.5) /* no SM or smaller */
      return false;

    if (len<=1 &&
        measurePos-(int)measurePos>0)
      return true;
    if (len>1 &&
        (measurePos-(int)measurePos>0 ||
        ((int)measurePos)%2!=0))
      return true;

    return false;
  }
}

/*------------------------------------------------------------------------
Class:   VoiceAnalysisData
Extends: -
Purpose: Analysis parameters for one voice
------------------------------------------------------------------------*/

class VoiceAnalysisData
{
  Voice  vData=null;

  int    numNotes[]=new int[] { 0,0,0,0 },
         totalNumNotes=0,
         numNotesOC=0;
  double totalNoteLengths[]=new double[] { 0,0,0,0 },
         rhythmicDensity[]=new double[] { 0,0,0,0 },
         totalPassageLengths[]=new double[] { 0,0,0,0 };

  Pitch  lowestPitch=Pitch.HIGHEST_PITCH,
         highestPitch=Pitch.LOWEST_PITCH;

  int    totalOffbeat=0;
  double syncopationDensity=0;

  public VoiceAnalysisData()
  {
  }
}
