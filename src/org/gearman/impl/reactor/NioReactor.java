/*
 * Copyright (C) 2010 by Isaiah van der Elst <isaiah.v@comcast.net>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.impl.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AcceptPendingException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import org.gearman.impl.GearmanConstants;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnectionManager.ConnectCallbackResult;

/**
 * The connection handler
 * @author isaiah
 */
public final class NioReactor {
	
	/** nio.2 thread pool */
	private final AsynchronousChannelGroup asyncChannelGroup;
	
	/** the set of open ports */
	private final ConcurrentHashMap<Integer, AsynchronousServerSocketChannel> ports = new ConcurrentHashMap<Integer, AsynchronousServerSocketChannel>();
	
	/**
	 * Creates a new NioReactor
	 * @param executor
	 * 		The underlying thread pool driving the underlying service
	 * @throws IOException
	 * 		If an I/O error occurs
	 */
	public NioReactor(final ExecutorService executor) throws IOException {
		this.asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(executor);
	}
	
	/**
	 * Closes all ports and shuts down the NioReactor service
	 */
	public synchronized void shutdown() {
		this.closePorts();
		asyncChannelGroup.shutdown();
	}
	
	/**
	 * Tests if this NioReactor service is shutdown
	 * @return
	 * 		<code>true</code> if this NioReactor service is shutdown
	 */
	public boolean isShutdown() {
		return asyncChannelGroup.isShutdown();
	}
	
	/**
	 * Attempts to close an open port
	 * @param port
	 * 		
	 * @return
	 * @throws IOException
	 */
	public synchronized boolean closePort(int port) throws IOException {
		final AsynchronousServerSocketChannel server = this.ports.remove(port);
		if(server==null) return false;
		
		server.close();
		return true;
	}
	
	/**
	 * Closes all open ports
	 */
	public synchronized void closePorts() {
		Iterator<AsynchronousServerSocketChannel> it = this.ports.values().iterator();
		while(it.hasNext()) {
			try {
				AsynchronousServerSocketChannel assc = it.next();
				assc.close();
				it.remove();
			} catch (IOException e) {
				GearmanConstants.LOGGER.warn("failed to close port",e);
			}
		}
	}
	
	/**
	 * Returns the list of open ports
	 * @return
	 * 		The list of open ports
	 */
	public synchronized Set<Integer> getOpenPorts() {
		return Collections.unmodifiableSet(this.ports.keySet());
	}
	
	/**
	 * Opens a new socket
	 * @param adrs
	 * 		The address of the server to connect to
	 * @param sHandler
	 * 		The socket handler
	 * @param callback
	 */
	public final <X> void openSocket(final InetSocketAddress adrs, final SocketHandler<X> sHandler, final GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult> callback) {
		final AsynchronousSocketChannel socket;
		try {
			socket = AsynchronousSocketChannel.open(this.asyncChannelGroup);
		} catch (ShutdownChannelGroupException scge) {
			try {
				callback.onComplete(adrs, ConnectCallbackResult.SERVICE_SHUTDOWN);
			} catch (Throwable th) {
				// user threw exception
				th.printStackTrace();
			}
			return;
		} catch (IOException e) {
			try {
				callback.onComplete(adrs, ConnectCallbackResult.CONNECTION_FAILED);
			} catch (Throwable th) {
				// user threw exception
				th.printStackTrace();
			}
			return;
		}
		
		socket.connect(adrs, null, new CompletionHandler<Void, Object>(){

			@Override
			public void completed(Void result, Object attachment) {
				try {
					callback.onComplete(adrs, ConnectCallbackResult.SUCCESS);
				} catch (Throwable th) {
					// User threw runtime exception
					th.printStackTrace();
				}
				
				try {
					SocketImpl<X> sImpl = new SocketImpl<X>(socket,sHandler);
					sHandler.onAccept(sImpl);
					sImpl.read();
				} catch (IOException e) {
					// failed to create SocketImpl.
				} catch (Throwable th) {
					// User threw runtime exception
					th.printStackTrace();
					return;
				}
			}

			@Override
			public void failed(Throwable exc, Object attachment) {
				if(exc instanceof ShutdownChannelGroupException) {
					try {
						callback.onComplete(adrs, ConnectCallbackResult.SERVICE_SHUTDOWN);
					} catch (Throwable th) {
						// user threw exception
						th.printStackTrace();
					}
				} else {
					try {
						callback.onComplete(adrs, ConnectCallbackResult.CONNECTION_FAILED);
					} catch (Throwable th) {
						// user threw exception
						th.printStackTrace();
					}
				}
			}
			
		});
	}
	
	public synchronized final <A> void openPort(final int port, final SocketHandler<A> handler) throws IOException {
		final AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(this.asyncChannelGroup);
		server.bind(new InetSocketAddress(port));
		
		//TODO set options
		
		final Object o;
		o = this.ports.putIfAbsent(port, server);
		
		// If this port is already open, an exception should have been thrown
		assert o==null;
		
		server.accept(new LinkedBlockingQueue<Socket<?>>(), new CompletionHandler<AsynchronousSocketChannel, Queue<Socket<?>>>() {

			@Override
			public void completed(AsynchronousSocketChannel result, Queue<Socket<?>> clientList) {
				try {
					SocketImpl<A> sImpl = new SocketImpl<A>(result,handler);
					handler.onAccept(sImpl);
					sImpl.read();
					
					clientList.add(sImpl);
				} catch (IOException e) {
					// failed to create SocketImpl.
				} catch (Throwable e) {
					// User threw runtime exception
					
					e.printStackTrace();
					return;
				} finally {
					server.accept(clientList, this);
				}
			}

			@Override
			public void failed(Throwable exc, Queue<Socket<?>> clientList) {
				for(Socket<?> socket : clientList) {
					socket.close();
				}
				
				if(exc instanceof ShutdownChannelGroupException) {
					// It is possible this method is entered. However, there's not much to do
					return;
				} else if(exc instanceof AsynchronousCloseException) {
					// It is possible this method is entered. However, there's not much to do
					return;
				} else if(exc instanceof AcceptPendingException) {
					assert false;
					exc.printStackTrace();
				} else if(exc instanceof NotYetBoundException) {
					assert false;
					exc.printStackTrace();
				} else {
					assert false;
					exc.printStackTrace();
				}
			}
		});
	}
	
}
