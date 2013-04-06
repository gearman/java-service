package org.gearman.test.pseudoserver;

import java.io.IOException;

import org.gearman.context.GearmanContext;
import org.gearman.impl.core.GearmanConnection;
import org.gearman.impl.core.GearmanConnectionHandler;
import org.gearman.impl.core.GearmanConnectionManager;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.util.GearmanUtils;

/**
 * A tool to send user defined responses back to the client/server 
 * 
 * @author isaiah
 *
 */
public class PseudoServer implements AutoCloseable{
	
	/** Specifies the user defined responses */
	private final Responsor responsor;
	
	/** Manages connections and encode/decodes packets */
	private final GearmanConnectionManager connectionManager;
	
	/**
	 * Constructor
	 * @param port
	 * 		the port number to listen on
	 * @param responsor
	 * 		the user defined responsor
	 * @throws IOException
	 * 		if an I/O exception occurs
	 */
	public PseudoServer(int port, Responsor responsor) throws IOException {
		this.responsor = responsor;
		this.connectionManager = new GearmanConnectionManager();
		
		this.connectionManager.openPort(port, new InnerGearmanConnectionHandler());
	}

	/**
	 * Closes the server
	 */
	@Override
	public void close() {
		connectionManager.shutdown();
	}
	
	/**
	 * Forwards packets to the requestor and sends responses
	 * @author isaiah
	 */
	private final class InnerGearmanConnectionHandler implements GearmanConnectionHandler<Object> {
		
		@Override
		public void onAccept(GearmanConnection<Object> conn) {
			GearmanContext.LOGGER.info(GearmanUtils.toString(conn) + ": Connected");
		}

		@Override
		public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
			GearmanContext.LOGGER.info(GearmanUtils.toString(conn) + ": IN :" + packet.getPacketType());
			try {
				GearmanPacket response = PseudoServer.this.responsor.onPacketReceived(packet);
				
				if(response!=null) {
					GearmanContext.LOGGER.info(GearmanUtils.toString(conn) + ": OUT :" + response.getPacketType());
					conn.sendPacket(response, null);
				}
			} catch (Exception e) {
				GearmanContext.LOGGER.warn("exception thrown", e);
			}
		}

		@Override
		public void onDisconnect(GearmanConnection<Object> conn) {
			GearmanContext.LOGGER.info(GearmanUtils.toString(conn) + ": Disconnected");
		}
	}
}
