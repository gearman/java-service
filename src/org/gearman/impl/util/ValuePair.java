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
