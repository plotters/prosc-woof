package com.prosc.fmpjdbc;

/**
 * @author sbarnum
 */

import junit.framework.*;
import com.prosc.fmpjdbc.StatementProcessor;

import java.sql.Statement;
import java.sql.ResultSet;

public class StatementProcessorTest extends TestCase {
	private Statement statement;

	public StatementProcessorTest() throws Exception {
		statement = new JDBCTestUtils().getConnection().createStatement();
	}

	public void test_escapeFMWildCards7() throws Exception {
		StringBuffer toAppendTo = new StringBuffer();
		new StatementProcessor(null, null)._escapeFMWildCards7("a@b", toAppendTo, "!#@$%^");
		assertEquals("a\\@b", toAppendTo.toString());
	}

	public void testLikeSearchMatch() throws Exception {
		// test begins with
		ResultSet resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName LIKE 'Barn%'");
		assertTrue(resultSet.next());
		do {
			//assertTrue(resultSet.getString(2).toLowerCase().startsWith("barn")); // NOTE! this doesn't actually work.  FMP will find all fields containing a word beginning with "Barn", not just ones where the entire field begins with "Barn"
		} while (resultSet.next());

		// test ends with
		resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName LIKE '%arnum'");
		assertTrue(resultSet.next());
		do {
			//assertTrue(resultSet.getString(2).toLowerCase().endsWith("arnum")); // NOTE! this doesn't work, see above
		} while (resultSet.next());

		// test contains
		resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName LIKE '%arn%'");
		assertTrue(resultSet.next());
		do {
			assertTrue(resultSet.getString(2).toLowerCase().indexOf("arn") >= 0);
		} while (resultSet.next());

		// test like, totally wild pattern, man
		resultSet = statement.executeQuery("SELECT * FROM foo WHERE foo LIKE '%'");
		assertTrue(resultSet.next());
	}

	/** @TestFails wildcards in the middle of a search term are not currently supported. --jsb */
	public void testLikeSearchMiddleCharsNotImpl() throws Exception {
		// test middle chars.  This isn't supported by our JDBC driver.
		ResultSet resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName LIKE 'Ba%um'");
		assertTrue(resultSet.next());
		do {
			String lastName = resultSet.getString(2).toLowerCase();
			assertTrue(lastName.startsWith("ba"));
			assertTrue(lastName.endsWith("um"));
		} while (resultSet.next());
	}

	public void testLikeSearchNoMatch() throws Exception {
		// test missing wildcard
		ResultSet resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName LIKE 'Barn'");
		assertFalse(resultSet.next());

		// test wrong wildcard, should be escaped
		resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName LIKE 'Barn*'");
		assertFalse(resultSet.next());

	}

	/*
	public void testLikeSearchPreparedStatement() throws Exception {
		fail("Not implemented");
	}

	public void testEqualsSearchPreparedStatement() throws Exception {
		fail("Not implemented");
	}
	*/

	/** @TestFails we use a '=' operator, not a '==' operator, in FileMaker. This makes text searches MUCH faster,
	 * but returns false matches if the search word occurs with other words in the same field. --jsb
	 * @throws Exception
	 */
	public void testEqualsSearchMatch() throws Exception {
		ResultSet resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName='Barnum'");
		assertTrue(resultSet.next());
		do {
			assertEquals("barnum", resultSet.getString("lastName").toLowerCase());
		} while (resultSet.next());
	}

	public void testEqualsSearchNoMatch() throws Exception {
		ResultSet resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName='Barn'"); // shouldn't find any matches, because we're doing an exact search
		assertFalse(resultSet.next());
	}

	public void testEscapeRangeSearch() throws Exception {
		ResultSet resultSet = statement.executeQuery("SELECT ID, lastName FROM Contacts WHERE lastName='Ba�Ca'");
		assertFalse(resultSet.next()); // should have escaped the ellipses
	}

}