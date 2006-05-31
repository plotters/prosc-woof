/**
 * @author sbarnum
 */

package com.prosc.fmpjdbc;

import junit.framework.*;

import java.util.Properties;
import java.util.Vector;
import java.util.List;
import java.util.Iterator;
import java.net.MalformedURLException;
import java.sql.*;

public class FmConnectionTest extends TestCase {
	private JDBCTestUtils jdbc;

	protected void setUp() {
		jdbc = new JDBCTestUtils();
		// initialization code goes here
	}

	public void testDatabaseSpecified() throws Exception {
		// you should be able to specify a database name in the URL.
		// all queries will point to specific tables within the database.
		FmConnection fmConnection = new FmConnection(jdbc.getJdbcUrl("Contacts"), new Properties());// contacts.fp7
		Statement statement = fmConnection.createStatement();
		ResultSet resultSet = statement.executeQuery("select * from portrait");
		assertTrue(resultSet.next());
		ResultSetMetaData meta = resultSet.getMetaData();
		assertTrue(meta.getColumnCount() > 3);

		// now try again while explicitly specifying the database name
		resultSet = statement.executeQuery("select * from contacts.portrait");
		assertTrue(resultSet.next());
		meta = resultSet.getMetaData();
		assertTrue(meta.getColumnCount() > 3);

	}

	public void testDatabaseUnspecified() throws Exception {
		FmConnection fmConnection = new FmConnection(jdbc.getJdbcUrl(null), new Properties());// contacts.fp7
		Statement statement = fmConnection.createStatement();
		ResultSet resultSet = statement.executeQuery("select * from contacts.portrait");
		assertTrue(resultSet.next());
		ResultSetMetaData meta = resultSet.getMetaData();
		assertTrue(meta.getColumnCount() > 3);

		// now try again without specifying the database
		try {
			statement.executeQuery("select * from portrait");
			fail("Should throw an exception; no database specified");
		} catch (SQLException e) {
			// success
		}
	}

	public void testUrlParsing() throws Exception {
		try {
			new FmConnection("jdbc:fmp360://fms7.360works.com", new Properties());
			fail("Should throw MalformedURLException due to missing DB name");
		} catch (MalformedURLException e) {
			// success, missing DB name
		}
	}

	public void testMetaData() throws Exception {
		FmConnection connection = new FmConnection(jdbc.getJdbcUrl(null), new Properties());// contacts.fp7
		DatabaseMetaData metaData = connection.getMetaData();
		assertTrue(metaData.getDatabaseMajorVersion() > 5);
		assertTrue(metaData.getDatabaseProductName().toLowerCase().indexOf("filemaker") >= 0);
	}

	public void testGetCatalogs() throws Exception {
		FmConnection connection = new FmConnection(jdbc.getJdbcUrl(null), new Properties());// contacts.fp7
		DatabaseMetaData metaData = connection.getMetaData();
		ResultSet catalogs = metaData.getCatalogs();

		List catalogNames = new Vector();

		while (catalogs.next()) {
			String catalogName = catalogs.getString("TABLE_CAT");
			catalogNames.add(catalogName);
		}

		System.out.println("Catalogs:");
		for (Iterator i = catalogNames.iterator(); i.hasNext();) {
			Object catalog = i.next();
			System.out.println("     " + catalog);
		}

		System.out.println("Done.");


		assertTrue(catalogNames.size() > 0);
	}
}