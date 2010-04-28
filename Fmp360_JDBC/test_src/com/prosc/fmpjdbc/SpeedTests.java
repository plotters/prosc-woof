package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Apr 27, 2010 Time: 6:30:41 PM
 */
public class SpeedTests extends TestCase {
	private static final Logger log = Logger.getLogger( SpeedTests.class.getName() );

	private JDBCTestUtils jdbc7;

	protected void setUp() throws Exception {
		jdbc7 = new JDBCTestUtils();
	}

	public void testNoAuthConnection() throws Exception {
		benchmark( 5, "Establish incorrect connection", new Callable() {
			public Object call() throws Exception {
				try {
					jdbc7.getConnection( "Contacts", "", "" ).close();
				} catch( SQLException e ) {
					if( "Unauthorized".equals( e.getMessage() ) ) {
						//Ignore, this is correct
					} else fail("Should have failed, using wrong username/password");
				}
				return null;
			}
		} );
	}

	public void testDirectHttpGetNoAuth() throws Exception {
		benchmark( 5, "Direct HTTP GET connection", new Callable() {
			public Object call() throws Exception {
				URL url = new URL( "http://hermes.360works.com/fmi/xml/FMPXMLRESULT.xml?-db=Contacts&-lay=NoSuchTable&-view" );
				HttpURLConnection theConnection = (HttpURLConnection)url.openConnection();
				theConnection.setInstanceFollowRedirects( false );
				theConnection.setUseCaches( false );
				theConnection.getResponseCode();
				return null;
			}
		} );
	}

	public void testDirectHttpGetWrongAuth() throws Exception {
		benchmark( 5, "Direct HTTP GET connection", new Callable() {
			public Object call() throws Exception {
				URL url = new URL( "http://hermes.360works.com/fmi/xml/FMPXMLRESULT.xml?-db=Contacts&-lay=NoSuchTable&-view" );
				HttpURLConnection theConnection = (HttpURLConnection)url.openConnection();
				theConnection.addRequestProperty("Authorization", "Basic " + "abcxyz123");
				theConnection.setInstanceFollowRedirects( false );
				theConnection.setUseCaches( false );
				theConnection.getResponseCode();
				return null;
			}
		} );
	}

	public void testIncorrectConnection() throws Exception {
		benchmark( 5, "Establish incorrect connection", new Callable() {
			public Object call() throws Exception {
				try {
					jdbc7.getConnection( "Contacts", "wrongusername", "badpass" ).close();
				} catch( SQLException e ) {
					if( "Unauthorized".equals( e.getMessage() ) ) {
						//Ignore, this is correct
					} else fail("Should have failed, using wrong username/password");
				}
				return null;
			}
		} );
	}

	public void testCorrectAuthConnection() throws Exception {
		benchmark( 5, "Establish incorrect connection", new Callable() {
			public Object call() throws Exception {
				try {
					jdbc7.getConnection( "Contacts", "jello", "jello" ).close();
				} catch( SQLException e ) {
					if( "Unauthorized".equals( e.getMessage() ) ) {
						//Ignore, this is correct
					} else fail("Should have failed, using wrong username/password");
				}
				return null;
			}
		} );
	}

	private void benchmark(int numTries, String message, Callable<?> task) throws Exception {
		long startTime = System.currentTimeMillis();
		for( int n=0; n<numTries; n++ ) {
			task.call();
		}
		long endTime = System.currentTimeMillis();
		int avgTime = (int)( (endTime - startTime) / numTries );
		log.fine( "Average time to " + message + " " + numTries + " times: " + avgTime + "ms" );
	}
}
