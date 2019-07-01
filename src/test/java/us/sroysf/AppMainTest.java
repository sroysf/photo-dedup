package us.sroysf;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * AppMainTest
 *
 * @author sroy
 */
public class AppMainTest {

    private Path tempDirectory;


    @BeforeMethod
    public void createTempDir() throws Exception {
        tempDirectory = Files.createTempDirectory(AppMainTest.class.getSimpleName());
    }

    @AfterMethod
    public void removeTempDir() throws Exception {
        Files.walkFileTree(tempDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    public void testOneDuplicateName() throws IOException {
        Path dir1 = Files.createDirectories(tempDirectory.resolve("dir1"));
        Path dir2 = Files.createDirectories(tempDirectory.resolve("dir2"));
        Path dup1 = dir1.resolve("dupFile.gif");
        Files.write(dup1, "test".getBytes());
        Path dup2 = dir2.resolve("dupFile.gif");
        Files.write(dup2, "test".getBytes());

        Path orig2 = dir2.resolve("original.gif");
        Files.write(orig2, "test".getBytes());

        new AppMain(
                new String[]{
                        "-m", "PickFirst",
                        "-r", tempDirectory.toAbsolutePath().toString(),
                        "-e", tempDirectory.toAbsolutePath().toString()
                },
                () -> fail("exited")).run();

        assertFalse(Files.exists(dup1));
        assertTrue(Files.exists(dup2));
        assertTrue(Files.exists(orig2));
    }

}
