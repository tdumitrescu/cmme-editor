package Gfx;

import java.util.*;

import DataStruct.*;

public class RenderedSonority
{
  LinkedList<Pitch>         pitches;
  LinkedList<RenderedEvent> rNotes; /* lowest to highest */
  Proportion                musicTime;

  public RenderedSonority()
  {
    pitches=new LinkedList<Pitch>();
    rNotes=new LinkedList<RenderedEvent>();
    musicTime=new Proportion(0,1);
  }

  public RenderedSonority(RenderedSonority toCopy)
  {
    pitches=new LinkedList<Pitch>(toCopy.pitches);
    rNotes=new LinkedList<RenderedEvent>(toCopy.rNotes);
    musicTime=toCopy.musicTime;
  }

  public RenderedSonority copyWithout(RenderedEvent re)
  {
    RenderedSonority newSonority=new RenderedSonority(this);
    newSonority.remove(re);
    return newSonority;
  }

  public void add(RenderedEvent re)
  {
    Event e=re.getEvent();
    switch (e.geteventtype())
      {
        case Event.EVENT_MULTIEVENT:
          for (Iterator i=((MultiEvent)re.getEvent()).iterator(); i.hasNext();)
            {
              Event ne=(Event)i.next();
              if (ne.geteventtype()==Event.EVENT_NOTE)
                insertNote(re,ne.getPitch());
            }
          break;
        case Event.EVENT_NOTE:
          insertNote(re,e.getPitch());
          break;
      }
  }

  public void remove(RenderedEvent re)
  {
    if (re==null)
      return;

    Event e=re.getEvent();
    switch (e.geteventtype())
      {
        case Event.EVENT_MULTIEVENT:
          for (Iterator i=((MultiEvent)re.getEvent()).iterator(); i.hasNext();)
            {
              Event ne=(Event)i.next();
              if (ne.geteventtype()==Event.EVENT_NOTE)
                removeNote(re,ne.getPitch());
            }
          break;
        case Event.EVENT_NOTE:
          removeNote(re,e.getPitch());
          break;
      }
  }

  void insertNote(RenderedEvent re,Pitch p)
  {
    int insertPos=calcInsertPos(p);
    rNotes.add(insertPos,re);
    pitches.add(insertPos,p);
  }

  void removeNote(RenderedEvent re,Pitch p)
  {   
    rNotes.remove(re);
    pitches.remove(p);
  }

  int calcInsertPos(Pitch p)
  {
    int pos;
    for (pos=0; pos<pitches.size(); pos++)
      if (p.isLowerThan(pitches.get(pos)))
        return pos;

    return pos;
  }


  public void setMusicTime(Proportion newMusicTime)
  {
    musicTime=newMusicTime;
  }

  public Proportion getMusicTime()
  {
    return musicTime;
  }

  public int getNumPitches()
  {
    return pitches.size();
  }

  public Pitch getPitch(int i)
  {
    return pitches.get(i);
  }

  public RenderedEvent getRenderedNote(int i)
  {
    return rNotes.get(i);
  }


  public String toString()
  {
    String s="Sonority at "+musicTime+":";
    for (Pitch p : pitches)
      s+=" "+p;
    return s;
  }
}
