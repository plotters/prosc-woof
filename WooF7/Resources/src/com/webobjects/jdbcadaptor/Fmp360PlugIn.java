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


package com.webobjects.jdbcadaptor;

import com.prosc.fmpjdbc.FmBlob;
import com.prosc.fmpjdbc.FileMakerException;
import com.prosc.woof7.FMPExpression;
import com.prosc.woof7.FMData;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.foundation.*;

import java.io.IOException;
import java.sql.*;
import java.util.Enumeration;

//FIX!! Need documentation - in progress, mostly done
//FIX!! Need windows support
//FIX! Need example project(s)
//FIX! Check into calculation support. Make sure that we're doing it the most efficient possible way.
//FIX!! Support search range operators like ..., >, �, etc.
//FIX!!! Support spaces in primary keys
//DONE! Cannot get value lists until the adaptor is instantiated. This is annoying, because many times, an application will initially read static value lists before doing any other operations.
//DONE!!! Fix problems with 'C' character in type code columnn messing up
//DONE!! Add expiration date
//DONE!! Change build product names to WooF

//OPTIMIZE: Store the filemaker record ID for updates

public class Fmp360PlugIn extends com.webobjects.jdbcadaptor.JDBCPlugIn {

	/**
	 * pseudo-deprecated This can't be a static variable, each plugin instance needs its own JDBCAdaptor reference.
	 */
	private JDBCAdaptor _storedAdaptor = null;


	public Fmp360PlugIn(JDBCAdaptor adaptor) {
		super(adaptor);
		if (_storedAdaptor == null) {
			_storedAdaptor = adaptor;
		}
		try {
			Class.forName("com.prosc.fmpjdbc.FmBlob");
		} catch (NoClassDefFoundError e) {
			String message = "WooF is not properly installed!  Make sure the fmp360_jdbc.jar is installed in the classpath.";
			System.err.println(message);
			throw new NoClassDefFoundError(message);
		} catch (ClassNotFoundException e) {
			String message = "WooF is not properly installed!  Make sure the fmp360_jdbc.jar is installed in the classpath.";
			System.err.println(message);
			throw new RuntimeException(e);
		}

		System.out.println( "*** Successfully created new FmproPlugIn instance ***" );
	}

	public Object fetchBLOB(ResultSet resultSet, int i, EOAttribute eoAttribute, boolean b) throws SQLException {
		Blob rawBlob = resultSet.getBlob(i);
		if( rawBlob == null ) return null;
		if( eoAttribute.className().endsWith("NSData") ) { //Convert to NSData
			try {
				return new FMData( rawBlob.getBinaryStream(), (int)rawBlob.length(), ((FmBlob)rawBlob).getMimeType() );
			} catch (IOException e) {
				System.err.println(e.getMessage());
				//Ignore and return the raw blob
			} catch ( NoClassDefFoundError e) {
				String message = "WooF does not appear to be properly installed!  Make sure the fmp360_jdbc.jar file is installed in the classpath.";
				System.err.println(message);
				throw new NoClassDefFoundError(message);
			}
		}
		return rawBlob;
	}

	public Object fetchCLOB(ResultSet resultSet, int i, EOAttribute eoAttribute, boolean b) throws SQLException {
		System.out.println("fetchCLOB (" + i + "), materialize: " + b);
		return super.fetchCLOB(resultSet, i, eoAttribute, b);
	}

	public String databaseProductName() {return "FileMaker Pro";}

	public String defaultDriverName() {return "com.prosc.fmpjdbc.Driver";}

	public Class defaultExpressionClass() {return FMPExpression.class;}

	/** Bypass the JDBC adaptor and access the FileMaker web companion directly to create a new record.
	 * This is necessary to get the primary key, which is not obtainable using JDBC. **/
	public NSArray newPrimaryKeys(int count, EOEntity entity, JDBCChannel channel) {
		//OPTIMIZE: Only do the setup stuff once and cache it in a dictionary
		//System.out.println("Need " + count + " new primary keys");
		//Jesse - 6/16/2002: fixed the # problem with primary keys. Using the java.net.URLEncoder seems to have taken care of it.
		//System.out.println("newPrimaryKeys: " + count + ", " + entity.name() + ", " + channel);
		NSMutableArray newKeys = new NSMutableArray();
		NSArray pkArray = entity.primaryKeyAttributes();
		if( pkArray.count() != 1 ) {
			//There is not a single primary key. Return a blank array.
			NSMutableDictionary emptyPK = new NSMutableDictionary(pkArray.count());
			for( Enumeration en = entity.primaryKeyAttributeNames().objectEnumerator(); en.hasMoreElements(); ) {
				emptyPK.setObjectForKey(new Integer(0), (String)en.nextElement());
			}
			for( int n = 0; n < count; n++ ) { newKeys.addObject( emptyPK ); }
			//System.out.println("newKeys: " + newKeys);
		} else {
			String pkColumnName = ((EOAttribute)pkArray.objectAtIndex(0)).columnName();
			String pkAttributeName = (String)entity.primaryKeyAttributeNames().objectAtIndex(0);
			EOAttribute eoAttribute = entity.attributeNamed(pkAttributeName);
			Class pkClass = null;
			if ("TEXT".equals(eoAttribute.externalType())) {
				pkClass = String.class;
			} else if ("NUMBER".equals(eoAttribute.externalType())) {
				pkClass = Integer.class;
			}
			String tableName = entity.externalName();
			ResultSet rs;

			Statement statement = null;
			try {
				Connection conn = ((JDBCContext)channel.adaptorContext()).connection();
				statement = conn.createStatement();
				for( int n=0; n < count; n++ ) {
					try {
						statement.executeUpdate("INSERT INTO \"" + tableName + "\"" );
					} catch( FileMakerException e ) {
						if( 509 == e.getErrorCode() ) throw new RuntimeException("FileMaker cannot generate primary keys for tables with required fields.", e);
						else throw new RuntimeException(e);
					}
					rs = statement.getGeneratedKeys();
					rs.next();
					//Object pkValue = rs.getObject( pkColumnName );
					//Integer pkValue = new Integer( rs.getInt( pkColumnName ) ); //FIX!! Make this smarter about different data type
					Object pkValue;
					if (pkClass == Integer.class) {
						pkValue = new Integer( rs.getInt(pkColumnName));
					} else if (pkClass == String.class) {
						pkValue = rs.getString(pkColumnName);
					} else {
						pkValue = rs.getObject(pkColumnName);
					}
					newKeys.addObject( new NSDictionary(pkValue, pkAttributeName) );
				}
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			} finally {
				try {
					if( statement != null ) statement.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return newKeys;
	}
}
