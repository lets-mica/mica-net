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

package org.tio.http.common.sse;

import org.tio.core.ChannelContext;
import org.tio.core.Tio;
import org.tio.core.intf.EncodedPacket;
import org.tio.utils.SysConst;

import java.nio.charset.StandardCharsets;

/**
 * 分片的 sse 发射器
 *
 * @author L.cm
 */
public class SseChunkedEmitter implements SseEmitter<SseChunkedPacket> {
	private final ChannelContext context;

	public SseChunkedEmitter(ChannelContext context) {
		this.context = context;
	}

	@Override
	public void push(SseChunkedPacket packet) {
		byte[] chunkedBytes = packet.getChunkedBytes();
		EncodedPacket encodedPacket = new EncodedPacket(encodeChunk(chunkedBytes));
		Tio.send(context, encodedPacket);
	}

	private static byte[] encodeChunk(byte[] chunkData) {
		int length = chunkData.length;
		String chunkSize = Integer.toHexString(length);
		byte[] chunkSizeBytes = (chunkSize + SysConst.CRLF).getBytes(StandardCharsets.UTF_8);

		byte[] chunk = new byte[chunkSizeBytes.length + length + 2];

		System.arraycopy(chunkSizeBytes, 0, chunk, 0, chunkSizeBytes.length);
		System.arraycopy(chunkData, 0, chunk, chunkSizeBytes.length, length);
		System.arraycopy(SysConst.CR_LF, 0, chunk, chunkSizeBytes.length + length, 2);
		return chunk;
	}
}
