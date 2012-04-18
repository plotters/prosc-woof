package com.prosc.fmpjdbc;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA. User: jesse Date: 4/17/12 Time: 8:03 PM
 */
public class MissingFieldException extends SQLException {
	private Collection<String> missingFields;
	private String whichLayout;

	public MissingFieldException( String reason, String SQLState, int vendorCode, String whichLayout, Collection<String> missingFields ) {
		super( reason, SQLState, vendorCode );
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
