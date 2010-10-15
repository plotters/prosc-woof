package com.prosc.fmpjdbc;

import java.sql.Time;
import java.util.TimeZone;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Oct 15, 2010 Time: 3:02:32 PM
 */
class TimeWithZone {
	Time time;
	TimeZone timeZone;

	TimeWithZone( Time time, TimeZone zone ) {
		if( time == null ) throw new IllegalArgumentException("time cannot be null");
		if( zone == null ) {
			throw new IllegalArgumentException("zone cannot be null");
		}
		this.time = time;
		this.timeZone = zone;
	}
}
