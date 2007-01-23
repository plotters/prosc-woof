package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.Statement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.Date;

/**
 * @author sbarnum
 */
public class PortalSelectTest extends TestCase {
	private Statement statement;

	public PortalSelectTest() throws SQLException {
		statement = new JDBCTestUtils().getConnection().createStatement();
	}

	public void testPortalSelect() throws Exception {
		ResultSet resultSet = statement.executeQuery("SELECT \"Contacts::ID\", \"Contacts::firstName\", \"Contacts::lastName\", description, name, state FROM cities WHERE name='Alpharetta'");
		resultSet.next();
		for (int i=1; i<=6; i++) {
			System.out.print(resultSet.getString(i) + "\t|\t");
		}
		System.out.println("");

		assertEquals("Alpharetta", resultSet.getString("name"));
		assertEquals("Alpharetta", resultSet.getString(5));
		//
		assertEquals("GA", resultSet.getString("state"));
		assertEquals("GA", resultSet.getString(6));
		//
		assertEquals("Al", resultSet.getString(2));
		assertEquals("Al", resultSet.getString("Contacts::firstName"));

	}

	public void testPortalUpdate() throws Exception {
		String description = "Updated on " + new Date().toString();
		statement.executeUpdate("UPDATE cities SET description='" + description + "' WHERE name='Alpharetta'");
		ResultSet resultSet = statement.executeQuery("SELECT * FROM cities WHERE name='Alpharetta'");
		resultSet.next();
		assertEquals(description, resultSet.getString("description"));
	}
}
