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