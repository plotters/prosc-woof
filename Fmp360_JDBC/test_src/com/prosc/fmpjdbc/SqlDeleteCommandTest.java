package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * @author sbarnum
 */
public class SqlDeleteCommandTest extends TestCase {
	SqlCommand command;
	String sql = "DELETE FROM foo WHERE id >=? AND recordid=?";

	protected void setUp() throws Exception {
		command = new SqlCommand(sql);
	}

	public void testGetOperation() {
		assertEquals(SqlCommand.DELETE, command.getOperation());
	}

	public void testGetSearchTerms() {
		Iterator iterator = command.getSearchTerms().iterator();
		SearchTerm search = (SearchTerm) iterator.next();
		assertEquals("id", search.getField().getColumnName());
		assertEquals(SearchTerm.GREATER_THAN_OR_EQUALS, search.getOperator());
		assertEquals(null, search.getValue());
		assertTrue(search.isPlaceholder());

		search = (SearchTerm) iterator.next();
		assertEquals("recordid", search.getField().getColumnName());
		assertEquals(SearchTerm.EQUALS, search.getOperator());
		assertEquals(null, search.getValue());
		assertTrue(search.isPlaceholder());
	}

	public void testGetTable() {
		assertEquals( "foo", command.getTable().getName());
	}
}
