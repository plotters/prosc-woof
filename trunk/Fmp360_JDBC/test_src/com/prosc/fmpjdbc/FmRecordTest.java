package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sbarnum
 */
public class FmRecordTest extends TestCase {
	private static final Logger log = Logger.getLogger(FmRecordTest.class.getName());
	
	public void testRepeatingReads() throws Exception {
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

	public void testAccessRestriction() throws Exception {
		final Properties properties = new Properties();
		properties.setProperty("user", "barb");
		properties.setProperty("password", "barb");
		FmConnection conn = new FmConnection("jdbc:fmp360://localhost/ContactSync", properties);
		//final String sql = "SELECT \"#ID\", \"Modification timestamp\", \"AlarmEmailRecipients\", \"AlarmTriggerMinutes\", \"AlarmTriggerTimestamp\", \"AlarmType\", \"createdBy\", \"Creation timestamp\", \"Description\", \"End Date\", \"End Time\", \"Location\", \"public\", \"Recurrence\", \"Start Date\", \"Start Time\", \"Summary\", \"Sync ID\", \"Timezone\", \"URL\" FROM \"Event\" ORDER BY \"#ID\"";
		final String sql = "SELECT * FROM \"Event\" ORDER BY \"#ID\"";
		final PreparedStatement ps = conn.prepareStatement(sql);
		final ResultSet rs = ps.executeQuery();
		final ResultSetMetaData metaData = rs.getMetaData();
		final int columnCount = metaData.getColumnCount();
		int maxKeyLength = 8;
		for (int i=1; i<=columnCount; i++) {
			maxKeyLength = Math.max(maxKeyLength, metaData.getColumnName(i).length());
		}
		while (rs.next()) {
			System.out.println("Fetched record " + rs.getString("recid"));
			//assertEquals(1, rs.getInt("public"));
			//assertNotNull(rs.getObject("#ID"));
			for(int i=1; i<= columnCount; i++) {
				final Object object = rs.getObject(i);
				final String columnName = metaData.getColumnName(i);
				System.out.println(String.format("%" + maxKeyLength + "s = %s", columnName, String.valueOf(object).replaceAll("\n", "\\n")));
			}
		}
	}

	public void testGetTime() throws Exception {
		final FmFieldList fieldList = new FmFieldList(new FmField(new FmTable("foo"), "time", null));
		final FmRecord fmRecord = new FmRecord(fieldList, "1", 1L);
		fmRecord.addRawValue("01:23:45", 0);
		final TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");
		final Time time = fmRecord.getTime(0, 1, timeZone);
		DateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.US);
		format.setTimeZone(timeZone);
		assertEquals("01:23:45", format.format(time));

	}
}
