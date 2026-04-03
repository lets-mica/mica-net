package net.dreamlu.mica.net.core.test;

import net.dreamlu.mica.net.client.*;
import net.dreamlu.mica.net.core.Node;
import net.dreamlu.mica.net.core.Tio;
import net.dreamlu.mica.net.core.intf.EncodedPacket;
import net.dreamlu.mica.net.utils.SysConst;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TcpClientTest {

	public static void main(String[] args) throws Exception {
		// 配置
		TioClientConfig config = new TioClientConfig(new TestTioClientHandler(), new DefaultTioClientListener());
		// 设置小一点，减少 dump 打印的无用字符
		config.setReadBufferSize(256);
		config.setReconnConf(new ReconnConf());
		TioClient tioClient = new TioClient(config);
		ClientChannelContext connect = tioClient.connect(new Node("3vs4299313.qicp.vip", 22372));

		// 休眠一秒
		Thread.sleep(1000L);
		// 1. 发送注册
		ByteBuffer buffer1 = ByteBuffer.allocate(11);
		buffer1.put("AdTest255".getBytes(StandardCharsets.UTF_8));
		buffer1.put(SysConst.CR);
		buffer1.put(SysConst.LF);
		Tio.send(connect, new EncodedPacket(buffer1.array()));

		// 示例定时上报消息
		tioClient.schedule(() -> {
			// AdReset
//				byte[] bytes = "AdReset101010101011111".getBytes(StandardCharsets.UTF_8);
//				ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 2);
//				buffer.put(bytes);
//				buffer.put(SysConst.CR);
//				buffer.put(SysConst.LF);

			// Action
//				byte[] bytes = "Action2551".getBytes(StandardCharsets.UTF_8);
//				ByteBuffer buffer = ByteBuffer.allocate(bytes.length + 2);
//				buffer.put(bytes);
//				buffer.put(SysConst.CR);
//				buffer.put(SysConst.LF);

			// 使用 Tio 发送数据
//				Tio.send(connect, new EncodedPacket(buffer.array()));
		}, 5000);

	}

}
