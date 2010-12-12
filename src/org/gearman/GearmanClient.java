package org.gearman;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.gearman.core.GearmanCompletionHandler;

/**
 * A GearmanCliet submits {@link GearmanJob}s and {@link GearmanBackgroundJob}s
 * to gearman job servers
 * 
 * @author isaiah.v
 */
public interface GearmanClient extends GearmanService {

	/**
	 * Adds a {@link GearmanServer} to this client. This goes into a list of
	 * servers that can be used to run tasks.<br>
	 * <br>
	 * Since a {@link GearmanServer} cannot be reopened, once it is shutdown
	 * it'll be removed from the list of servers.
	 * 
	 * @param server
	 *            The GearmanServer used to run tasks
	 */
	public void addServer(GearmanServer server);
	public <A> void addServer(GearmanServer server, A att, GearmanCompletionHandler<A> callback);

	/**
	 * Adds a remote job server to this client. This goes into a list of servers
	 * that can be used to run tasks. No socket I/O happens here, it is just
	 * added to a list.<br>
	 * 
	 * @param adrs
	 *            The address of the job server
	 */
	public void addServer(InetSocketAddress adrs);
	public <A> void addServer(InetSocketAddress adrs, A att, GearmanCompletionHandler<A> callback);

	/**
	 * Adds a remote job server to this client. This goes into a list of servers
	 * that can be used to run tasks. No socket I/O happens here, it is just
	 * added to a list.<br>
	 * 
	 * @param host
	 *            Hostname or IP address (IPv4 or IPv6) of the server to add.
	 * @param port
	 *            Port of the server to add.
	 */
	public void addServer(String host, int port);
	public <A> void addServer(String host, int port, A att, GearmanCompletionHandler<A> callback);
	

	/**
	 * Tests if this client is managing the specified server
	 * 
	 * @param server
	 *            The server in question
	 * @return true if the client is currently managing the specified server.
	 *         false if the client is not managing specified server
	 */
	public boolean hasServer(GearmanServer server);

	/**
	 * Tests if this client is managing the specified server
	 * 
	 * @param adrs
	 *            The address of the server in question
	 * @return <code>true</code> if the client is currently managing the
	 *         specified server. This does not imply the connection is currently
	 *         active unless the automatic reconnect feature is disabled.
	 *         <code>false</code> if the client is not managing specified server
	 */
	public boolean hasServer(InetSocketAddress adrs);

	/**
	 * Tests if this client is managing the specified server
	 * 
	 * @param host
	 *            Hostname or IP address (IPv4 or IPv6) of the server in
	 *            question
	 * @param port
	 *            The port number of the server in question
	 * @return <code>true</code> if the client is currently managing the
	 *         specified server. This does not imply the connection is currently
	 *         active unless the automatic reconnect feature is disabled.
	 *         <code>false</code> if the client is not managing specified server
	 * 
	 */
	public boolean hasServer(String host, int port);

	/**
	 * Removes the specified {@link GearmanServer} from this client.
	 * 
	 * @param server
	 *            The server to remove
	 * @return <code>true</code> if specified server was removed from this
	 *         client. <code>false</code> if this client was not managing the
	 *         specified server
	 */
	public boolean removeServer(GearmanServer server);

	/**
	 * Remove the specified job server from this client
	 * 
	 * @param adrs
	 *            The address of the job server to remove
	 * @return <code>true</code> if specified server was removed from this
	 *         client. <code>false</code> if this client was not managing the
	 *         specified server
	 */
	public boolean removeServer(InetSocketAddress adrs);

	/**
	 * Remove the specified job server from this client
	 * 
	 * @param host
	 *            Hostname or IP address (IPv4 or IPv6) of the server to add.
	 * @param port
	 *            Port of the server to add.
	 * @return <code>true</code> if specified server was removed from this
	 *         client. <code>false</code> if this client was not managing the
	 *         specified server
	 */
	public boolean removeServer(String host, int port);

	/**
	 * Remove all job servers from this client. This includes both local and
	 * remote job servers
	 */
	public void removeAllServers();

	/**
	 * Specifies this client's ID
	 * 
	 * @param id
	 *            The client's new ID
	 */
	public void setClientID(String id);

	/**
	 * Submits a {@link GearmanBackgroundJob} to be executed. Background jobs
	 * are detached from the client, so no callback information or result will
	 * be received through the {@link GearmanBackgroundJob} object. However,
	 * status information can be polled from the server using the
	 * {@link GearmanBackgroundJob} object.
	 * 
	 * @param job
	 *            The job to be submitted
	 * @throws IOException
	 *             Thrown if the job cannot be sent to any of the listed job
	 *             servers
	 */
	public <A> void submitJob(GearmanBackgroundJob job, A att, GearmanCompletionHandler<A> callback);

	/**
	 * Submits a {@link GearmanJob} to be executed.<br>
	 * 
	 * @param job
	 *            The job being submitted
	 * @throws IOException
	 *             Thrown if the job cannot be sent to any of the listed job
	 *             servers
	 */
	public <A> void submitJob(GearmanJob job, A att, GearmanCompletionHandler<A> callback);

	/**
	 * By default the exception callback channel is closed. The client will need
	 * to communicate to job servers that exceptions should be forwarded to the
	 * client.
	 * 
	 * @param isOpen
	 *            If set to true, the exception callback channel is opened. If
	 *            set to false the exception callback channel is closed. The
	 *            default value is false.
	 */
	public void setExceptionChannel(boolean isOpen);

	/**
	 * Specifies the minimum amount of time this client should keep established,
	 * but inactive, connections alive.
	 * 
	 * @param milliseconds
	 *            The minimum amount of time, in milliseconds, to keep inactive
	 *            connections alive (Default: 5000).
	 * @throws IllegalArgumentException
	 *             If the parameter is less then zero
	 */
	public void setStayAlive(long milliseconds);
}
