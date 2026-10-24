package net.officefloor.hq.app.claim;

/** A clerk settling a claim whose due is above the sheet's clerk limit. */
public class ClerkLimitException extends Exception {
    public ClerkLimitException(String message) {
        super(message);
    }
}
