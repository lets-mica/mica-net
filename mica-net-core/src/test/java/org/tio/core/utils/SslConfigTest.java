package org.tio.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tio.core.ssl.SslConfig;

class SslConfigTest {

	@Test
	void testClient() {
		SslConfig sslConfig = SslConfig.forClient();
		Assertions.assertNotNull(sslConfig);
	}

	@Test
	void testServer1() {
		SslConfig sslConfig = SslConfig.forServer("classpath:test.jks", "501937");
		Assertions.assertNotNull(sslConfig);
	}

	@Test
	void testServer2() {
		SslConfig sslConfig = SslConfig.forServer("classpath:generate.pfx", "12345678");
		Assertions.assertNotNull(sslConfig);
	}
}
