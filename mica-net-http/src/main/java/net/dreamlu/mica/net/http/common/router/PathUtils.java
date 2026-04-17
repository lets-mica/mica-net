package net.dreamlu.mica.net.http.common.router;

/**
 * 路径工具
 *
 * @author L.cm
 */
class PathUtils {

	/**
	 * 将路径按 '/' 分割为段数组，避免 String.split 的正则开销
	 *
	 * @param path 路径，如 "/api/v1/user"
	 * @return 段数组，如 ["api", "v1", "user"]
	 */
	static String[] splitPath(String path) {
		if (path == null || path.isEmpty() || "/".equals(path)) {
			return new String[0];
		}
		// 计算段数
		int segmentCount = 0;
		int len = path.length();
		int i = (path.charAt(0) == '/') ? 1 : 0;
		while (i < len) {
			int next = path.indexOf('/', i);
			if (next < 0) {
				segmentCount++;
				break;
			}
			if (next > i) {
				segmentCount++;
			}
			i = next + 1;
		}
		// 填充段数组
		String[] segments = new String[segmentCount];
		int idx = 0;
		i = (path.charAt(0) == '/') ? 1 : 0;
		while (i < len) {
			int next = path.indexOf('/', i);
			if (next < 0) {
				segments[idx++] = path.substring(i);
				break;
			}
			if (next > i) {
				segments[idx++] = path.substring(i, next);
			}
			i = next + 1;
		}
		return segments;
	}

	/**
	 * 判断段是否为路径参数（以 { 开头且以 } 结尾）
	 *
	 * @param segment 路径段
	 * @return 是否为路径参数
	 */
	static boolean isParam(String segment) {
		return segment.length() > 2 && segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}';
	}

	/**
	 * 提取路径参数名
	 *
	 * @param segment 路径段，如 "{id}"
	 * @return 参数名，如 "id"
	 */
	static String extractParamName(String segment) {
		return segment.substring(1, segment.length() - 1);
	}

	/**
	 * 判断段是否为通配符
	 *
	 * @param segment 路径段
	 * @return 是否为通配符
	 */
	static boolean isWildcard(String segment) {
		return "**".equals(segment);
	}

	/**
	 * 过滤器路径模式匹配
	 *
	 * @param pattern 模式，如 "/api/**"、"/user/*"、"/user/list"
	 * @param path    请求路径
	 * @return 是否匹配
	 */
	static boolean matchPattern(String pattern, String path) {
		if ("/**".equals(pattern)) {
			return true;
		}
		if (pattern.endsWith("/**")) {
			String prefix = pattern.substring(0, pattern.length() - 3);
			return path.startsWith(prefix + "/");
		}
		if (pattern.endsWith("/*")) {
			String prefix = pattern.substring(0, pattern.length() - 2);
			if (!path.startsWith(prefix + "/")) {
				return false;
			}
			return path.indexOf('/', prefix.length() + 1) < 0;
		}
		return pattern.equals(path);
	}

}
