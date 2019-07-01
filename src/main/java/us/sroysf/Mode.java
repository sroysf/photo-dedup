package us.sroysf;

public enum Mode {
    /** pick the first, delete the rest */
    PickFirst,
    /** ask per file */
    InteractivePickFile,
    /** ask per shared directories */
    InteractivePickDirectory,
    ;

}
