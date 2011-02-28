package org.gearman;

import static org.junit.Assert.*;

import java.io.IOException;

import org.gearman.core.GearmanConnection;
import org.gearman.dbg.GearmanMockServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestGearmanClient {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCreateControllerGearmanServer() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateControllerInetSocketAddress() {
		fail("Not yet implemented");
	}

	@Test
	public void testSubmitJob() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetGearman() throws IOException {
		Gearman gearman = new Gearman();
		GearmanClient client = gearman.createGearmanClient();
			
		assertEquals(gearman, client.getGearman());
		
		client.shutdown();
		gearman.shutdown();
	}

	@Test
	public void testAddServerGearmanServer() throws IOException {		
	}

	@Test
	public void testAddServerInetSocketAddress() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetClientID() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetReconnectPeriod() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetServerCount() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasServerInetSocketAddress() {
		fail("Not yet implemented");
	}

	@Test
	public void testHasServerGearmanServer() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveAllServers() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveServerGearmanServer() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveServerInetSocketAddress() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetClientID() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetLostConnectionPolicy() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetReconnectPeriod() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsShutdown() {
		fail("Not yet implemented");
	}

	@Test
	public void testShutdown() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetConnections() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDefaultPolicy() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateControllerGearmanServer1() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreateControllerInetSocketAddress1() {
		fail("Not yet implemented");
	}

}
