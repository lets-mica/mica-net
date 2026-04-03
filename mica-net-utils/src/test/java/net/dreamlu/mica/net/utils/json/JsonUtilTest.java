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
		User user = JsonUtil.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());
	}

}
