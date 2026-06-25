package net.dreamlu.mica.net.http.mcp.server.transport;

import net.dreamlu.mica.net.http.mcp.server.McpServer;
import net.dreamlu.mica.net.http.mcp.server.McpServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
	private final ScheduledExecutorService scheduler;
	private final AtomicLong sessionTimeoutMs = new AtomicLong(McpTransport.DEFAULT_SESSION_TIMEOUT_MS);
	private final AtomicLong heartbeatIntervalMs = new AtomicLong(McpTransport.DEFAULT_HEARTBEAT_INTERVAL_MS);
	private volatile ScheduledFuture<?> cleanupTask;
	private volatile ScheduledFuture<?> heartbeatTask;

	public SessionManager(String name, McpServer mcpServer) {
		this.name = name;
		this.mcpServer = mcpServer;
		this.scheduler = Executors.newScheduledThreadPool(2, r -> {
			Thread t = new Thread(r, "mcp-" + name);
			t.setDaemon(true);
			return t;
		});
		startBackgroundTasks();
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

	public void sessionTimeout(long timeoutMs) {
		sessionTimeoutMs.set(timeoutMs <= 0 ? Long.MAX_VALUE : timeoutMs);
		startBackgroundTasks();
	}

	public long getSessionTimeout() {
		long v = sessionTimeoutMs.get();
		return v == Long.MAX_VALUE ? 0 : v;
	}

	public void heartbeatInterval(long intervalMs) {
		heartbeatIntervalMs.set(intervalMs <= 0 ? 0 : intervalMs);
		startBackgroundTasks();
	}

	public long getHeartbeatInterval() {
		return heartbeatIntervalMs.get();
	}

	public void sendHeartbeat() {
		for (McpServerSession session : sessions.values()) {
			try {
				session.sendHeartbeat();
			} catch (Exception e) {
				log.warn("[{}] sendHeartbeat failed for session {}", name, session.getSessionId(), e);
			}
		}
	}

	private void startBackgroundTasks() {
		// 取消旧任务
		if (cleanupTask != null) {
			cleanupTask.cancel(false);
			cleanupTask = null;
		}
		if (heartbeatTask != null) {
			heartbeatTask.cancel(false);
			heartbeatTask = null;
		}

		long timeout = sessionTimeoutMs.get();
		if (timeout > 0 && timeout != Long.MAX_VALUE) {
			long interval = Math.max(5_000L, timeout / 4);
			cleanupTask = scheduler.scheduleWithFixedDelay(this::cleanupIdleSessions,
				interval, interval, TimeUnit.MILLISECONDS);
		}

		long hb = heartbeatIntervalMs.get();
		if (hb > 0) {
			heartbeatTask = scheduler.scheduleWithFixedDelay(this::sendHeartbeat,
				hb, hb, TimeUnit.MILLISECONDS);
		}
	}

	private void cleanupIdleSessions() {
		try {
			long now = System.currentTimeMillis();
			long timeout = sessionTimeoutMs.get();
			int removed = 0;
			for (Map.Entry<String, McpServerSession> e : sessions.entrySet()) {
				McpServerSession session = e.getValue();
				if (now - session.getLastAccessTime() > timeout || !session.hasStream()) {
					if (sessions.remove(e.getKey(), session)) {
						try {
							session.close();
						} catch (Exception ignore) {
						}
						mcpServer.unregisterSession(session.getSessionId());
						removed++;
					}
				}
			}
			if (removed > 0) {
				log.debug("[{}] cleaned up {} idle session(s), remaining={}", name, removed, sessions.size());
			}
		} catch (Exception ex) {
			log.warn("[{}] cleanup error", name, ex);
		}
	}

	@Override
	public void close() {
		if (cleanupTask != null) {
			cleanupTask.cancel(false);
			cleanupTask = null;
		}
		if (heartbeatTask != null) {
			heartbeatTask.cancel(false);
			heartbeatTask = null;
		}
		scheduler.shutdownNow();
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
