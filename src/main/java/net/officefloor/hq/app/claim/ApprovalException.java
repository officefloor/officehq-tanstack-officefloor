package net.officefloor.hq.app.claim;

/** Only a steward may approve a PENDING claim, and never the user who opened it. */
public class ApprovalException extends Exception {
    public ApprovalException(String message) {
        super(message);
    }
}
