package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Apr 20, 2005 Time: 10:38:42 PM
 */
public class PassthruDriverTests extends TestCase {
	private Connection connection;
	private Statement statement;

	protected void setUp() throws Exception {
		connection = new JDBCTestUtils().getConnection();
		statement = connection.createStatement();
	}

	protected void tearDown() throws Exception {
		statement.close();
		connection.close();
	}

	public void testNothing() {}

	//Test joins, table aliasing

	//Test table alteration

	//Test functions embedded in SQL

	//Test HAVING, GROUP BY

	//Test mixed AND/OR searches
}
