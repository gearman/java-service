/*
 * Copyright (C) 2010 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.dbg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanConnectionHandler;
import org.gearman.core.GearmanConnectionManager;
import org.gearman.core.GearmanConstants;
import org.gearman.core.GearmanPacket;
import org.gearman.core.GearmanConnection.SendCallbackResult;
import org.gearman.core.GearmanConnectionManager.ConnectCallbackResult;

import com.googlecode.jgasp.ArgumentParser;

/**
 * The GearmanShell will allow users to interact with the server using command
 * line input. A command consists of a message type followed by the arguments.
 * 
 * Command Example:
 * 		gearman-client> SUBMIT_JOB reverse 1 "reverse this string"
 * 
 * This will cause a SUBMIT_JOB packet to be sent to the server with the given
 * three arguments:
 * 
 * 	"reverse"				- function name
 * 	"1"						- unique ID
 * 	"reverse this string"	- data
 * 
 * Text based messages are sent using the TEXT command followed by the arguments
 * 
 * Command Example:
 * 		gearman-client> TEXT maxqueue reverse 10
 * 
 * @author isaiah
 *
 */
public class GearmanShell {
	
	final GearmanConnectionManager gcm = new GearmanConnectionManager();
	
	public static final void main(final String[] argv) {
		final ArgumentController argCon = new ArgumentController(argv);
		final InetSocketAddress adrs = new InetSocketAddress(argCon.getHost(), argCon.getPort());
		
		try {
			new GearmanShell(adrs);
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	private GearmanShell(InetSocketAddress adrs) throws IOException {
		this.gcm.createGearmanConnection(adrs, new Handler() ,new GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult>(){
			
			@Override
			public void onComplete(InetSocketAddress data, ConnectCallbackResult result) {
				if(result.isSuccessful())return;
				
				System.out.println(result);
				GearmanShell.this.gcm.shutdown();			}
		});
	}
	
	private final static class ArgumentController {
		private static final String HELP =
			"\nGearmanShell\n"+
			"usage: java [VM OPTIONS] GearmanShell [OPTIONS]\n\n" +
			"Main Options:\n" +
			" -h, --host=HOST            The host address of the gearman server" +
			" -p, --port=PORT            Port the server should listen on\n" +
			" -?, --help                 Print this help menu and exit\n";
		
		private String host = "localhost";
		private int port = GearmanConstants.DEFAULT_PORT;
		
		public ArgumentController (final String[] args) {
			
			// Make new argument parser
			ArgumentParser ap = new ArgumentParser();
			
			// register options with argument parser
			ap.addOption('?', "help", false);
			ap.addOption('h', "host", true);
			ap.addOption('p', "port", true);
			
			// parse arguments into options
			ArrayList<String> arguments = ap.parse(args);
			
			// if arguments == null, parsing failed.  If arguments.size > 0, then some
			// unknown arguments were acquired
			if(arguments==null || arguments.size()>0)
				printHelp();
			
			// Iterates over the set of flagged options
			for(ArgumentParser.Option o : ap) {
				switch(o.getShortName()) {
				case '?':		// --help
					printHelp();
					break;
				case 'h':		// --host
					setHost(o.getValue());
					break;
				case 'p':		// --port
					setPort(o.getValue());
					break;
				default:
					printHelp();
				}
			}
			
			ap.close();
		}
		
		private static final void printHelp() {
			System.out.println(HELP);
			System.exit(1);
		}
		
		/**
		 * Set the host
		 * @param host
		 * 		The host name
		 */
		private final void setHost(final String host) {
			this.host = host;
		}
		
		/**
		 * The the port number
		 * @param port
		 * 		The port number as a String
		 */
		private final void setPort(final String port) {
			try { this.port = Integer.parseInt(port); }
			catch(NumberFormatException nfe) { printHelp(); }
		}
		
		/** 
		 * Returns the port number specified by the user. If the user
		 * did not specify a port, the default is returned.
		 * @return
		 * 		The port number
		 */
		public final int getPort(){
			return this.port;
		}
		
		/**
		 * Returns the host name specified by the user.  If the user
		 * did not specify a host, the defualt is returned
		 * @return
		 * 		The host name
		 */
		public final String getHost() {
			return this.host;
		}
	}
	private final class Handler implements GearmanConnectionHandler<Object> {

		private final LinkedList<String> args	= new LinkedList<String>();
		private final Scanner in				= new Scanner(System.in);
		private final Logger log				= Logger.getLogger("GearmanShell");
		private final Object printLock			= new Object();
		
		private org.gearman.core.GearmanConnection<?> client;
		
		@Override
		public void onAccept(org.gearman.core.GearmanConnection<Object> conn) {
			this.client = conn;
			
			Runnable inputLoop = new Runnable() {

				@Override
				public void run() {
					try {
						
						// Start the shell loop
						String command = "";
						while(!command.equals("exit"))
						{
							//Get input and save it in "args"
							getParsedInput();
							
							//Pull the command out of args
							command = args.poll();
							
							//If the command is "exit", break the loop
							if(command.equals("exit"))
								break;
							
							else //execute the command
								executeCommand(command, args);
						}
					} finally {		
						try {
							Handler.this.client.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}				
			};
			new Thread(inputLoop).start();
		}

		@Override
		public void onDisconnect(org.gearman.core.GearmanConnection<Object> conn) {
			log.log(Level.INFO, "Disconnected");
			GearmanShell.this.gcm.shutdown();
			System.exit(0);
		}

		@Override
		public void onPacketReceived(org.gearman.core.GearmanPacket packet,
				org.gearman.core.GearmanConnection<Object> conn) {
			
			log.log(Level.INFO, "Received Packet:\n"+this.packetToString(packet));
			synchronized(this.printLock) {
				System.out.print("gearman-client> ");
			}
		}
		
		/**
		 * Executes the given command
		 * @param command	the command
		 * @param args		the command's arguments
		 * @throws IOException 
		 * @throws UnsupportedEncodingException 
		 */
		private final void executeCommand(String command, LinkedList<String> args)
		{
			final int size = args.size();
			org.gearman.core.GearmanPacket packet;
			if(command.equals("TEXT")) {
				if(size==0)
					log.log(Level.WARNING, "No packet data to send");
				StringBuffer sb = new StringBuffer();
				for(int i=0; i<size-1; i++)
				{
					sb.append(args.poll());
					sb.append(' ');
				}
				sb.append(args.poll());
				sb.append('\n');
				
				packet = org.gearman.core.GearmanPacket.createTEXT(sb.toString());
			}
			else
			{
				final org.gearman.core.GearmanPacket.Type type;
				try{
					type = org.gearman.core.GearmanPacket.Type.valueOf(command);
				} catch (IllegalArgumentException iae) {
					log.log(Level.WARNING, "Unknown packet type");
					return;
				}
				
				byte[][] data = null;
				if(size>0)	{
					data = new byte[type.getArgumentCount()][]; 
					
					int i=0;
					
					//Write all but the last argument to the data[][]
					for(; i<type.getArgumentCount()-1 && i<size; i++) {
						data[i] = args.poll().getBytes(GearmanConstants.UTF_8);
					}

					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						for(; i<size; i++) {
							baos.write(args.poll().getBytes(GearmanConstants.UTF_8));
							if(i<size-1)
								baos.write(0);
						}
						data[data.length-1] = baos.toString().getBytes(GearmanConstants.UTF_8);
						baos.close();
					} catch (Exception e) {
						// Should not end up here, the baos does not throw
						// IOExceptions on writes
						assert false;
						log.log(Level.SEVERE, "failed to contruct packet");
						return;
					}
				}
				
				// Construct the packet
				try {
					packet = new org.gearman.core.GearmanPacket(org.gearman.core.GearmanPacket.Magic.REQ ,type , data);
				} catch (IllegalArgumentException e) {
					// On failure, log message
					log.log(Level.WARNING, "Failed to construct packet: " + e.getMessage());
					return;
				}
			}
			
			
			
			log.log(Level.INFO, "Sending Packet:\n"+this.packetToString(packet));
			
			this.client.sendPacket(packet, new GearmanCallbackHandler<GearmanPacket, SendCallbackResult>(){

				@Override
				public void onComplete(GearmanPacket data, SendCallbackResult result) {
					if(!result.isSuccessful())
						log.log(Level.WARNING, "Sending Packet Failed: " + result);
				}
				
			});
		}
		
		/**
		 * Parses the user input into a list of strings.
		 * 
		 * Quotations, single and double, are grouped such that it allows for any character,
		 * including spaces and newlines.
		 * 
		 * Escape Characters:
		 * 		Single Quotation:
		 * 			\'	=>	'
		 * 			\\	=>	\
		 * 		Double Quotation:
		 * 			\"	=>	"
		 * 			\\	=>	\
		 * 		No Quotation
		 * 			\?	=>	?
		 * 			? is a wild card representing any printable character, including spaces
		 * 
		 * The end result is a command prompt that should behave like that of a standard 
		 * Bash shell.
		 *  
		 */
		private final void getParsedInput()
		{	
			//The number of double quotation marks
			int doubleQuotes = 0;
			//The number of single quotation marks
			int singleQuotes = 0;
			//A StringBuffer representing an argument
			StringBuffer arg = new StringBuffer();
			
			//true if more user input is needed.
			boolean isInputNeeded = true;
			while(isInputNeeded)
			{
				//Get a line of user input
				String input = getInput();
		
				int size = input.length();
				
				//For each char in the input
				for(int i=0; i<size; i++)
				{
					char ch = input.charAt(i);
					switch(ch)
					{
					case('\\'):
						char nextChar = input.charAt(i+1);
						
						if((doubleQuotes&1)==1) {
							switch(nextChar) {
							case('"'):
							case('\\'):
								arg.append(nextChar);
								i++;
								break;
							default:
								arg.append(ch);
							}  // exit switch
						} // exit if
						else if((singleQuotes&1)==1) {
							switch(nextChar) {
							case('\''):
							case('\\'):
								arg.append(nextChar);
								i++;
								break;
							default:
								arg.append(ch);
							} // exit switch
						} // exit else if
						else {
							arg.append(nextChar);
							i++;
						}
						break;
					case('"'):
						if((singleQuotes&1)==1)
							arg.append(ch);
						else
							doubleQuotes++;
						break;
					case('\''):
						if((doubleQuotes&1)==1)
							arg.append(ch);
						else
							singleQuotes++;
						break;
					case(' '):
						if((doubleQuotes&1)==1 || (singleQuotes&1)==1)
							arg.append(' ');
						else
						{
							if(arg.length()>0)
							{
								args.add(arg.toString());
								arg = new StringBuffer();
							}
						}
						break;
					default:
						arg.append(ch);
					}
				}
				isInputNeeded = (doubleQuotes&1)==1 || (singleQuotes&1)==1;
			}
			
			if(arg.length()>0)
				args.add(arg.toString());
		}
		/**
		 * Gets the user input with the given Scanner.
		 * 
		 * An input line ending with a backslash ('\') specifies that the user want to 
		 * continue inputting data on the next line. 
		 * 
		 * @param in
		 * 		The Scanner used to get user input
		 * @return
		 * 		A String of the user input
		 */
		private final String getInput() {
			StringBuffer input = new StringBuffer();
			
			int size = 0;
			do {	
				if(size!=0) {
					//If the size > 0, then the last character is a backslash, this removed it.
					synchronized(printLock){
					System.out.print("> ");			//Print prompt
					}
					input = input.deleteCharAt(size-1);
				}
				else
					synchronized(printLock){
					System.out.print("gearman-client> ");			//Print prompt
					}
				
				//Append the user's input to the end of the input acquired thus far.
				input = input.append(in.nextLine());
				size = input.length();	//set size
			} while(size==0 || input.charAt(size-1)=='\\');
			
			/*
			 * If the size == 0, then the user has yet to enter anything
			 * 
			 * If the user entered a backslash as the last character on the line,
			 * then input is continued on the next line.
			 */
			
			return input.toString();	//return input as a String
		}
		private final String packetToString(final org.gearman.core.GearmanPacket packet) {
			StringBuilder sb = new StringBuilder();
			sb.append("\tMagic: ");
			sb.append(packet.getMagic());
			sb.append('\n');
			
			sb.append("\tType: ");
			sb.append(packet.getPacketType());
			
			if(packet.getPacketType().getArgumentCount()>0)
				sb.append('\n');
			
			for(int i=0; i<packet.getPacketType().getArgumentCount(); i++) {
				byte[] data = packet.getArgumentData(i);
				sb.append("\tArgument ");
				sb.append(i);
				sb.append(": ");
				sb.append(new String(data, GearmanConstants.UTF_8));
				sb.append(" { ");
				for(byte b : data) {
					sb.append(b);
					sb.append(' ');
				}
				sb.append("}");
				if(i<packet.getPacketType().getArgumentCount()-1) {
					sb.append('\n');
				}
			}
			
			return sb.toString();
		}
	}
}
