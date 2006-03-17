package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * @author sbarnum
 */
public class SqlUpdateCommandTest2 extends TestCase {
	SqlCommand sqlCommand;
	private String query ="UPDATE \"Funny Table Name\" SET a = '1 spelled \\'backwards\\' is \\\"1\\\"',  b=    ? , c= NULL, \"d field\"=?  WHERE id <> 10 OR a IS NULL OR z IS  NOT NULL";

	protected void setUp() throws Exception {
		sqlCommand = new SqlCommand(query);
	}


	public void testGetTable() {
		assertEquals("Funny Table Name", sqlCommand.getTable().getName());
	}


	public void testFoo() throws SqlParseException {
		assertEquals(SqlCommand.UPDATE, sqlCommand.getOperation());
	}

	public void testGetSearchTerms() throws Exception {
		// testing WHERE id<>10 OR a IS NULL OR z IS  NOT NULL
		Iterator iterator = sqlCommand.getSearchTerms().iterator();

		SearchTerm aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("Funny Table Name", aSearchTerm.getField().getTable().getName());
		assertEquals("id", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.NOT_EQUALS, aSearchTerm.getOperator());
		assertEquals("10", aSearchTerm.getValue());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("Funny Table Name", aSearchTerm.getField().getTable().getName());
		assertEquals("a", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.EQUALS, aSearchTerm.getOperator());
		assertEquals(null, aSearchTerm.getValue());

		aSearchTerm = (SearchTerm) iterator.next();
		assertEquals("Funny Table Name", aSearchTerm.getField().getTable().getName());
		assertEquals("z", aSearchTerm.getField().getColumnName());
		assertEquals(SearchTerm.NOT_EQUALS, aSearchTerm.getOperator());
		assertEquals(null, aSearchTerm.getValue());

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

	public void testSpeediness() {
		JDBCTestUtils.assertIsSpeedy(10, new Runnable() {

			public void run() {
				try {
					new SqlCommand(new String(query));
				} catch (SqlParseException e) {
					throw new RuntimeException(e);
				}
			}

			public String toString() {
				return "parsing update SqlCommand";
			}
		});
	}

	public void testGetLogicalOperator() throws Exception {
		assertEquals(SqlCommand.OR, sqlCommand.getLogicalOperator()); // uses an "OR" where clause logical operator
	}


}
