package org.tio.core.utils;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author talent.tan
 */
public class DecodeByteBufferList {
	private final int					listCapacity;
	private final List<ByteBuffer>	list;
	private int					byteSize		= 0;
	private int					listIndex		= 0;

	public DecodeByteBufferList(int listCapacity) {
		this.listCapacity = listCapacity;
		this.list = new ArrayList<>(this.listCapacity);
	}

	public DecodeByteBufferList() {
		this(10);
	}

	public List<ByteBuffer> getList() {
		return list;
	}

	/**
	 * 有多少个byte
	 * @return
	 * @author talent.tan
	 */
	public int getByteSize() {
		return byteSize;
	}

	/**
	 * 应用需要设置好limit和position
	 * @param byteBuffer
	 * @return
	 * @author talent.tan
	 */
	public DecodeByteBufferList add(ByteBuffer byteBuffer) {
		if (byteBuffer.remaining() == 0) {
			byteBuffer.position(0);
		}

		byteBuffer.mark();
		if (listCapacity == list.size()) {
			ByteBuffer old = toByteBuffer();
			old.mark();
			clearList();
			list.add(old);
		}

		byteSize += byteBuffer.remaining();
		list.add(byteBuffer);
		return this;
	}

	public ByteBuffer checkGet(int len) {
		check(len);
		return curr();
	}

	private void clearList() {
		list.clear();
		listIndex = 0;
	}

	private boolean check(int len) {
		ByteBuffer curr = curr();
		int remaining = curr.remaining();
		if (remaining >= len) {
			return true;
		}

		if (listIndex == list.size() - 1) {
			throw new BufferUnderflowException();
		}
		listIndex++;

		ByteBuffer next = list.get(listIndex);
		if (curr.hasRemaining()) {
			ByteBuffer next1 = ByteBufferUtils.composite(curr, next);
			next1.mark();
			curr.limit(curr.limit() - remaining);
			list.set(listIndex, next1);
		}
		return check(len);
	}

	/**
	 * 应用告之，已经解码成功
	 * 此时需要清空前面参与编码的
	 * @author talent.tan
	 */
	public void notifySuccess() {
		ByteBuffer curr = curr();
		int remaining = curr.remaining();
		int removeSize = listIndex + 1;
		if (remaining > 0) {
			curr.mark();
			removeSize--;
		}

		for (int i = 0; i < removeSize; i++) {
			ByteBuffer buf = list.get(0);
			buf.reset();
			byteSize -= buf.remaining();
			list.remove(0);
		}
		listIndex = 0;
	}

	/**
	 * 应用告之，已经解码失败，相当于半包
	 * 此时需要恢复各状态到编码前
	 * @author talent.tan
	 */
	public void notifyFail() {
		for (int i = 0; i < listIndex + 1; i++) {
			ByteBuffer buffer = list.get(i);
			buffer.reset();
		}
		listIndex = 0;
	}

	/**
	 * 应用告之，已经解码异常
	 *
	 * @author talent.tan
	 */
	public void notifyError() {
		clearList();
	}

	/**
	 * 应用告之，准备开始解码
	 *
	 * @author talent.tan
	 */
	public void notifyStart() {
		listIndex = 0;
	}

	private ByteBuffer curr() {
		return list.get(listIndex);
	}

	public ByteBuffer toByteBuffer() {
		ByteBuffer all = ByteBuffer.allocate(byteSize);
		for (ByteBuffer byteBuffer : list) {
			if (byteBuffer != null) {
				byteBuffer.position(0);
				all.put(byteBuffer);
			}
		}
		all.position(0);
		return all;
	}

	public byte get() {
		return checkGet(1).get();
	}

	public char getChar() {
		return checkGet(2).getChar();
	}

	public short getShort() {
		return checkGet(2).getShort();
	}

	public int getInt() {
		return checkGet(4).getInt();
	}

	public long getLong() {
		return checkGet(8).getLong();
	}

	public float getFloat() {
		return checkGet(4).getFloat();
	}

	public double getDouble() {
		return checkGet(8).getDouble();
	}

	public DecodeByteBufferList get(byte[] dst, int offset, int length) {
		checkGet(length).get(dst, offset, length);
		return this;
	}

	public DecodeByteBufferList get(byte[] dst) {
		checkGet(dst.length).get(dst);
		return this;
	}

	public DecodeByteBufferList skip(int len) {
		ByteBuffer buffer = checkGet(len);
		buffer.position(buffer.position() + len);
		return this;
	}

	public String readString(int length, Charset charset) {
		ByteBuffer buffer = checkGet(length);
		return ByteBufferUtils.readString(buffer, length, charset);
	}

}
