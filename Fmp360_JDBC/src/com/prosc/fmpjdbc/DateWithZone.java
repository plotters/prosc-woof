package com.prosc.fmpjdbc;

import java.util.TimeZone;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Oct 14, 2010 Time: 9:06:12 AM
 */
class DateWithZone {
	Date date;
	TimeZone timeZone;

	public DateWithZone( java.sql.Date date, TimeZone zone ) {
		this.date = date;
		this.timeZone = zone;
	}
}
