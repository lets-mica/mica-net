package net.dreamlu.net.cluster.test.queue;

import net.dreamlu.net.cluster.test.ProtoStuffUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

final class Reader<E> extends Mapped {
	private static final Logger log = LoggerFactory.getLogger(Reader.class);
	static final String NAME = "data.read";
	/**
	 * Grow后，将已读完的文件放入队列。
	 * tick后，如果队列有文件，则执行清理
	 */
	private final BlockingQueue<Path> clearQueue = new LinkedBlockingQueue<>(12);

	private final Path path;
	private final long mfs;
	private final long mds;
	Writer<E> writer;

	long dataIdx;
	private OffsetFile offset;
	private long offsetIdx;
	private long offsetName;
	private DataFile data;
	private long dataName;
	private long maxDataIdx;
	private long maxOffsetIdx;
	private final ReentrantLock lock = new ReentrantLock();

	public Reader(Path path, long mfs, long mds, Writer<E> writer) throws IOException {
		super(path.resolve(NAME), 0, 8);
		this.path = path;
		this.mfs = mfs;
		this.mds = mds;
		this.writer = writer;

		dataIdx = readCurrentDataIndex();
		offset = initOffsetMapped();
		offsetIdx = readCurrentOffsetIndex();
		data = initDataMapped();
		maxDataIdx = readMaxDataIndex();

		startCleanThread();
	}

	public E take() throws InterruptedException {
		E poll;
		while ((poll = poll()) == null) {
			while (offsetIdx >= writer.offsetIdx) {
				writer.waiting();
			}
		}
		return poll;
	}

	@SuppressWarnings("unchecked")
	public E poll() {
		lock.lock();
		try {
			byte[] bytes = poll0();
			if (bytes == null) {
				return null;
			}
			dataIdx += 1;
			buffer.putLong(dataIdx);
			buffer.flip();
			return (E) ProtoStuffUtil.deserialize(bytes, Wrap.class).getValue();
		} finally {
			lock.unlock();
		}
	}

	private byte[] poll0() {
		if (offset == null) {
			return null; // 停服释放锁时，线程进入本方法后offset为空，将导致异常
		}
		if (offsetIdx >= writer.offsetIdx) {
			return null; // 越界
		}
		if (dataIdx >= maxDataIdx) {
			nextOffset(); // 读下个偏移量文件
		}
		long cur = offset.read();
		if (cur > maxOffsetIdx) {
			nextData(); // 读下个数据文件
		}
		int len = (int) (cur - offsetIdx); // 计算数据长度
		if (len > mds) {
			throw new RuntimeException("数据超长！ 最大长度：" + mds + ", 当前长度: " + len);
		}
		byte[] buf = new byte[len];
		data.read(buf, len); // 读数据到buf
		offsetIdx += len; // 偏移量移位
		return buf;
	}

	private void nextData() {
		try {
			Path nextFile = Mapped.pathname(path, offsetIdx, DataFile.EXTENSION);
			data.close();
			maxOffsetIdx = offsetIdx + mfs;
			data = new DataFile(nextFile, 0, mfs);
			// 上一个文件放入清理队列
			clearQueue.put(Mapped.pathname(path, dataName, DataFile.EXTENSION));
			dataName = offsetIdx;
			log.debug("读取下一个数据文件: " + nextFile);
		} catch (Exception e) {
			throw new RuntimeException("创建读取日志文件映射地址异常", e);
		}
	}

	private void nextOffset() {
		try {
			Path nextFile = Mapped.pathname(path, dataIdx, OffsetFile.EXTENSION);
			if (Files.notExists(nextFile)) {
				throw new RuntimeException("文件不存在！- " + nextFile);
			}
			offset.close();
			maxDataIdx += mfs / 8;
			offset = new OffsetFile(nextFile, 0, mfs);
			// 上一个文件放入清理队列
			clearQueue.put(Mapped.pathname(path, offsetName, OffsetFile.EXTENSION));
			offsetName = dataIdx;
			log.debug("读取下一个偏移量文件: " + nextFile);
		} catch (Exception e) {
			throw new RuntimeException("创建数据偏移量文件映射地址异常", e);
		}
	}

	private DataFile initDataMapped() throws IOException {
		dataName = DataFile.name(path, offsetIdx, mfs);
		if (dataName < 0 || offsetIdx - dataName > mfs) {
			throw new RuntimeException("文件偏移量异常, 获取的数据文件: " + dataName + ", 当前需要写入的偏移量: " + offsetIdx);
		}
		Path pathname = Mapped.pathname(path, dataName, DataFile.EXTENSION);
		if (dataIdx != 0 && Files.notExists(pathname)) {
			throw new RuntimeException("文件不存在！" + pathname);
		}
		maxOffsetIdx = dataName + mfs;
		//最大offset减去上个文件最大的offset等于当前文件开始写的offset
		return new DataFile(pathname, offsetIdx - dataName, dataName + mfs - offsetIdx);
	}

	private long readCurrentOffsetIndex() throws IOException {
		if (dataIdx == 0) {
			return 0;
		}
		long pos = dataIdx * 8 % mfs - 8;
		if (dataIdx * 8 % mfs == 0) {
			pos = mfs - 8;
		}
		return offset.get(pos, 8);
	}

	private OffsetFile initOffsetMapped() throws IOException {
		long name = 0L;
		if (dataIdx != 0 && dataIdx % (mfs / 8) == 0) {
			name = dataIdx - mfs / 8;
		} else if (dataIdx % (mfs / 8) != 0) {
			name = dataIdx - dataIdx % (mfs / 8);
		}
		Path pathname = Mapped.pathname(path, name, OffsetFile.EXTENSION);
		if (dataIdx != 0 && Files.notExists(pathname)) {
			throw new RuntimeException("程序有误,需要读的文件找不到,文件名:" + pathname);
		}
		offsetName = name;
		if (dataIdx != 0 && dataIdx * 8 % mfs == 0) {
			return new OffsetFile(pathname, mfs, 0);
		}
		return new OffsetFile(pathname, dataIdx * 8 % mfs, mfs - (dataIdx * 8 % mfs));
	}

	private long readCurrentDataIndex() {
		long idx;
		if (newed) { // 新data.read，写数据下标 （首读）
			buffer.putLong(idx = writer.dataIdx);
			log.debug("首次读取: " + path);
		} else {
			idx = buffer.getLong(); // 读数据
			log.debug("继续读取: " + path);
		}
		buffer.flip();
		return idx;
	}

	private long readMaxDataIndex() {
		return (dataIdx == 0 || dataIdx % (mfs / 8) != 0
			? (dataIdx / (mfs / 8) + 1)
			: dataIdx / (mfs / 8)) * (mfs / 8);
	}

	private void startCleanThread() {
		Thread thread = new Thread(() -> {
			while (true) {
				try {
					Path consumed = clearQueue.take();
					Files.deleteIfExists(consumed);
					log.debug("已读并删除：" + consumed);
				} catch (InterruptedException | IOException e) {
					log.error(e.getMessage(), e);
					break;
				}
			}
		});
		thread.setDaemon(true);
		thread.setName("FileQueueClear");
		thread.start();
	}

	@Override
	public void close() throws IOException {
		lock.lock();
		try {
			super.close();
			data.close();
			data = null;
			offset.close();
			offset = null;
		} finally {
			lock.unlock();
		}
	}
}
