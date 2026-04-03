package net.dreamlu.mica.net.utils.hutool;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.function.Predicate;

/**
 * 网络工具
 *
 * @author looly
 * @author L.cm
 */
public class NetUtil {

	/**
	 * 获取网卡 ipv4
	 *
	 * @param interfaceName 网卡名
	 * @return InetAddress
	 */
	public static String getNetworkInterfaceIpV4(String interfaceName) {
		LinkedHashSet<InetAddress> addressList = getAddressList((networkInterface) -> networkInterface.getName().equalsIgnoreCase(interfaceName), (inetAddress) -> inetAddress instanceof Inet4Address);
		return addressList.isEmpty() ? null : addressList.iterator().next().getHostAddress();
	}

	/**
	 * 获取所有满足过滤条件的本地IP地址对象
	 *
	 * @param addressFilter          过滤器，null表示不过滤，获取所有地址
	 * @param networkInterfaceFilter 过滤器，null表示不过滤，获取所有网卡
	 * @return 过滤后的地址对象列表
	 */
	public static LinkedHashSet<InetAddress> getAddressList(Predicate<NetworkInterface> networkInterfaceFilter,
															Predicate<InetAddress> addressFilter) {
		Enumeration<NetworkInterface> networkInterfaces;
		try {
			networkInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			throw new IllegalStateException(e);
		}
		final LinkedHashSet<InetAddress> ipSet = new LinkedHashSet<>();
		while (networkInterfaces.hasMoreElements()) {
			final NetworkInterface networkInterface = networkInterfaces.nextElement();
			if (networkInterfaceFilter != null && !networkInterfaceFilter.test(networkInterface)) {
				continue;
			}
			final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
			while (inetAddresses.hasMoreElements()) {
				final InetAddress inetAddress = inetAddresses.nextElement();
				if (inetAddress != null && (null == addressFilter || addressFilter.test(inetAddress))) {
					ipSet.add(inetAddress);
				}
			}
		}
		return ipSet;
	}

}
