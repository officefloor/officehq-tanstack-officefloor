package net.officefloor.hq.app.voidreason;

/** A void reason that cannot be added: a blank one, or one already on the sheet's list. */
public class InvalidVoidReasonException extends Exception {
    public InvalidVoidReasonException(String message) {
        super(message);
    }
}
