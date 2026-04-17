open module net.dreamlu.mica.net.http {
	requires transitive net.dreamlu.mica.net.core;
	// http
	exports net.dreamlu.mica.net.http.common;
	exports net.dreamlu.mica.net.http.common.handler;
	exports net.dreamlu.mica.net.http.common.router;
	exports net.dreamlu.mica.net.http.common.sse;
	exports net.dreamlu.mica.net.http.common.stream;
	exports net.dreamlu.mica.net.http.common.utils;
	exports net.dreamlu.mica.net.http.jsonrpc;
	exports net.dreamlu.mica.net.http.mcp;
	exports net.dreamlu.mica.net.http.mcp.schema;
	exports net.dreamlu.mica.net.http.mcp.server;
	exports net.dreamlu.mica.net.http.mcp.util;
	exports net.dreamlu.mica.net.http.server;
	// websocket
	exports net.dreamlu.mica.net.websocket.common;
	exports net.dreamlu.mica.net.websocket.server;
	exports net.dreamlu.mica.net.websocket.server.handler;
}
