package net.dreamlu.mica.net.utils.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * JsonUtil 测试
 *
 * @author L.cm
 */
class JsonUtilTest {

	@Test
	void testMapToBean() {
		String name = "张三";
		Map<String, Object> map = new HashMap<>();
		map.put("name", name);
		map.put("age", 18);
		JsonUtil.getJsonAdapter(new Jackson2JsonAdapter());
		User user = JsonUtil.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());
	}

	/**
	 * 按 classpath 探测出来的适配器必须能真正完成序列化。
	 *
	 * <p>回归点：编译期用的 jackson3 桩若残留在运行时 classpath 上，
	 * 会被误判为「用户已安装 jackson3」，进而选中一个只会返回 null 的空实现。
	 */
	@Test
	void testDetectedAdapter_shouldBeUsable() {
		JsonAdapter jsonAdapter = JsonUtil.getJsonAdapter(null);
		Map<String, Object> map = new HashMap<>();
		map.put("name", "张三");
		map.put("age", 18);
		Assertions.assertNotNull(jsonAdapter.toJsonString(map));
	}

}
