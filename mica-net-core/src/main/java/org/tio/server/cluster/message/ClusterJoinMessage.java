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

package org.tio.server.cluster.message;

import org.tio.core.Node;

/**
 * 节点加入集群
 * <p>
 * 触发场景：后加入节点连上种子节点后，主动发送 JOIN 通知对方。<br>
 * 作用：让现有节点收到 JOIN 后主动建立返回连接，实现节点间双向通信。<br>
 * 拓扑说明：星形拓扑下，新节点作为 client 连上种子节点，种子节点收到 JOIN 后再作为 client 连回新节点，
 * 保证任意两节点间都有双向 TCP 连接，避免单向通信死锁。
 *
 * @author L.cm
 */
public class ClusterJoinMessage extends AbsClusterMessage {

	/**
	 * 加入的成员
	 */
	private final Node joinMember;

	public ClusterJoinMessage(Node joinMember) {
		this.joinMember = joinMember;
	}

	public Node getJoinMember() {
		return joinMember;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.JOIN;
	}

}
