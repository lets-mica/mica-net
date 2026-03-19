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

import java.util.Map;

/**
 * 同步消息
 *
 * @author L.cm
 */
public class ClusterSyncMessage extends ClusterDataMessage {
	/**
	 * 消息Id
	 */
	private final long messageId;

	public ClusterSyncMessage(byte[] payload) {
		this(System.currentTimeMillis(), null, payload);
	}

	public ClusterSyncMessage(ClusterDataMessage dataMessage) {
		this(System.currentTimeMillis(), dataMessage.getHeaders(), dataMessage.getPayload());
	}

	public ClusterSyncMessage(long timestamp, Map<String, String> headers, byte[] payload) {
		super(timestamp, headers, payload);
		this.messageId = System.currentTimeMillis();
	}

	public ClusterSyncMessage(long messageId, long timestamp, Map<String, String> headers, byte[] payload) {
		super(timestamp, headers, payload);
		this.messageId = messageId;
	}

	public ClusterSyncMessage(long messageId, ClusterDataMessage dataMessage) {
		this(messageId, dataMessage.getTimestamp(), dataMessage.getHeaders(), dataMessage.getPayload());
	}

	/**
	 * 转换成 ack 消息
	 *
	 * @return ack 消息
	 */
	public ClusterSyncAckMessage toAckMessage() {
		return new ClusterSyncAckMessage(this.messageId);
	}

	public long getMessageId() {
		return messageId;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.SYNC;
	}

}
