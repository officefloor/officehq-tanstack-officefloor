package net.officefloor.hq.app.claim;

public class ClaimNotFoundException extends Exception {
    public ClaimNotFoundException(String claimId) {
        super("No such claim: " + claimId);
    }
}
