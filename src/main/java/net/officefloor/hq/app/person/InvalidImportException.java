package net.officefloor.hq.app.person;

import java.util.List;

/** Some lines of an import are not valid people; nothing is imported. */
public class InvalidImportException extends Exception {

    private final List<Integer> lines;

    public InvalidImportException(List<Integer> lines) {
        super("Lines " + lines + " are not valid people.");
        this.lines = List.copyOf(lines);
    }

    public List<Integer> getLines() {
        return lines;
    }
}
