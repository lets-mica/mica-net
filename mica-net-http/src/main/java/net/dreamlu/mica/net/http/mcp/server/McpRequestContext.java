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

package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.mcp.schema.McpClientCapabilities;
import net.dreamlu.mica.net.http.mcp.schema.McpImplementation;
import net.dreamlu.mica.net.http.mcp.schema.McpSchema;

import java.util.Map;

/**
 * MCP 请求上下文。
 *
 * <p>2026-07-28+ 协议把协议版本、client 信息、client 能力从初始化握手迁移到了每个请求的 {@code _meta} 字段中。
 * transport 层解析后通过 {@link McpRequestHandler} 注入，handler 可直接读取。</p>
 *
 * <p>旧协议（2025-11-25 及更早）通过 {@link #getSession()} 拿到协议级 session；modern 协议下
 * session 为 {@code null}，所有跨调用状态由 server 颁发的显式 handle 维护。</p>
 *
 * @author L.cm
 */
public class McpRequestContext {
	/**
	 * 协商后的协议版本。
	 */
	private final String protocolVersion;
	/**
	 * client 实现信息（modern 协议）。
	 */
	private final McpImplementation clientInfo;
	/**
	 * client capabilities（modern 协议）。
	 */
	private final McpClientCapabilities clientCapabilities;
	/**
	 * 原始 _meta 字段，便于业务读取自定义扩展。
	 */
	private final Map<String, Object> meta;
	/**
	 * legacy session：仅 2025-11-25 及更早协议有值。
	 */
	private final McpServerSession session;

	public McpRequestContext(String protocolVersion,
	                         McpImplementation clientInfo,
	                         McpClientCapabilities clientCapabilities,
	                         Map<String, Object> meta,
	                         McpServerSession session) {
		this.protocolVersion = protocolVersion;
		this.clientInfo = clientInfo;
		this.clientCapabilities = clientCapabilities;
		this.meta = meta;
		this.session = session;
	}

	/**
	 * 是否为 modern 协议（2026-07-28+）。
	 *
	 * @return true 当且仅当协议版本 >= 2026-07-28
	 */
	public boolean isModern() {
		return McpSchema.MCP_2026_07_28.equals(protocolVersion);
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public McpImplementation getClientInfo() {
		return clientInfo;
	}

	public McpClientCapabilities getClientCapabilities() {
		return clientCapabilities;
	}

	public Map<String, Object> getMeta() {
		return meta;
	}

	public McpServerSession getSession() {
		return session;
	}
}