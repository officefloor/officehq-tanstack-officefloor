package net.officefloor.hq.app.voidreason;

/** A void reason add with no signed-in user, a blank reason, or a reason already on the list. */
public class InvalidVoidReasonException extends Exception {
    public InvalidVoidReasonException(String message) {
        super(message);
    }
}
