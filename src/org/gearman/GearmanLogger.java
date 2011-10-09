package org.gearman;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.gearman.core.GearmanConnection;

class GearmanLogger {
	
	static GearmanLogger createGearmanLogger(Gearman gearman, GearmanClient client) {
		return new GearmanLogger(gearman.getLogger(), GearmanLogger.getDefaultLoggerID(client));
	}
	
	static GearmanLogger createGearmanLogger(Gearman gearman, GearmanServer server) {
		return new GearmanLogger(gearman.getLogger(), GearmanLogger.getDefaultLoggerID(server));
	}
	
	static GearmanLogger createGearmanLogger(Gearman gearman, GearmanWorker worker) {
		return new GearmanLogger(gearman.getLogger(), GearmanLogger.getDefaultLoggerID(worker));
	}
	
	private final Logger logger;
	private String loggerId;
	
	private GearmanLogger(Logger logger, String loggerId) {
		this.logger = logger;
		this.loggerId = loggerId;
	}
	
	final Logger getLogger() {
		return logger;
	}
	
	final String getLoggerID() {
		return this.loggerId;
	}
	
	final void setLoggerID(String loggerId) {
		this.loggerId = loggerId;
	}
	
	final void log(Throwable th) {
		logger.log(Level.SEVERE, loggerId + ": " + th.getMessage() , th);
	}
	
	final void log(Level level, String msg) {
		logger.log(level, msg);
	}
	
	final void log(String msg) {
		logger.log(Level.INFO, loggerId+": "+msg);
	}
	
	public static String getDefaultLoggerID(GearmanClient client) {
		return getDefaultLoggerID("CLIENT",client);
	}
	
	public static String getDefaultLoggerID(GearmanWorker worker) {
		return getDefaultLoggerID("WORKER", worker);
	}
	
	public static String getDefaultLoggerID(GearmanServer server) {
		return getDefaultLoggerID("SERVER", server);
	}
	
	public static String getDefaultLoggerID(Gearman gearman) {
		return getDefaultLoggerID("GEARMAN", gearman);
	}
	
	public static final String getDefaultLoggerID(String servicetype, Object o) {
		return servicetype + '@' + Integer.toHexString(o.hashCode());
	}
	
	public static final String toString(GearmanConnection<?> conn) {
		return "["+conn.getHostAddress() + ":" + conn.getPort() +"]";
	}
}
