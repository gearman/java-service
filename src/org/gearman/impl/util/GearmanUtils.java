/*
 * Copyright (C) 2010 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.util;

import java.net.InetSocketAddress;
import java.util.UUID;

import org.gearman.impl.core.GearmanConnection;

public class GearmanUtils {
	
	public boolean testJDBCConnection(String jdbcDriver, String jdbcConnectionString) {
		return false;
	}
	
	public boolean testGearmanConnection(InetSocketAddress address) {
		return false;
	}
	
	public static final String toString(GearmanConnection<?> conn) {
		return "["+conn.getHostAddress() + ":" + conn.getPort() +"]";
	}
	
	public static final byte[] createUID() {
		return UUID.randomUUID().toString().getBytes();
	}
}
