package net.officefloor.hq.app.claim;

/** The claim is dated after its person's retirement, so it cannot be added or edited to that date; nothing is changed. */
public class RetiredPersonException extends Exception {
    public RetiredPersonException(String message) {
        super(message);
    }
}
