package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.util.Iterator;

/**
 * @author sbarnum
 */
public class SqlInsertCommandTest extends TestCase {
	SqlCommand command;
	private String query = "INSERT INTO foo (field1, field2 , field3,field4, \"field 5\", FIELD6) VALUES(value1, 'value2', null, ?, ?, '#6')";

	protected void setUp() throws Exception {
		command = new SqlCommand(query);
	}

	public void testOperation() {
		assertEquals(SqlCommand.INSERT, command.getOperation());
	}

	public void testGetTable() {
		assertEquals("foo", command.getTable().getName());
	}


	public void testAssignmentTerms() {
		Iterator iterator = command.getAssignmentTerms().iterator();
		AssignmentTerm assignment = (AssignmentTerm) iterator.next();
		assertEquals("field1", assignment.getField().getColumnName());
		assertEquals("value1", assignment.getValue());
		assertEquals("foo", assignment.getField().getTable().getName());
		assertFalse(assignment.isPlaceholder());

		assignment = (AssignmentTerm) iterator.next();
		assertEquals("field2", assignment.getField().getColumnName());
		assertEquals("value2", assignment.getValue());
		assertFalse(assignment.isPlaceholder());

		assignment = (AssignmentTerm) iterator.next();
		assertEquals("field3", assignment.getField().getColumnName());
		assertEquals(null, assignment.getValue());
		assertFalse(assignment.isPlaceholder());

		assignment = (AssignmentTerm) iterator.next();
		assertEquals("field4", assignment.getField().getColumnName());
		assertEquals(null, assignment.getValue());
		assertTrue(assignment.isPlaceholder());

		assignment = (AssignmentTerm) iterator.next();
		assertEquals("field 5", assignment.getField().getColumnName());
		assertEquals("foo", assignment.getField().getTable().getName());
		assertEquals(null, assignment.getValue());
		assertTrue(assignment.isPlaceholder());

		assignment = (AssignmentTerm) iterator.next();
		assertEquals("FIELD6", assignment.getField().getColumnName());
		assertEquals("#6", assignment.getValue());
		assertFalse(assignment.isPlaceholder());
	}

	public void testSpeediness() {
		new JDBCTestUtils().assertIsSpeedy(10, new Runnable() {
			public void run() {
				try {
					new SqlCommand("INSERT INTO myTableWithTheLongName (123, 234, 345, 456, 567, 678, 789, 890, qwe, wer, ert, rty, tyu, yui, uio, iop, asd, sdf, dfg, fgh, ghj, hjk, jkl, zxc, xcv, cvb, vbn, bnm) VALUes (123, 234, 345, 456, 567, 678, 789, 890, 'qwe', 'wer', 'ert','rty','tyu','yui','uio','iop','asd','sdf','dfg','fgh','ghj','hjk','jkl','zxc','xcv','cvb','vbn','bnm')");
				} catch (SqlParseException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	// FIX! write some other test cases with invalid SQL, check that a reasonable error is generated.  Pretty low priority... -ssb
}
