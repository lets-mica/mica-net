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

package org.tio.core.task;

import org.tio.core.stat.ChannelStat;

/**
 * 心跳检测模式
 *
 * @author L.cm
 */
public enum HeartbeatMode {

	/**
	 * 最后的请求
	 */
	LAST_REQ {
		@Override
		public long getLastTime(ChannelStat stat) {
			return stat.latestTimeOfSentPacket;
		}
	},

	/**
	 * 最后的响应
	 */
	LAST_RESP {
		@Override
		public long getLastTime(ChannelStat stat) {
			return stat.latestTimeOfReceivedPacket;
		}
	},

	/**
	 * 请求或响应有一个在保活时间内就行
	 */
	ANY {
		@Override
		public long getLastTime(ChannelStat stat) {
			return Math.max(stat.latestTimeOfReceivedPacket, stat.latestTimeOfSentPacket);
		}
	},

	/**
	 * 请求和响应都得小于保活时间
	 */
	ALL {
		@Override
		public long getLastTime(ChannelStat stat) {
			return Math.min(stat.latestTimeOfReceivedPacket, stat.latestTimeOfSentPacket);
		}
	};

	/**
	 * 获取最后的时间
	 *
	 * @param stat ChannelStat
	 * @return 时间戳
	 */
	public abstract long getLastTime(ChannelStat stat);

}
