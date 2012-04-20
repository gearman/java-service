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

/**
 * ValuePair objects contain two genaric object values. 
 * 
 * @author isaiah
 *
 * @param <T1>
 * 		The type for the first value
 * @param <T2>
 * 		The type for the second value
 */
public class ValuePair <T1, T2> {
	
	/** The first value */
	public final T1 value1;
	
	/** The second value */
	public final T2 value2;
	
	/**
	 * Constructor
	 * @param value1
	 * 		The first value
	 * @param value2
	 * 		The second value
	 */
	public ValuePair(T1 value1, T2 value2) {
		this.value1 = value1;
		this.value2 = value2;
	}
	
	/**
	 * Returns the first value
	 * @return
	 * 		The first value
	 */
	public T1 getValue1() {
		return value1;
	}
	
	/**
	 * Returns the second value
	 * @return
	 * 		The second value
	 */
	public T2 getValue2() {
		return value2;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof ValuePair))
			return false;
		
		ValuePair<?,?> that = (ValuePair<?,?>)o;
		return this.value1.equals(that.value1) && this.value2.equals(that.value2);
	}
	
	@Override
	public int hashCode() {
		return this.value1.hashCode() + this.value2.hashCode();
	}
}
