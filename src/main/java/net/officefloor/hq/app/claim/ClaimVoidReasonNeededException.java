package net.officefloor.hq.app.claim;

/** A claim was voided without a reason from the sheet's list, while the list is not empty. */
public class ClaimVoidReasonNeededException extends Exception {

    public ClaimVoidReasonNeededException(String message) {
        super(message);
    }
}
