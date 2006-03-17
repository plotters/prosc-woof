package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * @author sbarnum
 */
public class SqlUpdateCommandTest extends TestCase {
	SqlCommand sqlCommand;

	protected void setUp() throws Exception {
		String query = "UPDATE SampleTable SET a='1 spelled \\'backwards\\' is \\\"1\\\"',b=?, c=NULL, \"d field\"=?  WHERE id<10 AND a != b";
		sqlCommand = new SqlCommand(query);
	}

	public void testFoo() throws SqlParseException {
		assertEquals(SqlCommand.UPDATE, sqlCommand.getOperation());
	}

	public void testGetTable() {
		assertEquals("SampleTable", sqlCommand.getTable().getName());
	}


	public void testGetSearchTerms() throws Exception {
		Iterator iterator = sqlCommand.getSearchTerms().iterator();
		SearchTerm aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("SampleTable", aSearchTerm.getField().getTable().getName());
		assertEquals("id", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.LESS_THAN, aSearchTerm.getOperator());
		assertEquals("10", aSearchTerm.getValue());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("SampleTable", aSearchTerm.getField().getTable().getName());
		assertEquals("a", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.NOT_EQUALS, aSearchTerm.getOperator());
		assertEquals("b", aSearchTerm.getValue());

		assertFalse(iterator.hasNext());
	}

	public void testGetAssignmentTerms() {
		Iterator iterator = sqlCommand.getAssignmentTerms().iterator();
		AssignmentTerm term = (AssignmentTerm) iterator.next();
		assertEquals("a", term.getField().getColumnName());
		assertEquals("1 spelled 'backwards' is \"1\"", term.getValue());
		assertFalse(term.isPlaceholder());

		term = (AssignmentTerm) iterator.next();
		assertEquals("b", term.getField().getColumnName());
		assertEquals(null, term.getValue());
		assertTrue(term.isPlaceholder());

		term = (AssignmentTerm) iterator.next();
		assertEquals("c", term.getField().getColumnName());
		assertEquals(null, term.getValue());
		assertFalse(term.isPlaceholder());

		term = (AssignmentTerm) iterator.next();
		assertEquals("d field", term.getField().getColumnName());
		assertEquals(null, term.getValue());
		assertTrue(term.isPlaceholder());
	}

	public void testGetOperation() throws Exception {
		assertTrue(sqlCommand.getOperation() == SqlCommand.UPDATE);
	}

	public void testGetLogicalOperator() throws Exception {
		assertEquals(SqlCommand.AND, sqlCommand.getLogicalOperator());
	}


}
