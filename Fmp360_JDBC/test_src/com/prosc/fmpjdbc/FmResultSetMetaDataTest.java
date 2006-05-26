package com.prosc.fmpjdbc;

/**
 * @author sbarnum
 */

import junit.framework.*;
import com.prosc.fmpjdbc.FmResultSetMetaData;

import java.sql.Statement;
import java.sql.ResultSetMetaData;

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
}