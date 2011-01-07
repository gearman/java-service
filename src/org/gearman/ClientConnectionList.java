package org.gearman;

/**
 * @author isaiah
 */
class ClientConnectionList <V, K> {
	private final class Node {
		private Node last = null;
		private Node next = null;
		
		private K failKey;
		private final V value;
		
		public Node(final V value) {
			this.value = value;
		}
	}
	
	private Node head = null;
	private Node tail = null;
	
	public final synchronized boolean add(final V value) {
		final Node node = new Node(value);
		
		if(this.head==null) {
			
			assert tail==null;
			
			this.head = node;
			this.tail = node;
			
		} else {
			
			assert tail!=null;
			assert tail.next==null;
			
			node.last = tail;
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
			assert head.last==null;
			
			node.next = this.head;
			this.head.last = node;
			this.head = node;
		}
		return true;
	}
	
	public final synchronized K remove(final V value) {
		
		for(Node n=this.head; n!=null; n=n.next) {
			
			if(!n.value.equals(value)) continue;
			
			if(n.next!=null) {
				n.next.last = n.last;
			} else {
				assert n==this.tail;
				this.tail = n.last;
			}
			
			if(n.last!=null) {
				n.last.next = n.next;
				
				if(n.failKey!=null)
					n.last.failKey = n.failKey;
				
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
		return tail.value;
	}
	
	public final synchronized V peek() {
		return this.tail==null? null: this.tail.value;
	}
	
	public final synchronized K removeFirst() {
		if(this.head==null) return null;
		assert this.head.last == null;
		
		final K failKey = head.failKey;
		
		this.head = this.head.next;
		if(this.head!=null)
			this.head.last = null;
		
		return failKey;		
	}
	
	public final synchronized void clearFailKeys() {
		for(Node n = this.head; n!=null; n=n.next) {
			n.failKey = null;
		}
	}
}
