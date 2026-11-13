package net.officefloor.hq.app.claim;

public class ClaimNotFoundException extends Exception {
    public ClaimNotFoundException(long id) {
        super("No claim " + id);
    }
}
