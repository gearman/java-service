package org.gearman;

/**
 * @author isaiah
 */
class ClientConnectionList <V, K> {
	private final class Node {
		private Node prev = null;
		private Node next = null;
		
		private K failKey;
		private final V value;
		
		public Node(final V value) {
			this.value = value;
		}
	}
	
	private Node head = null;
	private Node tail = null;
	
	public final synchronized boolean contains(final V value) {
		Node n = this.head;
		while(n!=null) {
			if(n.value.equals(value)) return true;
			n = n.next;
		}
		return false;		
	}
	
	public final synchronized boolean hasFailKeys() {
		
		Node n = head;
		while(n != null) {
			if(n.failKey!=null) {
				return true;
			} else {
				n=n.next;
			}
		}
			
		return false;
	}
	
	public final synchronized boolean add(final V value) {
		final Node node = new Node(value);
		
		if(this.head==null) {
			
			assert tail==null;
			
			this.head = node;
			this.tail = node;
			
		} else {
			
			assert tail!=null;
			assert tail.next==null;
			
			node.prev = tail;
			tail.next = node;
			tail = node;
		}
		return true;
	}
	
	public final synchronized boolean addFirst(final V value) {
		final Node node = new Node(value);
		
		if(this.head==null) {
			assert tail==null;
			
			this.head = node;
			this.tail = node;
			
		} else {
			assert head.prev==null;
			
			node.next = this.head;
			this.head.prev = node;
			this.head = node;
		}
		return true;
	}
	
	public final synchronized K remove(final V value) {
		
		for(Node n=this.head; n!=null; n=n.next) {
			
			if(!n.value.equals(value)) continue;
			
			if(n.next!=null) {
				n.next.prev = n.prev;
			} else {
				assert n==this.tail;
				this.tail = n.prev;
			}
			
			if(n.prev!=null) {
				n.prev.next = n.next;
				
				if(n.failKey!=null)
					n.prev.failKey = n.failKey;
				
				return null;		// removed value, fail key moved back 
			} else {
				assert n==this.head;
				this.head = n.next;
				
				return n.failKey;	// removed value, fail key returned
			}
		}
		
		return null;	// Value no in structure
	}
	
	public final synchronized V tryFirst(final K failKey) {
		if (this.head==null || this.tail==null) return null;
		
		this.tail.failKey = failKey;
		return head.value;
	}
	
	public final synchronized V peek() {
		return this.tail==null? null: this.tail.value;
	}
	
	public final synchronized K removeFirst() {
		if(this.head==null) return null;
		assert this.head.prev == null;
		
		final K failKey = head.failKey;
		
		this.head = this.head.next;
		if(this.head!=null)
			this.head.prev = null;
		
		return failKey;		
	}
	
	public final synchronized void clearFailKeys() {
		for(Node n = this.head; n!=null; n=n.next) {
			n.failKey = null;
		}
	}
}
