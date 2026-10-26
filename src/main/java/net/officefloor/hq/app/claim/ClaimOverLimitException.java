package net.officefloor.hq.app.claim;

/** A clerk tried to settle a claim whose due is above the sheet's clerk limit. */
public class ClaimOverLimitException extends Exception {

    public ClaimOverLimitException(String message) {
        super(message);
    }
}
