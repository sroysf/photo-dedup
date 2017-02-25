package us.sroysf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AppMain
 *
 * @author sroy
 */
public class AppMain {

    private static final Logger log = LoggerFactory.getLogger(AppMain.class);

    public static void main(String[] args) throws IOException {
        String photoDir = args[0];

        Path path = Paths.get(photoDir);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            log.error("Invalid directory: {}", path);
            return;
        }

        new DedupAnalyzer(path, "Inbox", "2016").analyze();
    }
}
