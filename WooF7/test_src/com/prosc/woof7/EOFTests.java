package com.prosc.woof7;

import com.prosc.fmpjdbc.JDBCTestUtils;
import com.prosc.fmpjdbc.StatementProcessor;
import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import com.webobjects.jdbcadaptor.JDBCContext;
import junit.framework.TestCase;

import java.util.Enumeration;
import java.util.TimeZone;
import java.io.File;
import java.net.MalformedURLException;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Apr 21, 2005 Time: 8:01:04 AM
 */
public class EOFTests extends TestCase {
	private static EOAdaptorChannel channel;
	private static EOModel model;
	private static NSArray tableNames;
	private static EOEntity contactEntity;
	private static EOEntity portraitEntity;
	private static JDBCTestUtils jdbcTestUtils;

	protected void setUp() throws Exception {
		Class.forName( "com.prosc.fmpjdbc.Driver" );
	}

	public void testChannelSetup() {
		//EOModel amsLogic = new EOModel("/Users/jesse/VersionControl/AMSLogic/trunk/Resources/AMSLogic.eomodeld");
		EOAdaptor adaptor = EOAdaptor.adaptorWithName( "JDBC" );
		NSMutableDictionary connectionDict = new NSMutableDictionary();
		jdbcTestUtils = new JDBCTestUtils();
		String jdbcUrl = jdbcTestUtils.getJdbcUrl( jdbcTestUtils.dbName );
		connectionDict.setObjectForKey( jdbcUrl, "URL" );
		connectionDict.setObjectForKey( jdbcTestUtils.driverClassName, "driver");
		//The ddtek driveer seems to need these, even though they are already embedded in the URL
		connectionDict.setObjectForKey( jdbcTestUtils.dbUsername, "username" );
		connectionDict.setObjectForKey( jdbcTestUtils.dbPassword, "password" );
		adaptor.setConnectionDictionary( connectionDict );
		JDBCContext context = new JDBCContext( adaptor );
		channel = context.createAdaptorChannel();
		channel.openChannel();
	}

	/*public void testForSherry2() throws MalformedURLException {
		EOModel model = new EOModel( new File("/Users/brittany/Desktop/mediaSales.eomodeld").toURL() );
		EOModelGroup.defaultGroup().addModel( model );
		EOEditingContext eoEditingContext = new EOEditingContext();
		EOEnterpriseObject eo =  EOUtilities.createAndInsertInstance(eoEditingContext, "AVOrders");

		eo.takeValueForKey("Web", "contactType");
		eo.takeValueForKey(new NSTimestamp(), "dateCreated");
		eo.takeValueForKey("Yes", "paidWithCreditCard");
		eo.takeValueForKey("No", "taxIndicator");

		eoEditingContext.saveChanges();
		eoEditingContext.invalidateAllObjects();

		NSDictionary pk = EOUtilities.primaryKeyForObject(eoEditingContext, eo);
		EOEditingContext getBackEditingContext = new EOEditingContext();
		EOEnterpriseObject returnObject = EOUtilities.objectWithPrimaryKey(getBackEditingContext, "AVOrders", pk);
		NSTimestamp dateCreated = (NSTimestamp) returnObject.valueForKey("dateCreated");
		System.out.println("Date Created: " + dateCreated);

	}*/


	/*public void testForSherry() throws MalformedURLException {
		EOModel model = new EOModel( new File("/Users/jesse/Desktop/mediaSales.eomodeld").toURL() );
		EOModelGroup.defaultGroup().addModel( model );
		String whichEntity;
//		whichEntity = "AVLItemI";
		whichEntity = "AVOrders";
		EOFetchSpecification fetchSpec = new EOFetchSpecification( whichEntity, null, null );
		fetchSpec.setFetchLimit( 50 );
		fetchSpec.setRawRowKeyPaths( NSArray.EmptyArray );
		NSArray items = new EOEditingContext().objectsWithFetchSpecification( fetchSpec );
	}*/

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
		if( jdbcTestUtils.use360driver ) expectedTableName = "Contacts" + "|" + expectedTableName;
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
		if( jdbcTestUtils.fmVersion < 7 ){
		Saveddict = channel.adaptorContext().adaptor().connectionDictionary().mutableClone();
		NSMutableDictionary dict = channel.adaptorContext().adaptor().connectionDictionary().mutableClone();
		dict.setObjectForKey( jdbcTestUtils.getJdbcUrl( "Contacts" ), "URL");
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

		if( jdbcTestUtils.fmVersion < 7 ){
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
