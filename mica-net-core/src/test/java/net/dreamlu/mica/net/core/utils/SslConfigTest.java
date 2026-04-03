package net.dreamlu.mica.net.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.dreamlu.mica.net.core.ssl.SslConfig;
import net.dreamlu.mica.net.utils.hutool.ResourceUtil;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
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

	@Test
	void testServer4() {
		SslConfig sslConfig = SslConfig.forClient("classpath:x.crt");
		Assertions.assertNotNull(sslConfig);
	}

	@Test
	void testServer5() throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(null, null);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		trustManagerFactory.init((KeyStore) null);
		KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
		TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		Assertions.assertNotNull(keyManagers);
		Assertions.assertNotNull(trustManagers);
	}

}
