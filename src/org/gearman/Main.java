package org.gearman;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.gearman.util.ArgumentParser;
import org.gearman.util.ArgumentParser.Option;

/**
 * The class that starts the standalone server
 * @author isaiah.v
 */
class Main {
	
	private static String VERSION = "java-gearman-service-0.1";
	private static String HELP = 
		VERSION + "\n" +
		"\n" +
		"usage:\n" +
		"java [jvm options] -jar "+VERSION+".jar [server options]\n" +
		"\n" +
		"Options:\n" +
		"   -p PORT   --port=PORT     Defines what port number the server will listen on (Default: 4730)\n" +
		"   -v        --version       Display the version of java gearman service and exit\n" +
		"   -?        --help          Print this help menu and exit";
	
	/**
	 * Prints the current version and 
	 * @param out
	 */
	private static final void printHelp(PrintStream out) {
		out.println(HELP);
		System.exit(0);
	}
	
	private static final void printVersion() {
		System.out.println(VERSION);
		System.exit(0);
	}
	
	/**
	 * Starts the standalone gearman job server.
	 * @throws IOException 
	 */
	public static void main(final String[] args) {
		try {
			Gearman gearman = new Gearman();
			GearmanServer server = gearman.createGearmanServer();
			((ServerImpl)server).closeGearmanOnShutdown(true);
			
			server.openPort(new Main(args).getPort());
		} catch (Throwable th) {
			System.err.println(th.getMessage());
			System.err.println();
			printHelp(System.err);
		}
	}
	
	private int port = 4730;
	
	private Main(final String[] args) {
		final ArgumentParser ap = new ArgumentParser();
		
		boolean t1, t2, t3;
		t1 = ap.addOption('p', "port", true);
		t2 = ap.addOption('v', "version", false);
		t3 = ap.addOption('?', "help", false);
		
		assert t1&&t2&&t3;
		
		ArrayList<String> arguments = ap.parse(args);
		if(arguments==null) {
			System.err.println("argument parsing failed");
			System.err.println();
			printHelp(System.err);
		} else if(!arguments.isEmpty()) {
			System.err.print("received unexpected arguments:");
			for(String arg : arguments) {
				System.err.print(" "+arg);
			}
			System.err.println('\n');
			printHelp(System.err);
		}
		
		for(Option op : ap) {
			switch(op.getShortName()) {
			case 'p':
				try {
					this.port = Integer.parseInt(op.getValue());
				} catch(NumberFormatException nfe) {
					System.err.println("failed to parse port to integer: "+op.getValue());
					System.err.println();
					printHelp(System.err);
				}
				break;
			case 'v':
				printVersion();
			case 'h':
				printHelp(System.out);
			}
		}
	}
	
	private int getPort() {
		return this.port;
	}
}
