package com.prosc.fmpjdbc;

import com.prosc.shared.MBeanUtils;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
 * Initially, all these methods should be modified to wrap around a ddtek Connection. We can then selectively
 * start replacing them with our own methods.
 *
 * Supported properties:
 * ssl: True for ssl encryption (be sure to pass the correct SSL port in the jdbc URL). The default is false.
 * fmversion: A decimal number indicating the version of FileMaker. Versions 5 and higher are supported. The default is 7.
 * catalogseparator: A string that is used for combining the databasename and table name. For example, if this is set to "|", then "Contacts|detail" would refer to the detail layout of the Contacts database. If unspecified, this defaults to ".". This is necessary for use with EOF, because EOF will complain when creating a new EOModel if any of the layout names are the same.
 */
public class FmConnection implements Connection {
	private static final Logger log = Logger.getLogger( FmConnection.class.getName() );
	public static final String URL_EXAMPLE = " URL should be in the form 'jdbc:fmp360://hostname[:portNumber]/[DatabaseName][?key1=value1&key2=value2&...]";
	private String mBeanName = FmConnection.class.getName() + ":instance=" + System.identityHashCode( this );
	private String url;
	private Properties properties;
	private URL xmlUrl;
	//private String databaseName;
	//private FmXmlRequest requestHandler;
	//private FmXmlRequest recIdHandler;
	private FmMetaData metaData;
	private float fmVersion;
	private String catalog;
	private boolean isClosed;


	/** Creates a connection to the ddtekDriver and keeps a private variable so that we can forward
	 * unsupported calls to the other driver.
	 * Also verifies that we are able to connect to the FileMaker Server, and does all one-shot preparation,
	 * such as parsing DTDs.
	 * @param url
	 * @param properties
	 */
	public FmConnection( String url, Properties properties ) throws MalformedURLException, SQLException {
		this.url = url;
		isClosed = false;
		this.properties = properties;
		extractUrlProperties();
		String logLevel =  properties.getProperty("loglevel" ); // default log level is INFO
		if( logLevel != null ) { //Override the JVM logging level with the one in the URL
			Logger packageLogger = Logger.getLogger( "com.prosc.fmpjdbc" );
			packageLogger.setLevel(Level.parse(logLevel));
		}
		log.log(Level.CONFIG, "Connecting to " + url);
		fmVersion = Float.valueOf( properties.getProperty("fmversion", "7") ).floatValue();
		if( fmVersion >= 7 ) {
			//requestHandler = new FmXmlRequest( getProtocol(), getHost(), "/fmi/xml/FMPXMLRESULT.xml", getPort(), getUsername(), getPassword() );
			//recIdHandler = new FmXmlRequest( getProtocol(), getHost(), "/fmi/xml/FMPXMLRESULT.xml", getPort(), getUsername(), getPassword(), fmVersion );
		} else if( fmVersion >= 5 ) {
			//requestHandler = new FmXmlRequest( getProtocol(), getHost(), "/FMPro", getPort(), getUsername(), getPassword() );
			//requestHandler.setPostPrefix("-format=-fmp_xml&");
			//recIdHandler = new FmXmlRequest( getProtocol(), getHost(), "/FMPro", getPort(), getUsername(), getPassword(), fmVersion );
			//recIdHandler.setPostPrefix("-format=-fmp_xml&");
		} else throw new IllegalArgumentException(fmVersion + " is not a valid version number. Currently, only FileMaker versions 5 and higher are supported.");

		// lastly, check the username/pwd are valid by trying to access the db
		if (catalog != null) {
			testUsernamePassword();
			//((FmMetaData) getMetaData()).testUsernamePassword(); // this will throw a new SQLException(FmXmlRequest.HttpAuthenticationException)
			//FIX!! Right now, this is very inefficient and runs every time a connection is open. We shoudl 1) make it more efficient, 2) make it configurable whether to do this, and 3) perhaps skip it if the same credentials are applied multiple times --jsb
		}
		// Commented out until we switched to JDK 1.5: MBeanUtils.registerMBean( mBeanName, this );
	}

	private void testUsernamePassword() throws SQLException {
		FmXmlRequest request = new FmXmlRequest(getProtocol(), getHost(), getFMVersionUrl(),
				getPort(), getUsername(), getPassword(), getFmVersion() );
		try {
			String databaseName = getCatalog();
			String encodedDBName;
			try {
				encodedDBName = URLEncoder.encode(databaseName, "utf-8");
			} catch( UnsupportedEncodingException e ) {
				throw new RuntimeException(e);
			}
			String postArgs = "-db=" + encodedDBName + "&-lay=ProscNoSuchTable&-view";
			try {
				request.doRequest( postArgs );
			} catch( FmXmlRequest.HttpAuthenticationException e ) {
				//Username and password are invalid
				throw new SQLException( e.getMessage() );
			} catch( IOException e ) {
				SQLException sqlE = new SQLException();
				sqlE.initCause( e );
				throw sqlE;
			} catch( FileMakerException e ) {
				if( request.getErrorCode() == 105 ) { //Success, our username/password is valid and there is no such layout
					return;
				} else throw e;
			}
		} finally {
			request.closeRequest();
		}
	}

	private volatile int resultSetCount = 0;
	void notifyNewResultSet( FmResultSet set ) {
		resultSetCount++;
	}

	void notifyClosedResultSet( FmResultSet set ) {
		resultSetCount--;
	}

	public int getResultSetCount() {
		return resultSetCount;
	}

	public String getFMVersionUrl() {
		if (fmVersion >= 7) {
			return "/fmi/xml/FMPXMLRESULT.xml";
		} else if (fmVersion >= 5) {
			return "/FMPro";
		} else throw new IllegalArgumentException(fmVersion + " is not a valid version number. Currently, only FileMaker version 5 and higher are supported.");
	}

	public String getProtocol() {
		return xmlUrl.getProtocol();
	}

	public String getHost() {
		return xmlUrl.getHost();
	}

	public int getPort() {
		return xmlUrl.getPort();
	}
	private void extractUrlProperties() throws MalformedURLException {
		int mark = url.indexOf( "://" );
		try {
			if( mark == - 1) throw new MalformedURLException(url + " is not a valid JDBC URL." );
			String urlTrailer = url.substring( mark );
			xmlUrl = new URL("http" + urlTrailer);
			String path = xmlUrl.getPath();
			if (path == null || path.length() == 0) throw new MalformedURLException("Missing database name.");
			String catalogString = path.substring(1);
			if( catalogString != null && catalogString.length() > 1 ) setCatalog( catalogString ); //FIX!!! What do we do if there is no database name specified in the URL?
			if( xmlUrl.getQuery() != null ) {
				for( StringTokenizer queryParamTokens = new StringTokenizer( xmlUrl.getQuery(), "&", false ); queryParamTokens.hasMoreElements(); ) {
					for( StringTokenizer keyValueTokens = new StringTokenizer(queryParamTokens.nextToken(), "=", false); keyValueTokens.hasMoreElements(); ) {
						String key = keyValueTokens.nextToken();
						String value = keyValueTokens.hasMoreTokens() ? keyValueTokens.nextToken() : "";
						properties.setProperty( key.toLowerCase(), value );
					}
				}
			}
			if( "true".equals(properties.getProperty("ssl")) ) {
				xmlUrl = new URL("https" + urlTrailer);
			}
		} catch( MalformedURLException e ) {
			throw new MalformedURLException( e.getMessage() + URL_EXAMPLE );
		}
	}

	public float getFmVersion() {
		return fmVersion;
	}

	public URL getXmlUrl() {
		return xmlUrl;
	}

	/*public String getDatabaseName() {
		return databaseName;
	}*/

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return properties.getProperty("user");
	}

	public String getPassword() { //FIX! Should this be public? It's needed by FmBlob
		return properties.getProperty("password");
	}

	public Properties getProperties() {
		return properties;
	}

	//FmXmlRequest getXmlRequestHandler() { return requestHandler; }

	//FmXmlRequest getRecIdHandler() { return recIdHandler; }


	// --- These methods must be implemented ---

	public Statement createStatement() throws SQLException {
		return new FmStatement(this);
	}

	public void setTransactionIsolation( int i ) throws SQLException {
		if( i != Connection.TRANSACTION_NONE ) throw new SQLException("Transactions are not supported by FileMaker.");
	}

	public int getTransactionIsolation() throws SQLException {
		return Connection.TRANSACTION_NONE;
	}

	public DatabaseMetaData getMetaData() throws SQLException {
		try {
			if( metaData == null ) metaData = new FmMetaData( this );
			metaData.setCatalogSeparator( properties.getProperty("catalogseparator", "|") ); //This used to default to ".", changed it to "|" to fix duplicate entity names in EOF. Needs testing! --jsb
		} catch (IOException e) {
			SQLException sqle = new SQLException(e.toString());
			sqle.initCause(e);
			throw sqle;
		}
		return metaData;
	}

	public void close() throws SQLException {
		//requestHandler.closeRequest();
		//requestHandler = null;
		isClosed = true;
		//recIdHandler.closeRequest();
		//recIdHandler = null;
		// Commented out until we switched to JDK 1.5: MBeanUtils.deregisterMBean( mBeanName );
	}

	public boolean isClosed() throws SQLException {
		return isClosed;
	}

	public PreparedStatement prepareStatement( String s ) throws SQLException {
		return new FmPreparedStatement( this, s);
	}

	public CallableStatement prepareCall( String s ) throws SQLException {
		FmCallableStatement fmcs = new FmCallableStatement(this);
		fmcs.setScriptName(s);
		return fmcs;
	}

	public void setCatalog( String s ) { //Maybe this is what we should be doing instead of setDatabaseName()? --jsb
		catalog = s;
	}

	public String getCatalog() throws SQLException {
		return catalog;
	}

	//---These methods can be ignored

	public String nativeSQL( String s ) throws SQLException {
		throw new AbstractMethodError( "nativeSQL is not implemented yet." ); //FIX! Broken placeholder
	}

	public void setAutoCommit( boolean b ) throws SQLException {
		if( b ) {
			//Ignore; we're always in auto-commit mode
		} else {
			//throw new UnsupportedOperationException( "FileMaker does not support transactions; you cannot set auto commit to false." );
			log.warning( "FileMaker does not support transactions; setting auto commit to false has no effect." );
		}
	}

	/** This always returns true, because we do not support real transactions. */
	public boolean getAutoCommit() throws SQLException {
		return true;
	}

	/** This method does nothing. */
	public void commit() throws SQLException {}

	/** This method does nothing. */
	public void rollback() throws SQLException {}

	public void setReadOnly( boolean b ) throws SQLException {
		throw new AbstractMethodError( "setReadOnly is not implemented yet." ); //FIX! Broken placeholder
	}

	public boolean isReadOnly() throws SQLException {
		throw new AbstractMethodError( "isReadOnly is not implemented yet." ); //FIX! Broken placeholder
	}

	public SQLWarning getWarnings() throws SQLException {
		return null; //FIX! Should I be doing something here?
	}

	public void clearWarnings() throws SQLException {
		//FIX! Should I be doing something here?
	}

	public Statement createStatement( int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "createStatement is not implemented yet." ); //FIX! Broken placeholder
	}

	public PreparedStatement prepareStatement( String s, int resultSetType, int resultSetConcurrency ) throws SQLException {
		if( resultSetType != ResultSet.TYPE_FORWARD_ONLY ) throw new UnsupportedOperationException("Forward-only is the only type of supported ResultSet" );
		if( resultSetConcurrency != ResultSet.CONCUR_READ_ONLY ) throw new UnsupportedOperationException("Read-only is the only type of concurrency supported" );
		return prepareStatement( s );
	}

	public CallableStatement prepareCall( String s, int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "prepareCall is not implemented yet." ); //FIX! Broken placeholder
	}

	public Map getTypeMap() throws SQLException {
		throw new AbstractMethodError( "getTypeMap is not implemented yet." ); //FIX! Broken placeholder
	}

	public void setHoldability( int i ) throws SQLException {
		throw new AbstractMethodError( "setHoldability is not implemented yet." ); //FIX! Broken placeholder
	}

	public int getHoldability() throws SQLException {
		throw new AbstractMethodError( "getHoldability is not implemented yet." ); //FIX! Broken placeholder
	}

	public Savepoint setSavepoint() throws SQLException {
		throw new AbstractMethodError( "setSavepoint is not implemented yet." ); //FIX! Broken placeholder
	}

	public Savepoint setSavepoint( String s ) throws SQLException {
		throw new AbstractMethodError( "setSavepoint is not implemented yet." ); //FIX! Broken placeholder
	}

	public void rollback( Savepoint savepoint ) throws SQLException {
		throw new AbstractMethodError( "rollback is not implemented yet." ); //FIX! Broken placeholder
	}

	public void releaseSavepoint( Savepoint savepoint ) throws SQLException {
		throw new AbstractMethodError( "releaseSavepoint is not implemented yet." ); //FIX! Broken placeholder
	}

	public Statement createStatement( int i, int i1, int i2 ) throws SQLException {
		throw new AbstractMethodError( "createStatement is not implemented yet." ); //FIX! Broken placeholder
	}

	public PreparedStatement prepareStatement( String s, int i, int i1, int i2 ) throws SQLException {
		throw new AbstractMethodError( "prepareStatement is not implemented yet." ); //FIX! Broken placeholder
	}

	public CallableStatement prepareCall( String s, int i, int i1, int i2 ) throws SQLException {
		throw new AbstractMethodError( "prepareCall is not implemented yet." ); //FIX! Broken placeholder
	}

	public PreparedStatement prepareStatement( String s, int i ) throws SQLException {
		return prepareStatement( s + "" );
	}

	public PreparedStatement prepareStatement( String s, int[] ints ) throws SQLException {
		throw new AbstractMethodError( "prepareStatement is not implemented yet." ); //FIX! Broken placeholder
	}

	public PreparedStatement prepareStatement( String s, String[] strings ) throws SQLException {
		throw new AbstractMethodError( "prepareStatement is not implemented yet." ); //FIX! Broken placeholder
	}

	//---These methods were added to the interface in Java 1.5.... ? How bizarre that they would add methods to an existing interface???

	public void setTypeMap( Map<String, Class<?>> map ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public Clob createClob() throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public Blob createBlob() throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public boolean isValid( int i ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public String getClientInfo( String s ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public Properties getClientInfo() throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public Array createArrayOf( String s, Object[] objects ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public Struct createStruct( String s, Object[] objects ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public <T> T unwrap( Class<T> aClass ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public boolean isWrapperFor( Class<?> aClass ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}
	
	// === Java 6 stuff - this must be commented out to compile in Java 5. Screw you Sun! ===

	public NClob createNClob() throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public SQLXML createSQLXML() throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setClientInfo( String s, String s1 ) throws SQLClientInfoException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void setClientInfo( Properties properties ) throws SQLClientInfoException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}
}
