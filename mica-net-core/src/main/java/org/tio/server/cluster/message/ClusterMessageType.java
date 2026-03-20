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

import org.tio.core.exception.TioDecodeException;

/**
 * 集群消息类型
 *
 * @author L.cm
 */
public enum ClusterMessageType {

	/**
	 * 心跳消息 ping
	 */
	PING((byte) 1),
	/**
	 * 心跳回复消息 pong
	 */
	PONG((byte) 2),
	/**
	 * 新节点加入
	 */
	JOIN((byte) 3),
	/**
	 * 数据
	 */
	DATA((byte) 4),
	/**
	 * 数据同步
	 */
	SYNC((byte) 7),
	/**
	 * 数据同步回复
	 */
	SYNC_ACK((byte) 8);

	private final byte type;

	ClusterMessageType(byte type) {
		this.type = type;
	}

	public byte getType() {
		return type;
	}

	/**
	 * Value of byte.
	 *
	 * @param value integer value
	 * @return command packet type enum
	 * @throws TioDecodeException Protocol decode exception
	 */
	public static ClusterMessageType from(final byte value) throws TioDecodeException {
		for (ClusterMessageType each : values()) {
			if (each.type == value) {
				return each;
			}
		}
		throw new TioDecodeException("Unsupported ClusterMessageType type:" + value);
	}

}
