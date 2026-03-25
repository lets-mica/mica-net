open module net.dreamlu.mica.net.http {
	requires transitive net.dreamlu.mica.net.core;
	// http
	exports org.tio.http.common;
	exports org.tio.http.common.handler;
	exports org.tio.http.common.sse;
	exports org.tio.http.common.stream;
	exports org.tio.http.common.utils;
	exports org.tio.http.jsonrpc;
	exports org.tio.http.mcp;
	exports org.tio.http.mcp.schema;
	exports org.tio.http.mcp.server;
	exports org.tio.http.mcp.util;
	exports org.tio.http.server;
	// websocket
	exports org.tio.websocket.common;
	exports org.tio.websocket.server;
	exports org.tio.websocket.server.handler;
}
