package com.prosc.woof7;

//
// FMPExpression.java
// Project FMPPlugIn
//
// Created by jesse on Mon Sep 17 2001
//

import com.webobjects.eoaccess.*;
import com.webobjects.foundation.*;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.jdbcadaptor.Fmp360PlugIn;

import java.util.logging.Logger;
import java.util.logging.Level;

public class FMPExpression extends com.webobjects.jdbcadaptor.JDBCExpression {
	private static final Logger log = Logger.getLogger( FMPExpression.class.getName() );
	static final String TIME_TYPE="TIME", DATE_TYPE="DATE", REPEATING_FIELD="CLOB";
	static final String placeholder="?";
	static final NSTimestampFormatter timeFormat=new NSTimestampFormatter("%H:%M:%S");
	static final NSTimestampFormatter dateFormat=new NSTimestampFormatter("%m/%d/%Y");

	public FMPExpression(EOEntity theEntity) {
		super(theEntity);
	}

	/** FileMaker does not support table aliases **/
	public boolean useAliases() {return false;}

	/** Add quote characters around all attributes **/
	public String sqlStringForAttribute(EOAttribute anAttribute) {
		return ('\"' + anAttribute.columnName() + '\"');
	}

	/** This overrides the default implementation to surround table names with quote characters. It also looks for an entry in the Entity's UserInfo dictionary named LAYOUT. If it finds this, it will add the named layout to the table name.
	 **/
	public String tableListWithRootEntity(EOEntity entity) {
		return ("\"" + entity.externalName() + "\"");
	}

	/** Override the default implementation to transform this into an update, because the FmproPlugin has already created the row for us.
	 **/
	public void prepareInsertExpressionWithRow( NSDictionary row ) {
		//Get the primary key name and value from the row dictionary to qualify the UPDATE statement.
		NSArray pkNames = entity().primaryKeyAttributeNames();
		if( pkNames.count() != 1 ) {
			super.prepareInsertExpressionWithRow( row );
		}
		else {
			String pkName = (String)pkNames.objectAtIndex(0);
			Object pkValue = row.objectForKey(pkName);
			EOQualifier pkQualifier = EOQualifier.qualifierWithQualifierFormat(pkName + " = %@", new NSArray(pkValue)); //FIX! This won't work for anything but integer keys. Is this a problem?
			//Remove primary key from dictionary - we don't want to set it; it's just for the WHERE clause of the update
			NSMutableDictionary processedRow = new NSMutableDictionary(row);
			processedRow.removeObjectForKey(pkName);
			if (processedRow.count() > 0) {
				try {
					prepareUpdateExpressionWithRow(processedRow,  pkQualifier);
				} catch (RuntimeException e) {
					log.log( Level.SEVERE, e.toString(), e );
					throw e;
				}
			}
			//OPTIMIZE: Can we do nothing if the row only contains a primary key?
			log.fine("INSERT STATEMENT: " + this);
		}
	}

	public String assembleInsertStatementWithRow(NSDictionary nsDictionary, String s, String s1, String s2) {
		String result = super.assembleInsertStatementWithRow(nsDictionary, s, s1, s2);
		log.fine(result);
		return result;
	}

	public int jdbcTypeForUnknownExternalType(String externalType, int precision, int scale) {
		log.fine("Getting type for " + externalType);
		if( "TEXT".equals(externalType) ) return java.sql.Types.VARCHAR;
		if( "NUMBER".equals(externalType) ) return java.sql.Types.DOUBLE;
		if( "DATE".equals(externalType) ) return java.sql.Types.DATE;
		if( "TIME".equals(externalType) ) return java.sql.Types.TIME;
		if( "TIMESTAMP".equals(externalType) ) return java.sql.Types.TIMESTAMP;
		//FIX!! What about containers? --jsb
		return java.sql.Types.OTHER;
	}

	//The next three methods are to fix FileMaker not writing date & time values correctly.
	public boolean shouldUseBindVariableForAttribute(EOAttribute attribute) {
		return true;
	}

	public boolean mustUseBindVariableForAttribute(EOAttribute attribute) {
		return true;
	}

	public String sqlStringForCaseInsensitiveLike( String valueString, String keyString) {
		return keyString + " LIKE " + valueString; //FIX: Do we need to worry about FileMaker LIKE weirdness?
	}

	// sbarnum 2002-03-20:  added following functions to handle LIKE searches //
	// Filemaker maps LIKE searches to *pattern*
	// Percent signs should never be used
	// Begins With searches should use ='pattern*'
	// Ends With searches should use ='*pattern%'
	/** LIKE searches should translate to = with **/


	/** Filemaker does not support ESCAPE syntax **/
	public char sqlEscapeChar() {
		return (char)0;
	}

	/** This method is passed a pattern with asterixes as wildcards.
	 Noraml SQL would convert these to percent signs.
	 For our purposes, we leave them as asterixes,
	 since Filemaker is anything but normal **/
	public String sqlPatternFromShellPattern(String pattern) {
		return pattern; // no change! //
	}

	/** Skip any SQL statements that are searching for some value equal to "*". For some reason, this can slow down FileMaker alot, especially when searching across related fields. */
	public String sqlStringForKeyValueQualifier(EOKeyValueQualifier qualifier) {
		if( "*".equals( qualifier.value() ) ) {
			return "";
		}
		else return super.sqlStringForKeyValueQualifier(qualifier);
	}

}
