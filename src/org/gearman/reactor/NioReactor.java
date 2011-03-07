package org.gearman.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AcceptPendingException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetBoundException;
import java.nio.channels.ShutdownChannelGroupException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanConnectionManager.ConnectCallbackResult;

public final class NioReactor {
	
	private final AsynchronousChannelGroup asyncChannelGroup;
	private final ConcurrentHashMap<Integer, AsynchronousServerSocketChannel> ports = new ConcurrentHashMap<Integer, AsynchronousServerSocketChannel>();
	
	public NioReactor(final ExecutorService executor) throws IOException {
		this.asyncChannelGroup = AsynchronousChannelGroup.withThreadPool(executor);
	}
	
	public synchronized void shutdown() {
		this.closePorts();
		asyncChannelGroup.shutdown();
	}
	
	public boolean isShutdown() {
		return asyncChannelGroup.isShutdown();
	}
	
	public synchronized boolean closePort(int port) throws IOException {
		final AsynchronousServerSocketChannel server = this.ports.remove(port);
		if(server==null) return false;
		
		server.close();
		return true;
	}
	
	public synchronized void closePorts() {
		Iterator<AsynchronousServerSocketChannel> it = this.ports.values().iterator();
		while(it.hasNext()) {
			try {
				AsynchronousServerSocketChannel assc = it.next();
				assc.close();
				it.remove();
			} catch (IOException e) {
				// TODO log error
			}
		}
	}
	
	public synchronized Set<Integer> getOpenPorts() {
		return Collections.unmodifiableSet(this.ports.keySet());
	}
	
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
		
		server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {

			@Override
			public void completed(AsynchronousSocketChannel result, Object attachment) {
				server.accept(null, this);
				
				try {
					SocketImpl<A> sImpl = new SocketImpl<A>(result,handler);
					handler.onAccept(sImpl);
					sImpl.read();
				} catch (IOException e) {
					// failed to create SocketImpl.
				} catch (Throwable e) {
					// User threw runtime exception
					
					e.printStackTrace();
					return;
				}
			}

			@Override
			public void failed(Throwable exc, Object attachment) {
				if(exc instanceof ShutdownChannelGroupException) {
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
