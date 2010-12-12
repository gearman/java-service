package org.gearman;

import java.io.IOException;
import java.util.Set;

import org.gearman.core.GearmanCodec;
import org.gearman.core.GearmanConnectionHandler;

public interface GearmanServer extends GearmanService{
	
	/**
	 * Attempts to open the default port to listen on
	 * @throws IOException
	 * 		thrown if opening the port fails
	 */
	public void openPort() throws IOException;
	
	/**
	 * Attempts to open and listen on a given port.
	 * @param port
	 * 		The port to open
	 * @throws IOException
	 * 		thrown if opening the port fails
	 */
	public void openPort(final int port) throws IOException;
	
	/**
	 * Attempts to open and listen on a given port.<br>
	 * <br>
	 * Allows the user specifies the GearmanCodec used on this port.   
	 * @param port
	 * 		The port to open
	 * @param codec
	 * 		The codec to use for encoding and decoding GearmanPackets
	 * @throws IOException
	 * 		thrown if opening the port fails
	 */
	public <X> void openPort(final int port, final GearmanCodec<X> codec) throws IOException;
	
	/**
	 * Attempts to close a port that this GearmanServer is listening on.
	 * @param port
	 * 		The port to close
	 * @return
	 * 		<code>true</code> if and only if the this GearmanServer secedes at closing
	 * 		the given port number
	 */
	public boolean closePort(final int port);
	
	/**
	 * Closes all ports currently opened by this GearmanServer
	 */
	public void closeAllPorts();
	
	/**
	 * Returns a set of Integers representing the all of the open ports 
	 * @return
	 * 		a set of Integers representing the all of the open ports
	 */
	public Set<Integer> getOpenPorts();
	
	/**
	 * Returns a local connection to this GearmanServer
	 * @return
	 * 		A local connection to this GearmanServer
	 */
	public <X> void createGearmanConnection(GearmanConnectionHandler<X> handler);
}

