package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Properties;

/**
 * @author sbarnum
 */
public class FmRecordTest extends TestCase {
	public void testRepeatingReads() throws Exception {
		Thread.sleep(300L);
		final Properties properties = new Properties();
		properties.setProperty("user", "Admin");
		properties.setProperty("password", "1200");
		FmConnection conn = new FmConnection("jdbc:fmp360://localhost/Library", properties);

		/*
		final PreparedStatement ps = conn.prepareStatement("select * from borrower where id=16124");
		final ResultSet rs = ps.executeQuery();
		rs.next();
		assertEquals(16124, rs.getInt("id"));
		Object[] repeating = (Object[]) rs.getArray("repeatingNumbers").getArray();
		assertEquals("1", repeating[0].toString());
		assertEquals("2", repeating[1].toString());
		*/

		System.out.println("Select * from gratuitousJoin");
		final ResultSet gj = conn.prepareStatement("select * from GratuitousJoin order by borrowerId ASC, groupId ASC").executeQuery();
		while(gj.next()) {
			final Object object1 = gj.getObject(1);
			final Object object2 = gj.getObject(2);
			final Object object3 = gj.getObject(3);
			System.out.println("Fetched record from GratuitousJoin: " + Arrays.asList(object1, object2, object3));
		}

	}
}
