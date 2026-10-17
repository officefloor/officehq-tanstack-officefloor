package net.officefloor.hq.app.claim;

/** The claim's status cannot make the requested change (SETTLED and VOID are final); nothing is changed. */
public class InvalidClaimStatusException extends Exception {
    public InvalidClaimStatusException(String message) {
        super(message);
    }
}
