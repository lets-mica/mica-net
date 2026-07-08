package net.dreamlu.mica.net.http.test;

import net.dreamlu.mica.net.http.common.HttpConfig;
import net.dreamlu.mica.net.websocket.common.WsResponse;
import net.dreamlu.mica.net.websocket.server.WsTioServerHandler;
import net.dreamlu.mica.net.websocket.server.handler.IWsMsgHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * WsTioServerHandler tests.
 */
class WsTioServerHandlerTest {

	@Test
	void processRetObjUsesByteBufferRemainingBytes() throws Exception {
		WsTioServerHandler handler = new WsTioServerHandler(new HttpConfig(), new IWsMsgHandler() {
		});
		ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3, 4, 5});
		buffer.position(1);
		buffer.limit(4);

		WsResponse response = processRetObj(handler, buffer);

		assertArrayEquals(new byte[]{2, 3, 4}, response.getBody());
	}

	@Test
	void processRetObjAcceptsDirectByteBuffer() throws Exception {
		WsTioServerHandler handler = new WsTioServerHandler(new HttpConfig(), new IWsMsgHandler() {
		});
		ByteBuffer buffer = ByteBuffer.allocateDirect(4);
		buffer.put(new byte[]{6, 7, 8, 9});
		buffer.flip();
		buffer.get();

		WsResponse response = processRetObj(handler, buffer);

		assertArrayEquals(new byte[]{7, 8, 9}, response.getBody());
	}

	private static WsResponse processRetObj(WsTioServerHandler handler, ByteBuffer buffer) throws Exception {
		Method method = WsTioServerHandler.class.getDeclaredMethod("processRetObj", Object.class, String.class, net.dreamlu.mica.net.core.ChannelContext.class);
		method.setAccessible(true);
		return (WsResponse) method.invoke(handler, buffer, "test", null);
	}

}
