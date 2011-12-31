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

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Feb 23, 2006
 * Time: 10:04:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class FmResultSetRequest extends FmRequest {
	private static final Logger log = Logger.getLogger( FmXmlRequest.class.getName() );
	private static final int SERVER_STREAM_BUFFERSIZE = 16384;

	private final URL theUrl;
	private final SAXParser xParser;
	private final String username;
	private final String authString;
	public String tableOccurrence;

	private InputStream serverStream;
	private String postPrefix = "";

	public FmResultSetRequest(String protocol, String host, String url, int portNumber, String username, String password) throws MalformedURLException {
		this.theUrl = new URL(protocol, host, portNumber, url);
		this.username = username;
		if (username != null || password != null) {
			String tempString = username + ":" + password;
			authString = new BASE64Encoder().encode(tempString.getBytes());
		} else {
			authString = null;
		}
		try {
			xParser = javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser();
			setFeature( "http://xml.org/sax/features/validation", false );
			setFeature( "http://xml.org/sax/features/namespaces", false );
			setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
			log.finest( "Created an XML parser; class is: " + xParser.getClass() );
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
			out.print(postPrefix);
			out.println(postArgs);
			out.close();
		}

		if( theConnection.getResponseCode() == 401 ) throw new FmXmlRequest.HttpAuthenticationException( theConnection.getResponseMessage(), username );
		serverStream = new BufferedInputStream(theConnection.getInputStream(), SERVER_STREAM_BUFFERSIZE);
		try {
			readResult();
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
		private String parsedXML = "";
		private int columnIndex;
		private int tabCount = 0;
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
			tabCount = 0;
		}

		public void startElement(String uri, String xlocalName, String qName, Attributes attributes) {
			if ("fmresultset".equals(qName)) {
				parsedXML += tabs(tabCount);
				parsedXML += "- <fmresultset version=\"" + attributes.getValue("version") + "\">";
				tabCount++;
			}
			else if ("error".equals(qName)) {
				parsedXML += tabs(tabCount);
				parsedXML += "<error code=\"" + attributes.getValue("code") + "\"";
			}
			else if ("product".equals(qName)) {
				parsedXML += tabs(tabCount);
				parsedXML += "<product build=\"" + attributes.getValue("build") + "\"" +
						" name=\"" + attributes.getValue("name") + "\"" +
						" version=\"" + attributes.getValue("version") + "\"";
			}
			else if ("datasource".equals(qName)) {
				parsedXML += tabs(tabCount);
				parsedXML += "<datasource database=\"" + attributes.getValue("database") + "\"" +
						" date-format=\"" + attributes.getValue("date-format") + "\"" +
						" layout=\"" + attributes.getValue("layout") + "\"" +
						" table=\"" + attributes.getValue("table") + "\"" +
						" time-format=\"" + attributes.getValue("time-format") + "\"" +
						" timestamp-format=\"" + attributes.getValue("timestamp-format") + "\"" +
						" total-count=\"" + attributes.getValue("total-count")+ "\"";

				if (fieldDefinitions == null) {
					fieldDefinitions = new FmFieldList();
				}

				tableOccurrence = attributes.getValue( "table" );

				fmTable =  new FmTable( attributes.getValue("database") );
			}
			else if ("metadata".equals(qName)) {
				parsedXML += tabs(tabCount);
				parsedXML += "- <metadata>" + "\n";
				tabCount++;
			}
			else if ("field-definition".equals(qName)) {
				parsedXML += tabs(tabCount);
				parsedXML += "<field-definition auto-enter=\"" + attributes.getValue("auto-enter") + "\"" +
						" global=\"" + attributes.getValue("global") + "\"" +
						" max-repeat=\"" + attributes.getValue("max-repeat") + "\"" +
						" name=\"" + attributes.getValue("name") + "\"" +
						" not-empty=\"" + attributes.getValue("not-empty") + "\"" +
						" result=\"" + attributes.getValue("result") + "\"" +
						" type=\"" + attributes.getValue("type")+ "\"";

				String fieldName = attributes.getValue("name");
				String fieldTypeName = attributes.getValue("result");
				FmFieldType fieldType = (FmFieldType) FmFieldType.typesByName.get(fieldTypeName.toUpperCase());
				boolean allowsNulls = "no".equals(attributes.getValue("not-empty"));
				boolean readOnly = "calculation".equals(attributes.getValue("type") ) || "summary".equals( attributes.getValue("type") );
				boolean autoEnter = "yes".equals( attributes.getValue( "auto-enter" ) );

				FmField field = new FmField(fmTable, fieldName, fieldName, fieldType, allowsNulls, readOnly, autoEnter );
				fieldDefinitions.add(field);
			}
			else if ("resultset".equals(qName)) {
				parsedXML += tabs(tabCount);
				parsedXML += "<field-definition count=\"" + attributes.getValue("count") + "\"" +
						" fetch-size=\"" + attributes.getValue("fetch-size")+ "\"";
			}
		}

		public void endElement(String uri, String localName, String qName) {
			if ("fmresultset".equals(qName)) {
				parsedXML += "</fmresultset>" + "\n";
			}
			else if ("error".equals(qName)) {
				parsedXML += "/>" + "\n";
			}
			else if ("product".equals(qName)) {
				parsedXML += "/>";
			}
			else if ("datasource".equals(qName)) {
				parsedXML += "/>" + "\n";
			}
			else if ("metadata".equals(qName)) {
				tabCount--;
				parsedXML += tabs(tabCount);
				parsedXML += "</metadata>" + "\n";
			}
			else if ("field-definition".equals(qName)) {
				parsedXML += "/>" + "\n";
			}
			else if ("resultset".equals(qName)) {
				parsedXML += "/>" + "\n";
				tabCount--;
			}
		}

		private String tabs(int numTabs) {
			String tabs = "";

			for (int i = 0; i < numTabs; i++) {
				tabs += "     ";
			}

			return tabs;
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

	/*public static class HttpAuthenticationException extends IOException {
		public HttpAuthenticationException(String s) {
			super(s);
		}
	}*/


	//public static void main(String[] args) throws IOException, FileMakerException {
	//	FmResultSetRequest request = new FmResultSetRequest("http", "orion.360works.com", "/fmi/xml/fmresultset.xml", 80, null, null);
	//
	//	try {
	//		request.doRequest("-db=Contacts&-lay=Calc Test&-max=0&-findany");
	//	}
	//	finally {
	//		request.closeRequest();
	//	}
	//}

	public String getTableOccurrence() {
		return tableOccurrence;
	}
}
