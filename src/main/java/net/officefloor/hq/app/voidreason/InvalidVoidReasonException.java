package net.officefloor.hq.app.voidreason;

/** The void reason is blank, or already on the sheet's list. */
public class InvalidVoidReasonException extends Exception {
    public InvalidVoidReasonException(String message) {
        super(message);
    }
}
