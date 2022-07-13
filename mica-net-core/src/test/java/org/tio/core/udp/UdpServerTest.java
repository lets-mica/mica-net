package org.tio.core.udp;

import org.tio.core.Node;
import org.tio.core.udp.intf.UdpHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

public class UdpServerTest {

	/**
	 * @param args
	 * @author tanyaowu
	 */
	public static void main(String[] args) throws IOException {
		final AtomicLong count = new AtomicLong();
		UdpHandler udpHandler = new UdpHandler() {
			@Override
			public void handler(UdpPacket udpPacket, DatagramSocket datagramSocket) {
				byte[] data = udpPacket.getData();
				String msg = new String(data);
				Node remote = udpPacket.getRemote();
				long c = count.incrementAndGet();
				if (c % 10000 == 0) {
					String str = "【" + msg + "】 from " + remote;
					System.out.println(str);
				}
				String str = "【" + msg + "】 from " + remote;
				System.out.println(str);
				// 演示 start ---- 下面的代码仅作演示，如果两边要交互，那么两边都要开udpclient和udpserver
				int otherPartyPort = 8000;
				DatagramPacket datagramPacket = new DatagramPacket(data, data.length, new InetSocketAddress(remote.getIp(), otherPartyPort));
				try {
					datagramSocket.send(datagramPacket);
				} catch (Throwable e) {
					e.printStackTrace();
				}
				// 演示 end
			}
		};
		UdpServerConf udpServerConf = new UdpServerConf(3000, udpHandler, 5000);
		UdpServer udpServer = new UdpServer(udpServerConf);
		udpServer.start();
	}

}
