/*
 * Copyright (C) 2010 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A {@link java.util.Set} implementation using a {@link java.util.concurrent.ConcurrentHashMap} as the backing
 * map
 * 
 * @author isaiah
 *
 */
public final class ConcurrentHashSet<X> extends AbstractSet<X> {

	private static final long serialVersionUID = 1L;
	
	private transient final ConcurrentHashMap<X,ConcurrentHashSet<X>> backingMap = new ConcurrentHashMap<X,ConcurrentHashSet<X>>();

	@Override
	public Iterator<X> iterator() {
		return backingMap.keySet().iterator();
	}

	@Override
	public int size() {
		return backingMap.size();
	}
	
	@Override
	public final boolean add(X o) {
		return backingMap.putIfAbsent(o, this)==null;
	}
	
	@Override
	public void clear() {
		backingMap.clear();
	}
	
	@Override
	public boolean contains(Object o) {
		return backingMap.containsKey(o);
	}
	
	@Override
	public boolean isEmpty() {
		return backingMap.isEmpty();
	}
	
	@Override
	public boolean remove(Object o) {
		return backingMap.remove(o) != null;
	}
}
