package com.prosc.fmpjdbc;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 17, 2005 Time: 1:49:59 AM
 */
public class FmResultSetMetaData implements ResultSetMetaData {
	private FmFieldList fieldDefinitions;

	public FmResultSetMetaData(FmFieldList fieldDefinitions) {
		if( fieldDefinitions == null ) {
			throw new IllegalArgumentException("You must supply a non-null FmFieldList value" );
		}
		this.fieldDefinitions = fieldDefinitions;
	}

	//---These methods must be implemented---
	public int getColumnCount() throws SQLException {
		return fieldDefinitions.getFields().size();
	}

	public String getColumnClassName( int i ) throws SQLException {
		return fieldDefinitions.get(i-1).getType().getJavaClass().getName();
	}

	public int getColumnType( int i ) throws SQLException {
		FmFieldType type = fieldDefinitions.get( i - 1 ).getType();
		return type.getSqlDataType();
	}

	public String getColumnLabel( int i ) throws SQLException {
		return getColumnName(i); // FIX! less than ideal -ssb
	}

	public String getColumnName( int i ) throws SQLException {
		return fieldDefinitions.get(i - 1).getColumnName();
	}

	public String getColumnTypeName( int i ) throws SQLException {
		throw new AbstractMethodError( "getColumnTypeName is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getPrecision( int i ) throws SQLException {
		throw new AbstractMethodError( "getPrecision is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getScale( int i ) throws SQLException {
		throw new AbstractMethodError( "getScale is not implemented yet." ); //FIX!!! Broken placeholder
	}


	//---These can be left abstract for now---

	public boolean isAutoIncrement( int i ) throws SQLException {
		throw new AbstractMethodError( "isAutoIncrement is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isCaseSensitive( int i ) throws SQLException {
		throw new AbstractMethodError( "isCaseSensitive is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isSearchable( int i ) throws SQLException {
		throw new AbstractMethodError( "isSearchable is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isCurrency( int i ) throws SQLException {
		throw new AbstractMethodError( "isCurrency is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int isNullable( int i ) throws SQLException {
		throw new AbstractMethodError( "isNullable is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isSigned( int i ) throws SQLException {
		throw new AbstractMethodError( "isSigned is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getColumnDisplaySize( int i ) throws SQLException {
		throw new AbstractMethodError( "getColumnDisplaySize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getSchemaName( int i ) throws SQLException {
		throw new AbstractMethodError( "getSchemaName is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getTableName( int i ) throws SQLException {
		throw new AbstractMethodError( "getTableName is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getCatalogName( int i ) throws SQLException {
		throw new AbstractMethodError( "getCatalogName is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isReadOnly( int i ) throws SQLException {
		throw new AbstractMethodError( "isReadOnly is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isWritable( int i ) throws SQLException {
		throw new AbstractMethodError( "isWritable is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isDefinitelyWritable( int i ) throws SQLException {
		throw new AbstractMethodError( "isDefinitelyWritable is not implemented yet." ); //FIX!!! Broken placeholder
	}
	
	// ==== New methods added in Java 1.5 ====

	public <T> T unwrap( Class<T> aClass ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public boolean isWrapperFor( Class<?> aClass ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}
}
