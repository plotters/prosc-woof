package com.prosc.fmpjdbc;

/**
 * @author sbarnum
 */

import junit.framework.*;
import com.prosc.fmpjdbc.FmResultSetMetaData;

import java.io.IOException;
import java.sql.*;

public class FmResultSetMetaDataTest extends TestCase {
	FmResultSetMetaData fmResultSetMetaData;

	public void testEmptyResultSetMetaData() throws Exception {
		Statement statement = new JDBCTestUtils().getConnection().createStatement();
		ResultSetMetaData meta = statement.executeQuery("select * from portrait where contactId = -1").getMetaData();
		assertEquals(meta.getColumnCount(), 0);
		//assertNull(meta.getColumnClassName(0));
		//assertNull(meta.getColumnLabel(0));
		//assertNull(meta.getTableName(0));
	}
	
	public void testGetTableOccurrenceForLayout() throws ClassNotFoundException, SQLException, IOException {
		Class.forName( "com.prosc.fmpjdbc.Driver" );
		Connection connection = DriverManager.getConnection( "jdbc:fmp360://localhost/Sandbox", "Admin", "" );
		FmMetaData metaData = (FmMetaData)connection.getMetaData();
		assertEquals( "BlankTableOccurrence", metaData.getTableOccurrenceForLayout( "Sandbox", "BlankLayout" ) );
	}
}