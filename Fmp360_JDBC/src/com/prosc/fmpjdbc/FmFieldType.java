package com.prosc.fmpjdbc;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.Map;
import java.util.HashMap;

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
 * Date: Apr 20, 2005
 * Time: 11:41:39 AM
 */
public class FmFieldType {
	public static final Map typesByName = new HashMap(6);
	public static final FmFieldList resultSetFormat = new FmFieldList();
	public static final FmFieldType TEXT, NUMBER, RECID, DATE, TIME, TIMESTAMP, CONTAINER;

	static {
		TEXT = new FmFieldType("TEXT", Types.VARCHAR, Integer.MAX_VALUE); //Used to be LONGVARCHAR, but then EOModeler models that as a 'C' CharacterStream instead of 'S' String
		TEXT.setAutoIncrement(true);

		NUMBER = new FmFieldType("NUMBER", Types.DECIMAL, 400);
		NUMBER.setLiteralPrefix(null);
		NUMBER.setLiteralSuffix(null);
		NUMBER.setFixedPrecisionScale(true);
		NUMBER.setAutoIncrement(true);

		RECID = new FmFieldType( "RECID", Types.INTEGER, 400 ); //FIX!! Don't know what to put for precision for integers
		RECID.setLiteralPrefix( null );
		RECID.setLiteralSuffix( null );
		RECID.setFixedPrecisionScale( false ); //FIX!! I don't really know what goes here; just guessing
		RECID.setAutoIncrement( true ); //FIX!! I don't really know what goes here; just copied from previous

		DATE = new FmFieldType("DATE", Types.DATE, 32 );

		TIME = new FmFieldType("TIME", Types.TIME, 32 );

		TIMESTAMP = new FmFieldType("TIMESTAMP", Types.TIMESTAMP, 64 );

		CONTAINER = new FmFieldType("BLOB", Types.BLOB, Integer.MAX_VALUE );
		CONTAINER.setSearchable( (short)DatabaseMetaData.typePredNone);
		
		FmFieldType[] types = new FmFieldType[] { TEXT, NUMBER, RECID, DATE, TIME, TIMESTAMP };
		for( int n=0; n<types.length; n++ ) {
			typesByName.put( types[n].getTypeName(), types[n] );
		}
        typesByName.put( "CONTAINER", CONTAINER ); //Needs special handling because we name it "BLOB", not "CONTAINER" like FileMaker.

		FmTable metaDataTable = new FmTable("fmp_jdbc_metadata");
		resultSetFormat.add( new FmField(metaDataTable, "TYPE_NAME", null, types[0], false) );
		resultSetFormat.add( new FmField(metaDataTable, "DATA_TYPE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "PRECISION", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "LITERAL_PREFIX", null, types[0], true) );
		resultSetFormat.add( new FmField(metaDataTable, "LITERAL_SUFFIX", null, types[0], true) );
		resultSetFormat.add( new FmField(metaDataTable, "CREATE_PARAMS", null, types[0], true) );
		resultSetFormat.add( new FmField(metaDataTable, "NULLABLE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "CASE_SENSITIVE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "SEARCHABLE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "UNSIGNED_ATTRIBUTE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "FIXED_PREC_SCALE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "AUTO_INCREMENT", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "LOCAL_TYPE_NAME", null, types[0], true) );
		resultSetFormat.add( new FmField(metaDataTable, "MINIMUM_SCALE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "MAXIMUM_SCALE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "SQL_DATA_TYPE", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "SQL_DATETIME_SUB", null, types[1], false) );
		resultSetFormat.add( new FmField(metaDataTable, "NUM_PREC_RADIX", null, types[1], false) );
	}

	private String typeName;
	private int sqlDataType;
	private int precision;
	private String literalPrefix = "'";
	private String literalSuffix = "'";
	private String createParams = null;
	private short nullable = DatabaseMetaData.attributeNullable;
	private boolean caseSensitive = false;
	private short searchable = DatabaseMetaData.typeSearchable;
	private boolean unsignedAttribute = false;
	private boolean fixedPrecisionScale = false;
	private boolean autoIncrement = false;
	private String localTypeName = null;
	private short minimumScale = 0; //FIX!!! I don't really know what this is asking for --jsb
	private short maximumScale = 100; //FIX!!! I don't really know what this is asking for --jsb
	private int numberPrecisionRadix = 10;

	private FmFieldType(String typeName, int sqlDataType, int precision) {
		this.typeName = typeName;
		this.sqlDataType = sqlDataType;
		this.precision = precision;
	}

	public FmRecord getInResultSetFormat() {
		FmRecord result = new FmRecord( resultSetFormat, null, null );
		result.setRawValue( typeName, 0 );
		result.setRawValue( "" + sqlDataType, 1 );
		result.setRawValue( "" + precision, 2 );
		result.setRawValue( literalPrefix, 3 );
		result.setRawValue( literalSuffix, 4 );
		result.setRawValue( createParams, 5 );
		result.setRawValue( "" + nullable, 6 );
		result.setRawValue( "" + caseSensitive, 7 );
		result.setRawValue( "" + searchable, 8 );
		result.setRawValue( "" + unsignedAttribute, 9 );
		result.setRawValue( "" + fixedPrecisionScale, 10 );
		result.setRawValue( "" + autoIncrement, 11 );
		result.setRawValue( localTypeName, 12 );
		result.setRawValue( "" + minimumScale, 13 );
		result.setRawValue( "" + maximumScale, 14 );
		result.setRawValue( "0", 15 );
		result.setRawValue( "0", 16 );
		result.setRawValue( "" + numberPrecisionRadix, 17 );
		return result;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public int getSqlDataType() {
		return sqlDataType;
	}

	public void setSqlDataType(int sqlDataType) {
		this.sqlDataType = sqlDataType;
	}

	public int getPrecision() {
		return precision;
	}

	public void setPrecision(int precision) {
		this.precision = precision;
	}

	public String getLiteralPrefix() {
		return literalPrefix;
	}

	public void setLiteralPrefix(String literalPrefix) {
		this.literalPrefix = literalPrefix;
	}

	public String getLiteralSuffix() {
		return literalSuffix;
	}

	public void setLiteralSuffix(String literalSuffix) {
		this.literalSuffix = literalSuffix;
	}

	public String getCreateParams() {
		return createParams;
	}

	public void setCreateParams(String createParams) {
		this.createParams = createParams;
	}

	public short getNullable() {
		return nullable;
	}

	public void setNullable(short nullable) {
		this.nullable = nullable;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public short getSearchable() {
		return searchable;
	}

	public void setSearchable(short searchable) {
		this.searchable = searchable;
	}

	public boolean isUnsignedAttribute() {
		return unsignedAttribute;
	}

	public void setUnsignedAttribute(boolean unsignedAttribute) {
		this.unsignedAttribute = unsignedAttribute;
	}

	public boolean isFixedPrecisionScale() {
		return fixedPrecisionScale;
	}

	public void setFixedPrecisionScale(boolean fixedPrecisionScale) {
		this.fixedPrecisionScale = fixedPrecisionScale;
	}

	public boolean isAutoIncrement() {
		return autoIncrement;
	}

	public void setAutoIncrement(boolean autoIncrement) {
		this.autoIncrement = autoIncrement;
	}

	public String getLocalTypeName() {
		return localTypeName;
	}

	public void setLocalTypeName(String localTypeName) {
		this.localTypeName = localTypeName;
	}

	public short getMinimumScale() {
		return minimumScale;
	}

	public void setMinimumScale(short minimumScale) {
		this.minimumScale = minimumScale;
	}

	public short getMaximumScale() {
		return maximumScale;
	}

	public void setMaximumScale(short maximumScale) {
		this.maximumScale = maximumScale;
	}

	public int getNumberPrecisionRadix() {
		return numberPrecisionRadix;
	}

	public void setNumberPrecisionRadix(int numberPrecisionRadix) {
		this.numberPrecisionRadix = numberPrecisionRadix;
	}
}
