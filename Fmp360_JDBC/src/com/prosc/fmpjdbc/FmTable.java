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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a Filemaker layout, so the FmStatement class knows how to construct the URL for a table.  This is used primarily to uniquely identify FmField objects with identical names.
 * Much of the previous functionality of FmTable has been moved to {@link FmFieldList}.
 * @see FmFieldList
 * @author sbarnum
 */
public class FmTable {
	private final String name;
	private final String alias;
	private final String databaseName;
	private final String originalName;

	/**
	 * Create a new table object with a null alias and a catalog separator of '.' and '|'
	 * @param name The name of the table, and an optional prefix containing the database name followed by a period.
	 * <p>
	 * Example: perform a SELECT on the person layout of the staff.fp7 database.
	 * <code>SELECT t0.firstName f0, t0.lastName f1 FROM staff.person t0</code>
	 */
	public FmTable(String name) {
		this( name, null, ".|" );
	}

	/**
	 * Create a new table object which uses an alias.
	 * @param name The name of the table, and an optional prefix containing the database name followed by a period.
	 * @param alias the table alias, or null if no table alias is used.
	 */
	public FmTable(String name, String alias) {
		this( name, alias, ".|" );
	}

	/** Create a new table object which uses a name, alias, and catalogSeparator.
	 * 
	 * @param catalogSeparators Each character in this string is used as a separator to distinguish the database name from the layout name. It is typically set to "|.". This
	 * means that a table named "MyDatabase|MyTable" or "MyDatabase.MyTable" will have a db name of MyDatabase and a layout name of MyTable.
	 */
	public FmTable( @NotNull String name, @Nullable String alias, @Nullable String catalogSeparators ) {
		this.alias = alias;
		
		this.originalName = name;
		int index = -1;
		for( char c : catalogSeparators.toCharArray() ) {
			index = name.indexOf( c );
			if( index >= 0 ) break;
		}
		if (index > 0) {
			this.databaseName = name.substring(0, index);
			this.name = name.substring(index+1);
		} else {
			this.databaseName = null;
			this.name = name;
		}
	}

	/** Returns the name of the table. */
	public String getName() {
		return name;
	}

	/** Returns the name exactly as it appears in the SQL without processing '.' or '|' characters. */
	public String getOriginalName() {
		return originalName;
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

	@Override
	public String toString() {
		return name;
	}
}
