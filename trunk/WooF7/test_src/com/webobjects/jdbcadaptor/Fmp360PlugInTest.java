package com.webobjects.jdbcadaptor;

import junit.framework.TestCase;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSLog;

import java.net.URL;
import java.net.MalformedURLException;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: jesse
 * Date: Jun 15, 2005
 * Time: 3:56:02 PM
 */
public class Fmp360PlugInTest extends TestCase {
	private static boolean initialized = false;

	protected void setUp() throws Exception {
		if( ! initialized ) {
			NSLog.allowDebugLoggingForGroups(NSLog.DebugGroupSQLGeneration |
					NSLog.DebugGroupDatabaseAccess |
					NSLog.DebugGroupEnterpriseObjects);

			NSLog.debug.setAllowedDebugLevel( NSLog.DebugLevelInformational );

			URL modelUrl = new File("test_files/Portrait.eomodeld").toURL();
			EOModel portraitModel = new EOModel( modelUrl );
			EOModelGroup.defaultGroup().addModel( portraitModel );
			initialized = true;
		}
	}

	public void testFetchAllPortraitRecords() throws MalformedURLException {
		NSArray objects = EOUtilities.objectsForEntityNamed( new EOEditingContext(), "Portrait" );
		NSArray blobs = (NSArray)objects.valueForKey("portrait");
	}

	public void testCreatePhone() {
		EOEditingContext ec = new EOEditingContext();
		EOEnterpriseObject newRecord = EOUtilities.createAndInsertInstance( ec, "Portrait" );
		//newRecord.takeValueForKey( "JustATest", "firstName" );
		ec.saveChanges();
	}

}
