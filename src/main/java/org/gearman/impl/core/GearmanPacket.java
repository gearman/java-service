/*
 * Copyright (C) 2009 by Eric Lambert (Eric.Lambert@sun.com)
 * Copyright (C) 2012 by Isaiah van der Elst (isaiah.v@comcast.net)
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

package org.gearman.impl.core; 

import org.gearman.impl.util.GearmanUtils;

/**
 * A <tt>GearmanPacket</tt> represents an individual message that can either be
 * sent to a gearman job Server or received from a gearman job server.
 * 
 * <pre>
 * A <tt>GearmanPacket</tt> consists of three components:
 * 
 * - The Packet Magic: The component that identifies the message as a gearman
 *                     message and is used to determine if the message is a
 *                     request message (REQ) or a response message (RES).
 * 
 * - The Packet Type:  The component that indentifies operations specified by
 *                     the message.
 * 
 * - The Packet Data:  The payload of the message, the exact structure of which
 *                     depends on the packet type.
 * </pre>
 *
 *@author Eric Lambert
 *@author isaiah.v
 */
public final class GearmanPacket {
	
	public static final GearmanPacket NO_JOB = new GearmanPacket(Magic.RES, Type.NO_JOB);
	public static final GearmanPacket NOOP = new GearmanPacket(Magic.RES, Type.NOOP);
	public static final GearmanPacket PRE_SLEEP = new GearmanPacket(Magic.REQ, Type.PRE_SLEEP);
	public static final GearmanPacket RESET_ABILITIES = new GearmanPacket(Magic.REQ, Type.RESET_ABILITIES);
	public static final GearmanPacket GRAB_JOB = new GearmanPacket(Magic.REQ, Type.GRAB_JOB);
	
	/**
	 * Creates a text based packet for administrative tasks.<br>
	 * <br>
	 * Magic: N/A<br>
	 * Type: TEXT<br>
	 * @param packet
	 * 		The text packet
	 * @return
	 * 		A GearmanPacket representing the given text packet
	 */
	public static final GearmanPacket createTEXT(String packet) {
		final char end = packet.charAt(packet.length()-1);
		if(end!='\n' && end!='\r')
			packet = packet+'\n';
		
		return new GearmanPacket(Magic.REQ, Type.TEXT, packet.getBytes(GearmanUtils.getCharset()));
	}
	
	public static final GearmanPacket createECHO_REQ(final byte[] data) {
		return new GearmanPacket(Magic.REQ, Type.ECHO_REQ, data);
	}
	
	/**
	 * Creates a CAN_DO packet. This is sent to notify the server that the
	 * worker is able to perform the given function. The worker is then put
	 * on a list to be woken up whenever the job server receives a job for
	 * that function.<br>
	 * <br>
	 * Magic: REQ<br>
	 * Type: CAN_DO<br>
	 * @param funcName
	 * 		The function name
	 * @return
	 * 		A CAN_DO GearmanPacket
	 */
	public static final GearmanPacket createCAN_DO(final String funcName) {
		return new GearmanPacket(Magic.REQ, Type.CAN_DO, funcName.getBytes(GearmanUtils.getCharset()));
	}
	
	/**
	 * Creates a CANT_DO packet. This is sent to notify the server that the
	 * worker is no longer able to perform the given function.<br>
	 * <br>
	 * Magic: REQ<br>
	 * Type: CANT_DO<br>
	 * @param funcName
	 * 		The function name
	 * @return
	 * 		A CANT_DO GearmanPacket
	 */
	public static final GearmanPacket createCANT_DO(final String funcName) {
		return new GearmanPacket(Magic.REQ, Type.CANT_DO, funcName.getBytes(GearmanUtils.getCharset()));
	}
	
	/**
	 * Creates a RESET_ABILITIES packet. This is sent to notify the server
	 * that the worker is no longer able to do any functions it previously
	 * registered with CAN_DO or CAN_DO_TIMEOUT.<br>
	 * <br>
	 * Magic: REQ<br>
	 * Type: RESET_ABILITIES
	 * @return
	 * 		A RESET_ABILITIES GearmanPacket
	 */
	public static final GearmanPacket createRESET_ABILITIES() {
		return GearmanPacket.RESET_ABILITIES;
	}
	public static final GearmanPacket createPRE_SLEEP() {
		return GearmanPacket.PRE_SLEEP;
	}
	public static final GearmanPacket createNOOP() {
		return GearmanPacket.NOOP;
	}
	public static final GearmanPacket createSUBMIT_JOB(final String funcName, final byte[] uID, final byte[] data) {
		return new GearmanPacket(Magic.REQ, Type.SUBMIT_JOB, funcName.getBytes(GearmanUtils.getCharset()), uID, data);
	}
	public static final GearmanPacket createSUBMIT_JOB_BG(final String funcName, final byte[] uID, final byte[] data) {
		return new GearmanPacket(Magic.REQ, Type.SUBMIT_JOB_BG, funcName.getBytes(GearmanUtils.getCharset()), uID, data);
	}
	public static final GearmanPacket createSUBMIT_JOB_HIGH(final String funcName, final byte[] uID, final byte[] data) {
		return new GearmanPacket(Magic.REQ, Type.SUBMIT_JOB_HIGH, funcName.getBytes(GearmanUtils.getCharset()), uID, data);
	}
	public static final GearmanPacket createSUBMIT_JOB_HIGH_BG(final String funcName, final byte[] uID, final byte[] data) {
		return new GearmanPacket(Magic.REQ, Type.SUBMIT_JOB_HIGH_BG, funcName.getBytes(GearmanUtils.getCharset()), uID, data);
	}
	public static final GearmanPacket createSUBMIT_JOB_LOW(final String funcName, final byte[] uID, final byte[] data) {
		return new GearmanPacket(Magic.REQ, Type.SUBMIT_JOB_LOW, funcName.getBytes(GearmanUtils.getCharset()), uID, data);
	}
	public static final GearmanPacket createSUBMIT_JOB_LOW_BG(final String funcName, final byte[] uID, final byte[] data) {
		return new GearmanPacket(Magic.REQ, Type.SUBMIT_JOB_LOW_BG, funcName.getBytes(GearmanUtils.getCharset()), uID, data);
	}
	public static final GearmanPacket createGRAB_JOB() {
		return GearmanPacket.GRAB_JOB;
	}
	public static final GearmanPacket createWORK_COMPLETE(final Magic magic, final byte[] jobHandle, final byte[] data) {
		return new GearmanPacket(magic, Type.WORK_COMPLETE, jobHandle, data);
	}
	public static final GearmanPacket createWORK_FAIL(final Magic magic, final byte[] jobHandle) {
		return new GearmanPacket(magic, Type.WORK_FAIL, jobHandle);
	}
	public static final GearmanPacket createWORK_DATA(final Magic magic, final byte[] jobHandle, final byte[] data) {
		return new GearmanPacket(magic, Type.WORK_DATA, jobHandle, data);
	}
	public static final GearmanPacket createWORK_WARNING(final Magic magic, final byte[] jobHandle, final byte[] data) {
		return new GearmanPacket(magic, Type.WORK_WARNING, jobHandle, data);
	}
	public static final GearmanPacket createWORK_STATUS(final Magic magic, final byte[] jobHandle, final long numerator, final long denominator) {
		final byte[] num = Long.toString(numerator).getBytes(GearmanUtils.getCharset());
		final byte[] den = Long.toString(denominator).getBytes(GearmanUtils.getCharset());
		
		return new GearmanPacket(magic, Type.WORK_STATUS, jobHandle, num, den);
	}
	public static final GearmanPacket createWORK_EXCEPTION(final Magic magic, final byte[] jobHandle, final byte[] data) {
		return new GearmanPacket(magic, Type.WORK_EXCEPTION, jobHandle, data);
	}
	public static final GearmanPacket createSET_CLIENT_ID(String id) {
		return new GearmanPacket(Magic.REQ, Type.SET_CLIENT_ID, id.getBytes(GearmanUtils.getCharset()));
	}
	public static final GearmanPacket createCAN_DO_TIMEOUT(final String funcName, final long timeout) {
		// TODO not sure if this is right... For the timeout value, is the server expecting
		// a binary number or a string representation???
		return new GearmanPacket(Magic.REQ, Type.CAN_DO_TIMEOUT, funcName.getBytes(GearmanUtils.getCharset()), Long.toString(timeout).getBytes(GearmanUtils.getCharset()));
	}
	
	public static final GearmanPacket createJOB_ASSIGN(final byte[] jobHandle, final String funcName, final byte[] data) {
		return new GearmanPacket(Magic.RES, Type.JOB_ASSIGN, jobHandle, funcName.getBytes(GearmanUtils.getCharset()), data);
	}
	
	public static final GearmanPacket createJOB_ASSIGN_UNIQ(final byte[] jobHandle, final String funcName, final byte[] uniqueID, final byte[] data) {
		return new GearmanPacket(Magic.RES, Type.JOB_ASSIGN_UNIQ, jobHandle, funcName.getBytes(GearmanUtils.getCharset()), uniqueID, data);
	}
	
	public static final GearmanPacket createGET_STATUS(final byte[] jobHandle) {
		return new GearmanPacket(Magic.REQ, Type.GET_STATUS, jobHandle);
	}
	
	
	
	private static final int HEADER_SIZE = 12;
	
	private Magic magic;
    private Type type;
    private byte[][] arguments;
    
    public GearmanPacket(final Magic magic, final Type type , byte[]...arguments) {
    	this.magic = magic;
    	this.type = type;

    	this.arguments = arguments==null? new byte[0][]: arguments;
    	
    	for(int i=0; i<this.arguments.length; i++) {
    		if(this.arguments[i]==null) this.arguments[i] = new byte[0];
    	}
    	
    	if(this.arguments.length!=type.getArgumentCount()) {
    		throw new IllegalArgumentException("Packet type " + type + " requires " + type.getArgumentCount() + " argument(s). Aquired " + this.arguments.length + " argument(s)");
    	}
    	for(int i=0; i<this.arguments.length-1; i++) {
    		for(byte b : this.arguments[i]) {
    			if(b==0)  throw new IllegalArgumentException("Argument " + i + "contains a null value. Only the last argument can contain null values");
    		}
    	}
    }
    
    public final Type getPacketType() {
    	return this.type;
    }
    
    public final void setPacketType(final Type type) {
    	this.type = type;
    }
    
    /**
     * Retrieves the magic type for this packet.
     * @return The {@link GearmanPacketMagic} for this packet.
     */
    public final Magic getMagic() {
    	return this.magic;
    }
    
    public final void setMagic(final Magic magic) {
    	this.magic = magic;
    }
    
    public final byte[] getArgumentData(final Argument arg) {
    	final int pos = GearmanPacket.getArgumentNumber(this.type, arg);
    	return pos==-1? null: this.arguments[pos];
    }
    
    public final byte[] getArgumentData(final int arg) {
    	return this.arguments[arg];
    }
    

    /**
     * Retrieves the Packet as a series of bytes. Typically called when about
     * to send the packet over a {@link  GearmanJobServerConnection}.
     *
     * @return a byte array representing the packet.
     */
    public byte[] toBytes() {
    	
    	if(this.getPacketType().equals(Type.TEXT)) {
    		return this.getArgumentData(0);
    	}
    	
    	// Find the packet size
    	int packet_size = HEADER_SIZE;
    	for(byte[] arg : this.arguments) {
    		packet_size += arg.length;
    	}
    	if(this.arguments.length>0) packet_size += this.arguments.length-1;
    	
    	// Allocate a byte[] to hold the entire packet
    	final byte[] packet = new byte[packet_size];
    	
    	// Set the magic code
    	final int magic = this.magic.getMagicCode();
    	packet[0] = (byte)(((0xFF000000)&magic)>>24);
    	packet[1] = (byte)(((0x00FF0000)&magic)>>16);
    	packet[2] = (byte)(((0x0000FF00)&magic)>>8);
    	packet[3] = (byte)(((0x000000FF)&magic));
    	
    	// Set the type
    	final int type = this.type.getTypeValue();
    	packet[4] = (byte)(((0xFF000000)&type)>>24);
    	packet[5] = (byte)(((0x00FF0000)&type)>>16);
    	packet[6] = (byte)(((0x0000FF00)&type)>>8);
    	packet[7] = (byte)(((0x000000FF)&type));
    	
    	// Set the size
    	final int size = packet_size-12;
    	packet[8] = (byte)(((0xFF000000)&size)>>24);
    	packet[9] = (byte)(((0x00FF0000)&size)>>16);
    	packet[10] = (byte)(((0x0000FF00)&size)>>8);
    	packet[11] = (byte)(((0x000000FF)&size));
    	
    	// Inject the arguments
    	int pos = HEADER_SIZE;
    	for(byte[] arg : this.arguments) {
    		System.arraycopy(arg, 0, packet, pos, arg.length);
    		pos += arg.length+1;
    	}
    	
    	return packet;
    }

    /**
     * The data or payload of a packet can contain different set of components
     * depending on the type of packet. Clients of the packet class may want to
     * extract these components from the message payload. This method provides
     * a means for clients to extract the component without requiring knowledge
     * of the format of the payload.
     *
     * @param component The name of the component to be extracted.
     *
     * @return the value of the specified component. Should return an empty
     * array if the component is not contained the packet.
     */


    /**
     * Determine if the packet represents a request message that requires a
     * responses from the server. There are certain request messages that
     * logically require a response, for example the GRAB_JOB message should
     * result in a JOB_ASSIGN or NO_JOB reply from the server. While there are
     * other messages that do not, such as CAN_DO.
     *
     * @return true if this packet represents a request message that requires a
     * response, else returns false.
     */
    public boolean requiresResponse() {
    	//TODO
    	return false;
    }
    
    public static final int getArgumentNumber(Type type, Argument argument) {
    	switch(type) {
    	case TEXT:
    		switch(argument) {
    		case TEXT: return 0;
    		default: return -1;
    		}
    	case CAN_DO:
    	case CANT_DO:
    		switch(argument) {
    		case FUNCTION_NAME: return 0;
    		default: return -1;
    		}
    	case RESET_ABILITIES:
    	case PRE_SLEEP:
    	case UNUSED:
    	case NOOP:
    	case GRAB_JOB:
    	case NO_JOB:
    	case ALL_YOURS:
    	case GRAB_JOB_UNIQ:
    	default:
    		return -1;
    	case SUBMIT_JOB:
    	case SUBMIT_JOB_BG:
    	case SUBMIT_JOB_HIGH:
    	case SUBMIT_JOB_HIGH_BG:
    	case SUBMIT_JOB_LOW:
    	case SUBMIT_JOB_LOW_BG:
    		switch(argument) {
    		case FUNCTION_NAME: return 0;
    		case UNIQUE_ID: return 1;
    		case DATA: return 2;
    		default: return -1;
    		}
    	case JOB_CREATED:
    	case WORK_FAIL:
    	case GET_STATUS:
    		switch(argument) {
    		case JOB_HANDLE: return 0;
    		default: return -1;
    		}
    	case JOB_ASSIGN:
    		switch(argument) {
    		case JOB_HANDLE: return 0;
    		case FUNCTION_NAME: return 1;
    		case DATA: return 2;
    		default: return -1;
    		}
    	case WORK_STATUS:
    		switch(argument) {
    		case JOB_HANDLE: return 0;
    		case NUMERATOR: return 1;
    		case DENOMINATOR: return 2;
    		default: return -1;
    		}
    	case WORK_COMPLETE:
    	case WORK_EXCEPTION:
    	case WORK_DATA:
    	case WORK_WARNING:
    		switch(argument) {
    		case JOB_HANDLE: return 0;
    		case DATA: return 1;
    		default: return -1;
    		}
    	case ECHO_REQ:
    	case ECHO_RES:
    		switch(argument) {
    		case DATA: return 0;
    		default: return -1;
    		}
    	case ERROR:
    		switch(argument) {
    		case ERROR_CODE: return 0;
    		case ERROR_TEXT: return 1;
    		default: return -1;
    		}
    	case STATUS_RES:
    		switch(argument) {
    		case JOB_HANDLE: return 0;
    		case KNOWN_STATUS: return 1;
    		case RUNNING_STATUS: return 2;
    		case NUMERATOR: return 3;
    		case DENOMINATOR: return 4;
    		default: return -1;
    		}
    	case SET_CLIENT_ID:
    		switch(argument) {
    		case CLIENT_ID: return 0;
    		default: return -1;
    		}
    	case CAN_DO_TIMEOUT:
    		switch(argument) {
    		case FUNCTION_NAME: return 0;
    		case TIME_OUT: return 1;
    		default: return -1;
    		}
    	case OPTION_REQ:
    	case OPTION_RES:
    		switch(argument) {
    		case OPTION: return 0;
    		default: return -1;
    		}
    	case JOB_ASSIGN_UNIQ:
    		switch(argument) {
    		case JOB_HANDLE: return 0;
    		case FUNCTION_NAME: return 1;
    		case UNIQUE_ID: return 2;
    		case DATA: return 3;
    		default: return -1;
    		}
    	case SUBMIT_JOB_SCHED:
    		switch(argument) {
    		case FUNCTION_NAME: return 0;
    		case UNIQUE_ID: return 1;
    		case MINUTE: return 2;
    		case HOUR: return 3;
    		case DAY_OF_MONTH: return 4;
    		case MONTH: return 5;
    		case DAY_OF_WEEK: return 6;
    		case DATA: return 7;
    		default: return -1;
    		}
    	case SUBMIT_JOB_EPOCH:
    		switch(argument) {
    		case FUNCTION_NAME: return 0;
    		case UNIQUE_ID: return 1;
    		case EPOCH: return 2;
    		case DATA: return 3;
    		default: return -1;
    		}
    	}
    }
    
    public static enum Argument {
    	TEXT, FUNCTION_NAME, UNIQUE_ID, MINUTE, HOUR, DAY_OF_MONTH, MONTH, DAY_OF_WEEK,
        EPOCH, JOB_HANDLE, OPTION, KNOWN_STATUS, RUNNING_STATUS, NUMERATOR, DENOMINATOR,
        TIME_OUT, DATA, ERROR_CODE, ERROR_TEXT, CLIENT_ID
    }
    
    /**
     * An enumerator representing a gearman packet's magic code. The magic code is the
     * first four bytes in the gearman packet's header. The value is "\0REQ" for request
     * or "\0RES" for response.
     * 
     * @see <a href=http://gearman.org/?id=protocol>gearman protocol</a>
     * @author isaiah.v
     */
    public static enum Magic {
    	/**
    	 * A request packet is sent to the server to request a service.<br>
    	 * Magic code = "\0REQ" = 5391697
    	 */
    	REQ(5391697),
    	
    	/**
    	 * A response packet is sent from the server to a client or worker as a response
    	 * from handling a request.<br>
    	 * Magic code = "\0RES" = 5391699
    	 */
    	RES(5391699);
    	
    	/** The magic number */
    	private final int code;
    	
    	private Magic(int code) {
    		this.code = code;
    	}
    	
    	/**
    	 * Returns the magic code
    	 * @return
    	 * 		The magic code.
    	 */
    	public final int getMagicCode() {
    		return code;
    	}
    	
    	/**
    	 * Gets the <code>Magic</code> enumerator based on the magic code 
    	 * @param code
    	 * 		The magic code.
    	 * @return
    	 * 		The Magic object. If the code is invalid, null is returned
    	 */
    	public static final Magic fromMagicCode(final int code) {
    		switch(code) {
    		case 5391697:
    			return REQ;
    		case 5391699:
    			return RES;
    		default:
    			return null;
    		}
    	}
    }
    public static enum Type{
    	TEXT(0,1), CAN_DO(1,1), CANT_DO(2,1), RESET_ABILITIES(3,0), PRE_SLEEP(4,0), UNUSED(5,0),
    	NOOP(6,0), SUBMIT_JOB(7,3), JOB_CREATED(8,1), GRAB_JOB(9,0), NO_JOB(10,0), JOB_ASSIGN(11,3),
    	WORK_STATUS(12, 3), WORK_COMPLETE(13,2), WORK_FAIL(14,1), GET_STATUS(15,1), ECHO_REQ(16,1),
    	ECHO_RES(17,1), SUBMIT_JOB_BG(18,3), ERROR(19, 2), STATUS_RES(20, 5), SUBMIT_JOB_HIGH(21,3),
    	SET_CLIENT_ID(22,1), CAN_DO_TIMEOUT(23,2), ALL_YOURS(24,0), WORK_EXCEPTION(25,2),
    	OPTION_REQ(26,1), OPTION_RES(27,1), WORK_DATA(28,2), WORK_WARNING(29, 2), GRAB_JOB_UNIQ(30,0),
    	JOB_ASSIGN_UNIQ(31,4), SUBMIT_JOB_HIGH_BG(32,3), SUBMIT_JOB_LOW(33,3), SUBMIT_JOB_LOW_BG(34,3),
    	SUBMIT_JOB_SCHED(35,8), SUBMIT_JOB_EPOCH(36,4);
    	
    	private final int type;
    	private final int args;
    	private Type(final int type, final int args) {
    		this.type = type;
    		this.args = args;
    	}
    	
    	public final int getTypeValue() {
    		return type;
    	}
    	public final int getArgumentCount() {
    		return args;
    	}
    	
    	public static final Type fromTypeValue(final int value) {
    		switch(value) {
    		case(0): return TEXT; 				case(1): return CAN_DO;			case(2): return CANT_DO;
    		case(3): return RESET_ABILITIES; 	case(4): return PRE_SLEEP;			case(5): return UNUSED;
    		case(6): return NOOP;				case(7): return SUBMIT_JOB;		case(8): return JOB_CREATED;
    		case(9): return GRAB_JOB;			case(10): return NO_JOB; 			case(11): return JOB_ASSIGN;
    		case(12): return WORK_STATUS;		case(13): return WORK_COMPLETE;	case(14): return WORK_FAIL;
    		case(15): return GET_STATUS;		case(16): return ECHO_REQ;			case(17): return ECHO_RES;
    		case(18): return SUBMIT_JOB_BG;	case(19): return ERROR;			case(20): return STATUS_RES;
    		case(21): return SUBMIT_JOB_HIGH;	case(22): return SET_CLIENT_ID;	case(23): return CAN_DO_TIMEOUT;
    		case(24): return ALL_YOURS;		case(25): return WORK_EXCEPTION;	case(26): return OPTION_REQ;
    		case(27): return OPTION_RES;		case(28): return WORK_DATA;		case(29): return WORK_WARNING;
    		case(30): return GRAB_JOB_UNIQ;	case(31): return JOB_ASSIGN_UNIQ;	case(32): return SUBMIT_JOB_HIGH_BG;
    		case(33): return SUBMIT_JOB_LOW;	case(34): return SUBMIT_JOB_LOW_BG;case(35): return SUBMIT_JOB_SCHED; 
    		case(36): return SUBMIT_JOB_EPOCH;
    		default:
    			return null;
    		}
    	}
    }
	public int getArgumentCount() {
		return this.arguments.length;
	}
}
