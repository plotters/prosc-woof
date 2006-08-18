package com.prosc.fmpjdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.*;

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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 17, 2005 Time: 10:26:23 AM
 */
public class StatementProcessor {
	private SqlCommand command;
	private FmStatement statement;
	private int updateRowCount = 0;
	private FmRecord insertedRecord;
	private FmResultSet results;
	private Vector params;
	static final String WILDCARDS_EQUALS ="<>=�!?@#\"~*";
	static final String WILDCARDS_LIKE ="<>=�!?@#\"~";// note: * is not included, because that does what it is supposed to for LIKE searches.

	private static final String ESCAPE_C = "\\"; // escaped backslash.  Note, this should NOT be unicode encoded!
	//private FieldPosition sharedFieldPosition = new FieldPosition(0);
	private Logger logger = Logger.getLogger("com.prosc.fmpjdbc");

	public StatementProcessor( FmStatement statement, SqlCommand command ) {
		this.command = command;
		this.statement = statement;
	}

	/**
	 * List of valid FMP commands: -dbnames � -delete X -dup � -edit � -find, -findall, -findany -layoutnames � -new
	 * -process -script -view
	 */
	public void execute() throws SQLException {
		if (logger.isLoggable(Level.FINE)) {
			logger.log(Level.FINE, command.getSql());
		}

/*		if (params !=null) {
		for( int i = 0; i < params.size(); i++ ) {
			Object o = (Object)params.elementAt( i );
			System.out.println( "Where:" + o );
		}
		}*/
		if (command.getTable() == null) {
			throw new SQLException("No table was specified");
		}

		FmXmlRequest recIdHandler = null;
		FmXmlRequest actionHandler = null;
		try {
			String dbLayoutString;
			if( ( (FmConnection)statement.getConnection() ).getFmVersion() < 7 ) {
				dbLayoutString = "-db=" + getDatabaseName();
				String layoutName = getLayoutName();
				if( layoutName != null ) dbLayoutString += "&-lay=" + layoutName;
				else logger.info( "Executing an SQL query without a layout name can be slow. Specify a layout name for best efficiency." );
			} else {
				dbLayoutString = "-db=" + getDatabaseName() + "&-lay=" + getLayoutName();
			}
			int currentParam = 0;

			StringBuffer updateClause = new StringBuffer();
			for( Iterator it = command.getAssignmentTerms().iterator(); it.hasNext(); ) {
				AssignmentTerm eachTerm = (AssignmentTerm)it.next();
				//String encodedParam;
				updateClause.append("&");
				updateClause.append( URLEncoder.encode(eachTerm.getField().getColumnName(), "UTF-8") + "=" );
				if( eachTerm.isPlaceholder() ) {
					// OPIMIZE!! there is a lot of string creation going on inside loops.  Everything could just be appeneded to the buffers instead, including formatter functions. -ssb
					updateClause.append( urlEncodedValue( params.elementAt( currentParam ), true, null, false) );
					currentParam++;
				} else {
					updateClause.append( urlEncodedValue( eachTerm.getValue(), false, null, false) );
				}
			}

			StringBuffer whereClause = null;
			Map whereSegments = new LinkedHashMap( command.getSearchTerms().size() );

			for( Iterator it = command.getSearchTerms().iterator(); it.hasNext(); ) {
				SearchTerm eachTerm = (SearchTerm)it.next();
				if( "recid".equals( eachTerm.getField().getColumnName().toLowerCase() ) ) { //Throw away all other params, just use recid
					whereClause = new StringBuffer( "&-recid=" + eachTerm.getValue() );
					break;
				}
				String fieldName = eachTerm.getField().getColumnName(); //FIX!! use fully qualified table names for related fields
				Object[] eachTermSegments = new Object[4];
				String wildcardsToEscape;
				eachTermSegments[0] = "";
				final int operator = eachTerm.getOperator();
				eachTermSegments[3] = new Integer( operator );
				if (operator == SearchTerm.EQUALS) {
					wildcardsToEscape = WILDCARDS_EQUALS;
				} else if (operator == SearchTerm.LIKE) {
					wildcardsToEscape = WILDCARDS_LIKE;
				} else {
					wildcardsToEscape = WILDCARDS_EQUALS;
					String operatorString;
					if( operator == SearchTerm.BEGINS_WITH )
						operatorString = "bw";
					else if( operator == SearchTerm.CONTAINS )
						operatorString = "cn";
					else if( operator == SearchTerm.ENDS_WITH )
						operatorString = "ew";
					else if( operator == SearchTerm.GREATER_THAN )
						operatorString = "gt";
					else if( operator == SearchTerm.GREATER_THAN_OR_EQUALS )
						operatorString = "gte";
					else if( operator == SearchTerm.LESS_THAN )
						operatorString = "lt";
					else if( operator == SearchTerm.LESS_THAN_OR_EQUALS )
						operatorString = "lte";
					else if( operator == SearchTerm.NOT_EQUALS )
						operatorString = "neq";
					else
						throw new IllegalArgumentException("Unknown search term operator " + operator );
					eachTermSegments[0] = "&" + URLEncoder.encode(fieldName + ".op", "UTF-8") + "=" + operatorString;
				}
				Object value;
				boolean applyFormatter = false;
				if( eachTerm.isPlaceholder() ) {
					value = params.elementAt(currentParam++);
					applyFormatter = true;
				} else {
					value = eachTerm.getValue();
				}
				eachTermSegments[1] = "&" + URLEncoder.encode(fieldName, "UTF-8") + "=";
				eachTermSegments[2] = urlEncodedValue(value, applyFormatter, wildcardsToEscape, operator == SearchTerm.EQUALS);

				/* This checks to see if the same term is used multiple times, and if it is, it
					 * checks to see if it's a range operation. If so, it re-adds the two search terms using a "..." notation.
					 * They are always added in the correct order so that the first term is the lower value and the second value is the higher value.
					 * This is necessary, since FM7 can't handle multiple operators for the same search term (FM6 could).
					 */
				Object[] matchingField = (Object[])whereSegments.get( fieldName );
				if( matchingField != null ) { //We've already used this field earlier in the qualifier
					eachTermSegments[0] = "";
					int op1 = ((Integer)matchingField[3]).intValue();
					if( (op1 == SearchTerm.GREATER_THAN || op1 == SearchTerm.GREATER_THAN_OR_EQUALS) && (operator == SearchTerm.LESS_THAN || operator == SearchTerm.LESS_THAN_OR_EQUALS ) ) {
						eachTermSegments[2] = matchingField[2] + "..." + eachTermSegments[2];
					} else if( (op1 == SearchTerm.LESS_THAN || op1 == SearchTerm.LESS_THAN_OR_EQUALS) && ( operator == SearchTerm.GREATER_THAN || operator == SearchTerm.GREATER_THAN_OR_EQUALS ) ) {
						eachTermSegments[2] = eachTermSegments[2] + "..." + matchingField[2];
					} else {
						throw new SqlParseException( "You cannot use the same search time twice unless they are being used with >, >=, <, or <= operators for ranges.");
					}
				}

				whereSegments.put( fieldName, eachTermSegments );
			} // end of for loop is = command.getSearchTerms.getIterator()
			if( whereClause == null ) {
				whereClause = new StringBuffer();
				Object[] segments;
				for( Iterator it = whereSegments.values().iterator(); it.hasNext(); ) {
					segments = (Object[])it.next();
					whereClause.append( segments[0] );
					whereClause.append( segments[1] );
					whereClause.append( segments[2] );
				}
			}

			FmConnection connection = (FmConnection) statement.getConnection();
			recIdHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
					connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
			actionHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
					connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
			Integer recordId;

			switch( command.getOperation() ) {
				case SqlCommand.SELECT:
					StringBuffer postArgs = new StringBuffer( 200 );
					//FIX!!! Need to be able to get a subset of all fields on the layout
					int sortPriority = 1;
					for( Iterator it = command.getSortTerms().iterator(); it.hasNext(); ) {
						SortTerm eachTerm = (SortTerm)it.next();
						String order = eachTerm.getOrder() == SortTerm.ASCENDING ? "ascend" : "descend";
						String fieldName = eachTerm.getField().getColumnName(); //FIX!! Use fully qualified names? Does this apply for sorting?
						postArgs.append( "&-sortfield." + sortPriority + "=" + fieldName + "&-sortorder." + sortPriority + "=" + order );
						sortPriority++;
					}
					if( whereClause.length() == 0 )
						postArgs.append( "&-max=all&-findall" );
					else {
						if( command.getLogicalOperator() == SqlCommand.OR ) postArgs.append( "&-lop=or" );
						postArgs.append( whereClause );
						if( command.getMaxRows() != null ) postArgs.append( "&-max=" + command.getMaxRows().intValue() );
						if( command.getSkipRows() != null ) postArgs.append( "&-skip=" + command.getSkipRows().intValue() );
						postArgs.append( "&-max=all&-find" );
					}

					actionHandler.setSelectFields(command.getFields()); // Set the fields that are used in the select statement
					actionHandler.doRequest( dbLayoutString + postArgs );

					results = new FmResultSet( actionHandler.getRecordIterator(), actionHandler.getFieldDefinitions(), (FmConnection)statement.getConnection() );
					// DO NOT CLOSE the request since the result set needs to stream the records
					break;


				case SqlCommand.UPDATE:
					recIdHandler.doRequest( dbLayoutString + whereClause + "&-max=all&-find" );
					for( Iterator it = recIdHandler.getRecordIterator(); it.hasNext(); ) {
						recordId = ( (FmRecord)it.next() ).getRecordId();
						actionHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
								connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
						try {
							actionHandler.doRequest( dbLayoutString + updateClause + "&-recid=" + recordId + "&-edit" );
							actionHandler.closeRequest(); // the parsing thread should take care of this... but just in case it's taking too long
						} catch (RuntimeException e) {
							actionHandler.closeRequest(); // the parsing thread should take care of this... but just in case it's taking too long
							updateRowCount = 0;
							throw e;
						}
					}
					updateRowCount = recIdHandler.getFoundCount();
					recIdHandler.closeRequest();
					break;

				case SqlCommand.DELETE:
					recIdHandler.doRequest( dbLayoutString + whereClause + "&-max=all&-find" );
					for( Iterator it = recIdHandler.getRecordIterator(); it.hasNext(); ) {
						recordId = ( (FmRecord)it.next() ).getRecordId();
						actionHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
								connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
						try {
							actionHandler.doRequest( dbLayoutString + "&-recid=" + recordId + "&-delete" );
							actionHandler.closeRequest(); // the parsing thread should take care of this... but just in case it's taking too long
						} catch (RuntimeException e) {
							actionHandler.closeRequest(); // the parsing thread should take care of this... but just in case it's taking too long
							throw e;
						}
					}
					updateRowCount = recIdHandler.getFoundCount();
					recIdHandler.closeRequest();
					break;

				case SqlCommand.INSERT:
					try {
						actionHandler.doRequest( dbLayoutString + updateClause + "&-new" ); //FIX!! What about exception handling?
					} catch( FileMakerException e ) {
						throw e;
					}
					updateRowCount = actionHandler.getFoundCount();
					insertedRecord = (FmRecord) actionHandler.getRecordIterator().next();
					actionHandler.closeRequest();// inserts can only insert a single record, so we'll assume that the first record is the one that we want
					break;
			}
		} catch( IOException e ) {
			SQLException e1 = new SQLException( e.toString(), "What is SQLState?", 42 ); //FIX!! Need better exceptions
			e1.initCause( e );
			throw e1;
		} catch( FileMakerException e ) {
			e.setStatementProcessor( this );
			throw e;
		} finally {
			try {
				// the requests should be closed in their respective case block

			} catch (Exception e) {
				throw new RuntimeException("Exception occurred in finally clause", e);
			}
		}

		/*if( command.getOperation() == SqlCommand.SHOW_DATABASES ) {
		url.append("-dbnames");
		} else if( command.getOperation() == SqlCommand.SHOW_TABLES ) {
		url.append( "-db=" + getDatabaseName() + "&-layoutnames" );
		}
		else {
		}
*/
	}

	/*private String applyFormater(Object o) {
		if (o instanceof Time) {
			//SimpleDateFormat format = new SimpleDateFormat( "HH:mm:ss");
			//String time = format.format( o);
			return ((DateFormat) FmRecord.timeFormat.get()).format(o);
		} else if (o instanceof Timestamp) {
			return ((DateFormat) FmRecord.timestampFormat.get()).format(o);
		} else if (o instanceof Date) {
			return ((DateFormat) FmRecord.dateFormat.get()).format(o);
		} else {
			return o.toString();
		}
	}*/

	/**
	 * Appends a key/value pair to a StringBuffer, performing appropriate formatting, character escaping, and other actions.
	 * @param value
	 * @param applyFormatter
	 * @param wildcardsToEscape
	 * @param isEqualsQualifier Whether the key/value pair signifies an EQUALS search, in which case an escaped "=" sign will be prepended to the value, indicating to Filemaker that it should perform an exact search.
	 * @throws SQLException
	 */
	private String urlEncodedValue(Object value, boolean applyFormatter, String wildcardsToEscape, boolean isEqualsQualifier) throws SQLException {
		try {
			StringBuffer result = new StringBuffer();
			if (isEqualsQualifier) {
				result.append("%3D"); // one encoded "equals" sign, signifying field content match. Used to use two, which is more precise, but horribly slow. --jsb
			}
			if (applyFormatter ) {
				// all the things we were checking for subclass java.util.Date, just check once.
				// FIX!!! need to URLEncode formatted values too -ssb
				if (value instanceof Time) {
					value = ((DateFormat)FmRecord.timeFormat.get()).format(value);
				} else if (value instanceof Timestamp) {
					value = ((DateFormat)FmRecord.timestampFormat.get()).format(value);
				} else if ( value instanceof Date ) {
					value = ((DateFormat) FmRecord.dateFormat.get()).format(value);
				}
			}
			if (wildcardsToEscape != null) {
				String s = value == null ? "" : value.toString();
				//s = String.valueOf( value );
				StringBuffer escapedValue = new StringBuffer(s==null ? 1 : s.length());
				appendEscapedFmWildcards(s, escapedValue, wildcardsToEscape);
				result.append(URLEncoder.encode(escapedValue.toString(), "UTF-8"));
			} else if (value != null) {
				result.append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
			}
			return result.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	private String urlEncode( String key, String value ) {
		//System.out.println("Encoding " + key + "=" + value );
		try {
			StringBuffer result = new StringBuffer(URLEncoder.encode( key, "UTF-8" ) + "=");
			if( value != null ) result.append( URLEncoder.encode( value, "UTF-8" ) );
			return result.toString();
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException( e );
		}
	}
	*/

	/**
	 * Appends the input string to the stringBuffer, with any characters in wildcardsToEscape escaped with a backslash character.
	 * @param input The input string
	 * @param toAppendTo The output StringBuffer
	 * @param wildcardsToEscape String containing characters to be escaped
	 * @throws SQLException
	 */
	void appendEscapedFmWildcards( Object input, StringBuffer toAppendTo, String wildcardsToEscape ) throws SQLException {
		// OPTIMIZE! accept a StringBuffer here instead, this gets called often, and the strings are large -ssb
		// Escapes FileMaker Wild Cards - Matt White
		//FM6 or lower the whole string is enclosed in duoble quotes
		//FM7 or higher just the wildcard is escaped with a backslash

		if( input == null ) {
			toAppendTo.append('='); // null searches in FMP are represented by an equals sign
			//return "="; //FIX! Integrate better with code below
		} else {
			String incoming = input.toString();
			if (incoming.length() == 0) return;
			if (( (FmConnection)statement.getConnection() ).getFmVersion() < 7) {
				_escapeFMWildCards6(incoming, toAppendTo, wildcardsToEscape);
			} else {
				_escapeFMWildCards7(incoming, toAppendTo, wildcardsToEscape);
			}
		}
	}

	void _escapeFMWildCards6(String incoming, StringBuffer toAppendTo, String wildcardsToEscape) {
		StringTokenizer s = new StringTokenizer(incoming, wildcardsToEscape, false);
		s.nextElement();
		if (s.hasMoreElements()) {
			// this contains at least one wildcard
			toAppendTo.append("\"").append(incoming).append("\"");
		} else {
			toAppendTo.append(incoming);
		}
	}

	void _escapeFMWildCards7(String incoming, StringBuffer toAppendTo, String wildcardsToEscape) {
		String tk;
		StringTokenizer s = new StringTokenizer(incoming, wildcardsToEscape, true);
		while (s.hasMoreTokens()) {
			tk = s.nextToken();
			if (tk.length() == 1 && wildcardsToEscape.indexOf(tk) >= 0) {
				toAppendTo.append( ESCAPE_C ).append(tk); //FIX!!! Figure out why this was here - Matt added it, but it's screwing up searches --jsb
				//temp.append(tk);
			} else {
				toAppendTo.append(tk);
			}
		}
	}

	private String getLayoutName() throws SQLException {
		String layoutName = command.getTable().getName();
		if (((FmConnection) statement.getConnection()).getFmVersion() < 7) {
			// this could be the dbName, we need to make sure the catalog and dbName in the FmTable are empty
			if (statement.getConnection().getCatalog() == null && command.getTable().getDatabaseName() == null) {
				return null; // this is the dbName, NOT the layout name
			} else {
				return layoutName;
			}
		} else { // FM version >= 7
			//getDatabaseName should have already caught if there was no db name given, so this name should be
			// the layout name
			return layoutName;
		}
	}

	/** Gets the databasename for the sqlCommand being executed.
	 * If no database is specified in the SQL, the database if the JDBC URL is used.
	 * @see FmTable#FmTable(String)  */
	private String getDatabaseName() throws SQLException{
		String result =command.getTable().getDatabaseName(); // gives priority to dbName.layout over the catalog setting
		if (result == null) {
			result = statement.getConnection().getCatalog(); //FIX!! What do we do if no catalog is specified?
			// if there is nothing in the databaseName, and nothing in the catalog, then ONLY if FmVersion is < 7
			// should we assume that the dbname the name provided to FmTable
			if (((FmConnection) statement.getConnection()).getFmVersion() < 7 && result == null) {
				result = command.getTable().getName();
			} else if (((FmConnection) statement.getConnection()).getFmVersion() >= 7 && result == null) {
				// then there is no db specified, and it MUST be specified either as dbName.layout, or as the catalog for FM >=7
				throw new SQLException("You must specify a database name either when creating the connection, or in the sql statement for FileMaker version 7+");
			}
		}
		return result;
	}

	public boolean hasResultSet() {
		return results != null;
	}

	public ResultSet getResultSet() {
		return results;
	}

	public int getUpdateRowCount() {
		return updateRowCount;
	}

	public ResultSet getGeneratedKeys() {
		// generate a map of keys & values which have non-null values, but were not in the assignmentTerms for the SqlCommand.
		// these are the auto-generated items.
		Map resultMap = new LinkedHashMap( 2 );
		FmFieldList fieldList = insertedRecord.getFieldList();
		Set newFields = new LinkedHashSet( fieldList.getFields() ); // this contains FmField objects
		for( Iterator it = command.getAssignmentTerms().iterator(); it.hasNext(); ) {
			// remove fields which were assigned values in the SqlCommand
			AssignmentTerm theTerm = (AssignmentTerm)it.next();
			newFields.remove( theTerm.getField() );
		}
		for( Iterator it = newFields.iterator(); it.hasNext(); ) {
			FmField field = (FmField) it.next();
			int i = insertedRecord.getFieldList().indexOfFieldWithAlias( field.getAlias() );
			String generatedValue;
			generatedValue = insertedRecord.getRawValue( i );
			if( generatedValue != null && generatedValue.length() > 0 )//add this to exclude empty fields
				resultMap.put( field, generatedValue );
		}
		FmConnection connection = null;
		try {
			connection = (FmConnection)statement.getConnection();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		if( resultMap.size() == 0 ) {
			return new FmResultSet( null, null, connection );
		} else {
			int keysFound = 0;
			FmFieldList resultFieldList = new FmFieldList();
			for( Iterator it = resultMap.keySet().iterator(); it.hasNext(); ) {
				resultFieldList.add( (FmField)it.next() );
			}
			FmRecord resultRecord = new FmRecord( resultFieldList, null, null );
			for( Iterator it = resultMap.keySet().iterator(); it.hasNext(); ) {
				resultRecord.setRawValue( (String)resultMap.get( it.next() ), keysFound++ );
			}
			return new FmResultSet( Collections.singletonList( resultRecord ).iterator(), resultFieldList, connection );

		}
	}

	public void setParams( Vector params ) {
		this.params = new Vector( params );
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "Setting " + params.size() + " param(s)");
			/*
			for( int i = 0; i < this.params.size(); i++ ) {
				Object o = this.params.elementAt( i );
				System.out.println("    " +  i + " " + o );
			}
			*/
		}
	}

	FmStatement getStatement() {
		return statement;
	}

	String getSQL() {
		return command.getSql();
	}
}
