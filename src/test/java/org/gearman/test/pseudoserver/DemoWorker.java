package org.gearman.test.pseudoserver;

import org.gearman.GearmanServer;
import org.gearman.GearmanWorker;
import org.gearman.impl.GearmanImpl;
import org.gearman.impl.server.remote.GearmanServerRemote;
import org.gearman.impl.worker.GearmanWorkerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by yangjunming on 6/25/16.
 */
public class DemoWorker {

    public static void main(String[] args) throws IOException, InterruptedException {
        GearmanImpl gearmanImpl = new GearmanImpl();
        GearmanWorker worker = new GearmanWorkerImpl(gearmanImpl);
        GearmanServer gearmanServer = new GearmanServerRemote(gearmanImpl, new InetSocketAddress("localhost", 4730));
        worker.addServer(gearmanServer);

        worker.addFunction("demoTask", (function, data, callback) -> {
            String param = new String(data, "utf-8");
            System.out.println("demoTask => param：" + param);
            return ("demoTask => hello," + param).getBytes();
        });

        worker.addFunction("anotherTask", (function, data, callback) -> {
            String param = new String(data, "utf-8");
            System.out.println("anotherTask => param：" + param);
            return ("anotherTask => hello," + param).getBytes();
        });

        while (true) {
            Thread.sleep(100);
        }
    }
}
