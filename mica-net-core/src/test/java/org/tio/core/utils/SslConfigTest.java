package org.tio.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tio.core.ssl.SslConfig;

class SslConfigTest {

	@Test
	void testClient() throws Exception {
		SslConfig sslConfig = SslConfig.forClient();
		Assertions.assertNotNull(sslConfig);
	}

	@Test
	void testServer() throws Exception {
		SslConfig sslConfig = SslConfig.forServer("classpath:test.jks", "classpath:test.jks", "501937");
		Assertions.assertNotNull(sslConfig);
	}

}
