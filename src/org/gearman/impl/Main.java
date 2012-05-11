/*
 * Copyright (c) 2012, Isaiah van der Elst (isaiah.v@comcast.net)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.gearman.impl;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import org.gearman.Gearman;
import org.gearman.impl.util.ArgumentParser;
import org.gearman.impl.util.ArgumentParser.Option;
import org.gearman.properties.GearmanProperties;

/**
 * The class that starts the standalone server
 * @author isaiah.v
 */
public class Main {
	
	private static String HELP = 
		GearmanConstants.PROJECT_NAME + " v" + GearmanConstants.VERSION + "\n" +
		"\n" +
		"usage:\n" +
		"java [jvm options] -jar "+GearmanConstants.PROJECT_NAME+ "-" +GearmanConstants.VERSION+".jar [server options]\n" +
		"\n" +
		"Options:\n" +
		"   -p PORT   --port=PORT     port number to listen on (Default: 4730)\n" +
		"   -w        --writefile     write out gearman.properties\n" +
		"   -v        --version       display the version and exit\n" +
		"   -h        --help          print help menu and exit";
	
	/**
	 * Prints the current version and 
	 * @param out
	 */
	private static final void printHelp(PrintStream out) {
		out.println(HELP);
		System.exit(0);
	}
	
	private static final void printVersion() {
		System.out.println(GearmanConstants.VERSION);
		System.exit(0);
	}
	
	/**
	 * Starts the standalone gearman job server.
	 * @throws IOException 
	 */
	public static void main(final String[] args) {
		try {
			Main main = new Main(args);
			
			// Create a gearman instance
			Gearman gearman = Gearman.createGearman();
			
			// Create the server and listen on the given port
			gearman.startGearmanServer(main.getPort());
			
		} catch (Throwable th) {
			System.err.println(th.getMessage());
			System.err.println();
			printHelp(System.err);
		}
	}
	
	private int port = GearmanConstants.PORT;
	
	private Main(final String[] args) {
		final ArgumentParser ap = new ArgumentParser();
		
		boolean t1, t2, t3, t4;
		t1 = ap.addOption('p', "port", true);
		t2 = ap.addOption('v', "version", false);
		t3 = ap.addOption('w', "writefile", false);
		t4 = ap.addOption('h', "help", false);
		
		assert t1&&t2&&t3&&t4;
		
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
			case 'w':
				writefile();
			case 'v':
				printVersion();
			case 'h':
				printHelp(System.out);
			}
		}
	}
	
	private void writefile() {
		try {
			GearmanProperties.save();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}
	
	private int getPort() {
		return this.port;
	}
}
