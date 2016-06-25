package org.gearman.test.pseudoserver;

import org.gearman.GearmanServer;
import org.gearman.impl.GearmanImpl;
import org.gearman.impl.client.ClientImpl;
import org.gearman.impl.server.remote.GearmanServerRemote;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by yangjunming on 6/25/16.
 */
public class DemoClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        GearmanImpl gearmanImpl = new GearmanImpl();
        GearmanServer gearmanServer = new GearmanServerRemote(gearmanImpl, new InetSocketAddress("localhost", 4730));
        ClientImpl client = new ClientImpl(gearmanImpl);
        client.addServer(gearmanServer);

        //异步方式派发任务
        client.submitBackgroundJob("demoTask", "jimmy".getBytes());

        //同步方式派发任务
        client.submitJob("anotherTask", "mike".getBytes());
    }
}
