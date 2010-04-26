package com.prosc.fmpjdbc;

import java.sql.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Calendar;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.InputStream;
import java.io.Reader;
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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 16, 2005 Time: 6:38:55 PM
 */
public class FmCallableStatement extends FmPreparedStatement implements CallableStatement {
	private static final String woPrefix = "{ call ";
	private static final String woSuffix = "}";

	private FmXmlRequest request; //FIX!! Create a new instance as needed
	private String scriptName;
	private String postArgs = "-db=<database>&-lay=<layout>&-max=0&-script=<script>&-findany";

	public FmCallableStatement( FmConnection connection ) {
    super( connection );
    request = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
            connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion()); //connection.getXmlRequestHandler();
  }

	public void setScriptName(String scriptName) {
		if( scriptName.startsWith(woPrefix) ) {
			boolean stripSuffix = scriptName.endsWith(woSuffix);
			scriptName = scriptName.substring( woPrefix.length(), scriptName.length() - (stripSuffix ? 1 : 0) );
		}
		this.scriptName = scriptName;
	}

	public String getScriptName() { return scriptName; }

	public boolean execute() throws SQLException {
		postArgs = postArgs.replaceAll("<database>", getConnection().getCatalog() );
		postArgs = postArgs.replaceAll("<layout>", ((FmMetaData)getConnection().getMetaData()).getAnyTableName());
		postArgs = postArgs.replaceAll("<script>", scriptName);

		try {
			request.doRequest(postArgs);
		} catch (IOException ioe) {
			SQLException sqle = new SQLException(ioe.toString());
			sqle.initCause(ioe);
			throw sqle;
    } finally {
		request.closeRequest();
    }

		return true;
	}

	//------ TBD if need -------//

	public void registerOutParameter( int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "registerOutParameter is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void registerOutParameter( int i, int i1, int i2 ) throws SQLException {
		throw new AbstractMethodError( "registerOutParameter is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean wasNull() throws SQLException {
		throw new AbstractMethodError( "wasNull is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getString( int i ) throws SQLException {
		throw new AbstractMethodError( "getString is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean getBoolean( int i ) throws SQLException {
		throw new AbstractMethodError( "getBoolean is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public byte getByte( int i ) throws SQLException {
		throw new AbstractMethodError( "getByte is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public short getShort( int i ) throws SQLException {
		throw new AbstractMethodError( "getShort is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getInt( int i ) throws SQLException {
		throw new AbstractMethodError( "getInt is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public long getLong( int i ) throws SQLException {
		throw new AbstractMethodError( "getLong is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public float getFloat( int i ) throws SQLException {
		throw new AbstractMethodError( "getFloat is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public double getDouble( int i ) throws SQLException {
		throw new AbstractMethodError( "getDouble is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public BigDecimal getBigDecimal( int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "getBigDecimal is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public byte[] getBytes( int i ) throws SQLException {
		throw new AbstractMethodError( "getBytes in FmCallableStatement is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Date getDate( int i ) throws SQLException {
		throw new AbstractMethodError( "getDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Time getTime( int i ) throws SQLException {
		throw new AbstractMethodError( "getTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Timestamp getTimestamp( int i ) throws SQLException {
		throw new AbstractMethodError( "getTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Object getObject( int i ) throws SQLException {
		throw new AbstractMethodError( "getObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public BigDecimal getBigDecimal( int i ) throws SQLException {
		throw new AbstractMethodError( "getBigDecimal is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Ref getRef( int i ) throws SQLException {
		throw new AbstractMethodError( "getRef is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Blob getBlob( int i ) throws SQLException {
		throw new AbstractMethodError( "getBlob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Clob getClob( int i ) throws SQLException {
		throw new AbstractMethodError( "getClob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Array getArray( int i ) throws SQLException {
		throw new AbstractMethodError( "getArray is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Date getDate( int i, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "getDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Time getTime( int i, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "getTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Timestamp getTimestamp( int i, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "getTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void registerOutParameter( int i, int i1, String s ) throws SQLException {
		throw new AbstractMethodError( "registerOutParameter is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void registerOutParameter( String s, int i ) throws SQLException {
		throw new AbstractMethodError( "registerOutParameter is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void registerOutParameter( String s, int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "registerOutParameter is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void registerOutParameter( String s, int i, String s1 ) throws SQLException {
		throw new AbstractMethodError( "registerOutParameter is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public URL getURL( int i ) throws SQLException {
		throw new AbstractMethodError( "getURL is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setURL( String s, URL url ) throws SQLException {
		throw new AbstractMethodError( "setURL is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNull( String s, int i ) throws SQLException {
		throw new AbstractMethodError( "setNull is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBoolean( String s, boolean b ) throws SQLException {
		throw new AbstractMethodError( "setBoolean is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setByte( String s, byte b ) throws SQLException {
		throw new AbstractMethodError( "setByte is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setShort( String s, short i ) throws SQLException {
		throw new AbstractMethodError( "setShort is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setInt( String s, int i ) throws SQLException {
		throw new AbstractMethodError( "setInt is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setLong( String s, long l ) throws SQLException {
		throw new AbstractMethodError( "setLong is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setFloat( String s, float v ) throws SQLException {
		throw new AbstractMethodError( "setFloat is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setDouble( String s, double v ) throws SQLException {
		throw new AbstractMethodError( "setDouble is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBigDecimal( String s, BigDecimal decimal ) throws SQLException {
		throw new AbstractMethodError( "setBigDecimal is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setString( String s, String s1 ) throws SQLException {
		throw new AbstractMethodError( "setString is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBytes( String s, byte[] bytes ) throws SQLException {
		throw new AbstractMethodError( "setBytes is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setDate( String s, Date date ) throws SQLException {
		throw new AbstractMethodError( "setDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setTime( String s, Time time ) throws SQLException {
		throw new AbstractMethodError( "setTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setTimestamp( String s, Timestamp timestamp ) throws SQLException {
		throw new AbstractMethodError( "setTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setAsciiStream( String s, InputStream stream, int i ) throws SQLException {
		throw new AbstractMethodError( "setAsciiStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBinaryStream( String s, InputStream stream, int i ) throws SQLException {
		throw new AbstractMethodError( "setBinaryStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setObject( String s, Object o, int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "setObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setObject( String s, Object o, int i ) throws SQLException {
		throw new AbstractMethodError( "setObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setObject( String s, Object o ) throws SQLException {
		throw new AbstractMethodError( "setObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setCharacterStream( String s, Reader reader, int i ) throws SQLException {
		throw new AbstractMethodError( "setCharacterStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setDate( String s, Date date, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "setDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setTime( String s, Time time, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "setTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setTimestamp( String s, Timestamp timestamp, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "setTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNull( String s, int i, String s1 ) throws SQLException {
		throw new AbstractMethodError( "setNull is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getString( String s ) throws SQLException {
		throw new AbstractMethodError( "getString is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean getBoolean( String s ) throws SQLException {
		throw new AbstractMethodError( "getBoolean is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public byte getByte( String s ) throws SQLException {
		throw new AbstractMethodError( "getByte is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public short getShort( String s ) throws SQLException {
		throw new AbstractMethodError( "getShort is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getInt( String s ) throws SQLException {
		throw new AbstractMethodError( "getInt is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public long getLong( String s ) throws SQLException {
		throw new AbstractMethodError( "getLong is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public float getFloat( String s ) throws SQLException {
		throw new AbstractMethodError( "getFloat is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public double getDouble( String s ) throws SQLException {
		throw new AbstractMethodError( "getDouble is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public byte[] getBytes( String s ) throws SQLException {
		throw new AbstractMethodError( "getBytes in FmCallableStatement is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Date getDate( String s ) throws SQLException {
		throw new AbstractMethodError( "getDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Time getTime( String s ) throws SQLException {
		throw new AbstractMethodError( "getTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Timestamp getTimestamp( String s ) throws SQLException {
		throw new AbstractMethodError( "getTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Object getObject( String s ) throws SQLException {
		throw new AbstractMethodError( "getObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public BigDecimal getBigDecimal( String s ) throws SQLException {
		throw new AbstractMethodError( "getBigDecimal is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Ref getRef( String s ) throws SQLException {
		throw new AbstractMethodError( "getRef is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Blob getBlob( String s ) throws SQLException {
		throw new AbstractMethodError( "getBlob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Clob getClob( String s ) throws SQLException {
		throw new AbstractMethodError( "getClob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Array getArray( String s ) throws SQLException {
		throw new AbstractMethodError( "getArray is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Date getDate( String s, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "getDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Time getTime( String s, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "getTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Timestamp getTimestamp( String s, Calendar calendar ) throws SQLException {
		throw new AbstractMethodError( "getTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public URL getURL( String s ) throws SQLException {
		throw new AbstractMethodError( "getURL is not implemented yet." ); //FIX!!! Broken placeholder
	}
	
	// ==== These methods were added in JDK 1.5 ====

	public Object getObject( int i, Map<String, Class<?>> map ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public Object getObject( String parameterName, Map<String, Class<?>> map ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public RowId getRowId( int i ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public RowId getRowId( String s ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setRowId( String s, RowId id ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNString( String s, String s1 ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNCharacterStream( String s, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNClob( String s, NClob clob ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setClob( String s, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBlob( String s, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNClob( String s, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public NClob getNClob( int i ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public NClob getNClob( String s ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setSQLXML( String s, SQLXML sqlxml ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public SQLXML getSQLXML( int i ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public SQLXML getSQLXML( String s ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getNString( int i ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getNString( String s ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public Reader getNCharacterStream( int i ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public Reader getNCharacterStream( String s ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public Reader getCharacterStream( int i ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public Reader getCharacterStream( String s ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBlob( String s, Blob blob ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setClob( String s, Clob clob ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setAsciiStream( String s, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBinaryStream( String s, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setCharacterStream( String s, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setAsciiStream( String s, InputStream stream ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBinaryStream( String s, InputStream stream ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setCharacterStream( String s, Reader reader ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNCharacterStream( String s, Reader reader ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setClob( String s, Reader reader ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setBlob( String s, InputStream stream ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setNClob( String s, Reader reader ) throws SQLException {
		throw new AbstractMethodError( "This feature has not been implemented yet." ); //FIX!!! Broken placeholder
	}
}
