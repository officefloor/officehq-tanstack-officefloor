package net.officefloor.hq.app.claim;

/** Only a steward may re-settle a claim. */
public class ResettleException extends Exception {
    public ResettleException(String message) {
        super(message);
    }
}
