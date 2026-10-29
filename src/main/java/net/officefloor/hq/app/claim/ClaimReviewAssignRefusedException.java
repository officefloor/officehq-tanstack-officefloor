package net.officefloor.hq.app.claim;

/** A review assignment by someone other than a steward, of a claim not in REVIEW, or to a blank user. */
public class ClaimReviewAssignRefusedException extends Exception {

    public ClaimReviewAssignRefusedException(String message) {
        super(message);
    }
}
