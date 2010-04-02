package com.prosc.fmpjdbc;

import junit.framework.*;

import java.sql.DriverManager;
import java.net.URL;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA. User: brian Date: Apr 20, 2005 Time: 4:57:57 PM To change this template use File | Settings
 * | File Templates.
 */
public class FMScriptExecutionTest extends TestCase {
	private FmConnection connection;
	private FmXmlRequest request;
	private static final String username = null;
	private static final String password = null;
	private static final String host = "orion.360works.com:80";
	private static final String driverClassName = "com.prosc.fmpjdbc.Driver";
	private String jdbcUrl;
	private FmStatement statement;

	protected void setUp() throws Exception {
		Class.forName( driverClassName );
		jdbcUrl = "jdbc:fmp360://" + host + "/Contacts";
		connection = (FmConnection) DriverManager.getConnection( jdbcUrl, username, password );
		Logger.getLogger("com.prosc.fmpjdbc").setLevel(Level.ALL);
		statement = (FmStatement) connection.createStatement();
	}

	protected void tearDown() throws Exception {
		statement.close();
		connection.close();
	}

	public void testFMScriptExecution() throws IOException, FileMakerException {
		//-db=employees&-lay=departments&-script=myscript&-findany
		if ( new JDBCTestUtils().use360driver ) {
			try {
				request = new FmXmlRequest(connection.getProtocol(), connection.getHost(), connection.getFMVersionUrl(),
						connection.getPort(), connection.getUsername(), connection.getPassword(), connection.getFmVersion());
				try {
					request.doRequest("-db=Contacts&-lay=Contacts&-max=0&-script=Create New Person&-findall");

				} finally {
					request.closeRequest();
				}
			} catch (IOException ioe) {
				throw ioe;
			}
		}
	}

}
