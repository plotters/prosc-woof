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
		Connection connection = jdbc.getConnection( "NoSuchDatabase", "user", "password" );
		try {
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
