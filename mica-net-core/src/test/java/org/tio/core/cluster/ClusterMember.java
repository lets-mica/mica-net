package org.tio.core.cluster;

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
	/**
	 * 命名空间
	 */
	private String namespace;

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

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ClusterMember)) {
			return false;
		}
		ClusterMember that = (ClusterMember) o;
		return Objects.equals(id, that.id)
			&& Objects.equals(address, that.address)
			&& Objects.equals(namespace, that.namespace);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, address, namespace);
	}

	@Override
	public String toString() {
		return "ClusterMember{" +
			"id='" + id + '\'' +
			", alias='" + alias + '\'' +
			", address=" + address +
			", namespace='" + namespace + '\'' +
			'}';
	}
}
