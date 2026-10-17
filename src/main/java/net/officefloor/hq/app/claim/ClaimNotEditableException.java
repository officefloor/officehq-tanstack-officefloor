package net.officefloor.hq.app.claim;

/** The claim is no longer a DRAFT, so it cannot be edited (void it and add a new one); nothing is changed. */
public class ClaimNotEditableException extends Exception {
    public ClaimNotEditableException(String message) {
        super(message);
    }
}
