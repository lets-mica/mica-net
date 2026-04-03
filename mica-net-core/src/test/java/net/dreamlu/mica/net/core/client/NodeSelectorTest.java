package net.dreamlu.mica.net.core.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.dreamlu.mica.net.client.intf.NodeSelector;
import net.dreamlu.mica.net.core.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * NodeSelector Test
 *
 * @author L.cm
 */
class NodeSelectorTest {

	@Test
	void test1() {
		NodeSelector selector = new NodeSelector() {
		};
		List<Node> nodes = new ArrayList<>();
		Node current = null;
		for (int i = 0; i < 10; i++) {
			current = selector.select(nodes, current);
			System.out.println(current);
		}
		Assertions.assertNull(current);
	}

	@Test
	void test2() {
		NodeSelector selector = new NodeSelector() {
		};
		List<Node> nodes = new ArrayList<>();
		nodes.add(new Node("127.0.0.1", 3001));
		nodes.add(new Node("127.0.0.1", 3002));
		nodes.add(new Node("127.0.0.1", 3003));
		Node current = null;
		for (int i = 0; i < 100; i++) {
			current = selector.select(nodes, current);
			System.out.println(current);
		}
		Assertions.assertNotNull(current);
	}

	@Test
	void test3() {
		NodeSelector selector = new NodeSelector() {
		};
		List<Node> nodes = new ArrayList<>();
		nodes.add(new Node("127.0.0.1", 3001));
		Node current = null;
		for (int i = 0; i < 10; i++) {
			current = selector.select(nodes, current);
			System.out.println(current);
		}
		Assertions.assertNotNull(current);
	}

}
