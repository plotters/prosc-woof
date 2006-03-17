package com.prosc.fmpjdbc;

import java.util.*;

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
 * Object-oriented representation of a single SQL query. This can parse both prepared and unprepared (haha) SQL statements.
 * Created by IntelliJ IDEA. User: jesse Date: Apr 17, 2005
 */
public class SqlCommand {

	/*
	///////////////////////////////////////////////////////////////////////////
	//  DDL QUERY TYPES
	///////////////////////////////////////////////////////////////////////////
	*/
	public static final int ALTER = 1;
	public static final int CREATE_INDEX = 2;
	public static final int CREATE_TABLE = 3;
	public static final int CREATE_VIEW = 4;
	public static final int DROP_INDEX = 5;
	public static final int DROP_TABLE = 6;
	public static final int DROP_VIEW = 7;
	public static final int GRANT = 8;
	public static final int REVOKE = 9;

	/*
	///////////////////////////////////////////////////////////////////////////
	// LOGICAL OPERATOR DEFINITIONS
	///////////////////////////////////////////////////////////////////////////
	*/
	public static final int AND = 1;
	public static final int OR = 2;

	/*
	///////////////////////////////////////////////////////////////////////////
	//  DATA MANIPULATION QUERY TYPES
	///////////////////////////////////////////////////////////////////////////
	*/
	public static final int SELECT = 10;
	public static final int INSERT = 11;
	public static final int UPDATE = 12;
	public static final int DELETE = 13;

	/*
	///////////////////////////////////////////////////////////////////////////
	// INSTANCE VARIABLES
	///////////////////////////////////////////////////////////////////////////
	*/
	private String sql;
	private int operation;
	private int logicalOperator;

	/**
	 * Fields which were in the SELECT clause.
	 */
	private FmFieldList fields ;
	private Integer maxRows;
	private Integer skipRows;
	/**
	 * Map of tables, where the keys in the map are the table aliases, and values are the actual table objects.
	 */
	private FmTable table;

	private List searchTerms;
	private List sortTerms;
	private List assignmentTerms;

	public SqlCommand(String sql) throws SqlParseException {
		this( sql, null );
	}

	public SqlCommand(String sql, String catalogSeparator) throws SqlParseException {
		if (sql == null) throw new IllegalArgumentException("sql must not be null.");
		if (sql.length() < 8) throw new IllegalArgumentException("sql command is too short.");
		this.sql = sql;
		fields = new FmFieldList();
		sortTerms = new LinkedList();
		searchTerms = new LinkedList();
		assignmentTerms = new LinkedList();
		doParse();
	}

	/**
	 * Parses the raw SQL.
	 */
	private void doParse() throws SqlParseException {
		String operationString = sql.substring(0, 6).toUpperCase();
		Parser parser = null;
		if ("SELECT".equals(operationString)) {
			operation = SELECT;
			parser = new SelectParser();
		} else if ("INSERT".equals(operationString)) {
			operation = INSERT;
			parser = new InsertParser();
		} else if ("UPDATE".equals(operationString)) {
			operation = UPDATE;
			parser = new UpdateParser();
		} else if ("DELETE".equals(operationString)) {
			operation = DELETE;
			parser = new SelectParser(); // uses same syntax as selects
		} else {
			throw new SqlParseException("Unknown operation type for query: " + sql);
		}
		parser.doParse();
	}


	/*
	///////////////////////////////////////////////////////////////////////////
	// ACCESSORS
	///////////////////////////////////////////////////////////////////////////
	*/
	/**
	 * Returns the table being queried.
	 */
	public FmTable getTable() {
		// FIX! support for multiple tables not implemented, right now there's just one table.
		return table;
	}

	/**
	 * Returns the operation type of this SqlCommand, usually one of: SELECT, UPDATE, INSERT, DELETE.
	 */
	protected int getOperation() {
		return operation;
	}

	public int getLogicalOperator() {
		return logicalOperator;
	}

	public Integer getMaxRows() {
		return maxRows;
	}

	public Integer getSkipRows() {
		return skipRows;
	}

	/**
	 * Returns the original SQL string which was used to create this SqlCommand, if applicable.
	 */
	protected String getSql() {
		return sql;
	}

	/**
	 * Returns the array of SearchTerm objects used by the query.
	 */
	public List getSearchTerms() {
		return searchTerms;
	}

	/**
	 * Returns the List of {@link SortTerm} objects for the query.
	 */
	public List getSortTerms() {
		return sortTerms;
	}

	/**
	 * Returns the List of {@link AssignmentTerm} objects for the query (only applicable for UPDATE and INSERT queries).
	 */
	public List getAssignmentTerms() {
		return assignmentTerms;
	}

	/** Returns fields specified in the SELECT part of an SQL query. */
	public FmFieldList getFields() {
		return fields;
	}

	public String toString() {
		return "SqlCommand{" +
		        "table=" + table +
		        ", assignmentTerms=" + assignmentTerms +
		        ", sortTerms=" + sortTerms +
		        ", searchTerms=" + searchTerms +
		        "}";
	}


	private abstract class Parser {
		private static final short IN_SQLCODE = 0;
		private static final short IN_STRING = 1;
		private static final short IN_IDENTIFIER = 2;
		private static final short BACKSLASH = 3;

		public static final int SELECTED_FIELDS = 0;
		public static final int TABLES = 1;
		public static final int ASSIGNED_FIELDS = 2;
		public static final int ASSIGNED_VALUES = 3; // for inserts only, since the values are separate from the fields
		public static final int WHERE_CLAUSE = 4;
		public static final int ORDERBY_CLAUSE = 5;
		public static final int LIMIT = 6;

		int queryPart;

		String currentSearchTermFieldName = null;
		int currentSearchTermOperator = -1;
		FmField currentAssignmentField;
		private int nestedParentheses;

		void doParse() throws SqlParseException {
			StringBuffer buffer = new StringBuffer();
			int length = sql.length();
			int state = IN_SQLCODE;
			queryPart = getInitialQueryPart();
			nestedParentheses = 0;
			char c = 0;
			for (int i = 6; i < length; i++) {
				c = sql.charAt(i);
				switch (state) {
					case IN_SQLCODE:
						if (Character.isWhitespace(c)) {
							if (buffer.length() > 0) {
								handleWhitespaceTerminatedSqlCode(buffer);
							}
						} else if (c == ',') { // comma separating fields or tables or sortOrderings
							if (buffer.length() > 0) handleWhitespaceTerminatedSqlCode(buffer);
							handleComma();
						} else if (c == '\'') {  // start of String
							state = IN_STRING;
						} else if (c == '"') { // start of identifier
							state = IN_IDENTIFIER;
						} else if (c == '(') {// begin parenth block, maybe a function, or search grouping
							nestedParentheses++;
							handleOpeningParentheses();
							if (buffer.length() > 0) {
								handleWhitespaceTerminatedSqlCode(buffer);
							}
						} else if (c == ')') { // end parenth block
							handleClosingParentheses();
							nestedParentheses--;
							if (nestedParentheses < 0) throw new SqlParseException("Parentheses mismatch");
						} else if (c == '?') {
							if (buffer.length() > 0) throw new RuntimeException("placeholder shouldn't have any buffer text before it");
							handlePlaceholderCharacter();
						} else if (c == '>' || c == '<' || c == '=' || c == '!') {
							int operator = -1;
							char next = sql.charAt(i + 1);
							if (next == '=') {
								i++;
								if (c == '!')
									operator = SearchTerm.NOT_EQUALS;
								else if (c == '>')
									operator = SearchTerm.GREATER_THAN_OR_EQUALS;
								else if (c == '<') operator = SearchTerm.LESS_THAN_OR_EQUALS;
							} else if (next == '>') {
								if (c == '<') {
									i++;
									operator = SearchTerm.NOT_EQUALS; // a <> b
								} else
									throw new SqlParseException("Unexpected operator combination " + c + next + ": " + sql);
							} else if (c == '>') {
								operator = SearchTerm.GREATER_THAN;
							} else if (c == '<') {
								operator = SearchTerm.LESS_THAN;
							} else if (c == '=') {
								operator = SearchTerm.EQUALS;
							}

							if (buffer.length() > 0) {
								handleWhitespaceTerminatedSqlCode(buffer);
							}
							handleComparisonOperator(operator);
						} else {
							buffer.append(c);
						}
						break;
					case IN_STRING:
						if (c == '\'') {// end of a string
							handleString(buffer);
							state = IN_SQLCODE;
						} else if (c == '\\') {
							state = BACKSLASH;
						} else {
							buffer.append(c);
						}
						break;
					case IN_IDENTIFIER:
						if (c == '\"') {
							handleIdentifier(buffer);
							state = IN_SQLCODE;
						} else {
							buffer.append(c);
						}
						break;
					case BACKSLASH:
						state = IN_STRING;
						buffer.append(c); // append whatever was escaped
						break;

				}
			}
			if (buffer.length() > 0) {
				handleWhitespaceTerminatedSqlCode(buffer);
			}
			if (nestedParentheses != 0) throw new SqlParseException("Parentheses mismatch");
		}

		private void handleClosingParentheses() {
		}

		private void handleOpeningParentheses() {
		}

		/** Returns the queryPart which the statement is initially in, usually either SELECTED_FIELDS or TABLES */
		abstract int getInitialQueryPart();

		/** Handle some non-quoted string data in the query. */
		abstract void handleWhitespaceTerminatedSqlCode(StringBuffer buffer) throws SqlParseException;

		/** Called whenever an unquoted comma is encountered */
		abstract void handleComma() throws SqlParseException;

		/** Called whenever a '?' character is encountered. */
		void handlePlaceholderCharacter() throws SqlParseException {
			if (queryPart == ASSIGNED_FIELDS) {
				AssignmentTerm term = new AssignmentTerm(currentAssignmentField, null, true);
				assignmentTerms.add(term);
				currentAssignmentField = null;
			} else if (queryPart == WHERE_CLAUSE) {
				//FmField field = new FmField(table, currentSearchTermFieldName, null, FmFieldType.TEXT, true);
				FmField field = getFieldWithNameOrAlias(currentSearchTermFieldName);
				addSearchTerm( field, currentSearchTermOperator, null, true );
				currentSearchTermFieldName = null;
			} else {
				throw new SqlParseException("Unexpected character '?'");
			}
		}

		void handleComparisonOperator(int operator) throws SqlParseException {
			if (queryPart == WHERE_CLAUSE) {
				currentSearchTermOperator = operator;
			}
		}

		/** Handles some single-quoted string data, either part of an assignment term or a search term. */
		void handleString(StringBuffer buffer) throws SqlParseException {
			if (queryPart == ASSIGNED_FIELDS) {
				AssignmentTerm term = new AssignmentTerm(currentAssignmentField, buffer.toString(), false);
				assignmentTerms.add(term);
				currentAssignmentField = null;
			} else if (queryPart == WHERE_CLAUSE) {
				String string = buffer.toString();
				String lower = string.toLowerCase();
				parseWhereSql(string, lower);
			}
			buffer.setLength(0);
		}

		/** Handles some double-quoted string data, typically an SQL table or column which has characters or spaces that need to be escaped. */
		abstract void handleIdentifier(StringBuffer buffer) throws SqlParseException;

		/** Parse a string in the WHERE part of the query.
		 * @param lowercase This is the same thing as whereFragment, converted to lowercase. */
		void parseWhereSql(String whereFragment, String lowercase) throws SqlParseException {
			if ("order".equals(lowercase)) {
				queryPart = ORDERBY_CLAUSE;
			} else if ("limit".equals(lowercase)) {
				queryPart = LIMIT;
			} else if ("and".equals(lowercase)) {
				if (logicalOperator == OR) throw new SqlParseException("Cannot mix 'AND' and 'OR' queries: " + sql);
				logicalOperator = AND;
			} else if ("or".equals(lowercase)) {
				if (logicalOperator == AND) throw new SqlParseException("Cannot mix 'AND' and 'OR' queries: " + sql);
				logicalOperator = OR;
			} else if ("is".equals(lowercase)) {
				if (currentSearchTermFieldName != null) {
					currentSearchTermOperator = SearchTerm.EQUALS;
				} else {
					throw new SqlParseException("Unexpected token 'is' in query: " + sql);
				}
			} else if ("not".equals(lowercase)) {
				if (currentSearchTermOperator == SearchTerm.EQUALS) {
					currentSearchTermOperator = SearchTerm.NOT_EQUALS;
				} else {
					throw new SqlParseException("Unexpected token 'not' in query: " + sql);
				}
			} else if ("like".equals(lowercase)) {
				currentSearchTermOperator = SearchTerm.LIKE;
			} else {
				if (currentSearchTermFieldName == null) {
					currentSearchTermFieldName = whereFragment;
				} else {
					//FmField field = new FmField(table, currentSearchTermFieldName, FmField.TEXT, true);
					FmField field = getFieldWithNameOrAlias(currentSearchTermFieldName);
					if (currentSearchTermOperator == SearchTerm.LIKE) {
						if (whereFragment.length() == 0) {
							currentSearchTermOperator = SearchTerm.EQUALS;
						} else if (whereFragment.startsWith("%")) {
							if (whereFragment.length() == 1) {
								currentSearchTermOperator = SearchTerm.CONTAINS;
								whereFragment = "";
							} else if (whereFragment.endsWith("%")) {
								currentSearchTermOperator = SearchTerm.CONTAINS;
								whereFragment = whereFragment.substring(1, whereFragment.length() - 1);
							} else {
								currentSearchTermOperator = SearchTerm.ENDS_WITH;
								whereFragment = whereFragment.substring(1);
							}
						} else if (whereFragment.endsWith("%")) {
							currentSearchTermOperator = SearchTerm.BEGINS_WITH;
							whereFragment = whereFragment.substring(0, whereFragment.length() - 1);
						} else {
							currentSearchTermOperator = SearchTerm.EQUALS;
						}
					}
					addSearchTerm( field, currentSearchTermOperator, whereFragment, false );
					currentSearchTermFieldName = null;
				}
			}
		}

	}
	
	private void addSearchTerm(FmField field, int operator, String whereFragment, boolean isPlaceholder ) throws SqlParseException {
		searchTerms.add( new SearchTerm(field, operator, whereFragment, isPlaceholder) );
	}

	/** Returns the field from the fields list whose columnName or alias exactly matches the input string. */
	FmField getFieldWithNameOrAlias(String input) {
		int index = input.indexOf('.');
		String name = input;
		if (index > 0) {
			name = name.substring(index + 1);
		}
		for (Iterator iterator = fields.iterator(); iterator.hasNext();) {
			FmField selectFieldPlaceholder = (FmField) iterator.next();
			if (selectFieldPlaceholder.getColumnName().equals(name)) return selectFieldPlaceholder;
			if (selectFieldPlaceholder.getAlias().equals(name)) return selectFieldPlaceholder;
		}
		return new FmField(table, input, null, FmFieldType.TEXT, true);
	}

	/** Handles UPDATE operator queries. */
	private class UpdateParser extends Parser {
		void handleWhitespaceTerminatedSqlCode(StringBuffer buffer) throws SqlParseException {
			handleSqlCode(buffer);
		}

		int getInitialQueryPart() {
			return TABLES;
		}

		void handleComma() throws SqlParseException {
			// not interested for updates
		}


		void handleSqlCode(StringBuffer buffer) throws SqlParseException {
			String string = buffer.toString();
			String lowercase = string.toLowerCase();
			if (queryPart == TABLES) {
				if ("set".equals(lowercase)) {
					queryPart = ASSIGNED_FIELDS;
				} else {
					if (table == null) {
						table = new FmTable(string);
					} else {
						// table alias, not interested
					}
				}
			} else if (queryPart == ASSIGNED_FIELDS) {
				if ("where".equals(lowercase)) {
					queryPart = WHERE_CLAUSE;
				} else if ("order".equals(lowercase)) {
					queryPart = ORDERBY_CLAUSE;
				} else {
					if (currentAssignmentField == null) {
						currentAssignmentField = new FmField(table, string, null, FmFieldType.TEXT, true); // type is unknown
					} else {
						AssignmentTerm term = new AssignmentTerm(currentAssignmentField, string, false);
						assignmentTerms.add(term);
						currentAssignmentField = null;
					}
				}
			} else if (queryPart == WHERE_CLAUSE) {
				parseWhereSql(string, lowercase);
			}

			buffer.setLength(0);
		}

		void handleIdentifier(StringBuffer buffer) throws SqlParseException {
			handleSqlCode(buffer);
		}

	}

	private class InsertParser extends Parser {
		boolean didParseInto = false;
		List assignedFieldNames = new LinkedList();
		private Iterator assignedFieldNamesIterator;


		int getInitialQueryPart() {
			return TABLES;
		}

		void handleSqlCode(StringBuffer buffer) throws SqlParseException {
			String string = buffer.toString();
			String lower = string.toLowerCase();
			buffer.setLength(0);
			if (!didParseInto) {
				didParseInto = true;
				return; // first sqlCode is always "INTO"
			}
			if (queryPart == TABLES) {
				table = new FmTable(string);
				queryPart = ASSIGNED_FIELDS;
			} else if (queryPart == ASSIGNED_FIELDS) {
				if ("values".equals(lower)) {
					if (assignedFieldNames.size() == 0) {
						// need to get the field names from metadata!
						throw new SqlParseException("No field list was specified before values: " + sql);
					}
					assignedFieldNamesIterator = assignedFieldNames.iterator();
					queryPart = ASSIGNED_VALUES;
				} else {
					assignedFieldNames.add(string);
				}
			} else if (queryPart == ASSIGNED_VALUES) {
				String fieldName = (String) assignedFieldNamesIterator.next();
				FmField field = new FmField(table, fieldName, null, FmFieldType.TEXT, true);
				AssignmentTerm term = new AssignmentTerm(field, string, false);
				assignmentTerms.add(term);
			}
		}

		void handleWhitespaceTerminatedSqlCode(StringBuffer buffer) throws SqlParseException {
			handleSqlCode(buffer);
		}

		void handleComma() throws SqlParseException {
			// no special handling needed for INSERT statements
		}

		void handlePlaceholderCharacter() {
			if (queryPart == ASSIGNED_VALUES) {
				String fieldName = (String) assignedFieldNamesIterator.next();
				FmField field = new FmField(table, fieldName, null, FmFieldType.TEXT, true);
				AssignmentTerm term = new AssignmentTerm(field, null, true);
				assignmentTerms.add(term);
			}
		}

		void handleComparisonOperator(int operator) throws SqlParseException {
			// doesn't apply to inserts
		}

		void handleString(StringBuffer buffer) throws SqlParseException {
			handleSqlCode(buffer);
		}

		void handleIdentifier(StringBuffer buffer) throws SqlParseException {
			handleSqlCode(buffer);
		}
	}

	private class SelectParser extends Parser {
		private FmField currentSelectFieldPlaceholder;
		private SortTerm currentSortTerm;

		int getInitialQueryPart() {
			return SELECTED_FIELDS;
		}

		void handleSqlCode(StringBuffer buffer) throws SqlParseException {
			String string = buffer.toString();
			String lower = string.toLowerCase();
			buffer.setLength(0);
			if (queryPart == SELECTED_FIELDS) {
				if ("from".equals(lower)) {
					queryPart = TABLES;
				} else if ("as".equals(lower)) {
					// this is a field alias, ignore it
				} else if ("distinct".equals(lower)) {
					throw new SqlParseException("DISTINCT not supported: " + sql);
				} else {
					if (currentSelectFieldPlaceholder == null) {
						// found a new field name.  Might have an alias, we won't know until later.
						int index = string.indexOf('.');
						if (index > 0) string = string.substring(index+1); // FIX! ignoring leading table names for now, until we want to support JOIN queries
						currentSelectFieldPlaceholder = new FmField(table, string, null);
						fields.add(currentSelectFieldPlaceholder);
					} else {
						// string is a field alias
						currentSelectFieldPlaceholder.setAlias(string);
						currentSelectFieldPlaceholder = null; // get ready for the next field
					}
				}
			} else if (queryPart == TABLES) {
				if ("where".equals(lower)) {
					queryPart = WHERE_CLAUSE;
				} else if ("order".equals(lower)) {
					queryPart = ORDERBY_CLAUSE;
				} else if ("limit".equals(lower)) {
					queryPart = LIMIT;
				} else if (table == null) {
					table = new FmTable(string);
					for (Iterator iterator = fields.iterator(); iterator.hasNext();) {
						FmField selectFieldPlaceholder = (FmField) iterator.next();
						selectFieldPlaceholder.setTable(table);
					}
				} else {
					// table alias, not really interested
				}
			} else if (queryPart == WHERE_CLAUSE) {
				parseWhereSql(string, lower);
			} else if (queryPart == ORDERBY_CLAUSE) {
				if ("by".equals(lower)) {
					// this is the "BY" part of "ORDER BY", ignore it
				} else if (currentSortTerm != null && "asc".equals(lower)) {
					currentSortTerm.order = SortTerm.ASCENDING;
					currentSortTerm = null;
				} else if (currentSortTerm != null && "desc".equals(lower)) {
					currentSortTerm.order = SortTerm.DESCENDING;
					currentSortTerm = null;
				} else if ("limit".equals(lower)) {
					queryPart = LIMIT;
				} else {
					currentSortTerm = new SortTerm(getFieldWithNameOrAlias(string), SortTerm.ASCENDING);
					sortTerms.add(currentSortTerm);
				}
			} else if (queryPart == LIMIT) {
				if (maxRows == null) {
					maxRows = new Integer(string);
				} else if (skipRows == null) {
					skipRows = new Integer(string);
				} else {
					throw new SqlParseException("too many LIMIT arguments");
				}
			}
		}

		void handleWhitespaceTerminatedSqlCode(StringBuffer buffer) throws SqlParseException {
			handleSqlCode(buffer);
		}

		void handleComma() throws SqlParseException {
			if (queryPart == SELECTED_FIELDS) {
				if (currentSelectFieldPlaceholder != null) {
					// we know there is no field alias for this, since there was a comma after it
					currentSelectFieldPlaceholder = null;
				}
			}
		}

		void handleIdentifier(StringBuffer buffer) throws SqlParseException {
			handleSqlCode(buffer);
		}
	}


}

