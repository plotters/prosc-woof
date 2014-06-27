package com.prosc.fmpjdbc;

import com.prosc.database.JDBCHelper;
import com.prosc.database.JDBCUtils;
import junit.framework.TestCase;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;

/**
 * @author sbarnum
 */
public class PortalsTest extends TestCase {
	private static final String EMAIL = "ssb_testInsertIntoPortal@360works.com";
	private Connection connection7;
	private JDBCHelper jdbcHelper;

	@Override
	protected void setUp() throws Exception {
		connection7 = DriverManager.getConnection("jdbc:fmp360://localhost/Contact Management", "Admin", "");
		jdbcHelper = new JDBCHelper(connection7);
	}

	@Override
	protected void tearDown() throws Exception {
		connection7.close();
	}

	public void testInsertIntoPortal() throws Exception {
		final List<Record> notes = new ArrayList<Record>();
		notes.add(createNote("Hi"));
		notes.add(createNote("Okay"));
		notes.add(createNote("Bye"));
		PreparedStatement ps = connection7.prepareStatement("insert into jdbc_portal_test (Email, Notes) values (?, ?)");
		JDBCUtils.populateArgs(ps, new Object[] {EMAIL, notes});
		assertEquals(1, ps.executeUpdate());
	}

	public void testDeleteNote() throws Exception {
		String noteRecordIdToDelete = jdbcHelper.executeQuerySingleRow(new JDBCHelper.RowHandler<String>() {
			@Nullable
			public String handle(final ResultSet currentRow) throws SQLException {
				Array array = currentRow.getArray(1);
				ResultSet rs = array.getResultSet(2L, 1);
				return rs.getString("recid");
			}
		}, "select Notes from jdbc_portal_test where Email=?", EMAIL);
		assertNotNull(noteRecordIdToDelete);
		//
		final List<Record> notes = Collections.singletonList(new Record(noteRecordIdToDelete, null, true, new HashMap<String, Object>()));
		jdbcHelper.executeUpdate("update jdbc_portal_test set Notes=? where Email=?", notes, EMAIL);
	}

	public void testFetchPortal() throws Exception {
		PreparedStatement ps = connection7.prepareStatement("select Email, Notes from jdbc_portal_test where Email=?");
		JDBCUtils.populateArgs(ps, new Object[] {EMAIL});
		//Map<String, Object> sam = new JDBCHelper(connection7).executeQueryRows("select * from jdbc_test where email=?", "sam@360works.com").toMaps().get(0);
		ResultSet personResultSet = ps.executeQuery();
		assertTrue(personResultSet.next());
		assertEquals("sam@360works.com", personResultSet.getString("Email"));
		Array noteArray = personResultSet.getArray("Notes");
		assertEquals("JAVA_OBJECT", noteArray.getBaseTypeName());
		assertEquals(Types.JAVA_OBJECT, noteArray.getBaseType());

		ResultSet noteResultSet = noteArray.getResultSet();
		assertTrue(noteResultSet.next());
		assertEquals("Hi", noteResultSet.getString("Text"));

		Object nativeArray = noteArray.getArray();
		assertTrue(noteArray instanceof Array);

		noteArray.free();
		try {
			noteArray.getArray();
			fail("No exception thrown after freeing the array result set");
		} catch (Exception e) {
			// success
		}
	}

	public void testDeletePortalRow() throws Exception {

	}

	private Record createNote(final String text) {
		HashMap<String, Object> values = new HashMap<String, Object>();
		values.put("Text", text);
		return new Record(null, null, false, values);
	}

	private class Record {
		private String recordId;
		private Long modCount;
		private boolean deletedFlag;
		private Map<String,Object> values;

		private Record(final String recordId, final Long modCount, final boolean deletedFlag, final Map<String, Object> values) {
			this.recordId = recordId;
			this.modCount = modCount;
			this.deletedFlag = deletedFlag;
			this.values = values;
		}
	}
}
