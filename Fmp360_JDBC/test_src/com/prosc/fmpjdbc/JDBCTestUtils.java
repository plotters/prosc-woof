package com.prosc.fmpjdbc;

import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author sbarnum
 */
public class JDBCTestUtils {

	//Configured via properties
	public static boolean use360driver;
	public static String dbUsername;
	public static String dbPassword;
	public static String fmServer;
	public static String xmlServer;
	public static String dbName;
	public static int fmVersion;
	public static String debugLevel;

	//Not configured via properties
	public static int port;
	public static String driverClassName;
	public static String catalogSeparator;

	static {
		fmVersion = Integer.valueOf( System.getProperty("fmVersion", "7") ).intValue();
		debugLevel = System.getProperty("debugLevel", "FINE");
		use360driver = Boolean.valueOf( System.getProperty("use360driver", "true") ).booleanValue();
		System.out.println("use 360 driver is set to : " + System.getProperty("use360driver") );
		dbUsername = System.getProperty("dbUsername", "wo");
		dbPassword = System.getProperty("dbPassword", "wo");
		fmServer= System.getProperty("fmServer", "orion.360works.com");
		xmlServer = System.getProperty("xmlServer", "orion.360works.com" );
		dbName = System.getProperty( "dbName", "Contacts" );

		if( fmVersion < 7 ) {
			xmlServer="forge.360works.com";
			port = 4000;
			dbName = "";
		}

		if(use360driver) {
			driverClassName = "com.prosc.fmpjdbc.Driver";
			if( port == 0 ) port = 80;
		} else {
			driverClassName = "com.ddtek.jdbc.sequelink.SequeLinkDriver";
			if( port == 0 ) port = 2399;
		}
	}

	public static String getJdbcUrl(String whichDatabase) {
		if( use360driver ) {
			StringBuffer result = new StringBuffer();
			result.append("jdbc:fmp360://" + xmlServer + ":" + port + "/");
			if( whichDatabase != null ) result.append( whichDatabase );
			result.append("?loglevel=" + debugLevel );
			result.append("&fmversion=" + fmVersion );
			if( catalogSeparator != null ) result.append("&catalogseparator=" + catalogSeparator );
			return result.toString();
		}
		else return "jdbc:sequelink://" + fmServer + ":" + port + ";serverDataSource=" + whichDatabase;
	}


	public static void assertIsSpeedy(int maxAverageExecutionTime, Runnable runnable) {
		int loopCount = 1000 / maxAverageExecutionTime;
		long sum = 0;
		for (int i=0; i< loopCount; i++) {
			long then = System.currentTimeMillis();
			runnable.run();
			long elapsed = System.currentTimeMillis() - then;
			if (elapsed > maxAverageExecutionTime) {
				System.err.println("Loop #" + i + " of " + loopCount + " execution time of " + elapsed + "ms exceeded maximum of " + maxAverageExecutionTime + "ms for " + runnable);
			}
			sum += elapsed;
		}
		long avg = sum/loopCount;
		if (avg > maxAverageExecutionTime) {
			throw new AssertionFailedError("Avg time of " + avg + "ms exceeded maximum of " + maxAverageExecutionTime + "ms for " + runnable);
		}
		System.out.println("Avg execution time for " + runnable + ": " + avg + "ms");
	}

	public static Connection getConnection() {
		return getConnection(dbName, dbUsername, dbPassword);
	}

	public static Connection getConnection(String whichDatabaseName, String username, String password) {
		String jdbcUrl = getJdbcUrl(whichDatabaseName);
		try {
			Class.forName( driverClassName );
			Connection result = DriverManager.getConnection( jdbcUrl, username, password );
			return result;
		} catch( Exception e ) {
			throw new RuntimeException( "Could not connect to JDBC URL: " + jdbcUrl, e );
		}
	}
}