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

package net.dreamlu.mica.net.utils.hutool;

import org.junit.jupiter.api.Test;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DateUtil 单元测试
 *
 * @author L.cm
 */
class DateUtilTest {

	@Test
	void testGuessPatternNormDate() {
		// 标准 10 位 yyyy-MM-dd
		assertEquals(DatePattern.NORM_DATE_PATTERN, DateUtil.guessPattern("2024-05-20"));
	}

	@Test
	void testGuessPatternNormDateTime() {
		assertEquals(DatePattern.NORM_DATETIME_PATTERN, DateUtil.guessPattern("2024-05-20 10:30:45"));
	}

	@Test
	void testGuessPatternNormDateTimeMs() {
		// 注意：毫秒格式长度 ≥ NORM_DATETIME_MS_PATTERN.length() - 2 = 21
		assertEquals(DatePattern.NORM_DATETIME_MS_PATTERN, DateUtil.guessPattern("2024-05-20 10:30:45.123"));
	}

	@Test
	void testGuessPatternPureDateTime() {
		assertEquals(DatePattern.PURE_DATETIME_PATTERN, DateUtil.guessPattern("20240520103045"));
	}

	@Test
	void testGuessPatternPureDateTimeMs() {
		assertEquals(DatePattern.PURE_DATETIME_MS_PATTERN, DateUtil.guessPattern("20240520103045123"));
	}

	@Test
	void testGuessPatternPureDate() {
		assertEquals(DatePattern.PURE_DATE_PATTERN, DateUtil.guessPattern("20240520"));
	}

	@Test
	void testGuessPatternPureTime() {
		assertEquals(DatePattern.PURE_TIME_PATTERN, DateUtil.guessPattern("103045"));
	}

	@Test
	void testGuessPatternUnknownLength() {
		// 不匹配任何已知长度时返回 null
		assertNull(DateUtil.guessPattern("xx"));
	}

	@Test
	void testParseToDate() {
		Date date = DateUtil.parseToDate("2024-05-20 10:30:45");
		assertNotNull(date);

		Date normDate = DateUtil.parseToDate("2024-05-20");
		assertNotNull(normDate);

		assertNull(DateUtil.parseToDate("xx"));
	}

	@Test
	void testParseToSqlDate() {
		java.sql.Date sqlDate = DateUtil.parseToSqlDate("2024-05-20 10:30:45");
		assertNotNull(sqlDate);
		assertNull(DateUtil.parseToSqlDate("xx"));
	}

	@Test
	void testParseToTimestamp() {
		Timestamp timestamp = DateUtil.parseToTimestamp("2024-05-20 10:30:45");
		assertNotNull(timestamp);
		assertNull(DateUtil.parseToTimestamp("xx"));
	}

	@Test
	void testParseToTime() {
		Time time = DateUtil.parseToTime("2024-05-20 10:30:45");
		assertNotNull(time);
		assertNull(DateUtil.parseToTime("xx"));
	}

	@Test
	void testFormatDateTime() {
		LocalDateTime now = LocalDateTime.of(2024, 5, 20, 10, 30, 45);
		String formatted = DateUtil.formatDateTime(now);
		assertEquals("2024-05-20 10:30:45", formatted);

		assertNull(DateUtil.formatDateTime(null));
	}

	@Test
	void testHttpDate() {
		String httpDate = DateUtil.httpDate(0L);
		// 1970-01-01 GMT
		assertTrue(httpDate.contains("1970"));
		assertTrue(httpDate.endsWith("GMT"));

		LocalDateTime now = LocalDateTime.now();
		String formatted = DateUtil.httpDate(now.atZone(java.time.ZoneId.systemDefault()));
		assertTrue(formatted.endsWith("GMT"));
	}

	@Test
	void testDaysBetween() {
		Date date1 = new Date(0L);
		Date date2 = new Date(TimeUnit.DAYS.toMillis(5));
		assertEquals(5, DateUtil.daysBetween(date1, date2));
		assertEquals(-5, DateUtil.daysBetween(date2, date1));
		assertEquals(0, DateUtil.daysBetween(date1, date1));
	}

	@Test
	void testHttpDateFormatConstant() {
		// HTTP_DATE_FMT 应该是非空的
		assertNotNull(DateUtil.HTTP_DATE_FMT);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss 'GMT'", java.util.Locale.US);
		assertNotNull(formatter);
	}
}