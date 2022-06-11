package org.tio.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tio.core.ssl.SslConfig;

public class SslConfigTest {

	@Test
	public void test() throws Exception {
		SslConfig sslConfig = SslConfig.forClient();
		Assertions.assertNotNull(sslConfig);
	}

}
