package net.officefloor.hq.app.sheet;

/** A sheet change the app refuses; answered 400 with the message. */
public class InvalidSheetException extends Exception {
    public InvalidSheetException(String message) {
        super(message);
    }
}
