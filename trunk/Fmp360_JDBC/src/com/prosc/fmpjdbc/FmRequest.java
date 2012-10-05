package com.prosc.fmpjdbc;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: May 31, 2006
 * Time: 12:32:54 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class FmRequest {
	protected String fullUrl;

	public abstract FmFieldList getFieldDefinitions() throws IOException, SQLException;

	public abstract void closeRequest();

	public abstract void doRequest(String postArgs) throws IOException, SQLException;

	String getFullUrl() {
		return fullUrl;
	}
}
