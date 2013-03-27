package com.prosc.shared;

import java.util.Date;
import java.util.logging.Logger;

/** This is used to do millisecond-level timing of execution code, for optimization purposes. Typical useage would be something like this:
<pre>
DebugTimer dt = new DebugTimer("Starting Big Task setup code");
//A bunch of setup code
dt.markTime("Beginning main code");
//Main code
dt.markTime("Teardown & cleanup");
//Teardown and cleanup
dt.stop();
</pre>
After stop() is called, it will dump a stopwatch log of each step to System.out. Because it does not generate any output until stop() is called, it has no significant effect on the execution speed of the code it is timing.
*/
public class DebugTimer {
	private static final Logger log = Logger.getLogger( DebugTimer.class.getName() );
	protected String lastMessage = null;
	protected Date startTime;
	protected Date lastMark;
	protected Date currentTime;
	protected StringBuffer progressMessage = new StringBuffer();
	protected boolean enabled;

	/** Equivalent to calling DebugTimer(debugMessage, true). */
	public DebugTimer(String debugMessage) {
		this(debugMessage, true);
	}

	/** Starts a new timer.
	@param debugMessage The initial message to describe the execution events that will follow.
	@param enabled If this is true, the debug timer behaves normally. If false, it disables the timer. This is convenient if you wish to disable the debug timer, but you wish to leave all of the markers in the code so that they can be easily turned back on later.
	*/
	public DebugTimer(String debugMessage, boolean enabled) {
		this.startTime = new Date();
		this.enabled = enabled;
		markTime("Starting timer: " + debugMessage);
	}

	/** Call this method at each milestone in the execution code. This will subdivide the output log into smaller sections with the markMessage associated with each point in time. */
	public void markTime( String markMessage ) {
		currentTime = new Date();
		if( lastMessage != null ) {
			//Display previous timestamp
			long markTime = currentTime.getTime() - lastMark.getTime();
			long totalTime = currentTime.getTime() - startTime.getTime();
			progressMessage.append(markTime + "ms / " + totalTime + "ms: " + lastMessage + "\n");
		}
		lastMessage = markMessage;
		lastMark = currentTime;
	}

	/** This stops the timer and dumps the timing results to System.out. */
	public void stop() {
		//currentTime = new Date();
		//long totalTime = currentTime.getTime() - startTime.getTime();
		markTime(null);
		if(enabled) {
			log.info(progressMessage.toString());
		}
	}

	/** For testing only - not part of the API. */
	static public void main(String[] args) {
		DebugTimer testTimer = new DebugTimer("Counting to 5,000,000");
		int counter;
		for( counter = 0; counter < 5000000; counter++);
		testTimer.markTime("Counting to 100,000");
		for( counter = 0; counter < 100000; counter++);
		testTimer.markTime("Counting  to 20,000,000");
		for( counter = 0; counter < 20000000; counter++);
		testTimer.stop();
	}
}