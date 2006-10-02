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
	public boolean use360driver;
	public String dbUsername;
	public String dbPassword;
	public String fmServer;
	public String xmlServer;
	public String dbName;
	public int fmVersion; //FIX! Make private
	public String debugLevel;

	//Not configured via properties
	public int port;
	public String driverClassName;
	public String catalogSeparator;

	public JDBCTestUtils() {
		fmVersion = Integer.valueOf( System.getProperty("fmVersion", "7") ).intValue();
		debugLevel = System.getProperty("debugLevel", "FINE");
		use360driver = Boolean.valueOf( System.getProperty("use360driver", "true") ).booleanValue();
		System.out.println("use 360 driver is set to : " + System.getProperty("use360driver") );
		dbUsername = System.getProperty("dbUsername", "wo");
		dbPassword = System.getProperty("dbPassword", "wo");
		fmServer= System.getProperty("fmServer", "orion.360works.com");
		xmlServer = System.getProperty("xmlServer", "orion.360works.com" );
		dbName = System.getProperty( "dbName", "Contacts" );
		port = Integer.valueOf(System.getProperty("portNumber", "80")).intValue();

		if( fmVersion < 7 ) {
			setFmVersion( 6 );
		}

		if(use360driver) {
			driverClassName = "com.prosc.fmpjdbc.Driver";
			if( port == 0 ) port = 80;
		} else {
			driverClassName = "com.ddtek.jdbc.sequelink.SequeLinkDriver";
			if( port == 0 ) port = 2399;
		}
	}

	public String getJdbcUrl(String whichDatabase) {
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


	public void assertIsSpeedy(int maxAverageExecutionTime, Runnable runnable) {
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

	/*public Connection getConnection() {
		return getConnection(dbName, dbUsername, dbPassword);
	}

	public Connection getConnection(String whichDatabaseName, String username, String password) {
		String jdbcUrl = getJdbcUrl(whichDatabaseName);
		try {
			Class.forName( driverClassName );
			Connection result = DriverManager.getConnection( jdbcUrl, username, password );
			return result;
		} catch( Exception e ) {
			throw new RuntimeException( "Could not connect to JDBC URL: " + jdbcUrl, e );
		}
	}*/

	public Connection getConnectionNoExceptions() {
		return getConnectionNoExceptions(dbName, dbUsername, dbPassword);
	}

	public Connection getConnection() throws SQLException {
		return getConnection(dbName, dbUsername, dbPassword);
	}


	public Connection getConnection(String whichDatabaseName, String username, String password) throws SQLException {
		String jdbcUrl = getJdbcUrl(whichDatabaseName);
		try {
			Class.forName( driverClassName );
			Connection result = DriverManager.getConnection( jdbcUrl, username, password );
			return result;
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException("Couldn't find the class: " + driverClassName);
		}
	}
	public Connection getConnectionNoExceptions(String whichDatabaseName, String username, String password) {
		try {
			return getConnection(whichDatabaseName, username, password);
		} catch( Exception e ) {
			throw new RuntimeException( "Could not connect to JDBC URL: " + getJdbcUrl(whichDatabaseName), e );
		}
	}

	public void setFmVersion( int i ) {
		fmVersion = 6;
		xmlServer="forge.360works.com";
		port = 4000;
		dbName = "";
	}
}
