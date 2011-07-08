/*----------------------------------------------------------------------*/
/*

        Module          : XMLReader.java

        Package         : DataStruct

        Classes Included: XMLReader,ProgressSAXBuilder

        Purpose         : set up generic XML parser

        Programmer      : Ted Dumitrescu

        Date Started    : 2/23/05

Updates:
3/14/05: made XML validation optional (to avoid requiring xercesImpl.jar,
         for quick client applet loading)
1/24/06: added custom entity resolver so that cmme.xsd is always loaded
         from the same location (to avoid requiring a separate copy in each
         directory with a .cmme.xml file)
6/30/10: added noEntityBuilder for reading MusicXML documents while
         ignoring DOCTYPE declarations

                                                                        */
/*----------------------------------------------------------------------*/

package DataStruct;

/*----------------------------------------------------------------------*/
/* Imported packages */

import org.jdom.*;
import org.jdom.input.*;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Class:   XMLReader
Extends: -
Purpose: Sets up XML parsing
------------------------------------------------------------------------*/

public class XMLReader
{
/*----------------------------------------------------------------------*/
/* Class variables */

  static final String ValParserName="org.apache.xerces.parsers.SAXParser",
                      NonvalParserName="gnu.xml.aelfred2.SAXDriver";
  static SAXBuilder   builder,
                      noEntityBuilder=null;
  static boolean      inited=false;

/*----------------------------------------------------------------------*/

/*----------------------------------------------------------------------*/
/* Instance variables */

/*----------------------------------------------------------------------*/

/*------------------------------------------------------------------------
Inner Class: CMMEEntityResolver
Implements:  org.xml.sax.EntityResolver
Purpose:     Resolves entities so that the CMME music schema is always
             loaded from the same location (regardless of the location of
             individual .cmme.xml docs)
------------------------------------------------------------------------*/

  static class CMMEEntityResolver implements EntityResolver
  {
    /* base data directory for finding CMME Schema file */
    String database;

/*------------------------------------------------------------------------
Constructor: CMMEEntityResolver(String db)
Purpose:     Initialize custom entity resolver
Parameters:
  Input:  String db - base data directory
  Output: -
  Return: -
------------------------------------------------------------------------*/

    public CMMEEntityResolver(String db)
    {
      database=db;
    }

    public InputSource resolveEntity(String publicId,String systemId)
    {
      if (systemId.matches(".*cmme\\.xsd"))
        return new InputSource(database+"music/cmme.xsd");
      return null;
    }
  }

/*----------------------------------------------------------------------*/
/* Class methods */

/*------------------------------------------------------------------------
Method:  void initparser(String db,boolean validate)
Purpose: Create XML parser
Parameters:
  Input:  String db        - base data directory
          boolean validate - whether to validate XML input
  Output: -
  Return: -
------------------------------------------------------------------------*/

  public static void initparser(String db,boolean validate)
  {
    if (validate)
      {
        /* validates based on XML Schema */
        builder=new SAXBuilder(ValParserName,true);
        builder.setFeature("http://apache.org/xml/features/validation/schema",
                           true);
        builder.setEntityResolver(new CMMEEntityResolver(db));
      }
    else
      builder=new SAXBuilder(NonvalParserName,false);
    inited=true;
  }

/*------------------------------------------------------------------------
Method:  SAXBuilder getparser()
Purpose: Return XML parser
Parameters:
  Input:  -
  Output: -
  Return: Parser
------------------------------------------------------------------------*/

  public static SAXBuilder getNonValidatingParser()
  {
    if (inited && !builder.getValidation())
      return builder;
    else
      return new SAXBuilder(NonvalParserName,false);
  }

  public static SAXBuilder getNoEntityParser()
  {
    if (noEntityBuilder==null)
      {
        noEntityBuilder=new SAXBuilder("org.apache.xerces.parsers.SAXParser",false);
        noEntityBuilder.setFeature(
          "http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
      }

    return noEntityBuilder;
  }

  public static SAXBuilder getparser()
  {
    if (inited)
      return builder;
    System.err.println("XML Reader called before initialization");
    return null;
  }

/*------------------------------------------------------------------------
Method:  [] get*Val(Element el)
Purpose: Parse value in XML node
Parameters:
  Input:  Element el - element with value
  Output: -
  Return: value represented in el
------------------------------------------------------------------------*/

  public static char getCharVal(Element el)
  {
    if (el==null)
      return ' ';
    String eVal=el.getText();
    if (eVal.length()<1)
      return ' ';
    return eVal.charAt(0);
  }

  public static char getCharVal(Object o)
  {
    return getCharVal((Element)o);
  }

  public static int getIntVal(Element el)
  {
    return el==null ? -1 : Integer.parseInt(el.getText());
  }

  public static int getIntVal(Object o)
  {
    return getIntVal((Element)o);
  }
}
