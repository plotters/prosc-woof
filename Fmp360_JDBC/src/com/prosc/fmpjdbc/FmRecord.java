package com.prosc.fmpjdbc;

import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.ParseException;
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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 17, 2005 Time: 4:36:27 PM
 */
public class FmRecord {
	private static final Logger log = Logger.getLogger( FmRecord.class.getName() );
	private static final TimeZone defaultTimeZone = TimeZone.getDefault();

	//FIX!! If we're using FileMaker 6, we need much smarter parsers, because FM6 doesn't normalize the data in the XML

	final static ThreadLocal<DateFormat> timeFormat = new ThreadLocal<DateFormat>() {
		protected DateFormat initialValue() {
			return new SimpleDateFormat("HH:mm:ss");
		}
	};

	final static ThreadLocal<DateFormat> timestampFormat = new ThreadLocal<DateFormat>() {
		protected DateFormat initialValue() {
			return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		}
	};

	final static ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
		protected DateFormat initialValue() {
			return new SimpleDateFormat("MM/dd/yyyy");
		}
	};

	/** Record IDs are typically Long values, but with ESS tables it can be a String like this: ROW MODID="0" RECORDID="RID_!19" */
	private String recordId;

	private Long modCount;
	private FmFieldList fieldList;

	/** rawValues is an array that contains either String (for non-repeating) or String[] (for repeating) objects. */
	private Object[] rawValues;
	private int[] valueCounts;

	FmRecord(FmFieldList fieldList, String recordId, Long modCount) {
		this.fieldList = fieldList;
		this.rawValues = new Object[fieldList.size()];
		this.valueCounts = new int[ fieldList.size() ];
		this.recordId = recordId;
		this.modCount = modCount;
	}


	public String getRecordId() {
		return recordId;
	}

	public Long getModCount() {
		return modCount;
	}

	/*public FmFieldList getFieldList() {
		return fieldList;
	}*/

	public String toString() {
		return "RecordId: " + recordId + "; ModCount: " + modCount;

		/*StringBuilder result = new StringBuilder();
				result.append("RecordId: " + recordId + "; ModCount: " + modCount + "; Data: ");
				for( Object rawValue : rawValues ) {
					result.append( "(" + rawValue + ")" );
				}
				return result.toString();*/
	}

	void dumpRawValues( StringBuilder writeTo ) {
		writeTo.append( "Raw values: [" );
		String delim = "";
		writeTo.append( "[" );
		for( Object value : rawValues ) {
			if( value instanceof String[] ) {
				writeTo.append( delim ).append( Arrays.asList( value ).toString() );
			} else {
				writeTo.append( delim ).append( String.valueOf( value ) );
			}
			delim = ",";
		}
		writeTo.append( "]" );
	}

	FmFieldList getFieldList() {
		return fieldList;
	}

	/**
	 * This is fairly inefficient - it is much faster to reference the value by its index
	 */
	/*public String getRawValue(String s) {
		int index = 0;
		for (Iterator it = fieldList.getFields().keySet().iterator(); it.hasNext();) {
			if (s.equals(it.next())) return getRawValue(index);
			index++;
		}
		throw new IllegalArgumentException("No field exists in this record named " + s);
	}*/

	/** Reads data directly from the array of String objects parsed from the XML response. */
	private String getRawStringValue( int columnIndex, int repetition ) {
		Object rawResult = rawValues[columnIndex];
		if( rawResult == null ) return null;
		if( rawResult instanceof String && repetition == 1 ) return (String)rawResult;
		else if( rawResult instanceof String[] ) {
			String[] rawResultArray = (String[])rawResult;
			if( repetition > rawResultArray.length ) {
				throw new IllegalArgumentException( "Repetition " + repetition + " was requested, but " + fieldList.get(columnIndex).getColumnName() + " only has " + rawResultArray.length + " repetitions." );
			}
			return rawResultArray[ repetition - 1 ];
		}
		else if( rawResult instanceof String && repetition > 1 ) {
			throw new IllegalArgumentException( "Repetition " + repetition + " was requested, but " + fieldList.get(columnIndex).getColumnName() + " is not a repeating field." );
		} else {
			throw new IllegalStateException( "rawResult class is " + rawResult.getClass() + "; this is incorrect" );
		}
	}

	/** Provides write access directly to the String array for this FmRecord. */
	protected void addRawValue( String newValue, int columnIndex ) {
		addRawValue( newValue, new int[] {columnIndex}, 1, 1 );
		rawValues[columnIndex] = newValue;
	}

	/** columnIndeces must be sorted in ascending order
	 * @param whichRep This is a 1-indexed value representing the repetition that is being set
	 * */
	protected void addRawValue( String newValue, int[] columnIndices, int maxRepetitions, int whichRep ) {
		final int firstColumn = columnIndices[0]; //We will actually store a reference to the same value or array in all the target indices, so we only need to worry about the first one
		int lastColumn = columnIndices[ columnIndices.length - 1 ];
		ensureCapacity( firstColumn, maxRepetitions );
		if( lastColumn >= valueCounts.length ) {
			throw new ArrayIndexOutOfBoundsException( "Tried to set a value for column index " + lastColumn + ", but there are only " + valueCounts.length + " items in the array");
		}
		valueCounts[ firstColumn ] = Math.max( valueCounts[ firstColumn ], whichRep );
		if( maxRepetitions < 2 ) {
			rawValues[ firstColumn ] = newValue;
		} else {
			if( whichRep > maxRepetitions ) {
				throw new IllegalStateException( "Called addRawValue too many times (" + whichRep + "), maxRepetitions is only " + maxRepetitions );
			}
			@SuppressWarnings({"MismatchedReadAndWriteOfArray"})
			String[] stringArray = (String[])rawValues[firstColumn];
			stringArray[ whichRep-1 ] = newValue;
		}
		for( int n=1; n<columnIndices.length; n++ ) { //Start at index 1, not 0, because we're only setting the values after the first one --jsb
			rawValues[columnIndices[n]] = rawValues[firstColumn];
		}
	}

	private void ensureCapacity( int columnIndex, int maxRepetitions ) {
		if( maxRepetitions == 1 ) return; //We'll just set it as a String, not a String[] array, so it doesn't matter what rawValues currently contains
		if( rawValues[columnIndex] == null ) {
			rawValues[columnIndex] = new String[ maxRepetitions ];
		} else if( rawValues[columnIndex] instanceof String[] ) {
			String[] array = (String[])rawValues[columnIndex];
			if( array.length < maxRepetitions ) {
				String[] newArray = new String[ maxRepetitions ];
				System.arraycopy( array, 0, newArray, 0, array.length );
				rawValues[columnIndex] = newArray;
			}
		} else if( rawValues[columnIndex] instanceof String ) {
			String[] newArray = new String[ maxRepetitions ];
			newArray[0] = (String)rawValues[columnIndex];
			rawValues[columnIndex] = newArray;
		} else {
			throw new IllegalStateException( "rawValues[" + columnIndex + "] contains an object of class: " + rawValues[columnIndex].getClass() );
		}
	}


	public Object getObject( int columnIndex, int repetition, FmConnection connection ) {
		Object result;
		FmField field = fieldList.get( columnIndex );
		FmFieldType fmType = field.getType();
		int sqlType = fmType.getSqlDataType();

		switch (sqlType ) {
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
				result = getString(columnIndex, repetition );
				break;
			case Types.DECIMAL: result = getBigDecimal(columnIndex, repetition ); //All filemaker numbers are treated as decimal by default
				break;
			case Types.DATE: result = getDate(columnIndex, repetition );
				break;
			case Types.TIME: result = getTime(columnIndex, repetition );
				break;
			case Types.TIMESTAMP: result = getTimestamp(columnIndex, repetition );
				break;
			case Types.INTEGER: result = getInt( columnIndex, repetition ); //This will only happen for recIds
				break;
			case Types.BLOB: result = getBlob(columnIndex, repetition, connection);
				break;
			default: result = getString(columnIndex, repetition );
		}

		if( fieldList.wasNull && field.isNullable() ) { //FIX! This condition really ought to be in each getXXX() method, not just in getObject(), but this will cover 80% or so of the usages, I think --jsb
			return null;
		} else {
			return result;
		}
	}

	// Implementing the get methods for FmResultSet at the FmRecord level

	/**
	 * Returns the string value for the record. This implementation returns null for empty strings (since FileMaker does not distinguish between null and empty); it's
	 * debatable which behavior is correct.
	 * @return returns String value
	 * @param columnIndex
	 * @param repetition
	 */
	public String getString( int columnIndex, int repetition ) { //OPTIMIZE Shouldn't need to check for both null and length 0; check to see which way FmXmlResult handles it
		String rawValue = getRawStringValue( columnIndex, repetition );

		if( rawValue == null ) {
			fieldList.wasNull = true;
			return fieldList.get(columnIndex).isNullable() ? null : "";
		}
		if( rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return fieldList.get(columnIndex).isNullable() ? null : "";
		} else {
			fieldList.wasNull = false;
			return rawValue;
		}
	}

	/*public Object getValue(int index) {
		Object result = rawValues[index];
		if( result == null ) {
			fieldList.wasNull = true;
			if( ! fieldList.get(index).isNullable() ) result = ""; //FIX!!! Use formatter to parse value; return zero for numeric values
		} else fieldList.wasNull = false;
		return result;
	}*/

	/*
	* @Return returns the boolean and column i
	*/
	public boolean getBoolean( int columnIndex, int repetition ) {
		String rawValue = getRawStringValue( columnIndex, repetition );
		fieldList.wasNull = (rawValue == null);
		return getBooleanForRawValue(rawValue);
	}

	/*
	* A helper method for methods that need a boolean from a raw value
	*/
	private boolean getBooleanForRawValue(String rawValue) {
		if (	rawValue == null || // if it's null
				rawValue.trim().length() == 0 || // if it's empty
				"false".equals(rawValue.toLowerCase().trim()) || // if it says false
				"0".equals(rawValue.trim()) // if it says 0
				) return false;

		return true;
	}


	public byte getByte( int columnIndex, int repetition ) throws NumberFormatException {
		String rawValue = getRawStringValue( columnIndex, repetition );
		fieldList.wasNull = (rawValue == null);
		if( rawValue == null || rawValue.length() == 0 ) return 0;
		return Byte.valueOf( rawValue );
	}

	public short getShort( int columnIndex, int repetition ) throws NumberFormatException {
		String rawValue = getRawStringValue( columnIndex, repetition );
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Short.valueOf( rawValue );
		} catch( NumberFormatException e ) {
			return Short.valueOf( NumberUtils.removeNonNumericChars( rawValue ) );
		}
	}

	public int getInt( int columnIndex, int repetition ) throws NumberFormatException {
		String rawValue = getRawStringValue( columnIndex, repetition );
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Integer.valueOf( rawValue );
		} catch(NumberFormatException e) {
			String stripDigits = NumberUtils.removeNonNumericChars( rawValue );
			return new BigDecimal( stripDigits ).intValue(); //We do big decimal here so that if there is a decimal, we just toss it instead of getting a NumberFormatException
		}
	}

	public long getLong( int columnIndex, int repetition ) throws NumberFormatException {
		String rawValue = getRawStringValue( columnIndex, repetition );
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Long.valueOf( rawValue );
		} catch( NumberFormatException e ) {
			return Long.valueOf( NumberUtils.removeNonNumericChars( rawValue ) );
		}
	}

	public float getFloat( int columnIndex, int repetition ) throws NumberFormatException {
		String rawValue = getRawStringValue( columnIndex, repetition );
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Float.valueOf( rawValue );
		} catch( NumberFormatException e ) {
			return Float.valueOf( NumberUtils.removeNonNumericChars( rawValue ) );
		}
	}

	public double getDouble( int columnIndex, int repetition ) throws NumberFormatException {
		String rawValue = getRawStringValue( columnIndex, repetition );
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Double.valueOf( rawValue );
		} catch( NumberFormatException e ) {
			return Double.valueOf( NumberUtils.removeNonNumericChars( rawValue ) );
		}
	}


	public BigDecimal getBigDecimal( int columnIndex, int repetition ) throws NumberFormatException {
		String rawValue = getRawStringValue( columnIndex, repetition );
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return new BigDecimal(0d);
		} else {
			fieldList.wasNull = false;
			try {
				return new BigDecimal(rawValue);
			} catch(NumberFormatException e) { //Strip all non-numeric characters and try again
				return new BigDecimal( NumberUtils.removeNonNumericChars( rawValue ) );
			}
		}
	}

	public Date getDate( int columnIndex, int repetition ) throws IllegalArgumentException {
		final TimeZone zone = defaultTimeZone;
		return getDate( columnIndex, repetition, zone);
	}

	public Date getDate( final int columnIndex, int repetition, final TimeZone zone ) {
		String rawValue = getRawStringValue( columnIndex, repetition );
		return getDate( zone, rawValue );
	}

	private Date getDate( TimeZone zone, String rawValue ) {
		if( rawValue == null || rawValue.length() == 0 || "?".equals(rawValue) ) { //FIX! I don't know if ignoring "?" is the best policy --jsb
			fieldList.wasNull = true;
			return null;
		} else fieldList.wasNull = false;
		try {
			DateFormat format = dateFormat.get();
			format.setTimeZone(zone);
			java.util.Date date;
			try {
				date = format.parse( rawValue );
			} catch( ParseException e ) {
				//final SimpleDateFormat fallback1 = new SimpleDateFormat( "MM.dd.yyyy" );
				//fallback1.setTimeZone( zone );
				String normalizedDate = rawValue.replace( '.', '/' ).replace( '-', '/' );
				date = format.parse( normalizedDate );
			}
			if (log.isLoggable( Level.FINE)) {
				log.fine( "Return date " + date + " for raw value " + rawValue );
			}
			return new Date( date.getTime() );
		} catch( ParseException e ) {
			IllegalArgumentException e1 = new IllegalArgumentException(e.toString());
			e1.initCause(e);
			//FIX!! Need configurable exception handling on whether to return null or rethrow --jsb
			throw e1;
			//log.log( Level.WARNING, "Can't parse this as a date: " + rawValue, e1 );
			//return null;
		}
	}

	Time getTime( int columnIndex, int repetition ) throws IllegalArgumentException {
		final TimeZone timeZone = defaultTimeZone;
		return getTime( columnIndex, repetition, timeZone);
	}

	Time getTime( final int columnIndex, int repetition, final TimeZone timeZone ) {
		String rawValue = getRawStringValue( columnIndex, repetition );
		if( rawValue == null || rawValue.length() == 0 || "?".equals(rawValue) ) { //FIX! I don't know if ignoring "?" is the best policy --jsb
			fieldList.wasNull = true;
			return null;
		} else fieldList.wasNull = false;
		try {
			DateFormat format = timeFormat.get();
			format.setTimeZone(timeZone);
			return new Time( format.parse(rawValue).getTime() ); //This is where it fails
		} catch( ParseException e ) {
			IllegalArgumentException e1 = new IllegalArgumentException(e.toString());
			e1.initCause(e);
			//FIX!! Need configurable exception handling on whether to return null or rethrow --jsb
			//log.log( Level.WARNING, "Can't parse this as a time: " + rawValue, e1 );
			//return null;
			throw e1;
		}
	}

	Timestamp getTimestamp( int columnIndex, int repetition ) throws IllegalArgumentException {
		String rawValue = getRawStringValue( columnIndex, repetition );
		if( rawValue == null || rawValue.length() == 0 || "?".equals(rawValue) ) { //FIX! I don't know if ignoring "?" is the best policy --jsb
			fieldList.wasNull = true;
			return null;
		} else fieldList.wasNull = false;
		DateFormat format = timestampFormat.get();
		try {
			return new java.sql.Timestamp( format.parse(rawValue).getTime() );
		} catch( ParseException failure1 ) {
			try { //This is a fix for Nico Kobes in Netherlands, where he is getting dashes instead of slashes. See my bug report here: http://forums.filemaker.com/posts/d7304ce2e5 --jsb
				//Optimize Slow to catch an exception for every date value, but I don't know whether this is happening consistently or just for certain cells, so I don't really have a choice without finding the answer to that. --jsb
				String processedValue = rawValue.replace( '-', '/' ).replace( '.', '/' );
				return new java.sql.Timestamp( format.parse(processedValue).getTime() );
			} catch( ParseException failure2 ) {
				try {
					//There is a bug in the Web Publishing Engine. Some international dates, which appear in FileMaker Pro UI as "22.12.2012 13:29:33", get output by the WPE as "12 13:29:33.22.2012". Might as well roll with it. --jsb
					SimpleDateFormat fuckedUpWebPublishingFormat = new SimpleDateFormat( "MM HH:mm:ss.dd.yyyy" );
					java.util.Date date = fuckedUpWebPublishingFormat.parse( rawValue );
					return new java.sql.Timestamp( date.getTime() );
				} catch( ParseException cannotParse ) {
					IllegalArgumentException e1 = new IllegalArgumentException(cannotParse.toString() + " for column " + columnIndex + "[" + repetition + "]");
					e1.initCause(cannotParse);
					//FIX!! Need configurable exception handling on whether to return null or rethrow --jsb
					//log.log( Level.WARNING, "Can't parse this as a timestamp: " + rawValue, e1 );
					//return null;
					throw e1;
				}
			}
		}
	}

	Blob getBlob( int columnIndex, int repetition, FmConnection connection ) throws IllegalArgumentException {
		String rawValue = getRawStringValue( columnIndex, repetition );

		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return null;
		} else fieldList.wasNull = false;
		try {
			URL xmlUrl = connection.getXmlUrl();
			xmlUrl.getProtocol();
			xmlUrl.getHost();
			xmlUrl.getPort();

			URL containerUrl = new URL( xmlUrl.getProtocol(), xmlUrl.getHost(), xmlUrl.getPort(), rawValue );
			return new FmBlob( containerUrl, connection.getUsername(), connection.getPassword() );
		} catch( Exception e ) {
			IllegalArgumentException e1 = new IllegalArgumentException(e.toString());
			e1.initCause(e);
			throw e1;
		}
	}


	/** This is used for repeating fields */
	Array getArray( int fieldIndex, FmResultSet resultSet ) {
		return new FmArray( fieldIndex, resultSet );
	}

	class FmArray implements Array {
		private int fieldIndex;
		private FmResultSet resultSet;

		FmArray( int fieldIndex, FmResultSet resultSet ) {
			this.fieldIndex = fieldIndex;
			this.resultSet = resultSet;
		}

		public String getBaseTypeName() throws SQLException {
			FmField whichField = fieldList.get( fieldIndex );
			return whichField.getType().getExternalTypeName();
		}

		public int getBaseType() throws SQLException {
			FmField whichField = fieldList.get( fieldIndex );
			return whichField.getType().getSqlDataType();
		}

		public void free() throws SQLException {}

		public Object getArray() throws SQLException {
			return getArray( 0, valueCounts[fieldIndex], null );
		}

		public Object getArray( Map<String, Class<?>> map ) throws SQLException {
			return getArray( 0, valueCounts[fieldIndex], map );
		}

		public Object getArray( long index, int count ) throws SQLException {
			return getArray( index, count, null );
		}

		public Object getArray( long index, int count, @Nullable Map<String, Class<?>> map ) throws SQLException {
			Object[] result = new Object[ count ];
			ResultSet rs = getResultSet( index, count, map );
			int n=0;
			while( rs.next() ) {
				result[n++] = rs.getObject( 1 );
			}
			return result;
		}

		public ResultSet getResultSet() throws SQLException {
			return getResultSet( 0, valueCounts[fieldIndex], null );
		}

		public ResultSet getResultSet( Map<String, Class<?>> map ) throws SQLException {
			return getResultSet( 0, valueCounts[fieldIndex], null );
		}

		public ResultSet getResultSet( long index, int count ) throws SQLException {
			return getResultSet( index, count, null );
		}

		public ResultSet getResultSet( long index, int count, @Nullable Map<String, Class<?>> map ) throws SQLException {
			if( count + index > valueCounts[fieldIndex] )
				throw new SQLException( "Invalid index(" + index + ") and count (" + count + "), there are only " + valueCounts[fieldIndex] + " elements in the repetition" );
			FmFieldList repetitionFieldList = new FmFieldList();
			repetitionFieldList.add( fieldList.get( fieldIndex ) );
			return new FmResultSet( FmRecord.this, 0, valueCounts[fieldIndex], fieldIndex, repetitionFieldList, resultSet, null );
		}
	}
}
