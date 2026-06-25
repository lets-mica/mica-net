package net.dreamlu.mica.net.http.mcp.server.transport;

import net.dreamlu.mica.net.http.mcp.server.McpServer;
import net.dreamlu.mica.net.http.mcp.server.McpServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE/Streamable HTTP 传输共享的会话管理器。
 *
 * <p>职责：
 * <ul>
 *   <li>维护 sessionId → McpServerSession 映射</li>
 *   <li>向 {@link McpServer} 中央注册表注册/注销</li>
 *   <li>周期性清理空闲 session</li>
 *   <li>周期性发送心跳（可选）</li>
 * </ul>
 *
 * @author L.cm
 */
public class SessionManager implements AutoCloseable {
	private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

	private final String name;
	private final McpServer mcpServer;
	private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();

	public SessionManager(String name, McpServer mcpServer) {
		this.name = name;
		this.mcpServer = mcpServer;
	}

	public McpServerSession createSession(String sessionId, net.dreamlu.mica.net.http.common.stream.HttpStream stream) {
		McpServerSession session = new McpServerSession(sessionId, stream);
		sessions.put(sessionId, session);
		mcpServer.registerSession(session);
		return session;
	}

	public McpServerSession get(String sessionId) {
		if (sessionId == null) {
			return null;
		}
		McpServerSession session = sessions.get(sessionId);
		return session != null && session.hasStream() ? session : null;
	}

	public void remove(String sessionId) {
		if (sessionId == null) {
			return;
		}
		McpServerSession session = sessions.remove(sessionId);
		if (session != null) {
			try {
				session.close();
			} catch (Exception ignore) {
			}
		}
		mcpServer.unregisterSession(sessionId);
	}

	public int size() {
		return sessions.size();
	}

	/**
	 * 发送心跳
	 */
	public void sendHeartbeat() {
		for (McpServerSession session : sessions.values()) {
			String sessionId = session.getSessionId();
			// 如果已经关闭，删除 session
			if (session.isClosed()) {
				sessions.remove(sessionId);
				mcpServer.unregisterSession(sessionId);
			} else {
				try {
					// 发送心跳
					session.sendHeartbeat();
				} catch (Exception e) {
					log.warn("[{}] sendHeartbeat failed for session {}", name, sessionId, e);
				}
			}
		}
	}

	@Override
	public void close() {
		for (McpServerSession session : sessions.values()) {
			try {
				session.close();
			} catch (Exception ignore) {
			}
		}
		for (String sessionId : sessions.keySet()) {
			mcpServer.unregisterSession(sessionId);
		}
		sessions.clear();
	}
}
