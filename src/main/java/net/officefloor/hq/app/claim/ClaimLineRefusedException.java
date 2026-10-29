package net.officefloor.hq.app.claim;

/** A line was refused (an empty label, a quantity under 1, a unit amount with more than two decimals), or an edit typed another amount than a claim's lines add up to. */
public class ClaimLineRefusedException extends Exception {

    public ClaimLineRefusedException(String message) {
        super(message);
    }
}
