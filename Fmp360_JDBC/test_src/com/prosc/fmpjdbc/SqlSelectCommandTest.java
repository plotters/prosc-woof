package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * @author sbarnum
 */
public class SqlSelectCommandTest extends TestCase {
	SqlCommand command;
	private String query = "SELECT firstname, lastname f2, t0.email AS f3, t0.username f4, t0.password f5 FROM person t0 " +
	        "WHERE firstname='sam' AND t0.f2='barnum' AND username=? LIMIT 10, 20";

	protected void setUp() throws Exception {
		command = new SqlCommand(query);
	}

	public void testGetFields() {
		Iterator iterator = command.getFields().iterator();
		FmField field = (FmField) iterator.next();
		assertEquals("firstname", field.getColumnName());
		assertEquals("person", field.getTable().getName());
		assertEquals("firstname", field.getAlias());

		field = (FmField) iterator.next();
		assertEquals("lastname", field.getColumnName());
		assertEquals("person", field.getTable().getName());
		assertEquals("f2", field.getAlias());

		field = (FmField) iterator.next();
		assertEquals("email", field.getColumnName());
		assertEquals("person", field.getTable().getName());
		assertEquals("f3", field.getAlias());

		field = (FmField) iterator.next();
		assertEquals("username", field.getColumnName());
		assertEquals("person", field.getTable().getName());
		assertEquals("f4", field.getAlias());

		field = (FmField) iterator.next();
		assertEquals("password", field.getColumnName());
		assertEquals("person", field.getTable().getName());
		assertEquals("f5", field.getAlias());

		assertFalse(iterator.hasNext());

	}

	public void testGetTable() {
		assertEquals("person", command.getTable().getName());
	}

	public void testOperation() {
		assertEquals(SqlCommand.SELECT, command.getOperation());
	}

	public void testGetSearchTerms() {
		Iterator iterator = command.getSearchTerms().iterator();
		SearchTerm search = (SearchTerm) iterator.next();
		assertEquals("firstname", search.getField().getColumnName());
		assertEquals(SearchTerm.EQUALS, search.getOperator());
		assertEquals("sam", search.getValue());
		assertFalse(search.isPlaceholder());

		search = (SearchTerm) iterator.next();
		assertEquals("lastname", search.getField().getColumnName());
		assertEquals(SearchTerm.EQUALS, search.getOperator());
		assertEquals("barnum", search.getValue());
		assertFalse(search.isPlaceholder());

		search = (SearchTerm) iterator.next();
		assertEquals("username", search.getField().getColumnName());
		assertEquals(SearchTerm.EQUALS, search.getOperator());
		assertEquals(null, search.getValue());
		assertTrue(search.isPlaceholder());

		assertFalse(iterator.hasNext());
	}

	public void testWildcard() throws SqlParseException {
		SqlCommand command = new SqlCommand("SELECT * FROM myTable");
		FmField first = (FmField) command.getFields().get(0);
		assertEquals("*", first.getColumnName());
	}

	public void testGetMaxRows() throws SqlParseException {
		assertEquals(10, command.getMaxRows().intValue());
		SqlCommand command = new SqlCommand("SELECT * FROM myTable LIMIT 10");
		assertEquals(10, command.getMaxRows().intValue());
		assertNull(command.getSkipRows());

		command = new SqlCommand("SELECT * FROM myTable LIMIT 10, 0");
		assertEquals(10, command.getMaxRows().intValue());
		assertEquals(0, command.getSkipRows().intValue());
	}

	public void testWilcardSelect() throws SqlParseException {
		SqlCommand command = new SqlCommand("SELECT * FROM myTable");
		FmField fmField = command.getFields().get(0);
		assertEquals("*", fmField.getColumnName());
		assertEquals(1, command.getFields().size());
	}

	public void testGetSkipRows() throws SqlParseException {
		assertEquals(20, command.getSkipRows().intValue());
		SqlCommand command = new SqlCommand("SELECT * FROM myTable WHERE a=b LIMIT 10,100");
		assertEquals(10, command.getMaxRows().intValue());
		assertEquals(100, command.getSkipRows().intValue());
	}
}
