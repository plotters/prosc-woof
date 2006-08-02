package com.prosc.shared;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA. User: jesse Date: Aug 1, 2006 Time: 1:40:01 PM
 */
public class MBeanUtils {
	private static final Logger log = Logger.getLogger( MBeanUtils.class.getName() );

	/* Commented out for now; need to wait until we move to 1.5 JDK
	private static MBeanServer mBeanServer = null;

	static {
		try {
			ArrayList serverList = MBeanServerFactory.findMBeanServer( null );
			if( serverList.size() == 0 ) log.info( "No MBean server available for performance monitoring. This is not a critical problem." );
			else mBeanServer = (MBeanServer)serverList.get( 0 );
		} catch( Throwable t ) {
			log.log( Level.WARNING, "Could not enable MBean for performance monitoring. This is not a critical problem.", t );
		}
	}

	public static void registerMBean(String name, Object bean) {
		if( mBeanServer != null ) try {
			mBeanServer.registerMBean( bean, new ObjectName(name) );
		} catch( Throwable t ) {
			log.log( Level.WARNING, "Could not enable MBean for performance monitoring. This is not a critical problem.", t );
		}
	}

	public static void deregisterMBean(String name) {
		if( mBeanServer != null ) try {
			mBeanServer.unregisterMBean( new ObjectName(name) );
		} catch( Throwable t ) {
			log.log( Level.WARNING, "Could not unregister MBean for performance monitoring. This is not a critical problem.", t );
		}
	}*/
}
