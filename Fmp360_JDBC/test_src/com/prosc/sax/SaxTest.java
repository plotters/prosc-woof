package com.prosc.sax;

import junit.framework.TestCase;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA. User: jesse Date: 12/2/11 Time: 12:46 PM
 */
public class SaxTest extends TestCase {
	private static final Logger log = Logger.getLogger( SaxTest.class.getName() );
	
	String bigXml = "http://wpeoffice.360works.com/fmi/xml/FMPXMLRESULT.xml?-db=Extremely%20Large%20Database&-lay=Many%20records&-findall";

	public void testDownloadXml() throws Exception {
		long bytesRead=0;
		long startTime = System.currentTimeMillis();
		InputStream in = new URL( bigXml ).openStream();
		try {
			byte[] buffer = new byte[ 1024 * 1024 ]; //1 megabyte
			int chunkSize;
			while( (chunkSize=in.read( buffer )) != -1 ) {
				bytesRead += chunkSize;
				System.out.println("Chunk size " + chunkSize + "; total size " + bytesRead );
			}
		} finally {
			in.close();
			long duration = System.currentTimeMillis() - startTime;
			System.out.println("Total bytes read: " + bytesRead + "; time was " + duration + "ms" );
		}
	}

	public void testLargeXmlParsing() throws Exception {
		SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

		//setFeature( parser, "http://xml.org/sax/features/validation", false );
		//setFeature( parser, "http://xml.org/sax/features/namespaces", false );
		//setFeature( parser, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false );

		final InputSource emptyInput = new InputSource( new ByteArrayInputStream(new byte[0]) );


		URL bigXmlUrl = new URL( bigXml );
		InputStream in = bigXmlUrl.openStream();
		try {
			parser.parse( in, new DefaultHandler() {
				int rows=0;

				@Override
				public InputSource resolveEntity( String publicId, String systemId ) throws IOException, SAXException {
					return emptyInput;
				}

				@Override
				public void startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException {
					if( "ROW".equals( qName) ) {
						System.out.println("Row " + ++rows);
					}
				}
			} );
		} finally {
			in.close();
		}
	}

	private void setFeature( SAXParser parser, String feature, boolean enabled ) {
		try {
			parser.getXMLReader().setFeature( feature, enabled );
		} catch( SAXException e ) { //Ignore
			log.warning( "Could not enable feature " + feature + " because of a SAXException: " + e );
		}
	}
}
