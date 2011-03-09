/*
 * Copyright (c) 2010, Isaiah van der Elst
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *  - Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *    
 *  - Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *    
 *  - The names of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 *  SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 *  TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 *  DAMAGE.
 */

package org.gearman.util;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * An argument parser that follows the GNU argument and option convention.<br><br>
 * 
 * <b>GNU Convention:</b><br>
 * 		01. Arguments are options if they begin with a hyphen delimiter ('-').<br>
 * 		02. Multiple options may follow a hyphen delimiter in a single token if the options do not take arguments. Thus, '-abc' is equivalent to '-a -b -c'.<br>
 * 		03. Certain options require an argument. For example, the '-o' command of the ld command requires an argument—an output file name.<br>
 * 		04. An option and its argument may or may not appear as separate tokens. (In other words, the whitespace separating them is optional.) Thus, '-o foo' and '-ofoo' are equivalent.<br>
 * 		05. The argument "--" terminates all options; any following arguments are treated as non-option arguments, even if they begin with a hyphen.<br>
 * 		06. A token consisting of a single hyphen character is interpreted as an ordinary non-option argument.<br>
 * 		07. Options may be supplied in any order, or appear multiple times. The interpretation is left up to the particular application program.<br>
 * 		08. Long name options consist of '--' followed by a name made of alphanumeric characters and dashes.<br>
 * 		09. A Long name option's argument may be two separate tokens, or specified by the '=' symbol.  "--threads=4" is equivalent to "--threads 4"<br>
 * 		10. Long name options only need to be uniquely identified.  For example, "--th=4" is equivalent to "--threads=4" if the prefix "--th" can only belong the the option "--threads"<br><br>
 * 
 * Note: this class is not thread-safe
 * 
 * @author isaiah
 */
public final class ArgumentParser implements Iterable<ArgumentParser.Option>{
	
	/** Specifies if the argument parser is closed */
	private boolean isClosed = false;
	
	/** A list of all registered options ordered by the long name */
	private final ArrayList<Option> optionsLong = new ArrayList<Option>();
	/** A list of all registered options ordered by the short name */
	private final ArrayList<Option> optionsShort = new ArrayList<Option>();
	
	/** A list of all options that were flagged in the parsing proccess */
	private final ArrayList<Option> flagged = new ArrayList<Option>();
	
	/**
	 * Registers an option with the argument parser
	 * @param shortname
	 * 		A single char representing the option's short name.  If, for example, -v flags the option,
	 * 		this value should be 'v'
	 * @param longname
	 * 		A string representing the option's long name.  If, for example, --version flags the option,
	 * 		this value should be "version"
	 * @param takesArgument
	 * 		If true, the option being added is expected to accept an argument.  Otherwise the option
	 * 		takes no argument
	 * @return
	 * 		Returns true if the option was added successfully.  Adding a new option will fail if an
	 * 		option's shortname or longname is already used
	 */
	public final boolean addOption(final char shortname, final String longname, boolean takesArgument) {
		if(isClosed)
			throw new java.lang.IllegalStateException("ArgumentParser Closed");
		
		Option o = new Option(shortname, longname, takesArgument);
		
		final int pos_short = binarySearch(shortname, 0, this.optionsShort.size()-1);
		final int pos_long = binarySearch(longname, 0, this.optionsShort.size()-1);
		
		// Check if the short name is already registered
		if(pos_short<this.optionsShort.size()&&this.optionsShort.get(pos_short).shortname == shortname)
			return false;
		// Check if the long name is already registered
		if(pos_long<this.optionsLong.size()&&this.optionsLong.get(pos_long).longname.equals(longname))
			return false;
		
		this.optionsShort.add(pos_short, o);
		this.optionsLong.add(pos_long, o);
		
		return true;
	}
	
	/**
	 * Performs a binary search on optionsLong set.
	 * @param value
	 * 		The value to search for
	 * @param low
	 * 		The low end of the search range
	 * @param high
	 * 		The high end of the search range
	 * @return
	 * 		Returns the position of the element if it exists.  If it does not exist, then the
	 * 		insert position is returned
	 */
	private final int binarySearch(final String value, final int low, final int high) {
		if(high<low) 
			return low;
		
		final int mid = low + ((high - low) / 2);
		if (optionsLong.get(mid).longname.compareTo(value) > 0)
			return binarySearch(value, low, mid-1);
		if (optionsLong.get(mid).longname.compareTo(value) < 0)
			return binarySearch(value, mid+1, high);
		else
		 	return mid;	
	}
	
	/**
	 * Performs a binary search on optionsShort set.
	 * @param value
	 * 		The value to search for
	 * @param low
	 * 		The low end of the search range
	 * @param high
	 * 		The high end of the search range
	 * @return
	 * 		Returns the position of the element if it exists.  If it does not exist, then the
	 * 		insert position is returned
	 */
	private final int binarySearch(final char value, final int low, final int high) {
		if(high<low) 
			return low;
		
		final int mid = low + ((high - low) / 2);
		if (optionsShort.get(mid).shortname > value)
			return binarySearch(value, low, mid-1);
		if (optionsShort.get(mid).shortname < value)
			return binarySearch(value, mid+1, high);
		else
		 	return mid;	
	}
	
	/**
	 * Removes the option with the given shortname. 
	 * @param shortname
	 * 		The sortname that identifies the option to be removed
	 * @return
	 * 		Returns true if the option was removed
	 */
	public final boolean removeOption(final char shortname) {
		if(isClosed)
			throw new java.lang.IllegalStateException("ArgumentParser Closed");
		
		// Get the position of the option in the optionsShort set
		final int pos_short = this.binarySearch(shortname, 0, this.optionsShort.size()-1);
		
		// Make sure it is a valid position to get from
		if(pos_short>=this.optionsShort.size())
			return false;
		
		// Get the option
		Option o = this.optionsShort.get(pos_short);
		assert o!=null;
		
		// Check that the given short name is equivalent to the acquired option's short name
		if(o.shortname!=shortname)
			return false;
		
		// Get the position of the option in the optionsLong set
		final int pos_long = this.binarySearch(o.longname, 0, this.optionsLong.size()-1);
		
		// Make sure it is a valid position to get from
		if(pos_long>=this.optionsLong.size())
			return false;
		
		Option t1, t2;
		t1 = this.optionsShort.remove(pos_short);
		t2 = this.optionsLong.remove(pos_long);
		
		// Both options removed should be the same as the one pulled from the optionsShort set
		assert t1==o && t2==o;
		
		return true;
	}
	
	/**
	 * Removes a registered option by its long name
	 * @param longname
	 * 		The option's long name
	 * @return
	 * 		true if the option was removed
	 */
	public final boolean removeOption(final String longname) {
		if(isClosed)
			throw new java.lang.IllegalStateException("ArgumentParser Closed");
		
		// Get the position of the option in the optionsLong set
		final int pos_long = this.binarySearch(longname, 0, this.optionsLong.size()-1);
		
		// Make sure it is a valid position to get from
		if(pos_long>=this.optionsShort.size())
			return false;
		
		// Get the option
		Option o = this.optionsLong.get(pos_long);
		assert o!=null;
		
		// Check that the given long name is equivalent to the acquired option's long name
		if(!o.longname.equals(longname))
			return false;
		
		// Get the position of the option in the optionsLong set
		final int pos_short = this.binarySearch(o.shortname, 0, this.optionsShort.size()-1);
		
		// Make sure it is a valid position to get from
		if(pos_short>=this.optionsShort.size())
			return false;
		
		Option t1, t2;
		t1 = this.optionsShort.remove(pos_short);
		t2 = this.optionsLong.remove(pos_long);
		
		// Both options removed should be the same as the one pulled from the optionsShort set
		assert t1==o && t2==o;
		
		return true;
	}

	/**
	 * Parses the given command line arguments into a set of options and argument.  To retrieve
	 * the flagged options, use the iterator() method
	 * @param args
	 * 		The set of command line arguments
	 * @return
	 * 		The set of arguments that were not parsed into options.  null is returned if parsing
	 * 		failed.
	 */
	public final ArrayList<String> parse(String[] args) {
		if(isClosed)
			throw new java.lang.IllegalStateException("ArgumentParser Closed");
		
		// Reset the state of the parser and reset all options
		flagged.clear();
		for(Option o : optionsLong) {
			o.isFlagged = false;
			o.value = null;
		}
		
		ArrayList<String> arguments = new ArrayList<String>();
		
		String str;
		int size;
		for(int i=0; i<args.length; i++) {
			str = args[i];
			str = str.trim();
			size = str.length();
			if(size>=1 && str.charAt(0)=='-') {
				// An argument with a single dash ('-') is just an argument
				if(size==1) {
					arguments.add(str);
				} else if(size>=2 && str.charAt(1)=='-') {
					// long name or terminating argument
					
					if(size==2) {
						// terminating argument
						
						// place remaining values in arguments set and return 
						i++;
						for(;i<args.length; i++){
							str = args[i];
							str = str.trim();
							if(str.length()>0)
								arguments.add(str);
						}
						break;
					} else {
						// remove dashes and split on "=" char
						String[] split = str.substring(2).split("=", 2);
						
						Option o = this.getOptionByPrefix(split[0]);
						if(o==null) return null;
						
						o.isFlagged = true;
						this.flagged.add(o);
						if(o.takesArgument) {
							if(split.length==2) {
								o.value = split[1];
							} else {
								if(++i<args.length) {
									str = args[i];
									str = str.trim();
									o.value = str; 
								} else {
									// Argument requires an argument, but there is no next
									// argument
									return null;
								}
							}
						}
					}
				} else {
					// short name
					
					for(int j=1; j<size; j++) {
						int pos = this.binarySearch(str.charAt(j), 0, this.optionsShort.size()-1);
						if(pos<this.optionsShort.size()) {
							Option o = this.optionsShort.get(pos);
							if(o.shortname!=str.charAt(j)) return null;
							
							o.isFlagged = true;
							this.flagged.add(o);
							if(o.takesArgument) {
								if(j==size-1) {
									if(++i<args.length) {
										str = args[i];
										str = str.trim();
										o.value = str;
									} else {
										// Argument requires an argument, but there is no
										// next argument
										return null;
									}
								} else {
									o.value = str.substring(j+1);
									break;
								}
							}
						} else {
							// Out of range
							return null;
						}
					}
				}
			} else {
				// Add to argument set				
				if(size>0)
					arguments.add(str);
			}
		}
		
		return arguments;
	}
	
	/**
	 * The GNU standard states that you only need to uniquely identify an option's
	 * long name.  For example, "--th" may be used to identify the option "--threads"
	 * as long as "--th" is not a prefix to any other option. 
	 * @param prefix
	 * 		The prefix (not including dashes) that should identify a single option.
	 * 		This can be a the full long name
	 * @return
	 * 		The option that is identified by the given prefix.  If no Option is
	 * 		identified or more then one option is identified by this prefix, null
	 * 		is returned 
	 */
	private final Option getOptionByPrefix(String prefix) {
		final int size = this.optionsLong.size();
		final int pos = this.binarySearch(prefix, 0, size-1);
		
		if(pos>=size)
			return null;
		
		Option o_center = this.optionsLong.get(pos);
		Option o_right = pos+1<size ? this.optionsLong.get(pos+1): null;
		
		// Check if the prefix is the whole long name
		if(o_center.longname.equals(prefix))
			return o_center;
		
		// If the prefix is not in o_center, then it's not a prefix to any of the options
		if(!o_center.longname.startsWith(prefix)) {
			return null;
		}
		
		// If the given prefix is the prefix to both the center and right options, then
		// more then one option shares the given prefix 
		if(o_right!=null&&o_right.longname.startsWith(prefix)) {
			return null;
		}
		
		// o_center is the only option with the given prefix
		return o_center;
	}
	
	/**
	 * Returns an iterator for iterating over the set of flagged options
	 */
	public Iterator<Option> iterator() {
		return this.flagged.iterator();
	}
	
	/**
	 * Closes the argument parser. Free resources and locks the object   
	 */
	public final void close() {
		this.isClosed = true;
		this.optionsLong.clear();
		this.optionsShort.clear();
		this.flagged.clear();
	}
	
	/**
	 * Represents an option
	 * @author isaiah.v
	 */
	public final class Option {
		private final char		shortname;
		private final String		longname;
		private final boolean		takesArgument;
		private boolean			isFlagged;
		private String 				value;
		
		public Option(final char shortname, final String longname, final boolean takesArgument) {
			this.shortname = shortname;
			this.longname = longname;
			this.takesArgument = takesArgument;
		}
		
		/**
		 * Returns the option's short name
		 * @return
		 * 		The short name
		 */
		public final char getShortName() {
			return shortname;
		}
		
		/**
		 * Returns the option's long name
		 * @return
		 * 		The long name
		 */
		public final String getLongName() {
			return longname;
		}
		
		/**
		 * Tests if this option requires an argument
		 * @return
		 * 		<code>true</code> if and only if the option takes an argument
		 */
		public final boolean takesArgument() {
			return this.takesArgument;
		}
		
		/**
		 * Returns true if the user flagged this option
		 * @return
		 * 		<code>true</code> if and only if a user specified this option
		 */
		public final boolean isFlagged() {
			return isFlagged;
		}
		
		/**
		 * Returns the argument value if one is available
		 * @return
		 * 		Returns the argument value if one is available. Null is returned if
		 * 		no argument is available.
		 */
		public final String getValue() {
			return value;
		}
	}
}
