package org.gearman.reactor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.gearman.core.GearmanCallbackHandler;
import org.gearman.core.GearmanConnectionManager.ConnectCallbackResult;
import org.gearman.util.Scheduler;

public class ClientReactorStresser implements SocketHandler<Object> {

	/**
	 * Starts the client
	 */
	public static void main(String[] args) throws IOException {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		executor.prestartAllCoreThreads();
		
		// Create reactor
		final NioReactor reactor = new NioReactor(new Scheduler(executor));
		
		// Connect
		reactor.openSocket(new InetSocketAddress(ServerReactorStresser.PORT), new ClientReactorStresser(reactor), new GearmanCallbackHandler<InetSocketAddress, ConnectCallbackResult>() {
			@Override
			public void onComplete(InetSocketAddress data, ConnectCallbackResult result) {
				if(!result.isSuccessful()) {
					System.err.println("failed to connected");
					reactor.shutdown();
				}
			}
		});
		
		// close reactor on exit
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("shutting down");
				reactor.shutdown();
			}
		}));
	}
	
	private final NioReactor reactor;
	ClientReactorStresser(NioReactor reactor) {
		this.reactor = reactor;
	}
	
	@Override
	public ByteBuffer createSocketBuffer() {
		return ByteBuffer.allocateDirect(8);
	}

	@Override
	public void onAccept(Socket<Object> socket) {
		socket.write(ByteBuffer.wrap(ServerReactorStresser.toBytes(0)), null, null);
	}

	@Override
	public void onDisconnect(Socket<Object> socket) {
		System.err.println("disconnected from server");
		this.reactor.shutdown();
	}

	@Override
	public void onRead(Integer bytes, Socket<Object> socket) {
		final ByteBuffer buffer = socket.getByteBuffer();
		if(buffer.hasRemaining()) return;
		
		// prepare for reading
		buffer.flip();
		long value = buffer.getLong();
		
		// prepare for writting
		buffer.flip();
		
		// print value
		System.out.println(value++);
		
		// send next value
		socket.write(ByteBuffer.wrap(ServerReactorStresser.toBytes(value)), null, null);
	}
}
