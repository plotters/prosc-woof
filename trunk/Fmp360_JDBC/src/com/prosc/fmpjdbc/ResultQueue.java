package com.prosc.fmpjdbc;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
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
 * User: brittany
 * Date: Jun 6, 2006
 * Time: 12:17:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ResultQueue implements Iterator {
	private static final Logger log = Logger.getLogger( ResultQueue.class.getName() );
	private long maxSize; // this is the max size of the queue
	private long resumeSize; // this is when we'll start adding items again
	private long currentSize; // this is the currentSize of the queue
	private boolean finished;
	private LinkedList objects; // LinkedLists are un-synchronized
	private LinkedList sizes;
	private volatile Throwable storedError;

	private int rowsReturned = 0;
	private int rowsReceived = 0;
	private int errorRow = -1;
	private String errorFieldName;


	public ResultQueue(long msize, long rsize) {
		maxSize = msize; // so they can't change the size in the middle of processing
		resumeSize = rsize;
		currentSize = 0;
		finished = false;
		objects = new LinkedList();
		sizes = new LinkedList();
	}

	public synchronized void setStoredError( Throwable storedError, String field ) {
		log.finest( "*** Stored an error ***" );
		this.storedError = storedError;
		this.errorFieldName = field;
		errorRow = rowsReceived;
		notify();
	}


	/**
	 * This method will add something to the "queue", it takes in an estimate of the objects size
	 *  and whether or not this is the last item to EVER be added to the queue.
	 * @param toAdd - The item (Object) to add to the queue
	 * @param size - An estimate of the size of toAdd
	 */
	public synchronized void add(Object toAdd, long size) {
		// keep track of total size and only blocks until it can add new elements when
		// what about when the first item you try to add to the list is LARGER than the max size?
		// INCREASE THE MAX SIZE AND ADD IT ANYWAY
		if (!(currentSize >= maxSize && objects.size() == 0)) {
			while (currentSize >= maxSize) {
				try {
					wait();
				} catch (InterruptedException ie) { // interrupted exceptions are thrown when interupt() is called
					throw new RuntimeException(ie);
				}
			} // now it's ok to add something to the queue
		} else {
			// increase the current size because i'm trying to add the first object to the queue, and it's
			// bigger than the max size
			maxSize = size;
		}

		// when it's ok for me to add (notify will be called), add toAdd and size to
		// respective queues, and update the current size of the queue
		objects.addLast(toAdd);
		sizes.addLast(new Long(size));
		currentSize += size;
		rowsReceived++;
		notifyAll(); // just in case someone's waiting to get something out of the queue
	}

	/**
	 * This iterator has a next if the last item has not been added yet
	 * @return true if there are more items to iterate thru
	 */
	public synchronized boolean hasNext() {
		// might not be finished, but nothing ready now so wait...
		while (objects.size() <= 0 && !finished) {
			try {
				wait();
				if( storedError != null ) return true; //This will be thrown in the next() method
			} catch (InterruptedException ie) {
				throw new RuntimeException(ie);
			}
		} // end of while, ready to decide

		if (objects.size() > 0) {
			return true;
		} else {
			return false; // finished putting stuff in the queue
		}
	}

	public synchronized void setFinished() {
		finished = true;
		notifyAll();
	}

	/**
	 * This method will return the next item in the iterator, and it will notify
	 * the add method that it's OK to add more Objects to the iterator
	 * @return the next item in the iterator
	 */
	public synchronized Object next() {
		while (objects.size() == 0 && storedError == null ) { // objects and sizes should always have the same # of elements
			// just in case i'm taking them out faster than i can put them in
			if (finished) {
				// somebody forgot to check for hasNext before calling next!!!!
				throw new NoSuchElementException("There are no elements left in the ResultQueue");
			} else {
				try {
					wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		} // now there's something in the queue

		if( storedError != null && errorRow == rowsReturned ) {
			if( storedError instanceof RuntimeException ) throw (RuntimeException)storedError;
			else throw new RuntimeException("Error while trying to access field '" + errorFieldName + "' in zero-indexed row " + errorRow, storedError);
		}

		Object toReturn = objects.removeFirst();
		Object toReturnSize = sizes.removeFirst();

		currentSize -= ((Long) toReturnSize).longValue();
		if (currentSize < resumeSize) {
			notifyAll();
		}

		rowsReturned++;
		return toReturn;
	}

	public void remove() {
		throw new AbstractMethodError("Not implemented");
	}
}
