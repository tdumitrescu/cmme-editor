/*----------------------------------------------------------------------*/
/*

        Module          : TextEditorFrame.java

        Package         : Editor

        Classes Included: TextEditorFrame

        Purpose         : Provide interface for inputting/manipulating
                          texting (original and modern) in score

        Programmer      : Ted Dumitrescu

        Date Started    : 8/19/08 (moved from EditorWin)

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package Editor;

/*----------------------------------------------------------------------*/
/* Imported packages */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

/*------------------------------------------------------------------------
Class:   TextEditorFrame
Extends: JDialog
Purpose: GUI for manipulating texting in score
------------------------------------------------------------------------*/

public class TextEditorFrame extends JDialog implements ActionListener
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  EditorWin owner;

  JTextPane origScrapTextArea,
            modScrapTextArea;
  JButton   setSyllButton,
            removeSyllButton,
            noteLeftButton,
            noteRightButton,
            insertPhraseButton;
  JComboBox loadOrigTextComboBox,
            loadModTextComboBox;
  JButton   loadOrigTextButton,
            loadModTextButton;

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  int get[Syllable|Phrase][Begin|End](String text,int curPos)
Purpose: Get index of current syllable/phrase beginning or end in a string
         (delimited in original texting by @, in modern texting by space or -)
Parameters:
  Input:  String text - text to check
          int curPos  - index within string to check
  Output: -
  Return: index of beginning or end
------------------------------------------------------------------------*/

  static boolean isOriginalTextChar(char c)
  {
    if (c=='@' || c=='\n')
      return false;
    return true;
  }

  static int getPhraseBegin(String text,int curPos)
  {
    for (; curPos>0 && isOriginalTextChar(text.charAt(curPos-1)); curPos--)
      ;
    return curPos;
  }

  static int getPhraseEnd(String text,int curPos)
  {
    for (; curPos<text.length() && isOriginalTextChar(text.charAt(curPos)); curPos++)
      ;
    return curPos;
  }

  static boolean isModernTextChar(char c)
  {
    if (c==' ' || c=='-' || c=='\n')
      return false;
    return true;
  }

  static int getSyllableBegin(String text,int curPos)
  {
    for (; curPos>0 && isModernTextChar(text.charAt(curPos-1)); curPos--)
      ;
    return curPos;
  }

  static int getSyllableEnd(String text,int curPos)
  {
    for (; curPos<text.length() && isModernTextChar(text.charAt(curPos)); curPos++)
      ;
    return curPos;
  }

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: TextEditorFrame(EditorWin owner)
Purpose:     Init GUI
Parameters:
  Input:  EditorWin owner - parent frame
  Output: -
------------------------------------------------------------------------*/

  public TextEditorFrame(EditorWin owner)
  {
    super(owner,"Text",false);
    this.owner=owner;

    Container tecp=getContentPane();
    tecp.setLayout(new BoxLayout(tecp,BoxLayout.Y_AXIS));

    /* original/modern texting choice */
    JTabbedPane textingChoiceTabs=new JTabbedPane();
    JPanel      originalTextingPanel,modernTextingPanel;

    /* original texting panel */
    originalTextingPanel=new JPanel();
    originalTextingPanel.setLayout(new BoxLayout(originalTextingPanel,BoxLayout.Y_AXIS));

    JPanel scrapTextPanel=new JPanel();
    scrapTextPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Scrap text area"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    origScrapTextArea=new JTextPane();
    origScrapTextArea.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty,"\n");
    JScrollPane origScrapTextScrollPane=new JScrollPane(origScrapTextArea);
    origScrapTextScrollPane.setPreferredSize(new Dimension(400,250));
    origScrapTextScrollPane.setMinimumSize(new Dimension(10,10));
    scrapTextPanel.add(origScrapTextScrollPane);

    loadOrigTextButton=new JButton("Load scrap text from voice:");
    loadOrigTextComboBox=new JComboBox();
    owner.loadVoiceNamesInComboBox(loadOrigTextComboBox);
    Box topButtonPane=Box.createHorizontalBox();
    topButtonPane.add(loadOrigTextButton);
    topButtonPane.add(Box.createHorizontalStrut(10));
    topButtonPane.add(loadOrigTextComboBox);
    topButtonPane.add(Box.createHorizontalGlue());
    topButtonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    insertPhraseButton=new JButton("Insert phrase");
    Box bottomButtonPane=Box.createHorizontalBox();
    bottomButtonPane.add(Box.createHorizontalGlue());
    bottomButtonPane.add(insertPhraseButton);
    bottomButtonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    originalTextingPanel.add(topButtonPane);
    originalTextingPanel.add(scrapTextPanel);
    originalTextingPanel.add(bottomButtonPane);

    /* modern texting panel */
    modernTextingPanel=new JPanel();
    modernTextingPanel.setLayout(new BoxLayout(modernTextingPanel,BoxLayout.Y_AXIS));

    loadModTextButton=new JButton("Load scrap text from voice:");
    loadModTextComboBox=new JComboBox();
    owner.loadVoiceNamesInComboBox(loadModTextComboBox);
    topButtonPane=Box.createHorizontalBox();
    topButtonPane.add(loadModTextButton);
    topButtonPane.add(Box.createHorizontalStrut(10));
    topButtonPane.add(loadModTextComboBox);
    topButtonPane.add(Box.createHorizontalGlue());
    topButtonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    scrapTextPanel=new JPanel();
    scrapTextPanel.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createTitledBorder("Scrap text area"),
      BorderFactory.createEmptyBorder(5,5,5,5)));
    modScrapTextArea=new JTextPane();
    modScrapTextArea.getDocument().putProperty(DefaultEditorKit.EndOfLineStringProperty,"\n");
    JScrollPane scrapTextScrollPane=new JScrollPane(modScrapTextArea);
    scrapTextScrollPane.setPreferredSize(new Dimension(400,250));
    scrapTextScrollPane.setMinimumSize(new Dimension(10,10));
    scrapTextPanel.add(scrapTextScrollPane);

    /* action buttons */
    removeSyllButton=new JButton("Remove Syllable");
    setSyllButton=new JButton("Set Syllable");
    noteLeftButton=new JButton("<");
    noteRightButton=new JButton(">");
    bottomButtonPane=Box.createHorizontalBox();
    bottomButtonPane.add(noteLeftButton);
    bottomButtonPane.add(Box.createHorizontalStrut(10));
    bottomButtonPane.add(new JLabel("Note"));
    bottomButtonPane.add(Box.createHorizontalStrut(10));
    bottomButtonPane.add(noteRightButton);
    bottomButtonPane.add(Box.createHorizontalGlue());
    bottomButtonPane.add(removeSyllButton);
    bottomButtonPane.add(Box.createHorizontalStrut(10));
    bottomButtonPane.add(setSyllButton);
    bottomButtonPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    modScrapTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,java.awt.Event.CTRL_MASK),"SetSyllable");
    modScrapTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,java.awt.Event.CTRL_MASK),"NoteRight");
    modScrapTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT,java.awt.Event.CTRL_MASK),"NoteRight");
    modScrapTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,java.awt.Event.CTRL_MASK),"NoteLeft");
    modScrapTextArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT,java.awt.Event.CTRL_MASK),"NoteLeft");
    modScrapTextArea.getActionMap().put("SetSyllable",
      new AbstractAction()
        {
          public void actionPerformed(ActionEvent e)
          {
            setSyllButton.doClick();
          }
        });
    modScrapTextArea.getActionMap().put("NoteRight",
      new AbstractAction()
        {
          public void actionPerformed(ActionEvent e)
          {
            noteRightButton.doClick();
          }
        });
    modScrapTextArea.getActionMap().put("NoteLeft",
      new AbstractAction()
        {
          public void actionPerformed(ActionEvent e)
          {
            noteLeftButton.doClick();
          }
        });

    modernTextingPanel.add(topButtonPane);
    modernTextingPanel.add(scrapTextPanel);
    modernTextingPanel.add(bottomButtonPane);

//    tecp.add(voiceTextAreasPanel);
    textingChoiceTabs.addTab("Original texting",originalTextingPanel);
    textingChoiceTabs.addTab("Modern texting",modernTextingPanel);
    tecp.add(textingChoiceTabs);

//    disableTextEditorInsertPhrase();
    disableSetSyllable();
    disableRemoveSyllable();

    registerListeners();
    pack();
  }

/*------------------------------------------------------------------------
Method:  void [un]registerListeners()
Purpose: Add and remove event listeners
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  protected void registerListeners()
  {
    insertPhraseButton.addActionListener(this);
    removeSyllButton.addActionListener(this);
    setSyllButton.addActionListener(this);
    noteLeftButton.addActionListener(this);
    noteRightButton.addActionListener(this);
    loadOrigTextButton.addActionListener(this);
    loadModTextButton.addActionListener(this);
  }

  protected void unregisterListeners()
  {
    insertPhraseButton.removeActionListener(this);
    removeSyllButton.removeActionListener(this);
    setSyllButton.removeActionListener(this);
    noteLeftButton.removeActionListener(this);
    noteRightButton.removeActionListener(this);
    loadOrigTextButton.removeActionListener(this);
    loadModTextButton.removeActionListener(this);
  }

/*------------------------------------------------------------------------
Method:     void actionPerformed(ActionEvent event)
Implements: ActionListener.actionPerformed
Purpose:    Check for action types in tools and take appropriate action
Parameters:
  Input:  ActionEvent event - event to handle
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void actionPerformed(ActionEvent event)
  {
    Object item=event.getSource();

    if (item==insertPhraseButton)
      insertOriginalTextPhrase();
    else if (item==setSyllButton)
      applyTextSyllableToNote();
    else if (item==removeSyllButton)
      removeTextSyllableFromNote();
    else if (item==noteLeftButton)
      owner.highlightPreviousNote();
    else if (item==noteRightButton)
      owner.highlightNextNote();
    else if (item==loadOrigTextButton)
      loadOriginalText(owner.voiceOrigTextToStr(loadOrigTextComboBox.getSelectedIndex()));
    else if (item==loadModTextButton)
      loadModernText(owner.voiceModTextToStr(loadModTextComboBox.getSelectedIndex()));
  }

/*------------------------------------------------------------------------
Method:  void [enable|disable]*()
Purpose: Enable or disable capability to use individual buttons
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public void enableInsertPhrase()
  {
    insertPhraseButton.setEnabled(true);
  }

  public void disableInsertPhrase()
  {
    insertPhraseButton.setEnabled(false);
  }

  public void enableSetSyllable()
  {
    setSyllButton.setEnabled(true);
  }

  public void disableSetSyllable()
  {
    setSyllButton.setEnabled(false);
  }

  public void enableRemoveSyllable()
  {
    removeSyllButton.setEnabled(true);
  }

  public void disableRemoveSyllable()
  {
    removeSyllButton.setEnabled(false);
  }

/*------------------------------------------------------------------------
Method:  void insertOriginalTextPhrase()
Purpose: Get current phrase from original scrap text area and insert as
         new OriginalText event
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void insertOriginalTextPhrase()
  {
    String text=origScrapTextArea.getText();
    int    curPos=origScrapTextArea.getCaretPosition(),
           phraseBegin=getPhraseBegin(text,curPos),
           phraseEnd=getPhraseEnd(text,curPos);
    if (phraseEnd<=phraseBegin)
      return;
    owner.addOriginalText(text.substring(phraseBegin,phraseEnd));

    /* highlight next phrase */
    for (curPos=phraseEnd+1; curPos<text.length() && !isOriginalTextChar(text.charAt(curPos)); curPos++)
      ;
    if (curPos>=text.length())
      origScrapTextArea.setCaretPosition(text.length());
    else
      {
        origScrapTextArea.setSelectionStart(getPhraseBegin(text,curPos));
        origScrapTextArea.setSelectionEnd(getPhraseEnd(text,curPos));
      }
    origScrapTextArea.requestFocusInWindow();
  }

/*------------------------------------------------------------------------
Method:  void load[Original|Modern]Text(String insertText)
Purpose: Insert text into scrap text area at current caret position
Parameters:
  Input:  String insertText - text to insert
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void loadOriginalText(String insertText)
  {
    loadTextIntoArea(insertText,origScrapTextArea);
  }

  void loadModernText(String insertText)
  {
    loadTextIntoArea(insertText,modScrapTextArea);
  }

  void loadTextIntoArea(String insertText,JTextPane textArea)
  {
    String origText=textArea.getText();
    int    curPos=textArea.getCaretPosition(),
           si1=curPos>0 ? curPos-1 : 0;
    String newText=origText.substring(0,si1)+insertText+origText.substring(curPos);
    textArea.setText(newText);
    textArea.setCaretPosition(curPos);
    textArea.requestFocusInWindow();
  }

/*------------------------------------------------------------------------
Method:  void applyTextSyllableToNote()
Purpose: Get current syllable from modern scrap text area and apply to highlighted
         note
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void applyTextSyllableToNote()
  {
    String text=modScrapTextArea.getText();
    int    curPos=modScrapTextArea.getCaretPosition(),
           syllBegin=getSyllableBegin(text,curPos),
           syllEnd=getSyllableEnd(text,curPos);
    if (syllEnd<=syllBegin)
      return;
    boolean wordEnd=syllEnd>=text.length() || text.charAt(syllEnd)!='-';

    owner.setNoteSyllable(text.substring(syllBegin,syllEnd),wordEnd);

    /* highlight next syllable */
    for (curPos=syllEnd+1; curPos<text.length() && !isModernTextChar(text.charAt(curPos)); curPos++)
      ;
    if (curPos>=text.length())
      modScrapTextArea.setCaretPosition(text.length());
    else
      {
        modScrapTextArea.setSelectionStart(getSyllableBegin(text,curPos));
        modScrapTextArea.setSelectionEnd(getSyllableEnd(text,curPos));
      }
    modScrapTextArea.requestFocusInWindow();
  }

/*------------------------------------------------------------------------
Method:  void removeTextSyllableFromNote()
Purpose: Delete syllable on highlighted note
Parameters:
  Input:  -
  Output: -
  Return: -
------------------------------------------------------------------------*/

  void removeTextSyllableFromNote()
  {
    owner.setNoteSyllable(null,false);
    modScrapTextArea.requestFocusInWindow();
  }
}