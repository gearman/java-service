package org.gearman.impl.serverpool;

import org.gearman.impl.GearmanConstants;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnection.SendCallbackResult;
import org.gearman.impl.core.GearmanPacket;

class SendCallback implements GearmanCallbackHandler<GearmanPacket, SendCallbackResult> {
	private final GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback;
	
	SendCallback(GearmanCallbackHandler<GearmanPacket, SendCallbackResult> callback) {
		this.callback = callback;
	}
	
	@Override
	public void onComplete(GearmanPacket data, SendCallbackResult result) {
		if(!result.isSuccessful()) {
			GearmanConstants.LOGGER.warn("FAILED TO SEND PACKET : " + data.getPacketType().toString());
		}
		
		if(callback!=null)
			callback.onComplete(data, result);
	}
}
