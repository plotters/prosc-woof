package com.prosc.fmpjdbc;

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
	
	private Long recordId;
	private Long modCount;
	private FmFieldList fieldList;
	private String[] rawValues;

	public FmRecord(FmFieldList fieldList, Long recordId, Long modCount) {
		this.fieldList = fieldList;
		this.rawValues = new String[fieldList.getFields().size()];
		this.recordId = recordId;
		this.modCount = modCount;
	}


	public Long getRecordId() {
		return recordId;
	}

	public Long getModCount() {
		return modCount;
	}

	public FmFieldList getFieldList() {
		return fieldList;
	}

	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("RecordId: " + recordId + "; ModCount: " + modCount + "; Data: ");
		for (int n = 0; n < rawValues.length; n++) {
			result.append("(" + rawValues[n] + ")");
		}
		return result.toString();
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
	protected String getRawValue(int index) {
		return rawValues[index];
	}

	/** Provides write access directly to the String array for this FmRecord. */
	protected void setRawValue(String newValue, int index) {
		rawValues[index] = newValue;
	}

	public Object getValue(int index) {
		Object result = rawValues[index];
		if( result == null ) {
			fieldList.wasNull = true;
			if( ! fieldList.get(index).isNullable() ) result = ""; //FIX!!! Use formatter to parse value; return zero for numeric values
		} else fieldList.wasNull = false;
		return result;
	}


	public Object getObject(int index, FmConnection connection) {
		Object result;
		FmFieldType fmType = fieldList.get( index ).getType();
		int sqlType = fmType.getSqlDataType();

		switch (sqlType ) {
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
				result = getString(index);
				break;
			case Types.DECIMAL: result = getBigDecimal(index); //All filemaker numbers are treated as decimal by default
				break;
			case Types.DATE: result = getDate(index);
				break;
			case Types.TIME: result = getTime(index);
				break;
			case Types.TIMESTAMP: result = getTimestamp(index);
				break;
			case Types.INTEGER: result = new Integer( getInt(index) ); //This will only happen for recIds
				break;
			case Types.BLOB: result = getBlob(index, connection);
				break;
			default: result = getString(index);
		}

		return result;
	}

	// Implementing the get methods for FmResultSet at the FmRecord level

	/**
	 * Returns the string value for the record. This implementation returns null for empty strings (since FileMaker does not distinguish between null and empty); it's
	 * debatable which behavior is correct.
	* @return returns String value
	*/
	public String getString(int i) { //OPTIMIZE Shouldn't need to check for both null and length 0; check to see which way FmXmlResult handles it
		Object rawValue = rawValues[i];
		if( rawValue == null ) {
			fieldList.wasNull = true;
			return fieldList.get(i).isNullable() ? null : "";
		}
		String result = rawValue.toString();
		if( result.length() == 0 ) {
			fieldList.wasNull = true;
			return fieldList.get(i).isNullable() ? null : "";
		} else {
			fieldList.wasNull = false;
			return result;
		}
	}

	/*
	* @Return returns the boolean and column i
	*/
	public boolean getBoolean(int i) {
		String rawValue = getRawValue(i);
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


	public byte getByte(int i) throws NumberFormatException {
		String rawValue = getRawValue(i);
		fieldList.wasNull = (rawValue == null);
		if( rawValue == null || rawValue.length() == 0 ) return 0;
		return Byte.valueOf( rawValue ).byteValue();
	}

	public short getShort(int i) throws NumberFormatException {
		String rawValue = getRawValue(i);
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Short.valueOf( rawValue ).shortValue();
		} catch( NumberFormatException e ) {
			return Short.valueOf( NumberUtils.removeNonNumericChars( rawValue ) ).shortValue();
		}
	}

	public int getInt(int i) throws NumberFormatException {
		String rawValue = getRawValue(i);
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Integer.valueOf( rawValue ).intValue();
		} catch(NumberFormatException e) {
			String stripDigits = NumberUtils.removeNonNumericChars( rawValue );
			return new BigDecimal( stripDigits ).intValue(); //We do big decimal here so that if there is a decimal, we just toss it instead of getting a NumberFormatException
		}
	}

	public long getLong(int i) throws NumberFormatException {
		String rawValue = getRawValue(i);
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Long.valueOf( rawValue ).longValue();
		} catch( NumberFormatException e ) {
			return Long.valueOf( NumberUtils.removeNonNumericChars( rawValue ) ).longValue();
		}
	}

	public float getFloat(int i) throws NumberFormatException {
		String rawValue = getRawValue(i);
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Float.valueOf( rawValue ).floatValue();
		} catch( NumberFormatException e ) {
			return Float.valueOf( NumberUtils.removeNonNumericChars( rawValue ) ).floatValue();
		}
	}

	public double getDouble(int i) throws NumberFormatException {
		String rawValue = getRawValue(i);
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return 0;
		} else fieldList.wasNull = false;
		try {
			return Double.valueOf( rawValue ).doubleValue();
		} catch( NumberFormatException e ) {
			return Double.valueOf( NumberUtils.removeNonNumericChars( rawValue ) ).doubleValue();
		}
	}


	public BigDecimal getBigDecimal( int i ) throws NumberFormatException {
		String rawValue = rawValues[i];
		if( rawValue == null || rawValue.length() == 0 ) {
			fieldList.wasNull = true;
			return new BigDecimal(0d);
		} else {
			fieldList.wasNull = false;
		}
		try {
			return new BigDecimal(rawValue);
		} catch(NumberFormatException e) { //Strip all non-numeric characters and try again
			return new BigDecimal( NumberUtils.removeNonNumericChars( rawValue ) );
		}
	}

	public static ThreadLocal dateFormat = new ThreadLocal() {
		protected Object initialValue() {
			return new SimpleDateFormat("MM/dd/yyyy");
		}
	};

	//FIX!! If we're using FileMaker 6, we need much smarter parsers, because FM6 doesn't normalize the data in the XML

	public static ThreadLocal timeFormat = new ThreadLocal() {
		protected Object initialValue() {
			return new SimpleDateFormat("HH:mm:ss");
		}
	};

	public static ThreadLocal timestampFormat = new ThreadLocal() {
		protected Object initialValue() {
			return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		}
	};

	public Date getDate(int i) throws IllegalArgumentException {
		final TimeZone zone = defaultTimeZone;
		return getDate(i, zone);
	}

	public Date getDate(final int i, final TimeZone zone) {
		String rawValue = getRawValue(i);
		if( rawValue == null || rawValue.length() == 0 || "?".equals(rawValue) ) { //FIX! I don't know if ignoring "?" is the best policy --jsb
			fieldList.wasNull = true;
			return null;
		} else fieldList.wasNull = false;
		try {
			DateFormat format = (DateFormat)dateFormat.get();
			format.setTimeZone(zone);
			java.util.Date date = format.parse( rawValue );
			if (log.isLoggable(Level.FINE)) {
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

	public Time getTime(int i) throws IllegalArgumentException {
		final TimeZone timeZone = defaultTimeZone;
		return getTime(i, timeZone);
	}

	public Time getTime(final int i, final TimeZone timeZone) {
		String rawValue = getRawValue(i);
		if( rawValue == null || rawValue.length() == 0 || "?".equals(rawValue) ) { //FIX! I don't know if ignoring "?" is the best policy --jsb
			fieldList.wasNull = true;
			return null;
		} else fieldList.wasNull = false;
		try {
			DateFormat format = (DateFormat)timeFormat.get();
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

	public Timestamp getTimestamp(int i) throws IllegalArgumentException {
		String rawValue = getRawValue(i);
		if( rawValue == null || rawValue.length() == 0 || "?".equals(rawValue) ) { //FIX! I don't know if ignoring "?" is the best policy --jsb
			fieldList.wasNull = true;
			return null;
		} else fieldList.wasNull = false;
		try {
			DateFormat format = (DateFormat)timestampFormat.get();
			return new java.sql.Timestamp( format.parse(rawValue).getTime() );
		} catch( ParseException e ) {
			IllegalArgumentException e1 = new IllegalArgumentException(e.toString());
			e1.initCause(e);
			//FIX!! Need configurable exception handling on whether to return null or rethrow --jsb
			//log.log( Level.WARNING, "Can't parse this as a timestamp: " + rawValue, e1 );
			//return null;
			throw e1;
		}
	}

	public Blob getBlob(int i, FmConnection connection) throws IllegalArgumentException {

		String rawValue = getRawValue(i);

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



}
