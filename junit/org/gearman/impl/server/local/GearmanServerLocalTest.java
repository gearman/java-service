/**
 * 
 */
package org.gearman.impl.server.local;

import static org.junit.Assert.*;

import org.gearman.impl.GearmanImpl;
import org.gearman.impl.core.GearmanCallbackHandler;
import org.gearman.impl.core.GearmanConnection;
import org.gearman.impl.core.GearmanConnectionHandler;
import org.gearman.impl.core.GearmanConnectionManager.ConnectCallbackResult;
import org.gearman.impl.core.GearmanPacket;
import org.gearman.impl.server.GearmanServerInterface;
import org.gearman.impl.server.ServerShutdownListener;
import org.gearman.impl.util.TaskJoin;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author isaiah
 *
 */
public class GearmanServerLocalTest {

	private static GearmanImpl gearman;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		gearman = new GearmanImpl();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		gearman.shutdown();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#hashCode()}.
	 */
	@Test
	public void testHashCode() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#finalize()}.
	 */
	@Test
	public void testFinalize() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#GearmanServerLocal(org.gearman.impl.GearmanImpl, org.gearman.GearmanPersistence, int[])}.
	 */
	@Test
	public void testGearmanServerLocalGearmanImplGearmanPersistenceIntArray() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#GearmanServerLocal(org.gearman.impl.GearmanImpl, org.gearman.GearmanPersistence, java.lang.String, int[])}.
	 */
	@Test
	public void testGearmanServerLocalGearmanImplGearmanPersistenceStringIntArray() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#getClientSet()}.
	 */
	@Test
	public void testGetClientSet() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#isLocalServer()}.
	 * @throws Exception 
	 */
	@Test
	public void testIsLocalServer() throws Exception {
		GearmanServerLocal server = (GearmanServerLocal)gearman.startGearmanServer();
		assertTrue(server.isLocalServer());
		server.shutdown();
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#getHostName()}.
	 */
	@Test
	public void testGetHostName() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#shutdown()}.
	 * @throws Exception 
	 */
	@Test
	public void testShutdown() throws Exception {
		GearmanServerLocal server = (GearmanServerLocal)gearman.startGearmanServer();
		
		// Test 1: shutdown server and make sure the shutdown listener was called 
		{
			InnerServerShutdownListener shutdownListener = new InnerServerShutdownListener();
			server.addShutdownListener(shutdownListener);
			assertFalse(shutdownListener.join());	// if false, the listener was not called
			server.shutdown();	// shutdown
			assertTrue(shutdownListener.join());	// if true, the listener was called
		}
		
		// Test 2: make sure the shutdown lister is called if the server is already shutdown
		{
			InnerServerShutdownListener shutdownListener = new InnerServerShutdownListener();
			
			try {
				server.addShutdownListener(shutdownListener);
				fail("expected exception");
			} catch(IllegalStateException ise) {
			}
		}
		
		// Test 3: make sure when 
		{
		}
		
		//TODO make sure all state modifying methods throw an IllegalStateException
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#isShutdown()}.
	 */
	@Test
	public void testIsShutdown() throws Exception {
		GearmanServerLocal server = (GearmanServerLocal)gearman.startGearmanServer();
		assertFalse(server.isShutdown());
		server.shutdown();
		assertTrue(server.isShutdown());
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#getGearman()}.
	 */
	@Test
	public void testGetGearman() throws Exception {
		GearmanServerLocal server = (GearmanServerLocal)gearman.startGearmanServer();
		assertTrue(server.getGearman()==gearman);
		server.shutdown();
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#getServerID()}.
	 */
	@Test
	public void testGetServerID() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#toString()}.
	 */
	@Test
	public void testToString() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#createGearmanConnection(org.gearman.impl.core.GearmanConnectionHandler, org.gearman.impl.core.GearmanCallbackHandler)}.
	 */
	@Test
	public void testCreateGearmanConnection() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#getPorts()}.
	 */
	@Test
	public void testGetPorts() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#onAccept(org.gearman.impl.core.GearmanConnection)}.
	 */
	@Test
	public void testOnAccept() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#onPacketReceived(org.gearman.impl.core.GearmanPacket, org.gearman.impl.core.GearmanConnection)}.
	 */
	@Test
	public void testOnPacketReceived() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#onDisconnect(org.gearman.impl.core.GearmanConnection)}.
	 */
	@Test
	public void testOnDisconnect() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#addShutdownListener(org.gearman.impl.server.ServerShutdownListener)}.
	 */
	@Test
	public void testAddShutdownListener() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.gearman.impl.server.local.GearmanServerLocal#removeShutdownListener(org.gearman.impl.server.ServerShutdownListener)}.
	 */
	@Test
	public void testRemoveShutdownListener() {
		fail("Not yet implemented");
	}
	
	private static class InnerServerShutdownListener implements ServerShutdownListener {
		private static final long JOIN_TIMEOUT = 500;
		final TaskJoin<Boolean> taskJoin = new TaskJoin<Boolean>();
		
		@Override
		public void onShutdown(GearmanServerInterface server) {
			taskJoin.setValue(Boolean.TRUE);
		}
		
		public boolean join() {
			return Boolean.TRUE.equals(taskJoin.getValue(JOIN_TIMEOUT));
		}
	}
	
	private static class InnerGearmanCallbackHandler implements GearmanCallbackHandler<GearmanServerInterface, ConnectCallbackResult> {
		private static final long JOIN_TIMEOUT = 500;
		final TaskJoin<ConnectCallbackResult> taskJoin = new TaskJoin<ConnectCallbackResult>();
		
		@Override
		public void onComplete(GearmanServerInterface data, ConnectCallbackResult result) {
			taskJoin.setValue(result);
		}
		
		public ConnectCallbackResult join() {
			return taskJoin.getValue(JOIN_TIMEOUT);
		}
	}
	
	private static class InnerGearmanConnectionHandler<A> implements GearmanConnectionHandler<A> {

		@Override
		public void onAccept(GearmanConnection<A> conn) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onPacketReceived(GearmanPacket packet,
				GearmanConnection<A> conn) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onDisconnect(GearmanConnection<A> conn) {
			// TODO Auto-generated method stub
			
		}
		
	}
}
