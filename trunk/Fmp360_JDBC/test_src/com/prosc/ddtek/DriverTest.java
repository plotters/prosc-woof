package com.prosc.ddtek;

import junit.framework.TestCase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/*
Issues with current driver:
* Size is an issue - currently 632K. This might not seem too bad, except that a common use case would be to embed this into an applet which is loaded on a web page. Our JDBC driver is 104K, without the advantage of having some of the code running server-side.
* Installation process is currently very, very bad. It does not install it by default into the correct directory, and even if you specify the correct directory in the installer, it puts it inside a subfolder of that dir, which doesn't work. You have to manually know where the actual driver is (amid all the crap from the installer) and move it into the right place. It would be better to not even have an installer, and just drag files into the right place.

Wish list:
* Fix all failing test cases
* Work with either tables (not table occurrences) or layouts. Treat layouts as a view. If table and layout have same name, give table priority, but have some reserved 'magic word' syntax to specify that you're referring to a layout, not a table. Make it possible to get the list of fields on a layout.
* Make it easy to deploy Java applications on FileMaker Server. Enable port 8080 and/or map a URL like /fmi/tomcat to any webapp we drop into Tomcat.
* It would be nice if Statement.toString() showed the SQL value (instead of com.ddtek.jdbc.slbase.BasePreparedStatement@b398da), for logging purposes
* It would be nice if we got better error messages than "Parse Error in SQL"
* I don't see way to get the record ID or use it as a search criteria. This is not so important if indexed searches are just as fast as record IDs, but I have a feeling that record ID's are faster.

* 
Test cases to write:
* Table aliasing and relationships
*/

public class DriverTest extends TestCase {
	private static final Logger log = Logger.getLogger( DriverTest.class.getName() );

	String hostname = "jesse.360works.com";
	String dbName = "Contacts";
	private Connection conn;
	private String jdbcUrl;

	protected void setUp() throws Exception {
		Class.forName( "com.ddtek.jdbc.sequelink.SequeLinkDriver" );
		jdbcUrl = "jdbc:sequelink://" + hostname + ":2399;serverDataSource=" + dbName;
		conn = DriverManager.getConnection( jdbcUrl, "wo", "wo" );
	}

	protected void tearDown() throws Exception {
		conn.close();
	}

	//--- Read comments on these test cases to understand issues ---

	/** This passes */
	public void testConnection() {
		//Do nothing, just make sure setup and teardown work without failure
	}
	
	/** This test fails. I don't see a way to login as a guest. */
	public void testGuestLogin() throws SQLException {
		Connection conn = DriverManager.getConnection( jdbcUrl, "", "" );//No username / password specified
		conn.close();
	}

	/** This passes. It won't work with spaces or +, you need the %20 */
	public void testSpacesInDatabaseName() throws Exception {
		String jdbcUrl = "jdbc:sequelink://" + hostname + ":2399;serverDataSource=Extremely%20Large%20Database";
		log.info( "Connecting to JDBC URL " + jdbcUrl );
		DriverManager.getConnection( jdbcUrl, "wo", "wo" );
	}

	/** This is valid SQL, but it fails. */
	public void testBlankInsertion() throws SQLException {
		PreparedStatement stmt = conn.prepareStatement( "INSERT INTO Contacts() VALUES()" );
		int rowCount = stmt.executeUpdate();
		assertEquals( 1, rowCount );
	}

	/** This passes */
	public void testValuesInsertion() throws SQLException {
		PreparedStatement stmt = conn.prepareStatement( "INSERT INTO Contacts(firstName, lastName) VALUES ('Fred', 'Flintstone')" );
		int rowCount = stmt.executeUpdate();
		assertEquals( 1, rowCount );
	}

	/** This test fails with the error message: Unsupported method: Connection.prepareStatement
	 * If I don't pass the Statement.RETURN_GENERATED_KEYS, it will get past the prepareStatement part, but it dies with this error 
	 * when you try to get the generated keys: Auto-generated keys were not requested, or the SQL was not a simple INSERT statement.
	 * @throws SQLException
	 */
	public void testNextSerial() throws SQLException {
		PreparedStatement stmt = conn.prepareStatement( "INSERT INTO Contacts(firstName, lastName) VALUES ('Fred', 'Flintstone')", Statement.RETURN_GENERATED_KEYS );
		int rowCount = stmt.executeUpdate();
		assertEquals( 1, rowCount );
		stmt.getGeneratedKeys();
	}
	
	/** This test passes!!!!!!!! Who knew? */
	public void testTransaction() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.executeUpdate( "DELETE FROM Contacts WHERE firstName LIKE 'rollback'" ); //Empty out all previous test records
		conn.setAutoCommit( false );
		stmt.executeUpdate( "INSERT INTO Contacts(firstName) VALUES('commit this')" );
		conn.commit(); //This should write it to database
		stmt.executeUpdate( "INSERT INTO Contacts(firstName) VALUES('rollback')" );
		conn.rollback(); //This should roll it back
		assertFalse( stmt.executeQuery( "SELECT * FROM Contacts WHERE firstName LIKE 'rollback'" ).next() ); //Make sure it was rolled back
	}

	/** This passes. This used to fail in an older version of the driver. */
	public void testPreparedStatementDataTypes() throws SQLException {
		PreparedStatement stmt = conn.prepareStatement( "SELECT * FROM Contacts WHERE \"dateOfBirth\" > '2005-01-01 00:00:00'" );
		log.info( "Query: " + stmt );
		ResultSet rs = stmt.executeQuery();
		int foundCount = countResultSet( rs );
		log.info( "Found " + foundCount + " matching records" );
		assertTrue( foundCount > 0 );

		GregorianCalendar when = new GregorianCalendar( 2005, 1, 1 );

		//Now try the same thing with Timestamp placeholder values
		stmt = conn.prepareStatement( "SELECT * FROM Contacts WHERE \"dateOfBirth\" > ?" );
		stmt.setTimestamp( 1, new Timestamp( when.getTimeInMillis() ) );
		rs = stmt.executeQuery();
		int foundCount2 = countResultSet( rs );
		assertEquals( foundCount, foundCount2 );

		//Now try the same thing with Date placeholder values
		stmt = conn.prepareStatement( "SELECT * FROM Contacts WHERE dateValue > ?" );
		stmt.setDate( 1, new Date( when.getTimeInMillis()) );
		rs = stmt.executeQuery();
		assertTrue( rs.next() );

		//Now try the same thing with Time placeholder values
		stmt = conn.prepareStatement( "SELECT * FROM Contacts WHERE timeValue > ?" );
		stmt.setTime( 1, new Time( when.getTimeInMillis() ) );
		rs = stmt.executeQuery();
		assertTrue( rs.next() );
	}

	/** This passes. It did not used to. */
	public void testNumericDataTypes() throws SQLException {
		ResultSet rs = conn.prepareStatement( "SELECT GPA FROM Contacts WHERE GPA > 0" ).executeQuery();
		while( rs.next() ) {
			Object obj = rs.getObject( 1 );
			log.info( obj.getClass() + ": " + obj );
			rs.getFloat( 1 );
			rs.getInt( 1 );
			rs.getDouble( 1 );
			rs.getBigDecimal( 1 );
			rs.getShort( 1 );
			rs.getBoolean( 1 );
		}
	}

	/** This test fails. This is debatable, but in my opinion, it should treat LIKE searches as case insensitive. */
	public void testCaseSensitivity() throws SQLException {
		// = operator should be case sensitive
		assertEquals( 1, countResultSet( conn.prepareStatement( "SELECT firstName FROM Contacts WHERE firstName = 'Lucy'" ).executeQuery() ) );
		assertEquals( 0, countResultSet( conn.prepareStatement( "SELECT firstName FROM Contacts WHERE firstName = 'lucy'" ).executeQuery() ) );

		// Try UCASE operator
		int foundCount1 = countResultSet( conn.prepareStatement( "SELECT firstName FROM Contacts WHERE UCASE(firstName) LIKE UPPER('Lucy')" ).executeQuery() );
		assertTrue( foundCount1 > 0 );
		log.info( "UCASE returned " + foundCount1 + " matching records." );

		// LIKE operator should, in my opinion, be case insensitive. This is also how MySQL works.
		int foundCount2 = assertFoundCountsMatch(
				conn.prepareStatement( "SELECT firstName FROM Contacts WHERE firstName LIKE 'Lucy'" ).executeQuery(),
				conn.prepareStatement( "SELECT firstName FROM Contacts WHERE firstName LIKE 'lucy'" ).executeQuery()
		);
		assertEquals( "UCASE returns different found set than regular LIKE search.", foundCount1, foundCount2 );
	}

	/** This fails with the error message '251'. We should be able to get a list of scripts in the database. */
	public void testGetScripts() throws SQLException {
		conn.getMetaData().getProcedures( null, null, null );
	}

	/** This fails. We should be able to call a script in the database. */
	public void testCallScript() throws SQLException {
		conn.prepareCall( "'Create New Person'" ).executeQuery(); //Call a script with no params
		final CallableStatement callableStatement = conn.prepareCall("'Create New Person'('Jesse')");
		callableStatement.setString("script.parameter", "Jesse"); // doesn't matter what the parameter name is
		ResultSet rs = callableStatement.executeQuery(); //Call a script and pass in a param
		rs.next();
		assertEquals( "Jesse", rs.getString( "firstName" ) );
	}

	/** This test fails because there are several data types that are not supported. I've commented each line that fails.
	 * @throws SQLException
	 */
	public void testDataTypes() throws SQLException {
		ResultSet rs = conn.getMetaData().getTypeInfo();

		Set supportedTypes = new HashSet();
		while( rs.next() ) {
			supportedTypes.add( new Integer(rs.getInt("DATA_TYPE")) );
		}

		log.info( "All supported data types: " + supportedTypes );

		//Date and time types
		assertTrue( "DATE data type unsupported", supportedTypes.contains( new Integer(Types.DATE) ) );
		assertTrue( "TIME data type unsupported", supportedTypes.contains( new Integer(Types.TIME) ) );
		assertTrue( "TIMESTAMP data type unsupported", supportedTypes.contains( new Integer(Types.TIMESTAMP) ) );

		//TEXT types
		assertTrue( "VARCHAR data type unsupported", supportedTypes.contains( new Integer(Types.VARCHAR) ) );
		assertTrue( "CLOB data type unsupported", supportedTypes.contains( new Integer(Types.CLOB) ) ); //Fails
		assertTrue( "CHAR data type unsupported", supportedTypes.contains( new Integer(Types.CHAR) ) ); //Fails
		assertTrue( "LONGVARCHAR data type unsupported", supportedTypes.contains( new Integer(Types.LONGVARCHAR) ) ); //Fails

		//NUMBER types
		assertTrue( "INTEGER data type unsupported", supportedTypes.contains( new Integer(Types.INTEGER) ) );
		assertTrue( "FLOAT data type unsupported", supportedTypes.contains( new Integer(Types.FLOAT) ) );
		assertTrue( "DOUBLE data type unsupported", supportedTypes.contains( new Integer(Types.DOUBLE) ) );
		assertTrue( "DECIMAL data type unsupported", supportedTypes.contains( new Integer(Types.DECIMAL) ) );
		assertTrue( "BOOLEAN data type unsupported", supportedTypes.contains( new Integer(Types.BOOLEAN) ) ); //Fails

		//Other types
		assertTrue( "BLOB data type unsupported", supportedTypes.contains( new Integer(Types.BLOB) ) ); //Fails
		assertTrue( "LONGVARBINARY data type unsupported", supportedTypes.contains( new Integer(Types.LONGVARBINARY) ) );
	}

	/** This test passes. */
	public void testResultSetMetaData() throws SQLException {
		ResultSet rs = conn.createStatement().executeQuery( "SELECT mimeType, contactID, \"Date created\", \"Time inserted\", \"Picture taken\", portrait FROM Portrait" );
		ResultSetMetaData metaData = rs.getMetaData();
		int n=0;
		assertEquals( String.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.VARCHAR, metaData.getColumnType(n) );
		assertEquals( Double.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.DOUBLE, metaData.getColumnType(n) );
		assertEquals( java.sql.Date.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.DATE, metaData.getColumnType(n) );
		assertEquals( java.sql.Time.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.TIME, metaData.getColumnType(n) );
		assertEquals( java.sql.Timestamp.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.TIMESTAMP, metaData.getColumnType(n) );
		assertEquals( "byte[]", metaData.getColumnClassName( ++n ) );
		assertEquals( Types.LONGVARBINARY, metaData.getColumnType(n) );
	}

	/** This test seems to pass. One hitch is that when the BLOB data is written back to FileMaker, it is named 'Untitled.dat'.
	 * There should be getName(), setName(), getSize() and setSize(), and setPreview() methods in the JDBC driver class that implements BLOB.
	 * */
	public void testContainer() throws SQLException, IOException {

		//First try to read
		ResultSet rs = conn.createStatement().executeQuery( "SELECT Picture FROM Contacts WHERE ID=2280" );
		rs.next();
		Blob blob = rs.getBlob( 1 );
		BufferedImage image = ImageIO.read( blob.getBinaryStream() );
		log.info( "Picture dimensions are " + image.getWidth() + "/" + image.getHeight() );

		//Now try creating a new record with that BLOB
		PreparedStatement stmt = conn.prepareStatement( "INSERT INTO Contacts(Picture) VALUES(?)" );
		stmt.setBlob( 1, blob );
		stmt.executeUpdate();
	}


	/** This test used to fail but now it passes.
	 * This tests the amount of time it takes to do a select across a large number of columns (315) with a small number of records (6).
	 * I established a threshold of 2000ms as a minimal acceptable time to complete this.
	 * @throws SQLException
	 */
	public void testManyColumnsSpeed() throws SQLException {
		log.info("Starting testManyColumnsSpeed()");
		for( int n=0; n<5; n++ ) {
			java.util.Date then = new java.util.Date();
			ResultSet resultSet = conn.createStatement().executeQuery( "select * from ManyTextFields" );
			assertTrue( "There should be at least 300 fields on this layout", resultSet.getMetaData().getColumnCount() >= 300 );
			long elapsedTime = new java.util.Date().getTime() - then.getTime();
			log.info( "elapsed time: " + elapsedTime + "ms" );
			assertTrue( "Test took " + elapsedTime + "ms to execute, should not be more than 2000ms", elapsedTime <= 2000 ); //ddtek driver fails for me; this takes 3,672 on my dual-processor 2.5GHz G5
		}
	}

	/** This test fails. I don't see a way to connect to a database other than the one specified in the JDBC URL. */
	public void testDifferentDatabase() throws SQLException {
		ResultSet rs = conn.createStatement().executeQuery( "SELECT * FROM \"Extremely Large Database.ELD\"" );
		assertTrue( countResultSet( rs ) > 0 );
	}

	/** This test use to fail, but now passes.
	 * Tests a query of 500,000 records. The current test stops after reading 1000 rows; you could take off this max to really stress-test
	 * the memory handling.
	 * @throws Exception
	 */
	public void testLargeResultSet() throws Exception {
		String jdbcUrl = "jdbc:sequelink://" + hostname + ":2399;serverDataSource=Extremely%20Large%20Database";
		Connection tempConn = DriverManager.getConnection( jdbcUrl, "wo", "wo" );
		try {
			Statement statement = tempConn.createStatement();

			long startTime = System.currentTimeMillis();
			//A single record: ResultSet resultSet = statement.executeQuery( "select * from \"" + tableName + "\" where counter=1" );
			ResultSet resultSet = statement.executeQuery( "select * from \"Many records\" " );
			long elapsedTime = System.currentTimeMillis() - startTime;
			log.info("Query finished in " + elapsedTime + " milliseconds");
			assertTrue("Query should complete in less than 3 seconds", elapsedTime < 3000 );
			boolean justCountRows = false;
			int rowCount = 0;
			if( justCountRows ) {
				rowCount = countResultSet( resultSet );
			} else {
				StringBuffer result = new StringBuffer( 100000 );
				while( resultSet.next() && rowCount < 1000 ) { //Get the first 1000 rows
					rowCount++;
					result.append( rowCount + ": " + resultSet.getObject(1) + " / " + resultSet.getObject(2) + " / " + resultSet.getObject(3) + " / " + resultSet.getObject(4) + '\n' );
					resultSet.getObject( 5 );
				}
				//log.info( result.toString() );
			}
			elapsedTime = System.currentTimeMillis() - startTime;
			log.info( "Took " + elapsedTime + " ms to complete query and get " + rowCount + " items." );
			if( rowCount <= 1000 ) {
				assertTrue("Should be able to get 1000 records in less than ten seconds; took " + elapsedTime, elapsedTime < 10000 );
			}
			resultSet.close();
			try {
				resultSet.next();
				fail("Should throw an exception if we try to get more items after closing the ResultSet." );
			} catch(SQLException ex) {
				//We're expecting this exception
			}
		} finally {
			tempConn.close();
		}
	}

	/** This test fails, because the following metadata properties could not be read:
	 * <ul>
	 * <li>clientAccountingInfoLength</li>
	 * <li>clientApplicationNameLength</li>
	 * <li>clientHostNameLength</li>
	 * <li>clientUserLength</li>
	 * <li>schemas</li>
	 * </ul>
	 * @throws SQLException
	 * @throws IntrospectionException
	 * @throws IllegalAccessException
	 */
	public void testMetaData() throws SQLException, IntrospectionException, IllegalAccessException {
		DatabaseMetaData m = conn.getMetaData();
		BeanInfo info = Introspector.getBeanInfo( m.getClass() );
		PropertyDescriptor[] properties = info.getPropertyDescriptors();
		StringBuffer dump = new StringBuffer();
		StringBuffer failedProperties = new StringBuffer();
		Object[] NO_ARGS = new Object[0];
		for( int n=0; n<properties.length; n++ ) {
			if( properties[n].getReadMethod() != null ) {
				try {
					dump.append( properties[n].getDisplayName() + ": " + properties[n].getReadMethod().invoke( m, NO_ARGS ) + '\n' );
				} catch( InvocationTargetException e ) {
					failedProperties.append( properties[n].getDisplayName() + ": " + e.getCause().toString() + '\n' );
				}
			}
		}
		log.info( "Driver metadata: \n" + dump );
		if( failedProperties.length() > 0 ) {
			fail("The following properties could not be read:\n" + failedProperties.toString() );
		}
	}
	
	/** This test passes. This is so cool. */
	public void testSchemaAlteration1() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.execute( "CREATE TABLE SimpleTable (text VARCHAR)" ); //Create a simple table
		try {
			stmt.execute( "CREATE TABLE SimpleTable (text VARCHAR)" ); //Create a simple table
			fail("Should get an error when trying to create the same table twice");
		} catch( SQLException e ) {
			//Ignore, this is expected
		}
		stmt.execute( "ALTER TABLE SimpleTable ADD moreText VARCHAR" ); //Add a field
		stmt.execute( "ALTER TABLE SimpleTable DROP moreText" ); //Delete it
		stmt.execute( "DROP TABLE SimpleTable" ); //Delete it
		stmt.execute( "CREATE TABLE SimpleTable (text VARCHAR)" ); //Repeat the process. This is to check to see if the drop table also dropped the table occurrences
		stmt.execute( "DROP TABLE SimpleTable" ); //Delete it
		
		stmt.execute( "CREATE TABLE TempTable (text VARCHAR, \"text with spaces\" VARCHAR, NotEmptyText VARCHAR NOT NULL, EightChars VARCHAR (8), Number DECIMAL, SomeDate DATE, SomeTime TIME, TheTimestamp TIMESTAMP, Container BLOB)" );
		stmt.execute( "DROP TABLE TempTable" );
	}
	
	/** This fails - it would be handy, especially for the Separation Model� people, if they could alter field types of existing fields */
	public void testSchemaAlteration2() throws SQLException {
		Statement stmt = conn.createStatement();
		stmt.execute( "CREATE TABLE SimpleTable (text VARCHAR)" ); //Create a simple table
		try {
			stmt.execute( "ALTER TABLE SimpleTable CHANGE moreText DECIMAL" ); //Convert it to a number field
		} finally {
			stmt.execute( "DROP TABLE SimpleTable" ); //Delete it
		}
	}

	//--- Private utility methods, not important ---
	private int assertFoundCountsMatch( ResultSet rs1, ResultSet rs2 ) throws SQLException {
		int foundCount1 = countResultSet( rs1 );
		int foundCount2 = countResultSet( rs2 );
		assertEquals( "ResultSet found counts do not match. ", foundCount1, foundCount2 );
		return foundCount1;
	}

	private int countResultSet( ResultSet rs ) throws SQLException {
		int result = 0;
		while( rs.next() ) result++;
		return result;
	}
}
