package net.officefloor.hq.app.claim;

/** A claim was reminded while not OPEN or PENDING, or a second time on the same sheet's today. */
public class ClaimRemindRefusedException extends Exception {

    public ClaimRemindRefusedException(String message) {
        super(message);
    }
}
