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

package net.dreamlu.mica.net.utils.mica;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExceptionUtils 单元测试
 *
 * @author L.cm
 */
class ExceptionUtilsTest {

	@Test
	void testUncheckedRuntimeException() {
		IllegalArgumentException ex = new IllegalArgumentException("test");
		RuntimeException result = ExceptionUtils.unchecked(ex);
		// IllegalArgumentException 会被包装到新的 IllegalArgumentException 中
		assertNotSame(ex, result);
		assertSame(ex, result.getCause());
	}

	@Test
	void testUncheckedIllegalAccessException() {
		IllegalAccessException ex = new IllegalAccessException("denied");
		RuntimeException result = ExceptionUtils.unchecked(ex);
		assertTrue(result instanceof IllegalArgumentException);
		assertSame(ex, result.getCause());
	}

	@Test
	void testUncheckedCheckedException() {
		SQLException ex = new SQLException("db error");
		// 非 RuntimeException 会被原样抛出（runtime() 借助泛型擦除绕过 checked 异常声明）
		assertThrows(SQLException.class, () -> ExceptionUtils.unchecked(ex));
	}

	@Test
	void testUncheckedInvocationTargetException() {
		IllegalArgumentException rootCause = new IllegalArgumentException("root");
		InvocationTargetException ex = new InvocationTargetException(rootCause);
		// InvocationTargetException 会被解包为目标异常再抛出
		RuntimeException thrown = assertThrows(RuntimeException.class, () -> ExceptionUtils.unchecked(ex));
		assertSame(rootCause, thrown);
	}

	@Test
	void testUnwrapInvocationTargetException() {
		Exception rootCause = new RuntimeException("root");
		InvocationTargetException wrapped = new InvocationTargetException(rootCause);
		Throwable unwrapped = ExceptionUtils.unwrap(wrapped);
		assertSame(rootCause, unwrapped);
	}

	@Test
	void testUnwrapUndeclaredThrowableException() {
		Exception rootCause = new RuntimeException("root");
		UndeclaredThrowableException wrapped = new UndeclaredThrowableException(rootCause);
		Throwable unwrapped = ExceptionUtils.unwrap(wrapped);
		assertSame(rootCause, unwrapped);
	}

	@Test
	void testUnwrapNestedWrappers() {
		Exception rootCause = new RuntimeException("root");
		InvocationTargetException ite = new InvocationTargetException(rootCause);
		UndeclaredThrowableException ute = new UndeclaredThrowableException(ite);
		Throwable unwrapped = ExceptionUtils.unwrap(ute);
		assertSame(rootCause, unwrapped);
	}

	@Test
	void testUnwrapPlainException() {
		RuntimeException ex = new RuntimeException("plain");
		assertSame(ex, ExceptionUtils.unwrap(ex));
	}
}