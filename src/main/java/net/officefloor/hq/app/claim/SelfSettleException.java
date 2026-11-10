package net.officefloor.hq.app.claim;

/** The user who opened a claim, or last edited it, may not also settle it. */
public class SelfSettleException extends Exception {
    public SelfSettleException(String user) {
        this(user, "opened");
    }

    public SelfSettleException(String user, String did) {
        super("A claim cannot be settled by " + user + ", who " + did + " it");
    }
}
