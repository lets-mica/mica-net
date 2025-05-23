package org.tio.utils.mica;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Digest 测试
 *
 * @author L.cm
 */
class HexUtilTest {

	@Test
	void test() {
		String text = "mica 最牛逼";
		String hex = "6d69636120e69c80e7899be980bc";
		String hexText = HexUtils.encodeToString(text);
		Assertions.assertEquals(hex, hexText);

		String decode = HexUtils.decodeToString(hexText);
		Assertions.assertEquals(text, decode);
		String decodeHex = HexUtils.decodeToString(hex);
		Assertions.assertEquals("mica 最牛逼", decodeHex);
	}

}
