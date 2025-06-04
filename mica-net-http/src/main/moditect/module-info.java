open module net.dreamlu.mica.net.http {
	requires transitive net.dreamlu.mica.net.core;
	// http
	exports org.tio.http.common;
	exports org.tio.http.common.handler;
	exports org.tio.http.common.jsonrpc;
	exports org.tio.http.common.mcp;
	exports org.tio.http.common.mcp.schema;
	exports org.tio.http.common.mcp.server;
	exports org.tio.http.common.mcp.util;
	exports org.tio.http.common.sse;
	exports org.tio.http.common.utils;
	exports org.tio.http.server;
	// websocket
	exports org.tio.websocket.common;
	exports org.tio.websocket.server;
	exports org.tio.websocket.server.handler;
}
