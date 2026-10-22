package net.officefloor.hq.app.claim;

/** A claim add or edit with a bad amount or date, or no signed-in user. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
