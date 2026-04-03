package net.dreamlu.mica.net.utils.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 内存映射文件抽象
 *
 * @author leon、L.cm
 */
public abstract class Mapped implements Closeable {
	protected static final int DATA_FILENAME_MAX_LENGTH = 19;
	private static final Logger log = LoggerFactory.getLogger(Mapped.class);
	protected static Consumer<MappedByteBuffer> buffCleaner;

	static {
		// java 9+ Unsafe 有 invokeCleaner
		try {
			Class<?> unsafeCls = Class.forName("sun.misc.Unsafe");
			Field field = unsafeCls.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			Object unsafe = field.get(null);
			Method method = unsafeCls.getMethod("invokeCleaner", ByteBuffer.class);
			buffCleaner = buffer -> {
				try {
					method.invoke(unsafe, buffer);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			};
		} catch (Exception e) {
			log.debug(e.getMessage(), e);
		}
		// java8 使用 DirectByteBuffer 上的 cleaner
		if (buffCleaner == null) {
			ByteBuffer direct = ByteBuffer.allocateDirect(0);
			try {
				Field cleanerField = direct.getClass().getDeclaredField("cleaner");
				cleanerField.setAccessible(true);
				final Object cleaner = cleanerField.get(direct);
				Method cleanMethod = cleaner.getClass().getDeclaredMethod("clean");
				cleanMethod.invoke(cleaner);
				buffCleaner = buffer -> {
					try {
						final Object bufferCleaner = cleanerField.get(buffer);
						cleanMethod.invoke(bufferCleaner);
					} catch (Exception ee) {
						log.error(ee.getMessage(), ee);
					}
				};
			} catch (Exception e) {
				log.debug(e.getMessage(), e);
			}
		}
		// 没有 DirectByteBuffer cleaner，构造一个空的。
		if (buffCleaner == null) {
			log.warn("没有找到 DirectByteBuffer cleaner");
			buffCleaner = Buffer::clear;
		}
	}

	protected boolean newed;
	protected FileChannel channel;
	protected MappedByteBuffer buffer;
	private RandomAccessFile rw;

	protected Mapped(Path path, long pos, long size) throws IOException {
		newed = !Files.exists(path);
		rw = new RandomAccessFile(path.toFile(), "rw");
		channel = rw.getChannel();
		buffer = channel.map(FileChannel.MapMode.READ_WRITE, pos, size);
	}

	public static Path pathname(Path path, long name, String extension) {
		return path.resolve(String.format("%0" + DATA_FILENAME_MAX_LENGTH + "d", name) + extension);
	}

	@Override
	public void close() throws IOException {
		buffCleaner.accept(buffer);
		force();
		channel.close();
		rw.close();
		buffer = null;
		channel = null;
		rw = null;
	}

	public void force() throws IOException {
		channel.force(true);
	}
}
