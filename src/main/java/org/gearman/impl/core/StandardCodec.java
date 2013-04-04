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

package org.gearman.impl.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.gearman.impl.core.GearmanPacket.Magic;
import org.gearman.impl.core.GearmanPacket.Type;
import org.gearman.impl.util.GearmanUtils;

import static org.gearman.context.GearmanContext.LOGGER;

public final class StandardCodec implements GearmanCodec<Integer>{
	
	private static final int FORMAT 	= 0;
	private static final int HEADER		= 1;
	private static final int BODY		= 2;
	private static final int TEXT		= 3;
	
//	private static final int MAGIC_POS		= 0;
//	private static final int TYPE_POS		= 4;
	private static final int SIZE_POS		= 8;
	private static final int HEADER_SIZE	= 12;
	
	
	@Override
	public final ByteBuffer createByteBuffer() {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
		buffer.limit(1);
		return buffer;
	}

	@Override
	public final void decode(final GearmanCodecChannel<Integer> channel, final int byteCount) {
		
		switch(channel.getCodecAttachement()) {
		case FORMAT:
			format(channel);
			return;
		case HEADER:
			header(channel);
			return;
		case BODY:
			body(channel);
			return;
		case TEXT:
			text(channel);
			return;
		default:
			assert false;
		}		
	}

	@Override
	public final byte[] encode(final GearmanPacket packet) {
		return packet.toBytes();
	}

	@Override
	public final void init(final GearmanCodecChannel<Integer> channel) {
		channel.setCodecAttachement(FORMAT);
	}
	
	private final void format(final GearmanCodecChannel<Integer> channel) {
		final ByteBuffer buffer = channel.getBuffer();
		if(buffer.hasRemaining()) return;
		
		if(buffer.get(0)==0) {
			buffer.limit(HEADER_SIZE);
			channel.setCodecAttachement(HEADER);
		} else {
			channel.setCodecAttachement(TEXT);
			text(channel);
		}
	}
	
	private final void header(final GearmanCodecChannel<Integer> channel) {
		ByteBuffer buffer = channel.getBuffer();
		if(buffer.hasRemaining()) return;
		
		final int size = buffer.getInt(SIZE_POS);
		
		if(size==0) {
			buffer.flip();
			
			final Magic magic = Magic.fromMagicCode(buffer.getInt());
			final Type type = Type.fromTypeValue(buffer.getInt());
			
			buffer.clear();
			buffer.limit(1);
			channel.setCodecAttachement(FORMAT);
			
			final GearmanPacket packet = new GearmanPacket(magic, type);
			
			channel.onDecode(packet);
		} else {
			final int headerAndSize = HEADER_SIZE+size;
			
			if(headerAndSize>buffer.capacity()) {
				// Grow Buffer
				ByteBuffer newbuf = ByteBuffer.allocateDirect(headerAndSize);
				
				buffer.clear();
				buffer.limit(HEADER_SIZE);
				newbuf.put(buffer);
			
				channel.setBuffer(newbuf);
				buffer = newbuf;
			}
			
			buffer.limit(headerAndSize);
			channel.setCodecAttachement(BODY);
		}
	}
	
	private final void body(final GearmanCodecChannel<Integer> channel) {
		try {
			final ByteBuffer buffer = channel.getBuffer();
			if(buffer.hasRemaining()) return;
			
			final Magic magic;
			final Type type;
			final byte[] body;
			
			try {
				buffer.flip();
				
				magic = Magic.fromMagicCode(buffer.getInt());
				type = Type.fromTypeValue(buffer.getInt());
				final int size = buffer.getInt();
				
				body = new byte[size];
				buffer.get(body);
				
			} finally {
				buffer.clear();
				buffer.limit(1);
				channel.setCodecAttachement(FORMAT);
			}
			
			final GearmanPacket packet = new GearmanPacket(magic, type, parseArguments(body,type));
			
			channel.onDecode(packet);
		} catch (Exception e) {
			LOGGER.warn("Unexpected Exception", e);
		}
	}
	
	private final void text(final GearmanCodecChannel<Integer> channel) {
		try {
			final ByteBuffer buffer = channel.getBuffer();
			if(buffer.hasRemaining()) return;
			
			final char newValue = (char) buffer.get(buffer.position()-1);
			
			if(newValue!='\n' && newValue!='\r') {
				buffer.limit(buffer.limit()+1);
				return;
			}
			
			try {
				final byte[] strBytes = new byte[buffer.position()];
				
				buffer.flip();
				buffer.get(strBytes);
				
				final String str = new String(strBytes, GearmanUtils.getCharset());
				final GearmanPacket packet = GearmanPacket.createTEXT(str);
				
				channel.onDecode(packet);
			} finally {
				buffer.clear();
				buffer.limit(1);
				channel.setCodecAttachement(FORMAT);
			}
		} catch (Throwable th) {
			LOGGER.warn("Unexpected Exception", th);
		}
	}
	
	private static final byte[][] parseArguments(final byte[] body, final Type type) {
		final int argCount = type.getArgumentCount();
		if(argCount==0) return null;
		
		final byte[][] value = new byte[argCount][];
		final ByteArrayOutputStream buff = new ByteArrayOutputStream();
		
		byte b;
		int pos=0, arg=0;
		for(; pos<body.length && arg<argCount-1 ; pos++) {
			b = body[pos];
			if(b!=0) {
				buff.write(b);
			} else {
				value[arg] = buff.toByteArray();
				buff.reset();
				arg++;
			}
		}
		try {
			buff.close();
		} catch (IOException e) {
			LOGGER.warn("Failed to close ByteArrayOutputStream", e);
		}
		
		if(arg==argCount-1) {
			byte[] bArray = new byte[body.length-pos];
			System.arraycopy(body, pos, bArray, 0, bArray.length);
			value[arg] = bArray;
			
			return value;
		} else {
			return null;
		}
	}
}
