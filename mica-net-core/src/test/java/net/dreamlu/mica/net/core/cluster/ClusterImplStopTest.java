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

package net.dreamlu.mica.net.core.cluster;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.dreamlu.mica.net.server.cluster.core.ClusterApi;
import net.dreamlu.mica.net.server.cluster.core.ClusterConfig;
import net.dreamlu.mica.net.server.cluster.core.ClusterImpl;

/**
 * ClusterImpl 停止相关回归测试。
 * <p>
 * 覆盖三类场景：未启动直接停止、启动成功后重复停止、启动中途失败后停止。
 * 三者都不应抛出异常，且停止后端口等资源应被正常释放。
 *
 * @author L.cm
 */
class ClusterImplStopTest {

	/**
	 * 未调用 start() 直接 stop()，不应抛出 NullPointerException
	 */
	@Test
	void stopWithoutStartShouldNotThrow() throws IOException {
		ClusterApi cluster = new ClusterImpl(newClusterConfig(findFreePort()));
		Assertions.assertDoesNotThrow(cluster::stop);
	}

	/**
	 * start() 成功后连续 stop() 两次，第二次应为真正的 no-op，且不应抛异常
	 */
	@Test
	void stopTwiceAfterStartShouldNotThrow() throws Exception {
		int port = findFreePort();
		ClusterApi cluster = new ClusterImpl(newClusterConfig(port));
		cluster.start();
		Assertions.assertDoesNotThrow(cluster::stop);
		Assertions.assertDoesNotThrow(cluster::stop);
		// 停止后端口应被释放，可以重新绑定
		Assertions.assertDoesNotThrow(() -> {
			try (ServerSocket socket = new ServerSocket(port)) {
				Assertions.assertTrue(socket.isBound());
			}
		});
	}

	/**
	 * start() 中途失败（如端口被占用）后 stop()，此时服务端已创建、客户端尚未创建，不应抛出 NullPointerException
	 */
	@Test
	void stopAfterFailedStartShouldNotThrow() throws Exception {
		int port = findFreePort();
		try (ServerSocket occupied = new ServerSocket(port)) {
			ClusterApi cluster = new ClusterImpl(newClusterConfig(port));
			// 端口被占用，服务端启动失败
			Assertions.assertThrows(Exception.class, cluster::start);
			Assertions.assertDoesNotThrow(cluster::stop);
		}
	}

	/**
	 * stop() 把字段置为 null 后，集群仍可再次 start()，不应影响重启
	 */
	@Test
	void restartAfterStopShouldWork() throws Exception {
		int port = findFreePort();
		ClusterApi cluster = new ClusterImpl(newClusterConfig(port));
		cluster.start();
		cluster.stop();
		Assertions.assertDoesNotThrow(cluster::start);
		Assertions.assertDoesNotThrow(cluster::stop);
	}

	private static ClusterConfig newClusterConfig(int port) {
		ClusterConfig config = new ClusterConfig("127.0.0.1", port, message -> {
			// 测试中无需处理集群消息
		});
		// 只登记自己作为种子成员，避免 start() 时发起无意义的连接
		config.addSeedMember("127.0.0.1", port);
		return config;
	}

	private static int findFreePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

}
