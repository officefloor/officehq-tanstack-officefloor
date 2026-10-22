package net.officefloor.hq.app.sheet;

/** A sheet load with a blank or over-long id, or no signed-in user. */
public class InvalidSheetException extends Exception {
    public InvalidSheetException(String message) {
        super(message);
    }
}
