package com.prosc.fmpjdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;
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
 * Date: Apr 18, 2005
 * Time: 9:43:44 AM
 */
public class FileMakerException extends SQLException {
	private final static Logger log = Logger.getLogger( FileMakerException.class.getName() );
	private static final Properties errorMessages = new Properties();
	//private StatementProcessor statementProcessor;
	private String jdbcUrl;
	private String sql;
	private Object params;

	static {
		InputStream stream = FileMakerException.class.getResourceAsStream("ErrorCodes.txt");
		if( stream == null ) log.warning( "Couldn't locate ErrorCodes.txt file; no human-readable error messages will be generated.");
		else try {
			errorMessages.load(stream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				stream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private FileMakerException(Integer errorCode, String errorMessage) {
		super( errorMessage, null, errorCode.intValue() );
	}

	public static FileMakerException exceptionForErrorCode(Integer errorCode) {
		return new FileMakerException(errorCode, getErrorMessage(errorCode) );
	}

	public String getMessage() {
		return super.getMessage() + " (JDBC URL: " + getJdbcUrl() + " / SQL statement: " + sql + " / params: " + params + " )";
	}

	protected static String getErrorMessage(Integer errorCode) {
		String message = errorMessages.getProperty( String.valueOf(errorCode) );
		if( message == null ) message = "Unknown error";
		return errorCode + ": " + message;
	}

	/*public StatementProcessor getStatemenProcessor() {
		return statementProcessor;
	}*/

	public void setStatementProcessor( StatementProcessor statementProcessor ) {
		//this.statementProcessor = statemenProcessor;
		jdbcUrl = ((FmConnection)statementProcessor.getStatement().getConnection()).getUrl();
		sql = statementProcessor.getSQL();
		params = statementProcessor.getParams();
	}

	public String getJdbcUrl() {
		return jdbcUrl;
		/*if( statementProcessor == null ) return null;
		try {
			return ((FmConnection)statementProcessor.getStatement().getConnection7()).getUrl();
		} catch( SQLException e ) {
			return null;
		}*/
	}

	/*private String getSQL() {
		if( statementProcessor == null ) return null;
		return statementProcessor.getSQL();
	}*/
}