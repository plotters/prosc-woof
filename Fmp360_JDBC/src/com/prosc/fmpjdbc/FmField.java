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

import org.jetbrains.annotations.Nullable;

/**
 * @author sbarnum
 */
public class FmField {

	/** Where this field lives. */
	private FmTable table;
	/** The column name in the database. */
	private String columnName;
	/** The alias in the SQL command, or <code>null</code> if no alias was specified. */
	private String alias;
	/** The type of field.  This may be modified after the field is created. */
	private FmFieldType type;
	/** Whether the field can be assigned NULL. */
	private boolean isNullable = true;
	/** Whether the field is read-only. */
	private boolean readOnly = false;
	private boolean autoEnter;
	private int maxReps;
	private boolean global;
	private boolean isCalc;
	private boolean isSummary;

	/** Creates an FMField without any metadata.
	 * This method is usually called by SqlCommand, during parsing of SQL query strings.
	 * Fields created using this constructor have a type of <code>null</code> and isNullable set to <code>true</code>,
	 * these values should be set later during parsing the XML header response from a filemaker query.
	 * @param table The table
	 * @param name The column name, including any repetition index.
	 * @param alias The alias, or null if no alias is used.
	 */
	public FmField(FmTable table, String name, String alias) {
		this(table, name, alias, null, true);
	}

	/**
	 * Complete constructor for FmField.
	 * @param table The table
	 * @param name The column name, including any repetition index.
	 * @param alias The alias, or null
	 * @param type The type of field
	 * @param isNullable Whether null values are allowed.
	 */
	public FmField(FmTable table, String name, String alias, @Nullable FmFieldType type, boolean isNullable) {
		this(table, name, alias, type, isNullable, false, false, 1, false, false, false);
	}

	/**
	 * Complete constructor for FmField.
	 * @param table The table
	 * @param name The column name
	 * @param alias The alias, or null
	 * @param type The type of field
	 * @param isNullable Whether null values are allowed.
	 * @param readOnly Whether the field is readonly (calculation)
	 */
	public FmField(FmTable table, String name, String alias, FmFieldType type, boolean isNullable, boolean readOnly, boolean autoEnter, int maxReps, boolean global, boolean isCalc, boolean isSummary ) {
		this.table = table;
		this.columnName = name;
		this.alias = alias;
		this.type = type;
		this.isNullable = isNullable;
		this.readOnly = readOnly;
		this.autoEnter = autoEnter;
		this.maxReps = maxReps;
		this.global = global;
		this.isCalc = isCalc;
		this.isSummary = isSummary;
	}

	/**
	 * Returns the table containing this field.
	 */
	public FmTable getTable() {
		return table;
	}

	public void setTable(FmTable table) {
		this.table = table;
	}

	/**
	 * The name of the column in the Filemaker table.
	 */
	public String getColumnName() {
		return columnName;
	}

	/**
	 * Returns the alias if one was used for this FmField, otherwise returns the columnName.
	 * This should never return null.
	 */
	public String getAlias() {
		return alias == null ? columnName : alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public boolean isAutoEnter() {
		return autoEnter;
	}
	
	/** A field is a primary key candidate if it has an auto-enter value and is validated. 
	 * It would be more conclusive if we also checked for a unique value validation, but FileMaker's XML web publishing doesn't tell us that. */ 
	public boolean isPrimaryKeyCandidate() {
		return autoEnter && !isNullable;
	}

	/** A field is a modification timestamp candidate if it has an auto-enter value and is a timestamp. This could definitely be wrong (for example, it could be a creation timestamp), but it's the best
	 * we can do given the limited information we get from FileMaker's XML. It would be a good idea to check for the word 'mod' somewhere in the field name.
	 * @return
	 */
	public boolean isModstampCandidate() {
		return autoEnter && type == FmFieldType.TIMESTAMP;
	}

	public void setNullable(boolean nullable) {
		isNullable = nullable;
	}

	public FmFieldType getType() {
		return type;
	}

	public void setType(FmFieldType type) {
		this.type = type;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public int getMaxReps() {
		return maxReps;
	}

	public boolean isGlobal() {
		return global;
	}

	public boolean isCalculation() {
		return isCalc;
	}

	public boolean isSummary() {
		return isSummary;
	}

	public String toString() {
		return "FmField{" +
		        "table=" + table +
		        ", columnName='" + columnName + "'" +
		        "}";
	}


	/** Equals and hashcode methods do not look at field types or isNulllable, only the table name and column name. */
	/*
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof FmField)) return false;

		final FmField fmField = (FmField) o;

		if (!columnName.equals(fmField.columnName)) return false;
		//if (!table.equals(fmField.table)) return false; //

		return true;
	}
*/
	/** Equals and hashcode methods do not look at field types or isNulllable, only the table name and column name. */
	/*
	public int hashCode() {
		int result;
		result = table.hashCode();
		result = 29 * result + columnName.hashCode();
		return result;
	}
*/

	//Fix!!! Change equals and hashCode back to include table when we get the parsing working
	/** Equals and hashcode methods do not look at field types isNulllable, or table name only the column name. for now */
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof FmField)) {
			return false;
		}

		final FmField field = (FmField)o;

		if (columnName != null ? !columnName.equals(field.columnName) : field.columnName != null) {
			return false;
		}

		return true;
	}

	//Fix!!! Change equals and hashCode back to include table when we get the parsing working
	/** Equals and hashcode methods do not look at field types isNulllable, or table name only the column name. for now */
	public int hashCode() {
		return (columnName != null ? columnName.hashCode() : 0);
	}

}
