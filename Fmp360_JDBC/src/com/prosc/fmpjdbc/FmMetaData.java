package com.prosc.fmpjdbc;

import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.MalformedURLException;
import java.net.URLEncoder;

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
	private static final Logger log = Logger.getLogger( FmMetaData.class.getName() );

	private final FmConnection connection;

	private boolean didReadDbInfo = false;
	//These next fields are read on demand
	private String databaseProductName;
	private String databaseProductVersion;
	private int dbMajorVersion;
	private int dbMinorVersion;

	//private List databaseNames;

	private static AtomicInteger counter = new AtomicInteger(0);

	private String anyTableName;
	private int fieldCount;
	private String catalogSeparator;
	private Logger logger = Logger.getLogger( FmMetaData.class.getName() );

	private String lastTable;
	private String lastFile;
	private List<FmRecord> lastColumns;
	private FmFieldList lastRawFields;
	private Map<String,Set<String>> writeableFields = new HashMap<String, Set<String>>();
	private Map<String,Set<String>> readableFields = new HashMap<String, Set<String>>();
	private Map<String,String> tableOccurrenceNames = new HashMap<String, String>();

	public FmMetaData(FmConnection connection) throws IOException, FileMakerException {
		this.connection = connection;
		//databaseNames = iterator2List( requestHandler.getRecordIterator() );
	}

	private void readDbInfo() {
		if( ! didReadDbInfo ) {
			FmXmlRequest requestHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(), connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
			int callCount = counter.getAndIncrement();
			log.fine( "FmMetaData call tracker: Created -dbnames request " + callCount );
			try {
				logger.log( Level.FINEST, "Creating FmMetaData");
				requestHandler.doRequest("-dbnames"); //Removed the max=0 part of the request, because it had no effect in FMS 11 and causes an error in other versions. --jsb
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
				didReadDbInfo = true;
			} catch (Exception e) {
				log.log( Level.WARNING, "Unable to parse metadata", e );
			} finally {
				requestHandler.closeRequest();
				log.fine( "FmMetaData call tracker: Closed -dbnames request " + callCount );
			}
		}
	}

//	private List iterator2List(Iterator it) {
//		List result = new LinkedList();
//		while( it.hasNext() ) result.add( it.next() );
//		return result;
//	}

	public void testUsernamePassword() throws SQLException {
		// if no exception gets thrown, then we're ok
		ResultSet rs = getTables(connection.getCatalog(), null, null, true);
		rs.close();
	}


	public String getAnyTableName() throws SQLException {
		if (anyTableName == null) {
			ResultSet tableNames = getTables(null, null, null, new String[] {"TEXT"});
			tableNames.next();
			anyTableName = tableNames.getString("TABLE_NAME");
		}
		//remove database name if there is one
		if(catalogSeparator != null && anyTableName.indexOf(catalogSeparator) != -1)
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
			Iterator<FmRecord> scriptIterator;
			if( dbName == null ) {
				logger.warning( "Cannot read stored procedures unless a database name is specified." );
				scriptIterator = Collections.EMPTY_LIST.iterator();
			} else {
				String encodedDbName = URLEncoder.encode( dbName, "utf-8" );
				handler.doRequest("-db=" + encodedDbName +"&-scriptnames"); //fixed hard-coded database columnName -bje
				//if (logger.isLoggable(Level.FINEST)) {
				//	logger.log(Level.FINEST, "Script count: " + handler.getFoundCount() );
				//}
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

			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, String.valueOf(scriptInfo));
			}

			List<FmRecord> scripts = new LinkedList<FmRecord>();
			FmRecord scriptObject;
			while( scriptIterator.hasNext() ) {
				FmRecord scriptRecord = scriptIterator.next();
				String scriptName = String.valueOf( scriptRecord.getString( 0, 1 ) );
				if( procedureNamePattern != null && !procedureNamePattern.equalsIgnoreCase( scriptName ) ) {
					continue; //Script name doesn't match requested pattern
				}
				scriptObject = new FmRecord( scriptInfo, null, null );
				scriptObject.addRawValue( scriptRecord.getString( 0, 1 ), 2 );
				scriptObject.addRawValue( "" + DatabaseMetaData.procedureNoResult, 7 );
				scripts.add( scriptObject );
				if (logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, String.valueOf(scriptObject));
				}
			}
			return new FmResultSet( scripts.iterator(), scripts.size(), scriptInfo, connection );
		} catch (IOException e) {
			SQLException sqle = new SQLException(e.toString());
			sqle.initCause(e);
			throw sqle;
		} finally {
			handler.closeRequest(); // The parsing thread should take care of this... but just in case it's taking too long
		}
	}

	public ResultSet getProcedureColumns( String s, String s1, String s2, String s3 ) throws SQLException {
		return new FmResultSet( null, 0, null, connection );
	}

	public ResultSet getTables( String catalog, String schemaPattern, String tableNamePattern, String[] fieldTypes ) throws SQLException {
		return getTables(catalog, schemaPattern, tableNamePattern, false);
	}

	private ResultSet getTables( String catalog, String schemaPattern, String tableNamePattern, boolean testingConnection ) throws SQLException {
		//log.log( Level.FINE, "getTables stack trace (NOT AN ERROR)", new RuntimeException("Just a stack trace") );
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "getTables(" + catalog + ", " + schemaPattern + ", " + tableNamePattern + ")");
		}

		String postArgs;
		String layoutName;
		String databaseName;
		int mark;
		List<String> databaseNames = new LinkedList<String>();
		if( catalog == null ) catalog = connection.getCatalog();
		if( catalog != null ) {
			databaseNames.add( catalog );
		} else { //Should never need to do this if they have specified a database name in the URL
			postArgs = "-dbnames";
			FmXmlRequest request = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
					connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
			int callCount = counter.getAndIncrement();
			log.fine( "FmMetaData call tracker: Created -dbnames request " + callCount );
			try {
				request.doRequest( postArgs );
				for( Iterator it = request.getRecordIterator(); it.hasNext(); ) {
					databaseName = ((FmRecord)it.next()).getString(0, 1 );
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
				log.fine( "FmMetaData call tracker: Closed -dbnames request " + callCount );
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
		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, String.valueOf(tableFormat));
		}
		List<FmRecord> tables = new LinkedList<FmRecord>();
		for( Iterator<String> dbIterator=databaseNames.iterator(); dbIterator.hasNext(); ) {
			databaseName = dbIterator.next();
			String encodedDBName;
			try {
				encodedDBName = URLEncoder.encode( databaseName, "utf-8" );
			} catch( UnsupportedEncodingException e ) {
				throw new RuntimeException( e );
			}
			postArgs = "-db=" + encodedDBName + "&-layoutnames"; //fixed hard-coded test value -bje
			FmXmlRequest request = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
					connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
			int callCount = counter.getAndIncrement();
			log.fine( "FmMetaData call tracker: Created -layoutnames request " + callCount );
			try {
				request.doRequest( postArgs );
				for( Iterator<FmRecord> it = request.getRecordIterator(); it.hasNext(); ) {
					FmRecord rawRecord = it.next();
					layoutName = rawRecord.getString( 0, 1 );
					mark = layoutName.toLowerCase().indexOf(".fp");
					if( mark != -1 ) layoutName = layoutName.substring(0, mark);
					if (tableNamePattern != null && !tableNamePattern.equalsIgnoreCase(layoutName) && !tableNamePattern.equalsIgnoreCase(databaseName + "|" + layoutName)) {
						continue;
					}
					FmRecord processedRecord = new FmRecord( tableFormat, null, null );

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

					processedRecord.addRawValue( databaseName, 0 );
					if( getCatalogSeparator() != null && !getCatalogSeparator().equals( "." ) ) {
						processedRecord.addRawValue( databaseName + getCatalogSeparator() + layoutName, 2 );
					} else {
						//processedRecord.setRawValue( getTableOccurrenceForLayout( catalog, layoutName ), 1); //I skip this because it's quite slow
						processedRecord.addRawValue( layoutName, 2 );
					}
					processedRecord.addRawValue( "TABLE", 3 );
					tables.add( processedRecord );
					if (logger.isLoggable(Level.FINEST)) {
						logger.log(Level.FINEST, String.valueOf(processedRecord ));
					}
				}
			} catch( FmXmlRequest.HttpAuthenticationException e) {
				if (testingConnection) {
					// then i'm trying to see if i CAN access this db...
					SQLException sqle = new SQLException( e.getMessage() );
					sqle.initCause(e);
					throw sqle;
				} else {
					//Ignore this database, we can't get to it with our username and password
				}
			} catch (IOException e) {
				SQLException sqle = new SQLException(e.toString());
				sqle.initCause(e);
				throw sqle;
			} finally {
				request.closeRequest();
				log.fine( "FmMetaData call tracker: Closed -layoutnames request " + callCount );
			}
		}
		return new FmResultSet( tables.iterator(), tables.size(), tableFormat, connection );
	}

	/** FileMaker 'tables' are actually layout names. Sometimes, however, you need to find out the table occurrence name associated with a layout, and
	 * you can use this method to do that. The results are cached on a per-connection basis, so it is fine to call this method repeatedly for a given connection.
	 */
	public String getTableOccurrenceForLayout( String database, String layoutName ) throws FileMakerException {
		String lookupKey = database + "~" + layoutName;
		String result = tableOccurrenceNames.get( lookupKey );
		if( result == null ) {
			FmResultSetRequest request = new FmResultSetRequest( connection.getProtocol(), connection.getHost(), "/fmi/xml/fmresultset.xml", connection.getPort(), connection.getUsername(), connection.getPassword() );
			try {
				String postArgs = "-db=" + URLEncoder.encode( database, "utf-8" ) + "&-lay=" + URLEncoder.encode( layoutName, "utf-8" ) + "&-findany";
				request.doRequest( postArgs );
				result = request.getTableOccurrence();
				tableOccurrenceNames.put( lookupKey, result );
			} catch( IOException e ) {
				FileMakerException sqle = new FileMakerException( -1, e.getMessage(), request.getFullUrl(), connection.getUsername() );
				sqle.initCause( e );
				throw sqle;
			}
		}
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
	public ResultSet getColumns( @Nullable String catalog, @Nullable String schemaPattern, String tableNamePattern, @Nullable String columnNamePattern ) throws SQLException {
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, "getColumns(" + catalog + ", " + schemaPattern + ", " + tableNamePattern + ", " + columnNamePattern + ")");
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
		fields.add( new FmField(dummyTable, "SCOPE_CATALOG", null, FmFieldType.TEXT, true) ); //18
		fields.add( new FmField(dummyTable, "SCOPE_SCHEMA", null, FmFieldType.TEXT, true) ); //19
		fields.add( new FmField(dummyTable, "SCOPE_TABLE", null, FmFieldType.TEXT, true) ); //20
		fields.add( new FmField(dummyTable, "SOURCE_DATA_TYPE", null, FmFieldType.NUMBER, true) ); //21
		fields.add( new FmField(dummyTable, "IS_AUTOINCREMENT", null, FmFieldType.TEXT, true ) ); //22

		if (logger.isLoggable(Level.FINEST)) {
			logger.log(Level.FINEST, String.valueOf(fields ));
		}

		if( lastColumns == null || lastFile == null || !lastFile.equals( catalog ) || lastTable == null || !lastTable.equals( tableNamePattern ) ) {
//		FmResultSetRequest handler = null;
			FmRequest handler;
			if( connection.getFmVersion() >= 7 ) {
				handler = new FmResultSetRequest(connection.getProtocol(), connection.getHost(), "/fmi/xml/fmresultset.xml",
						connection.getPort(), connection.getUsername(), connection.getPassword());
			} else {
				handler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
						connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
			}
			try {
				String dbName;
				if( tableNamePattern != null && getCatalogSeparator() != null && tableNamePattern.contains( getCatalogSeparator() ) ) {
					int mark = tableNamePattern.indexOf(getCatalogSeparator());
					dbName = tableNamePattern.substring(0, mark);
					tableNamePattern = tableNamePattern.substring( mark+1 );
				} else {
					dbName = schemaPattern;
					if( dbName == null ) dbName = catalog;
					if( dbName == null ) dbName = connection.getCatalog();
				}
				//FIX!! What do we do if it's still null?
				String encodedDbName = URLEncoder.encode( dbName, "utf-8" );
				String encodedTableName = URLEncoder.encode( tableNamePattern, "utf-8" );
				String postArgs = "-db=" + encodedDbName + "&-lay=" + encodedTableName + "&-max=0&-findany";
				handler.doRequest(postArgs);
				lastRawFields = handler.getFieldDefinitions();
			} catch (IOException e) {
				SQLException sqle = new SQLException(e.toString());
				sqle.initCause(e);
				throw sqle;
			} finally {
				handler.closeRequest();
			}


			lastColumns = new LinkedList<FmRecord>();
			lastFile = catalog;
			lastTable = tableNamePattern;

			fieldCount = lastRawFields.size();
			FmField eachField;
			FmRecord fieldRecord;
			for( int n=0; n<fieldCount; n++ ) {
				eachField = lastRawFields.get( n );
				String fieldName = eachField.getAlias();
				if( columnNamePattern != null && ! columnNamePattern.equals( fieldName ) ) {
					continue;
				}
				fieldRecord = new FmRecord( fields, null, null );
				fieldRecord.addRawValue( tableNamePattern, 2 ); //FIX! Is this the right param to pass in? --jsb
				fieldRecord.addRawValue( fieldName, 3 );
				try {
					eachField.getType().getSqlDataType();
				} catch(NullPointerException e) {
					log.log( Level.SEVERE, "NPE while trying to get the SQL data type", e); //FIX! Brian wrote this code - are we supposed to do something here? Are we expecting this to fail? --jsb
				}
				fieldRecord.addRawValue( "" + eachField.getType().getSqlDataType(), 4 );
				fieldRecord.addRawValue( eachField.getType().getExternalTypeName(), 5 );
				fieldRecord.addRawValue( "" + eachField.getType().getPrecision(), 6 );
				fieldRecord.addRawValue( "" + 16, 8 ); //FIX!! Wild-ass guess, really don't know what to put here --jsb
				fieldRecord.addRawValue( "" + 10, 9 );
				fieldRecord.addRawValue( "" + ( eachField.isNullable() ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls ), 10 );
				StringBuilder comments = new StringBuilder( 32 );
				String delim = "";
				if( eachField.isReadOnly() ) {
					comments.append( "readonly" );
					delim = " ";
				}
				if( eachField.getMaxReps() > 1 ) {
					comments.append( delim + "maxRepetitions(" + eachField.getMaxReps() + ")" );
					delim = " ";
				}
				if( eachField.isGlobal() ) {
					comments.append( delim + "global" );
					delim = " ";
				}
				if( eachField.isCalculation() ) {
					comments.append( delim + "calculation" );
					delim = " ";
				}
				if( eachField.isSummary() ) {
					comments.append( delim + "summary" );
					delim = " ";
				}
				fieldRecord.addRawValue( comments.toString(), 11 );
				fieldRecord.addRawValue( "" + eachField.getType().getPrecision(), 15 ); //FIX! What's the difference between this and COLUMN_SIZE?
				fieldRecord.addRawValue( "" + n + 1, 16 );
				fieldRecord.addRawValue( eachField.isNullable() ? "YES" : "NO", 17 );
				fieldRecord.addRawValue( "", 22 ); //Can't tell whether a column is auto-incremented or not
				lastColumns.add( fieldRecord );
				if (logger.isLoggable(Level.FINEST)) {
					logger.log(Level.FINEST, String.valueOf(fieldRecord ));
				}
			}
		}
		return new FmResultSet( lastColumns.iterator(), lastColumns.size(), fields, connection );
	}

	public ResultSet getColumnPrivileges( String catalog, String schema, String table, String columnNamePattern ) throws SQLException {
		getColumns( catalog, schema, table,columnNamePattern ); //Do this first so that we know that internal caches are correct

		FmFieldList fields = new FmFieldList();
		FmTable dummyTable = new FmTable("Field definitions");
		fields.add( new FmField(dummyTable, "TABLE_CAT", null, FmFieldType.TEXT, true) ); //0
		fields.add( new FmField(dummyTable, "TABLE_SCHEM", null, FmFieldType.TEXT, true) ); //1
		fields.add( new FmField(dummyTable, "TABLE_NAME", null, FmFieldType.TEXT, false) ); //2
		fields.add( new FmField(dummyTable, "COLUMN_NAME", null, FmFieldType.TEXT, false) ); //3
		fields.add( new FmField(dummyTable, "GRANTOR", null, FmFieldType.TEXT, false) ); //4
		fields.add( new FmField(dummyTable, "GRANTEE", null, FmFieldType.TEXT, false) ); //5
		fields.add( new FmField(dummyTable, "PRIVILEGE", null, FmFieldType.TEXT, false) ); //6
		fields.add( new FmField(dummyTable, "IS_GRANTABLE", null, FmFieldType.TEXT, false) ); //7

		if( catalog == null ) {
			catalog = connection.getCatalog();
		}
		List<String> privileges = getFieldPrivileges( catalog, table, columnNamePattern );

		List<FmRecord> result = new LinkedList<FmRecord>();
		for( String privilege : privileges ) {
			FmRecord record = new FmRecord( fields, null, null );
			record.addRawValue( catalog, 0 );
			record.addRawValue( table, 2 );
			record.addRawValue( columnNamePattern, 3 );
			record.addRawValue( connection.getUsername(), 5 );
			record.addRawValue( privilege, 6 );
			record.addRawValue( "NO", 7 );
			result.add( record );
		}
		return new FmResultSet( result.iterator(), result.size(), fields, connection );
	}

	private List<String> getFieldPrivileges( String dbName, String tableName, String fieldName ) throws SQLException {
		FmField fieldDefinition = null;

		for( FmField column : lastRawFields.getFields() ) {
			if( column.getColumnName().equalsIgnoreCase( fieldName ) ) {
				fieldDefinition = column;
				break;
			}
		}
		if( fieldDefinition == null ) {
			throw new SQLException( "No such field '" + fieldName + "' exists in database '" + dbName + "' table '" + tableName + "'.", null, 102 );
		}

		/*if( ! fieldDefinition.isAutoEnter() ) {
			return true; //If a field is neither a calc nor auto-enter, assume it's writeable
		}*/

		String lookup = dbName + "~" + tableName;
		Set<String> writeable = writeableFields.get( lookup );
		Set<String> readable = readableFields.get( lookup );
		if( writeable == null ) {
			String recid = null;
			writeable = new HashSet<String>();
			readable = new HashSet<String>();

			//First we try writing all fields that appear to be writeable. Summary fields and calculation fields are skipped here.
			try {
				getColumns( dbName, null, tableName, null );

				boolean includeAutoEnter = true;
				while( true ) {
					List<FmField> potentialWriteable = new ArrayList<FmField>( lastRawFields.getFields().size() );
					StringBuilder sql = new StringBuilder();
					sql.append( "INSERT INTO " + quotedIdentifier( tableName ) + "" );
					String delim="(";
					for( FmField field : lastRawFields.getFields() ) {
						if( ! field.isReadOnly() && field.getType() != FmFieldType.CONTAINER && ! field.getColumnName().contains("::") ) { // I don't know if there is any way to check container fields.
							if( field.isAutoEnter() && ! includeAutoEnter ) {
								continue; //Even though auto enter fields are 'writeable', they are much more likely to have write access prohibited, so we'll try once with them and if necessary we try again without them.
							}
							potentialWriteable.add( field );
							sql.append( delim ).append( quotedIdentifier( field.getColumnName() ) );
							delim=",";
						}
					}
					if( ",".equals( delim ) ) {
						sql.append( ")" );
					}
					delim = " VALUES(";
					for( FmField field : potentialWriteable ) {
						sql.append( delim ).append( "?" );
						delim = ",";
					}
					if( ",".equals( delim ) ) {
						sql.append( ")" );
					}
					PreparedStatement stmt = connection.prepareStatement( sql.toString(), PreparedStatement.RETURN_GENERATED_KEYS );
					int n=1;
					for( FmField field : potentialWriteable ) {
						stmt.setObject( n++, randomValueForType( field.getType().getSqlDataType() ) );
					}
					try {
						stmt.executeUpdate();
					} catch( SQLException e ) {
						if( includeAutoEnter ) {
							includeAutoEnter = false;
							continue; //Don't throw the exception; try again with this set to false
						} else {
							throw e;
						}
					}
					ResultSet keys = stmt.getGeneratedKeys();
					if( keys.next() ) {
						recid = keys.getString( "recid" );
						//connection.createStatement().executeUpdate( "DELETE FROM " + quotedIdentifier( tableName ) + " WHERE recid=" + recid );
					}
					keys.close();
					for( FmField field : potentialWriteable ) {
						writeable.add( field.getColumnName() );
						readable.add( field.getColumnName() );
					}
					break; //Break out of the loop if INSERT succeeded
				}
			} catch( SQLException e ) {
				log.log( Level.INFO, "One or more fields could not be written; will need to check each field individually: " + e.toString() );
			}


			//If we get a validation failure, or if there are fields that we did not test above, then our only option is to write to each field separately to see which ones work and which ones don't
			Statement stmt = getConnection().createStatement();
			//boolean insertWorked = true;
			try {
				try {
					if( recid == null ) { //We may have a recid from our previous test
						insertEmptyRecord( stmt, dbName, tableName );
						ResultSet rs = stmt.getGeneratedKeys();
						if( rs.next() ) {
							recid = rs.getString( "recid" ); //which field is the primary key? How will we delete this row when we're done?
						}
						rs.close();
					}
				} catch( SQLException e ) {
					if( e.getErrorCode() == 509 ) { //Validation failure, this is normal
						log.log( Level.INFO, "FileMaker's validation prevented a new record from being created in table " + tableName );
					} else {
						log.log( Level.WARNING, "Error while creating new row in table " + tableName + " to test which fields are writeable", e );
					}
					recid = null;
				}

				if( recid != null ) {
					try {
						for( FmField field : lastRawFields.getFields() ) {
							if( writeable.contains( field.getColumnName() ) ) {
								continue; //We've already tested it previously	
							}
							if( field.isReadOnly() ) {
								continue; //Don't need to test calc fields / summary fields. Testing is really pretty slow. The only problem with skipping this is that we won't know whether they are readable.
							}
							if( field.getColumnName().contains( "::" ) ) {
								continue; //Don't test related fields for whether they're writeable
							}
							try {
								String value = randomValueForType( field.getType().getSqlDataType() );
								if( value == null ) { //This indicates a container field or some other field type that cannot be written to
									continue;
								}
	
								String sql = "UPDATE \"" + tableName + "\" SET \"" + field.getColumnName() + "\"= \"" + value + "\" WHERE recid=" + recid;
								stmt.executeUpdate( sql );
								writeable.add( field.getColumnName() );
								readable.add( field.getColumnName() );
							} catch( FileMakerException e ) {
								final int errorCode = e.getErrorCode();
								if( errorCode >= 500 && errorCode < 600 ) { //This is a validation error. Don't assume that the field is not writeable, it just means that we don't know what the valid values are.
									writeable.add( field.getColumnName() );
									readable.add( field.getColumnName() );
								} else if( errorCode == 201 || (errorCode >=500 && errorCode < 600) ) {
									//Field is not writeable; skip
									readable.add( field.getColumnName() );
								} else if( errorCode == 102 ) { //This happens when a field is completely unreadable.
									//Field is not readable; skip
								} else {
									throw e;
								}
							}
						}
					} finally {
						String sql = "DELETE FROM \"" + tableName + "\" WHERE recid=" + recid;
						stmt.executeUpdate( sql );
					}
				}
			} finally {
				stmt.close();
			}
			if( recid == null ) {
				log.warning( "Could not create a test row in the database for checking field permissions; will assume that all rows are writeable" );
				for( FmField field : lastRawFields.getFields() ) {
					writeable.add( field.getColumnName() );
					readable.add( field.getColumnName() );
				}
				//Could not create test row, assume all fields are writeable?
			}

			writeableFields.put( lookup, writeable );
			readableFields.put( lookup, readable );
		}

		List<String> result = new LinkedList<String>();
		if( readable.contains( fieldName ) ) {
			result.add( "SELECT" );
		}
		if( writeable.contains( fieldName ) ) {
			result.add( "UPDATE" );
			result.add( "INSERT" ); //FIX!! It's possible that field can be inserted even if it's not readable; currently our process will not detect that.
		}
		return result;
	}

	public void insertEmptyRecord( Statement stmt, @Nullable String catalog, String tableName ) throws SQLException {
		List<String> requiredColumns = new LinkedList<String>();
		List<String> requiredValues = new LinkedList<String>();
		/*final ResultSet columns = getColumns( null, null, tableName, null );
		while( columns.next() ) {
			if( columns.getInt( 11 ) == DatabaseMetaData.columnNoNulls ) {
				requiredColumns.add( columns.getString( 4 ) );
				requiredValues.add( randomValueForType( columns.getInt( 5 ) ) );
			}
		}*/
		getColumns( catalog, null, tableName, null );
		for( FmField field : lastRawFields.getFields() ) {
			if( ! field.isNullable() && ! field.isAutoEnter() ) {
				requiredColumns.add( field.getColumnName() );
				requiredValues.add( randomValueForType( field.getType().getSqlDataType() ) );
			}
		}

		StringBuilder sql = new StringBuilder( 256 );
		sql.append("INSERT INTO '" + tableName + "'"); //FIX!! Do we need to quote this? Try with spaces and see what happens
		String plainSql = sql.toString();
		if( requiredColumns.size() > 0 ) {
			sql.append( '(' ) ;

			String delim="";
			for( String column : requiredColumns ) {
				sql.append( delim + quotedIdentifier( column ) );
				delim = ",";
			}
			sql.append( ") VALUES(" );
			delim = "";
			for( String value : requiredValues ) {
				sql.append( delim + "'" + value + "'" );
				delim=",";
			}
			sql.append( ")" );
		}
		try {
			stmt.executeUpdate( sql.toString(), Statement.RETURN_GENERATED_KEYS );
		} catch( SQLException e ) {
			if( e.getErrorCode() == 201 ) { //This indicates that a field cannot be modified. If a field has both required validation and prevent modification of auto-entered values, this will occur (as is often the case with primary keys)
				stmt.executeUpdate( plainSql, Statement.RETURN_GENERATED_KEYS );
			} else if( e.getErrorCode() == 303 ) {
				throw new SQLException( "You or somebody else on the network has the database field definitions window open. This must be closed before proceeding.", e.getSQLState(), 303 );
			} else if( e.getErrorCode() == 504 ) {
				throw e; //FIX!! 504 indicates that there is already a record with this value. Since these are supposed to be meaningless values, either 1) the user has a record with that same primary key (unlikely) or there is a test record left over from previously calling this function (much more likely). --jsb
			} else throw e;
		}
	}

	private String quotedIdentifier( String column ) throws SQLException {
		return getIdentifierQuoteString() + column + getIdentifierQuoteString();
	}

	public String randomValueForType( int dataType ) {
		String result;
		if( dataType == Types.DATE ) {
			result = "1/1/2000";
		} else if( dataType == Types.TIMESTAMP ) {
			result = "1/1/2000 11:08am";
		} else if( dataType == Types.TIME ) {
			result = "11:07am";
		} else if( dataType == Types.BLOB ) { //Can't write to container fields
			result = null;
		} else {
			result = "4289134876123876123";
		}
		return result;
	}


	public ResultSet getVersionColumns( String catalog, String schema, String table ) throws SQLException {
		getColumns( catalog, schema, table, null );
		List<FmField> versionCandidates = new ArrayList<FmField>(3);
		for( FmField field : lastRawFields.getFields() ) {
			if( field.isModstampCandidate() ) {
				versionCandidates.add( field );
			}
		}
		Comparator<? super FmField> modstampComparator = new Comparator<FmField>() {
			public int compare( FmField o1, FmField o2 ) {

				//If one field contains 'mod' then it's probably a mod stamp
				String name1 = o1.getColumnName().toLowerCase();
				if( name1.contains( "mod" ) ) return -1;
				String name2 = o2.getColumnName().toLowerCase();
				if( name2.contains( "mod" ) ) return 1;

				//No clue, treat them the same
				return 0;
			}
		};
		Collections.sort( versionCandidates, modstampComparator );
		if( versionCandidates.size() == 0 ) {
			return new FmResultSet(null, 0, new FmFieldList(), connection );
		} else {
			FmFieldList rsColumns = new FmFieldList();
			FmTable dummyTable = new FmTable("Field definitions");

			rsColumns.add( new FmField(dummyTable, "SCOPE", null, FmFieldType.TEXT, true) ); //0 SCOPE short => is not used
			rsColumns.add( new FmField(dummyTable, "COLUMN_NAME", null, FmFieldType.TEXT, false) ); //1 COLUMN_NAME String => column name
			rsColumns.add( new FmField(dummyTable, "DATA_TYPE", null, FmFieldType.NUMBER, false) ); //2 DATA_TYPE int => SQL data type from java.sql.Types
			rsColumns.add( new FmField(dummyTable, "TYPE_NAME", null, FmFieldType.TEXT, false) ); //3 TYPE_NAME String => Data source-dependent type name
			rsColumns.add( new FmField(dummyTable, "COLUMN_SIZE", null, FmFieldType.NUMBER, false) ); //4 COLUMN_SIZE int => precision
			rsColumns.add( new FmField(dummyTable, "BUFFER_LENGTH", null, FmFieldType.NUMBER, true) ); //5 BUFFER_LENGTH int => length of column value in bytes
			rsColumns.add( new FmField(dummyTable, "DECIMAL_DIGITS", null, FmFieldType.NUMBER, false) ); //6 DECIMAL_DIGITS short => scale - Null is returned for data types where DECIMAL_DIGITS is not applicable.
			rsColumns.add( new FmField(dummyTable, "PSEUDO_COLUMN", null, FmFieldType.NUMBER, false) ); //7 PSEUDO_COLUMN short => whether this is pseudo column like an Oracle ROWID

			FmRecord result = new FmRecord( rsColumns, "0", 0L );

			FmField versionField = versionCandidates.get( 0 );
			result.addRawValue( "" + versionField.getColumnName(), 1 );
			result.addRawValue( "" + versionField.getType().getSqlDataType(), 2 );
			result.addRawValue( versionField.getType().getExternalTypeName(), 3 );
			result.addRawValue( "" + versionField.getType().getPrecision(), 4 );
			result.addRawValue( "" + 17, 6 ); //FIX!! Wild-ass guess, really don't know what to put here --jsb
			result.addRawValue( "NO", 7 );

			Iterator<FmRecord> it = Collections.singleton( result ).iterator();
			return new FmResultSet( it, 1, rsColumns, connection );
		}
	}

	public ResultSet getPrimaryKeys( String catalog, String schema, String table ) throws SQLException {
		getColumns( catalog, schema, table, null );
		List<FmField> pkCandidates = new ArrayList<FmField>(3);
		for( FmField field : lastRawFields.getFields() ) {
			if( field.isPrimaryKeyCandidate() ) {
				pkCandidates.add( field );
			}
		}
		Comparator<? super FmField> pkComparator = new Comparator<FmField>() {
			public int compare( FmField o1, FmField o2 ) {
				String name1 = o1.getColumnName().toLowerCase();

				//If one field contains 'pk' or starts with 'id' then it's probably a primary key.
				if( name1.startsWith( "id" ) || name1.contains( "pk" ) ) return -1;
				String name2 = o2.getColumnName().toLowerCase();
				if( name2.startsWith( "id" ) || name2.contains( "pk" ) ) return 1;

				//If one of the fields is a number and the other one is not, the numeric field is probably the primary key
				if( o1.getType() == FmFieldType.NUMBER && o2.getType() != FmFieldType.NUMBER ) return -1;
				if( o2.getType() == FmFieldType.NUMBER && o1.getType() != FmFieldType.NUMBER ) return 1;

				//No clue, treat them the same
				return 0;
			}
		};
		Collections.sort( pkCandidates, pkComparator );
		FmFieldList rsColumns = new FmFieldList();
		if( pkCandidates.size() == 0 ) {
			return new FmResultSet(null, 0, rsColumns, connection );
		} else {
			FmTable dummyTable = new FmTable("Field definitions");
			rsColumns.add( new FmField(dummyTable, "TABLE_CAT", null, FmFieldType.TEXT, true) ); //0
			rsColumns.add( new FmField(dummyTable, "TABLE_SCHEM", null, FmFieldType.TEXT, true) ); //1
			rsColumns.add( new FmField(dummyTable, "TABLE_NAME", null, FmFieldType.TEXT, false) ); //2
			rsColumns.add( new FmField(dummyTable, "COLUMN_NAME", null, FmFieldType.TEXT, false) ); //3
			rsColumns.add( new FmField(dummyTable, "KEY_SEQ", null, FmFieldType.NUMBER, false) ); //3
			rsColumns.add( new FmField(dummyTable, "PK_NAME", null, FmFieldType.TEXT, false) ); //3

			FmRecord result = new FmRecord( rsColumns, "0", 0L );
			//TABLE_CAT String => table catalog (may be null)
			//TABLE_SCHEM String => table schema (may be null)
			result.addRawValue( table, 2 ); //TABLE_NAME String => table name
			result.addRawValue( pkCandidates.get( 0 ).getColumnName(), 3 ); //COLUMN_NAME String => column name
			result.addRawValue( "1", 4 ); //KEY_SEQ short => sequence number within primary key( a value of 1 represents the first column of the primary key, a value of 2 would represent the second column within the primary key).
			result.addRawValue( pkCandidates.get( 0 ).getColumnName(), 5 ); //PK_NAME String => primary key name (may be null)
			Iterator<FmRecord> it = Collections.singleton( result ).iterator();
			return new FmResultSet( it, 1, rsColumns, connection );
		}
	}

	public ResultSet getExportedKeys( String catalog, String schema, String layout ) throws SQLException {
		throw new AbstractMethodError("This feature has not been implemented yet."); //FIX!!! Broken placeholder
	}

	public ResultSet getImportedKeys( String s, String s1, String s2 ) throws SQLException {
		return new FmResultSet(null, 0, new FmFieldList(), connection );
	}

	public ResultSet getTypeInfo() throws SQLException {
		List typesList = new LinkedList();
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, String.valueOf( FmFieldType.resultSetFormat));
		}
		Object eachType;
		for( Iterator it = FmFieldType.publishedTypes.iterator(); it.hasNext(); ) {
			eachType = ((FmFieldType)it.next()).getInResultSetFormat();
			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, String.valueOf(eachType ));
			}
			typesList.add( eachType );
		}
		return new FmResultSet( typesList.iterator(), typesList.size(), FmFieldType.resultSetFormat, connection );
	}

	public Connection getConnection() throws SQLException {
		return connection;
	}

	public boolean supportsGetGeneratedKeys() throws SQLException {
		return true;
	}

	public String getDatabaseProductName() throws SQLException {
		readDbInfo();
		return databaseProductName;
	}

	public int getDatabaseMajorVersion() throws SQLException {
		readDbInfo();
		return dbMajorVersion;
	}

	public int getDatabaseMinorVersion() throws SQLException {
		readDbInfo();
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
		readDbInfo();
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
				//String databaseName = rawRecord.getRawStringValue( 0 );
				String databaseName = rawRecord.getString( 0, 1 );
				FmRecord processRecord = new FmRecord(format, null, null);
				processRecord.addRawValue( databaseName, 0 );
				databases.add(processRecord);
			}
		} catch( SQLException e ) {
			if( e.getErrorCode() == 9 ) {
				throw new SQLException( "The username and password are invalid.", null, e.getErrorCode(), e );
			} else if( e.getErrorCode() == 18 ) {
				throw new SQLException( "FileMaker Server is configured to require a valid username and password to retrieve a list of databases.", null, e.getErrorCode(), e );
			}
			throw e;
		} catch (IOException e) {
			SQLException sqlException = new SQLException( "Could not get list of databases: " + e.toString() );
			sqlException.initCause(e);
			throw sqlException;
		}
		finally {
			request.closeRequest(); // the parsing thread should take care of this... but just in case it's taking too long
		}

		ResultSet result = new FmResultSet(databases.iterator(), databases.size(), format, connection );
		return result;
	}

	public boolean usesLocalFiles() throws SQLException {
		return false;
	}

	public int getJDBCMajorVersion() throws SQLException {
		return 3;
	}

	public int getJDBCMinorVersion() throws SQLException {
		return 12;
	}

	public boolean usesLocalFilePerTable() throws SQLException {
		return false;
	}

	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		return false;
	}

	public boolean storesUpperCaseIdentifiers() throws SQLException {
		return false;
	}

	public boolean storesLowerCaseIdentifiers() throws SQLException {
		return false;
	}

	public boolean storesMixedCaseIdentifiers() throws SQLException {
		return true;
	}

	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		return false;
	}

	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		return true;
	}

	public String getSQLKeywords() throws SQLException {
		return ""; //FIX!! Do we have any special functions? What about recid?
	}

	public String getNumericFunctions() throws SQLException {
		return "";
	}

	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		return false;
	}

	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		return false;
	}

	public boolean supportsConvert() throws SQLException {
		return false;
	}

	public boolean supportsConvert( int i, int i1 ) throws SQLException {
		return false;
	}

	public boolean supportsGroupBy() throws SQLException {
		return false;
	}

	public boolean supportsGroupByUnrelated() throws SQLException {
		return false;
	}

	public boolean supportsGroupByBeyondSelect() throws SQLException {
		return false;
	}

	public boolean supportsMultipleResultSets() throws SQLException {
		return false;
	}

	public boolean supportsMultipleTransactions() throws SQLException {
		return false;
	}

	public boolean supportsOuterJoins() throws SQLException {
		return false;
	}

	public boolean supportsFullOuterJoins() throws SQLException {
		return false;
	}

	public boolean supportsLimitedOuterJoins() throws SQLException {
		return false;
	}

	public String getCatalogSeparator() throws SQLException {
		return catalogSeparator;
	}

	public void setCatalogSeparator(String o) {
		catalogSeparator = o;
	}

	public boolean supportsSelectForUpdate() throws SQLException {
		return false;
	}

	public boolean supportsSubqueriesInComparisons() throws SQLException {
		return false;
	}

	public boolean supportsSubqueriesInExists() throws SQLException {
		return false;
	}

	public boolean supportsSubqueriesInIns() throws SQLException {
		return false;
	}

	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return false;
	}

	public boolean supportsCorrelatedSubqueries() throws SQLException {
		return false;
	}

	public boolean supportsUnion() throws SQLException {
		return false;
	}

	public boolean supportsUnionAll() throws SQLException {
		return false;
	}

	public int getMaxConnections() throws SQLException {
		return Integer.MAX_VALUE; //Connections in woof are stateless
	}

	public boolean supportsBatchUpdates() throws SQLException {
		return false;
	}

	public boolean supportsMultipleOpenResults() throws SQLException {
		return true;
	}




	//--- These methods can be ignored ---
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

	public String getSearchStringEscape() throws SQLException {
		throw new AbstractMethodError( "getSearchStringEscape is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getExtraNameCharacters() throws SQLException {
		return "";
	}

	public boolean supportsColumnAliasing() throws SQLException {
		throw new AbstractMethodError( "supportsColumnAliasing is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean nullPlusNonNullIsNull() throws SQLException {
		throw new AbstractMethodError( "nullPlusNonNullIsNull is not implemented yet." ); //FIX!!! Broken placeholder
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

	public boolean supportsLikeEscapeClause() throws SQLException {
		throw new AbstractMethodError( "supportsLikeEscapeClause is not implemented yet." ); //FIX!!! Broken placeholder
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

	public String getSchemaTerm() throws SQLException {
		throw new AbstractMethodError( "getSchemaTerm is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public String getCatalogTerm() throws SQLException {
		throw new AbstractMethodError( "getCatalogTerm is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isCatalogAtStart() throws SQLException {
		throw new AbstractMethodError( "isCatalogAtStart is not implemented yet." ); //FIX!!! Broken placeholder
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

	public ResultSet getUDTs( String s, String s1, String s2, int[] ints ) throws SQLException {
		throw new AbstractMethodError( "getUDTs is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsSavepoints() throws SQLException {
		throw new AbstractMethodError( "supportsSavepoints is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsNamedParameters() throws SQLException {
		throw new AbstractMethodError( "supportsNamedParameters is not implemented yet." ); //FIX!!! Broken placeholder
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

	public int getSQLStateType() throws SQLException {
		throw new AbstractMethodError( "getSQLStateType is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean locatorsUpdateCopy() throws SQLException {
		throw new AbstractMethodError( "locatorsUpdateCopy is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsStatementPooling() throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	// === These are new methods added in Java 1.5 ===

	public ResultSet getSchemas( String s, String s1 ) throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean supportsStoredFunctionsUsingCallSyntax() throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getClientInfoProperties() throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getFunctions( String s, String s1, String s2 ) throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public ResultSet getFunctionColumns( String s, String s1, String s2, String s3 ) throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public <T> T unwrap( Class<T> aClass ) throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean isWrapperFor( Class<?> aClass ) throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}

	// === New methods added in Java 1.6. Commment them out to compile in Java 1.5. ===

	public RowIdLifetime getRowIdLifetime() throws SQLException {
		throw new AbstractMethodError( "supportsStatementPooling is not implemented yet." ); //FIX!!! Broken placeholder
	}
}
