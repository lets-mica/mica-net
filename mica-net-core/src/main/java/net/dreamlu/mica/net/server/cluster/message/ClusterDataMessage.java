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

import java.util.Collections;
import java.util.Map;

/**
 * 集群 data 消息
 *
 * @author L.cm
 */
public class ClusterDataMessage extends AbsClusterMessage {

	/**
	 * 时间戳
	 */
	private final long timestamp;
	/**
	 * 头信息
	 */
	private final Map<String, String> headers;
	/**
	 * 消息数据
	 */
	private final byte[] payload;

	public ClusterDataMessage(byte[] payload) {
		this(System.currentTimeMillis(), null, payload);
	}

	public ClusterDataMessage(long timestamp, Map<String, String> headers, byte[] payload) {
		this.timestamp = timestamp;
		this.headers = headers;
		this.payload = payload;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public Map<String, String> getHeaders() {
		if (headers == null) {
			return Collections.emptyMap();
		} else {
			return Collections.unmodifiableMap(headers);
		}
	}

	public String getHeader(String name) {
		if (headers == null) {
			return null;
		} else {
			return headers.get(name);
		}
	}

	public byte[] getPayload() {
		return payload;
	}

	@Override
	public ClusterMessageType getMessageType() {
		return ClusterMessageType.DATA;
	}

}
