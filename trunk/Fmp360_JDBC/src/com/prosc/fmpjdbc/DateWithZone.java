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
		if( date == null ) throw new IllegalArgumentException("date cannot be null");
		if( zone == null ) {
			throw new IllegalArgumentException("zone cannot be null");
		}
		this.date = date;
		this.timeZone = zone;
	}
}
