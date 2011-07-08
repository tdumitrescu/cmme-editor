/*----------------------------------------------------------------------*/
/*

        Module          : MusicTextSection.java

        Package         : DataStruct

        Classes Included: MusicTextSection

        Purpose         : Contents of one text section

        Programmer      : Ted Dumitrescu

        Date Started    : 2/14/07

        Updates         :

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*------------------------------------------------------------------------
Class:   MusicTextSection
Extends: MusicSection
Purpose: Contents of one text section
------------------------------------------------------------------------*/

public class MusicTextSection extends MusicSection
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  String sectionText;

/*----------------------------------------------------------------------*/
/* Instance methods */

/*------------------------------------------------------------------------
Constructor: MusicTextSection(String sectionText,[,boolean editorial])
Purpose:     Initialize section
Parameters:
  Input:  String sectionText - text of section
          boolean editorial  - whether this section is in a source (not
                               editorial) or not
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public MusicTextSection(String sectionText,boolean editorial)
  {
    initParams(editorial,MusicSection.TEXT);

    this.sectionText=sectionText;
  }

  public MusicTextSection(String sectionText)
  {
    this(sectionText,false);
  }

/*------------------------------------------------------------------------
Methods: get*() / is*()
Purpose: Routines to return attribute variables
Parameters:
  Input:  -
  Output: -
  Return: attribute variables
------------------------------------------------------------------------*/

  public String getSectionText()
  {
    return sectionText;
  }
}
