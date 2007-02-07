package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Apr 20, 2005 Time: 11:07:58 PM
 */
public class ExceptionTests extends TestCase {
	private JDBCTestUtils jdbc;
	private Connection connection;
	private Statement statement;

	protected void setUp() throws Exception {
		jdbc = new JDBCTestUtils();
		connection = jdbc.getConnection();
		statement = connection.createStatement();
	}

	protected void tearDown() throws Exception {
		statement.close();
		connection.close();
	}

	//Test for misformatted SQL
	public void testBadSQL() throws SQLException {

		try {
			statement.executeUpdate( "INSERT INTO CONTACTS (firstName, lastName, emailAddress) values('Fred', 'flintstone', 'fred@rubble.com'");
			fail("Should have thrown an exception; trailing ')' is missing.");
		} catch (SQLException e) {
			//Ignore - this is correct
		}
	}

	public void testMissingDatabase() throws SQLException {
		try {
			Connection connection = jdbc.getConnection( "NoSuchDatabase", "user", "password" );
			fail("You should not be able to get a connection to a non-existant database");
		} catch (SQLException sqle) {
			// GOOD
		}

		try {
			Connection connection = jdbc.getConnection(null, null, null);
			// i should be able to connect to a null db, with no username/pwd
			connection.createStatement().executeQuery( "SELECT * FROM NoSuchTable" );
			fail("Should have gotten an FMException");
		} catch (SQLException e) {
			System.out.println(e);
		}
	}

	//Test missing remote server, IOException

	//Testing missing fields, both from the table and from the layout

	//Test missing table

	//Test incorrect database columnName
}
