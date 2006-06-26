package com.prosc.fmpjdbc;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Filter;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

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
	private String url;
	private Properties properties;
	private URL xmlUrl;
	//private String databaseName;
	//private FmXmlRequest requestHandler;
	//private FmXmlRequest recIdHandler;
	private FmMetaData metaData;
	private float fmVersion;
	private String catalog;
	Logger logger = Logger.getLogger("com.prosc.fmpjdbc");
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
    String logLevel =  properties.getProperty("loglevel", "INFO" ); // default log level is INFO
    logger.setLevel(Level.parse(logLevel));
    if (logger.isLoggable(Level.FINE)) {
      Logger.getLogger( "com.prosc.fmpjdbc").setLevel( logger.getLevel() );
//			Logger.getLogger("").getHandlers()[0].setLevel(logger.getLevel());
    }
    if (logger.isLoggable(Level.CONFIG)) {
      logger.log(Level.CONFIG, "Connecting to " + url + " with properties " + properties);
    }
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
		  ((FmMetaData) getMetaData()).testUsernamePassword(); // this will throw a new SQLException(FmXmlRequest.HttpAuthenticationException)
	  }
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
		String urlTrailer = url.substring( url.indexOf("://") );
		xmlUrl = new URL("http" + urlTrailer);
		String path = xmlUrl.getPath();
		if (path == null || path.length() == 0) throw new MalformedURLException("Missing database name");
		String catalogString = path.substring(1);
		if( catalogString != null && catalogString.length() > 1 ) setCatalog( catalogString ); //FIX!!! What do we do if there is no database name specified in the URL?
		if( xmlUrl.getQuery() != null ) {
			for( StringTokenizer queryParamTokens = new StringTokenizer( xmlUrl.getQuery(), "&", false ); queryParamTokens.hasMoreElements(); ) {
				for( StringTokenizer keyValueTokens = new StringTokenizer(queryParamTokens.nextToken(), "=", false); keyValueTokens.hasMoreElements(); ) {
					String key = keyValueTokens.nextToken();
					String value = keyValueTokens.hasMoreTokens() ? keyValueTokens.nextToken() : "";
					properties.setProperty( key, value );
				}
			}
		}
		if( "true".equals(properties.getProperty("ssl")) ) {
			xmlUrl = new URL("https" + urlTrailer);
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
		throw new AbstractMethodError( "setAutoCommit is not implemented yet." ); //FIX! Broken placeholder
	}

	public boolean getAutoCommit() throws SQLException {
		throw new AbstractMethodError( "getAutoCommit is not implemented yet." ); //FIX! Broken placeholder
	}

	public void commit() throws SQLException {
		throw new AbstractMethodError( "commit is not implemented yet." ); //FIX! Broken placeholder
	}

	public void rollback() throws SQLException {
		throw new AbstractMethodError( "rollback is not implemented yet." ); //FIX! Broken placeholder
	}

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

	public PreparedStatement prepareStatement( String s, int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "prepareStatement is not implemented yet." ); //FIX! Broken placeholder
	}

	public CallableStatement prepareCall( String s, int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "prepareCall is not implemented yet." ); //FIX! Broken placeholder
	}

	public Map getTypeMap() throws SQLException {
		throw new AbstractMethodError( "getTypeMap is not implemented yet." ); //FIX! Broken placeholder
	}

	public void setTypeMap( Map map ) throws SQLException {
		throw new AbstractMethodError( "setTypeMap is not implemented yet." ); //FIX! Broken placeholder
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
		throw new AbstractMethodError( "prepareStatement is not implemented yet." ); //FIX! Broken placeholder
	}

	public PreparedStatement prepareStatement( String s, int[] ints ) throws SQLException {
		throw new AbstractMethodError( "prepareStatement is not implemented yet." ); //FIX! Broken placeholder
	}

	public PreparedStatement prepareStatement( String s, String[] strings ) throws SQLException {
		throw new AbstractMethodError( "prepareStatement is not implemented yet." ); //FIX! Broken placeholder
	}
}
