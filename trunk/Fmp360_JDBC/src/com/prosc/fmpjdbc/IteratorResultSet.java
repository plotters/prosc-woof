package com.prosc.fmpjdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * @author sbarnum
 */
public class IteratorResultSet implements ResultSet {
	final Iterator<LinkedHashMap<String, Object>> iterator;
	private LinkedHashMap current;
	private Object lastObject;
	private boolean first = true;

	public IteratorResultSet(final Iterator<LinkedHashMap<String,Object>> iterator) {
		this.iterator = iterator;
	}

	public boolean next() throws SQLException {
		try {
			current = iterator.next();
			first = false;
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void close() throws SQLException {
	}

	public boolean wasNull() throws SQLException {
		return lastObject == null;
	}

	private Object _getByIndex(int columnIndex) throws SQLException {
		if (current == null) {
			throw new SQLException("You must call next()");
		}
		int i=0;
		for (Object eachValue : current.values()) {
			if (++i == columnIndex) return eachValue;
		}
		throw new NoSuchElementException(columnIndex + ">" + current.values().size());
	}

	private Object _getByLabel(final String columnLabel) {
		return current.get(columnLabel);
	}
	
	public String getString(final int columnIndex) throws SQLException {
		return (String) _getByIndex(columnIndex);
	}

	public boolean getBoolean(final int columnIndex) throws SQLException {
		return (Boolean) _getByIndex(columnIndex);
	}

	public byte getByte(final int columnIndex) throws SQLException {
		return (Byte) _getByIndex(columnIndex);
	}

	public short getShort(final int columnIndex) throws SQLException {
		return (Short) _getByIndex(columnIndex);
	}

	public int getInt(final int columnIndex) throws SQLException {
		return (Integer) _getByIndex(columnIndex);
	}

	public long getLong(final int columnIndex) throws SQLException {
		return (Long) _getByIndex(columnIndex);
	}

	public float getFloat(final int columnIndex) throws SQLException {
		return (Float) _getByIndex(columnIndex);
	}

	public double getDouble(final int columnIndex) throws SQLException {
		return (Double) _getByIndex(columnIndex);
	}

	public BigDecimal getBigDecimal(final int columnIndex, final int scale) throws SQLException {
		return (BigDecimal) _getByIndex(columnIndex);
	}

	public byte[] getBytes(final int columnIndex) throws SQLException {
		return (byte[]) _getByIndex(columnIndex);
	}

	public Date getDate(final int columnIndex) throws SQLException {
		return (Date) _getByIndex(columnIndex);
	}

	public Time getTime(final int columnIndex) throws SQLException {
		return (Time) _getByIndex(columnIndex);
	}

	public Timestamp getTimestamp(final int columnIndex) throws SQLException {
		return (Timestamp) _getByIndex(columnIndex);
	}

	public InputStream getAsciiStream(final int columnIndex) throws SQLException {
		return (InputStream) _getByIndex(columnIndex);
	}

	public InputStream getUnicodeStream(final int columnIndex) throws SQLException {
		return (InputStream) _getByIndex(columnIndex);
	}

	public InputStream getBinaryStream(final int columnIndex) throws SQLException {
		return (InputStream) _getByIndex(columnIndex);
	}

	public String getString(final String columnLabel) throws SQLException {
		return (String) _getByLabel(columnLabel);
	}

	public boolean getBoolean(final String columnLabel) throws SQLException {
		return (Boolean)_getByLabel(columnLabel);
	}

	public byte getByte(final String columnLabel) throws SQLException {
		return ((Number)_getByLabel(columnLabel)).byteValue();
	}

	public short getShort(final String columnLabel) throws SQLException {
		return ((Number)_getByLabel(columnLabel)).shortValue();
	}

	public int getInt(final String columnLabel) throws SQLException {
		return ((Number)_getByLabel(columnLabel)).intValue();
	}

	public long getLong(final String columnLabel) throws SQLException {
		return ((Number)_getByLabel(columnLabel)).longValue();
	}

	public float getFloat(final String columnLabel) throws SQLException {
		return ((Number)_getByLabel(columnLabel)).floatValue();
	}

	public double getDouble(final String columnLabel) throws SQLException {
		return ((Number)_getByLabel(columnLabel)).doubleValue();
	}

	public BigDecimal getBigDecimal(final String columnLabel, final int scale) throws SQLException {
		return ((BigDecimal)_getByLabel(columnLabel));
	}

	public byte[] getBytes(final String columnLabel) throws SQLException {
		return (byte[]) _getByLabel(columnLabel);
	}

	public Date getDate(final String columnLabel) throws SQLException {
		return (Date) _getByLabel(columnLabel);
	}

	public Time getTime(final String columnLabel) throws SQLException {
		return (Time) _getByLabel(columnLabel);
	}

	public Timestamp getTimestamp(final String columnLabel) throws SQLException {
		return (Timestamp) _getByLabel(columnLabel);
	}

	public InputStream getAsciiStream(final String columnLabel) throws SQLException {
		return (InputStream) _getByLabel(columnLabel);
	}

	public InputStream getUnicodeStream(final String columnLabel) throws SQLException {
		return (InputStream) _getByLabel(columnLabel);
	}

	public InputStream getBinaryStream(final String columnLabel) throws SQLException {
		return (InputStream) _getByLabel(columnLabel);
	}

	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException {
	}

	public String getCursorName() throws SQLException {
		return null;
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return null;
	}

	public Object getObject(final int columnIndex) throws SQLException {
		return _getByIndex(columnIndex);
	}

	public Object getObject(final String columnLabel) throws SQLException {
		return _getByLabel(columnLabel);
	}

	public int findColumn(final String columnLabel) throws SQLException {
		return new ArrayList( current.keySet()).indexOf(columnLabel);
	}

	public Reader getCharacterStream(final int columnIndex) throws SQLException {
		return (Reader) _getByIndex(columnIndex);
	}

	public Reader getCharacterStream(final String columnLabel) throws SQLException {
		return (Reader) _getByLabel(columnLabel);
	}

	public BigDecimal getBigDecimal(final int columnIndex) throws SQLException {
		return (BigDecimal) _getByIndex(columnIndex);
	}

	public BigDecimal getBigDecimal(final String columnLabel) throws SQLException {
		return (BigDecimal) _getByLabel(columnLabel);
	}

	public boolean isBeforeFirst() throws SQLException {
		return false;
	}

	public boolean isAfterLast() throws SQLException {
		return false;
	}

	public boolean isFirst() throws SQLException {
		return first;
	}

	public boolean isLast() throws SQLException {
		return !iterator.hasNext();
	}

	public void beforeFirst() throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public void afterLast() throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public boolean first() throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public boolean last() throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public int getRow() throws SQLException {
		return 0;
	}

	public boolean absolute(final int row) throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public boolean relative(final int rows) throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public boolean previous() throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public void setFetchDirection(final int direction) throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public int getFetchDirection() throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public void setFetchSize(final int rows) throws SQLException {
		throw new SQLException("Not Implemented");
	}

	public int getFetchSize() throws SQLException {
		return 0;
	}

	public int getType() throws SQLException {
		return 0;
	}

	public int getConcurrency() throws SQLException {
		return 0;
	}

	public boolean rowUpdated() throws SQLException {
		return false;
	}

	public boolean rowInserted() throws SQLException {
		return false;
	}

	public boolean rowDeleted() throws SQLException {
		return false;
	}

	public void updateNull(final int columnIndex) throws SQLException {

	}

	public void updateBoolean(final int columnIndex, final boolean x) throws SQLException {

	}

	public void updateByte(final int columnIndex, final byte x) throws SQLException {

	}

	public void updateShort(final int columnIndex, final short x) throws SQLException {

	}

	public void updateInt(final int columnIndex, final int x) throws SQLException {

	}

	public void updateLong(final int columnIndex, final long x) throws SQLException {

	}

	public void updateFloat(final int columnIndex, final float x) throws SQLException {

	}

	public void updateDouble(final int columnIndex, final double x) throws SQLException {

	}

	public void updateBigDecimal(final int columnIndex, final BigDecimal x) throws SQLException {

	}

	public void updateString(final int columnIndex, final String x) throws SQLException {

	}

	public void updateBytes(final int columnIndex, final byte[] x) throws SQLException {

	}

	public void updateDate(final int columnIndex, final Date x) throws SQLException {

	}

	public void updateTime(final int columnIndex, final Time x) throws SQLException {

	}

	public void updateTimestamp(final int columnIndex, final Timestamp x) throws SQLException {

	}

	public void updateAsciiStream(final int columnIndex, final InputStream x, final int length) throws SQLException {

	}

	public void updateBinaryStream(final int columnIndex, final InputStream x, final int length) throws SQLException {

	}

	public void updateCharacterStream(final int columnIndex, final Reader x, final int length) throws SQLException {

	}

	public void updateObject(final int columnIndex, final Object x, final int scaleOrLength) throws SQLException {

	}

	public void updateObject(final int columnIndex, final Object x) throws SQLException {

	}

	public void updateNull(final String columnLabel) throws SQLException {

	}

	public void updateBoolean(final String columnLabel, final boolean x) throws SQLException {

	}

	public void updateByte(final String columnLabel, final byte x) throws SQLException {

	}

	public void updateShort(final String columnLabel, final short x) throws SQLException {

	}

	public void updateInt(final String columnLabel, final int x) throws SQLException {

	}

	public void updateLong(final String columnLabel, final long x) throws SQLException {

	}

	public void updateFloat(final String columnLabel, final float x) throws SQLException {

	}

	public void updateDouble(final String columnLabel, final double x) throws SQLException {

	}

	public void updateBigDecimal(final String columnLabel, final BigDecimal x) throws SQLException {

	}

	public void updateString(final String columnLabel, final String x) throws SQLException {

	}

	public void updateBytes(final String columnLabel, final byte[] x) throws SQLException {

	}

	public void updateDate(final String columnLabel, final Date x) throws SQLException {

	}

	public void updateTime(final String columnLabel, final Time x) throws SQLException {

	}

	public void updateTimestamp(final String columnLabel, final Timestamp x) throws SQLException {

	}

	public void updateAsciiStream(final String columnLabel, final InputStream x, final int length) throws SQLException {

	}

	public void updateBinaryStream(final String columnLabel, final InputStream x, final int length) throws SQLException {

	}

	public void updateCharacterStream(final String columnLabel, final Reader reader, final int length) throws SQLException {

	}

	public void updateObject(final String columnLabel, final Object x, final int scaleOrLength) throws SQLException {

	}

	public void updateObject(final String columnLabel, final Object x) throws SQLException {

	}

	public void insertRow() throws SQLException {

	}

	public void updateRow() throws SQLException {

	}

	public void deleteRow() throws SQLException {

	}

	public void refreshRow() throws SQLException {

	}

	public void cancelRowUpdates() throws SQLException {

	}

	public void moveToInsertRow() throws SQLException {

	}

	public void moveToCurrentRow() throws SQLException {

	}

	public Statement getStatement() throws SQLException {
		return null;
	}

	public Object getObject(final int columnIndex, final Map<String, Class<?>> map) throws SQLException {
		return null;
	}

	public Ref getRef(final int columnIndex) throws SQLException {
		return null;
	}

	public Blob getBlob(final int columnIndex) throws SQLException {
		return null;
	}

	public Clob getClob(final int columnIndex) throws SQLException {
		return null;
	}

	public Array getArray(final int columnIndex) throws SQLException {
		return null;
	}

	public Object getObject(final String columnLabel, final Map<String, Class<?>> map) throws SQLException {
		return null;
	}

	public Ref getRef(final String columnLabel) throws SQLException {
		return null;
	}

	public Blob getBlob(final String columnLabel) throws SQLException {
		return null;
	}

	public Clob getClob(final String columnLabel) throws SQLException {
		return null;
	}

	public Array getArray(final String columnLabel) throws SQLException {
		return null;
	}

	public Date getDate(final int columnIndex, final Calendar cal) throws SQLException {
		return null;
	}

	public Date getDate(final String columnLabel, final Calendar cal) throws SQLException {
		return null;
	}

	public Time getTime(final int columnIndex, final Calendar cal) throws SQLException {
		return null;
	}

	public Time getTime(final String columnLabel, final Calendar cal) throws SQLException {
		return null;
	}

	public Timestamp getTimestamp(final int columnIndex, final Calendar cal) throws SQLException {
		return null;
	}

	public Timestamp getTimestamp(final String columnLabel, final Calendar cal) throws SQLException {
		return null;
	}

	public URL getURL(final int columnIndex) throws SQLException {
		return null;
	}

	public URL getURL(final String columnLabel) throws SQLException {
		return null;
	}

	public void updateRef(final int columnIndex, final Ref x) throws SQLException {

	}

	public void updateRef(final String columnLabel, final Ref x) throws SQLException {

	}

	public void updateBlob(final int columnIndex, final Blob x) throws SQLException {

	}

	public void updateBlob(final String columnLabel, final Blob x) throws SQLException {

	}

	public void updateClob(final int columnIndex, final Clob x) throws SQLException {

	}

	public void updateClob(final String columnLabel, final Clob x) throws SQLException {

	}

	public void updateArray(final int columnIndex, final Array x) throws SQLException {

	}

	public void updateArray(final String columnLabel, final Array x) throws SQLException {

	}

	public RowId getRowId(final int columnIndex) throws SQLException {
		return null;
	}

	public RowId getRowId(final String columnLabel) throws SQLException {
		return null;
	}

	public void updateRowId(final int columnIndex, final RowId x) throws SQLException {

	}

	public void updateRowId(final String columnLabel, final RowId x) throws SQLException {

	}

	public int getHoldability() throws SQLException {
		return 0;
	}

	public boolean isClosed() throws SQLException {
		return false;
	}

	public void updateNString(final int columnIndex, final String nString) throws SQLException {

	}

	public void updateNString(final String columnLabel, final String nString) throws SQLException {

	}

	public void updateNClob(final int columnIndex, final NClob nClob) throws SQLException {

	}

	public void updateNClob(final String columnLabel, final NClob nClob) throws SQLException {

	}

	public NClob getNClob(final int columnIndex) throws SQLException {
		return null;
	}

	public NClob getNClob(final String columnLabel) throws SQLException {
		return null;
	}

	public SQLXML getSQLXML(final int columnIndex) throws SQLException {
		return null;
	}

	public SQLXML getSQLXML(final String columnLabel) throws SQLException {
		return null;
	}

	public void updateSQLXML(final int columnIndex, final SQLXML xmlObject) throws SQLException {

	}

	public void updateSQLXML(final String columnLabel, final SQLXML xmlObject) throws SQLException {

	}

	public String getNString(final int columnIndex) throws SQLException {
		return null;
	}

	public String getNString(final String columnLabel) throws SQLException {
		return null;
	}

	public Reader getNCharacterStream(final int columnIndex) throws SQLException {
		return null;
	}

	public Reader getNCharacterStream(final String columnLabel) throws SQLException {
		return null;
	}

	public void updateNCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {

	}

	public void updateNCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {

	}

	public void updateAsciiStream(final int columnIndex, final InputStream x, final long length) throws SQLException {

	}

	public void updateBinaryStream(final int columnIndex, final InputStream x, final long length) throws SQLException {

	}

	public void updateCharacterStream(final int columnIndex, final Reader x, final long length) throws SQLException {

	}

	public void updateAsciiStream(final String columnLabel, final InputStream x, final long length) throws SQLException {

	}

	public void updateBinaryStream(final String columnLabel, final InputStream x, final long length) throws SQLException {

	}

	public void updateCharacterStream(final String columnLabel, final Reader reader, final long length) throws SQLException {

	}

	public void updateBlob(final int columnIndex, final InputStream inputStream, final long length) throws SQLException {

	}

	public void updateBlob(final String columnLabel, final InputStream inputStream, final long length) throws SQLException {

	}

	public void updateClob(final int columnIndex, final Reader reader, final long length) throws SQLException {

	}

	public void updateClob(final String columnLabel, final Reader reader, final long length) throws SQLException {

	}

	public void updateNClob(final int columnIndex, final Reader reader, final long length) throws SQLException {

	}

	public void updateNClob(final String columnLabel, final Reader reader, final long length) throws SQLException {

	}

	public void updateNCharacterStream(final int columnIndex, final Reader x) throws SQLException {

	}

	public void updateNCharacterStream(final String columnLabel, final Reader reader) throws SQLException {

	}

	public void updateAsciiStream(final int columnIndex, final InputStream x) throws SQLException {

	}

	public void updateBinaryStream(final int columnIndex, final InputStream x) throws SQLException {

	}

	public void updateCharacterStream(final int columnIndex, final Reader x) throws SQLException {

	}

	public void updateAsciiStream(final String columnLabel, final InputStream x) throws SQLException {

	}

	public void updateBinaryStream(final String columnLabel, final InputStream x) throws SQLException {

	}

	public void updateCharacterStream(final String columnLabel, final Reader reader) throws SQLException {

	}

	public void updateBlob(final int columnIndex, final InputStream inputStream) throws SQLException {

	}

	public void updateBlob(final String columnLabel, final InputStream inputStream) throws SQLException {

	}

	public void updateClob(final int columnIndex, final Reader reader) throws SQLException {

	}

	public void updateClob(final String columnLabel, final Reader reader) throws SQLException {

	}

	public void updateNClob(final int columnIndex, final Reader reader) throws SQLException {

	}

	public void updateNClob(final String columnLabel, final Reader reader) throws SQLException {

	}

	public <T> T getObject(final int columnIndex, final Class<T> type) throws SQLException {
		return null;
	}

	public <T> T getObject(final String columnLabel, final Class<T> type) throws SQLException {
		return null;
	}

	public <T> T unwrap(final Class<T> iface) throws SQLException {
		return null;
	}

	public boolean isWrapperFor(final Class<?> iface) throws SQLException {
		return false;
	}
}
