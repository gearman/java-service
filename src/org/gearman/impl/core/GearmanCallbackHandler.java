/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.core;

public interface GearmanCallbackHandler<D,R extends GearmanCallbackResult> {
	public void onComplete(D data, R result);
}
