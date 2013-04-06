package org.gearman.test.pseudoserver;

import org.gearman.impl.core.GearmanPacket;

/**
 * Defines the responses to the pseudo server
 * @author isaiah
 */
public interface Responsor {
	
	/**
	 * Defines how the pseudo server will respond to packets
	 * @param packet
	 * 		in packet
	 * @return
	 * 		out packet
	 */
	public GearmanPacket onPacketReceived(GearmanPacket packet) throws Exception;
}
