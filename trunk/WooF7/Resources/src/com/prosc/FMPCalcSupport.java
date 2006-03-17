package com.prosc;

import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eoaccess.EODatabaseOperation;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;

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

//FIX: Calcs are not updating when creating multiple new objects that have initial values.

/** @deprecated WooF7 does not use this class. It might be added back in at some time though. */
public class FMPCalcSupport extends Object {
	private static final FMPCalcSupport supportObject = new FMPCalcSupport();
	private boolean needsListener = true;
	
	private NSMutableArray invalidObjects = new NSMutableArray();
	private EODatabaseContext whichContext;
	private EOEditingContext ec;
	private EOObjectStoreCoordinator coordinator;
	private NSArray updatedIDs;
	private NSArray insertedIDs;
	
	static public void enable() {
		System.out.println("¥Enabling FMPCalcSupport¥");
		EODatabaseContext.setDefaultDelegate(supportObject);
		//EOAdaptorContext.setDefaultDelegate(supportObject);
	}
	
	/*public NSArray databaseContextWillPerformAdaptorOperations( EODatabaseContext aDatabaseContext, NSArray adaptorOperations, EOAdaptorChannel adaptorChannel) {
		whichContext = aDatabaseContext;
		coordinator = aDatabaseContext.coordinator();
		NSSelector selector = new NSSelector("recordChanges", new Class[] {NSNotification.class});
		NSNotificationCenter.defaultCenter().addObserver(this, selector, EOObjectStore.ObjectsChangedInStoreNotification, whichContext);
		selector = new NSSelector("processChanges", new Class[] {NSNotification.class});
		NSNotificationCenter.defaultCenter().addObserver(this, selector, EOEditingContext.EditingContextDidSaveChangesNotification, null);
		aDatabaseContext.setDelegate(null);
		return adaptorOperations;
	}
	
	public void recordChanges(NSNotification theNotification) {
		System.out.println("¥Received recordChanges message from " + theNotification.object());
		updatedIDs = (NSArray)theNotification.userInfo().objectForKey(EOObjectStore.UpdatedKey);
		insertedIDs = (NSArray)theNotification.userInfo().objectForKey(EOObjectStore.InsertedKey);
		System.out.println("Updated list:" + updatedIDs);
		System.out.println("Inserted list:" + insertedIDs);
		System.out.println("¥Done with recordChanges");
	}*/
	
	public void processChanges(NSNotification theNotification) {
		EOObjectStore objectStore = (EOObjectStore)theNotification.object();
		objectStore.invalidateObjectsWithGlobalIDs(invalidObjects);
		
	}
	
	//EODatabaseContext delegate messages
	/*public abstract NSDictionary databaseContextNewPrimaryKey( EODatabaseContext aDatabaseContext, Object object, EOEntity anEntity) {
		
	}*/
	
	
	public NSArray databaseContextWillOrderAdaptorOperations( EODatabaseContext aDatabaseContext, NSArray databaseOperations) {
		if (needsListener) {
			NSSelector selector = new NSSelector("processChanges", new Class[] {NSNotification.class});
			NSNotificationCenter.defaultCenter().addObserver(this, selector, EOEditingContext.EditingContextDidSaveChangesNotification, null);
			needsListener = false;
		}
		
		NSMutableArray allAdaptorOperations = new NSMutableArray();
		NSArray adaptorOperations;
		
		invalidObjects.removeAllObjects();
		EODatabaseOperation eachOperation;
		int operation;
		java.util.Enumeration en = databaseOperations.objectEnumerator();
		while (en.hasMoreElements()) {
			eachOperation = (EODatabaseOperation)en.nextElement();
			operation = eachOperation.databaseOperator();
			adaptorOperations = eachOperation.adaptorOperations();
			allAdaptorOperations.addObjectsFromArray(adaptorOperations);
			if (adaptorOperations.count() > 0 && (operation == EODatabaseOperation.DatabaseInsertOperator || operation == EODatabaseOperation.DatabaseUpdateOperator)) {
				invalidObjects.addObject(eachOperation.globalID());
			}
		}
		//aDatabaseContext.coordinator().invalidateObjectsWithGlobalIDs(invalidObjects);
		//aDatabaseContext.coordinator().invalidateAllObjects();
		//whichContext = aDatabaseContext;
		//System.out.println("¥InvalidObject list contains " + invalidObjects.count() + " items");
		return allAdaptorOperations;
	}
	
	/*public  NSDictionary databaseContextShouldUpdateCurrentSnapshot( EODatabaseContext aDatabaseContext, NSDictionary currentSnapshot, NSDictionary newSnapshot, com.webobjects.eocontrol.EOGlobalID globalID, EODatabaseChannel channel) {
		System.out.println("¥Replace with snapshot: " + newSnapshot);
		return newSnapshot;
	}
	
	//EOAdaptorContext delegate messages
	public Throwable adaptorChannelDidPerformOperations( Object channel, NSArray operations, Throwable exception) {
		//whichContext.invalidateObjectsWithGlobalIDs(invalidObjects);
		//whichContext.invalidateAllObjects();
		//whichContext.forgetSnapshotsForGlobalIDs(invalidObjects);
		//System.out.println("¥Invalidated " + invalidObjects.count() + " snapshots");
		//System.out.println(invalidObjects);
		//EOEditingContext ec = ((EOEnterpriseObject)invalidObjects.objectAtIndex(0)).editingContext();
		//ec.invalidateObjectsWithGlobalIDs(invalidObjects);
		//ec.invalidateAllObjects();
		//ec.refaultObjects();
		java.util.Enumeration en = invalidObjects.objectEnumerator();
		EOEnterpriseObject invalidEO;
		while(en.hasMoreElements()) {
			invalidEO = (EOEnterpriseObject)en.nextElement();
			invalidEO.editingContext().refaultObject(invalidEO, invalidEO.globalID(), invalidEO.editingContext());
		}
		//invalidObjects.removeAllObjects();
		//System.out.println("¥All objects invalidated");
		return exception;
	}*/
}