package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.mcp.schema.McpReadResourceRequest;
import net.dreamlu.mica.net.http.mcp.schema.McpReadResourceResult;
import net.dreamlu.mica.net.http.mcp.schema.McpResourceTemplate;
import net.dreamlu.mica.net.http.mcp.util.UriTemplate;

import java.util.function.BiFunction;

/**
 * mcp resource template 定义
 *
 * <p>在构造时即预编译 {@link UriTemplate},避免 {@code resources/read} 时
 * 反复解析模板与编译 regex。</p>
 *
 * @author L.cm
 */
public class McpResourceTemplateSpecification {
	private final McpResourceTemplate resource;
	private final BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler;
	private final UriTemplate uriTemplate;

	public McpResourceTemplateSpecification(McpResourceTemplate resource,
	                                        BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.resource = resource;
		this.readHandler = readHandler;
		String uriTemplateStr = resource == null ? null : resource.getUriTemplate();
		this.uriTemplate = uriTemplateStr == null || uriTemplateStr.isEmpty() ? null : new UriTemplate(uriTemplateStr);
	}

	public McpResourceTemplate getResource() {
		return resource;
	}

	public BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> getReadHandler() {
		return readHandler;
	}

	/**
	 * 获取预编译的 {@link UriTemplate},可能为 {@code null}(当模板 uriTemplate 为空时)。
	 *
	 * @return UriTemplate or null
	 */
	public UriTemplate getUriTemplate() {
		return uriTemplate;
	}
}
