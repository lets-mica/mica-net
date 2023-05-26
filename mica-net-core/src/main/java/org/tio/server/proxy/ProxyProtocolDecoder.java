/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tio.server.proxy;

import org.tio.core.ChannelContext;
import org.tio.core.exception.TioDecodeException;

import java.nio.ByteBuffer;

/**
 * 开启 nginx 代理协议时需要开启，转发代理 ip 信息
 *
 * <p>
 * proxy_protocol on;
 * </p>
 *
 * @author L.cm
 */
public final class ProxyProtocolDecoder {
	/**
	 * Maximum possible length of a v1 proxy protocol header per spec
	 */
	private static final int V1_MAX_LENGTH = 108;

	/**
	 * @param buffer         ByteBuffer
	 * @param position       position
	 * @param readableLength readableLength
	 * @param context        ChannelContext
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	public static ProxyProtocolMessage decode(ByteBuffer buffer, int position, int readableLength, ChannelContext context) throws TioDecodeException {
		int endOfLine = findEndOfLine(buffer);
		if (endOfLine == -1) {
			if (readableLength > V1_MAX_LENGTH) {
				throw new TioDecodeException("Error v1 proxy protocol, readableLength: " + readableLength);
			}
			return null;
		}
		buffer.position(position);
		// PROXY TCP4 192.168.0.1 192.168.0.11 56324 443\r\n
		byte[] data = new byte[endOfLine];
		buffer.get(data);
		return null;
	}

	/**
	 * Returns the index in the buffer of the end of line found.
	 * Returns -1 if no end of line was found in the buffer.
	 */
	private static int findEndOfLine(final ByteBuffer buffer) {
		final int n = buffer.limit();
		for (int i = buffer.position(); i < n; i++) {
			final byte b = buffer.get(i);
			if (b == '\r' && i < n - 1 && buffer.get(i + 1) == '\n') {
				return i + 1;  // \r\n
			}
		}
		return -1;  // Not found.
	}

}
