package com.prosc.fmpjdbc;

import java.sql.*;
import java.util.Properties;
import java.net.MalformedURLException;

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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 16, 2005 Time: 5:44:02 PM
 */
public class Driver implements java.sql.Driver {
	static {
		try {
			DriverManager.registerDriver( new Driver() );
		} catch( SQLException e ) {
			throw new RuntimeException( e );
		}
	}

	/** Creates a new {@link FmConnection} by passing the parameters into the constructor of the Connection. */
	public Connection connect( String url, Properties properties ) throws SQLException {
		try {
			return new FmConnection(url, properties);
		} catch( MalformedURLException e ) {
			SQLException sqlException = new SQLException( url + " is not a valid JDBC URL (" + e.getMessage() + "). URL's should be in the format: jdbc:fmp360://hostname:portnumber/databasename?property1=value1&property2=value2" );
			sqlException.initCause(e);
			throw sqlException;
		}
	}

	/** Returns true if the word 'fmp360' appears in the URL. */
	public boolean acceptsURL( String s ) throws SQLException {
		return ( s != null && s.indexOf("fmp360") > -1 );
	}

	/** Alwasy returns an empty array. This method is unnecessary for basic operation; it is an optional
	 * method to allow a GUI to prompt a user for more required connection parameters. */
	public DriverPropertyInfo[] getPropertyInfo( String s, Properties properties ) throws SQLException {
		return new DriverPropertyInfo[0];
	}

	public int getMajorVersion() {
		return 1;
	}

	public int getMinorVersion() {
		return 0;
	}

	/** Returns false, since there are many aspects of standard ANSI SQL that we will not be supporting. */
	public boolean jdbcCompliant() {
		return false;
	}
}
