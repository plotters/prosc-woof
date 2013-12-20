package com.prosc.fmpjdbc;

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
 * Represents a single term in a WHERE clause in an SQL statement.
 * @author sbarnum
 */
public class SearchTerm {
	public static final int EQUALS = 0;
	public static final int GREATER_THAN = 1;
	public static final int GREATER_THAN_OR_EQUALS = 2;
	public static final int LESS_THAN = 3;
	public static final int LESS_THAN_OR_EQUALS = 4;
	public static final int NOT_EQUALS = 5;
	public static final int BEGINS_WITH = 6;
	public static final int ENDS_WITH = 7;
	public static final int CONTAINS = 8;
	protected static final int LIKE = 9;
	public static final int IS_NULL = 10;
	public static final int IS_NOT_NULL = 11;

	private final int operator;
	private final FmField field;
	private final Object value;
	private final boolean isPlaceholder;
	private final boolean specialTerm;

	public SearchTerm(FmField field, int operator, Object value, boolean isPlaceholder) {
		if (isPlaceholder && value!=null) throw new IllegalArgumentException("placeholder SearchTerms must use null for value.");
		if ("null".equalsIgnoreCase(String.valueOf(value))) value = null;
		this.field = field;
		
		specialTerm = field.getColumnName().length() > 0 && field.getColumnName().charAt(0) == '-';
		this.operator = specialTerm ? LIKE : operator; //LIKE searches don't modify the search terms at all, which is what we want for any special terms like -script or -script.param
		this.value = value;
		this.isPlaceholder = isPlaceholder;
	}

	/** The FmField being searched on. */
	public FmField getField() {
		return field;
	}

	/** The type of search argument for this SearchTerm. */
	public int getOperator() {
		return operator;
	}

	/** The value for this Term, or null if the value is NULL. */
	public Object getValue() { //FIX!! Should this return a String instead? When would it return a non-String value?
		return value;
	}

	/** Whether this Term is a placeHolder in a PreparedStatement. */
	public boolean isPlaceholder() {
		return isPlaceholder;
	}

	/** Returns true if this is a special filemaker term like -script, -script.param, or anything else that starts with a hyphen. */
	public boolean isSpecialTerm() {
		return specialTerm;
	}
}
