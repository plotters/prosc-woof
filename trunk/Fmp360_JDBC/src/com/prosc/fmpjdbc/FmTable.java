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
 * Represents a Filemaker layout, so the FmStatement class knows how to construct the URL for a table.  This is used primarily to uniquely identify FmField objects with identical names.
 * Much of the previous functionality of FmTable has been moved to {@link FmFieldList}.
 * @see FmFieldList
 * @author sbarnum
 */
public class FmTable {
	private String name;
	private String alias;
	private String databaseName;

	/**
	 * Create a new table object.
	 * @param name The name of the table, and an optional prefix containing the database name followed by a period.
	 * <p>
	 * Example: perform a SELECT on the person layout of the staff.fp7 database.
	 * <code>SELECT t0.firstName f0, t0.lastName f1 FROM staff.person t0</code>
	 */
	public FmTable(String name) {
		int index = name.indexOf('.');
		if (index == -1) {
			index = name.indexOf('|');
		}
		if (index > 0) {
			this.databaseName = name.substring(0, index);
			this.name = name.substring(index+1);
		} else {
			this.name = name;
		}
	}

	/**
	 * Create a new table object which uses an alias.
	 * @param name The name of the table, and an optional prefix containing the database name followed by a period.
	 * @param alias the table alias, or null if no table alias is used.
	 */
	public FmTable(String name, String alias) {
		this(name);
		this.alias = alias;
	}

	/** Returns the name of the table. */
	public String getName() {
		return name;
	}

	/** This is a placeholder for planned support for table aliasing using JOIN queries. Currently always returns the name of the table. */
	public String getAlias() {
		return alias == null ? getName() : alias;
	}

	/** Whether the other table object has the same name as this table object. */
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof FmTable)) return false;

		final FmTable fmTable = (FmTable) o;

		if (!name.equals(fmTable.name)) return false;

		return true;
	}

	/** Hashcode only uses the table name */
	public int hashCode() {
		return name.hashCode();
	}

	/** Returns the databaseName specified for this table, or null if no databaseName was specified. */
	public String getDatabaseName() {
		return databaseName;
	}
}
