package org.tio.core.cluster.core;

import org.tio.core.Node;

import java.util.List;

/**
 * 集群配置
 *
 * @author L.cm
 */
public class ClusterConfig {

	/**
	 * 集群端口
	 */
	private int port;
	/**
	 * 种子成员
	 */
	private List<Node> seedMembers;

}
