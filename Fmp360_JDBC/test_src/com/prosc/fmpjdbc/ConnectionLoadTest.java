package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: jesse
 * Date: 1/3/14
 * Time: 11:27 AM
 */
public class ConnectionLoadTest extends TestCase {
	private static final Logger log = Logger.getLogger( ConnectionLoadTest.class.getName() );
	
	public void testRepeatedConnections() throws Exception {
		Class.forName( "com.prosc.fmpjdbc.Driver" );
		String url = "jdbc:fmp360://apollo.local/Tasks";
		Properties props = new Properties();
		props.put( "user", "admin" );
		props.put( "testCredentials", "always" );
		int count = 0;
		while( true ) {
			final Connection connection = DriverManager.getConnection( url, props );
			connection.close();
			if( ++count % 1000 == 0 ) {
				log.info( "Connected " + count + " times" );
			}
		}
	}
}
