package net.officefloor.hq.app.sheet;

/** A sheet change the server refuses; answered 400 with the message. */
public class InvalidSheetRequestException extends Exception {
    public InvalidSheetRequestException(String message) {
        super(message);
    }
}
