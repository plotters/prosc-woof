package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.util.Locale;

public class ErrorCodesTest extends TestCase {

	public void testGetMessageEnglish() throws Exception {
		assertEquals("XML Web Publishing is not enabled - Run the FileMaker Server deployment assistant and make sure that the XML web publishing checkbox is selected", ErrorCodes.getMessage(959));
		assertEquals("Comment is not terminated with \"*/\"", ErrorCodes.getMessage(1205));
		assertEquals("Comment is not terminated with \"*/\"", ErrorCodes.getMessage(1205, Locale.ENGLISH));
	}

	public void testGetMessageGerman() throws Exception {
		assertEquals("Kommentar ist nicht mit \"*/\" beendet", ErrorCodes.getMessage(1205, Locale.GERMAN));
	}

	public void testGetMessageKorean() throws Exception {
		assertEquals("Comment is not terminated with \"*/\"", ErrorCodes.getMessage(1205, Locale.KOREAN)); // should be the same as english
	}

	public void testUnknownError() throws Exception {
		assertEquals("Unknown error (1234567)", ErrorCodes.getMessage(1234567, Locale.ENGLISH));
	}
}