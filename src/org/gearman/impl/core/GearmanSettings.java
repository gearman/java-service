/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.core;

import java.nio.charset.Charset;

public class GearmanSettings {
	private GearmanSettings() {
	}
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final int DEFAULT_PORT = 4730;
}
