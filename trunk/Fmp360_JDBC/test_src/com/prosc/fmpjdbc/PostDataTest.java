/**
 * @author sbarnum
 */

package com.prosc.fmpjdbc;

import junit.framework.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class PostDataTest extends TestCase {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		JDBCTestUtils.configureLogging();
	}

	public void testLargeEditOperation() throws Exception {
		final Driver driver1 = new Driver();
		Connection connection = DriverManager.getConnection("jdbc:fmp360://" + "localhost" + "/" + "FMCalDAV", "sam", "shmert");
		final String sql = "update event_mapping set " +
				"ICAL_DATA=?, " +
				"SUMMARY='Locked event?', " +
				"LOCATION='home', " +
				"ALARM_TYPE='DISPLAY', " +
				"START_TIME='19:00:00', " +
				"UUID='55BED072-304D-4393-B7F5-BD768A3A84E5', " +
				"END_DATE='09/13/2009', " +
				"END_TIME='20:00:00', " +
				"START_DATE='09/13/2009', " +
				"ALARM_TRIGGER_MINUTES=16 " +
				"where PKEVENT=56";
		final PreparedStatement ps = connection.prepareStatement(sql);
		//ps.setString(1, "BEGIN:VCALENDAR\n" +
		//		"PRODID:-//360Works//FMCalDAV 0.91//EN\n" +
		//		"VERSION:2.0\n" +
		//		"CALSCALE:GREGORIAN\n" +
		//		"BEGIN:VEVENT\n" +
		//		"SEQUENCE:6\n" +
		//		"DTSTART:20090913T190000\n" +
		//		"DTSTAMP:20090930T210102Z\n" +
		//		"SUMMARY:Locked event?\n" +
		//		"ATTENDEE;CN=FMCalDAV;CUTYPE=INDIVIDUAL;PARTSTAT=ACCEPTED:http://dorfl.loc\n" +
		//		" al:8081/360caldav/calendars/FMCalDAV/\n" +
		//		"ATTENDEE;CN=Sam Barnum;CUTYPE=INDIVIDUAL:mailto:sam@360works.com\n" +
		//		"ATTENDEE;CN=Bernadette Barnum;CUTYPE=INDIVIDUAL:mailto:bernadette@pmateam\n" +
		//		" .com\n" +
		//		"DTEND:20090913T200000\n" +
		//		"LOCATION:home\n" +
		//		"TRANSP:OPAQUE\n" +
		//		"UID:55BED072-304D-4393-B7F5-BD768A3A84E5\n" +
		//		"ORGANIZER;CN=FMCalDAV:http://dorfl.local:8081/360caldav/calendars/FMCalDA\n" +
		//		" V/\n" +
		//		"X-WR-ITIPSTATUSML:UNCLEAN\n" +
		//		"CREATED:20090930T205828Z\n" +
		//		"BEGIN:VALARM\n" +
		//		"X-WR-ALARMUID:72A020BE-4F58-44CE-AB4B-38C110C2D7B7\n" +
		//		"ACTION:DISPLAY\n" +
		//		"DESCRIPTION:\n" +
		//		"TRIGGER:-PT16M\n" +
		//		"END:VALARM\n" +
		//		"END:VEVENT\n" +
		//		"END:VCALENDAR\n" +
		//		"");
		for (int j=0; j<2; j++) {
			System.out.println("");
			ps.setString(1, "Query #" + j);
			System.out.println("=== QUERY_" + j + " ===");
			System.out.println("");
			int i = ps.executeUpdate();
			assertEquals(1, i);
		}
	}
}