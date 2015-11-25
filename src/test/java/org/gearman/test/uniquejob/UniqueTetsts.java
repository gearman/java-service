package org.gearman.test.uniquejob;

import static org.junit.Assert.*;
import org.gearman.Gearman;
import org.gearman.GearmanClient;
import org.gearman.GearmanFunction;
import org.gearman.GearmanFunctionCallback;
import org.gearman.GearmanJobEvent;
import org.gearman.GearmanJobPriority;
import org.gearman.GearmanJobReturn;
import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;
import org.junit.Before;
import org.junit.Test;


public class UniqueTetsts {

	Gearman gearman;
	GearmanClient client;
	GearmanServer server;
	GearmanJobReturn jr;
	GearmanJobEvent event;
	GearmanWorker worker;
	
	private final String IP_SERVER = "127.0.0.1";
	private final Integer PORT_GEARMAN = 4730;
	
	private final String UNIQUE = "unique";
	private final String NOTUNIQUE = "notUnique";
	
	private Integer counter = 0;
	private Integer functionsReachedUnique = 0;
	private Integer functionsReachedNotUnique = 0;
	
	@Before
	public void setup(){
		this.counter = 0;
		this.functionsReachedNotUnique = 0;
		this.functionsReachedUnique = 0;
		gearman = Gearman.createGearman();
		client = gearman.createGearmanClient();
		server = gearman.createGearmanServer(IP_SERVER, PORT_GEARMAN);
		client.addServer(server);
		worker = gearman.createGearmanWorker();
		worker.addServer(server);
		worker.removeAllFunctions();
	}
	
	@Test
	public void testJobNotUnique() {
		client.submitBackgroundJob(getQueueInName(NOTUNIQUE), getSendByte("NOTUnique"), getPriority());
		client.submitBackgroundJob(getQueueInName(NOTUNIQUE), getSendByte("NOTUnique"), getPriority());
		client.submitBackgroundJob(getQueueInName(NOTUNIQUE), getSendByte("NOTUnique"), getPriority());
		
		worker.addFunction(getQueueInName(NOTUNIQUE), new GearmanFunction() {
			@Override
			public byte[] work(String function, byte[] data,
					GearmanFunctionCallback callback) throws Exception {
				functionsReachedNotUnique++;
				return null;
			}
			
		});
		
		//Sleep because of server communication latency		
		sleep();
		assertEquals((Integer)3, (Integer)this.functionsReachedNotUnique);
	}
	
	@Test
	public void testJobUnique() {
		client.submitBackgroundJob(getQueueInName(UNIQUE), getSendByte("Unique"), getPriority(), "uniqueid");
		client.submitBackgroundJob(getQueueInName(UNIQUE), getSendByte("Unique"), getPriority(), "uniqueid");
		client.submitBackgroundJob(getQueueInName(UNIQUE), getSendByte("Unique"), getPriority(), "uniqueid");
		
		worker.addFunction(getQueueInName(UNIQUE), new GearmanFunction() {
			@Override
			public byte[] work(String function, byte[] data,
					GearmanFunctionCallback callback) throws Exception {
				functionsReachedUnique++;
				System.out.println(functionsReachedUnique);
				return null;
			}
		});
		
		//Sleep because of server communication latency
		sleep();
		assertEquals((Integer)1, (Integer)this.functionsReachedUnique);
	}
	
	private String getQueueInName(String function){
		return "test_queu_" + function;
	}
	
	private byte[] getSendByte(String fromTest){
		String result = "fromTest: " + fromTest;
		result += counter++;
		result += " ##";
		return result.getBytes();
	}
	
	private GearmanJobPriority getPriority(){
		return GearmanJobPriority.NORMAL_PRIORITY;
	}
	
	private void sleep(){
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
