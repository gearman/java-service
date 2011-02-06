package org.gearman.dbg;

import java.io.IOException;

import org.gearman.core.GearmanConnection;
import org.gearman.core.GearmanConnectionHandler;
import org.gearman.core.GearmanConnectionManager;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanConstants;

/**
 * A simple stateless server used to monitor incoming packets. The server does
 * not manage jobs. However, it will return pre-defined response packets.
 * 
 * @author isaiah van der Elst
 */
public class GearmanPacketLogger {
	public static void main(String[] args) throws IOException {
		GearmanConnectionManager gcm = new GearmanConnectionManager();
		try {
			gcm.openPort(GearmanConstants.DEFAULT_PORT, new Handler());
		} catch (IOException ioe) {
			ioe.printStackTrace();
			gcm.shutdown();
		}
	}
	
	private static class Handler implements GearmanConnectionHandler<Object> {

		@Override
		public void onAccept(GearmanConnection<Object> conn) {
			System.out.println(this.toString(conn) + " : Connected");
		}

		@Override
		public void onDisconnect(GearmanConnection<Object> conn) {
			System.out.println(this.toString(conn) + " : Disconnected");
		}

		@Override
		public void onPacketReceived(GearmanPacket packet, GearmanConnection<Object> conn) {
			System.out.println(this.toString(conn) + " : IN : "+packet.getPacketType());
			
			switch(packet.getPacketType()) {
			}
		}
		
		private final String toString(GearmanConnection<Object> conn) {
			return "["+conn.getHostAddress() + ":" + conn.getPort() +"]";
		}
	}
}
