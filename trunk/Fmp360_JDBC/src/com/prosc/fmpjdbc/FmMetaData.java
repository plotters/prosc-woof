package com.prosc.fmpjdbc;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Connection;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.MalformedURLException;

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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 16, 2005 Time: 6:32:38 PM
 */
public class FmMetaData implements DatabaseMetaData {
	private String databaseProductName;
	private String databaseProductVersion;
	private int dbMajorVersion;
	private int dbMinorVersion;
	private FmConnection connection;
	//private List databaseNames;

	private String anyTableName;
	private int fieldCount;
	private String catalogSeparator;
	private Logger logger = Logger.getLogger("com.prosc.fmpjdbc");

	public FmMetaData(FmConnection connection) throws IOException, FileMakerException {
		this.connection = connection;
		FmXmlRequest requestHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
            connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
		logger.log(Level.FINER, "Creating FmMetaData");
		requestHandler.doRequest("-max=0&-dbnames");
		//databaseNames = iterator2List( requestHandler.getRecordIterator() );
		try {
			databaseProductName = requestHandler.getDatabaseName();
			databaseProductVersion = requestHandler.getProductVersion();
			int endIndex = databaseProductVersion.indexOf('.');
			if (endIndex == -1) endIndex = databaseProductVersion.length();
			dbMajorVersion = Integer.valueOf( databaseProductVersion.substring(0, endIndex ) ).intValue();
			int minorStartIndex = databaseProductVersion.indexOf('v') + 1;
			if (minorStartIndex == 0) minorStartIndex = databaseProductVersion.indexOf('.') + 1;
			int minorEndIndex = databaseProductVersion.indexOf(".", minorStartIndex);
			if (minorStartIndex > 0 && minorEndIndex > 0) {
				dbMinorVersion = Integer.valueOf( databaseProductVersion.substring(minorStartIndex, minorEndIndex ) ).intValue();
			}
			//databaseNames = iterator2List( requestHandler.getRecordIterator() );
		} catch (Exception e) {
			logger.warning("Unable to parse metadata: " + e.getMessage());
			e.printStackTrace();
		} finally {
			requestHandler.closeRequest();
		}
	}

//	private List iterator2List(Iterator it) {
//		List result = new LinkedList();
//		while( it.hasNext() ) result.add( it.next() );
//		return result;
//	}

	public String getAnyTableName() throws SQLException {
		if (anyTableName == null) {
			ResultSet tableNames = getTables(null, null, null, new String[] {"TEXT"});
			tableNames.next();
			anyTableName = tableNames.getString("TABLE_NAME");
		}
		//remove database name if there is one
		if( anyTableName.indexOf(catalogSeparator) > -1 )
			anyTableName = anyTableName.substring(anyTableName.indexOf(catalogSeparator)+1);

		return anyTableName;
	}

	//--- These methods must be implemented ---




	public boolean supportsStoredProcedures() throws SQLException {
		return true;
	}

	public ResultSet getProcedures( String catalog, String schemaPattern, String procedureNamePattern ) throws SQLException {
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "getProcedures(" + catalog + ", " + schemaPattern + ", " + procedureNamePattern + ")");
		}
		FmXmlRequest handler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
            connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());  // connection.getXmlRequestHandler(); // FIX!!! Just create a new instance
		String dbName = schemaPattern;
		if( dbName == null ) dbName = catalog;
		if( dbName == null ) dbName = connection.getCatalog();
		try {
			Iterator scriptIterator;
			if( dbName == null ) {
				logger.warning( "Cannot read stored procedures unless a database name is specified." );
				scriptIterator = Collections.EMPTY_LIST.iterator();
			} else {
				handler.doRequest("-db=" + dbName +"&-scriptnames"); //fixed hard-coded database columnName -bje
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINE, "Script count: " + handler.getFoundCount() );
				}
				scriptIterator = handler.getRecordIterator();
			}

			FmFieldList scriptInfo = new FmFieldList();
			FmTable storedProcDummyTable = new FmTable("fmp_jdbc_stored_procedures");
			scriptInfo.add( new FmField(storedProcDummyTable, "PROCEDURE_CAT", null, FmFieldType.TEXT, true ) );
			scriptInfo.add( new FmField(storedProcDummyTable, "PROCEDURE_SCHEM", null, FmFieldType.TEXT, true ) );
			scriptInfo.add( new FmField(storedProcDummyTable, "PROCEDURE_NAME",null, FmFieldType.TEXT, false ) );
			scriptInfo.add( new FmField(storedProcDummyTable, "RESERVED1",null, FmFieldType.NUMBER, true ) );
			scriptInfo.add( new FmField(storedProcDummyTable, "RESERVED2",null, FmFieldType.NUMBER, true ) );
			scriptInfo.add( new FmField(storedProcDummyTable, "RESERVED3",null, FmFieldType.NUMBER, true ) );
			scriptInfo.add( new FmField(storedProcDummyTable, "REMARKS",null, FmFieldType.TEXT, false ) );
			scriptInfo.add( new FmField(storedProcDummyTable, "PROCEDURE_TYPE",null, FmFieldType.NUMBER, false ) );

			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, String.valueOf(scriptInfo));
			}

			List scripts = new LinkedList();
			FmRecord scriptObject;
			while( scriptIterator.hasNext() ) {
				FmRecord scriptRecord = (FmRecord)scriptIterator.next();
				scriptObject = new FmRecord( scriptInfo, null, null );
				scriptObject.setRawValue( (String)scriptRecord.getValue(0), 2 );
				scriptObject.setRawValue( "" + DatabaseMetaData.procedureNoResult, 7 );
				scripts.add( scriptObject );
				if (logger.isLoggable(Level.FINER)) {
					logger.log(Level.FINER, String.valueOf(scriptObject));
				}
			}
			return new FmResultSet( scripts.iterator(), scriptInfo, connection );
		} catch (IOException e) {
			SQLException sqle = new SQLException(e.toString());
			sqle.initCause(e);
			throw sqle;
		} finally {
			handler.closeRequest(); // The parsing thread should take care of this... but just in case it's taking too long
    }
	}

	public ResultSet getProcedureColumns( String s, String s1, String s2, String s3 ) throws SQLException {
		return new FmResultSet( null, null, connection );
	}

	public ResultSet getTables( String catalog, String schemaPattern, String tableNamePattern, String[] fieldTypes ) throws SQLException {
		if (logger.isLoggable(Level.FINE)) {
			List fieldTypeList = Arrays.asList(fieldTypes);
			logger.log(Level.FINE, "getTables(" + catalog + ", " + schemaPattern + ", " + tableNamePattern + ", " + fieldTypeList + ")");
		}
		FmXmlRequest request = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
            connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
		String postArgs;
		String tableName;
		String databaseName;
		int mark;
		List databaseNames = new LinkedList();
		if( catalog == null ) catalog = connection.getCatalog();
		if( catalog != null ) {
			databaseNames.add( catalog );
		} else {
			postArgs = "-dbnames";
			try {
				request.doRequest( postArgs );
				for( Iterator it = request.getRecordIterator(); it.hasNext(); ) {
					databaseName = ((FmRecord)it.next()).getString(0);
					mark = databaseName.toLowerCase().indexOf(".fp");
					if( mark != -1 ) databaseName = databaseName.substring(0, mark);
					databaseNames.add( databaseName );
				}
			} catch (IOException e) {
				SQLException sqlException = new SQLException( e.toString() );
				sqlException.initCause(e);
				throw sqlException;
			} finally {
				request.closeRequest(); // The parsing thread should take care of this, but just in case it's taking too long
			}
		}
		FmFieldList tableFormat = new FmFieldList();
		FmTable dummyTable = new FmTable("fmp_jdbc_table_data");
		tableFormat.add( new FmField(dummyTable, "TABLE_CAT",null, FmFieldType.TEXT, true) ); //Return dbNames here, maybe?
		tableFormat.add( new FmField(dummyTable, "TABLE_SCHEM",null, FmFieldType.TEXT, true) ); //Return layout names here, maybe?
		tableFormat.add( new FmField(dummyTable, "TABLE_NAME",null, FmFieldType.TEXT, false) );
		tableFormat.add( new FmField(dummyTable, "TABLE_TYPE",null, FmFieldType.TEXT, false) );
		tableFormat.add( new FmField(dummyTable, "TABLE_REMARKS",null, FmFieldType.TEXT, false) );
		tableFormat.add( new FmField(dummyTable, "TYPE_CAT",null, FmFieldType.TEXT, true) );
		tableFormat.add( new FmField(dummyTable, "TYPE_SCHEME", null, FmFieldType.TEXT, true) );
		tableFormat.add( new FmField(dummyTable, "TYPE_NAME", null, FmFieldType.TEXT, true) );
		tableFormat.add( new FmField(dummyTable, "SELF_REFERENCING_COL_NAME", null, FmFieldType.TEXT, true) );
		tableFormat.add( new FmField(dummyTable, "REF_GENERATION",null, FmFieldType.TEXT, true) );
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, String.valueOf(tableFormat));
		}
		List tables = new LinkedList();
		for( Iterator dbIterator=databaseNames.iterator(); dbIterator.hasNext(); ) {
			databaseName = (String)dbIterator.next();
			postArgs = "-db=" + databaseName + "&-layoutnames"; //fixed hard-coded test value -bje
			try {
				request.doRequest( postArgs );
				for( Iterator it = request.getRecordIterator(); it.hasNext(); ) {
					FmRecord rawRecord = (FmRecord)it.next();
					FmRecord processedRecord = new FmRecord( tableFormat, null, null );
					tableName = rawRecord.getRawValue(0);
					mark = tableName.toLowerCase().indexOf(".fp");
					if( mark != -1 ) tableName = tableName.substring(0, mark);

					//FIX!!! Just here for experimentation
					//tableName = "dbName." + tableName + ".someLayoutName";
					//processedRecord.setRawValue( "catalogName", 0);
					//processedRecord.setRawValue( "schemaName", 1);

					/*
					It seems that for FM6, we should use -dbnames to get the names of all the open databases, and then we
					iterate through each databasename and use -layoutnames to get the names of all the layouts for each
					database. We store the database name as the table name, and the layout name as the schema name.
					Will this cause a problem, since we'll have duplication on the table names (if the client app
					doesn't consider the schema for uniqueness?) The nice thing about that is that things that don't know
					about schemas will ask us for the databasename, which won't be optimized, but will still work. If the user
					specifies an xxx.yyy syntax in the SQL query, we would treat that as the databasename / layout name.

					For FM7, we would treat the database name as the catalog. The layout names would be the jdbc tables, and
					the fm tables would be the schema (I think?). If everything is stored in a single database, we can always
					get the database name from the connection's catalog, and if the catalog is null, then we would assume that
					the database name is the same as the fm table name, which we can get from the schema. If the user specified
					an xxx.yyy syntax in the SQL query, we would treat that as the databasename / layout name.
					*/

					processedRecord.setRawValue( "Catalog name goes here", 0); //FIX!!! Temporary for testing... does this get used anywhere?
					if( getCatalogSeparator() != null && getCatalogSeparator() != "." ) {
						processedRecord.setRawValue( databaseName + getCatalogSeparator() + tableName, 2 );
					} else {
						processedRecord.setRawValue( databaseName, 1);
						processedRecord.setRawValue( tableName, 2 );
					}
					processedRecord.setRawValue( "TABLE", 3 );
					tables.add( processedRecord );
					if (logger.isLoggable(Level.FINER)) {
						logger.log(Level.FINER, String.valueOf(processedRecord ));
					}
				}
			} catch( FmXmlRequest.HttpAuthenticationException e) {
				//Ignore this database, we can't get to it with our username and password
			} catch (IOException e) {
				SQLException sqle = new SQLException(e.toString());
				sqle.initCause(e);
				throw sqle;
			} finally {
				request.closeRequest(); // the resultSet/parsing thread should take care of this, but just in case it's taking too long
			}
		}
		ResultSet result = new FmResultSet( tables.iterator(), tableFormat, connection );
		return result;
	}

	/**
	 *
	 * @param catalog
	 * @param schemaPattern
	 * @param tableNamePattern
	 * @param columnNamePattern
	 * @return
	 * @throws SQLException
	 */
	public ResultSet getColumns( String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern ) throws SQLException {
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "getColumns(" + catalog + ", " + schemaPattern + ", " + tableNamePattern + ", " + columnNamePattern + ")");
		}

//		FmResultSetRequest handler = null;
		FmRequest handler;
		if( connection.getFmVersion() >= 7 ) {
			try {
				handler = new FmResultSetRequest(connection.getProtocol(), connection.getHost(), "/fmi/xml/fmresultset.xml",
				                                 connection.getPort(), connection.getUsername(), connection.getPassword());
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		} else {
			handler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
            connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
		}
		FmFieldList rawFields;
		try {
			String dbName;
			if( tableNamePattern != null && tableNamePattern.indexOf( getCatalogSeparator() ) >= 0 ) {
				int mark = tableNamePattern.indexOf(getCatalogSeparator());
				dbName = tableNamePattern.substring(0, mark);
				tableNamePattern = tableNamePattern.substring( mark+1 );
			} else {
				dbName = schemaPattern;
				if( dbName == null ) dbName = catalog;
				if( dbName == null ) dbName = connection.getCatalog();
			}
			//FIX!! What do we do if it's still null?
			String postArgs = "-db=" + dbName + "&-lay=" + tableNamePattern + "&-max=0&-findany";
			handler.doRequest(postArgs);
			rawFields = handler.getFieldDefinitions();
		} catch (IOException e) {
			SQLException sqle = new SQLException(e.toString());
			sqle.initCause(e);
			throw sqle;
		} finally {
			handler.closeRequest();
		}

		FmFieldList fields = new FmFieldList();
		FmTable dummyTable = new FmTable("Field definitions");
		fields.add( new FmField(dummyTable, "TABLE_CAT", null, FmFieldType.TEXT, true) ); //0
		fields.add( new FmField(dummyTable, "TABLE_SCHEM", null, FmFieldType.TEXT, true) ); //1
		fields.add( new FmField(dummyTable, "TABLE_NAME", null, FmFieldType.TEXT, false) ); //2
		fields.add( new FmField(dummyTable, "COLUMN_NAME", null, FmFieldType.TEXT, false) ); //3
		fields.add( new FmField(dummyTable, "DATA_TYPE", null, FmFieldType.NUMBER, false) ); //4
		fields.add( new FmField(dummyTable, "TYPE_NAME", null, FmFieldType.TEXT, false) ); //5
		fields.add( new FmField(dummyTable, "COLUMN_SIZE", null, FmFieldType.NUMBER, false) ); //6
		fields.add( new FmField(dummyTable, "BUFFER_LENGTH", null, FmFieldType.NUMBER, true) ); //7
		fields.add( new FmField(dummyTable, "DECIMAL_DIGITS", null, FmFieldType.NUMBER, false) ); //8
		fields.add( new FmField(dummyTable, "NUM_PREC_RADIX", null, FmFieldType.NUMBER, false) ); //9
		fields.add( new FmField(dummyTable, "NULLABLE", null, FmFieldType.NUMBER, false) ); //10
		fields.add( new FmField(dummyTable, "REMARKS", null, FmFieldType.TEXT, true) ); //11
		fields.add( new FmField(dummyTable, "COLUMN_DEF", null, FmFieldType.TEXT, true) ); //12
		fields.add( new FmField(dummyTable, "SQL_DATA_TYPE", null, FmFieldType.NUMBER, true) ); //13
		fields.add( new FmField(dummyTable, "SQL_DATETIME_SUB", null, FmFieldType.NUMBER, true) ); //14
		fields.add( new FmField(dummyTable, "CHAR_OCTET_LENGTH", null, FmFieldType.NUMBER, false) ); //15
		fields.add( new FmField(dummyTable, "ORDINAL_POSITION", null, FmFieldType.NUMBER, false) ); //16
		fields.add( new FmField(dummyTable, "IS_NULLABLE", null, FmFieldType.TEXT, false) ); //17
		fields.add( new FmField(dummyTable, "SCOPE_CATLOG", null, FmFieldType.TEXT, true) ); //18
		fields.add( new FmField(dummyTable, "SCOPE_SCHEMA", null, FmFieldType.TEXT, true) ); //19
		fields.add( new FmField(dummyTable, "SCOPE_TABLE", null, FmFieldType.TEXT, true) ); //20
		fields.add( new FmField(dummyTable, "SOURCE_DATA_TYPE", null, FmFieldType.NUMBER, true) ); //21

		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, String.valueOf(fields ));
		}


		List columns = new LinkedList();
		fieldCount = rawFields.getFields().size();
		FmField eachField;
		FmRecord fieldRecord;
		for( int n=0; n<fieldCount; n++ ) {
			eachField = rawFields.get(n);
			fieldRecord = new FmRecord( fields, null, null );
			fieldRecord.setRawValue( tableNamePattern, 2 ); //FIX! Is this the right param to pass in? --jsb
			fieldRecord.setRawValue( eachField.getAlias(), 3 );
            try {
                eachField.getType().getSqlDataType();
            } catch(NullPointerException e) {
                System.out.println(e);
            }
			fieldRecord.setRawValue( "" + eachField.getType().getSqlDataType(), 4 );
			fieldRecord.setRawValue( eachField.getType().getTypeName(), 5 );
			fieldRecord.setRawValue( "" + eachField.getType().getPrecision(), 6 );
			fieldRecord.setRawValue( "" + 17, 8 ); //FIX!! Wild-ass guess, really don't know what to put here --jsb
			fieldRecord.setRawValue( "" + 10, 9 );
			fieldRecord.setRawValue( "" + (eachField.isNullable() ?  DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls), 10 );
			fieldRecord.setRawValue(eachField.isReadOnly() ? "readonly" : "", 11);
			fieldRecord.setRawValue( "" + eachField.getType().getPrecision(), 15 ); //FIX! What's the difference between this and COLUMN_SIZE?
			fieldRecord.setRawValue( "" + n+1, 16 );
			fieldRecord.setRawValue( eachField.isNullable() ? "YES" : "NO", 17 );
			columns.add( fieldRecord );
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, String.valueOf(fieldRecord ));
			}
		}
		return new FmResultSet( columns.iterator(), fields, connection );
	}

	public ResultSet getVersionColumns( String s, String s1, String s2 ) throws SQLException {
		throw new AbstractMethodError( "getVersionColumns is not implemented yet." ); //FIX!!! return modid column here
	}

	public ResultSet getPrimaryKeys( String catalog, String schema, String table ) throws SQLException {
		return new FmResultSet(null, null, connection);
	}

	public ResultSet getImportedKeys( String s, String s1, String s2 ) throws SQLException {
		return new FmResultSet(null, null, connection);
	}

	public ResultSet getTypeInfo() throws SQLException {
		List typesList = new LinkedList();
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, String.valueOf( FmFieldType.resultSetFormat));
		}
		Object eachType;
		for( Iterator it = FmFieldType.typesByName.values().iterator(); it.hasNext(); ) {
			eachType = ((FmFieldType)it.next()).getInResultSetFormat();
			if (logger.isLoggable(Level.FINER)) {
				logger.log(Level.FINER, String.valueOf(eachType ));
			}
			typesList.add( eachType );
		}
		return new FmResultSet( typesList.iterator(), FmFieldType.resultSetFormat, connection );
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public boolean supportsGetGeneratedKeys() throws SQLException {
		return true;
	}

	public String getDatabaseProductName() throws SQLException {
		return databaseProductName;
	}

	public int getDatabaseMajorVersion() throws SQLException {
		return dbMajorVersion;
	}

	public int getDatabaseMinorVersion() throws SQLException {
		return dbMinorVersion;
	}

	public String getStringFunctions() throws SQLException {
		return "";
	}

	public String getSystemFunctions() throws SQLException {
		return "";
	}

	public String getTimeDateFunctions() throws SQLException {
		return "";
	}

	public String getProcedureTerm() throws SQLException {
		return "Script";
	}

	public int getDefaultTransactionIsolation() throws SQLException {
		return Connection.TRANSACTION_NONE;
	}

	public boolean supportsTransactions() throws SQLException {
		return false;
	}

	public boolean supportsTransactionIsolationLevel( int i ) throws SQLException {
		return i == Connection.TRANSACTION_NONE;
	}


	public String getURL() throws SQLException {
		return connection.getUrl();
	}

	public String getUserName() throws SQLException {
		return connection.getUsername();
	}

	public String getDatabaseProductVersion() throws SQLException {
		return databaseProductVersion;
	}

	public String getDriverName() throws SQLException {
		return "fmp360_jdbc";
	}

	public String getDriverVersion() throws SQLException {
		return "1.0";
	}

	public int getDriverMajorVersion() {
		return 1;
	}

	public int getDriverMinorVersion() {
		return 0;
	}

	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		return false; //FIX!! Maybe we do support this? Find out what this means
	}

	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		return false;
	}

	public boolean supportsANSI92FullSQL() throws SQLException {
		return false;
	}

	public String getIdentifierQuoteString() throws SQLException {
		return "\"";
	}

	public boolean supportsNonNullableColumns() throws SQLException {
		return true;
	}

	public ResultSet getSchemas() throws SQLException {
		throw new AbstractMethodError( "getSchemas is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getCatalogs() throws SQLException {
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "getCatalogs()");
		}

		FmXmlRequest request = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
            connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
		String postArgs;
		List databases = new LinkedList();
		postArgs = "-dbnames";

		FmFieldList format = new FmFieldList();
		FmTable dummyTable = new FmTable("fmp_jdbc_table_data");

		format.add(new FmField(dummyTable, "TABLE_CAT",null, FmFieldType.TEXT, true));

		try {
			request.doRequest(postArgs);

			for (Iterator it = request.getRecordIterator(); it.hasNext();) {
				FmRecord rawRecord = (FmRecord) it.next();
				String databaseName = rawRecord.getRawValue(0);
				FmRecord processRecord = new FmRecord(format, null, null);
				processRecord.setRawValue(databaseName, 0);
				databases.add(processRecord);
			}
		}
		catch (IOException e) {
			SQLException sqlException = new SQLException(e.toString());
			sqlException.initCause(e);
			throw sqlException;
		}
		finally {
			request.closeRequest(); // the parsing thread should take care of this... but just in case it's taking too long
		}

		ResultSet result = new FmResultSet(databases.iterator(), format, connection );
		return result;
	}




	//--- These methods can be ignored ---

	public ResultSet getColumnPrivileges( String s, String s1, String s2, String s3 ) throws SQLException {
		throw new AbstractMethodError( "getColumnPrivileges is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean allProceduresAreCallable() throws SQLException {
		throw new AbstractMethodError( "allProceduresAreCallable is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean allTablesAreSelectable() throws SQLException {
		throw new AbstractMethodError( "allTablesAreSelectable is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isReadOnly() throws SQLException {
		throw new AbstractMethodError( "isReadOnly is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean nullsAreSortedHigh() throws SQLException {
		throw new AbstractMethodError( "nullsAreSortedHigh is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean nullsAreSortedLow() throws SQLException {
		throw new AbstractMethodError( "nullsAreSortedLow is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean nullsAreSortedAtStart() throws SQLException {
		throw new AbstractMethodError( "nullsAreSortedAtStart is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean nullsAreSortedAtEnd() throws SQLException {
		throw new AbstractMethodError( "nullsAreSortedAtEnd is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean usesLocalFiles() throws SQLException {
		throw new AbstractMethodError( "usesLocalFiles is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean usesLocalFilePerTable() throws SQLException {
		throw new AbstractMethodError( "usesLocalFilePerTable is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		throw new AbstractMethodError( "supportsMixedCaseIdentifiers is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean storesUpperCaseIdentifiers() throws SQLException {
		throw new AbstractMethodError( "storesUpperCaseIdentifiers is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean storesLowerCaseIdentifiers() throws SQLException {
		throw new AbstractMethodError( "storesLowerCaseIdentifiers is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean storesMixedCaseIdentifiers() throws SQLException {
		throw new AbstractMethodError( "storesMixedCaseIdentifiers is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		throw new AbstractMethodError( "supportsMixedCaseQuotedIdentifiers is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		throw new AbstractMethodError( "storesUpperCaseQuotedIdentifiers is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		throw new AbstractMethodError( "storesLowerCaseQuotedIdentifiers is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		throw new AbstractMethodError( "storesMixedCaseQuotedIdentifiers is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getSQLKeywords() throws SQLException {
		throw new AbstractMethodError( "getSQLKeywords is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getNumericFunctions() throws SQLException {
		throw new AbstractMethodError( "getNumericFunctions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getSearchStringEscape() throws SQLException {
		throw new AbstractMethodError( "getSearchStringEscape is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getExtraNameCharacters() throws SQLException {
		throw new AbstractMethodError( "getExtraNameCharacters is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		throw new AbstractMethodError( "supportsAlterTableWithAddColumn is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		throw new AbstractMethodError( "supportsAlterTableWithDropColumn is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsColumnAliasing() throws SQLException {
		throw new AbstractMethodError( "supportsColumnAliasing is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean nullPlusNonNullIsNull() throws SQLException {
		throw new AbstractMethodError( "nullPlusNonNullIsNull is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsConvert() throws SQLException {
		throw new AbstractMethodError( "supportsConvert is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsConvert( int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "supportsConvert is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsTableCorrelationNames() throws SQLException {
		throw new AbstractMethodError( "supportsTableCorrelationNames is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsDifferentTableCorrelationNames() throws SQLException {
		throw new AbstractMethodError( "supportsDifferentTableCorrelationNames is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsExpressionsInOrderBy() throws SQLException {
		throw new AbstractMethodError( "supportsExpressionsInOrderBy is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsOrderByUnrelated() throws SQLException {
		throw new AbstractMethodError( "supportsOrderByUnrelated is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsGroupBy() throws SQLException {
		throw new AbstractMethodError( "supportsGroupBy is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsGroupByUnrelated() throws SQLException {
		throw new AbstractMethodError( "supportsGroupByUnrelated is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsGroupByBeyondSelect() throws SQLException {
		throw new AbstractMethodError( "supportsGroupByBeyondSelect is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsLikeEscapeClause() throws SQLException {
		throw new AbstractMethodError( "supportsLikeEscapeClause is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsMultipleResultSets() throws SQLException {
		throw new AbstractMethodError( "supportsMultipleResultSets is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsMultipleTransactions() throws SQLException {
		throw new AbstractMethodError( "supportsMultipleTransactions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsMinimumSQLGrammar() throws SQLException {
		throw new AbstractMethodError( "supportsMinimumSQLGrammar is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsCoreSQLGrammar() throws SQLException {
		throw new AbstractMethodError( "supportsCoreSQLGrammar is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsExtendedSQLGrammar() throws SQLException {
		throw new AbstractMethodError( "supportsExtendedSQLGrammar is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		throw new AbstractMethodError( "supportsIntegrityEnhancementFacility is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsOuterJoins() throws SQLException {
		throw new AbstractMethodError( "supportsOuterJoins is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsFullOuterJoins() throws SQLException {
		throw new AbstractMethodError( "supportsFullOuterJoins is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsLimitedOuterJoins() throws SQLException {
		throw new AbstractMethodError( "supportsLimitedOuterJoins is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getSchemaTerm() throws SQLException {
		throw new AbstractMethodError( "getSchemaTerm is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getCatalogTerm() throws SQLException {
		throw new AbstractMethodError( "getCatalogTerm is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isCatalogAtStart() throws SQLException {
		throw new AbstractMethodError( "isCatalogAtStart is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getCatalogSeparator() throws SQLException {
		return catalogSeparator;
	}

	public void setCatalogSeparator(String o) {
		catalogSeparator = o;
	}

	public boolean supportsSchemasInDataManipulation() throws SQLException {
		throw new AbstractMethodError( "supportsSchemasInDataManipulation is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		throw new AbstractMethodError( "supportsSchemasInProcedureCalls is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		throw new AbstractMethodError( "supportsSchemasInTableDefinitions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		throw new AbstractMethodError( "supportsSchemasInIndexDefinitions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		throw new AbstractMethodError( "supportsSchemasInPrivilegeDefinitions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		throw new AbstractMethodError( "supportsCatalogsInDataManipulation is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		throw new AbstractMethodError( "supportsCatalogsInProcedureCalls is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		throw new AbstractMethodError( "supportsCatalogsInTableDefinitions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		throw new AbstractMethodError( "supportsCatalogsInIndexDefinitions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsCatalogsInPrivilegeDefinitions() throws SQLException {
		throw new AbstractMethodError( "supportsCatalogsInPrivilegeDefinitions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsPositionedDelete() throws SQLException {
		throw new AbstractMethodError( "supportsPositionedDelete is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsPositionedUpdate() throws SQLException {
		throw new AbstractMethodError( "supportsPositionedUpdate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSelectForUpdate() throws SQLException {
		throw new AbstractMethodError( "supportsSelectForUpdate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSubqueriesInComparisons() throws SQLException {
		throw new AbstractMethodError( "supportsSubqueriesInComparisons is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSubqueriesInExists() throws SQLException {
		throw new AbstractMethodError( "supportsSubqueriesInExists is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSubqueriesInIns() throws SQLException {
		throw new AbstractMethodError( "supportsSubqueriesInIns is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		throw new AbstractMethodError( "supportsSubqueriesInQuantifieds is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsCorrelatedSubqueries() throws SQLException {
		throw new AbstractMethodError( "supportsCorrelatedSubqueries is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsUnion() throws SQLException {
		throw new AbstractMethodError( "supportsUnion is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsUnionAll() throws SQLException {
		throw new AbstractMethodError( "supportsUnionAll is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		throw new AbstractMethodError( "supportsOpenCursorsAcrossCommit is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		throw new AbstractMethodError( "supportsOpenCursorsAcrossRollback is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		throw new AbstractMethodError( "supportsOpenStatementsAcrossCommit is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		throw new AbstractMethodError( "supportsOpenStatementsAcrossRollback is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxBinaryLiteralLength() throws SQLException {
		throw new AbstractMethodError( "getMaxBinaryLiteralLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxCharLiteralLength() throws SQLException {
		throw new AbstractMethodError( "getMaxCharLiteralLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxColumnNameLength() throws SQLException {
		throw new AbstractMethodError( "getMaxColumnNameLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxColumnsInGroupBy() throws SQLException {
		throw new AbstractMethodError( "getMaxColumnsInGroupBy is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxColumnsInIndex() throws SQLException {
		throw new AbstractMethodError( "getMaxColumnsInIndex is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxColumnsInOrderBy() throws SQLException {
		throw new AbstractMethodError( "getMaxColumnsInOrderBy is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxColumnsInSelect() throws SQLException {
		throw new AbstractMethodError( "getMaxColumnsInSelect is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxColumnsInTable() throws SQLException {
		throw new AbstractMethodError( "getMaxColumnsInTable is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxConnections() throws SQLException {
		throw new AbstractMethodError( "getMaxConnections is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxCursorNameLength() throws SQLException {
		throw new AbstractMethodError( "getMaxCursorNameLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxIndexLength() throws SQLException {
		throw new AbstractMethodError( "getMaxIndexLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxSchemaNameLength() throws SQLException {
		throw new AbstractMethodError( "getMaxSchemaNameLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxProcedureNameLength() throws SQLException {
		throw new AbstractMethodError( "getMaxProcedureNameLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxCatalogNameLength() throws SQLException {
		throw new AbstractMethodError( "getMaxCatalogNameLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxRowSize() throws SQLException {
		throw new AbstractMethodError( "getMaxRowSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		throw new AbstractMethodError( "doesMaxRowSizeIncludeBlobs is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxStatementLength() throws SQLException {
		throw new AbstractMethodError( "getMaxStatementLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxStatements() throws SQLException {
		throw new AbstractMethodError( "getMaxStatements is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxTableNameLength() throws SQLException {
		throw new AbstractMethodError( "getMaxTableNameLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxTablesInSelect() throws SQLException {
		throw new AbstractMethodError( "getMaxTablesInSelect is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxUserNameLength() throws SQLException {
		throw new AbstractMethodError( "getMaxUserNameLength is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsDataDefinitionAndDataManipulationTransactions() throws SQLException {
		throw new AbstractMethodError( "supportsDataDefinitionAndDataManipulationTransactions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsDataManipulationTransactionsOnly() throws SQLException {
		throw new AbstractMethodError( "supportsDataManipulationTransactionsOnly is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		throw new AbstractMethodError( "dataDefinitionCausesTransactionCommit is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		throw new AbstractMethodError( "dataDefinitionIgnoredInTransactions is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getTableTypes() throws SQLException {
		throw new AbstractMethodError( "getTableTypes is not implemented yet." ); //FIX!!! Broken placeholder
	}
	public ResultSet getTablePrivileges( String s, String s1, String s2 ) throws SQLException {
		throw new AbstractMethodError( "getTablePrivileges is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getBestRowIdentifier( String s, String s1, String s2, int i, boolean b ) throws SQLException {
		throw new AbstractMethodError( "getBestRowIdentifier is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getExportedKeys( String s, String s1, String s2 ) throws SQLException {
		throw new AbstractMethodError( "getExportedKeys is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getCrossReference( String s, String s1, String s2, String s3, String s4, String s5 ) throws SQLException {
		throw new AbstractMethodError( "getCrossReference is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getIndexInfo( String s, String s1, String s2, boolean b, boolean b1 ) throws SQLException {
		throw new AbstractMethodError( "getIndexInfo is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsResultSetType( int i ) throws SQLException {
		throw new AbstractMethodError( "supportsResultSetType is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsResultSetConcurrency( int i, int i1 ) throws SQLException {
		throw new AbstractMethodError( "supportsResultSetConcurrency is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean ownUpdatesAreVisible( int i ) throws SQLException {
		throw new AbstractMethodError( "ownUpdatesAreVisible is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean ownDeletesAreVisible( int i ) throws SQLException {
		throw new AbstractMethodError( "ownDeletesAreVisible is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean ownInsertsAreVisible( int i ) throws SQLException {
		throw new AbstractMethodError( "ownInsertsAreVisible is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean othersUpdatesAreVisible( int i ) throws SQLException {
		throw new AbstractMethodError( "othersUpdatesAreVisible is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean othersDeletesAreVisible( int i ) throws SQLException {
		throw new AbstractMethodError( "othersDeletesAreVisible is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean othersInsertsAreVisible( int i ) throws SQLException {
		throw new AbstractMethodError( "othersInsertsAreVisible is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean updatesAreDetected( int i ) throws SQLException {
		throw new AbstractMethodError( "updatesAreDetected is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean deletesAreDetected( int i ) throws SQLException {
		throw new AbstractMethodError( "deletesAreDetected is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean insertsAreDetected( int i ) throws SQLException {
		throw new AbstractMethodError( "insertsAreDetected is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsBatchUpdates() throws SQLException {
		throw new AbstractMethodError( "supportsBatchUpdates is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getUDTs( String s, String s1, String s2, int[] ints ) throws SQLException {
		throw new AbstractMethodError( "getUDTs is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSavepoints() throws SQLException {
		throw new AbstractMethodError( "supportsSavepoints is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsNamedParameters() throws SQLException {
		throw new AbstractMethodError( "supportsNamedParameters is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsMultipleOpenResults() throws SQLException {
		throw new AbstractMethodError( "supportsMultipleOpenResults is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getSuperTypes( String s, String s1, String s2 ) throws SQLException {
		throw new AbstractMethodError( "getSuperTypes is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getSuperTables( String s, String s1, String s2 ) throws SQLException {
		throw new AbstractMethodError( "getSuperTables is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getAttributes( String s, String s1, String s2, String s3 ) throws SQLException {
		throw new AbstractMethodError( "getAttributes is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsResultSetHoldability( int i ) throws SQLException {
		throw new AbstractMethodError( "supportsResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getResultSetHoldability() throws SQLException {
		throw new AbstractMethodError( "getResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getJDBCMajorVersion() throws SQLException {
		throw new AbstractMethodError( "getJDBCMajorVersion is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getJDBCMinorVersion() throws SQLException {
		throw new AbstractMethodError( "getJDBCMinorVersion is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getSQLStateType() throws SQLException {
		throw new AbstractMethodError( "getSQLStateType is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean locatorsUpdateCopy() throws SQLException {
		throw new AbstractMethodError( "locatorsUpdateCopy is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsStatementPooling() throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}
}
