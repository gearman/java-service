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

package org.gearman.impl.util;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Almost Immutable byte array
 * @author isaiah
 */
public final class ByteArray {
	private final byte[] array;
	
	public ByteArray(final byte[] array) {
		this.array = array;
	}
	
	public ByteArray(final String value) {
		this.array = value.getBytes(GearmanUtils.getCharset());
	}
	
	@Override
	public final int hashCode() {
		return Arrays.hashCode(array);
	}
	
	public final byte[] getBytes() {
		return array.clone();
	}
	
	public final byte get(int index) {
		return array[index];
	}
	
	public final String toString(Charset charset) {
		return new String(array, charset);
	}
	
	public final boolean isEmpty() {
		return this.array.length==0;
	}
	
	public final int length() {
		return this.array.length;
	}
	
	@Override
	public final String toString() {
		return new String(array);
	}
	
	@Override
	public final boolean equals(final Object obj) {
		if(obj instanceof ByteArray)
			return this.equals((ByteArray)obj);
		else if(obj instanceof byte[])
			return this.equals((byte[])obj);
		else
			return false;
	}
	
	public final boolean equals(final ByteArray array) {
		return this == array || this.equals(array.array);
	}
	
	public final boolean equals(final byte[] array) {
		return Arrays.equals(this.array, array);
	}
}
