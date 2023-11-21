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

package org.tio.server.intf;

import org.tio.core.ChannelContext;
import org.tio.core.exception.TioDecodeException;
import org.tio.core.intf.Packet;

import java.nio.ByteBuffer;

/**
 * decode 函数
 *
 * @author L.cm
 */
@FunctionalInterface
public interface DecoderFunction {

	/**
	 * 解码函数
	 *
	 * @param buffer         ByteBuffer
	 * @param readableLength readableLength
	 * @param context        ChannelContext
	 * @return ProxyProtocolMessage
	 * @throws TioDecodeException TioDecodeException
	 */
	Packet apply(ByteBuffer buffer, int readableLength, ChannelContext context) throws TioDecodeException;

}
