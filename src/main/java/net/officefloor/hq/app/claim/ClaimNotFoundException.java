package net.officefloor.hq.app.claim;

/** A claim id that is not on the sheet. */
public class ClaimNotFoundException extends Exception {
    public ClaimNotFoundException(String id) {
        super("No claim " + id + " on this sheet.");
    }
}
