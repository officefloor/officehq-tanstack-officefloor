package net.officefloor.hq.app.claim;

/** The user who opened a claim may not also settle it. */
public class SelfSettleException extends Exception {
    public SelfSettleException(String user) {
        super("A claim cannot be settled by " + user + ", who opened it");
    }
}
