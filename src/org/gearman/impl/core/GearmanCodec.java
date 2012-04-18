/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.core;

import java.nio.ByteBuffer;

public interface GearmanCodec<X> {
	
	/**
	 * The init method is called to allow the user to initialize the {@link GearmanCodecChannel}
	 * before the decode method is called.
	 * @param channel
	 * 		The channel to initialize
	 */
	public void init(GearmanCodecChannel<X> channel);
	public ByteBuffer createByteBuffer();
	public void decode(GearmanCodecChannel<X> channel, int byteCount);
	public byte[] encode(GearmanPacket packet);
}
