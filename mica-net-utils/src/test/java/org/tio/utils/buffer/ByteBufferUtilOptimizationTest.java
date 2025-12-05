package org.tio.utils.buffer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ByteBufferUtil 优化测试
 */
class ByteBufferUtilOptimizationTest {

    private ByteBuffer buffer;

    @BeforeEach
    void setUp() {
        buffer = ByteBuffer.allocate(1024);
    }

    @Test
    @DisplayName("测试 readUnsignedShortLE 优化")
    void testReadUnsignedShortLE() {
        // 准备测试数据
        buffer.putShort((short) 0xABCD); // 43981
        buffer.flip();
        
        // 测试优化后的实现
        int result = ByteBufferUtil.readUnsignedShortLE(buffer);
        assertEquals(0xCDAB, result); // 小端序应该是 0xCDAB (52651)
    }

    @Test
    @DisplayName("测试 readUnsignedShortBE 优化")
    void testReadUnsignedShortBE() {
        // 准备测试数据
        buffer.putShort((short) 0xABCD); // 43981
        buffer.flip();
        
        // 测试优化后的实现
        int result = ByteBufferUtil.readUnsignedShortBE(buffer);
        assertEquals(0xABCD, result); // 大端序应该是 0xABCD (43981)
    }

    @Test
    @DisplayName("测试 writeShortLE 优化")
    void testWriteShortLE() {
        // 测试优化后的实现
        ByteBufferUtil.writeShortLE(buffer, 0xABCD);
        
        buffer.flip();
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        
        assertEquals((byte) 0xCD, b1); // 小端序低位在前
        assertEquals((byte) 0xAB, b2); // 小端序高位在后
    }

    @Test
    @DisplayName("测试 writeShortBE 优化")
    void testWriteShortBE() {
        // 测试优化后的实现
        ByteBufferUtil.writeShortBE(buffer, 0xABCD);
        
        buffer.flip();
        byte b1 = buffer.get();
        byte b2 = buffer.get();
        
        assertEquals((byte) 0xAB, b1); // 大端序高位在前
        assertEquals((byte) 0xCD, b2); // 大端序低位在后
    }

    @Test
    @DisplayName("测试 readUnsignedIntLE 优化")
    void testReadUnsignedIntLE() {
        // 准备测试数据
        buffer.putInt(0x12345678);
        buffer.flip();
        
        // 测试优化后的实现
        long result = ByteBufferUtil.readUnsignedIntLE(buffer);
        assertEquals(0x78563412L, result); // 小端序
    }

    @Test
    @DisplayName("测试 readUnsignedIntBE 优化")
    void testReadUnsignedIntBE() {
        // 准备测试数据
        buffer.putInt(0x12345678);
        buffer.flip();
        
        // 测试优化后的实现
        long result = ByteBufferUtil.readUnsignedIntBE(buffer);
        assertEquals(0x12345678L, result); // 大端序
    }

    @Test
    @DisplayName("测试 writeIntLE 优化")
    void testWriteIntLE() {
        // 测试优化后的实现
        ByteBufferUtil.writeIntLE(buffer, 0x12345678L);
        
        buffer.flip();
        assertEquals((byte) 0x78, buffer.get()); // 小端序低位在前
        assertEquals((byte) 0x56, buffer.get());
        assertEquals((byte) 0x34, buffer.get());
        assertEquals((byte) 0x12, buffer.get()); // 小端序高位在后
    }

    @Test
    @DisplayName("测试 writeIntBE 优化")
    void testWriteIntBE() {
        // 测试优化后的实现
        ByteBufferUtil.writeIntBE(buffer, 0x12345678L);
        
        buffer.flip();
        assertEquals((byte) 0x12, buffer.get()); // 大端序高位在前
        assertEquals((byte) 0x34, buffer.get());
        assertEquals((byte) 0x56, buffer.get());
        assertEquals((byte) 0x78, buffer.get()); // 大端序低位在后
    }

    @Test
    @DisplayName("测试 readUnsignedLongLE 优化")
    void testReadUnsignedLongLE() {
        // 准备测试数据
        buffer.putLong(0x123456789ABCDEF0L);
        buffer.flip();
        
        // 测试优化后的实现
        long result = ByteBufferUtil.readUnsignedLongLE(buffer);
        assertEquals(0xF0DEBC9A78563412L, result); // 小端序
    }

    @Test
    @DisplayName("测试 readUnsignedLongBE 优化")
    void testReadUnsignedLongBE() {
        // 准备测试数据
        buffer.putLong(0x123456789ABCDEF0L);
        buffer.flip();
        
        // 测试优化后的实现
        long result = ByteBufferUtil.readUnsignedLongBE(buffer);
        assertEquals(0x123456789ABCDEF0L, result); // 大端序
    }

    @Test
    @DisplayName("测试 writeLongLE 优化")
    void testWriteLongLE() {
        // 测试优化后的实现
        ByteBufferUtil.writeLongLE(buffer, 0x123456789ABCDEF0L);
        
        buffer.flip();
        assertEquals((byte) 0xF0, buffer.get()); // 小端序低位在前
        assertEquals((byte) 0xDE, buffer.get());
        assertEquals((byte) 0xBC, buffer.get());
        assertEquals((byte) 0x9A, buffer.get());
        assertEquals((byte) 0x78, buffer.get());
        assertEquals((byte) 0x56, buffer.get());
        assertEquals((byte) 0x34, buffer.get());
        assertEquals((byte) 0x12, buffer.get()); // 小端序高位在后
    }

    @Test
    @DisplayName("测试 writeLongBE 优化")
    void testWriteLongBE() {
        // 测试优化后的实现
        ByteBufferUtil.writeLongBE(buffer, 0x123456789ABCDEF0L);
        
        buffer.flip();
        assertEquals((byte) 0x12, buffer.get()); // 大端序高位在前
        assertEquals((byte) 0x34, buffer.get());
        assertEquals((byte) 0x56, buffer.get());
        assertEquals((byte) 0x78, buffer.get());
        assertEquals((byte) 0x9A, buffer.get());
        assertEquals((byte) 0xBC, buffer.get());
        assertEquals((byte) 0xDE, buffer.get());
        assertEquals((byte) 0xF0, buffer.get()); // 大端序低位在后
    }
}