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

package org.tio.server;

import java.io.IOException;

/**
 * 抽象服务接口
 */
public interface IServer {

	/**
	 * 启动服务
	 *
	 * @param serverPort serverPort
	 * @throws IOException IOException
	 */
	default void start(int serverPort) throws IOException {
		start(null, serverPort);
	}

	/**
	 * 启动服务
	 *
	 * @param serverIp   serverIp
	 * @param serverPort serverPort
	 * @throws IOException IOException
	 */
	void start(String serverIp, int serverPort) throws IOException;

	/**
	 * 停止服务
	 *
	 * @return 是否停止成功
	 */
	boolean stop();

}
