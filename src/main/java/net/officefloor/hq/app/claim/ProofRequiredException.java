package net.officefloor.hq.app.claim;

/** Settling or approving a claim whose due is above the proof threshold while it has no proof. */
public class ProofRequiredException extends Exception {
    public ProofRequiredException(String message) {
        super(message);
    }
}
