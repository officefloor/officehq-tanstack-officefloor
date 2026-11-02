package net.officefloor.hq.app;

/** A change made against values someone else has since saved: answered 409 with its message (escalation/…ConflictException.yml). */
public class ConflictException extends Exception {
    public ConflictException(String message) {
        super(message);
    }
}
