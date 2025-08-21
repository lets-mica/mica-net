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

package org.tio.utils.mica;

import java.util.*;

/**
 * 字符串模板解析
 *
 * @author L.cm
 */
public class StrTemplateParser {
	/**
	 * 固定文本序列
	 */
	private final List<String> literals;
	/**
	 * 变量名序列
	 */
	private final List<String> variables;

	/**
	 * 构造函数
	 *
	 * @param template 模板字符串，例如："abc${name}xx${like}cc"
	 */
	public StrTemplateParser(String template) {
		// 预估容量：通常一个模板不会有太多变量，预设为4是合理的
		this.literals = new ArrayList<>(4);
		this.variables = new ArrayList<>(4);
		parseTemplate(template);
	}

	/**
	 * 解析模板结构
	 * 示例输入："abc${name}xx${like}cc"
	 * 输出：
	 * literals = ["abc", "xx", "cc"]
	 * variables = ["name", "like"]
	 */
	private void parseTemplate(String template) {
		if (template == null || template.isEmpty()) {
			literals.add("");
			return;
		}

		int start = 0;
		int templateLength = template.length();

		while (start < templateLength) {
			int varStart = template.indexOf("${", start);
			if (varStart == -1) {
				break;
			}

			// 记录固定文本
			literals.add(template.substring(start, varStart));

			int varIdx = varStart + 2;
			int varEnd = template.indexOf('}', varIdx);
			if (varEnd == -1) {
				throw new IllegalArgumentException("模板格式错误：缺少结束符 '}'");
			}

			// 检查变量名是否为空
			if (varEnd == varIdx) {
				throw new IllegalArgumentException("模板格式错误：变量名为空");
			}

			// 记录变量名
			variables.add(template.substring(varIdx, varEnd));
			start = varEnd + 1;
		}

		// 添加末尾固定文本
		literals.add(template.substring(start));
	}

	/**
	 * 提取变量值
	 * 示例输入："abcDreamluxxHellocc"
	 * 输出：{name=Dreamlu, like=Hello}
	 *
	 * @param input 输入字符串
	 * @return 变量映射，解析失败时返回空Map
	 */
	public Map<String, String> getVariables(String input) {
		// 预检查：长度不匹配时快速失败
		if (input == null || literals.isEmpty()) {
			return Collections.emptyMap();
		}

		// 预估容量，减少HashMap扩容
		Map<String, String> result = new LinkedHashMap<>(variables.size());
		int currentPos;
		// 校验第一个固定文本
		String firstLiteral = literals.get(0);
		if (!input.startsWith(firstLiteral)) {
			return Collections.emptyMap();
		}
		currentPos = firstLiteral.length();

		// 优化：缓存literals.size()以避免重复计算
		int literalsSize = literals.size();

		// 遍历后续固定文本来提取变量
		for (int i = 1; i < literalsSize; i++) {
			String literal = literals.get(i);
			int literalPos = input.indexOf(literal, currentPos);

			if (literalPos == -1) {
				// 未找到固定文本
				return Collections.emptyMap();
			}

			// 提取变量值 - 优化：避免不必要的字符串创建
			if (literalPos > currentPos) {
				String varValue = input.substring(currentPos, literalPos);
				result.put(variables.get(i - 1), varValue);
			} else {
				// 空变量值
				result.put(variables.get(i - 1), "");
			}

			currentPos = literalPos + literal.length();
		}

		// 校验末尾是否匹配
		return (currentPos == input.length()) ? result : Collections.emptyMap();
	}

}
