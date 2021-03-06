/**
 * JBoss, Home of Professional Open Source. Copyright 2011, Red Hat, Inc., and
 * individual
 * contributors as indicated by the @author tags. See the copyright.txt file in
 * the distribution
 * for a full listing of individual contributors.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either
 * version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.server.xnio3.async;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jboss.server.common.FileLoader;
import org.xnio.ChannelListener;
import org.xnio.channels.StreamChannel;

/**
 * {@code ReadChannelListener}
 * 
 * Created on Nov 22, 2011 at 4:44:01 PM
 * 
 * @author <a href="mailto:nbenothm@redhat.com">Nabil Benothman</a>
 */
public class ReadChannelListener implements ChannelListener<StreamChannel> {

	private String sessionId;
	private ByteBuffer readBuffer;
	private ByteBuffer writeBuffers[];
	private long fileLength;

	/**
	 * Create a new instance of {@code ReadChannelListener}
	 */
	public ReadChannelListener() {
		this.init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xnio.ChannelListener#handleEvent(java.nio.channels.Channel )
	 */
	public void handleEvent(StreamChannel channel) {
		try {
			int nBytes = channel.read(readBuffer);
			if (nBytes < 0) {
				// means that the connection was closed remotely
				channel.close();
				return;
			}

			if (nBytes > 0) {
				readBuffer.flip();
				byte bytes[] = new byte[nBytes];
				readBuffer.get(bytes);
				readBuffer.clear();
				writeResponse(channel);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param channel
	 * @throws Exception
	 */
	void writeResponse(StreamChannel channel) throws Exception {
		try {
			// Write the file content to the channel
			write(channel, writeBuffers, fileLength);
			// write(channel, writeBuffer);
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	/**
	 * 
	 * @param channel
	 * @param buffers
	 * @throws Exception
	 */
	protected void write(final StreamChannel channel, final ByteBuffer[] buffers, long total)
			throws Exception {

		for (ByteBuffer bb : buffers) {
			bb.flip();
		}

		long nw = 0, x = 0;

		while (nw < total) {
			// Wait until the channel becomes writable again
			channel.awaitWritable();
			x = channel.write(buffers);
			if (x < 0) {
				throw new IOException("Channel is closed");
			}
			nw += x;
		}
	}

	/**
	 * 
	 * @param channel
	 * @param buffer
	 * @throws IOException
	 */
	void write(StreamChannel channel, ByteBuffer byteBuffer) throws IOException {
		byteBuffer.flip();
		// Wait until the channel becomes writable again
		channel.awaitWritable();
		channel.write(byteBuffer);
	}

	/**
	 * Read the file from HD and initialize the write byte buffers array.
	 */
	private void init() {
		this.readBuffer = ByteBuffer.allocateDirect(512);
		this.writeBuffers = FileLoader.cloneData();
		this.fileLength = FileLoader.getFileLength();
	}

	/**
	 * Getter for sessionId
	 * 
	 * @return the sessionId
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Setter for the sessionId
	 * 
	 * @param sessionId
	 *            the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
}
