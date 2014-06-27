package com.prosc.fmpjdbc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The JDBC specification wants you to use X/Open standard error codes as the SQLState parameter for an SQLException.
 * These are the standard codes for error conditions that are relevant to FileMaker.
 * These are taken from ftp://ftp.software.ibm.com/ps/products/db2/info/vr6/htm/db2m0/db2state.htm#HDRSTTMSG
 */
public class ErrorCodes {
	private static final Logger log = Logger.getLogger( ErrorCodes.class.getName() );
	
	private static final Properties errorMessages = new Properties();
	
	public static final String AUTH_INVALID = "28000";
	
	static {
		InputStream stream = FileMakerException.class.getResourceAsStream("ErrorCodes.txt");
		if( stream == null ) {
			log.warning( "Couldn't locate ErrorCodes.txt file; no human-readable error messages will be generated.");
		}
		else try {
			errorMessages.load(stream);
		} catch (IOException e) {
			log.log( Level.SEVERE, "Could not load error messages resource ErrorCodes.txt", e );
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static String getMessage( Integer errorCode ) {
		String result = errorMessages.getProperty( String.valueOf(errorCode) );
		if( result == null ) result = "Unknown error";
		return result;
	}
}
