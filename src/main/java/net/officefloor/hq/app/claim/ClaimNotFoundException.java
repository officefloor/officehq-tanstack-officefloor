package net.officefloor.hq.app.claim;

/** No such claim on the sheet; nothing is changed. */
public class ClaimNotFoundException extends Exception {
    public ClaimNotFoundException(String message) {
        super(message);
    }
}
