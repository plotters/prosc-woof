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

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSLog;

import java.sql.SQLException;

/**
 * Abstract superclass for various types of Filemaker Input components which pull the list of options from a Filemaker ValueList.
 * @deprecated WooF7 does not use this class. It might be added back in at some time though.
 */
public abstract class FMValueListInput extends WOComponent {
    public String aString;
	private NSArray valueListValues;

    public FMValueListInput(WOContext context) {
        super(context);
    }
	public boolean synchronizesVariablesWithBindings() {
		return false;
	}

	protected EOEditingContext ec() {
		EOEditingContext ec = (EOEditingContext)valueForBinding("editingContext");
		if (ec!=null) {
			return ec;
		}
		return session().defaultEditingContext();
	}

	protected String entityName() {
		return (String)valueForBinding("entityName");
	}

	protected EOEntity valueListEntity() {
		return EOUtilities.entityNamed(ec(), entityName());
	}

	protected String valueListName() {
		return (String)valueForBinding("valueListName");
	}

	protected String layoutName() {
		return (String)valueForBinding("layout");
	}

	public NSArray valueListValues() {
		if (valueListValues==null) {
			String layout = layoutName();
			try {
				FMValueList valueList = null;
				if (layout != null) {
					valueList = new FMValueList(ec(), valueListEntity(), valueListName(), layout);
				} else {
					valueList = new FMValueList(ec(), valueListEntity(), valueListName());
				}
				valueListValues = valueList.getValues();
			} catch (SQLException e) {
				NSLog.err.appendln("FMValueListInput: Error fetching valueList items for " + valueListEntity() + ":" + valueListName());
				valueListValues = NSArray.EmptyArray;
			}
		}
		return valueListValues;
    }
}
