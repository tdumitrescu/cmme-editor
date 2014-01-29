/*----------------------------------------------------------------------*/
/*

        Module          : MIDIPlayer.java

        Package         : Gfx

        Classes Included: MIDIPlayer

        Purpose         : MIDI playback functions

        Programmer      : Ted Dumitrescu

        Date Started    : 7/20/08

        Updates         :
12/19/08: can now play back starting at an arbitrary measure
2/20/09:  added callback functions for alerting parent window of measure
          changes and end of sequence (for visual playback)
9/4/09:   added MAX_NORMAL_CHANNELS to avoid overrunning channel limit or
          running into percussion channels
9/8/09:   created default values for NOTE_ON velocity and overall gain,
          lower than before to avoid distortion on some implementations
11/6/10:  plays tied notes properly in modern notation

                                                                        */
/*----------------------------------------------------------------------*/

package Gfx;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.io.*;
import java.util.*;
import javax.sound.midi.*;
import javax.swing.*;

import DataStruct.*;

/*------------------------------------------------------------------------
Class:   MIDIPlayer
Extends: -
Purpose: MIDI playback for CMME scores
------------------------------------------------------------------------*/

public class MIDIPlayer
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static final float DEFAULT_BPM=Float.parseFloat(System.getProperty( "midi.bpm", "80")),
                     DEFAULT_MPQ=Float.parseFloat(System.getProperty( "midi.mpq", "800000"));

  static final int TICKS_PER_BEAT=Integer.parseInt(System.getProperty("midi.ticksPerBeat", "96")),
                   TICKS_PER_MINIM=TICKS_PER_BEAT/2,

                   REST_BETWEEN_SECTIONS=Integer.parseInt(System.getProperty("midi.restBetweenSections", "2")),
                   DEFAULT_INSTRUMENT=Integer.parseInt(System.getProperty("midi.defaultInstrument", "52")),
                   MAX_NORMAL_CHANNELS=7,
                   VOLUME_CONTROLLER=7,
                   DEFAULT_VELOCITY=Integer.parseInt(System.getProperty("midi.defaultVelocity", "50")),

                   MIDI_EVENT_MARKER=    6,
                   MIDI_EVENT_ENDOFTRACK=47;

  static final double DEFAULT_GAIN=Double.parseDouble(System.getProperty( "midi.defaultGain", "0.9"));

/*----------------------------------------------------------------------*/
/* Instance variables */

  MusicWin        parentWin;        /* parent window */
  PieceData       musicData;        /* original event lists */
  ScoreRenderer[] renderedSections; /* event lists rendered into measures */

  Sequence  sequenceData;
  Sequencer sequencer=null;

  MetaEventListener playbackListener;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MIDIPlayer(MusicWin parentWin,PieceData musicData,ScoreRenderer[] renderedSections)
Purpose:     Initialize player for one piece
Parameters:
  Input:  MusicWin parentWin               - parent window
          PieceData musicData              - original event lists
          ScoreRenderer[] renderedSections - event lists rendered into measures
  Output: -
------------------------------------------------------------------------*/

  public MIDIPlayer(MusicWin parentWin,PieceData musicData,ScoreRenderer[] renderedSections)
  {
    this.parentWin=parentWin;
    setMusicData(musicData,renderedSections);
  }

/*------------------------------------------------------------------------
Method:  void setMusicData(PieceData musicData,ScoreRenderer[] renderedSections)
Purpose: Initialize player with new data and create sequence
Parameters:
  Input:  PieceData musicData              - original event lists
          ScoreRenderer[] renderedSections - event lists rendered into measures
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void setMusicData(PieceData musicData,ScoreRenderer[] renderedSections)
  {
    this.musicData=musicData;
    this.renderedSections=renderedSections;

    try
      {
        sequenceData=constructSequence(musicData,renderedSections);
      }
    catch (Exception e)
      {
        JOptionPane.showMessageDialog(parentWin,"Error initializing MIDI system: "+e,"Error",JOptionPane.ERROR_MESSAGE);
        sequenceData=null;
      }
  }

/*------------------------------------------------------------------------
Method:  Sequence constructSequence(PieceData musicData,ScoreRenderer[] renderedSections)
Purpose: Create sequence out of music data
Parameters:
  Input:  PieceData musicData              - original event lists
          ScoreRenderer[] renderedSections - event lists rendered into measures
  Output: -
  Return: new MIDI sequence
------------------------------------------------------------------------*/

  class SequenceParams
  {
    Proportion curTime,
               curProportion,
               sectionStartTime;
    int        vnum;
    boolean    inTie,
               beginTie,endTie,doubleTied;
  }

  Sequence constructSequence(PieceData musicData,ScoreRenderer[] renderedSections) throws Exception
  {
    Sequence s=new Sequence(Sequence.PPQ,TICKS_PER_BEAT,musicData.getVoiceData().length);
    Track[]  t=s.getTracks();

    ShortMessage MIDImsg;

    /* set patch (instrument) number for each voice */
    for (int vi=0; vi<musicData.getVoiceData().length; vi++)
      {
        MIDImsg=new ShortMessage();
        MIDImsg.setMessage(ShortMessage.PROGRAM_CHANGE,vi%MAX_NORMAL_CHANNELS,DEFAULT_INSTRUMENT,0);
        t[vi].add(new MidiEvent(MIDImsg,0));
      }

    SequenceParams params=new SequenceParams();
    Proportion     sectionEndTime;
    params.sectionStartTime=new Proportion(0,1);
    for (ScoreRenderer rs : renderedSections)
      {
        sectionEndTime=new Proportion(params.sectionStartTime);

        for (int vi=0; vi<musicData.getVoiceData().length; vi++)
          if (rs.eventinfo[vi]!=null)
            {
              params.vnum=vi;
              params.curTime=new Proportion(params.sectionStartTime);
              params.curProportion=new Proportion(1,1);
              params.inTie=false;

              for (RenderedEvent re : rs.eventinfo[vi])
                sequenceEvent(re,t[vi],params);

              if (params.curTime.greaterThan(sectionEndTime))
                sectionEndTime=params.curTime;
            }

        addMeasureMarkers(rs,t[0],params.sectionStartTime);

        /* insert beat of rest between sections */
        params.sectionStartTime=Proportion.sum(
          sectionEndTime,new Proportion(REST_BETWEEN_SECTIONS,1));
      }

    return s;
  }

  void sequenceEvent(NoteEvent e,Track t,SequenceParams params,Proportion length) throws Exception
  {
    if (!(params.doubleTied || params.endTie))
      {
        ShortMessage MIDImsg=new ShortMessage();
        MIDImsg.setMessage(ShortMessage.NOTE_ON,params.vnum%MAX_NORMAL_CHANNELS,e.getMIDIPitch(),DEFAULT_VELOCITY);
        t.add(new MidiEvent(MIDImsg,(long)(params.curTime.toDouble()*TICKS_PER_MINIM)));
      }

    params.curTime.add(length);

    if (!(params.beginTie || params.doubleTied))
      {
        ShortMessage MIDImsg=new ShortMessage();
        MIDImsg.setMessage(ShortMessage.NOTE_OFF,params.vnum%MAX_NORMAL_CHANNELS,e.getMIDIPitch(),DEFAULT_VELOCITY);
        t.add(new MidiEvent(MIDImsg,(long)(params.curTime.toDouble()*TICKS_PER_MINIM)));
      }
  }

  void sequenceEvent(RestEvent e,Track t,SequenceParams params,Proportion length) throws Exception
  {
//    params.curTime.add(Proportion.quotient(e.getmusictime(),params.curProportion));
  }

  void sequenceEvent(ProportionEvent e,Track t,SequenceParams params,Proportion length) throws Exception
  {
    params.curProportion.multiply(e.getproportion());
  }

  void sequenceEvent(MultiEvent me,Track t,SequenceParams params,Proportion length) throws Exception
  {
    Proportion origTime=new Proportion(params.curTime),
               lastTime=new Proportion(params.curTime);

    for (Iterator i=me.iterator(); i.hasNext();)
      {
        Event e=(Event)i.next();
        switch (e.geteventtype())
          {
            case Event.EVENT_NOTE:
              sequenceEvent((NoteEvent)e,t,params,length);
              if (params.curTime.greaterThan(lastTime))
                lastTime=new Proportion(params.curTime);
              params.curTime=new Proportion(origTime);
              break;
            case Event.EVENT_REST:
              sequenceEvent((RestEvent)e,t,params,length);
              if (params.curTime.greaterThan(lastTime))
                lastTime=new Proportion(params.curTime);
              params.curTime=new Proportion(origTime);
              break;
          }
      }

    params.curTime=new Proportion(lastTime);
  }

  void sequenceEvent(Event e,Track t,SequenceParams params,Proportion length) throws Exception
  {
    switch (e.geteventtype())
      {
        case Event.EVENT_NOTE:
          sequenceEvent((NoteEvent)e,t,params,length);
          break;
        case Event.EVENT_REST:
          sequenceEvent((RestEvent)e,t,params,length);
          break;
        case Event.EVENT_PROPORTION:
          sequenceEvent((ProportionEvent)e,t,params,length);
          break;
        case Event.EVENT_MULTIEVENT:
          sequenceEvent((MultiEvent)e,t,params,length);
          break;
        default:
          break;
      }
  }

  void sequenceEvent(RenderedEvent re,Track t,SequenceParams params) throws Exception
  {
    if (!params.inTie)
      {
        params.beginTie=params.inTie=re.getTieInfo().firstEventNum!=-1;
        params.doubleTied=false;
        params.endTie=false;
      }
    else
      {
        params.beginTie=false;
        params.doubleTied=re.doubleTied();
        params.endTie=!params.doubleTied;
      }

    params.curTime=Proportion.sum(params.sectionStartTime,re.getmusictime());
    sequenceEvent(re.getEvent(),t,params,re.getMusicLength());

    if (params.endTie)
      params.inTie=false;
  }

/*------------------------------------------------------------------------
Methods: void addMeasureMarkers(ScoreRenderer rs,Track t,Proportion startTime)
Purpose: Insert meta-event markers for measure beginnings
Parameters:
  Input:  ScoreRenderer rs     - section to calculate
          Proportion startTime - starting time of section
  Output: Track t              - track to update with events
  Return: -
------------------------------------------------------------------------*/

  void addMeasureMarkers(ScoreRenderer rs,Track t,Proportion startTime) throws Exception
  {
    Proportion curTime=new Proportion(startTime);

    for (int mi=rs.getFirstMeasureNum(); mi<=rs.getLastMeasureNum(); mi++)
      {
        /* add marker message for measure start */
        MetaMessage measureMsg=new MetaMessage();
        byte[] msgData=("m"+mi).getBytes();
        measureMsg.setMessage(MIDI_EVENT_MARKER,msgData,msgData.length);
        t.add(new MidiEvent(measureMsg,(long)(curTime.toDouble()*TICKS_PER_MINIM)));

        /* advance timer */
        MeasureInfo m=rs.getMeasure(mi);
        curTime.add(Proportion.quotient(new Proportion(m.numMinims,1),m.defaultTempoProportion));
      }
  }

/*------------------------------------------------------------------------
Method:  void exportMIDIFile(String fn)
Purpose: Save to MIDI file
Parameters:
  Input:  String fn - filename
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void exportMIDIFile(String fn) throws Exception
  {
    System.out.println("Wrote "+MidiSystem.write(sequenceData,1,new File(fn))+" bytes");
  }

/*------------------------------------------------------------------------
Method:  void play/stop()
Purpose: Play back or stop pre-loaded music
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  int currentlyPlaying=0;

  public void play()
  {
    play(0);
  }

  public void play(int measureNum)
  {
    if (sequenceData==null)
      return;

    try
      {
        Synthesizer synthesizer;

        sequencer=MidiSystem.getSequencer();
        if (sequencer==null)
          throw new Exception("No sequencer available from system (may already be in use)");
        sequencer.open();
        sequencer.setSequence(sequenceData);

        if (!(sequencer instanceof Synthesizer))
          {
            /* sequencer is not a synthesizer
               open default MIDI synth and chain to output of sequencer */
            synthesizer=MidiSystem.getSynthesizer();
            synthesizer.open();
            sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
          }
        else
          synthesizer=(Synthesizer)sequencer;

        /* set callback functions for playback */
        playbackListener=new MetaEventListener()
        {
          public void meta(MetaMessage event)
          {
            switch (event.getType())
              {
                case MIDI_EVENT_MARKER:
                  byte[] msgData=event.getData();
                  parentWin.MIDIMeasureStarted(
                    Integer.valueOf(new String(msgData,1,msgData.length-1)).intValue());
                  break;
                case MIDI_EVENT_ENDOFTRACK:
                  parentWin.MIDIEnded();
                  stop();
                  break;
              }
          }
        };
        sequencer.addMetaEventListener(playbackListener);

        sequencer.setTickPosition(TICKS_PER_MINIM*calcNumMinims(measureNum));
        sequencer.setTempoFactor(1.0f);
        sequencer.setTempoInBPM(DEFAULT_BPM);
//        sequencer.setTempoInMPQ(DEFAULT_MPQ);

        /* set volume */
        for (MidiChannel mc : synthesizer.getChannels())
          mc.controlChange(VOLUME_CONTROLLER,(int)(DEFAULT_GAIN*127.0));

        sequencer.start();
        currentlyPlaying++;
      }
    catch (Exception e)
      {
        JOptionPane.showMessageDialog(parentWin,"Error initializing MIDI system: "+e,"Error",JOptionPane.ERROR_MESSAGE);
      }
  }

  void stop()
  {
    if (currentlyPlaying<=0)
      return;
    currentlyPlaying--;
    sequencer.stop();
    sequencer.close();
    sequencer.removeMetaEventListener(playbackListener);
  }

/*------------------------------------------------------------------------
Methods: long calcNumMinims(int measureNum)
Purpose: Calculate the number of minims passed in the entire score at the
         beginning of a given measure
Parameters:
  Input:  int measureNum - measure number
  Output: -
  Return: music time in minims
------------------------------------------------------------------------*/

  long calcNumMinims(int measureNum)
  {
    long totalMinims=0;

    for (ScoreRenderer rs : renderedSections)
      {
        for (int mi=rs.getFirstMeasureNum(); mi<=rs.getLastMeasureNum(); mi++)
          {
            if (mi>=measureNum)
              return totalMinims;

            MeasureInfo m=rs.getMeasure(mi);
            totalMinims+=(long)(m.numMinims/m.defaultTempoProportion.toDouble());
          }
        totalMinims+=REST_BETWEEN_SECTIONS;
      }

    return totalMinims;
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public boolean currentlyPlaying()
  {
    return currentlyPlaying>0;
  }
}