package net.dreamlu.mica.net.http.mcp.server;

import net.dreamlu.mica.net.http.mcp.schema.McpReadResourceRequest;
import net.dreamlu.mica.net.http.mcp.schema.McpReadResourceResult;
import net.dreamlu.mica.net.http.mcp.schema.McpResource;

import java.util.function.BiFunction;

/**
 * 资源定义。
 *
 * <p>与 {@link McpResourceTemplateSpecification} 保持一致,采用不可变设计,
 * 通过 {@link #of(McpResource, BiFunction)} 或带参构造器创建实例。</p>
 *
 * @author L.cm
 */
public class McpResourceSpecification {
	private final McpResource resource;
	private final BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler;

	public McpResourceSpecification() {
		this(null, null);
	}

	public McpResourceSpecification(McpResource resource,
	                                 BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		this.resource = resource;
		this.readHandler = readHandler;
	}

	/**
	 * 静态工厂方法,语义与 {@link #McpResourceSpecification(McpResource, BiFunction)} 相同。
	 *
	 * @param resource     resource 定义
	 * @param readHandler  resource 读 handler
	 * @return McpResourceSpecification
	 */
	public static McpResourceSpecification of(McpResource resource,
	                                           BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> readHandler) {
		return new McpResourceSpecification(resource, readHandler);
	}

	public McpResource getResource() {
		return resource;
	}

	public BiFunction<McpServerSession, McpReadResourceRequest, McpReadResourceResult> getReadHandler() {
		return readHandler;
	}
}
