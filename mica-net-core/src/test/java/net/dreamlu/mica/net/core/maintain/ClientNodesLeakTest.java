/*
 * Copyright 2020 t-io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dreamlu.mica.net.core.maintain;

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.TioConfig;
import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.server.DefaultTioServerListener;
import net.dreamlu.mica.net.server.ServerChannelContext;
import net.dreamlu.mica.net.server.TioServer;
import net.dreamlu.mica.net.server.TioServerConfig;
import net.dreamlu.mica.net.server.intf.TioServerHandler;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证连接断开后 clientNodes 是否释放。
 */
public class ClientNodesLeakTest {

	@Test
	public void testClientNodesReleasedAfterDisconnect() throws Exception {
		TioServerConfig config = newServerConfig();
		TioServer server = new TioServer(0, config);
		server.start();
		int port = server.getServerSocketChannel() == null ? -1
			: ((InetSocketAddress) server.getServerSocketChannel().getLocalAddress()).getPort();

		int connCount = 5;
		List<Socket> sockets = new ArrayList<>();
		for (int i = 0; i < connCount; i++) {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress("127.0.0.1", port));
			sockets.add(socket);
		}

		waitFor(() -> config.connections.size() == connCount, 5000);
		System.out.println(">>> 连接建立后: connections=" + config.connections.size()
			+ ", clientNodes=" + config.clientNodes.size());

		for (Socket socket : sockets) {
			socket.close();
		}

		waitFor(() -> config.connections.isEmpty(), 5000);
		Thread.sleep(500);

		System.out.println(">>> 连接断开后: connections=" + config.connections.size()
			+ ", clientNodes=" + config.clientNodes.size());
		int clientNodesSize = config.clientNodes.size();

		// 顺带检查其它维护集合，确认没有第二处泄漏
		System.out.println(">>> 其它集合残留: ids=" + config.ids.getMap().size()
			+ ", users=" + config.users.size()
			+ ", groups=" + config.groups.size()
			+ ", tokens=" + config.tokens.size());
		int idsSize = config.ids.getMap().size();
		int usersSize = config.users.size();
		int groupsSize = config.groups.size();
		int tokensSize = config.tokens.size();

		server.stop();
		assertEquals(0, clientNodesSize, "连接断开后 clientNodes 仍有残留");
		assertEquals(0, idsSize, "连接断开后 ids 仍有残留");
		assertEquals(0, usersSize, "连接断开后 users 仍有残留");
		assertEquals(0, groupsSize, "连接断开后 groups 仍有残留");
		assertEquals(0, tokensSize, "连接断开后 tokens 仍有残留");
	}

	/**
	 * 死连接是否真的被回收？如果 clientNodes 仍持有 ChannelContext，弱引用不会被清空。
	 */
	@Test
	public void testDeadConnectionIsGarbageCollectable() throws Exception {
		TioServerConfig config = newServerConfig();
		TioServer server = new TioServer(0, config);
		server.start();
		int port = ((InetSocketAddress) server.getServerSocketChannel().getLocalAddress()).getPort();

		Socket socket = new Socket();
		socket.connect(new InetSocketAddress("127.0.0.1", port));
		waitFor(() -> config.connections.size() == 1, 5000);

		ChannelContext ctx = config.connections.iterator().next();
		WeakReference<ChannelContext> ref = new WeakReference<>(ctx);
		String clientIp = ctx.getClientNode().getIp();
		int clientPort = ctx.getClientNode().getPort();

		socket.close();
		waitFor(() -> config.connections.isEmpty(), 5000);
		Thread.sleep(500);

		// 断开后应当还能按 ip:port 找到「已死」的连接（未修复前的现状）
		ChannelContext found = Tio.getByClientNode(config, clientIp, clientPort);
		System.out.println(">>> 断开后按 ip:port 查找: " + (found == null ? "null" : "still there"));

		// 强制 GC，看死连接能否被回收
		ctx = null;
		boolean collected = false;
		for (int i = 0; i < 10 && !collected; i++) {
			System.gc();
			Thread.sleep(200);
			collected = ref.get() == null;
		}
		System.out.println(">>> 死连接是否可被 GC 回收: " + collected);

		server.stop();
		assertTrue(collected, "死连接仍被强引用，无法被 GC 回收（内存泄漏）");
	}

	/**
	 * 修复后，活的连接仍要能按 ip:port 查到 / 发消息 / 踢人。
	 */
	@Test
	public void testLiveConnectionStillReachable() throws Exception {
		TioServerConfig config = newServerConfig();
		TioServer server = new TioServer(0, config);
		server.start();
		int port = ((InetSocketAddress) server.getServerSocketChannel().getLocalAddress()).getPort();

		Socket socket = new Socket();
		socket.connect(new InetSocketAddress("127.0.0.1", port));
		waitFor(() -> config.connections.size() == 1, 5000);

		ChannelContext ctx = config.connections.iterator().next();
		String clientIp = ctx.getClientNode().getIp();
		int clientPort = ctx.getClientNode().getPort();

		// 活连接：必须能查到
		assertNotNull(Tio.getByClientNode(config, clientIp, clientPort), "活连接应能按 ip:port 查到");
		assertNotNull(Tio.getByClientNode(config, ctx.getClientNode()), "活连接应能按 Node 查到");
		System.out.println(">>> 活连接可查: ok, clientNodes=" + config.clientNodes.size());

		// 踢人（remove）后，clientNodes 应清空
		Tio.remove(config, clientIp, clientPort, null, "test", ChannelContext.CloseCode.NO_CODE);
		waitFor(() -> config.connections.isEmpty(), 5000);
		Thread.sleep(500);
		System.out.println(">>> 踢人后: connections=" + config.connections.size()
			+ ", clientNodes=" + config.clientNodes.size());
		int sizeAfterRemove = config.clientNodes.size();

		socket.close();
		server.stop();
		assertEquals(0, sizeAfterRemove, "踢人后 clientNodes 应清空");
		assertNull(Tio.getByClientNode(config, clientIp, clientPort), "踢人后不应再查到该连接");
	}

	/**
	 * 同一 ip:port 断开重连（端口复用）时，修复不能把新连接的索引误删。
	 * <p>
	 * 直接在 ClientNodes 层面模拟：旧连接 A 被同端口的新连接 B 覆盖后，A 的 remove 不能误删 B。
	 */
	@Test
	public void testSamePortReconnectNotBroken() throws Exception {
		TioServerConfig config = newServerConfig();

		ServerChannelContext ctxA = new ServerChannelContext(config, "ctx-a");
		ServerChannelContext ctxB = new ServerChannelContext(config, "ctx-b");
		Node node = new Node("127.0.0.1", 54321);

		ctxA.setClientNode(node);
		assertEquals(1, config.clientNodes.size());
		assertEquals(ctxA, Tio.getByClientNode(config, node));

		// 新连接复用了同一个 ip:port
		ctxB.setClientNode(node);
		assertEquals(1, config.clientNodes.size(), "同 ip:port 应只保留 1 条索引");
		assertEquals(ctxB, Tio.getByClientNode(config, node));

		// 旧连接断开时做 remove —— 不能误删新连接的索引
		config.clientNodes.remove(ctxA);
		assertEquals(1, config.clientNodes.size(), "旧连接 remove 不应误删新连接");
		assertEquals(ctxB, Tio.getByClientNode(config, node), "新连接应仍可按 ip:port 查到");

		// 新连接断开时做 remove —— 应被清掉
		config.clientNodes.remove(ctxB);
		assertEquals(0, config.clientNodes.size());
		assertNull(Tio.getByClientNode(config, node));
	}

	private static TioServerConfig newServerConfig() {
		return new TioServerConfig(new TioServerHandler() {
			@Override
			public Packet decode(ByteBuffer buffer, int limit, int position, int readableLength, ChannelContext channelContext) {
				return null;
			}

			@Override
			public ByteBuffer encode(Packet packet, TioConfig tioConfig, ChannelContext channelContext) {
				return null;
			}

			@Override
			public void handler(Packet packet, ChannelContext channelContext) {
			}
		}, new DefaultTioServerListener());
	}

	private static void waitFor(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		while (System.currentTimeMillis() < deadline) {
			if (condition.getAsBoolean()) {
				return;
			}
			Thread.sleep(50);
		}
	}

}
