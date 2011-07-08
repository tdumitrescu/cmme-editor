/*----------------------------------------------------------------------*/
/*

        Module          : MIDIReaderWriter.java

        Package         : DataStruct

        Classes	Included: MIDIReaderWriter

        Purpose         : functions for dealing with MIDI data

        Programmer      : Ted Dumitrescu

        Date Started    : 1/13/06

Updates:
7/19/06: updated note/rest-lengths to use minim-based system
2/22/09: updated ModernAccidental creation to match current pitch-offset
         system

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import javax.sound.midi.*;
import java.util.*;

/*------------------------------------------------------------------------
Class:   MIDIReaderWriter
Extends: -
Purpose: MIDI functions
------------------------------------------------------------------------*/

public class MIDIReaderWriter
{
/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  PieceData MIDtoCMME(File mf)
Purpose: Load CMME music data from a MIDI file
Parameters:
  Input:  File mf - MIDI file
  Output: -
  Return: CMME music data structure
------------------------------------------------------------------------*/

  public static PieceData MIDtoCMME(File mf)
  {
    Sequence  inputSeq;
    Track[]   tracks;
    PieceData piece;
    int       timeResolution;

    /* load data from file into sequence */
    try
      {
        inputSeq=MidiSystem.getSequence(mf);
        tracks=inputSeq.getTracks();
        timeResolution=inputSeq.getResolution();
      }
    catch (Exception e)
      {
        System.err.println("Error: "+e);
        return null;
      }

    /* initialize output structure */
    piece=new PieceData();
    piece.setGeneralData("Title",null,"Composer","Editor","","Converted from MIDI data");
    piece.addVariantVersion(new VariantVersionData("Default"));

    /* create temporary pitch/rhythm lists */
    LinkedList<LinkedList> tracksPitches=new LinkedList<LinkedList>(),
                           tracksRhythms=new LinkedList<LinkedList>();
    LinkedList<Integer>    tracksPitchAvg=new LinkedList<Integer>();
    for (int i=0; i<tracks.length; i++)
      {
        LinkedList<Integer> pitchList=new LinkedList<Integer>();
        LinkedList<Float>   rhythmList=new LinkedList<Float>();
        int        notenum=-1,
                   numpitches=0;
        long       notestartedtime=-1,
                   reststartedtime=0,
                   pitchtotal=0;

        for (int ti=0; ti<tracks[i].size(); ti++)
          {
            MidiEvent   event=tracks[i].get(ti);
            MidiMessage msg=event.getMessage();
            byte[]      msgbytes=msg.getMessage();
            float       notelength=-1,
                        restlength=-1;
            switch (msg.getStatus()&0xF0)
              {
                case 0x80: /* note off */
                  if (notestartedtime!=-1)
                    {
                      notelength=(float)(event.getTick()-notestartedtime)/(timeResolution*4);
                      notestartedtime=-1;
                      pitchList.add(new Integer(notenum));
                      rhythmList.add(new Float(notelength));
                    }
                  reststartedtime=event.getTick();
                  break;
                case 0x90: /* note on */
                  notenum=(int)msgbytes[1];
                  notestartedtime=event.getTick();
                  restlength=(float)(notestartedtime-reststartedtime)/(timeResolution*4);
                  if (restlength>0)
                    {
                      pitchList.add(new Integer(-1));
                      rhythmList.add(new Float(restlength));
                    }
                  pitchtotal+=notenum;
                  numpitches++;
                  break;
              }
          }

        if (pitchList.size()>0)
          {
            tracksPitches.add(pitchList);
            tracksRhythms.add(rhythmList);
            tracksPitchAvg.add(new Integer((int)(pitchtotal/numpitches)));
          }
      }

    /* convert pitch/rhythm info to CMME data */
    int     numvoices=tracksPitches.size();
    Voice[] vl=new Voice[numvoices];
    MusicMensuralSection curSec=new MusicMensuralSection(numvoices);
//    curSec.setVersion(piece.getVariantVersions().get(0));

    for (int i=0; i<numvoices; i++)
      {
        /* initialize voice */
        Voice             curmdata=new Voice(piece,i+1,"Voice "+(i+1),false);
        VoiceMensuralData curv=new VoiceMensuralData(curmdata,curSec);

        Clef curclef=chooseClef(((Integer)tracksPitchAvg.get(i)).intValue());
        curv.addEvent(new ClefEvent(curclef,null,null));

        /* traverse pitch/rhythm lists */
        LinkedList pitchList=(LinkedList)tracksPitches.get(i);
        LinkedList rhythmList=(LinkedList)tracksRhythms.get(i);
        Iterator pi=pitchList.iterator(),
                 ri=rhythmList.iterator();
        while (pi.hasNext())
          {
            int   notenum=((Integer)(pi.next())).intValue();
            float notelen=((Float)(ri.next())).floatValue();
            if (notenum>=0)
              /* Note */
              addNoteEvent(curv,curclef,notenum,notelen);
            else
              /* Rest */
              addRestEvents(curv,notelen);
          }

        /* add PieceEnd event at end of each voice */
        curv.addEvent(new Event(Event.EVENT_SECTIONEND));

        /* add voice to list */
        vl[i]=curmdata;
        curSec.setVoice(i,curv);
      }

    piece.setVoiceData(vl);
    piece.addSection(curSec);
    return piece;
  }

/*------------------------------------------------------------------------
Method:  Clef chooseClef(int avgPitch)
Purpose: Pick a clef for one voice based on average pitch value
Parameters:
  Input:  int avgPitch - average MIDI pitch number for this voice
  Output: -
  Return: clef
------------------------------------------------------------------------*/

  public static Clef chooseClef(int avgPitch)
  {
    /* C1 */
    if (avgPitch>62)
      return new Clef(Clef.CLEF_C,1,Clef.DefaultClefPitches[Clef.CLEF_C],false,false,null);

    /* C4 */
    if (avgPitch>55)
      return new Clef(Clef.CLEF_C,7,Clef.DefaultClefPitches[Clef.CLEF_C],false,false,null);

    /* F4 */
    return new Clef(Clef.CLEF_F,7,Clef.DefaultClefPitches[Clef.CLEF_F],false,false,null);
  }

/*------------------------------------------------------------------------
Method:  Pitch MIDInumToPitch(int notenum,Clef clef)
Purpose: Convert MIDI pitch number to CMME Pitch
Parameters:
  Input:  int notenum - pitch number
          Clef clef   - current clef on staff
  Output: -
  Return: pitch
------------------------------------------------------------------------*/

  static Pitch MIDInumToPitch(int notenum,Clef clef)
  {
    int octave=(notenum-21)/12;
    char noteletter='X';
    switch ((notenum+3)%12)
      {
        case 0:
          noteletter='A';
          break;
        case 1:
        case 2:
          noteletter='B';
          break;
        case 3:
        case 4:
          noteletter='C';
          break;
        case 5:
          noteletter='D';
          break;
        case 6:
        case 7:
          noteletter='E';
          break;
        case 8:
        case 9:
          noteletter='F';
          break;
        case 10:
        case 11:
          noteletter='G';
          break;
      }
    return new Pitch(noteletter,octave,clef);
  }

/*------------------------------------------------------------------------
Method:  Pitch MIDInumToModAcc(int notenum)
Purpose: Generate modern accidental (if necessary) based on MIDI pitch number
Parameters:
  Input:  int notenum - pitch number
  Output: -
  Return: modern accidental (null for none)
------------------------------------------------------------------------*/

  static ModernAccidental MIDInumToModAcc(int notenum)
  {
    switch ((notenum+3)%12)
      {
        case 1:
        case 6:
          return new ModernAccidental(-1,false);
        case 4:
        case 9:
        case 11:
          return new ModernAccidental(1,false);
      }
    return new ModernAccidental(0,false);
  }

/*------------------------------------------------------------------------
Method:  void addNoteEvent(VoiceMensuralData v,Clef curclef,int pitchnum,float len)
Purpose: Generate NoteEvent (and optional dot) based on an MIDI pitch and
         duration
Parameters:
  Input:  VoiceMensuralData v - voice to which to add events
          Clef curclef        - current clef
          int pitchnum        - MIDI pitch number of note
          float len           - absolute duration of note
  Output: -
  Return: -
------------------------------------------------------------------------*/

  static void addNoteEvent(VoiceMensuralData v,Clef curclef,int pitchnum,float len)
  {
    String     nt="";
    Proportion prop=null;
    boolean    dot=false;
    NoteEvent  ne;

    if (len>=8)
      {
        nt="Maxima";
        prop=new Proportion((int)(len*2),1);
      }
    else if (len>=4)
      {
        nt="Longa";
        prop=new Proportion((int)(len*2),1);
        if (len>=6)
          dot=true;
      }
    else if (len>=2)
      {
        nt="Brevis";
        prop=new Proportion((int)(len*2),1);
        if (len>=3)
          dot=true;
      }
    else if (len>=1)
      {
        nt="Semibrevis";
        prop=new Proportion((int)(len*2),1);
        if (len>=1.5)
          dot=true;
      }
    else if (len>=.5)
      {
        nt="Minima";
        prop=new Proportion((int)(len*4),2);
        prop.reduce();
        if (len>=.75)
          dot=true;
      }
    else if (len>=.25)
      {
        nt="Semiminima";
        prop=new Proportion((int)(len*8),4);
        prop.reduce();
        if (len>=.375)
          dot=true;
      }
    else if (len>=.125)
      {
        nt="Fusa";
        prop=new Proportion((int)(len*16),8);
        prop.reduce();
        if (len>=.1875)
          dot=true;
      }
    else
      {
        nt="Semifusa";
        prop=new Proportion((int)(len*32),16);
        prop.reduce();
        if (len>=.09375)
          dot=true;
      }
    ne=new NoteEvent(nt,prop,MIDInumToPitch(pitchnum,curclef),
                     MIDInumToModAcc(pitchnum),NoteEvent.LIG_NONE,false,NoteEvent.HALFCOLORATION_NONE,
                     -1,-1,0,null);
    v.addEvent(ne);
    if (dot)
      {
        Pitch dotPitch=new Pitch(ne.getPitch());
        if (dotPitch.staffspacenum%2==0)
          dotPitch.add(1);

        v.addEvent(new DotEvent(DotEvent.DT_Addition,dotPitch,ne));
      }
  }

/*------------------------------------------------------------------------
Method:  void addRestEvents(VoiceMensuralData v,float len)
Purpose: Generate RestEvents based on an absolute duration
Parameters:
  Input:  VoiceMensuralData v - voice to which to add rests
          float len           - absolute duration of rest
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void addRestEvents(VoiceMensuralData v,float len)
  {
    /* long rests */
    while (len>=8)
      {
        v.addEvent(new RestEvent("Maxima",NoteEvent.DefaultLengths[NoteEvent.NT_Maxima],3,2,2));
        len-=8;
      }
    while (len>=4)
      {
        v.addEvent(new RestEvent("Longa",NoteEvent.DefaultLengths[NoteEvent.NT_Longa],3,2,2));
        len-=4;
      }
    while (len>=2)
      {
        v.addEvent(new RestEvent("Brevis",NoteEvent.DefaultLengths[NoteEvent.NT_Brevis],3,1,2));
        len-=2;
      }
    while (len>=1)
      {
        v.addEvent(new RestEvent("Semibrevis",NoteEvent.DefaultLengths[NoteEvent.NT_Semibrevis],3,0,2));
        len-=1;
      }
    while (len>=.5)
      {
        v.addEvent(new RestEvent("Minima",NoteEvent.DefaultLengths[NoteEvent.NT_Minima],3,0,2));
        len-=.5;
      }
    while (len>=.25)
      {
        v.addEvent(new RestEvent("Semiminima",NoteEvent.DefaultLengths[NoteEvent.NT_Semiminima],3,0,2));
        len-=.25;
      }
    while (len>=.125)
      {
        v.addEvent(new RestEvent("Fusa",NoteEvent.DefaultLengths[NoteEvent.NT_Fusa],3,0,2));
        len-=.125;
      }
    while (len>=.0625)
      {
        v.addEvent(new RestEvent("Semifusa",NoteEvent.DefaultLengths[NoteEvent.NT_Semifusa],3,0,2));
        len-=.0625;
      }
  }
}
