package com.prosc.fmpjdbc;

import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 17, 2005 Time: 1:41:57 AM
 */
public class FmResultSet implements ResultSet {
	private static final Logger log = Logger.getLogger( FmResultSet.class.getName() );

	private final Iterator<FmRecord> fmRecords;
	private final FmResultSetMetaData metaData;
	private final FmFieldList fieldDefinitions;
	private final FmConnection connection;
	private final FmXmlRequest xmlRequest;
	private final FmStatement statement;
	private final int foundCount;
	private final FmRecord singleRecord;
	private final int columnOffset;

	//These fields are only used when the ResultSet represents a repeating field
	//private int repetitionStartIndex;
	private int repetitionMaxIndex;
	private int repetitionCurrentIndex;

	private boolean isOpen = true;
	//private boolean isBeforeFirst = true;
	//private boolean isFirst = false;
	private boolean isAfterLast = false;
	//private boolean isLast = false;
	private int rowNum = -1;
	private FmRecord currentRecord;
	private long totalRecordCount;

	/** Pass in an iterator of {@link FmRecord} objects, which will be used as the ResultSet. Pass null for an empty ResultSet. */
	FmResultSet( Iterator<FmRecord> fmRecordsIterator, int foundCount, FmFieldList fieldDefinitions, FmConnection connection ) {
		this( fmRecordsIterator, foundCount, fieldDefinitions, null, connection, null );
	}

	/** Pass in an iterator of {@link FmRecord} objects, which will be used as the ResultSet. Pass null for an empty ResultSet. */
	FmResultSet( Iterator<FmRecord> fmRecordsIterator, int foundCount, FmFieldList fieldDefinitions, @Nullable FmStatement statement, FmConnection connection, @Nullable FmXmlRequest xmlRequest ) {
		this.statement = statement;
		this.connection = connection;
		if( fmRecordsIterator == null ) this.fmRecords = new ArrayList<FmRecord>().iterator();
		else this.fmRecords = fmRecordsIterator;
		this.metaData = new FmResultSetMetaData( fieldDefinitions );
		this.fieldDefinitions = fieldDefinitions;
		this.foundCount = foundCount;
		connection.notifyNewResultSet(this);
		this.xmlRequest = xmlRequest;
		this.singleRecord = null;
		this.repetitionCurrentIndex = 1;
		this.columnOffset = 0;
	}

	/** This constructor is used to create a ResultSet representing a repeating field. See {@link Array} */
	FmResultSet( FmRecord record, int index, int count, int whichColumnIndex, FmFieldList fieldList, FmResultSet resultSet, Object typeMap ) {
		if( typeMap != null ) {
			throw new AbstractMethodError("Custom typeMaps are not currently supported."); //FIX!!! Broken placeholder
		}
		this.singleRecord = record;
		this.currentRecord = record;
		this.repetitionMaxIndex = index + count;
		this.rowNum = index-1;
		this.repetitionCurrentIndex = index;
		this.fieldDefinitions = fieldList;
		this.foundCount = count;
		this.fmRecords = new ArrayList<FmRecord>().iterator();

		this.metaData = resultSet.metaData;
		this.connection = resultSet.connection;
		this.xmlRequest = resultSet.xmlRequest;
		this.statement = resultSet.statement;
		this.columnOffset = whichColumnIndex;
	}

	void setTotalRecordCount( long totalRecordCount ) {
		this.totalRecordCount = totalRecordCount;
	}

	/** This returns the total number of records in the table, which is not (necessarily) the same as the found count for this query. */
	public long getTotalRecordCount() {
		return totalRecordCount;
	}

	/** This returns the total number of records found in the query. This is a FileMaker-specific attribute which is not part of the JDBC
	 * spec, so cast the ResultSet to this class if you need to access this attribute. This found count includes records not viewable based
	 * on record-level security restrictions, so if you need to get an accurate count of the visible records, the only way is to repeatedly
	 * call next() and count the results.
	 * @return
	 */
	public int getFoundCount() {
		return foundCount;
	}
	//OPTIMIZE make all methods final

	private SQLException handleFormattingException( Exception e, String columnName, int columnIndex ) {
		//log.log(Level.WARNING, e.toString(), e);
		StringBuilder sb = new StringBuilder();
		String delimiter = "";
		if( statement != null ) {
			sb.append( delimiter ).append( "Failing SQL statement: " + statement.toString() );
			delimiter = "\n";
		}
		if( xmlRequest != null ) {
			sb.append(delimiter).append( "XML request URL: " + xmlRequest.getFullUrl() );
			delimiter = "\n";
		}
		if( currentRecord != null ) {
			sb.append( delimiter ).append( "Raw record fields: " + currentRecord.getFieldList() );
			sb.append( "\n" ).append( "Raw record contents: " );
			currentRecord.dumpRawValues( sb );
			delimiter = "\n";
		}
		if( sb.length() > 0 ) {
			log.info( sb.toString() );
		}

		SQLException sqlException = new SQLException( e.toString() + " (requested column '" + columnName + "' / zero-indexed column: " + columnIndex +  " / zero-indexed row: " + rowNum + ")" );
		sqlException.initCause( e );
		return sqlException;
	}

	private AbstractMethodError handleMissingMethod(String message) {
		AbstractMethodError result = new AbstractMethodError(message);
		log.log(Level.WARNING, result.toString());
		return result;
	}

	//---These methods must be implemented---
	public boolean next() throws SQLException {
		if( ! isOpen ) throw new SQLException("The ResultSet has been closed; you cannot read any more records from it." );

		if( singleRecord == null ) { //This is a regular result set representing zero or more rows

			if( fmRecords.hasNext() ) {
				try {
					currentRecord = fmRecords.next();
					if( currentRecord == null ) {
						throw new IllegalStateException( "fmRecords.hasNext() returns true, but fmRecords.next() returns null" );
					}
				} catch( RuntimeException e ) {
					//log.log( Level.SEVERE, "Got an exception while trying to fetch next row from database.", e );
					SQLException e1 = new SQLException( e.toString() );
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
				currentRecord = null; // Should currentRecord be set to false since we are after the last record? Yes, it should: When a call to the next method returns false, the cursor is positioned after the last row. Any invocation of a ResultSet method which requires a current row will result in a SQLException being thrown. -sam
				return false;
			}
		} else { //This ResultSet represents a repetition within a single row
			rowNum++;
			repetitionCurrentIndex++;
			if( rowNum < repetitionMaxIndex ) {
				return true;
			} else {
				isAfterLast = true;
				return false;
			}
		}
	}

	private void checkResultSet() {
		//if( currentRecord == null ) throw new IllegalStateException("You must call next() before trying to work with this ResultSet." );
		if( rowNum == -1 ) throw new IllegalStateException("The ResultSet is not positioned on a valid row - call next() before trying to read.");
		else if( isAfterLast ) throw new IllegalStateException("The ResultSet is not positioned on a valid row - rowNum is " + rowNum + "; which is after the last row.");
	}


	public void close() throws SQLException {
		//fmRecords = null;
		//metaData = null;
		currentRecord = null;
		rowNum = -1;
		//fieldDefinitions = null;
		isOpen = false;
		connection.notifyClosedResultSet( this );
		if( xmlRequest != null ) {
			xmlRequest.closeRequest();
		}
	}

	public Long getModCount() {
		checkResultSet();
		return currentRecord.getModCount();
	}
	
	public String getRecordId() {
		checkResultSet();
		return currentRecord.getRecordId();
	}


	public String getString( String s ) throws SQLException { return getString( fieldDefinitions.indexOfFieldWithAlias( s ), s ); }
	public String getString( int i ) throws SQLException { return getString( i-1, null ); }
	private String getString( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if( "recid".equals( s ) ) {
			return currentRecord.getRecordId();
		}
		if (i == -1) {
			throw new SQLException( "'" + s + "' is not a field on the requested layout." );
		}

		try {
			String result;
			final FmField field = fieldDefinitions.get( i );
			if( Types.BLOB == field.getType().getSqlDataType() ) {
				FmBlob fmBlob = (FmBlob)getBlob( i );
				if( fmBlob == null ) return null;
				result = fmBlob.getURL().toExternalForm();
			} else {
				int whichRep = field.getRepetition( repetitionCurrentIndex );
				result = currentRecord.getString(i + columnOffset, whichRep );
			}
			log.log(Level.FINEST, result);
			return result;
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}


	public boolean getBoolean(String s) throws SQLException {
		return getBoolean( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public boolean getBoolean( int i ) throws SQLException {
		return getBoolean( i - 1, null );
	}
	private boolean getBoolean( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getBoolean(i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}


	public byte getByte(String s) throws SQLException {
		return getByte( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public byte getByte( int i ) throws SQLException {
		return getByte( i - 1, null );
	}
	private byte getByte( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getByte( i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}


	public short getShort(String s) throws SQLException {
		return getShort( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public short getShort( int i ) throws SQLException {
		return getShort( i-1, null );
	}
	private short getShort( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if( "recid".equals( s ) ) {
			return Short.valueOf( currentRecord.getRecordId() );
		}
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getShort(i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	public int getInt(String s) throws SQLException {
		return getInt( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public int getInt( int i ) throws SQLException {
		return getInt( i - 1, null );
	}
	private int getInt( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if( "recid".equals( s ) ) {
			return Integer.valueOf( currentRecord.getRecordId() );
		}
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getInt( i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	public long getLong(String s) throws SQLException {
		return getLong( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public long getLong( int i ) throws SQLException {
		return getLong( i - 1, null );
	}
	private long getLong( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if( "recid".equals( s ) ) {
			return Long.valueOf( currentRecord.getRecordId() );
		}
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getLong( i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	public float getFloat(String s) throws SQLException {
		return getFloat( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public float getFloat( int i ) throws SQLException {
		return getFloat( i - 1, null );
	}
	private float getFloat( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getFloat( i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	public double getDouble(String s) throws SQLException {
		return getDouble( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public double getDouble( int i ) throws SQLException {
		return getDouble( i - 1, null );
	}
	private double getDouble( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getDouble( i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	public Date getDate(String s) throws SQLException {
		return getDate( fieldDefinitions.indexOfFieldWithAlias( s ), s, null );
	}
	public Date getDate( String s, Calendar calendar ) throws SQLException {
		return getDate( fieldDefinitions.indexOfFieldWithAlias( s ), s, calendar );
	}
	public Date getDate( int i ) throws SQLException {
		return getDate( i-1, null, null );
	}
	public Date getDate( int i, Calendar calendar ) throws SQLException {
		return getDate( i-1, null, calendar );
	}
	private Date getDate( int i, @Nullable String s, @Nullable Calendar calendar ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			if( calendar == null ) {
				return currentRecord.getDate(i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
			} else {
				return currentRecord.getDate(i + columnOffset, field.getRepetition( repetitionCurrentIndex ), calendar.getTimeZone() );
			}
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}


	public Time getTime(String s) throws SQLException {
		return getTime( fieldDefinitions.indexOfFieldWithAlias( s ), s, null );
	}
	public Time getTime( String s, Calendar calendar ) throws SQLException {
		return getTime( fieldDefinitions.indexOfFieldWithAlias( s ), calendar );
	}
	public Time getTime( int i ) throws SQLException {
		return getTime( i - 1, null, null );
	}
	public Time getTime( int i, Calendar calendar ) throws SQLException {
		return getTime( i-1, null, calendar );
	}
	private Time getTime( int i, @Nullable String s, @Nullable Calendar calendar ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			if( calendar == null ) {
				return currentRecord.getTime( i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
			} else {
				return currentRecord.getTime( i + columnOffset, field.getRepetition( repetitionCurrentIndex ), calendar.getTimeZone() );
			}
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	public Timestamp getTimestamp(String s) throws SQLException {
		return getTimestamp( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public Timestamp getTimestamp( int i ) throws SQLException {
		return getTimestamp( i - 1, (String)null );
	}
	private Timestamp getTimestamp( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getTimestamp( i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	public Blob getBlob(String s) throws SQLException {
		return getBlob( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public Blob getBlob( int i ) throws SQLException {
		return getBlob( i - 1, null );
	}
	private Blob getBlob( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			return currentRecord.getBlob( i + columnOffset, field.getRepetition( repetitionCurrentIndex ), connection );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	public BigDecimal getBigDecimal(String s) throws SQLException {
		return getBigDecimal( fieldDefinitions.indexOfFieldWithAlias( s ), -1, s );
	}
	public BigDecimal getBigDecimal(String s, int scale) throws SQLException {
		return getBigDecimal( fieldDefinitions.indexOfFieldWithAlias( s ), scale, s );
	}
	public BigDecimal getBigDecimal( int i ) throws SQLException {
		return getBigDecimal( i - 1, -1, null );
	}
	public BigDecimal getBigDecimal( int i, int scale ) throws SQLException {
		return getBigDecimal( i - 1, scale, null );
	}
	private BigDecimal getBigDecimal( int i, int scale, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			BigDecimal result = currentRecord.getBigDecimal(i + columnOffset, field.getRepetition( repetitionCurrentIndex ) );
			if( scale != -1 ) {
				result = result.setScale( scale, BigDecimal.ROUND_HALF_UP );
			}
			return result;
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}

	/** Used for BLOB / Container data to return a URL that contains the data. Be cautious with where you show this URL, because if there is a username / password, it will be
	 * embedded into the URL.
	 */
	public URL getURL( int i ) throws SQLException {
		FmBlob blob = (FmBlob)getBlob(i);
		return blob.getURL();
	}

	public URL getURL( String s ) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		if (i == -1) throw new SQLException(s + " is not a defined field.");
		return getURL(i + 1);
	}

	public Object getObject(String s) throws SQLException {
		return getObject( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public Object getObject( int i ) throws SQLException {
		return getObject( i - 1, (String)null );
	}
	private Object getObject( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			final FmField field = fieldDefinitions.get( i );
			Object result = currentRecord.getObject(i + columnOffset, field.getRepetition( repetitionCurrentIndex ), connection );
			if( wasNull() ) result = null;
			return result;
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
	}


	public byte[] getBytes(String s) throws SQLException {
		return getBytes( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public byte[] getBytes( int i ) throws SQLException {
		return getBytes( i - 1, null );
	}
	private byte[] getBytes( int i, @Nullable String s ) throws SQLException {
		Blob blob = getBlob( i, s );
		if( blob == null ) return new byte[0];
		long length = blob.length();
		if( length > Integer.MAX_VALUE ) throw new SQLException("Could not return a byte array, result size (" + length + ") is too large.");
		return blob.getBytes( 0, (int)length );
	}

	public Array getArray(String s) throws SQLException {
		return getArray( fieldDefinitions.indexOfFieldWithAlias( s ), s );
	}
	public Array getArray( int i ) throws SQLException {
		return getArray( i - 1, null );
	}
	private Array getArray( int i, @Nullable String s ) throws SQLException {
		checkResultSet();
		if (i == -1) throw new SQLException(s + " is not a field on the requested layout.");
		try {
			return currentRecord.getArray( i + columnOffset, this );
		} catch (Exception e) {
			throw handleFormattingException(e, s, i );
		}
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

	public int findColumn(String s) throws SQLException {
		int i = fieldDefinitions.indexOfFieldWithAlias(s);
		if (i == -1) throw new SQLException(s + " is not a defined field.");
		return i + 1;
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

	public Statement getStatement() throws SQLException {
		return statement;
	}

	public boolean isClosed() throws SQLException {
		return ! isOpen;
	}

	//---These methods do not have to be implemented, but technically could be



	public int getType() throws SQLException {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	public void beforeFirst() throws SQLException {
		throw new SQLException("Not supported");
	}

	public void afterLast() throws SQLException {
		throw new SQLException("Not supported");
	}

	public boolean first() throws SQLException { //FIX! This could be implemented by resending the query to FileMaker
		throw new SQLException("Not supported");
	}

	public boolean last() throws SQLException { //FIX! This could be implemented either repeatedly calling next(), or by resending the query to FileMaker. Could see the result set size so figure out which one is faster.
		throw new SQLException("Not supported");
	}

	public int getRow() throws SQLException {
		return 0;
	}

	public void setFetchSize( int i ) throws SQLException {
		throw handleMissingMethod( "setFetchSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getFetchSize() throws SQLException {
		throw handleMissingMethod( "getFetchSize is not implemented yet." ); //FIX!!! Broken placeholder
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

	public Timestamp getTimestamp( int i, Calendar calendar ) throws SQLException {
		throw handleMissingMethod( "getTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public Timestamp getTimestamp( String s, Calendar calendar ) throws SQLException {
		throw handleMissingMethod( "getTimestamp is not implemented yet." ); //FIX!!! Broken placeholder
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

	//===These methods were added in Java 1.5 ===

	public Object getObject( int i, Map<String, Class<?>> map ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public Object getObject( String colName, Map<String, Class<?>> map ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public int getHoldability() throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNString( int i, String s ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNString( String s, String s1 ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public String getNString( int i ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public String getNString( String s ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public Reader getNCharacterStream( int i ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public Reader getNCharacterStream( String s ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNCharacterStream( int i, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNCharacterStream( String s, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateAsciiStream( int i, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateBinaryStream( int i, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateCharacterStream( int i, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateAsciiStream( String s, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateBinaryStream( String s, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateCharacterStream( String s, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateBlob( int i, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateBlob( String s, InputStream stream, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateClob( int i, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateClob( String s, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNClob( int i, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNClob( String s, Reader reader, long l ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNCharacterStream( int i, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNCharacterStream( String s, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateAsciiStream( int i, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateBinaryStream( int i, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateCharacterStream( int i, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateAsciiStream( String s, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateBinaryStream( String s, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateCharacterStream( String s, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateBlob( int i, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateBlob( String s, InputStream stream ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateClob( int i, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateClob( String s, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNClob( int i, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNClob( String s, Reader reader ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public <T> T unwrap( Class<T> aClass ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public boolean isWrapperFor( Class<?> aClass ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	// ===These methods were added in Java 6. Comment them out to compile in Java 1.5. ===


	public RowId getRowId( int i ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public RowId getRowId( String s ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateRowId( int i, RowId id ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateRowId( String s, RowId id ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNClob( int i, NClob clob ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateNClob( String s, NClob clob ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public NClob getNClob( int i ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public NClob getNClob( String s ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public SQLXML getSQLXML( int i ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public SQLXML getSQLXML( String s ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateSQLXML( int i, SQLXML sqlxml ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public void updateSQLXML( String s, SQLXML sqlxml ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}
}
