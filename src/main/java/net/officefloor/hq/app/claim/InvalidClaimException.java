package net.officefloor.hq.app.claim;

/** The requested claim is not valid (a missing or malformed amount, or a bad date); nothing is changed. */
public class InvalidClaimException extends Exception {
    public InvalidClaimException(String message) {
        super(message);
    }
}
