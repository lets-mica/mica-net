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

package net.dreamlu.mica.net.server.cluster.message;

/**
 * 集群 ping 消息
 *
 * @author L.cm
 */
public class ClusterPingMessage extends AbsClusterMessage {
	/**
	 * 实例
	 */
	public static final ClusterPingMessage INSTANCE = new ClusterPingMessage();

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.PING;
	}

}
