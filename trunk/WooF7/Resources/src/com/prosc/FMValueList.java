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

package com.prosc;

import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.jdbcadaptor.JDBCContext;
import com.webobjects.foundation.NSArray;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps values fetched from a valuelist on a Filemaker database and layout.
 * The valueList must appear on a Filemaker layout.
 * If the same field has several different valueLists on the Filemaker layout, this may return inaccurate results.
 * @author sbarnum
 * @deprecated WooF7 does not use this class. It might be added back in at some time though.
 */
public class FMValueList {
	/**
	 * The valuelist values.
	 */
	private NSArray values;



	/**
	 * Create a new FMValueList object using the layout specified by the LAYOUT key in the entity userInfo dictionary.
	 * @param ec The editing context of the entity.
	 * @param entity The entity whose valuelist is being fetched
	 * @param valueListName The name of the valueList in Filemaker.
	 * @throws SQLException if something goes wrong fetching the valueLists.
	 */
	public FMValueList(EOEditingContext ec, EOEntity entity, String valueListName) throws SQLException {
		this(ec, entity, valueListName, (String) entity.userInfo().objectForKey("LAYOUT"));
	}

	/**
	 * Create a new FMValueList object using a specific filemaker layout.
	 * @param ec The editing context of the entity.
	 * @param entity The entity whose valuelist is being fetched
	 * @param valueListName The name of the valueList in Filemaker.
	 * @param layoutName the name of the Filemaker layout where the valuelist is used.
	 * @throws SQLException if something goes wrong fetching the valueLists.
	 */
	public FMValueList(EOEditingContext ec, EOEntity entity, String valueListName, String layoutName) throws SQLException {
		String tableName = entity.externalName();
		if( layoutName == null ) {
			throw new IllegalArgumentException(entity.name() + " does not have a LAYOUT defined in the model file, and cannot reference a valueList.");
		}

		JDBCContext dbContext = (JDBCContext)EODatabaseContext.registeredDatabaseContextForModel(entity.model(), ec).adaptorContext();
		Connection connection = dbContext.connection();
		/* FIX!!! Do something here --jsb
		DatabaseMetaDataExt databaseMetaData = (DatabaseMetaDataExt)connection.getMetaData();
		FMPLayoutMetaData layoutMetaData = databaseMetaData.getLayoutMetaData(null, null, tableName, layoutName);
		values = new NSArray(layoutMetaData.getValueListValues(valueListName));*/
	}

	/**
	 * Returns the valuelist values.
	 * @return The valuelist values.
	 */
	public NSArray getValues() throws SQLException {
		return values;
	}

}
