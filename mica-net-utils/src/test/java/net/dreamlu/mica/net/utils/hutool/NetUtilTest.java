package net.dreamlu.mica.net.utils.hutool;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * NetUtil 测试
 *
 * @author L.cm
 */
public class NetUtilTest {

	public static void main(String[] args) throws SocketException {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface networkInterface = interfaces.nextElement();
			// 获取接口名称
			String interfaceName = networkInterface.getName();
			String ipV4 = NetUtil.getNetworkInterfaceIpV4(interfaceName);
			if (StrUtil.isNotBlank(ipV4)) {
				System.out.println("网卡名称: " + interfaceName);
				System.out.println("显示名称: " + networkInterface.getDisplayName());
				System.out.println("IP地址: " + ipV4);
			}
		}
	}

}
