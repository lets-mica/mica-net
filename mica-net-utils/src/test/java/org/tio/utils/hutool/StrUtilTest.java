package org.tio.utils.hutool;

public class StrUtilTest {

	public static void main(String[] args) {
		for (int i = 0; i < 200; i++) {
			System.out.println(StrUtil.getUUId());
		}
		for (int i = 0; i < 200; i++) {
			System.out.println(StrUtil.getNanoId());
		}
	}

}
