package org.tio.core.cluster.core;

/**
 * 集群成员状态
 *
 * @author L.cm
 */
public enum ClusterMemberStatus {

	/**
	 * 存活
	 */
	ALIVE,
	/**
	 * 无法联系到会员并被标记为疑似失败。
	 */
	SUSPECT,
	/**
	 * 成员想要优雅地离开集群。
	 */
	LEAVING,
	/**
	 * 死亡，达到 {@link #SUSPECT} 配置的时间或当node已经优雅地关闭并离开集群。
	 */
	DEAD

}
