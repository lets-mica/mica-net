package net.dreamlu.mica.net.http.test.mcp;

import net.dreamlu.mica.net.http.mcp.util.UriTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UriTemplate 单元测试。
 */
class UriTemplateTest {

	@Test
	void testBlankTemplateThrows() {
		assertThrows(IllegalArgumentException.class, () -> new UriTemplate(null));
		assertThrows(IllegalArgumentException.class, () -> new UriTemplate(""));
		assertThrows(IllegalArgumentException.class, () -> new UriTemplate("   "));
	}

	@Test
	void testUnclosedExpressionThrows() {
		assertThrows(IllegalArgumentException.class, () -> new UriTemplate("http://example.com/{foo"));
	}

	@Test
	void testEmptyExpressionThrows() {
		assertThrows(IllegalArgumentException.class, () -> new UriTemplate("http://example.com/{}"));
	}

	@Test
	void testLiteralOnlyTemplate() {
		UriTemplate t = new UriTemplate("file://readme");
		assertTrue(t.matchesTemplate("file://readme"));
		assertFalse(t.matchesTemplate("file://other"));
	}

	@Test
	void testSimpleVariable() {
		UriTemplate t = new UriTemplate("file://{name}");
		assertTrue(t.matchesTemplate("file://readme"));
		assertFalse(t.matchesTemplate("file://"));
	}

	@Test
	void testDotOperator() {
		UriTemplate t = new UriTemplate("file://{name}.txt");
		assertTrue(t.matchesTemplate("file://readme.txt"));
		assertFalse(t.matchesTemplate("file://readme"));
	}

	@Test
	void testPathOperator() {
		UriTemplate t = new UriTemplate("files{/path}");
		assertTrue(t.matchesTemplate("files/foo"));
		assertFalse(t.matchesTemplate("file/foo"));
	}

	@Test
	void testMultipleVariables() {
		UriTemplate t = new UriTemplate("users/{userId}/posts/{postId}");
		assertTrue(t.matchesTemplate("users/123/posts/456"));
		assertFalse(t.matchesTemplate("users/123/comments/456"));
	}

	@Test
	void testEqualsAndHashCode() {
		UriTemplate a = new UriTemplate("file://{name}");
		UriTemplate b = new UriTemplate("file://{name}");
		UriTemplate c = new UriTemplate("file://{other}");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}

	@Test
	void testGetTemplate() {
		UriTemplate t = new UriTemplate("file://{name}");
		assertEquals("file://{name}", t.getTemplate());
		assertEquals("file://{name}", t.toString());
	}

	@Test
	void testFragmentOperator() {
		UriTemplate t = new UriTemplate("page#{section}");
		assertTrue(t.matchesTemplate("page#intro"));
		assertTrue(t.matchesTemplate("page#abc/def"));
		assertTrue(t.matchesTemplate("page#abc#def"));
		assertFalse(t.matchesTemplate("page"));
	}

	@Test
	void testLeadingPlusOperator() {
		// + operator allows reserved chars; single segment
		UriTemplate t = new UriTemplate("X{+path}/here");
		assertTrue(t.matchesTemplate("Xfoo/here"));
		assertFalse(t.matchesTemplate("X/here"));
	}

	@Test
	void testExplodedPathOperator() {
		// exploded / allows comma-separated values within a single path segment
		UriTemplate t = new UriTemplate("files{/path*}");
		assertTrue(t.matchesTemplate("files/a,b,c"));
		assertFalse(t.matchesTemplate("files/a/b/c"));
	}
}