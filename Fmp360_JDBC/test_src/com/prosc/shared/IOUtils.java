//
// IOUtils.java
// Project ProscBundle
//
// Created by jesse on Sun Sep 01 2002
//
package com.prosc.shared;

import java.io.*;
import java.net.*;
import java.util.*;

/** @deprecated use com.prosc.IOUtils
 *  General purpose utility class for dealing with input streams and urls.*/
public class IOUtils extends Object {
	//---InputStream utilities---
	/** The default size to use for buffers when reading from InputStreams - 8192 bytes. */
	public static final int CHUNK_SIZE = 8192;

	/** Reads bytes from an InputStream and transfers them to an OutputStream.
	 * It will block and keep reading until the InputStream is exhausted and returns a -1 on read().
	 * It will flush() the OutputStream, but it does not close the InputStream or OutputStream.
	 * @param bufferSize The size of the chunks that will be used for the transfer. Large buffers take more memory, but are generally more efficient.
	 * @param out If this is null, then the InputStream will be read and the total bytes will be returned, but no writing will be done.
	 * @return The actual number of bytes transferred.
	 */ //OPTIMIZE! This will be used heavily by all of our applications, so it would be worth pulling out DebugTimer and doing some tests.
	static public long writeInputToOutput(InputStream in, OutputStream out, int bufferSize) throws IOException {
		byte[] buffer = new byte[bufferSize];
		int lastBytesRead;
		long totalBytesRead = 0;
		while( (lastBytesRead=in.read(buffer)) != -1 ) {
			out.write( buffer, 0, lastBytesRead );
			totalBytesRead += lastBytesRead;
		}
		if( out != null ) out.flush();
		return totalBytesRead;
	}

	/**
	 * Reads an InputStream and writes it to an OutputStream. Stops after encountering stopSequence. This reads and writes a single byte at a time, so it is a good idea for performance purposes to use buffered streams.
	 * @param in The InputStream to read. Must be non-null.
	 * @param out the OutputStream to write to. If null, then the input will still be scanned, but nothing will be written.
	 * @param stopSequence Reading will stop after encountering this byte sequence.
	 * @return The total number of bytes read.
	 * @throws IOException
	 */
	static public long writeInputToOutput(InputStream in, OutputStream out, byte[] stopSequence) throws IOException {
		int result = 0;
		int sequenceMark = 0;
		int sequenceLength = stopSequence.length;
		int eachByte;
		while ((sequenceMark < sequenceLength) || (result > 800) ) { //FIX!!! Temporary stopgap to prevent crash
			eachByte = in.read();
			if( eachByte == -1 ) break;
			//System.out.print( (char)eachByte );
			out.write( eachByte );
			if( eachByte == stopSequence[sequenceMark] ) sequenceMark++;
			else sequenceMark = 0;
			result++;
		}
		return result;
	}

	/** Reads the entire input stream and returns it as an array of bytes. It does NOT close the input stream.*/
	static public byte[] inputStreamAsBytes(InputStream stream) throws IOException {
		ByteArrayOutputStream allBytes = new ByteArrayOutputStream( stream.available() );
		writeInputToOutput(stream, allBytes, CHUNK_SIZE);
		allBytes.close();
		return allBytes.toByteArray();
	}

	/** Reads the entire input stream and returns it as a string. It does NOT close the input stream.
	 * It works by calling {@link #inputStreamAsBytes} and passing that to the String constructor. It will never return null - it returns an empty string instead. */
	static public String inputStreamAsString(InputStream stream) throws IOException {
		return new String( inputStreamAsBytes(stream) );
	}

	//---URL utilities---

	/** Returns the contents of the URL as a string. This opens the input stream and calls {@link #inputStreamAsString}. It DOES close the input stream after it complete.*/
	static public String urlConnectionAsString(URLConnection theConnection) throws IOException {
		InputStream theStream = null;
		String result = null;
		try {
			theStream = theConnection.getInputStream();
			result = inputStreamAsString( theStream );
		}
		finally {
			if( theStream != null ) theStream.close();
		}
		return result;
	}

	/** Posts the specified data using the HTTP POST protocol to the supplied URLConnection. You can call combine this method with {@link #urlConnectionAsString} by calling this method first.*/
	static public void postDataToUrlConnection(String postArgs, URLConnection theConnection) throws IOException {
		theConnection.setDoOutput(true);
		PrintStream out = new PrintStream( theConnection.getOutputStream() );
		out.print(postArgs +"\n\n");
		out.close();
	}

	/** This is equivalent to calling {@link #getUrlContents(String, String)} with a null parameter for postArgs. */
	static public String getUrlContents(String theUrl) throws MalformedURLException, IOException {
		return getUrlContents(theUrl, null);
	}

	/** This is just a wrapper for {@link #urlConnectionAsString} that takes a URL string as an argument. It is intended to make it extremely simple to get the string contents of a URL. If passed a non-null paramater for postArgs, it will POST that data before receiving the response. */
	static public String getUrlContents(String theUrl, String postArgs) throws MalformedURLException, IOException {
		URLConnection theConnection = new URL(theUrl).openConnection();
		if( postArgs != null ) postDataToUrlConnection( postArgs, theConnection );
		return urlConnectionAsString( theConnection );
	}

	/** If you are using Java 1.3.1 and have the optional JSSE module installed, this will programatically enable it. You ordinarily have to modify a security text file to do this. This method is not required in Java 1.4 or later, which have SSL support built-in. */
	static public void enableSSL() {
		//The next two lines enable SSL encryption
		System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
		java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
	}

	//---File utilities---

	/** Returns the contents of a file path as byte array.
	@param whichFile A String which contains the path to the file.*/
	static public byte[] fileAsBytes( String whichFile ) throws IOException {
		return fileAsBytes( new java.io.File(whichFile) );
	}

	/** Returns the contents of a file as a byte array.
	@param whichFile A java.io.File
	*/
	static public byte[] fileAsBytes( File whichFile ) throws IOException {
		byte[] data = new byte[(int)whichFile.length()];
		FileInputStream in = new FileInputStream( whichFile );
		in.read(data);
		in.close();
		return data;
	}

	/** Returns the contents of a file as a String.
	@param whichFile A java.io.File
	*/
	static public String fileAsString( File whichFile ) throws IOException {
		return new String( fileAsBytes(whichFile) );
	}

	/** Finds the first numeric suffix that can be appended to the filename to create a unique filename. This works by appending -x to the file name, before the file suffix, until it finds a unique filename. For example, if it was passed a file name kidphoto.jpg and the a file already existed with the same name, it would return a File pointing to kidphoto-1.jpg. If that file also existed, it would return kidphoto-2.jpg, etc.*/
	static public File ensureUniqueFilename(File whichFile) {
		if( whichFile.exists() ) {
			int suffixMarker = whichFile.getName().lastIndexOf('.');
			String suffix = "";
			String prefix = whichFile.getPath();
			if( suffixMarker != -1 ) {
				suffix = whichFile.getName().substring(suffixMarker);
				prefix = whichFile.getParent() + File.separator + whichFile.getName().substring(0, suffixMarker);
			}
			for( int appendInt = 1; whichFile.exists(); appendInt++ ) {
				whichFile = new File( prefix + '-' + appendInt + suffix );
			}
		}
		return whichFile;
	}

	//---MIME handling---

	/** This gets the MIME type of a file, based on its name. If no matching MIME type is found, this method returns "untyped/binary". It calls {@link #getExactMimeType} and returns "untyped/binary" if the result is null. **/
	static public String getMimeType(String fileName) {
		if( fileName == null ) return null;
		String mimeType = getExactMimeType(fileName);
		if( mimeType == null ) mimeType = "untyped/binary";
		return mimeType;
	}

	/** This gets the MIME type of a file, based on its name. If no matching MIME type is found, this method returns null. It works by calling {@link URLConnection#getFileNameMap}, and does some custom processing for known types (js, tgz, css, ico, and sit) that are not in that map. **/
	static public String getExactMimeType(String fileName) {
		if( fileName == null ) return null;
		String mimeType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
		if( mimeType == null ) {
			try {
				String suffix = fileName.substring( fileName.lastIndexOf(".") + 1 ).toLowerCase();
				if( "js".equals(suffix) ) mimeType = "text/javascript";
				else if( "tgz".equals(suffix) ) mimeType = "application/x-tar";
				else if( "css".equals(suffix) ) mimeType = "text/css";
				else if( "ico".equals(suffix) ) mimeType = "image/x-icon";
				else if( "sit".equals(suffix) ) mimeType = "application/x-stuffit";
			} catch(Exception err) {}
		}
		return mimeType;
	}



	/**
	 * Takes a Map of key value pairs and encodes them into a URL string.
	 * If one of the values in the Map is a List, then it's elements will be repeated with the same key.
	 * For example, this code: <pre>
	 * Hashtable theDict = new Hashtable(3);
	 * theDict.put("name", "Jesse Barnum");
	 * theDict.put("zip code", "30022");
	 * Vector petArray = new Vector();
	 * petArray.add("Megan");
	 * petArray.add("Buddha");
	 * theDict.put("pets", petArray);
	 * IOUtils.urlEncodeMap(theDict);
	 * </pre>
	 * would return: zip+code=30022&name=Jesse+Barnum&pets=Megan&pets=Buddha
	 * @param dict a Map of key-value pairs
	 * @return The URL-encoded string
	 */
	public static String urlEncodeMap(Map dict) {
		String urlString = "";
		Iterator iterator = dict.keySet().iterator();
		// enumerate over the keys
		while (iterator.hasNext()) {
			String aKey = (String)iterator.next();
			Object anObject = dict.get(aKey);

			// encode the key first
			aKey = encode(aKey);
			if (anObject instanceof List) {
				Iterator en = ((List)anObject).iterator();
				while (en.hasNext()) {
					String nextElement = (String)en.next();
					urlString += aKey + "=" + encode(nextElement);
					if (en.hasNext()) {
						urlString += "&";
					}
				}
			} else {
				urlString += aKey + "=" + encode((String)anObject);
			}
			if (iterator.hasNext()) {
				urlString += "&";
			}
		}
		return urlString;
	}

	private static String systemEncoding = System.getProperty( "file.encoding" );
	public static String systemEncoding() { return systemEncoding; }

	public static String encode(String input) {
		try {
			return URLEncoder.encode( input, systemEncoding );
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}

	public static String decode(String input) {
		try {
			return URLDecoder.decode( input, systemEncoding );
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException(e);
		}
	}


	//---Self test code---

}
