package com.prosc.fmpjdbc;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: May 31, 2006
 * Time: 12:32:54 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class FmRequest {

	public abstract FmFieldList getFieldDefinitions() throws IOException;

	public abstract void closeRequest();

	public abstract void doRequest(String postArgs) throws IOException, FileMakerException;
}
