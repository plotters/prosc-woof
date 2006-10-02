package com.prosc.clients.OnHoldMarketing;

import junit.framework.TestCase;

import java.sql.*;
import java.io.*;

import com.prosc.fmpjdbc.Driver;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Sep 1, 2006 Time: 3:28:54 PM
 */
public class InsertTest extends TestCase {
	private Connection conn;

	protected void setUp() throws Exception {
		Driver.class.getName(); //Initialize JDBC driver
		conn = DriverManager.getConnection( "jdbc:fmp360://localhost:591/?fmversion=6" );
	}

	protected void tearDown() throws Exception {
		conn.close();
	}

	public void testInsertRecord() throws SQLException {
		Statement s = conn.createStatement();
		String sql = "INSERT INTO \"production.defaultLayout\" (\"clientID\", \"distroMethod\", \"spaceMethod\", \"spaceLength\", \"actionID\", \"pathToProduction\", \"mixType\") VALUES('1095', 'Telink 700', 'AUTO', 'MATCH', '29652', 'm:\\audio\\AUDUBO12.wav', '0')";
		System.out.println( sql );
		s.execute( sql );
	}
}
