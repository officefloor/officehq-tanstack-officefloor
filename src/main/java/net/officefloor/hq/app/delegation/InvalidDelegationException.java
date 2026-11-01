package net.officefloor.hq.app.delegation;

/** The delegation is not made by a steward, names no user, or its dates are missing, malformed or out of order. */
public class InvalidDelegationException extends Exception {
    public InvalidDelegationException(String message) {
        super(message);
    }
}
