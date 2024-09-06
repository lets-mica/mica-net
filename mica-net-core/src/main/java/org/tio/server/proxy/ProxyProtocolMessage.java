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

package org.tio.server.proxy;

/**
 * 代理协议消息
 *
 * @author L.cm
 */
public class ProxyProtocolMessage {

	/**
	 * 代理的协议，ipv4/6
	 */
	private final String protocol;
	/**
	 * 源地址
	 */
	private final String sourceAddress;
	/**
	 * 目标地址
	 */
	private final String destinationAddress;
	/**
	 * 源端口
	 */
	private final int sourcePort;
	/**
	 * 目标端口
	 */
	private final int destinationPort;

	public ProxyProtocolMessage(String protocol,
								String sourceAddress,
								String destinationAddress,
								String sourcePort,
								String destinationPort) {
		this(protocol, sourceAddress, destinationAddress, portStringToInt(sourcePort), portStringToInt(destinationPort));
	}

	public ProxyProtocolMessage(String protocol,
								String sourceAddress,
								String destinationAddress,
								int sourcePort,
								int destinationPort) {
		this.protocol = protocol;
		this.sourceAddress = sourceAddress;
		this.destinationAddress = destinationAddress;
		this.sourcePort = sourcePort;
		this.destinationPort = destinationPort;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getSourceAddress() {
		return sourceAddress;
	}

	public String getDestinationAddress() {
		return destinationAddress;
	}

	public int getSourcePort() {
		return sourcePort;
	}

	public int getDestinationPort() {
		return destinationPort;
	}

	/**
	 * Convert port to integer
	 *
	 * @param value the port
	 * @return port as an integer
	 * @throws IllegalArgumentException if port is not a valid integer
	 */
	private static int portStringToInt(String value) {
		int port;
		try {
			port = Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid port: " + value, e);
		}
		if (port <= 0 || port > 65535) {
			throw new IllegalArgumentException("invalid port: " + value + " (expected: 1 ~ 65535)");
		}
		return port;
	}

	@Override
	public String toString() {
		return "ProxyProtocolMessage{" +
			"message='" + protocol + '\'' +
			", sourceAddress='" + sourceAddress + '\'' +
			", destinationAddress='" + destinationAddress + '\'' +
			", sourcePort=" + sourcePort +
			", destinationPort=" + destinationPort +
			'}';
	}
}
