open module net.dreamlu.mica.net.core {
	requires transitive net.dreamlu.mica.net.utils;
	// core
	exports net.dreamlu.mica.net.core;
	exports net.dreamlu.mica.net.core.exception;
	exports net.dreamlu.mica.net.core.intf;
	exports net.dreamlu.mica.net.core.maintain;
	exports net.dreamlu.mica.net.core.ssl;
	exports net.dreamlu.mica.net.core.ssl.facade;
	exports net.dreamlu.mica.net.core.stat;
	exports net.dreamlu.mica.net.core.stat.vo;
	exports net.dreamlu.mica.net.core.task;
	exports net.dreamlu.mica.net.core.tcp;
	exports net.dreamlu.mica.net.core.udp;
	exports net.dreamlu.mica.net.core.utils;
	exports net.dreamlu.mica.net.core.uuid;
	// client
	exports net.dreamlu.mica.net.client;
	exports net.dreamlu.mica.net.client.intf;
	exports net.dreamlu.mica.net.client.task;
	exports net.dreamlu.mica.net.client.udp;
	// server
	exports net.dreamlu.mica.net.server;
	exports net.dreamlu.mica.net.server.cluster.codec;
	exports net.dreamlu.mica.net.server.cluster.core;
	exports net.dreamlu.mica.net.server.cluster.message;
	exports net.dreamlu.mica.net.server.intf;
	exports net.dreamlu.mica.net.server.proxy;
	exports net.dreamlu.mica.net.server.task;
	exports net.dreamlu.mica.net.server.udp;
}
