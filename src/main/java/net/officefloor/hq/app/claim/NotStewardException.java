package net.officefloor.hq.app.claim;

/** A user who is not a steward tried to approve or reject a PENDING claim; nothing is changed. */
public class NotStewardException extends Exception {
    public NotStewardException(String message) {
        super(message);
    }
}
