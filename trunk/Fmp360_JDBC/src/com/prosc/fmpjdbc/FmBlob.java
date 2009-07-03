package com.prosc.fmpjdbc;

import java.sql.Blob;
import java.sql.SQLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.DataInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
    Fmp360_JDBC is a FileMaker JDBC driver that uses the XML publishing features of FileMaker Server Advanced.
    Copyright (C) 2006  Prometheus Systems Consulting, LLC d/b/a 360Works

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

/**
 * Created by IntelliJ IDEA.
 * User: jesse
 * Date: Jun 15, 2005
 * Time: 11:22:03 AM
 */
public class FmBlob implements Blob {
	private URL containerUrl;
	private String username;
	private String password;
	private HttpURLConnection connection;
	//private InputStream stream = null;
	private Logger logger = Logger.getLogger( FmBlob.class.getName() );

	public FmBlob(URL containerUrl, String username, String password) {
		this.containerUrl = containerUrl;
		this.username = username;
		this.password = password;
	}

	public String getMimeType() throws IOException {
		return getConnection().getHeaderField("content-type");
	}

	public byte[] getBytes() {
		try {
			return getBytes( 0, (int)length() );
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public URL getURL() {
		return containerUrl;
		
		/*URL u = containerUrl;
		String authority = "";
		if( username != null || password != null ) {
			authority = (username == null ? "" : username) + ":" +
					(password == null ? "": password) + "@";
		}
		String port = "";
		if( u.getPort() != -1 && u.getPort() != 80 && u.getPort() != 443 ) {
			port = ":" + u.getPort();
		}
		String fragment = "";
		if( u.getRef() != null ) {
			fragment = "#" + u.getRef();
		}
		String spec = u.getProtocol() + "://" + authority + u.getHost() + port + u.getFile() + fragment;
		try {
			return new URL( spec );
		} catch( MalformedURLException e ) {
			throw new RuntimeException(e);
		}*/
	}

	public int hashCode() {
		return containerUrl.hashCode();
	}

	//---Implemention of Blob interface---

	public long length() throws SQLException {
		try {
			return Long.valueOf( getConnection().getHeaderField("content-length") ).longValue();
		} catch (IOException e) {
			throw handleIOException(e);
		}
	}

	public byte[] getBytes(long pos, final int length) throws SQLException {
		if( pos == 0 ) {
			byte[] result = new byte[length];
			try {
				InputStream stream = getStream();
				DataInputStream dis = new DataInputStream(stream);
				try {
					dis.readFully(result);
					//getStream().read( result, 0, length );
					return result;
				} catch (IOException e) {
					throw handleIOException(e);
				} finally {
					dis.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new AbstractMethodError("Currently, can only read from position 0."); //FIX!!! Broken placeholder
		}
	}

	public InputStream getBinaryStream() throws SQLException {
		try {
			return getStream();
		} catch (IOException e) {
			throw handleIOException(e);
		}
	}

	public long position(byte pattern[], long start) throws SQLException {
		throw new AbstractMethodError("This functionality is not implemented yet."); //FIX!!! Broken placeholder
	}

	public long position(Blob pattern, long start) throws SQLException {
		throw new AbstractMethodError("This functionality is not implemented yet."); //FIX!!! Broken placeholder
	}

	public int setBytes(long pos, byte[] bytes) throws SQLException {
		throw new AbstractMethodError("Container fields cannot be written to.");
	}

	public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
		throw new AbstractMethodError("Container fields cannot be written to.");
	}

	public OutputStream setBinaryStream(long pos) throws SQLException {
		throw new AbstractMethodError("Container fields cannot be written to.");
	}

	public void truncate(long len) throws SQLException {
		throw new AbstractMethodError("Container fields cannot be written to.");
	}

	//---Internal methods ---

	private SQLException handleIOException(IOException ioException) {
		SQLException sqlEx = new SQLException( ioException.toString() );
		sqlEx.initCause(ioException);
		logger.log(Level.WARNING, sqlEx.toString());
		return sqlEx;
	}

	private HttpURLConnection getConnection() throws IOException {
		HttpURLConnection connection = (HttpURLConnection)containerUrl.openConnection();
		if( username != null && password != null ) {
			String encodedAuth = new sun.misc.BASE64Encoder().encode( (username + ":" + password).getBytes() );
			connection.addRequestProperty("Authorization", "Basic " + encodedAuth);
		}
		return connection;
	}

	private InputStream getStream() throws IOException {
		return getConnection().getInputStream();
	}
}
