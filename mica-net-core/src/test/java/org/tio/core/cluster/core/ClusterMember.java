package org.tio.core.cluster.core;

import org.tio.core.Node;

import java.io.Serializable;
import java.util.Objects;

/**
 * 集群成员
 *
 * @author L.cm
 */
public class ClusterMember implements Serializable {

	/**
	 * 成员 id
	 */
	private String id;
	/**
	 * 成员别名
	 */
	private String alias;
	/**
	 * 成员地址
	 */
	private Node address;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Node getAddress() {
		return address;
	}

	public void setAddress(Node address) {
		this.address = address;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ClusterMember that = (ClusterMember) o;
		return Objects.equals(id, that.id) && Objects.equals(alias, that.alias) && Objects.equals(address, that.address);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, alias, address);
	}

	@Override
	public String toString() {
		return "ClusterMember{" +
			"id='" + id + '\'' +
			", alias='" + alias + '\'' +
			", address=" + address +
			'}';
	}
}
