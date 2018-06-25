package us.sroysf;

import java.nio.file.Path;

/**
 * Created by sroy on 2/24/17.
 */
public class FileInfo {
    private long size;
    private Path path;

    FileInfo(Path path, long size) {
        this.size = size;
        this.path = path;
    }

    public String getKey() {
        return String.format("%s.%d", path.getFileName(), size);
    }

    public long getSize() {
        return size;
    }

    public Path getPath() {
        return path;
    }
}
