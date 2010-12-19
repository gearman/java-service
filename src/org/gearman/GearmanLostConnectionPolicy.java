package org.gearman;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Specifies what to do when a job server unexpectedly disconnects or a service cannot connect
 * to a job server.
 * 
 * @author isaiah
 *
 */
public interface GearmanLostConnectionPolicy {
	
	/**
	 * Defines why a method is being called
	 * @author isaiah
	 *
	 */
	public static enum Grounds {
		
		/**
		 * The server in question unexpectedly disconnected.
		 */
		UNEXPECTED_DISCONNECT,
		
		/**
		 * The gearman service failed to connect to a server registed with the service  
		 */
		FAILED_CONNECTION,
		
		/** 
		 * The connection was closed due to a server failing to respond
		 */
		RESPONCE_TIMEOUT,
	};
	
	/**
	 * Called when a gearman service fails to connect to a remote job server or is unexpectedly
	 * disconnected.<br>
	 * <br>
	 * If null is returned or some runtime exception is thrown, the default policy will be taken.
	 * The default policy is normally to reconnect after a period of time.
	 * 
	 * @param adrs
	 * 		The address of the server in question 
	 * @param service
	 * 		The gearman service calling this method
	 * @param grounds
	 * 		The grounds for calling this method
	 * @return
	 * 		An {@link Action} telling the gearman service what actions to take
	 */
	public Action lostRemoteServer(InetSocketAddress adrs, GearmanJobServerPool service, Grounds grounds);
	
	/**
	 * Called when a gearman service fails to connect to a local job server or is unexpectedly
	 * disconnected.<br>
	 * <br>
	 * Servers running in the local address space only cause connection failures if it's been
	 * shutdown. Reconnecting to a shutdown local job server is not an option. Therefore
	 * they're always removed from the service. This method notifies the user that the server is
	 * being removed from the service.
	 * 
	 * @param server
	 * 		The local gearman server in question
	 * @param service
	 * 		The gearman service calling this method
	 * @param grounds
	 * 		The grounds for calling this method
	 */
	public void lostLocalServer(GearmanServer server, GearmanJobServerPool service, Grounds grounds);
	
	/**
	 * A class who's objects represent an action the gearman service should take when a job
	 * server disconnects.
	 *  
	 * @author isaiah
	 */
	public final static class Action {
		
		static final Action RECONNECT = new Action(0);
		static final Action DROP = new Action(0);
		
		private final long nanoTime;
		
		private Action(final long nanoTime) {
			this.nanoTime = nanoTime;
		}
		
		/**
		 * Tells the calling service to reconnect at it's own discretion
		 */
		public static Action reconnectServer() {
			return RECONNECT;
		}
		
		/**
		 * Tells the calling service to reconnect no earlier then the the specified delay
		 * 
		 * @param time
		 * 		The amount of time that must pass before any attempts to reconnect can occur
		 * @param unit
		 * 		The unit time
		 */
		public static Action reconnectServer(long time, final TimeUnit unit) {
			if(time<0) time=0;
			return new Action(unit.toNanos(time));
		}
		
		/**
		 * Tells the calling service to drop the server from its control
		 */
		public static Action dropServer() {
			return DROP;
		}
		
		/**
		 * Returns the amount of time the gearman service must wait before attempting
		 * to reconnect
		 * 
		 * @return
		 * 		The amount of time the gearman service must wait before attempting
		 * 		to reconnect
		 */
		long getNanoTime() {
			return nanoTime;
		}
	}
}
