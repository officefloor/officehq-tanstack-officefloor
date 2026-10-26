package net.officefloor.hq.app.claim;

/** No claim with this id is on the sheet. */
public class ClaimNotFoundException extends Exception {

    public ClaimNotFoundException(String message) {
        super(message);
    }
}
