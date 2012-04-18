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
