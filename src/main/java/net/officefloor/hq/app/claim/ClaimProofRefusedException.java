package net.officefloor.hq.app.claim;

/** A proof was added to a SETTLED or VOID claim, or to one changed meanwhile by another user. */
public class ClaimProofRefusedException extends Exception {

    public ClaimProofRefusedException(String message) {
        super(message);
    }
}
