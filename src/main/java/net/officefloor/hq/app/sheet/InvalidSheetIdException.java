package net.officefloor.hq.app.sheet;

/** A sheet id that is blank, or one that cannot be used as asked. */
public class InvalidSheetIdException extends Exception {
    public InvalidSheetIdException() {
        super("A sheet id is required");
    }

    public InvalidSheetIdException(String message) {
        super(message);
    }
}
