package org.tio.utils.queue;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * 保存偏移量的文件
 *
 * @author leon
 */
final class OffsetFile extends Mapped {
	static final String EXTENSION = ".offset";

	OffsetFile(Path path, long pos, long size) throws IOException {
		super(path, pos, size);
	}

	public long get(long pos, int size) throws IOException {
		MappedByteBuffer byteBuffer = null;
		try {
			byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, pos, size);
			return byteBuffer.getLong();
		} finally {
			buffCleaner.accept(byteBuffer);
		}
	}

	void write(long offset) {
		buffer.putLong(offset);
	}

	long read() {
		return buffer.getLong();
	}
}
