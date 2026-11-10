package net.officefloor.hq.app.sheet;

/** A sheet id that is blank. */
public class InvalidSheetIdException extends Exception {
    public InvalidSheetIdException() {
        super("A sheet id is required");
    }
}
