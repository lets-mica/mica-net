open module net.dreamlu.mica.net.core {
	requires transitive net.dreamlu.mica.net.utils;
	// core
	exports org.tio.core;
	exports org.tio.core.exception;
	exports org.tio.core.intf;
	exports org.tio.core.maintain;
	exports org.tio.core.ssl;
	exports org.tio.core.ssl.facade;
	exports org.tio.core.stat;
	exports org.tio.core.stat.vo;
	exports org.tio.core.task;
	exports org.tio.core.udp;
	exports org.tio.core.udp.intf;
	exports org.tio.core.udp.task;
	exports org.tio.core.utils;
	exports org.tio.core.uuid;
	// client
	exports org.tio.client;
	exports org.tio.client.intf;
	exports org.tio.client.task;
	// server
	exports org.tio.server;
	exports org.tio.server.cluster.codec;
	exports org.tio.server.cluster.core;
	exports org.tio.server.cluster.message;
	exports org.tio.server.intf;
	exports org.tio.server.proxy;
	exports org.tio.server.task;
}
