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

package net.dreamlu.mica.net.server.proxy;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.intf.IgnorePacket;

import java.nio.ByteBuffer;

/**
 * 代理头预解析器（用于在 SSL/业务解码前累积并解析代理头）。
 * <p>
 * 封装了"累积 → 半包判定 → 解析 → 错误处理"的完整流程，调用方只需按 {@link Result} 路由数据即可。
 * 典型用法见 {@link net.dreamlu.mica.net.core.ReadCompletionHandler}：在 I/O 读回调入口处先
 * 用本解析器处理代理头，解析成功后再把剩余数据喂给 SSL 或业务解码器。
 * </p>
 * <p>
 * 线程不安全，仅在 I/O 线程中使用。
 * </p>
 *
 * @author L.cm
 */
public class ProxyProtocolPreParser {
	/** 默认初始缓冲区大小（V1 最大 108 字节，V2 含 TLV 可达 64K+，4096 覆盖绝大多数场景） */
	public static final int DEFAULT_INITIAL_SIZE = 4096;
	/** 默认最大缓冲区大小（64KB），超过此尺寸仍无法解析则回退 */
	public static final int DEFAULT_MAX_SIZE = 64 * 1024;

	private final ChannelContext context;
	private final int initialSize;
	private final int maxSize;
	private ByteBuffer buffer;

	public ProxyProtocolPreParser(ChannelContext context) {
		this(context, DEFAULT_INITIAL_SIZE, DEFAULT_MAX_SIZE);
	}

	public ProxyProtocolPreParser(ChannelContext context, int initialSize, int maxSize) {
		this.context = context;
		this.initialSize = initialSize;
		this.maxSize = maxSize;
	}

	/**
	 * 判断是否仍在累积（半包等待中）。
	 */
	public boolean isActive() {
		return buffer != null;
	}

	/**
	 * 喂入新数据并尝试解析代理头。
	 *
	 * @param newData 新到的数据（方法会消费其中能装入累积缓冲区的部分）
	 * @return 解析结果，调用方根据 {@link Result#state} 决定路由
	 */
	public Result feed(ByteBuffer newData) {
		// 1) 累积
		if (buffer == null) {
			buffer = ByteBuffer.allocate(initialSize);
		}
		int toCopy = Math.min(newData.remaining(), buffer.remaining());
		if (toCopy < newData.remaining()) {
			// 缓冲区装不下当前数据
			int needed = buffer.position() + newData.remaining();
			if (needed > maxSize) {
				// 累积超过最大尺寸仍无法识别为 PROXY 头，回退（合并所有数据）
				ByteBuffer combined = combineBuffers(buffer, newData);
				buffer = null;
				ProxyProtocolDecoder.removeProxyProtocol(context);
				return new Result(State.NOT_PROXY, combined, null);
			}
			// 扩大缓冲区
			int newCapacity = Math.min(Math.max(buffer.capacity() * 2, needed), maxSize);
			ByteBuffer newBuf = ByteBuffer.allocate(newCapacity);
			buffer.flip();
			newBuf.put(buffer);
			buffer = newBuf;
			toCopy = Math.min(newData.remaining(), buffer.remaining());
		}
		if (toCopy > 0) {
			int oldLimit = newData.limit();
			newData.limit(newData.position() + toCopy);
			buffer.put(newData);
			newData.limit(oldLimit);
		}

		// 2) 准备解析
		buffer.flip();
		int readableLength = buffer.remaining();

		// 至少需要 V1_MIN_HEAD_LENGTH(6) 字节才能判定
		if (readableLength < 6) {
			buffer.position(buffer.limit());
			buffer.limit(buffer.capacity());
			return Result.NEED_MORE_INSTANCE;
		}

		// 3) 复制一份用于解析（不破坏 buffer 的回退可能）
		ByteBuffer proxyBuf = ByteBuffer.allocate(readableLength);
		proxyBuf.put(buffer);
		proxyBuf.flip();
		int startPos = proxyBuf.position();

		try {
			// 用 no-op next，解析成功时不会触发后续业务解码；剩余数据由调用方处理
			ProxyProtocolDecoder.decode(context, proxyBuf, readableLength,
				(c, b, l) -> IgnorePacket.INSTANCE);
		} catch (Exception e) {
			// 解析异常，合并所有数据并回退
			buffer.position(buffer.limit());
			buffer.limit(buffer.capacity());
			ByteBuffer combined = combineBuffers(buffer, newData);
			buffer = null;
			ProxyProtocolDecoder.removeProxyProtocol(context);
			return new Result(State.ERROR, combined, e);
		}

		boolean stillEnabled = ProxyProtocolDecoder.isProxyProtocolEnabled(context);
		int consumed = proxyBuf.position() - startPos;

		if (stillEnabled) {
			// 半包（V1 \r\n 未到达等）
			buffer.position(buffer.limit());
			buffer.limit(buffer.capacity());
			return Result.NEED_MORE_INSTANCE;
		}

		// 4) 代理头处理完成
		buffer = null;

		if (consumed == 0) {
			// 不是 PROXY 头（decode 读到非 "PROXY " 前缀后会 remove key + reset buffer）
			// 用全部累积数据回退
			proxyBuf.position(0);
			proxyBuf.limit(readableLength);
			return new Result(State.NOT_PROXY, proxyBuf, null);
		}

		// PROXY 头解析成功，剩余数据交给调用方
		if (proxyBuf.hasRemaining()) {
			ByteBuffer remaining = ByteBuffer.allocate(proxyBuf.remaining());
			remaining.put(proxyBuf);
			remaining.flip();
			return new Result(State.PARSED, remaining, null);
		}
		// 首批数据刚好是完整代理头，无剩余
		return new Result(State.PARSED, ByteBuffer.allocate(0), null);
	}

	/**
	 * 合并累积缓冲区和当前 newData 剩余数据。
	 */
	private ByteBuffer combineBuffers(ByteBuffer accumulated, ByteBuffer newData) {
		accumulated.flip();
		int len1 = accumulated.remaining();
		int len2 = newData.remaining();
		ByteBuffer combined = ByteBuffer.allocate(len1 + len2);
		combined.put(accumulated);
		if (len2 > 0) {
			combined.put(newData);
		}
		combined.flip();
		return combined;
	}

	/**
	 * 预解析状态。
	 */
	public enum State {
		/** 半包，需要更多数据 */
		NEED_MORE,
		/** 代理头解析成功，data 是代理头之后的剩余数据（可能为空） */
		PARSED,
		/** 不是代理头，data 是全部数据 */
		NOT_PROXY,
		/** 解析异常，data 是全部数据，error 是异常 */
		ERROR
	}

	/**
	 * 预解析结果。
	 */
	public static class Result {
		/** 单例 NEED_MORE 结果，避免重复创建 */
		private static final Result NEED_MORE_INSTANCE = new Result(State.NEED_MORE, null, null);

		public final State state;
		/** 待路由的数据（NEED_MORE 时为 null） */
		public final ByteBuffer data;
		/** 异常（仅 ERROR 时有值） */
		public final Throwable error;

		private Result(State state, ByteBuffer data, Throwable error) {
			this.state = state;
			this.data = data;
			this.error = error;
		}
	}
}
