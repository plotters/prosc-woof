package com.prosc.fmpjdbc;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

public class FmXmlRequestTest extends TestCase {

	public void testDoRequest() throws Exception {
		FmXmlRequest request = new FmXmlRequest("http", "localhost", "/fmi/xml/FMPXMLRESULT.xml", 80, "Admin", "insecur3", 13.0f);
		request.doRequest("-db=Backbone&-lay=Singleton&-edit&-recid=1&name=wow え wow");
	}

	public void testEncoding() throws Exception {
		List<String> encodings = Arrays.asList(
				"UTF-8",
				"UTF-16",
				"UTF-32",
				"ISO-8859-1",
				"EUC-JP",
				"ISO-2022-JP",
				"Shift_JIS",
				"windows-31j",
				"x-euc-jp-linux",
				"x-eucJP-Open");
		for (String encoding : encodings) {
			System.out.println("==== ENCODING USING " + encoding + "====");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			OutputStreamWriter writer = new OutputStreamWriter(out, encoding);
			writer.write("え");
			writer.close();
			for (String decoding : encodings) {
				System.out.println(decoding + ": " + out.toString(decoding));
			}
			String string = out.toString("UTF-8");
			//assertEquals("え", string);

		}
	}
}