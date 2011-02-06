package org.gearman.core;

import java.nio.charset.Charset;

public class GearmanConstants {
	private GearmanConstants() {
	}
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final int DEFAULT_PORT = 4730;
}
