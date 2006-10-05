package com.prosc.fmpjdbc;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

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
 * An ordered list of FmFields which may be referenced by index or alias (slower).
 * @author sbarnum
 */
public class FmFieldList {
	private List fields;
	public boolean wasNull;

	public FmFieldList() {
		this.fields = new ArrayList();
	}

	public void add(FmField field) {
		fields.add(field);
	}

	public FmField get(int index) {
		return (FmField) fields.get(index);
	}

	/** Returns the FmField objects contained in this FmFieldList. */
	public List getFields() {
		return fields;
	}

	public int size() {
		return fields.size();
	}

	//OPTIMIZE This could be faster
	public int indexOfFieldWithAlias(String alias) {
		int i=0;
		for (Iterator iterator = fields.iterator(); iterator.hasNext();) {
			FmField fmField = (FmField) iterator.next();
			if (fmField.getAlias().equalsIgnoreCase(alias)) return i;
			else i++;
		}
		//throw new IllegalArgumentException("No such field '" + alias + "'");
		return -1;
	}

	/**
	 * Returns the index of the field whose column name is case-insensitive equal to to the provided <code>columnName</code>.
	 * @param columnName The columnName to search for, with optional repetition index brackets.
	 * @return the index of the matching field, or -1 for no match.
	 */
	public int indexOfFieldWithColumnName(String columnName) {
		//FIX!! This is really, really slow - we should build a hashset of all of the fields instead.
		int i=0;
		for (Iterator iterator = fields.iterator(); iterator.hasNext();) {
			FmField fmField = (FmField) iterator.next();
			if (fmField.getColumnName().equalsIgnoreCase(columnName)) return i;

			else i++;
		}
		//throw new IllegalArgumentException("No such field '" + columnName + "'");
		return -1;
	}

	public Iterator iterator() {
		return fields.iterator();
	}

	public String toString() {
		return fields.toString();
	}
}
