package net.dreamlu.mica.net.core;

import net.dreamlu.mica.net.core.intf.Packet;
import net.dreamlu.mica.net.core.intf.Packet.Meta;
import net.dreamlu.mica.net.core.intf.PacketListener;
import net.dreamlu.mica.net.core.intf.TioListener;
import net.dreamlu.mica.net.core.ssl.SslFacadeContext;
import net.dreamlu.mica.net.core.stat.ChannelStat;
import net.dreamlu.mica.net.core.task.AbstractDecodeRunnable;
import net.dreamlu.mica.net.core.task.AbstractSendRunnable;
import net.dreamlu.mica.net.core.task.HandlerRunnable;
import net.dreamlu.mica.net.utils.hutool.StrUtil;
import net.dreamlu.mica.net.utils.prop.MapPropSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author tanyaowu
 * 2017年10月19日 上午9:39:46
 */
public abstract class ChannelContext extends MapPropSupport {
	public static final String UNKNOWN_ADDRESS_IP = "$UNKNOWN";
	public static final AtomicInteger UNKNOWN_ADDRESS_PORT_SEQ = new AtomicInteger();
	private static final Logger log = LoggerFactory.getLogger(ChannelContext.class);
	// ⭐ 字段对齐优化：按照 JVM 内存对齐原则重新排列字段，减少内存填充
	// 1. 引用类型放在一起（8字节对齐）
	public final ReentrantReadWriteLock closeLock = new ReentrantReadWriteLock();
	public final ChannelStat stat = new ChannelStat();
	public final CloseMeta closeMeta = new CloseMeta();

	public TioConfig tioConfig;
	public AbstractDecodeRunnable decodeRunnable;
	public HandlerRunnable handlerRunnable;
	public AbstractSendRunnable sendRunnable;
	public SslFacadeContext sslFacadeContext;
	/**
	 * 此值不设时，心跳时间取org.tio.core.TioConfig.heartbeatTimeout
	 * 当然这个值如果小于org.tio.core.TioConfig.heartbeatTimeout，定时检查的时间间隔还是以org.tio.core.TioConfig.heartbeatTimeout为准，只是在判断时用此值
	 */
	public Long heartbeatTimeout;
	/**
	 * 一个packet所需要的字节数（用于应用告诉框架，下一次解码所需要的字节长度，省去冗余解码带来的性能损耗）
	 */
	public Integer packetNeededLength;
	private Node clientNode;
	/**
	 * 一些连接是代理的，譬如web服务器放在nginx后面，此时需要知道最原始的ip
	 */
	private Node proxyClientNode;
	private Node serverNode;
	/**
	 * 该连接在哪些组中
	 */
	private Set<String> groups;
	private String userId;
	private String token;
	private String bsId;

	// 2. 包装类型（可能为 null）
	private String id;
	/**
	 * 连接关闭的原因码
	 */
	private CloseCode closeCode = CloseCode.INIT_STATUS;
	/**
	 * 个性化readBufferSize
	 */
	private Integer readBufferSize;

	// 3. 原始类型放最后（1字节）
	/**
	 * 状态位，使用二进制标识位来判断状态
	 *
	 * <p>
	 * 0~2 位，配置状态 isVirtual(虚拟用于压测):0,isReconnect(重连):0logWhenDecodeError(解码出现异常时，是否打印异常日志):0
	 * 3~5 位，连接状态 isWaitingClose(等待关闭):0,isClosed(已关闭):1,isRemoved(已关闭):0
	 * 6~7 位，扩展状态 isAccepted(已接受,用于业务例如：mqtt):0,isBizStatus(业务自定义状态):0
	 * </p>
	 */
	private byte states = 0;

	/**
	 * ChannelContext
	 *
	 * @param tioConfig TioConfig
	 */
	public ChannelContext(TioConfig tioConfig) {
		super();
		this.id = tioConfig.getTioUuid().uuid();
		this.setTioConfig(tioConfig);
		tioConfig.ids.bind(this);
		// Removed channel assignment
		this.setLogWhenDecodeError(tioConfig.logWhenDecodeError);
		initOther();
		setUpSSL();
	}

	/**
	 * 创建一个虚拟ChannelContext，主要用来模拟一些操作，譬如压力测试，真实场景中用得少
	 *
	 * @param tioConfig TioConfig
	 * @param id        ChannelContext id
	 */
	public ChannelContext(TioConfig tioConfig, String id) {
		this.setVirtual(true);
		this.tioConfig = tioConfig;
		this.clientNode = new Node("127.0.0.1", 26254);
		this.id = id;
		if (StrUtil.isBlank(id)) {
			this.id = tioConfig.getTioUuid().uuid();
		}
		initOther();
	}

	public static Node createUnknownNode() {
		return new Node(UNKNOWN_ADDRESS_IP, UNKNOWN_ADDRESS_PORT_SEQ.incrementAndGet());
	}

	protected void assignAnUnknownClientNode() {
		setClientNode(createUnknownNode());
	}

	/**
	 * 设置 SSL/TLS
	 */
	public abstract void setUpSSL();

	public abstract boolean isUdp();

	/**
	 * 判断是否 ssl
	 *
	 * @return the ssl
	 */
	public boolean isSsl() {
		return sslFacadeContext != null;
	}

	/**
	 * @return the remoteNode
	 */
	public Node getClientNode() {
		return clientNode;
	}

	/**
	 * @param clientNode the clientNode to set
	 */
	public void setClientNode(Node clientNode) {
		if (!this.tioConfig.isShortConnection && this.clientNode != null) {
			tioConfig.clientNodes.remove(this);
		}
		this.clientNode = clientNode;
		if (this.tioConfig.isShortConnection) {
			return;
		}
		if (this.clientNode != null && !Objects.equals(UNKNOWN_ADDRESS_IP, this.clientNode.getIp())) {
			tioConfig.clientNodes.put(this);
		}
	}

	public Set<String> getGroups() {
		return groups;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the serverNode
	 */
	public Node getServerNode() {
		return serverNode;
	}

	/**
	 * @param serverNode the serverNode to set
	 */
	public void setServerNode(Node serverNode) {
		this.serverNode = serverNode;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	// Removed init(TioConfig, AsynchronousSocketChannel) - moved logic to constructor or subclass

	void initOther() {
		// 默认 closed 设置为 true
		setClosedState(true);
		if (!tioConfig.isShortConnection) {
			// 在长连接中，绑定群组几乎是必须要干的事，所以直接在初始化时给它赋值，省得在后面做同步处理
			groups = ConcurrentHashMap.newKeySet();
		}
		// SSL 设置已移至 TcpChannelContext（UDP 不支持 SSL/TLS）
	}

	/**
	 * @param packet        Packet
	 * @param isSentSuccess isSentSuccess
	 */
	public void processAfterSent(Packet packet, boolean isSentSuccess) {
		Meta meta = packet.getMeta();
		if (meta != null) {
			CountDownLatch countDownLatch = meta.getCountDownLatch();
			countDownLatch.countDown();
		}
		try {
			if (log.isDebugEnabled()) {
				log.debug("{} 已经发送 {}", this, packet.logstr());
			}
			//非SSL or SSL已经握手
			if (this.sslFacadeContext == null || this.sslFacadeContext.isHandshakeCompleted()) {
				TioListener tioListener = tioConfig.getTioListener();
				if (tioListener != null) {
					try {
						tioListener.onAfterSent(this, packet, isSentSuccess);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				}
				if (tioConfig.statOn) {
					tioConfig.groupStat.sentPackets.increment();
					stat.sentPackets.incrementAndGet();
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		PacketListener packetListener = packet.getPacketListener();
		if (packetListener != null) {
			try {
				packetListener.onAfterSent(this, packet, isSentSuccess);
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			}
		}

	}

	public boolean isVirtual() {
		return getState(0);
	}

	public void setVirtual(boolean virtual) {
		setState(0, virtual);
	}

	public boolean isReconnect() {
		return getState(1);
	}

	public void setReconnect(boolean isReconnect) {
		setState(1, isReconnect);
	}

	public boolean isLogWhenDecodeError() {
		return getState(2);
	}

	public void setLogWhenDecodeError(boolean logWhenDecodeError) {
		setState(2, logWhenDecodeError);
	}

	public boolean isWaitingClose() {
		return getState(3);
	}

	public void setWaitingClose(boolean waitingClose) {
		setState(3, waitingClose);
	}

	public boolean isClosed() {
		return getState(4);
	}

	/**
	 * @param isClosed the isClosed to set
	 */
	public void setClosed(boolean isClosed) {
		setClosedState(isClosed);
		if (isClosed && (clientNode == null || !UNKNOWN_ADDRESS_IP.equals(clientNode.getIp()))) {
			String before = this.toString();
			assignAnUnknownClientNode();
			log.info("关闭前{}, 关闭后{}", before, this);
		}
	}

	private void setClosedState(boolean isClosed) {
		setState(4, isClosed);
	}

	public boolean isRemoved() {
		return getState(5);
	}

	/**
	 * @param isRemoved the isRemoved to set
	 */
	public void setRemoved(boolean isRemoved) {
		setState(5, isRemoved);
	}

	public boolean isAccepted() {
		if (isClosed()) {
			return false;
		}
		return getState(6);
	}

	public void setAccepted(boolean accepted) {
		setState(6, accepted);
	}

	public boolean isBizStatus() {
		return getState(7);
	}

	public void setBizStatus(boolean bizStatus) {
		setState(7, bizStatus);
	}

	public void setPacketNeededLength(Integer packetNeededLength) {
		this.packetNeededLength = packetNeededLength;
	}

	public SslFacadeContext getSslFacadeContext() {
		return sslFacadeContext;
	}

	/**
	 * 设置 SslFacadeContext 用于动态 ssl 的过程
	 *
	 * @param sslFacadeContext sslFacadeContext
	 */
	public void setSslFacadeContext(SslFacadeContext sslFacadeContext) {
		this.sslFacadeContext = sslFacadeContext;
	}

	/**
	 * 获取 用户 id
	 *
	 * @return 用户 id
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userid to set
	 *               给框架内部用的，用户请勿调用此方法
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @return the bsId
	 */
	public String getBsId() {
		return bsId;
	}

	/**
	 * @param bsId the bsId to set
	 */
	public void setBsId(String bsId) {
		this.bsId = bsId;
	}

	public TioConfig getTioConfig() {
		return tioConfig;
	}

	/**
	 * 设置 TioConfig 并初始化协议相关的 Runnable（由子类实现）
	 *
	 * @param tioConfig the tioConfig to set
	 */
	protected abstract void setTioConfig(TioConfig tioConfig);

	/**
	 * 是否是服务器端
	 *
	 * @return boolean
	 */
	public abstract boolean isServer();

	/**
	 * @return the heartbeatTimeout
	 */
	public Long getHeartbeatTimeout() {
		return heartbeatTimeout;
	}

	/**
	 * @param heartbeatTimeout the heartbeatTimeout to set
	 */
	public void setHeartbeatTimeout(Long heartbeatTimeout) {
		this.heartbeatTimeout = heartbeatTimeout;
	}

	public int getReadBufferSize() {
		if (readBufferSize != null && readBufferSize > 0) {
			return readBufferSize;
		}
		return this.tioConfig.getReadBufferSize();
	}

	public void setReadBufferSize(int readBufferSize) {
		this.readBufferSize = Math.min(readBufferSize, TcpConst.MAX_DATA_LENGTH);
	}

	/**
	 * @return the proxyClientNode
	 */
	public Node getProxyClientNode() {
		return proxyClientNode;
	}

	/**
	 * @param proxyClientNode the proxyClientNode to set
	 */
	public void setProxyClientNode(Node proxyClientNode) {
		this.proxyClientNode = proxyClientNode;
	}

	public CloseCode getCloseCode() {
		return closeCode;
	}

	public void setCloseCode(CloseCode closeCode) {
		this.closeCode = closeCode;
	}

	/**
	 * 获取指定状态位的值
	 *
	 * @param position 0~7 8位
	 */
	private boolean getState(int position) {
		return (this.states & (1 << position)) != 0;
	}

	/**
	 * 设置指定状态位的值
	 *
	 * @param position 0~7 8位
	 * @param state    状态
	 */
	private void setState(int position, boolean state) {
		if (state) {
			// 使用或运算将指定位设置为1
			this.states |= (byte) (1 << position);
		} else {
			// 使用与运算将指定位设置为0
			this.states &= (byte) ~(1 << position);
		}
	}

	/**
	 * 发送消息
	 *
	 * @param packet Packet
	 * @return 发送结果
	 */
	public boolean send(Packet packet) {
		return Tio.send(this, packet);
	}

	/**
	 * 发送消息
	 *
	 * @param packet Packet
	 * @return 发送结果
	 */
	public boolean bSend(Packet packet) {
		return Tio.bSend(this, packet);
	}

	/**
	 * 获取解码队列目前消息数
	 *
	 * @return 数据量
	 */
	public int getDecodeQueueSize() {
		return this.decodeRunnable.getMsgQueueSize();
	}

	/**
	 * 获取处理队列目前消息数
	 *
	 * @return 数据量
	 */
	public int getHandlerQueueSize() {
		return this.handlerRunnable.getMsgQueueSize();
	}

	/**
	 * 获取发送队列目前消息数
	 *
	 * @return 数据量
	 */
	public int getSendQueueSize() {
		return this.sendRunnable.getMsgQueueSize();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ChannelContext other = (ChannelContext) obj;
		if (id == null) {
			return other.id == null;
		} else {
			return id.equals(other.id);
		}
	}

	@Override
	public int hashCode() {
		if (StrUtil.isNotBlank(id)) {
			return this.id.hashCode();
		} else {
			return super.hashCode();
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(64);
		if (serverNode != null) {
			sb.append("server:").append(serverNode);
		} else {
			sb.append("server:").append("NULL");
		}
		if (clientNode != null) {
			sb.append(", client:").append(clientNode);
		} else {
			sb.append(", client:").append("NULL");
		}
		if (this.isVirtual()) {
			sb.append(", virtual");
		}
		return sb.toString();
	}

	/**
	 * 连接关闭码
	 */
	public enum CloseCode {
		/**
		 * 没有提供原因码
		 */
		NO_CODE((byte) 1),
		/**
		 * 读异常
		 */
		READ_ERROR((byte) 2),
		/**
		 * 写异常
		 */
		WRITER_ERROR((byte) 3),
		/**
		 * 解码异常
		 */
		DECODE_ERROR((byte) 4),
		/**
		 * 通道未打开
		 */
		CHANNEL_NOT_OPEN((byte) 5),
		/**
		 * 读到的数据长度是0
		 */
		READ_COUNT_IS_ZERO((byte) 6),
		/**
		 * 对方关闭了连接
		 */
		CLOSED_BY_PEER((byte) 7),
		/**
		 * 读到的数据长度小于-1
		 */
		READ_COUNT_IS_NEGATIVE((byte) 8),
		/**
		 * 写数据长度小于0
		 */
		WRITE_COUNT_IS_NEGATIVE((byte) 9),
		/**
		 * 心跳超时
		 */
		HEARTBEAT_TIMEOUT((byte) 10),
		/**
		 * 连接失败
		 */
		CLIENT_CONNECTION_FAIL((byte) 80),

		/**
		 * SSL握手时发生异常
		 */
		SSL_ERROR_ON_HANDSHAKE((byte) 50),
		/**
		 * SSL session关闭了
		 */
		SSL_SESSION_CLOSED((byte) 51),
		/**
		 * SSL加密时发生异常
		 */
		SSL_ENCRYPTION_ERROR((byte) 52),
		/**
		 * SSL解密时发生异常
		 */
		SSL_DECRYPT_ERROR((byte) 53),

		/**
		 * 供用户使用
		 */
		USER_CODE_0((byte) 100),
		/**
		 * 供用户使用
		 */
		USER_CODE_1((byte) 101),
		/**
		 * 供用户使用
		 */
		USER_CODE_2((byte) 102),
		/**
		 * 供用户使用
		 */
		USER_CODE_3((byte) 103),
		/**
		 * 供用户使用
		 */
		USER_CODE_4((byte) 104),
		/**
		 * 供用户使用
		 */
		USER_CODE_5((byte) 105),
		/**
		 * 供用户使用
		 */
		USER_CODE_6((byte) 106),
		/**
		 * 供用户使用
		 */
		USER_CODE_7((byte) 107),
		/**
		 * 供用户使用
		 */
		USER_CODE_8((byte) 108),
		/**
		 * 供用户使用
		 */
		USER_CODE_9((byte) 109),
		/**
		 * 供用户使用
		 */
		USER_CODE_10((byte) 110),
		/**
		 * 初始值
		 */
		INIT_STATUS((byte) 199),
		/**
		 * 其它异常
		 */
		OTHER_ERROR((byte) 200),
		;

		final byte value;

		CloseCode(Byte value) {
			this.value = value;
		}

		public static CloseCode from(byte value) {
			CloseCode[] values = CloseCode.values();
			for (CloseCode v : values) {
				if (v.value == value) {
					return v;
				}
			}
			return null;
		}

		public byte getValue() {
			return value;
		}
	}

	public static class CloseMeta {
		public Throwable throwable;
		public String remark;
		public boolean isNeedRemove;

		public Throwable getThrowable() {
			return throwable;
		}

		public void setThrowable(Throwable throwable) {
			this.throwable = throwable;
		}

		public String getRemark() {
			return remark;
		}

		public void setRemark(String remark) {
			this.remark = remark;
		}

		public boolean isNeedRemove() {
			return isNeedRemove;
		}

		public void setNeedRemove(boolean isNeedRemove) {
			this.isNeedRemove = isNeedRemove;
		}
	}
}
