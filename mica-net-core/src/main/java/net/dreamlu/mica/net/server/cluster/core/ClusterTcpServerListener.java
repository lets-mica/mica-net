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

import net.dreamlu.mica.net.core.ChannelContext;
import net.dreamlu.mica.net.server.intf.TioServerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群服务监听器
 *
 * @author L.cm
 */
public class ClusterTcpServerListener implements TioServerListener {
	private static final Logger log = LoggerFactory.getLogger(ClusterTcpServerListener.class);

	@Override
	public void onBeforeClose(ChannelContext context, Throwable throwable, String remark, boolean isRemove) throws Exception {
		if (throwable != null) {
			log.error(throwable.getMessage(), throwable);
		}
	}
}
