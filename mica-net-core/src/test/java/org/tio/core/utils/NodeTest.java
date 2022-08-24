package org.tio.core.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tio.core.Node;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class NodeTest {

	@Test
	void test() {
		ConcurrentMap<Node, Object> map = new ConcurrentHashMap<>();
		map.put(new Node("127.0.0.1", 1234), "abc");
		map.put(new Node("127.0.0.2", 2345), "bcd");
		Assertions.assertEquals("abc", map.get(new Node("127.0.0.1", 1234)));
		Assertions.assertEquals("bcd", map.get(new Node("127.0.0.2", 2345)));
	}

}
