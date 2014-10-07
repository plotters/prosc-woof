package com.prosc.fmpjdbc.util;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a convenience class for calling shell commands.
 */
public class ProcessUtils {
	private static final Logger log = Logger.getLogger( ProcessUtils.class.getName() );

	/** Equivalent to calling doShellCommand(commandArray, env, workingDir, 0). No TimeoutException is thrown. */
	public static byte[] doShellCommand( String[] commandArray, @Nullable Map<String,String> env, @Nullable File workingDir ) throws ProcessExecutionException, InterruptedException, IOException {
		try {
			return doShellCommand( commandArray, env, workingDir, 0 );
		} catch( TimeoutException e ) { //This can't happen
			throw new InterruptedException();
		}
	}


	/** Calls the shell script in the current thread, blocking until it finishes. You call it the same way that you would call Runtime.getRuntime().exec, 
	 * and it returns the stdout contents as a 
	 * byte array if the exit status is 0.
	 *
	 * @param commandArray The array of shell command params
	 * @param env Map of key value pairs of environment variables. These are overlaid with any existing environment variables.
	 * @param workingDir The working directory in which to execute the command. Use null to inherit the process working directory.
	 * @param timeout Number of milliseconds to wait for the command to finish before a TimeoutException is thrown.
	 * @return The stdout as a byte array. You can get it as a string by using the new String( byte[], String charsetName ) constructor.
	 * @throws ProcessExecutionException If the exit status is non-zero, this exception is thrown. It contains any bytes read from stdout, the error message as a string, and the exit status.
	 * @throws InterruptedException If the current Thread is interrupted while waiting for the external process to finish.
	 * @throws IOException
	 */
	public static byte[] doShellCommand( final String[] commandArray, @Nullable final Map<String,String> env, @Nullable final File workingDir, final int timeout ) throws ProcessExecutionException, InterruptedException, IOException, TimeoutException {
		Callable<byte[]> callable = new Callable<byte[]>() {
			public byte[] call() throws IOException, InterruptedException, ProcessExecutionException {
				boolean shouldLog = false ; //Disable this because it can reveal confidential information in logs --jsb

				for( String s : commandArray ) {
					if( s == null ) throw new IllegalArgumentException( "None of the elements of the commandArray may be null: " + Arrays.asList( commandArray ) );
				}
				if( shouldLog && log.isLoggable( Level.INFO ) ) {
					String message = "Executing shell command with params: " + Arrays.asList( commandArray );
					if( workingDir != null ) message += " with directory " + workingDir.getAbsolutePath();
					if( env != null ) message += "\n Env variables: " + Arrays.asList(env);
					log.log( Level.INFO, message );
				}

				Map<String,String> envMap = new LinkedHashMap<String, String>( System.getenv() );
				if( env != null ) {
					for (Map.Entry<String, String> entry : env.entrySet()) {
						envMap.put(entry.getKey(), entry.getValue());
					}
				}
				//String[] finalEnv = new String[envMap.size()];
				/*int n=0;
				for (Map.Entry<String, String> entry : envMap.entrySet()) {
					finalEnv[n++] = entry.getKey() + "=" + entry.getValue();
				}*/
				ProcessBuilder builder = new ProcessBuilder( commandArray );
				if( workingDir != null ) {
					builder.directory( workingDir );
				}
				if( env != null ) {
					for( Map.Entry<String, String> entry : envMap.entrySet() ) {
						builder.environment().put( entry.getKey(), entry.getValue() );
					}
				}
				builder.redirectErrorStream( true );
				Process process = builder.start();
				//Process process = Runtime.getRuntime().exec( commandArray, finalEnv, workingDir ); //FIX! UseProcessBuilder instead
				//FIX!! BE sure to read http://kylecartmell.com/?p=9; very good tips there on Process pitfalls

				int exitStatus;
				byte[] data;
				//InputStream errorStream = process.getErrorStream();
				InputStream inputStream = process.getInputStream();
				//StreamReaderThread errorReader = new StreamReaderThread( errorStream );

				try {
					//errorReader.start();
					data = IOUtils.inputStreamAsBytes( inputStream );
					exitStatus = process.waitFor();

					if( exitStatus == 0 ) {
						/*String errorResult = new String( errorReader.getOutput(), "utf-8" );
						if( errorResult.length() > 0 ) {
							log.info("Process completed with successful status code, but there was output to stderr: " + errorResult );
						}*/
						return data;
					} else {
						//String cmdErr = new String( errorReader.getOutput(), "UTF-8" );
						/*if( cmdErr.length() == 0 ) {
							cmdErr = new String( data, "UTF-8" );
						}*/
						
						String[] commandCopy = new String[commandArray.length];
						System.arraycopy(commandArray, 0, commandCopy, 0, commandArray.length);
						
						//Special handling to mask any parameter that follows another parameter which contains the string "pass"
						for( int m=0; m<commandCopy.length; m++ ) {
							if( commandCopy[m] != null && commandCopy[m].toLowerCase().contains( "pass" ) && m+1<commandCopy.length ) {
								commandCopy[m+1] = "xxxxx";
							}
						}
						
						String errorMessage = "Command " + Arrays.asList( commandCopy ) + " failed with exit status " + exitStatus;
						//if( cmdErr.length() > 0 ) errorMessage += "\nError message: " + cmdErr;
						if( data.length > 0 ) errorMessage += "\nCommand output: \n" + new String( data, "UTF-8" );
						throw new ProcessExecutionException( data, errorMessage, exitStatus );
					}
				} finally {
					inputStream.close();
					//errorReader.interrupt();
					//errorStream.close();
				}
			}
		};

		try {
			if( timeout <= 0 ) {
				return callable.call();
			} else {
				ExecutorService service = Executors.newSingleThreadExecutor();
				try {
					Future<byte[]> future = service.submit( callable );
					return future.get( timeout, TimeUnit.MILLISECONDS );
				} finally {
					service.shutdown();
				}
			}
		} catch( Throwable t ) { //Exception handling of nested exceptions is painfully clumsy in Java
			if( t instanceof ExecutionException ) {
				t = t.getCause();
			}
			if( t instanceof ProcessExecutionException ) {
				throw (ProcessExecutionException)t;
			} else if( t instanceof InterruptedException ) {
				throw (InterruptedException)t;
			} else if( t instanceof IOException ) {
				throw (IOException)t;
			} else if( t instanceof TimeoutException ) {
				throw (TimeoutException)t;
			} else if( t instanceof Error ) {
				throw (Error)t;
			} else if( t instanceof RuntimeException) {
				throw (RuntimeException)t;
			} else {
				throw new RuntimeException( t );
			}
		}
	}


	public static Collection<String> findPids( String processName, String commandSearch ) throws TimeoutException {
		String[] lines;
		String[] cmd;
		if( Platform.current == Platform.windows ) {
			cmd = new String[] {"wmic", "PROCESS", "where", "name like '%" + processName + "%'", "GET", "Processid,Caption,Commandline" };
		} else {
			cmd = new String[] {"ps", "axwww"};
		}
		try {
			byte[] bytes = ProcessUtils.doShellCommand( cmd, null, null, 15000 );
			String searchIn = new String( bytes, "utf-8" );
			
			lines = searchIn.split( System.getProperty( "line.separator" ) );
		} catch( TimeoutException e ) {
			log.log( Level.SEVERE, "Process timed out: " + Arrays.asList( cmd ), e );
			throw e;
		} catch( Exception e ) {
			throw new RuntimeException( e ); //This really shouldn't happen, this is all built-in command stuff.
		}

		Collection<String> result = new LinkedList<String>();
		for( String line : lines ) {
			if( line.contains( processName ) && line.contains( commandSearch ) ) {
				if( Platform.current == Platform.windows ) {
					//wmic has no discernable structure that I can see. The pid is the last thing on the line, and it's surrounded by what seems to be an arbitrary number of spaces. Fun!
					line = line.trim();
					result.add( line.substring( line.lastIndexOf( ' ' ) + 1 ) );
				} else {
					//ps lists the pid first, so it's easy
					result.add( line.substring(0, line.indexOf(" ")) );
				}
			}
		}

		return result;
	}

	/** This will kill the designated process ID. It will do a graceful kill command (kill on Mac, taskkill on Windows), waiting up to millisecondsForSoft. If the process still has not exited, then it will
	 * do a hard kill instead (kill -9 on Mac, taskkill /f on Windows). If the hard kill does not complete within millisecondsForhardKill, then it throws a CouldNotKillProcessException.
	 * Note that the millisecondsForHardKill does not start running until millisecondsForSoft concludes, so the total max time to wait is millsecondsForSoft + millisecondsForHardKill
	 * @param processId
	 * @param millisecondsForSoft Specify 
	 * @param millisecondsForHardKill
	 */
	//public static void killProcess( String processId, int millisecondsForSoft, int millisecondsForHardKill ) {
	//	
	//}
}
 