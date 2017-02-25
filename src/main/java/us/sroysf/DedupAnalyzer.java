package us.sroysf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by sroy on 2/24/17.
 */
public class DedupAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DedupAnalyzer.class);
    private Map<String, List<FileInfo>> fileDB;
    private List<FileInfo> tinyFiles;

    private Path root;
    private Set<String> mutablePaths;

    public DedupAnalyzer(Path root, String... mutableDirs) throws IOException {
        this.root = root;
        this.fileDB = new HashMap<>();
        this.tinyFiles = new ArrayList<>();
        this.mutablePaths = new HashSet<>();

        for (int i=0; i < mutableDirs.length; i++) {
            Path resolved = root.resolve(mutableDirs[i]);
            if (!Files.isDirectory(resolved)) {
                throw new IOException("Bad mutable dir: " + mutableDirs[i]);
            }
            mutablePaths.add(resolved.toAbsolutePath().toString());
        }
    }

    public void analyze() throws IOException {
        gatherFileData(root);

        printReport();
    }

    private void printReport() {
        printTinyFiles();
        printDuplicates();
    }

    private void printTinyFiles() {
        System.out.println("================================");
        System.out.println(" Tiny Files");
        System.out.println("================================");

        tinyFiles.forEach(fileInfo -> {
            Path filePath = fileInfo.getPath();
            if (isMutablePath(filePath)) {
                if (filePath.getFileName().toString().startsWith(".")) {
                    System.out.println(String.format("DELETING ===> %s [%d]", filePath, fileInfo.getSize()));
                    try {
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        }
                    } catch (IOException e) {
                        log.error("", e);
                    }
                } else {
                    System.out.println(String.format("%s [%d]", filePath, fileInfo.getSize()));
                }
            }
        });
    }

    private void printDuplicates() {
        System.out.println("================================");
        System.out.println(" Duplicates");
        System.out.println("================================");

        fileDB.forEach((key, dups) -> {
            if ((dups.size() > 1) && containsMutablePath(dups)) {
                System.out.println("========");
                dups.forEach(fileInfo -> {
                    System.out.println(String.format("%s [%d]", fileInfo.getPath(), fileInfo.getSize()));
                });
            }
        });
    }

    private boolean containsMutablePath(List<FileInfo> dups) {

        for (FileInfo fileInfo : dups) {
            if (isMutablePath(fileInfo.getPath())) {
                return true;
            }
        }

        return false;
    }

    private boolean isMutablePath(Path path) {

        for (String prefix : mutablePaths) {
            if (path.toAbsolutePath().toString().startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    public void gatherFileData(Path dir) throws IOException {
        Stream<Path> list = Files.list(dir);
        list.forEach(entry -> {
            try {
                if (Files.isDirectory(entry)) {
                    gatherFileData(entry);
                } else if (Files.isRegularFile(entry)) {
                    addFile(entry);
                }
            } catch (Exception ex) {
                log.error("", ex);
            }
        });
    }

    private void addFile(Path entry) throws IOException {

        if (isMutablePath(entry) && entry.getFileName().toString().startsWith(".DS_Store")) {
            System.out.println(String.format("DELETING ===> %s", entry));
            //Files.delete(entry);
        }

        long size = Files.size(entry);
        FileInfo fileInfo = new FileInfo(entry, size);

        if (fileInfo.getSize() < 15000) {
            tinyFiles.add(fileInfo);
            return;
        }

        List<FileInfo> dups = fileDB.get(fileInfo.getKey());
        if (dups == null) {
            dups = new ArrayList<>();
            dups.add(fileInfo);
            fileDB.put(fileInfo.getKey(), dups);
        } else {
            dups.add(fileInfo);
        }
    }
}
