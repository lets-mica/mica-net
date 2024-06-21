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
