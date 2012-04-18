package org.gearman.impl.serverpool;

public enum ControllerState {
	
	/**
	 * The state specifying the ConnectionController is in the processes of connecting to a server<br>
	 * <br>
	 * You can enter this state from the CLOSED and WAITING state
	 */
	CONNECTING,
	
	/**
	 * The state specifying the ConnectionController is connected to a job server<br>
	 * <br>
	 * You can enter this state from the CONNECTING state.
	 */
	OPEN,
	
	/**
	 * The state specifying the ConnectionController is waiting to close. The controller
	 * will enter this state if the user has specified it wants to close the connection
	 * but there are pending JOB_STATUS 
	 */
	CLOSE_PENDING,
	
	
	/**
	 * The state specifying the ConnectionController is not connected to a job server<br>
	 * <br>
	 * You can enter this state from the CONNECTING, OPEN, CLOSED, and WAITING states. This is the initial state.
	 */
	CLOSED,
	
	/**
	 * The state specifying the ConnectionController is no longer in regular use or controlled by
	 * the ServiceClient
	 * <br>
	 *  You can enter this state from the CONNECTING, OPEN, CLOSED, CLOSE_PENDING ,DROPPED, and WAITING states. This is the final state 
	 */
	DROPPED,
	
	/**
	 * The state specifying the ConnectionController is in a suggested timeout period. It is
	 * suggested that we wait out a period time before attempting to move to the OPEN state.
	 * However, this is only a suggestion. It is legal to move into the OPEN state.   
	 * <br>
	 * You can enter this state from the CONNECTING, OPEN, CLOSED, and WAITING states.
	 */
	WAITING
}
