package net.officefloor.hq.app.claim;

/** A clerk doing what only a steward may: approving or rejecting a PENDING claim. */
public class ClerkLimitException extends Exception {
    public ClerkLimitException(String message) {
        super(message);
    }
}
