package net.officefloor.hq.app.claim;

/** A clerk tried to settle a claim whose due is above the sheet's clerk limit: refused and nothing
 *  is written. */
public class ClaimLimitException extends Exception {
    public ClaimLimitException(String message) {
        super(message);
    }
}
