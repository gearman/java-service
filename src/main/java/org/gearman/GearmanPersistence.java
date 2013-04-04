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

package org.gearman;

import java.util.Collection;

/**
 * An application hook telling the framework how jobs will persist.
 * @author isaiah
 */
public interface GearmanPersistence {
	
	/**
	 * Write the {@link GearmanPersistable} item to a persistent medium.<br>
	 * <br>
	 * When this method returns, it is guaranteed the write operation has completed.
	 * @param item
	 * 		The item to write to a persistent medium
	 * @throws Exception
	 * 		If an exception occurs while writing the item
	 */
	public void write(GearmanPersistable item) throws Exception;
	
	/**
	 * Removes the {@link GearmanPersistable} item from the persistent medium.<br>
	 * <br>
	 * When this method returns, it is guaranteed the delete operation has completed.
	 * @param item
	 * 		The item to remove
	 * @throws Exception
	 * 		If an exception occurs while removing the item
	 */
	public void delete(GearmanPersistable item) throws Exception;
	
	/**
	 * Removes all items from the persistent medium.<br>
	 * <br>
	 * When this method returns, it is guaranteed all delete operations have completed.
	 * @throws Exception
	 * 		If an exception occurs while removing the items
	 */
	public void deleteAll() throws Exception;
	
	/**
	 * Reads all persistable items from the persistent medium.
	 * @return
	 * 		A collection {@link GearmanPersistable} 
	 * @throws Exception
	 * 		If an exception occurs while removing the items
	 */
	public Collection<GearmanPersistable> readAll() throws Exception;
}