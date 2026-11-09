package net.dreamlu.mica.net.utils.json;

import net.dreamlu.mica.net.utils.hutool.ClassUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * JsonAdapter 测试
 *
 * @author L.cm
 */
class JsonAdapterTest {
	private static final Jackson2JsonAdapter jackson2JsonAdapter = new Jackson2JsonAdapter();
	private static final FastJson2JsonAdapter fastJson2JsonAdapter = new FastJson2JsonAdapter();
	private static final GsonJsonAdapter gsonJsonAdapter = new GsonJsonAdapter();
	private static final FastJson1JsonAdapter fastJson1JsonAdapter = new FastJson1JsonAdapter();
	private static final HuToolJsonAdapter huToolJsonAdapter = new HuToolJsonAdapter();
	private static final Snack3JsonAdapter snack3JsonAdapter = new Snack3JsonAdapter();
	private static final Snack4JsonAdapter snack4JsonAdapter = new Snack4JsonAdapter();

	@Test
	void testIsValidJson() {
		String json = "{\"name\":\"张三\",\"age\":18}";
		Assertions.assertTrue(jackson2JsonAdapter.isValidJson(json));
		Assertions.assertTrue(fastJson2JsonAdapter.isValidJson(json));
		Assertions.assertTrue(gsonJsonAdapter.isValidJson(json));
		Assertions.assertTrue(fastJson1JsonAdapter.isValidJson(json));
		Assertions.assertTrue(huToolJsonAdapter.isValidJson(json));
		Assertions.assertTrue(snack3JsonAdapter.isValidJson(json));
		Assertions.assertTrue(snack4JsonAdapter.isValidJson(json));
	}

	@Test
	void testReadMap() {
		String json = "{\"name\":\"张三\",\"age\":18}";
		Map<String, Object> map;
		map = jackson2JsonAdapter.readMap(json, String.class, Object.class);
		Assertions.assertNotNull(map);
		map = fastJson2JsonAdapter.readMap(json, String.class, Object.class);
		Assertions.assertNotNull(map);
		map = gsonJsonAdapter.readMap(json, String.class, Object.class);
		Assertions.assertNotNull(map);
		map = fastJson1JsonAdapter.readMap(json, String.class, Object.class);
		Assertions.assertNotNull(map);
		map = huToolJsonAdapter.readMap(json, String.class, Object.class);
		Assertions.assertNotNull(map);
		map = snack3JsonAdapter.readMap(json, String.class, Object.class);
		Assertions.assertNotNull(map);
		map = snack4JsonAdapter.readMap(json, String.class, Object.class);
		Assertions.assertNotNull(map);
	}

	@Test
	void testMapToBean() {
		String name = "张三";
		Map<String, Object> map = new HashMap<>();
		map.put("name", name);
		map.put("age", 18);
		User user;
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

		user = snack4JsonAdapter.convertValue(map, User.class);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals(name, user.getName());
	}

	/**
	 * jackson3 是可选依赖，只有真实引入 jackson3 时才断言其适配器行为。
	 *
	 * <p>编译期用的 jackson3 桩（src/main/java17）不是真实实现，
	 * 且在 process-classes 阶段就被移出运行时 classpath，
	 * 因此不存在「桩在 classpath 上被误判成已安装 jackson3」的情况。
	 */
	@Test
	void testJackson3Adapter() {
		Assumptions.assumeTrue(ClassUtil.isPresent(Jackson3JsonAdapter.CLAZZ_NAME));
		Jackson3JsonAdapter jackson3JsonAdapter = new Jackson3JsonAdapter();
		String json = "{\"name\":\"张三\",\"age\":18}";
		Assertions.assertTrue(jackson3JsonAdapter.isValidJson(json));

		Map<String, Object> map = new HashMap<>();
		map.put("name", "张三");
		map.put("age", 18);
		User user = jackson3JsonAdapter.convertValue(map, User.class);
		Assertions.assertNotNull(user);
		Assertions.assertEquals(18, user.getAge());
		Assertions.assertEquals("张三", user.getName());
	}
}
