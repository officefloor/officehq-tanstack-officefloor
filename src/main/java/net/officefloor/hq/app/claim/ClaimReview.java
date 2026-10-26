package net.officefloor.hq.app.claim;

/** Whether a status change is a steward's review of a PENDING claim (approve or reject), and a reject's reason; or
 *  (`release`) a steward's review of a REVIEW claim, opening it. */
public record ClaimReview(boolean asked, String reason, boolean release) {

    /** An open, settle or void: not a review. */
    public static final ClaimReview NONE = new ClaimReview(false, null, false);

    /** A steward's review of a REVIEW claim: it opens. */
    public static final ClaimReview RELEASE = new ClaimReview(false, null, true);
}
