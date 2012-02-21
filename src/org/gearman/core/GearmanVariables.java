package org.gearman.core;

import java.nio.charset.Charset;

public class GearmanVariables {
	private GearmanVariables() {
	}
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final int DEFAULT_PORT = 4730;
	public static String VERSION = "java-gearman-service-0.5.1";
}
