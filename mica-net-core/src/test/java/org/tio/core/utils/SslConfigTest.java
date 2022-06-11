package org.tio.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tio.core.ssl.SslConfig;

public class SslConfigTest {

	@Test
	public void testClient() throws Exception {
		SslConfig sslConfig = SslConfig.forClient();
		Assertions.assertNotNull(sslConfig);
	}

	@Test
	public void testServer() throws Exception {
		SslConfig sslConfig = SslConfig.forServer("classpath:test.jks", "classpath:test.jks", "501937");
		Assertions.assertNotNull(sslConfig);
	}

}
