package com.prosc.fmpjdbc;

import java.sql.*;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;

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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 17, 2005 Time: 1:41:57 AM
 */
public class FmResultSet implements ResultSet {
	private static final Logger log = Logger.getLogger( FmResultSet.class.getName() );
	private Iterator fmRecords;
	private FmResultSetMetaData metaData;
	private FmRecord currentRecord;
	private FmFieldList fieldDefinitions;
	private FmConnection connection;

	private boolean isOpen = true;
	//private boolean isBeforeFirst = true;
	//private boolean isFirst = false;
	private boolean isAfterLast = false;
	//private boolean isLast = false;
	private Logger logger = Logger.getLogger( FmResultSet.class.getName() );
	private int rowNum = -1;

	/** Pass in an iterator of {@link FmRecord} objects, which will be used as the ResultSet. Pass null for an empty ResultSet. */
	public FmResultSet(Iterator fmRecordsIterator, FmFieldList fieldDefinitions, FmConnection connection) {
		this.connection = connection;
		if( fmRecordsIterator == null ) this.fmRecords = Collections.EMPTY_LIST.iterator();
		else this.fmRecords = fmRecordsIterator;
		this.metaData = new FmResultSetMetaData( fieldDefinitions );
		this.fieldDefinitions = fieldDefinitions;
		connection.notifyNewResultSet(this);
	}

	//OPTIMIZE make all methods final

	private SQLException handleFormattingException(Exception e, int position) {
		String columnName = fieldDefinitions.get( position - 1 ).getColumnName();
		return handleFormattingException(e, columnName);
	}

	private SQLException handleFormattingException(Exception e, String columnName) {
		logger.log(Level.WARNING, e.toString());
		SQLException sqlException = new SQLException( e.toString() + " (requested column '" + columnName + "' / zero-indexed row: " + rowNum + ")" );
		sqlException.initCause(e);
		return sqlException;
	}

	private AbstractMethodError handleMissingMethod(String message) {
		AbstractMethodError result = new AbstractMethodError(message);
		logger.log(Level.WARNING, result.toString());
		return result;
	}

	//---These methods must be implemented---
	public boolean next() throws SQLException {
		if( ! isOpen ) throw new IllegalStateException("The ResultSet has been closed; you cannot read any more records from it." );
		if( fmRecords.hasNext() ) {
			try {
				currentRecord = (FmRecord) fmRecords.next();
			} catch( RuntimeException e ) {
				log.log( Level.SEVERE, "Got an exception while trying to fetch next row from database.", e );
				SQLException e1 = new SQLException( e.getMessage() );
				e1.initCause( e );
				throw e1;
			}
			// The first time through isBeforeFirst is still true, because we haven't set
			// it to false yet, which means this is the first record.
			rowNum++;
			//if (isBeforeFirst) {
			//	isFirst = true;
			//	isBeforeFirst = false; // set isBeforeFirst to false now. We're on the first record
			//} else { // The following needs to happen in the else!
			//	isFirst = false; // Set isFirst to false
			//}
			// If there are no records after the current record
			// we're on the last record.  Set isLast to true.
			return true;
		} else {
			// This method 'next()'  has been called again and there are no more records.
			// This means that we are after the last record
			//isLast = false;
			isAfterLast = true;
			//currentRecord = null; //Fix!!! should currentRecord be set to false since we are after the last record?
			return false;
		}
	}

	private void checkResultSet() {
		//if( currentRecord == null ) throw new IllegalStateException("You must call next() before trying to work with this ResultSet." );
		if( rowNum == -1 || isAfterLast ) throw new IllegalStateException("The ResultSet is not positioned on a valid row.");
	}


	public void close() throws SQLException {
		fmRecords = null;
		metaData = null;
		currentRecord = null;
		fieldDefinitions = null;
		isOpen = false;
		connection.notifyClosedResultSet( this );
		//try {
		// need to close the FmXmlResult here!!!
//      actionHandler = ( (FmConnection)statement.getConnection7() ).getXmlRequestHandler();
		//}
	}


	public String getString( int i ) throws SQLException {
		if( rowNum == -1 || isAfterLast ) throw new IllegalStateException("The ResultSet is not positioned on a valid row.");
		String result = currentRecord.getString(i - 1);
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, result);
		}
		return result;
	}

	public boolean getBoolean( int i ) throws SQLException {
		checkResultSet();
		return currentRecord.getBoolean(i - 1);
	}

	public byte getByte( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getByte(i - 1);
		} catch (NumberFormatException e) {
			throw handleFormattingException(e, i);
		}
	}

	public short getShort( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getShort(i - 1);
		} catch (NumberFormatException e) {
			throw handleFormattingException(e, i);
		}
	}

	public int getInt( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getInt(i - 1);
		} catch (NumberFormatException e) {
			throw handleFormattingException(e, i);
		}
	}

	public long getLong( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getLong(i - 1);
		} catch (NumberFormatException e) {
			throw handleFormattingException(e, i);
		}
	}

	public float getFloat( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getFloat(i - 1);
		} catch (NumberFormatException e) {
			throw handleFormattingException(e, i);
		}
	}

	public double getDouble( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getDouble(i - 1);
		} catch (NumberFormatException e) {
			throw handleFormattingException(e, i);
		}
	}

	// Deprecated but implemented
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		checkResultSet();
		BigDecimal value = getBigDecimal(columnIndex);
		try {
			return value.setScale(scale);
		} catch (ArithmeticException e) {
			throw handleFormattingException(e, columnIndex);
		}
	}


	public Date getDate( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getDate(i - 1);
		} catch (IllegalArgumentException e) {
			throw handleFormattingException(e, i);
		}
	}

	public Time getTime( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getTime(i - 1);
		} catch (IllegalArgumentException e) {
			throw handleFormattingException(e, i);
		}
	}

	public Timestamp getTimestamp( int i ) throws SQLException {
		checkResultSet();
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, String.valueOf(i));
		}
		try {
			return currentRecord.getTimestamp(i - 1);
		} catch (IllegalArgumentException e) {
			throw handleFormattingException( e, i );
		}
	}

	public Blob getBlob( int i ) throws SQLException {
		checkResultSet();
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, String.valueOf(i));
		}
		try {
			return currentRecord.getBlob(i - 1, connection );
		} catch (IllegalArgumentException e) {
			throw handleFormattingException(e, i);
		}
	}

	public String getString( String s ) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		if( rowNum == -1 || isAfterLast ) throw new IllegalStateException("The ResultSet is not positioned on a valid row.");
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getString(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public boolean getBoolean(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getBoolean(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public byte getByte(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getByte(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public short getShort(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getShort(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public int getInt(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getInt(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public long getLong(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getLong(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public float getFloat(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getFloat(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public double getDouble(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getDouble(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	// Deprecated method but implemented
	public BigDecimal getBigDecimal(String s, int i) throws SQLException {
		int columnIndex = fieldDefinitions.indexOfFieldWithAlias(s);
		if (columnIndex == -1) throw new SQLException(s + " is not a defined field.");
		return getBigDecimal(columnIndex, i);
	}


	public Date getDate(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getDate(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public Time getTime(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getTime(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public Timestamp getTimestamp(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		try {
			if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
			return currentRecord.getTimestamp(i);
		} catch (Exception e) {
			throw handleFormattingException(e, s);
		}
	}

	public Blob getBlob( String s ) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		if (i == -1) throw new SQLException(s + " is not a defined field.");
		return getBlob(i + 1);
	}

	public SQLWarning getWarnings() throws SQLException {
		return null; //FIX!! Should we be returning anything here?
	}

	public void clearWarnings() throws SQLException {
		//FIX!! Should we be doing anything here?
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return metaData;
	}

	public Object getObject( int i ) throws SQLException {
		checkResultSet();
		Object result = currentRecord.getObject(i - 1, connection);
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "getObject(" + i + ") is " + result);
		}
		return result;
	}

	public Object getObject(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		if (i == -1) throw new SQLException(s + " is not a defined field.");
		return getObject(i + 1);
	}

	public int findColumn(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		if (i == -1) throw new SQLException(s + " is not a defined field.");
		return i + 1;
	}

	public BigDecimal getBigDecimal( int i ) throws SQLException {
		checkResultSet();
		try {
			return currentRecord.getBigDecimal(i - 1);
		} catch (NumberFormatException e) {
			throw handleFormattingException(e, i);
		}
	}

	public BigDecimal getBigDecimal(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		if (i == -1) throw new SQLException(s + " is not a defined field.");
		return getBigDecimal(i + 1);
	}


	public final boolean isBeforeFirst() throws SQLException {
		return rowNum == -1;
	}

	public final boolean isAfterLast() throws SQLException {
		return isAfterLast;
	}

	public final boolean isFirst() throws SQLException {
		return rowNum == 0;
	}

	public final boolean isLast() throws SQLException {
		return ( !fmRecords.hasNext() );
	}

	public final boolean wasNull() throws SQLException {
		return fieldDefinitions.wasNull;
	}

	public int getType() throws SQLException {
		throw handleMissingMethod( "getType is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public byte[] getBytes( int i ) throws SQLException {
		Blob blob = getBlob(i);
		if( blob == null ) return new byte[0];
		long length = blob.length();
		if( length > Integer.MAX_VALUE ) throw new SQLException("Could not return a byte array, result size (" + length + ") is too large.");
		return blob.getBytes( 0, (int)length );
	}

	//---These methods do not have to be implemented, but technically could be


	public void beforeFirst() throws SQLException {
		throw handleMissingMethod( "beforeFirst is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void afterLast() throws SQLException {
		throw handleMissingMethod( "afterLast is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean first() throws SQLException { //FIX! This could be implemented by resending the query to FileMaker
		throw handleMissingMethod( "first is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean last() throws SQLException { //FIX! This could be implemented either repeatedly calling next(), or by resending the query to FileMaker. Could see the result set size so figure out which one is faster.
		throw handleMissingMethod( "last is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getRow() throws SQLException {
		throw handleMissingMethod( "getRow is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setFetchSize( int i ) throws SQLException {
		throw handleMissingMethod( "setFetchSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getFetchSize() throws SQLException {
		throw handleMissingMethod( "getFetchSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Statement getStatement() throws SQLException {
		throw handleMissingMethod( "getStatement is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public byte[] getBytes( String s ) throws SQLException {
		throw handleMissingMethod( "getBytes (" + s + ") is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public InputStream getBinaryStream( int i ) throws SQLException {
		throw handleMissingMethod( "getBinaryStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public InputStream getBinaryStream( String s ) throws SQLException {
		throw handleMissingMethod( "getBinaryStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean absolute( int i ) throws SQLException { //FIX! This could be implemented by resending the query to FileMaker
		throw handleMissingMethod( "absolute is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean relative( int i ) throws SQLException { //FIX! This could be implemented by resending the query to FileMaker, or by calling next() for positive args
		throw handleMissingMethod( "relative is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean previous() throws SQLException { //FIX! This could be implemented by resending the query to Filemaker, or by keeping an in-memory List until the result set reaches a certain size
		throw handleMissingMethod( "previous is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void refreshRow() throws SQLException {
		throw handleMissingMethod( "refreshRow is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Object getObject( int i, Map map ) throws SQLException {
		throw handleMissingMethod( "getObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Object getObject( String s, Map map ) throws SQLException {
		throw handleMissingMethod( "getObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Date getDate( int i, Calendar calendar ) throws SQLException {
		throw handleMissingMethod( "getDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Date getDate( String s, Calendar calendar ) throws SQLException {
		throw handleMissingMethod( "getDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Time getTime( int i, Calendar calendar ) throws SQLException {
		throw handleMissingMethod( "getTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Time getTime( String s, Calendar calendar ) throws SQLException {
		throw handleMissingMethod( "getTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Timestamp getTimestamp( int i, Calendar calendar ) throws SQLException {
		throw handleMissingMethod( "getTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Timestamp getTimestamp( String s, Calendar calendar ) throws SQLException {
		throw handleMissingMethod( "getTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Array getArray( int i ) throws SQLException { //I think we could use this for repeating fields --jsb
		throw handleMissingMethod( "getArray is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Array getArray( String s ) throws SQLException {
		throw handleMissingMethod( "getArray is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public URL getURL( int i ) throws SQLException {
		throw handleMissingMethod( "getURL is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public URL getURL( String s ) throws SQLException {
		throw handleMissingMethod( "getURL is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void deleteRow() throws SQLException {
		throw handleMissingMethod( "deleteRow is not implemented yet." ); //FIX!!! Broken placeholder
	}



	//---These methods are lower priority, or do not need to be implemented at all---


	public InputStream getAsciiStream( int i ) throws SQLException {
		throw handleMissingMethod( "getAsciiStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public InputStream getUnicodeStream( int i ) throws SQLException {
		throw handleMissingMethod( "getUnicodeStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public InputStream getAsciiStream( String s ) throws SQLException {
		throw handleMissingMethod( "getAsciiStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public InputStream getUnicodeStream( String s ) throws SQLException {
		throw handleMissingMethod( "getUnicodeStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getCursorName() throws SQLException {
		throw handleMissingMethod( "getCursorName is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Reader getCharacterStream( int i ) throws SQLException {
		throw handleMissingMethod( "getCharacterStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Reader getCharacterStream( String s ) throws SQLException {
		throw handleMissingMethod( "getCharacterStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setFetchDirection( int i ) throws SQLException {
		throw handleMissingMethod( "setFetchDirection is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getFetchDirection() throws SQLException {
		throw handleMissingMethod( "getFetchDirection is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getConcurrency() throws SQLException {
		throw handleMissingMethod( "getConcurrency is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean rowUpdated() throws SQLException {
		throw handleMissingMethod( "rowUpdated is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean rowInserted() throws SQLException {
		throw handleMissingMethod( "rowInserted is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean rowDeleted() throws SQLException {
		throw handleMissingMethod( "rowDeleted is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateNull( int i ) throws SQLException {
		throw handleMissingMethod( "updateNull is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBoolean( int i, boolean b ) throws SQLException {
		throw handleMissingMethod( "updateBoolean is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateByte( int i, byte b ) throws SQLException {
		throw handleMissingMethod( "updateByte is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateShort( int i, short i1 ) throws SQLException {
		throw handleMissingMethod( "updateShort is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateInt( int i, int i1 ) throws SQLException {
		throw handleMissingMethod( "updateInt is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateLong( int i, long l ) throws SQLException {
		throw handleMissingMethod( "updateLong is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateFloat( int i, float v ) throws SQLException {
		throw handleMissingMethod( "updateFloat is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateDouble( int i, double v ) throws SQLException {
		throw handleMissingMethod( "updateDouble is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBigDecimal( int i, BigDecimal decimal ) throws SQLException {
		throw handleMissingMethod( "updateBigDecimal is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateString( int i, String s ) throws SQLException {
		throw handleMissingMethod( "updateString is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBytes( int i, byte[] bytes ) throws SQLException {
		throw handleMissingMethod( "updateBytes is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateDate( int i, Date date ) throws SQLException {
		throw handleMissingMethod( "updateDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateTime( int i, Time time ) throws SQLException {
		throw handleMissingMethod( "updateTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateTimestamp( int i, Timestamp timestamp ) throws SQLException {
		throw handleMissingMethod( "updateTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateAsciiStream( int i, InputStream stream, int i1 ) throws SQLException {
		throw handleMissingMethod( "updateAsciiStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBinaryStream( int i, InputStream stream, int i1 ) throws SQLException {
		throw handleMissingMethod( "updateBinaryStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateCharacterStream( int i, Reader reader, int i1 ) throws SQLException {
		throw handleMissingMethod( "updateCharacterStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateObject( int i, Object o, int i1 ) throws SQLException {
		throw handleMissingMethod( "updateObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateObject( int i, Object o ) throws SQLException {
		throw handleMissingMethod( "updateObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateNull( String s ) throws SQLException {
		throw handleMissingMethod( "updateNull is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBoolean( String s, boolean b ) throws SQLException {
		throw handleMissingMethod( "updateBoolean is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateByte( String s, byte b ) throws SQLException {
		throw handleMissingMethod( "updateByte is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateShort( String s, short i ) throws SQLException {
		throw handleMissingMethod( "updateShort is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateInt( String s, int i ) throws SQLException {
		throw handleMissingMethod( "updateInt is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateLong( String s, long l ) throws SQLException {
		throw handleMissingMethod( "updateLong is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateFloat( String s, float v ) throws SQLException {
		throw handleMissingMethod( "updateFloat is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateDouble( String s, double v ) throws SQLException {
		throw handleMissingMethod( "updateDouble is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBigDecimal( String s, BigDecimal decimal ) throws SQLException {
		throw handleMissingMethod( "updateBigDecimal is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateString( String s, String s1 ) throws SQLException {
		throw handleMissingMethod( "updateString is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBytes( String s, byte[] bytes ) throws SQLException {
		throw handleMissingMethod( "updateBytes is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateDate( String s, Date date ) throws SQLException {
		throw handleMissingMethod( "updateDate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateTime( String s, Time time ) throws SQLException {
		throw handleMissingMethod( "updateTime is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateTimestamp( String s, Timestamp timestamp ) throws SQLException {
		throw handleMissingMethod( "updateTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateAsciiStream( String s, InputStream stream, int i ) throws SQLException {
		throw handleMissingMethod( "updateAsciiStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBinaryStream( String s, InputStream stream, int i ) throws SQLException {
		throw handleMissingMethod( "updateBinaryStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateCharacterStream( String s, Reader reader, int i ) throws SQLException {
		throw handleMissingMethod( "updateCharacterStream is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateObject( String s, Object o, int i ) throws SQLException {
		throw handleMissingMethod( "updateObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateObject( String s, Object o ) throws SQLException {
		throw handleMissingMethod( "updateObject is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void insertRow() throws SQLException {
		throw handleMissingMethod( "insertRow is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateRow() throws SQLException {
		throw handleMissingMethod( "updateRow is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void cancelRowUpdates() throws SQLException {
		throw handleMissingMethod( "cancelRowUpdates is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void moveToInsertRow() throws SQLException {
		throw handleMissingMethod( "moveToInsertRow is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void moveToCurrentRow() throws SQLException {
		throw handleMissingMethod( "moveToCurrentRow is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Ref getRef( int i ) throws SQLException {
		throw handleMissingMethod( "getRef is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Clob getClob( int i ) throws SQLException {
		throw handleMissingMethod( "getClob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Ref getRef( String s ) throws SQLException {
		throw handleMissingMethod( "getRef is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Clob getClob( String s ) throws SQLException {
		throw handleMissingMethod( "getClob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateRef( int i, Ref ref ) throws SQLException {
		throw handleMissingMethod( "updateRef is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateRef( String s, Ref ref ) throws SQLException {
		throw handleMissingMethod( "updateRef is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBlob( int i, Blob blob ) throws SQLException {
		throw handleMissingMethod( "updateBlob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateBlob( String s, Blob blob ) throws SQLException {
		throw handleMissingMethod( "updateBlob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateClob( int i, Clob clob ) throws SQLException {
		throw handleMissingMethod( "updateClob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateClob( String s, Clob clob ) throws SQLException {
		throw handleMissingMethod( "updateClob is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateArray( int i, Array array ) throws SQLException {
		throw handleMissingMethod( "updateArray is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void updateArray( String s, Array array ) throws SQLException {
		throw handleMissingMethod( "updateArray is not implemented yet." ); //FIX!!! Broken placeholder
	}
}
