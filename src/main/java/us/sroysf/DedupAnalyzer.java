package us.sroysf;

import org.mockito.internal.util.collections.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by sroy on 2/24/17.
 * TODO: delete empty directories?
 */
public class DedupAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DedupAnalyzer.class);

    /** These elements will be ignored */
    private static final Set<String> IGNORE_FILES = Sets.newSet(".DS_Store");
    /** These elements will be deleted */
    private static final Set<String> DELETED_FILES = Sets.newSet(".picasa.ini", "Thumbs.db" /*, ".DS_Store"*/);

    private final Map<String, List<FileInfo>> fileDB = new TreeMap<>();
    private final List<FileInfo> tinyFiles = new ArrayList<>();
    private final boolean debug;
    private final boolean interactive;

    private final Path root;
    private final int tinyFilesSize;
    private final List<String> mutablePaths;

    private long numberBytesDeleted;

    DedupAnalyzer(Path root, boolean debug, int tinyFilesSize, boolean interactive, String... mutableDirs) throws IOException {
        this.root = root;

        this.mutablePaths = new ArrayList<>();
        this.debug = debug;
        this.tinyFilesSize = tinyFilesSize;
        this.interactive = interactive;

        for (String mutableDir : mutableDirs) {
            Path resolved = root.resolve(mutableDir);
            if (!Files.isDirectory(resolved)) {
                throw new IOException("Bad mutable dir: " + mutableDir);
            }
            mutablePaths.add(resolved.toAbsolutePath().toString());
        }
        Collections.sort(mutablePaths);
    }


    public void analyze() throws IOException {
        gatherFileData(root);
        execute();
        System.out.println("Total amount of bytes cleaned: " + humanReadableByteCount(numberBytesDeleted));
    }

    private void execute() {
        if (tinyFilesSize > 0) {
            handleTinyFiles();
        }
        handleDuplicates();
    }

    private void handleTinyFiles() {
        System.out.println("================================");
        System.out.println(" Tiny Files");
        System.out.println("================================");

        tinyFiles.forEach(fileInfo -> {
            Path filePath = fileInfo.getPath();
            if (isMutablePath(filePath)) {
                if (filePath.getFileName().toString().startsWith(".")) {
                    deleteFile(fileInfo, "tiny file");
                } else {
                    System.out.printf("skipping tiny file %s [%d]", filePath, fileInfo.getSize());
                }
            }
        });
    }

    private void handleDuplicates() {
        System.out.println("================================");
        System.out.println(" Duplicates");
        System.out.println("================================");

        fileDB.forEach((key, dups) -> {
            if ((dups.size() > 1) && containsMutablePath(dups)) {
                dups.sort(Comparator.comparing(o -> o.getPath().toAbsolutePath().toString()));
                if (interactive) {
                    System.out.println("Choose the version of the file to keep:");
                    for (int i=0; i<dups.size(); i++) {
                        FileInfo dup = dups.get(i);
                        System.out.printf("\t%d) %s [%d]\n", i, dup.getPath(), dup.getSize());
                    }
                    System.out.printf("\t%d) SKIP\n", dups.size());

                    int answer = new Scanner(System.in).nextInt();
                    if (answer > -1 && answer < dups.size()) {
                        for (int i=0; i<dups.size(); i++) {
                            if (i != answer) {
                                deleteFile(dups.get(i), "duplicate");
                            }
                        }
                    }
                } else {
                    FileInfo first = dups.get(0);
                    System.out.printf("========\n%s [%d]\n", first.getPath().getFileName(), first.getSize());
                    dups.forEach(fileInfo ->
                            System.out.printf("\t%s\n", fileInfo.getPath().getParent().toAbsolutePath()));
                    deleteDuplicates(dups);
                }
            }
        });
    }

    private void deleteDuplicates(List<FileInfo> dups) {
        int numDeletes = 0;
        for (FileInfo dup : dups) {
            if (isMutablePath(dup.getPath())) {
                deleteFile(dup, "duplicate");
                numDeletes++;

                if (numDeletes == (dups.size()-1)) {
                    break;
                }
            }
        }
    }

    private void deleteFile(FileInfo fileInfo, String context) {
        try {
            if (Files.exists(fileInfo.getPath())) {
                if (!debug) {
                    Files.delete(fileInfo.getPath());
                }
                System.out.printf("\t\tFile deleted: %s [%d]\n", fileInfo.getPath(), fileInfo.getSize());
                numberBytesDeleted += fileInfo.getSize();
            }
        } catch (Exception e) {
            log.error("problem deleting {} {} [{}]", context, fileInfo.getPath(), fileInfo.getSize(), e);
        }
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

    private void gatherFileData(Path dir) throws IOException {
        Stream<Path> list = Files.list(dir);
        list.forEach(entry -> {
            try {
                if (Files.isDirectory(entry)) {
                    gatherFileData(entry);
                } else if (Files.isRegularFile(entry)) {
                    addFile(entry);
                }
            } catch (Exception ex) {
                log.error("problem collecting file data", ex);
            }
        });
    }

    private void addFile(Path entry) throws IOException {

        // ignore requested files in IGNORE_FILES variable
        if (IGNORE_FILES.contains(entry.getFileName().toString())) {
            System.out.println(String.format("IGNORING ===> %s", entry));
            return;
        }

        long size = Files.size(entry);
        FileInfo fileInfo = new FileInfo(entry, size);

        // if tinyFileSize is defined, add them to tinyFiles instead
        if (tinyFilesSize > 0 && fileInfo.getSize() < tinyFilesSize) {
            tinyFiles.add(fileInfo);
            return;
        }

        // delete all requested files in DELETED_FILES variable
        if (isMutablePath(entry) && DELETED_FILES.contains(entry.getFileName().toString())) {
            deleteFile(fileInfo, "requested deleted file");
        }

        List<FileInfo> fileInfos = fileDB.get(fileInfo.getKey());
        if (fileInfos == null) {
            fileInfos = new ArrayList<>();
            fileInfos.add(fileInfo);
            fileDB.put(fileInfo.getKey(), fileInfos);
        } else {
            fileInfos.add(fileInfo);
        }
    }

    // https://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    private static String humanReadableByteCount(long bytes) {
        int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "kMGTPE".substring(exp-1, exp);
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
