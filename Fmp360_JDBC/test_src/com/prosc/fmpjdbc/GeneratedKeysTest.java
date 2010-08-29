package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.*;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Aug 24, 2010 Time: 5:45:49 AM
 */
public class GeneratedKeysTest extends TestCase {
	//You can keep these values to test with the 360Works server, or change to your own values to test internally
	
	String fmpHost = "fmsdb.360works.com"; //Only used for Actual driver
	String wpeHost = "fmswpe.360works.com"; //Only used for 360Works driver
	String dbName = "JDBCTest";
	String tableName = "JDBCTest";
	String fieldName = "TextField";
	String username = "Admin";
	String password = "";
	
	public void testGeneratedKeys360Driver() throws Exception {
		Class.forName( "com.prosc.fmpjdbc.Driver" );
		Connection connection = DriverManager.getConnection( "jdbc:fmp360://" + wpeHost + "/" + dbName, username, password );
		runTest( connection );
	}
	
	public void testGeneratedKeysActualDriver() throws Exception {
		Class.forName( "com.filemaker.jdbc.Driver" );
		Connection connection = DriverManager.getConnection( "jdbc:filemaker://" + fmpHost + "/" + dbName, username, password );
		runTest( connection );
	}

	private void runTest( Connection connection ) throws SQLException {
		Statement statement = connection.createStatement();
		//statement.executeUpdate( "INSERT INTO " + tableName ); //The Actual driver requires you to specify something for the insert, so switched to the line below.
		statement.executeUpdate( "INSERT INTO " + tableName + "(" + fieldName + ") VALUES('Hi')" );
		ResultSet rs = statement.getGeneratedKeys(); //Actual driver fails on this line
		
		//Now get the generated primary key
		rs.next();
		Object primaryKey = rs.getObject( 1 );
		assertNotNull( primaryKey );
		System.out.println( "Primary key is " + primaryKey );
	}
}
