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

package org.gearman.impl.server.local;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

import org.gearman.impl.util.ByteArray;
import org.gearman.impl.util.EqualsLock;

class FunctionMap {
	
	private final ConcurrentHashMap<ByteArray, Reference<InnerFunction>> funcMap = new ConcurrentHashMap<ByteArray, Reference<InnerFunction>>();
	private final EqualsLock lock = new EqualsLock();
	
	public final Function getFunction(ByteArray name) {
		Integer key = name.hashCode();
		try {
			lock.lock(key);
			
			final Reference<InnerFunction> ref = funcMap.get(name);
			InnerFunction func;
			
			if(ref==null || (func=ref.get())==null) {
				func = new InnerFunction(name);
				final Reference<InnerFunction> ref2 = new SoftReference<InnerFunction>(func);
				func.ref = ref2;
				
				final Object o = this.funcMap.put(name, ref2);
				assert o==null;
			}
			return func;
		} finally {
			lock.unlock(key);
		}
	}
	
	public final Function getFunctionIfDefined(ByteArray name) {
		final Reference<InnerFunction> ref = funcMap.get(name);
		return ref==null? null: ref.get();
	}
	
	public final void sendStatus(Client client) {
		
		for(Reference<InnerFunction> funcRef : funcMap.values()) {
			InnerFunction func = funcRef.get();
			if(func!=null) 
				client.sendPacket(func.getStatus(), null);
		}
		
		client.sendPacket(StaticPackets.TEXT_DONE, null /*TODO*/);
	}
	
	private final class InnerFunction extends Function {
		private Reference<?> ref;
		
		public InnerFunction(ByteArray name) {
			super(name);
		}
		
		@Override
		protected final void finalize() throws Throwable {
			super.finalize();
			FunctionMap.this.funcMap.remove(super.getName(), ref);
		}
	}
}
