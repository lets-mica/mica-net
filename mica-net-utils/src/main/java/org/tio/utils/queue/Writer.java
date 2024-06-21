package org.tio.utils.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * 文件写
 *
 * @param <E> 泛型
 * @author leon
 */
final class Writer<E> extends Mapped {
	private static final Logger log = LoggerFactory.getLogger(Writer.class);
	static final String NAME = "data.write";
	private final Path path;
	private final long mfs;
	private final long mds;
	private final ReentrantLock lock = new ReentrantLock();
	private final Condition condition;

	private OffsetFile offset;
	private DataFile data;
	long dataIdx;
	long offsetIdx;
	private long maxDataIdx;
	private long maxOffsetIdx;

	Writer(Path path, long mfs, long mds) throws IOException {
		super(path.resolve(NAME), 0, 8);
		this.path = path;
		this.mfs = mfs;
		this.mds = mds;

		condition = lock.newCondition();
		dataIdx = readCurrentDataIndex();
		offset = initOffsetMapped();
		offsetIdx = readCurrentOffsetIndex();
		data = initDataMapped();
		maxDataIdx = readMaxDataIndex();
	}

	void write(E element, Function<E, byte[]> mapper) {
		if (element == null) {
			throw new NullPointerException("文件队列数据不能为空！");
		}
		byte[] bytes = mapper.apply(element);
		if (bytes.length > mds) {
			throw new IllegalArgumentException("数据超长, max: " + mds + ", cur: " + bytes.length);
		}
		this.write(bytes);
	}

	void write(byte[] bytes) {
		lock.lock();
		try {
			writeData(bytes);
			condition.signalAll();
		} finally {
			lock.unlock();
		}
	}

	private void writeData(byte[] bytes) {
		if (data == null) {
			return; // 停服释放锁时，线程进入本方法后data为空，将导致异常
		}
		long inc = offsetIdx + bytes.length; // 偏移量增加
		if (inc > maxOffsetIdx) {
			dataGrow();// 数据文件扩容
		}
		data.write(bytes); // 写入数据
		if (++dataIdx > maxDataIdx) {
			offsetGrow(); // 偏移量文件扩容
		}
		offset.write(inc); // 写入偏移量

		buffer.putLong(dataIdx); // 记录数据下标
		buffer.flip(); // 翻转为原状态 （可读）
		offsetIdx = inc; // 记录偏移
	}

	private void offsetGrow() {
		try {
			Path newFile = pathname(path, dataIdx - 1, OffsetFile.EXTENSION);
			offset.close();
			data.force();
			this.force();
			maxDataIdx += mfs / 8;
			offset = new OffsetFile(newFile, 0, mfs);
			log.debug("偏移量文件扩容:{}", newFile);
		} catch (Exception e) {
			throw new IllegalStateException("创建数据偏移量文件映射地址异常", e);
		}
	}

	private void dataGrow() {
		try {
			Path newFile = pathname(path, offsetIdx, DataFile.EXTENSION);
			data.close();
			offset.force();
			this.force();
			maxOffsetIdx = offsetIdx + mfs;
			data = new DataFile(newFile, 0, mfs);
			log.debug("数据文件扩容:{}", newFile);
		} catch (Exception e) {
			throw new IllegalStateException("创建数据文件映射地址异常", e);
		}
	}

	private long readCurrentDataIndex() {
		long idx = buffer.getLong();
		buffer.flip(); // 读复原 （可写）
		return idx;
	}

	private OffsetFile initOffsetMapped() throws IOException {
		long name = 0L;
		if (dataIdx != 0 && dataIdx % (mfs / 8) == 0) {
			name = dataIdx - mfs / 8;
		} else if (dataIdx % (mfs / 8) != 0) {
			name = dataIdx - dataIdx % (mfs / 8);
		}
		Path pathname = pathname(path, name, OffsetFile.EXTENSION);
		if (dataIdx != 0 && Files.notExists(pathname)) {
			throw new FileNotFoundException("程序有误,需要读的文件找不到,文件名:" + pathname);
		}
		// 刚好写完上次文件没有扩容的情况
		if (dataIdx != 0 && dataIdx * 8 % mfs == 0) {
			return new OffsetFile(pathname, mfs, 0);
		}
		return new OffsetFile(pathname, dataIdx * 8 % mfs, mfs - (dataIdx * 8 % mfs));
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

	private DataFile initDataMapped() throws IOException {
		long name = DataFile.name(path, offsetIdx, mfs);
		if (name < 0 || offsetIdx - name > mfs || offsetIdx < name) {
			throw new IOException("文件偏移量异常, 获取的数据文件: " + name + ", 当前需要写入的偏移量: " + offsetIdx);
		}
		Path pathname = pathname(path, name, DataFile.EXTENSION);
		if (dataIdx != 0 && Files.notExists(pathname)) {
			throw new FileNotFoundException("文件不存在！" + pathname);
		}
		maxOffsetIdx = name + mfs;
		//最大offset减去上个文件最大的offset等于当前文件开始写的offset
		return new DataFile(pathname, offsetIdx - name, name + mfs - offsetIdx);
	}

	private long readMaxDataIndex() {
		return (dataIdx == 0 || dataIdx % (mfs / 8) != 0
			? (dataIdx / (mfs / 8) + 1)
			: dataIdx / (mfs / 8)) * (mfs / 8);
	}

	void waiting() throws InterruptedException {
		lock.lock();
		try {
			condition.await();
		} finally {
			lock.unlock();
		}
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
