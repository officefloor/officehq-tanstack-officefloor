package net.officefloor.hq.app.claim;

/** Whether a status change is a steward's review of a PENDING claim (approve or reject), and a reject's reason. */
public record ClaimReview(boolean asked, String reason) {

    /** An open, settle or void: not a review. */
    public static final ClaimReview NONE = new ClaimReview(false, null);
}
