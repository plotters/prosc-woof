package com.prosc.fmpjdbc;

import java.sql.*;
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
 * Created by IntelliJ IDEA. User: jesse Date: Apr 16, 2005 Time: 6:19:55 PM
 */
public class FmStatement implements Statement {
	private FmConnection connection;
	private StatementProcessor processor;
	private Logger log = Logger.getLogger("com.prosc.fmpjdbc");

	public FmStatement( FmConnection connection ) {
		this.connection = connection;
	}

	protected StatementProcessor processor() { return processor; }

	protected void setProcessor( StatementProcessor processor ) {
		this.processor = processor;
	}

	//---These methods must be implemented---
	public ResultSet executeQuery( String s ) throws SQLException {
		SqlCommand command = new SqlCommand(s);
		processor = new StatementProcessor(this, command);
		processor.execute();
		return processor.getResultSet();
	}

	public int executeUpdate( String s ) throws SQLException {
		SqlCommand command = new SqlCommand(s);
		processor = new StatementProcessor(this, command);
		processor.execute();
		return processor.getUpdateRowCount();
	}

	public boolean execute( String s ) throws SQLException {
		SqlCommand command = new SqlCommand(s);
		processor = new StatementProcessor(this, command);
		processor.execute();
		return processor.hasResultSet();
	}

	public ResultSet getResultSet() throws SQLException {
		if( processor == null || !processor.hasResultSet() ) return null;
		return processor.getResultSet();
	}

	public int getUpdateCount() throws SQLException {
		if( processor == null || !processor.hasResultSet() ) return -1;
		return processor.getUpdateRowCount();
	}

	public void close() throws SQLException {
		processor = null; //Assist garbage collection
	}

	public ResultSet getGeneratedKeys() throws SQLException {
		return processor.getGeneratedKeys(); //FIX!! throws NullPointerException
	}

	public boolean getMoreResults() throws SQLException {
		return false;
	}

	public Connection getConnection() {
		return connection;
	}

	public boolean execute( String s, int i ) throws SQLException {
		return execute(s);
	}

	public int getQueryTimeout() throws SQLException {
		throw new AbstractMethodError( "getQueryTimeout is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setQueryTimeout( int i ) throws SQLException {
		throw new AbstractMethodError( "setQueryTimeout is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public SQLWarning getWarnings() throws SQLException {
		throw new AbstractMethodError( "getWarnings is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void clearWarnings() throws SQLException {
		throw new AbstractMethodError( "clearWarnings is not implemented yet." ); //FIX!!! Broken placeholder
	}


	//---These can be left abstract for now---


	public int executeUpdate( String s, int i ) throws SQLException {
		return executeUpdate(s);
	}

	public int executeUpdate( String s, int[] ints ) throws SQLException {
		throw new AbstractMethodError( "executeUpdate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int executeUpdate( String s, String[] strings ) throws SQLException {
		throw new AbstractMethodError( "executeUpdate is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean execute( String s, int[] ints ) throws SQLException {
		throw new AbstractMethodError( "execute is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean execute( String s, String[] strings ) throws SQLException {
		throw new AbstractMethodError( "execute is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxFieldSize() throws SQLException {
		throw new AbstractMethodError( "getMaxFieldSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setMaxFieldSize( int i ) throws SQLException {
		throw new AbstractMethodError( "setMaxFieldSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getMaxRows() throws SQLException {
		throw new AbstractMethodError( "getMaxRows is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setMaxRows( int i ) throws SQLException {
		throw new AbstractMethodError( "setMaxRows is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setEscapeProcessing( boolean b ) throws SQLException {
		throw new AbstractMethodError( "setEscapeProcessing is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void cancel() throws SQLException {
		throw new AbstractMethodError( "cancel is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setCursorName( String s ) throws SQLException {
		throw new AbstractMethodError( "setCursorName is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setFetchDirection( int i ) throws SQLException {
		throw new AbstractMethodError( "setFetchDirection is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getFetchDirection() throws SQLException {
		throw new AbstractMethodError( "getFetchDirection is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void setFetchSize( int i ) throws SQLException {
		throw new AbstractMethodError( "setFetchSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getFetchSize() throws SQLException {
		throw new AbstractMethodError( "getFetchSize is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getResultSetConcurrency() throws SQLException {
		throw new AbstractMethodError( "getResultSetConcurrency is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getResultSetType() throws SQLException {
		throw new AbstractMethodError( "getResultSetType is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void addBatch( String s ) throws SQLException {
		throw new AbstractMethodError( "addBatch is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public void clearBatch() throws SQLException {
		throw new AbstractMethodError( "clearBatch is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int[] executeBatch() throws SQLException {
		throw new AbstractMethodError( "executeBatch is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public boolean getMoreResults( int i ) throws SQLException {
		throw new AbstractMethodError( "getMoreResults is not implemented yet." ); //FIX!!! Broken placeholder
	}

	public int getResultSetHoldability() throws SQLException {
		throw new AbstractMethodError( "getResultSetHoldability is not implemented yet." ); //FIX!!! Broken placeholder
	}
}
