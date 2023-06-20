package org.tio.core.queue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 保存数据的文件
 */
final class DataFile extends Mapped {
	static final File[] EMPTY_FILES = {};
	static final String EXTENSION = ".data";

	DataFile(Path path, long pos, long size) throws IOException {
		super(path, pos, size);
	}

	void writ(byte[] bytes) {
		buffer.put(bytes);
	}

	void read(byte[] dst, int len) {
		buffer.get(dst, 0, len);
	}

	private static long fmtName(File file) {
		return Long.parseLong(file.getName().substring(0, DATA_FILENAME_MAX_LENGTH));
	}

	private static File[] files(Path path) {
		File[] files = path.toFile().listFiles((dir, name) -> name.endsWith(EXTENSION));
		if (files == null || files.length == 0) {
			return EMPTY_FILES;
		}
		Arrays.sort(files, Comparator.comparingLong(DataFile::fmtName));
		return files;
	}

	public static long name(Path path, long offset, long maxFileSize) {
		File[] files = files(path);
		if (files.length == 0) {
			return 0;
		}
		for (File file : files) {
			long format = fmtName(file);
			if (format == offset) {
				return format;
			}
			if (format < offset && format >= offset - maxFileSize) {
				return format;
			}
		}
		return -1;
	}

}
