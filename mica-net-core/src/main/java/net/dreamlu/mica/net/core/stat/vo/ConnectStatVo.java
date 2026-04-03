package net.dreamlu.mica.net.core.stat.vo;

/**
 * 连接统计
 *
 * @author L.cm
 */
public class ConnectStatVo {

	/**
	 * 共接受过连接数
	 */
	private long accepted;
	/**
	 * 当前连接数
	 */
	private long size;
	/**
	 * 关闭过的连接数
	 */
	private long closed;

	public long getAccepted() {
		return accepted;
	}

	public void setAccepted(long accepted) {
		this.accepted = accepted;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getClosed() {
		return closed;
	}

	public void setClosed(long closed) {
		this.closed = closed;
	}

	@Override
	public String toString() {
		return "ConnectStatVo{" +
			"accepted=" + accepted +
			", size=" + size +
			", closed=" + closed +
			'}';
	}
}
