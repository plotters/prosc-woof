package com.prosc.fmpjdbc;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.*;
import java.sql.ResultSet;
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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 17, 2005 Time: 10:26:23 AM
 */
//FIX!!! Searches WHERE x=NULL do not appear to work correctly. Need a test case. --jsb
//FIX!!! Search WHERE x LIKE '=' to not appear to work either; you need to do x LIKE '==' to find null values.
public class StatementProcessor {
	private final static String DISPLAY_LAYOUT_MARKER="^^";
	private final static TimeZone defaultTimeZone = TimeZone.getDefault();
	private SqlCommand command;
	private FmStatement statement;
	private int updateRowCount = 0;
	//private FmRecord insertedRecord;
	private Vector params;
	private boolean is7OrLater = true;
	private final String encoding;
	private final String maxRecords;
	static final String WILDCARDS_EQUALS ="<>=�!?@#\"~*";
	//static final String WILDCARDS_LIKE ="<>=�!?@#\"~";// note: * is not included, because that does what it is supposed to for LIKE searches.
	static final String WILDCARDS_LIKE = null;// note: this is null b/c when using LIKE we would like to pass in search strings exactly like we would in FileMaker

	private static final String ESCAPE_C = "\\"; // escaped backslash.  Note, this should NOT be unicode encoded!
	//private FieldPosition sharedFieldPosition = new FieldPosition(0);
	private Logger logger = Logger.getLogger( StatementProcessor.class.getName() );
	private boolean returnGeneratedKeys = false;
	private FmResultSet results;
	private FmResultSet autoGeneratedKeys = null;

	public StatementProcessor( FmStatement statement, SqlCommand command ) {
		this.command = command;
		this.statement = statement;
		FmConnection connection = (FmConnection)statement.getConnection();
		is7OrLater = ((FmConnection)connection ).getFmVersion() >= 7;
		maxRecords = connection.getProperties().getProperty( "maxrecords", "all" );
		encoding = is7OrLater ? "UTF-8" : "ISO-8859-1";
	}

	/**
	 * List of valid FMP commands: -dbnames � -delete X -dup � -edit � -find, -findall, -findany -layoutnames � -new
	 * -process -script -view
	 */
	public void execute() throws SQLException {
		if (logger.isLoggable(Level.CONFIG)) {
			logger.log(Level.CONFIG, command.getSql());
		}

		if (command.getTable() == null) {
			throw new SQLException("No table was specified");
		}

		FmXmlRequest actionHandler = null;
		try {
			String dbLayoutString;
			String layout = getLayoutName();
			String displayLayout = null;
			int mark = layout.indexOf( DISPLAY_LAYOUT_MARKER );
			if( mark >= 0 ) {
				displayLayout = layout.substring( mark + DISPLAY_LAYOUT_MARKER.length() );
				layout = layout.substring( 0, mark );
			}
			if( layout != null ) layout = URLEncoder.encode( layout, "UTF-8" );
			if( displayLayout != null ) displayLayout = URLEncoder.encode( displayLayout, "UTF-8" );
			if( ( (FmConnection)statement.getConnection() ).getFmVersion() < 7 ) {
				dbLayoutString = "-db=" + URLEncoder.encode( getDatabaseName(), "UTF-8" );
				//String layoutName = getLayoutName();
				if( displayLayout == null ) displayLayout = layout; //FM6 only needs a displayLayout, it's OK to search for fields that don't exist on the layout
				if( displayLayout != null ) dbLayoutString += "&-lay=" + displayLayout;
				else logger.info( "Executing an SQL query without a layout name can be slow. Specify a layout name for best efficiency." );
			} else {
				dbLayoutString = "-db=" + URLEncoder.encode( getDatabaseName(), "UTF-8" ) + "&-lay=" + layout + ( displayLayout == null ? "" : "&-lay.response=" + displayLayout );
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
			/** Contains keys used in the where segment, used to identify duplicates, which is handy for doing ranged searches. */
			Map whereSegmentKeys = new HashMap( command.getSearchTerms().size() );

			Iterator recordIdIterator = null;
			for( Iterator it = command.getSearchTerms().iterator(); it.hasNext(); ) {
				SearchTerm eachTerm = (SearchTerm)it.next();
				if( "recid".equals( eachTerm.getField().getColumnName().toLowerCase() ) ) { //Throw away all other params, just use recid
					Object value = eachTerm.getValue();
					if( eachTerm.isPlaceholder() ) {
						value = params.elementAt(currentParam++);
					}
					whereClause = new StringBuffer( "&-recid=" + value );
					List<Long> recordIds = new LinkedList<Long>();
					recordIds.add( Long.valueOf( value.toString() ) );
					recordIdIterator = recordIds.iterator();
					break;
				}
				String fieldName = eachTerm.getField().getColumnName(); //FIX!! use fully qualified table names for related fields
				/**
				 * 0: the operator
				 * 1: the ampersand, field name, and equals sign
				 * 2: the value being searched for
				 * 3: operator code, one of the SearchTerm constants
				 */
				Object[] eachTermSegments = new Object[4]; // FIX! document this thing -ssb 2006-09-28
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
				if ("%3D".equals(eachTermSegments[2]) && (eachTerm.getOperator() == SearchTerm.EQUALS)) {
					eachTermSegments[2] = "%3D%3D"; // need two == signs for an exact empty match
				}

				/* This checks to see if the same term is used multiple times, and if it is, it
					 * checks to see if it's a range operation. If so, it re-adds the two search terms using a "..." notation.
					 * They are always added in the correct order so that the first term is the lower value and the second value is the higher value.
					 * This is necessary, since FM7 can't handle multiple operators for the same search term (FM6 could).
					 */
				Object[] matchingField = (Object[])whereSegmentKeys.get( fieldName );
				if( matchingField != null ) { //We've already used this field earlier in the qualifier
					eachTermSegments[0] = "";
					int op1 = ((Integer)matchingField[3]).intValue();
					if( (op1 == SearchTerm.GREATER_THAN || op1 == SearchTerm.GREATER_THAN_OR_EQUALS) && (operator == SearchTerm.LESS_THAN || operator == SearchTerm.LESS_THAN_OR_EQUALS ) ) {
						eachTermSegments[2] = matchingField[2] + "..." + eachTermSegments[2];
					} else if( (op1 == SearchTerm.LESS_THAN || op1 == SearchTerm.LESS_THAN_OR_EQUALS) && ( operator == SearchTerm.GREATER_THAN || operator == SearchTerm.GREATER_THAN_OR_EQUALS ) ) {
						eachTermSegments[2] = eachTermSegments[2] + "..." + matchingField[2];
					} else if (command.getLogicalOperator() != SqlCommand.OR) {
						throw new SqlParseException( "You cannot use the same search time twice unless they are being used with >, >=, <, or <= operators for ranges, or the logical operator is 'OR'.");
					} else {
						// this is an OR search.  copy the old term segment to a new location so it won't be overwritten by the next one.
						whereSegmentKeys.put(new Double(Math.random()), matchingField);
					}
				}

				whereSegmentKeys.put( fieldName, eachTermSegments );
			} // end of for loop is = command.getSearchTerms.getIterator()
			if( whereClause == null ) {
				whereClause = new StringBuffer();
				Object[] segments;
				for( Iterator it = whereSegmentKeys.values().iterator(); it.hasNext(); ) {
					segments = (Object[])it.next();
					whereClause.append( segments[0] );
					whereClause.append( segments[1] );
					whereClause.append( segments[2] );
				}
			}

			FmConnection connection = (FmConnection) statement.getConnection();
			actionHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
					connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
			Long recordId;

			boolean recordIdIsPreset;
			int rowCount = 0;
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
						postArgs.append( "&-max=" + maxRecords + "&-findall" );
					else {
						if( command.getLogicalOperator() == SqlCommand.OR ) postArgs.append( "&-lop=or" );
						postArgs.append( whereClause );
						if( command.getMaxRows() != null ) postArgs.append( "&-max=" + command.getMaxRows().intValue() );
						if( command.getSkipRows() != null ) postArgs.append( "&-skip=" + command.getSkipRows().intValue() );
						postArgs.append( "&-max=" + maxRecords + "&-find" );
					}

					actionHandler.setSelectFields(command.getFields()); // Set the fields that are used in the select statement
					actionHandler.doRequest( dbLayoutString + postArgs );

					results = new FmResultSet( actionHandler.getRecordIterator(), actionHandler.getFoundCount(), actionHandler.getFieldDefinitions(), statement, connection, actionHandler );
					// DO NOT CLOSE the request since the result set needs to stream the records
					break;


				case SqlCommand.UPDATE:

				{
					FmXmlRequest recIdHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
							connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
					recordIdIsPreset = ( recordIdIterator != null );
					if( recordIdIterator == null ) { //Might already be set if we passed in a record ID for the WHERE clause
						recIdHandler.doRequest( dbLayoutString + whereClause + "&-max=" + maxRecords + "&-find" );
						recordIdIterator = recIdHandler.getRecordIterator();
					}
					List updateRecords = null;
					if( returnGeneratedKeys ) {
						updateRecords = new LinkedList();
					}
					while( recordIdIterator.hasNext() ) {
						if( recordIdIsPreset ) {
							recordId = (Long)recordIdIterator.next();
						} else {
							recordId = ( (FmRecord)recordIdIterator.next() ).getRecordId();
						}
						actionHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
								connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
						try {
							actionHandler.doRequest( dbLayoutString + updateClause + "&-recid=" + recordId + "&-edit");
							for (Iterator iterator = actionHandler.getRecordIterator(); iterator.hasNext();) {
								Object o = iterator.next();
								if( updateRecords != null ) {
									updateRecords.add( o ); //If we're returning auto-generated keys, need to keep an in-memory record of all results
								}
								if (logger.isLoggable(Level.FINE)) {
									logger.log(Level.FINE, "Record was updated: " + o);
								}
								rowCount++;
							}
							actionHandler.closeRequest(); // the parsing thread should take care of this... but just in case it's taking too long
						} catch (RuntimeException e) {
							//actionHandler.closeRequest(); // the parsing thread should take care of this... but just in case it's taking too long
							updateRowCount = 0;
							throw e;
						}
					}
					recIdHandler.closeRequest();
					updateRowCount = rowCount;
					//recIdHandler.closeRequest();
					if( returnGeneratedKeys ) {
						autoGeneratedKeys = new FmResultSet( updateRecords.iterator(), actionHandler.getFoundCount(), actionHandler.getFieldDefinitions(), (FmConnection)statement.getConnection() );
					}

				}
				break;

				case SqlCommand.DELETE:
				{
					FmXmlRequest recIdHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
							connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
					recordIdIsPreset = ( recordIdIterator != null );
					if( recordIdIterator == null ) { //Might already be set if we passed in a record ID for the WHERE clause
						recIdHandler.doRequest( dbLayoutString + whereClause + "&-max=" + maxRecords + "&-find" );
						recordIdIterator = recIdHandler.getRecordIterator();
					}
					while( recordIdIterator.hasNext() ) {
						if( recordIdIsPreset ) {
							recordId = (Long)recordIdIterator.next();
						} else {
							recordId = ( (FmRecord)recordIdIterator.next() ).getRecordId();
						}
						actionHandler = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
								connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
						try {
							actionHandler.doRequest( dbLayoutString + "&-recid=" + recordId + "&-delete" );
						} catch (RuntimeException e) {
							throw e;
						} finally {
							actionHandler.closeRequest();
						}
						rowCount++;
					}
					recIdHandler.closeRequest();
					updateRowCount = rowCount;
					//recIdHandler.closeRequest();
				}
				break;

				case SqlCommand.INSERT:
					try {
						actionHandler.doRequest( dbLayoutString + updateClause + "&-new" ); //FIX!! What about exception handling?
					} catch( FileMakerException e ) {
						throw e;
					}
					updateRowCount = actionHandler.getFoundCount();
					if( returnGeneratedKeys ) { //FIX!! This will break previous backwards compatibility. Previously we filtered only the fields that changed, now we're just returning the raw ResultSet.
						autoGeneratedKeys = new FmResultSet( actionHandler.getRecordIterator(), updateRowCount, actionHandler.getFieldDefinitions(), statement, connection, actionHandler );
					} else {
						actionHandler.closeRequest();// inserts can only insert a single record, so we'll assume that the first record is the one that we want
					}
					break;
			}
		} catch( IOException e ) {
			SQLException e1 = new SQLException( e.toString(), "What is SQLState?", 42 ); //FIX!! Need better exceptions
			e1.initCause( e );
			throw e1;
		} catch( FileMakerException e ) {
			e.setConnection( (FmConnection)statement.getConnection() );
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
			StringBuffer buffer = new StringBuffer();
			if (isEqualsQualifier) {
				buffer.append("%3D"); // one encoded "equals" sign, signifying field content match. Used to use two, which is more precise, but horribly slow. --jsb
			}
			if (applyFormatter ) {
				// all the things we were checking for subclass java.util.Date, just check once.
				// FIX!!! need to URLEncode formatted values too -ssb
				if (value instanceof java.sql.Time) {
					value = ((DateFormat)FmRecord.timeFormat.get()).format(value);
				} else if (value instanceof java.sql.Timestamp) {
					value = ((DateFormat)FmRecord.timestampFormat.get()).format(value);
				} else if ( value instanceof java.sql.Date ) {
					DateFormat dateFormat = (DateFormat)FmRecord.dateFormat.get();
					dateFormat.setTimeZone( defaultTimeZone );
					value = dateFormat.format(value);
				} else if( value instanceof DateWithZone ) {
					Date date = ( (DateWithZone)value ).date;
					TimeZone tz = ( (DateWithZone)value ).timeZone;
					DateFormat dateFormat = (DateFormat)FmRecord.dateFormat.get();
					dateFormat.setTimeZone( tz );
					value = ((DateFormat) FmRecord.dateFormat.get()).format(date);
				} else if( value instanceof TimeWithZone ) {
					Date date = ( (TimeWithZone)value ).time;
					TimeZone tz = ( (TimeWithZone)value ).timeZone;
					DateFormat timeFormat = (DateFormat)FmRecord.timeFormat.get();
					timeFormat.setTimeZone( tz );
					value = timeFormat.format(date);
				} else if ( value instanceof java.util.Date ) {
					value = ((DateFormat) FmRecord.timestampFormat.get()).format(value);
				}
			}
			if (wildcardsToEscape != null) {
				String s = value == null ? "" : value.toString();
				//s = String.valueOf( value );
				StringBuffer escapedValue = new StringBuffer(s==null ? 1 : s.length());
				appendEscapedFmWildcards(s, escapedValue, wildcardsToEscape);
				buffer.append(URLEncoder.encode(escapedValue.toString(), encoding));
			} else if (value != null) {
				buffer.append(URLEncoder.encode(String.valueOf(value), encoding));
			}
			String result = buffer.toString();
			if( is7OrLater ) result = result.replaceAll( "%0A", "%0A%0D" ); //This seems to be necessary to properly insert carriage returns in FM7
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/*
	private String urlEncode( String key, String value ) {
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
	
	public void close() throws SQLException {
		if( autoGeneratedKeys != null ) {
			autoGeneratedKeys.close();
		}
		if( results != null ) {
			results.close();
		}
	}

	public ResultSet getGeneratedKeys() {
		if( autoGeneratedKeys != null ) {
			return autoGeneratedKeys;
		} else {
			throw new IllegalStateException("To retrieve generated keys, you must pass in the Statement.RETURN_GENERATED_KEYS parameter to the prepareStatement() or executeSql(String, int) method");
		}
		// generate a map of keys & values which have non-null values, but were not in the assignmentTerms for the SqlCommand.
		// these are the auto-generated items.

		/*
				Currently, we only support this for INSERT operations, not UPDATE. This is because INSERT is fairly simple, it is always a single record, and also any non-null item after an INSERT
				is probably a primary key or an auto-enter value. If we want to support UPDATE, we will need to hold a reference to all of the raw record data for all updated records, which could potentially
				be many records. Also, in an UPDATE, any existing values other than the ones set in the SQL will be returned, which means that you'll get a bunch of stuff other than auto-updated values.
				I do think it would be useful though to support UPDATES, because that's the only efficient way to get back calculation fields that changed as a result of the update. It is also important
				if we need to efficiently get the record modification count after each UPDATE (like for synchronization). --jsb
				 */
		/*Map resultMap = new LinkedHashMap( 3 );
		FmFieldList resultFieldList = new FmFieldList();
		FmConnection connection = (FmConnection)statement.getConnection();
		if( insertedRecord == null ) { //Return an empty record set. FIX!! Right now won't work with updates, only inserts. --jsb
			return new FmResultSet( Collections.EMPTY_LIST.iterator(), 0, resultFieldList, connection );
		}
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
		int keysFound = 0;
		for( Iterator it = resultMap.keySet().iterator(); it.hasNext(); ) {
			resultFieldList.add( (FmField)it.next() );
		}
		FmTable recIdTable = null; //FIX!! Will this break anything? Where should this value come from?
		FmField recIdField = new FmField( recIdTable, "recid", "recid", FmFieldType.RECID, false, true );
		resultFieldList.add( recIdField ); //Always include record id as an auto-generatetd key
		resultMap.put( recIdField, insertedRecord.getRecordId().toString() );

		FmRecord resultRecord = new FmRecord( resultFieldList, null, null );
		for( Iterator it = resultMap.keySet().iterator(); it.hasNext(); ) {
			resultRecord.setRawValue( (String)resultMap.get( it.next() ), keysFound++ );
		}

		return new FmResultSet( Collections.singletonList( resultRecord ).iterator(), 1, resultFieldList, connection );*/
		
	}

	public void setParams( Vector params ) {
		this.params = new Vector( params );
		if (logger.isLoggable(Level.FINER)) {
			logger.log(Level.FINER, "Setting " + params.size() + " param(s)");
		}
	}

	FmStatement getStatement() {
		return statement;
	}

	String getSQL() {
		return command.getSql();
	}

	public Object getParams() {
		return params;
	}

	public void setReturnGeneratedKeys( boolean returnGeneratedKeys ) {
		this.returnGeneratedKeys = returnGeneratedKeys;
	}
}
