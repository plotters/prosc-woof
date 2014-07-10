package com.prosc.fmpjdbc;

import com.prosc.database.JDBCUtils;
import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FmMetaDataTest extends TestCase {

	private Connection connection;

	@Override
	protected void setUp() throws Exception {
		Class.forName("com.prosc.fmpjdbc.Driver");
		connection = DriverManager.getConnection("jdbc:fmp360://localhost/Itineris_Web", "Web", "insecur3");
	}

	public void testGetTables() throws Exception {
		_dump(connection.getMetaData().getTables("Itineris_Web", null, null, null));
	}

	public void testGetColumnsForOneTable() throws Exception {
		_dump(connection.getMetaData().getTables("Itineris_Web", null, "Error", null));
	}

	public void testGetColumnsForAllTables() throws Exception {
		_dump(connection.getMetaData().getTables("Itineris_Web", null, null, null));
	}

	public void testGetScripts() throws Exception {
		_dump(connection.getMetaData().getProcedures("Itineris_Web", null, null));
	}

	public void testGetSchemas() throws Exception {
		_dump(connection.getMetaData().getSchemas());
	}

	private void _dump(final ResultSet schemas) throws SQLException {
		System.out.println("Schemas:");
		JDBCUtils.dumpResultSet(schemas, System.out, false);
		System.out.println("");
	}
}