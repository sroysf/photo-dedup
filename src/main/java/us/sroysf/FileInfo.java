package us.sroysf;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Created by sroy on 2/24/17.
 */
class FileInfo {
    private long size;
    private Path path;

    FileInfo(Path path, long size) {
        this.size = size;
        this.path = path;
    }

    String getKey() {
        return String.format("%s.%d", path.getFileName(), size);
    }

    long getSize() {
        return size;
    }

    Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInfo fileInfo = (FileInfo) o;
        return size == fileInfo.size &&
                Objects.equals(path, fileInfo.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, path);
    }

}
