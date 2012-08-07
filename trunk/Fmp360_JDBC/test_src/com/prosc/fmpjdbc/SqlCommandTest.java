package com.prosc.fmpjdbc;

/**
 * @author sbarnum
 */

import junit.framework.*;
import com.prosc.fmpjdbc.SqlCommand;

import java.util.Iterator;

public class SqlCommandTest extends TestCase {
	SqlCommand sqlCommand;
	private String query;

	protected void setUp() throws Exception {
		try {
			query = "SELECT id FROM foo " +
			        "WHERE columnName='joe' AND rank>4 AND level >= 2 AND parentID < 0 AND rootID <= 100 AND status <> 'inactive' AND otherStatus != 'active' " +
			        "AND nullColumn=NULL AND firstName LIKE 's%' AND firstName LIKE '%m' AND firstName LIKE '%a%' AND preparedField=? AND rangeSearch > 1 AND rangeSearch < 10 " +
			        "ORDER BY id, columnName DESC, rank ASC";
			sqlCommand = new SqlCommand(query);
		} catch (SqlParseException e) {
			throw new RuntimeException(e);
		}
	}

	public void testGetTable() {
		assertEquals("foo", sqlCommand.getTable().getName());
	}

	public void testGetSortTerms() throws Exception {
		Iterator iterator = sqlCommand.getSortTerms().iterator();
		SortTerm aSortTerm = (SortTerm) iterator.next();
		assertEquals(SortTerm.ASCENDING, aSortTerm.getOrder());
		assertEquals("foo", aSortTerm.getField().getTable().getName());
		assertEquals("id", aSortTerm.getField().getColumnName());

		aSortTerm = (SortTerm) iterator.next();
		assertEquals(SortTerm.DESCENDING, aSortTerm.getOrder());
		assertEquals("foo", aSortTerm.getField().getTable().getName());
		assertEquals("columnName", aSortTerm.getField().getColumnName());

		aSortTerm = (SortTerm) iterator.next();
		assertEquals(SortTerm.ASCENDING, aSortTerm.getOrder());
		assertEquals("foo", aSortTerm.getField().getTable().getName());
		assertEquals("rank", aSortTerm.getField().getColumnName());

		assertFalse(iterator.hasNext()); // should be done
	}

	public void testGetSearchTerms() throws Exception {
		Iterator iterator = sqlCommand.getSearchTerms().iterator();
		SearchTerm aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("foo", aSearchTerm.getField().getTable().getName());
		assertEquals("columnName", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.EQUALS, aSearchTerm.getOperator());
		assertEquals("joe", aSearchTerm.getValue());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("foo", aSearchTerm.getField().getTable().getName());
		assertEquals("rank", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.GREATER_THAN, aSearchTerm.getOperator());
		assertEquals("4", aSearchTerm.getValue());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("foo", aSearchTerm.getField().getTable().getName());
		assertEquals("level", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.GREATER_THAN_OR_EQUALS, aSearchTerm.getOperator());
		assertEquals("2", aSearchTerm.getValue());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("foo", aSearchTerm.getField().getTable().getName());
		assertEquals("parentID", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.LESS_THAN, aSearchTerm.getOperator());
		assertEquals("0", aSearchTerm.getValue());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("rootID", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.LESS_THAN_OR_EQUALS, aSearchTerm.getOperator());
		assertEquals("100", aSearchTerm.getValue());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("status", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.NOT_EQUALS, aSearchTerm.getOperator());
		assertEquals("inactive", aSearchTerm.getValue());
		assertFalse(aSearchTerm.isPlaceholder());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("otherStatus", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.NOT_EQUALS, aSearchTerm.getOperator());
		assertEquals("active", aSearchTerm.getValue());
		assertFalse(aSearchTerm.isPlaceholder());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("nullColumn", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.EQUALS, aSearchTerm.getOperator());
		assertEquals(null, aSearchTerm.getValue());
		assertFalse(aSearchTerm.isPlaceholder());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("firstName", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.BEGINS_WITH, aSearchTerm.getOperator());
		assertEquals("s", aSearchTerm.getValue());
		assertFalse(aSearchTerm.isPlaceholder());


		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("firstName", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.ENDS_WITH, aSearchTerm.getOperator());
		assertEquals("m", aSearchTerm.getValue());
		assertFalse(aSearchTerm.isPlaceholder());


		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("firstName", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.CONTAINS, aSearchTerm.getOperator());
		assertEquals("a", aSearchTerm.getValue());
		assertFalse(aSearchTerm.isPlaceholder());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("preparedField", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.EQUALS, aSearchTerm.getOperator());
		assertTrue(aSearchTerm.isPlaceholder());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("rangeSearch", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.GREATER_THAN, aSearchTerm.getOperator());
		//assertEquals("2...9", aSearchTerm.getValue());
		assertFalse(aSearchTerm.isPlaceholder());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("rangeSearch", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.LESS_THAN, aSearchTerm.getOperator());
		assertFalse(aSearchTerm.isPlaceholder());
		//  FIX! be a masochist and parse the query, converting multiple ranges to ... syntax, and adding or subtracting 1 from numerical values if GT, LT instead of GTE, LTE -ssb

		assertFalse(iterator.hasNext());
	}

	public void testSqlCommand() throws Exception, SqlParseException {
		assertNotNull(sqlCommand.getSearchTerms());
	}

	public void testGetOperation() throws Exception {
		assertTrue(sqlCommand.getOperation() == SqlCommand.SELECT);
	}

	public void testGetSql() throws Exception {
		assertEquals(query, sqlCommand.getSql());
	}

	public void testGetLogicalOperator() throws Exception {
		assertEquals(SqlCommand.AND, sqlCommand.getLogicalOperator());
		SqlCommand orCmd = new SqlCommand("SELECT * FROM FOO WHERE name='this' OR name='that' OR name='the_other'");
		assertEquals(SqlCommand.OR, orCmd.getLogicalOperator());
		//
		try {
			SqlCommand broken = new SqlCommand("SELECT * FROM FOO WHERE (name='this' AND (foo=1 OR bar=2))");
			fail("SHouldn't be able to mix AND and OR logical operators");
		} catch (SqlParseException e) {
			// ok
		}
	}

	/** Test the specification of a database in a table name. */
	public void testDatabaseSpecification() throws SqlParseException {
		SqlCommand command = new SqlCommand("SELECT t0.firstName f0, t0.lastName f1 FROM staff.person t0");
		FmField firstField = command.getFields().get(0);
		assertEquals("staff", firstField.getTable().getDatabaseName());
		assertEquals("person", firstField.getTable().getName());
		assertEquals("t0.firstName", firstField.getColumnName());
		assertEquals(command.getTable(), firstField.getTable());

		command = new SqlCommand("SELECT t0.firstName f0, t0.lastName f1 FROM person t0");
		firstField = command.getFields().get(0);
		assertEquals(null, firstField.getTable().getDatabaseName());

		command = new SqlCommand("SELECT t0.city,t0.emailAddress, t0.firstName, t0.ID, t0.lastName FROM Contacts t0 where t0.city=? AND city=?");
		firstField = command.getFields().get(0);
		assertEquals("Contacts", command.getTable().getName());
		assertEquals("city", firstField.getColumnName());
		SearchTerm searchTerm = (SearchTerm) command.getSearchTerms().get(0);
		assertEquals("Contacts", searchTerm.getField().getTable().getName());
		assertEquals("city", searchTerm.getField().getColumnName());
		//
		searchTerm = (SearchTerm) command.getSearchTerms().get(1);
		assertEquals("Contacts", searchTerm.getField().getTable().getName());
		assertEquals("city", searchTerm.getField().getColumnName());
	}

	/** Test the specification of a database in a table name. This differs from {@link #testDatabaseSpecification()} in that
	 * it uses a non-standard separator character; it uses a "|" instead, because this can be set as a connection property. */
	public void testCustomDatabaseSpecification() throws SqlParseException {
		SqlCommand command = new SqlCommand("SELECT t0.firstName f0, t0.lastName f1 FROM staff|person t0", "|");
		FmField firstField = command.getFields().get(0);
		assertEquals("staff", firstField.getTable().getDatabaseName());
		assertEquals(command.getTable(), firstField.getTable());

		command = new SqlCommand("SELECT t0.firstName f0, t0.lastName f1 FROM person t0");
		firstField = command.getFields().get(0);
		assertEquals(null, firstField.getTable().getDatabaseName());
	}

	public void testRepeatingFields() throws Exception {
		SqlCommand cmd = new SqlCommand("SELECT nonRepeating, f2[1], f2[2], f2[3] FROM foo WHERE f2[1]='x'");
		assertEquals(4, cmd.getFields().size());
		Iterator<FmField> iterator = cmd.getFields().iterator();
		FmField eachField = iterator.next();
		assertEquals("nonRepeating", eachField.getColumnName());
		//assertEquals(1, eachField.getRepetition());
		//
		eachField = (FmField) iterator.next();
		assertEquals("f2[1]", eachField.getColumnName());
		//assertEquals(1, eachField.getRepetition());
		//
		eachField = (FmField) iterator.next();
		assertEquals("f2[2]", eachField.getColumnName());
		//assertEquals(2, eachField.getRepetition());
		//
		eachField = (FmField) iterator.next();
		assertEquals("f2[3]", eachField.getColumnName());
		//assertEquals(3, eachField.getRepetition());
	}

	public void testReservedWords() throws Exception {
		SqlCommand cmd = new SqlCommand("SELECT \"fred\", \"from\", \"joe\" FROM people");
		assertEquals(3, cmd.getFields().size());
		assertEquals("people", cmd.getTable().getName());
	}
}