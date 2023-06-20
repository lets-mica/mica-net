package org.tio.utils.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 内存映射文件抽象
 *
 * @author leon
 */
public abstract class Mapped implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(Mapped.class);
	protected static final int DATA_FILENAME_MAX_LENGTH = 19;
	protected static Consumer<MappedByteBuffer> buffCleaner;

	static {
		try {
			Class<?> cls = Class.forName("sun.misc.Unsafe");
			Field field = cls.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			Object unsafe = field.get(null);
			Method method = cls.getMethod("invokeCleaner", ByteBuffer.class);
			buffCleaner = buffer -> {
				try {
					method.invoke(unsafe, buffer);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			};
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			buffCleaner = buffer -> {
			};
		}
	}

	protected boolean newed;
	private RandomAccessFile rw;
	protected FileChannel channel;
	protected MappedByteBuffer buffer;

	protected Mapped(Path path, long pos, long size) throws IOException {
		newed = !Files.exists(path);
		rw = new RandomAccessFile(path.toFile(), "rw");
		channel = rw.getChannel();
		buffer = channel.map(FileChannel.MapMode.READ_WRITE, pos, size);
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

	public static Path pathname(Path path, long name, String extension) {
		return path.resolve(String.format("%0" + DATA_FILENAME_MAX_LENGTH + "d", name) + extension);
	}
}
