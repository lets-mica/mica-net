/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & dreamlu.net).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.net.http.test.ws;

import net.dreamlu.mica.net.websocket.common.WsCloseCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WsCloseCode 校验方法语义测试，防止 isValid / isInvalid 的返回值再次反转。
 *
 * @author L.cm
 */
class WsCloseCodeTest {

	@Test
	void isValid_acceptsRfcAllowedCodes() {
		// RFC 6455 §7.4.1 允许出现在 Close 帧中的状态码
		assertTrue(WsCloseCode.isValid(1000));
		assertTrue(WsCloseCode.isValid(1001));
		assertTrue(WsCloseCode.isValid(1002));
		assertTrue(WsCloseCode.isValid(1003));
		assertTrue(WsCloseCode.isValid(1007));
		assertTrue(WsCloseCode.isValid(1008));
		assertTrue(WsCloseCode.isValid(1009));
		assertTrue(WsCloseCode.isValid(1011));
		assertTrue(WsCloseCode.isValid(1014));
		// 库/框架/应用与私有区间
		assertTrue(WsCloseCode.isValid(3000));
		assertTrue(WsCloseCode.isValid(4999));
	}

	@Test
	void isValid_rejectsReservedAndUndefinedCodes() {
		assertFalse(WsCloseCode.isValid(999));
		assertFalse(WsCloseCode.isValid(1004));
		assertFalse(WsCloseCode.isValid(1005));
		assertFalse(WsCloseCode.isValid(1006));
		assertFalse(WsCloseCode.isValid(1015));
		assertFalse(WsCloseCode.isValid(1016));
		assertFalse(WsCloseCode.isValid(2999));
		assertFalse(WsCloseCode.isValid(5000));
	}

	@Test
	void isInvalid_isTheOppositeOfIsValid() {
		int[] codes = {0, 999, 1000, 1003, 1004, 1005, 1006, 1007, 1014, 1015, 1016, 2999, 3000, 4999, 5000};
		for (int code : codes) {
			assertTrue(WsCloseCode.isInvalid(code) != WsCloseCode.isValid(code), "closeCode=" + code);
		}
	}
}
