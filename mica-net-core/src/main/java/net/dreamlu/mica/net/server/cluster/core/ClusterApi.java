/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.dreamlu.net).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.net.server.cluster.core;

import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.server.cluster.message.ClusterDataMessage;
import net.dreamlu.mica.net.server.cluster.message.ClusterSyncAckMessage;
import net.dreamlu.mica.net.utils.timer.TimerTask;

import java.util.Collection;
import java.util.concurrent.Executor;

/**
 * 集群接口
 *
 * @author L.cm
 */
public interface ClusterApi {

	/**
	 * 启动
	 */
	void start() throws Exception;

	/**
	 * 停止
	 */
	void stop();

	/**
	 * 发送消息
	 *
	 * @param member member
	 * @param data   集群消息
	 * @return 消息id.
	 */
	default boolean send(Node member, byte[] data) {
		return send(member, new ClusterDataMessage(data));
	}

	/**
	 * 发送消息
	 *
	 * @param member  member
	 * @param message 集群消息
	 * @return 消息id.
	 */
	boolean send(Node member, ClusterDataMessage message);

	/**
	 * 同步发送消息
	 *
	 * @param member Node
	 * @param data   集群消息
	 * @return 消息id.
	 */
	default ClusterSyncAckMessage sendSync(Node member, byte[] data) {
		return sendSync(member, new ClusterDataMessage(data));
	}

	/**
	 * 同步发送消息
	 *
	 * @param member  Node
	 * @param message 集群消息
	 * @return 消息id.
	 */
	ClusterSyncAckMessage sendSync(Node member, ClusterDataMessage message);

	/**
	 * 在集群中广播消息
	 *
	 * @param data 集群消息
	 */
	default void broadcast(byte[] data) {
		broadcast(new ClusterDataMessage(data));
	}

	/**
	 * 在集群中广播消息
	 *
	 * @param message 集群消息
	 */
	void broadcast(ClusterDataMessage message);

	/**
	 * 添加定时任务，注意：如果抛出异常，会终止后续任务，请自行处理异常
	 *
	 * @param command runnable
	 * @param delay   delay
	 * @return TimerTask
	 */
	TimerTask schedule(Runnable command, long delay);

	/**
	 * 添加定时任务，注意：如果抛出异常，会终止后续任务，请自行处理异常
	 *
	 * @param command  runnable
	 * @param delay    delay
	 * @param executor 用于自定义线程池，处理耗时业务
	 * @return TimerTask
	 */
	TimerTask schedule(Runnable command, long delay, Executor executor);

	/**
	 * 添加定时任务
	 *
	 * @param command runnable
	 * @param delay   delay
	 * @return TimerTask
	 */
	TimerTask scheduleOnce(Runnable command, long delay);

	/**
	 * 添加定时任务
	 *
	 * @param command  runnable
	 * @param delay    delay
	 * @param executor 用于自定义线程池，处理耗时业务
	 * @return TimerTask
	 */
	TimerTask scheduleOnce(Runnable command, long delay, Executor executor);

	/**
	 * 是后加入的成员
	 *
	 * @return 是否后加入的成员
	 */
	boolean isLateJoinMember();

	/**
	 * 获取种子成员
	 *
	 * @return 种子成员列表
	 */
	Collection<Node> getSeedMembers();

	/**
	 * 获取集群中的成员，不包含当前成员
	 *
	 * @return 成员列表，不包含自己
	 */
	Collection<Node> getRemoteMembers();

	/**
	 * 获取本地成员
	 *
	 * @return 本地成员
	 */
	Node getLocalMember();

}
