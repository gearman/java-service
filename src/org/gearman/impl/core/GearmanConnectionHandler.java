/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.core;

public interface GearmanConnectionHandler<X> {
	public void onAccept(GearmanConnection<X> conn);
	public void onPacketReceived(GearmanPacket packet, GearmanConnection<X> conn);
	public void onDisconnect(GearmanConnection<X> conn);
}
