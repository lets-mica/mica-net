package org.tio.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tio.core.ssl.SslConfig;
import org.tio.utils.hutool.ResourceUtil;

import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

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

	@Test
	void testServer3() throws Exception {
		// 创建一个新的KeyStore实例
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null, null); // 初始化空的KeyStore
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		InputStream certInputStream = ResourceUtil.getResourceAsStream("classpath:x.crt");
		X509Certificate cert = (X509Certificate) cf.generateCertificate(certInputStream);
		// 将证书添加到KeyStore中
		keyStore.setCertificateEntry("Certificate", cert);
		// 初始化TrustManagerFactory
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(keyStore);
		SslConfig sslConfig = new SslConfig(trustManagerFactory.getTrustManagers());
		Assertions.assertNotNull(sslConfig);
	}
}
