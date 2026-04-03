package net.dreamlu.mica.net.utils.queue;

import net.dreamlu.mica.net.utils.hutool.CollUtil;
import net.dreamlu.mica.net.utils.mica.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 文件队列
 *
 * @param <E> 泛型
 * @author leon
 */
public final class FileQueue<E> {
	private static final Logger log = LoggerFactory.getLogger(FileQueue.class);
	static final Map<Path, FileQueue<?>> CACHE = new ConcurrentHashMap<>(16);
	private final Reader<E> reader;
	private final Writer<E> writer;

	FileQueue(Path path, long maxFileSize, long maxDataSize) throws IOException {
		if (Files.notExists(path)) {
			Files.createDirectories(path);
		}
		this.writer = new Writer<>(path, maxFileSize, maxDataSize);
		this.reader = new Reader<>(path, maxFileSize, maxDataSize, this.writer);
	}

	public static Builder builder() {
		return new Builder();
	}

	public void put(E element, Function<E, byte[]> mapper) {
		writer.write(element, mapper);
	}

	public void put(byte[] data) {
		writer.write(data);
	}

	public E take(Function<byte[], E> mapper) throws InterruptedException {
		return reader.take(mapper);
	}

	public E poll(Function<byte[], E> mapper) {
		return reader.poll(mapper);
	}

	public void close() throws IOException {
		writer.close();
		reader.close();
	}

	public static final class Builder {
		private long maxFileSize = 100 * 1024 * 1024L; // 100m
		private long maxDataSize = 64 * 1024L; // 64k
		private Path path;

		public Builder path(String path) {
			this.path = Paths.get(path);
			return this;
		}

		public Builder path(Path path) {
			this.path = path;
			return this;
		}

		public Builder maxFileSize(long maxFileSize) {
			this.maxFileSize = maxFileSize;
			return this;
		}

		public Builder maxDataSize(long maxDataSize) {
			this.maxDataSize = maxDataSize;
			return this;
		}

		public <E> FileQueue<E> build() {
			FileQueue<?> queue = CollUtil.computeIfAbsent(CACHE, path, key -> {
				try {
					return new FileQueue<>(path, maxFileSize, maxDataSize);
				} catch (IOException e) {
					throw ExceptionUtils.unchecked(e);
				}
			});
			if (CACHE.size() == 1) {
				Runtime.getRuntime().addShutdownHook(new Thread(() -> CACHE.forEach((k, v) -> {
					try {
						v.close();
						log.debug("队列停止，清理资源:{}", k);
					} catch (Exception e) {
						log.error("程序退出关闭文件异常, path:{}", k, e);
					}
				})));
			}
			return (FileQueue<E>) queue;
		}
	}

}
