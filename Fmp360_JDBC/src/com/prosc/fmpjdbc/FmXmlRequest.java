package com.prosc.fmpjdbc;

import sun.misc.BASE64Encoder;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.SQLException;

import org.xml.sax.*;

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
 * User: jesse
 * Date: Apr 17, 2005
 * Time: 2:22:20 PM
 */
public class FmXmlRequest extends FmRequest {
	//private static final int READ_TIMEOUT = 15 * 1000;
	private static final int CONNECT_TIMEOUT = 60 * 1000;

	/**
	 * The URL (not including post data) where the response is retrieved from
	 */
	private URL theUrl;
	/**
	 * Input stream opened from the URL
	 */
	private InputStream serverStream;
	/**
	 * Parser used to extract data from the filemaker response stream
	 */
	private final SAXParser xParser;
	private String authString;
	private String postPrefix = "";
	private String postArgs;
	private Logger log = Logger.getLogger( FmXmlRequest.class.getName() );
	private volatile boolean isStreamOpen = false;
	private int recIdColumnIndex;
	private long requestStartTime;

	/** A set that initially contains all requested fields, and is trimmed down as metadata is parsed.  If there are any missingFields left after parsing metadata, an exception is thrown listing the missing fields. */
	private Set missingFields;
	private RuntimeException creationStackTrace;
	private String username;
	private String fullUrl;
	private Thread parsingThread;

	public FmXmlRequest(String protocol, String host, String url, int portNumber, String username, String password, float fmVersion) {
		try {
			this.theUrl = new URL(protocol, host, portNumber, url);
		} catch (MalformedURLException murle) {
			log.severe("Trying to create the url " + protocol + host + ":" + portNumber + "/" + url + " threw this exception" + murle);
			throw new RuntimeException(murle);
		}
		if ( (username != null && username.length() > 0) || (password != null && password.length() > 0 ) ) {
			if( password == null ) password = ""; //Otherwise Java will use the word 'null' as the password
			String tempString = username + ":" + password;
			authString = new BASE64Encoder().encode(tempString.getBytes());
			this.username = username;
		}
		if (fmVersion >= 5 && fmVersion < 7) {
			this.setPostPrefix("-format=-fmp_xml&");
		}
		try {
			xParser = javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser();
			setFeature( "http://xml.org/sax/features/validation", false );
			setFeature( "http://xml.org/sax/features/namespaces", false );
			//setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
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
			log.warning( "Could not enable feature " + feature + " because of a SAXException: " + e );
		}
	}

	public void setPostPrefix(String s) {
		postPrefix = s;
	}


	public void doRequest(String input) throws IOException, SQLException {
		synchronized( FmXmlRequest.this ) {
			if (serverStream != null) {
				throw new IllegalStateException("You must call closeRequest() before sending another request.");
			}
		}
		requestStartTime = System.currentTimeMillis();
		postArgs = input;

		int retryCount = 3; //This is how many times to retry the attempt, in case FileMaker returns an error code 16 / retry. This will hopefully fix Zulu-195 Contact Syncing
		for( int n=1; n<=retryCount; n++ ) {
			HttpURLConnection theConnection = (HttpURLConnection) theUrl.openConnection();
			theConnection.setInstanceFollowRedirects( false );
			theConnection.setUseCaches(false);
			theConnection.setConnectTimeout( CONNECT_TIMEOUT ); //FIX!! Make this a configurable connection property
			//FIX!!! Make this a configurable connection property: theConnection.setReadTimeout( READ_TIMEOUT );
			if (authString != null) {
				theConnection.addRequestProperty("Authorization", "Basic " + authString);
			}
			if (postArgs != null) {
				//postArgs = postPrefix + postArgs;
				fullUrl = theUrl + "?" + postPrefix + postArgs;
				log.log(Level.FINE, "Starting request: " + fullUrl );
				theConnection.setDoOutput(true);
				PrintWriter out = new PrintWriter( theConnection.getOutputStream() );
				out.print(postPrefix);
				out.println(postArgs);
				out.close();
			}

			try {
				int httpStatusCode = theConnection.getResponseCode();
				if( httpStatusCode >= 200 && httpStatusCode < 300 ) {} //Fine, no problem
				else if( httpStatusCode >= 300 && httpStatusCode < 400 ) throw new IOException("Server has moved to new location: " + theConnection.getHeaderField("Location") );
				else if( httpStatusCode == 401 ) throw new HttpAuthenticationException( theConnection.getResponseMessage(), username );
				else if( httpStatusCode == 500 ) throw new IOException("Server returned a 500 (Internal server) error. Check and make sure that the FileMaker Web Publishing Engine is running at " + theUrl );
				else if( httpStatusCode == 501 ) throw new IOException("Server returned a 501 (Not Implemented) error. If you are using FileMaker 6, be sure to add ?&fmversion=6 to the end of your JDBC URL.");
				else if( httpStatusCode == 503 ) throw new IOException("Server returned a 503 (Service Unavailable) error. Make sure that the Web Publishing Engine is running.");
				else {
					InputStream err = theConnection.getErrorStream();
					ByteArrayOutputStream baos = new ByteArrayOutputStream( err.available() );
					try {
						byte[] buffer = new byte[1024];
						int bytesRead;
						while( (bytesRead=err.read(buffer)) != -1 ) {
							baos.write( buffer, 0, bytesRead );
						}
						String message = new String( baos.toByteArray(), "utf-8" );
						throw new IOException("Server returned unexpected status code: " + httpStatusCode + "; message: " + message );
					} finally {
						err.close();
					}
				}
				synchronized( FmXmlRequest.this ) {
					serverStream = theConnection.getInputStream(); // new BufferedInputStream(theConnection.getInputStream(), SERVER_STREAM_BUFFERSIZE);
					isStreamOpen = true;
				}
				if( log.isLoggable( Level.CONFIG ) ) {
					creationStackTrace = new RuntimeException("Created FmXmlRequest and opened stream");
				}
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
			} catch( FileMakerException e ) {
				if( e.getErrorCode() == 16 && n < retryCount ) { //Error code 16 means retry
					log.warning( "Received an error 16 retry message from FileMaker Server on attempt " + n + ", will try again" );
					continue;
				}
				throw e;
			}
			break;
		}
	}

	public void closeRequest() {
		//useSelectFields = false;
		//fieldDefinitions = null;
		//usedFieldArray = null;
		//allFieldNames = new ArrayList();
		//fmTable = null;
		//    foundCount = 0;
		synchronized( FmXmlRequest.this) {
			if( parsingThread != null && parsingThread.isAlive() ) {
				log.fine( "closeRequest: interrupting parsing thread" );
				parsingThread.interrupt();
			}
			if (serverStream != null)
				//try {
				//serverStream = null;
				//Don't close the serverStream - this is automatically done by the parser. This was causing deadlocks before. --jsb : serverStream.close();
				if( isStreamOpen ) {
					if( log.isLoggable( Level.CONFIG ) ) {
						log.config( "Closed request; request duration " + (System.currentTimeMillis() - requestStartTime) + " ms ( " + fullUrl + " )" );
					}
					isStreamOpen = false;
				}
			//} catch (IOException e) {
			//	throw new RuntimeException(e);
			//}
			if( xParser != null ) {
				xParser.reset();
			}
		}
	}

	protected void finalize() throws Throwable {
		synchronized( FmXmlRequest.this ) {
			if( isStreamOpen ) {
				if( log.isLoggable( Level.CONFIG ) ) {
					log.log( Level.CONFIG, "Warning - request was finalized without ever being closed. The stack trace that follows shows the thread that created this request. (" + theUrl + "?" + postArgs + ")", creationStackTrace );
				} else {
					log.warning( "Warning - request was finalized without ever being closed. Set log level to FINE to get a stack trace of when this request was created. (" + theUrl + "?" + postArgs + " )" );
				}
				closeRequest();
			}
		}
		//if (serverStream != null) serverStream.close();
		super.finalize();
	}

	private void readResult() throws SQLException {
		synchronized( FmXmlRequest.this ) {
			parsingThread = new Thread("FileMaker JDBC Parsing Thread") {
				public void run() {
					final InputStream streamToParse;
					synchronized( FmXmlRequest.this ) {
						streamToParse = serverStream;
					}
					InputSource input = new InputSource(streamToParse);
					//				input.setSystemId("http://" + theUrl.getHost() + ":" + theUrl.getPort() + "/fmi/xml/" );
					//input.setSystemId("http://" + theUrl.getHost() + ":" + theUrl.getPort() );
					input.setSystemId("http://");
					FmXmlHandler xmlHandler = new FmXmlHandler();
					try {
						xParser.parse( input, xmlHandler );
					} catch (IOException ioe) {
						boolean ignore = false; //FIX!! Have the close() method set a thread-safe variable which is checked here, if it was closed then ignore the exception --jsb
						if (ioe.getMessage().equals("stream is closed") || ioe.getMessage().equalsIgnoreCase("stream closed") || ioe.getMessage().equalsIgnoreCase( "Socket closed" )) {
							synchronized( FmXmlRequest.this ) {
								if( ! isStreamOpen ) ignore = true;
							}
						}
						if( ! ignore ) {
							log.log(Level.WARNING, "The parsing thread was in the middle of parsing data from FM when an IOException occurred.", ioe );
							//log.info("There was an error, so i'm setting all of the variables and continuing");
							onErrorSetAllVariables(ioe);
							//throw new RuntimeException(ioe);
						}
					} catch (SAXException e) {
						//log.fine("There was SAXException: " + e.getMessage() + ", so the parsing thread is setting all of the threading variables to true and notifying all threads.\n Here's the stack trace: ");
						onErrorSetAllVariables(e);
						//throw new RuntimeException(e);
					} catch (RuntimeException e) {
						//log.fine("There was an error in the parsing thread: " + e.getMessage() + ", so the parsing thread is setting all of the threading " + "variables to true and notifying all threads.");
						onErrorSetAllVariables(e);
						//throw new RuntimeException(e);
					} catch( Error e ) {
						onErrorSetAllVariables( e );
					} finally {
						closeRequest();
					}
				}


			};
			parsingThread.start();
		}
		if(hasError()) {
			throw FileMakerException.exceptionForErrorCode( errorCode, fullUrl );
		}

	}

	private synchronized void onErrorSetAllVariables(Throwable t) {
		String fieldName = null;
		if( t instanceof StopParsingException ) {
			//We were stopped normally, parsing thread was intentionally interrupted
		} else {
			try {
				fieldName = (String)columnNames.get( columnIndex + 1 );
			} catch( Exception e ) {
				log.info( "Error occured while parsing XML data; couldn't tell which field caused the error." );
			}
		}
		recordIterator.setStoredError(t, fieldName );
		productVersionIsSet = true;
		databaseNameIsSet = true;
		foundCountIsSet = true;
		recordIteratorIsSet = true;
		//recordIterator.setFinished();
		//recordIterator = null;
		fieldDefinitionsListIsSet = true;
		fieldDefinitions = null;
		notifyAll();
	}

	public synchronized String getProductVersion() throws SQLException {
		while (!productVersionIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				SQLException sqle = new SQLException( "Thread was interrupted while parsing FileMaker XML response from Web Publishing Engine" );
				sqle.initCause( e );
				throw sqle;
			}
		}
		return productVersion;
	}

	private synchronized void setProductVersion(String pv) {
		// some thread stuff
		productVersion = pv;
		productVersionIsSet = true;
		notifyAll();
	}

	public synchronized String getDatabaseName() throws SQLException {
		boolean resetInterrupt = false;
		while (!databaseNameIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				log.log( Level.WARNING, "Interrupted while getting database name" );
				resetInterrupt = true;
			}
		}
		if( resetInterrupt ) {
			Thread.currentThread().interrupt();
		}
		return databaseName;
	}

	private synchronized void setDatabaseName(String dbName) {
		// some thread stuff
		databaseName = dbName;
		databaseNameIsSet = true;
		notifyAll();
	}

	public synchronized int getFoundCount() throws SQLException {
		boolean resetInterrupt = false;
		while (!foundCountIsSet) {
			try {
				wait();
			} catch( InterruptedException e ) {
				log.log( Level.WARNING, "Interrupted while waiting for found count" );
				resetInterrupt = true;
			}
		}
		if( resetInterrupt ) {
			Thread.currentThread().interrupt();
		}
		return foundCount;
	}

	private synchronized void setFoundCount(int i) {
		// some thread stuff
		foundCount = i;
		foundCountIsSet = true;
		notifyAll();
	}


	public synchronized Iterator<FmRecord> getRecordIterator() throws SQLException {
		boolean resetInterrupt = false;
		while (!recordIteratorIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				log.log( Level.WARNING, "Interrupted while waiting for record iterator" );
				resetInterrupt = true; //We can't bail from this method on interruption, because that would break transactions (if we have already inserted an object, we can't throw an exception).
			}
		}
		if( resetInterrupt ) {
			Thread.currentThread().interrupt();
		}
		return recordIterator;
	}

	private synchronized void setRecordIterator(ResultQueue i) {
		// some thread stuff
		recordIterator = i;
		recordIteratorIsSet = true;
		notifyAll();
	}

	public synchronized FmFieldList getFieldDefinitions() throws SQLException {
		boolean resetInterrupt = false;
		while (!fieldDefinitionsListIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				log.log( Level.WARNING, "Interrupted while getting field definitions" );
				resetInterrupt = true;
			}
		}
		if( resetInterrupt ) {
			Thread.currentThread().interrupt();
		}
		if( getErrorCode() == 0 && missingFields != null && missingFields.size() > 0 ) {
			List missingFieldNames = new LinkedList();
			for( Iterator it = missingFields.iterator(); it.hasNext(); ) {
				missingFieldNames.add( ((FmField)it.next()).getColumnName() );
			}
			closeRequest();
			throw new SQLException("The requested fields are not on the layout: " + missingFieldNames, null, 102 );
		}
		return fieldDefinitions;
	}

	private synchronized void setErrorCode(int code) {
		// 0 is ok
		// 401 is no results
		errorCode = code;
		errorCodeIsSet = true;
		notifyAll();

	}

	public synchronized int getErrorCode() {
		while(!errorCodeIsSet) {
			try {
				wait();
			} catch (InterruptedException e) {
				log.log( Level.WARNING, "Thread was interrupted while waiting for error code from last operation; assuming success and resetting interrupt flag." );
				Thread.currentThread().interrupt();
				errorCode = 0;
				errorCodeIsSet = true;
				//SQLException sqle = new SQLException( "Thread was interrupted while parsing FileMaker XML response from Web Publishing Engine" );
				//sqle.initCause( e );
				//throw sqle;
			}
		}
		return errorCode;
	}

	public boolean hasError() {
		// 0 is ok
		// 401 is no results
		int error = getErrorCode();
		if (error == 0 || error == 401) {
			return false;
		} else {
			return true;
		}
	}


	/**
	 * In this class we have a reference to FmFieldList called fieldDefinitions (see below). In all the uses of this class,
	 * the state of this reference falls into 3 categories. If it is set by the setSelectFields() method below, it has one
	 * or more FmFields.  Or, it might just contain 1 FmField which contains an asterisk '*" for 'select *'. The last case
	 * is when fieldDefinitions is initially null. This will be the case when it is not set by setSelectFields(). i.e  for
	 * updates and inserts etc.
	 */
	private volatile FmFieldList fieldDefinitions;
	private FmTable fmTable;
	private boolean useSelectFields = false;

	private volatile String productVersion;
	private volatile String databaseName;
	private volatile int foundCount = -1;
	/**
	 * Contains resultSet FmRecords
	 */
	private volatile ResultQueue recordIterator; // FIX! does this really need to be volatile? -ssb

	private int columnIndex;
	private List columnNames = new LinkedList();
	private volatile int errorCode;

	private boolean fieldDefinitionsListIsSet = false;
	private boolean productVersionIsSet = false;
	private boolean databaseNameIsSet = false;
	private boolean foundCountIsSet = false;
	private boolean recordIteratorIsSet = false;
	private boolean errorCodeIsSet = false;

	/**
	 * XML parsing SAX implementation.
	 * This parses the result metadata and record data, adding FmRecord objects to the recordIterator.
	 * <p>
	 * Initially, metadata is parsed.  For each FIELD in the METADATA element, a FieldPositionPointer is added to the usedFieldArray list.
	 * For repeating fields, multiple FieldPositionPointers are created, one for each repetition, each pointing to an index in the FmRecord to send data to.
	 * <p>
	 * Then, the RESULTSET data is parsed.  For each ROW, a new FmRecord is created and things are zeroed out.  For every COL, the next fieldPositionPointer is gotten.
	 * WHen a DATA element is encountered, the current fieldPositionPointer is called to set the data in the FmRecord, if applicable. If multiple DATA elements are encountered within a column, the current fieldPositionPointer is queried to determine whether the COL is a repeating field or a portal.
	 * If the former, the next fieldPositionPointer is gotten, and data is set.  If the latter, any data is ignored.
	 */
	private class FmXmlHandler extends org.xml.sax.helpers.DefaultHandler {
		//private StringBuffer requestContent = new StringBuffer();
		//private static final boolean debugMode = false; //If true, then the content of the XML will be stored in requestContent
		private InputSource emptyInput = new InputSource( new ByteArrayInputStream(new byte[0]) );
		private int sizeEstimate;
		/** Incremented as metadata fields are parsed */
		//private int currentMetaDataFieldIndex;

		/**
		 * The row currently being parsed. Raw data is appended to the row at the appropriate index. When the row is completely parsed, it is appended to the recordIterator.
		 */
		private FmRecord currentRow;
		/**
		 * Contains string data for the current data element being parsed
		 */
		private transient StringBuffer currentData = new StringBuffer(255); // OPTIMIZE! use a StringBuilder instead
		private boolean foundDataForColumn;
		private boolean foundDataForRow;
		private boolean foundColStart = false; // added to fix a bug with columns that only have an end element - mww
		//private int columnDataIndex;

		/**
		 * Maps data indices in the XML data with data indices in the currentRow.  If a field in the XML data is not used, the value of that index will be -1.
		 */
		private List usedFieldArray = new ArrayList(64); // The array used by the characters() method in xmlHandler.
		private FieldPositionPointer fieldPositionPointer;
		private Iterator fieldPositionIterator;
		private int nodeType;
		private static final int NODE_TYPE_ERROR = 1;
		private static final int NODE_TYPE_DATA = 2;

		public FmXmlHandler() {
			super();
		}

		public void fatalError(SAXParseException e) throws SAXException {
			//We don't need to log, because we're throwing the exception to the ResultQueue : log.log(Level.SEVERE, e.toString(), e );
			log.log( Level.SEVERE, "fatalError for request: " + fullUrl + "; " + e.toString() );
			super.fatalError(e);
		}

		public void warning( SAXParseException e ) throws SAXException {
			log.log( Level.WARNING, "warning for request: " + fullUrl + "; " + e.toString() );
			super.warning( e );	//To change body of overridden methods use File | Settings | File Templates.
		}

		public void error( SAXParseException e ) throws SAXException {
			log.log( Level.SEVERE, "error for request: " + fullUrl + "; " + e.toString() );
			super.error( e );	//To change body of overridden methods use File | Settings | File Templates.
		}

		/** This is necessary to work around a bug in the Crimson XML parser, which is used in the 1.4 JDK. Crimson
		 * cannot handle relative HTTP URL's, which is what FileMaker uses for it's DTDs: "/fmi/xml/FMPXMLRESULT.dtd"
		 * By returning an empty value here, we short-circuit the DTD lookup process.
		 */
		public InputSource resolveEntity( String publicId, String systemId ) {
			return emptyInput;
		}

		public void startDocument() {
			//if( debugMode ) requestContent.append( "Starting document\n" );
			log.log(Level.FINEST, "Start parsing response");
			setRecordIterator(new ResultQueue(256*1024, 64*1024));  // FIX! is this a good size? -britt
			nodeType = 0;
		}

		public void startElement(String uri, String xlocalName, String qName, Attributes attributes) throws SAXException {
			if( Thread.interrupted() ) {
				Thread.currentThread().interrupt();
				throw new StopParsingException( "Parsing thread was interrupted" );
			}
			/*if( debugMode ) {
				requestContent.append( "<" + qName);
				for( int n=0; n<attributes.getLength(); n++ ) {
					requestContent.append( " " + attributes.getQName(n) + "=" + attributes.getValue(n) );
				}
				requestContent.append( ">");
			}*/
			// Frequently repeated nodes
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Starting element qName = " + qName + " for " + this);
			}
			if ("DATA".equals(qName)) {
				if( foundDataForColumn ) {
					// this is not the first DATA in the COL. It's either a repeating field or portal
					// FIX!! if a portal, only return the first item -ssb
					if (fieldPositionPointer != null && fieldPositionPointer.isRepeating) {
						fieldPositionPointer = (FieldPositionPointer) fieldPositionIterator.next();
					} else {
						fieldPositionPointer = null; // ignore any other data
					}
				} else {
					// this is the first <DATA> element in the <COL>
					foundDataForColumn = true;
					foundDataForRow = true;
				}

				//currentData.delete( 0, currentData.length() );
				currentData = new StringBuffer( 255 ); // OPTIMIZE! just call reset() on the existing currentData instead
			} else if ("COL".equals(qName)) {
				foundDataForColumn = false;
				foundColStart = true; // added to fix a bug with columns that only have an end element - mww
				//columnDataIndex++;
				fieldPositionPointer = (FieldPositionPointer) fieldPositionIterator.next();
				columnIndex++;
				if( columnIndex == recIdColumnIndex ) {
					currentRow.setRawValue(currentRow.getRecordId().toString(), columnIndex);
					columnIndex++;
				}
			} else if ("ROW".equals(qName)) {
				foundDataForRow = false;
				//dt.markTime("  Starting row");
				//This refers directly to the fieldDefinitions instance variable, because we don't care if we're missing fields and we don't want a checked exception. --jsb
				currentRow = new FmRecord(fieldDefinitions, Long.valueOf(attributes.getValue("RECORDID")), Integer.valueOf(attributes.getValue("MODID")));
				columnIndex = -1;
				//columnDataIndex = -1;
				fieldPositionIterator = usedFieldArray.iterator();
			}
			// One-shot nodes
			else if ("FIELD".equals(qName)) {
				String fieldName = attributes.getValue("NAME");
				columnNames.add( fieldName );

				String fieldType = attributes.getValue("TYPE");
				FmFieldType theType = (FmFieldType)FmFieldType.typesByName.get(fieldType);
				boolean allowsNulls = "YES".equals(attributes.getValue("EMPTYOK"));
				int maxRepeat = Integer.parseInt(attributes.getValue("MAXREPEAT"));

				if (!useSelectFields) { // this is a select * query.  create a new non-repeating field and add it to the fieldDefinitions
					FmField fmField = new FmField(fmTable, fieldName, fieldName, theType, allowsNulls);
					fieldDefinitions.add(fmField);
				}
				if (maxRepeat > 1) { // this is a repeating field.  handle each repetition as a virtual field.
					for (int eachRepIndex =1; eachRepIndex <= maxRepeat; eachRepIndex++) {
						handleParsedMetaDataField(fieldName, eachRepIndex, theType, allowsNulls);
					}
				} else {
					handleParsedMetaDataField(fieldName, 0, theType, allowsNulls);
				}

			} else if ("RESULTSET".equals(qName)) {
				setFoundCount(Integer.valueOf(attributes.getValue("FOUND")).intValue()); //foundCount = Integer.valueOf(attributes.getValue("FOUND")).intValue();
				log.log(Level.FINE, "Resultset size: " + foundCount);
				nodeType = NODE_TYPE_DATA;
			} else if ("PRODUCT".equals(qName)) {
				setDatabaseName(attributes.getValue("NAME")); // databaseName = attributes.getValue("NAME");
				setProductVersion(attributes.getValue("VERSION")); // productVersion = attributes.getValue("VERSION");
			} else if ("DATABASE".equals(qName)) {
				fmTable =  new FmTable( attributes.getValue("NAME") );

				if (fieldDefinitions == null) {
					fieldDefinitions = new FmFieldList();
				}


			} else if ("ERRORCODE".equals(qName)) {
				log.log( Level.FINE, "Took " + ( System.currentTimeMillis() - requestStartTime ) + "ms to start receiving XML data" );
				nodeType = NODE_TYPE_ERROR;
			}
		}

		/**
		 * Handle a field which was parsed in the metadata.  This will set the usedFieldArrayIndex, remove the field from missingFields, and set some metadata on the FmField object in the fieldDefintions list.
		 * @param fieldName
		 * @param repetitionIndex for non-repeating fields, this should be zero.
		 * @param theType
		 * @param allowsNulls
		 */
		private void handleParsedMetaDataField(String fieldName, int repetitionIndex, FmFieldType theType, boolean allowsNulls) {
			String adjustedName = repetitionIndex == 0 ? fieldName : fieldName + "[" + repetitionIndex + "]";
			int columnIndex = -1;
			boolean foundOneOccurrence = false;
			int[] indeces = new int[1];
			do {
				int indexToTry = columnIndex + 1;
				columnIndex = fieldDefinitions.indexOfFieldWithColumnName(adjustedName, indexToTry );
				if (columnIndex == -1 && repetitionIndex == 1) {
					// this is the first repetition of a repeating field, which does not have a [] bracket.  Look for a fieldDefinition name without the brackets
					columnIndex = fieldDefinitions.indexOfFieldWithColumnName(fieldName, indexToTry );
				}
				if (columnIndex != -1) { // set the type and nullable if this is a field in the field definitions
					FmField fmField = fieldDefinitions.get(columnIndex);
					fmField.setType(theType);
					fmField.setNullable(allowsNulls);
					if( foundOneOccurrence ) { //We've already found at least one occurrence in the SELECT field list; this is a subsequent one
						int[] biggerArray = new int[ indeces.length + 1 ]; //Copy into a bigger array and add this index to the end
						System.arraycopy( indeces, 0, biggerArray, 0, indeces.length );
						biggerArray[ biggerArray.length -1 ] = columnIndex;
						indeces = biggerArray;
					} else {
						indeces[0] = columnIndex;
						foundOneOccurrence = true;
						if( missingFields != null ) { //Remove this from the list of missing fields
							missingFields.remove(fmField);
						}
					}
				}
			} while( columnIndex != -1 );
			if( foundOneOccurrence ) {
				usedFieldArray.add( new FieldPositionPointer( indeces, repetitionIndex != 0 ) );
			} else {
				usedFieldArray.add( new FieldPositionPointer( new int[0], repetitionIndex != 0 ) ); // ignore this field
			}

		}

		public void endDocument() throws SAXException {
			recordIterator.setFinished();
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			//if( debugMode ) requestContent.append( "</" + qName + ">" );
			if( "DATA".equals(qName) ) {
				if (fieldPositionPointer != null) {
					fieldPositionPointer.setDataInRow(currentData, currentRow);
				}
			}
			if ("ROW".equals(qName)) {
				if( foundDataForRow ) { //If this is false, then record-level privileges prevented us from seeing this record; don't add it to the list of records.
					try {
						recordIterator.add(currentRow, (long) sizeEstimate);
					} catch( InterruptedException e ) {
						Thread.currentThread().interrupt();
						throw new SAXException( "Parsing thread was interrupted while waiting to add more objects to the result queue" );
					}
				} else {
					log.config( "Skipped an empty row, record ID " + currentRow.getRecordId() );
				}
				sizeEstimate = 0; // set it to 0 and start estimating again
				if( columnIndex == recIdColumnIndex ) { //This is necessary in case the record id is the last selected field; it won't be caught in the begin of the <COL> element.
					currentRow.setRawValue(currentRow.getRecordId().toString(), columnIndex);
					columnIndex++;
				}
				//records.add(currentRow);
			}
			if("COL".equals(qName)){ // added to fix a bug with columns that only have an end element - mww
				if( ! foundDataForColumn ) { //If there is a related field but the relationship is invalid, then FM does not contain a <DATA> element. We need to catch that specially and treat it as null.
					if (fieldPositionPointer != null) {
						fieldPositionPointer.setDataInRow(currentData, currentRow); //FIX!! This should actually be null instead of an empty string, but currently that will throw a NPE - need to fix many things to support this. --jsb
					}
				}
				if(!foundColStart){ // do only if there was no start COL element
					fieldPositionPointer = (FieldPositionPointer) fieldPositionIterator.next();
					columnIndex++;
					if( columnIndex == recIdColumnIndex ) {
						// no need to set the data since there is none
						//currentRow.setRawValue(currentRow.getRecordId().toString(), columnIndex);
						columnIndex++;
					}
					foundColStart = false;
				}
			}
			if ("METADATA".equals(qName)) { // Create the usedorder array.  This is done once.
				//usedFieldArray = new int[allFieldNames.size()];
				//missingFields = new LinkedHashSet( fieldDefinitions.getFields() );
				//
				//int i = 0;
				//Iterator it = allFieldNames.iterator();
				//
				//while ( it.hasNext() ) {
				//	String aFieldName = (String)it.next();
				//
				//	int columnIndex;
				//
				//	if ( (columnIndex = fieldDefinitions.indexOfFieldWithColumnName(aFieldName)) > -1) {
				//		// Get the index of the fieldName w.r.t fieldDefinitions, and put that value into the usedFieldArray
				//		usedFieldArray[i] = columnIndex;
				//		missingFields.remove( fieldDefinitions.get( columnIndex ) );
				//	} else {
				//		usedFieldArray[i] = -1; // This field columnName will not be used.
				//	}
				//	i++;
				//}
				// when i come to the metadata tag, i know all of the fields that are going to be in the table, so
				// I can let people get the fieldDefinitions

				synchronized (FmXmlRequest.this) { // this is different from the other attributes in the xml, since this one is being built on the fly and the variable is not just being "set" once we're finished reading it
					fieldDefinitionsListIsSet = true;
					FmXmlRequest.this.notifyAll();
				}
			}
		}

		public void characters(char ch[], int start, int length) {
			//if( debugMode ) requestContent.append( ch, start, length );
			sizeEstimate += length;
			switch(nodeType) {
				case 0:
					log.log(Level.WARNING, "Unexpected character data : " + new String(ch, start, length));
					break;
				case NODE_TYPE_ERROR:
					setErrorCode(Integer.parseInt(new String(ch, start, length)));
					break;
				case NODE_TYPE_DATA:
					if (fieldPositionPointer != null && fieldPositionPointer.targetIndeces.length > 0 ) {
						currentData.append(ch, start, length);
					}
					break;
			}
		} // end of characters

	}

	private static class FieldPositionPointer {
		private int[] targetIndeces;
		private boolean isRepeating;

		public FieldPositionPointer(int[] targetIndeces, boolean repeating) {
			this.targetIndeces = targetIndeces;
			isRepeating = repeating;
		}

		public void setDataInRow(StringBuffer data, FmRecord row) {
			for( int n=0; n<targetIndeces.length; n++ ) {
				row.setRawValue( data.toString(), targetIndeces[n] );
			}
		}
	}


	/**
	 *  Use this to set the fields that are actually used in the select statement.
	 *  The data we get from filemaker contains all the fields so we need to parse it appropriately.
	 */
	public void setSelectFields(FmFieldList selectFields) {
		fieldDefinitions = selectFields;

		// Code will use select fields if they exist and the first one is not an asterisk
		if ("*".equals(fieldDefinitions.get(0).getColumnName() )) {
			fieldDefinitions.getFields().remove(0);
		} else {
			useSelectFields = true;
		}

		//missingFields = new LinkedHashSet( fieldDefinitions.getFields() ); // this will be trimmed down as metadata is parsed
		missingFields = new LinkedHashSet( fieldDefinitions.getFields().size() );
		int n=0;
		for( Iterator it = fieldDefinitions.getFields().iterator(); it.hasNext(); ) {
			FmField eachField = (FmField)it.next();
			if( "recid".equalsIgnoreCase( eachField.getColumnName() ) ) {
				eachField.setNullable( false );
				eachField.setReadOnly( true );
				eachField.setType( FmFieldType.RECID );
				recIdColumnIndex = n;
			}
			else {
				missingFields.add( eachField );
			}
			n++;
		}
	}

	public static class HttpAuthenticationException extends FileMakerException {
		public HttpAuthenticationException(String message, String username) {
			super(212, "Invalid FileMaker user account and/or password. Please try again - username '" + username + "'" );
		}
	}


	//public static void main(String[] args) throws IOException, FileMakerException {
	//	FmXmlRequest request;
	//	//request = new FmXmlRequest("http", "hercules.360works.com", "/fmi/xml/FMPXMLRESULT.xml", 80, null, null);
	//	//request.doRequest("-db=Contacts&-lay=Contacts&-findall");
	//	//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "fmp.360works.com", "/FMPro?-db=Names&-format=-fmp_xml&-findall", 4000);
	//	//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "fmp.360works.com", "/FMPro?-db=Insertions&-lay=AMSLogic&-format=-fmp_xml&-findall", 4000, "exchange", "waffle");
	//	//FmXmlRequest fmXmlRequest = new FmXmlRequest("http", "localhost", "/fmi/xml/FMPXMLRESULT.xml?-db=Contacts&-lay=Contacts&-findall", 3000, null, null);
	//
	//	request = new FmXmlRequest("http", "orion.360works.com", "/fmi/xml/FMPXMLRESULT.xml", 80, null, null, 5);
	//	for (int n = 1; n <= 10; n++) {
	//		try {
	//			request.doRequest("-db=Contacts&-lay=Calc Test&-max=100&-findany");
	//		} finally {
	//			request.closeRequest();
	//		}
	//	}
	//}

}
