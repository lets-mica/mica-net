package net.dreamlu.net.cluster.test.queue;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

/**
 * 保存偏移量的文件
 */
final class OffsetFile extends Mapped {
    static final String EXTENSION = ".offset";

    OffsetFile(Path path, long pos, long size) throws IOException {
        super(path, pos, size);
    }

    public long get(long pos, int size) throws IOException {
        MappedByteBuffer map = null;
        try {
            map = channel.map(FileChannel.MapMode.READ_WRITE, pos, size);
            return map.getLong();
        } finally {
            BUFF_CLEANER.accept(map);
        }
    }

    void writ(long offset) {
        buffer.putLong(offset);
    }

    long read() {
        return buffer.getLong();
    }
}
