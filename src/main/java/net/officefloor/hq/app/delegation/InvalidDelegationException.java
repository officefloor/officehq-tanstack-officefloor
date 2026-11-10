package net.officefloor.hq.app.delegation;

/** A delegation by anyone but a steward, with no user, or with a to-date before its from-date. */
public class InvalidDelegationException extends Exception {
    public InvalidDelegationException(String message) {
        super(message);
    }
}
