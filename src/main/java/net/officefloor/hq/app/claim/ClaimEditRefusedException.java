package net.officefloor.hq.app.claim;

/** An edit of a claim that is no longer a DRAFT; answered 409. To change an open claim, void it and add a new one. */
public class ClaimEditRefusedException extends Exception {
    public ClaimEditRefusedException(ClaimStatus status) {
        super("A " + status + " claim cannot be edited: only a DRAFT can");
    }
}
