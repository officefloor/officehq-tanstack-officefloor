package net.officefloor.hq.app.role;

/** A delegation that cannot be made: not by a steward, a blank user, a bad date or a to-date before the from-date. */
public class InvalidDelegationException extends Exception {
    public InvalidDelegationException(String message) {
        super(message);
    }
}
