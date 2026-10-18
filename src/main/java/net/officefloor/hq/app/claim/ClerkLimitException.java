package net.officefloor.hq.app.claim;

/** A clerk tried to settle a claim whose due is above the sheet's clerk limit; nothing is changed. */
public class ClerkLimitException extends Exception {
    public ClerkLimitException(String message) {
        super(message);
    }
}
