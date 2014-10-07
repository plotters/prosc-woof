package com.prosc.fmpjdbc.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.PrintWriter;
import java.io.Writer;
import java.io.StringWriter;
import java.io.IOException;

/**
 * Various methods for abusing Strings.
 */
public class StringUtils {
	private static final Logger log = Logger.getLogger( StringUtils.class.getName() );

	private static final Pattern searchTermsPattern = Pattern.compile("\\w[\\w']*|('|\")[^'\"]+\\1");

	public static final String CR = System.getProperty( "line.separator" );

	public static String emptyStringForNull(String s) {
		return s == null ? "" : s;
	}

	public static String escapeXMLString(String s) {
		if( s == null ) return null;
		StringBuffer buffer = new StringBuffer((int)(s.length() * 1.1));
		escapeXMLString(s, buffer);
		return buffer.toString();
	}

	public static void escapeXMLString(String s, Appendable toAppendTo) {
		if( s == null ) return; //Do not append anything in this case
		for (int i=0; i<s.length(); i++) {
			escapeXMLChar (s.charAt(i), toAppendTo);
		}
	}

	public static void escapeXMLChar(char c, Appendable toAppendTo) {
		try {
			switch (c) {
				case '<':  escapeEntity("lt",toAppendTo); return;
				case '>':  escapeEntity("gt",toAppendTo); return;
				case '&':  escapeEntity("amp",toAppendTo); return;
				case '"':  escapeEntity("quot",toAppendTo); return;
				default:   toAppendTo.append(c);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void escapeEntity(String xmlEntity, Appendable sb) {
		try {
			sb.append('&');
			sb.append(xmlEntity);
			sb.append(';');
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Escapes quotes with a backslash character.
	 * @param in String which may contain quotes and backslash characters.
	 * @return The string with all quotes and backslash characters escaped by a preceding backslash.
	 */
	public static String addSlashes(String in) {
		if (in == null) return null;
		return in.replaceAll("([\"'\\\\])", "\\\\$1");
	}

	/**
	 * Removes backslashes which are being used in a String to escape a single quote, double quote, or backslash character.
	 * @param in String which may contain backslash-escaped characters.
	 * @return The input String with escaping backslashes removed.
	 */
	public static String stripSlashes(String in) {
		if (in == null) return null;
		return in.replaceAll("\\\\([\\\\\"''])", "$1");
	}

	/**
	 * Performs a replace operation on a literal search and replace string, escaping any special regex characters in the search string and the replace string.
	 * <p>
	 * The <code>searchText</code> parameter may contain brackets, stars, whatever, they will be treated as regular text.
	 * <p>
	 * The <code>replaceText</code> can contain dollar signs and backslashes, which would normally indicate a group replace number.
	 * @param input The string to do the replacement on
	 * @param searchText a literal string, e.g. <code>"${firstname}"</code> or <code>"[firstname]"</code> or <code>"&lt;&lt;firstname&gt;&gt;"</code>
	 * @param replaceText a literal string to replace <code>searchText</code> with.
	 * @return the <code>input</code> string, with all literal occurences of <code>searchText</code> replaced with the literal <code>replaceText</code>.
	 */
	public static String replaceAllLiteral(final String input, final String searchText, final String replaceText) {
		final String replaceTextEscaped = replaceText == null || replaceText.length() == 0 ? "" : replaceText.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\$", "\\\\\\$");
		return input.replaceAll("\\Q" + searchText + "\\E", replaceTextEscaped);
	}

	public static String join(String delimiter, Object... parts) {
		return join( Arrays.asList( parts ), delimiter );
	}

	public static String join( Object[] parts, String delimiter ) {
		return join( Arrays.asList( parts ), delimiter );
	}

	/**
	 * Joins the String values of the contents of the Object array (parts) by inserting (delimiter) between each String.
	 * This is the counterpart to String.split() and does the exact opposite of String.split().
	 * @param parts The Object array that contains the objects, the String values of which you want to join together.
	 * @param delimiter The String to insert between each object's String value in (parts)
	 * @return Returns a String object constructed by joining each String value of the objects in (parts) using
	 *  String.valueOf() on each object and with the given (delimiter). Returns null if the (parts) array is null
	 *  or returns the empty String if the (parts) array is empty. A null delimiter throws a NullPointerException.
	 * @author: David Kovacs
	 */
	public static String join(Iterable<?> parts, String delimiter) {
		if (parts == null) return null;
		if (delimiter == null) throw new NullPointerException("The delimiter cannnot be null. Use an empty string if you want no delimiter.");

		StringBuilder result = new StringBuilder();
		String currentDelim = null;
		for( Object part : parts ) {
			if( currentDelim == null ) currentDelim = delimiter;
			else result.append( currentDelim );
			result.append( String.valueOf( part ) );
		}
		return result.toString();
	}



	/**
	 * Removes single or double quotes which surround a String.  Matching quotes must appear at the
	 * beginning and end of the String for it to be considered quoted.
	 * @param maybeQuoted A String which may or may not be surrounded with single or double quotes.
	 * @return The input String with zero or one pairs of surrounding quotes removed, or null if the input String is null.
	 * @author: Sam Barnum
	 */
	public static String unquote(String maybeQuoted) {
		if (maybeQuoted == null || maybeQuoted.length() < 2) return maybeQuoted;
		boolean isQuoted = false;
		int len = maybeQuoted.length();
		if (maybeQuoted.charAt(0) == '"' && maybeQuoted.charAt(len - 1) == '"') isQuoted = true;
		else if (maybeQuoted.charAt(0) == '\'' && maybeQuoted.charAt(len - 1) == '\'') isQuoted = true;
		if (isQuoted) {
			maybeQuoted = maybeQuoted.substring(1, len-1);
		}
		return maybeQuoted;
	}

	/**
	 * Splits the input String into parts separated by the separator argument, where the separator does not fall between single or double quotes.
	 * The array of Strings will include quotes, you can use {@link #unquote(java.lang.String)} to remove them.
	 * <p>If a separator occurs more than once sequentially, it is treated as a single separator.
	 * <p>Note: apostrophes will throw this off, since there's no good way to differentiate them from quotes.
	 * @param in The String to split apart
	 * @param separator The token to split the string apart by
	 * @return An array of Strings which were separated by tokens in the input String.
	 * @author: Sam Barnum
	 */
	public static String[] quotedExplode(String in, char separator) {
		String[] out = new String[patternCount(in, String.valueOf(separator)) + 1];
		int arrayIndex = 0, lastTokenIndex = 0, i=0;
		char quoteType = 0, c, lastChar=0;
		for ( ; i<in.length(); i++) {
			c = in.charAt(i);
			if ((c == '\'' || c == '"') && lastChar != '\\') {
				if (quoteType == c) {
					// leaving a quote block
					quoteType = 0;
				} else if (quoteType == 0) {
					// starting a new quote block
					quoteType = c;
				} else {
					// this is a quoted quote, safe to ignore
				}
			} else if (quoteType == 0 && c == separator) {
				// unquoted separator character
				if (lastChar == c) {
					// keep scanning until we are past the separators
					lastTokenIndex = i+1;
				} else {
					String tok = in.substring(lastTokenIndex, i);
					if (tok.length() > 0) {
						out[arrayIndex++] = tok;
					}
					lastTokenIndex = i+1;
				}
			}
		}
		if (i > lastTokenIndex) {
			// add the last trailing token
			out[arrayIndex++] = in.substring(lastTokenIndex, i);
		}
		if (arrayIndex < out.length) {
			// copy the array to an array of the correct size
			String[] tmp = out;
			out = new String[arrayIndex];
			System.arraycopy(tmp, 0, out, 0, arrayIndex);
		}
		return out;
	}

	/**
	 * Returns the number of times <code>pattern</code> occurs in <code>string</code>
	 * @param string The String to count the pattern in
	 * @param pattern The pattern to count
	 * @return  the number of times <code>pattern</code> occurs in <code>string</code>
	 */
	public static int patternCount(String string, String pattern) {
		int out = 0, index = 0, len = string.length();
		while (index < len && (index = string.indexOf(pattern, index) + 1) > 0) {
			out++;
		}
		return out;
	}

	/**
	 * Encodes a String in a format suitable for use in a javascript.
	 * <ul>
	 * <li>Escapes backslash characters</li>
	 * <li>Converts newlines to backslash-n sequences</li>
	 * </ul>
	 * @param text The text to encode
	 * @return Encoded String.
	 */
	public static String encodeJS(String text) {
		text = addSlashes(text);
		text = text.replaceAll("\n", "\\\\n");
		return text;
	}

	/**
	 * Inserts spaces in an HTML string to prevent very long blocks of text without whitespace from wrapping the screen or expanding elements beyond their desired size.
	 * Only text outside of angled brackets will be altered.
	 * @param result The input text
	 * @param maxLineLength The maximum number of unbroken non-whitespace characters that are allowed to occur.
	 * @return Either the original string, or a modified version with spaces added every maxLineLength characters of unbroken non-whitespace characters.
	 */
	public static String wrapLongHTMLLines(String result, int maxLineLength) {
		int len = 0, lastAppendStartIndex=0;
		boolean isInTag = false;
		char c;
		StringBuffer wrappedResult = null;
		for (int i=0; i<result.length(); i++) {
			c = result.charAt(i);
			if (isInTag) {
				if (c == '>') {
					isInTag = false;
				} else {
					continue;
				}
			}
			if (c == '<') {
				isInTag = true;
			}
			if (Character.isWhitespace(c)) {
				len = 0;
			} else {
				len++;
			}
			if (len > maxLineLength) {
				if (wrappedResult == null) {
					wrappedResult = new StringBuffer(result.length() + 10);
				}
				wrappedResult.append(result.substring(lastAppendStartIndex, i));
				wrappedResult.append(" ");
				lastAppendStartIndex = i;
				len = 0;
			}
		}
		if (wrappedResult != null) {
			wrappedResult.append(result.substring(lastAppendStartIndex));
			return wrappedResult.toString();
		} else {
			return result;
		}
	}



	public static void main(String[] args) {
		String result = addSlashes("this has a sing'le q\"uo\\ted block' in it.");
		log.info( "Result: " + result );
		result = stripSlashes(result);
		log.info( "Result: " + result );
		quotedExplode(result, ' ');
		String[] test = new String[] { null, null, "foo", "moo", "goo", "doo"};
		result =  StringUtils.join( test , ":");
		log.info( result );
	}

	/**
	 * Returns words or phrases in the searchString which are separated by whitespace and optionally grouped by single or double quotes.
	 * @param searchString A search string as typed into a searchfield.
	 * @return Collection containing each word or quoted phrase in the searchString.
	 */
	public static Collection<String> searchTerms(String searchString) {
		Collection<String> result = new LinkedList<String>();
		if (searchString == null || searchString.length() == 0) return result;
		Matcher m = searchTermsPattern.matcher(searchString);
		while (m.find()) {
			String eachPart = m.group().trim();
			if (eachPart.length() == 0) continue;
			if ((eachPart.charAt(0) == '"' || eachPart.charAt(0) == '\'') && eachPart.charAt(eachPart.length()-1) == eachPart.charAt(0)) {
				if (eachPart.length() > 1) {
					//strip the quotes from this search group
					eachPart = eachPart.substring(1, eachPart.length() - 1).trim();
					if (eachPart.length() == 0) continue; // quoted empty string.  Not interested...
				} else {
					continue; // a quote all by itself.  Not interested...
				}
			}
			result.add(eachPart);
		}
		return result;
	}

	/** Pluralize a property name, which is smart about things like nouns ending in 's' or 'y'.
	 * */
	public static String pluralize(String input) {
		//  See http://firstschoolyears.com/literacy/word/other/plurals/resources/rules.htm for a decent list of rules
		String result;
		if (input.endsWith("ife")) {
			return input.substring(0, input.length()-3) + "ives";
		} else if (input.endsWith("s") || input.endsWith("sh") || input.endsWith("ch") || input.endsWith("x") || input.endsWith("z")) {
			result = input.concat("es");
		} else if (input.endsWith("f") && !input.substring(input.length()-2, 1).matches("[efo]")) {
			return input.substring(0, input.length()-1) + "ves";
		} else if (input.endsWith("o")) {
			return input + "es";
		} else if (input.endsWith("y") && !isVowel(input.charAt(input.length() - 2))) {
			result = input.substring(0, input.length() - 1).concat("ies");
		} else {
			result = input.concat("s");
		}
		return result;
	}

	public static String pluralizeIf(int count, @NotNull String word, @Nullable String pluralFormOptional) {
		if (count == 1) return word;
		return isEmpty(pluralFormOptional) ? pluralize(word) : pluralFormOptional;
	}

	private static boolean isVowel(char c) {
		char lower = Character.toLowerCase(c);
		return lower == 'a' || lower == 'e' || lower == 'i' || lower == 'o' || lower == 'u';
	}

	/**
	 * If the requestedName is already taken, appends "Copy" plus an optional numeric index to the requestedName.
	 * If requestedName ends with "Copy" plus an optional numeric index, it will be stripped for purposes of finding the unique name with the smallest numeric prefix.
	 * Strings are compares in a case-insensitive manner.
	 * @param requestedName The desired name
	 * @param takenNames
	 * @return a unique name.
	 */
	public static String nonConflictingName(String requestedName, Collection takenNames) {
		if (caseInsensitiveSearch(takenNames, requestedName) == null) return requestedName; // no conflict
		Pattern pattern = Pattern.compile("^(.*) copy ?[0-9]*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(requestedName);
		String root = requestedName;
		if (matcher.find()) root = matcher.group(1);
		root += " copy";
		//String root = matcher.group() + " copy"; // strips any trailing copy and number, then appends "copy" string without number
		String result = root;
		int suffix = 0;
		while (caseInsensitiveSearch(takenNames, result) != null){
			result = root + " " + (++suffix);
		}
		return result;
	}

	/**
	 * Looks through a collection of objects to find one whose string value is a case insensitive match for param s.
	 * @param names the collection to search through.
	 * @param s the string to search for.
	 * @return The matching name, or null if no match was found.
	 */
	private static String caseInsensitiveSearch(Collection<String> names, String s) {
		for( String name : names ) {
			String o = String.valueOf( name );
			if( s.equalsIgnoreCase( String.valueOf( o ) ) ) return o;
		}
		return null;
	}

	/**
	 * Converts a camelCaseName to CAMEL_CASE_NAME
	 */
	public static String camelCaseToCONSTANT_NAME(String s) {
		Pattern p = Pattern.compile("[A-Z]*[a-z0-9]*");
		Matcher m = p.matcher(s);
		StringBuilder result = new StringBuilder();
		while (m.find()) {
			if (result.length() > 0 && m.group(0).length() > 0) result.append('_');
			result.append(m.group(0));
		}
		return result.toString().toUpperCase();
	}

	/**
	 * Converts a string to camel case, so "Get \"some\" 'big' object" becomes "getSomeObject".
	 * @param input The string to convert
	 * @param capitalizeFirsLetter Self-explanatory
	 */
	public static String toCamelCase( String input, boolean capitalizeFirsLetter ) {
		if( input == null ) return null;
		input = input.replaceAll( "[^A-Za-z0-9 ]", "" ).trim();
		StringBuilder result = new StringBuilder( input.length() );
		char[] chars = input.toCharArray();
		for( int n=0; n<chars.length; n++ ) {
			if( n == 0 ) {
				result.append( capitalizeFirsLetter ? Character.toUpperCase(chars[n]) : Character.toLowerCase(chars[n]) );
			} else {
				if( chars[n] != ' ' ) result.append( chars[n] );
				else {
					while( chars[n] == ' ') n++;
					result.append( Character.toUpperCase(chars[n]) );
				}
			}
		}
		return result.toString();
	}

	/** This strips out all special characters that would be illegal for class names or file names and replaces them with _ characters. */
	public static String normalizeString( String s ) {
		return s.replaceAll( "[^A-Za-z_0-9]", "_" );
	}

	/**
	 * Returns a nice version of an attribute name, so thisIsATest becomes 'This Is A Test'.
	 * and this_is_a_test becomes 'This Is A Test';
	 */
	public static String beautifyAttributeName(String name) {
		if( name == null ) return null;
		int len = name.length();
		if( len == 0 ) return name;
		StringBuilder out = new StringBuilder(len + 4);
		out.append(Character.toUpperCase(name.charAt(0)));
		char c;
		for (int i=1; i<len; i++) {
			c = name.charAt(i);
			if (c=='_') {
				out.append(' ');
				out.append(Character.toUpperCase(name.charAt(++i)));
			} else {
				if ( (Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i-1)) ) ) {
					out.append( ' ' );
				}
				out.append(c);
			}
		}
		return out.toString();
	}

	/** Truncates the input sting so that it is not longer than maxLength. If it is, it will replace all the excess text with "...". The total length including the '...' will not exceed maxLength.
	 * If input is null, returns null.
	 * @throws IllegalArgumentException If maxLength < 3 (needed for the '...' characters).
	 * */
	public static String truncate( String input, int maxLength ) throws IllegalArgumentException {
		if( input == null ) return null;
		if( input.length() < maxLength ) return input;
		String ellipsis = "\u2026";
		if( maxLength < ellipsis.length() ) throw new IllegalArgumentException("maxLength must be at least " + ellipsis.length() );
		return input.substring( 0, maxLength - ellipsis.length() ) + ellipsis;
	}

	public static String titleCase(final String s) {
		if (s == null || s.length() == 0) {
			return s;
		} else if (s.length() == 1) {
			return s.toUpperCase();
		} else {
			StringBuilder result = new StringBuilder(s.length());
			String delim = "";
			for (String eachPart : s.split(" ")) {
				result.append(delim);
				delim = " ";
				if (eachPart.length() < 2) {
					result.append(eachPart);
				} else {
					result.append(eachPart.substring(0, 1).toUpperCase()).append(eachPart.substring(1).toLowerCase());
				}
			}
			return result.toString();
		}
	}

	/** Return true if the string is not null, and the length is greater than zero. */
	public static boolean isEmpty(final String s) {
		return s == null || s.length() == 0;
	}

	public static String exceptionToString(final Throwable e) {
		final Writer writer = new StringWriter();
		e.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	/** Returns the text between the first occurrence of the beginning and end marker. If the searchString is null or the beginning marker cannot be found, returns null.
	 * If the ending marker cannot be found (or <code>end</code> is null, returns everything after the starting mark. */
	public static String textBetween(final String searchString, final String start, final String end) {
		if( searchString == null ) return null;
		int startIndex = searchString.indexOf(start);
		if( startIndex == -1 ) return null;
		startIndex += start.length();
		int endIndex = end == null ? -1 : searchString.indexOf(end, startIndex );
		if( endIndex == -1 ) return searchString.substring( startIndex );
		else return searchString.substring(startIndex, endIndex);
	}

	/** This generates an MD5 hex-encoded version of a String. It's output is identical to calling <pre>md5 -s "theString"</pre> on OS X command line. */
	public static String generateMD5HashString( String theString ) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance( "MD5" );
			digest.update( theString.getBytes( "utf-8" ) );
		} catch( Exception e ) {
			log.log( Level.SEVERE, "Something is wrong with JDK, cannot login", e );
			throw new IllegalStateException( "Misconfiguration on server; cannot login.", e );
		}
		byte[] hash = digest.digest();
		return byteArrayToHex( hash );
	}

	/** This converts a byte array to a hex-encoded String, in lowercase. This code was taken from http://stackoverflow.com/questions/332079/ */
	public static String byteArrayToHex( byte[] array ) {
		BigInteger bi = new BigInteger(1, array);
		return String.format("%0" + (array.length << 1) + "X", bi).toLowerCase();
	}

	/** Null-safe string comparison, where null values are treated as 'less than' non-null values. */
	public static int compare(@Nullable final String s1, @Nullable final String s2) {
		if (s1 == null) return s2 == null ? 0 : -1;
		if (s2 == null) return 1;
		return s1.compareTo(s2);
	}

	/** This works the same as {@link String#valueOf(Object)}, except it returns an empty string instead of "null" if the input is null. */
	public static String valueOf( Object o ) {
		return o == null ? "" : String.valueOf( o );
	}

	/** This returns a String representation of a Collection, with a maximum length specified. This is useful in getting a string representation of a potentially very large collection object without running out of memory.
	 * It protects against large numbers of elements in collections, but cannot protect against very large single elements in collections whose toString() method exceeds available memory. */
	public static String valueOfCollection( Collection<?> c, int maxLength ) {
		if( c == null ) return "";
		
		Iterator<?> i = c.iterator();
		if (! i.hasNext())
			return "[]";

		StringBuilder sb = new StringBuilder( (int)(maxLength * 1.5) );
		sb.append('[');
		for (;;) {
			Object e = i.next();
			String s;
			if( e == c ) {
				s = "(this Collection)";
			} else {
				s = String.valueOf( e );
				if( s.length() > maxLength ) {
					s = s.substring( 0, maxLength-1 );
				}
			}
			sb.append( s );
			if( sb.length() >= maxLength ) {
				if( sb.length() > maxLength ) {
					sb.delete( 1000, sb.length()-1 );
				}
				return sb.append( "<truncated...>" ).toString();
			} else if (! i.hasNext() ) {
				return sb.append(']').toString();
			}
			sb.append(", ");
		}
	}

	public static String repeat(final String s, final int times) {
		StringBuilder sb = new StringBuilder(s.length() * times);
		for(int i=0; i<times; i++) {
			sb.append(s);
		}
		return sb.toString();
	}
}