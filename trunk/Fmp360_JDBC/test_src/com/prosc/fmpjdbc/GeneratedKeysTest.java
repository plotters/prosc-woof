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

	private void runTest( Connection connection ) throws SQLException, InterruptedException {
		Statement statement = connection.createStatement();
		//statement.executeUpdate( "INSERT INTO " + tableName ); //The Actual driver requires you to specify something for the insert, so switched to the line below.
		statement.executeUpdate( "INSERT INTO " + tableName + "(" + fieldName + ") VALUES('Hi')", Statement.RETURN_GENERATED_KEYS );
		ResultSet rs = statement.getGeneratedKeys(); //Actual driver fails on this line
		
		//Now get the generated primary key
		rs.next();
		Object primaryKey = rs.getObject( "ID" );
		java.util.Date insertTime = rs.getTimestamp( "ModTimestamp" );
		assertNotNull( primaryKey );
		assertNotNull( insertTime );
		
		System.out.println( "Primary key is " + primaryKey );
		
		Thread.sleep( 1000 ); //To make sure that our timestamp changes
		
		statement.executeUpdate( "UPDATE " + tableName + " SET " + fieldName + "=Bye WHERE ID=" + primaryKey, Statement.RETURN_GENERATED_KEYS );
		rs = statement.getGeneratedKeys();
		rs.next();
		assertEquals( primaryKey, rs.getObject( "ID" ) );
		java.util.Date modTime = rs.getTimestamp( "ModTimestamp" );
		System.out.println( "Insert time: " + insertTime + " / Mod time: " + modTime );
		assertNotSame( insertTime.getTime(), modTime.getTime() );
	}
}
