package org.gearman.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.gearman.util.Scheduler;

public class ServerReactorStresser implements SocketHandler<Object> {
	/** The streeser's port */
	public static int PORT = 4321;
	
	/**
	 * Starts the server
	 */
	public static void main(String[] args) throws IOException {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		executor.prestartAllCoreThreads();
		
		final NioReactor reactor = new NioReactor(new Scheduler(executor));
		reactor.openPort(PORT, new ServerReactorStresser());
	}
	
	@Override
	public ByteBuffer createSocketBuffer() {
		return ByteBuffer.allocateDirect(8);
	}

	@Override
	public void onAccept(Socket<Object> socket) {
	}

	@Override
	public void onDisconnect(Socket<Object> socket) {
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
		System.out.println(value);
		
		// send value
		socket.write(ByteBuffer.wrap(ServerReactorStresser.toBytes(value)), null, null);
	}
	
	/**
	 * Converts a byte array to a long 
	 * @param array
	 * 		An array representing a long
	 * @return
	 * 		The long
	 * @throws IndexOutOfBoundsException
	 * 		if the size of the array is less then 8
	 */
	public static final long toLong(final byte[] array) {
		 return ((array[0] & 0xffL) <<  56) |
		 		((array[1] & 0xffL) <<  48) |
		 		((array[2] & 0xffL) <<  40) |
		 		((array[3] & 0xffL) <<  32) |
		 		((array[4] & 0xffL) <<  24) |
		 		((array[5] & 0xffL) <<  16) |
		 		((array[6] & 0xffL) <<  8)  |
		 		((array[7] & 0xffL));
	}
	
	/**
	 * Converts a long to a byte array
	 * @param number
	 * 		The long
	 * @return
	 * 		the byte array
	 */
	public static final byte[] toBytes(final long number) {
		return new byte[] {
				(byte)((number & 0xFF00000000000000L)>>56L),
				(byte)((number & 0x00FF000000000000L)>>48L),
				(byte)((number & 0x0000FF0000000000L)>>40L),
				(byte)((number & 0x000000FF00000000L)>>32L),
				(byte)((number & 0x00000000FF000000L)>>24L),
				(byte)((number & 0x0000000000FF0000L)>>16L),
				(byte)((number & 0x000000000000FF00L)>>8L),
				(byte)((number & 0x00000000000000FFL))
		};
	}
}
