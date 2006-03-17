package com.prosc.woof7;

import com.webobjects.foundation.NSData;

import java.io.InputStream;
import java.io.IOException;

/**
 * A thin subclass of NSData which includes the ability to get the filename and mimeType for the data.
 * @author sbarnum
 */
public class FMData extends NSData {
	private String mimeType;

	public FMData(InputStream binaryStream, int length, String mimeType) throws IOException {
		super(binaryStream, length);
		this.mimeType = mimeType;
	}

	public String getMimeType() {
		return mimeType;
	}
}
