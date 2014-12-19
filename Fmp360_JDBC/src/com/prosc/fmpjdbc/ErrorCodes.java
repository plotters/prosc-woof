package com.prosc.fmpjdbc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The JDBC specification wants you to use X/Open standard error codes as the SQLState parameter for an SQLException.
 * These are the standard codes for error conditions that are relevant to FileMaker.
 * These are taken from ftp://ftp.software.ibm.com/ps/products/db2/info/vr6/htm/db2m0/db2state.htm#HDRSTTMSG
 */
public class ErrorCodes {
	private static final Logger log = Logger.getLogger( ErrorCodes.class.getName() );
	
	//private static final Properties errorMessages = new Properties();
	
	public static final String AUTH_INVALID = "28000";
	
	//static {
	//	InputStream stream = FileMakerException.class.getResourceAsStream("ErrorCodes.txt");
	//	if( stream == null ) {
	//		log.warning( "Couldn't locate ErrorCodes.txt file; no human-readable error messages will be generated.");
	//	}
	//	else try {
	//		errorMessages.load(stream);
	//	} catch (IOException e) {
	//		log.log( Level.SEVERE, "Could not load error messages resource ErrorCodes.txt", e );
	//	} finally {
	//		try {
	//			stream.close();
	//		} catch (IOException e) {
	//			throw new RuntimeException(e);
	//		}
	//	}
	//}

	public static String getMessage( Integer errorCode ) {
		return getMessage(errorCode, Locale.getDefault());
	}

	public static String getMessage( Integer errorCode, Locale locale ) {
		ResourceBundle bundle;
		try {
			bundle = ResourceBundle.getBundle("com.prosc.fmpjdbc.errorcodes", locale);
		} catch (MissingResourceException e) {
			bundle = ResourceBundle.getBundle("com.prosc.fmpjdbc.errorcodes", new Locale("en"));
		}
		String key = String.valueOf(errorCode);
		String result = bundle.containsKey(key) ? bundle.getString(key) : null;
		if( result == null ) result = "Unknown error (" + errorCode + ")";
		return result;
	}

	public static boolean isTemporary( int errorCode ) {
		if( errorCode == 16 ) {
			//16, by definition, means 'try again'
			return true;
		} else if( errorCode >= 200 && errorCode <= 299 ) {
			//2xx range means permission problem. Assume that permissions will be corrected and the operation will be retried
			return true;
		} else if( errorCode >= 300 && errorCode <= 399 ) {
			//Everything in 3xx range is a temporary failure due to a user interaction, such as a locked record
			return true;
		} else if( isValidation( errorCode ) ) {
			//Validation failures will not succeed until we either change the validation rules or the data itself. I'm assuming that we are not changing the rules, so it's permanent until the source data changes.
			return false;
		} else if( errorCode >= 800 && errorCode <= 899 ) {
			//All 8xx codes are temporary failures due to system problems, such as hard drive being full
			return true;
		} else if( errorCode == 8003 ) {
			return true; //This is just like a 301; means record is in use
		} else {
			//Return true for everything else
			return true;
		}
	}
	
	public static boolean isValidation( int errorCode ) {
		return errorCode >= 500 && errorCode <= 599;
	}
}
