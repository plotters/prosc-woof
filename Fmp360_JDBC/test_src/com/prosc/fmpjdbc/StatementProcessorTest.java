package com.prosc.fmpjdbc;

/**
 * @author sbarnum
 */

import junit.framework.*;
import com.prosc.fmpjdbc.StatementProcessor;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Connection;

public class StatementProcessorTest extends TestCase {
	private Statement statement;
	/* I disabled this - it is only for Sherry Tirko --jsb
	public static final String SQL_LONG = "UPDATE \"AVCUSTS|webobjects\" " +
		   "SET \"CustomerBilling Zip\" = ?, " +
		   "\"CustomerShipping Zip\" = ?, " +
		   "\"CustomerBilling City\" = ?, " +
		   "\"CustomerContact Phone\" = ?, " +
		   "\"CustomerShipping Address\" = ?, " +
		   "\"CustomerBilling Department\" = NULL, " +
		   "\"CustomerBilling Room Building\" = NULL, " +
		   "\"CustomerBilling Address\" = ?, " +
		   "\"CustomerContact Fax\" = ?, " +
		   "\"CustomerShipping City\" = ?, " +
		   "\"CustomerShipping Room Building\" = NULL, " +
		   "\"CustomerShipping Department\" = NULL " +
		   "WHERE (\"CustomerCode\" = ? " +
		   "AND \"ponumber\" is NULL " +
		   "AND \"upsnumber\" is NULL " +
		   "AND \"fedexnumber\" is NULL " +
		   "AND \"friendsofpennstate\" = ? " +
		   "AND \"CustomerName\" = ? " +
		   "AND \"CustomerShipping Department\" = ? " +
		   "AND \"CustomerShipping Room Building\" = ? " +
		   "AND \"CustomerShipping Address\" = ? " +
		   "AND \"CustomerShipping City\" = ? " +
		   "AND \"CustomerShipping State\" = ? " +
		   "AND \"CustomerShipping Zip\" = ? " +
		   "AND \"customerShipping Attn\" = ? " +
		   "AND \"CustomerCategory\" is NULL " +
		   "AND \"CustomerSolicitation Code\" = ? " +
		   "AND \"CustomerContact Name Prefix\" = ? " +
		   "AND \"CustomerContact Name First\" = ? " +
		   "AND \"CustomerContact Name Middle Initial\" = ? " +
		   "AND \"CustomerContact Name Last\" = ? " +
		   "AND \"CustomerContact Name Suffix\" is NULL " +
		   "AND \"CustomerContact Phone\" = ? " +
		   "AND \"CustomerContact Fax\" is NULL " +
		   "AND \"CustomerContact Email\" = ? " +
		   "AND \"CustomerGrant Category\" is NULL " +
		   "AND \"CustomerBilling Name\" = ? " +
		   "AND \"CustomerBilling Department\" = ? " +
		   "AND \"CustomerBilling Room Building\" = ? " +
		   "AND \"CustomerBilling Address\" = ? " +
		   "AND \"CustomerBilling City\" = ? " +
		   "AND \"CustomerBilling State\" = ? " +
		   "AND \"CustomerBilling Zip\" = ? " +
		   "AND \"CustomerBilling Attn\" = ? " +
		   "AND \"Date Creationcalc\" = ? " +
		   "AND \"taxexemptCheck\" = ? " +
		   "AND \"taxexemptOnfile\" is NULL " +
		   "AND \"taxexemptNumber\" = ? " +
		   "AND \"Date Modification\" = ? " +
		   "AND \"federalid\" is NULL)"; */

	public StatementProcessorTest() throws Exception {
		statement = new JDBCTestUtils().getConnection().createStatement();
	}

	/* I disabled this test - it only applies to Sherry Tirko; we don't host this database. --jsb
	public void test_escapeFMWildCards6() throws Exception {
		System.setProperty("fmVersion", "6");
		Connection connection = new JDBCTestUtils().getConnection();
		FmPreparedStatement stmt = (FmPreparedStatement) connection.prepareStatement(SQL_LONG);
		//
		stmt.setObject(1, "16877"); stmt.setObject(2, "16877"); stmt.setObject(3, "WARRIORS MARK"); stmt.setObject(4, ""); stmt.setObject(5, "247 FYE RD."); stmt.setObject(6, "247 FYE RD."); stmt.setObject(7, ""); stmt.setObject(8, "WARRIORS MARK"); stmt.setObject(9, "9713"); stmt.setObject(10, "slt5025"); stmt.setObject(11, "MRS. SHERRY L TIRKO"); stmt.setObject(12, "Outreach"); stmt.setObject(13, "USB 1 Suite D"); stmt.setObject(14, "123 Testing Ln"); stmt.setObject(15, "STATE COLLEGE"); stmt.setObject(16, "PA"); stmt.setObject(17, "16801"); stmt.setObject(18, "MRS. SHERRY L TIRKO"); stmt.setObject(19, "web"); stmt.setObject(20, "MRS."); stmt.setObject(21, "SHERRY"); stmt.setObject(22, "L"); stmt.setObject(23, "TIRKO"); stmt.setObject(24, ""); stmt.setObject(25, "slk24@outreach.psu.edu"); stmt.setObject(26, "MRS. SHERRY L TIRKO"); stmt.setObject(27, "Outreach"); stmt.setObject(28, "USB 1 Suite D"); stmt.setObject(29, "123 Testing Ln"); stmt.setObject(30, "STATE COLLEGE"); stmt.setObject(31, "PA"); stmt.setObject(32, "16801"); stmt.setObject(33, "MRS. SHERRY L TIRKO"); stmt.setObject(34, "2006-04-25 00:00:00"); stmt.setObject(35, "Yes"); stmt.setObject(36, "12345678"); stmt.setObject(37, "2006-05-16 00:00:00");
		//stmt.execute();
		StatementProcessor processor = stmt.processor();
		processor.setParams(stmt.params);
		processor.execute();
	}*/

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

	/**
	 * @TestFails wildcards in the middle of a search term are not currently supported. --jsb
	 */
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

	/**
	 * @throws Exception
	 * @TestFails we use a '=' operator, not a '==' operator, in FileMaker. This makes text searches MUCH faster, but
	 * returns false matches if the search word occurs with other words in the same field. --jsb
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

	public void testOrLogicalOperator() throws Exception {
		ResultSet resultSet = statement.executeQuery("SELECT ID, firstName, lastName FROM Contacts WHERE firstName='Sam' OR firstName='Benjamin' ORDER BY firstName");
		assertTrue(resultSet.next());
		assertEquals("benjamin", resultSet.getString(2).toLowerCase());
		//
		assertTrue(resultSet.next());
		assertEquals("sam", resultSet.getString(2).toLowerCase());
		//
		while (resultSet.next()) {
			assertEquals("sam", resultSet.getString(2).toLowerCase());
		}
	}
}