package net.officefloor.hq.app.claim;

/** A claim cannot move to the person asked: not by this user, not in its status, or not to that person. */
public class ClaimReassignRefusedException extends Exception {

    public ClaimReassignRefusedException(String message) {
        super(message);
    }
}
