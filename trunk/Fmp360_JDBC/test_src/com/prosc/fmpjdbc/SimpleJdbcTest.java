package com.prosc.fmpjdbc;

import junit.framework.TestCase;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.prosc.shared.DebugTimer;

/** This test case is designed to test many of the basic functions of any JDBC driver. I have documented the cases
 * where this test fails for the ddtek driver, read the detailed method descriptions towards the bottom to see my notes
 * on these. You can run the tests yourself by installing <a href="http://ant.apache.org">ant</a> and then typing 'ant testddtek'
 * in the directory containing the build.xml file.
 */
public class SimpleJdbcTest extends TestCase {
	private static final Logger log = Logger.getLogger( SimpleJdbcTest.class.getName() );

	//private Connection connection;
	//private Statement statement;
	//private JDBCTestUtils jdbc;


	private Connection connection7;
	private Connection connection6;
	private Statement statement7;
	private Statement statement6;
	private JDBCTestUtils jdbc7;
	private JDBCTestUtils jdbc6;
	private boolean test6 = JDBCTestUtils.TEST_6; // where's the FM 6 server?

	protected void setUp() throws Exception {

		jdbc7 = new JDBCTestUtils();
		connection7 = jdbc7.getConnection();
		statement7 = connection7.createStatement();

		if (test6) {
			jdbc6 = new JDBCTestUtils();
			jdbc6.setFmVersion(6);
			connection6 = jdbc6.getConnection();
			statement6 = connection6.createStatement();
		}

		//jdbc = new JDBCTestUtils();
		//connection = jdbc.getConnection();
		//statement = connection.createStatement();
	}

	protected void tearDown() throws Exception {
		statement7.close();
		connection7.close();
		if (test6) {
			statement6.close();
			connection6.close();
		}
	}

	/** This test passes. */
	public void testConnectWithoutPassword() throws SQLException {
		try {
			jdbc7.getConnection( jdbc7.dbName, "nopassword", "" );
			// good
		} catch (SQLException sqle) {
			fail("This is a valid username/password combination for the db: " + jdbc7.dbName);
		}
	}

	public void testWrongHost() throws SQLException {
		try { //Try with hostname
			DriverManager.getConnection( "jdbc:fmp360://hercules.360works.com/Contacts", jdbc7.dbUsername, jdbc7.dbPassword );
			fail( "This is the wrong host; should have failed." );
		} catch( SQLException e ) {
			assertTrue( e.getMessage().indexOf( "Server returned unexpected status code: 404" ) >= 0 );
			//assertEquals( 802, e.getErrorCode() ); //802 means database is not open
		}

		try { //Try without hostname
			Connection connection = DriverManager.getConnection( "jdbc:fmp360://hercules.360works.com/", jdbc7.dbUsername, jdbc7.dbPassword ); //This by itself won't throw an exception, because we don't connect to FM server initially if no catalog is set
			connection.getMetaData(); //This will actually connect to FM to get catalog names, and should fail
			fail( "This is the wrong host; should have failed." );
		} catch( SQLException e ) {
			assertTrue( e.getMessage().indexOf( "java.io.IOException: Server returned unexpected status code: 404" ) >= 0 );
			//assertTrue( e.getMessage().indexOf( "IOException: Server has moved to new location" ) >= 0 );
		}
	}

	/** This test does not apply to the ddtek driver. */
	public void testRawConnection() throws IOException {
		if( jdbc7.use360driver ) {
			String passwordString = "";
			String authString = null;
			if( jdbc7.dbUsername != null && jdbc7.dbPassword != null ) {
				passwordString = jdbc7.dbUsername + ":" + jdbc7.dbPassword + "@";
				authString = new sun.misc.BASE64Encoder().encode( (jdbc7.dbUsername + ":" + jdbc7.dbPassword).getBytes() );
			}
			String rawUrl;

			//Try version 7
			rawUrl = "http://" + passwordString + jdbc7.xmlServer + ":" + jdbc7.port + "/fmi/xml/FMPXMLRESULT.xml?-db=Contacts&-lay=Contacts&-findall";
			rawConnection( rawUrl, authString );

			//Try version 6
			rawUrl = "http://" + passwordString + jdbc6.xmlServer + ":" + jdbc6.port + "/FMPro?-format=-fmp_xml&-db=Contacts&-lay=Contacts&-findall";
			rawConnection( rawUrl, authString );
		}
	}

	private void rawConnection( String rawUrl, String authString ) throws IOException {
		URL url = new URL(rawUrl);
		java.net.HttpURLConnection connection = (java.net.HttpURLConnection)url.openConnection();
		if( authString != null ) connection.setRequestProperty( "Authorization", "Basic " + authString );
		InputStream in = connection.getInputStream();
		byte[] bytes = new byte[in.available()];
		in.read( bytes );
		String result = new String(bytes);
		int mark = result.indexOf( "<ERRORCODE>");
		if( mark == -1 ) fail("Could not find <ERRORCODE> tag");
		mark += "<ERRORCODE>".length();
		int mark2 = result.indexOf( "</ERRORCODE" );
		String errorCode = result.substring( mark, mark2 );
		assertEquals( "0", errorCode );
	}

	/** This test passes. */
	public void testSelectAllWithLayout() throws SQLException {
		//First try 7
		String tableName = "Contacts";
		ResultSet resultSet = statement7.executeQuery( "SELECT * FROM \""+tableName+"\"" );
		assertEquals( "firstName", resultSet.getMetaData().getColumnName(1) );

		//Then try 6
		if (test6) {
			tableName = "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?
			resultSet = statement6.executeQuery( "SELECT * FROM \""+tableName+"\"" );
			assertEquals( "firstName", resultSet.getMetaData().getColumnName(1) );
		}
	}

	/** This test passes. */
	public void testSimpleInsert() throws SQLException {
		//First try 7
		String tableName = "Contacts";
		String sql = "INSERT INTO \""+tableName+"\" (firstName, lastName, emailAddress) values('Al', 'Leong', 'kungfudude@mcgyver.com')";
		int rowCount = statement7.executeUpdate( sql );
		assertEquals( 1, rowCount );

		//Then try 6
		if (test6) {
			tableName = "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?
			//sql = "INSERT INTO \"Contact profiles\" LAYOUT WebObjects (Contact,Email,\"Active status\") values('Al Leong', 'kungfudude@mcgyver.com', 'Inactive')";
			sql = "INSERT INTO \""+tableName+"\" (firstName, lastName, emailAddress) values('Al', 'Leong', 'kungfudude@mcgyver.com')";
			rowCount = statement6.executeUpdate( sql );
			assertEquals( 1, rowCount );
		}
	}

	/** This test passes. */
	public void testSimpleSelect() throws SQLException {
		//First try 7
		String tableName = "Contacts";
		String sql = "SELECT * FROM "+tableName+" where lastName='Leong' and firstName='Al'";
		ResultSet resultSet = statement7.executeQuery( sql );
		simpleSelectCheck( resultSet );

		//Then try 6
		if (test6) {
			tableName = "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?
			sql = "SELECT * FROM "+tableName+" where lastName='Leong' and firstName='Al'";
			resultSet = statement6.executeQuery( sql );
			simpleSelectCheck( resultSet );
		}
	}

	private int simpleSelectCheck( ResultSet resultSet ) throws SQLException {
		int rowCount = 0;
		while( resultSet.next() ) {
			rowCount++;
			if( rowCount == 1 ) assertEquals( "Al", resultSet.getObject("firstName") );
		}
		assertTrue( "No results in found set", rowCount > 0 );
		return rowCount;
	}

	/** This test sometimes fails. Still working on figuring out exactly why. */
	public void testSortedSelect() throws SQLException {
		//First try 7
		String tableName = "Contacts";
		sortedAssertion( statement7.executeQuery( "SELECT * FROM " + tableName + " ORDER BY firstName" ), true );
		sortedAssertion( statement7.executeQuery( "SELECT * FROM " + tableName + " ORDER BY firstName asc" ), true );
		sortedAssertion( statement7.executeQuery( "SELECT * FROM " + tableName + " ORDER BY firstName desc" ), false );

		//Then try 6
		if (test6) {
			tableName = "Contacts.Contacts"; //Need to include the db & layout name if using 6, right?
			sortedAssertion( statement6.executeQuery( "SELECT * FROM " + tableName + " ORDER BY firstName" ), true );
			sortedAssertion( statement6.executeQuery( "SELECT * FROM " + tableName + " ORDER BY firstName asc" ), true );
			sortedAssertion( statement6.executeQuery( "SELECT * FROM " + tableName + " ORDER BY firstName desc" ), false );
		}
	}

	private void sortedAssertion(ResultSet rs, boolean isAscending) throws SQLException {
		String lastValue = null;
		while( rs.next() ) {
			String eachValue = rs.getString("firstName");
			//System.out.println( eachValue );
			if( lastValue != null ) {
				int comparison = lastValue.toLowerCase().compareTo( eachValue.toLowerCase() );
				if( isAscending ) assertTrue( lastValue + " should have been before " + eachValue, comparison <= 0 );
				else  assertTrue( lastValue + " should have been after " + eachValue, comparison >= 0 );
			}
			lastValue = eachValue;
		}
	}

	/** This test passes. */
	public void testSelectWithNamedAttributes() throws SQLException {
		selectWithNamedAttributes( "Contacts", statement7 ); //First try 7
		if (test6) {
			selectWithNamedAttributes( "Contacts.Contacts", statement6 ); //Then try 6
		}
	}

	private void selectWithNamedAttributes( String tableName, Statement statement ) throws SQLException {
		ResultSet resultSet = statement.executeQuery( "SELECT \"firstName\", \"lastName\", emailAddress, state, city, ZIP from \""+tableName+"\" where lastName='Leong' and CITY ='San Francisco'" );
		//ResultSet resultSet = statement.executeQuery( "SELECT \"firstName\", \"lastName\", emailAddress, state, city from \"Contacts\" where lastName='Erdos' and CITY ='Budapest'" );
		int rowCount = 0;
		while( resultSet.next() ) {
			rowCount++;
			if( rowCount == 1 ) assertEquals( "leong", resultSet.getObject("lastName").toString().toLowerCase() );

		}
		assertTrue( "No results in found set", rowCount > 0 );
		ResultSetMetaData metaData = resultSet.getMetaData();
		assertEquals( "Should be five rows in result set", 6, metaData.getColumnCount() );
		assertEquals( "firstName", metaData.getColumnName(1) );
		assertEquals( "lastName", metaData.getColumnName(2) );
		assertEquals( "emailAddress", metaData.getColumnName(3) );
		assertEquals( "state", metaData.getColumnName(4) ); //Deliberately flipped the order on this - we should be returning them in the order requested
		assertEquals( "city", metaData.getColumnName(5) );
		assertEquals( "zip", metaData.getColumnName(6) );
	}

	/** This test passes. */
	public void testSimpleUpdate() throws SQLException {
		simpleUpdate( "Contacts", statement7 );
		if (test6) {
			simpleUpdate( "Contacts.Contacts", statement6 );
		}
	}

	private void simpleUpdate( String tableName, Statement statement ) throws SQLException {
		String sql = "INSERT INTO "+tableName+" ( FIRSTNAME, LASTNAME, EMAILADDRESS, \"COMPANY ID\") VALUES( 'John', 'Hero', 'zero@yahoo.com', '3')";
		int rowCount = statement.executeUpdate( sql );
		assertEquals( "Should have inserted one row", 1, rowCount );
		sql = "UPDATE \""+tableName+"\" set \"LASTNAME\"='Zero' where emailAddress='zero@yahoo.com'"; //Currently fails with '@' symbol in search
		//String sql = "UPDATE \"Contacts\" set \"ZIP\"='98989' where lastName='leong'";
		rowCount = statement.executeUpdate( sql );
		assertTrue( "No results in found set", rowCount > 0 );
	}

	/** This test fails because the ddtek driver does not support the relationshipName::field syntax of FileMaker.
	 * This does not seem like a problem, since we can use table aliases with joins now instead.
	 * @throws SQLException
	 */
	public void testRelationalSearch() throws SQLException {
		relationalSearch( "Company", statement7 ); //Test 7
		if (test6) {
			relationalSearch( "Company.company", statement6 ); //Test 6
		}
	}

	private void relationalSearch( String tableName, Statement statement ) throws SQLException {
		String clientName = "Long-haired Kung Fu Dudes R Us";
		ResultSet rs = statement.executeQuery( "SELECT ID from " + tableName + " where \"ID\"='100'" );
		if( rs.next() == false ) { //Record doesn't exist, create it
			statement.execute( "INSERT INTO CONTACTS (FIRSTNAME, LASTNAME, \"COMPANY ID\") VALUES('John', 'Doe', 100)" );
			statement.executeUpdate( "INSERT INTO \"COMPANY\" (Name,ID) values('" + clientName + "','100') " );
		}
		rs = statement.executeQuery( "SELECT name, ID from " + tableName + " where \"Contacts::lastName\"='DOE' AND \"Contacts::firstName\"='JOHN'" ); //ddtek driver does not support :: notation for accessing related fields
		rs.next();
		assertEquals( clientName, rs.getObject("name"));
		statement.close();
	}

	/** This test passes. */
	public void testManyFieldsEdit() throws SQLException {
		statement7.executeUpdate( "DELETE FROM ManyTextFields where \"text1\" = 'jeremiah'" );
		//This has double quotes for the value, which kills the xDBC driver: int insertedCount = statement.executeUpdate( "INSERT INTO ManyTextFields(\"text1\") values(\"jeremiah\")" );
		int insertedCount = statement7.executeUpdate( "INSERT INTO ManyTextFields(\"text1\") values('jeremiah')" );
		assertEquals( 1, insertedCount );
		java.util.Date then = new java.util.Date();
		int updatedRowCount = statement7.executeUpdate( "UPDATE ManyTextFields set \"text1 Copy\" = 'jones' where \"text1\" = 'jeremiah'");
		java.util.Date now = new java.util.Date();
		assertEquals( 1, updatedRowCount );
		long milliseconds = now.getTime() - then.getTime();
		long maxTime = 2500;
		System.out.println("textManyFieldsEdit: " + milliseconds + " ms");
		assertTrue( "Statement took " + milliseconds + " to execute; should be less than " + maxTime, milliseconds < maxTime );
	}

	//Test invalid relationships
	/** This test passes. */
	public void testSimpleDelete() throws SQLException {
		simpleDelete( "Contacts", statement7 );
		if (test6) {
			simpleDelete( "Contacts.Contacts", statement6 );
		}
	}

	private void simpleDelete( String tableName, Statement statement ) throws SQLException {//int rowCount = statement.executeUpdate("INSERT INTO "+tableName+" ( FIRSTNAME, LASTNAME, \"COMPANY ID\") VALUES( 'Jesse', 'Barnum', 3)" );
		//assertEquals( "Should have inserted one row", 1, rowCount );
		String sql = "INSERT INTO "+tableName+" ( FIRSTNAME, LASTNAME, \"COMPANY ID\") VALUES( 'John', 'Hero', '3')";
		int rowCount = statement.executeUpdate( sql );
		assertEquals( "Should have inserted one row", 1, rowCount );
		//rowCount = statement.executeUpdate("INSERT INTO "+tableName+"( FIRSTNAME, LASTNAME, \"COMPANY ID\") VALUES( 'Jesse', 'Spen', 4)" );
		//assertEquals( "Should have inserted one row", 1, rowCount );
		//rowCount = statement.executeUpdate("INSERT INTO "+tableName+"( FIRSTNAME, LASTNAME, \"COMPANY ID\") VALUES( 'Jesse', 'Barnum', 5)" );
		//assertEquals( "Should have inserted one row", 1, rowCount );
		rowCount = statement.executeUpdate( "DELETE FROM \""+tableName+"\" where lastName LIKE 'Hero'");
		assertTrue( "Should have found at least one row to delete", rowCount >= 1 ); //ddtek driver fails here, because searches are case-sensitive
	}

	/** This test passes for FM7 */
	public void testTimestampParsing7 () throws SQLException {
		//FIX!! This is a bad test, it should create some sample data to test with. We're getting empty dates that are causing parse errors. Or maybe we should be able to handle this? --jsb
		timestampParsing( "Contacts", statement7 ); //First test 7
	}

	/** This test fails for FM6, because the FM6 XML returns dates like this '2 pm' exactly as the user entered, without normalizing them. We can fix this but it will be some work. */
	public void testTimestampParsing6() throws SQLException {
		if (test6) {
			timestampParsing( "Contacts.Contacts", statement6 ); //Then test 6
		}
	}

	private void timestampParsing( String tableName, Statement statement ) throws SQLException {
		ResultSet rs = statement.executeQuery("SELECT * from "+tableName+" where ID='144'");
		rs.next();
		System.out.println(rs.getString(1) );
		System.out.println(rs.getString(2) );
		System.out.println(rs.getString(3) );
		System.out.println(rs.getString(4) );
		System.out.println(rs.getString(5) );
		System.out.println("City: " + rs.getString(14) );
		System.out.println("State: " + rs.getString(15) );
		System.out.println("ZIP: " + rs.getString(16) );
		System.out.println("Timestamp: " + rs.getTimestamp(6) );
	}


	/** This test fails because the ddtek ResultSet implementation does not support getting a FileMaker number field as
	 * an integer with the getInt() call. Although there are workarounds for this, this seems like a pretty basic expectation.
	 * It also fails on calls to getBigDecimal(), which is the Java class for dealing with arbitrary decimal precision.
	 * I think that the only numeric type supported by the ddtek driver is Double, which has less decimal precision than
	 * FileMaker's native calculation engine, more decimal precision than is needed for most operations, and is a poor choice
	 * for dealing with fixed decimal values like monetary units.
	 * @throws SQLException
	 */
	public void testDataParsing() throws SQLException {
		dataParsing( "Portrait", statement7 ); //First test 7
	}

	/** This test fails for FM6, because of date parsing problems with 6 */
	public void testDataParsing6() throws SQLException {
		dataParsing( "Portrait.portrait", statement6 ); //Then test 6
	}

	private void dataParsing( String tableName, Statement statement ) throws SQLException {
		ResultSet rs = statement.executeQuery("SELECT * from " + tableName);
		while( rs.next() ) {
			rs.getInt(1);
			rs.getBlob(2);
			rs.getString(3);
			rs.getString(4);
			rs.getBlob(5);
			rs.getBigDecimal(6); //ddtek driver fails on getBigDecimal() call
			rs.getInt(6); //ddtek drivers fails on getInt() call
			rs.getDate(7);
			rs.getInt(8);
			rs.getTime(9);
			rs.getTimestamp(10);
			rs.getDouble(11);
		}
	}

	/** This test fails because the ResultSet getObject() method returns a Double value for numeric fields, where I expect it
	 * to return a BigDecimal instead, because that way you are guaranteed to not lose precision, and because FileMaker's internal
	 * FMXFixPt representation maps very closely to BigDecimal. This is a debatable point; there is no clear-cut 'right' answer
	 * in this case, so this can be safely ignored.
	 * @throws SQLException
	 */
	public void testGetObjectParsing7() throws SQLException {
		getObjectParsing( "Portrait", statement7 ); //First test 7
	}


	/** This test fails for FM6, because of date parsing problems with 6. */
	public void testGetObjectParsing6() throws SQLException {
		if (test6) {
			getObjectParsing( "Portrait.portrait", statement6 ); //Then test 6
		}
	}

	private void getObjectParsing( String tableName, Statement statement ) throws SQLException {
		ResultSet rs = statement.executeQuery("SELECT * from " + tableName);
		Object eachValue;
		while( rs.next() ) {
			eachValue =  rs.getObject(1);
			if (eachValue != null) assertEquals( BigDecimal.class, eachValue.getClass() ); //Should FileMaker number return a Double or a BigDecimal? ddtek returns a Double, our driver returns a BigDecimal.
			eachValue = rs.getObject(2);
			if (eachValue != null) assertTrue( eachValue instanceof Blob );
			eachValue = rs.getObject(3);
			if (eachValue != null) assertEquals( String.class, eachValue.getClass() );
			eachValue = rs.getObject(4);
			if (eachValue != null) assertEquals( String.class, eachValue.getClass() );
			eachValue = rs.getObject(5);
			if (eachValue != null) assertTrue( eachValue instanceof Blob );
			eachValue = rs.getObject(6);
			if (eachValue != null) assertEquals( BigDecimal.class, eachValue.getClass() );
			eachValue = rs.getObject(7);
			if (eachValue != null) assertEquals( Date.class, eachValue.getClass() );
			eachValue = rs.getObject(8);
			if (eachValue != null) assertEquals( String.class, eachValue.getClass() );
			eachValue = rs.getObject(9);
			if (eachValue != null) assertEquals( Time.class,eachValue.getClass() );
			eachValue = rs.getObject(10);
			if (eachValue != null) assertEquals( Timestamp.class, eachValue.getClass() );
			eachValue =  rs.getObject(11);
			if (eachValue != null) assertEquals( BigDecimal.class, eachValue.getClass() );
		}
	}


	/** Test various bad number formats */
	public void testNumberParsing() throws SQLException {
		numberParsing( "WeirdTextFields", statement7 ); //First test 7
		//We don't have this database set up on our 6 server: numberParsing( "WeirdTextFields.WeirdTextFields", statement6 ); //Then test 6
	}

	private void numberParsing( String tableName, Statement statement ) throws SQLException {
		ResultSet rs = statement.executeQuery("SELECT * from " + tableName);
		Number eachValueNumber;
		int rowNum = 0;
		while( rs.next() ) {
			rowNum++;
			eachValueNumber =  (Number)rs.getObject(1);
			if( rs.wasNull() ) {
				eachValueNumber = null;
			}
			if (eachValueNumber != null) assertEquals( BigDecimal.class, eachValueNumber.getClass() ); //Should FileMaker number return a Double or a BigDecimal? ddtek returns a Double, our driver returns a BigDecimal.
			String eachValueString = rs.getString(1);
			Number eachValueExpected = rs.getBigDecimal(2);
			if( rs.wasNull() ) eachValueExpected = null;
			rs.getDate( "DateValue" ); //Just to see if we get an exception
			if (eachValueNumber != null) assertEquals( BigDecimal.class, eachValueNumber.getClass() );
			int i = rs.getInt( "Integer value" );
			if( eachValueNumber == null && ! rs.wasNull() ) {
				eachValueNumber = new Integer( i );
			}
			String comment = rs.getString("Comment");
			log.info( comment + ": (" + eachValueString + ") is read as (" + eachValueNumber + ")" );
			assertEquals( String.valueOf( eachValueExpected ), String.valueOf( eachValueNumber ) );
		}
	}


	/** This test fails because the ddtek Statement implementations does not support the getGeneratedkeys() method, which is the
	 * only way to get the serial number of a newly-created FileMaker record. <b>This is a critical failure which must be fixed in
	 * order for us to be able to use this driver.</b>
	 * @throws SQLException
	 */
	public void testAutoGeneratedKeys() throws SQLException {
		autoGeneratedKeys( "Contacts", statement7 ); //First test 7
		if (test6) {
			autoGeneratedKeys( "Contacts.Contacts", statement6 ); //Then test 6
		}
	}

	private void autoGeneratedKeys( String tableName, Statement statement ) throws SQLException {//sql = "INSERT INTO \"Contact profiles\" LAYOUT WebObjects (Contact,Email,\"Active status\") values('Al Leong', 'kungfudude@mcgyver.com', 'Inactive')";
		String sql = "INSERT INTO \""+tableName+"\" (firstName, lastName, emailAddress) values ('Al', 'Leong', 'kungfudude@mcgyver.com')";
		int rowCount = statement.executeUpdate( sql );
		assertEquals( 1, rowCount );
		ResultSet rs = statement.getGeneratedKeys(); //dtek driver does not support FMP serial #'s; this is a serious problem
		assertTrue( "Should be at least one row in generated keys result set", rs.next() );
		assertEquals( 4, rs.getMetaData().getColumnCount() ); //FIX Why is this 3?-Jo
		Set autoColumns = new HashSet();
		autoColumns.add( rs.getMetaData().getColumnName(1) );
		autoColumns.add( rs.getMetaData().getColumnName(2) );
		autoColumns.add( rs.getMetaData().getColumnName(3) );
		autoColumns.add( rs.getMetaData().getColumnName(4) );

		assertTrue( autoColumns.contains( "ID" ) );
		assertTrue( autoColumns.contains( "recid" ) );
		assertTrue( autoColumns.contains( "city" ) );
		assertTrue( autoColumns.contains( "Timestamp created" ) );

		String idString = rs.getObject("ID").toString();
		int idInt = Integer.valueOf(idString).intValue();
		assertTrue( "ID should be an integer greater than zero", idInt > 0 );
	}

	/** This test passes. */
	public void testCaseSensitivity() throws SQLException {
		statement7.executeUpdate( "DELETE FROM Contacts where firstName LIKE 'toomsuba'" );
		statement7.executeUpdate( "DELETE FROM Contacts where firstName LIKE 'Toomsuba'" );
		String sql = "SELECT * FROM Contacts where firstName='Toomsuba'";
		sql = "SELECT * FROM Contacts where firstName='toomsuba'";
		ResultSet resultSet = statement7.executeQuery( sql );
		//statement.executeUpdate( "DELETE FROM Contacts where firstName LIKE 'Toomsuba'" );
		//statement.executeUpdate( "INSERT INTO Contacts(firstName) values 'Olivia'" );
		//statement.executeUpdate( "INSERT INTO Contacts(firstName) values 'Olivia'" );
		//statement.executeUpdate( "INSERT INTO Contacts(firstName) values 'olivia'" );
		statement7.executeUpdate( "INSERT INTO Contacts(firstName,lastName) values ('TOOMSUBA','Riley')" );
		statement7.executeUpdate( "INSERT INTO Contacts(firstName,lastName) values ('Toomsuba','Rawley')" );
		statement7.executeUpdate( "INSERT INTO Contacts(firstName,lastName) values ('toomsuba','Bailey')" );
		int foundCount;
		if( jdbc7.use360driver ) {
			foundCount = statement7.executeUpdate( "DELETE FROM Contacts where firstName LIKE 'TOOMSUBA'" );
		} else {
			foundCount = statement7.executeUpdate( "DELETE FROM Contacts where UCASE(firstName) LIKE 'TOOMSUBA'" );
		}
		if( foundCount == 2 ) fail( "Only found the 2 uppercase records, did not correctly do case-sensitive search" );
		assertEquals( 3, foundCount );
	}

	/** This test does not apply to the ddtek driver. */
	/* I've disabled this because it takes a long time to run, and it's only purpose is for benchmarking, not for unit testing. --jsb
	public void testXmlSpeed() throws IOException {
		if (jdbc.use360driver) {
			if( jdbc.fmVersion < 7 ) fail("Only the FMPXMLRESULT format is supported in pre-7 FileMaker." );

			URL format1 = new URL("http://" + jdbc.xmlServer + "/fmi/xml/FMPXMLRESULT.xml?-db=Contacts&-field=firstName&-lay=Contacts&-find");
			URL format2 = new URL("http://" + jdbc.xmlServer + "/fmi/xml/fmresultset.xml?-db=Contacts&-field=firstName&-lay=Contacts&-find");
			URL format3 = new URL("http://" + jdbc.xmlServer + "/fmi/xml/FMPDSORESULT.xml?-db=Contacts&-field=firstName&-lay=Contacts&-find");


			int loops=3;
			DebugTimer dt = new DebugTimer("FMPXMLRESULT");
			for( int n=1; n<=loops; n++ ) {
				dt.markTime("Starting loop " + n);
				URLConnection connection = format1.openConnection();
				connection.setUseCaches(false);
				InputStream stream = connection.getInputStream();
				IOUtils.inputStreamAsBytes( stream );
				stream.close();
			}

			dt.markTime("fmresultset");
			for( int n=1; n<=loops; n++ ) {
				dt.markTime("Starting loop " + n);
				URLConnection connection = format2.openConnection();
				connection.setUseCaches(false);
				InputStream stream = connection.getInputStream();
				IOUtils.inputStreamAsBytes( stream );
				stream.close();
			}

			dt.markTime("FMPDSORESULT");
			for( int n=1; n<=loops; n++ ) {
				dt.markTime("Starting loop " + n);
				URLConnection connection = format3.openConnection();
				connection.setUseCaches(false);
				InputStream stream = connection.getInputStream();
				IOUtils.inputStreamAsBytes( stream );
				stream.close();
			}

			dt.stop();
		}
	}*/

	//FIX!! Add test for null searches, and various operators ie. begins with, ends with, contains, not equals, etc.

	/** This test passes.  This test is for testing LIKE opperator in the WHERE clause */
	public void testSelectWithExactWordMatch() throws SQLException {
		selectWithExactWordMatch( "Contacts", statement7 ); //First test 7
		//selectWithExactWordMatch( "Contacts.Contacts", statement6 ); //Then test 6
	}

	private void selectWithExactWordMatch( String tableName, Statement statement ) throws SQLException {
		String sql = "SELECT * FROM " + tableName + " where firstName LIKE 'sam'";

		ResultSet resultSet = statement.executeQuery( sql );
		int rowCount = 0;
		while( resultSet.next() ) {
			rowCount++;

			System.out.println(resultSet.getObject("firstName") + " PASSWORD:  " + resultSet.getObject("lastName"));
		}
	}

	public void testWorkingXmlParser() throws ParserConfigurationException, SAXException {
		javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser();
	}

}