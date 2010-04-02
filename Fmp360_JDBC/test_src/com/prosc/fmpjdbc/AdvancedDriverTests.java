package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.sql.*;
import java.util.*;
import java.util.logging.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;

import com.prosc.shared.IOUtils;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Apr 20, 2005 Time: 10:43:32 PM
 */
public class AdvancedDriverTests extends TestCase {
	private Connection connection7;
	//private Connection connection6;
	private Statement statement7;
	//private Statement statement6;
	private JDBCTestUtils jdbc7;
	//private JDBCTestUtils jdbc6;

	//private Logger logger;


	protected void setUp() throws Exception {
		jdbc7 = new JDBCTestUtils();
		connection7 = jdbc7.getConnection();
		statement7 = connection7.createStatement();

		//jdbc6 = new JDBCTestUtils();
		//jdbc6.setFmVersion(6);
		//connection6 = jdbc6.getConnection();
		//statement6 = connection6.createStatement();

		//Logger.getLogger("").setLevel(Level.FINE);
	}

	protected void tearDown() throws Exception {
		statement7.close();
		connection7.close();
		//statement6.close();
		//connection6.close();
	}


	/** Simple select test for FM6 */
	/*public void testSimpleSelectForFM6() throws SQLException {
		//String tableName = jdbc.fmVersion >= 7 ? "Contacts" : "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?
		System.out.println("Begin select in fm6");
		long start = System.currentTimeMillis();

		String tableName =  "Contacts.Contacts"; // Need to include the db & layout name if using 6
		String sql = "SELECT * FROM " + tableName + " where lastName='Leong' and emailAddress='kungfudude@mcgyver.com'";
		ResultSet resultSet = statement6.executeQuery( sql );
		int rowCount = 0;
		while( resultSet.next() ) {
			rowCount++;
			if( rowCount == 1 ) assertEquals( "Al", resultSet.getObject("firstName") );
		}
		assertTrue( "No results in found set", rowCount > 0 );

		long end = System.currentTimeMillis();
		System.out.println("End select in fm6.  duration : " + (end - start) + " milliseconds.  rowCount = " + rowCount);
	}*/


	/** Simple select test in FM7 */
	public void testSimpleSelectForFM7() throws SQLException {
		System.out.println("Begin select in fm7");
		long start = System.currentTimeMillis();

		String tableName =  "Contacts";
		String sql = "SELECT * FROM " + tableName + " where lastName='Leong' and emailAddress='kungfudude@mcgyver.com'";
		ResultSet resultSet = statement7.executeQuery( sql );
		int rowCount = 0;
		while( resultSet.next() ) {
			rowCount++;
			if( rowCount == 1 ) assertEquals( "Al", resultSet.getObject("firstName") );
		}
		assertTrue( "No results in found set", rowCount > 0 );

		long end = System.currentTimeMillis();
		System.out.println("End select in fm7.  duration : " + (end - start) + " milliseconds.  rowCount = " + rowCount);
	}


	/** @TestPasses */
	public void testEscapeFMWildCards() throws SQLException {
		String tableName = jdbc7.fmVersion >= 7 ? "Contacts" : "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?
		statement7.executeUpdate( "DELETE FROM \""+tableName+"\" where city = 'a@b*c#d?e!f=g<h>i\"j' ");

		//  insert record containing wildcards
		String sql = "INSERT INTO \""+tableName+"\" (firstName, lastName, emailAddress, city) values('Wildcards', 'Test', 'wildcards@justatest.com', 'a@b*c#d?e!f=g<h>i\"j' )";
		int rowCount = statement7.executeUpdate( sql );
		assertEquals( 1, rowCount );

		//search for wildcards
		sql = "SELECT firstName FROM "+tableName+" where city = 'a@b*c#d?e!f=g<h>i\"j'";
		ResultSet rs = statement7.executeQuery( sql );
		assertTrue("Did not find a match when searching using special chars", rs.next());
		assertEquals( "Wildcards", rs.getObject("firstName") );

		//Delete wild card record
		rowCount = statement7.executeUpdate( "DELETE FROM \""+tableName+"\" where city = 'a@b*c#d?e!f=g<h>i\"j' ");
		assertTrue( "Should have found 1 row to delete, not " + rowCount, rowCount == 1 ); // FIX!! is there something wrong with the count returned from deletes? -ssb
	}

	//Test with username/password authentication

	//Test SSL encryption over https

	//Test stored procedures

	/** This test fails because the ddtek driver throws an 'Optional feature not implemented' exception. Although this method
	 * is not strictly required for the functionality we need for Bernard Hodes Group, it is necessary for most tools (including
	 * WebObjects) which scan the contents of a database to build a model of its structure.
	 * @throws SQLException
	 */
	public void testGetStoredProcedures() throws SQLException {
		if (jdbc7.fmVersion < 7){connection7.setCatalog("Contacts");} //Need to set the db if using 6, right?
		ResultSet procedures = connection7.getMetaData().getProcedures(null, null, null); //ddtek driver does not implement this
		int scriptCount = 0;
		while( procedures.next() ) {
			System.out.println("Script columnName: " + procedures.getString("PROCEDURE_NAME") );
			scriptCount++;
		}
		assertTrue( "There are at least three scripts in the test database", scriptCount >= 3 );
	}

	/** This test fails when it tries to call a stored procedure (FileMaker script), because I get a "Parse error in SQL"
	 * message when I pass in the name of a script to prepareCall(). I'm not sure if stored procedures are supported at all,
	 * and if they are, I don't know the correct way to execute them. I would classify this feature as a 'nice to have'.
	 * @throws SQLException
	 */
	public void testExecuteStoredProcedure() throws SQLException {
		connection7.prepareCall("capitalizeLastNames").execute();
		String tableName = jdbc7.fmVersion >= 7 ? "Contacts" : "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?
		statement7.executeUpdate( "INSERT INTO "+tableName+" (firstName, lastName, emailAddress) values('Fred', 'flintstone', 'fred@rubble.com')");
		ResultSet rs = statement7.getGeneratedKeys();
		rs.next();
		Object id = rs.getObject( "ID" );

		if (jdbc7.fmVersion < 7){connection7.setCatalog("Contacts");} //Need to set the db if using 6, right?
		connection7.prepareCall("capitalizeLastNames").execute();
		//rs.close();
		rs = statement7.executeQuery( "SELECT lastName FROM "+tableName+" where ID='" + id + "'" );
		rs.next();
		assertEquals( "FLINTSTONE", rs.getString("lastName") );
	}

	//Need to add test cases for executeQuery() with several parameters and executeUpdate() with prepared statements.

	/** This test fails because there seem to be problem with using dates & times in prepared statements. A prepared statement is the way that
	 * most JDBC access frameworks communicate with the database, because they let you build your SQL query with placeholders in which
	 * to insert variables. The variables can be set programatically, using type-safe operators, so that it is not necessary to follow a
	 * rigid string format. In particular, PreparedStatements are good for using date & time values, since there are so many formatting
	 * variations on how to supply date & time parameters.
	 *
	 * When I try to execute this INSERT statement with a date parameter, I get this error message from the ddtek driver (parameter 3 is the Date parameter):
	 * String data, right truncated.  Error in parameter 3.
	 *
	 * @TestPasses
	 * */
	public void testPreparedStatement() throws SQLException {
		String tableName = jdbc7.fmVersion >= 7 ? "Portrait" : "Portrait.portrait"; //Need to include the db & layout name if using 6, right?
		statement7.executeUpdate( "DELETE FROM " + tableName + " WHERE \"Alternate Mime Type\"='JDBC testing' "); // cleanup
		//
		PreparedStatement insertStatement = connection7.prepareStatement( "INSERT INTO " + tableName + " (contactID, mimeType, \"Alternate Mime Type\", \"Date Created\", \"Time inserted\", \"Picture taken\") values(?,?,'JDBC testing',?,?,?)");
		java.util.Date now = new java.util.Date();
		insertStatement.setString( 1, "100");
		insertStatement.setString( 2, "video/mpeg" );
		insertStatement.setDate( 3, new java.sql.Date( now.getTime() ) );
		insertStatement.setTime( 4, new java.sql.Time( now.getTime() ) );
		insertStatement.setTimestamp( 5, new Timestamp( now.getTime() ) );
		insertStatement.execute(); // insert #1.  dtek driver fails with Date objects in prepared statements
		ResultSet rs = insertStatement.getGeneratedKeys();
		rs.next();
		Object id1 = rs.getString("ID");
		insertStatement.setString( 1, "101"); // insert #2
		insertStatement.execute();
		insertStatement.setString( 1, "102");
		insertStatement.execute(); // insert #3
		insertStatement.clearParameters();
		rs = insertStatement.getGeneratedKeys();
		rs.next();
		Object id3 = rs.getString("ID");
		//FIX!! Should we store the record ID in getGeneratedKeys()?
		rs = statement7.executeQuery( "select * from portrait where ID='" + id1 + "'");
		assertTrue( rs.next() );
		assertEquals( 100, rs.getInt("contactID") );
		assertEquals( "video/mpeg", rs.getString("mimeType") );
		assertEquals( "JDBC testing", rs.getString("Alternate Mime Type") ); //FIX! Should we support case-insensitive name retrieval? --jsb
		assertEquals( new java.sql.Date(now.getTime()).toString(), rs.getDate("Date Created").toString() );
		assertEquals( new java.sql.Time(now.getTime()).toString(), rs.getTime("Time inserted").toString() );
		long time = now.getTime();
		long roundedTime = (time / 1000) * 1000; // trim milliseconds from time, since we're not storing that.
		assertEquals((roundedTime), rs.getTimestamp("Picture taken").getTime() );
		assertTrue( rs.isLast() );
		rs = statement7.executeQuery( "select * from portrait where ID='" + id3 + "'");
		assertTrue( rs.isBeforeFirst() );
		rs.next();
		//assertNull( rs.getObject("mimeType") );
		//assertEquals( 103, rs.getInt("contactID") );
		assertEquals("video/mpeg", rs.getObject("mimeType"));
		assertEquals( 102, rs.getInt("contactID") );
		rs.next();
		assertTrue( rs.isAfterLast() );
		int rowCount = statement7.executeUpdate( "DELETE from Portrait where \"alternate mime type\" = \"JDBC testing\" ");
		assertEquals( 3, rowCount );
	}

	//Test MetaData.getVersionColumns()

	//Test containers/BLOBs
	public Blob testContainerFields() throws SQLException, IOException {
		String tableName = jdbc7.fmVersion >= 7 ? "Portrait" : "Portrait.portrait"; //Need to include the db & layout name if using 6, right?
		ResultSet rs = statement7.executeQuery("SELECT contactID, portrait from " + tableName + " where contactID != null");
		Blob eachValue = null;
		int successCount = 0;
		while( rs.next() ) {
			eachValue = rs.getBlob(2);

			if( eachValue != null ) {
				int length = (int)eachValue.length();
				byte[] bytes = eachValue.getBytes(0, length);

				//Make sure that we can read multiple times
				length = (int)eachValue.length();
				bytes = eachValue.getBytes(0, length);

				assertTrue( length > 20 );
				assertTrue ( bytes.length == length );
				String mimeType = ( (FmBlob)eachValue ).getMimeType();
				assertTrue( "image/jpeg".equals( mimeType ) || "application/octet-stream".equals( mimeType ) );
				successCount++;

				URL url = rs.getURL( 2 );
				InputStream in = url.openStream();
				try {
					byte[] urlBytes = IOUtils.inputStreamAsBytes( in );
					assertTrue( "BLOB bytes are not the same as URL bytes", Arrays.equals( bytes, urlBytes ) );
				} finally {
					in.close();
				}
				
				url = new URL (rs.getString( 2 ) );
				in = url.openStream();
				try {
					byte[] urlBytes = IOUtils.inputStreamAsBytes( in );
					assertTrue( "BLOB bytes are not the same as URL bytes", Arrays.equals( bytes, urlBytes ) );
				} finally {
					in.close();
				}
			}
		}
		if( successCount == 0 ) fail("No container data found to test with." );
		return eachValue;
	}

	//Test streaming result sets with enormous data sets

	//Finished in about 1:40 with ddtek driver
	//Finished in about 1:20 with our driver
	/*public void testLargeDataSet() throws SQLException {
		Connection connection;
		connection = JDBCTestUtils.getConnection("Extremely Large Database");
		Statement statement = connection.createStatement();
		String sql = "SELECT * FROM ELD";
		java.util.Date then = new java.util.Date();
		ResultSet rs = statement.executeQuery(sql);
		int foundCount = 0;
		while( rs.next() ) foundCount++; //Cycle through all records
		long elapsedTime = new java.util.Date().getTime() - then.getTime();
		System.out.println("Elapsed time: " + elapsedTime);
		assertTrue( foundCount >= 10000 );
		connection.close();
		System.out.println("<- Closed connection");
	}*/

	//Test SHOW DATABASES, SHOW TABLES, DESCRIBE TABLE syntax

	//Test special characters in search, such as @

	//Test wildcard searches, LIKE, begins with, ends with, contains, etc.

	//Test compatibility with FileMaker 6

	//Test get character stream in ResultSet (this is used by EOF)

	//Test unicode international charcacters for reading and writing

	/** This test passes. */
	public void testSingleTableAliasing() throws SQLException {
		PreparedStatement ps = connection7.prepareStatement( "SELECT t0.city,t0.emailAddress, t0.firstName, t0.ID, t0.lastName FROM Contacts t0 where t0.city=?" );
		ps.setString( 1, "San Francisco" );
		ResultSet rs = ps.executeQuery();
		rs.next();
		assertEquals("San Francisco", rs.getString("city") );
	}

	public  void testFmpRecordIDs1() throws SQLException {
		//Make sure that we get record ids when requested in the select statement
		ResultSet rs = statement7.executeQuery( "SELECT recId, firstName, lastName FROM Contacts WHERE firstName='Robin'" );
		rs.next();
		assertEquals( rs.getObject(1).getClass(), Integer.class ); //getObject on recId should return an Integer
		int recId = rs.getInt( 1 );
		assertTrue( "Record is " + recId + "; expected non-zero value.", recId != 0 );
		rs.close();

		//Now try updating with that record ID
		statement7.executeUpdate( "UPDATE Contacts SET firstName='Robin' WHERE recId=" + recId );
		//rs = statement7.getGeneratedKeys();
		//rs.next();
		//assertNotNull( rs.getObject( "Mod count" ) );
		//rs.close();

		//Now try updating with record ID with prepared statements
		PreparedStatement ps = connection7.prepareStatement( "UPDATE Contacts SET firstName=? WHERE recID=?" );
		ps.setString( 1, "Robin" );
		ps.setInt( 2, recId );
		assertEquals( 1, ps.executeUpdate() );

		//Now see if we get the record ID in a newly created record
		assertEquals( 1, statement7.executeUpdate( "INSERT INTO Contacts(firstName) VALUES(\"recid test\") " ) );
		ResultSet keys = statement7.getGeneratedKeys();
		keys.next();
		recId = keys.getInt( "recid" );

		//Now test a deletion by the record ID
		assertEquals( 1, statement7.executeUpdate( "DELETE FROM Contacts WHERE recId = " + recId ) );
	}
	
	/*public void testGeneratedKeys() {
		statement7.executeUpdate( "" )
	}*/

	/** This test does not apply to the ddtek driver.
	 * @TestFails */
	public void testFmpRecordIDs2() throws SQLException {
		if( jdbc7.use360driver ) {
			String tableName = jdbc7.fmVersion >= 7 ? "Contacts" : "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?
			statement7.executeUpdate( "DELETE from "+tableName+" where firstName='Robin' and lastName='Williams' "); //Start out with an empty record set
			int rowCount = statement7.executeUpdate("INSERT INTO "+tableName+" (firstName, lastName) values('Robin', 'Williams')" );
			assertEquals( 1, rowCount );

			ResultSet generatedKeys = statement7.getGeneratedKeys();
			generatedKeys.next();
			int recId = generatedKeys.getInt("recId"); //Generated keys should always include a column for "recId"; // FIX!! this fails.  Not implemented -ssb

			ResultSet rs = statement7.executeQuery("SELECT * from "+tableName+" where firstName='Robin' and lastName='Williams' "); //Should not return a recId, since we do not explicitly ask for it
			rs.next();
			try {
				rs.getInt("recId");
				fail("Should have failed - we only get the recId if we explicitly ask for it"); //Is this definitely how we want this to work?
			} catch (IllegalArgumentException e) {
				//This is correct - we did not specify recId as one of the search terms
			}

			rs = statement7.executeQuery("SELECT firstName, lastName, recId, state, city from "+tableName+" where firstName='Robin' and lastName='Williams' "); //Should get back a recId that matches the one from our insertion
			rs.next();
			assertEquals( recId, rs.getInt(3) );
			assertEquals( recId, rs.getInt("recId") );
			try {
				rs.getInt("RECID");
				fail("Should have failed - this is case sensitive, depending on how the field is requested");
			} catch (IllegalArgumentException e) {
				//This is correct - get by name is case sensitive
			}

			rs = statement7.executeQuery("SELECT firstName, RECID, lastName from "+tableName+" where city='San Francisco' or ReCiD=" + recId ); //Search terms are not case-sensitive
			rs.next();
			assertEquals(recId, rs.getInt("RECID") );
			assertFalse( rs.next() ); //Shouldn't be more than one matching record
		}
	}

	//Test for FileMaker mod count

	/** This test passes. */
	public void testPreparedStatementBindings() throws SQLException {
		statement7.executeUpdate("DELETE FROM Portrait WHERE \"Floating point\" is NULL" );
		statement7.executeUpdate("INSERT INTO PORTRAIT(\"Floating point\", mimeType) values(NULL, 'image/jpg')");
		statement7.executeUpdate("INSERT INTO PORTRAIT(\"Floating point\", mimeType) values(NULL, 'image/jpg')");
		PreparedStatement statement = connection7.prepareStatement("SELECT * FROM Portrait WHERE \"Floating point\" IS NULL and mimeType=?");
		statement.setString(1, "image/jpg");
		ResultSet rs = statement.executeQuery();
		int foundCount = 0;
		while( rs.next() ) foundCount++;
		assertTrue( "Should have found exactly 2 records, found " + foundCount, foundCount == 2 );
	}

	/** This is another example of prepared statements with dates. It is a more specific test designed specifically for dates and times,
	 * where the {@link #testPreparedStatement()} test does a more general test. The ddtek fails this test for the same reasons as the other test.
	 * @throws SQLException
	 */
	public void testPreparedStatementsWithDates() throws Exception {
		java.sql.Date testDate = new java.sql.Date(1000000000000L);
		String testDateString = "09/08/2001";
		java.sql.Time testTime = new java.sql.Time( new GregorianCalendar(2001,9,8,11,0,0).getTimeInMillis() );

		java.sql.Timestamp testTimestamp = new Timestamp(1000000000000L);

		statement7.executeUpdate("DELETE FROM Portrait WHERE \"Date created\" = " + testDateString );

		statement7.executeUpdate("INSERT INTO PORTRAIT(\"Date created\") VALUES('" + testDateString + "')" ); //Create 1 record

		PreparedStatement insertDates = connection7.prepareStatement("INSERT INTO Portrait(\"Date created\", \"Time inserted\", \"Picture taken\") values(?,?,?)" );
		insertDates.setDate( 1, testDate );
		insertDates.setTime( 2, testTime );
		insertDates.setTimestamp( 3, testTimestamp );
		insertDates.executeUpdate(); //Create 2 records
		insertDates.executeUpdate(); //Create 3 records

		//Try a select with a prepared statement
		PreparedStatement selectedDatesPS = connection7.prepareStatement("SELECT * FROM Portrait WHERE \"Date created\" = ? AND \"Time inserted\" = ? AND \"Picture taken\" = ?" );
		selectedDatesPS.setDate( 1, testDate );
		selectedDatesPS.setTime( 2, testTime );
		selectedDatesPS.setTimestamp( 3, testTimestamp );
		/*
		PreparedStatement selectedDatesPS = connection.prepareStatement("SELECT * FROM Portrait WHERE \"Date created\" = ?" );
		selectedDatesPS.setDate( 1, testDate );
		*/

		ResultSet rs = selectedDatesPS.executeQuery(); // doesn't find the first one inserted (1 of 3), because timeInserted and Picture Taken are null -ssb
		int foundCount = 0;
		while( rs.next() ) foundCount++;
		assertTrue( "Should have found exactly 2 records, found " + foundCount, foundCount == 2 );

		//Now try with hard-coded select
		rs = statement7.executeQuery("SELECT * FROM PORTRAIT WHERE \"Date created\" = " + testDateString );
		foundCount = 0;
		while( rs.next() ) foundCount++;
		assertTrue( "Should have found exactly 3 records, found " + foundCount, foundCount == 3 );

		//Now try with hard-coded delete
		foundCount = statement7.executeUpdate("DELETE FROM Portrait WHERE \"Date created\" = " + testDateString );
		assertTrue( "Should have deleted exactly 3 records, found " + foundCount, foundCount == 3 );
	}

	/** This tests support for various JDBC data types used when programatically creating tables and fields. The more data types
	 * which are supported, the easier it is for a generic database management tool to programatically create fields and tables
	 * in FileMaker. Here is a list of data types that seem like fairly basic types, which map well to FileMaker's built-in data types.
	 * I've put an X next to data types which are unsupported by the ddtek driver.
	 *
	 * <pre>
	 * DATE
	 * TIME
	 * TIMESTAMP
	 * CHAR (X)
	 * VARCHAR
	 * LONGVARCHAR (X)
	 * INTEGER (X)
	 * FLOAT (X)
	 * DOUBLE
	 * DECIMAL (X)
	 * </pre>
	 *
	 * @TestFails I get a CHAR data type unsupported with the 360Works driver. Should this be fixed? --jsb
	 * */
	public void testBasicDataTypesNotImpl() throws SQLException {
		Set supportedTypes = new HashSet();
		ResultSet rs = connection7.getMetaData().getTypeInfo();
		while( rs.next() ) {
			supportedTypes.add( new Integer(rs.getInt("DATA_TYPE")) );
		}

		//DATE types
		assertTrue( "DATE data type unsupported", supportedTypes.contains( new Integer(Types.DATE) ) );

		//TIME types
		assertTrue( "TIME data type unsupported", supportedTypes.contains( new Integer(Types.TIME) ) );

		//TIMESTAMP types
		assertTrue( "TIMESTAMP data type unsupported", supportedTypes.contains( new Integer(Types.TIMESTAMP) ) );

		//TEXT types
		assertTrue( "VARCHAR data type unsupported", supportedTypes.contains( new Integer(Types.VARCHAR) ) );
		assertTrue( "CHAR data type unsupported", supportedTypes.contains( new Integer(Types.CHAR) ) ); //ddtek driver does not support CHAR
		assertTrue( "LONGVARCHAR data type unsupported", supportedTypes.contains( new Integer(Types.LONGVARCHAR) ) ); //ddtek driver does not support LONGVARCHAR

		//NUMBER types
		assertTrue( "INTEGER data type unsupported", supportedTypes.contains( new Integer(Types.INTEGER) ) );
		assertTrue( "FLOAT data type unsupported", supportedTypes.contains( new Integer(Types.FLOAT) ) );
		assertTrue( "DOUBLE data type unsupported", supportedTypes.contains( new Integer(Types.DOUBLE) ) );
		assertTrue( "DECIMAL data type unsupported", supportedTypes.contains( new Integer(Types.DECIMAL) ) ); //ddtek driver does not support DECIMAL
	}

	/** This tests a broader selection of data types, which could also map to FileMaker data types, but are less commonly used. None
	 * of these types except BLOB are supported by the ddtek driver. I'm not sure of the status of container fields, and how they should be mapped -
	 * the latest release notes with the beta version of the driver seem to indicate that containers are supported, but I couldn't figure
	 * out how to make that work, and I couldn't find the referenced documentation.
	 * <pre>
	 * CLOB (X)
	 * BOOLEAN (X)
	 * TINYINT (X)
	 * BIGINT (X)
	 * NUMERIC (X)
	 * SMALLINT (X)
	 * BLOB
	 * LONGVARBINARY (X)
	 * VARBINARY (X)
	 * </pre>
	 * @throws SQLException
	 * @TestFails I get a CLOB data type unsupported. Should this be fixed? --jsb
	 */
	public void testOptionalDataTypesNotImpl() throws SQLException {
		Set supportedTypes = new HashSet();
		ResultSet rs = connection7.getMetaData().getTypeInfo();
		while( rs.next() ) {
			supportedTypes.add( new Integer(rs.getInt("DATA_TYPE")) );
		}

		//TEXT types
		assertTrue( "CLOB data type unsupported", supportedTypes.contains( new Integer(Types.CLOB) ) );

		//NUMBER types
		assertTrue( "BIT data type unsupported", supportedTypes.contains( new Integer(Types.BIT) ) );
		assertTrue( "BOOLEAN data type unsupported", supportedTypes.contains( new Integer(Types.BOOLEAN) ) );
		assertTrue( "TINYINT data type unsupported", supportedTypes.contains( new Integer(Types.TINYINT) ) );
		assertTrue( "BIGINT data type unsupported", supportedTypes.contains( new Integer(Types.BIGINT) ) );
		assertTrue( "NUMERIC data type unsupported", supportedTypes.contains( new Integer(Types.NUMERIC) ) );
		assertTrue( "SMALLINT data type unsupported", supportedTypes.contains( new Integer(Types.SMALLINT) ) );

		//CONTAINER types
		assertTrue( "BLOB data type unsupported", supportedTypes.contains( new Integer(Types.BLOB) ) );
		assertTrue( "LONGVARBINARY data type unsupported", supportedTypes.contains( new Integer(Types.LONGVARBINARY) ) );
		assertTrue( "VARBINARY data type unsupported", supportedTypes.contains( new Integer(Types.VARBINARY) ) );
	}

	/*public void testConnectionTeardown() throws SQLException {
		Connection c;
		for( int n=0; n<260; n++ ) {
			if( ! JDBCTestUtils.use360driver ) System.out.println("connection #" + n);
			c = DriverManager.getConnection( JDBCTestUtils.getJdbcUrl( JDBCTestUtils.dbName), JDBCTestUtils.dbUsername, JDBCTestUtils.dbPassword );
			c.close();
		}
	}*/

	public void testRangeSearchesNotImpl() throws SQLException {
		//Now try a date range
		PreparedStatement ps = connection7.prepareStatement("SELECT ID, firstName, lastName, \"Timestamp created\" from Contacts where gpa >= ? and gpa <= ?");
		//ps = connection.prepareStatement("SELECT ID, firstName, lastName, \"Timestamp created\" from Contacts where gpa <= ?");
		ps.setFloat(1, 1.0f );
		ps.setFloat(2, 3.5f );
		ResultSet rs = ps.executeQuery();
		int resultCount = 0;
		while (rs.next()) resultCount++;
		assertTrue( resultCount > 1 );

		Timestamp startRange = new Timestamp( new GregorianCalendar(2003,1,1).getTimeInMillis() );
		Timestamp endRange = new Timestamp( new java.util.Date().getTime() );
		//First try a single date criteria
		ps = connection7.prepareStatement("SELECT ID, firstName, lastName, \"Timestamp created\" from Contacts where \"Timestamp created\" > ?");
		ps.setTimestamp( 1, startRange );
		rs = ps.executeQuery();
		resultCount = 0;
		while (rs.next()) resultCount++;
		assertTrue( resultCount > 100 );
		//Now try a date range
		ps = connection7.prepareStatement("SELECT ID, firstName, lastName, \"Timestamp created\" from Contacts where \"Timestamp created\" > ? and \"Timestamp created\" < ?");
		ps.setTimestamp( 1, startRange );
		ps.setTimestamp( 2, endRange );
		rs = ps.executeQuery();
		resultCount = 0;
		while (rs.next()) resultCount++;
		assertTrue( resultCount > 100 );
	}

	public void testGetColumnNamesFm7() throws SQLException {
		ResultSet rs = connection7.getMetaData().getColumns("Contacts", null, "Calc Test", null);

		do {
			rs.next();
		} while (!rs.getString(4).equals("c")); //Check to make sure we have a column called 'C'

		assertNotNull("Expected a string of \"readonly\" and received null.", rs.getString(12));
		assertTrue(rs.getString(12).indexOf("readonly") > -1);
	}

	/*public void testGetColumnNamesFm6() throws SQLException {
		JDBCTestUtils testUtils = new JDBCTestUtils();
		testUtils.fmVersion = 6;
		testUtils.xmlServer = "forge.360works.com";
		testUtils.port = 4000;
		Connection c = testUtils.getConnection();
		Set columnNames = new HashSet();
		ResultSet rs = c.getMetaData().getColumns( null, null, "Contacts", null );
		while( rs.next() ) {
			columnNames.add( rs.getString(4) );
		}
		c.close();

		assertTrue( columnNames.contains("firstName") );
		assertTrue( columnNames.contains("lastName") );
	}*/

	//FIX!! Write tests for FmResultSetMetaData, it is mostly abstract errors right now

	//FIX!! Write tests for ORDER BY clause

	//FIX!! Write tests for LIMIT clause

	/** This tests the amount of time it takes to do a select across a large number of columns (315) with a small number of records (6).
	 * I established a threshold of 2000ms as a minimal acceptable time to complete this. The XML interface delivers the result in about 1000ms,
	 * and the ddtek driver fails this test because it takes about 5000ms.
	 *
	 * Note that this is a different problem than the original critical speed issue that I reported, which affected all UPDATE and INSERT
	 * operations into large tables, regardless of how many fields were actually being inserted or updated. This speed issue is much less
	 * important, because it's much less severe than it was before (when it was taking 20-30 seconds to do this), and because it can be
	 * avoided by only selecting the fields needed, which was not the case in the other bug.
	 * @throws SQLException
	 */
	public void testManyColumnsSpeed() throws SQLException {
		System.out.println("Starting testLargeTableSpeed()");
		String tableName = jdbc7.fmVersion >= 7 ? "ManyTextFields" : "ManyTextFields.Layout #2"; //Need to include the db & layout name if using 6, right?
		for( int n=0; n<5; n++ ) {
			java.util.Date then = new java.util.Date();
			ResultSet resultSet = statement7.executeQuery( "select * from \"" + tableName + "\"" );
			assertTrue( "There should be at least 300 fields on this layout", resultSet.getMetaData().getColumnCount() >= 300 );
			long elapsedTime = new java.util.Date().getTime() - then.getTime();
			System.out.println( "elapsed time: " + elapsedTime + "ms" );
			assertTrue( "Test took " + elapsedTime + "ms to execute, should not be more than 2000ms", elapsedTime <= 2000 ); //ddtek driver fails for me; this takes 3,672 on my dual-processor 2.5GHz G5
		}
	}

	public void testResultSetMetaData() throws SQLException {
		ResultSet rs = statement7.executeQuery( "SELECT mimeType, contactID, \"Date created\", \"Time inserted\", \"Picture taken\", portrait FROM Portrait" );
		ResultSetMetaData metaData = rs.getMetaData();
		int n=0;
		assertEquals( String.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.VARCHAR, metaData.getColumnType(n) );
		assertEquals( BigDecimal.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.DECIMAL, metaData.getColumnType(n) );
		assertEquals( java.sql.Date.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.DATE, metaData.getColumnType(n) );
		assertEquals( java.sql.Time.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.TIME, metaData.getColumnType(n) );
		assertEquals( java.sql.Timestamp.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.TIMESTAMP, metaData.getColumnType(n) );
		assertEquals( Blob.class.getName(), metaData.getColumnClassName( ++n ) );
		assertEquals( Types.BLOB, metaData.getColumnType(n) );
	}

	/** Tests a download of 500,000 records. You can try setting the -Xmx8m JVM param to set the max size to 8 megs, to ensure
	 * that this runs out of memory. The point of this test is to make sure that the results are being streamed instead of
	 * downloaded at one pass. The current test stops after reading 1000 rows; you could take off this max to really stress-test
	 * the memory handling.
	 * @throws Exception
	 */
	/*public void testLargeResultSet() throws Exception {
		FmConnection fmConnection = new FmConnection(jdbc7.getJdbcUrl("Extremely Large Database"), new Properties());// contacts.fp7
		Statement statement = fmConnection.createStatement();

		String tableName = "Many records";
		long startTime = System.currentTimeMillis();
		//A single record: ResultSet resultSet = statement.executeQuery( "select * from \"" + tableName + "\" where counter=1" );
		ResultSet resultSet = statement.executeQuery( "select * from \"" + tableName + "\" " );
		assertTrue("Query should complete in less than 3 seconds", System.currentTimeMillis() - startTime < 3000 );
		System.out.println("Done with the query");
		int rowCount = 0;
		while( resultSet.next() && rowCount < 1000 ) { //Get the first 1000 rows
			rowCount++;
			System.out.println( rowCount + ": " + resultSet.getObject(1) + " / " + resultSet.getObject(2) + " / " + resultSet.getObject(3) + " / " + resultSet.getObject(4) );
		}
		assertTrue("Should be able to get 1000 records in less than ten seconds", System.currentTimeMillis() - startTime < 13000 );
		resultSet.close();
		System.out.println( "Took " + (System.currentTimeMillis() - startTime) + " ms to complete query and get 1000 items." );
		try {
			resultSet.next();
			fail("Should throw an exception if we try to get more items after closing the ResultSet." );
		} catch(IllegalStateException ex) {
			System.out.println( "Expected this exception: " + ex );
		}
	}*/

	/** This passes 10/2/2006 --jsb */
	public void testInsertSpecialCharacters7() throws IOException, SQLException {
		InputStream stream = getClass().getResourceAsStream( "InsertText.txt" );
		byte[] buffer = new byte[stream.available()];
		assertEquals( buffer.length, stream.read( buffer ) );
		stream.close();
		String value = new String( buffer );

		{ //Test FM7
			String sql = "INSERT INTO Contacts (firstName) values(?)";
			PreparedStatement ps = connection7.prepareStatement( sql );
			ps.setString( 1, value ); //This is the critical line - we pass the string to the prepared statement instead of embedding it in the SQL
			ps.execute();
			ResultSet rs = ps.getGeneratedKeys();
			rs.next();
			int pk = rs.getInt( "ID" );
			rs = connection7.createStatement().executeQuery( "SELECT firstName FROM Contacts WHERE ID=" + pk );
			rs.next();
			assertEquals( value, rs.getString(1) );
			rs.close();
		}
	}

	/** This test fails 10/2/2006 - I can't figure out how to insert a curly quote (Õ) into FileMaker 6. --jsb */
	/*public void testInsertSpecialCharacters6() throws IOException, SQLException {
		InputStream stream = getClass().getResourceAsStream( "InsertText.txt" );
		byte[] buffer = new byte[stream.available()];
		assertEquals( buffer.length, stream.read( buffer ) );
		stream.close();
		String value = new String( buffer );

		{ //Test FM6
			String sql = "INSERT INTO Contacts.Contacts (firstName) values(?)";
			PreparedStatement ps = connection6.prepareStatement( sql );
			ps.setString( 1, value ); //This is the critical line - we pass the string to the prepared statement instead of embedding it in the SQL
			ps.execute();
			ResultSet rs = ps.getGeneratedKeys();
			rs.next();
			int pk = rs.getInt( "ID" );
			rs = connection6.createStatement().executeQuery( "SELECT firstName FROM Contacts.Contacts WHERE ID=" + pk );
			rs.next();
			assertEquals( value, rs.getString(1) );
			rs.close();
		}
	}*/

	/*public void testSelectSpecialCharacters6() throws SQLException {
		ResultSet rs = connection6.prepareStatement( "SELECT firstName FROM Contacts.Contacts WHERE ID=234" ).executeQuery();
		rs.next();
		String dbValue = rs.getString( 1 );
		System.out.println( dbValue );
		assertTrue( dbValue.indexOf( "Õ") >= 0 );
	}*/

	/** When you do a search in FileMaker, all search fields must be on the layout, otherwise you get an error 102. However, sometimes for speed
	 * or security reasons, you do not want to get all of the search fields returned in the result. If you specify your table name as 'searchLayout->displayLayout', 
	 * then the search will be done on the searchLayout, and the values returned will be from the displayLayout.
	 * This test shows an example of how this is done. */
	public void testSelectFromDifferentLayout() throws SQLException {
		//String tableName = jdbc.fmVersion >= 7 ? "Contacts" : "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?

		try {
			String sql = "SELECT * FROM Contacts_minimal where lastName='BARNUM' and firstName='Benjamin' and zip='94109'";
			statement7.executeQuery( sql );
			fail("Should fail with an error 102");
		} catch( SQLException e ) {
			assertEquals( 102, e.getErrorCode() );
		}

		String sql = "SELECT * FROM Contacts^^Contacts_minimal where lastName='BARNUM' and firstName='Benjamin' and zip='94109'";
		ResultSet rs = statement7.executeQuery( sql );
		rs.next();
		rs.getObject( "lastName" );
		rs.getObject( "firstName" );
		try {
			rs.getObject( "zip" );
			fail("Should not be able to get zip column, it is not on the Contacts_minimal layout");
		} catch(SQLException e) {
			//This is correct
		}
	}
}
