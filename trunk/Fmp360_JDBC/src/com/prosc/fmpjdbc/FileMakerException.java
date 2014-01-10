package com.prosc.fmpjdbc;

import com.prosc.sql.ErrorCodes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
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
 * Date: Apr 18, 2005
 * Time: 9:43:44 AM
 */
public class FileMakerException extends SQLException {
	private final static Logger log = Logger.getLogger( FileMakerException.class.getName() );
	
	//private StatementProcessor statementProcessor;
	private String jdbcUrl;
	private String sql;
	private Object params;
	private boolean ssl;
	private String requestUrl;
	private String username;


	protected FileMakerException(Integer errorCode, String errorMessage, String requestUrl, String username ) {
		this( errorCode, errorMessage, requestUrl, username, null );
	}
	
	protected FileMakerException(Integer errorCode, String errorMessage, String requestUrl, String username, @Nullable String sqlState ) {
		super( errorMessage, sqlState, errorCode );
		this.requestUrl = requestUrl;
		this.username = username;
		if( errorCode == 105 && requestUrl.contains( "ProscNoSuchTable" ) ) {
			//Special case, this error is thrown when establishing a connection. Don't log anything.
		} else {
			log.log( Level.INFO, toString() + " / requestUrl: " + requestUrl );
		}
	}

	public static FileMakerException exceptionForErrorCode( Integer errorCode, String requestUrl, String username ) {
		return exceptionForErrorCode(errorCode, requestUrl, null, username );
	}

	public static FileMakerException exceptionForErrorCode( Integer errorCode, @NotNull String requestUrl, @Nullable String whichLayout, @Nullable String username ) {
		if( errorCode == 102 ) {
			return new MissingFieldException( "Error " + errorCode + ": " + ErrorCodes.getMessage(errorCode), 102, requestUrl, whichLayout, null, username );
		} else {
			return new FileMakerException(errorCode, "Error " + errorCode + ": " + ErrorCodes.getMessage(errorCode), requestUrl, username );
		}
	}

	public String getMessage() {
		StringBuilder msg = new StringBuilder( super.getMessage().length() + 512 );
		msg.append( super.getMessage() );
		boolean extraParams = false;
		if( jdbcUrl != null || sql != null || params != null || requestUrl != null ) {
			extraParams = true;
			msg.append( " (" );
		}
		if( jdbcUrl != null ) {
			msg.append( "JDBC URL: " + jdbcUrl + ", SSL: " + ssl );
		}
		if( sql != null ) {
			msg.append( " / SQL statement: " + sql );
		}
		if( params != null ) {
			msg.append( " / SQL params: " + params );
		}
		/*if( requestUrl != null ) {
			msg.append( " / request URL: " + requestUrl );
		}*/
		if( extraParams ) {
			msg.append( ")" );
		}
		if( username != null ) {
			msg.append( " username: " + username );	
		}
		return msg.toString();
	}

	public String getRequestUrl() {
		return requestUrl;
	}

	/*public StatementProcessor getStatemenProcessor() {
		return statementProcessor;
	}*/

	public void setStatementProcessor( StatementProcessor statementProcessor ) {
		//this.statementProcessor = statemenProcessor;
		//jdbcUrl = ((FmConnection)statementProcessor.getStatement().getConnection()).getUrl();
		sql = statementProcessor.getSQL();
		params = statementProcessor.getParams();
	}

	public void setConnection( FmConnection connection ) {
		this.jdbcUrl = connection.getUrl();
		ssl = Boolean.valueOf( connection.getProperties().getProperty( "ssl", "false" ) );
	}
	
	/*public void setDatabase( String dbName ) {
		this.dbName = dbName;
	}*/

	/*public String getJdbcUrl() {
		return jdbcUrl;
	}*/

	/*private String getSQL() {
		if( statementProcessor == null ) return null;
		return statementProcessor.getSQL();
	}*/
}
