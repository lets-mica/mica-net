package net.dreamlu.mica.net.http.test;

import net.dreamlu.mica.net.http.common.HttpRequestDecoder;
import net.dreamlu.mica.net.utils.SysConst;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

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

}
