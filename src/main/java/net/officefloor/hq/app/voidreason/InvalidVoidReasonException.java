package net.officefloor.hq.app.voidreason;

/** A void reason that cannot be added (blank, or already on the list); nothing is written. */
public class InvalidVoidReasonException extends Exception {
    public InvalidVoidReasonException(String message) {
        super(message);
    }
}
