package net.officefloor.hq.app.sheet;

/** An archive refused: the user is not a steward, or the sheet is already archived. */
public class InvalidArchiveException extends Exception {
    public InvalidArchiveException(String message) {
        super(message);
    }
}
