/*
 * Copyright (c) 2012, Isaiah van der Elst (isaiah.v@comcast.net)
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *   
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.gearman.impl.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.gearman.impl.core.GearmanConnection;

public class GearmanUtils {
	
	public boolean testJDBCConnection(String jdbcDriver, String jdbcConnectionString) {
		return false;
	}
	
	public boolean testGearmanConnection(InetSocketAddress address) {
		return false;
	}
	
	public static final String toString(GearmanConnection<?> conn) {
		return "["+conn.getHostAddress() + ":" + conn.getPort() +"]";
	}
	
	public static final byte[] createUID() {
		return UUID.randomUUID().toString().getBytes();
	}
	
	public static final URL[] createUrlClasspath(String classpath) {
		String[] paths = classpath.split(File.pathSeparator);
		
		List<URL> urls = new LinkedList<URL>();
		for(String path : paths) {
			if(path.isEmpty()) continue;
			if(path.endsWith("*")) {
				// jar wildcard
				
				FileFilter jarFilter = new JarFileFilter();
				
				String newPath = path.substring(0, path.lastIndexOf(File.separatorChar));
				File file = new File(newPath);
				if(file.isDirectory()) {
					File[] jars = file.listFiles(jarFilter);
					if(jars==null) continue;
					
					for(File jar : file.listFiles(jarFilter)) {
						addURL(urls, jar);
					}
				}
			} else {
				addURL(urls, new File(path));
			}
		}
		
		return urls.toArray(new URL[urls.size()]);
	}
	
	private final static void addURL(List<URL> urls, File file) {

		try {
			file = new File(file.getCanonicalPath());
			urls.add(file.toURI().toURL());
		} catch (IOException e) {
			// skip this path
		}
	}
	
	private static final class JarFileFilter implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			if(pathname.isDirectory()) return false;
			
			String name = pathname.getName();
			int size = name.length();
			String extension = name.substring(size-4, size);
			
			return extension.equalsIgnoreCase(".jar");
		}
		
	}
}
 