package org.gearman.core;

import java.nio.ByteBuffer;

public interface GearmanCodec<X> {
	public void init(GearmanCodecChannel<X> channel);
	public ByteBuffer createByteBuffer();
	public void decode(GearmanCodecChannel<X> channel);
	public byte[] encode(GearmanPacket packet);
}
