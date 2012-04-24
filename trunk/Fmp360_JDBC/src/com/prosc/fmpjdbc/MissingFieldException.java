package com.prosc.fmpjdbc;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: jesse Date: 4/17/12 Time: 8:03 PM
 */
public class MissingFieldException extends FileMakerException {
	private Collection<String> missingFields;
	private String whichLayout;

	public MissingFieldException( String reason, int vendorCode, String whichLayout, Collection<String> missingFields ) {
		super( vendorCode, reason );
		this.whichLayout = whichLayout;
		this.missingFields = missingFields;
	}

	public String getWhichLayout() {
		return whichLayout;
	}

	public Collection<String> getMissingFields() {
		return missingFields;
	}
}
