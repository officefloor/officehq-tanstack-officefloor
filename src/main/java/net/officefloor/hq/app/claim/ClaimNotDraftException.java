package net.officefloor.hq.app.claim;

/** Only a DRAFT claim can be edited: an open claim is changed by voiding it and adding a new one. */
public class ClaimNotDraftException extends Exception {

    public ClaimNotDraftException(String message) {
        super(message);
    }
}
