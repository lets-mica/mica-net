package net.dreamlu.mica.net.core.intf;

/**
 * 网络通道抽象接口，承载发送 / 关闭等操作。
 *
 * @author L.cm
 */
public interface NetChannel {

	/**
	 * 回写业务包到对端，由具体实现负责编码 / 入队 / 落 socket。
	 *
	 * @param packet 业务包
	 * @return 是否成功
	 */
	boolean send(Packet packet);

	/**
	 * 关闭连接，释放资源。
	 *
	 * @param remark remark
	 */
	default void close(String remark) {
	}

}
