package net.officefloor.hq.app;

/** A change made against a version of a record someone else has since saved; answered 409 with the
 *  message (see HandleConflict), so the screen can re-read the record. */
public class ConflictException extends Exception {
    public ConflictException(String message) {
        super(message);
    }
}
