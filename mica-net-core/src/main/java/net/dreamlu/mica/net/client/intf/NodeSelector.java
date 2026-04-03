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

package net.dreamlu.mica.net.client.intf;

import net.dreamlu.mica.net.core.Node;

import java.util.List;

/**
 * 节点选择器
 *
 * @author L.cm
 */
public interface NodeSelector {

	/**
	 * 选择节点
	 *
	 * @param nodes nodes
	 * @return node
	 */
	default Node select(List<Node> nodes) {
		return select(nodes, null);
	}

	/**
	 * 选择节点
	 *
	 * @param nodes   nodes
	 * @param current current Node
	 * @return node
	 */
	default Node select(List<Node> nodes, Node current) {
		if (nodes == null || nodes.isEmpty()) {
			return current;
		}
		// 如果当前节点为 null
		if (current == null) {
			return nodes.get(0);
		}
		int size = nodes.size();
		int index = nodes.indexOf(current);
		// 下一个节点位置
		int next = index >= size - 1 ? 0 : index + 1;
		return nodes.get(next);
	}

}
