package com.prosc.fmpjdbc;

import sun.misc.BASE64Encoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/*
    Fmp360_JDBC is a FileMaker JDBC driver that uses the XML publishing features of FileMaker Server Advanced.
    Copyright (C) 2006  Prometheus Systems Consulting, LLC d/b/a 360Works

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

//FIX!! It's probably not a good idea, architecturally, to share instances of this class. You could certainly do other JDBC operations while in the middle of a result set, and that would hose everything. --jsb
/**
 * Created by IntelliJ IDEA.
 * User: jesse
 * Date: Apr 17, 2005
 * Time: 2:22:20 PM
 */
public class FmXmlRequest {
	private static final int SERVER_STREAM_BUFFERSIZE = 16384;
	private URL theUrl;
	private InputStream serverStream;
	private SAXParser xParser;
	private String authString;
	private String postPrefix = "";
	private Logger log = Logger.getLogger( FmXmlRequest.class.getName() );

	public FmXmlRequest(String protocol, String host, String url, int portNumber, String username, String password) throws MalformedURLException {
		this.theUrl = new URL(protocol, host, portNumber, url);
		if (username != null || password != null) {
			String tempString = username + ":" + password;
			authString = new BASE64Encoder().encode(tempString.getBytes());
		}
		try {
			xParser = javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser();
			setFeature( "http://xml.org/sax/features/validation", false );
			setFeature( "http://xml.org/sax/features/namespaces", false );
			setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
			log.finer( "Created an XML parser; class is: " + xParser.getClass() );
		} catch( ParserConfigurationException e ) {
			throw new RuntimeException( e );
		} catch ( SAXException e) {
			throw new RuntimeException(e);
		}
	}

	private void setFeature( String feature, boolean enabled ) {
		try {
			xParser.getXMLReader().setFeature( feature, enabled );
		} catch( SAXException e ) { //Ignore
			log.finer( "Could not enable feature " + feature + " because of a SAXException: " + e );
		}
	}

	public void setPostPrefix(String s) {
		postPrefix = s;
	}


	public void doRequest(String postArgs) throws IOException, FileMakerException {
		if (serverStream != null) throw new IllegalStateException("You must call closeRequest() before sending another request.");
		HttpURLConnection theConnection = (HttpURLConnection) theUrl.openConnection();
		theConnection.setUseCaches(false);
		if (authString != null) theConnection.addRequestProperty("Authorization", "Basic " + authString);
		if (postArgs != null) {
			postArgs = postPrefix + postArgs;
			log.log(Level.FINE, theUrl + "?" + postArgs);
			theConnection.setDoOutput(true);
			PrintWriter out = new PrintWriter( theConnection.getOutputStream() );
			//out.println("-db=Contacts&-lay=Contacts&-findall=");
			out.print(postPrefix);
			out.println(postArgs);
			out.close();
		}

		try {
			int httpStatusCode = theConnection.getResponseCode();
			if( httpStatusCode == 401 ) throw new HttpAuthenticationException( theConnection.getResponseMessage() );
			if( httpStatusCode == 501 ) throw new IOException("Server returned a 501 (Not Implemented) error. If you are using FileMaker 6, be sure to add ?&fmversion=6 to the end of your JDBC URL.");
			serverStream = new BufferedInputStream(theConnection.getInputStream(), SERVER_STREAM_BUFFERSIZE);
		} catch( IOException e ) {
			if( e.getCause() instanceof FileNotFoundException ) {
				String message = "Remote URL " + e.getCause().getMessage() + " could not be located.";
				String missingFileName = e.getCause().getMessage();
				if( missingFileName.endsWith("FMPXMLRESULT.xml") ) message += " If you are using FileMaker 6, be sure to add ?&fmversion=6 to the end of your JDBC URL.";
					throw new IOException(message);
			}
			else throw e;
		}
		try {
			readResult();
			//System.out.println(IOUtils.inputStreamAsString(serverStream));
			//throw new SAXException("Just testing");
		} catch (SAXException e) {
			throw new RuntimeException(e); //FIX!! Better error handling than just rethrowing?
		} catch( RuntimeException e ) {
			Throwable t = e.getCause();
			if( t instanceof FileMakerException ) throw (FileMakerException)t;
			else throw e;
		}
	}

	public void closeRequest() {
		useSelectFields = false;
		fieldDefinitions = null;
		usedFieldArray = null;
		allFieldNames = new ArrayList();
		fmTable = null;
        foundCount = 0;
		if (serverStream != null)
			try {
				serverStream.close();
				serverStream = null;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
	}

	protected void finalize() throws Throwable {
		if (serverStream != null) serverStream.close();
		super.finalize();
	}

	private void readResult() throws IOException, SAXException {
		InputStream streamToParse;
		streamToParse = serverStream;
		InputSource input = new InputSource(streamToParse);
		input.setSystemId("http://" + theUrl.getHost() + ":" + theUrl.getPort());
		xParser.parse( input, xmlHandler );
	}

	public String getProductVersion() {
		return productVersion;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public int getFoundCount() {
		return foundCount;
	}

	public FmRecord getLastRecord() {
		return currentRow;
	}

	public Iterator getRecordIterator() {
		return records.iterator(); //FIX!! Do this on a row-by-row basis instead of storing the whole list in memory
	}

	public FmFieldList getFieldDefinitions() {
		return fieldDefinitions;
	}


	/*
	In this class we have a reference to FmFieldList called fieldDefinitions (see below). In all the uses of this class,
	the state of this reference falls into 3 categories. Either it is set by the setSelectFields() method below,
	in which case it has one or more FmFields or it might just contain 1 FmField which contains an asterisk '*" for 'select *'.
	The last case is when fieldDefinitions is initially null. This will be the case when it is not set by setSelectFields().
	i.e  for updates and inserts etc.
	*/

	private FmFieldList fieldDefinitions;
	private FmTable fmTable;
	private boolean useSelectFields = false;

	private String productVersion;
	private String databaseName;
	private int foundCount = -1;
	private FmRecord currentRow;
	private List records = new LinkedList(); //FIX!! Temporary for development - get rid of in final version
	private transient StringBuffer currentData = new StringBuffer(255);
	private transient int insertionIndex;

	private static transient int code = 0;
	private static Integer IGNORE_NODE = new Integer(code++);
	private static Integer DATA_NODE = new Integer(code++);
	private static Integer ERROR_NODE = new Integer(code++);

	private int[] usedFieldArray; // The array used by the characters() method in xmlHandler.
	private List allFieldNames = new ArrayList(); // a list of Strings.  All the Field names inside the METADATA tag.


	// ---XML parsing SAX implementation ---
	private DefaultHandler xmlHandler = new org.xml.sax.helpers.DefaultHandler() {
		private Integer currentNode = null;
		private int columnIndex;
		private InputSource emptyInput = new InputSource( new ByteArrayInputStream(new byte[0]) );

		public void fatalError(SAXParseException e) throws SAXException {
			log.log(Level.SEVERE, String.valueOf(e));
			super.fatalError(e);
		}

		/** This is necessary to work around a bug in the Crimson XML parser, which is used in the 1.4 JDK. Crimson
		 * cannot handle relative HTTP URL's, which is what FileMaker uses for it's DTDs: "/fmi/xml/FMPXMLRESULT.dtd"
		 * By returning an empty value here, we short-circuit the DTD lookup process.
		 */
		public InputSource resolveEntity( String publicId, String systemId ) {
			return emptyInput;
		}

		public void warning( SAXParseException e ) throws SAXException {
			super.warning( e );	//To change body of overridden methods use File | Settings | File Templates.
		}

		public void error( SAXParseException e ) throws SAXException {
			super.error( e );	//To change body of overridden methods use File | Settings | File Templates.
		}

		public void startDocument() {
			log.log(Level.FINEST, "Start parsing response");
			records = new LinkedList();
			currentNode = null;
		}

		public void startElement(String uri, String xlocalName, String qName, Attributes attributes) {
			// Frequently repeated nodes
			if ("DATA".equals(qName)) { //FIX! What if we have multiple DATA nodes per COL, ie. repeating fields? Our insertionIndex won't change? --jsb
				currentNode = (insertionIndex>-1) ? DATA_NODE : IGNORE_NODE;
				//currentData.delete( 0, currentData.length() );
				currentData = new StringBuffer( 255 );
			} else if ("COL".equals(qName)) {
				columnIndex++;
				insertionIndex = usedFieldArray[columnIndex];
			} else if ("ROW".equals(qName)) {
				//dt.markTime("  Starting row");

				currentRow = new FmRecord(fieldDefinitions, Integer.valueOf(attributes.getValue("RECORDID")), Integer.valueOf(attributes.getValue("MODID")));
				columnIndex = -1;
			}
			// One-shot nodes
			else if ("FIELD".equals(qName)) {
				String fieldName = attributes.getValue("NAME");

				String fieldType = attributes.getValue("TYPE");
				FmFieldType theType = (FmFieldType)FmFieldType.typesByName.get(fieldType);
				boolean allowsNulls = "YES".equals(attributes.getValue("EMPTYOK"));

				allFieldNames.add(fieldName); // allFieldNames is used in endElement() together with fieldDefinitions to construct the usedFieldArray

				if (useSelectFields) {

					int columnIndex;
					if ((columnIndex = fieldDefinitions.indexOfFieldWithColumnName(fieldName)) > -1) { // set the type and nullable if this is a field in the field definitions

						FmField fmField = fieldDefinitions.get(columnIndex);
						fmField.setType(theType);
						fmField.setNullable(allowsNulls);
					}

				} else {
					FmField fmField = new FmField(fmTable, fieldName, fieldName, theType, allowsNulls);
					fieldDefinitions.add(fmField);
				}

			} else if ("RESULTSET".equals(qName)) {
				foundCount = Integer.valueOf(attributes.getValue("FOUND")).intValue();
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "Resultset size: " + foundCount);
				}
			} else if ("PRODUCT".equals(qName)) {
				databaseName = attributes.getValue("NAME");
				productVersion = attributes.getValue("VERSION");
			} else if ("DATABASE".equals(qName)) {
				fmTable =  new FmTable( attributes.getValue("NAME") );

				if (fieldDefinitions == null) {
					fieldDefinitions = new FmFieldList();
				}


			} else if ("ERRORCODE".equals(qName)) {
				currentNode = ERROR_NODE;
			}
		}

		public void endElement(String uri, String localName, String qName) {
			if( "DATA".equals(qName) ) {
				if ( currentNode == DATA_NODE ) {
					currentRow.setRawValue(currentData.toString(), insertionIndex);
				}
			}
			if ("ROW".equals(qName)) {
				records.add(currentRow);
				//if( FmConnection.getDebugLevel() >= 3 ) System.out.println("Finished record: " + ++currentRowIndex);
			}
			if ("METADATA".equals(qName)) { // Create the usedorder array.  This is done once.
				usedFieldArray = new int[allFieldNames.size()];

				int i = 0;
				Iterator it = allFieldNames.iterator();

				while ( it.hasNext() ) {
					String aFieldName = (String)it.next();

					int columnIndex;

					if ( (columnIndex = fieldDefinitions.indexOfFieldWithColumnName(aFieldName)) > -1) {
						// Get the index of the fieldName w.r.t fieldDefinitions, and put that value into the usedFieldArray
						usedFieldArray[i] = columnIndex;
					} else {
						usedFieldArray[i] = -1; // This field columnName will not be used.
					}
					i++;
				}
			}
		}

		public void characters(char ch[], int start, int length) {
			if (currentNode == DATA_NODE) {
				currentData.append( ch, start, length );
			} else if (currentNode == ERROR_NODE) {
				if (length == 1 && ch[start] == '0'); //Error code is zero, proceed
				else {
					String errorCode = new String(ch, start, length);
					if( "401".equals( errorCode) ) {
						//Ignore, this means no results
					} else {
						FileMakerException fileMakerException = FileMakerException.exceptionForErrorCode( Integer.valueOf(errorCode) );
						log.log(Level.WARNING, fileMakerException.toString());
						throw new RuntimeException( fileMakerException );
					}
				}
			}
		}
	};


	/*
	*  Use this to set the fields that are actually used in the select statement.
	*  The data we get from filmaker contains all the fields so we need to parse it appropriately.
	*/
	public void setSelectFields(FmFieldList selectFields) {
		fieldDefinitions = selectFields;

		// Code will use select fields if they exist and the first one is not an asterisk
		if ("*".equals(fieldDefinitions.get(0).getColumnName() ))
			fieldDefinitions.getFields().remove(0);
		else
			useSelectFields = true;
	}

	public static class HttpAuthenticationException extends IOException {
		public HttpAuthenticationException(String s) {
			super(s);
		}
	}


	public static void main(String[] args) throws IOException, FileMakerException {
		FmXmlRequest request;
		//request = new FmXmlRequest("http", "hercules.360works.com", "/fmi/xml/FMPXMLRESULT.xml", 80, null, null);
		//request.doRequest("-db=Contacts&-lay=Contacts&-findall");
		//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "fmp.360works.com", "/FMPro?-db=Names&-format=-fmp_xml&-findall", 4000);
		//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "fmp.360works.com", "/FMPro?-db=Insertions&-lay=AMSLogic&-format=-fmp_xml&-findall", 4000, "exchange", "waffle");
		//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "localhost", "/fmi/xml/FMPXMLRESULT.xml?-db=Contacts&-lay=Contacts&-findall", 3000, null, null);

		request = new FmXmlRequest("http", "orion.360works.com", "/fmi/xml/FMPXMLRESULT.xml", 80, null, null);
		for (int n = 1; n <= 10; n++) {
			try {
				request.doRequest("-db=Contacts&-lay=Calc Test&-max=100&-findany");
			} finally {
				request.closeRequest();
			}
		}
	}

}
