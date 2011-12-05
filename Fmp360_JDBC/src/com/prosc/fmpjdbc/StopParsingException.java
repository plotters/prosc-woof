package com.prosc.fmpjdbc;

import org.xml.sax.SAXException;

/**
 * Created by IntelliJ IDEA. User: jesse Date: 12/2/11 Time: 12:06 PM
 */
public class StopParsingException extends SAXException {
	public StopParsingException( String s ) {
		super(s);
	}
}
