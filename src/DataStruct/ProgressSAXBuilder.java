/*----------------------------------------------------------------------*/
/*

        Module          : XMLReader.java

        Package         : DataStruct

        Classes Included: XMLReader,ProgressSAXBuilder

        Purpose         : set up generic XML parser

        Programmer      : Ted Dumitrescu

        Date Started    : 2/23/05

Updates:

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import org.jdom.*;
import org.jdom.input.*;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   ProgressSAXBuilder
Extends: org.jdom.input.SAXBuilder
Purpose: SAXBuilder which provides access to Locator (for updating progress bar)
------------------------------------------------------------------------*/

public class ProgressSAXBuilder extends SAXBuilder
{
/*----------------------------------------------------------------------*/
/* Instance variables */

  SAXHandler handler=null;

/*----------------------------------------------------------------------*/
/* Instance methods */

  public ProgressSAXBuilder(java.lang.String saxDriverClass,boolean validate)
  {
    super(saxDriverClass,validate);
  }

  protected SAXHandler createContentHandler()
  {
    handler=super.createContentHandler();
    return handler;
  }

  public org.xml.sax.Locator getDocumentLocator()
  {
    return handler.getDocumentLocator();
  }
}
