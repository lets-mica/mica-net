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

package net.dreamlu.mica.net.utils.compression;

import net.dreamlu.mica.net.utils.hutool.ZipUtil;

/**
 * gzip 压缩
 *
 * @author L.cm
 */
public class GzipCompression implements Compression {
	private final int bufferSize;

	public GzipCompression() {
		this(ZipUtil.BUFFER_SIZE);
	}

	public GzipCompression(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	@Override
	public byte[] compress(byte[] buffer) {
		return ZipUtil.gzip(buffer);
	}

	@Override
	public byte[] decompress(byte[] buffer) {
		return ZipUtil.unGzip(buffer, bufferSize);
	}

}
