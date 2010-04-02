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

 /* public void testSQLParsingFM6() throws Exception {
    JDBCTestUtils testUtils = new JDBCTestUtils();
    FmConnection c;
    Statement statement;
    ResultSet resultSet;


    // catalog = null
    testUtils.fmVersion = 6;
    testUtils.xmlServer = "forge.360works.com";
    testUtils.port = 4000;
    testUtils.dbName = null;

    c = (FmConnection) testUtils.getConnection();
    statement = c.createStatement();
    try {
      resultSet = statement.executeQuery("select * from contacts.portrait"); // db=contacts and lay=portrait
      fail("Contacts.portrait db and layout don't exist");
    } catch (FileMakerException e) {
      // this layout doesn't exist
    }

    resultSet = statement.executeQuery("select * from contacts"); // db=contacts
    assertTrue(resultSet.next());



    resultSet = statement.executeQuery("select * from portrait"); // db=portrait
    assertTrue(resultSet.next());


    try {
      resultSet = statement.executeQuery("select * from abc.portrait"); // db=abc and lay=portrait
      fail("abc.portrait doesn't exist");
    } catch (FileMakerException e) {
      // this db doesn't exist
    }


    // catalog = "Portrait" (the fm5 db has multiple layouts)
    testUtils = new JDBCTestUtils();
    testUtils.fmVersion = 6;
    testUtils.xmlServer = "forge.360works.com";
    testUtils.port = 4000;
    testUtils.dbName = "Portrait";


    c = (FmConnection) testUtils.getConnection();
    statement = c.createStatement();


    try {
      resultSet = statement.executeQuery("select * from contacts"); // db=Portrait and lay=contacts
      fail("Portrait.contacts doesn't exits");
    } catch (FileMakerException e) {
      // this layout doesn't exist passes
    }


    resultSet = statement.executeQuery("select * from portrait"); // db=Portrait and lay=portrait
    assertTrue(resultSet.next());

    try {
      resultSet = statement.executeQuery("select * from abc.portrait"); // db=abc and lay=portrait
      fail("abc.portrait doesn't exist");
    } catch (FileMakerException e) {
      // this db doesn't exist -- passes
    }


  }*/

  public void testSQLParsingFM7() throws Exception {
    JDBCTestUtils testUtils = new JDBCTestUtils();
    FmConnection c;
    Statement statement;
    ResultSet resultSet;


    // catalog = null
    testUtils.dbName = null;

    c = (FmConnection) testUtils.getConnection();
    statement = c.createStatement();
    resultSet = statement.executeQuery("select * from contacts.portrait"); // db=contacts and lay=portrait
    assertTrue(resultSet.next());

    try {
      resultSet = statement.executeQuery("select * from contacts"); // db=contacts -->throw SQLException
      fail("You must specify a database name in either the connection or the sql statement for FM7+");
    } catch(SQLException sqle) {
      System.out.println("Number 1 passed");// passes
    }

    try {
      resultSet = statement.executeQuery("select * from portrait"); // db=portrait -->throw SQLException
      fail("You must specify a database name in either the connection or the sql statement for FM7+");
    } catch (SQLException sqle) {
      System.out.println("Number 2 passed");// passes
    }

    try {
      resultSet = statement.executeQuery("select * from contacts.noLayout"); // db=contacts lay=noLayout
      fail("contacts.noLayout doesn't exits");
    } catch (FileMakerException fme) {
      // this layout doesn't exist
    }

    try {
      resultSet = statement.executeQuery("select * from abc.portrait"); // db=abc and lay=portrait
      fail("abc.portrait doesn't exist");
    } catch (FileMakerException fme) {
      // this db doesn't exist -- passes
    }


    // catalog = "Contacts"
    testUtils = new JDBCTestUtils();

    c = (FmConnection) testUtils.getConnection();
    statement = c.createStatement();

    resultSet = statement.executeQuery("select * from contacts.portrait"); // db=contacts and lay=portrait
    assertTrue(resultSet.next());


    resultSet = statement.executeQuery("select * from contacts"); // db=Contacts and lay=contacts
    assertTrue(resultSet.next());


    resultSet = statement.executeQuery("select * from portrait"); // db=Contacts and lay=portrait
    assertTrue(resultSet.next());

    try {
      resultSet = statement.executeQuery("select * from noLayout"); // db=Contacts and lay=noLayout
      fail("Contacts.noLayout doesn't exist");
    } catch (FileMakerException fme) {
      // this layout doesn't exist -- passes
    }

    try {
      resultSet = statement.executeQuery("select * from abc.portrait"); // db=abc and lay=portrait
      fail("abc.portrait doesn't exist");
    } catch (FileMakerException fme) {
      // this db doesn't exist -- passes
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