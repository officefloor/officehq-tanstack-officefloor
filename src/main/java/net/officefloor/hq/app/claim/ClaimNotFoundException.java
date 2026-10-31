package net.officefloor.hq.app.claim;

/** No claim with this id on the sheet; answered 404. */
public class ClaimNotFoundException extends Exception {
    public ClaimNotFoundException(long claimId) {
        super("No such claim: " + claimId);
    }
}
