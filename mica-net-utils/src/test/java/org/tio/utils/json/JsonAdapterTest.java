package org.tio.utils.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * JsonAdapter 测试
 *
 * @author L.cm
 */
class JsonAdapterTest {
	private static final Jackson3JsonAdapter jackson3JsonAdapter = new Jackson3JsonAdapter();
	private static final Jackson2JsonAdapter jackson2JsonAdapter = new Jackson2JsonAdapter();
	private static final FastJson2JsonAdapter fastJson2JsonAdapter = new FastJson2JsonAdapter();
	private static final GsonJsonAdapter gsonJsonAdapter = new GsonJsonAdapter();
	private static final FastJson1JsonAdapter fastJson1JsonAdapter = new FastJson1JsonAdapter();
	private static final HuToolJsonAdapter huToolJsonAdapter = new HuToolJsonAdapter();
	private static final Snack3JsonAdapter snack3JsonAdapter = new Snack3JsonAdapter();

	@Test
	void testMapToBean() {
		String name = "张三";
		Map<String, Object> map = new HashMap<>();
		map.put("name", name);
		map.put("age", 18);
		User user = jackson3JsonAdapter.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());

		user = jackson2JsonAdapter.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());

		user = fastJson2JsonAdapter.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());

		user = gsonJsonAdapter.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());

		user = fastJson1JsonAdapter.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());

		user = huToolJsonAdapter.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());

		user = snack3JsonAdapter.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());
	}
}
