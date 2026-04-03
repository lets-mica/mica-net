package net.dreamlu.mica.net.core.stat.vo;

/**
 * 消息统计
 *
 * @author L.cm
 */
public class MessageStatVo {

	/**
	 * 处理的包
	 */
	private long handledPackets;
	/**
	 * 处理的消息字节数
	 */
	private long handledBytes;
	/**
	 * 接收的包
	 */
	private long receivedPackets;
	/**
	 * 接收的字节数
	 */
	private long receivedBytes;
	/**
	 * 发送的包
	 */
	private long sendPackets;
	/**
	 * 发送的字节数
	 */
	private long sendBytes;
	/**
	 * 平均每次TCP包接收的字节数
	 */
	private double bytesPerTcpReceive;
	/**
	 * 平均每次TCP包接收的业务包
	 */
	private double packetsPerTcpReceive;

	public long getHandledPackets() {
		return handledPackets;
	}

	public void setHandledPackets(long handledPackets) {
		this.handledPackets = handledPackets;
	}

	public long getHandledBytes() {
		return handledBytes;
	}

	public void setHandledBytes(long handledBytes) {
		this.handledBytes = handledBytes;
	}

	public long getReceivedPackets() {
		return receivedPackets;
	}

	public void setReceivedPackets(long receivedPackets) {
		this.receivedPackets = receivedPackets;
	}

	public long getReceivedBytes() {
		return receivedBytes;
	}

	public void setReceivedBytes(long receivedBytes) {
		this.receivedBytes = receivedBytes;
	}

	public long getSendPackets() {
		return sendPackets;
	}

	public void setSendPackets(long sendPackets) {
		this.sendPackets = sendPackets;
	}

	public long getSendBytes() {
		return sendBytes;
	}

	public void setSendBytes(long sendBytes) {
		this.sendBytes = sendBytes;
	}

	public double getBytesPerTcpReceive() {
		return bytesPerTcpReceive;
	}

	public void setBytesPerTcpReceive(double bytesPerTcpReceive) {
		this.bytesPerTcpReceive = bytesPerTcpReceive;
	}

	public double getPacketsPerTcpReceive() {
		return packetsPerTcpReceive;
	}

	public void setPacketsPerTcpReceive(double packetsPerTcpReceive) {
		this.packetsPerTcpReceive = packetsPerTcpReceive;
	}

	@Override
	public String toString() {
		return "MessageStatVo{" +
			"handledPackets=" + handledPackets +
			", handledBytes=" + handledBytes +
			", receivedPackets=" + receivedPackets +
			", receivedBytes=" + receivedBytes +
			", sendPackets=" + sendPackets +
			", sendBytes=" + sendBytes +
			", bytesPerTcpReceive=" + bytesPerTcpReceive +
			", packetsPerTcpReceive=" + packetsPerTcpReceive +
			'}';
	}
}
