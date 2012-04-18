/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.core;

import java.nio.ByteBuffer;

public interface GearmanCodecChannel<X> {
	public ByteBuffer getBuffer();
	public void setBuffer(ByteBuffer buffer);
	public void setCodecAttachement(X att);
	public X getCodecAttachement();
	public void onDecode(GearmanPacket packet);
}
