package com.prosc.fmpjdbc;

import com.prosc.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import sun.misc.BASE64Encoder;

import javax.net.ssl.SSLHandshakeException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Time;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	private volatile SAXParser xParser;
	private volatile SAXParser runningParser = null;
	private String authString;
	private String postPrefix = "";
	private String postArgs;
	private Logger log = Logger.getLogger( FmXmlRequest.class.getName() );
	private volatile boolean isStreamOpen = false;
	private long requestStartTime;

	/** A set that initially contains all requested fields, and is trimmed down as metadata is parsed.  If there are any missingFields left after parsing metadata, an exception is thrown listing the missing fields. */
	private Set<String> missingFields;
	private RuntimeException creationStackTrace;
	private String username;
	private Thread parsingThread;
	private SQLException metadataError;
	private Integer fmVersion;
	private String concatUrl;
	private boolean retry802 = true;
	private boolean redirected;

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
		xParser = buildParser();
	}

	/** 802 errors are sometimes generated intermittently by FileMaker Server, especially with connections to ESS servers. For this reason, all 802
	 * errors will be retried 3 times. If this behavior is not desired, set this to false.
	 * @param retry802
	 */
	public void setRetry802( boolean retry802 ) {
		this.retry802 = retry802;
	}

	private SAXParser buildParser() {
		try {
			SAXParser result = javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser();
			setFeature( result, "http://xml.org/sax/features/validation", false );
			setFeature( result, "http://xml.org/sax/features/namespaces", false );
			//setFeature( "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );
			log.finest( "Created an XML parser; class is: " + result.getClass() );
			return result;
		} catch( ParserConfigurationException e ) {
			throw new RuntimeException( e );
		} catch ( SAXException e) {
			throw new RuntimeException(e);
		}
	}

	private void setFeature( SAXParser parser, String feature, boolean enabled ) {
		try {
			parser.getXMLReader().setFeature( feature, enabled );
		} catch( SAXException e ) { //Ignore
			log.warning( "Could not enable feature " + feature + " because of a SAXException: " + e );
		}
	}

	public void setPostPrefix(String s) {
		postPrefix = s;
	}


	public void doRequest(String input) throws IOException, SQLException {
		requestStartTime = System.currentTimeMillis();
		postArgs = input;
		redirected = false;
		doRequest( 0 );
	}

	private void doRequest( int retryCount ) throws IOException, SQLException {
		synchronized( FmXmlRequest.this ) {
			if (serverStream != null) {
				throw new IllegalStateException("You must call closeRequest() before sending another request.");
			}
		}

		//int retryCount = 3; //This is how many times to retry the attempt, in case FileMaker returns an error code 16 / retry. This will hopefully fix Zulu-195 Contact Syncing

		HttpURLConnection theConnection = (HttpURLConnection) theUrl.openConnection();
		theConnection.setInstanceFollowRedirects( true ); //Set this to true because OS X Lion Server always redirects all requests to https://
		theConnection.setUseCaches(false);
		theConnection.setConnectTimeout( CONNECT_TIMEOUT ); //FIX!! Make this a configurable connection property
		//FIX!!! Make this a configurable connection property: theConnection.setReadTimeout( READ_TIMEOUT );
		if (authString != null) {
			theConnection.addRequestProperty("Authorization", "Basic " + authString);
		}
		File xmlFile = null;
		try {
			String fullUrl;
			if (postArgs != null) {
				//postArgs = postPrefix + postArgs;
				fullUrl = theUrl + "?" + postPrefix + postArgs;
				theConnection.setDoOutput(true);
				PrintWriter out = new PrintWriter( theConnection.getOutputStream() );
				out.print(postPrefix);
				out.println(postArgs);
				out.close();
			} else {
				fullUrl = theUrl.toExternalForm();
			}
			concatUrl = fullUrl.length() < 512 ? fullUrl : fullUrl.substring( 0, 512 ) + "...<etc>";
			log.log( Level.INFO, "Starting request: " + concatUrl );
			log.log( Level.INFO, "Full URL: " + fullUrl);


			int httpStatusCode = theConnection.getResponseCode();
			if( httpStatusCode >= 200 && httpStatusCode < 300 ) {} //Fine, no problem
			else if( httpStatusCode >= 300 && httpStatusCode < 400 ) { //Follow the redirect, because OS X Lion automatically redirects http to https
				redirected = true;
				String redirect = theConnection.getHeaderField( "location" );
				theUrl = new URL( theUrl, redirect );
				doRequest( retryCount );
				return;
				//throw new IOException("Server has moved to new location: " + theConnection.getHeaderField("Location") );
			}
			else if( httpStatusCode == 401 ) throw new HttpAuthenticationException( theConnection.getResponseMessage(), username, concatUrl );
			else if( httpStatusCode == 405 ) throw new IOException( "Server returned a 405 (Method not supported) error. This usually means that the FileMaker Web Publishing is not installed on this server. The URL that generated the error is " + concatUrl );
			else if( httpStatusCode == 500 ) throw new IOException("Server returned a 500 (Internal server) error. The URL that generated the error is " + concatUrl );
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
				if(System.getProperty("com.prosc.fmxml.debug","false").equals("true")) { //careful!!! This will run a machine out of disk space if you leave it set.
					//File xmlFile;
					Random random = new Random(System.currentTimeMillis());
					if(System.getProperty("os.name").toLowerCase().contains("windows")) {
						xmlFile = new File("C:\\WINDOWS\\Temp\\FmXml." + Thread.currentThread().getName()
								+ "." + random.nextLong() + ".xml");
					}else {
						xmlFile = new File("/tmp/FmXml." + Thread.currentThread().getName()
								+ "." + random.nextLong() + ".xml");
					}
					if(xmlFile.createNewFile()) {
						InputStream resultStream = theConnection.getInputStream();
						FileOutputStream fileOut = new FileOutputStream(xmlFile);
						IOUtils.writeInputToOutput(resultStream, fileOut, 8192);
						resultStream.close();
						fileOut.close();
						FileInputStream xmlInputStream = new FileInputStream(xmlFile);
						serverStream = xmlInputStream;
						log.log(Level.INFO, "Debug mode is enabled. This request was stored in " + xmlFile.getName() + ". It will be deleted once the response is successfully parsed");
					}else{
						log.log(Level.WARNING, "unable to create output file for fmxml debug");
						serverStream = theConnection.getInputStream();
					}
				}else{
					serverStream = theConnection.getInputStream(); // new BufferedInputStream(theConnection.getInputStream(), SERVER_STREAM_BUFFERSIZE);
				}
				isStreamOpen = true;
			}
			if( log.isLoggable( Level.CONFIG ) ) {
				creationStackTrace = new RuntimeException("Created FmXmlRequest and opened stream");
			}
		} catch( SSLHandshakeException e ) {
			IOException ioe;
			if( redirected ) {
				ioe = new IOException( "This request was automatically redirected to " + theUrl.toString() +", which is not running a trusted certificate. " +
						"Self-signed certificates are not supported, unless you have set them up with your Java Key store. If you are running OS X Lion Server, " +
						"you can fix this by disabling SSL for the default web site." );
			} else {
				ioe = new IOException( "The server at " + theUrl.toString() + " is not running a trusted certificate. Self-signed certificates are not supported, " +
						"unless you have set them up with your Java Key store." );
			}
			ioe.initCause( e );
			throw ioe;
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
			readResult(serverStream);
		} catch( FileMakerException e ) {
			boolean retry = false;
			if( ++retryCount < 3 ) {
				if( e.getErrorCode() == 16 ) {
					retry = true;
					log.warning( "Received an error 16 retry message from FileMaker Server on attempt " + retryCount + ", will try again" );
				} else if( e.getErrorCode() == 802 && retry802 ) {
					retry = true;
					log.warning( "Received an error 802 (no database available) from FileMaker Server on attempt " + retryCount + ", will try again" );
				}
			}
			if( retry ) { //Error code 16 means retry
				doRequest( retryCount );
			} else {
				throw e;
			}
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
			try {
				if( parsingThread != null && parsingThread.isAlive() ) {
					log.fine( "closeRequest: interrupting parsing thread" );
					parsingThread.interrupt();
					parsingThread = null;
				}
				if (serverStream != null) {
					//try {
					//serverStream = null;
					//Don't close the serverStream - this is automatically done by the parser. This was causing deadlocks on Windows because two different threads are calling close(). --jsb :
					try {
						serverStream.close(); //I turned this back on because I removed it from the parsing thread. Need to test on Windows.
						serverStream = null;
					} catch( Exception e ) {
						log.log( Level.WARNING, "Error while closing fm XML stream: " + e.toString(), e );
					}
				}
			} finally {
				if( isStreamOpen ) {
					if( log.isLoggable( Level.CONFIG ) ) {
						log.config( "Closed request; request duration " + (System.currentTimeMillis() - requestStartTime) + " ms ( " + concatUrl + " )" );
					}
					isStreamOpen = false;
				}
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
					log.warning( "Warning - request was finalized without ever being closed. Set log level to CONFIG to get a stack trace of when this request was created. (" + theUrl + "?" + postArgs + " )" );
				}
				closeRequest();
			}
		}
		//if (serverStream != null) serverStream.close();
		super.finalize();
	}

	private void readResult(@NotNull final InputStream streamToParse) throws SQLException {
		synchronized( FmXmlRequest.this ) {
			parsingThread = new Thread("FileMaker JDBC Parsing Thread" ) {
				public void run() {
					FmXmlHandler xmlHandler = new FmXmlHandler();
					xmlHandler.setDocumentLocator( null );

					InputSource input;
					try {
						input = new InputSource(new UnicodeReader(streamToParse, "UTF-8")); // we use a character stream, otherwise the parser will try to open the SystemId on the InputSource
						if( runningParser != null ) { //You can't call parse() while it's still parsing XML from the last call (remember that parsing happens on a separate thread, which may not have finished), so create a new parser if the current one is still in use.
							xParser = buildParser();
						}
						runningParser = xParser;
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
							onErrorSetAllVariables(ioe, xmlHandler.getCurrentColumnName()  );
							//throw new RuntimeException(ioe);
						}
					} catch( StopParsingException e ) {
						//It's safe to ignore these, because once we've been closed, the client is not going to be requesting any more data
					} catch (SAXException e) {
						//log.fine("There was SAXException: " + e.getMessage() + ", so the parsing thread is setting all of the threading variables to true and notifying all threads.\n Here's the stack trace: ");
						onErrorSetAllVariables(e, xmlHandler.getCurrentColumnName()  );
						//throw new RuntimeException(e);
					} catch (RuntimeException e) {
						//log.fine("There was an error in the parsing thread: " + e.getMessage() + ", so the parsing thread is setting all of the threading " + "variables to true and notifying all threads.");
						onErrorSetAllVariables(e, xmlHandler.getCurrentColumnName()  );
						//throw new RuntimeException(e);
					} catch( Error e ) {
						onErrorSetAllVariables( e, xmlHandler.getCurrentColumnName() );
					} finally {
						recordIterator.setFinished();
						if( runningParser == xParser ) { //Set runningParser to null IF no other thread has changed the running parser since this one started
							runningParser = null;
						}
					}
				}


			};
			parsingThread.start();
		}
		synchronized( FmXmlRequest.this ) {
			if(hasError()) {
				closeRequest();
				FileMakerException fileMakerException = FileMakerException.exceptionForErrorCode(errorCode, concatUrl, fmLayout);
				if (metadataError != null) {
					fileMakerException.initCause(metadataError);
				}
				throw fileMakerException;
			}
			if( getFieldDefinitions() == null ) {
				throw new SQLException(metadataError); // wrap the metadataError so we have the complete stack of the current calling code
			}
		}

	}

	private synchronized void onErrorSetAllVariables(Throwable t, String fieldName ) {
		recordIterator.setStoredError(t, fieldName );
		productVersionIsSet = true;
		databaseNameIsSet = true;
		foundCountIsSet = true;
		recordIteratorIsSet = true;
		//recordIterator.setFinished();
		//recordIterator = null;
		fieldDefinitionsListIsSet = true;
		fieldDefinitions = null;
		metadataError = new SQLException( "Error occurred while reading XML: " + t.toString() );
		metadataError.initCause( t );

		if( t instanceof InterruptedException ) {
			//This is normal, it just means the clent closed the ResultSet before reading the whole thing
		} else {
			log.warning( "Exception " + t.toString() + " occurred while processing request: " + concatUrl );
			if( t instanceof NullPointerException ) {
				log.log(Level.WARNING, "Stack Trace: ", t);
			}
		}
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

		try {
			String versionString = productVersion;
			int mark = versionString.indexOf( '.' );
			if( mark > 0 ) {
				versionString = versionString.substring( 0, mark );
			}
			fmVersion = Integer.valueOf( versionString );
		} catch( NumberFormatException e1 ) {
			log.log( Level.WARNING, "Could not parse product version " + productVersion, e1 );
		}

		notifyAll();
	}

	public Integer getFmVersion() {
		return fmVersion;
	}

	public synchronized String getDatabaseName() {
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

	public long getTotalRecordCount() {
		getDatabaseName(); //Do this first to make sure that we've read the headers in the XML
		return totalRecordCount;
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
			if( errorCodeIsSet && errorCode > 0 ) {
				return 0;
			}
			try {
				wait(1000);
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
			throw new MissingFieldException("The requested fields are not on the layout: " + missingFields, 102, concatUrl, fmLayout, missingFields, username );
		}
		return fieldDefinitions;
	}

	private synchronized void setErrorCode(int code) {
		errorCode = code;
		errorCodeIsSet = true;
		if( code == 0 || code == 401 || code == 105 ) {
			// 0 is ok
			// 401 is no results
			// 105 might be interesting, but it also occurs every time we test connection, so just ignore
		} else {
			//Interesting error
			log.finest( "Error code " + code + " occurred" );
		}
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
		return !( error == 0 || error == 401 );
	}


	/**
	 * In this class we have a reference to FmFieldList called fieldDefinitions (see below). In all the uses of this class,
	 * the state of this reference falls into 3 categories. If it is set by the setSelectFields() method below, it has one
	 * or more FmFields.  Or, it might just contain 1 FmField which contains an asterisk '*" for 'select *'. The last case
	 * is when fieldDefinitions is initially null. This will be the case when it is not set by setSelectFields(). i.e  for
	 * updates and inserts etc.
	 */
	private volatile FmFieldList fieldDefinitions;
	private String fmLayout;
	private FmTable fmTable;
	private boolean useSelectFields = false;
	private long totalRecordCount;

	private volatile String productVersion;
	private volatile String databaseName;
	private volatile int foundCount = -1;
	/**
	 * Contains resultSet FmRecords
	 */
	private volatile ResultQueue recordIterator; // FIX! does this really need to be volatile? -ssb

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
		private Column countColumn;

		class Column {
			String name;
			int[] targetIndices;
			private int maxRepetitions;

			public Column( String name, int[] indices, int maxRepetitions ) {
				this.name = name;
				this.targetIndices = indices;
				this.maxRepetitions = maxRepetitions;
			}

			public void setDataInRow(StringBuilder data, FmRecord row, int dataIndex ) {
				if( targetIndices != null ) {
					final String newValue = data.toString();
					row.addRawValue(newValue, targetIndices, maxRepetitions, dataIndex );
				}
			}
		}

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
		private transient StringBuilder currentData = new StringBuilder(255);
		//private boolean foundDataForColumn;
		private boolean foundDataForRow;
		private boolean foundColStart = false; // added to fix a bug with columns that only have an end element - mww
		//private int columnDataIndex;

		/**
		 * Maps data indices in the XML data with data indices in the currentRow.  If a field in the XML data is not used, the value of that index will be -1.
		 */
		private List<Column> xmlColumns = new ArrayList<Column>( 64 );
		private Iterator<Column> columnIterator;
		private Column currentColumn;
		private int currentRep=0;

		private int nodeType;
		private static final int NODE_TYPE_ERROR = 1;
		private static final int NODE_TYPE_DATA = 2;

		public FmXmlHandler() {
			super();
		}

		public void fatalError(SAXParseException e) throws SAXException {
			//We don't need to log, because we're throwing the exception to the ResultQueue : log.log(Level.SEVERE, e.toString(), e );
			log.log( Level.SEVERE, "fatalError for request: " + concatUrl + "; " + e.toString() );
			super.fatalError(e);
		}

		public void warning( SAXParseException e ) throws SAXException {
			log.log( Level.WARNING, "warning for request: " + concatUrl + "; " + e.toString() );
			super.warning( e );	//To change body of overridden methods use File | Settings | File Templates.
		}

		public void error( SAXParseException e ) throws SAXException {
			log.log( Level.SEVERE, "error for request: " + concatUrl + "; " + e.toString() );
			super.error( e );	//To change body of overridden methods use File | Settings | File Templates.
		}

		/** This is necessary to work around a bug in the Crimson XML parser, which is used in the 1.4 JDK. Crimson
		 * cannot handle relative HTTP URL's, which is what FileMaker uses for it's DTDs: "/fmi/xml/FMPXMLRESULT.dtd"
		 * By returning an empty value here, we short-circuit the DTD lookup process.
		 */
		public InputSource resolveEntity( String publicId, String systemId ) {
			return emptyInput;
		}

		public String getCurrentColumnName() {
			return currentColumn == null ? null : currentColumn.name;
		}

		public void startDocument() {
			//if( debugMode ) requestContent.append( "Starting document\n" );
			log.log(Level.FINEST, "Start parsing response");
			setRecordIterator(new ResultQueue(256*1024, 64*1024));  // FIX! is this a good size? -britt
			nodeType = 0;
		}

		public void startElement(String uri, String xlocalName, String qName, Attributes attributes) throws SAXException {
			if( Thread.currentThread().isInterrupted() ) {
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
			if( "DATA".equals( qName ) ) {
				//Do nothing, this is just an optimization to break out of the else loop sooner	
			} else if ("ROW".equals(qName)) {
				foundDataForRow = false;
				final String recidString = attributes.getValue( "RECORDID" );
				currentRow = new FmRecord(fieldDefinitions, recidString, Long.valueOf(attributes.getValue("MODID")));
				columnIterator = xmlColumns.iterator();
				currentColumn = columnIterator.next(); //This is always the 'recid' column
				currentColumn.setDataInRow( new StringBuilder( recidString ), currentRow, 1 );
			} else if ("COL".equals(qName)) {
				foundColStart = true; // added to fix a bug with columns that only have an end element - mww
				try {
					currentColumn = columnIterator.next();
				} catch( NoSuchElementException e ) {
					throw e;
				}
			}
			// One-shot nodes
			else if ("FIELD".equals(qName)) {
				String fieldName = attributes.getValue("NAME");

				String fieldType = attributes.getValue("TYPE");
				FmFieldType theType = FmFieldType.typesByName.get(fieldType);
				boolean allowsNulls = "YES".equals(attributes.getValue("EMPTYOK"));
				int maxRepeat = Integer.parseInt(attributes.getValue("MAXREPEAT"));

				if (!useSelectFields) { // this is a select * query.  create a new field and add it to the fieldDefinitions
					//We don't know these things - they are only available in fmpresultset.xml
					boolean isReadOnly = false;
					boolean isAutoEnter = false;
					boolean isGlobal = false;
					boolean isCalc = false;
					boolean isSummary = false;
					FmField fmField = new FmField(fmTable, fieldName, fieldName, theType, allowsNulls, isReadOnly, isAutoEnter, maxRepeat, isGlobal, isCalc, isSummary );
					fieldDefinitions.add(fmField);
				}
				handleParsedMetaDataField(fieldName, maxRepeat, theType, allowsNulls); //FIX!! By calling it at this time, the target indeces will not be correct if the same field occurs more than once on the layout - we'll only get the column index of the first field, until we get to the duplicate field, at which point we'll get both column indeces. I don't think this actually causes a problem though --jsb
			} else if( "METADATA".equals( qName ) ) { //Create a virtual 'recid' column
				handleParsedMetaDataField( "recid", 1, FmFieldType.RECID, false );
				final List<FmField> fields = fieldDefinitions.getFields();
				for (int i = 0, fieldsSize = fields.size(); i < fieldsSize; i++) {
					final FmField eachField = fields.get(i);
					if (eachField.getType() == FmFieldType.COUNT) {
						xmlColumns.add(countColumn = new Column(eachField.getColumnName(), new int[] {i}, 1));
					}
				}
			} else if ("RESULTSET".equals(qName)) {
				setFoundCount( Integer.valueOf( attributes.getValue( "FOUND" ) ) ); //foundCount = Integer.valueOf(attributes.getValue("FOUND")).intValue();
				log.log(Level.FINE, "Resultset size: " + foundCount);
				nodeType = NODE_TYPE_DATA;
			} else if ("PRODUCT".equals(qName)) {
				setDatabaseName(attributes.getValue("NAME")); // databaseName = attributes.getValue("NAME");
				setProductVersion(attributes.getValue("VERSION")); // productVersion = attributes.getValue("VERSION");
				setErrorCode( Integer.valueOf( currentData.toString() ) ); //This is the value read from the <ERRORCODE>foo</ERRORCODE> section
				currentData.setLength( 0 );
			} else if ("DATABASE".equals(qName)) {
				fmLayout = attributes.getValue( "LAYOUT" );
				fmTable = new FmTable( attributes.getValue("NAME") );
				totalRecordCount = Long.valueOf( attributes.getValue( "RECORDS" ) );

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
		 * @param theType
		 * @param allowsNulls
		 */
		private void handleParsedMetaDataField(String fieldName, int maxRepetitions, FmFieldType theType, boolean allowsNulls) {
			int whichColumn = -1;
			int[] targetIndices = null;
			do {
				int indexToTry = whichColumn + 1;
				whichColumn = fieldDefinitions.indexOfFieldWithColumnName(fieldName, indexToTry );
				if (whichColumn != -1) { // set the type and nullable if this is a field in the field definitions
					FmField fmField = fieldDefinitions.get(whichColumn);
					fmField.setType(theType);
					fmField.setMaxReps( maxRepetitions );
					fmField.setNullable(allowsNulls);

					if( targetIndices == null ) {
						targetIndices = new int[1];
						targetIndices[0] = whichColumn;
						if( missingFields != null ) { //Remove this from the list of missing fields
							missingFields.remove( fieldName.toLowerCase() );
						}
					} else { //We've already found at least one occurrence in the SELECT field list; this is a subsequent one
						int[] biggerArray = new int[ targetIndices.length + 1 ]; //Copy into a bigger array and add this index to the end
						System.arraycopy( targetIndices, 0, biggerArray, 0, targetIndices.length );
						biggerArray[ biggerArray.length -1 ] = whichColumn;
						targetIndices = biggerArray;
					}
				}
			} while( whichColumn != -1 );
			xmlColumns.add( new Column(fieldName, targetIndices, maxRepetitions) );
		}

		public void endDocument() throws SAXException {
			//recordIterator.setFinished(); //Don't need to call this, it happens in the finally block of the parse() command.
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			//if( debugMode ) requestContent.append( "</" + qName + ">" );
			if( "DATA".equals(qName) ) {

				//foundDataForColumn = true;
				foundDataForRow = true;
				currentColumn.setDataInRow( currentData, currentRow, currentRep+1 );
				currentRep++;

				// OPTIMIZE! Not sure which is faster, calling setLength(0), delete() or creating new object --jsb : probably setLength(0), as the char buffer is re-used -ssb
				//currentData.setLength(0);
				//currentData.delete( 0, currentData.length() );
				currentData = new StringBuilder( 255 );

			}
			if ("ROW".equals(qName)) {
				if (countColumn != null) {
					countColumn.setDataInRow(new StringBuilder(String.valueOf(foundCount)), currentRow, 0);
					foundDataForRow = true;
				}
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
				if (countColumn != null) {
					closeRequest();
				}
				sizeEstimate = 0; // set it to 0 and start estimating again
			}
			if("COL".equals(qName)){ // added to fix a bug with columns that only have an end element - mww
				if( currentRep == 0 ) { //If there is a related field but the relationship is invalid, then FM does not contain a <DATA> element. We need to catch that specially and treat it as null.
					currentData.setLength( 0 );
					currentColumn.setDataInRow( currentData, currentRow, 1 ); //FIX!! This should actually be null instead of an empty string, but currently that will throw a NPE - need to fix many things to support this. --jsb
				}
				if(!foundColStart){ // do only if there was no start COL element FIX! This doesn't seem necessary to me --jsb
					currentColumn = columnIterator.next();
				}
				currentRep = 0;
			}
			if ("METADATA".equals(qName)) { // Create the usedorder array.  This is done once.
				// when i come to the metadata tag, i know all of the fields that are going to be in the table, so
				// I can let people get the fieldDefinitions

				/* Waiting until this point to add field definitions doesn't work for SELECT statements, because the XML fields don't get added to fieldDefinitions for selects. --jsb
				for( FmField field : fieldDefinitions.getFields() ) {
					handleParsedMetaDataField( field.getColumnName(), field.getMaxReps(), field.getType(), field.isNullable() );
				}*/

				synchronized (FmXmlRequest.this) { // this is different from the other attributes in the xml, since this one is being built on the fly and the variable is not just being "set" once we're finished reading it
					fieldDefinitionsListIsSet = true;
					if( fieldDefinitions == null ) {
						throw new IllegalStateException( "Finished parsing METADATA, bt did not create a fieldDefinitions list" ); //This should never happen. --jsb
					}
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
					currentData.append( ch, start, length );
					break;
				case NODE_TYPE_DATA:
					if( currentColumn != null && currentColumn.targetIndices != null ) {
						currentData.append(ch, start, length);
					}
					break;
			}
		} // end of characters

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

		missingFields = new LinkedHashSet<String>( fieldDefinitions.size() );
		for( FmField eachField : fieldDefinitions.getFields() ) {
			if( "recid".equalsIgnoreCase( eachField.getColumnName() ) ) {
				eachField.setNullable( false );
				eachField.setReadOnly( true );
				eachField.setType( FmFieldType.RECID );
			} else if (eachField.getType() == FmFieldType.COUNT) {
				// don't require this field name
			} else {
				missingFields.add( eachField.getColumnNameNoRep().toLowerCase() );
			}
		}
	}

	public static class HttpAuthenticationException extends FileMakerException {
		public HttpAuthenticationException(String message, String username, String requestUrl) {
			super(212, "Invalid FileMaker user account and/or password. Make sure that the FMXML extended privilege is enabled for this account. Please try again - username '" + username + "'", requestUrl, username );
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
