package us.sroysf;

import org.apache.commons.cli.*;

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

    private static final Option OPTION_HELP = Option.builder("h")
            .longOpt("help")
            .desc("print help and exit")
            .required(false)
            .build();

    private static final Option OPTION_DEBUG = Option.builder("d")
            .longOpt("debug")
            .desc("don't physically delete files")
            .required(false)
            .build();

    private static final Option OPTION_TINY_FILES_SIZE = Option.builder("s")
            .longOpt("tinyFilesSize")
            .desc("if present, it'll separate tiny files from regular ones")
            .required(false)
            .hasArg()
            .build();

    private static final Option OPTION_EDITABLE_DIRS = Option.builder("e")
            .longOpt("editableDirs")
            .desc("directory where changes are allowed")
            .required(false)
            .hasArg()
            .valueSeparator(',')
            .build();

    private static final Option OPTION_ROOT = Option.builder("r")
            .longOpt("root")
            .desc("root directory to start search")
            .required(true)
            .hasArg()
            .build();

    private static final Option OPTION_MODE = Option.builder("m")
            .longOpt("mode")
            .desc("mode - how to handle duplicates: [PickFirst, InteractivePickFile, InteractivePickDirectory]")
            .required(true)
            .hasArg()
            .build();

    private static final Options options = new Options()
            .addOption(OPTION_HELP)
            .addOption(OPTION_DEBUG)
            .addOption(OPTION_TINY_FILES_SIZE)
            .addOption(OPTION_EDITABLE_DIRS)
            .addOption(OPTION_ROOT)
            .addOption(OPTION_MODE)
            ;

    private final Runnable systemExitHandler;
    private final String[] args;

    public static void main(String[] args) throws IOException {
        new AppMain(args, () -> System.exit(1)).run();
    }

    AppMain(String[] args, Runnable systemExitHandler) {
        this.args = args;
        this.systemExitHandler = systemExitHandler;
    }

    void run() throws IOException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmdLine;
        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            printHelpAndTerminate();
            return;
        }

        if (cmdLine.hasOption(OPTION_HELP.getOpt())) {
            printHelpAndTerminate();
            return;
        }

        String photoDir = cmdLine.getOptionValue(OPTION_ROOT.getOpt());
        String[] mutableDirs = cmdLine.getOptionValues(OPTION_EDITABLE_DIRS.getOpt());
        if (mutableDirs == null) {
            mutableDirs = new String[]{photoDir};
        }
        boolean debug = cmdLine.hasOption(OPTION_DEBUG.getOpt());
        Mode mode;
        try {
            mode = Mode.valueOf(cmdLine.getOptionValue(OPTION_MODE.getOpt()));
        } catch (IllegalArgumentException e) {
            System.out.printf("\nInvalid mode: %s\n", cmdLine.getOptionValue(OPTION_MODE.getOpt()));
            printHelpAndTerminate();
            return;
        }
        int tinyFilesSize = Integer.parseInt(cmdLine.getOptionValue(OPTION_TINY_FILES_SIZE.getOpt(), "0"));
        Path path = Paths.get(photoDir);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            System.out.printf("\nInvalid directory: %s\n", path);
            printHelpAndTerminate();
            return;
        }
        // Set to true to perform actual deletes of duplicate files
        DedupAnalyzer dedup = new DedupAnalyzer(path, debug, tinyFilesSize, mode, mutableDirs);
        dedup.analyze();
    }

    private void printHelpAndTerminate() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("appmain", AppMain.options);
        systemExitHandler.run();
    }

}
