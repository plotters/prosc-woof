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
 * Represents a single sorting criteria in an ORDER BY clause in an SQL statement.
 * @author sbarnum
 */
public class SortTerm {
	public static final int ASCENDING = 1;
	public static final int DESCENDING = -1;

	private FmField field;
	protected int order;

	public SortTerm(FmField field, int order) {
		this.field = field;
		this.order = order;
	}

	public FmField getField() {
		return field;
	}

	public int getOrder() {
		return order;
	}


}
