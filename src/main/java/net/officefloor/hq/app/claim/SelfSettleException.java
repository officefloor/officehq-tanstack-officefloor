package net.officefloor.hq.app.claim;

/** The user settling the claim is the one who opened it (four eyes); nothing is changed. */
public class SelfSettleException extends Exception {
    public SelfSettleException(String message) {
        super(message);
    }
}
