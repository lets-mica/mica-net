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

package net.dreamlu.mica.net.server.cluster.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.server.cluster.message.ClusterDataMessage;

/**
 * ClusterImpl 未启动（或已停止）时调用集群能力的健壮性测试。
 * <p>
 * 集群客户端是 start() 的子步骤才创建的，未启动时调用 broadcast/schedule/addJoinMember
 * 均不应抛出 NullPointerException。
 *
 * @author L.cm
 */
class ClusterImplUnstartedTest {

	/**
	 * 未启动时广播，不应抛出 NullPointerException
	 */
	@Test
	void broadcastBeforeStartShouldNotThrow() throws IOException {
		ClusterApi cluster = new ClusterImpl(newClusterConfig());
		ClusterDataMessage message = new ClusterDataMessage("hello".getBytes(StandardCharsets.UTF_8));
		Assertions.assertDoesNotThrow(() -> cluster.broadcast(message));
	}

	/**
	 * 未启动时添加定时任务，应返回 null 而非抛出 NullPointerException
	 */
	@Test
	void scheduleBeforeStartShouldReturnNull() throws IOException {
		ClusterApi cluster = new ClusterImpl(newClusterConfig());
		Executor executor = Runnable::run;
		Assertions.assertNull(cluster.schedule(() -> {
		}, 1000));
		Assertions.assertNull(cluster.schedule(() -> {
		}, 1000, executor));
		Assertions.assertNull(cluster.scheduleOnce(() -> {
		}, 1000));
		Assertions.assertNull(cluster.scheduleOnce(() -> {
		}, 1000, executor));
	}

	/**
	 * 未启动时收到新成员 JOIN，不应抛出 NullPointerException，也不应把成员标记为已加入
	 */
	@Test
	void addJoinMemberBeforeStartShouldNotThrow() throws IOException {
		ClusterImpl cluster = new ClusterImpl(newClusterConfig());
		Node joinMember = new Node("127.0.0.1", 3999);
		Assertions.assertDoesNotThrow(() -> cluster.addJoinMember(joinMember));
		// 未建立连接，不应被记为已加入的成员
		Assertions.assertFalse(cluster.getRemoteMembers().contains(joinMember));
	}

	private static ClusterConfig newClusterConfig() throws IOException {
		int port;
		try (ServerSocket socket = new ServerSocket(0)) {
			port = socket.getLocalPort();
		}
		ClusterConfig config = new ClusterConfig("127.0.0.1", port, message -> {
			// 测试中无需处理集群消息
		});
		config.addSeedMember("127.0.0.1", port);
		return config;
	}

}
