package com.prosc.woof7;

import com.prosc.fmpjdbc.JDBCTestUtils;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.jdbcadaptor.JDBCContext;
import junit.framework.TestCase;

import java.util.Enumeration;
import java.util.TimeZone;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Apr 21, 2005 Time: 8:01:04 AM
 */
public class EOFTests extends TestCase {
	private static EOAdaptorChannel channel;
	private static EOModel model;
	private static NSArray tableNames;
	private static EOEntity contactEntity;
	private static EOEntity portraitEntity;

	protected void setUp() throws Exception {
		Class.forName( "com.prosc.fmpjdbc.Driver" );
	}

	public void testChannelSetup() {
		//EOModel amsLogic = new EOModel("/Users/jesse/VersionControl/AMSLogic/trunk/Resources/AMSLogic.eomodeld");
		EOAdaptor adaptor = EOAdaptor.adaptorWithName( "JDBC" );
		NSMutableDictionary connectionDict = new NSMutableDictionary();
		String jdbcUrl = JDBCTestUtils.getJdbcUrl( JDBCTestUtils.dbName );
		connectionDict.setObjectForKey( jdbcUrl, "URL" );
		connectionDict.setObjectForKey( JDBCTestUtils.driverClassName, "driver");
		//The ddtek driveer seems to need these, even though they are already embedded in the URL
		connectionDict.setObjectForKey( JDBCTestUtils.dbUsername, "username" );
		connectionDict.setObjectForKey( JDBCTestUtils.dbPassword, "password" );
		adaptor.setConnectionDictionary( connectionDict );
		JDBCContext context = new JDBCContext( adaptor );
		channel = context.createAdaptorChannel();
		channel.openChannel();
	}

	/*public void testDuplicateLayoutNames() {
		//channelSetup
		EOAdaptor adaptor = EOAdaptor.adaptorWithName( "JDBC" );
		NSMutableDictionary connectionDict = new NSMutableDictionary();
		String jdbcUrl = JDBCTestUtils.getJdbcUrl( JDBCTestUtils.dbName );
		connectionDict.setObjectForKey( "jdbc:fmp360://orion.360works.com/?catalogseparator=|", "URL" );
		connectionDict.setObjectForKey( JDBCTestUtils.driverClassName, "driver");
		//The ddtek driveer seems to need these, even though they are already embedded in the URL
		connectionDict.setObjectForKey( "wo", "username" );
		connectionDict.setObjectForKey( "wo", "password" );
		adaptor.setConnectionDictionary( connectionDict );
		JDBCContext context = new JDBCContext( adaptor );
		channel = context.createAdaptorChannel();
		channel.openChannel();

		//readTableNames()
		tableNames = channel.describeTableNames();
		System.out.println( tableNames );
		String expectedTableName = "Contacts";
		if( JDBCTestUtils.use360driver ) expectedTableName = "Contacts" + JDBCTestUtils.catalogSeparator + expectedTableName;
		assertTrue( tableNames.containsObject(expectedTableName) );
		assertTrue( "There are at least 3 tables in the test database.", tableNames.count() >= 3 );

		//modelCreation
		model = channel.describeModelWithTableNames( tableNames );
		model.setName( "ContactsTest" );

		//Trying to set our JDBC plugin, but it's not working --jsb
		NSMutableDictionary params = model.connectionDictionary().mutableClone();
		params.setObjectForKey("Fmp360PlugIn", "plugin");
		model.setConnectionDictionary(params);

		model.writeToFile("/tmp/testModel.eomodeld");
		EOModelGroup.defaultGroup().addModel( model );
		System.out.println( model.entities() );
		contactEntity = model.entityNamed("CONTACTS"); //FIX!! Why is this coming in with uppercase?
		if( contactEntity == null ) contactEntity = model.entityNamed("CONTACTS_CONTACTS"); //It comes in this way if you use a catalog separator
		portraitEntity = contactEntity.model().entityNamed( "PORTRAIT" );
		if( portraitEntity == null ) portraitEntity = contactEntity.model().entityNamed("PORTRAIT_PORTRAIT");
		EOAttribute pk = portraitEntity.attributeNamed( "ID" );
		portraitEntity.setPrimaryKeyAttributes( new NSArray(pk) );
		assertNotNull( contactEntity );
		EOAttribute mimeType = portraitEntity.attributeNamed("MIMETYPE");
		assertEquals("S", mimeType.valueType() ); //Make sure that String attributes have an 'S' value type (String), not 'C' (CharacterStream)
	}*/

	public void testReadTableNames() {
		tableNames = channel.describeTableNames();
		System.out.println( tableNames );
		String expectedTableName = "Contacts";
		if( JDBCTestUtils.use360driver ) expectedTableName = "Contacts" + "|" + expectedTableName;
		assertTrue( tableNames.containsObject(expectedTableName) );
		assertTrue( "There are at least 3 tables in the test database.", tableNames.count() >= 3 );
	}

	public void testModelCreation() {
		model = channel.describeModelWithTableNames( tableNames );
		model.setName( "ContactsTest" );

		//Trying to set our JDBC plugin, but it's not working --jsb
		NSMutableDictionary params = model.connectionDictionary().mutableClone();
		params.setObjectForKey("Fmp360PlugIn", "plugin");
		model.setConnectionDictionary(params);

		model.writeToFile("/tmp/testModel.eomodeld");
		EOModelGroup.defaultGroup().addModel( model );
		System.out.println( model.entities() );
		contactEntity = model.entityNamed("CONTACTS"); //FIX!! Why is this coming in with uppercase?
		if( contactEntity == null ) contactEntity = model.entityNamed("CONTACTS_CONTACTS"); //It comes in this way if you use a catalog separator
		portraitEntity = contactEntity.model().entityNamed( "PORTRAIT" );
		if( portraitEntity == null ) portraitEntity = contactEntity.model().entityNamed("CONTACTS_PORTRAIT");
		EOAttribute pk = portraitEntity.attributeNamed( "ID" );
		portraitEntity.setPrimaryKeyAttributes( new NSArray(pk) );
		assertNotNull( contactEntity );
		EOAttribute mimeType = portraitEntity.attributeNamed("MIMETYPE");
		assertEquals("S", mimeType.valueType() ); //Make sure that String attributes have an 'S' value type (String), not 'C' (CharacterStream)
	}

	public void testReadStoredProcedures() {
		//if using FM 6 we need to change the database in the connection URL
		//this means we have to close the channel and reopen it with a new connection dictionary
		//then when we are done with this test put it back to the way it was
		NSMutableDictionary Saveddict = new NSMutableDictionary();
		if( JDBCTestUtils.fmVersion < 7 ){
		Saveddict = channel.adaptorContext().adaptor().connectionDictionary().mutableClone();
		NSMutableDictionary dict = channel.adaptorContext().adaptor().connectionDictionary().mutableClone();
		dict.setObjectForKey( JDBCTestUtils.getJdbcUrl( "Contacts" ), "URL");
		channel.closeChannel();
		channel.adaptorContext().adaptor().setConnectionDictionary(dict);
		channel.openChannel();
		}

		NSArray scripts = channel.describeStoredProcedureNames();
		Enumeration en = scripts.objectEnumerator();
		while( en.hasMoreElements() ) {
			String procedureName = (String)en.nextElement();
			EOStoredProcedure storedProcedure = new EOStoredProcedure( procedureName );
			storedProcedure.setExternalName( procedureName );
			model.addStoredProcedure( storedProcedure );
		}
		System.out.println( scripts );

		if( JDBCTestUtils.fmVersion < 7 ){
			//restore channel to it's original state - only do for FM 6
			channel.closeChannel();
			channel.adaptorContext().adaptor().setConnectionDictionary(Saveddict);
			channel.openChannel();
		}

		assertTrue( "Couldn't find capitalizeLastNames script", scripts.containsObject("capitalizeLastNames") );
		assertTrue( "There are at least 3 scripts in the test database.", scripts.count() >= 3 );
	}

	public void testReadAllContactRawRows() {
		EOEditingContext ec = new EOEditingContext();
		EOFetchSpecification fetchSpec = new EOFetchSpecification( contactEntity.name(), null, null );
		fetchSpec.setRawRowKeyPaths( NSArray.EmptyArray );
		NSArray results = ec.objectsWithFetchSpecification( fetchSpec );
		assertTrue( "Should get at least one row", results.count() > 0 );
		System.out.println( "First row: " + results.objectAtIndex(0) );
		ec.dispose();
	}

	public void testReadPortraitEOs() {
		EOEditingContext ec = new EOEditingContext();
		NSMutableArray args = new NSMutableArray();
		NSArray sortOrder = new NSArray( new EOSortOrdering("DATE_CREATED", EOSortOrdering.CompareAscending ) );
		args.addObject( new Integer(4) );
		args.addObject( new NSTimestamp(2005, 3, 1, 4, 0, 0, TimeZone.getTimeZone("EST") ) );
		EOQualifier qualifier = EOQualifier.qualifierWithQualifierFormat( "CONTACTID=%@ or DATE_CREATED >%@", args );
		EOFetchSpecification fetchSpec = new EOFetchSpecification( portraitEntity.name(), qualifier, sortOrder );
		NSArray results = ec.objectsWithFetchSpecification( fetchSpec );
		assertTrue( "Should get at least one row", results.count() > 0 );
		for( Enumeration en = results.objectEnumerator(); en.hasMoreElements(); ) {
			System.out.println( en.nextElement() );
		}
	}

	public void testChannelTeardown() {
		channel.closeChannel();
	}

}
