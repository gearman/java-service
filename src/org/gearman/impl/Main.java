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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.gearman.impl.util.ArgumentParser;
import org.gearman.impl.util.ArgumentParser.Option;
import org.gearman.impl.util.GearmanUtils;
import org.gearman.properties.GearmanProperties;
import org.gearman.properties.PropertyName;

/**
 * The class that starts the standalone server
 * @author isaiah.v
 */
public class Main {
	
	/** help message */
	private static String HELP = 
		GearmanConstants.PROJECT_NAME + " v" + GearmanConstants.VERSION + "\n" +
		"\n" +
		"usage:\n" +
		"java [jvm options] -jar "+GearmanConstants.PROJECT_NAME+ "-" +GearmanConstants.VERSION+".jar [server options]\n" +
		"\n" +
		"Options:\n" +
		"   -p PORT   --port=PORT        port number to listen on (Default: 4730)\n" +
		"   -c PATH   --classpath=PATH   define the classpath\n" +
		"   -s NAME   --persistence=NAME GearmanPersistence class name\n" +
		"   -w        --writefile        write out the .properties file\n" +
		"   -v        --version          display the version and exit\n" +
		"   -h        --help             print help menu and exit";
	
	/**
	 * Prints the help menu and exits 
	 * @param out
	 * 		The print stream to write the version to
	 */
	private static final void printHelp(PrintStream out) {
		out.println(HELP);
		System.exit(0);
	}
	
	/**
	 * Prints the version number and exits
	 */
	private static final void printVersion() {
		
		System.out.println(GearmanConstants.VERSION);
		System.exit(0);
	}
	
	/**
	 * Starts the standalone gearman job server.
	 */
	public static void main(final String[] args) throws IOException {
		
		/*
		 *  Having a standalone gearman job server packaged within an executable jar
		 *  intrudes some difficult problems. In order to dynamically set the class
		 *  path, we have to load the classpath into a sub ClassLoader and start the
		 *  server using reflection only
		 */
		
		try {
			// Find application arguments and options
			Main main = new Main(args);
			
			// create the classpath
			String classpath = createFullClasspath(main);
			
			// Convert the classpath into an array of URLs
			URL[] urls = GearmanUtils.createUrlClasspath(classpath);
			
			// Start the server
			start(main, urls);
		} catch (Throwable th) {
			// If an error occurs, print the error message, print the help message, and exit
			
			System.err.println("Error: " + th.getClass().getSimpleName() + " : " + th.getMessage());
			System.err.println();
			printHelp(System.err);
		}
	}
	
	/**
	 * Constructs the full class derived from all of the places the classpath can be set
	 * @param main
	 * 		The Main implementation containing the applications arguments and options
	 * @return
	 * 		A complete classpath
	 */
	private static final String createFullClasspath(Main main) {
		return
			main.getClasspath() + File.pathSeparator +
			GearmanProperties.getProperty(PropertyName.GEARMAN_CLASSPATH) +
			System.getProperty("java.class.path") + File.pathSeparator;
	}
	
	/**
	 * Starts the gearman job server
	 * @param main
	 * 		
	 * @param urls
	 * 		The classpath as a set of URLs
	 * @throws Exception
	 * 		If any exception is thrown in the proccess
	 */
	private static final void start(Main main, URL[] urls) throws Exception {
		
		/*
		 * Must specify a 'null' parent, otherwise the system's classloader will be the
		 * parent. 
		 */
		@SuppressWarnings("resource")
		ClassLoader classLoader = new URLClassLoader(urls, null);
		
		// Load the gearman implementation class
		Class<?> gearmanClass = classLoader.loadClass(main.getGearman());
		
		// Create a new gearman instance
		Object gearmanInstance = gearmanClass.newInstance();
		
		// Check id the GearmanPersistence class name was set
		String persistenceName = main.getPersistence();
		if(main.getPersistence()==null || (persistenceName=persistenceName.trim()).isEmpty()) {
			// Without GearmanPersistence
			
			// Get the method, Gearman#startGearmanServer(int)
			Method startGearmanServerMethod = gearmanClass.getMethod("startGearmanServer", Integer.TYPE);
			
			// Invoke the method. This invocation will start the server
			startGearmanServerMethod.invoke(gearmanInstance, main.getPort());
			
		} else {
			// With GearmanPersistence
			
			// Load the GearmanPersistence interface
			Class<?> gearmanPersistenceInterface = classLoader.loadClass("org.gearman.GearmanPersistence");
			
			// Load the GearmanPersistence implementation
			Class<?> gearmanPersistenceClass = classLoader.loadClass(persistenceName);
			
			// Create the GearmanPersistence instance
			Object gearmanPersistenceInstance = gearmanPersistenceClass.newInstance();
			
			// Get the method, Gearman#startGearmanServer(int, GearmanPersistence)
			Method startGearmanServerMethod = gearmanClass.getMethod("startGearmanServer", Integer.TYPE, gearmanPersistenceInterface);
			
			// Invoke the method. This invocation will start the server
			startGearmanServerMethod.invoke(gearmanInstance, main.getPort(), gearmanPersistenceInstance);
		}
	}
	
	private int port = -1;
	private String classpath = "";
	private String persistence = null;
	private String gearman = null;
	
	private Main(final String[] args) {
		try {
			port = Integer.parseInt(GearmanProperties.getProperty(PropertyName.GEARMAN_PORT));
		} catch (NumberFormatException nfe) {
			System.err.println("failed to parse port to integer: " + GearmanProperties.getProperty(PropertyName.GEARMAN_PORT));
			System.err.println();
			printHelp(System.err);
		}
		persistence = GearmanProperties.getProperty(PropertyName.GEARMAN_PERSISTANCE);
		gearman = GearmanProperties.getProperty(PropertyName.GEARMAN_CLASSNAME);
		
		final ArgumentParser ap = new ArgumentParser();
		
		boolean t1, t2, t3, t4, t5, t6;
		t1 = ap.addOption('p', "port", true);
		t2 = ap.addOption('c', "classpath", true);
		t3 = ap.addOption('s', "persistence", true);
		t4 = ap.addOption('v', "version", false);
		t5 = ap.addOption('w', "writefile", false);
		t6 = ap.addOption('h', "help", false);
		
		assert t1&&t2&&t3&&t4&&t5&&t6;
		
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
			case 'c':
				this.classpath = op.getValue();
				break;
			case 's':
				this.persistence = op.getValue();
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
	
	private String getPersistence() {
		return this.persistence;
	}
	
	private String getClasspath() {
		return this.classpath;
	}
	
	private String getGearman() {
		return this.gearman;
	}
}
