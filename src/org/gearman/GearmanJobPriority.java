/*
 * Copyright (C) 2012 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman;

import java.io.Serializable;

/**
 * Used to define the job priority at the time of submission
 * @author isaiah
 */
public enum GearmanJobPriority implements Serializable {
	/** Low job priority */
	LOW_PRIORITY,
	/** Normal job priority */
	NORMAL_PRIORITY,
	/** High job priority */
	HIGH_PRIORITY;
}
