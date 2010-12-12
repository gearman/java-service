package org.gearman.core;

import java.nio.ByteBuffer;

public interface GearmanCodecChannel<X> {
	public ByteBuffer getBuffer();
	public void setBuffer(ByteBuffer buffer);
	public void setCodecAttachement(X att);
	public X getCodecAttachement();
	public void onDecode(GearmanPacket packet);
}
