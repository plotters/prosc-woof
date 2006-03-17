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
 * Represents a field assignment in an UPDATE or INSERT query, where a field is being assigned a value.
 * @author sbarnum
 */
public class AssignmentTerm {
	private FmField field;
	private Object value;
	private boolean isPlaceholder;

	public AssignmentTerm(FmField field, Object value, boolean placeholder) {
		if ("null".equalsIgnoreCase(String.valueOf(value))) value = null;
		this.field = field;
		this.value = value;
		isPlaceholder = placeholder;
	}

	/** The field being set. */
	public FmField getField() {
		return field;
	}

	/** The value the field is being set to. */
	public Object getValue() {
		return value;
	}

	/**
	 * Whether the assignment uses a placeholder '?' in a prepared statement.
	 */
	public boolean isPlaceholder() {
		return isPlaceholder;
	}

}
