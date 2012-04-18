package org.gearman.impl.serverpool;

import org.gearman.GearmanJobStatus;

class GearmanJobStatusImpl implements GearmanJobStatus {
	
	private final boolean isKnown;
	private final boolean isRunning;
	private final long numerator;
	private final long denominator;
	
	GearmanJobStatusImpl(boolean isKnown, boolean isRunning, long num, long den) {
		this.isKnown = isKnown;
		this.isRunning = isRunning;
		this.numerator = num;
		this.denominator = den;
	}
	
	@Override
	public boolean isKnown() {
		return isKnown;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public long getNumerator() {
		return numerator;
	}

	@Override
	public long getDenominator() {
		return denominator;
	}

}
