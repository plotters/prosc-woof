package com.prosc.fmpjdbc;

/**
 * Created by IntelliJ IDEA.
 * User: dkovacs
 * Date: May 26, 2006
 * Time: 5:18:15 PM
 *
 * copied from ProscFramework
 */


public class NumberUtils {

	/** Returns any non-numeric characters from a string.
	* negative signs and decimals are preserved.
	* @see #removeNonIntegerChars
	*/
	static public String removeNonNumericChars(String text) {
		char[] chars = text.trim().toCharArray();
		StringBuffer extracted = new StringBuffer(chars.length);
		boolean foundDecimal = false;
		for (int i=0; i< chars.length; i++) {
			switch(chars[i]) {
				case '-':
					if (extracted.length()>0) break; // only negative sign if it is 1st non-numeric char
					extracted.append(chars[i]);
					break;
				case '.':
					if (foundDecimal) break; // only keep the first decimal point
					if( extracted.length() == 0 ) extracted.append( '0' ); //Make sure there is a leading 0 before the decimal place
					foundDecimal = true;
				case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
					extracted.append(chars[i]);
					break;
			}
		}
		if( extracted.length() == 0 ) return "0"; //This way we won't get a NumberFormatException
		return extracted.toString();
	}

	/** Removes anything that isn't 0-9, including decimals and negative signs.
	* @see #removeNonNumericChars
	*/
	static public String removeNonIntegerChars(String text) {
		if (text==null) return "";
		char[] chars = text.toCharArray();
		StringBuffer extracted = new StringBuffer(chars.length);
		boolean foundDecimal = false;
		for (int i=0; i< chars.length; i++) {
			if (chars[i] >= '0' && chars[i] <= '9') {
				extracted.append(chars[i]);
			}
		}
		return (extracted.toString());
	}

	static public void main(String[] args) {
		if (args.length==0) {
			System.out.println("Usage: ");
			String s = (NumberUtils.removeNonIntegerChars(null));
			Integer n = new Integer(s);
			System.out.println(n);
		}
		else {
			String s = (NumberUtils.removeNonIntegerChars(args[0]));
			Integer n = new Integer(s);
			System.out.println(n);
		}
	}
}
