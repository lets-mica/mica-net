package net.dreamlu.net.cluster.test.queue;

import io.protostuff.LinkedBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FileQueue<E> {
	private static final Logger log = LoggerFactory.getLogger(FileQueue.class);
	static final Map<Path, FileQueue<?>> CACHE = new ConcurrentHashMap<>(16);

	private final Reader<E> r;
	private final Writer<E> w;

	private final int bufSize;

	FileQueue(Path path, int bufferSize, long maxFileSize, long maxDataSize) throws IOException {
		if (Files.notExists(path)) {
			Files.createDirectories(path);
		}
		w = new Writer<>(path, maxFileSize, maxDataSize);
		r = new Reader<>(path, maxFileSize, maxDataSize, w);
		bufSize = bufferSize;
	}

	public static Builder builder() {
		return new Builder();
	}

	@SuppressWarnings("unchecked")
	public void put(E element) {
		if (element instanceof List<?>) {
			w.writ(element, bufSize);
		} else if (element instanceof Map<?, ?>) {
			w.writ(element, bufSize);
		} else {
			w.writ(element, bufSize);
		}
	}

	public E take() throws InterruptedException {
		return r.take();
	}

	public E poll() {
		return r.poll();
	}

	public void close() throws IOException {
		w.close();
		r.close();
	}

	public static final class Builder {
		private long maxFileSize = 100 * 1024 * 1024; // 100m
		private long maxDataSize = 64 * 1024; // 64k
		private int bufferSize = LinkedBuffer.DEFAULT_BUFFER_SIZE;
		private Path path;

		public Builder path(String path) {
			this.path = Paths.get(path);
			return this;
		}

		public Builder path(Path path) {
			this.path = path;
			return this;
		}

		public Builder bufferSize(int bufferSize) {
			this.bufferSize = bufferSize;
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

		@SuppressWarnings("unchecked")
		public <E> FileQueue<E> build() throws IOException {
			FileQueue<?> queue = CACHE.get(path);
			if (queue == null) {
				synchronized (FileQueue.class) {
					queue = CACHE.get(path);
					if (queue == null) {
						CACHE.put(path, queue = new FileQueue<>(path, bufferSize, maxFileSize, maxDataSize));
					}
				}
			}
			if (CACHE.size() == 1) {
				Runtime.getRuntime().addShutdownHook(new Thread(() -> CACHE.forEach((k, v) -> {
					try {
						v.close();
						log.debug("队列停止，清理资源 " + k);
					} catch (Exception e) {
						log.error("程序退出关闭文件异常, path: " + k, e);
					}
				})));
			}
			return (FileQueue<E>) queue;
		}
	}
}
