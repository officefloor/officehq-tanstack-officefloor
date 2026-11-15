package net.officefloor.hq.app.sheet;

/** The clone is refused (no id, or the other sheet was already used): nothing is written. */
public class InvalidSheetCloneException extends Exception {
    public InvalidSheetCloneException(String message) {
        super(message);
    }
}
