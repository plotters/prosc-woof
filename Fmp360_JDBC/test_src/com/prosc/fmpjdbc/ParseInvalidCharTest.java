package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: jesse
 * Date: 1/19/14
 * Time: 10:36 AM
 */
public class ParseInvalidCharTest extends TestCase {
	public void testReadInvalidChars() throws SQLException, IOException {
		FmXmlRequest request = new FmXmlRequest( "http", "localhost", "/doesntmatter", 80, null, null, 12 );
		InputStream stream = getClass().getResourceAsStream( "InvalidChar13.xml" );
		try {
			request.readResult( stream );
			Iterator<FmRecord> recordIterator = request.getRecordIterator();
			while( recordIterator.hasNext() ) {
				FmRecord next = recordIterator.next();
				System.out.println(next);
			}
		} finally {
			stream.close();
		}
	}
}
