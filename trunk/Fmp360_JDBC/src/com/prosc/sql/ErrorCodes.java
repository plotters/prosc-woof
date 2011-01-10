package com.prosc.sql;

/**
 * The JDBC specification wants you to use X/Open standard error codes as the SQLState parameter for an SQLException.
 * These are the standard codes for error conditions that are relevant to FileMaker.
 * These are taken from ftp://ftp.software.ibm.com/ps/products/db2/info/vr6/htm/db2m0/db2state.htm#HDRSTTMSG
 */
public class ErrorCodes {
	public static final String AUTH_INVALID = "28000";
}
