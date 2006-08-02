package com.prosc.fmpjdbc;

/**
 * @author sbarnum
 */

import junit.framework.*;
import com.prosc.fmpjdbc.FmBlob;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Blob;
import java.sql.SQLException;

public class FmBlobTest extends TestCase {
	FmBlob fmBlob;
	private FmBlob blob;

	public void setUp() throws Exception {
		Statement statement = new JDBCTestUtils().getConnection().createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT portrait FROM Portrait WHERE contactID=1");
		assertTrue( resultSet.next() ); //FIX!! Something somewhere is deleting our test record, so this fails
		blob = (FmBlob) resultSet.getBlob(1);
	}

	/** @TestFails */
	public void testGetMimeType() throws Exception {
		assertEquals("image/jpeg", blob.getMimeType());
	}
}