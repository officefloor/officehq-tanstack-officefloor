package net.officefloor.hq.app.voidreason;

/** A void reason that is blank, or already on the sheet's list. */
public class InvalidVoidReasonException extends Exception {
    public InvalidVoidReasonException(String message) {
        super(message);
    }
}
