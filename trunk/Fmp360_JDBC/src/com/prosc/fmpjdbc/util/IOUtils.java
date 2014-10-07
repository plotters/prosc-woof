//
// IOUtils.java
// Project ProscBundle
///Users/ericarnoldy/Java/ProscLib/ProscCore/src/com/prosc/io/IOUtils.java
// Created by jesse on Sun Sep 01 2002
//
package com.prosc.fmpjdbc.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.*;
import java.security.Provider;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/** General purpose utility class for dealing with input streams and urls.*/
public class IOUtils {
	private static final Logger log = Logger.getLogger(IOUtils.class.getName());

	//---InputStream utilities---
	/** The default size to use for buffers when reading from InputStreams - 8192 bytes. */
	public static final int CHUNK_SIZE = 8192;

	/** Reads bytes from an InputStream and transfers them to an OutputStream.
	 * It will block and keep reading until the InputStream is exhausted and returns a -1 on read().
	 * It will flush() the OutputStream, but it does NOT close the InputStream or OutputStream.
	 * @param bufferSize The size of the chunks that will be used for the transfer. Large buffers take more memory, but are generally more efficient. Use IOUtils.CHUNK_SIZE when in doubt.
	 * @param in The stream to read from.
	 * @param out The stream to write to.
	 * @return The actual number of bytes transferred.
	 * @throws java.io.IOException
	 */
	static public long writeInputToOutput(InputStream in, OutputStream out, int bufferSize) throws IOException {
		if( in == null ) throw new IllegalArgumentException("You cannot pass a null InputStream");
		if( out == null ) throw new IllegalArgumentException("You cannot pass a null OutputStream");
		byte[] buffer = new byte[bufferSize];
		int lastBytesRead;
		long totalBytesRead = 0;
		while( (lastBytesRead=in.read(buffer)) != -1 ) {
			out.write( buffer, 0, lastBytesRead );
			totalBytesRead += lastBytesRead;
		}
		out.flush();
		return totalBytesRead;
	}


	/** Writes bytes from in to out, stopping after reading a fixed number of bytes.
	 *
	 * @param in The stream to read from
	 * @param out The stream to write to
	 * @param size The number of bytes to write.
	 * @return The total number of bytes actually read.
	 * @throws java.io.IOException
	 */
	public static long writeFixedByteCount( InputStream in, OutputStream out, long size ) throws IOException {
		byte[] buffer = new byte[ Math.min(CHUNK_SIZE, (int)size) ];
		long totalBytesRead=0;
		while( totalBytesRead < size ) {
			long nextChunk = Math.min(size - totalBytesRead, buffer.length);
			int bytesRead = in.read( buffer, 0, (int)nextChunk );
			if( bytesRead == -1 ) break; //Exit before reading full size
			out.write( buffer, 0, bytesRead );
			totalBytesRead += bytesRead;
		}
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
		while ((sequenceMark < sequenceLength) && (result < 1600) ) { //FIX! Temporary stopgap to prevent crash; stop after reading 1600 bytes
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

	/**
	 * Reads the entire input stream and returns it as an array of bytes. It does NOT close the input stream.
	 * @param stream The stream to read from
	 * @return The read bytes
	 * @throws java.io.IOException
	 */
	static public byte[] inputStreamAsBytes(InputStream stream) throws IOException {
		if( stream == null ) throw new IllegalArgumentException("Null stream was passed to inputStreamAsBytes");
		ByteArrayOutputStream allBytes = new ByteArrayOutputStream( stream.available() );
		try {
			writeInputToOutput(stream, allBytes, CHUNK_SIZE);
		} finally {
			allBytes.close();
		}
		return allBytes.toByteArray();
	}

	/**
	 * Reads the entire input stream and returns it as a string. It assumes utf-8 encoding. It does NOT close the input stream.
	 * It works by calling {@link #inputStreamAsBytes} and passing that to the String constructor. It will never return null - it returns an empty string instead.
	 */
	@NotNull
	static public String inputStreamAsString(InputStream stream) throws IOException {
		return inputStreamAsString( stream, "utf-8" );
	}

	/**
	 * Reads the entire input stream and returns it as a string. It does NOT close the input stream.
	 * It works by calling {@link #inputStreamAsBytes} and passing that to the String constructor. It will never return null - it returns an empty string instead.
	 */
	@NotNull
	static public String inputStreamAsString(InputStream stream, String charset) throws IOException {
		return new String( inputStreamAsBytes(stream), charset );
	}

	/**
	 * Reads all text from a Reader object, appending it all to a provided StringBuffer.
	 * @param reader the reader to read
	 * @param toAppendTo the buffer to append to
	 * @return the <code>toAppendTo</code> buffer.
	 * @throws IOException
	 */
	static public <T extends Appendable> T readerAsString(Reader reader, T toAppendTo) throws IOException {
		char[] buffer = new char[CHUNK_SIZE];
		int charsRead;
		while ((charsRead = reader.read(buffer)) != -1) {
			toAppendTo.append( new String( buffer, 0, charsRead ) );
		}
		return toAppendTo;
	}

	//---URL utilities---

	/**
	 * Generate a URL or POST data with properly encoded parameters, e.g.
	 * <pre>
	 *     urlEncoded("http://example.com?comments=%s&name=%s", "this & that", "sam barnum");
	 * </pre>
	 * Returns <code>http://example.com?comments=this+%26+that&name=sam+barnum</code>
	 * @param formatString String suitable for passing to
	 * @param values
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String urlEncoded(String formatString, Object... values) {
		try {
			final Object[] encodedArgs = new Object[values.length];
			for (int i = 0; i < encodedArgs.length; i++) {
				Object eachValue = values[i];
				encodedArgs[i] = eachValue == null ? "" : URLEncoder.encode(eachValue.toString(), "UTF-8").replace( "+", "%20" ); //URLEncoder uses + signs, but it's really better to do %20
			}
			return new Formatter().format(formatString, encodedArgs).toString();
		} catch( UnsupportedEncodingException e ) {
			throw new RuntimeException( e ); //Can't happen
		}
	}

	public static String urlDecoded(@Nullable String string) throws UnsupportedEncodingException {
		return string == null ? null : URLDecoder.decode(string, "UTF-8");
	}

	/** Returns the contents of the URL as a string. This opens the input stream and calls {@link #inputStreamAsString}. It DOES close the input stream after it complete.*/
	@NotNull
	static public String urlConnectionAsString(URLConnection theConnection) throws IOException {
		InputStream theStream;
		try {
			theStream = theConnection.getInputStream();
		} catch (IOException e) {
			if(theConnection instanceof HttpURLConnection) {
				IOException ioe = new IOException( inputStreamAsString( ( (HttpURLConnection)theConnection ).getErrorStream() ) );
				ioe.initCause( e );
				throw ioe;
			} else {
				throw e;
			}
		}
		try {
			return inputStreamAsString( theStream ); //FIX!! Read the charset from the connection and use that
		}
		finally {
			theStream.close();
		}
	}

	/** Posts the specified data using the HTTP POST protocol to the supplied URLConnection. It uses utf-8 encoding.
	 * You can call combine this method with {@link #urlConnectionAsString} by calling this method first.*/
	static public void postDataToUrlConnection(@NotNull String postArgs, URLConnection theConnection) throws IOException {
		if (!theConnection.getDoOutput()) {
			theConnection.setDoOutput(true);
		}
		theConnection.setRequestProperty( "content-length", "" + postArgs.length() );
		PrintStream out = new PrintStream( theConnection.getOutputStream(), false, "utf-8" );
		try {
			out.print(postArgs);
		} finally {
			closeQuietly(out);
		}
	}

	/** This is equivalent to calling {@link #getUrlContents(String, String)} with a null parameter for postArgs. */
	static public String getUrlContents(String theUrl) throws IOException {
		return getUrlContents( theUrl, null );
	}

	/** This is equivalent to calling {@link #getUrlContents(String, String, Integer)} with a null parameter for timeout. */
	static public String getUrlContents( String theUrl, String postArgs) throws IOException {
		return getUrlContents(theUrl, postArgs, null);
	}

	/** This is just a wrapper for {@link #urlConnectionAsString} that takes a URL string as an argument. It is intended to make it extremely simple to get the string contents of a URL. If passed a non-null paramater for postArgs, it will POST that data before receiving the response. */
	static public String getUrlContents(String theUrl, @Nullable String postArgs, Integer timeout) throws IOException {
		log.log(Level.FINE, "Reading URL contents of " + theUrl + " postArgs size = " + (postArgs == null ? 0 : postArgs.length()));
		URLConnection theConnection = new URL(theUrl).openConnection();
		if( timeout != null ) {
			theConnection.setConnectTimeout(timeout);
			theConnection.setReadTimeout(timeout);
		}
		if( postArgs != null ) postDataToUrlConnection( postArgs, theConnection );
		return urlConnectionAsString( theConnection );
	}

	/** If you are using Java 1.3.1 and have the optional JSSE module installed, this will programatically enable it. You ordinarily have to modify a security text file to do this. This method is not required in Java 1.4 or later, which have SSL support built-in. */
	static public void enableSSL() {
		//The next two lines enable SSL encryption
		System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
		try {
			java.security.Security.addProvider((Provider) Class.forName("com.sun.net.ssl.internal.ssl.Provider").newInstance());
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
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
		log.log(Level.FINE, "Reading file contents " + whichFile);
		byte[] data = new byte[(int)whichFile.length()];
		FileInputStream in = new FileInputStream( whichFile );
		try {
			in.read(data);
		} finally {
			in.close();
		}
		return data;
	}

	/** Returns the contents of a file as a String.
	 @param whichFile A java.io.File
	 */
	static public String fileAsString( File whichFile ) throws IOException {
		return fileAsString(whichFile, "UTF-8");
	}

	static public String fileAsString( File whichFile, String charset ) throws IOException {
		return new String( fileAsBytes(whichFile) , charset);
	}

	/** Finds the first numeric suffix that can be appended to the filename to create a unique filename.
	 * This works by appending -x to the file name, before the file suffix, until it finds a unique filename.
	 * For example, if it was passed a file name kidphoto.jpg and the a file already existed with the same name,
	 * it would return a File pointing to kidphoto-1.jpg. If that file also existed, it would return kidphoto-2.jpg, etc.
	 * This works for either regular files or directories.
	 * */
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
				else if( "fdx".equals(suffix) ) mimeType = "application/xml";
			} catch(Exception err) {}
		}
		return mimeType;
	}

	/**
	 * This skips and discards all of the input from the source until the searchString occurs. It will block until the
	 * searchString is found. When it returns, the reader will be positioned after the last character of the searchString.
	 * It does NOT close the source Reader. Because this method reads one byte at a time, It is recommended for efficiency
	 * that the Reader be buffered.
	 * @return The number of characters that were skipped. If all of the bytes are read from the source without finding the
	 *         searchString, -1 is returned.
	 */
	/*
	Currently, this is broken, so I've commented it out. --Jesse
	static public int skipToSearchString( Reader source, String searchString ) throws IOException {
		char[] searchChars = searchString.toCharArray();
		int searchIndex = 0;
		int result = 0;
		int character;
		while( searchIndex < searchChars.length ) {
			character = source.read();
			if( character == -1 ) return -1;
			if( (char)character == searchChars[searchIndex] ) searchIndex++;
			else searchIndex = 0;
			result++;
		}
		System.out.println("Found " + searchString + "!");
		return result;
	}*/



	/**
	 * Takes a Map of key value pairs and encodes them into a URL string.
	 * If one of the values in the Map is a List, then its elements will be repeated with the same key.
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

	/**
	 * @param query e.g. "?q=123&action=go"
	 * @return query string parsed into map values, one value per key
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String,String> urlDecodeToMap(String query) throws UnsupportedEncodingException {
		if (query == null) return Collections.emptyMap();
		final Matcher matcher = Pattern.compile("[\\?&]?([^=&]+)(=([^&]*))?").matcher(query);
		final LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		while(matcher.find()) {
			result.put(urlDecoded(matcher.group(1)), urlDecoded(matcher.group(3)));
		}
		return result;
	}

	private static String systemEncoding = System.getProperty( "file.encoding" );
	public static String systemEncoding() { return systemEncoding; }

	public static String encode(String input) {
		try {
			return URLEncoder.encode( input, systemEncoding ).replace( "+", "%20" ); // FIX! usually we want UTF-8, which is not necessarily the system encoding.
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

	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				final String msg = "Failed to close " + closeable;
				log.log(Level.WARNING, msg, e);
			}
		}
	}

	/**
	 * Unconditionally close an OutputStream.
	 * Equivalent to OutputStream.close(), except any exceptions will be ignored. This is typically used in finally blocks.
	 * @param outputStream
	 */
	public static void closeQuietly(OutputStream outputStream) {
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch (IOException e) {
				String msg = "Failed to close outputStream " + outputStream;
				log.log(Level.WARNING, msg, e);
				//Commented out by Jesse - closeQuietly should not rethrow checked exceptions as runtime exceptions: throw new RuntimeException(msg, e);
			}
		} else {
			log.log(Level.INFO, "Received null outputStream");
		}
	}

	/**
	 * Unconditionally close a Reader.
	 * Equivalent to Reader.close(), except any exceptions will be ignored. This is typically used in finally blocks
	 * @param reader
	 */
	public static void closeQuietly(Reader reader) {
		if (reader != null) {
			try {
				reader.close();
			} catch (IOException e) {
				String msg = "Failed to close reader " + reader;
				log.log(Level.WARNING, msg, e);
			}
		} else {
			log.log(Level.INFO, "Received null reader");
		}
	}


	/**
	 * Unconditionally close an InputStream.
	 * Equivalent to InputStream.close(), except any exceptions will be ignored. This is typically used in finally blocks
	 * @param inputStream
	 */
	public static void closeQuietly(InputStream inputStream) {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				log.log(Level.WARNING, "Failed to close inputStream " + inputStream, e);
			}
		} else {
			log.log(Level.INFO, "Received null inputStream");
		}
	}


	/**
	 * Unconditionally close a Writer.
	 * Equivalent to Writer.close(), except any exceptions will be ignored. This is typically used in finally blocks
	 * @param writer
	 */
	public static void closeQuietly(Writer writer) {
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				log.log(Level.WARNING, "Failed to close writer " + writer, e);
			}
		} else {
			log.log(Level.INFO, "Received null writer");
		}
	}

	public static void copyFile( File src, File dest ) throws IOException {
		InputStream in = new FileInputStream( src );
		OutputStream out = new FileOutputStream( dest );
		try {
			writeInputToOutput( in, out, 8192 );
		} finally {
			in.close();
			out.close();
		}
	}

	/**
	 * Copies source directory to destination directory. Does not delete the destination directory or any of its contents, although it will overwrite files with the same name.
	 * @param srcDir source directory. The contents of this directory (not the directory itself) are copied to the dstDir.
	 * @param dstDir destination directory. The contents of srcDir will be written to dstDir.
	 * @throws IOException
	 */
	public static void copyDirectory(File srcDir, File dstDir) throws IOException {
		if (srcDir.isDirectory()) {
			if (!dstDir.exists()) {
				dstDir.mkdir();
			}

			String[] children = srcDir.list();
			for (int i=0; i<children.length; i++) {
				copyDirectory(new File(srcDir, children[i]),
						new File(dstDir, children[i]));
			}
		} else {
			copyFile(srcDir, dstDir);
		}
	}

	public static void closeQuietly(final Socket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				log.log(Level.WARNING, "Failed to close socket " + socket, e);
			}
		} else {
			log.log(Level.INFO, "Received null socket");
		}
	}

	public static void closeQuietly(final ServerSocket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				log.log(Level.WARNING, "Failed to close socket " + socket, e);
			}
		} else {
			log.log(Level.INFO, "Received null socket");
		}
	}

	/** This reads characters from a reader and writes them to a writer.
	 * @return The number of characters written.
	 */
	public static long writeReaderToWriter(Reader reader, Writer writer) throws IOException {
		return writeReaderToWriter(reader, writer, new char[1024]);
	}

	/** This reads characters from a reader and writes them to a writer, using the supplied buffer.
	 * @return The number of characters written.
	 */
	public static long writeReaderToWriter(Reader reader, Writer writer, char[] buffer) throws IOException {
		int len = buffer.length;
		int charsRead;
		long totalChars=0;
		while(true) {
			charsRead = reader.read(buffer, 0, len);
			if (charsRead > 0) {
				writer.write(buffer, 0, charsRead);
				totalChars += charsRead;
			} else {
				break;
			}
		}
		return totalChars;
	}

	/** Deletes a file or directory. If fileToDelete is a directory, deletes all of its contents recursively. If any deletion
	 * fails, returns false, otherwise returns true. */
	public static boolean deleteRecursive( File fileToDelete ) {
		log.log(Level.INFO, "Deleting " + fileToDelete + " recursively");
		return _deleteRecursive( fileToDelete );
	}

	private static boolean _deleteRecursive( File fileToDelete ) {
		if( fileToDelete.isDirectory() ) {
			File[] children = fileToDelete.listFiles();
			if( children == null ) {
				log.log( Level.WARNING, "Could not get children for directory " + fileToDelete + "; returning false" );
				return false;
			}
			for( File aChildren : children ) {
				if( !_deleteRecursive( aChildren ) ) return false;
			}
		}
		final boolean b = fileToDelete.delete();
		if (!b) {
			log.log( Level.WARNING, "Could not delete " + fileToDelete);
		}
		return b;
	}

	/**
	 * Recursively zips a directory
	 * @param dir2zip path of the directory to zip.
	 * @param zipFileName name of the resulting zip file
	 * @throws IOException
	 *
	 * @deprecated Do not use, it embeds the wrong paths into the zip file. Use ZipCreator instead. --jsb
	 */
	public static void zipDir(String dir2zip, String zipFileName) throws IOException {
		ZipOutputStream zos = new ZipOutputStream( new FileOutputStream(zipFileName) );
		try {
			_zipDir(dir2zip, zos);
		} finally {
			zos.close();
		}
	}

	public static void _zipDir( String dir2zip, ZipOutputStream zos ) throws IOException {
		File zipDir = new File(dir2zip);
		String[] dirList = zipDir.list();
		byte[] readBuffer = new byte[CHUNK_SIZE];
		int bytesIn;
		//loop through dirList, and zip the files
		for( int i=0; i < dirList.length; i++ ) {
			File f = new File( zipDir, dirList[i] );
			if(f.isDirectory()) {
				//if the File object is a directory, call this
				//function again to add its content recursively
				String filePath = f.getPath();
				_zipDir(filePath, zos);
				continue;
			}
			FileInputStream fis = new FileInputStream(f);
			ZipEntry anEntry = new ZipEntry(f.getPath()); // FIX! shouldn't this be a relative path?
			zos.putNextEntry(anEntry);
			while((bytesIn = fis.read(readBuffer)) != -1) {
				zos.write(readBuffer, 0, bytesIn);
			}
			fis.close();
		}
	}

	public static void unzipDir( String file2unzip ) throws IOException {
		BufferedOutputStream dest;
		BufferedInputStream is;
		ZipEntry entry;
		ZipFile zipfile = new ZipFile(file2unzip);
		Enumeration e = zipfile.entries();
		while(e.hasMoreElements()) {
			entry = (ZipEntry) e.nextElement();
			log.info("Extracting: " +entry);
			is = new BufferedInputStream
					(zipfile.getInputStream(entry));
			int count;
			byte data[] = new byte[CHUNK_SIZE];
			FileOutputStream fos = new FileOutputStream(entry.getName());
			dest = new BufferedOutputStream(fos, CHUNK_SIZE);
			while ((count = is.read(data, 0, CHUNK_SIZE)) != -1) {
				dest.write(data, 0, count);
			}
			dest.flush();
			dest.close();
			is.close();
		}
	}

	/** Reads a ZipInputStream and writes all of the files contained in it into the specified dir.
	 * Preserves the timestamps of the zip entries, if there are any. The stream is NOT closed after this
	 * method finishes.
	 * @param stream The ZipInputStream to read from. It will NOT be closed.
	 * @param dir The directory where the contents will be written to.
	 * @return True if this is a valid zip stream, false if it is invalid or has no entries.
	 */
	public static boolean unzipStream( ZipInputStream stream, File dir ) throws IOException {
		ZipEntry eachEntry;
		boolean hasEntries = false;
		while( (eachEntry=stream.getNextEntry()) != null ) {
			hasEntries = true;
			String entryName = eachEntry.getName();
			if( File.separatorChar == '/' ) {
				entryName = entryName.replace( '\\', File.separatorChar ); //Change all backslashes to forward slashes if forward slashes are used as path separator
			}
			File eachFile = new File( dir, entryName );
			if( eachEntry.isDirectory() ) {
				eachFile.mkdirs(); //Create the directory
			} else {
				eachFile.getParentFile().mkdirs(); //Just in case there was an explicit zip entry for the directory
				//if (!eachEntry.isDirectory()) {
				FileOutputStream outStream = new FileOutputStream( eachFile );
				try {
					writeInputToOutput( stream, outStream, 8192 );
				} finally {
					outStream.close();
				}
				//}
			}
			if( eachEntry.getTime() != -1 ) {
				eachFile.setLastModified( eachEntry.getTime() );
			}
		}
		return hasEntries;
	}

	/** Moves a file from one location to another. Attempts to call rename() to move it quickly, which should work if the files are on the same volume as each other.
	 * Otherwise it will copy the source file to the dest location and then delete the source file. If the source file cannot be deleted, it will be left where it is.
	 * This method calls @see IOUtils#moveFile( File source, File dest, boolean overwriteDest ) with <code>overwriteDest = false</code>
	 * @param source
	 * @param dest
	 * @throws IOException if the dest file cannot be created.
	 */
	public static void moveFile( File source, File dest ) throws IOException {
		moveFile(source, dest, false);
	}

	/** Moves a file from one location to another. Attempts to call rename() to move it quickly, which should work if the files are on the same volume as each other.
	 * Otherwise it will copy the source file to the dest location and then delete the source file. If the source file cannot be deleted, it will be left where it is.
	 * @param source
	 * @param dest
	 * @param overwriteDest overwrite destination if it exists.
	 * @throws IOException if the dest file cannot be created.
	 */
	public static void moveFile( @NotNull File source, @NotNull File dest, boolean overwriteDest) throws IOException {
		if(dest.exists() && !overwriteDest) throw new IOException( "File already exists.  " + dest.getAbsolutePath() + " already exists.");
		if( source.getCanonicalPath().equals( dest.getCanonicalPath() ) ) {
			//Nothing to do, files are pointing to the same place
			return;
		}
		if( ! source.renameTo( dest )) {
			copyFile( source, dest );
			source.delete();
		}
	}


	/** This is a replacement version for File.mkdirs() that throws an IOException if the directory cannot be created. It returns true
	 * if the directory was created, false if it already exists, and throws a FileNotFoundException if it does not exist and cannot be created. It
	 * does not allow folders to be created in /Volumes on OS X. */
	public static boolean mkdirs( File dir ) throws FileNotFoundException {
		if( dir.exists() ) return false;
		File realParent = dir;
		while( realParent != null && ! realParent.exists() ) {
			realParent = realParent.getParentFile();
		}
		if( realParent != null && "/Volumes".equals( realParent.getPath() ) && Platform.current == Platform.mac ) {
			throw new FileNotFoundException("You cannot create a directory in the /Volumes folder. This is reserved for mounted volumes." );
		}
		if( ! dir.mkdirs() ) {
			if( realParent != null && !realParent.canWrite() ) {
				throw new FileNotFoundException("Directory " + dir.getAbsolutePath() + " could not be created because the parent directory " + realParent.getAbsolutePath() + " is not writeable for user '" + System.getProperty( "user.name" ) + "'" );
			} else {
				throw new FileNotFoundException("Directory " + dir.getAbsolutePath() + " could not be created because of an unknown error." );
			}
		} else {
			return true;
		}
	}

	/**
	 * Gets around annoying lack of chaining in java 1.5 and earlier
	 * @param ioException
	 * @param e
	 * @return IOException
	 */
	public static IOException ioExceptionWithCause(final IOException ioException, final Throwable e) {
		if (e != null) {
			ioException.initCause(e);
		}
		return ioException;
	}

	public static boolean areInputStreamsEqual(InputStream i1, InputStream i2) throws IOException {
		byte[] buf1 = new byte[CHUNK_SIZE];
		byte[] buf2 = new byte[CHUNK_SIZE];
		try {
			DataInputStream d2 = new DataInputStream(i2);
			int len;
			while((len = i1.read(buf1)) > 0) {
				d2.readFully(buf2, 0, len);
				for(int i = 0; i < len; i++)
					if(buf1[i] != buf2[i]) return false;
			}
			return d2.read() < 0; // is the end of the second file also.
		} catch(EOFException ioe) {
			return false;
		} finally {
			i1.close();
			i2.close();
		}
	}

	/** This locates a temporary directory that is writeable. It first tries the directory at the java.io.tmpdir System property. If that does not exist or is not writeable, tries /tmp on OS X or C:\Windows\TEMP.
	 * If that does not work either, then it checks the user's home direcotry at System.getProperty("user.home"). If that also does not work, then it throws an IOException.
	 * @throws IOException
	 */
	public static File findWriteableTempDirectory() throws IOException {
		File result = new File( System.getProperty( "java.io.tmpdir" ) );
		if( result.canWrite() ) return result;

		result = Platform.current == Platform.windows ? new File( "C:\\Windows\\TEMP" ) : new File( "/tmp/" );
		if( result.canWrite() ) return result;

		return findWriteablePersistentDirectory( false ); //Can't find temp location, use a persistent location
	}

	/** This will look for a writeable directory which is not cleared out on restart. It gives preference to directories that are not visible to the user (ie. not the user's home directory)
	 * @param preferUserDir If true, this will try to find a user-specific directory before storing in a system location. If false, it will try to store in a system location before storing in a user directory. */
	public static File findWriteablePersistentDirectory( boolean preferUserDir ) throws IOException {

		File result;
		if( preferUserDir ) {
			result = findWriteablePersistentUserDirectory();
			if( result == null ) result = findWriteablePersistentSystemDirectory();
		} else {
			result = findWriteablePersistentSystemDirectory();
			if( result == null ) result = findWriteablePersistentUserDirectory();
		}

		if( result != null ) {
			return result;
		} else {
			throw new IOException( "Could not find a writeable directory" );
		}
	}

	private static File findWriteablePersistentSystemDirectory() {
		File result = null;

		if( Platform.current == Platform.mac ) {
			result = new File("/Library/Application Support/360Works");
		} else if( Platform.current == Platform.windows ) {
			//FIX!!! Where is the best system path on Windows?
		} else if( Platform.current == Platform.linux ) {
			result = new File( "/var/lib/360Works" );
		} else {
			log.log( Level.WARNING, "Unsupported platform: " + Platform.current );
		}

		if( result != null ) {
			try {
				ensureDirectoryIsReady( result );
			} catch( FileNotFoundException e ) {
				log.log( Level.WARNING, "Could not find writeable system directory", e );
				result = null;
			}
		}
		return result;
	}

	private static File findWriteablePersistentUserDirectory() {
		File result = new File( System.getProperty( "user.home" ) );
		if( result.canWrite() ) return result;

		return null;
	}

	/** Calls {@link #ensureDirectoryIsReady(java.io.File, boolean)} with a writeableForAll param of false. */
	public static void ensureDirectoryIsReady( File targetDir ) throws FileNotFoundException {
		ensureDirectoryIsReady( targetDir, false );
	}

	/** This ensures that a directory exists at the designated file, and creates one if it does not exist. It checks to makes sure that the directory is readable and writeable.
	 * It also checks to make sure there is not already some other non-directory file at this path. If there is, it will attempt to delete that file and replace it with the directory.
	 * @throws FileNotFoundException If the directory is not able to be created, or does not exist, or has incorrect permissions. An attempt is made to provide as detailed error reporting as possible.
	 * @param targetDir The directory to check
	 * @param writeableForAll If true, then on OS X the directory is recursively chmodded to allow read/write access for all users (777). On Windows this has no effect.
	 */
	public static void ensureDirectoryIsReady( File targetDir, boolean writeableForAll ) throws FileNotFoundException {
		if( targetDir == null ) throw new IllegalArgumentException( "Null value" );
		do {
			if( targetDir.exists() ) {
				if( targetDir.isDirectory() ) {
					if( ! targetDir.canRead() ) {
						throw new FileNotFoundException( "There is an existing directory at " + targetDir + ", but it is not readable." );
					}
					if( ! targetDir.canWrite() ) {
						throw new FileNotFoundException( "There is an existing directory at " + targetDir + ", but it is not writeable." );
					}
					else break; //Found existing directory that is readable and writeable; we're good
				} else { //There is a file at this location. Delete and try again.
					log.warning( "Deleting file at " + targetDir + " to allow creation of directory at that location" );
					if( targetDir.delete() ) {
						//Now allow the loop to repeat
					} else {
						throw new FileNotFoundException( "An existing file at " + targetDir + " could not be deleted; the directory cannot be created." );
					}
				}
			} else if( targetDir.mkdirs() ) {
				log.log(Level.INFO, "successfully created \"Internal\" directory");
				break; //We created the directory successfully; we're good
			} else { //Directory does not exist and we cannot create it, try to come up with a good error message
				String message = "Directory at " + targetDir + " does not exist and cannot be created.";
				File currentDir = targetDir;
				while( (currentDir=currentDir.getParentFile()) != null ) {
					if( currentDir.exists() && ! currentDir.canWrite() ) {
						message += " This is because a parent directory at " + currentDir + " is not writeable.";
						break;
					}
				}
				throw new FileNotFoundException( message );
			}
		} while( true );

		if( writeableForAll && Platform.isMac() ) {
			try {
				ProcessUtils.doShellCommand( new String[] {"/bin/chmod", "-R", "777", targetDir.getAbsolutePath()}, null, null );
			} catch( Exception e ) {
				log.log( Level.WARNING, "Directory exists at " + targetDir.getAbsolutePath() + ", but it could not be changed to allow access for all users.", e );
			}
		}
	}

	/** This attempts to read the error text from the connection, and then rethrow the IOException with that text included. */
	public static IOException rethrowHttpException( IOException e, HttpURLConnection connection ) throws IOException {
		final InputStream errStream = connection.getErrorStream();
		String newMessage;
		if( errStream == null ) {
			throw e; //Just preserve original exception
		} else {
			try {
				newMessage = e.getMessage() + ". HTTP Server message: " + inputStreamAsString( errStream );
			} catch( IOException e1 ) {
				log.log( Level.SEVERE, "Could not read error stream from HTTP server", e1 );
				newMessage = e.getMessage();
			} finally {
				errStream.close();
			}
			IOException rethrow = new IOException( newMessage );
			rethrow.initCause( e );
			throw rethrow;
		}
	}
}
