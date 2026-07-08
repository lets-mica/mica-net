package net.dreamlu.mica.net.http.test;

import net.dreamlu.mica.net.http.common.HttpConfig;
import net.dreamlu.mica.net.http.common.HttpRequestDecoder;
import net.dreamlu.mica.net.http.common.Method;
import net.dreamlu.mica.net.http.common.RequestLine;
import net.dreamlu.mica.net.utils.SysConst;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HttpRequestDecoder tests.
 */
class HttpRequestDecoderTest {

	@Test
	void decodeParamsPreservesEmptyValue() throws Exception {
		Map<String, Object[]> params = new HashMap<>();

		HttpRequestDecoder.decodeParams(params, "a=&b", SysConst.DEFAULT_CHARSET);

		assertArrayEquals(new String[]{""}, params.get("a"));
		assertArrayEquals(new String[]{null}, params.get("b"));
	}

	@Test
	void decodeParamsAllowsEqualsInValue() throws Exception {
		Map<String, Object[]> params = new HashMap<>();

		HttpRequestDecoder.decodeParams(params, "token=a=b%3Dc", SysConst.DEFAULT_CHARSET);

		assertArrayEquals(new String[]{"a=b=c"}, params.get("token"));
	}

	@Test
	void parseRequestLineSupportsQueryMethod() throws Exception {
		ByteBuffer buffer = ByteBuffer.wrap("QUERY /search?q=mica HTTP/1.1\r\n".getBytes(SysConst.DEFAULT_CHARSET));

		RequestLine requestLine = HttpRequestDecoder.parseRequestLine(buffer, new HttpConfig());

		assertEquals(Method.QUERY, requestLine.getMethod());
		assertEquals("/search", requestLine.getPath());
		assertEquals("q=mica", requestLine.getQueryString());
	}

}
