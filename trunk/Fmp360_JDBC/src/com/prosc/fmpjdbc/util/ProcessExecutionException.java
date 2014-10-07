package com.prosc.fmpjdbc.util;


import com.prosc.fmpjdbc.util.StringUtils;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Mar 4, 2010 Time: 8:30:23 AM
 */
public class ProcessExecutionException extends Exception {
	private byte[] data;
	private int exitStatus;

	public ProcessExecutionException( byte[] data, String errorMessage, int exitStatus ) {
		super( errorMessage );
		this.data = data;
		this.exitStatus = exitStatus;
	}

	public byte[] getData() {
		return data;
	}

	public int getExitStatus() {
		return exitStatus;
	}
	
	//FIX! This is specific to FileMaker Server. Move this code into whatever calls this method.
	public Integer getErrorCode() {
		String errorCodeString = StringUtils.textBetween(getMessage(), "Error: ", StringUtils.CR);
		if( errorCodeString == null ) {
			return null;
		} else {
			int mark1 = errorCodeString.indexOf( ' ' );
			if( mark1 != -1 ) {
				errorCodeString = errorCodeString.substring( 0, mark1 ).trim(); //Strip out everything prior to the first space. Sometimes we get replies like this: "809 (Unknown error)"
			}
			return Integer.valueOf( errorCodeString );
		}
	}
}
