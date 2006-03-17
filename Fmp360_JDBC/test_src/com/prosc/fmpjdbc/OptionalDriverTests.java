package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;

/**
 * These features are not necessary to implement the JDBC spec, but they would be nice
 */
public class OptionalDriverTests extends TestCase {
	private Connection connection;
	private Statement statement;

	protected void setUp() throws Exception {
		connection = JDBCTestUtils.getConnection();
		statement = connection.createStatement();
	}

	protected void tearDown() throws Exception {
		statement.close();
		connection.close();
	}
	//Test parsing script arguments from script names

	//Test metadata to determine which columns are calculation fields

	//Test batch insertions

	/** @TestFails This test currently fails; we have not implemented this feature yet. --jsb */
	public void testShowTablesNotImpl() throws SQLException {
		statement.executeQuery( "SHOW TABLES "); //FIX!! no operation string for "SHOW TABLES" in doParse() of SqlCommand
		ResultSet rs = statement.getGeneratedKeys();
		assertEquals( connection.getMetaData().getTables(null,null,null,null), rs );
	}

	/** @TestFails this test fails; we have not implemented this feature yet. --jsb */
	public void testDescribeNotImpl() throws SQLException {
		statement.executeQuery( "DESCRIBE Contacts"); //FIX!! no operation string for "DESCRIBE <table name>" in doParse() of SqlCommand
		ResultSet rs = statement.getGeneratedKeys();
		assertEquals( connection.getMetaData().getColumns(null,null,null,null), rs );
	}
}
